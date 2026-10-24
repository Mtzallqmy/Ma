package com.dawahayah.app.core.contentpacks

import com.dawahayah.app.core.security.IntegrityVerifier
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Caller uses Dispatchers.IO. A server that ignores Range safely restarts the file. */
class ResumableAssetDownloader(
    private val open: (String) -> HttpsURLConnection = { URL(it).openConnection() as HttpsURLConnection }
) {
    suspend fun download(url: String, file: File, size: Long, progress: suspend (Long) -> Unit = {}) {
        require(IntegrityVerifier.isSecureHttpsEndpoint(url) && size > 0)
        var offset = if (file.isFile && file.length() <= size) file.length() else 0L
        if (offset == size) return
        val connection = open(url)
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        connection.instanceFollowRedirects = false
        if (offset > 0) connection.setRequestProperty("Range", "bytes=$offset-")
        try {
            when (connection.responseCode) {
                200 -> offset = 0L
                206 -> {
                    val range = Regex("bytes (\\d+)-(\\d+)/(\\d+)").matchEntire(connection.getHeaderField("Content-Range") ?: "")
                        ?: throw IllegalArgumentException("Invalid Content-Range")
                    val values = range.groupValues.drop(1).map { it.toLongOrNull() ?: -1 }
                    require(values[0] == offset && values[1] in offset until size && values[2] == size) { "Range does not match expected asset" }
                }
                else -> throw IOException("Asset response HTTP ${connection.responseCode}")
            }
            var received = offset
            connection.inputStream.use { input ->
                FileOutputStream(file, offset > 0).buffered().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        if (n == 0) continue
                        received += n
                        require(received <= size) { "Asset exceeds declared size" }
                        output.write(buffer, 0, n)
                        progress(received)
                    }
                }
            }
            if (received != size) throw IOException("Incomplete asset; partial bytes retained for retry")
        } finally { connection.disconnect() }
    }
}

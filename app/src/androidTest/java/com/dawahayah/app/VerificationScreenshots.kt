package com.dawahayah.app

import android.content.Context
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Keep evidence outside app-owned storage, which AGP removes when uninstalling test APKs. */
object VerificationScreenshots {
    fun save(context: Context, name: String, bitmap: Bitmap) {
        require(name.matches(Regex("[a-z0-9-]+")))
        val file = File(context.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val destination = "/sdcard/Download/dawa-hayah-verification"
        val command = "mkdir -p '$destination' && cp '${file.absolutePath}' '$destination/$name.png' && echo copied"
        val descriptor = automation.executeShellCommand(command)
        val output = android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        check(output.trim() == "copied") { "Could not retain screenshot: $output" }
    }
}

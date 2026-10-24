package com.dawahayah.app.data.repository

import android.content.Context
import com.dawahayah.app.core.security.IntegrityVerifier
import com.dawahayah.app.data.local.*
import com.dawahayah.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AcademyRepository(private val context: Context, private val dao: AcademyDao,
    private val baseUrl: String, private val json: Json = Json { ignoreUnknownKeys = true }) {
    private val answerWrites = Mutex()
    val catalog = dao.observeCatalog().map { it?.let { row -> json.decodeFromString<AcademyBundle>(row.payload) } }
    val attempts = dao.observeAttempts()
    suspend fun ensureSeed() = withContext(Dispatchers.IO) {
        if (dao.catalog() == null) {
            val raw = context.assets.open("seed/academy_v1.json").bufferedReader().use { it.readText() }
            val bundle = json.decodeFromString<AcademyBundle>(raw)
            AcademyContracts.validate(bundle)
            dao.putCatalog(AcademyContentEntity("catalog", json.encodeToString(bundle)))
        }
    }
    suspend fun refresh() = withContext(Dispatchers.IO) {
        val url = baseUrl.trimEnd('/') + "/v1/academy/snapshot"
        require(IntegrityVerifier.isSecureHttpsEndpoint(url))
        val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS)
            .followRedirects(false).build()
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                val body = checkNotNull(response.body)
                // Bound before decoding even when a server omits Content-Length.
                val raw = body.byteStream().use { input ->
                    val out = java.io.ByteArrayOutputStream()
                    val buf = ByteArray(8192)
                    while (true) { val n = input.read(buf); if (n < 0) break; check(out.size()+n <= 2*1024*1024); out.write(buf,0,n) }
                    out.toString("UTF-8")
                }
                val bundle = json.decodeFromString<AcademyBundle>(raw)
                AcademyContracts.validate(bundle)
                // A complete catalog replaces only course content. Attempts and drafts remain intact.
                dao.putCatalog(AcademyContentEntity("catalog", json.encodeToString(bundle)))
            }
        } finally { client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown() }
    }
    suspend fun draft(lesson: AcademyLesson): Map<String, Int> = dao.draft(lesson.id)?.takeIf { it.lessonRevision == lesson.revision }
        ?.let { json.decodeFromString<Map<String, Int>>(it.answersJson) } ?: emptyMap()
    suspend fun saveDraft(lesson: AcademyLesson, answers: Map<String, Int>) = answerWrites.withLock {
        dao.putDraft(AcademyDraftEntity(lesson.id, lesson.revision, json.encodeToString(answers)))
    }
    suspend fun complete(lesson: AcademyLesson, answers: Map<String, Int>): AcademyAttemptEntity = answerWrites.withLock {
        val row = AcademyAttemptEntity(UUID.randomUUID().toString(),lesson.id,lesson.revision,
            json.encodeToString(answers),AcademyContracts.score(lesson,answers),lesson.questions.size,System.currentTimeMillis())
        dao.complete(row)
        row
    }
}

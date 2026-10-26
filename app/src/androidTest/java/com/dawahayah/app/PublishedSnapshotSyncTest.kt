package com.dawahayah.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.core.contentpacks.ContentPackManager
import com.dawahayah.app.core.errors.AppResult
import com.dawahayah.app.data.local.*
import com.dawahayah.app.data.remote.*
import com.dawahayah.app.data.sync.PublishedKnowledgeSynchronizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PublishedSnapshotSyncTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var db: DawaHayahDatabase
    private lateinit var seed: SeedBundleDto
    private lateinit var sync: PublishedKnowledgeSynchronizer

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, DawaHayahDatabase::class.java).build()
        SeedDatabaseInitializer(context, db.knowledgeDao()).ensureSeededIfEmpty()
        // Cloud transport schema v1 is independent of the bundled seed's Room-era version.
        seed = json.decodeFromString<SeedBundleDto>(context.assets.open("seed/base_seed_v1.json").bufferedReader().use { it.readText() }).copy(schemaVersion = 1)
        sync = PublishedKnowledgeSynchronizer(context, db, DisabledNeonDataSource())
        db.knowledgeDao().upsertBookmark(BookmarkEntity("organ-heart", "ORGAN", "القلب", "Heart", "", 1))
    }
    @After fun tearDown() { db.close() }
    private fun snapshot(bundle: SeedBundleDto = seed, revision: String = "a".repeat(64)) = PublishedSnapshot(1, true, revision, bundle)

    @Test fun editAndWithdrawalAreAtomicAndDoNotResurrectSeedOrDeleteBookmarks() = runBlocking {
        assertTrue(sync.applySnapshot(snapshot()) is AppResult.Success)
        val edited = seed.copy(organs = seed.organs.map { if (it.id == "organ-heart") it.copy(nameEn = "Updated heart") else it })
        assertTrue(sync.applySnapshot(snapshot(edited, "b".repeat(64))) is AppResult.Success)
        assertEquals("Updated heart", db.knowledgeDao().observeAllOrgans().first().first { it.id == "organ-heart" }.nameEn)
        val empty = seed.copy(systems = emptyList(), organs = emptyList(), diseases = emptyList(), activeIngredients = emptyList())
        assertTrue(sync.applySnapshot(snapshot(empty, "c".repeat(64))) is AppResult.Success)
        SeedDatabaseInitializer(context, db.knowledgeDao()).ensureSeededIfEmpty()
        assertEquals(0, db.knowledgeDao().getBodySystemsCount())
        assertTrue(db.knowledgeDao().observeAllOrgans().first().isEmpty())
        assertTrue(db.knowledgeDao().observeAllDiseases().first().isEmpty())
        assertEquals(1, db.knowledgeDao().observeBookmarks().first().size)
    }

    @Test fun partialResponseAndFailedTransactionKeepPreviousContentAndCursor() = runBlocking {
        assertTrue(sync.applySnapshot(snapshot()) is AppResult.Success)
        val before = db.knowledgeDao().getSyncValue("snapshot")
        assertTrue(sync.applySnapshot(snapshot().copy(complete = false)) is AppResult.Error)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_broken BEFORE INSERT ON organs WHEN NEW.nameEn='Broken' BEGIN SELECT RAISE(ABORT,'test failure'); END")
        val broken = seed.copy(organs = seed.organs.map { it.copy(nameEn = "Broken") })
        assertTrue(sync.applySnapshot(snapshot(broken,"d".repeat(64))) is AppResult.Error)
        assertEquals(before, db.knowledgeDao().getSyncValue("snapshot"))
        assertEquals(seed.organs.size, db.knowledgeDao().observeAllOrgans().first().size)
        assertFalse(db.knowledgeDao().observeAllOrgans().first().any { it.nameEn == "Broken" })
    }

    @Test fun liveNeonPublishedSnapshotIsDecodedCommittedAndReadableWithoutAnotherRequest() = runBlocking {
        val base = "https://br-steep-moon-b9kxm3uf-api.compute.c-7.eu-central-1.aws.neon.tech/"
        val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
        try {
            val api = Retrofit.Builder().baseUrl(base).client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(NeonApiService::class.java)
            val source = RetrofitNeonDataSource(base, api)
            val result = PublishedKnowledgeSynchronizer(context, db, source).refresh()
            assertTrue("Live snapshot failed: $result", result is AppResult.Success)
            val persisted = json.decodeFromString<PublishedSnapshot>(db.knowledgeDao().getSyncValue("snapshot")!!)
            assertTrue(persisted.complete)
            assertEquals(13, persisted.bundle.systems.size)
            assertTrue("Every published system has an organ", persisted.bundle.systems.all { system ->
                persisted.bundle.organs.any { it.systemId == system.id }
            })
            val skin = persisted.bundle.organs.first { it.id == "organ-skin" }
            assertTrue(skin.sections.any { it.bodyAr.isNotBlank() && it.bodyEn.isNotBlank() })
            assertTrue(persisted.bundle.sources.any { it.id in skin.sourceIds && it.url.startsWith("https://openstax.org/") })
            assertFalse(persisted.bundle.diseases.any { it.reviewStatus == "DRAFT" })
            assertEquals(persisted.bundle.systems.size, db.knowledgeDao().getBodySystemsCount())
            val dao = db.knowledgeDao()
            val packId = "pack-cardio-histology-ar-v1"
            dao.upsertPackRecord(dao.getPackRecordById(packId)!!.copy(isBundledOfflineDemoAvailable = false))
            val installed = ContentPackManager(context, dao, source).installPack(packId)
            assertTrue("Actual cloud download failed: $installed", installed is AppResult.Success)
            assertTrue(dao.observeAssetsForPack(packId).first().all { java.io.File(it.absolutePersistentPath).isFile })
            assertTrue(dao.observeLocalContents().first().any { it.origin == packId && it.reviewStatus == "EDUCATIONAL_UNREVIEWED" })
            // Close networking before proving that subsequent reads use SQLite only.
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
            assertTrue(db.knowledgeDao().observeAllOrgans().first().isNotEmpty())
            assertEquals(1, db.knowledgeDao().observeBookmarks().first().size)
        } finally { client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown() }
    }
}

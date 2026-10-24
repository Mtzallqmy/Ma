package com.dawahayah.app

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.core.contentpacks.ContentPackManager
import com.dawahayah.app.core.errors.AppResult
import com.dawahayah.app.data.local.*
import com.dawahayah.app.data.remote.DisabledNeonDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/** Real SQLite + real bundled files, with the cloud deliberately disabled. */
@RunWith(AndroidJUnit4::class)
class OfflinePersistenceTest {
    private lateinit var context: Context
    private lateinit var db: DawaHayahDatabase
    private lateinit var manager: ContentPackManager
    private val packId = "pack-cardio-histology-ar-v1"
    private val databaseName = "offline-persistence-test.db"

    @Before fun setUp() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val isolatedFiles = File(app.cacheDir, "offline-persistence-test").apply { deleteRecursively(); mkdirs() }
        context = object : ContextWrapper(app) { override fun getFilesDir() = isolatedFiles }
        context.deleteDatabase(databaseName)
        openDatabase()
        SeedDatabaseInitializer(context, db.knowledgeDao()).ensureSeededIfEmpty()
        manager = ContentPackManager(context, db.knowledgeDao(), DisabledNeonDataSource())
    }

    private fun openDatabase() {
        db = Room.databaseBuilder(context, DawaHayahDatabase::class.java, databaseName)
            .addMigrations(DawaHayahDatabase.MIGRATION_1_2, DawaHayahDatabase.MIGRATION_2_3, DawaHayahDatabase.MIGRATION_3_4).build()
    }

    @After fun tearDown() { db.close(); context.deleteDatabase(databaseName); context.filesDir.deleteRecursively() }

    @Test fun seedSearchBookmarksAndReadingSurviveDatabaseReopen() = runBlocking {
        val dao = db.knowledgeDao()
        assertEquals(13, dao.getBodySystemsCount())
        assertTrue(dao.searchOrgans("قلب").isNotEmpty())
        dao.upsertBookmark(BookmarkEntity("cor", "ORGAN", "القلب", "Heart", "", 1))
        dao.upsertReadingProgress(ReadingProgressEntity("cor", "ORGAN", "القلب", "Heart", 65, 2))
        db.close()
        openDatabase()
        SeedDatabaseInitializer(context, db.knowledgeDao()).ensureSeededIfEmpty()
        assertEquals(13, db.knowledgeDao().getBodySystemsCount())
        assertEquals("cor", db.knowledgeDao().observeBookmarks().first().single().itemId)
        assertEquals(65, db.knowledgeDao().observeReadingProgress().first().single().progressPercent)
    }

    @Test fun verifiedOfflineInstallPersistsAndDeleteKeepsSeedAndBookmark() = runBlocking {
        assertTrue(manager.installPack(packId) is AppResult.Success)
        val dao = db.knowledgeDao()
        val assets = dao.observeAssetsForPack(packId).first()
        assertTrue(assets.isNotEmpty())
        assertTrue(assets.all { File(it.absolutePersistentPath).isFile })
        assertTrue(dao.observeLocalContents().first().any { it.origin == packId })
        dao.upsertBookmark(BookmarkEntity("cor", "ORGAN", "القلب", "Heart", "", 1))
        db.close()
        openDatabase()
        manager = ContentPackManager(context, db.knowledgeDao(), DisabledNeonDataSource())
        assertNotNull(db.knowledgeDao().getPackRecordById(packId)?.installedAt)
        assertTrue(manager.deleteInstalledPack(packId) is AppResult.Success)
        assertTrue(db.knowledgeDao().observeAssetsForPack(packId).first().isEmpty())
        assertFalse(db.knowledgeDao().observeLocalContents().first().any { it.origin == packId })
        assertEquals(13, db.knowledgeDao().getBodySystemsCount())
        assertEquals(1, db.knowledgeDao().observeBookmarks().first().size)
    }

    @Test fun corruptReinstallRetainsLastVerifiedFilesAndLesson() = runBlocking {
        assertTrue(manager.installPack(packId) is AppResult.Success)
        val dao = db.knowledgeDao()
        val original = dao.observeAssetsForPack(packId).first()
        assertTrue(manager.installPack(packId, simulateCorruptedHash = true) is AppResult.Error)
        assertEquals(original, dao.observeAssetsForPack(packId).first())
        assertTrue(original.all { File(it.absolutePersistentPath).isFile })
        assertEquals("COMPLETED", dao.getPackRecordById(packId)?.installState)
        assertTrue(dao.observeLocalContents().first().any { it.origin == packId })
    }
    @Test fun failedRoomCommitLeavesPreviousGenerationReadable() = runBlocking {
        assertTrue(manager.installPack(packId) is AppResult.Success)
        val dao = db.knowledgeDao()
        val original = dao.observeAssetsForPack(packId).first()
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_asset_replace BEFORE DELETE ON local_assets BEGIN SELECT RAISE(ABORT, 'injected transaction failure'); END"
        )
        try {
            assertTrue(manager.installPack(packId) is AppResult.Error)
            assertEquals(original, dao.observeAssetsForPack(packId).first())
            assertTrue(original.all { File(it.absolutePersistentPath).isFile })
            assertEquals("COMPLETED", dao.getPackRecordById(packId)?.installState)
            assertTrue(dao.observeLocalContents().first().any { it.origin == packId })
        } finally {
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_asset_replace")
        }
    }

}

package com.dawahayah.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Reconstructs the documented legacy columns; Room validates the entire migrated v3 schema. */
@RunWith(AndroidJUnit4::class)
class RoomUpgradeTest {
    @Test fun versionOneAndTwoUpgradeWithoutLosingUserData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (legacyVersion in listOf(1, 2, 3, 4, 5, 6)) {
            val name = "upgrade-from-$legacyVersion.db"
            context.deleteDatabase(name)
            fun open() = Room.databaseBuilder(context, DawaHayahDatabase::class.java, name)
                .addMigrations(DawaHayahDatabase.MIGRATION_1_2, DawaHayahDatabase.MIGRATION_2_3, DawaHayahDatabase.MIGRATION_3_4, DawaHayahDatabase.MIGRATION_4_5, DawaHayahDatabase.MIGRATION_5_6, DawaHayahDatabase.MIGRATION_6_7).build()
            var db = open()
            SeedDatabaseInitializer(context, db.knowledgeDao()).ensureSeededIfEmpty()
            db.knowledgeDao().upsertBookmark(BookmarkEntity("cor", "ORGAN", "القلب", "Heart", "", 1))
            db.knowledgeDao().upsertReadingProgress(ReadingProgressEntity("cor", "ORGAN", "القلب", "Heart", 75, 2))
            db.close()
            val raw = SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, 0)
            raw.beginTransaction()
            try {
                if (legacyVersion < 3) {
                rebuildWithout(raw, "body_systems", setOf("hasDetailedPathway", "interSystemRelationsAr", "interSystemRelationsEn", "relatedDrugClassesCsv"))
                rebuildWithout(raw, "organs", setOf("locationAr", "locationEn", "has3dAsset", "asset3dId", "relatedDiseaseIdsCsv", "relatedDrugClassesCsv"))
                rebuildWithout(raw, "diseases", setOf("icd11Code", "secondarySystemIdsCsv", "affectedOrganIdsCsv", "relatedDrugClassesCsv", "importedAt", "humanReviewedAt"))
                if (legacyVersion == 1) rebuildWithout(raw, "pack_records", setOf("companionDependencyNotice"))
                listOf("anatomical_structures", "anatomy_labels", "organ_sections", "science_figures", "anatomy_3d_assets", "disease_sections", "disease_system_links", "disease_organ_links", "source_attributions", "room_master_table")
                    .forEach { raw.execSQL("DROP TABLE `$it`") }
                }
                if (legacyVersion < 5) rebuildWithout(raw, "active_ingredients", setOf("detailsJson"))
                if (legacyVersion < 6) listOf("academy_content", "academy_attempts", "academy_drafts").forEach { raw.execSQL("DROP TABLE `$it`") }
                if (legacyVersion < 4) raw.execSQL("DROP TABLE IF EXISTS sync_state")
                else raw.execSQL("DELETE FROM sync_state WHERE `key`='medicine_metadata_seed'")
                raw.execSQL("DROP TABLE user_notes")
                raw.version = legacyVersion
                raw.setTransactionSuccessful()
            } finally { raw.endTransaction(); raw.close() }
            try {
                db = open()
                // Opening a DAO triggers Room's post-migration column and index validation.
                assertEquals(13, db.knowledgeDao().getBodySystemsCount())
                assertEquals("cor", db.knowledgeDao().observeBookmarks().first().single().itemId)
                assertEquals(75, db.knowledgeDao().observeReadingProgress().first().single().progressPercent)
                SeedDatabaseInitializer(context, db.knowledgeDao()).ensureSeededIfEmpty()
                assertTrue(db.knowledgeDao().getAnatomicalStructuresCount() > 0)
            } finally { db.close(); context.deleteDatabase(name) }
        }
    }

    private fun rebuildWithout(db: SQLiteDatabase, table: String, removed: Set<String>) {
        val columns = mutableListOf<Pair<String, String>>()
        db.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1)
                if (name in removed) continue
                val type = cursor.getString(2)
                val required = if (cursor.getInt(3) == 1) " NOT NULL" else ""
                val primary = if (cursor.getInt(5) > 0) " PRIMARY KEY" else ""
                columns += name to "`$name` $type$required$primary"
            }
        }
        val indexes = mutableListOf<String>()
        db.rawQuery("SELECT sql FROM sqlite_master WHERE type='index' AND tbl_name=? AND sql IS NOT NULL", arrayOf(table)).use {
            while (it.moveToNext()) indexes += it.getString(0)
        }
        db.execSQL("CREATE TABLE `${table}_legacy` (${columns.joinToString { it.second }})")
        val names = columns.joinToString { "`${it.first}`" }
        db.execSQL("INSERT INTO `${table}_legacy` ($names) SELECT $names FROM `$table`")
        db.execSQL("DROP TABLE `$table`")
        db.execSQL("ALTER TABLE `${table}_legacy` RENAME TO `$table`")
        indexes.forEach { db.execSQL(it) }
    }
}

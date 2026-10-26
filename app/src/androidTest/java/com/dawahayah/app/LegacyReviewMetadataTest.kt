package com.dawahayah.app
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class LegacyReviewMetadataTest {
 @Test fun unsupportedSeedStampsAreRemovedWithoutTouchingQualifiedStatus()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,DawaHayahDatabase::class.java).build()
  try {
   val initializer=SeedDatabaseInitializer(context,db.knowledgeDao());initializer.ensureSeededIfEmpty()
   val original = db.knowledgeDao().observeAllScienceFigures().first().first { it.id == "fig-cor-conduction-v1" }
   val editedFigure = db.knowledgeDao().observeAllScienceFigures().first().first { it.id == "fig-lungs-alveoli-v1" }.copy(titleEn="Independently edited title")
   db.knowledgeDao().insertScienceFigures(listOf(original.copy(titleEn="Verified Anatomical Diagram: Four Chambers & Conduction Pathway of the Heart (Cor)",provenanceType="VERIFIED_SCIENTIFIC_DIAGRAM"),editedFigure))
   db.openHelper.writableDatabase.execSQL("DELETE FROM sync_state WHERE `key`='legacy_schematic_captions_v1'")
   initializer.ensureSeededIfEmpty()
   val corrected = db.knowledgeDao().observeAllScienceFigures().first().associateBy { it.id }
   assertEquals("SCHEMATIC_EDUCATIONAL",corrected.getValue(original.id).provenanceType)
   assertEquals(original.titleEn,corrected.getValue(original.id).titleEn)
   assertEquals(editedFigure.titleEn,corrected.getValue(editedFigure.id).titleEn)
   val diseases=db.knowledgeDao().observeAllDiseases().first();assertTrue(diseases.all{it.reviewStatus=="EDUCATIONAL_UNREVIEWED"&&it.humanReviewedAt==null})
   val first=diseases[0].id;val reviewed=diseases[1].id
   db.openHelper.writableDatabase.execSQL("UPDATE diseases SET reviewStatus='verified',humanReviewedAt='2026-10-22' WHERE id=?",arrayOf(first))
   db.openHelper.writableDatabase.execSQL("UPDATE diseases SET reviewStatus='CLINICALLY_REVIEWED_PUBLISHED',humanReviewedAt='2026-10-23' WHERE id=?",arrayOf(reviewed))
   db.openHelper.writableDatabase.execSQL("DELETE FROM sync_state WHERE `key`='legacy_seed_review_cleanup_v2'")
   initializer.ensureSeededIfEmpty()
   val after=db.knowledgeDao().observeAllDiseases().first().associateBy{it.id}
   assertEquals("EDUCATIONAL_UNREVIEWED",after.getValue(first).reviewStatus);assertNull(after.getValue(first).humanReviewedAt)
   assertEquals("CLINICALLY_REVIEWED_PUBLISHED",after.getValue(reviewed).reviewStatus);assertEquals("2026-10-23",after.getValue(reviewed).humanReviewedAt)
  }finally{db.close()}
 }
}

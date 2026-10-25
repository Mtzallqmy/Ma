package com.dawahayah.app
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.data.local.*
import com.dawahayah.app.data.repository.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserBackupTest {
 @Test fun personalDataRoundTripsAndNewerRecordsWin()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>();val first=Room.inMemoryDatabaseBuilder(context,DawaHayahDatabase::class.java).build();val second=Room.inMemoryDatabaseBuilder(context,DawaHayahDatabase::class.java).build()
  try {
   val bookmark=BookmarkEntity("organ-heart","ORGAN","القلب","Heart","",1)
   first.knowledgeDao().upsertBookmark(bookmark);first.knowledgeDao().upsertReadingProgress(ReadingProgressEntity("organ-heart","ORGAN","القلب","Heart",50,1))
   first.academyDao().insertAttempt(AcademyAttemptEntity("backup-fixture","academy-units",1,"{\"q\":0}",1,2,1))
   first.knowledgeDao().upsertNote(UserNoteEntity("organ-heart","ORGAN","Personal learning note",1))
   val bytes=UserBackupRepository(first).export();assertFalse(String(bytes).contains("deviceToken"))
   val repository=UserBackupRepository(second);assertEquals(4,repository.restore(bytes));assertEquals(4,repository.restore(bytes))
   assertEquals("Personal learning note",second.knowledgeDao().allNotes().single().text)
   second.knowledgeDao().upsertNote(UserNoteEntity("organ-heart","ORGAN","",2));repository.restore(bytes);assertEquals("",second.knowledgeDao().allNotes().single().text)
   assertEquals(1,second.academyDao().allAttempts().size);assertEquals(50,second.knowledgeDao().allReadingProgress().single().progressPercent)
   second.knowledgeDao().upsertBookmark(bookmark.copy(titleEn="Newer",savedAtEpochMillis=2));repository.restore(bytes);assertEquals("Newer",second.knowledgeDao().allBookmarks().single().titleEn)
  }finally{first.close();second.close()}
 }
 @Test fun failedRestoreRollsBackEveryRecord()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,DawaHayahDatabase::class.java).build()
  try {
   db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_backup BEFORE INSERT ON bookmarks WHEN NEW.itemId='reject' BEGIN SELECT RAISE(ABORT,'fixture failure'); END")
   val backup=UserBackup(bookmarks=listOf(BookmarkEntity("first","ORGAN","أ","A","",1),BookmarkEntity("reject","ORGAN","ب","B","",1)),reading=emptyList(),attempts=emptyList())
   try{UserBackupRepository(db).restore(Json.encodeToString(backup).toByteArray());fail("Expected rollback")}catch(_:Exception){}
   assertTrue(db.knowledgeDao().allBookmarks().isEmpty())
  }finally{db.close()}
 }
}

package com.dawahayah.app.data.repository

import androidx.room.withTransaction
import com.dawahayah.app.data.local.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class UserBackup(val schemaVersion:Int=2,val bookmarks:List<BookmarkEntity>,val reading:List<ReadingProgressEntity>,val attempts:List<AcademyAttemptEntity>,val notes:List<UserNoteEntity> = emptyList())
class UserBackupRepository(private val db:DawaHayahDatabase) {
 private val json=Json{ignoreUnknownKeys=true}
 suspend fun export():ByteArray {
  val backup=db.withTransaction{UserBackup(bookmarks=db.knowledgeDao().allBookmarks(),reading=db.knowledgeDao().allReadingProgress(),attempts=db.academyDao().allAttempts(),notes=db.knowledgeDao().allNotes())}
  return json.encodeToString(backup).toByteArray(Charsets.UTF_8).also{require(it.size<=MAX_BYTES)}
 }
 suspend fun restore(bytes:ByteArray):Int {
  require(bytes.size<=MAX_BYTES)
  val data=json.decodeFromString<UserBackup>(String(bytes,Charsets.UTF_8));require(data.schemaVersion in 1..2)
  require(data.bookmarks.size<=10000&&data.reading.size<=10000&&data.attempts.size<=1000&&data.notes.size<=10000)
  require(data.bookmarks.map{it.itemId}.distinct().size==data.bookmarks.size&&data.reading.map{it.itemId}.distinct().size==data.reading.size&&data.attempts.map{it.id}.distinct().size==data.attempts.size)
  fun id(v:String)=v.matches(Regex("[A-Za-z0-9._:-]{1,256}"))
  fun time(v:Long)=v>=0&&v<=System.currentTimeMillis()+86400000
  require(data.notes.map{it.itemId}.distinct().size==data.notes.size)
  data.notes.forEach{require(id(it.itemId)&&it.itemType.matches(Regex("[A-Z_]{1,40}"))&&it.text.length<=20000&&time(it.updatedAt))}
  data.bookmarks.forEach{require(id(it.itemId)&&it.itemType.matches(Regex("[A-Z_]{1,40}"))&&it.titleAr.length<=4096&&it.titleEn.length<=4096&&it.subtitle.length<=4096&&time(it.savedAtEpochMillis))}
  data.reading.forEach{require(id(it.itemId)&&it.itemType.matches(Regex("[A-Z_]{1,40}"))&&it.titleAr.length<=4096&&it.titleEn.length<=4096&&it.progressPercent in 0..100&&time(it.updatedAtEpochMillis))}
  data.attempts.forEach{require(id(it.id)&&id(it.lessonId)&&it.lessonRevision>0&&it.total in 1..100&&it.correct in 0..it.total&&it.answersJson.length<=16384&&time(it.completedAt));require(json.parseToJsonElement(it.answersJson) is kotlinx.serialization.json.JsonObject)}
  db.withTransaction {
   val dao=db.knowledgeDao();val bookmarks=dao.allBookmarks().associateBy{it.itemId};val reading=dao.allReadingProgress().associateBy{it.itemId};val attempts=db.academyDao().allAttempts().associateBy{it.id}
   data.attempts.forEach{row->attempts[row.id]?.let{old->require(old.copy(answersJson="")==row.copy(answersJson="")&&json.parseToJsonElement(old.answersJson)==json.parseToJsonElement(row.answersJson)){"Conflicting attempt"}}}
   data.bookmarks.filter{(bookmarks[it.itemId]?.savedAtEpochMillis?:-1)<it.savedAtEpochMillis}.forEach{dao.upsertBookmark(it)}
   data.reading.filter{(reading[it.itemId]?.updatedAtEpochMillis?:-1)<it.updatedAtEpochMillis}.forEach{dao.upsertReadingProgress(it)}
   val notes=dao.allNotes().associateBy{it.itemId}
   data.notes.filter{(notes[it.itemId]?.updatedAt?:-1)<it.updatedAt}.forEach{dao.upsertNote(it)}
   db.academyDao().mergeAttempts(data.attempts)
  }
  return data.bookmarks.size+data.reading.size+data.attempts.size+data.notes.size
 }
 companion object {const val MAX_BYTES=5*1024*1024}
}

package com.dawahayah.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "academy_content")
data class AcademyContentEntity(@PrimaryKey val id: String, val payload: String)
@kotlinx.serialization.Serializable
@Entity(tableName = "academy_attempts", indices = [Index("lessonId")])
data class AcademyAttemptEntity(@PrimaryKey val id: String, val lessonId: String, val lessonRevision: Int,
    val answersJson: String, val correct: Int, val total: Int, val completedAt: Long)
@Entity(tableName = "academy_drafts")
data class AcademyDraftEntity(@PrimaryKey val lessonId: String, val lessonRevision: Int, val answersJson: String)

@Dao abstract class AcademyDao {
    @Query("SELECT * FROM academy_content WHERE id='catalog'") abstract fun observeCatalog(): Flow<AcademyContentEntity?>
    @Query("SELECT * FROM academy_content WHERE id='catalog'") abstract suspend fun catalog(): AcademyContentEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putCatalog(value: AcademyContentEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun seedCatalog(value: AcademyContentEntity): Long
    @Query("SELECT * FROM academy_attempts ORDER BY completedAt DESC") abstract fun observeAttempts(): Flow<List<AcademyAttemptEntity>>
    @Query("SELECT * FROM academy_attempts ORDER BY completedAt") abstract suspend fun allAttempts(): List<AcademyAttemptEntity>
    @Transaction open suspend fun mergeAttempts(rows: List<AcademyAttemptEntity>) { rows.forEach { insertAttempt(it) } }
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertAttempt(value: AcademyAttemptEntity): Long
    @Query("SELECT * FROM academy_drafts WHERE lessonId=:id") abstract suspend fun draft(id: String): AcademyDraftEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putDraft(value: AcademyDraftEntity)
    @Query("DELETE FROM academy_drafts WHERE lessonId=:id") abstract suspend fun deleteDraft(id: String)
    @Transaction open suspend fun complete(value: AcademyAttemptEntity) { insertAttempt(value); deleteDraft(value.lessonId) }
}

package com.dawahayah.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.data.local.DawaHayahDatabase
import com.dawahayah.app.data.repository.AcademyRepository
import com.dawahayah.app.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AcademyOfflineTest {
    @Test fun draftsScoresAndLessonRevisionsSurviveReopenWithoutNetwork() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="academy-offline-test.db";context.deleteDatabase(name)
        fun open()=Room.databaseBuilder(context,DawaHayahDatabase::class.java,name).build()
        var db=open()
        fun repo()=AcademyRepository(context,db.academyDao(),"https://unreachable.invalid/")
        try {
            var repository=repo();repository.ensureSeed()
            val lesson=repository.catalog.first { it!=null }!!.lessons.first {it.id=="academy-units"}
            val first=mapOf(lesson.questions.first().id to lesson.questions.first().correctIndex)
            repository.saveDraft(lesson,first);db.close();db=open();repository=repo()
            assertEquals(first,repository.draft(lesson))
            val answers=lesson.questions.associate {it.id to it.correctIndex}
            val attempt=repository.complete(lesson,answers)
            assertEquals(2,attempt.correct);assertTrue(repository.draft(lesson).isEmpty())
            db.close();db=open();repository=repo()
            val persisted=repository.attempts.first().single()
            assertEquals(attempt,persisted);assertEquals(lesson.revision,persisted.lessonRevision)
            try {AcademyContracts.score(lesson,first);fail()}catch(_:IllegalArgumentException){}
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun liveAcademyCatalogIsValidatedStoredAndReadableOffline()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,DawaHayahDatabase::class.java).build()
        try {
            val repository=AcademyRepository(context,db.academyDao(),BuildConfig.NEON_PUBLIC_API_BASE_URL)
            repository.ensureSeed();repository.refresh()
            val bundle=repository.catalog.first {it!=null}!!;AcademyContracts.validate(bundle)
            assertEquals(9,bundle.lessons.size)
            assertTrue(bundle.lessons.all {it.reviewStatus=="EDUCATIONAL_UNREVIEWED"})
        }finally {db.close()}
    }
}

package com.dawahayah.app
import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.core.contentpacks.*
import com.dawahayah.app.core.errors.AppResult
import com.dawahayah.app.data.local.*
import com.dawahayah.app.data.remote.DisabledNeonDataSource
import com.dawahayah.app.domain.model.ContentPackManifest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class BundledPackContractsTest {
 @Test fun everyBundledManifestMatchesTheActualInstallerAndRespiratoryPackInstalls()=runBlocking {
  val base=ApplicationProvider.getApplicationContext<Context>();val sandbox=java.io.File(base.cacheDir,"bundled-pack-test-${System.nanoTime()}").apply{mkdirs()};val context=object:ContextWrapper(base){override fun getFilesDir()=sandbox};val json=Json{ignoreUnknownKeys=true}
  for(id in context.assets.list("packs")!!.toList()){
   val raw=context.assets.open("packs/$id/manifest.json").bufferedReader().use{it.readText()}
   val manifest=json.decodeFromString<ContentPackManifest>(raw);assertTrue("Invalid bundled contract: $id",PackManifestValidator.isValid(manifest,id))
   for(entry in manifest.files){val bytes=context.assets.open("packs/$id/${entry.path}").use{it.readBytes()};assertEquals(entry.sizeBytes,bytes.size.toLong());assertEquals(entry.sha256,com.dawahayah.app.core.security.IntegrityVerifier.computeSha256(bytes))}
  }
  val db=Room.inMemoryDatabaseBuilder(context,DawaHayahDatabase::class.java).build()
  try {
   SeedDatabaseInitializer(context,db.knowledgeDao()).ensureSeededIfEmpty()
   val model=ContentPackManager(context,db.knowledgeDao(),DisabledNeonDataSource()).installPack("heart-model-low-v1");assertTrue(model.toString(),model is AppResult.Success)
   val result=ContentPackManager(context,db.knowledgeDao(),DisabledNeonDataSource()).installPack("respiratory-diseases-ar-v1")
   assertTrue(result.toString(),result is AppResult.Success)
   val record=db.knowledgeDao().getPackRecordById("respiratory-diseases-ar-v1")!!;assertEquals("1.0.1",record.version)
   val lesson=db.knowledgeDao().observeLocalContents().first().single{it.origin=="respiratory-diseases-ar-v1"};assertEquals("EDUCATIONAL_UNREVIEWED",lesson.reviewStatus)
  }finally{db.close();sandbox.deleteRecursively()}
 }
}

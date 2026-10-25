package com.dawahayah.app

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.core.contentpacks.ContentPackManager
import com.dawahayah.app.core.errors.AppResult
import com.dawahayah.app.data.local.*
import com.dawahayah.app.features.body.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class SourcedAnatomyTest {
    @Test fun realNeonPackDownloadsAndActualFiveMeshGlbRendersOffline(): Unit = runBlocking {
        val app=ApplicationProvider.getApplicationContext<DawaHayahApplication>()
        val sandbox=File(app.filesDir,"sourced-anatomy-test-${System.nanoTime()}").apply{mkdirs()}
        val context=object:ContextWrapper(app){override fun getFilesDir()=sandbox}
        val db=Room.inMemoryDatabaseBuilder(context,DawaHayahDatabase::class.java).build()
        try {
            val manager=ContentPackManager(context,db.knowledgeDao(),app.container.remoteDataSource)
            val catalog=manager.discoverCloudPacks("ar");assertTrue(catalog.toString(),catalog is AppResult.Success)
            val result=manager.installPack("bodyparts-cardiac-v1");assertTrue(result.toString(),result is AppResult.Success)
            val asset=db.knowledgeDao().observeAssetsForPack("bodyparts-cardiac-v1").first().single()
            val meta=JSONObject(context.assets.open("anatomy/bodyparts-cardiac-source.json").bufferedReader().use{it.readText()})
            assertEquals(meta.getString("sha256"),asset.sha256)
            val parts=meta.getJSONArray("parts");val names=(0 until parts.length()).map{parts.getJSONObject(it).getString("node")}
            val descriptor=VerifiedAnatomyModel(File(asset.absolutePersistentPath),meta.getLong("sizeBytes"),meta.getString("sha256"),names)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val ready=CountDownLatch(1);val failure=AtomicReference<String?>(null);val count=AtomicInteger()
                lateinit var viewer:LocalAnatomyWebView
                scenario.onActivity { activity ->
                    viewer=LocalAnatomyWebView(activity,onMeshSelected={},onFallback={failure.set(it);ready.countDown()},onModelLoaded={count.set(it);ready.countDown()},verifiedModel=descriptor)
                    activity.setContentView(viewer)
                }
                try {
                    assertTrue("Source GLB timeout",ready.await(40,TimeUnit.SECONDS));assertNull(failure.get());assertEquals(5,count.get())
                    scenario.onActivity {assertTrue(viewer.settings.blockNetworkLoads);viewer.updateControls("Valva_mitralis",false,true,true,setOf("Aorta_ascendens"))}
                } finally {scenario.onActivity{viewer.releaseViewer()}}
            }
        } finally {db.close();sandbox.deleteRecursively()}
    }
}

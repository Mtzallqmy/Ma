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
                    val drawn=CountDownLatch(1)
                    scenario.onActivity { viewer.postOnAnimation { viewer.postOnAnimation { drawn.countDown() } } }
                    assertTrue("First visible frame timeout",drawn.await(10,TimeUnit.SECONDS))
                    val instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    fun modelPixels(bitmap:android.graphics.Bitmap):Int {
                        var count=0
                        for(y in bitmap.height/5 until bitmap.height*4/5 step 3) {
                            for(x in bitmap.width/10 until bitmap.width*9/10 step 3) {
                                val pixel=bitmap.getPixel(x,y)
                                val red=android.graphics.Color.red(pixel);val green=android.graphics.Color.green(pixel)
                                if(red>50 && red>green*1.4)count++
                            }
                        }
                        return count
                    }
                    var screenshot=instrumentation.uiAutomation.takeScreenshot()
                    val deadline=android.os.SystemClock.uptimeMillis()+5000
                    while((screenshot==null || modelPixels(screenshot)<50) && android.os.SystemClock.uptimeMillis()<deadline) {
                        android.os.SystemClock.sleep(100)
                        screenshot=instrumentation.uiAutomation.takeScreenshot()
                    }
                    assertNotNull("Model screenshot",screenshot)
                    val visibleScreenshot=requireNotNull(screenshot)
                    VerificationScreenshots.save(app,"bodyparts-five-meshes",visibleScreenshot)
                    assertTrue("Loaded meshes must produce visible coloured anatomy pixels",modelPixels(visibleScreenshot)>=50)
                    scenario.onActivity {assertTrue(viewer.settings.blockNetworkLoads);viewer.updateControls("Valva_mitralis",false,true,true,setOf("Aorta_ascendens"))}
                } finally {scenario.onActivity{viewer.releaseViewer()}}
            }
        } finally {db.close();sandbox.deleteRecursively()}
    }
}

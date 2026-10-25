package com.dawahayah.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.core.storage.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OverhaulJourneyTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun actualNeonSyncSearchReaderNoteAndRecreation() {
        val container=(compose.activity.application as DawaHayahApplication).container
        runBlocking {container.preferencesDataStore.setLanguage(AppLanguage.ARABIC)}
        compose.waitUntil(20000) {compose.onAllNodesWithTag("home_search").fetchSemanticsNodes().isNotEmpty()}
        screenshot("home-ar")
        val before=runBlocking{container.database.knowledgeDao().getSyncValue("last_sync_at")}
        compose.onNodeWithText("تحديث المحتوى").performScrollTo().performClick()
        compose.waitUntil(60000) {runBlocking{container.database.knowledgeDao().getSyncValue("last_sync_at")}?.let{it!=before}==true}
        val persisted=runBlocking{container.database.knowledgeDao().getSyncValue("snapshot")}
        assertNotNull("HTTPS response persisted in Room",persisted)
        assertTrue(persisted!!.contains("organ-heart"))
        compose.onNodeWithContentDescription("البحث العام").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Cor")
        compose.waitUntil(15000){compose.onAllNodesWithText("القلب",substring=false).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("القلب",substring=false).performScrollTo().performClick()
        compose.onNodeWithTag("knowledge_reader").assertExists()
        compose.onNodeWithTag("reader_note").performScrollTo().performTextInput("ملاحظة اختبار القراءة")
        compose.onNodeWithTag("save_note").performScrollTo().performClick()
        compose.waitUntil(15000){runBlocking{container.database.knowledgeDao().allNotes()}.any{it.itemId=="organ-heart"&&it.text.contains("ملاحظة اختبار القراءة")}}
        compose.activityRule.scenario.recreate()
        compose.waitUntil(20000){compose.onAllNodesWithTag("knowledge_reader").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("reader_note").performScrollTo().assertTextContains("ملاحظة اختبار القراءة",substring=true)
        screenshot("reader-ar")
    }
    private fun screenshot(name:String) {
        val directory=File(compose.activity.getExternalFilesDir(null),"verification").also{it.mkdirs()}
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
}

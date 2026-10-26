package com.dawahayah.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.core.storage.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverhaulJourneyTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun actualNeonSyncSearchReaderNoteAndRecreation() {
        val container=(compose.activity.application as DawaHayahApplication).container
        runBlocking {container.preferencesDataStore.setLanguage(AppLanguage.ARABIC)}
        compose.waitUntil(20000) {compose.onAllNodesWithTag("home_search").fetchSemanticsNodes().isNotEmpty()}
        val before=runBlocking{container.database.knowledgeDao().getSyncValue("last_sync_at")}
        compose.onNodeWithTag("home_feed").performScrollToNode(hasText("تحديث المحتوى"))
        compose.onNodeWithText("تحديث المحتوى").performClick()
        compose.waitUntil(60000) {runBlocking{container.database.knowledgeDao().getSyncValue("last_sync_at")}?.let{it!=before}==true}
        val persisted=runBlocking{container.database.knowledgeDao().getSyncValue("snapshot")}
        assertNotNull("HTTPS response persisted in Room",persisted)
        assertTrue(persisted!!.contains("organ-heart"))
        compose.onNodeWithTag("home_feed").performScrollToIndex(0)
        screenshot("home-ar")
        compose.onNodeWithContentDescription("البحث العام").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Cor")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitUntil(15000){compose.onAllNodesWithTag("search_has_results").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("search_results").performScrollToNode(hasTestTag("hit_organ-heart"))
        screenshot("search-ar")
        compose.onNodeWithTag("hit_organ-heart").performClick()
        compose.onNodeWithTag("knowledge_reader").assertExists()
        compose.onNodeWithTag("knowledge_reader").performScrollToNode(hasTestTag("reader_note"))
        compose.onNodeWithTag("reader_note").performTextInput("ملاحظة اختبار القراءة")
        compose.onNodeWithTag("knowledge_reader").performScrollToNode(hasTestTag("save_note"))
        compose.onNodeWithTag("save_note").performClick()
        compose.waitUntil(15000){runBlocking{container.database.knowledgeDao().allNotes()}.any{it.itemId=="organ-heart"&&it.text.contains("ملاحظة اختبار القراءة")}}
        compose.activityRule.scenario.recreate()
        compose.waitUntil(20000){compose.onAllNodesWithTag("knowledge_reader").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("knowledge_reader").performScrollToNode(hasTestTag("reader_note"))
        compose.onNodeWithTag("reader_note").assertTextContains("ملاحظة اختبار القراءة",substring=true)
        screenshot("reader-ar")
    }
    private fun screenshot(name:String) {
        VerificationScreenshots.capture(compose.activity, name)
    }
}

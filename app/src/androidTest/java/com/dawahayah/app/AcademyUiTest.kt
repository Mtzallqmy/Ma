package com.dawahayah.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AcademyUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun academyLessonQuizIsInteractiveAndSavesScore() {
        compose.onNodeWithContentDescription("أكاديمية الصيدلة").performClick()
        compose.waitUntil(20000) {compose.onAllNodesWithText("الوحدات والتحليل البُعدي").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("الوحدات والتحليل البُعدي").performClick()
        compose.onNodeWithText("250").performScrollTo().performClick()
        compose.onNodeWithText("mg إلى mL").performScrollTo().performClick()
        compose.onNodeWithText("تسليم وحفظ النتيجة").performScrollTo().performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithText("النتيجة المحفوظة: 2/2").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("النتيجة المحفوظة: 2/2").assertExists()
    }
}

package com.dawahayah.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicineLibraryUiTest {
 @get:Rule val compose = createAndroidComposeRule<MainActivity>()
 @Test fun searchByRxNormOpensIngredientIdentityAndReference() {
  compose.waitUntil(20000) {compose.onAllNodesWithText("الأدوية").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("الأدوية").performClick()
  compose.waitUntil(20000) {compose.onAllNodesWithText("الاسم العلمي، التجاري أو المعرّف").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("الاسم العلمي، التجاري أو المعرّف").performTextInput("6809")
  compose.waitUntil(10000) {compose.onAllNodesWithText("ميتفورمين").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("ميتفورمين").assertExists().performClick()
  compose.onNodeWithText("المعرّفات").performClick()
  compose.onNodeWithText("NLM RxNorm — active ingredient identifiers").assertExists()
  compose.onNodeWithText("نسخ الهوية").assertExists()
 }
}

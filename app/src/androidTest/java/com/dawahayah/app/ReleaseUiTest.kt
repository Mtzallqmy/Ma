package com.dawahayah.app

import android.os.Debug
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.core.storage.*
import com.dawahayah.app.features.settings.PRIVACY_EN
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReleaseUiTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Test fun englishDarkLargeTextPrivacyAndSettingsSurviveRecreation() {
  val preferences=(compose.activity.application as DawaHayahApplication).container.preferencesDataStore
  try {
   runBlocking {preferences.setLanguage(AppLanguage.ENGLISH);preferences.setThemeMode(AppThemeMode.DARK);preferences.setFontScale(1.4f)}
   compose.waitUntil(15000){compose.onAllNodesWithContentDescription("Settings & About").fetchSemanticsNodes().isNotEmpty()}
   VerificationScreenshots.save(compose.activity,"home-en-dark-large",compose.onRoot().captureToImage().asAndroidBitmap())
   compose.onNodeWithContentDescription("Settings & About").performClick()
   compose.onNodeWithText("Privacy and data policy").performScrollTo().performClick()
   compose.onNodeWithText(PRIVACY_EN).assertExists()
   compose.activityRule.scenario.recreate()
   compose.waitUntil(15000){compose.onAllNodesWithContentDescription("Settings & About").fetchSemanticsNodes().isNotEmpty()}
   val persisted=runBlocking{preferences.settingsFlow.first()}
   assertEquals(AppLanguage.ENGLISH,persisted.language);assertEquals(AppThemeMode.DARK,persisted.themeMode);assertEquals(1.4f,persisted.fontScale)
   val memory=Debug.MemoryInfo();Debug.getMemoryInfo(memory);Log.i("ReleaseVerification","API=${android.os.Build.VERSION.SDK_INT}; totalPssKiB=${memory.totalPss}; large-font/dark/English/recreation passed")
  }finally {runBlocking {preferences.setLanguage(AppLanguage.ARABIC);preferences.setThemeMode(AppThemeMode.LIGHT);preferences.setFontScale(1f)}}
 }
}

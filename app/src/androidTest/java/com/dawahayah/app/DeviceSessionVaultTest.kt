package com.dawahayah.app
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.core.security.DeviceSessionVault
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class DeviceSessionVaultTest {
 @Test fun credentialsAreEncryptedAndCorruptStorageFailsClosed(){
  val context=ApplicationProvider.getApplicationContext<Context>();val vault=DeviceSessionVault(context);val prefs=context.getSharedPreferences("learner_device_session",Context.MODE_PRIVATE)
  try {vault.clear();assertNull(vault.read());val token="a".repeat(64);vault.save(token);assertEquals(token,vault.read());assertFalse(prefs.getString("value","")!!.contains(token));prefs.edit().putString("value","broken").commit();assertNull(vault.read());assertNull(prefs.getString("value",null))}finally{vault.clear()}
 }
}

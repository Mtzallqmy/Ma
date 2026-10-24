package com.dawahayah.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-only opaque learner session; never grants editorial permissions. */
class DeviceSessionVault(context: Context) {
 private val prefs=context.getSharedPreferences("learner_device_session",Context.MODE_PRIVATE)
 private fun key(): SecretKey {
  val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
  (store.getKey("dawa-learner-session",null) as? SecretKey)?.let{return it}
  return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{init(KeyGenParameterSpec.Builder("dawa-learner-session",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())}.generateKey()
 }
 fun save(token:String) {
  require(token.matches(Regex("[a-f0-9]{64}")))
  val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply{init(Cipher.ENCRYPT_MODE,key())}
  prefs.edit().putString("value",Base64.encodeToString(cipher.iv+cipher.doFinal(token.toByteArray()),Base64.NO_WRAP)).apply()
 }
 fun read():String?=try {
  prefs.getString("value",null)?.let{v->val raw=Base64.decode(v,Base64.NO_WRAP);val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply{init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,raw.copyOfRange(0,12)))};String(cipher.doFinal(raw.copyOfRange(12,raw.size)))}
 }catch(_:Exception){clear();null}
 fun clear(){prefs.edit().clear().apply()}
}

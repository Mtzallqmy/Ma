package com.dawahayah.app.data.repository

import android.content.Context
import com.dawahayah.app.core.security.DeviceSessionVault
import com.dawahayah.app.data.local.AcademyDao
import com.dawahayah.app.data.local.AcademyAttemptEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable data class DeviceLink(val deviceCode:String,val userCode:String,val expiresIn:Int)
@Serializable private data class AttemptBatch(val attempts:List<AcademyAttemptEntity>)
class LearnerSyncRepository(context:Context,private val dao:AcademyDao) {
 private val vault=DeviceSessionVault(context)
 private val json=Json{ignoreUnknownKeys=true}
 private val base="https://br-steep-moon-b9kxm3uf-academy.compute.c-7.eu-central-1.aws.neon.tech"
 private suspend fun post(path:String,body:String,authenticated:Boolean=false):Pair<Int,String> = withContext(Dispatchers.IO) {
  val client=OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(40,TimeUnit.SECONDS).followRedirects(false).build()
  try {
   val request=Request.Builder().url(base+path).post(body.toRequestBody("application/json".toMediaType()))
   if(authenticated)request.header("Authorization","Device "+checkNotNull(vault.read()){"Sign in first"})
   client.newCall(request.build()).execute().use{r->val input=checkNotNull(r.body).byteStream();val bytes=input.use{it.readBytesLimited(2*1024*1024)};r.code to String(bytes,Charsets.UTF_8)}
  }finally {client.connectionPool.evictAll();client.dispatcher.executorService.shutdown()}
 }
 suspend fun start():DeviceLink {val (code,raw)=post("/v1/link/start","{}");check(code==200);return json.decodeFromString(raw)}
 fun approvalUrl(link:DeviceLink):String {require(link.userCode.matches(Regex("[a-f0-9]{32}")));return "$base/?connect=${link.userCode}"}
 suspend fun finish(link:DeviceLink):Boolean {
  require(link.deviceCode.matches(Regex("[a-f0-9]{64}")))
  val (code,raw)=post("/v1/link/poll",buildJsonObject{put("deviceCode",link.deviceCode)}.toString())
  if(code==202)return false
  check(code==200){"Link expired; start again"};vault.save(json.parseToJsonElement(raw).jsonObject.getValue("deviceToken").jsonPrimitive.content);return true
 }
 suspend fun sync():Int {
  val local=dao.allAttempts();check(local.size<=1000)
  val chunks=local.chunked(100).ifEmpty{listOf(emptyList())};var remote=emptyList<AcademyAttemptEntity>()
  for(chunk in chunks){val(code,raw)=post("/v1/learner/sync",json.encodeToString(AttemptBatch(chunk)),true);check(code==200){if(code==409)"Attempt conflict; local data retained" else "Sync failed; local data retained"};remote=json.decodeFromString<AttemptBatch>(raw).attempts}
  check(remote.size<=1000&&remote.map{it.id}.distinct().size==remote.size)
  for(r in remote){check(r.id.matches(Regex("[a-zA-Z0-9-]{1,80}"))&&r.lessonRevision>0&&r.correct in 0..r.total&&r.total in 1..100&&r.completedAt>=0);val existing=local.firstOrNull{it.id==r.id};if(existing!=null)check(existing.copy(answersJson="")==r.copy(answersJson="")&&json.parseToJsonElement(existing.answersJson)==json.parseToJsonElement(r.answersJson)){"Attempt conflict"}}
  dao.mergeAttempts(remote);return remote.size
 }
 suspend fun signOut(){try{post("/v1/learner/sign-out","{}",true)}finally{vault.clear()}}
 private fun java.io.InputStream.readBytesLimited(max:Int):ByteArray {val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=read(buffer);if(n<0)break;check(out.size()+n<=max);out.write(buffer,0,n)};return out.toByteArray()}
}

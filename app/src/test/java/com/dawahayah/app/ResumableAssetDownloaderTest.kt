package com.dawahayah.app
import com.dawahayah.app.core.contentpacks.ResumableAssetDownloader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

class ResumableAssetDownloaderTest {
 private class Reply(private val code:Int,private val payload:ByteArray,private val range:String?=null):HttpsURLConnection(URL("https://example.org/asset")) {
  override fun getResponseCode()=code
  override fun getInputStream():InputStream=ByteArrayInputStream(payload)
  override fun getHeaderField(name:String)=if(name=="Content-Range")range else null
  override fun disconnect(){}
  override fun usingProxy()=false
  override fun connect(){}
  override fun getCipherSuite()="test"
  override fun getLocalCertificates():Array<Certificate>?=null
  override fun getServerCertificates():Array<Certificate> = emptyArray()
 }
 @Test fun resumesExpectedRangeAndRechecksTotalSize()=runBlocking {
  val file=File.createTempFile("resume",".partial")
  try {file.writeText("abc");val reply=Reply(206,"def".toByteArray(),"bytes 3-5/6")
   ResumableAssetDownloader{reply}.download("https://example.org/asset",file,6)
   assertEquals("bytes=3-",reply.getRequestProperty("Range"));assertEquals("abcdef",file.readText())
  }finally{file.delete()}
 }
 @Test fun serverIgnoringRangeRestartsRatherThanAppending()=runBlocking {
  val file=File.createTempFile("restart",".partial")
  try{file.writeText("abc");ResumableAssetDownloader{Reply(200,"abcdef".toByteArray())}.download("https://example.org/asset",file,6);assertEquals("abcdef",file.readText())}finally{file.delete()}
 }
 @Test fun wrongRangeCannotReplacePartialFile()=runBlocking {
  val file=File.createTempFile("wrong-range",".partial")
  try{file.writeText("abc");try{ResumableAssetDownloader{Reply(206,"def".toByteArray(),"bytes 2-4/6")}.download("https://example.org/asset",file,6);fail()}catch(_:IllegalArgumentException){};assertEquals("abc",file.readText())}finally{file.delete()}
 }
 @Test fun prematureEndRetainsBytesForRetry()=runBlocking {
  val file=File.createTempFile("interrupted",".partial")
  try{try{ResumableAssetDownloader{Reply(200,"abc".toByteArray())}.download("https://example.org/asset",file,6);fail()}catch(_:IOException){};assertEquals("abc",file.readText())}finally{file.delete()}
 }
}

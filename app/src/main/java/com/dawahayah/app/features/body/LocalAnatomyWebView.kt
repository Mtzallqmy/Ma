package com.dawahayah.app.features.body

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import com.dawahayah.app.core.contentpacks.Stage01AnatomyAssetPlayer
import com.dawahayah.app.core.security.IntegrityVerifier
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** Only trusted, bundled HTML can access this bridge. Callbacks are dispatched on the UI thread. */
@Keep
class AnatomyJavascriptBridge(
    private val selected: (String) -> Unit,
    private val fallback: (String) -> Unit,
    private val loaded: (Int) -> Unit
) {
    @JavascriptInterface fun onMeshSelected(name: String) = selected(name)
    @JavascriptInterface fun onWebGlFallback(reason: String) = fallback(reason)
    @JavascriptInterface fun onModelLoaded(meshCount: Int) = loaded(meshCount)
}

@SuppressLint("SetJavaScriptEnabled")
class LocalAnatomyWebView(
    context: Context,
    private val onMeshSelected: (String) -> Unit,
    private val onFallback: (String) -> Unit,
    private val onModelLoaded: (Int) -> Unit = {},
    private val verifiedModel: VerifiedAnatomyModel? = null
) : WebView(context) {
    private val scope = MainScope()
    private val contract = Stage01AnatomyAssetPlayer.STAGE_01_REGISTERED_3D_CONTRACTS
        .first { it.localRelativePath == MODEL_PATH }
    private val meshNames = verifiedModel?.meshNames ?: contract.selectableSubMeshes
    private var released = false
    private var pageReady = false
    private var selectedMesh = "Ventriculus_Sinister"
    private var transparent = false
    private var isolated = false
    private var dark = true
    private var hidden = emptySet<String>()

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            allowFileAccess = true
            allowContentAccess = false
            blockNetworkLoads = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        addJavascriptInterface(AnatomyJavascriptBridge(
            selected = { name -> post { if (!released && name in meshNames) onMeshSelected(name) } },
            fallback = { reason -> post { if (!released) onFallback(reason) } },
            loaded = { count -> post {
                if (!released) {
                    if (count == meshNames.size) { applyControls(); onModelLoaded(count) }
                    else onFallback("Unexpected mesh count")
                }
            } }
        ), BRIDGE_NAME)
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.url.toString() != VIEWER_URL

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                if (request.url.toString() == VIEWER_URL) null
                else if(request.url.toString()=="file:///android_asset/viewer3d/anatomy_math.js")
                    WebResourceResponse("application/javascript", "utf-8", context.assets.open("viewer3d/anatomy_math.js"))
                else WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))

            override fun onPageFinished(view: WebView, url: String) {
                if (released || url != VIEWER_URL) return
                pageReady = true
                scope.launch {
                    try {
                        val encoded = withContext(Dispatchers.IO) {
                            val model = verifiedModel
                            val bytes = if(model==null) context.assets.open(MODEL_PATH).use { it.readBytes() }
                                else {
                                    check(model.file.canonicalPath.startsWith(context.filesDir.canonicalPath+java.io.File.separator))
                                    check(model.sizeBytes in 1..8_388_608 && model.file.length()==model.sizeBytes)
                                    model.file.readBytes()
                                }
                            check(bytes.size.toLong() == (model?.sizeBytes ?: contract.actualSizeBytes) &&
                                IntegrityVerifier.verifySha256(bytes, model?.sha256 ?: contract.sha256Checksum)) { "Model integrity failed" }
                            Base64.encodeToString(bytes, Base64.NO_WRAP)
                        }
                        if (!released) evaluateJavascript("window.loadGlbFromBase64 && window.loadGlbFromBase64(${JSONObject.quote(encoded)});", null)
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { if (!released) onFallback("Model unavailable or checksum mismatch") }
                }
            }
        }
        loadUrl(VIEWER_URL)
    }

    fun updateControls(mesh: String, xray: Boolean, isolate: Boolean, darkCanvas: Boolean, hiddenMeshes: Set<String>) {
        selectedMesh = mesh
        transparent = xray
        isolated = isolate
        dark = darkCanvas
        hidden = hiddenMeshes
        applyControls()
    }

    private fun applyControls() {
        if (!pageReady || released) return
        val layers = meshNames.joinToString(";") {
            "v.toggleLayer(${JSONObject.quote(it)},${it !in hidden})"
        }
        evaluateJavascript("(function(){var v=window.viewerControl;if(!v)return;v.selectMesh(${JSONObject.quote(selectedMesh)});v.setTransparentMode($transparent);v.setIsolateMode($isolated);v.setDarkCanvas($dark);$layers;})();", null)
    }

    fun releaseViewer() {
        released = true
        scope.cancel()
        stopLoading()
        removeJavascriptInterface(BRIDGE_NAME)
        destroy()
    }

    companion object {
        const val VIEWER_URL = "file:///android_asset/viewer3d/anatomy_viewer.html"
        const val MODEL_PATH = "models/cor_chambers_low_v1.glb"
        const val BRIDGE_NAME = "AndroidAnatomyBridge"
    }
}

data class VerifiedAnatomyModel(val file:java.io.File,val sizeBytes:Long,val sha256:String,val meshNames:List<String>)

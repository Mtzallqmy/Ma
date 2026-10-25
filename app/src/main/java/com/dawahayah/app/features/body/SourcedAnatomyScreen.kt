package com.dawahayah.app.features.body

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dawahayah.app.DawaHayahApplication
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.domain.model.PackInstallState
import com.dawahayah.app.features.settings.MainUiState
import org.json.JSONObject
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourcedAnatomyScreen(state:MainUiState,onInstall:(String)->Unit,onRefresh:()->Unit,onOpen:(String,String)->Unit) {
    val context=LocalContext.current
    val container=(context.applicationContext as DawaHayahApplication).container
    val ar=state.settings.language==AppLanguage.ARABIC
    val uri=LocalUriHandler.current
    val metadata=remember {JSONObject(context.assets.open("anatomy/bodyparts-cardiac-source.json").bufferedReader().use{it.readText()})}
    val parts=remember {val array=metadata.getJSONArray("parts");(0 until array.length()).map{array.getJSONObject(it)}}
    val assets by container.database.knowledgeDao().observeAssetsForPack("bodyparts-cardiac-v1").collectAsState(initial=emptyList())
    val asset=assets.firstOrNull{it.assetId==metadata.getString("assetId")}
    val pack=state.packs.firstOrNull{it.id=="bodyparts-cardiac-v1"}
    var selected by remember{mutableStateOf(parts.first().getString("node"))}
    var hidden by remember{mutableStateOf(emptySet<String>())}
    var isolate by remember{mutableStateOf(false)}
    var loaded by remember(asset?.absolutePersistentPath){mutableStateOf(false)}
    var failed by remember(asset?.absolutePersistentPath){mutableStateOf(false)}
    var viewer by remember{mutableStateOf<LocalAnatomyWebView?>(null)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Text(if(ar) "تراكيب قلبية من BodyParts3D" else "BodyParts3D cardiac structures",style=MaterialTheme.typography.headlineLarge)
            Text(if(ar) "خمسة تراكيب أصلية: صمامات القلب والأبهر. هذه مجموعة جزئية وليست نموذجًا كاملًا للقلب. الألوان تعليمية، والمجسمات ليست أداة تشخيص." else "Five sourced valve and aortic structures. This is a subset, not a complete heart. Colours aid study; models are not a diagnostic tool.")
        }
        if(asset==null) item {
            Text(if(ar) "الحزمة اختيارية: ${metadata.getLong("sizeBytes")/1024} كيلوبايت. تبقى متاحة دون اتصال بعد تثبيتها." else "Optional pack: ${metadata.getLong("sizeBytes")/1024} KiB. Available offline after installation.")
            if(pack==null)Button(onClick=onRefresh,enabled=!state.isRefreshingCloud) {Text(if(ar) "جلب الحزم المتاحة" else "Find available packs")}
            else Button(onClick={onInstall(pack.id)},enabled=pack.installState!=PackInstallState.RUNNING) {Text(if(pack.installState==PackInstallState.RUNNING){if(ar) "جارٍ التنزيل…" else "Downloading…"}else{if(ar) "تنزيل المجسمات" else "Download models"})}
            pack?.lastErrorMessage?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        } else if(!failed) item {
            key(asset!!.absolutePersistentPath) {
                AndroidView(modifier=Modifier.fillMaxWidth().height(360.dp).testTag("sourced_glb"),factory={ctx->
                    LocalAnatomyWebView(ctx,onMeshSelected={selected=it},onFallback={failed=true},onModelLoaded={loaded=true},
                        verifiedModel=VerifiedAnatomyModel(File(asset!!.absolutePersistentPath),metadata.getLong("sizeBytes"),metadata.getString("sha256"),parts.map{it.getString("node")})).also{viewer=it}
                },update={it.updateControls(selected,false,isolate,true,hidden)},onRelease={it.releaseViewer();viewer=null})
            }
            Text(if(loaded){if(ar) "تم التحقق من الملف وتحميل التراكيب الخمسة" else "Verified file; all five parts loaded"}else{if(ar) "جارٍ فحص المجسم وفتحه…" else "Verifying and loading model…"})
            Text(if(ar) "اسحب بإصبع للتدوير؛ استخدم إصبعين للتكبير والتحريك؛ انقر على سطح الجزء لتحديده." else "Drag to orbit; use two fingers to zoom and pan; tap a part's surface to select it.")
            TextButton(onClick={viewer?.evaluateJavascript("window.viewerControl.resetCamera()",null)}){Text(if(ar) "إعادة ضبط العرض" else "Reset view")}
        } else item {
            Text(if(ar) "تعذر عرض المجسم على هذا الجهاز؛ الرسم التشريحي البديل متاح أدناه." else "The model could not be rendered on this device; the anatomy plate is available below.")
            AnatomyPlate(ar)
        }
        item {
            Text(if(ar) "الأجزاء والطبقات" else "Parts & layers",style=MaterialTheme.typography.titleLarge)
            FilterChip(selected=isolate,onClick={isolate=!isolate},label={Text(if(ar) "عزل الجزء المحدد" else "Isolate selection")})
            parts.forEach { part ->
                val node=part.getString("node")
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected=selected==node,onClick={selected=node},label={Text(part.getString(if(ar) "nameAr" else "nameEn"))})
                    TextButton(onClick={hidden=if(node in hidden)hidden-node else hidden+node}) {Text(if(node in hidden){if(ar) "إظهار" else "Show"}else{if(ar) "إخفاء" else "Hide"})}
                }
                if(selected==node)Text("${part.getString("fma")} · ${part.getString("nameEn")}",style=MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick={onOpen("ORGAN","organ-heart")}){Text(if(ar) "اقرأ عن القلب والمواضيع المرتبطة" else "Read about the heart and related topics")}
        }
        item {
            Text(metadata.getString("attribution"),style=MaterialTheme.typography.bodySmall)
            Text(metadata.getString("changes"),style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={uri.openUri(metadata.getString("sourceUrl"))}){Text(if(ar) "المصدر وملفات الأصل" else "Source geometry")}
            TextButton(onClick={uri.openUri(metadata.getString("licenseUrl"))}){Text(metadata.getString("license"))}
        }
    }
}

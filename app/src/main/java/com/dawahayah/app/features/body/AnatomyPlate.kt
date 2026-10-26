package com.dawahayah.app.features.body

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Original attributed OpenStax plate, displayed natively, with bounded pan and zoom. */
@Composable
fun AnatomyPlate(isAr:Boolean) {
    val context=LocalContext.current
    val uri=LocalUriHandler.current
    val present=remember {runCatching{context.assets.open("anatomy/openstax-heart.jpg").use{it.read()!=-1}}.getOrDefault(false)}
    var zoom by remember{mutableFloatStateOf(1f)}
    var pan by remember{mutableStateOf(Offset.Zero)}
    var imageFailed by remember{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(if(isAr) "التشريح الداخلي للقلب — OpenStax" else "Internal heart anatomy — OpenStax",style=MaterialTheme.typography.titleLarge)
        if(present && !imageFailed) {
            Box(Modifier.fillMaxWidth().height(300.dp).clipToBounds().testTag("anatomical_plate").pointerInput(Unit){
                detectTransformGestures{_,movement,scale,_->
                    zoom=(zoom*scale).coerceIn(1f,5f)
                    val next=pan+movement
                    pan=Offset(next.x.coerceIn(-size.width*(zoom-1)/2,size.width*(zoom-1)/2),next.y.coerceIn(-size.height*(zoom-1)/2,size.height*(zoom-1)/2))
                }
            }) {
                AsyncImage(model="file:///android_asset/anatomy/openstax-heart.jpg",contentDescription=if(isAr) "رسم مرخص لتشريح القلب الداخلي، من OpenStax" else "Licensed OpenStax internal heart anatomy illustration",onError={imageFailed=true},modifier=Modifier.fillMaxSize().graphicsLayer{scaleX=zoom;scaleY=zoom;translationX=pan.x;translationY=pan.y})
            }
            Text(if(isAr) "باعد بين إصبعين للتكبير واسحب لتحريك الرسم." else "Pinch to zoom and drag to pan.",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={zoom=1f;pan=Offset.Zero}) {Text(if(isAr) "إعادة ضبط الرسم" else "Reset image")}
        } else Text(if(isAr) "الرسم غير متاح في هذه النسخة؛ يمكنك فتح المصدر الأصلي." else "The plate is unavailable in this build; open the original source.")
        Text("OpenStax College · CC BY 3.0 · "+if(isAr) "الصورة الأصلية دون تعديل" else "Unmodified original",style=MaterialTheme.typography.labelSmall)
        TextButton(onClick={uri.openUri("https://commons.wikimedia.org/wiki/File:2008_Internal_Anatomy_of_the_HeartN.jpg")}){Text(if(isAr) "المصدر" else "Source")}
        TextButton(onClick={uri.openUri("https://creativecommons.org/licenses/by/3.0/")}){Text(if(isAr) "شروط الترخيص" else "License")}
    }
}

package com.dawahayah.app.features.academy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.security.IntegrityVerifier
import com.dawahayah.app.data.repository.AcademyRepository
import com.dawahayah.app.domain.model.*
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AcademyScreen(repository: AcademyRepository, isAr: Boolean) {
    val bundle by repository.catalog.collectAsState(initial = null)
    val attempts by repository.attempts.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context=LocalContext.current
    val learner=(context.applicationContext as com.dawahayah.app.DawaHayahApplication).container.learnerSyncRepository
    val browser=LocalUriHandler.current
    var link by remember {mutableStateOf<com.dawahayah.app.data.repository.DeviceLink?>(null)}
    var accountOpen by rememberSaveable {mutableStateOf(false)}
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var calculators by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(repository) { try { repository.ensureSeed() } catch (_: Exception) { message = if(isAr) "تعذر فتح المحتوى المحلي" else "Cannot open local content" } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(if(isAr) "أكاديمية الصيدلة" else "Pharmacy Academy", style = MaterialTheme.typography.headlineMedium)
        Text(if(isAr) "تعليم قيد المراجعة العلمية؛ الأمثلة افتراضية وليست وصفات علاجية." else "Education awaiting scientific review; fictional examples are not prescriptions.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={ selected=null; calculators=false }) { Text(if(isAr) "الدروس" else "Lessons") }
            TextButton(onClick={ selected=null; calculators=true }) { Text(if(isAr) "الحاسبات" else "Calculators") }
            TextButton(enabled=!busy, onClick={ scope.launch { busy=true; try { repository.refresh(); message=if(isAr) "حُفظت الدروس للعمل دون إنترنت" else "Lessons saved for offline use" } catch (_: Exception) { message=if(isAr) "تعذر التحديث؛ النسخة المحلية محفوظة" else "Update failed; local content retained" } finally { busy=false } } }) { Text(if(isAr) "تحديث" else "Update") }
        }
        TextButton(onClick={accountOpen=!accountOpen}) {Text(if(isAr) "الحساب ومزامنة النتائج" else "Account and score sync")}
        if(accountOpen) {
            Text(if(isAr) "تُنقل محاولات التعلم لهذا الحساب عند طلب المزامنة فقط. لا تدخل معلومات مرضى." else "Learning attempts are transferred to this account only when you request sync. Do not enter patient data.")
            TextButton(enabled=!busy,onClick={scope.launch{busy=true;try{link=learner.start();browser.openUri(learner.approvalUrl(link!!))}catch(_:Exception){message=if(isAr) "تعذر بدء تسجيل الدخول" else "Cannot start sign-in"}finally{busy=false}}}){Text(if(isAr) "تسجيل الدخول في المتصفح" else "Sign in using browser")}
            if(link!=null)TextButton(enabled=!busy,onClick={scope.launch{busy=true;try{val done=learner.finish(link!!);message=if(done){link=null;if(isAr) "تم ربط الحساب" else "Account connected"}else{if(isAr) "أكمل الموافقة في المتصفح أولًا" else "Approve in the browser first"}}catch(_:Exception){link=null;message=if(isAr) "انتهى طلب الربط؛ ابدأ مجددًا" else "Link expired; start again"}finally{busy=false}}}){Text(if(isAr) "أكملت الموافقة" else "I approved the connection")}
            TextButton(enabled=!busy,onClick={scope.launch{busy=true;try{val count=learner.sync();message=if(isAr) "تمت المزامنة: $count محاولة" else "Synced: $count attempts"}catch(_:Exception){message=if(isAr) "تعذرت المزامنة؛ النتائج المحلية محفوظة. تحقق من الحساب أو تعارض المحاولات." else "Sync failed; local attempts retained. Check sign-in or attempt conflicts."}finally{busy=false}}}){Text(if(isAr) "مزامنة محاولات هذا الهاتف" else "Sync this device’s attempts")}
            TextButton(enabled=!busy,onClick={scope.launch{busy=true;try{learner.signOut()}catch(_:Exception){}finally{busy=false;link=null;message=if(isAr) "حُذفت جلسة هذا الهاتف" else "Device session removed"}}}){Text(if(isAr) "تسجيل الخروج" else "Sign out")}
        }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(message.isNotEmpty()) Text(message, style=MaterialTheme.typography.labelMedium)
        if(calculators) {
            CalculatorPanel(isAr)
        } else {
            val data=bundle
            val lesson=data?.lessons?.firstOrNull { it.id==selected }
            if(lesson!=null && data!=null) {
                LessonPanel(lesson,data,repository,isAr)
            } else if(data!=null) {
                LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    data.courses.forEach { course ->
                        item { Text(course.title.get(isAr),style=MaterialTheme.typography.titleLarge);Text(course.description.get(isAr)) }
                        items(data.lessons.filter { it.courseId==course.id },key={it.id}) { l ->
                            ElevatedCard(onClick={selected=l.id},modifier=Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(l.title.get(isAr),style=MaterialTheme.typography.titleMedium)
                                    val latest=attempts.firstOrNull { it.lessonId==l.id && it.lessonRevision==l.revision }
                                    Text(if(latest==null) { if(isAr) "درس واختبار محفوظان محليًا" else "Offline lesson and quiz" }
                                        else { if(isAr) "آخر نتيجة: ${latest.correct}/${latest.total}" else "Latest score: ${latest.correct}/${latest.total}" })
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LessonPanel(lesson: AcademyLesson, bundle: AcademyBundle, repository: AcademyRepository, isAr: Boolean) {
    val scope=rememberCoroutineScope()
    val uri=LocalUriHandler.current
    var answers by remember(lesson.id,lesson.revision) { mutableStateOf<Map<String,Int>>(emptyMap()) }
    var ready by remember(lesson.id,lesson.revision) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var score by remember(lesson.id,lesson.revision) { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(lesson.id,lesson.revision) { answers=repository.draft(lesson);ready=true }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(lesson.title.get(isAr),style=MaterialTheme.typography.headlineSmall)
        if(lesson.isSimulation) Text(if(isAr) "محاكاة تعليمية — لا تخص مريضًا حقيقيًا" else "Educational simulation — not a real patient")
        Text(if(isAr) "أهداف التعلم" else "Learning outcomes",style=MaterialTheme.typography.titleMedium)
        lesson.outcomes.forEach { Text("• "+it.get(isAr)) }
        Text(lesson.body.get(isAr));Text(lesson.example.get(isAr),style=MaterialTheme.typography.bodyLarge)
        Text(if(isAr) "اختبار الدرس" else "Lesson quiz",style=MaterialTheme.typography.titleLarge)
        lesson.questions.forEach { q ->
            Text(q.prompt.get(isAr),style=MaterialTheme.typography.titleMedium)
            q.options.forEachIndexed { index, option ->
                Row(Modifier.fillMaxWidth().clickable(enabled=ready && !saving && score==null) {
                    val next=answers + (q.id to index);answers=next
                    scope.launch { try { repository.saveDraft(lesson,next) } catch (_: Exception) { error=if(isAr) "تعذر حفظ الإجابة" else "Cannot save answer" } }
                }) {
                    RadioButton(selected=answers[q.id]==index,onClick=null)
                    Text(option.get(isAr),Modifier.padding(top=12.dp))
                }
            }
            if(score!=null) Text((if(isAr) "التفسير: " else "Explanation: ")+q.explanation.get(isAr))
        }
        if(error.isNotEmpty())Text(error)
        Button(enabled=ready && !saving && score==null && answers.size==lesson.questions.size,onClick={ scope.launch {
            saving=true;try { score=repository.complete(lesson,answers).correct } catch (_: Exception) {error=if(isAr) "تعذر حفظ المحاولة" else "Cannot save attempt"} finally { saving=false }
        } }) { Text(if(isAr) "تسليم وحفظ النتيجة" else "Submit and save score") }
        score?.let { Text(if(isAr) "النتيجة المحفوظة: $it/${lesson.questions.size}" else "Saved score: $it/${lesson.questions.size}") }
        Text(if(isAr) "المصادر والحقوق" else "Sources and rights",style=MaterialTheme.typography.titleMedium)
        lesson.sourceIds.forEach { id -> bundle.sources.firstOrNull { it.id==id }?.let { source ->
            Text(source.title);Text(source.attribution,style=MaterialTheme.typography.bodySmall)
            if(IntegrityVerifier.isSecureHttpsEndpoint(source.url))TextButton(onClick={uri.openUri(source.url)}) {Text(if(isAr) "فتح المصدر" else "Open source")}
        } }
        Text(lesson.license+" · "+lesson.attribution,style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
    }
}

private fun number(value: String): Double = value.map { c -> when(c) {
    in '٠'..'٩' -> '0'+(c-'٠'); in '۰'..'۹' -> '0'+(c-'۰'); '٫',',' -> '.'; else -> c
} }.joinToString("").trim().toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number")

@Composable
fun CalculatorPanel(isAr: Boolean) {
    var mode by rememberSaveable { mutableStateOf(0) }
    var first by rememberSaveable { mutableStateOf("") };var second by rememberSaveable { mutableStateOf("") };var third by rememberSaveable { mutableStateOf("") }
    var from by rememberSaveable { mutableStateOf("G") };var to by rememberSaveable { mutableStateOf("MG") }
    var output by remember { mutableStateOf("") }
    val labels=if(isAr) listOf("الوحدات","التخفيف","نصف العمر") else listOf("Units","Dilution","Half-life")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { labels.forEachIndexed { i,label -> FilterChip(selected=mode==i,onClick={mode=i;output=""},label={Text(label)}) } }
        Text(when(mode){0->if(isAr) "تحويل كتلة أو حجم ضمن البُعد نفسه" else "Convert mass or volume within the same dimension";1->"C1 × V1 = C2 × V2";else->"remaining % = 100 × (1/2)^(t/T)"})
        OutlinedTextField(value=first,onValueChange={first=it;output=""},label={Text(when(mode){0->if(isAr) "القيمة" else "Value";1->"C1 (mg/mL)";else->if(isAr) "الزمن المنقضي (h)" else "Elapsed time (h)"})},singleLine=true)
        if(mode==0) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                UnitMenu(from,{from=it;output=""},if(isAr) "من" else "From")
                UnitMenu(to,{to=it;output=""},if(isAr) "إلى" else "To")
            }
        } else {
            OutlinedTextField(value=second,onValueChange={second=it;output=""},label={Text(if(mode==1) "C2 (mg/mL)" else if(isAr) "نصف العمر (h)" else "Half-life (h)")},singleLine=true)
            if(mode==1)OutlinedTextField(value=third,onValueChange={third=it;output=""},label={Text("V2 (mL)")},singleLine=true)
        }
        Button(onClick={ output=try {
            val result=when(mode){0->EducationalCalculators.convert(number(first),EducationalCalculators.Unit.valueOf(from),EducationalCalculators.Unit.valueOf(to));1->EducationalCalculators.dilutionStockVolume(number(first),number(second),number(third));else->EducationalCalculators.remainingPercent(number(first),number(second))}
            val unit=when(mode){0->to.lowercase(Locale.ROOT);1->"mL";else->"%"}
            (if(isAr) "الناتج التعليمي ≈ " else "Educational result ≈ ")+String.format(Locale.ROOT,"%.6g",result)+" "+unit
        } catch (_: IllegalArgumentException) {if(isAr) "مدخلات غير صالحة: تحقق من الوحدات والقيم الموجبة وحدود النموذج." else "Invalid input: check dimensions, positive values and model limits."} }) {Text(if(isAr) "احسب تعليميًا" else "Calculate for learning")}
        if(output.isNotEmpty())Text(output,style=MaterialTheme.typography.titleMedium)
        Text(if(isAr) "حد المدخلات 10¹². يُرفض خلط الكتلة بالحجم والتخفيف إلى تركيز أعلى. نموذج نصف العمر محدود بـ100 فترة. النتائج تقريبية للتدريب ولا تحدد جرعات أو معدلات إعطاء لمريض." else "Input limit: 10¹². Mixed dimensions and dilution to higher concentration are rejected. The half-life model is limited to 100 intervals. Rounded results are for training and do not determine patient doses or administration rates.")
    }
}

@Composable private fun UnitMenu(value:String,onChange:(String)->Unit,label:String) {
    var open by remember {mutableStateOf(false)}
    Box {OutlinedButton(onClick={open=true}){Text("$label: $value")};DropdownMenu(expanded=open,onDismissRequest={open=false}) {EducationalCalculators.Unit.entries.forEach { u->DropdownMenuItem(text={Text(u.name)},onClick={onChange(u.name);open=false})}}}
}

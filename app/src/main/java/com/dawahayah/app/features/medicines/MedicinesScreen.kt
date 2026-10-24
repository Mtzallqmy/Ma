package com.dawahayah.app.features.medicines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.locale.ArabicSearchNormalizer
import com.dawahayah.app.core.security.IntegrityVerifier
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.domain.model.*
import com.dawahayah.app.features.settings.MainUiState
import com.dawahayah.app.ui.components.KnowledgeTopicCard

private fun regionKey(value: String): String = if (value.uppercase() in setOf("YE","YEMEN","اليمن")) "YE" else value.uppercase()

@Composable
fun MedicinesScreen(
    state: MainUiState,
    onBookmarkToggle: (String, String, String, String, String, Boolean) -> Unit,
    onRecordReading: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = state.settings.language == AppLanguage.ARABIC
    val bookmarks = state.bookmarks.map { it.itemId }.toSet()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var market by rememberSaveable { mutableStateOf("ALL") }
    var categoryOpen by remember { mutableStateOf(false) }
    val normalized = ArabicSearchNormalizer.normalize(query)
    val categories = state.activeIngredients.map { it.className }.distinct().sorted()
    val markets = (listOf("ALL","YE") + state.activeIngredients.flatMap { it.details.verifiedProducts() }.map { regionKey(it.region) }).distinct()
    val filtered = state.activeIngredients.filter { item ->
        val tokens = ArabicSearchNormalizer.buildIndexableTokens(item.nameAr,item.nameEn,item.className,
            item.details.identifiers.joinToString(" ") { it.identifier },
            item.details.verifiedProducts().joinToString(" ") { it.brandName + " " + it.manufacturer })
        (normalized.isBlank() || tokens.contains(normalized)) && (category.isBlank() || category == item.className) &&
            (market == "ALL" || item.details.verifiedProducts().any { regionKey(it.region) == market })
    }
    LazyColumn(modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(if(isAr) "مكتبة الأدوية والمواد الفعالة" else "Medicine & Ingredient Library",style=MaterialTheme.typography.headlineMedium)
            Text(if(isAr) "محتوى تعليمي قيد المراجعة العلمية. التسجيل الدوائي لا يثبت التوفر في الصيدليات."
                else "Educational content awaiting scientific review. Registration does not establish pharmacy availability.",style=MaterialTheme.typography.bodyMedium)
        }
        item {
            OutlinedTextField(value=query,onValueChange={query=it},singleLine=true,
                label={Text(if(isAr) "الاسم العلمي، التجاري أو المعرّف" else "Generic, brand or identifier")},modifier=Modifier.fillMaxWidth())
            Box {
                OutlinedButton(onClick={categoryOpen=true}) { Text(category.ifBlank { if(isAr) "جميع الفئات الدوائية" else "All drug classes" }) }
                DropdownMenu(expanded=categoryOpen,onDismissRequest={categoryOpen=false}) {
                    DropdownMenuItem(text={Text(if(isAr) "الكل" else "All")},onClick={category="";categoryOpen=false})
                    categories.forEach { value -> DropdownMenuItem(text={Text(value)},onClick={category=value;categoryOpen=false}) }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                markets.forEach { value -> FilterChip(selected=market==value,onClick={market=value},label={Text(when(value){"ALL"->if(isAr) "كل الأسواق" else "All markets";"YE"->if(isAr) "اليمن" else "Yemen";else->value})}) }
            }
            Text(if(isAr) "${filtered.size} مادة فعالة" else "${filtered.size} ingredients",style=MaterialTheme.typography.labelMedium)
        }
        if(filtered.isEmpty()) item { Text(if(isAr) "لا توجد نتائج مطابقة أو منتجات منشورة موثقة لهذا السوق. غيّر المرشحات أو حدّث المحتوى."
            else "No matching records or verified published products for this market. Change filters or update content.") }
        items(filtered,key={it.id}) { item ->
            KnowledgeTopicCard(title=if(isAr)item.nameAr else item.nameEn,secondaryTitle=if(isAr)item.nameEn else item.nameAr,
                latinTerm=item.className,bodyText=if(isAr)item.generalDescriptionAr else item.generalDescriptionEn,
                safetyNotice=item.safetyNote,metadataItems=listOf(if(isAr) "تعليمي — المراجعة العلمية مؤجلة" else "Educational — scientific review pending",
                    item.details.identifiers.joinToString(" · ") { "${it.vocabulary}: ${it.identifier}" }),
                isBookmarked=item.id in bookmarks,
                onBookmarkToggle={onBookmarkToggle(item.id,"MEDICINE",item.nameAr,item.nameEn,item.className,item.id in bookmarks)},
                onCardClick={onRecordReading(item.id,"MEDICINE",item.nameAr,item.nameEn);selectedId=item.id})
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    state.activeIngredients.firstOrNull { it.id == selectedId }?.let {
        MedicineDetailDialog(it,isAr,state.sources.associateBy { source->source.id }) { selectedId=null }
    }
}

@Composable
private fun SourceLink(id:String,sources:Map<String,MedicalSource>,isAr:Boolean) {
    val source=sources[id]
    val handler=LocalUriHandler.current
    Text(source?.title ?: id,style=MaterialTheme.typography.labelMedium)
    if(source!=null && IntegrityVerifier.isSecureHttpsEndpoint(source.url)) {
        TextButton(onClick={handler.openUri(source.url)}) {Text(if(isAr) "فتح المصدر" else "Open source")}
    }
}

@Composable
private fun MedicineDetailDialog(item:ActiveIngredient,isAr:Boolean,sources:Map<String,MedicalSource>,onDismiss:()->Unit) {
    val clipboard=LocalClipboardManager.current
    val handler=LocalUriHandler.current
    var tab by rememberSaveable(item.id) {mutableStateOf(0)}
    val names=if(isAr)listOf("التعريف","الاستعمالات","السلامة","المعرّفات","المنتجات","الجرعات","المراجع")
        else listOf("Overview","Indications","Safety","Identifiers","Products","Dosing","Sources")
    fun empty()=if(isAr) "لا توجد سجلات منشورة مستوفية لشروط المراجعة والمصدر." else "No published records satisfy the source and review requirements."
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(isAr)item.nameAr else item.nameEn)},text={
        Column(Modifier.height(430.dp)) {
            ScrollableTabRow(selectedTabIndex=tab,edgePadding=0.dp) {names.forEachIndexed { i,label -> Tab(selected=tab==i,onClick={tab=i},text={Text(label)})}}
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                when(tab) {
                    0->{Text(item.className,style=MaterialTheme.typography.titleMedium);SelectionContainer{Text(if(isAr)item.generalDescriptionAr else item.generalDescriptionEn)}}
                    1->{val records=item.details.reviewedIndications();if(records.isEmpty())Text(empty());records.forEach{Text(if(isAr)it.detailsAr else it.detailsEn);SourceLink(it.sourceId,sources,isAr)}}
                    2->{SelectionContainer{Text(item.safetyNote)};item.details.reviewedSafety().forEach{HorizontalDivider();Text("${it.kind} · ${it.severity}",style=MaterialTheme.typography.labelLarge);Text(if(isAr)it.detailsAr else it.detailsEn);SourceLink(it.sourceId,sources,isAr)}}
                    3->{if(item.details.identifiers.isEmpty())Text(if(isAr) "لا توجد معرّفات موثقة لهذه المادة." else "No sourced identifiers for this ingredient.");item.details.identifiers.forEach{SelectionContainer{Text("${it.vocabulary}: ${it.identifier}")};SourceLink(it.sourceId,sources,isAr)}}
                    4->{val products=item.details.verifiedProducts();if(products.isEmpty())Text(empty());products.forEach{p->
                        Text(p.brandName,style=MaterialTheme.typography.titleMedium);Text(listOf(p.region,p.manufacturer,p.dosageForm,p.strength).filter{it.isNotBlank()}.joinToString(" · "))
                        Text("${p.regulator} · ${p.registrationNumber}");Text(if(p.verificationState=="NOT_CURRENTLY_AVAILABLE") {if(isAr) "غير متاح حاليًا وفق السجل" else "Recorded as currently unavailable"} else {if(isAr) "تسجيل موثق؛ التوفر غير مؤكد" else "Verified registration; availability unconfirmed"})
                        if(IntegrityVerifier.isSecureHttpsEndpoint(p.evidenceUrl))TextButton(onClick={handler.openUri(p.evidenceUrl)}){Text(if(isAr) "دليل التسجيل" else "Registration evidence")};SourceLink(p.sourceId,sources,isAr)
                    }}
                    5->{val doses=item.details.reviewedDoses();Text(if(isAr) "مراجع جرعات منشورة بمراجعة؛ لا تُنشئ وصفة فردية." else "Reviewed published dosing references; not an individual prescription.");if(doses.isEmpty())Text(empty());doses.forEach{d->
                        Text(listOf(d.patientGroup,d.route,d.region,d.context).filter{it.isNotBlank()}.joinToString(" · "),style=MaterialTheme.typography.titleSmall)
                        SelectionContainer{Text(if(isAr)d.detailsAr else d.detailsEn)}
                        listOf(d.maximumDose,d.renalAdjustment,d.hepaticAdjustment).filter{it.isNotBlank()}.forEach{Text(it)}
                        Text("${d.sourceRevision} · ${d.reviewedAt.orEmpty()}",style=MaterialTheme.typography.labelSmall);SourceLink(d.sourceId,sources,isAr)
                    }}
                    else->{val ids=(item.citationIds+item.details.identifiers.map{it.sourceId}).distinct();ids.forEach{id->SourceLink(id,sources,isAr);sources[id]?.let{Text(it.license,style=MaterialTheme.typography.labelSmall)}}}
                }
            }
        }
    },dismissButton={TextButton(onClick={clipboard.setText(AnnotatedString("${item.nameAr} / ${item.nameEn}\n${item.className}\n"+item.details.identifiers.joinToString("\n"){"${it.vocabulary}: ${it.identifier}"}))}){Text(if(isAr) "نسخ الهوية" else "Copy identity")}},
        confirmButton={TextButton(onClick=onDismiss){Text(if(isAr) "إغلاق" else "Close")}})
}

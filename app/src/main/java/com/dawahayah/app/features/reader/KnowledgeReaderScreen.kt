package com.dawahayah.app.features.reader

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.security.IntegrityVerifier
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.features.settings.MainUiState

private data class ReaderSection(val title:String,val body:String)
private data class ReaderLink(val id:String,val type:String,val label:String)

@Composable
fun KnowledgeReaderScreen(state:MainUiState,type:String,id:String,
    onOpen:(String,String)->Unit,onRead:(String,String,String,String)->Unit,
    onBookmark:(String,String,String,String,String,Boolean)->Unit,
    onSaveNote:(String,String,String)->Unit) {
    val ar=state.settings.language==AppLanguage.ARABIC
    val context=LocalContext.current
    val uri=LocalUriHandler.current
    fun text(a:String,b:String)=if(ar)a else b
    val system=state.systems.firstOrNull{it.id==id}
    val organ=state.organs.firstOrNull{it.id==id}
    val disease=state.diseases.firstOrNull{it.id==id}
    val medicine=state.activeIngredients.firstOrNull{it.id==id}
    val lesson=state.installedLessons.firstOrNull{it.id==id}
    val structure=state.organs.flatMap{it.structures}.firstOrNull{it.id==id}
    val titleAr=system?.nameAr?:organ?.nameAr?:disease?.nameAr?:medicine?.nameAr?:lesson?.titleAr?:structure?.nameAr
    val titleEn=system?.nameEn?:organ?.nameEn?:disease?.nameEn?:medicine?.nameEn?:lesson?.titleEn?:structure?.nameEn
    if(titleAr==null || titleEn==null) {
        Column(Modifier.padding(24.dp)) {Text(text("المحتوى غير متاح محليًا","Content unavailable offline"),style=MaterialTheme.typography.titleLarge)
            Text(text("قد يكون سُحب أو لم يُحمّل بعد. بقيت المفضلة وبياناتك الشخصية محفوظة.","The item may have been withdrawn or not downloaded. Your bookmarks and personal data are retained."))}
        return
    }
    LaunchedEffect(id) {onRead(id,type,titleAr,titleEn)}
    val sourceIds=(system?.sourceIds?:organ?.sourceIds?:disease?.citationIds?:medicine?.citationIds?:listOfNotNull(lesson?.sourceId,structure?.sourceId)).distinct()
    val sources=state.sources.filter{it.id in sourceIds}
    val intro=when {
        system!=null->text(system.descriptionAr,system.descriptionEn)
        organ!=null->text(organ.summaryAr,organ.summaryEn)
        disease!=null->text(disease.introductionAr,disease.introductionEn)
        medicine!=null->text(medicine.generalDescriptionAr,medicine.generalDescriptionEn)
        lesson!=null->text(lesson.bodyAr,lesson.bodyEn)
        structure!=null->text(structure.functionAr,structure.functionEn)
        else->""
    }
    val savedNote=state.notes.firstOrNull{it.itemId==id}
    var draftNote by rememberSaveable(id,savedNote?.updatedAt) {mutableStateOf(savedNote?.text.orEmpty())}
    val sections=buildList {
        organ?.sections?.sortedBy{it.orderIndex}?.forEach{add(ReaderSection(text(it.titleAr,it.titleEn),text(it.bodyAr,it.bodyEn)))}
        disease?.sections?.sortedBy{it.orderIndex}?.forEach{add(ReaderSection(text(it.titleAr,it.titleEn),text(it.contentAr,it.contentEn)))}
        medicine?.let { m ->
            add(ReaderSection(text("التصنيف","Drug class"),m.className))
            add(ReaderSection(text("المعرّفات المرجعية","Reference identifiers"),m.details.identifiers.joinToString("\n"){"${it.vocabulary}: ${it.identifier}"}))
            m.details.reviewedIndications().forEach{add(ReaderSection(text("استعمال منشور","Published indication"),text(it.detailsAr,it.detailsEn)))}
            m.details.reviewedSafety().forEach{add(ReaderSection(it.kind,text(it.detailsAr,it.detailsEn)))}
        }
    }
    val links=buildList {
        if(system!=null) {
            state.organs.filter{it.systemId==id}.forEach{add(ReaderLink(it.id,"ORGAN",text(it.nameAr,it.nameEn)))}
            state.diseases.filter{it.primarySystemId==id || id in it.secondarySystemIds}.forEach{add(ReaderLink(it.id,"DISEASE",text(it.nameAr,it.nameEn)))}
        }
        if(organ!=null) {
            state.systems.filter{it.id==organ.systemId}.forEach{add(ReaderLink(it.id,"BODY_SYSTEM",text(it.nameAr,it.nameEn)))}
            state.diseases.filter{it.id in organ.relatedDiseaseIds || id in it.affectedOrganIds}.forEach{add(ReaderLink(it.id,"DISEASE",text(it.nameAr,it.nameEn)))}
            organ.structures.forEach{add(ReaderLink(it.id,"ANATOMICAL_STRUCTURE",text(it.nameAr,it.nameEn)))}
        }
        if(disease!=null) {
            state.systems.filter{it.id==disease.primarySystemId || it.id in disease.secondarySystemIds}.forEach{add(ReaderLink(it.id,"BODY_SYSTEM",text(it.nameAr,it.nameEn)))}
            state.organs.filter{it.id in disease.affectedOrganIds}.forEach{add(ReaderLink(it.id,"ORGAN",text(it.nameAr,it.nameEn)))}
            state.activeIngredients.filter{m->m.details.reviewedIndications().any{it.diseaseId==id}}.forEach{add(ReaderLink(it.id,"MEDICINE",text(it.nameAr,it.nameEn)))}
        }
        medicine?.details?.reviewedIndications()?.forEach{fact->state.diseases.firstOrNull{it.id==fact.diseaseId}?.let{add(ReaderLink(it.id,"DISEASE",text(it.nameAr,it.nameEn)))}}
        structure?.let{st->state.organs.firstOrNull{it.id==st.organId}?.let{add(ReaderLink(it.id,"ORGAN",text(it.nameAr,it.nameEn)))}}
    }.distinctBy{it.id}
    LazyColumn(Modifier.fillMaxSize().testTag("knowledge_reader"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Text(text(titleAr,titleEn),style=MaterialTheme.typography.headlineLarge)
            Text(text(titleEn,titleAr),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                val saved=state.bookmarks.any{it.itemId==id}
                FilledTonalButton(onClick={onBookmark(id,type,titleAr,titleEn,"",saved)}) {
                    Icon(Icons.Outlined.BookmarkBorder,null);Text(if(saved)text("محفوظ","Saved")else text("حفظ","Save"))
                }
                TextButton(onClick={
                    val shared="${text(titleAr,titleEn)}\n$intro\n\n"+text("محتوى تعليمي غير مراجع علميًا — دواء وحياة","Educational, not clinically reviewed — Dawa & Hayah")+"\n"+sources.joinToString("\n"){it.title+"\n"+it.url}
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,shared),text("مشاركة مع المصدر","Share with source")))
                }) {Icon(Icons.Outlined.Share,null);Text(text("مشاركة","Share"))}
            }
            Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.medium) {
                Text(text("مادة تعليمية؛ وجود المصادر لا يثبت المراجعة السريرية.","Educational material; references do not establish clinical review."),Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall)
            }
        }
        item {SelectionContainer {Text(intro,style=MaterialTheme.typography.bodyLarge)}}
        items(sections.filter{it.body.isNotBlank()}) { section ->
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(section.title,style=MaterialTheme.typography.titleLarge)
                SelectionContainer {Text(section.body,style=MaterialTheme.typography.bodyLarge)}
            }
        }
        if(links.isNotEmpty()) item {
            Text(text("مواضيع مرتبطة","Related topics"),style=MaterialTheme.typography.titleLarge)
            links.forEach{link->TextButton(onClick={onOpen(link.type,link.id)},modifier=Modifier.testTag("related_${link.id}")) {Text(link.label)}}
        }
        item {
            Text(text("ملاحظاتي الخاصة","My private notes"),style=MaterialTheme.typography.titleLarge)
            Text(text("تُحفظ على جهازك وتُضمّن في النسخة الاحتياطية التي تصدّرها. لا تدخل معلومات مرضى.","Stored on this device and included in your exported backup. Do not enter patient data."),style=MaterialTheme.typography.bodySmall)
            OutlinedTextField(value=draftNote,onValueChange={if(it.length<=20000)draftNote=it},modifier=Modifier.fillMaxWidth().testTag("reader_note"),minLines=3,label={Text(text("ملاحظة شخصية","Personal note"))})
            Button(onClick={onSaveNote(id,type,draftNote)},modifier=Modifier.testTag("save_note")) {Text(text("حفظ الملاحظة","Save note"))}
        }
        item {Text(text("المصادر والحقوق","Sources & rights"),style=MaterialTheme.typography.titleLarge)}
        items(sources,key={it.id}) {source->
            OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(source.title,style=MaterialTheme.typography.titleMedium)
                Text(source.attribution,style=MaterialTheme.typography.bodySmall)
                Text(source.license,style=MaterialTheme.typography.labelSmall)
                if(IntegrityVerifier.isSecureHttpsEndpoint(source.url))TextButton(onClick={uri.openUri(source.url)}) {Text(text("فتح المرجع الأصلي","Open original source"))}
            }}
        }
        if(sources.isEmpty())item {Text(text("لا يوجد مرجع متاح لهذا السجل حاليًا.","No source is currently available for this record."))}
    }
}

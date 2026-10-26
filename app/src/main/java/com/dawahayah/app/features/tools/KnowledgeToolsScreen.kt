package com.dawahayah.app.features.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.features.settings.MainUiState

@Composable
fun DrugClassesScreen(state:MainUiState,onOpen:(String,String)->Unit) {
    val ar=state.settings.language==AppLanguage.ARABIC
    val groups=state.activeIngredients.groupBy{it.className}.toSortedMap()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Text(if(ar) "الفئات الدوائية" else "Drug classes",style=MaterialTheme.typography.headlineLarge)
            Text(if(ar) "تصنيفات المواد الموجودة في مكتبتك؛ لا تمثل اختيارًا للعلاج." else "Browse ingredients in your library by class; this is not treatment selection.")}
        groups.forEach { (name,ingredients)->
            item {Text(name,style=MaterialTheme.typography.titleLarge)}
            items(ingredients,key={it.id}) {ingredient->
                OutlinedCard(onClick={onOpen("MEDICINE",ingredient.id)},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {Text(if(ar)ingredient.nameAr else ingredient.nameEn);Text(if(ar)ingredient.nameEn else ingredient.nameAr,style=MaterialTheme.typography.bodySmall)}
                }
            }
        }
    }
}

@Composable
fun InteractionsScreen(state:MainUiState) {
    val ar=state.settings.language==AppLanguage.ARABIC
    var first by rememberSaveable{mutableStateOf("")}
    var second by rememberSaveable{mutableStateOf("")}
    val uri=LocalUriHandler.current
    val records=state.activeIngredients.flatMap { ingredient ->
        ingredient.details.reviewedSafety().filter { fact -> fact.kind=="INTERACTION" &&
            ((ingredient.id==first && fact.relatedIngredientId==second)||(ingredient.id==second&&fact.relatedIngredientId==first)) &&
            state.sources.any{it.id==fact.sourceId&&it.url.startsWith("https://")} }
    }.distinctBy{it.id}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Text(if(ar) "سجل التداخلات المنشورة" else "Published interaction records",style=MaterialTheme.typography.headlineLarge)
            Text(if(ar) "يبحث في السجلات الموثقة المتاحة محليًا فقط. قاعدة التداخلات غير مكتملة ولا تصلح لإقرار سلامة الجمع بين الأدوية." else "Searches only sourced local published records. This database is incomplete and cannot establish that combining medicines is safe.")}
        item {IngredientChoice(state,first,if(ar) "المادة الأولى" else "First ingredient"){first=it}}
        item {IngredientChoice(state,second,if(ar) "المادة الثانية" else "Second ingredient"){second=it}}
        if(first.isNotBlank() && second.isNotBlank()) {
            if(first==second)item{Text(if(ar) "اختر مادتين مختلفتين." else "Choose two different ingredients.")}
            else if(records.isEmpty())item {Text(if(ar) "لا توجد سجلات مراجعة لهذه المقارنة في المكتبة. هذا لا يعني عدم وجود تداخل دوائي. ارجع إلى المرجع الأصلي أو الصيدلي." else "No reviewed records for this comparison are available. This does not mean there is no interaction. Consult an original reference or pharmacist.")}
            else items(records,key={it.id}) {fact->
                Text(if(ar)fact.detailsAr else fact.detailsEn)
                state.sources.firstOrNull{it.id==fact.sourceId}?.let{source->TextButton(onClick={uri.openUri(source.url)}){Text(source.title)}}
            }
        }
    }
}

@Composable
private fun IngredientChoice(state:MainUiState,selected:String,label:String,onSelect:(String)->Unit) {
    var open by remember{mutableStateOf(false)}
    val ar=state.settings.language==AppLanguage.ARABIC
    val item=state.activeIngredients.firstOrNull{it.id==selected}
    Box {
        OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()) {Text(item?.let{if(ar)it.nameAr else it.nameEn}?:label)}
        DropdownMenu(expanded=open,onDismissRequest={open=false}) {
            state.activeIngredients.forEach {ingredient->DropdownMenuItem(text={Text(if(ar)ingredient.nameAr else ingredient.nameEn)},onClick={onSelect(ingredient.id);open=false})}
        }
    }
}

@Composable
fun SourcesScreen(state:MainUiState) {
    val ar=state.settings.language==AppLanguage.ARABIC
    val uri=LocalUriHandler.current
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Text(if(ar) "المصادر وحقوق الاستخدام" else "Sources & rights",style=MaterialTheme.typography.headlineLarge)
            Text(if(ar) "ذكر المرجع لا يعني أن جميع النصوص خضعت لمراجعة بشرية. حقوق كل أصل مستقلة عن ترخيص التطبيق." else "Citing a reference does not establish human review. Each asset has rights separate from the app's license.")}
        items(state.sources,key={it.id}) {source->
            OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(source.title,style=MaterialTheme.typography.titleMedium);Text(source.publisher);Text(source.attribution);Text(source.license,style=MaterialTheme.typography.bodySmall)
                if(source.url.startsWith("https://"))TextButton(onClick={uri.openUri(source.url)}) {Text(if(ar) "فتح المصدر" else "Open source")}
            }}
        }
        item {Text("OpenStax College · Internal Anatomy of the Heart · CC BY 3.0 · Original JPEG unchanged.")
            TextButton(onClick={uri.openUri("https://creativecommons.org/licenses/by/3.0/")}) {Text("CC BY 3.0")}}
    }
}

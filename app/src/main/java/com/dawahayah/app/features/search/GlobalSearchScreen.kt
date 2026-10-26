package com.dawahayah.app.features.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.features.settings.MainUiState
import com.dawahayah.app.ui.components.UnboxedMetadataLine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlobalSearchScreen(
    state: MainUiState,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenContent: (String, String) -> Unit = { _, _ -> }
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val isAr = state.settings.language == AppLanguage.ARABIC

    LazyColumn(
        modifier = modifier
            .fillMaxSize().testTag("search_results")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isAr) "البحث في المكتبة" else "Search the library",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isAr) {
                    "ابحث بالعربية أو الإنجليزية أو بالمصطلح التشريحي. يعمل البحث في المحتوى المحفوظ على جهازك."
                } else {
                    "Search in Arabic, English or anatomical terminology. Results come from the content saved on your device."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = state.searchQuery,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                onValueChange = onQueryChange,
                label = {
                    Text(
                        if (isAr) "ابحث عن عضو، جهاز، مرض، أو مادة فعالة (مثال: القلب، أوميبرازول، Cor)"
                        else "Search organ, system, disease, or active ingredient..."
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if(state.searchResults.isNotEmpty())Text(if(isAr) "${state.searchResults.size} نتيجة" else "${state.searchResults.size} results",modifier=Modifier.testTag("search_has_results"))
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onQueryChange("القلب") }) {
                    Text(if(isAr) "القلب" else "Heart")
                }
                OutlinedButton(onClick = { onQueryChange("اوميبرازول") }) {
                    Text(if(isAr) "أوميبرازول" else "Omeprazole")
                }
                OutlinedButton(onClick = { onQueryChange("معده") }) {
                    Text(if(isAr) "المعدة" else "Stomach")
                }
            }
        }

        if (state.searchQuery.isNotBlank() && state.searchResults.isEmpty()) {
            item {
                Text(
                    text = if (isAr) "لا توجد نتائج مطابقة في قاعدة المعرفة المحلية." else "No matching records in local knowledge base.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if(state.searchResults.isEmpty()) {
            val choices=com.dawahayah.app.core.locale.ArabicSearchNormalizer.suggestions(state.searchQuery,
                state.activeIngredients.flatMap{listOf(it.nameAr,it.nameEn)}+state.organs.flatMap{listOfNotNull(it.nameAr,it.nameEn,it.latinName)}+state.diseases.flatMap{listOf(it.nameAr,it.nameEn)})
            if(choices.isNotEmpty())item {
                Text(if(isAr) "هل تقصد؟" else "Did you mean?")
                choices.forEach { choice -> OutlinedButton(onClick={onQueryChange(choice)}) {Text(choice)} }
            }
        }

        items(state.searchResults, key = { "${it.category}_${it.itemId}" }) { hit ->
            Card(
                onClick = { onOpenContent(hit.category, hit.itemId) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("hit_${hit.itemId}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isAr) hit.titleAr else hit.titleEn,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    UnboxedMetadataLine(
                        items = listOfNotNull(
                            when(hit.category) {
                                "BODY_SYSTEM" -> if(isAr) "جهاز" else "System"
                                "ORGAN" -> if(isAr) "عضو" else "Organ"
                                "DISEASE" -> if(isAr) "مرض" else "Disease"
                                "MEDICINE", "ACTIVE_INGREDIENT" -> if(isAr) "مادة فعالة" else "Ingredient"
                                "ANATOMICAL_STRUCTURE" -> if(isAr) "تركيب تشريحي" else "Structure"
                                else -> if(isAr) "موضوع" else "Topic"
                            },
                            if (isAr) hit.titleEn else hit.titleAr,
                            hit.latinSubtitle
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) hit.snippetAr else hit.snippetEn,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

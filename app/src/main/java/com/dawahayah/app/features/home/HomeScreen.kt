package com.dawahayah.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.domain.model.PackInstallState
import com.dawahayah.app.features.settings.MainUiState
import com.dawahayah.app.ui.navigation.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(state: MainUiState, onNavigateTab: (PrimaryBottomTab) -> Unit,
    onNavigateAuxiliary: (String) -> Unit, modifier: Modifier = Modifier,
    onOpenContent: (String, String) -> Unit = { _, _ -> }, onSync: () -> Unit = {}) {
    val ar = state.settings.language == AppLanguage.ARABIC
    LazyColumn(modifier.fillMaxSize().testTag("home_feed"), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant))).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if(ar) "دواء وحياة" else "Dawa & Hayah", style = MaterialTheme.typography.headlineLarge)
                    Text(if(ar) "علم الدواء لفهم الحياة" else "Pharmacology to understand life", style = MaterialTheme.typography.titleMedium)
                    Text(if(ar) "مرجعك التعليمي، أينما كنت." else "Your learning library, wherever you are.", style = MaterialTheme.typography.bodyLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick={onNavigateTab(PrimaryBottomTab.BODY_SYSTEMS)}, label={Text(if(ar) "${state.systems.size} جهازًا" else "${state.systems.size} systems")})
                        AssistChip(onClick={onNavigateTab(PrimaryBottomTab.DISEASES)}, label={Text(if(ar) "${state.diseases.size} صفحة مرض" else "${state.diseases.size} disease pages")})
                        AssistChip(onClick={onNavigateTab(PrimaryBottomTab.MEDICINES)}, label={Text(if(ar) "${state.activeIngredients.size} مادة فعالة" else "${state.activeIngredients.size} ingredients")})
                    }
                }
            }
        }
        item {
            OutlinedCard(onClick={onNavigateAuxiliary(AuxiliaryRoutes.GLOBAL_SEARCH)}, modifier=Modifier.fillMaxWidth().testTag("home_search")) {
                Row(Modifier.padding(18.dp), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Search, null)
                    Text(if(ar) "ابحث عن مرض، دواء أو عضو…" else "Search a disease, medicine or organ…")
                }
            }
        }
        item {
            Text(if(ar) "استكشف المعرفة" else "Explore the library",style=MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints {
                val columns=if(maxWidth>=600.dp) 3 else 2
                val entries=listOf(
                    HomeEntry(if(ar) "أجهزة الجسم" else "Body systems", if(ar) "التشريح والوظائف" else "Anatomy & function", Icons.Outlined.FavoriteBorder) {onNavigateTab(PrimaryBottomTab.BODY_SYSTEMS)},
                    HomeEntry(if(ar) "موسوعة الأمراض" else "Disease library", if(ar) "الفهم والروابط العلمية" else "Knowledge & connections", Icons.Outlined.Biotech) {onNavigateTab(PrimaryBottomTab.DISEASES)},
                    HomeEntry(if(ar) "موسوعة الأدوية" else "Medicine library", if(ar) "المواد والفئات والمصادر" else "Ingredients, classes & sources", Icons.Outlined.Medication) {onNavigateTab(PrimaryBottomTab.MEDICINES)},
                    HomeEntry(if(ar) "التشريح التفاعلي" else "Interactive anatomy", if(ar) "نماذج أصلية قابلة للتنزيل" else "Sourced models to download", Icons.Outlined.ViewInAr) {onNavigateAuxiliary(AuxiliaryRoutes.SOURCED_ANATOMY)},
                    HomeEntry(if(ar) "أكاديمية الصيدلة" else "Pharmacy academy", if(ar) "دروس واختبارات وحاسبات" else "Lessons, quizzes & calculators", Icons.Outlined.School) {onNavigateAuxiliary(AuxiliaryRoutes.ACADEMY)},
                    HomeEntry(if(ar) "مكتبتي والتنزيلات" else "Saved & offline", if(ar) "المفضلة وحزم المحتوى" else "Bookmarks & content packs", Icons.Outlined.CloudDownload) {onNavigateTab(PrimaryBottomTab.MY_LIBRARY)}
                )
                Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    entries.chunked(columns).forEach { row ->
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            row.forEach { entry ->
                                OutlinedCard(onClick=entry.open,modifier=Modifier.weight(1f).heightIn(min=144.dp),shape=RoundedCornerShape(20.dp)) {
                                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                        Icon(entry.icon,null,tint=MaterialTheme.colorScheme.primary)
                                        Text(entry.title,style=MaterialTheme.typography.titleMedium)
                                        Text(entry.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(if(ar) "أدوات الدراسة" else "Study tools",style=MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={onNavigateAuxiliary(AuxiliaryRoutes.DRUG_CLASSES)}) {Text(if(ar) "الفئات الدوائية" else "Drug classes")}
                OutlinedButton(onClick={onNavigateAuxiliary(AuxiliaryRoutes.CALCULATORS)}) {Text(if(ar) "الحاسبات التعليمية" else "Educational calculators")}
                OutlinedButton(onClick={onNavigateAuxiliary(AuxiliaryRoutes.INTERACTIONS)}) {Text(if(ar) "سجل التداخلات" else "Interaction records")}
            }
        }
        state.readingProgress.firstOrNull()?.let { latest -> item {
            ElevatedCard(onClick={onOpenContent(latest.itemType,latest.itemId)},modifier=Modifier.fillMaxWidth().testTag("continue_reading")) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(if(ar) "تابع من حيث توقفت" else "Continue reading",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                    Text(if(ar) latest.titleAr else latest.titleEn,style=MaterialTheme.typography.titleLarge)
                }
            }
        } }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(if(ar) "المحتوى على جهازك" else "Your offline library",style=MaterialTheme.typography.titleMedium)
                    state.lastSyncAt?.let { time ->
                        Text((if(ar) "آخر تحديث ناجح: " else "Last successful sync: ")+java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(java.util.Date(time)),style=MaterialTheme.typography.labelMedium)
                    }
                    val count=state.packs.count{it.installState==PackInstallState.COMPLETED}
                    Text(if(ar) "$count حزم مثبتة · المحتوى المحفوظ متاح دون اتصال" else "$count installed packs · Saved content works offline")
                    Text(if(ar) "المحتوى الطبي تعليمي وغير مراجع علميًا. المراجع متاحة داخل كل موضوع." else "Medical content is educational and not clinically reviewed. References are available for each topic.",style=MaterialTheme.typography.bodySmall)
                    FilledTonalButton(onClick=onSync,enabled=!state.isRefreshingCloud) {
                        Icon(Icons.Outlined.Sync,null); Spacer(Modifier.width(8.dp))
                        Text(if(state.isRefreshingCloud){if(ar) "جارٍ التحديث…" else "Updating…"}else{if(ar) "تحديث المحتوى" else "Update content"})
                    }
                    TextButton(onClick={onNavigateAuxiliary(AuxiliaryRoutes.SOURCES_AND_LICENSES)}) {Text(if(ar) "المصادر وحقوق الاستخدام" else "Sources & rights")}
                }
            }
        }
    }
}
private data class HomeEntry(val title:String,val subtitle:String,val icon:ImageVector,val open:()->Unit)

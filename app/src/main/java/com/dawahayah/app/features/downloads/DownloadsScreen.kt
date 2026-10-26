package com.dawahayah.app.features.downloads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dawahayah.app.core.storage.AppLanguage
import com.dawahayah.app.domain.model.PackInstallState
import com.dawahayah.app.features.settings.MainUiState
import com.dawahayah.app.ui.components.UnboxedMetadataLine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadsScreen(
    state: MainUiState,
    onInstallPack: (String, Boolean) -> Unit,
    onDeletePack: (String) -> Unit,
    onRefreshCloudCatalog: () -> Unit,
    modifier: Modifier = Modifier,
    onPausePack: (String) -> Unit = {}
) {
    val isAr = state.settings.language == AppLanguage.ARABIC

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isAr) "التنزيلات والمساحة" else "Downloads & storage",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isAr) {
                    "حمّل الحزم التي تحتاجها للقراءة دون إنترنت. يُفحص اكتمال الملفات قبل التثبيت، ويمكن حذف الحزم مع الاحتفاظ بملاحظاتك ومفضلتك."
                } else {
                    "Download the packs you need for offline use. Files are verified before installation. Removing a pack preserves your notes and bookmarks."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedButton(onClick = onRefreshCloudCatalog, enabled = !state.isRefreshingCloud) {
                Text(if (state.isRefreshingCloud) {
                    if (isAr) "جارٍ تحديث المحتوى…" else "Updating content…"
                } else if (isAr) "تحديث الصفحات والحزم المتاحة" else "Update pages and available packs")
            }
        }

        items(state.packs, key = { it.id }) { pack ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isAr) pack.titleAr else pack.titleEn,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    UnboxedMetadataLine(
                        items = listOf(
                            "v${pack.version}",
                            android.text.format.Formatter.formatFileSize(androidx.compose.ui.platform.LocalContext.current,pack.sizeBytes)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    UnboxedMetadataLine(
                        items = listOf(
                            pack.license,
                            "الحالة: ${pack.installState.name}"
                        )
                    )

                    if (!pack.companionDependencyNotice.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pack.companionDependencyNotice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!pack.lastErrorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pack.lastErrorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (pack.installState == PackInstallState.RUNNING || pack.downloadedBytes > 0L) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val progress = if (pack.sizeBytes > 0) {
                            (pack.downloadedBytes.toFloat() / pack.sizeBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${pack.downloadedBytes} / ${pack.sizeBytes} Bytes",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pack.installState == PackInstallState.RUNNING || pack.installState == PackInstallState.QUEUED) {
                            OutlinedButton(onClick={onPausePack(pack.id)}) { Text(if(isAr) "إيقاف مؤقت" else "Pause") }
                        } else if (pack.installState != PackInstallState.COMPLETED) {
                            Button(
                                onClick = { onInstallPack(pack.id, false) }
                            ) {
                                Text(
                                    if (pack.installState == PackInstallState.PAUSED) {
                                        if (isAr) "استئناف التنزيل" else "Resume download"
                                    } else if (pack.isBundledOfflineDemoAvailable) {
                                        if (isAr) "تثبيت الحزمة وفحص SHA-256" else "Install & Verify SHA-256"
                                    } else {
                                        if (isAr) "طلب من سحابة Neon" else "Fetch from Neon Cloud"
                                    }
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onDeletePack(pack.id) }
                            ) {
                                Text(
                                    if (isAr) "حذف الحزمة من التخزين الدائم" else "Delete Pack from Persistent Storage"
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

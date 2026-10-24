package com.dawahayah.app.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.dawahayah.app.DawaHayahApplication
import com.dawahayah.app.data.repository.UserBackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun BackupControls(isAr:Boolean) {
 val context=LocalContext.current;val repository=remember{UserBackupRepository((context.applicationContext as DawaHayahApplication).container.database)}
 val scope=rememberCoroutineScope();var message by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
 val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)scope.launch{busy=true;try{val bytes=repository.export();withContext(Dispatchers.IO){checkNotNull(context.contentResolver.openOutputStream(uri,"wt")).use{it.write(bytes)}};message=if(isAr) "حُفظت نسخة البيانات الشخصية دون كلمات مرور أو جلسات دخول" else "Personal-data backup saved without passwords or sessions"}catch(_:Exception){message=if(isAr) "تعذر حفظ النسخة" else "Backup failed"}finally{busy=false}}}
 val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{busy=true;try{val bytes=withContext(Dispatchers.IO){checkNotNull(context.contentResolver.openInputStream(uri)).use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=UserBackupRepository.MAX_BYTES);out.write(buffer,0,n)};out.toByteArray()}};val count=repository.restore(bytes);message=if(isAr) "دُمجت النسخة: $count سجل؛ احتُفظ بالسجلات الأحدث" else "Backup merged: $count records; newer records retained"}catch(_:Exception){message=if(isAr) "تعذرت الاستعادة: ملف غير صالح أو محاولات متعارضة؛ البيانات السابقة محفوظة" else "Restore failed: invalid file or conflicting attempts; previous data retained"}finally{busy=false}}}
 Column {
  TextButton(enabled=!busy,onClick={export.launch("Dawa-Hayah-personal-backup.json")}){Text(if(isAr) "تصدير المفضلة والتقدم والنتائج" else "Export bookmarks, progress and scores")}
  TextButton(enabled=!busy,onClick={restore.launch(arrayOf("application/json","text/plain"))}){Text(if(isAr) "استعادة نسخة شخصية ودمجها" else "Restore and merge personal backup")}
  if(message.isNotEmpty())Text(message)
 }
}

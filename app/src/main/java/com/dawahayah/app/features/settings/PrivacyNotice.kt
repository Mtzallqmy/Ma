package com.dawahayah.app.features.settings

const val PRIVACY_AR="""الخصوصية والبيانات
القراءة والمفضلة ونتائج التعلم تعمل محليًا. لا تدخل أسماء مرضى أو معلوماتهم الصحية. لا تُرسل محاولات التعلم للحساب إلا بطلب المزامنة. تسجيل الدخول عبر Neon Auth؛ كلمة المرور لا تُحفظ في تطبيق أندرويد. تُحفظ جلسة الهاتف مشفرة بمفتاح Android Keystore؛ ولا تمنح صلاحية تحرير. حذف الجلسة محليًا متاح بتسجيل الخروج، وإلغاء الجلسة على الخادم يتطلب الاتصال. تنتهي جلسة الجهاز خلال 30 يومًا.
تحتفظ خدمة Neon ببيانات الحساب والمحاولات المتزامنة وسجلات المراجعة والنشر. لا تتضمن المحاولات بيانات مرضى. لا نُدرج أدوات إعلانات أو تحليلات استخدام في التطبيق. يظل تشغيل الخادم خاضعًا لسجلات البنية التحتية لدى مزود الخدمة. لا تُضمَّن كلمات مرور قاعدة البيانات أو مفاتيح التوقيع في التطبيق.
النسخ الاحتياطي التلقائي للتطبيق معطل. إزالة التطبيق تحذف البيانات المحلية؛ قبل إزالة نسخة اختبار استخدم تصدير المفضلة والتقدم والنتائج إلى ملف شخصي، ثم استعده بعد التثبيت. التوقيع التجريبي ليس مفتاح الإصدار الدائم، وقد يمنع تحديث نسخة اختبار بأخرى مباشرة.
المحتوى التعليمي قيد المراجعة العلمية. الحاسبات نماذج تدريب لا تنتج وصفات فردية. الجرعات والحقائق السريرية غير المعتمدة محجوبة عن النشر السريري."""
const val PRIVACY_EN="""Privacy and data
Reading, bookmarks and practice scores work locally. Do not enter patient names or health information. Learning attempts are sent to an account only when you request sync. Sign-in uses Neon Auth; the Android app does not store your password. The device session is encrypted with Android Keystore and grants no editorial access. Sign-out clears the local session; server revocation needs connectivity. Device sessions expire after 30 days.
Neon stores account data, synchronized attempts, and editorial review/publication audits. Attempts contain no patient data. The app includes no advertising or usage analytics tools. Infrastructure operation remains subject to provider logs. Database passwords and signing keys are not included in the app.
Automatic application backup is disabled. Uninstalling removes local data; export bookmarks, progress and scores to a personal file before removing a test build, then restore the file after installation. Temporary test signing is not the permanent release key and may prevent direct upgrades between test builds.
Educational content awaits scientific review. Calculators are training models, not individual prescriptions. Unapproved dosing and clinical facts remain excluded from clinical publication."""

@androidx.compose.runtime.Composable
fun PrivacyNotice(isAr:Boolean) {
 val expanded=androidx.compose.runtime.saveable.rememberSaveable {androidx.compose.runtime.mutableStateOf(false)}
 androidx.compose.foundation.layout.Column {
  androidx.compose.material3.TextButton(onClick={expanded.value=!expanded.value}) {androidx.compose.material3.Text(if(isAr) "سياسة الخصوصية والبيانات" else "Privacy and data policy")}
  if(expanded.value)androidx.compose.material3.Text(if(isAr) PRIVACY_AR else PRIVACY_EN)
 }
}

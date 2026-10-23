# دواء وحياة — توجيه تنفيذي إلزامي
المستودع الوحيد المسموح بالكتابة إليه: https://github.com/Mtzallqmy/Ma
الفرع الرئيسي: main. أنشئ فرعًا للعمل وPR لكل مرحلة ثم ادمج بعد نجاح CI. المستودع التاريخي https://github.com/mohmedali23182-jpg/Gi للقراءة والمقارنة فقط ولا ترفع التغييرات إليه.
Neon Project ID: icy-mode-02911943
Neon Branch ID: br-steep-moon-b9kxm3uf
قاعدة البيانات الوحيدة: appdb
Android public Function HTTPS: https://br-steep-moon-b9kxm3uf-api.compute.c-7.eu-central-1.aws.neon.tech/
Neon Data REST: https://ep-broad-violet-b96xtyh6.apirest.c-7.eu-central-1.aws.neon.tech/appdb/rest/v1
Neon Auth: https://ep-broad-violet-b96xtyh6.neonauth.c-7.eu-central-1.aws.neon.tech/appdb/auth
الأندرويد يُخاطب Function العامة المقيدة للقراءة فقط؛ لا تستخدم REST API بصورة مجهولة إذا كانت تحتاج JWT. لا تضمّن DATABASE_URL أو كلمة مرور أو JWT أو مفتاح توقيع داخل APK أو المستودع. أدر الأسرار خادميًا ودوّر بيانات الاتصال التي كُشفت سابقًا. لا تنشئ مشروع Android أو قاعدة Neon جديدة. احفظ Room والترحيلات والبيانات والمفضلة، واحترم حقوق المصادر الطبية. تحقق من الواقع أولًا ولا تدّع نجاحًا غير مختبر.

# المرحلة الأولى: تدقيق الأساس، لا بدء مشروع جديد
1. افحص الشيفرة الحالية: Kotlin + Jetpack Compose + Material3 + Room + DataStore + WorkManager + Retrofit، minSdk26، Android ARM64.
2. تأكد أن التطبيق يفتح فعليًا، والملاحة بخمس علامات تعمل بالعربية والإنجليزية والـRTL والوضعين الداكن والفاتح، مع تكبير الخط.
3. اختبر قاعدة Room seed بلا شبكة، ترحيلاتها، البحث العربي، المفضلة، قراءة المحتوى، واختيار التنزيلات.
4. راجع GitHub Actions: اختبار Kotlin، فحص الأسرار، lint، assembleDebug، أثر البناء; أصلح أخطاء Gradle لا تتجاوز الاختبارات الفاشلة.
5. افحص وظيفة Neon /health و/v1/catalog مع حماية بيانات النشر والتأكد من عدم كشف محتوى المسودات. لا تربط مباشرة بكلمة مرور PostgreSQL.
6. سلّم Commit وPR وتقرير الاختبارات؛ لا ترفع APK إنتاجي من هذه المرحلة.
معايير القبول: اختبارات CI ناجحة + Android minSdk26 + Seed + ترحيلات Room + عمل بدون إنترنت + اتصال HTTPS آمن وبدون أسرار على الطرف العميل.

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

# أمر استكمال تنفيذي شامل — لا تبدأ مشروعًا جديدًا
افتح مستودع Mtzallqmy/Ma الحالي وتابع من أحدث نسخة. هذه نسخة احتياطية للشيفرة من Gi والآن هي **وجهة العمل الوحيدة**. هناك أساس Android Kotlin/Compose وRoom، وشيفرة تشريح/أمراض، وربط وظائف Neon، وبنية أدوية؛ لا تكررها.
## الترتيب الإجباري
أولًا: شخّص التطبيق الحالي ومخطط Neon appdb وGitHub Actions وأي Release موجود؛ أنشئ قائمة دقيقة للوظائف: موجودة/معطوبة/وهمية/غير مختبرة. اختبر صحة Android build والـAPI قبل التغيير.
ثانيًا: أغلق نواقص Stage 1 (Offline seed، migrations، التنقل، البحث، الأمان، المفضلة وCI)، ثم Stage 2 (المجسم الفعلي، 13 جهازًا، الأعضاء، الأمراض، مصادر الصور وتنزيل المحتوى)، ثم Stage 3 (الأدوية، اليمن والعالم العربي، المراجعة العلمية، الجرعات والتداخلات الموثقة، التنزيلات، Beta).
ثالثًا: نفّذ كل متطلبات Stage 4 من prompts/04_Academy_Admin.md: الأكاديمية، الدروس، الحالات والاختبارات، الحاسبات التعليمية، لوحة الإدارة، سياسة مراجعة المحتوى والصلاحيات.
رابعًا: نفّذ كل متطلبات Stage 5 من prompts/05_Final_Release.md بما فيها QA وNeon end-to-end وتوقيع arm64 Android8+ وإصدار v1.0.0 فقط بعد النجاح.
## قواعد تقنية
اعمل في نفس الحزمة الحالية، وفق Android API26+ وKotlin/Jetpack Compose، Room/WorkManager/Retrofit، RTL عربي وإنجليزي، وتنزيل محتوى اختياري. لا تنقل المشروع إلى Flutter أو WebApp؛ WebView موضعية مسموحة فقط لمجسم WebGL إذا اجتازت الاختبار.
Neon الوحيدة appdb بالمشروع والفرع المذكورين؛ استخدم Functions HTTPS المقيدة وصلاحيات حقيقية. يمنع وضع كلمات مرور DB أو JWT أو مفاتيح Neon في BuildConfig أو APK أو Git.
اعمل بخطة فروع وعينات Commit/PR لكل مرحلة وتوثيق واضح. لا تمسح بيانات، لا تكرر المخطط أو المحركات، لا تكتب أسماء أدوية أو أسعار أو توافرًا متخيّلًا، ولا تنشر جرعات غير مراجعة من مختص.
## المخرجات المطلوبة فعليًا
GitHub commits + PRs + CI green + migrations Neon مطبقة ومتحقق منها + QA report + APK ARM64 مع رابط إصدار مباشر و SHA-256 + توثيق الحقوق العلمية والمصادر. إن تعذّر أي مطلب، سجّله Blocked ولا تدّع إنه أُنجز.

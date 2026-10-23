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

# المرحلة الثالثة: الأدوية والأصناف اليمنية والعربية والـBeta
1. افحص قاعدة appdb: active_ingredients وdrug_classes وdrug_indications وdrug_safety_facts وdrug_dosing_references وregional_medicine_products وpublication_status والمصادر؛ أضف ترحيلات idempotent عند الحاجة بدل جداول مكررة.
2. ابن موسوعة المادة الفعالة: الأسماء AR/EN والبدائل والتصنيف ATC/RxNorm/UNII حسب الترخيص، آلية العمل، الاستعمالات، موانع الاستعمال، السلامة، التفاعلات والمراجع.
3. دليل المنتجات اليمنية والعربية: manufacturer/form/strength/registration/evidence/region. اعرض حالة تحقق لا تسمح بتسويق اسم مفبرك أو ادعاء توفر دواء دون دليل موثوق.
4. جرعات الأطفال والبالغين والكلى والكبد والحمل والرضاعة: لا تُنشر كمرجع سريري إلا بمراجعة بشرية مؤهلة ومصدر صالح وسياق واضح؛ اجعلها مخفية عند عدم التحقق ولا تقدّم وصفات فردية تلقائية.
5. واجهات Compose بحث علمي وتجاري وتصفية، عرض المصادر، حفظ الصفحات، تنزيل المحتوى الانتقائي، عمل Offline محلي، مع تجربة ممتازة لأجهزة Android 8 الضعيفة.
6. أكمل WorkManager للتنزيل الحقيقي، HTTPS، SHA-256، أمان المسارات، التقدم والإلغاء والاستئناف، اختبارات التحديث والفساد والتخزين.
7. ابن v0.5.0-beta محددًا في GitHub Releases فقط بعد نجاح CI والاختبار الأمني والتحقق من التوقيع؛ اكتب صراحة إن كان توقيع CI تجريبيًا.
لا تعتبر جداول فارغة أو عناصر Seed القليلة «موسوعة مكتملة». لا تنشئ منتجات محلية من التخمين.

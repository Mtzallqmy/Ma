package com.dawahayah.app.core.errors

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val error: AppError) : AppResult<Nothing>
}

sealed class AppError(val messageAr: String, val messageEn: String) {
    data object CloudBackendNotConfigured : AppError(
        messageAr = "عنوان خدمة المحتوى غير مُعدّ لهذه النسخة. يمكنك قراءة المحتوى المحفوظ على جهازك.",
        messageEn = "The content service endpoint is not configured for this build. Saved content remains available offline."
    )

    data object NetworkUnavailable : AppError(
        messageAr = "لا يوجد اتصال بالشبكة حالياً. يمكنك تصفح المحتوى المحلي والحزم المثبتة في وضع الطيران.",
        messageEn = "Network is currently unavailable. You can browse built-in content and installed packs offline."
    )

    data object WifiRequiredForDownload : AppError(
        messageAr = "التنزيل مقيد بشبكة Wi-Fi فقط حسب إعداداتك الحالية.",
        messageEn = "Downloads are restricted to Wi-Fi only per your current settings."
    )

    data class InsufficientStorage(val requiredBytes: Long, val availableBytes: Long) : AppError(
        messageAr = "مساحة التخزين غير كافية لتثبيت الحزمة (المطلوب: $requiredBytes بايت).",
        messageEn = "Insufficient storage space to install pack (Required: $requiredBytes bytes)."
    )

    data class HashMismatch(
        val filePath: String,
        val expectedSha256: String,
        val actualSha256: String
    ) : AppError(
        messageAr = "فشل التحقق من سلامة الملف ($filePath): البصمة الرقمية SHA-256 غير مطابقة. تم إلغاء التثبيت لحماية البيانات.",
        messageEn = "File integrity check failed ($filePath): SHA-256 mismatch. Installation aborted to protect local data."
    )

    data class PathTraversalDetected(val unsafePath: String) : AppError(
        messageAr = "تم رفض مسار ملف غير آمن داخل الحزمة: $unsafePath",
        messageEn = "Rejected unsafe file path inside content pack: $unsafePath"
    )

    data class MissingPackFiles(val missingPaths: List<String>) : AppError(
        messageAr = "ملفات مفقودة داخل الحزمة: ${missingPaths.joinToString()}",
        messageEn = "Missing files inside content pack: ${missingPaths.joinToString()}"
    )

    data class DownloadInterrupted(val filePath: String) : AppError(
        "انقطع تنزيل الملف: $filePath. ستتم إعادة المحاولة في الخلفية.",
        "Download interrupted: $filePath. A background retry is scheduled."
    )

    data class GeneralFailure(val reasonAr: String, val reasonEn: String) : AppError(reasonAr, reasonEn)
}

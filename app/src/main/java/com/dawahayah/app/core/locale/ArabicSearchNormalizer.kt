package com.dawahayah.app.core.locale

/**
 * Arabic & English Search Normalizer for Dawa & Hayah.
 * Normalizes hamzas (أ/إ/آ/ٱ -> ا), taa marbuta (ة -> ه), alef maqsura (ى -> ي),
 * removes Arabic diacritics (tashkeel) and tatweel, and normalizes case/whitespace
 * for fast offline indexing in Room without altering the displayed scientific text.
 */
object ArabicSearchNormalizer {

    private val TASHKEEL_REGEX = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val MULTISPACE_REGEX = Regex("\\s+")

    fun normalize(rawText: String): String {
        if (rawText.isBlank()) return ""
        val sb = StringBuilder(rawText.length)
        val stripped = rawText.replace(TASHKEEL_REGEX, "")
        for (ch in stripped) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ة' -> sb.append('ه')
                'ى' -> sb.append('ي')
                'ؤ' -> sb.append('و')
                'ئ' -> sb.append('ي')
                '\u0640' -> { /* strip tatweel */ }
                else -> sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
            .replace(MULTISPACE_REGEX, " ")
            .trim()
    }

    /** Literal SQL LIKE search: user % and _ must not expand into all records. */
    fun literalLikeQuery(raw:String):String = normalize(raw).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    /** Suggestions are explicit choices; never silently replace a scientific query. */
    fun suggestions(raw:String, candidates:List<String>):List<String> {
        val query=normalize(raw)
        if(query.length !in 5..80)return emptyList()
        fun oneEdit(a:String,b:String):Boolean {
            if(kotlin.math.abs(a.length-b.length)>1 || a==b)return false
            var i=0;var j=0;var edits=0
            while(i<a.length && j<b.length) {
                if(a[i]==b[j]){i++;j++;continue}
                if(++edits>1)return false
                if(a.length>=b.length)i++
                if(b.length>=a.length)j++
            }
            return edits+(a.length-i)+(b.length-j)==1
        }
        return candidates.take(2000).distinct().filter{oneEdit(query,normalize(it))}.take(3)
    }

    private val aliases = listOf(
        listOf("القلب", "heart", "cor", "فؤاد"),
        listOf("الكليتان", "kidneys", "renes", "كلى"),
        listOf("الرئتان", "lungs", "pulmones", "رئة"),
        listOf("ارتفاع ضغط الدم", "فرط ضغط الدم", "hypertension"),
        listOf("سالبيوتامول", "سالبوتامول", "salbutamol", "albuterol")
    )
    fun queryVariants(raw:String):List<String> {
        val query=normalize(raw).take(160)
        if(query.isBlank())return emptyList()
        return (listOf(query)+aliases.firstOrNull{group->group.any{normalize(it)==query}}.orEmpty()).map(::literalLikeQuery).distinct()
    }

    /**
     * Expands common medical/anatomical search synonyms in Arabic so searching
     * for colloquial or alternative academic terms matches canonical entries.
     */
    fun buildIndexableTokens(vararg fields: String?): String {
        val joined = fields.filterNotNull().joinToString(" ")
        val normalizedBase = normalize(joined)
        val synonyms = buildList {
            if (normalizedBase.contains("القلب") || Regex("\\bcor\\b").containsMatchIn(normalizedBase)) {
                add("فؤاد")
                add("قلبيه")
                add("cardiac")
            }
            if (normalizedBase.contains("الكليتان") || Regex("\\bren(es)?\\b").containsMatchIn(normalizedBase)) {
                add("كليه")
                add("كلوي")
                add("كلى")
                add("renal")
            }
            if (normalizedBase.contains("الرئتان") || Regex("\\bpulmo(nes)?\\b").containsMatchIn(normalizedBase)) {
                add("رئه")
                add("تنفس")
                add("pulmonary")
            }
            if (normalizedBase.contains("فرط ضغط الدم") || normalizedBase.contains("hypertension")) {
                add("ارتفاع ضغط الدم")
                add("ضغط الدم المرتفع")
            }
        }
        return if (synonyms.isEmpty()) {
            normalizedBase
        } else {
            "$normalizedBase ${synonyms.joinToString(" ") { normalize(it) }}"
        }
    }
}

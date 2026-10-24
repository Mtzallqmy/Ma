package com.dawahayah.app.domain.model

import kotlinx.serialization.Serializable

@Serializable data class BilingualText(val ar: String, val en: String) {
    fun get(arabic: Boolean) = if (arabic) ar else en
}
@Serializable data class AcademyCourse(val id: String, val title: BilingualText, val description: BilingualText)
@Serializable data class AcademySource(val id: String, val title: String, val url: String, val license: String, val attribution: String)
@Serializable data class AcademyQuestion(val id: String, val prompt: BilingualText, val options: List<BilingualText>, val correctIndex: Int, val explanation: BilingualText)
@Serializable data class AcademyLesson(
    val id: String, val courseId: String, val moduleId: String, val revision: Int,
    val title: BilingualText, val outcomes: List<BilingualText>, val body: BilingualText,
    val example: BilingualText, val questions: List<AcademyQuestion>, val sourceIds: List<String>,
    val reviewStatus: String, val license: String, val attribution: String, val isSimulation: Boolean = false
)
@Serializable data class AcademyBundle(val schemaVersion: Int, val courses: List<AcademyCourse>, val lessons: List<AcademyLesson>, val sources: List<AcademySource>)

object AcademyContracts {
    fun validate(bundle: AcademyBundle) {
        require(bundle.schemaVersion == 1 && bundle.lessons.size <= 1000 && bundle.courses.size <= 100)
        require(bundle.courses.map { it.id }.distinct().size == bundle.courses.size)
        require(bundle.lessons.map { it.id }.distinct().size == bundle.lessons.size)
        val courses = bundle.courses.map { it.id }.toSet()
        val sources = bundle.sources.map { it.id }.toSet()
        bundle.lessons.forEach { l ->
            require(l.id.matches(Regex("[a-zA-Z0-9_-]{1,128}")) && l.courseId in courses && l.revision > 0)
            require(l.title.ar.isNotBlank() && l.title.en.isNotBlank() && l.body.ar.isNotBlank() && l.body.en.isNotBlank())
            require(l.sourceIds.isNotEmpty() && l.sourceIds.all { it in sources } && l.license.isNotBlank())
            require(l.questions.isNotEmpty() && l.questions.map { it.id }.distinct().size == l.questions.size)
            require(l.questions.map { it.prompt.en.trim().lowercase() }.distinct().size == l.questions.size)
            l.questions.forEach { q -> require(q.options.size in 2..6 && q.correctIndex in q.options.indices) }
        }
    }
    fun score(lesson: AcademyLesson, answers: Map<String, Int>): Int {
        require(answers.keys == lesson.questions.map { it.id }.toSet()) { "Answer every question exactly once" }
        require(lesson.questions.all { answers.getValue(it.id) in it.options.indices })
        return lesson.questions.count { answers[it.id] == it.correctIndex }
    }
}

package com.dawahayah.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DrugIdentifier(val vocabulary: String, val identifier: String, val sourceId: String)
@Serializable
data class DrugFact(
    val id: String, val kind: String, val detailsAr: String, val detailsEn: String, val sourceId: String,
    val severity: String = "", val relatedIngredientId: String? = null, val diseaseId: String? = null,
    val reviewStatus: String = "DRAFT", val reviewedAt: String? = null
)
@Serializable
data class DrugDosingReference(
    val id: String, val patientGroup: String, val route: String, val detailsAr: String, val detailsEn: String,
    val sourceId: String, val context: String = "", val region: String = "global", val sourceRevision: String = "",
    val maximumDose: String = "", val renalAdjustment: String = "", val hepaticAdjustment: String = "",
    val reviewStatus: String = "DRAFT", val reviewedAt: String? = null
)
@Serializable
data class RegionalMedicineProduct(
    val id: String, val brandName: String, val region: String, val sourceId: String,
    val manufacturer: String = "", val dosageForm: String = "", val strength: String = "",
    val regulator: String = "", val registrationNumber: String = "", val evidenceUrl: String = "",
    val verificationState: String = "UNVERIFIED", val verifiedAt: String? = null
)
@Serializable
data class MedicineDetails(
    val identifiers: List<DrugIdentifier> = emptyList(),
    val indications: List<DrugFact> = emptyList(),
    val safetyFacts: List<DrugFact> = emptyList(),
    val dosingReferences: List<DrugDosingReference> = emptyList(),
    val regionalProducts: List<RegionalMedicineProduct> = emptyList()
) {
    fun reviewedIndications() = indications.filter { it.reviewStatus == "CLINICALLY_REVIEWED_PUBLISHED" && !it.reviewedAt.isNullOrBlank() && it.sourceId.isNotBlank() }
    fun reviewedSafety() = safetyFacts.filter { it.reviewStatus == "CLINICALLY_REVIEWED_PUBLISHED" && !it.reviewedAt.isNullOrBlank() && it.sourceId.isNotBlank() }
    fun reviewedDoses() = dosingReferences.filter {
        it.reviewStatus == "CLINICALLY_REVIEWED_PUBLISHED" && !it.reviewedAt.isNullOrBlank() && it.sourceId.isNotBlank()
    }
    fun verifiedProducts() = regionalProducts.filter {
        it.verificationState in setOf("REGISTERED_VERIFIED", "NOT_CURRENTLY_AVAILABLE") &&
            !it.verifiedAt.isNullOrBlank() && it.registrationNumber.isNotBlank() && it.evidenceUrl.startsWith("https://") && it.sourceId.isNotBlank()
    }
}

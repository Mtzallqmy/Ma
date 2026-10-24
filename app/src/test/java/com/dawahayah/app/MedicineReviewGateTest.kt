package com.dawahayah.app
import com.dawahayah.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test
class MedicineReviewGateTest {
 @Test fun dosingNeedsPublishedReviewDateAndSource() {
  val draft=DrugDosingReference("fixture","fixture","fixture","اختبار","fixture","src")
  val reviewed=draft.copy(reviewStatus="CLINICALLY_REVIEWED_PUBLISHED",reviewedAt="2026-10-24")
  val details=MedicineDetails(dosingReferences=listOf(draft,reviewed,reviewed.copy(sourceId=""),reviewed.copy(reviewedAt=null)))
  assertEquals(listOf(reviewed),details.reviewedDoses())
 }
 @Test fun brandsNeedRegistrationEvidenceAndDate() {
  val raw=RegionalMedicineProduct("fixture","Software fixture","YE","src")
  val registered=raw.copy(registrationNumber="TEST",evidenceUrl="https://example.org/evidence",verificationState="REGISTERED_VERIFIED",verifiedAt="2026-10-24")
  assertEquals(listOf(registered),MedicineDetails(regionalProducts=listOf(raw,registered,registered.copy(evidenceUrl="http://example.org"))).verifiedProducts())
 }
}

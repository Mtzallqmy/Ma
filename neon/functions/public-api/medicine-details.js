const published = alias => `JOIN publication_status ps ON ps.code=${alias}.status_code AND ps.is_publicly_visible=TRUE`;
const txt = value => value == null ? '' : String(value);
const reviewed = alias => `${alias}.status_code='CLINICALLY_REVIEWED_PUBLISHED' AND ${alias}.status_code IN (SELECT code FROM publication_status WHERE is_publicly_visible=TRUE) AND ${alias}.reviewed_by IS NOT NULL AND btrim(${alias}.reviewed_by)<>'' AND ${alias}.reviewed_at IS NOT NULL`;
const iso = value => value ? new Date(value).toISOString() : null;

/** Parent publication and child review gates are enforced server-side. Never return reviewer identities. */
export async function medicineDetails(query, ingredientIds) {
  const result = new Map(ingredientIds.map(id => [id,{identifiers:[],indications:[],safetyFacts:[],dosingReferences:[],regionalProducts:[]} ]));
  if (!ingredientIds.length) return result;
  const load = async (table, alias, gate='TRUE') => {
    const rows = await query(`SELECT ${alias}.* FROM ${table} ${alias}
      JOIN active_ingredients i ON i.id=${alias}.ingredient_id ${published('i')}
      WHERE i.id=ANY($1::text[]) AND (${gate}) ORDER BY ${alias}.ingredient_id LIMIT 10001`,[ingredientIds]);
    if(rows.length>10000)throw new Error('Medication detail limit exceeded');
    // Stable ordering makes a content fingerprint independent of query planning.
    return rows.sort((a,b)=>JSON.stringify(a).localeCompare(JSON.stringify(b)));
  };
  for(const x of await load('ingredient_identifiers','x')) result.get(x.ingredient_id)?.identifiers.push({vocabulary:x.vocabulary,identifier:x.identifier,sourceId:x.source_id});
  for(const x of await load('drug_indications','x',reviewed('x'))) result.get(x.ingredient_id)?.indications.push({id:x.id,kind:'INDICATION',
    detailsAr:x.indication_ar,detailsEn:x.indication_en,diseaseId:x.disease_id,sourceId:x.source_id,reviewStatus:x.status_code,reviewedAt:iso(x.reviewed_at)});
  for(const x of await load('drug_safety_facts','x',reviewed('x'))) result.get(x.ingredient_id)?.safetyFacts.push({id:x.id,kind:x.fact_type,
    severity:x.severity,relatedIngredientId:x.related_ingredient_id,detailsAr:x.details_ar,detailsEn:x.details_en,sourceId:x.source_id,reviewStatus:x.status_code,reviewedAt:iso(x.reviewed_at)});
  for(const x of await load('drug_dosing_references','x',reviewed('x'))) result.get(x.ingredient_id)?.dosingReferences.push({id:x.id,patientGroup:x.patient_group,
    route:x.administration_route,detailsAr:x.dosage_text_ar,detailsEn:x.dosage_text_en,sourceId:x.source_id,context:txt(x.indication_context),region:x.applicable_region,
    sourceRevision:txt(x.source_revision),maximumDose:txt(x.maximum_dose_text),renalAdjustment:txt(x.renal_adjustment_text),hepaticAdjustment:txt(x.hepatic_adjustment_text),reviewStatus:x.status_code,reviewedAt:iso(x.reviewed_at)});
  for(const x of await load('regional_medicine_products','x',`x.status_code IN (SELECT code FROM publication_status WHERE is_publicly_visible=TRUE)
    AND x.verification_state IN ('REGISTERED_VERIFIED','NOT_CURRENTLY_AVAILABLE') AND x.verified_at IS NOT NULL
    AND x.registration_number IS NOT NULL AND btrim(x.registration_number)<>'' AND x.evidence_url LIKE 'https://%'`)) result.get(x.ingredient_id)?.regionalProducts.push({id:x.id,brandName:x.brand_name,region:x.market_region,
      manufacturer:txt(x.manufacturer),dosageForm:txt(x.dosage_form),strength:txt(x.strength_label),regulator:txt(x.regulator_name),registrationNumber:txt(x.registration_number),
      evidenceUrl:txt(x.evidence_url),verificationState:x.verification_state,verifiedAt:iso(x.verified_at),sourceId:x.source_id});
  return result;
}

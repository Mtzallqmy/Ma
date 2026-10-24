import { createHash } from 'node:crypto';
import { medicineDetails } from './medicine-details.js';

const text = value => value == null ? '' : String(value);
const date = value => value == null ? null : new Date(value).toISOString().slice(0, 10);
const visible = alias => `JOIN publication_status ps ON ps.code=${alias}.status_code AND ps.is_publicly_visible=TRUE`;
const camel = row => Object.fromEntries(Object.entries(row).map(([key, value]) => [key.replace(/_([a-z0-9])/g, (_, c) => c.toUpperCase()), value]));
const group = (rows, key) => Map.groupBy(rows, row => row[key]);

/** Run inside a REPEATABLE READ transaction. No drafts, credentials or editorial identities. */
export async function publishedSnapshot(query) {
  async function rows(statement) {
    const result = await query(statement + ' LIMIT 10001');
    if (result.length > 10000) throw new Error('Snapshot exceeds supported size; pagination required');
    return result;
  }
  const systems = await rows(`SELECT b.* FROM body_systems b ${visible('b')} ORDER BY b.id`);
  const organs = await rows(`SELECT o.* FROM organs o ${visible('o')} JOIN body_systems b ON b.id=o.system_id JOIN publication_status bs ON bs.code=b.status_code AND bs.is_publicly_visible=TRUE ORDER BY o.id`);
  const diseases = await rows(`SELECT d.* FROM diseases d ${visible('d')} ORDER BY d.id`);
  const ingredients = await rows(`SELECT i.* FROM active_ingredients i ${visible('i')} ORDER BY i.id`);
  const drugDetails = await medicineDetails(query,ingredients.map(i=>i.id));
  const sourceRows = await rows('SELECT id,title,url,publisher,publication_date,retrieved_at,reuse_license,attribution FROM sources ORDER BY id');
  const citations = await rows('SELECT entity_id,source_id FROM citations ORDER BY entity_id,source_id');
  const structures = await rows(`SELECT s.* FROM anatomical_structures s JOIN organs o ON o.id=s.organ_id ${visible('o')} ORDER BY s.id`);
  const sections = await rows(`SELECT s.* FROM organ_sections s JOIN organs o ON o.id=s.organ_id ${visible('o')} ORDER BY s.organ_id,s.order_index,s.id`);
  const figures = await rows(`SELECT s.* FROM science_figures s JOIN organs o ON o.id=s.organ_id ${visible('o')} ORDER BY s.id`);
  const diseaseSections = await rows(`SELECT s.* FROM disease_sections s JOIN diseases d ON d.id=s.disease_id ${visible('d')} ORDER BY s.disease_id,s.order_index,s.id`);
  const references = group(citations, 'entity_id');
  const sourceIds = id => [...new Set((references.get(id) ?? []).map(r => r.source_id))];
  const structuresByOrgan = group(structures, 'organ_id');
  const sectionsByOrgan = group(sections, 'organ_id');
  const figuresByOrgan = group(figures, 'organ_id');
  const sectionsByDisease = group(diseaseSections, 'disease_id');
  const publicDiseaseIds = new Set(diseases.map(d => d.id));
  const publicSystemIds = new Set(systems.map(s => s.id));
  const publicOrganIds = new Set(organs.map(o => o.id));
  const bundle = {
    schemaVersion: 1, seedRevision: 'published-snapshot',
    systems: systems.map(s => ({
      id:s.id,nameAr:s.name_ar,nameEn:s.name_en,latinName:s.latin_name,
      descriptionAr:s.description_ar,descriptionEn:s.description_en,sourceIds:sourceIds(s.id),
      lastReviewedAt:date(s.last_human_reviewed_at),hasDetailedPathway:s.has_detailed_pathway,
      interSystemRelationsAr:text(s.inter_system_relations_ar),interSystemRelationsEn:text(s.inter_system_relations_en),relatedDrugClasses:s.related_drug_classes ?? []
    })),
    organs: organs.map(o => ({
      id:o.id,systemId:o.system_id,nameAr:o.name_ar,nameEn:o.name_en,latinName:o.latin_name,
      summaryAr:o.summary_ar,summaryEn:o.summary_en,sourceIds:sourceIds(o.id),locationAr:text(o.location_ar),locationEn:text(o.location_en),
      has3dAsset:o.has_3d_asset,asset3dId:o.asset_3d_id,
      relatedDiseaseIds:(o.related_disease_ids ?? []).filter(id => publicDiseaseIds.has(id)),relatedDrugClasses:o.related_drug_classes ?? [],
      structures:(structuresByOrgan.get(o.id) ?? []).map(s => {
        return {id:s.id,organId:s.organ_id,meshNodeName:s.mesh_node_name,nameAr:s.name_ar,nameEn:s.name_en,
          latinName:s.latin_name,ta2Code:s.ta2_code,structureType:s.structure_type,functionAr:s.function_ar,functionEn:s.function_en,
          clinicalRelevanceAr:s.clinical_relevance_ar,clinicalRelevanceEn:s.clinical_relevance_en,sourceId:s.source_id};
      }),
      sections:(sectionsByOrgan.get(o.id) ?? []).map(camel),
      figures:(figuresByOrgan.get(o.id) ?? []).map(s => ({id:s.id,organId:s.organ_id,titleAr:s.title_ar,titleEn:s.title_en,
        provenanceType:s.provenance_type,license:s.license,attribution:s.attribution,annotations:s.annotations_json ?? []}))
    })),
    diseases:diseases.map(d => ({
      id:d.id,primarySystemId:d.primary_system_id,nameAr:d.name_ar,nameEn:d.name_en,
      introductionAr:d.introduction_ar,introductionEn:d.introduction_en,citationIds:sourceIds(d.id),reviewStatus:d.status_code,
      icd11Code:text(d.icd11_code),secondarySystemIds:(d.secondary_system_ids ?? []).filter(id => publicSystemIds.has(id)),
      affectedOrganIds:(d.affected_organ_ids ?? []).filter(id => publicOrganIds.has(id)),relatedDrugClasses:d.related_drug_classes ?? [],
      importedAt:date(d.imported_at) ?? '',humanReviewedAt:date(d.human_reviewed_at),sections:(sectionsByDisease.get(d.id) ?? []).map(camel)
    })),
    activeIngredients:ingredients.map(i => ({id:i.id,nameAr:i.name_ar,nameEn:i.name_en,className:i.class_name,
      generalDescriptionAr:i.general_description_ar,generalDescriptionEn:i.general_description_en,safetyNote:i.safety_note,
      citationIds:sourceIds(i.id),reviewStatus:i.status_code,details:drugDetails.get(i.id)})),
    sources:[]
  };
  const used = new Set([
    ...bundle.systems.flatMap(s => s.sourceIds),...bundle.organs.flatMap(o => o.sourceIds),
    ...bundle.organs.flatMap(o => o.structures.map(s => s.sourceId)),
    ...bundle.diseases.flatMap(d => d.citationIds),...bundle.activeIngredients.flatMap(i => [...i.citationIds,...Object.values(i.details).flat().map(x=>x.sourceId)])
  ]);
  bundle.sources = sourceRows.filter(s => used.has(s.id)).map(s => ({id:s.id,title:s.title,url:s.url,publisher:s.publisher,
    publicationDate:s.publication_date ?? null,accessedAt:date(s.retrieved_at),license:s.reuse_license,attribution:s.attribution}));
  const revision = createHash('sha256').update(JSON.stringify(bundle)).digest('hex');
  return {schemaVersion:1,complete:true,revision,bundle};
}

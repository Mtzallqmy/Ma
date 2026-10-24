"""Reproducible insert-only migration from existing, unreviewed educational assets."""
import json, pathlib, hashlib
root=pathlib.Path(__file__).resolve().parents[1]
seed=json.loads((root/'app/src/main/assets/seed/base_seed_v1.json').read_text())
def literal(v):
 if v is None:return 'NULL'
 if isinstance(v,(int,float)):return str(v)
 return "'"+str(v).replace("'","''")+"'"
statements=[]
def insert(table,fields,values):
 statements.append(f"INSERT INTO {table} ({','.join(fields)}) VALUES ({','.join(literal(v) for v in values)}) ON CONFLICT DO NOTHING;")
for source in seed['sources']:
 insert('sources',['id','title','url','publisher','publication_date','reuse_license','attribution'],[source[k] for k in ['id','title','url','publisher','publicationDate','license','attribution']])
for kind,key in [('SYSTEM','systems'),('ORGAN','organs'),('DISEASE','diseases'),('INGREDIENT','activeIngredients')]:
 for entity in seed[key]:
  for source in entity.get('sourceIds',entity.get('citationIds',[])):
   insert('citations',['entity_type','entity_id','source_id','citation_note'],[kind,entity['id'],source,'Existing educational Seed reference; scientific review deferred.'])
for organ in seed['organs']:
 for st in organ.get('structures',[]):
  fields=['id','organ_id','mesh_node_name','name_ar','name_en','latin_name','ta2_code','structure_type','function_ar','function_en','clinical_relevance_ar','clinical_relevance_en','source_id']
  keys=['id','organId','meshNodeName','nameAr','nameEn','latinName','ta2Code','structureType','functionAr','functionEn','clinicalRelevanceAr','clinicalRelevanceEn','sourceId']
  insert('anatomical_structures',fields,[st.get(k) for k in keys])
 for section in organ.get('sections',[]):
  insert('organ_sections',['id','organ_id','section_key','title_ar','title_en','body_ar','body_en','order_index'],[section[k] for k in ['id','organId','sectionKey','titleAr','titleEn','bodyAr','bodyEn','orderIndex']])
 for figure in organ.get('figures',[]):
  insert('science_figures',['id','organ_id','title_ar','title_en','provenance_type','license','attribution','annotations_json'],[figure['id'],organ['id'],figure['titleAr'],figure['titleEn'],'SCHEMATIC_EDUCATIONAL',figure['license'],figure['attribution'],json.dumps(figure.get('annotations',[]),ensure_ascii=False)])
 for source in organ['sourceIds']:
  insert('source_attributions',['id','entity_id','entity_type','source_id','imported_at','human_reviewed_at','review_status'],[f"stage02-{organ['id']}-{source}",organ['id'],'ORGAN',source,'2026-10-24',None,'under_review'])
for disease in seed['diseases']:
 for section in disease.get('sections',[]):
  insert('disease_sections',['id','disease_id','section_code','title_ar','title_en','content_ar','content_en','information_tier','order_index'],[section[k] for k in ['id','diseaseId','sectionCode','titleAr','titleEn','contentAr','contentEn','informationTier','orderIndex']])
# Serve existing byte-identical assets by immutable repository commit, without inventing a storage bucket.
commit='58cac1a131d015c334c3c0ad607d3ae9f4d3efa4'
for path in sorted((root/'app/src/main/assets/packs').glob('*/manifest.json')):
 pack=json.loads(path.read_text())
 insert('content_packs',['pack_id','version','scope_type','scope_id','title_ar','title_en','language','content_kind','quality','schema_version','min_app_version','total_size_bytes','license','attribution','status_code'],[pack[k] for k in ['packId','version','scopeType','scopeId','titleAr','titleEn','language','contentKind','quality','schemaVersion','minAppVersion','sizeBytes','license','attribution']]+['EDUCATIONAL_FOUNDATION_PUBLISHED'])
 for entry in pack['files']:
  raw=(path.parent/entry['path']).read_bytes()
  assert len(raw)==entry['sizeBytes'] and hashlib.sha256(raw).hexdigest()==entry['sha256']
  url=f"https://raw.githubusercontent.com/Mtzallqmy/Ma/{commit}/app/src/main/assets/packs/{pack['packId']}/{entry['path']}"
  insert('media_assets',['id','bucket_name','object_key','content_type','size_bytes','sha256_checksum','access_policy','quality_level','license','attribution','source_id','status_code','public_url'],[entry['assetId'],'repository-public-assets',f"stage02/{pack['packId']}/{entry['path']}",entry['contentType'],entry['sizeBytes'],entry['sha256'],'public_read',pack['quality'],pack['license'],pack['attribution'],pack['sources'][0] if pack.get('sources') else None,'EDUCATIONAL_FOUNDATION_PUBLISHED',url])
  insert('content_pack_files',['pack_id','relative_path','asset_id','content_type','size_bytes','sha256_checksum'],[pack['packId'],entry['path'],entry['assetId'],entry['contentType'],entry['sizeBytes'],entry['sha256']])
 for source in pack.get('sources',[]):insert('citations',['entity_type','entity_id','source_id','citation_note'],['CONTENT_PACK',pack['packId'],source,'Educational pack; human scientific review deferred.'])
text='-- Insert-only existing project content. No clinical approval or status changes.\nDO $stage02$\nBEGIN\n'+'\n'.join(statements)+'\nEND\n$stage02$;\n'
(root/'neon/migrations/005_stage02_existing_content.sql').write_text(text)
print(f'{len(statements)} insert-only statements; {len(text.encode())} bytes')

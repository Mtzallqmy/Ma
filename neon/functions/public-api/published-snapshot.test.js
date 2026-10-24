import { test } from 'node:test';
import assert from 'node:assert/strict';
import { publishedSnapshot } from './published-snapshot.js';

function fixture(overrides={}) {
  const data={body_systems:[{id:'sys-a',name_ar:'نظام',name_en:'System',description_ar:'أ',description_en:'A',status_code:'PUBLISHED',has_detailed_pathway:true}],
    organs:[{id:'organ-a',system_id:'sys-a',name_ar:'عضو',name_en:'Organ',summary_ar:'أ',summary_en:'A',has_3d_asset:false,related_disease_ids:['secret-draft']}],
    diseases:[],active_ingredients:[],sources:[{id:'src-public',title:'Public reference',url:'https://example.org',publisher:'Publisher',reuse_license:'Original',attribution:'Author'},{id:'src-private',title:'Unreferenced draft source'}],
    citations:[{entity_id:'organ-a',source_id:'src-public'},{entity_id:'secret-draft',source_id:'src-private'}],anatomical_structures:[],organ_sections:[],science_figures:[],disease_sections:[],...overrides};
  return async statement => {
    const table=statement.match(/FROM (\w+)/)[1];
    if(['body_systems','organs','diseases','active_ingredients'].includes(table)) assert.match(statement,/is_publicly_visible=TRUE/);
    return data[table] ?? [];
  };
}

test('snapshot keeps nested published content and excludes unrelated sources and draft links', async()=>{
  const s=await publishedSnapshot(fixture());
  assert.equal(s.complete,true);assert.match(s.revision,/^[0-9a-f]{64}$/);
  assert.deepEqual(s.bundle.organs[0].relatedDiseaseIds,[]);
  assert.deepEqual(s.bundle.sources.map(x=>x.id),['src-public']);
  assert.equal(s.bundle.diseases.length,0);
});
test('same content keeps token; same-count edit changes token',async()=>{
 const a=await publishedSnapshot(fixture());const b=await publishedSnapshot(fixture());
 assert.equal(a.revision,b.revision);
 const c=await publishedSnapshot(fixture({organ_sections:[{id:'sec',organ_id:'organ-a',title_ar:'Edit',order_index:1}]}));
 assert.notEqual(a.revision,c.revision);
});
test('withdrawal changes token; oversized snapshots fail instead of truncating',async()=>{
 const a=await publishedSnapshot(fixture());const b=await publishedSnapshot(fixture({organs:[]}));
 assert.notEqual(a.revision,b.revision);
 await assert.rejects(()=>publishedSnapshot(fixture({body_systems:Array(10001).fill({id:'x'})})),/exceeds/);
});

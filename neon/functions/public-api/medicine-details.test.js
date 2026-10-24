import {test} from 'node:test';
import assert from 'node:assert/strict';
import {medicineDetails} from './medicine-details.js';
test('no parent records means no child data requests',async()=>{
 const result=await medicineDetails(()=>{throw Error('Must not query drafts');},[]); assert.equal(result.size,0);
});
test('queries enforce parent, clinical and regional evidence gates and omit reviewer identity',async()=>{
 let calls=0;
 const result=await medicineDetails(async statement=>{
  calls++;assert.match(statement,/i\.status_code/);assert.match(statement,/i\.id=ANY\(\$1::text\[\]\)/);
  if(statement.includes('FROM drug_'))assert.match(statement,/reviewed_at IS NOT NULL/);
  if(statement.includes('FROM regional_')){assert.match(statement,/registration_number/);assert.match(statement,/https:\/\//);}
  if(statement.includes('FROM drug_dosing'))return [{id:'fixture',ingredient_id:'ing-test',patient_group:'Educational fixture',administration_route:'fixture',dosage_text_ar:'اختبار برمجي',dosage_text_en:'Software fixture',source_id:'src-test',status_code:'CLINICALLY_REVIEWED_PUBLISHED',reviewed_at:'2026-10-24',reviewed_by:'private-reviewer'}];
  return [];
 },['ing-test']);
 assert.equal(calls,5);assert.equal(result.get('ing-test').dosingReferences.length,1);
 assert.equal(JSON.stringify([...result.values()]).includes('private-reviewer'),false);
});

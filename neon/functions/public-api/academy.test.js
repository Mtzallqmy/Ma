import { test } from 'node:test';
import assert from 'node:assert/strict';
import { generateKeyPair, SignJWT, jwtVerify } from 'jose';
import { academyIdentity } from './academy-auth.js';
import { academyAdmin, validateLesson } from './academy-service.js';
import { academySnapshot } from './academy-snapshot.js';
import { readFileSync } from 'node:fs';
const bundle=JSON.parse(readFileSync(new URL('../../../app/src/main/assets/seed/academy_v1.json',import.meta.url)));
// This module lives in neon/functions/public-api: root is three parents above.
test('original bilingual lessons have unique questions and valid answer keys',()=>{
 for(const lesson of bundle.lessons)assert.equal(validateLesson(lesson),true);
 const invalid=structuredClone(bundle.lessons[0]);invalid.questions.push(invalid.questions[0]);assert.equal(validateLesson(invalid),false);
});
test('authenticated identity requires a valid signed, unexpired token and correct issuer',async()=>{
 const {privateKey,publicKey}=await generateKeyPair('EdDSA');
 const make=(issuer,exp)=>new SignJWT({}).setProtectedHeader({alg:'EdDSA'}).setSubject('verified-user').setIssuer(issuer).setIssuedAt().setExpirationTime(exp).sign(privateKey);
 const verify=token=>jwtVerify(token,publicKey,{issuer:'https://trusted.example',algorithms:['EdDSA']});
 const request=token=>new Request('https://api.example',{headers:{authorization:'Bearer '+token}});
 assert.equal(await academyIdentity(request(await make('https://trusted.example','2m')),verify),'verified-user');
 await assert.rejects(()=>academyIdentity(request('unsigned.forged.token'),verify));
 await assert.rejects(async()=>academyIdentity(request(await make('https://wrong.example','2m')),verify));
 await assert.rejects(async()=>academyIdentity(request(await make('https://trusted.example','-1m')),verify));
});
test('anonymous and unassigned users cannot read drafts or mutate; identity never comes from posted actor',async()=>{
 let calls=0;
 const noQuery=async()=>{calls++;return[]};
 assert.equal((await academyAdmin(new Request('https://api.example/v1/admin/lessons'),noQuery)).status,401);assert.equal(calls,0);
 assert.equal((await academyAdmin(new Request('https://api.example/v1/admin/lessons'),noQuery,async()=>'unassigned')).status,403);
 const req=new Request('https://api.example/v1/admin/transition',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({id:'academy-units',expectedRevision:1,action:'WITHDRAW',actor:'forged-owner',role:'OWNER'})});
 let actor;
 const response=await academyAdmin(req,async(sql,args)=>{if(sql.includes('memberships'))return [{role:'EDITOR'}];actor=args[0];throw Object.assign(new Error(),{code:'42501'});},async()=>'verified-editor');
 assert.equal(actor,'verified-editor');assert.equal(response.status,403);
});
test('public catalog uses only published payloads and referenced sources',async()=>{
 const queries=[];
 const b=await academySnapshot(async sql=>{queries.push(sql);if(sql.includes('academy.lessons'))return [{payload:bundle.lessons[0]}];if(sql.includes('academy.courses'))return bundle.courses.map(payload=>({payload}));return [...bundle.sources.map(payload=>({payload})),{payload:{id:'private-reference'}}];});
 assert.equal(b.lessons.length,1);assert.equal(b.courses.length,1);assert.deepEqual(b.sources,bundle.sources.filter(s=>bundle.lessons[0].sourceIds.includes(s.id)));
 assert.match(queries[0],/published_body IS NOT NULL/);assert.doesNotMatch(queries[0],/draft/);
});

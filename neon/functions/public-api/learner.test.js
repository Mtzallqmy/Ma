import {test} from 'node:test';import assert from 'node:assert/strict';
import {learnerService} from './learner-service.js';
import {readFileSync} from 'node:fs';
const lesson=JSON.parse(readFileSync(new URL('../../../app/src/main/assets/seed/academy_v1.json',import.meta.url))).lessons[0];
const request=(path,data,token)=>new Request('https://test.example'+path,{method:'POST',headers:{'content-type':'application/json',...(token?{authorization:'Device '+token}:{})},body:JSON.stringify(data)});
function database(){const state={link:null,session:null,attempts:[],user:'user-a'};const sql=async(parts,...args)=>{const s=parts.join('?');
 if(s.startsWith('DELETE FROM academy.device_links WHERE expires_at'))return[];
 if(s.startsWith('INSERT INTO academy.device_links')){state.link={device_hash:args[0],user_code:args[1],approved_user_id:null};return[{user_code:args[1]}];}
 if(s.startsWith('UPDATE academy.device_links')){if(state.link?.user_code!==args[1])return[];state.link.approved_user_id=args[0];return[{user_code:args[1]}];}
 if(s.startsWith('SELECT * FROM academy.device_links'))return state.link?.device_hash===args[0]?[state.link]:[];
 if(s.startsWith('INSERT INTO academy.device_sessions')){state.session={hash:args[0],user:args[1]};return[];}
 if(s.startsWith('DELETE FROM academy.device_links')){state.link=null;return[];}
 if(s.startsWith('SELECT user_id FROM academy.device_sessions'))return state.session?.hash===args[0]?[{user_id:state.session.user}]:[];
 if(s.startsWith('SELECT payload FROM academy.published_versions'))return args[0]===lesson.id&&args[1]===lesson.revision?[{payload:lesson}]:[];
 if(s.startsWith('SELECT payload FROM academy.learner_attempts'))return state.attempts.filter(a=>a.user===args[0]&&(!s.includes('AND id=')||a.payload.id===args[1])).map(a=>({payload:a.payload}));
 if(s.startsWith('SELECT payload=')){const a=state.attempts.find(a=>a.user===args[1]&&a.payload.id===args[2]);return[{equal:JSON.stringify(a?.payload)===JSON.stringify(args[0])}];}
 if(s.startsWith('INSERT INTO academy.learner_attempts')){if(!state.attempts.some(a=>a.user===args[0]&&a.payload.id===args[1]))state.attempts.push({user:args[0],payload:args[2]});return[];}
 throw new Error('Unexpected SQL '+s);
 };sql.begin=fn=>fn(sql);sql.json=x=>x;return{sql,state};}
test('device pairing stores hashes, requires approval, and consumes the device code once',async()=>{
 const {sql,state}=database();const start=await(await learnerService(request('/v1/link/start',{}),sql)).json();
 assert.notEqual(state.link.device_hash,start.deviceCode);assert.equal(state.link.device_hash.length,64);
 assert.equal((await learnerService(request('/v1/link/poll',{deviceCode:start.deviceCode}),sql)).status,202);
 await learnerService(request('/v1/link/approve',{userCode:start.userCode}),sql,async()=>'user-a');
 const token=await(await learnerService(request('/v1/link/poll',{deviceCode:start.deviceCode}),sql)).json();
 assert.equal(token.deviceToken.length,64);assert.notEqual(state.session.hash,token.deviceToken);
 assert.equal((await learnerService(request('/v1/link/poll',{deviceCode:start.deviceCode}),sql)).status,410);
});
test('sync regrades answers, isolates users, and rejects conflicting attempt IDs',async()=>{
 const {sql,state}=database();const answers=Object.fromEntries(lesson.questions.map(q=>[q.id,q.correctIndex]));
 const attempt={id:'software-fixture',lessonId:lesson.id,lessonRevision:lesson.revision,answersJson:JSON.stringify(answers),correct:999,total:999,completedAt:1};
 let response=await learnerService(request('/v1/learner/sync',{attempts:[attempt]}),sql,async()=>'user-a');assert.equal(response.status,200);const saved=(await response.json()).attempts[0];assert.equal(saved.correct,2);assert.equal(saved.total,2);
 response=await learnerService(request('/v1/learner/sync',{attempts:[]}),sql,async()=>'user-b');assert.deepEqual((await response.json()).attempts,[]);
 response=await learnerService(request('/v1/learner/sync',{attempts:[{...attempt,completedAt:2}]}),sql,async()=>'user-a');assert.equal(response.status,409);assert.equal(state.attempts[0].payload.completedAt,1);
});
test('unknown device credentials never read learner records',async()=>{const {sql}=database();assert.equal((await learnerService(request('/v1/learner/sync',{attempts:[]},'a'.repeat(64)),sql)).status,401);});

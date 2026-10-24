import { academyIdentity } from './academy-auth.js';

export function validateLesson(l) {
  const bilingual=x=>x && typeof x.ar==='string' && x.ar.trim() && typeof x.en==='string' && x.en.trim();
  if(!l || !bilingual(l.title) || !bilingual(l.body) || !bilingual(l.example) || !Array.isArray(l.outcomes) || !l.outcomes.length || !l.outcomes.every(bilingual) || typeof l.license!=='string' || !l.license.trim() || typeof l.attribution!=='string')return false;
  if(!Array.isArray(l.questions) || !l.questions.length || l.questions.length>100 || !Array.isArray(l.sourceIds) || !l.sourceIds.length || !l.sourceIds.every(x=>typeof x==='string'&&x.trim()))return false;
  const ids=new Set(),prompts=new Set();
  return l.questions.every(q=>{
    if(typeof q.id!=='string' || !q.id || ids.has(q.id) || !bilingual(q.prompt) || prompts.has(q.prompt.en.trim().toLowerCase()) || !bilingual(q.explanation) || !Array.isArray(q.options) || q.options.length<2 || q.options.length>6 || !q.options.every(bilingual) || !Number.isInteger(q.correctIndex) || q.correctIndex<0 || q.correctIndex>=q.options.length)return false;
    ids.add(q.id);prompts.add(q.prompt.en.trim().toLowerCase());return true;
  });
}
const reply=(body,status=200)=>Response.json(body,{status,headers:{'Cache-Control':'no-store'}});
async function bodyJson(request) {
  if(!request.headers.get('content-type')?.startsWith('application/json'))throw Object.assign(new Error('JSON required'),{status:415});
  const reader=request.body?.getReader();if(!reader)throw Object.assign(new Error('Body required'),{status:400});
  const chunks=[];let length=0;
  try {while(true){const {value,done}=await reader.read();if(done)break;length+=value.length;if(length>131072)throw Object.assign(new Error('Body too large'),{status:413});chunks.push(value);}} finally {await reader.cancel();}
  try{return JSON.parse(Buffer.concat(chunks).toString('utf8'));}catch{throw Object.assign(new Error('Invalid JSON'),{status:400});}
}
export async function academyAdmin(request,query,identity=academyIdentity) {
  try {
    const user=await identity(request);
    const members=await query('SELECT role FROM academy.memberships WHERE user_id=$1',[user]);
    const path=new URL(request.url).pathname;
    if(request.method==='GET'&&path==='/v1/admin/me')return reply({userId:user,role:members[0]?.role??null});
    if(!members.length)return reply({error:'Forbidden'},403);
    if(request.method==='GET'&&path==='/v1/admin/audit'){
      if(members[0].role!=='OWNER')return reply({error:'Forbidden'},403);
      return reply(await query('SELECT id,actor_id,action,entity_id,revision,occurred_at FROM academy.audit_logs ORDER BY id DESC LIMIT 200'));
    }
    if(request.method==='POST'&&path==='/v1/admin/membership'){
      if(members[0].role!=='OWNER')return reply({error:'Forbidden'},403);
      const b=await bodyJson(request);
      if(typeof b.userId!=='string'||!b.userId.trim()||b.userId.length>256||!['OWNER','REVIEWER','EDITOR','READ_ONLY','NONE'].includes(b.role))return reply({error:'Invalid membership'},400);
      const rows=await query('SELECT academy.set_membership($1,$2,$3) AS result',[user,b.userId,b.role]);return reply(rows[0].result);
    }
    if(request.method==='GET' && path==='/v1/admin/lessons')return reply(await query('SELECT id,revision,draft,reviewed_revision,published_revision FROM academy.lessons ORDER BY id LIMIT 1000'));
    if(request.method!=='POST' || path!=='/v1/admin/transition')return reply({error:'Route unavailable'},404);
    const b=await bodyJson(request);
    if(typeof b.id!=='string' || !/^[a-zA-Z0-9_-]{1,128}$/.test(b.id) || !Number.isInteger(b.expectedRevision) || b.expectedRevision<1 || !['EDIT','REVIEW','PUBLISH','WITHDRAW'].includes(b.action) || (b.action==='EDIT'&&!validateLesson(b.body)) || (b.note!==undefined&&typeof b.note!=='string'))return reply({error:'Invalid transition'},400);
    const rows=await query('SELECT academy.transition($1,$2,$3,$4,$5::jsonb,$6) AS result',[user,b.id,b.expectedRevision,b.action,b.body?JSON.stringify(b.body):null,(b.note??'').slice(0,4000)]);
    return reply(rows[0].result);
  } catch(e) {
    const status=e.status ?? ({'42501':403,'40001':409,'P0002':404,'22023':400}[e.code]) ?? 503;
    return reply({error:status===409?'Revision conflict; reload before editing':status===401?'Unauthorized':status===403?'Forbidden':'Request could not be completed'},status);
  }
}

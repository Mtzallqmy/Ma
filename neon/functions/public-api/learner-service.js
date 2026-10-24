import {randomBytes,createHash} from 'node:crypto';
import {academyIdentity} from './academy-auth.js';
const hash=v=>createHash('sha256').update(v).digest('hex');
const reply=(v,s=200)=>Response.json(v,{status:s,headers:{'Cache-Control':'no-store'}});
async function body(req){const r=req.body?.getReader();if(!r)throw {status:400};let bytes=0,chunks=[];try{while(true){const x=await r.read();if(x.done)break;bytes+=x.value.length;if(bytes>262144)throw {status:413};chunks.push(x.value);}}finally{await r.cancel();}try{return JSON.parse(Buffer.concat(chunks).toString('utf8'));}catch{throw {status:400};}}
export async function learnerService(req,sql,identity=academyIdentity){
 const path=new URL(req.url).pathname;if(!path.startsWith('/v1/learner/')&&!path.startsWith('/v1/link/'))return null;
 try{
  if(req.method==='POST'&&path==='/v1/link/start'){
   await sql`DELETE FROM academy.device_links WHERE expires_at<now()`;
   const deviceCode=randomBytes(32).toString('hex'),userCode=randomBytes(16).toString('hex');
   // Fixed lifetime, bounded population, no identifying data in anonymous link creation.
   const rows=await sql`INSERT INTO academy.device_links(device_hash,user_code,expires_at) SELECT ${hash(deviceCode)},${userCode},now()+interval '5 minutes' WHERE (SELECT count(*) FROM academy.device_links WHERE expires_at>now())<100 RETURNING user_code`;
   if(!rows.length)return reply({error:'Try later'},429);
   return reply({deviceCode,userCode,expiresIn:300});
  }
  if(req.method==='POST'&&path==='/v1/link/approve'){
   const user=await identity(req),b=await body(req);if(typeof b.userCode!=='string'||!/^[a-f0-9]{32}$/.test(b.userCode))return reply({error:'Invalid code'},400);
   const rows=await sql`UPDATE academy.device_links SET approved_user_id=${user} WHERE user_code=${b.userCode} AND expires_at>now() AND approved_user_id IS NULL RETURNING user_code`;
   return reply({approved:!!rows.length},rows.length?200:410);
  }
  if(req.method==='POST'&&path==='/v1/link/poll'){
   const b=await body(req);if(typeof b.deviceCode!=='string'||!/^[a-f0-9]{64}$/.test(b.deviceCode))return reply({error:'Invalid code'},400);
   return await sql.begin(async tx=>{
    const rows=await tx`SELECT * FROM academy.device_links WHERE device_hash=${hash(b.deviceCode)} AND expires_at>now() FOR UPDATE`;
    if(!rows.length)return reply({error:'Expired link'},410);
    if(!rows[0].approved_user_id)return reply({pending:true},202);
    const token=randomBytes(32).toString('hex');
    await tx`INSERT INTO academy.device_sessions VALUES(${hash(token)},${rows[0].approved_user_id},now()+interval '30 days')`;
    await tx`DELETE FROM academy.device_links WHERE device_hash=${hash(b.deviceCode)}`;
    return reply({deviceToken:token});
   });
  }
  const header=req.headers.get('authorization')??'';let user;
  if(header.startsWith('Device ')&&/^[a-f0-9]{64}$/.test(header.slice(7))){
   const rows=await sql`SELECT user_id FROM academy.device_sessions WHERE token_hash=${hash(header.slice(7))} AND expires_at>now()`;if(!rows.length)return reply({error:'Unauthorized'},401);user=rows[0].user_id;
  }else user=await identity(req);
  if(req.method==='POST'&&path==='/v1/learner/sign-out'){
   if(header.startsWith('Device '))await sql`DELETE FROM academy.device_sessions WHERE token_hash=${hash(header.slice(7))}`;
   return reply({signedOut:true});
  }
  if(req.method==='POST'&&path==='/v1/learner/sync'){
   const b=await body(req);if(!Array.isArray(b.attempts)||b.attempts.length>100)return reply({error:'Invalid batch'},400);
   return await sql.begin(async tx=>{
    for(const a of b.attempts){
     if(typeof a.id!=='string'||! /^[a-zA-Z0-9-]{1,80}$/.test(a.id)||typeof a.lessonId!=='string'||!Number.isInteger(a.lessonRevision)||typeof a.answersJson!=='string'||!Number.isSafeInteger(a.completedAt)||a.completedAt<0||a.completedAt>Date.now()+86400000)throw {status:400};
     const versions=await tx`SELECT payload FROM academy.published_versions WHERE lesson_id=${a.lessonId} AND revision=${a.lessonRevision}`;if(!versions.length)throw {status:409};
     let answers;try{answers=JSON.parse(a.answersJson);}catch{throw {status:400};}
     const qs=versions[0].payload.questions;if(!answers||Object.keys(answers).length!==qs.length||!qs.every(q=>Number.isInteger(answers[q.id])&&answers[q.id]>=0&&answers[q.id]<q.options.length))throw {status:400};
     const canonical={id:a.id,lessonId:a.lessonId,lessonRevision:a.lessonRevision,answersJson:JSON.stringify(Object.fromEntries(Object.entries(answers).sort(([a],[b])=>a.localeCompare(b)))),correct:qs.filter(q=>answers[q.id]===q.correctIndex).length,total:qs.length,completedAt:a.completedAt};
     const existing=await tx`SELECT payload FROM academy.learner_attempts WHERE user_id=${user} AND id=${a.id} FOR UPDATE`;
     if(existing.length&&JSON.stringify(existing[0].payload)!==JSON.stringify(JSON.parse(JSON.stringify(canonical)))){
      // jsonb key ordering differs from JS: compare canonical JSON in PostgreSQL instead.
      const same=await tx`SELECT payload=${tx.json(canonical)} AS equal FROM academy.learner_attempts WHERE user_id=${user} AND id=${a.id}`;if(!same[0].equal)throw {status:409};
     }
     await tx`INSERT INTO academy.learner_attempts(user_id,id,payload) VALUES(${user},${a.id},${tx.json(canonical)}) ON CONFLICT DO NOTHING`;
     const check=await tx`SELECT payload=${tx.json(canonical)} AS equal FROM academy.learner_attempts WHERE user_id=${user} AND id=${a.id}`;if(!check[0].equal)throw {status:409};
    }
    const saved=await tx`SELECT payload FROM academy.learner_attempts WHERE user_id=${user} ORDER BY received_at,id LIMIT 1001`;
    if(saved.length>1000)throw {status:413};
    return reply({attempts:saved.map(x=>x.payload)});
   });
  }
  return reply({error:'Unavailable'},404);
 }catch(e){return reply({error:e.status===409?'Attempt conflict; local data retained':'Request failed'},e.status??401);}
}

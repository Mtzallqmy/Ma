import postgres from 'postgres';
import {learnerService} from './learner-service.js';
import {academyAdmin} from './academy-service.js';
import {panelAsset} from './panel-assets.js';
export {academyAdmin,validateLesson} from './academy-service.js';
let sql;
export default {async fetch(request) {
 const asset=panelAsset(request);if(asset)return asset;
 sql ??= postgres(process.env.DAWA_DATABASE_URL ?? process.env.DATABASE_URL,{ssl:'require',max:3,idle_timeout:20,connect_timeout:15});
 const learner=await learnerService(request,sql);if(learner)return learner;
 return academyAdmin(request,(text,args=[])=>sql.unsafe(text,args));
}};

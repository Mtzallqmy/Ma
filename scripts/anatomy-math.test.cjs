const test=require('node:test');const assert=require('node:assert/strict');
const vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const sandbox={window:{}};vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../app/src/main/assets/viewer3d/anatomy_math.js'),'utf8'),sandbox);
const {pick,makeRay}=sandbox.window.AnatomyMath;
const triangle=(name,z)=>({name,positions:new Float32Array([-1,-1,z,1,-1,z,0,1,z]),indices:new Uint16Array([0,1,2])});
test('exact triangle picking selects nearest surface and respects hidden/isolation',()=>{
 const parts=[triangle('far',-2),triangle('near',0)],ray={origin:[0,0,3],direction:[0,0,-1]};
 assert.equal(pick(parts,ray,{},null),'near');assert.equal(pick(parts,ray,{near:true},null),'far');
 assert.equal(pick(parts,ray,{},'far'),'far');assert.equal(pick(parts,{origin:[2,2,3],direction:[0,0,-1]}, {},null),null);
});
test('camera ray uses aspect ratio and inverse view translation',()=>{
 const view=[1,0,0,0,0,1,0,0,0,0,1,0,0,0,-3,1],ray=makeRay(0,0,2,view);
 assert.equal(ray.origin.join(','),'0,0,3');assert.equal(ray.direction.join(','),'0,0,-1');
 assert.equal(pick([triangle('surface',0)],ray,{},null),'surface');
 assert.ok(makeRay(1,0,2,view).direction[0]>makeRay(1,0,1,view).direction[0]);
});

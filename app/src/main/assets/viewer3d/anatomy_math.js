/* Geometry-only picking. No selection is inferred from a nearby label or centre. */
(function(root) {
  'use strict';
  const sub=(a,b)=>a.map((x,i)=>x-b[i]);
  const dot=(a,b)=>a.reduce((s,x,i)=>s+x*b[i],0);
  const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
  function makeRay(x,y,aspect,view) {
    const t=Math.tan(0.78/2);
    const transpose=v=>[view[0]*v[0]+view[1]*v[1]+view[2]*v[2],view[4]*v[0]+view[5]*v[1]+view[6]*v[2],view[8]*v[0]+view[9]*v[1]+view[10]*v[2]];
    const origin=transpose([-view[12],-view[13],-view[14]]);
    const d=transpose([x*t*aspect,y*t,-1]);const length=Math.hypot(...d);
    return {origin,direction:d.map(v=>v/length)};
  }
  function triangleDistance(origin,direction,a,b,c) {
    const e1=sub(b,a),e2=sub(c,a),p=cross(direction,e2),det=dot(e1,p);
    if(Math.abs(det)<1e-9)return Infinity;
    const inv=1/det,t=sub(origin,a),u=dot(t,p)*inv;
    if(u<0||u>1)return Infinity;
    const q=cross(t,e1),v=dot(direction,q)*inv;
    if(v<0||u+v>1)return Infinity;
    const distance=dot(e2,q)*inv;
    return distance>1e-6?distance:Infinity;
  }
  function pick(parts,ray,hidden,isolate) {
    let best=null,nearest=Infinity;
    for(const part of parts) {
      if(hidden[part.name]||(isolate&&part.name!==isolate))continue;
      const point=i=>Array.from(part.positions.slice(i*3,i*3+3));
      for(let i=0;i<part.indices.length;i+=3) {
        const distance=triangleDistance(ray.origin,ray.direction,point(part.indices[i]),point(part.indices[i+1]),point(part.indices[i+2]));
        if(distance<nearest){nearest=distance;best=part.name;}
      }
    }
    return best;
  }
  const api={makeRay,triangleDistance,pick};
  if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.AnatomyMath=api;
})(typeof window!=='undefined'?window:globalThis);

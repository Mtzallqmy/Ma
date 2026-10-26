#!/usr/bin/env python3
"""Reproducibly convert five licensed BodyParts3D STL parts into one optional GLB.
Geometry is preserved: shared coordinate transform, uniform scale, vertex welding,
vertex normals and study colours only. Not a complete heart or clinical validation.
"""
import hashlib, json, math, struct
from pathlib import Path
from urllib.request import Request, urlopen

REV = 'f0eeb6e843380cfe6b83797cf8c3e1af74de5e61'
BASE = f'https://raw.githubusercontent.com/Kevin-Mattheus-Moerman/BodyParts3D/{REV}/'
PARTS = [
 ('FMA3736','Aorta_ascendens','الأبهر الصاعد','Ascending aorta',[0.78,0.22,0.24,1]),
 ('FMA3768','Arcus_aortae','قوس الأبهر','Aortic arch',[0.84,0.32,0.28,1]),
 ('FMA7234','Valva_tricuspidalis','الصمام ثلاثي الشرفات','Tricuspid valve',[0.25,0.62,0.73,1]),
 ('FMA7235','Valva_mitralis','الصمام التاجي','Mitral valve',[0.8,0.67,0.34,1]),
 ('FMA7246','Valva_trunci_pulmonalis','الصمام الرئوي','Pulmonary valve',[0.5,0.4,0.73,1]),
]
def fetch(path):
 req=Request(BASE+path,headers={'User-Agent':'DawaHayah educational anatomy import'})
 with urlopen(req,timeout=60) as response: data=response.read(8*1024*1024+1)
 assert len(data)<=8*1024*1024
 return data
license_text=fetch('LICENSE_content').decode()
assert '2.1 Japan' in license_text
meshes=[]
for fma,node,ar,en,color in PARTS:
 path=f'assets/BodyParts3D_data/stl/{fma}.stl';raw=fetch(path)
 count=struct.unpack_from('<I',raw,80)[0];assert len(raw)==84+50*count and 0<count<100000
 vertices=[];lookup={};faces=[]
 for i in range(count):
  values=struct.unpack_from('<12fH',raw,84+50*i);face=[]
  for j in (3,6,9):
   x,y,z=values[j:j+3];v=(x,z,-y);assert all(math.isfinite(c) for c in v)
   if v not in lookup:lookup[v]=len(vertices);vertices.append(v)
   face.append(lookup[v])
  faces.append(face)
 assert len(vertices)<65536
 meshes.append(dict(fma=fma,node=node,nameAr=ar,nameEn=en,color=color,vertices=vertices,faces=faces,sourceUrl=BASE+path,sourceSha256=hashlib.sha256(raw).hexdigest()))
lo=[min(v[k] for m in meshes for v in m['vertices']) for k in range(3)]
hi=[max(v[k] for m in meshes for v in m['vertices']) for k in range(3)]
scale=1.8/max(hi[k]-lo[k] for k in range(3));center=[(lo[k]+hi[k])/2 for k in range(3)]
gltf=dict(asset={'version':'2.0','generator':'DawaHayah licensed STL conversion'},scene=0,scenes=[{'nodes':list(range(len(meshes)))}],nodes=[],meshes=[],materials=[],accessors=[],bufferViews=[],buffers=[])
binary=bytearray()
def accessor(data,kind,component,count,minmax=None):
 while len(binary)%4:binary.append(0)
 offset=len(binary);binary.extend(data)
 view=len(gltf['bufferViews']);gltf['bufferViews'].append({'buffer':0,'byteOffset':offset,'byteLength':len(data)})
 index=len(gltf['accessors']);item={'bufferView':view,'componentType':component,'count':count,'type':kind}
 if minmax:item.update(min=minmax[0],max=minmax[1])
 gltf['accessors'].append(item);return index
for n,m in enumerate(meshes):
 positions=[tuple((v[k]-center[k])*scale for k in range(3)) for v in m['vertices']]
 normals=[[0.,0.,0.] for _ in positions]
 for a,b,c in m['faces']:
  u=[positions[b][k]-positions[a][k] for k in range(3)];v=[positions[c][k]-positions[a][k] for k in range(3)]
  normal=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
  for i in (a,b,c):
   for k in range(3):normals[i][k]+=normal[k]
 normals=[tuple(x/(math.sqrt(sum(y*y for y in v)) or 1.) for x in v) for v in normals]
 p=accessor(b''.join(struct.pack('<3f',*v) for v in positions),'VEC3',5126,len(positions),([min(v[k] for v in positions) for k in range(3)],[max(v[k] for v in positions) for k in range(3)]))
 norm=accessor(b''.join(struct.pack('<3f',*v) for v in normals),'VEC3',5126,len(normals))
 indices=accessor(b''.join(struct.pack('<3H',*v) for v in m['faces']),'SCALAR',5123,len(m['faces'])*3)
 gltf['nodes'].append({'mesh':n,'name':m['node']})
 gltf['meshes'].append({'name':m['node'],'primitives':[{'attributes':{'POSITION':p,'NORMAL':norm},'indices':indices,'material':n}]})
 gltf['materials'].append({'pbrMetallicRoughness':{'baseColorFactor':m['color'],'metallicFactor':0.,'roughnessFactor':0.8},'doubleSided':True})
while len(binary)%4:binary.append(0)
gltf['buffers']=[{'byteLength':len(binary)}]
j=json.dumps(gltf,separators=(',',':')).encode()
while len(j)%4:j+=b' '
glb=struct.pack('<III',0x46546C67,2,12+8+len(j)+8+len(binary))+struct.pack('<II',len(j),0x4E4F534A)+j+struct.pack('<II',len(binary),0x004E4942)+binary
assert len(glb)<8*1024*1024
public=Path('public/anatomy');public.mkdir(parents=True,exist_ok=True)
(public/'bodyparts-cardiac-v1.glb').write_bytes(glb)
(public/'BODYParts3D-LICENSE.txt').write_text(license_text+'\nDerivative GLB: CC BY-SA 2.1 Japan. Coordinates rotated, uniformly scaled and centered, vertices welded, normals and study colours added. Geometry not simplified.\n')
metadata={'packId':'bodyparts-cardiac-v1','assetId':'asset-bodyparts-cardiac-v1','sizeBytes':len(glb),'sha256':hashlib.sha256(glb).hexdigest(),'license':'CC BY-SA 2.1 Japan','licenseUrl':'https://creativecommons.org/licenses/by-sa/2.1/jp/deed.en','attribution':'BodyParts3D, (c) The Database Center for Life Science licensed under CC Attribution-Share Alike 2.1 Japan; STL mirror conversion by Kevin Mattheus Moerman; GLB conversion by Dawa & Hayah.','sourceRevision':REV,'sourceUrl':'https://github.com/Kevin-Mattheus-Moerman/BodyParts3D/tree/'+REV,'changes':'Shared rotation, centering and uniform scale, vertex welding, normals and study colours. No geometry simplification. Five structures, not a complete heart.','parts':[{k:m[k] for k in ('fma','node','nameAr','nameEn','sourceUrl','sourceSha256')}|{'triangles':len(m['faces']),'vertices':len(m['vertices'])} for m in meshes]}
assets=Path('app/src/main/assets/anatomy');assets.mkdir(parents=True,exist_ok=True)
(assets/'bodyparts-cardiac-source.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(metadata,ensure_ascii=False))

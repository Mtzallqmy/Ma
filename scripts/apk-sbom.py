"""SPDX file inventory of the built APK. Dependency report is shipped separately."""
import hashlib,json,sys,uuid,zipfile,datetime
from pathlib import Path
p=Path(sys.argv[1]);files=[]
with zipfile.ZipFile(p) as z:
 for i,n in enumerate(sorted(z.namelist())):
  if n.endswith('/'):continue
  files.append({'SPDXID':f'SPDXRef-File-{i}','fileName':n,'checksums':[{'algorithm':'SHA256','checksumValue':hashlib.sha256(z.read(n)).hexdigest()}],'licenseConcluded':'NOASSERTION','copyrightText':'NOASSERTION'})
print(json.dumps({'spdxVersion':'SPDX-2.3','dataLicense':'CC0-1.0','SPDXID':'SPDXRef-DOCUMENT','name':p.name+' file inventory','documentNamespace':'https://github.com/Mtzallqmy/Ma/spdx/'+str(uuid.uuid4()),'creationInfo':{'created':datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ'),'creators':['Tool: Dawa-Hayah-APK-inventory']},'files':files,'documentComment':'APK file inventory only; this is not a completed dependency license audit. See DEPENDENCIES.txt.'},indent=2))

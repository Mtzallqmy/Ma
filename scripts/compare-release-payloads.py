"""Prove emulator verification uses the delivered APK's optimized code and resources."""
import hashlib,json,sys,zipfile

def payload(path):
    with zipfile.ZipFile(path) as apk:
        return {name:hashlib.sha256(apk.read(name)).hexdigest() for name in apk.namelist()
                if not name.endswith('/') and not name.startswith(('lib/','META-INF/'))}
arm,emulator=map(payload,sys.argv[1:3])
assert any(name.endswith('.dex') for name in arm), 'Missing optimized bytecode'
assert arm == emulator, 'Release counterpart differs outside native libraries/signatures'
print(json.dumps({'sameOptimizedCodeAndResources':True,'comparedFiles':len(arm),'files':arm},indent=2))

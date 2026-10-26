"""Fail if a distribution APK has the wrong identity, ABI, signing, or native alignment."""
import os,pathlib,struct,subprocess,sys,zipfile

apk=pathlib.Path(sys.argv[1]);version=sys.argv[2]
assert apk.is_file() and apk.stat().st_size>100000
with zipfile.ZipFile(apk) as archive:
    libs=[n for n in archive.namelist() if n.startswith('lib/') and n.endswith('.so')]
    assert libs and all(n.startswith('lib/arm64-v8a/') for n in libs), 'Wrong native ABI'
    for name in libs:
        elf=archive.read(name)
        assert elf[:6]==b'\x7fELF\x02\x01', 'Expected little-endian ELF64'
        assert struct.unpack_from('<H',elf,18)[0]==183, 'Expected AArch64 machine code'
        offset=struct.unpack_from('<Q',elf,32)[0]
        size,count=struct.unpack_from('<HH',elf,54)
        loads=[]
        for index in range(count):
            segment=struct.unpack_from('<IIQQQQQQ',elf,offset+index*size)
            if segment[0]==1:loads.append(segment)
        assert loads and all(s[7]>=16384 and (s[2]-s[3])%16384==0 for s in loads), 'ELF lacks 16 KB alignment: '+name
    assert 'AndroidManifest.xml' in archive.namelist()
tools=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/35.0.0'
subprocess.run([str(tools/'apksigner'),'verify',str(apk)],check=True)
subprocess.run([str(tools/'zipalign'),'-c','-P','16','4',str(apk)],check=True)
meta=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(apk)],text=True)
assert "name='com.dawahayah.app'" in meta and f"versionName='{version}'" in meta and "sdkVersion:'26'" in meta
assert 'application-debuggable' not in meta, 'Release cannot be debuggable'
print('Verified package, version, API26, ARM64 ELF, 16 KB alignment, non-debuggable flag and signature')

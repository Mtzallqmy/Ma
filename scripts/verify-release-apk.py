import pathlib,subprocess,sys,zipfile,os
apk=pathlib.Path(sys.argv[1]);version=sys.argv[2]
assert apk.is_file() and apk.stat().st_size>100000
with zipfile.ZipFile(apk) as z:
 libs=[n for n in z.namelist() if n.startswith('lib/') and n.endswith('.so')]
 assert libs and all(n.startswith('lib/arm64-v8a/') for n in libs), 'Wrong native ABI'
 assert 'AndroidManifest.xml' in z.namelist()
tools=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/35.0.0'
subprocess.run([str(tools/'apksigner'),'verify',str(apk)],check=True)
meta=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(apk)],text=True)
assert "name='com.dawahayah.app'" in meta and f"versionName='{version}'" in meta and "sdkVersion:'26'" in meta
print('Verified APK version, package, minSdk, ARM64 ABI and signature')

"""Black-box checks of the signed, non-debuggable R8 release; no test hooks or DB injection."""
import json,re,subprocess,sys,time,xml.etree.ElementTree as ET
from pathlib import Path

OUT=Path('release-verification');OUT.mkdir(exist_ok=True)
PACKAGE='com.dawahayah.app'

def adb(*args,binary=False):
    return subprocess.check_output(['adb',*args],timeout=45,text=not binary)

def tree():
    adb('shell','uiautomator','dump','/sdcard/release-window.xml')
    xml=adb('shell','cat','/sdcard/release-window.xml')
    (OUT/'last-window.xml').write_text(xml)
    return ET.fromstring(xml)

def find(root,key,value):
    return next((n for n in root.iter('node') if n.get(key)==value),None)

def wait(key,value,seconds=30):
    until=time.monotonic()+seconds
    while time.monotonic()<until:
        node=find(tree(),key,value)
        if node is not None:return node
        time.sleep(.3)
    raise AssertionError(f'Missing {key}: {value}')

def tap(node):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
    assert x2>x1 and y2>y1
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))

def scroll_to(key,value):
    for _ in range(30):
        root=tree();node=find(root,key,value)
        if node is not None:return node
        size=adb('shell','wm','size');w,h=map(int,re.findall(r'(\d+)x(\d+)',size)[-1])
        adb('shell','input','swipe',str(w//2),str(int(h*.75)),str(w//2),str(int(h*.3)),'350')
    raise AssertionError(f'Not reachable: {value}')

def shot(name):
    (OUT/f'{name}.png').write_bytes(adb('exec-out','screencap','-p',binary=True))

def launch():
    adb('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')
    wait('resource-id','home_feed')

def search_heart():
    tap(wait('content-desc','البحث العام'))
    tap(wait('resource-id','search_query'))
    adb('shell','input','text','Cor')
    adb('shell','input','keyevent','66')
    tap(scroll_to('resource-id','hit_organ-heart'))
    wait('resource-id','knowledge_reader')

try:
    print(adb('install','-r',sys.argv[1]))
    adb('shell','svc','wifi','disable');adb('shell','svc','data','disable')
    launch()
    wait('text','13 جهازًا')
    shot('release-offline-home')
    search_heart()
    tap(scroll_to('resource-id','reader_note'))
    adb('shell','input','text','Release-note-persistence')
    adb('shell','input','keyevent','4')
    tap(scroll_to('resource-id','save_note'))
    # Leave the editor and terminate the entire process before verifying persistence.
    adb('shell','input','keyevent','4')
    adb('shell','am','force-stop',PACKAGE)
    launch();search_heart()
    note=scroll_to('resource-id','reader_note')
    assert 'Release-note-persistence' in note.get('text',''), 'Note lost after process death'
    shot('release-persisted-note')
    adb('shell','am','force-stop',PACKAGE)
    adb('shell','svc','wifi','enable');adb('shell','svc','data','enable')
    launch()
    tap(scroll_to('text','تحديث المحتوى'))
    until=time.monotonic()+90
    while time.monotonic()<until:
        root=tree()
        if any('آخر تحديث ناجح:' in n.get('text','') for n in root.iter('node')):break
        time.sleep(.5)
    else:raise AssertionError('No successful HTTPS/Room synchronization in optimized release')
    shot('release-neon-sync')
    (OUT/'result.json').write_text(json.dumps({'passed':True,'abi':adb('shell','getprop','ro.product.cpu.abi').strip(),
        'api':adb('shell','getprop','ro.build.version.sdk').strip(),
        'checks':['non-debuggable release launch','offline Room seed','Latin alias search','reader',
                  'note survives process death','live Neon HTTPS synchronization']},indent=2))
    print('Release black-box checks passed')
finally:
    shot('release-final-state')
    (OUT/'logcat.txt').write_text(adb('logcat','-d','-s','AndroidRuntime:E'))
    adb('shell','svc','wifi','enable');adb('shell','svc','data','enable')

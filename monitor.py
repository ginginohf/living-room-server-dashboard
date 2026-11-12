#!/usr/bin/env python3
"""Read-only LAN metrics collector. No shell commands or remote control API."""
import collections, datetime, glob, hmac, http.server, json, os, pathlib, socket, threading, time, urllib.request, xml.etree.ElementTree as ET
BASE = pathlib.Path(__file__).resolve().parent
TOKEN = os.environ.get('DASHBOARD_TOKEN','')
LOCK = threading.Lock()
STATE = {}
RATES = {}
HISTORY = collections.deque(maxlen=120)
MOUNTS = json.loads(os.environ.get('DASHBOARD_MOUNTS','[["/","System"]]'))

def read(path):
    return pathlib.Path(path).read_text().strip()

def counters():
    cpu = list(map(int, read('/proc/stat').splitlines()[0].split()[1:9]))
    net = {}
    for line in read('/proc/net/dev').splitlines()[2:]:
        name, values = line.split(':'); v = values.split(); net[name.strip()] = (int(v[0]), int(v[8]))
    interface = next((l.split()[0] for l in read('/proc/net/route').splitlines()[1:] if l.split()[1] == '00000000'), None)
    disks = {}
    for line in read('/proc/diskstats').splitlines():
        v = line.split(); disks[v[2]] = (int(v[5])*512, int(v[9])*512, int(v[12]))
    return {'time': time.monotonic(), 'total': sum(cpu), 'idle': cpu[3]+cpu[4], 'net': net, 'interface': interface, 'disks': disks}

def delta(a,b,dt): return max(0, a-b)/dt

def sample(old, cur):
    dt=max(.01,cur['time']-old['time']); ticks=cur['total']-old['total']
    cpu= max(0,min(100,100*(1-(cur['idle']-old['idle'])/ticks))) if ticks>0 else 0
    mem={k:int(v.split()[0])*1024 for k,v in (l.split(':',1) for l in read('/proc/meminfo').splitlines())}
    temps=[]
    for p in glob.glob('/sys/class/hwmon/hwmon*'):
        if read(p+'/name')=='coretemp':
            for t in glob.glob(p+'/temp*_input'):
                try: temps.append(int(read(t))/1000)
                except (ValueError,OSError): pass
    iface=cur['interface']; n=cur['net'].get(iface,(0,0)); prev=old['net'].get(iface,n)
    rx,tx=(delta(n[i],prev[i],dt) for i in (0,1))
    disks=[]
    for path,label in MOUNTS:
        mounted=os.path.ismount(path)
        item={'label':label,'mounted':mounted}
        if mounted:
            stat=os.statvfs(path); dev=os.stat(path).st_dev
            node=pathlib.Path('/sys/dev/block/%d:%d'%(os.major(dev),os.minor(dev))).resolve()
            disk=node.parent.name if (node/'partition').exists() else node.name
            d=cur['disks'].get(disk,(0,0,0)); prevd=old['disks'].get(disk,d)
            item.update(total=stat.f_blocks*stat.f_frsize,free=stat.f_bavail*stat.f_frsize,used=(stat.f_blocks-stat.f_bfree)*stat.f_frsize,
                        read=delta(d[0],prevd[0],dt),write=delta(d[1],prevd[1],dt),busy=min(100,delta(d[2],prevd[2],dt)/10))
        disks.append(item)
    return dict(timestamp=time.time(),hostname=socket.gethostname(),cpu=cpu,cores=os.cpu_count(),temperature=max(temps) if temps else None,
                memory_total=mem['MemTotal'],memory_used=mem['MemTotal']-mem['MemAvailable'],
                uptime=float(read('/proc/uptime').split()[0]),load=os.getloadavg()[0],interface=iface,rx=rx,tx=tx,disks=disks)

def rates_loop():
    global RATES
    cache=BASE/'rates-cache.json'
    try: RATES=json.loads(cache.read_text())
    except (ValueError,OSError): pass
    while True:
        try:
            with urllib.request.urlopen('https://www.cbr.ru/scripts/XML_daily.asp',timeout=20) as r: root=ET.fromstring(r.read(200000))
            values={v.findtext('CharCode'):float(v.findtext('Value').replace(',','.'))/int(v.findtext('Nominal')) for v in root.findall('Valute') if v.findtext('CharCode') in ('USD','EUR')}
            if len(values)!=2: raise ValueError('Incomplete CBR response')
            result=dict(values,date=root.attrib['Date'],fetched=time.time())
            cache.with_suffix('.tmp').write_text(json.dumps(result)); cache.with_suffix('.tmp').replace(cache)
            with LOCK: RATES=result
            time.sleep(21600)
        except Exception:
            time.sleep(300)

def sampler():
    global STATE
    old=counters()
    while True:
        time.sleep(5)
        try:
            cur=counters(); data=sample(old,cur); old=cur
            HISTORY.append({'cpu':data['cpu'],'rx':data['rx'],'tx':data['tx']})
            data['history']=list(HISTORY)
            with LOCK: STATE=data
        except Exception as e: print('Sample failed: '+type(e).__name__,flush=True)

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path=='/':
            body=(BASE/'index.html').read_bytes();self.send_response(200);self.send_header('Content-Type','text/html; charset=utf-8');self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body);return
        if self.path!='/metrics' or not hmac.compare_digest(self.headers.get('Authorization',''),'Bearer '+TOKEN):
            self.send_error(404); return
        with LOCK: payload=dict(STATE,rates=dict(RATES))
        body=json.dumps(payload,ensure_ascii=False).encode()
        self.send_response(200 if STATE else 503); self.send_header('Content-Type','application/json; charset=utf-8'); self.send_header('Cache-Control','no-store'); self.send_header('Content-Length',str(len(body))); self.end_headers(); self.wfile.write(body)
    def log_message(self,*args): pass

if __name__=='__main__':
    if len(TOKEN)<24: raise SystemExit('Set DASHBOARD_TOKEN to a fresh random value (24+ characters)')
    if os.environ.get('ENABLE_RATES')=='1':threading.Thread(target=rates_loop,daemon=True).start()
    threading.Thread(target=sampler,daemon=True).start()
    http.server.ThreadingHTTPServer((os.environ.get('DASHBOARD_HOST','127.0.0.1'),int(os.environ.get('DASHBOARD_PORT','18737'))),Handler).serve_forever()

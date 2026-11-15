"""Capture the shipped browser UI using synthetic API responses only.
Install playwright separately; set DEMO_CHROME to an existing Chrome executable,
or run `playwright install chromium`. No real server or account is contacted.
"""
import os, json, mimetypes
from pathlib import Path
from urllib.parse import urlparse, unquote
from playwright.sync_api import sync_playwright
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/images'; OUT.mkdir(parents=True,exist_ok=True)
WEB=ROOT
FIXTURES={'/metrics':{'cpu':23.4,'memory_used':6*1024**3,'memory_total':16*1024**3,'temperature':46,'rx':2.4*1048576,'disks':[{'label':'System (demo)','mounted':True,'free':180*1024**3},{'label':'Media (demo)','mounted':True,'free':640*1024**3}]}}
with sync_playwright() as pw:
 browser=pw.chromium.launch(headless=True,executable_path=os.environ.get('DEMO_CHROME') or None)
 page=browser.new_page(viewport={'width':1440,'height':960},device_scale_factor=1)
 def serve(route):
  u=urlparse(route.request.url)
  if u.hostname!='demo.local':route.abort();return
  path=unquote(u.path)
  if path in FIXTURES:route.fulfill(json=FIXTURES[path]);return
  if path.startswith(('/api/','/auth/')):route.fulfill(json={});return
  file=(WEB/('index.html' if path=='/' else path.lstrip('/'))).resolve()
  if not file.is_relative_to(WEB.resolve()) or not file.is_file():route.fulfill(status=404,body='Not found');return
  route.fulfill(body=file.read_bytes(),content_type=mimetypes.guess_type(str(file))[0] or 'application/octet-stream')
 page.route('**/*',serve)
 page.goto('http://demo.local/',wait_until='networkidle')
 page.locator("#token").fill("synthetic-demo-token-not-a-credential")
 page.locator("button").click()
 page.locator("#cards .card").first.wait_for()
 page.screenshot(path=str(OUT/"overview.png"),full_page=True)
 browser.close()

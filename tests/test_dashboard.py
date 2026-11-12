import unittest,threading,http.client
from unittest.mock import patch
from http.server import ThreadingHTTPServer
import monitor
class Tests(unittest.TestCase):
 def test_auth(self):
  with patch.object(monitor,'TOKEN','synthetic-test-only'),patch.object(monitor,'STATE',{'cpu':0}):
   srv=ThreadingHTTPServer(('127.0.0.1',0),monitor.Handler);t=threading.Thread(target=srv.serve_forever,daemon=True);t.start()
   try:
    c=http.client.HTTPConnection(*srv.server_address);c.request('GET','/metrics');self.assertEqual(c.getresponse().status,404);c.close()
    c=http.client.HTTPConnection(*srv.server_address);c.request('GET','/metrics',headers={'Authorization':'Bearer synthetic-test-only'});self.assertEqual(c.getresponse().status,200);c.close()
   finally:srv.shutdown();srv.server_close()

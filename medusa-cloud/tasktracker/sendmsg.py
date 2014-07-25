import BaseHTTPServer
import cgi
import sys
import httplib
import json
import urllib
import urllib2
import urlparse
# change to your own API_KEY from code.google.com with instruction
print "using GCM..."

api_f = open('../config/GCM.info')
API_KEY = api_f.read()[0:-1]

#reg_id_list = ["APA91bGRSHh0_f2NTGQWTeOqA2iSFXfg_TYZhzvDbnCzlMv58MTn-SLCZoAJF0_Olo6yF93iqrv9GpSBPQusQLV3B5e5n6DIgfwD-3f0ZjtkS7SxyvuTcAt5kQPt_KQJfm7nbAR716XiDXFSGDdN7UdVmzUXowW03Q"]
reg_id_list = [str(sys.argv[1])]
print reg_id_list
msg = sys.argv[2]
data = {
      'registration_ids' : reg_id_list,
      'data' : {
        'msg' : msg
      }
    }

headers = {'Content-Type' : 'application/json', 'Authorization' : 'key=' + API_KEY}
print headers
url = 'https://android.googleapis.com/gcm/send'
print json.dumps(data)
request = urllib2.Request(url, json.dumps(data), headers)
print request
response = urllib2.urlopen(request)
print json.loads(response.read())

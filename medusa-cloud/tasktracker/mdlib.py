#!/usr/bin/env python
#
#
# This file implements uility functions for
# the Medusa Script Interpreter.
#
# @modified: Dec. 9th 2011
# @author: Moo-Ryong Ra(mra@usc.edu)
#
from datetime import datetime
import re, base64
import httplib, urllib

initlib = False
starttime = None
logfile = None
DEBUG = True

DB_USERNAME = ((open('../config/db_account.info').read().rstrip()).split('|'))[0]
DB_PASSWORD = ((open('../config/db_account.info').read().rstrip()).split('|'))[1]

def log_init():
	global initlib, starttime, logfile
	starttime = datetime.now()
	logfile = open("logs/log_{0}.txt".format(datetime.now().strftime("%Y%m%d%H%M%S")), "w+")
	initlib = True

def log(msg):
	global initlib, starttime, logfile

	if initlib == False:
		log_init()

	now = datetime.now()
	if starttime == None:
		starttime = now
	diff = now - starttime

	#print now, diff, msg
	print diff, msg
	logfile.write("{0}, {1}\n".format(diff, msg))
	logfile.flush()

	return

def httpsend(host, urlloc, params, clock = None, method = "POST", headers = None):
	# this is temporary: should be removed when it is used with other codes..

	#print params
	print "***"
	encoded_params = urllib.urlencode(params)
	if DEBUG: print "host=%s, urlloc=%s, encoded_params=%s" %(host, urlloc, encoded_params)
	conn = httplib.HTTPConnection(host)

	if method == "POST" and headers == None:
		headers = {"Content-type": "application/x-www-form-urlencoded",
				   "Accept": "text/plain"}
		conn.request(method, urlloc, encoded_params, headers)
	else:
		conn.request(method, urlloc, encoded_params)
	
	if clock != None:
		log("* [overhead] http request latency: {0}".format(clock.stop()))

	resp = conn.getresponse()
	data = resp.read()
	conn.close()

	if clock != None:
		log("* [overhead] http response latency: {0}".format(clock.stop()))

	return resp.status, resp.reason


def get_single_mysql_data(host, dbname, sqlstmt):
	import MySQLdb as mdb

	con = None
	data = None
	
	try:
		con = mdb.connect(host, DB_USERNAME, DB_PASSWORD, dbname)
		cur = con.cursor()
		cur.execute(sqlstmt)

		data = cur.fetchone()
		#print data

	except mdb.Error, e:
		log("! get_mysql_data() err %d: %s" % (e.args[0], e.args[1]))

	finally:
		if con:
			con.close()

	return data

def get_raw_mysql_data(host, dbname, sqlstmt):
	import MySQLdb as mdb

	con = None
	data = None
		
	try:
		con = mdb.connect(host, DB_USERNAME, DB_PASSWORD, dbname)
		cur = con.cursor()
		cur.execute(sqlstmt)

	except mdb.Error, e:
		log("! get_raw_mysql_data() err %d: %s" % (e.args[0], e.args[1]))
	return con, cur


def send_email(subject, msgtext, recv):
	# from python tutorial 
	#	- http://docs.python.org/library/email-examples.html
	import smtplib
	from email.mime.text import MIMEText

	# set mail content
	msg = MIMEText(msgtext)
	me = 'medusa_mailman@usc.edu'
	
	msg['Subject'] = subject
	msg['From'] = me
	msg['To'] = recv

	s = smtplib.SMTP('localhost')
	s.sendmail(me, recv, msg.as_string())
	s.quit()


def convert_muid_to_url(keys_str, vals_str, db_host, db_name, base_url):

	keys = keys_str.split("|")
	vals = vals_str.split("|")
	
	retstr = None
	for i in range(len(keys)):
		for j in range(len(vals)):
			sqlstmt = "select path from RS_data where uid='{0}' and muid='{1}'".format(keys[i], vals[j])
			data = get_single_mysql_data(db_host, db_name, sqlstmt)
			if data != None:
				if retstr == None:
					retstr = "{0}{1}".format(base_url, data[0])
				else:
					retstr = "{0}|{1}{2}".format(retstr, base_url, data[0])
	
	return retstr

def update_program_state(host, dbname, pid, action, msg):
	sqlstmt = "insert into CS_event(pid, action, msg, userid, qtype, qid) values('{0}','{1}','{2}','-','-','-')" \
						.format(pid, action, msg)
	dat = get_single_mysql_data(host, dbname, sqlstmt)

def log_appname_pid_mapping(host, dbname, appname, pid):
	sqlstmt = "insert into CS_app2pid(appname, pid) values('{0}','{1}')" \
						.format(appname, pid)
	dat = get_single_mysql_data(host, dbname, sqlstmt)

def get_xmltag(tagname, document, orig):
	pr = document.find(tagname)
	if pr != None:
		return pr.text
	else:
		return orig


def encode_tag_content(tagstr, content):
	prs = re.findall("<{tag}>.*</{tag}>".format(tag=tagstr), content)
	if prs != []:
		prs = prs[0]
		prs = prs.replace("<{tag}>".format(tag=tagstr), "")
		prs = prs.replace("</{tag}>".format(tag=tagstr), "")
		encoded = base64.b64encode(prs)
		revised = re.sub("<{tag}>.*</{tag}>".format(tag=tagstr), "<{tag}>{enc}</{tag}>".format(tag=tagstr,enc=encoded), content)
	else:
		revised = content
	
	return revised

def encode_text(text):
	return base64.b64encode(text)
	
def apply_filter(uidlist, filter):

	if filter == None:
		return uidlist

	filter_list = filter.split("|")
	uid_list = uidlist.split("|")
	if len(filter_list) != len(uid_list):
		return None
	
	outlist = None
	for i in range(len(filter_list)):
		dat = uid_list[i] if filter_list[i] == '1' else None
		if dat == None:
			pass
		else:
			outlist = dat if outlist == None else "{0}|{1}".format(outlist, dat)

	return outlist

def get_dbhost():
	fp = open("../config/db_host.info", "r")
	dbhost = fp.readline()
	fp.close()

	return dbhost.strip()

def get_env_vars(dbhost, dbname, keys):
	keys = keys.split(",")
	sqlstmt = "select medvalue from CS_env where"
	for i in range(len(keys)):
		if i == 0:
			sqlstmt = "{0} medkey='{1}'".format(sqlstmt, keys[i])
		else:
			sqlstmt = "{0} or medkey='{1}'".format(sqlstmt, keys[i])

	rdict = dict()
	con, cur = get_raw_mysql_data(dbhost, dbname, sqlstmt)
	dat = cur.fetchone()
	i = 0
	while dat != None:
		rdict[keys[i]] = dat[0]
		i = i + 1
		dat = cur.fetchone()
	con.close()
	return rdict

#
def msg_send(conn, msg):
	conn.send(msg + '\n')

def msg_recv(conn, size):
	data = ''
	while data.find('\n') == -1:
		data += conn.recv(size)
	return data[:-1]



#!/usr/bin/env python
#
#
# This file implements Medusa Script Interpreter.
#
# @modified: April. 9th 2012
# @author: Moo-Ryong Ra(mra@usc.edu)
#
import sys, os, time, random, re, copy, ast
import mdlib, mdchecker
from xml.etree import ElementTree as ET
from mdtype import TFStage, TFConn, StopWatch
import socket
import threading
import urllib2, urllib

# Global Variables
debug = True
anonymity = True

default_timeout = "10 hour"
default_datalimit = None
default_cmdpush = 'c2dm'

data_base_url = None
spc_host = spc_urlloc = hit_host = hit_urlloc = None
wait4res_duration = poll4res_interval = None
failover_interval = failover_retrylimit = None
	
# Environment Variables.
ENV = dict()
ENV["R_RID"] = None		# should be provided by xml program.
ENV["R_RKEY"] = None	# should be provided by xml program.e
ENV["R_WID"] = None
ENV["R_C2DM_KEY"] = None
ENV["W_RID"] = None
ENV["W_RKEY"] = None
ENV["W_WID"] = "all"	# this will become mobile phone user's AMT Worker ID at recruit stage.
ENV["W_C2DM_KEY"] = None
ENV["APP_NAME"] = None
ENV["G_VAR"] = None

# Server configurations.
cs_dbhost = mdlib.get_dbhost()
cs_dbname = "medusa"

# for profiling
clock = StopWatch()

# socket for the async event processing.
asyncsoc = None


# end by YJ
# 
# function display_usage()
#		: display help message.
#
def display_usage():
	print """\
usage: 
	mdserv.py [OPTIONS] [path-to-the-xml-program]
options:	
	-h display usage
		"""
	exit()

#
# function parse_args()
#		: parse input parameters.
#
def parse_args():
	# input parameter parsing.
	for i in range(len(sys.argv)):
		if i == 0: continue
		if sys.argv[i] == "-h":
			display_usage()
		elif sys.argv[i][0] != '-':
			filename = sys.argv[i]
			break
	
	if filename and os.path.isfile(filename):
		mdlib.log('* verified input file: [' + filename + ']')
	else:
		mdlib.log('! ' + fname + 'is not a file')
		exit()

	return filename
	
#
# function parse_xml()
#		: parse xml input file.
#
def parse_xml(fname):
	global default_timeout, default_datalimit, default_cmdpush
	global ENV

	curstage = None
	in_stages = dict()
	in_conns = dict()

	doc = ET.parse(os.path.abspath(fname))
	ENV["APP_NAME"] = doc.find("app/name").text
	
	mdlib.log("* APP [" + ENV["APP_NAME"] + "] started")

	#
	# set user's amt credential
	#
	ENV["R_RID"] = mdlib.get_xmltag("app/rrid", doc, ENV["R_RID"])
	ENV["R_RKEY"] = mdlib.get_xmltag("app/rrkey", doc, ENV["R_RKEY"])
	ENV["R_WID"] = mdlib.get_xmltag("app/rwid", doc, ENV["R_WID"])
	ENV["R_C2DM_KEY"] = mdlib.get_xmltag("app/rc2dmkey", doc, ENV["R_C2DM_KEY"])
	ENV["W_RID"] = mdlib.get_xmltag("app/wrid", doc, ENV["W_RID"])
	ENV["W_RKEY"] = mdlib.get_xmltag("app/wrkey", doc, ENV["W_RKEY"])
	ENV["W_WID"] = mdlib.get_xmltag("app/wwid", doc, ENV["W_WID"])
	ENV["W_C2DM_KEY"] = mdlib.get_xmltag("app/wc2dmkey", doc, ENV["W_C2DM_KEY"])
	ENV["G_VAR"] = mdlib.get_xmltag("app/gvar", doc, ENV["G_VAR"])
	default_timeout = mdlib.get_xmltag("app/timeout", doc, 5)
	default_datalimit = mdlib.get_xmltag("app/dlimit", doc, None)
	default_cmdpush = mdlib.get_xmltag("app/cmdpush", doc, default_cmdpush)

	stages_str = doc.findall("app/stage")
	for s in stages_str:
		stage = TFStage()
		# tags
		stage.xmlstr = re.sub('\s*$', '', re.sub('>\s*<', '><', re.sub('#.*\n', '', ET.tostring(s))))
		stage.name = mdlib.get_xmltag("name", s, stage.name)
		stage.type = mdlib.get_xmltag("type", s, stage.type)
		stage.inst = mdlib.get_xmltag("inst", s, stage.inst)
		stage.listener = mdlib.get_xmltag("listener", s, stage.listener)
		stage.stype = mdlib.get_xmltag("binary", s, stage.stype)
		stage.timeout = mdlib.get_xmltag("timeout", s, stage.timeout)
		stage.device = mdlib.get_xmltag("device", s, stage.device)
		stage.trigger = mdlib.get_xmltag("trigger", s, stage.trigger)
		stage.notification = mdlib.get_xmltag('notification', s, None)
		stage.review = mdlib.get_xmltag("review", s, stage.review)
		stage.config_params = mdlib.get_xmltag("config/params", s, stage.config_params)
		stage.config_input = mdlib.get_xmltag("config/input", s, stage.config_input)
		stage.config_output = mdlib.get_xmltag("config/output", s, stage.config_output)
		# AMT-related tags
		stage.rid = mdlib.get_xmltag("rid", s, stage.rid)
		stage.rkey = mdlib.get_xmltag("rkey", s, stage.rkey)
		stage.wid = mdlib.get_xmltag("wid", s, stage.wid)
		stage.config_stmt = mdlib.get_xmltag("config/stmt", s, stage.config_stmt)
		stage.config_expiration = mdlib.get_xmltag("config/expiration", s, stage.config_expiration)
		stage.config_reward = mdlib.get_xmltag("config/reward", s, stage.config_reward)
		stage.config_numusers = mdlib.get_xmltag("config/numusers", s, stage.config_numusers)
	
		if curstage == None:
			curstage = stage
	
		if stage.name in in_stages:
			mdlib.log("! duplicated stage name")
		else:
			in_stages[stage.name] = stage
			del stage

	connectors_str = doc.findall("app/connector")
	for c in connectors_str:
		conlist = []
		src = c.find("src").text
		dsts_str = c.findall("dst")
		for d in dsts_str:
			con = TFConn() 
			con.src = src
			con.dst_success = d.find("success").text
			con.dst_failure = d.find("failure").text
			conlist.append(con)
	
		in_conns[con.src] = conlist;
		del con

	return in_stages, in_conns, curstage

def get_env_values(keys):
	global cs_dbhost, cs_dbname

	keys = keys.split(",")
	sqlstmt = "select medvalue from CS_env where"
	for i in range(len(keys)):
		if i == 0:
			sqlstmt = "{0} medkey='{1}'".format(sqlstmt, keys[i])
		else:
			sqlstmt = "{0} or medkey='{1}'".format(sqlstmt, keys[i])

	rdict = dict()
	con, cur = mdlib.get_raw_mysql_data(cs_dbhost, cs_dbname, sqlstmt)
	dat = cur.fetchone()
	i = 0
	while dat != None:
		rdict[keys[i]] = dat[0]
		i = i + 1
		dat = cur.fetchone()
	con.close()
	return rdict


def update_amt_cred(pid):

	global cs_dbhost, cs_dbname
	global ENV

	sqlstmt = "select envkey,envval from CS_globalvars where pid={0}".format(pid)
	con, cur = mdlib.get_raw_mysql_data(cs_dbhost, cs_dbname, sqlstmt)
	dat = cur.fetchone()
	while dat != None:
		ENV[dat[0]] = dat[1]
		#mdlib.log("* amtreq: key={0}, val={1}".format(dat[0], dat[1]))
		dat = cur.fetchone()
	con.close()
	
	mdlib.log("* AMT credential has been updated.")
	

#
# function mst_request_params()
#		: generate request parameter set for MST
#
def mst_request_params(puid, quid, cstage, userid, pdict):

	# to avoid AT&T SMS message corruption (it works!)
	xml_cmd_msg = cstage.xmlstr
	xml_cmd_msg = mdlib.encode_tag_content("params", xml_cmd_msg)

	if cstage.config_input != None:
		inputs = cstage.config_input.split(',')
		if len(inputs) > 0:
			# here, we should check input..
			filter = None
			if inputs[0].startswith('$') == True:
				filter = pdict[inputs[0]]
				
			for i in range(len(inputs)):
				if filter != None and i == 0:
					xml_cmd_msg = xml_cmd_msg.replace("{0},".format(inputs[i]), "")
				else:
					data = mdlib.apply_filter(pdict[inputs[i]], filter)
					if data != None and len(data) > 0:
						xml_cmd_msg = xml_cmd_msg.replace(inputs[i], "{0}={1}".format(inputs[i], mdlib.encode_text(data)))

	params = {'pid' : puid, 'qid' : quid, 'action' : 'regRS', 'qtype' : cstage.stype
			, 'custom' : xml_cmd_msg, 'amtid' : userid, 'cmdpush' : default_cmdpush
			,	'dlimit' : default_datalimit if default_datalimit != None else ""
			, 'rid' : ENV["R_RID"], 'wid' : ENV["W_WID"], 'rkey' : ENV["R_RKEY"] }

	return params
		

def announce_hittask_links(pid, qid, listeners):
	# get url.
	sqlstmt = "select msg from CS_event where action='gotoHIT' and pid={0} and qid={1}".format(pid, qid)
	content = mdlib.get_single_mysql_data(cs_dbhost, cs_dbname, sqlstmt)
	
	if content != None:
		# send it to the listeners
		emails = listeners.split(',')
		for i in range(len(emails)):
			mdlib.send_email("Medusa HIT task has been posted", content[0], emails[i])
	
		mdlib.log("* Weblink for HIT task has been announced: {0}".format(listeners))

	else:
		mdlib.log("! No registered link, please verify Worker Manager is running properly")

	
#
# function wait_for_hit_response()
#		: send requests to HIT server, wait for the response
#
def wait_for_hit_response(puid, quid, cstage, request_params):

	global failover_retrylimit, failover_interval, wait4res_duration, poll4res_interval
	global hit_host, hit_urlloc, cs_dbhost, cs_dbname, clock

	execres = False
	scnt = cnt = 0 
	msgstr = ""

	while cnt < failover_retrylimit and execres == False:
		# send a request to HITS
		resp = mdlib.httpsend(hit_host, hit_urlloc, request_params, clock)
		mdlib.log("* HIT request has been registered")

		# response from HITS 
		if resp[0] == 200:
			# announce the hit task link to the listeners
			if cstage.listener != None:
				announce_hittask_links(puid, quid, cstage.listener)

			# check CS_event table to get the results from AMT.
			sqlstmt = "select msg from CS_event where action='regHIT' and pid={0} and qid={1}".format(puid, quid)

			while scnt < (wait4res_duration/poll4res_interval):
				hitdata = mdlib.get_single_mysql_data(cs_dbhost, cs_dbname, sqlstmt)
				if hitdata != None:
					#print data
					mdlib.log("* HIT results have been received msg={0}".format(hitdata[0]))
					msgstr = hitdata[0]
					execres = True
					break
				else:
					if scnt == 0:
						mdlib.log("* waiting for HIT result, scnt={0}/{1}".format(scnt, (wait4res_duration/poll4res_interval)))
					scnt = scnt + 1
					time.sleep(poll4res_interval)   # every 5 sec.

		elif resp[0] == 404:
			mdlib.log('! will not retry since there is no service page (errcode=404)')
			cnt = failover_retrylimit   # no more retry
		else:
			mdlib.log('! HITS returned error: code={0} msg={1}'.format(resp[0], resp[1]))

		# prepare next iteration
		cnt = cnt + 1
		if execres == False:
			time.sleep(failover_interval)
			scnt = 0
		del resp

	return msgstr, execres
	
#
# function wait_for_mst_response()
#		: wait for the response from mobile sensing server
#
# by YJ
def pullout_db(puid, quid):
	global failover_retrylimit, failover_interval, wait4res_duration, poll4res_interval
	global spc_host, spc_urlloc, cs_dbhost, cs_dbname, clock

	execres = False
	scnt = cnt = 0 
	msgstr = None
	
	sqlstmt = "select msg from CS_event where action='completeTask' and pid={0} and qid={1} and userid='{2}'"	\
						.format(puid, quid, ENV["W_WID"])
	retdat = mdlib.get_single_mysql_data(cs_dbhost, cs_dbname, sqlstmt)

	if retdat != None:
		mdlib.log("* RS results user={0}, msg={1}".format(ENV["W_WID"], retdat[0]))
		msgstr = retdat[0]
		execres = True
				
	return msgstr, execres

### end YJ
def wait_for_mst_response(puid, quid, rparams_dict, cstage_config_output):

	global failover_retrylimit, failover_interval, wait4res_duration, poll4res_interval
	global spc_host, spc_urlloc, cs_dbhost, cs_dbname, clock
	execres = False
	scnt = cnt = 0 
	msgstr = None
	
	while cnt < failover_retrylimit and execres == False:

		scnt = 0

		while scnt < (wait4res_duration/poll4res_interval):
			# check CS_event table to get the results from Aqua Clients.
			sqlstmt = "select msg from CS_event where action='completeTask' and pid={0} and qid={1} and userid='{2}'"	\
						.format(puid, quid, ENV["W_WID"])

			retdat = mdlib.get_single_mysql_data(cs_dbhost, cs_dbname, sqlstmt)

			if retdat != None:
				mdlib.log("* RS results user={0}, msg={1}".format(ENV["W_WID"], retdat[0]))
				msgstr = retdat[0]
				execres = True
				break
			else:
				if scnt == 0:
					mdlib.log("* waiting: cnt={0}/{1},{2}th try".format(scnt, (wait4res_duration/poll4res_interval), cnt))
				scnt = scnt + 1
				time.sleep(poll4res_interval)

		#
		cnt = cnt + 1
		if execres == False:
			time.sleep(failover_interval)
			# retry
			scnt = 0
			retrycnt = 0
			ruser = ENV["W_WID"]
			resp = mdlib.httpsend(spc_host, spc_urlloc, rparams_dict, clock)
			if resp[0] == 200:
				retrycnt = retrycnt + 1
				mdlib.log("* retry sms cmd msg will be sent to userid={0}".format(ruser))
			else:
				mdlib.log('! retry request failed, uid={0}, code={1}, msg={2}'.format(ruser, resp[0], res[1]))
				
	return msgstr, execres

#
# function interpret_result()
#		: interpret the result and make dictionary structure 
#		  from the previous stage output.
#
def interpret_result(msgstrs, cstages, prev_params_dict):

	global ENV

	# msgstr has { 'userid' : 'uidlist' } structure..
	pdict = prev_params_dict.copy()

	for i in range(len(cstages)):
		# amt_recruit
		if cstages[i].stype == 'recruit':
			users = msgstrs[0].split('#')
			if cstages[i].config_output and len(users) > 0:
				ENV[cstages[i].config_output] = users[0]
				pdict = dict()

		# amt_curate
		elif cstages[i].stype == 'vote':
			ans = msgstrs[i].split('#')
			bitmask = None
			for j in range(len(ans)):
				yesno = ans[j].split(',')
				yes_cnt = yesno[0].split('-')[1]
				no_cnt = yesno[1].split('-')[1]
				if yes_cnt < no_cnt:
					bitmask = "0" if bitmask == None else "{0}|0".format(bitmask)
				else:
					bitmask = "1" if bitmask == None else "{0}|1".format(bitmask)
			mdlib.log("* bitmask: " + bitmask)

			if cstages[i].config_output != None:
				key = cstages[i].config_output
				pdict[key] = bitmask

		# anything else
		else:
			# put { '<output>' : 'uidlist' } into the params_dict..
			if cstages[i].config_output != None:
				pdict[cstages[i].config_output] = msgstrs[i]

	return pdict


#
# entry function for individual task.
#
def task_main(taskpath):

	global data_base_url, spc_host, spc_urlloc, hit_host, hit_urlloc
	global wait4res_duration, poll4res_interval
	global failover_interval, failover_retrylimit
	global debug, anonymity, clock, ENV
	global asyncsoc, cs_dbhost, cs_dbname

	mdlib.log("********************************")
	mdlib.log("*       Task-Flow Server       *")
	mdlib.log("********************************")
	mdlib.log("* starting task-flow server(TFS).")

	asyncsoc = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	asyncsoc.connect(('localhost', 20968))

	pid = os.getpid()
	mdlib.msg_send(asyncsoc, str(pid))

	######################################################
	# 	Program Start
	#
	
	# load envvars
	vdict = get_env_values("BASE-URL-DATA,SPC-HOST,SPC-URI,HIT-HOST,HIT-URI,FAIL-SLEEP-INTERVAL,FAIL-RETRY-CNT,WAIT-DURATION-FOR-STAGE,CHECK-PERIOD-FOR-STAGE-OUTPUT")
	
	# Server configurations.
	data_base_url = vdict["BASE-URL-DATA"]
	spc_host = vdict["SPC-HOST"]
	spc_urlloc = vdict["SPC-URI"] 
	hit_host = vdict["HIT-HOST"] 
	hit_urlloc = vdict["HIT-URI"] 
	# parameters on failover
	wait4res_duration = float(vdict["WAIT-DURATION-FOR-STAGE"])
	poll4res_interval = float(vdict["CHECK-PERIOD-FOR-STAGE-OUTPUT"])
	failover_interval = float(vdict["FAIL-SLEEP-INTERVAL"])
	failover_retrylimit = float(vdict["FAIL-RETRY-CNT"])
	
	# declare global data structures.
	cur_stages = []
	cur_stage = None
	
	# parse input arguments
	fname = taskpath
	dict_stages, dict_conns, cur_stage = parse_xml(fname)
	cur_stages.append(cur_stage)
	
	mdlib.log('* parsing completed.')
	
	mdlib.log('* starting parameter type checking.')
	mdchecker.check(dict_stages, dict_conns, cur_stage, ENV)
	mdlib.log("* [overhead] task interpretation delay: {0}".format(clock.stop()))
	#mdlib.log("* [overhead] type-checking delay: {0}".format(clock.stop()))
	mdlib.log('* done for type checking.')
	
	mdlib.log('* pid={pidval}'.format(pidval=pid))
	mdlib.log_appname_pid_mapping(cs_dbhost, cs_dbname, ENV["APP_NAME"], pid)
	
	# update program state
	if debug:
		mdlib.update_program_state(cs_dbhost, cs_dbname, pid, "startTFS", ENV["APP_NAME"])
	
	# run program
	params_dict = dict()
	newdevice = ""

	if ENV["G_VAR"] != None:
		g_vars = ENV["G_VAR"].split(",")
		for i in range(len(g_vars)):
			g_kv = g_vars[i].split("=")
			params_dict[g_kv[0]] = g_kv[1]
			mdlib.log("Global input var name {0} is {1}".format(g_kv[0], g_kv[1]))
	
	while len(cur_stages) > 0:
		
		# initialize
		exec_result = [[]] * len(cur_stages)
		qid = [[]] * len(cur_stages)
		respstr = [[]] * len(cur_stages)
	
		# generate qid
		for i in range(len(cur_stages)):
			exec_result[i] = False
			qid[i] = random.randint(1000, 9999)
			mdlib.log('* qid[{idx}]={qidval}'.format(idx=i, qidval=qid[i]))
		
		mid = random.randint(1000,9999)
		cur_stage = cur_stages[0]	# assume that all stages in the same level has the same type.
	
		mdlib.log("* [overhead] task tracker latency amonst stages: {0}".format(clock.stop()))
	
		########################################
		# Human Intelligence Task
		#
		if cur_stage.type == "HIT":
			mdlib.log('* invoking HIT ['+cur_stage.name+']')
	
			# process amt credentials.
			if cur_stage.rid == None:
				rid = ENV["R_RID"]
				rkey = ENV["R_RKEY"]
			else:
				rid = ENV[cur_stage.rid]
				rkey = ENV[cur_stage.rkey]
	
			if cur_stage.wid == None:
				wid = ENV["W_WID"]
			else:
				wid = ENV[cur_stage.wid] if cur_stage.wid in ENV else cur_stage.wid
				
			req_params = [[]] * len(cur_stages)
			
			# set data-owner
			if len(params_dict) > 0:
				dataowner = ENV["W_WID"]
			else:
				dataowner = 'None'
	
			# set data..
			if cur_stage.config_input in params_dict:
				data = params_dict[cur_stage.config_input]
			else:
				data = 'None'
				
			# make requests (aggregated)
			for i in range(len(cur_stages)):
				params = {'pid' : pid, 'qid' : qid[i], 'action' : 'regHIT', 'qtype' : cur_stage.stype
						, 'rid' : rid, 'rkey' : rkey, 'wid' : wid
						, 'query' : cur_stage.config_stmt, 'expiration' : cur_stage.config_expiration
						, 'reward' : cur_stage.config_reward, 'numusers' : cur_stage.config_numusers
						, 'dataowner' : dataowner, 'data' : data }
						
				req_params[i] = params.copy()
	
			# wait for the response
			for i in range(len(cur_stages)):
				respstr[i], exec_result[i] = wait_for_hit_response(pid, qid[i], cur_stages[i], req_params[i])
				
			mdlib.log("* [overhead] HIT task execution time: {0}".format(clock.stop()))
	
		#########################################
		# SPC (Sensing, Processing, Communicaion)
		#
		elif cur_stage.type == "SPC":
			rp_dict = [[]] * len(cur_stages)
	
			# request route
			for i in range(len(cur_stages)):
				mdlib.log('* SPC Task [' + cur_stages[i].name + ']')
				if i > 0: 
					time.sleep(3)
	
				rp_dict[i] = dict()
				emails = None
				cnt = 0
	
				# if <device> tag is set by email address, anonymity will be gone. 
				# commanding emails will be delivered to the phones directly.
				if cur_stages[i].device != None:
					type, addrs = cur_stages[i].device.split("=")
					mdlib.log("* <device> tag is set, content={0}".format(cur_stages[i].device))
					if type != "EMAIL":
						mdlib.log("! <device> config error. it should start with either 'EMAIL=' or 'AMT='")
					else:
						emails = addrs.split('|')
						if len(emails) > 0:
							anonymity = False
							userids = emails
	
				# make requests to mobile users
				uid = ENV["W_WID"]
				req_params = mst_request_params(pid, qid[i], cur_stages[i], uid, params_dict)
				if len(cur_stages) > 1:
					req_params['custom'] = "<multi>{0}|{1}/{2}</multi>{3}".format(mid, i, len(cur_stages), req_params['custom'])
				rp_dict[i] = req_params
				if anonymity == True:
					# case I) req. to AMT
					resp = mdlib.httpsend(spc_host, spc_urlloc, req_params, clock)
	
					if resp[0] == 200:
						cnt = cnt+1
					else:
						mdlib.log("! mst_request_http_error code={0}".format(resp[0]))
						exit()
				else:
					# case II) req. via direct email
					req_params['custom'] = "<xml><pid>{0}</pid><qid>{1}</qid><amtid>{2}</amtid>{3}</xml>"	\
												.format(pid, qid[i], uid, req_params['custom'])
					#print req_params['custom']
					mdlib.send_email('Medusa commanding message', req_params['custom'], uid)
	
			# wait for job completions
			mdlib.log("* command msg has been sent to userid={0}".format(uid))

			idx = 0
			info = []

			while idx < len(cur_stages):
				msg = mdlib.msg_recv(asyncsoc, 128)
				idx = idx + 1
				info = msg.split(',')

				if info[0] == 'completeTask':
					if int(info[2]) in qid:
						respstr[i], exec_result[i] = pullout_db(info[1], info[2])
				else:
					mdlib.log("! error: " + msg)

			# YX-project specific.
			if (cur_stage.notification != None):
				mdlib.log("* notifcation: " + cur_stage.notification)
				sqlstmt = "select path,uid,muid from CS_data where pid = {0} and qid = {1}"\
						.format(info[1], info[2])
				con, cur = mdlib.get_raw_mysql_data(cs_dbhost, cs_dbname, sqlstmt)
				path = ''
				row = cur.fetchone()
				file_ids = row[1]+'=' if row != None else ''
				while row != None:
					path = path + row[0]+","
					file_ids += row[2]+'|'
					row = cur.fetchone()
				con.close()
				
				path = path[:-1] if len(path) > 0 else path
				file_ids = file_ids[:-1] if len(file_ids) > 0 else file_ids
				d = [(file_ids, path)]
				req = urllib2.Request(cur_stage.notification, urllib.urlencode(d))
				u = urllib2.urlopen(req)

#			for i in range(len(cur_stages)):	
#				respstr[i], exec_result[i] = wait_for_mst_response(pid, qid[i], rp_dict[i], cur_stages[i].config_output)
	
			mdlib.log("* [overhead] SPC task execution time: {0}".format(clock.stop()))
	
		else:
			mdlib.log("! unknown stage type: " + cur_stage.type)
			exit()
	
		# judge if the operation was successful
		exec_resall = True
		for i in range(len(cur_stages)):
			if exec_result[i] == False:
				exec_resall = False
	
		#
		# augment passing parameter set.
		# { 'amt_wid' : { 'var_name' : 'uidlist', 'var_name' : 'uidlist', ... }, ... }
		#
		if exec_resall == True:
			params_dict = interpret_result(respstr, cur_stages, params_dict)
			update_amt_cred(pid)
		else:
			params_dict = dict();
	
		mdlib.log(params_dict)
	
		# decide next stage.
		if cur_stage.name in dict_conns:
			cstages = []
			for sg in cur_stages:
				for c in dict_conns[sg.name]:
					if exec_resall == True: 
						st = dict_stages[c.dst_success]
					else:
						st = dict_stages[c.dst_failure]
			
					if st not in cstages:
						cstages.append(st)
			cur_stages = cstages
		else:
			cur_stages = []
	
	# end of while cur_stage:
	#
	
	mdlib.log("* shutting down task-flow server..")
	
	# update program state
	if debug:
		mdlib.update_program_state(cs_dbhost, cs_dbname, pid, "exitTFS", ENV["APP_NAME"])
	asyncsoc.close()
	
if __name__ == '__main__':
	task_main(parse_args())
	

#!/usr/bin/env python
#
import sys, os, time, random
import mdlib
from xml.etree import ElementTree as ET
from mdtype import TFStage, TFConn, StopWatch


option = None
rid = None
rkey = None

# 
# display_usage(): display help message.
#
def display_usage():
	print """\
usage: 
	mdreset.py [OPTIONS]
options:	
	-h display usage
	-all delete all intermediate states
	-hit delete HIT related states
	-rs delete RS related states
	-rid [rid]
	-rkey [rkey]
		"""
	exit()

#
# parse_args(): parse input parameters.
#
def parse_args():
	global rid
	global rkey
	opt = None
	for i in range(len(sys.argv)):
		if i == 0: continue
		if sys.argv[i] == "-h":
			display_usage()
		elif sys.argv[i] == "-all":
			opt = 'all'
		elif sys.argv[i] == "-hit":
			opt = 'hit'
		elif sys.argv[i] == "-rs":
			opt = 'rs'
		elif sys.argv[i] == "-rid":
			rid = sys.argv[i+1];
		elif sys.argv[i] == "-rkey":
			rkey = sys.argv[i+1];
		#elif sys.argv[i][0] != '-':
			#print '! wrong options'
			#exit()
	return opt


######################################################
# 	Program Start
#
option = parse_args()
if option == None:
	mdlib.log("! no options specified")
	display_usage()

clock1 = StopWatch()
mdlib.log("********************************")
mdlib.log("*       Reset Utility          *")
mdlib.log("********************************")

# set configurations.
vdict = mdlib.get_env_vars(mdlib.get_dbhost(), "medusa", "HIT-HOST,HIT-URI");
hit_host = vdict["HIT-HOST"];
hit_urlloc = vdict["HIT-URI"];

pid = 'all'
qid = 'all'

# send delete messages to HIT server
if option == 'all' or option == 'hit':
	params = {'action' : 'rmHIT', 'pid' : pid, 'qid' : qid, 'rid' : rid, 'rkey' : rkey, 'wid' : 'all'}
	resp = mdlib.httpsend(hit_host, hit_urlloc, params)

	if resp[0] == 200:
		mdlib.log('deleted')
	else:
		mdlib.log('! HITS err, data may not be deleted: code={0} msg={1}'.format(resp[0], resp[1]))

# send delete messages to RS server
#if option == 'all' or option == 'rs':
#	params = {'pid' : pid, 'action' : 'regRS', 'qtype' : cur_stage.action_stype, 'qid' : qid }
#	resp = mdlib.httpsend(rs_host, rs_urlloc, params)
#	
#	if resp[0] == 200:
#		mdlib.log('deleted')
#	else:
#		mdlib.log('! RSS err, data may not be deleted: code={0} msg={1}'.format(resp[0], resp[1]))

mdlib.log("* shutting down..")
#mdlib.log("* " + clock1.stop() + " passed")




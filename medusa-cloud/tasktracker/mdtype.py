#!/usr/bin/env python

from datetime import datetime

class TFStage:
	def __init__(self):
		pass

	# member variables
	xmlstr = None		# full stage description
	name = None			# stage identifier
	type = None			# type of stage, e.g. HIT or RemoteSensing
	inst = None			# type of stage, e.g. HIT or RemoteSensing
	listener = None		# emails that overhear HIT task registrations
	stype = None		# <binary> tag
	device = None 		# devices that will run the stage, for the remote sensing stage.
	trigger = None 		# 
	review = None 		#
	notification = None	#	
	timeout = 5			#
	rid = None 			# requester-id
	rkey = None 		# requester-key
	cmdpush = 'c2dm' 	# default command push mechanism: sms, c2dm
	wid = "W_WID" 		# worker_id_list, e.g. id1|id2|id3 

	config_params = None
	config_input = None
	config_output = None
	config_stmt = None
	config_expiration = None
	config_reward = .01
	config_numusers = 1

class TFConn:
	def __init__(self):
		pass

	# member variables
	src = "name of the source stage of this connector"
	dst_success = "destination stage if the stage execution was successful."
	dst_failure = "destination stage if the stage execution failed."


class StopWatch:
	def __init__(self):
		self.starttime = datetime.now()
		return

	def stop(self):
		endtime = datetime.now()
		elapsed = endtime - self.starttime
		self.starttime = datetime.now()
		return elapsed

	starttime = None



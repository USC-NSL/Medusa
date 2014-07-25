#!/usr/bin/env python
#
# This file implements type checker function to 
# the Medusa Script Interpreter.
#
# @modified: Dec. 9th 2011
# @author: Moo-Ryong Ra(mra@usc.edu)
#
import re

def check(stages, conns, curstage, envvars):

	# stage check.
	stkeys = stages.keys()
	stkeys_ref = stages.keys()
	for stk in stkeys:
		st = stages[stk]

		# <input> tag
		if st.config_input != None:
			m = re.match("[\$\w][\w,]+", st.config_input)
			if m == None or len(m.group(0)) != len(st.config_input):
				print '! illegal <input> tag use [{0}], please check below'.format(st.config_input)
				print '\ta) only alphanumeric characters can be used.'
				print '\tb) no spaces are allowed'
				print '\tc) masking variable tag($) should be tagged at the beginning.'
				exit()
			# eligibility: input should be in envvars or one of output variable names..
			inps = st.config_input.split(",")
			for instr in inps:
				eligible = False
				if instr.find("GVAR") != -1:                #For INPUT at very beginning
					eligible = True
					break
				for ik in stkeys_ref:
					ist = stages[ik]
					if instr in envvars or instr == ist.config_output:
						eligible = True
						break
				if eligible == False:
					print '! ineligible <input> variable [{0}]'.format(instr)
					exit()

		# <output> tag
		if st.config_output != None:
			# illegal character usage.
			m = re.match("[\$\w]\w+", st.config_output)
			if m == None or len(m.group(0)) != len(st.config_output):
				print '! illegal <output> tag use [{0}], please check below'.format(st.config_output)
				print '\ta) only alphanumeric characters can be used.'
				print '\tb) no spaces are allowed'
				print '\tc) only one variable can be used as an output'
				exit()

		# mandatory field check.
		if st.name == None:
			print '! mandatory field <name> is not set'
			exit()
		if st.type == None:
			print '! mandatory field <type> is not set for the stage [{0}]'.format(st.name)
			exit()
		elif st.type != 'HIT' and st.type != 'SPC':
			print '! <type> should be either \'HIT\' or \'SPC\'. \'{0}\' is not supported.'.format(st.type)
			exit()
		if st.stype == None:
			print '! mandatory field <binary> is not set for the stage [{0}]'.format(st.name)
			exit()
 		if st.wid == None:
			print '! mandatory field <wid> is not set for the stage [{0}]'.format(st.name)
			exit()


	# connector check
	cnkeys = conns.keys()
	for cnk in cnkeys:
		conn_list = conns[cnk]
		if cnk not in stages:
			print '! src of the connector(src=[{0}]) is not matched with any of stage names'.format(cnk)
			exit()
		for i in range(len(conn_list)):
			if conn_list[i].dst_success not in stages:
				print '! dst of the connector(dst_succ=[{0}]) is not matched with any of stage names'	\
														.format(conn_list[i].dst_success)
				exit()
			if conn_list[i].dst_success not in stages or conn_list[i].dst_failure not in stages:
				print '! dst of the connector(dst_fail={0}]) is not matched with any of stage names'	\
														.format(conn_list[i].dst_failure)
				exit()


#!/usr/bin/env python
#
#	This program is intended to run as a background daemon.
#	Poll the runner table, and if there is an entry,
#	run or stop medscript program.
#
#	should run in /TFS/ directory.
#
import sys, os, re, time, signal
import mdlib, mdserv

# 
# function display_usage()
#		: display help message.
#
def display_usage():
	print """\
usage: 
	nohup ./mdscript_runner.py &
		"""
	exit()


def main():

	dbhost = mdlib.get_dbhost()
	dbname = "medusa"
	
	print '* starting mdscript_runner, host=%s dbname=%s' % (dbhost, dbname)

	while True:
		# check if there is a new request
		sqlstmt = "select cmd,medscript from CS_cmdqueue order by timestamp desc"
		con, cur = mdlib.get_raw_mysql_data(dbhost, dbname, sqlstmt)
		dat = cur.fetchone()

		i = 0
		while dat != None:
			#dat[0], dat[1]
			cmd = dat[0]
			taskpath = dat[1]

			if cmd == 'start':
				# fork and run medusa program.
				pid = os.fork()
				if pid == 0:
					# child 
					mdserv.task_main(taskpath)
					exit()
				else:
					# parent
					#mdlib.log('* created crowd-sensing task id={0}.'.format(pid))
					print '* created crowd-sensing task id={0}.'.format(pid)

			elif cmd == 'stop':
				# for future use.
				os.system('./sig2daemon.sh {0}'.format(taskpath));

			dat = cur.fetchone()
		con.close()

		sqlstmt = "delete from CS_cmdqueue"
		mdlib.get_single_mysql_data(dbhost, dbname, sqlstmt)

		# polling interval: 3 seconds.
		time.sleep(3)
	
	#mdlib.log('* exits medusa task runner.')
	print '* exits medusa task runner.'


if __name__ == '__main__':
	main()



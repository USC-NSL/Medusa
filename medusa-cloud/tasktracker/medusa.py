#!/usr/bin/env python
#
# Medusa server front-end
#
#
import sys, os, re, pty, time, signal
import mdlib, mdserv

#
# function parse_args()
#		: parse input parameters.
#
def parse_args():
	# input parameter parsing.
	if len(sys.argv) != 4:
		display_usage()
		exit()

	taskp = sys.argv[1]
	inst = sys.argv[2]
	dl = sys.argv[3]

	return taskp, int(inst), int(dl)

# 
# function display_usage()
#		: display help message.
#
def display_usage():
	print """\
usage: 
	./medusa.py <program> <# of instances> <deadline>
		"""
	exit()


def main():
	taskpath, instances, deadline = parse_args()	
	print taskpath, instances, deadline

	chids = [[]] * instances;
	
	for chidx in range(instances):
		pid = os.fork()
		if pid == 0:
			# child 
			mdserv.task_main(taskpath)
		else:
			# parent
			mdlib.log('* created {0}th task id={1}.'.format(chidx, pid))
			chids[chidx] = pid

	# parent will wait until the deadline
	if pid != 0:
		cnt = 0
		while True:
			time.sleep(1)
			cnt = cnt + 1

			# deadline check..
			if cnt == deadline:
				mdlib.log('! deadline ({0} seconds) passes.'.format(deadline))
				for cid in chids:
					os.kill(cid, signal.SIGTERM)
					mdlib.log('* task id {0} is killed'.format(cid))
				break

			#mdlib.log('* checked {0}/{1}'.format(cnt, deadline))

		mdlib.log('* exits medusa task runner.')


if __name__ == '__main__':
	main()





#!/usr/bin/env python

import os, pty

os.system("rm nohup.out")
os.system("./reset_hits.sh")
os.system("./sig2daemon.sh mdscript_runner.py")
os.system("./sig2daemon.sh mdscript_acceptor.py")

print "* starting daemon processes"

pid, fd = pty.fork()
if pid == 0:
	os.system("nohup ./mdscript_acceptor.py &")
else:
	os.system("nohup ./mdscript_runner.py &")

os.system("ps -ef | grep mdscript")


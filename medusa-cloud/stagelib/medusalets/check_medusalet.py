#!/usr/bin/env python
#
# This program has a dependency on the external program, dex2jar.
# You can download it at below link, and fix a broken link.
#
#	 	http://code.google.com/p/dex2jar/
#
# @author: Moo-Ryong Ra, mra@usc.edu
#
import sys, os, re

#
# function parse_args()
#		: parse input parameters.
#
def parse_args():
	# input parameter parsing.
	filename = ""

	for i in range(len(sys.argv)):
		if i == 0: continue
		if sys.argv[i] == "-h":
			display_usage()
		elif sys.argv[i][0] != '-':
			filename = sys.argv[i]
			break
		else:
			display_usage()
	
	if filename and os.path.isfile(filename):
		print '* verified input file: [' + filename + ']'
	else:
		print '! ' + filename + 'is not a file'
		exit()

	return filename

# 
# function display_usage()
#		: display help message.
#
def display_usage():
	print """\
usage: 
	check_medusalet.py [path-to-the-medusalet]
options:	
	-h display usage
		"""
	exit()

def inspect_violation(fname):
	print "* start inspecting.. " + fname

	ret = True
	fp = open(fname, 'r')
	line = fp.readline()
	#while line and ret:
	while line:
		line.replace('[\r\n]+', '')
		if 'invoke' in line:
			# level-2 package
			#package = re.findall("([a-zA-Z]+(/[a-zA-Z]+)+)(?=.)", line)
			package = re.findall("[a-zA-Z]+/[a-zA-Z]+.*(?=\.)", line)
			for p in package:
				pname = p.replace('/', '.')	# package name
				if re.search("medusa\.mobile", pname) >= 0 or	\
				   re.search("medusa\.medusalet", pname) >= 0 or \
				   re.search("Iterator", pname) >= 0 or \
				   re.search("Cursor", pname) >= 0 or \
				   re.search("java\.lang\.Boolean", pname) >= 0 or \
				   re.search("java\.lang\.Byte", pname) >= 0 or \
				   re.search("java\.lang\.Character", pname) >= 0 or \
				   re.search("java\.lang\.Double", pname) >= 0 or \
				   re.search("java\.lang\.Float", pname) >= 0 or \
				   re.search("java\.lang\.Integer", pname) >= 0 or \
				   re.search("java\.lang\.Long", pname) >= 0 or \
				   re.search("java\.lang\.Math", pname) >= 0 or \
				   re.search("java\.lang\.Number", pname) >= 0 or \
				   re.search("java\.lang\.Object", pname) >= 0 or \
				   re.search("java\.lang\.Short", pname) >= 0 or \
				   re.search("java\.lang\.String", pname) >= 0 or \
				   re.search("java\.lang\.StringBuffer", pname) >= 0 or \
				   re.search("java\.lang\.StringBuilder", pname) >= 0 or \
				   re.search("java\.lang\.Void", pname) >= 0 or \
				   re.search("java\.util\.HashMap", pname) >= 0 or	\
				   re.search("java\.util\.Hashtable", pname) >= 0 or	\
				   re.search("java\.util\.LinkedList", pname) >= 0 or	\
				   re.search("java\.util\.PriorityQueue", pname) >= 0 or	\
				   re.search("java\.util\.Vector", pname) >= 0 or	\
				   re.search("java\.util\.Stack", pname) >= 0 or	\
				   re.search("java\.util\.Set", pname) >= 0 or	\
				   re.search("java\.util\.Arrays", pname) >= 0 or	\
				   re.search("java\.util\.ArrayList", pname) >= 0 or	\
				   re.search("java\.util\.Map", pname) >= 0 or	\
				   re.search("java\.util\.TreeMap", pname) >= 0:
					print pname, "[OK]", line,
				else:
					print pname, "[REJECT]", line,
					ret = False

		line = fp.readline()
	fp.close()
	
	print "* done for inspecting.. " + fname

	return ret
	

def main():
	fname = parse_args()
	dirname = fname.split('.')[0]
	print "* [" + fname +"] will be analyzed"

	# remove remnant from the previous trial
	os.system('rm -rf ' + dirname)

	os.system('unzip ' + fname + ' -d ' + dirname)
	print "* [" + fname +"] has been unzipped"

	os.system('./dex2jar.sh ' + dirname + '/classes.dex')
	print "* converted classes.dex file to .jar file"

	os.system('javap -classpath ' + dirname + '/classes_dex2jar.jar -c -s $(jar -tf ' + dirname + '/classes_dex2jar.jar | grep class | sed \'s/.class//g\') > ' + dirname + '/analyzed.txt')
	print "* medusalet [" + fname + "] has been decompiled"

	result = inspect_violation(dirname + '/analyzed.txt')
	if result == True:
		print "* [OK] medusalet [" + fname + "] has been accepted"
	else:
		print "! [REJECTED] medusalet [" + fname + "] has been rejected, see above logs"

	# clean up
	os.system('rm -rf ' + dirname)

if __name__ == '__main__':
	main()







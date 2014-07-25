#!/bin/bash
#
# kill processes using name.
#
pid=`ps -ef | grep $1 | grep -v grep | grep -v sig2daemon | awk '{print $2}'`;
echo $pid
kill -9 $pid


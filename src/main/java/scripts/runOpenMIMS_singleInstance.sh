#!/bin/bash
#
################################################
## The script to call to start OpenMIMS in 
## single instance mode, with optional file 
## argument. Useful for interacting with an html
## tracking document.
##
################################################

# If symlinks get involved it gets complicated, see:
# http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
# This script assumes that we're in Fiji.app/lib/
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

FIJIPATH=$DIR/..
cd $FIJIPATH
./ImageJ-linux64 -eval "run('Open MIMS Image', '-single_instance $@')" >&  $FIJIPATH/logs/logfile_$(whoami)_$(hostname)_$$.txt

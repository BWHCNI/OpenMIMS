#!/bin/bash
#
################################################
## The script to call to start OpenMIMS. To
## open the plugin with a particular file, pass
## the file name as an argument.
##
################################################

# If symlinks get involved it gets complicated, see:
# http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
# This script assumes that we're in Fiji.app/lib/
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

FIJIPATH=$DIR/..
cd $FIJIPATH
./ImageJ-linux64 --allow-multiple -eval "run('Open MIMS Image', '$@'); run('Install...', 'install=$FIJIPATH/macros/openmims_tools.fiji.ijm');" >&  $FIJIPATH/logs/logfile_$(whoami)_$(hostname)_$$.txt &

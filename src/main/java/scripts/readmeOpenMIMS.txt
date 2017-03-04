Short notes about scripts in this directory.  Most scripts are self documented.

runOpenMIMS.sh - bash script to start Fiji and run OpenMIMS passing args to OpenMIMS.
Also runs a macro to do the toolbar setup and logs stdout/stderr to a file.

runOpenMIMS_singleInstance.sh - same as above, but with -single_instance flag added.

OpenMIMS.desktop - a .desktop file to allow filetype-application association. Copy
to ~/.local/share/applications/ or similar



./archive/
Note: runUI* are all IJ scripts that hit com.nrims.UI.main() first.
Should be considered deprecated. Left here as a warning to those who follow.

runUI - calls com.nrims.UI to start OpenMIMS directly
runUI_multi_instance - starts OpenMIMS in multi instance mode
runUI_single_instance - starts OpenMIMS in single instance mode

runUI_openjdk - use openjdk explicitly, not default java
runUI_nolog - skip logging, print to standard out


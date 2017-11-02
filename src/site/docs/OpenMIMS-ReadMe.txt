 National Resource for Imaging Mass Spectrometry

==================================
PRODUCT DESCRIPTION
==================================

 OpenMIMS version 3.0.3 (Nov 2017)

 OpenMIMS is an ImageJ plugin designed to open, process and analyze 
 images captured with NanoSIMS 50 & 50L secondary ion mass 
 spectrometers (Cameca).

 The OpenMIMS plugin has been developed at the National Resource for
 Imaging Mass Spectrometry (Center for NanoImaing, https://nano.bwh.harvard.edu/), 
 an NIH-supported National Resource developing Multi-isotope Imaging 
 Mass Spectrometry (MIMS) for biomedical research. Images and/or 
 stacks of images of up to 7 different isotopes can be opened, 
 analyzed and saved. Image ratios and Hue-Saturation-Intensity (HSI) 
 maps of any combination of isotopes can be displayed and data from 
 any number of Regions of Interest (ROIs) extracted, analyzed and 
 tabulated for single images or entire stacks.
 
==================================
ACKNOWLEDGEMENT
==================================
 
 OpenMIMS is an Open Source software project that is funded through 
 the NIH/NIBIB National Resource for Imaging Mass Spectrometry. 
 Please use the following acknowledgment and send us references to 
 any publications, presentations, or successful funding applications 
 that make use of OpenMIMS software:

 	"This work was made possible in part by the OpenMIMS software 
 	whose development is funded by the NIH/NIBIB National Resource 
 	for Imaging Mass Spectrometry, NIH/NIBIB 5P41 EB001974-10."

==================================
HARDWARE AND SOFTWARE REQUIREMENTS
==================================

 OpenMIMS is an ImageJ plugin. It will run on any system 
 running ImageJ 1.43u (or higher) and Java 1.8 (or higher). The 
 ImageJ application can be downloaded at the following location:

   https://imagej.net/Fiji/Downloads


==================================
CONTENTS AND INSTALLATION
==================================

 For instructions on how to install OpenMIMS, refer to the OpenMIMS manual 
(OpenMIMS-Manual.pdf).  For convenience, those instructions are repeated
here:

1. OpenMIMS requires Java 1.8 or later to be present on your computer. 
   If you do not already have Java 1.8 installed, you can download it at 
   http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html
2. Download and install Fiji (aka ImageJ), which you can get at 
  https://imagej.net/Fiji/Downloads
3. Run Fiji, then go to the ”Help” menu and select the ”Update Fiji” 
   option. This will do an online check for any possible updates to Fiji 
   dependencies. If any such dependencies appear in the displayed it, click 
   on the ”Apply changes” button at the bottom of the window to download and
   apply those updates. Do not close the ImageJ Updater yet.
4. Click on the ”Manage update sites” button at the bottom of the ImageJ 
   updater window. A window that shows a list of the Fiji update sites should 
   appear. Scroll to the bottom of the list. If you see an entry for OpenMIMS, 
   you can close this window and move to the next step. If not, you need to 
   add that row to the table: Click on the ”Add update site” button. In the 
   Name column, enter ”OpenMIMS”. In the URL column, enter 
   http://nrims.partners.org/OpenMIMS-Fiji/ Make sure the checkbox on the left
   side of this entry is checked, then close the window. Adding this line 
   ensures that, upon startup, Fiji will always download the newest version 
   of OpenMIMS from our update site at Partners whenever a new version is 
   posted to the update site by the OpenMIMS developers. If you do not want 
   these updates to occur, uncheck the checkbox in the table.
5. Quit and restart Fiji. Again, go the ”Help”, menu and select the 
   ”Update Fiji” option. It should download the most recent copy of OpenMIMS 
   from the Partners update site. Once again, quit and restart Fiji and the 
   most recent OpenMIMS plugin should be available on the ”Plugins” menu of 
   Fiji. After you launch the OpenMIMS plugin, you can determines the version
   by going to the ”Help” menu and selecting ”About Open MIMS”.


 See OpenMIMS-Manual.pdf for a brief guide/manual on the OpenMIMS plugin.

==================================
RELEASE NOTES
==================================

 Nov 2017 (v3.0.3):
 - OpenMIMS now requires Java 1.8 or higher. 
 - The OpenMIMS development project (in NetBeans) now uses Maven for builds. 
 - Numerous UI tweaks, code cleanup, performance upgrades and bug fixes.
 - Added the ability to have ROIs automatically saved on a timed basis.
 - Added an Interleave button to the stack editing tab.  This is used to 
   create separate images for masses that are peak-switched from one image 
   acquisition (or series) to the next.
 - Added many more tooltips throughout the user interface.
 - The segmentation functionality was integrated with the ROI manager.
 - Added ability to successfully read image files that have bad headers.
 - If notes are changed, the associated image file is now also updated.
 - Added an item to the preferences to enable/disable the high DPI kludge
   when OpenMIMS is running in Linux.
 - Fixed some bugs in EM registration (some remain).
 - Various bug fixes and improvements in the drag and drop functionality.
 - Added the ability to use the Cividis LUT from the Pacific Northwest 
   National Laboratory
 - Added a combobox to the preferences dialog to allow the setting of the
   default LUT.


 June 2012 (v2.5):
 - Allow for opening of individual tiles for mosaic images.
 - Auto generate derived images when opening individual tiles from mosaic.
 - Added 3D HSI export in QVis format.
 - Additional functionality of Generate Report feature.
 - Added sanity/error check between file header and actual data in file.
 - More organized layout when generating Sum images.
 - Decimal places in table adjustable.
 - Read/Write metadata key value pairs.
 - Various performance upgrade and bug fixes.


 December 2011 (v2.0):
 - Implemented QSA and dead time correction.
 - All data product report raw data only (not medianized).
 - Performance increases in the Roi Manager (and better behavior of brush tool).
 - Mass symbols from .im file displayed on Mims Data tab.
 - Able to open plugin as a single instance. 
 - Check for changes before opening new files.
 - Various performace improvements.
 - Changes to facilitate the api for scripting.
 - Fixed layout for MACs.

 October 2011 (v1.4x):
 - Various UI improvements and code cleanup.
 - Filter +/- infinity from plots.
 - Better behavior for generating tables.
 - Allow hsi to show up in tomography tab.
 - Allow users to generate reports.
 - Added terminal usage capability to plugin for batch jobs.
 - Allow user to delete columns from table.
 - Make opening file a background/cancelable task.
 - Read stage scan .im files.
 - Read line scan .im files.
 - Read 32 bit files,
 
 March 2011 (v1.4):
 - Added feature in RoManager allowing user to get pixel values for Rois.
 - Added a file filter to the open file dialog.
 - Status updates while opening large files.
 - Always update ImageJ default directory when opening or saving 
   files of any type.
 - Better behavior for Rois encapsulated within a Roi.
 - Enable ratio images with a denominator of 1, opening up all the features
   for ratio images to mass images.
 - Fixed buggy behavior of profile plots for line Rois.
 - Fixed 'rename' button for renaming Rois and Roi groups.
 - Added the N/D statistic and enabled all statistics in the Tomography tab.
 - Fixed bug that prevented zooming of the x-axis on Contrast plots.
 - General upgrades in performace .
 
 November 2010 (v1.0):
   The official version 1.0 release of the OpenMIMS plugin. 

==================================
LICENSE INFORMATION
==================================

 Copyright 2017 NRIMS, National Resource for Imaging Mass Spectrometry.
 All rights reserved.
 
https://nano.bwh.harvard.edu/
 
 Permission is hereby granted, free of charge, to any person
 obtaining a copy of this software and associated documentation
 files (the "Software"), copy, modify, merge, publish, or
 otherwise alter this software for educational or academic
 purposes subject to the following conditions:
 
 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software. 
 
 The copyright holders of other software modified and included in
 the Software retain their rights and the licenses on that software
 should not be removed.
 
 Cite the NRIMS acknowledgement (above) in any publication that 
 relies on the Software. Also cite those projects on which the 
 Software relies when applicable. See the "About OpenMIMS" menu 
 for the most up to date list.
 
 If you would like to use or modify the Software for commercial
 purposes contact us.
 
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 OTHER DEALINGS IN THE SOFTWARE.

 OpenMIMS has modified, uses, or depends upon: 
   * TurboReg:  http://bigwww.epfl.ch/thevenaz/turboreg/ 
   * libSVM: http://www.csie.ntu.edu.tw/~cjlin/libsvm/ 
   * NRRD file format: http://teem.sourceforge.net/nrrd/ 
   * nrrd plugins: http://flybrain.stanford.edu/nrrd 
   * jFreeChart:  http://www.jfree.org/jfreechart/ 
   * FileDrop:  http://iharder.sourceforge.net/current/java/filedrop/ 
   * Apache Commons: http://commons.apache.org/io/
   * jRTF: http://code.google.com/p/jrtf/
	* jUnique: http://www.sauronsoftware.it/projects/junique/

 Please cite OpenMIMS or any of the above projects when applicable.
 
==================================
CONTACT INFORMATION
================================== 

 National Resource for Imaging Mass Spectometry

   Website: https://nano.bwh.harvard.edu/
   Email: nrims@rics.bwh.harvard.edu
   Phone: (617) 768-8262 or (617) 768-8286
   Fax: (617) 768-8260

   
 For inquiries regarding feature requests, bug report, or anything
 else related to the OpenMIMS plugin, please send email to:
 
   nrims.software@rics.bwh.harvard.edu




 
 

 


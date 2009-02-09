package com.nrims;

import ij.measure.ResultsTable;
import ij.process.ImageStatistics;
import ij.gui.Roi;

/**
 * Display results from selected masses for all rois in the MimsRoiManager
 * @author Douglas Benson
 * @author <a href="mailto:rob.gonzalez@gmail.com">Rob Gonzalez</a>
 */
public class Measure {

    /**
     * Creates a new instance of Measure.
     * @param ui MIMS UI for this Measure.
     */
    public Measure(UI ui) {
        rTable = new ij.measure.ResultsTable();
        this.ui = ui;
        reset();
        int n = ui.getMimsImage().nMasses();
        System.out.println("nMasses() -> " + n);
        for (int i = 0; i < n; i++) {
            bMass[i] = true;
        }
        bMeasure[ResultsTable.MEAN] = true;
        bMeasure[ResultsTable.AREA] = true;
        bMeasure[ResultsTable.STD_DEV] = true;

        for (int i = 0; i < bMeasurePerImage.length; i++) {
            bMeasurePerImage[i] = false;
        }
        bMeasurePerImage[ResultsTable.MEAN] = true;
        bMeasurePerImage[ResultsTable.STD_DEV] = true;
        bMeasurePerImage[ResultsTable.MODE] = true;
        bMeasurePerImage[ResultsTable.MIN] = true;
        bMeasurePerImage[ResultsTable.MAX] = true;
        bMeasurePerImage[ResultsTable.X_CENTER_OF_MASS] = true;
        bMeasurePerImage[ResultsTable.Y_CENTER_OF_MASS] = true;
        bMeasurePerImage[ResultsTable.MEDIAN] = true;
        bMeasurePerImage[ResultsTable.INTEGRATED_DENSITY] = true;       
    }

    public void reset() {
        //ij.WindowManager wm = ij.WindowManager.get
        java.lang.String title = "";
        java.awt.Frame tempframe = ij.WindowManager.getFrame(fileName);
        
        if(tempframe != null) {
            title = tempframe.getTitle();
            System.out.println("title found: "+title); 
        }
        rTable.reset();
        bHasLabels = false;
    }

    public ij.measure.ResultsTable getTable() {
        return rTable;
    }

    public void setMasses(boolean bUseMass[]) {
        for (int i = 0; i < ui.getMimsImage().nMasses() && i < bUseMass.length; i++) {
            bMass[i] = bUseMass[i];
        }
    }

    public void setName(String name) {
        if (name != null) {
            fileName = name;
            if (!fileName.endsWith(".txt")) {
                fileName += ".txt";
            }
        }
    }

    public void getDataOptions() {
        ij.gui.GenericDialog gd = new ij.gui.GenericDialog("Data Options");
        gd.setLayout(new java.awt.GridLayout(0, 4));
        for (int i = 0; i < measureNames.length; i++) {
            gd.addCheckbox(measureNames[i], bMeasure[i]);
        }

        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        for (int i = 0; i < measureNames.length; i++) {
            bMeasure[i] = gd.getNextBoolean();
        }
    }

    public void getSourceOptions() {

        ij.gui.GenericDialog gd = new ij.gui.GenericDialog("Source Images..");

        for (int i = 0; i < ui.getMimsImage().nMasses(); i++) {
            MimsPlus mp = ui.getMassImage(i);
            if (mp == null) {
                bMass[i] = false;
            } else {
                gd.addCheckbox(ui.getMimsImage().getMassName(i), bMass[i]);
            }
        }

        gd.addCheckbox("Open ratio images", bMeasureRatios);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        for (int i = 0; i < ui.getMimsImage().nMasses(); i++) {
            if (ui.getMassImage(i) != null) {
                bMass[i] = gd.getNextBoolean();
            } else {
                bMass[i] = false;
            }
        }

        bMeasureRatios = gd.getNextBoolean();
    }

    private MimsPlus[] getMeasureImages() {
        MimsPlus mp[] = ui.getMassImages();
        MimsPlus rp[] = ui.getOpenRatioImages();
        if (mp.length == 0) {
            return new MimsPlus[0];
        }
        int nTotal = 0;
        for (int i = 0; i < bMass.length && i < mp.length; i++) {
            if (bMass[i] && mp[i] != null) {
                nTotal++;
            } else {
                bMass[i] = false;
            }
        }
        if (bMeasureRatios) {
            nTotal += rp.length;
        }

        MimsPlus images[] = new MimsPlus[nTotal];

        int nImages = 0;
        for (int i = 0; i < bMass.length && i < mp.length; i++) {
            if (bMass[i] && mp[i] != null) {
                images[nImages++] = mp[i];
            }
        }
        for (int i = 0; bMeasureRatios && i < rp.length; i++) {
            images[nImages++] = rp[i];
        }

        return images;
    }

    private int addResults(ij.process.ImageStatistics is, int n, Roi roi, int nSlice, int ncol) {
        for (int i = 0; i < bMeasure.length; i++) {
            if (bMeasure[i]) {
                if (!bMeasurePerImage[i] && n > 0) {
                    // no results for ROI's for additional images
                } else {
                    switch (i) {
                        case ResultsTable.AREA:
                            rTable.addValue(ncol++, is.area);
                            break;
                        case ResultsTable.MEAN:
                            rTable.addValue(ncol++, is.mean);
                            break;
                        case ResultsTable.STD_DEV:
                            rTable.addValue(ncol++, is.stdDev);
                            break;
                        case ResultsTable.MODE:
                            rTable.addValue(ncol++, is.mode);
                            break;
                        case ResultsTable.MIN:
                            rTable.addValue(ncol++, is.min);
                            break;
                        case ResultsTable.MAX:
                            rTable.addValue(ncol++, is.max);
                            break;
                        case ResultsTable.X_CENTROID:
                            rTable.addValue(ncol++, is.xCentroid);
                            break;
                        case ResultsTable.Y_CENTROID:
                            rTable.addValue(ncol++, is.yCentroid);
                            break;
                        case ResultsTable.X_CENTER_OF_MASS:
                            rTable.addValue(ncol++, is.xCenterOfMass);
                            break;
                        case ResultsTable.Y_CENTER_OF_MASS:
                            rTable.addValue(ncol++, is.yCenterOfMass);
                            break;
                        case ResultsTable.ROI_X:
                            rTable.addValue(ncol++, is.roiX);
                            break;
                        case ResultsTable.ROI_Y:
                            rTable.addValue(ncol++, is.roiY);
                            break;
                        case ResultsTable.ROI_WIDTH:
                            rTable.addValue(ncol++, is.roiWidth);
                            break;
                        case ResultsTable.ROI_HEIGHT:
                            rTable.addValue(ncol++, is.roiHeight);
                            break;
                        case ResultsTable.MAJOR:
                            rTable.addValue(ncol++, is.major);
                            break;
                        case ResultsTable.MINOR:
                            rTable.addValue(ncol++, is.minor);
                            break;
                        case ResultsTable.ANGLE:
                            rTable.addValue(ncol++, is.angle);
                            break;
                        case ResultsTable.FERET:
                            rTable.addValue(ncol++, roi != null ? roi.getFeretsDiameter() : 0.0);
                            break;
                        case ResultsTable.INTEGRATED_DENSITY:
                            rTable.addValue(ncol++, is.pixelCount * is.mean);
                            break;
                        case ResultsTable.MEDIAN:
                            rTable.addValue(ncol++, is.median);
                            break;
                        case ResultsTable.SKEWNESS:
                            rTable.addValue(ncol++, is.skewness);
                            break;
                        case ResultsTable.KURTOSIS:
                            rTable.addValue(ncol++, is.kurtosis);
                            break;
                        case ResultsTable.AREA_FRACTION:
                            rTable.addValue(ncol++, is.areaFraction);
                            break;
                        case ResultsTable.SLICE:
                            rTable.addValue(ncol++, nSlice);
                            break;
                        case ResultsTable.PERIMETER:
                        case ResultsTable.CIRCULARITY:
                             {
                                double perimeter;
                                if (roi != null) {
                                    perimeter = roi.getLength();
                                } else {
                                    perimeter = 0.0;
                                }
                                if (i == ResultsTable.PERIMETER) {
                                    rTable.addValue(ncol++, perimeter);
                                } else {
                                    double circularity = perimeter == 0.0 ? 0.0 : 4.0 * Math.PI * (is.area / (perimeter * perimeter));
                                    if (circularity > 1.0) {
                                        circularity = -1.0;
                                    }
                                    rTable.addValue(ncol++, circularity);
                                }
                            }
                            break;
                    }
                }
            }
        }
        return ncol;
    }
    
    // This method creates a results table for rois
    // that are NOT synched across all slices.
    // The results table for Rois which are synched across
    // all slices of a mass image has a different format. Possibly merge later...
    public void generateRoiTable() {       
       
       // Get all open images.
       MimsPlus[] images = getMeasureImages();                 
       if (images.length == 0) 
          return;

       // Get the number of slices in stack.
       int nSlices = images[0].getNSlices();
       if (nSlices < 1) 
          return;            
        
       // Get the list of ROIs.
       MimsRoiManager rm = ui.getRoiManager();
       Roi[] rois;
       javax.swing.JList rlist = rm.getList();        
       if (rm.getROIs().isEmpty()) 
          rois = new Roi[0];           
       else {
          int length = rlist.getModel().getSize();
          rois = new Roi[length];
          for (int i = 0; i < length; i++) {
              rois[i] = (Roi) rm.getROIs().get(rlist.getModel().getElementAt(i).toString());
          }            
       }               
       
       // Generate column headings. We are hard coding the "Plane" column
       // because for this table, we want it first in the list.
       rTable.setHeading(0, "Series");
       int ncol = 1;
       for (int i = 0; i < images.length; i++) { //number of mass-hsi images.
          for (int m = 0; m < bMeasure.length; m++) { //boolean list of fields to compute.
             if (bMeasure[m]) {
                String hd = measureNames[m];
                if (bMeasurePerImage[m] == false && i > 0) {
                   //do nothing.
                } else if (bMeasurePerImage[m] == false && i == 0) {
                   rTable.setHeading(ncol++, hd);
                } else {
                   hd += "_";
                   //changed to retrive mass name, eg m12.09 instead of m1
                   //old
/*                   if (images[i].getMimsType() == MimsPlus.RATIO_IMAGE) {
                      hd += "m" + (images[i].getNumMass() + 1) + "/m" + (images[i].getDenMass() + 1);
                   } else {
                      hd += "m" + (images[i].getMimsMassIndex() + 1);
                   }
 */
                   hd += images[i].getShortTitle();
                   rTable.setHeading(ncol++, hd);                   
                }
             }
          }
       }                    
        
       // No idea what this is for...
       int mOptions = 0;
       for (int m = 0; m < bMeasure.length; m++) {
          if (bMeasure[m]) {
             mOptions |= (1 << m);
          }
       }         
       
       // Fill in table data.
       int nrois = rois.length;
       if (nrois == 0) 
          nrois = 1;        
       for (int n = 1; n <= nSlices; n++) {   
          for (int r = 0; r < nrois; r++) {
             if (ui.getRoiManager().getSliceNumber(rois[r].getName()) == n ||
                 nSlices == 1) {
             ncol = 0;
             rTable.incrementCounter();                     
             rTable.addValue(ncol++, n); 
             for (int i = 0; i < images.length; i++) {
                
                // Set the mass image to the current slice.
                if (images[i].getMimsType() == MimsPlus.MASS_IMAGE) {
                   images[i].setSlice(n);
                }
                
                // Set the rois to the images.
                if (rois.length > 0) {
                   images[i].setRoi(rois[r]);
                }
                               
                // Generate Roi statistics and put in table.            
                ImageStatistics is =
                        ij.process.ImageStatistics.getStatistics(
                        images[i].getProcessor(),
                        mOptions,
                        images[i].getCalibration());
                ncol = addResults(is, i, rois.length > 0 ? rois[r] : null, n, ncol);
             }             
             
             String slicelabel = images[0].getStack().getShortSliceLabel(nSlices);
             String filename = "";
             String label = "";
             String labelHeader = "";
             if (slicelabel !=  null) {
                slicelabel = images[0].getStack().getShortSliceLabel(n);
                filename = slicelabel.substring(slicelabel.indexOf(":")+1, slicelabel.length());
                labelHeader = "File name : Roi name";
                label = "\'"+filename+"\' : \'"+rois[r].getName()+"\'";
             } else {
                labelHeader = "Roi name";
                label = "\'"+rois[r].getName()+"\'";
             }
             rTable.addLabel(labelHeader, label);
             }
          }          
       }               
       
       // Done! Show table.
       rTable.show(fileName);
    }

    public void generateStackTable() {
       
        // Get all open images.
        MimsPlus[] images = getMeasureImages();                 
        if (images.length == 0) return;

        // Get the number of slices in stack.
        int nSlices = images[0].getNSlices();
        if (nSlices < 1) return;            
        
        // Get the list of ROIs.
        MimsRoiManager rm = ui.getRoiManager();
        Roi[] rois;
        javax.swing.JList rlist = rm.getList();        
        if (rm.getROIs().isEmpty()) rois = new Roi[0];           
        else {
           int length = rlist.getModel().getSize();
           rois = new Roi[length];
           for (int i = 0; i < length; i++) {
               rois[i] = (Roi) rm.getROIs().get(rlist.getModel().getElementAt(i).toString());
           }            
        }        

        // Generate column heading.
        int ncol = 0;
        int currentMaxColumns = 150;
        int nrois = rois.length;
        if (nrois == 0) nrois = 1;        
        for (int r = 1; r <= nrois; r++) { //number of rois.           
            for (int i = 0; i < images.length; i++) { //number of mass-hsi images.
                for (int m = 0; m < bMeasure.length; m++) { //boolean list of fields to compute.
                   if (bMeasure[m]) {
                      if (bMeasurePerImage[m] == false && i > 0) { //do nothing.
                      } else {
                         String hd = measureNames[m] + "_";
                         //changed to retrive mass name, eg m12.09 instead of m1
                        //old
/*                         if (images[i].getMimsType() == MimsPlus.RATIO_IMAGE) {
                            hd += "m" + (images[i].getNumMass() + 1) + "/m" + (images[i].getDenMass() + 1);
                         } else {
                            hd += "m" + (images[i].getMimsMassIndex() + 1);
                         }
*/
                         hd += images[i].getShortTitle();
                         if (nrois > 1) {
                            hd += "_r" + r;
                         }
                         if (ncol == currentMaxColumns - 2) {
                            rTable.addColumns();
                            currentMaxColumns = currentMaxColumns * 2;
                         }
                         rTable.setHeading(ncol++, hd);
                      }
                  }
               }
            }
        }

       // No idea what this is for...
       int mOptions = 0;
       for (int m = 0; m < bMeasure.length; m++) {
          if (bMeasure[m]) {
             mOptions |= (1 << m);
          }
       }

       // Fill in table data.
       for (int n = 0; n < nSlices; n++) {           
          ncol = 0;
          rTable.incrementCounter();
          for (int r = 0; r < nrois; r++) {            
             for (int i = 0; i < images.length; i++) {
                
                // Set the mass image to the current slice.
                if (images[i].getMimsType() == MimsPlus.MASS_IMAGE) {
                   images[i].setSlice(n + 1);
                }
                
                // Set the rois to the images.
                if (rois.length > 0) {
                   images[i].setRoi(rois[r]);
                }
                
                // Generate Roi statistics and put in table.
                ImageStatistics is =
                        ij.process.ImageStatistics.getStatistics(
                        images[i].getProcessor(),
                        mOptions,
                        images[i].getCalibration());
                ncol = addResults(is, i, rois.length > 0 ? rois[r] : null, n+1, ncol);
             }
          }
       }

       // Done! Show table.
       rTable.show(fileName);
    }
    
    ///God damn it
    public void measureSums() {
        MimsPlus[] mSumImages = ui.getOpenSumImages();
        
        if (mSumImages.length == 0) {
            return;
        }

        MimsRoiManager rm = ui.getRoiManager();
        Roi[] rois;
        javax.swing.JList rlist = rm.getList();

        if (!rm.getROIs().isEmpty()) {
            int length = rlist.getModel().getSize();
            rois = new Roi[length];
            for (int i = 0; i < length; i++) {
                rois[i] = (Roi) rm.getROIs().get(rlist.getModel().getElementAt(i).toString());
            }
        } else {
            rois = new Roi[0];
        }

        int nSlices = mSumImages[0].getImageStackSize();
        if (nSlices < 1) {
            nSlices = 1;
        }
        int ncol = 0;

        int nrois = rois.length;
        int currentMaxColumns = 150;

        if (nrois == 0) {
            nrois = 1;
        }

        //all of this just to generate column headers...
        for (int r = 1; r <= nrois; r++) {

            for (int m = 0; m < bMeasure.length; m++) {

                if (bMeasure[m]) {
                    String hd = measureNames[m];

                    if (nrois > 1) {
                        hd += "_r" + r;
                    }
                    if (ncol == currentMaxColumns - 2) {

                        rTable.addColumns();
                        currentMaxColumns = currentMaxColumns * 2;
                    }
                    rTable.setHeading(ncol++, hd);


                }
            }

        }
        //end headers

        int mOptions = 0;
        for (int m = 0; m < bMeasure.length; m++) {
            if (bMeasure[m]) {
                mOptions |= (1 << m);
            }
        }


        ncol = 0;
        rTable.incrementCounter();
        int start = rTable.getCounter();
        
        for (int i = 0; i < mSumImages.length; i++) {
            ncol = 0;
            for (int r = 0; r < nrois; r++) {
                if (rois.length > 0) {
                    mSumImages[i].setRoi(rois[r]);
                }
                if (mSumImages[i].getProcessor() != null) {
                    ImageStatistics is =
                            ij.process.ImageStatistics.getStatistics(
                            mSumImages[i].getProcessor(),
                            mOptions,
                            mSumImages[i].getCalibration());
                    ncol = addResults(is, 0, rois.length > 0 ? rois[r] : null, 1, ncol);
                }
            }
            rTable.incrementCounter();
        }
        int end = rTable.getCounter();
        System.out.println("end: "+end);
        //set row labels
        int i = mSumImages.length-1;
        for (int j = end-1; i >= 0; j--) {
            String label = ui.getMimsImage().getName()+" ";
            label += mSumImages[i].getTitle();
            
            rTable.setLabel(label, j-1);
            i--;
        }

        rTable.show(fileName);

    }

    public String getName() {
        return fileName;
    }
    private String[] measureNames = {"Area", "Mean", "StdDev", "Mode", "Min", "Max",
        "X", "Y", "XM", "YM", "Perim.", "BX", "BY", "Width", "Height", "Major", "Minor", "Angle",
        "Circ.", "Feret", "IntDen", "Median", "Skew", "Kurt", "%Area", "Slice"
    };
    private boolean bStack = true;
    private boolean bHasLabels = false;
    private boolean bMeasureRatios = true;
    //must fix should not be 8... was 6
    private boolean bMass[] = new boolean[8];
    private boolean bMeasure[] = new boolean[ij.measure.ResultsTable.SLICE + 1];
    private boolean bMeasurePerImage[] = new boolean[ij.measure.ResultsTable.SLICE + 1];
    private String fileName = "NRIMS.txt";
    private ij.measure.ResultsTable rTable = null;
    private UI ui = null;
}

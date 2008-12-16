/*
 * UI.java
 *
 * Created on May 1, 2006, 12:59 PM
 */
package com.nrims;

import com.nrims.data.MIMSFileFilter;
import com.nrims.data.Opener;
import com.nrims.data.FileDrop;
import ij.IJ;
import ij.gui.Roi;
import ij.Prefs;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
*
 * The main user interface of the NRIMS ImageJ plugin.
 * 
 * @author  Douglas Benson
 * @author <a href="mailto:rob.gonzalez@gmail.com">Rob Gonzalez</a>
 */
public class UI extends PlugInJFrame implements WindowListener, MimsUpdateListener {

    public static final long serialVersionUID = 1;
    //more masses
    private int maxMasses = 8;
    
    /**
     * Whether we're running the UI in debug mode.
     */
    private boolean bDebug = false;
    private ij.ImageJ ijapp = null;
    private boolean bSyncStack = true;
    private boolean bSyncROIs = true;
    private boolean bAddROIs = true;
    private boolean bUpdating = false;
    /**
     * Flag that indicates whether images are currently being opened.
     */
    private boolean currentlyOpeningImages = false;
    private boolean bCloseOldWindows = true;
    private MimsRoiManager roiManager = null;
    private com.nrims.data.Opener image = null;
    /**
     * The folder from which the last image was loaded.
     */
    private String lastFolder = null;
    private boolean[] bOpenMass = new boolean[maxMasses];
    private com.nrims.MimsPlus[] massImages = new com.nrims.MimsPlus[maxMasses];
    private com.nrims.MimsPlus[] ratioImages = new com.nrims.MimsPlus[maxMasses];
    private com.nrims.MimsPlus[] hsiImages = new com.nrims.MimsPlus[maxMasses];
    private com.nrims.MimsPlus[] segImages = new com.nrims.MimsPlus[maxMasses];
    private com.nrims.MimsPlus[] sumImages = new com.nrims.MimsPlus[2*maxMasses];
    private com.nrims.MimsData mimsData = null;
    private com.nrims.MimsLog mimsLog = null;
    private com.nrims.MimsRoiControl roiControl = null;
    private com.nrims.HSIView hsiControl = null;
    private com.nrims.MimsStackEditing mimsStackEditing = null;
    private com.nrims.MimsTomography mimsTomography = null;
    protected com.nrims.MimsAction mimsAction = null;
    private com.nrims.SegmentationForm segmentation = null;
    protected ij.gui.Roi activeRoi;
    private int ratioScaleFactor = 10000;
    
    //tesing fixed contrast
    private boolean fixRatioContrast = true;
    
    private com.nrims.data.FileDrop mimsDrop;
    
    /**
     * Creates new form UI
     * @param fileName name of the .im image file to be opened.
     */
    public UI(String fileName) {
        super("NRIMS Analysis Module");
        
        System.out.println("Ui constructor");
        
        // Set look and feel to native OS
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            IJ.log("Error setting native Look and Feel:\n" + e.toString());
        }

        initComponents();

        ijapp = IJ.getInstance();
        if (ijapp == null || (ijapp != null && !ijapp.isShowing())) {
            ijapp = new ij.ImageJ(null);
            setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        }

        if (image == null) {
            for (int i = 0; i < maxMasses; i++) {
                massImages[i] = null;
                ratioImages[i] = null;
                hsiImages[i] = null;
                segImages[i] = null;
            }
        }

        int xloc, yloc = 150;
        if (ijapp != null) {
            xloc = ijapp.getX();
            if (xloc + ijapp.getWidth() + this.getPreferredSize().width + 10 < Toolkit.getDefaultToolkit().getScreenSize().width) {
                xloc += ijapp.getWidth() + 10;
                yloc = ijapp.getY();
            } else {
                yloc = ijapp.getY() + ijapp.getHeight() + 10;
            }
        } else {
            int screenwidth = Toolkit.getDefaultToolkit().getScreenSize().width;
            xloc = (int) (screenwidth > 832 ? screenwidth * 0.8 : screenwidth * 0.9);
            xloc -= this.getPreferredSize().width + 10;
        }

        this.setLocation(xloc, yloc);
        ij.WindowManager.addWindow(this);

        if (bDebug) {
            IJ.log("open UI ok...");
        }
        IJ.showProgress(1.0);
        
        this.mimsDrop = new FileDrop(null, this.mainTextField, /*dragBorder,*/ new FileDrop.Listener() {

            public void filesDropped(java.io.File[] files) {
                try {
                    for (int i = 0; i < files.length; i++) {
                        System.out.println("Dropped: " + files[i].getCanonicalPath());
                    }   // end for: through each dropped file
                    String lastfile = files[files.length-1].getCanonicalPath();
                    if (lastfile.endsWith(".im")) {
                        System.out.println("Opening last file: " + lastfile);
                        loadMIMSFile(lastfile);
                        //todo: mimsLog not cleared
                    }
                } // end try
                catch (java.io.IOException e) {
                }
            }   // end filesDropped
        }); // end FileDrop.Listener

        
        //??? todo: add if to open file chooser or not based of preference setting
        this.loadMIMSFile();

    }

    /**
     * Closes the current image and its associated set of windows if the mode is set to close open windows.
     */
    private synchronized void closeCurrentImage() {
        // TODO why is this 6? Changed to maxMasses
        for (int i = 0; i < maxMasses; i++) {
            if (segImages[i] != null) {
                segImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (segImages[i].getWindow() != null) {
                        segImages[i].getWindow().close();
                    }
                }
            }
            if (massImages[i] != null) {
                massImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (massImages[i].getWindow() != null) {
                        massImages[i].getWindow().close();
                    }
                }
            }
            bOpenMass[i] = false;
            if (hsiImages[i] != null) {
                hsiImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (hsiImages[i].getWindow() != null) {
                        hsiImages[i].getWindow().close();
                    }
                }
            }
            if (ratioImages[i] != null) {
                ratioImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (ratioImages[i].getWindow() != null) {
                        ratioImages[i].getWindow().close();
                    }
                }
            }
        }
        
        for (int i = 0; i < maxMasses*2; i++) {
            if (sumImages[i] != null) {
                sumImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (sumImages[i].getWindow() != null) {
                        sumImages[i].getWindow().close();
                    }
                }
            }
        }
        // FIXME: todo: the current opener is not dispose anywhere, so there are likely dangling file handles.
    }

    /**
     * Causes a dialog to open that allows a user to choose an image file.
     */
    private synchronized void loadMIMSFile() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        MIMSFileFilter filter = new MIMSFileFilter();
        fc.setFileFilter(filter);
        if (lastFolder != null) {
            fc.setCurrentDirectory(new java.io.File(lastFolder));
        }
        if (fc.showOpenDialog(this) == JFileChooser.CANCEL_OPTION) {
            return;
        }
        lastFolder = fc.getSelectedFile().getParent();
        String fileName = fc.getSelectedFile().getPath();
        try {
            loadMIMSFile(fileName);
        } catch (NullPointerException e) {
            System.err.println("A NullPointerException should not have occurred in loadMIMSFile.  This indicates that the fileName returned by the JFileChooser was null.");
        }
    }

    /**
     * Open a MIMS image file
     * @param fileName MIMS image file to open.
     * @throws NullPointerException if the given fileName is null or empty.
     */
    private synchronized void loadMIMSFile(String fileName) throws NullPointerException {
        if (fileName == null || fileName.length() == 0) {
            throw new NullPointerException("fileName cannot be null or empty when attempting to loadMIMSFile.");
        }

        try {
            currentlyOpeningImages = true;

            closeCurrentImage();

            try {
                image = new Opener(this, fileName);
            } catch (IOException e) {
                IJ.log("Failed to open " + fileName + ":\n" + e.getStackTrace());
                return;
            }
            
            int nMasses = image.nMasses();
            int nImages = image.nImages();

            long memRequired = nMasses * image.getWidth() * image.getHeight() * 2 * nImages;
            long maxMemory = IJ.maxMemory();
            
            for (int i = 0; i < nMasses; i++) {
                bOpenMass[i] = true;
            }
            while (memRequired > maxMemory) {
                ij.gui.GenericDialog gd = new ij.gui.GenericDialog("File Too Large");
                long aMem = memRequired;
                int canOpen = nImages;

                while (aMem > maxMemory) {
                    canOpen--;
                    aMem = nMasses * image.getWidth() * image.getHeight() * 2 * canOpen;
                }

                for (int i = 0; i < image.nMasses(); i++) {
                    String msg = "Open mass " + image.getMassName(i);
                    gd.addCheckbox(msg, bOpenMass[i]);
                }
                gd.addNumericField("Open only ", (double) canOpen, 0, 5, " of " + image.nImages() + " Images");

                gd.showDialog();
                if (gd.wasCanceled()) {
                    image = null;
                    return;
                }

                nMasses = 0;
                for (int i = 0; i < image.nMasses(); i++) {
                    bOpenMass[i] = gd.getNextBoolean();
                    if (bOpenMass[i]) {
                        nMasses++;
                    }
                }

                nImages = (int) gd.getNextNumber();

                memRequired = nMasses * image.getWidth() * image.getHeight() * 2 * nImages;
            }



            updateStatus("Opening " + fileName + " " + nMasses + " masses " + nImages + " sections");

            try {
                int n = 0;
                int t = image.nMasses() * nImages;
                for (int i = 0; i < image.nMasses(); i++) {
                    IJ.showProgress(++n, t);
                    if (bOpenMass[i]) {
                        MimsPlus mp = new MimsPlus(image, i);
                        mp.setAllowClose(false);
                        massImages[i] = mp;
                        double dMin = (double) image.getMin(i);
                        double dMax = (double) image.getMax(i);
                        if (mp != null) {
                            massImages[i].getProcessor().setMinAndMax(dMin, dMax);
                            massImages[i].getProcessor().setPixels(image.getPixels(i));
                        }
                    }
                }

                if (nImages > 1) {
                    // TODO why are we starting from 1 here?
                    for (int i = 1; i < nImages; i++) {
                        image.setStackIndex(i);
                        for (int mass = 0; mass < image.nMasses(); mass++) {
                            IJ.showProgress(++n, t);
                            if (bOpenMass[mass]) {
                                massImages[mass].appendImage(i);
                            }
                        }
                    }
                }

                for (int i = 0; i < image.nMasses(); i++) {
                    if (bOpenMass[i]) {
                        if (image.nImages() > 1) {
                            massImages[i].setSlice(1);
                        }
                        massImages[i].show();
                    }
                }
                ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
                wo.run("tile");

            } catch (Exception x) {
                updateStatus(x.toString());
                x.printStackTrace();
            }

            for (int i = 0; i < image.nMasses(); i++) {
                if (bOpenMass[i]) {
                    massImages[i].addListener(this);
                } else {
                    massImages[i] = null;
                }
            }

            if (mimsData == null) {
                mimsData = new com.nrims.MimsData(this, image);
                roiControl = new MimsRoiControl(this);
                hsiControl = new HSIView(this);

                mimsLog = new MimsLog(this, image);
                mimsStackEditing = new MimsStackEditing(this, image);
                mimsTomography = new MimsTomography(this, image);
                mimsAction = new MimsAction(this, image);
                segmentation = new SegmentationForm(this);
                
                //mimsLog.Log("\n\nNew image: " + image.getName() + "\n" + getImageHeader(image));
                
                jTabbedPane1.setComponentAt(0, mimsData);
                jTabbedPane1.setTitleAt(0, "MIMS Data");
                jTabbedPane1.add("Process", hsiControl);
                jTabbedPane1.add("Analysis", roiControl);
                jTabbedPane1.add("Stack Editing", mimsStackEditing);
                jTabbedPane1.add("Tomography", mimsTomography);
                jTabbedPane1.add("Segmentation", segmentation);
                jTabbedPane1.add("MIMS Log", mimsLog);

            } else {

                ///Added to solve restore MIMS problem (PG)   START
                mimsData = new com.nrims.MimsData(this, image);
                roiControl = new MimsRoiControl(this);
                hsiControl = new HSIView(this);
                //Commented out so mimsLog is persistent...
                //mimsLog = new MimsLog(this, image);
                mimsStackEditing = new MimsStackEditing(this, image);
                mimsTomography = new MimsTomography(this, image);
                mimsAction = new MimsAction(this, image);
                segmentation = new SegmentationForm(this);
                //    jTabbedPane1.removeAll();
                jTabbedPane1.setComponentAt(0, mimsData);
                jTabbedPane1.setTitleAt(0, "MIMS Data");
                jTabbedPane1.setComponentAt(1, hsiControl);
                jTabbedPane1.setComponentAt(2, roiControl);
                jTabbedPane1.setComponentAt(3, mimsStackEditing);
                jTabbedPane1.setComponentAt(4, mimsTomography);
                jTabbedPane1.setComponentAt(5, segmentation);



                //   jTabbedPane1.add("Process",hsiControl);
                //   jTabbedPane1.add("Analysis", roiControl);
                //  jTabbedPane1.add("Stack Editing", mimsStackEditing);
                //    jTabbedPane1.add("Tomography", mimsTomography);
                // jTabbedPane1.add("MIMS Log", mimsLog);
                ///Added to solve restore MIMS problem (PG)   END

                mimsData.setMimsImage(image);
                hsiControl.updateImage();
            }

            //jTabbedPane1.addChangeListener(new java.awt.ChangeListener());

            this.mimsLog.Log("\n\nNew image: " + image.getName() + "\n" + getImageHeader(image));
            this.mimsTomography.resetBounds();
            this.mimsTomography.resetImageNamesList();
            this.mimsStackEditing.resetSpinners();
            this.mimsStackEditing.setConcatGUI(false); //enables buttons in Stack Editing again
        } finally {
            currentlyOpeningImages = false;
        }
    }

    /**
     * @param props
     * @return -1 if the index doesn't exist or a value between on and MAX_RATIO_IMAGES if it does.
     */
    public int getRatioImageIndex(HSIProps props) {
        if (props == null) {
            return -1;
        }
        return getRatioImageIndex(props.getNumMass(), props.getDenMass());
    }

    public int getRatioImageIndex(int numIndex, int denIndex) {
        for (int i = 0; i < maxMasses; i++) {
            if (ratioImages[i] != null) {
                if (ratioImages[i].getNumMass() == numIndex && ratioImages[i].getDenMass() == denIndex) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void openSeg(int[] segImage, String description, int segImageHeight, int segImageWidth) {
//        int[] segImage = new int[512*512];
//        String  segPath ="/nrims/home3/pgormanns/segi";
//        try {
//            //open filechooser
//        
//            //method call for readFile
//            BufferedReader bfrSeg = new BufferedReader(new FileReader(new File(segPath)));
//            try {
//                String currentLine = bfrSeg.readLine();
//               
//                int currentPixel = 0 ; 
//                while (currentLine != null){
//                    segImage[currentPixel] = new Integer(currentLine);
//                    currentPixel++;
//                    currentLine = bfrSeg.readLine();
//                    
//                }
//            } catch (IOException ex) {
//                Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
//        }

        // call colorer 

//        String segImageName = segPath ;
        //BufferedReader bfrSeg = new BufferedReader(new FileReader(new File(segPath)));


        MimsPlus mp = new MimsPlus(this, segImageWidth, segImageHeight, segImage, description);
        mp.setHSIProcessor(new HSIProcessor(mp));
        boolean bShow = mp == null;
        // find a slot to save it
        boolean bFound = false;


        bFound = true;
        segImages[0] = mp;
        int segIndex = 0;

        if (!bFound) {
            segIndex = 5;
            segImages[segIndex] = mp;
        }

        mp.addListener(this);
        bShow = true;
        if (bShow) {
            while (mp.getHSIProcessor().isRunning()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException x) {
                }
            }
            mp.show();
//            jMenuItem2ActionPerformed(null);    //tile screws up plots          
        }
    }

    public synchronized boolean computeRatio(HSIProps props) {
        int i;

        int numIndex = props.getNumMass();
        int denIndex = props.getDenMass();

        int ratioIndex = getRatioImageIndex(numIndex, denIndex);

        MimsPlus num = massImages[numIndex];
        
        if (num == null) {
            updateStatus("Error no numerator");
            return false;
        }
        MimsPlus den = massImages[denIndex];
        if (den == null) {
            updateStatus("Error no denominator");
            return false;
        }

        if (num.getBitDepth() != 16) {
            updateStatus("Error numerator not 16 bits");
            return false;
        }
        if (den.getBitDepth() != 16) {
            updateStatus("Error denominator not 16 bits");
            return false;
        }

        MimsPlus mp = null;
        if (ratioIndex >= 0) {
            mp = ratioImages[ratioIndex];
        }
        boolean bAddSlice = false;
        boolean bShow = false;
        if (mp != null) {
            if (mp.getNSlices() > 1) {
                mp = null;
                bAddSlice = true;
            }
        }

        if (mp == null) {
            mp = new MimsPlus(image, props, false);
            if (bAddSlice) {
                ij.ImageStack stack = ratioImages[ratioIndex].getStack();
                stack.addSlice(null, mp.getProcessor(), num.getCurrentSlice());
                ratioImages[ratioIndex].setStack(null, stack);
                ratioImages[ratioIndex].setSlice(num.getCurrentSlice());
            } else {
                // find a slot to save it
                boolean bFound = false;
                for (i = 0; i < maxMasses && !bFound; i++) {
                    if (ratioImages[i] == null) {
                        bFound = true;
                        ratioImages[i] = mp;
                        ratioIndex = i;
                    }
                }
                if (!bFound) {
                    ratioIndex = 5;
                    ratioImages[ratioIndex] = mp;
                }

                mp.addListener(this);
                bShow = true;
            }
        }

        if (mp == null) {
            updateStatus("Error allocating ratio image");
            return false;
        }


        float[] rPixels = (float[]) mp.getProcessor().getPixels();
        short[] nPixels = (short[]) num.getProcessor().getPixels();
        short[] dPixels = (short[]) den.getProcessor().getPixels();

        int nt = props.getMinNum();
        int dt = props.getMinDen();
        float rMax = 0.0f;
        float rMin = 1000000.0f;
        for (i = 0; i < rPixels.length; i++) {
            if (nPixels[i] >= 0 && dPixels[i] > 0) {
                rPixels[i] = ratioScaleFactor *((float) nPixels[i] / (float) dPixels[i]);
                if (rPixels[i] > rMax) {
                    rMax = rPixels[i];
                } else if (rPixels[i] < rMin) {
                    rMin = rPixels[i];
                }
            } else {
                rPixels[i] = 0.0f;
            }

        }


        /*
        ij.process.ImageStatistics imgStats = ratioImages[ratioIndex].getStatistics();
        
        props.setMinRatio(java.lang.Math.max(0.0, imgStats.mean-(2*imgStats.stdDev)));
        props.setMaxRatio(imgStats.mean+(imgStats.stdDev));
         */


        mp.getProcessor().setMinAndMax(props.getMinRatio(), props.getMaxRatio());
        //mp.getProcessor().setMinAndMax(0.001,0.4);

        //System.out.println("rMin: "+rMin+" rMax: "+rMax);

        if (bShow) {
            mp.show();
            mp.updateAndDraw();
            //System.out.println("calibrated:  "+mp.getCalibration().calibrated());
            ij.measure.Calibration cal = new ij.measure.Calibration(mp);
            //cal.setFunction(dt, arg1, title)
            //cal.setFunction(ij.measure.Calibration.STRAIGHT_LINE, [0.0,0.05], "");
            mp.setCalibration(cal);

        //mp.updateAndRepaintWindow();  //changed from updateAndDraw
        //jMenuItem2ActionPerformed(null);    //tile
        } else {
            //System.out.println("calibrated:  "+mp.getCalibration().calibrated());
            mp.updateAndRepaintWindow();
        }

        return true;
    }
    
    public static String getImageHeader(Opener im) {
        String str = "\nHeader: \n";
        str += "Path: " + im.getImageFile().getAbsolutePath() + "/" + im.getName() + "\n";

        str += "Masses: ";
        for (int i = 0; i < im.nMasses(); i++) {
            str += im.getMassName(i) + " ";
        }
        str += "\n";
        str += "Pixels: " + im.getWidth() + "x" + im.getHeight() + "\n";
        str += "Duration: " + im.getDuration() + "\n";
        str += "Dwell time: " + im.getDwellTime() + "\n";
        str += "Position: " + im.getPosition() + "\n";
        str += "Sample name: " + im.getSampleName() + "\n";
        str += "Sample date: " + im.getSampleDate() + "\n";
        str += "Sample hour: " + im.getSampleHour() + "\n";
        str += "Pixel width: " + im.getPixelWidth() + "\n";
        str += "Pixel height: " + im.getPixelHeight() + "\n";
        str += "End header.\n\n";
        return str;
    }

    public synchronized boolean computeSum(MimsPlus mImage) {
        boolean fail = true;
        
        if(mImage==null) {return false;}
        
        int width = mImage.getWidth();
        int height = mImage.getHeight();
        String sumName = "Sum : "+mImage.getShortTitle();
        int templength = mImage.getProcessor().getPixelCount();
        double[] sumPixels = new double[templength];
        short[] tempPixels = new short[templength];
                
        int startSlice = mImage.getSlice();
        
        if(mImage.getMimsType()==MimsPlus.MASS_IMAGE) {
            for(int i=1; i<=mImage.getImageStackSize(); i++) {
                mImage.setSlice(i);
                tempPixels = (short[]) mImage.getProcessor().getPixels();
                for(int j=0; j<sumPixels.length; j++) { 
                    sumPixels[j]+=tempPixels[j]; 
                }
            }
            mImage.setSlice(startSlice);
            fail = false;
        }
        
        if(mImage.getMimsType()==MimsPlus.RATIO_IMAGE) {
            int numMass = mImage.getNumMass();
            int denMass = mImage.getDenMass();
            MimsPlus nImage = massImages[numMass];
            MimsPlus dImage = massImages[denMass];
            double[] numPixels = new double[templength];
            double[] denPixels = new double[templength];
            
            startSlice = nImage.getSlice();
            
            for(int i=1; i<=nImage.getImageStackSize(); i++) {
                nImage.setSlice(i);
                tempPixels = (short[]) nImage.getProcessor().getPixels();
                for(int j=0; j<numPixels.length; j++) { 
                    numPixels[j]+=tempPixels[j]; 
                }
            }
            for(int i=1; i<=dImage.getImageStackSize(); i++) {
                dImage.setSlice(i);
                tempPixels = (short[]) dImage.getProcessor().getPixels();
                for(int j=0; j<denPixels.length; j++) { 
                    denPixels[j]+=tempPixels[j]; 
                }
            }
            for(int i=0; i<sumPixels.length; i++) {
                if(denPixels[i]!=0) {
                    sumPixels[i] = ratioScaleFactor*(numPixels[i]/denPixels[i]);
                }else {
                    sumPixels[i] = 0;
                }
            }
            
            nImage.setSlice(startSlice);
            
            fail = false;
        }
        
        if(!fail) {
            MimsPlus mp = new MimsPlus(this, width, height, sumPixels, sumName);
            
            boolean bShow = (mp == null);
            // ??????
            // find a slot to save it

            boolean bFound = false;
            for (int i = 0; i < maxMasses*2 && !bFound; i++) {
                if (sumImages[i] == null) {
                    bFound = true;
                    sumImages[i] = mp;
                }
            }
            //why overwrite the last slot?
            if (!bFound) {
                sumImages[(maxMasses*2)-1] = mp;
            }
            
            mp.addListener(this);
            mp.show();
            if(mImage.getMimsType()==MimsPlus.RATIO_IMAGE){this.autocontrast(mp);}
            mp.updateAndDraw();
            
            bShow = true; 
            return true;
        } else {
            return false;
        }
    }
    
    public int getHSIImageIndex(HSIProps props) {
        if (props == null) {
            return -1;
        }
        int numIndex = props.getNumMass();
        int denIndex = props.getDenMass();
        for (int i = 0; i < maxMasses; i++) {
            if (hsiImages[i] != null) {
                if (hsiImages[i].getNumMass() == numIndex && hsiImages[i].getDenMass() == denIndex) {
                    return i;
                }
            }
        }
        return -1;
    }

    public synchronized boolean computeHSI(HSIProps props) {
        int i;
        int hsiIndex = getHSIImageIndex(props);
        MimsPlus num = massImages[props.getNumMass()];
        if (num == null) {
            updateStatus("Error no numerator");
            return false;
        }
        MimsPlus den = massImages[props.getDenMass()];
        if (den == null) {
            updateStatus("Error no denominator");
            return false;
        }

        if (num.getBitDepth() != 16) {
            updateStatus("Error numerator not 16 bits");
            return false;
        }
        if (den.getBitDepth() != 16) {
            updateStatus("Error denominator not 16 bits");
            return false;
        }

        int numIndex = props.getNumMass();
        int denIndex = props.getDenMass();

        MimsPlus mp = null;
        if (hsiIndex != -1) {
            mp = hsiImages[hsiIndex];
        }
        boolean bShow = mp == null;

        if (!bShow) {
            HSIProps oldProps = mp.getHSIProcessor().getProps();
            int height = mp.getHeight();
            if (oldProps.getLabelMethod() == 0 && props.getLabelMethod() > 0) {
                height = image.getHeight() + 16;
            } else if (oldProps.getLabelMethod() > 0 && props.getLabelMethod() == 0) {
                height = image.getHeight();
            }
            if (height != mp.getHeight()) {
                mp.setSize(mp.getWidth(), height);
                bShow = true;
            }
        }

        if (mp == null) {
            mp = new MimsPlus(image, props, true);
            mp.setHSIProcessor(new HSIProcessor(mp));
            // find a slot to save it
            boolean bFound = false;
            for (i = 0; i < maxMasses && !bFound; i++) {
                if (hsiImages[i] == null) {
                    bFound = true;
                    hsiImages[i] = mp;
                    hsiIndex = i;
                }
            }
            if (!bFound) {
                hsiIndex = 5;
                hsiImages[hsiIndex] = mp;
            }

            mp.addListener(this);
        }

        if (mp == null) {
            updateStatus("Error allocating ratio image");
            return false;
        }

        updateStatus("Computing HSI image..");

        try {
            mp.getHSIProcessor().setProps(props);
        } catch (Exception x) {
            IJ.log(x.toString());
            updateStatus("Failed computing HSI image");
        }

        if (bShow) {
            while (mp.getHSIProcessor().isRunning()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException x) {
                }
            }
            mp.show();
        //jMenuItem2ActionPerformed(null);    //tile screws up plots, why call menu?        
        }

        updateStatus("Ready");

        return true;
    }

    /**
     * Catch events such as changing the slice number of a stack
     * or drawing ROIs and if enabled,  update or synchronize all images
     * @param evt
     */
    @Override
    public synchronized void mimsStateChanged(MimsPlusEvent evt) {

        // do not call updateStatus() here - causes a race condition..

        if (currentlyOpeningImages || bUpdating) {
            return;
        }
        bUpdating = true; // Stop recursion 

        /* sychronize Stack displays */
        if (bSyncStack && evt.getAttribute() == MimsPlusEvent.ATTR_UPDATE_SLICE && image.nImages() > 1) {
            MimsPlus mp = (MimsPlus) evt.getSource();
            MimsPlus rp[] = this.getOpenRatioImages();
            MimsPlus hsi[] = this.getOpenHSIImages();



            ///
            //Testing fixed contrast
            ///
            //double minThresh = 0.0;
            //double maxThresh = 0.0;
            //if(rp[0]!=null) {
            //double minThresh = rp[0].getProcessor().getMinThreshold();
            //double maxThresh = rp[0].getProcessor().getMaxThreshold();
            //System.out.println("minThresh: "+minThresh+" maxThresh: "+maxThresh);
            //}
            ///

            for (int i = 0; i < rp.length; i++) {
                rp[i].updateAndRepaintWindow();
            }
            for (int i = 0; i < hsi.length; i++) {
                hsi[i].updateAndRepaintWindow();
            }
            mp.updateAndRepaintWindow();

            //Doesn't work right...
            //System.out.println("repaint?");
            //System.out.println("lengths: "+rp.length+" "+hsi.length);



            int nSlice = evt.getSlice();
            for (int i = 0; i < image.nMasses(); i++) {
                if ((massImages[i] != mp) && (massImages[i] != null) && bOpenMass[i]) {
                    massImages[i].setSlice(nSlice);
                }
            }
            for (int i = 0; i < maxMasses; i++) {
                if ((hsiImages[i] != mp) && (hsiImages[i] != null)) {
                    if (hsiImages[i].isStack()) {
                        hsiImages[i].setSlice(evt.getSlice());
                    } else {
                        computeHSI(hsiImages[i].getHSIProps());
                    }
                }
                if ((ratioImages[i] != mp) && (ratioImages[i] != null)) {

                    HSIProps fixedProps = ratioImages[i].getHSIProps();
                    double minThresh = 0.0;
                    double maxThresh = 0.0;
                    //fixedProps.setMinRatio(0.01);
                    //fixedProps.setMaxRatio(0.1);

                    fixRatioContrast = this.hsiControl.ratioIsFixed();

                    ij.process.ImageStatistics imgStats = null;

                    if (ratioImages[i].isStack()) {
                        ratioImages[i].setSlice(evt.getSlice());
                    } else //System.out.println("fixRatioContrast: "+fixRatioContrast);
                    if (!fixRatioContrast && i == hsiControl.whichRatio()) {

                        imgStats = ratioImages[i].getStatistics();
                        HSIProps props = ratioImages[i].getHSIProps();

                        //System.out.println("min: " + imgStats.min + " max: " + imgStats.max + " mean: " + imgStats.mean + " sd: " + imgStats.stdDev);

                        props.setMinRatio(java.lang.Math.max(0.0, imgStats.mean - (2 * imgStats.stdDev)));
                        props.setMaxRatio(imgStats.mean + (imgStats.stdDev));

                        computeRatio(props);
                        rp[i].getProcessor().setMinAndMax(props.getMinRatio(), props.getMaxRatio());

                        //System.out.println("minratio: "+fixedProps.getMinRatio()+" maxratio: "+fixedProps.getMaxRatio());
                        this.hsiControl.resetRatioSpinners(props);

                    }
                    if (fixRatioContrast || i != hsiControl.whichRatio()) {
                        //rp[i].getProcessor().setMinAndMax(0.0, 0.1);

                        //ratioImages[i].getHSIProps().setProps(fixedProps);
                        //System.out.println("set to fixedProps");
                        //fixedProps.setMinRatio(0.0);
                        //fixedProps.setMaxRatio(1.0);

                        //rp[i].getProcessor().setMinAndMax((double)0.0, (double)0.06);

                        //minThresh = rp[i].getProcessor().getMinThreshold();
                        //maxThresh = rp[i].getProcessor().getMaxThreshold();
                        //System.out.println("minThresh: "+minThresh+" maxThresh: "+maxThresh);

                        //System.out.println("minratio: "+fixedProps.getMinRatio()+" maxratio: "+fixedProps.getMaxRatio());
                        fixedProps.setMinRatio(this.hsiControl.getRatioMinVal());
                        fixedProps.setMaxRatio(this.hsiControl.getRatioMaxVal());

                        computeRatio(fixedProps);
                        rp[i].getProcessor().setMinAndMax(fixedProps.getMinRatio(), fixedProps.getMaxRatio());
                    //this.hsiControl.resetRatioSpinners(fixedProps);
                    }
                }
            }

        ///
        //Testing fixed contrast
        ///
        //if(rp[0]!=null) {
        //    rp[0].getProcessor().setMinAndMax(minThresh, maxThresh);

        //}
        ///
        } else if (evt.getAttribute() == MimsPlusEvent.ATTR_IMAGE_CLOSED) {
            /* If an image was closed by a window event,
             * dispose the corresponding reference
             */
            boolean bNotFound = true;
            int i;
            for (i = 0; bNotFound && i < image.nMasses(); i++) {
                if (massImages[i] == evt.getSource()) {
                    massImages[i].removeListener(this);
                    massImages[i] = null;
                    bNotFound = false;
                }
            }
            for (i = 0; bNotFound && i < maxMasses; i++) {
                if (hsiImages[i] == evt.getSource()) {
                    hsiImages[i].removeListener(this);
                    hsiImages[i] = null;
                    bNotFound = false;
                } else if (ratioImages[i] == evt.getSource()) {
                    ratioImages[i].removeListener(this);
                    ratioImages[i] = null;
                    bNotFound = false;
                }
            }
        } else if (evt.getAttribute() == MimsPlusEvent.ATTR_SET_ROI || evt.getAttribute() == MimsPlusEvent.ATTR_MOUSE_RELEASE) {
            /* Update all images with a selected ROI 
             * MOUSE_RELEASE catches drawing new ROIs
             */
            if (bSyncROIs) {
                int i;
                MimsPlus mp = (MimsPlus) evt.getSource();
                for (i = 0; i < image.nMasses(); i++) {
                    if (massImages[i] != mp && massImages[i] != null && bOpenMass[i]) {
                        massImages[i].setRoi(evt.getRoi());
//                        ij.WindowManager.setTempCurrentImage(ratioImages[i]);
                    }
                }
                for (i = 0; i < hsiImages.length; i++) {
                    if (hsiImages[i] != mp && hsiImages[i] != null) {
                        hsiImages[i].setRoi(evt.getRoi());
//                        ij.WindowManager.setTempCurrentImage(ratioImages[i]);
                    }
                }
                for (i = 0; i < ratioImages.length; i++) {
                    if (ratioImages[i] != mp && ratioImages[i] != null) {
                        ratioImages[i].setRoi(evt.getRoi());
//                        ij.WindowManager.setTempCurrentImage(ratioImages[i]);
                    }
                }
                for (i = 0; i < segImages.length; i++) {
                    if (segImages[i] != mp && segImages[i] != null) {
                        segImages[i].setRoi(evt.getRoi());
//                        ij.WindowManager.setTempCurrentImage(ratioImages[i]);
                    }
                }
            }
            /* Automatically appends a drawn ROI to the RoiManager
             * to improve work flow without extra mouse actions
             */
            if (bAddROIs && evt.getAttribute() == MimsPlusEvent.ATTR_MOUSE_RELEASE) {
                ij.gui.Roi roi = evt.getRoi();
                if (roi != null && roi.getState() != Roi.CONSTRUCTING) {
                    MimsRoiManager rm = getRoiManager();
                    rm.runCommand("add");
                    rm.showFrame();
                }
            }

        }

        bUpdating = false;

        //had to wait untill not changing....
        //System.out.println("mims state changed...");
        this.mimsStackEditing.resetTrueIndexLabel();
        this.mimsStackEditing.resetSpinners();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        mainTextField = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openNewMenuItem = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        aboutMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        utilitiesMenu = new javax.swing.JMenu();
        sumAllMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("NRIMS Analysis Module");
        setName("NRIMSUI"); // NOI18N

        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        jPanel1.setName("Images"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 637, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 347, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Images", jPanel1);

        mainTextField.setEditable(false);
        mainTextField.setText("Ready");
        mainTextField.setToolTipText("Status");

        fileMenu.setText("File");

        openNewMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openNewMenuItem.setMnemonic('o');
        openNewMenuItem.setText("Open MIMS Image");
        openNewMenuItem.setToolTipText("Open a MIMS image from an existing .im file.");
        openNewMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMIMSImageMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openNewMenuItem);
        openNewMenuItem.getAccessibleContext().setAccessibleDescription("Open a MIMS Image");

        jMenuItem5.setText("Open Action File");
        jMenuItem5.setToolTipText("Open Action File");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openActionFileMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem5);
        fileMenu.add(jSeparator1);

        aboutMenuItem.setText("About OpenMIMS");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(aboutMenuItem);
        fileMenu.add(jSeparator2);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.setToolTipText("Quit the NRIMS Application.");
        exitMenuItem.setName("ExitMenuItem"); // NOI18N
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        jMenuBar1.add(fileMenu);

        editMenu.setText("Edit");

        jMenuItem3.setText("Preferences...");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        editMenu.add(jMenuItem3);

        jMenuItem4.setText("Restore MIMS");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        editMenu.add(jMenuItem4);

        jMenuBar1.add(editMenu);

        viewMenu.setText("View");

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.ALT_MASK));
        jMenuItem2.setText("Tile Windows");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        viewMenu.add(jMenuItem2);

        jMenuBar1.add(viewMenu);

        utilitiesMenu.setText("Utilities");

        sumAllMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_MASK));
        sumAllMenuItem.setText("Sum all Open");
        sumAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sumAllMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(sumAllMenuItem);

        jMenuBar1.add(utilitiesMenu);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 642, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, mainTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 642, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                .add(18, 18, 18)
                .add(mainTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(28, 28, 28))
        );

        getAccessibleContext().setAccessibleDescription("NRIMS Analyais Module");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * restores any closed or modified massImages
     */
    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        if (image != null) {
            restoreMims();
        }
    }

    public void autocontrast(MimsPlus img) {
        ij.process.ImageStatistics imgStats = img.getStatistics();
        
        img.getProcessor().setMinAndMax(java.lang.Math.max(0.0, imgStats.mean - (2 * imgStats.stdDev)), imgStats.mean + (2*imgStats.stdDev));
    }
    
    public void restoreMims() {
        for (int i = 0; i < image.nMasses(); i++) {
            if (bOpenMass[i] == false) {
                // catch this below
            } else if (massImages[i] == null) {
                // catch this below ..
            } else if (massImages[i].getBitDepth() != 16) {
                if (massImages[i].getWindow() != null) {
                    massImages[i].getWindow().close();
                }
                massImages[i] = null;
            } else if (massImages[i].getProcessor() == null) {
                if (massImages[i].getWindow() != null) {
                    massImages[i].getWindow().close();
                }
                massImages[i] = null;
            } else if (massImages[i].getNSlices() != image.nImages()) {
                if (massImages[i].getWindow() != null) {
                    massImages[i].getWindow().close();
                }
                massImages[i] = null;
            }
        }

        for (int i = 0; i < image.nMasses(); i++) {
            if (massImages[i] == null && bOpenMass[i]) {
                currentlyOpeningImages = true;
//                int nMasses = image.nMasses();
//                int nImages = image.nImages();

                try {
                    MimsPlus mp = new MimsPlus(image, i);
                    mp.setAllowClose(false);
                    massImages[i] = mp;
                    double dMin = (double) image.getMin(i);
                    double dMax = (double) image.getMax(i);
                    massImages[i].getProcessor().setMinAndMax(dMin, dMax);
                    massImages[i].getProcessor().setPixels(image.getPixels(i));
                    if (image.nImages() > 1) {
                        for (int j = 1; j < image.nImages(); j++) {
                            image.setStackIndex(j);
                            massImages[i].appendImage(j);
                        }
                        massImages[i].setSlice(1);
                    }
                } catch (Exception x) {
                    currentlyOpeningImages = false;
                    IJ.log(x.toString());
                    x.printStackTrace();
                }

                massImages[i].show();
                massImages[i].addListener(this);
                currentlyOpeningImages = false;
            }

        }

        this.mimsStackEditing.resetImageStacks();
        this.mimsStackEditing.restoreAllPlanes();
        ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
        //wo.run("tile");
        this.getmimsStackEditing().setConcatGUI(false);
        this.mimsLog.Log("File restored.");
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        ij.gui.GenericDialog gd = new ij.gui.GenericDialog("Preferences");
        gd.addCheckbox("Close existing image when opening", bCloseOldWindows);
        gd.addCheckbox("Debug", bDebug);
        gd.addNumericField("Ratio Scale Factor", ratioScaleFactor, 0);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        } 
        bCloseOldWindows = gd.getNextBoolean();
        bDebug = gd.getNextBoolean();
        //this.setRatioScaleFactor((int)gd.getNextNumber());
        //HSI color scale not changing/bug, fix scale factor at 10000
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
        wo.run("tile");
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void openMIMSImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMIMSImageMenuItemActionPerformed
        loadMIMSFile();
}//GEN-LAST:event_openMIMSImageMenuItemActionPerformed

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
        //tab focus changed
        //reset tomography info
        if (this.mimsTomography != null) {
            this.mimsTomography.resetBounds();
            this.mimsTomography.resetImageNamesList();
        }

        if (this.mimsStackEditing != null) {
            this.mimsStackEditing.resetTrueIndexLabel();
            this.mimsStackEditing.resetSpinners();
        }

    }//GEN-LAST:event_jTabbedPane1StateChanged

    private void openActionFileMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openActionFileMenuItemActionPerformed
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.CANCEL_OPTION) {
            currentlyOpeningImages = false;
            return;
        }
        String actionFile = fc.getSelectedFile().getPath();
        String actionDir = fc.getSelectedFile().getParent();

        try {
            BufferedReader br = new BufferedReader(new FileReader(actionFile));
            String mainImageFile = br.readLine();
            mainImageFile = actionDir + System.getProperty("file.separator") + mainImageFile + ".im";
            if (!new File(mainImageFile).exists()) {
                mainImageFile = null;
            }
            loadMIMSFile(mainImageFile);
            // read and concatenate more files, if required
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals("")) {
                    break;
                }
                String addImageFile = actionDir + System.getProperty("file.separator") + line + ".im";
                if (!new File(addImageFile).exists()) {
                    addImageFile = null;
                }
                UI tempui = new UI(addImageFile);
                this.mimsStackEditing.concatImages(false, tempui);

                for (MimsPlus img : tempui.getMassImages()) {
                    if (img != null) {
                        img.setAllowClose(true);
                        img.close();
                    }
                }
            }

            // read and perform actions
            int trueIndex = 1;
            while ((line = br.readLine()) != null) {
                if (line.equals("")) {
                    break;
                }
                String[] actionRow = line.split("\t");
                for (int k = 0; k < image.nMasses(); k++) {     // set the current slice
                    massImages[k].setSlice(trueIndex);
                }
                int displayIndex = this.mimsAction.displayIndex(trueIndex);
                this.mimsStackEditing.XShiftSlice(displayIndex, Integer.parseInt(actionRow[1]));
                this.mimsStackEditing.YShiftSlice(displayIndex, Integer.parseInt(actionRow[2]));
                if (Integer.parseInt(actionRow[3]) == 1) {
                    this.mimsStackEditing.removeSlice(displayIndex);
                }
                trueIndex++;
            }
        // TODO we need more refined Exception checking here
        } catch (Exception e) {
            e.printStackTrace();
        }
}//GEN-LAST:event_openActionFileMenuItemActionPerformed

private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
    System.exit(0);
}//GEN-LAST:event_exitMenuItemActionPerformed

private void sumAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sumAllMenuItemActionPerformed
// TODO add your handling code here:
    MimsPlus[] openmass = this.getOpenMassImages();
    MimsPlus[] openratio = this.getOpenRatioImages();
    for(int i=0; i < openmass.length; i++) {
        this.computeSum(openmass[i]);
    }
    for(int i=0; i < openratio.length; i++) {
        this.computeSum(openratio[i]);
    }
}//GEN-LAST:event_sumAllMenuItemActionPerformed

private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
// TODO add your handling code here:
    
    //This should be better
    //Text is not selectable in a JOptionPane...
    
    String message = "OpenMIMS v0.7\n\n";
    message += "OpenMIMS was Developed at NRIMS, the National Resource\n";
    message += "for Imaging Mass Spectrometry.\n";
    message += "http://www.nrims.hms.harvard.edu/\n";
    message += "\nDeveloped by:\n Doug Benson, Collin Poczatek\n ";
    message += "Boris Epstein, Philip Gormanns\n Stephan Reckow, ";
    message += "Zeke Kauffman.";
    message += "\n\nOpenMIMS uses or depends upon:\n";
    message += " TurboReg:  http://bigwww.epfl.ch/thevenaz/turboreg/\n";
    message += " jFreeChart:  http://www.jfree.org/jfreechart/\n";
    message += " FileDrop:  http://iharder.sourceforge.net/current/java/filedrop/\n";
    
    //System.out.println(message);
    
    javax.swing.JOptionPane pane = new javax.swing.JOptionPane(message);
    javax.swing.JDialog dialog = pane.createDialog(new javax.swing.JFrame(), "About OpenMIMS");
    
    dialog.setVisible(true);
}//GEN-LAST:event_aboutMenuItemActionPerformed

//    private void imagesChanged(com.nrims.mimsPlusEvent e) {
//        mimsStackEditing.resetTrueIndexLabel();
//    }            
    /**
     * @return an instance of the RoiManager
     */
    public MimsRoiManager getRoiManager() {
        roiManager = MimsRoiManager.getInstance();
        if (roiManager == null) {
            roiManager = new MimsRoiManager();
        }
// RoiManager shouldn't necessarily show up, when the object is requested (S. Reckow)        
//        else
//            roiManager.toFront();
        return roiManager;
    }

    /** returns array of massImages indexed to the corresponding mass index
     * Note,  if a window is closed, the corresponding massImage is null
     * @return 
     */
    public MimsPlus[] getMassImages() {
        return massImages;
    }

    public MimsPlus getMassImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return massImages[i];
        }
        return null;
    }

    public MimsPlus getRatioImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return ratioImages[i];
        }
        return null;
    }

    /** returns only the open mass images as an array
     * @return 
     */
    public MimsPlus[] getOpenMassImages() {
        int i, nOpen = 0;
        for (i = 0; i < massImages.length; i++) {
            if (massImages[i] != null && bOpenMass[i]) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0        , nOpen = 0; i < massImages.length; i++) {
            if (massImages[i] != null && bOpenMass[i]) {
                mp[nOpen++] = massImages[i];
            }
        }
        return mp;
    }

    public MimsPlus[] getOpenRatioImages() {
        int i, nOpen = 0;
        for (i = 0; i < maxMasses; i++) {
            if (ratioImages[i] != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0        , nOpen = 0; i < maxMasses; i++) {
            if (ratioImages[i] != null) {
                mp[nOpen++] = ratioImages[i];
            }
        }
        return mp;
    }

    public MimsPlus[] getOpenHSIImages() {
        int i, nOpen = 0;
        for (i = 0; i < maxMasses; i++) {
            if (hsiImages[i] != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0        , nOpen = 0; i < maxMasses; i++) {
            if (hsiImages[i] != null) {
                mp[nOpen++] = hsiImages[i];
            }
        }
        return mp;
    }

    public MimsPlus[] getOpenSegImages() {
        int i, nOpen = 0;
        for (i = 0; i < maxMasses; i++) {
            if (segImages[i] != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0, nOpen = 0; i < maxMasses; i++) {
            if (segImages[i] != null) {
                mp[nOpen++] = segImages[i];
            }
        }
        return mp;
    }
    
    public MimsPlus[] getOpenSumImages() {
        int i, nOpen = 0;
        for (i = 0; i < 2*maxMasses; i++) {
            if (sumImages[i] != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0, nOpen = 0; i < 2*maxMasses; i++) {
            if (sumImages[i] != null) {
                mp[nOpen++] = sumImages[i];
            }
        }
        return mp;
    }

    public MimsPlus getImageByName(String name) {
        MimsPlus mp =null;
        
        MimsPlus[] tempimages = this.getOpenMassImages();
        
        for(int i=0; i<tempimages.length; i++){
            if(name == tempimages[i].getTitle()) {
                return tempimages[i];
            }
        }
        
        tempimages = this.getOpenRatioImages();
        
        for(int i=0; i<tempimages.length; i++){
            if(name == tempimages[i].getTitle()) {
                return tempimages[i];
            }
        }
        return mp;
    }
    
    public void setSyncStack(boolean bSync) {
        bSyncStack = bSync;
    }

    public boolean getSyncStack() {
        return bSyncStack;
    }

    public void setSyncROIs(boolean bSync) {
        bSyncROIs = bSync;
    }

    public boolean getSyncROIs() {
        return bSyncROIs;
    }

    public void setAddROIs(boolean bOnOff) {
        bAddROIs = bOnOff;
    }

    public void setmimsAction(MimsAction action) {
        mimsAction = action;
    }

    public boolean getAddROIs() {
        return bAddROIs;
    }

    public HSIView getHSIView() {
        return hsiControl;
    }

    public MimsData getMimsData() {
        return mimsData;
    }

    public MimsLog getmimsLog() {
        return mimsLog;
    }

    public MimsRoiControl getRoiControl() {
        return roiControl;
    }

    public MimsStackEditing getmimsStackEditing() {
        return mimsStackEditing;
    }

    public MimsTomography getmimsTomography() {
        return mimsTomography;
    }

    public MimsAction getmimsAction() {
        return mimsAction;
    }

    public boolean isOpening() {
        return currentlyOpeningImages;
    }

    public boolean isUpdating() {
        return bUpdating;
    }

    public com.nrims.data.Opener getMimsImage() {
        return image;
    }

    public void setActiveMimsPlus(MimsPlus mp) {
        for (int i = 0; i < maxMasses; i++) {
            if (mp == hsiImages[i]) {
                hsiControl.setHSIProps(hsiImages[i].getHSIProps());
            }
        }
    }

    public synchronized void updateStatus(String msg) {
        if (bUpdating) {
            return; // Don't run from other threads...
        } // Don't run from other threads...
        if (!currentlyOpeningImages) {
            mainTextField.setText(msg);
        } else {
            IJ.showStatus(msg);
        }
        if (bDebug) {
            IJ.log(msg);
//            System.out.println(msg);
        }
    }
    
    public int getRatioScaleFactor() {
        return this.ratioScaleFactor;
    }
    
    public int setRatioScaleFactor(int s) {
        this.ratioScaleFactor = s;
        return this.ratioScaleFactor;
    }
    
    @Override
    public void run(String cmd) {
        if (cmd.equalsIgnoreCase("open")) {
            super.run(cmd);
        } else {
            super.run("");
        }
        setVisible(true);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        
        for (int i = 0; i < args.length; i++) {
           if (args[i].startsWith("-ijpath") && i+1 < args.length) {
					Prefs.setHomeDir(args[i+1]);
            }
        }

        EventQueue.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                System.out.println("Ui.run called");
                new UI(null).setVisible(true);
            }
            
        });
        
         
    }

    public boolean getDebug() {
        return bDebug;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField mainTextField;
    private javax.swing.JMenuItem openNewMenuItem;
    private javax.swing.JMenuItem sumAllMenuItem;
    private javax.swing.JMenu utilitiesMenu;
    private javax.swing.JMenu viewMenu;
    // End of variables declaration//GEN-END:variables
}

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
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ImageWindow;
import ij.gui.ImageCanvas;
import ij.io.RoiEncoder;
import ij.process.ImageStatistics;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Point;
import java.awt.Image;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.WindowListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JOptionPane;

import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/* 
 * The main user interface of the NRIMS ImageJ plugin.
 */
public class UI extends PlugInJFrame implements WindowListener, MimsUpdateListener {

    public static final long serialVersionUID = 1;
    private int maxMasses = 8;
    private boolean bDebug = false;
    private ij.ImageJ ijapp = null;
    private boolean bSyncStack = true;
    private boolean bSyncROIs = true;
    private boolean bSyncROIsAcrossPlanes = true;
    private boolean bAddROIs = true;
    private boolean bUpdating = false;    
    private boolean currentlyOpeningImages = false;
    private boolean bCloseOldWindows = true;
    private MimsRoiManager roiManager = null;
    private com.nrims.data.Opener image = null;
    private String lastFolder = null;
    private String actionFileName = "action.txt"; 
    private String HSIprefix = "HSIimage";
    private String HSIextension = ".hsi";        
    private boolean[] bOpenMass = new boolean[maxMasses];
    private com.nrims.MimsPlus[] massImages = new com.nrims.MimsPlus[maxMasses];
    private com.nrims.MimsPlus[] ratioImages = new com.nrims.MimsPlus[maxMasses];
    private com.nrims.MimsPlus[] hsiImages = new com.nrims.MimsPlus[maxMasses];
    private com.nrims.MimsPlus[] segImages = new com.nrims.MimsPlus[maxMasses];
    private com.nrims.MimsPlus[] sumImages = new com.nrims.MimsPlus[2 * maxMasses];
    private com.nrims.MimsData mimsData = null;
    private com.nrims.MimsLog mimsLog = null;
    private com.nrims.MimsRoiControl roiControl = null;
    private com.nrims.HSIView hsiControl = null;
    private com.nrims.MimsCBControl cbControl = new MimsCBControl(this);
    private com.nrims.MimsStackEditing mimsStackEditing = null;
    private com.nrims.MimsTomography mimsTomography = null;
    protected com.nrims.MimsAction mimsAction = null;
    private com.nrims.SegmentationForm segmentation = null;
    protected ij.gui.Roi activeRoi;
    private int ratioScaleFactor = 10000;    //tesing fixed contrast
    
    private boolean fixRatioContrast = true;
    private boolean autoContrastMass = true;
    private boolean medianFilterRatios = false;
    
    private com.nrims.data.FileDrop mimsDrop;
    
    protected MimsLineProfile lineProfile;
    
    // fileName name of the .im image file to be opened.
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
            for (int i = 0; i < 2 * maxMasses; i++) {
                sumImages[i] = null;
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
                    String lastfile = files[files.length - 1].getCanonicalPath();
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
        if (fileName == null) {
            this.loadMIMSFile();
        } else {
            if(new File(fileName).isDirectory()) {
                System.out.println("fileName: "+fileName);
                loadMIMSFile();
            } else {
                this.loadMIMSFile(fileName);
            }
        }
        
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
                        segImages[i] = null;
                    }
                }
            }
            if (massImages[i] != null) {
                massImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (massImages[i].getWindow() != null) {
                        massImages[i].getWindow().close();
                        massImages[i] = null;
                    }
                }
            }
            bOpenMass[i] = false;
            if (hsiImages[i] != null) {
                hsiImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (hsiImages[i].getWindow() != null) {
                        hsiImages[i].getWindow().close();
                        hsiImages[i] = null;
                    }
                }
            }
            if (ratioImages[i] != null) {
                ratioImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (ratioImages[i].getWindow() != null) {
                        ratioImages[i].getWindow().close();
                        ratioImages[i] = null;
                    }
                }
            }
        }

        for (int i = 0; i < maxMasses * 2; i++) {
            if (sumImages[i] != null) {
                sumImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (sumImages[i].getWindow() != null) {
                        sumImages[i].getWindow().close();
                        sumImages[i] = null;
                    }
                }
            }
        }
    // FIXME: todo: the current opener is not dispose anywhere, so there are likely dangling file handles.
    }

    /**
     * Causes a dialog to open that allows a user to choose an image file.
     */
    //changed to public...
    public synchronized void loadMIMSFile() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        MIMSFileFilter filter = new MIMSFileFilter("im");
        fc.setFileFilter(filter);
        fc.setPreferredSize(new java.awt.Dimension(650, 500));

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
    //changed to public...
    public synchronized void loadMIMSFile(String fileName) throws NullPointerException {
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
            //added wiggle room to how big a file can be opened
            //was causing heap size exceptions to be thrown
            long maxMemory = IJ.maxMemory()-(128000000);
            
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

                jTabbedPane1.setComponentAt(0, mimsData);
                jTabbedPane1.setTitleAt(0, "MIMS Data");
                jTabbedPane1.add("Process", hsiControl);
                jTabbedPane1.add("Contrast", cbControl);
                jTabbedPane1.add("Analysis", roiControl);
                jTabbedPane1.add("Stack Editing", mimsStackEditing);
                jTabbedPane1.add("Tomography", mimsTomography);
                jTabbedPane1.add("Segmentation", segmentation);
                jTabbedPane1.add("MIMS Log", mimsLog);

            } else {

                mimsData = new com.nrims.MimsData(this, image);
                hsiControl = new HSIView(this);
                cbControl = new MimsCBControl(this);
                mimsStackEditing = new MimsStackEditing(this, image);
                mimsTomography = new MimsTomography(this, image);
                mimsAction = new MimsAction(this, image);
                segmentation = new SegmentationForm(this);
                jTabbedPane1.setComponentAt(0, mimsData);
                jTabbedPane1.setTitleAt(0, "MIMS Data");
                jTabbedPane1.setComponentAt(1, hsiControl);
                jTabbedPane1.setComponentAt(2, cbControl);
                jTabbedPane1.setComponentAt(3, roiControl);
                jTabbedPane1.setComponentAt(4, mimsStackEditing);
                jTabbedPane1.setComponentAt(5, mimsTomography);
                jTabbedPane1.setComponentAt(6, segmentation);
                


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
                rPixels[i] = ratioScaleFactor * ((float) nPixels[i] / (float) dPixels[i]);
                if (rPixels[i] > rMax) {
                    rMax = rPixels[i];
                } else if (rPixels[i] < rMin) {
                    rMin = rPixels[i];
                }
            } else {
                rPixels[i] = 0.0f;
            }

        }
        
        mp.getProcessor().setMinAndMax(props.getMinRatio(), props.getMaxRatio());
        
        //DANGER DANGER DANGER DANGER DANGER DANGER
        if (this.medianFilterRatios) {
            Roi temproi = mp.getRoi();
            mp.killRoi();
            ij.plugin.filter.RankFilters rfilter = new ij.plugin.filter.RankFilters();
            double r = this.hsiControl.getMedianRadius();
            //System.out.println("r: " + r);
            rfilter.rank(mp.getProcessor(), r, rfilter.MEDIAN);
            rfilter = null;
            mp.setRoi(temproi);
        }
        
        //End danger
        
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
        str += "Raster (nm): " + im.getRaster() + "\n";
        str += "Duration (s): " + im.getDuration() + "\n";
        str += "Dwell time (ms/xy): " + im.getDwellTime() + "\n";
        str += "Stage Position: " + im.getPosition() + "\n";
        str += "Sample name: " + im.getSampleName() + "\n";
        str += "Sample date: " + im.getSampleDate() + "\n";
        str += "Sample hour: " + im.getSampleHour() + "\n";
        str += "Pixel width (nm): " + im.getPixelWidth() + "\n";
        str += "Pixel height (nm): " + im.getPixelHeight() + "\n";
        str += "End header.\n\n";
        return str;
    }
    
    public void updateAllImages() {
        for (int i = 0; i < maxMasses; i++) {
            if (segImages[i] != null) {
                segImages[i].updateAndDraw();
            }
            if (massImages[i] != null) {
                massImages[i].updateAndDraw();
            }
            if (hsiImages[i] != null) {
                hsiImages[i].updateAndDraw();
            }
            if (ratioImages[i] != null) {
                ratioImages[i].updateAndDraw();
            }
        }

        for (int i = 0; i < maxMasses * 2; i++) {
            if (sumImages[i] != null) {
                sumImages[i].updateAndDraw();
            }
        }
    }

        public void recomputeAllRatio(HSIProps props) {
        for (int i = 0; i < maxMasses; i++) {
            if (ratioImages[i] != null) {
                computeRatio(props);
                ratioImages[i].updateAndDraw();
            }
        }

    }

    public synchronized boolean computeSum(MimsPlus mImage) {
        boolean fail = true;

        if (mImage == null) {
            return false;
        }

        int width = mImage.getWidth();
        int height = mImage.getHeight();
        String sumName = "Sum : " + mImage.getShortTitle();
        int templength = mImage.getProcessor().getPixelCount();
        double[] sumPixels = new double[templength];
        short[] tempPixels = new short[templength];

        int startSlice = mImage.getCurrentSlice();

        if (mImage.getMimsType() == MimsPlus.MASS_IMAGE) {
            for (int i = 1; i <= mImage.getImageStackSize(); i++) {
                mImage.setSlice(i);
                tempPixels = (short[]) mImage.getProcessor().getPixels();
                for (int j = 0; j < sumPixels.length; j++) {
                    sumPixels[j] += tempPixels[j];
                }
            }
            mImage.setSlice(startSlice);
            fail = false;
        }

        if (mImage.getMimsType() == MimsPlus.RATIO_IMAGE) {
            int numMass = mImage.getNumMass();
            int denMass = mImage.getDenMass();
            MimsPlus nImage = massImages[numMass];
            MimsPlus dImage = massImages[denMass];
            double[] numPixels = new double[templength];
            double[] denPixels = new double[templength];

            startSlice = nImage.getCurrentSlice();

            for (int i = 1; i <= nImage.getImageStackSize(); i++) {
                nImage.setSlice(i);
                tempPixels = (short[]) nImage.getProcessor().getPixels();
                for (int j = 0; j < numPixels.length; j++) {
                    numPixels[j] += tempPixels[j];
                }
            }
            for (int i = 1; i <= dImage.getImageStackSize(); i++) {
                dImage.setSlice(i);
                tempPixels = (short[]) dImage.getProcessor().getPixels();
                for (int j = 0; j < denPixels.length; j++) {
                    denPixels[j] += tempPixels[j];
                }
            }
            for (int i = 0; i < sumPixels.length; i++) {
                if (denPixels[i] != 0) {
                    sumPixels[i] = ratioScaleFactor * (numPixels[i] / denPixels[i]);
                } else {
                    sumPixels[i] = 0;
                }
            }

            nImage.setSlice(startSlice);

            fail = false;
        }

        if (!fail) {
            MimsPlus mp = new MimsPlus(this, width, height, sumPixels, sumName);

            boolean bShow = (mp == null);
            // ??????
            // find a slot to save it

            boolean bFound = false;
            for (int i = 0; i < maxMasses * 2 && !bFound; i++) {
                if (sumImages[i] == null) {
                    bFound = true;
                    sumImages[i] = mp;
                }
            }
            //why overwrite the last slot?
            if (!bFound) {
                sumImages[(maxMasses * 2) - 1] = mp;
            }

            mp.addListener(this);
            mp.show();
            if (mImage.getMimsType() == MimsPlus.RATIO_IMAGE) {
                this.autocontrastMassImage(mp);
            }
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
        if (currentlyOpeningImages || bUpdating)
            return;
        bUpdating = true; // Stop recursion

        /* sychronize Stack displays */
        if (bSyncStack && evt.getAttribute() == MimsPlusEvent.ATTR_UPDATE_SLICE) {
            MimsPlus mp[] = this.getOpenMassImages();
            MimsPlus rp[] = this.getOpenRatioImages();
            MimsPlus hsi[] = this.getOpenHSIImages();

            // Set mass images.
            int nSlice = evt.getSlice();
            for (int i = 0; i < mp.length; i++) {
               massImages[i].setSlice(nSlice);                    
            }                                                    
                            
            // Update HSI image slice.
            for (int i = 0; i < hsi.length; i++) {                   
               computeHSI(hsiImages[i].getHSIProps());
            }
            
            // Update ratio images.
            for (int i = 0; i < rp.length; i++) {                                                                     
               computeRatio(ratioImages[i].getHSIProps());               
            }                            
                
            autocontrastAllImages();
            cbControl.updateHistogram();
            
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
        } else if (evt.getAttribute() == MimsPlusEvent.ATTR_SET_ROI || 
                   evt.getAttribute() == MimsPlusEvent.ATTR_MOUSE_RELEASE) {
            /* Update all images with a selected ROI 
             * MOUSE_RELEASE catches drawing new ROIs
             */
            if (bSyncROIs) {
                int i;
                MimsPlus mp = (MimsPlus) evt.getSource();
                for (i = 0; i < image.nMasses(); i++) {
                    if (massImages[i] != mp && massImages[i] != null && bOpenMass[i]) {
                        massImages[i].setRoi(evt.getRoi());
                    }
                }
                for (i = 0; i < hsiImages.length; i++) {
                    if (hsiImages[i] != mp && hsiImages[i] != null) {
                        hsiImages[i].setRoi(evt.getRoi());
                    }
                }
                for (i = 0; i < ratioImages.length; i++) {
                    if (ratioImages[i] != mp && ratioImages[i] != null) {
                        ratioImages[i].setRoi(evt.getRoi());
                    }
                }
                for (i = 0; i < segImages.length; i++) {
                    if (segImages[i] != mp && segImages[i] != null) {
                        segImages[i].setRoi(evt.getRoi());
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
                    rm.add();
                    rm.showFrame();
                }
            }

        } else if (evt.getAttribute() == MimsPlusEvent.ATTR_ROI_MOVED) {
            ij.gui.Roi roi = evt.getRoi();
            MimsRoiManager rm = getRoiManager();
            rm.move();
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
      jMenuItem6 = new javax.swing.JMenuItem();
      jMenuItem7 = new javax.swing.JMenuItem();
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
      importIMListMenuItem = new javax.swing.JMenuItem();
      captureImageMenuItem = new javax.swing.JMenuItem();
      jSeparator3 = new javax.swing.JSeparator();
      closeAllRatioMenuItem = new javax.swing.JMenuItem();
      closeAllHSIMenuItem = new javax.swing.JMenuItem();
      closeAllSumMenuItem = new javax.swing.JMenuItem();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
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
         .add(0, 710, Short.MAX_VALUE)
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

      jMenuItem6.setText("Save Session");
      jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jMenuItem6ActionPerformed(evt);
         }
      });
      fileMenu.add(jMenuItem6);

      jMenuItem7.setText("Open Session");
      jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jMenuItem7ActionPerformed(evt);
         }
      });
      fileMenu.add(jMenuItem7);
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

      importIMListMenuItem.setText("Import .im List");
      importIMListMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            importIMListMenuItemActionPerformed(evt);
         }
      });
      utilitiesMenu.add(importIMListMenuItem);

      captureImageMenuItem.setText("Capture current Image");
      captureImageMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            captureImageMenuItemActionPerformed(evt);
         }
      });
      utilitiesMenu.add(captureImageMenuItem);
      utilitiesMenu.add(jSeparator3);

      closeAllRatioMenuItem.setText("Close All Ratio Images");
      closeAllRatioMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            closeAllRatioMenuItemActionPerformed(evt);
         }
      });
      utilitiesMenu.add(closeAllRatioMenuItem);

      closeAllHSIMenuItem.setText("Close All HSI Images");
      closeAllHSIMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            closeAllHSIMenuItemActionPerformed(evt);
         }
      });
      utilitiesMenu.add(closeAllHSIMenuItem);

      closeAllSumMenuItem.setText("Close All Sum Images");
      closeAllSumMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            closeAllSumMenuItemActionPerformed(evt);
         }
      });
      utilitiesMenu.add(closeAllSumMenuItem);

      jMenuBar1.add(utilitiesMenu);

      setJMenuBar(jMenuBar1);

      org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
               .add(org.jdesktop.layout.GroupLayout.LEADING, jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE)
               .add(org.jdesktop.layout.GroupLayout.LEADING, mainTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE))
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
    
    public void setAutoContrastMass(boolean set) {
        this.autoContrastMass = set;
    }
    
    public void setMedianFilterRatios(boolean set) {
        this.medianFilterRatios = set;
    }
    
    public void autocontrastAllImages() {       
       // All mass images      
       MimsPlus mp[] = getOpenMassImages();
        for (int i = 0; i < mp.length; i++) {
            if (mp[i].getAutoContrastAdjust())
               autocontrastMassImage(mp[i]);
        }
        
        // All ratio images
        MimsPlus rp[] = getOpenRatioImages();
        for (int i = 0; i < rp.length; i++) {
           if (rp[i].getAutoContrastAdjust())
              autocontrastRatioImage(rp[i]);
        }                
    }
    
    // Calls the default ImageJ autocontrast (reset).
    public void autocontrastMassImage(MimsPlus img) {       
       ContrastAdjuster ca = new ContrastAdjuster(img);
       ca.doReset = true;
       ca.doUpdate(img);             
    }
    
    // Custom contrasting code for ratio images.
   public void autocontrastRatioImage(MimsPlus img) {
      
      // Get the current image statistics.
      ImageStatistics imgStats = img.getStatistics();
      
      // Get the current image properties and override some fields.
      HSIProps props = img.getHSIProps();
      props.setMinRatio(java.lang.Math.max(0.0, imgStats.mean - (2 * imgStats.stdDev)));
      props.setMaxRatio(imgStats.mean + (imgStats.stdDev));
      
      // Generate new image.
      computeRatio(props);            
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

private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
    System.exit(0);
}//GEN-LAST:event_exitMenuItemActionPerformed

private void sumAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sumAllMenuItemActionPerformed
// TODO add your handling code here:
    MimsPlus[] openmass = this.getOpenMassImages();
    MimsPlus[] openratio = this.getOpenRatioImages();

    //clear all sum images
    for (int i = 0; i < maxMasses * 2; i++) {
        if (sumImages[i] != null) {
            sumImages[i].close();
            sumImages[i] = null;
        }
    }
    for (int i = 0; i < openmass.length; i++) {
        this.computeSum(openmass[i]);
    }
    for (int i = 0; i < openratio.length; i++) {
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
    message += "Rob Gonzales, Zeke Kauffman.";
    message += "\n\nOpenMIMS uses or depends upon:\n";
    message += " TurboReg:  http://bigwww.epfl.ch/thevenaz/turboreg/\n";
    message += " jFreeChart:  http://www.jfree.org/jfreechart/\n";
    message += " FileDrop:  http://iharder.sourceforge.net/current/java/filedrop/\n";

    //System.out.println(message);

    javax.swing.JOptionPane pane = new javax.swing.JOptionPane(message);
    javax.swing.JDialog dialog = pane.createDialog(new javax.swing.JFrame(), "About OpenMIMS");

    dialog.setVisible(true);
}//GEN-LAST:event_aboutMenuItemActionPerformed

private void captureImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureImageMenuItemActionPerformed
    //testing trying to grab screen pixels from image for rois and annotations

    /** Captures the active image window and returns it as an ImagePlus. */
    ImagePlus imp = ij.WindowManager.getCurrentImage();
    if (imp == null) {
        IJ.noImage();
        return;
    }
    ImagePlus imp2 = null;
    try {
        ImageWindow win = imp.getWindow();
        if (win == null) {
            return;
        }
        win.toFront();
        Point loc = win.getLocation();
        ImageCanvas ic = win.getCanvas();
        ic.update(ic.getGraphics());

        Rectangle bounds = ic.getBounds();
        loc.x += bounds.x;
        loc.y += bounds.y;
        Rectangle r = new Rectangle(loc.x, loc.y, bounds.width, bounds.height);
        Robot robot = new Robot();
        Image img = robot.createScreenCapture(r);
        if (img != null) {
            imp2 = new ImagePlus("Grab of " + imp.getTitle(), img);
            imp2.show();
        }
    } catch (Exception e) {
        ij.IJ.log(e.getMessage());
    }

}//GEN-LAST:event_captureImageMenuItemActionPerformed

private void importIMListMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importIMListMenuItemActionPerformed
// TODO add your handling code here:    
   com.nrims.data.LoadImageList testLoad = new com.nrims.data.LoadImageList(this);
    boolean read;
    read = testLoad.openList();
    if (!read) {
        return;
    }

    testLoad.printList();
    testLoad.simpleIMImport();
    this.mimsStackEditing.setConcatGUI(true);
}//GEN-LAST:event_importIMListMenuItemActionPerformed

private void closeAllRatioMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllRatioMenuItemActionPerformed
    for (int i = 0; i < ratioImages.length; i++) {
        if (ratioImages[i] != null) {
            ratioImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllRatioMenuItemActionPerformed

private void closeAllHSIMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllHSIMenuItemActionPerformed
    for (int i = 0; i < hsiImages.length; i++) {
        if (hsiImages[i] != null) {
            hsiImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllHSIMenuItemActionPerformed

private void closeAllSumMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllSumMenuItemActionPerformed
    for (int i = 0; i < sumImages.length; i++) {
        if (sumImages[i] != null) {
            sumImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllSumMenuItemActionPerformed

// Saves all *.im files, action file, roi list, and HSI images to a single zip file.
private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
               
   byte[] buf = new byte[1024];
   byte[] newline = (new String("\n")).getBytes();
   int len;    
   
   // Let user select name and location of zip file.
   JFileChooser fc = new JFileChooser();
   fc.setFileFilter(new MIMSFileFilter("zip"));   
   int returnVal = fc.showSaveDialog(this);   
   String zipName;
   if (returnVal == JFileChooser.APPROVE_OPTION)
      zipName = fc.getSelectedFile().getAbsolutePath();
   else
      return;      
   if (zipName == null) return;         
   if (!(zipName.endsWith(".zip") || zipName.endsWith(".ZIP"))) {
      zipName = zipName + ".zip";
   }
   
   try {
     
      // Creates the zip file.
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipName));      
         
      // Save *.im files to zip.        
      for (Opener im : mimsAction.getImages()) {
         if (im.getImageFile().exists()) {
            FileInputStream in = new FileInputStream(im.getImageFile().getAbsolutePath());
            zos.putNextEntry(new ZipEntry(im.getImageFile().getName()));           
            while ((len = in.read(buf)) > 0) {
                zos.write(buf, 0, len);
            }
            zos.closeEntry();            
            in.close();
         }                  
      }                     
      
      // Save the action file to zip.
      zos.putNextEntry(new ZipEntry(actionFileName));
      for (Opener im : mimsAction.getImages()) {
         byte[] b = im.getName().getBytes();
         zos.write(b);
         zos.write(newline);
      }
      zos.write(newline);
      for (int i = 1; i <= mimsAction.getSize(); i++) {
         byte[] b = mimsAction.getActionRow(i).getBytes();
         zos.write(b);
         zos.write(newline);
      }            
      zos.closeEntry();      
      
      // Save the ROI files to zip.       
      int[] indexes = getRoiManager().getAllIndexes();
      DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
      RoiEncoder re = new RoiEncoder(out);
      for (int i = 0; i < indexes.length; i++) {
         String label = getRoiManager().getList().getModel().getElementAt(indexes[i]).toString();
         Roi roi = (Roi) getRoiManager().getROIs().get(label);
         if (!label.endsWith(".roi")) {
            label += ".roi";
         }
         zos.putNextEntry(new ZipEntry(label));
         re.write(roi);
         out.flush();
      }      
      
      // Save the HSI images     
      MimsPlus hsi[] = this.getOpenHSIImages();
      ObjectOutputStream obj_out = null;
      for (int i = 0; i < hsi.length; i++) {
         HSIProps hsiprops = hsi[i].getHSIProps(); 
         String label = HSIprefix+i+HSIextension;
         zos.putNextEntry(new ZipEntry(label));
         obj_out = new ObjectOutputStream(zos);
         obj_out.writeObject(hsiprops);
         zos.closeEntry();
      }    
      obj_out.close();          
      zos.close();                              
      
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
}//GEN-LAST:event_jMenuItem6ActionPerformed

// Loads a previous session.
private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
                         
           // User gets Zip file containg all .im files, action file, ROI files and HSIs. 
           JFileChooser fc = new JFileChooser();
           fc.setFileFilter(new MIMSFileFilter("zip"));
           fc.setPreferredSize(new java.awt.Dimension(650, 500));
           if (fc.showOpenDialog(this) == JFileChooser.CANCEL_OPTION) {
              return;
           }
           currentlyOpeningImages = true;
           File selectedFile = fc.getSelectedFile();

           // Begin process of reading zip file contents
           // and extracting files if necessary.
           try {          
              
              // Read action file straight from zip.
              ZipFile zipFile = new ZipFile(selectedFile);
              ZipEntry actionEntry = zipFile.getEntry(actionFileName);
              if (actionEntry == null) {
                 JOptionPane.showMessageDialog(this, "Zip file does not contain " + actionFileName, "Error", JOptionPane.ERROR_MESSAGE);           
                 return;
              }                 
              InputStream input = zipFile.getInputStream(actionEntry);
              InputStreamReader isr = new InputStreamReader(input);
              BufferedReader br = new BufferedReader(isr);
                                          
              // Read image name from action file.
              String mainImageFile = br.readLine();
              mainImageFile += ".im";
              ZipEntry mainImageEntry = zipFile.getEntry(mainImageFile);
              if (mainImageEntry == null) {
                 System.out.println("Zip file does not contain " + mainImageFile);
                 return;
              }

              // Extract image file from zip to temp directory.  
              File imageFile = extractFromZipfile(zipFile, mainImageEntry, null);              
              if (imageFile == null || !imageFile.exists()) {
                 System.out.println("Unable to extract " + mainImageEntry + " from " + zipFile.getName());
                 return;
              }
              
              // Read main image file.
              loadMIMSFile(imageFile.getAbsolutePath());

              // Continue to read action file, and extract more images if necessary.
              String line;
              while ((line = br.readLine()) != null) {
                 if (line.equals("")) {break;}
                 String addImageFile = line + ".im";
                 ZipEntry imageEntry = zipFile.getEntry(addImageFile);
                 imageFile = extractFromZipfile(zipFile, imageEntry, null);
                 if (imageFile == null || !imageFile.exists()) {
                    System.out.println("Unable to extract " + imageFile + " from " + zipFile.getName());
                    return;
                 }
                 UI tempui = new UI(imageFile.getAbsolutePath());
                 this.mimsStackEditing.concatImages(false, false, tempui);
                 for (MimsPlus img : tempui.getMassImages()) {
                    if (img != null) {
                       img.setAllowClose(true);
                       img.close();
                    }
                 }
              }

              // Continue to read action file, and perform actions
              bUpdating = true;
              int trueIndex = 1;
              ArrayList<Integer> deleteList = new ArrayList<Integer>();
              while ((line = br.readLine()) != null) {
                 if (line.equals("")) {break;}
                 String[] actionRow = line.split("\t");
                 for (int k = 0; k < image.nMasses(); k++) {
                    massImages[k].setSlice(trueIndex);
                 }
                 int displayIndex = this.mimsAction.displayIndex(trueIndex);
                 this.mimsStackEditing.XShiftSlice(displayIndex, Integer.parseInt(actionRow[1]));
                 this.mimsStackEditing.YShiftSlice(displayIndex, Integer.parseInt(actionRow[2]));
                 if (Integer.parseInt(actionRow[3]) == 1) {
                    deleteList.add(trueIndex);
                 }
                 trueIndex++;
              }
              this.mimsStackEditing.removeSliceList(deleteList);
              for (int k = 0; k < image.nMasses(); k++) {
                 massImages[k].setSlice(1);
              }
              bUpdating = false;
              
              // Load ROI list.
              MimsRoiManager rm = getRoiManager();  
              rm.deleteAll();
              rm.openZip(selectedFile.getAbsolutePath());
              if (rm.getAllIndexes().length > 0)
                 rm.showFrame();
                            
              // Load HSI images.              
              FileInputStream fis = new FileInputStream(selectedFile);
              ZipInputStream zis = new ZipInputStream(fis);
              ZipEntry zipEntry = null;        
              while((zipEntry = zis.getNextEntry()) != null ) {
                 if (zipEntry.getName().endsWith(HSIextension)) {
                    ObjectInputStream ois = new ObjectInputStream(zis);
                    HSIProps hsiprops = (HSIProps)ois.readObject();                    
                    computeHSI(hsiprops);
                 }                                  
              }        
              zis.close();                           
                            
           // TODO we need more refined Exception checking here
           } catch (Exception e) {
              e.printStackTrace();
           }  
           
           currentlyOpeningImages = false;
}//GEN-LAST:event_jMenuItem7ActionPerformed
    
public void updateLineProfile(double[] newdata, String name) {
    if(this.lineProfile==null) {
        return;
    } else {
        lineProfile.updateData(newdata, name);
    }
}

    // Returns an instance of the RoiManager.
    public MimsRoiManager getRoiManager() {
        roiManager = MimsRoiManager.getInstance();
        if (roiManager == null) {
            roiManager = new MimsRoiManager(this, image);
        }
        return roiManager;
    }
            
    String getImageDir() {
        //won't work on windows?
        String path = image.getImageFile().getAbsolutePath();
        path = path.substring(0, path.lastIndexOf("/")+1);
        return path;
    }
    
    
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
            if(name.equals(tempimages[i].getTitle())) {
                return tempimages[i];
            }
        }
        
        tempimages = this.getOpenRatioImages();
        
        for(int i=0; i<tempimages.length; i++){
            if(name.equals(tempimages[i].getTitle())) {
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
    
    public void setSyncROIsAcrossPlanes(boolean bSync) {
        bSyncROIsAcrossPlanes = bSync;
    }

    public boolean getSyncROIsAcrossPlanes() {
        return bSyncROIsAcrossPlanes;
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
    
    public MimsCBControl getCBControl(){
       return cbControl;
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

    public void setUpdating(boolean bool) {
        bUpdating = bool;
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
					//Prefs.setHomeDir(args[i+1]);
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
   private javax.swing.JMenuItem captureImageMenuItem;
   private javax.swing.JMenuItem closeAllHSIMenuItem;
   private javax.swing.JMenuItem closeAllRatioMenuItem;
   private javax.swing.JMenuItem closeAllSumMenuItem;
   private javax.swing.JMenu editMenu;
   private javax.swing.JMenuItem exitMenuItem;
   private javax.swing.JMenu fileMenu;
   private javax.swing.JMenuItem importIMListMenuItem;
   private javax.swing.JMenuBar jMenuBar1;
   private javax.swing.JMenuItem jMenuItem2;
   private javax.swing.JMenuItem jMenuItem3;
   private javax.swing.JMenuItem jMenuItem4;
   private javax.swing.JMenuItem jMenuItem6;
   private javax.swing.JMenuItem jMenuItem7;
   private javax.swing.JPanel jPanel1;
   private javax.swing.JSeparator jSeparator1;
   private javax.swing.JSeparator jSeparator2;
   private javax.swing.JSeparator jSeparator3;
   private javax.swing.JTabbedPane jTabbedPane1;
   private javax.swing.JTextField mainTextField;
   private javax.swing.JMenuItem openNewMenuItem;
   private javax.swing.JMenuItem sumAllMenuItem;
   private javax.swing.JMenu utilitiesMenu;
   private javax.swing.JMenu viewMenu;
   // End of variables declaration//GEN-END:variables

   private File extractFromZipfile(ZipFile zipFile, ZipEntry zipEntry, File destinationFile) {
                  
      // If no destination specified, use temp directory.
      if (destinationFile == null) {
         File destinationDir = new File(System.getProperty("java.io.tmpdir"));
         if (!destinationDir.canRead() || !destinationDir.canWrite())
            return null;
         destinationFile = new File(destinationDir, zipEntry.getName());
      }
                                             
      try {
         // Create input and output streams.
         byte[] buf = new byte[2048]; int n;
         BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
         FileOutputStream fos = new FileOutputStream(destinationFile);
         BufferedOutputStream bos = new BufferedOutputStream(fos, buf.length);
         
         // Write the file.         
         while ((n = bis.read(buf, 0, buf.length)) != -1) {
            bos.write(buf, 0, n);
         }
         
         // Close all streams.
         bos.flush();
         bos.close();
         fos.close();
         bis.close();
                  
      } catch (Exception e) {e.printStackTrace();}      
      
      destinationFile.deleteOnExit();
      return destinationFile;
   }
}

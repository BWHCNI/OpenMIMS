/*
 * UI.java
 *
 * Created on May 1, 2006, 12:59 PM
 */
package com.nrims;

import com.nrims.data.*;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ImageWindow;
import ij.gui.ImageCanvas;
import ij.io.RoiEncoder;
import ij.process.ImageStatistics;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Point;
import java.awt.Image;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/* 
 * The main user interface of the NRIMS ImageJ plugin.
 */
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
public class UI extends PlugInJFrame implements WindowListener, MimsUpdateListener {

    public static final long serialVersionUID = 1;
    public static final String NRRD_EXTENSION = ".nrrd";
    public static final String MIMS_EXTENSION = ".im";
    public static final String ROIS_EXTENSION = ".rois.zip";
    public static final String RATIO_EXTENSION = ".ratio";
    public static final String HSI_EXTENSION = ".hsi";
    public static final String SUM_EXTENSION = ".sum";

    private int maxMasses = 8;
    private int ratioScaleFactor = 10000;
    private double medianFilterRadius = 1;
    
    private boolean bDebug = false;    
    private boolean bSyncStack = true;
    private boolean bSyncROIs = true;
    private boolean bSyncROIsAcrossPlanes = true;
    private boolean bAddROIs = true;
    private boolean bUpdating = false;    
    private boolean currentlyOpeningImages = false;
    private boolean bCloseOldWindows = true;
    private boolean medianFilterRatios = false;
    private boolean isSum = false;
    private boolean isWindow = false;
    private int     windowRange = -1;
    private boolean[] bOpenMass = new boolean[maxMasses];
            
    private String lastFolder = null;      
    public  File   tempActionFile;
    public  String tempActionFileString = ".act";
    public  String actionFileName = "action.txt";         
    
            
    private HashMap openers = new HashMap();
    
    private MimsPlus[] massImages = new MimsPlus[maxMasses];
    private MimsPlus[] ratioImages = new MimsPlus[maxMasses];
    private MimsPlus[] hsiImages = new MimsPlus[maxMasses];
    private MimsPlus[] segImages = new MimsPlus[maxMasses];
    private MimsPlus[] sumImages = new MimsPlus[2 * maxMasses];    
    private MimsData mimsData = null;
    private MimsLog mimsLog = null;
    private MimsRoiControl roiControl = null;    
    private MimsCBControl cbControl = new MimsCBControl(this);
    private MimsStackEditing mimsStackEditing = null;
    private MimsRoiManager roiManager = null;
    private MimsTomography mimsTomography = null;        
    private HSIView hsiControl = null;
    private SegmentationForm segmentation = null;    
    private javax.swing.JRadioButtonMenuItem[] viewMassMenuItems = null;
    private Opener image = null;
    private ij.ImageJ ijapp = null;
    private FileDrop mimsDrop;    
    
    private Point[] windowPositions = null;

    protected MimsLineProfile lineProfile;
    protected MimsAction mimsAction = null;
    protected Roi activeRoi;    
    
    // fileName name of the .im image file to be opened.
    public UI(String fileName) {
      super("NRIMS Analysis Module");

      System.out.println("Ui constructor");
      System.out.println(System.getProperty("java.version") + " : " + System.getProperty("java.vendor"));

      // Set look and feel to native OS
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         SwingUtilities.updateComponentTreeUI(this);
      } catch (Exception e) {
         IJ.log("Error setting native Look and Feel:\n" + e.toString());
      }

      initComponents();
      initComponentsCustom();

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
         if (xloc + ijapp.getWidth() + this.getPreferredSize().width + 10 <
                 Toolkit.getDefaultToolkit().getScreenSize().width) {
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

      this.mimsDrop = new FileDrop(null, jTabbedPane1, new FileDrop.Listener() {
         public void filesDropped(File[] files) {
            openFiles(files);
         }
      });
   }

    /**
     * Closes the current image and its associated set of windows if the mode is set to close open windows.
     */
    private synchronized void closeCurrentImage() {
        // TODO why is this 6? Changed to maxMasses
        this.windowPositions = gatherWindowPosistions();
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

    public synchronized void loadMIMSFile() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setMultiSelectionEnabled(true);        
        fc.setPreferredSize(new java.awt.Dimension(650, 500));

        if (lastFolder != null) {
            fc.setCurrentDirectory(new java.io.File(lastFolder));
        } else {
            String ijDir = new ij.io.OpenDialog("", "asdf").getDefaultDirectory();
            if(ijDir != null && !(ijDir.equalsIgnoreCase("")) )
                fc.setCurrentDirectory(new java.io.File(ijDir));
        }

        if (fc.showOpenDialog(this) == JFileChooser.CANCEL_OPTION) {
            lastFolder = fc.getCurrentDirectory().getAbsolutePath();
            return;
        }
        lastFolder = fc.getSelectedFile().getParent();
        setIJDefaultDir(lastFolder);

        File[] files = fc.getSelectedFiles();
        openFiles(files);
    }

    public void openFiles(File[] files) {

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.getAbsolutePath().endsWith(NRRD_EXTENSION) ||
                    file.getAbsolutePath().endsWith(MIMS_EXTENSION)) {
                    loadMIMSFile(file);
                    break;
                }
            }

            try {
            // Loop thru files.
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (!file.exists()) continue;

                // Load ratio image.
                if (file.getAbsolutePath().endsWith(RATIO_EXTENSION)) {
                    FileInputStream f_in = new FileInputStream(file);
                    ObjectInputStream obj_in = new ObjectInputStream(f_in);
                    Object obj = obj_in.readObject();
                    if (obj instanceof HSIProps) {
                        HSIProps hsiprops = (HSIProps)obj;
                        String dataFileString = hsiprops.getDatFileName();
                        File dataFile = new File(file.getParent(), dataFileString);
                        if (image == null)
                            loadMIMSFile(dataFile);
                        else if (image != null && !dataFileString.matches(image.getImageFile().getName()))
                            loadMIMSFile(dataFile);
                        computeRatio(hsiprops, true);
                    }
                }

                // Load hsi image.
                if (file.getAbsolutePath().endsWith(HSI_EXTENSION)) {
                    FileInputStream f_in = new FileInputStream(file);
                    ObjectInputStream obj_in = new ObjectInputStream(f_in);
                    Object obj = obj_in.readObject();
                    if (obj instanceof HSIProps) {
                        HSIProps hsiprops = (HSIProps)obj;
                        String dataFileString = hsiprops.getDatFileName();
                        File dataFile = new File(file.getParent(), dataFileString);
                        if (image == null)
                            loadMIMSFile(dataFile);
                        else if (image != null && !dataFileString.matches(image.getImageFile().getName()))
                            loadMIMSFile(dataFile);
                        computeHSI(hsiprops);
                    }
                }

                // Load sum image.
                if (file.getAbsolutePath().endsWith(SUM_EXTENSION)) {
                    FileInputStream f_in = new FileInputStream(file);
                    ObjectInputStream obj_in = new ObjectInputStream(f_in);
                    Object obj = obj_in.readObject();
                    if (obj instanceof SumProps) {
                        SumProps sumprops = (SumProps)obj;
                        String dataFileString = sumprops.getDataFileName();
                        File dataFile = new File(file.getParent(), dataFileString);
                        if (image == null)
                            loadMIMSFile(dataFile);
                        else if (image != null && !dataFileString.matches(image.getImageFile().getName()))
                            loadMIMSFile(dataFile);
                        computeSum(sumprops);
                    }
                }
            }
            int slice = massImages[0].getCurrentSlice();
            massImages[0].setSlice(slice+1);

            } catch (Exception e) {

            } finally {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }


    }

    public synchronized void loadMIMSFile(File file) throws NullPointerException {
        if (!file.exists()) {
            throw new NullPointerException("File " + file.getAbsolutePath() + " does not exist!");
        }

        try {
            currentlyOpeningImages = true;

            closeCurrentImage();

            try {

                //need to add checks
                if (file.getName().endsWith(MIMS_EXTENSION)) {
                   image = new Mims_Reader(file);
                } else if (file.getName().endsWith(NRRD_EXTENSION)) {
                   image = new Nrrd_Reader(file);
                } else {
                   return;
                }
               
            } catch (IOException e) {
                IJ.log("Failed to open " + file + "......  :\n" + e.getStackTrace());
                return;
            }

            int nMasses = image.getNMasses();
            int nImages = image.getNImages();

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

                String[] names = image.getMassNames();
                for (int i = 0; i < image.getNMasses(); i++) {
                    String msg = "Open mass " + names[i];
                    gd.addCheckbox(msg, bOpenMass[i]);
                }
                gd.addNumericField("Open only ", (double) canOpen, 0, 5, " of " + image.getNImages() + " Images");

                gd.showDialog();
                if (gd.wasCanceled()) {
                    image = null;
                    return;
                }

                nMasses = 0;
                for (int i = 0; i < image.getNMasses(); i++) {
                    bOpenMass[i] = gd.getNextBoolean();
                    if (bOpenMass[i]) {
                        nMasses++;
                    }
                }

                nImages = (int) gd.getNextNumber();

                memRequired = nMasses * image.getWidth() * image.getHeight() * 2 * nImages;
            }



            updateStatus("Opening " + file + " ....... " + nMasses + " masses " + nImages + " sections");

            try {
                int n = 0;
                int t = image.getNMasses() * nImages;
                for (int i = 0; i < image.getNMasses(); i++) {
                    IJ.showProgress(++n, t);
                    if (bOpenMass[i]) {
                        MimsPlus mp = new MimsPlus(this, image, i);
                        mp.setAllowClose(false);
                        massImages[i] = mp;
                        //double dMin = (double) image.getMin(i);
                        //double dMax = (double) image.getMax(i);
                        if (mp != null) {
                            massImages[i].getProcessor().setMinAndMax(0, 0);
                            massImages[i].getProcessor().setPixels(image.getPixels(i));                            
                        }
                    }
                }
               
                if (nImages > 1) {
                    // TODO why are we starting from 1 here?
                    for (int i = 1; i < nImages; i++) {
                        image.setStackIndex(i);
                        for (int mass = 0; mass < image.getNMasses(); mass++) {
                            IJ.showProgress(++n, t);
                            if (bOpenMass[mass]) {
                                massImages[mass].appendImage(i);
                            }
                        }
                    }
                }

                for (int i = 0; i < image.getNMasses(); i++) {
                    if (bOpenMass[i]) {
                        if (image.getNImages() > 1) {
                            massImages[i].setSlice(1);
                        }
                        massImages[i].show();
                    }
                }

                if(this.windowPositions != null) {
                    applyWindowPositions(windowPositions);
                } else {
                    //replace with mass image tile
                ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();

                wo.run("tile");
                }

            } catch (Exception x) {
                updateStatus(x.toString());
                x.printStackTrace();
            }

            for (int i = 0; i < image.getNMasses(); i++) {
                if (bOpenMass[i]) {
                    massImages[i].addListener(this);
                } else {
                    massImages[i] = null;
                }
            }

            if (mimsData == null) {
                initializeViewMenu();
                mimsData = new com.nrims.MimsData(this, image);
                roiControl = new MimsRoiControl(this);
                hsiControl = new HSIView(this);
                mimsLog = new MimsLog(this, image);
                mimsStackEditing = new MimsStackEditing(this, image);
                mimsTomography = new MimsTomography(this, image);
                mimsAction = new MimsAction(this, image);
                //TODO: throws an exception when opening an image with 2 masses
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
                resetViewMenu();
                mimsData = new com.nrims.MimsData(this, image);
                hsiControl = new HSIView(this);
                cbControl = new MimsCBControl(this);
                mimsStackEditing = new MimsStackEditing(this, image);
                mimsTomography = new MimsTomography(this, image);
                mimsAction = new MimsAction(this, image);
                //TODO: throws an exception when opening an image with 2 masses
                segmentation = new SegmentationForm(this);
                jTabbedPane1.setComponentAt(0, mimsData);
                jTabbedPane1.setTitleAt(0, "MIMS Data");
                jTabbedPane1.setComponentAt(1, hsiControl);
                jTabbedPane1.setComponentAt(2, cbControl);
                jTabbedPane1.setComponentAt(3, roiControl);
                jTabbedPane1.setComponentAt(4, mimsStackEditing);
                jTabbedPane1.setComponentAt(5, mimsTomography);
                jTabbedPane1.setComponentAt(6, segmentation);

                mimsData.setMimsImage(image);
                hsiControl.updateImage();                
            }
            
            jTabbedPane1.addChangeListener(new ChangeListener() {
               public void stateChanged(ChangeEvent e){
                  int selected = jTabbedPane1.getSelectedIndex();
                  if (selected == 2) {
                     cbControl.updateHistogram();
                  } 
               }
            });

            this.mimsLog.Log("\n\nNew image: " + getImageFilePrefix() + "\n" + getImageHeader(image));
            this.mimsTomography.resetBounds();
            this.mimsTomography.resetImageNamesList();
            this.mimsStackEditing.resetSpinners();

            //????????????????
            openers.clear();
            String fName = file.getName();
            openers.put(fName, image);            
            
            // Add the windows to the combobox in CBControl.            
            MimsPlus[] mp = getOpenMassImages();
            for(int i = 0; i < mp.length; i++) {
               cbControl.addWindowtoList(mp[i]);
            }    
            
        } finally {
            currentlyOpeningImages = false;
        }        
    }

    public Point[] gatherWindowPosistions() {
        Point[] positions = new Point[maxMasses];

        MimsPlus[] images = this.getOpenMassImages();
        if(images.length==0) return null;

        for( int i = 0; i < images.length; i++) {
            if( images[i].getWindow() != null)
                positions[i] = images[i].getWindow().getLocation();
        }

        return positions;
    }

    public void applyWindowPositions(Point[] positions) {
        for(int i = 0; i < positions.length; i++) {
           if ( positions[i] != null && massImages[i] != null)
               if (massImages[i].getWindow() != null)
                massImages[i].getWindow().setLocation(positions[i]);

        }
    }

    private void resetViewMenu() {
        for (int i = 0; i < viewMassMenuItems.length; i++) {
            if (i < image.getNMasses()) {
                viewMassMenuItems[i].setText(image.getMassNames()[i]);
                viewMassMenuItems[i].setVisible(true);
            } else {
                viewMassMenuItems[i].setText("foo");
                viewMassMenuItems[i].setVisible(false);
            }

        }
    }

    private void initializeViewMenu() {
        this.viewMassMenuItems = new javax.swing.JRadioButtonMenuItem[this.maxMasses];

        for (int i = 0; i < viewMassMenuItems.length; i++) {
            javax.swing.JRadioButtonMenuItem massRButton = new javax.swing.JRadioButtonMenuItem();

            if (i < image.getNMasses()) {
                massRButton.setSelected(true);
                massRButton.setText(image.getMassNames()[i]);
                massRButton.setVisible(true);
            } else {
                massRButton.setSelected(false);
                massRButton.setText("foo");
                massRButton.setVisible(false);
            }

            massRButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    viewMassChanged(evt);
                }
            });
            viewMassMenuItems[i] = massRButton;

            this.viewMenu.add(massRButton);
        }

    }

    private void viewMassChanged(java.awt.event.ActionEvent evt) {
        int index = 0;
        for (int i = 0; i < viewMassMenuItems.length; i++) {
            if (evt.getActionCommand() == viewMassMenuItems[i].getText()) {
                index = i;
            }
        }
        if (massImages[index] != null) {
            if (viewMassMenuItems[index].isSelected() && !massImages[index].isVisible()) {
                //int plane = getVisableMassImages()[0].getSlice();
                massImages[index].show();
                if(windowPositions[index] != null) massImages[index].getWindow().setLocation(windowPositions[index]);
                massImages[index].setbIgnoreClose(true);
                //massImages[index].setSlice(plane);
                //massImages[index].updateAndDraw();
            } else if( !viewMassMenuItems[index].isSelected() && massImages[index].isVisible()) {
                massImages[index].hide();
            }
        }


        System.out.print(evt.getActionCommand() + " index: " + index);
        System.out.print(" selected: " + viewMassMenuItems[index].isSelected());
        System.out.print(" visable: " + massImages[index].isVisible() + "\n");
    }

    public void massImageClosed(MimsPlus im) {
        for (int i = 0; i < massImages.length; i++) {
            if (massImages[i] == im) {
                viewMassMenuItems[i].setSelected(false);
            }
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
        MimsPlus mp[] = getOpenRatioImages();
        for (int i = 0; i < mp.length; i++) {
                if (mp[i].getNumMass() == numIndex && mp[i].getDenMass() == denIndex) {
                    return i;
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

    public synchronized MimsPlus computeRatio(HSIProps props, boolean show) {

        int numIndex = props.getNumMass();
        int denIndex = props.getDenMass();
        MimsPlus mp = null;

        // If were not showing the image than forget trying 
        // to see if one already exists (ratioIndex >= 0).
        int ratioIndex;
        if (show)
           ratioIndex = getRatioImageIndex(numIndex, denIndex);
        else {
           ratioIndex = -1;
           mp = new MimsPlus(this, image, props, false);
        }

        // Get the numerator and denominator mass images.
        MimsPlus parentNum = getMassImage( props.getNumMass() );
        MimsPlus parentDen = getMassImage( props.getDenMass() );

        // Setup list for sliding window, entire image, or single plane.
        java.util.ArrayList<Integer> list = new java.util.ArrayList<Integer>();
        int windowSize = getWindowRange();
        int currentplane = parentNum.getSlice();
            if (!getIsSum() && !getIsWindow()) {
                list.add(currentplane);
            } else if (getIsSum()) {
                for (int i = 1; i <= parentNum.getNSlices(); i++) {
                    list.add(i);
                }
            } else if (getIsWindow()) {
                int lb = currentplane - windowSize;
                int ub = currentplane + windowSize;
                for (int i = lb; i <= ub; i++) {
                    list.add(i);
                }
            }

        // Compute the sum of the numerator and denominator mass images.
        MimsPlus num = computeSum(parentNum, false, list);
        MimsPlus den = computeSum(parentDen, false, list);        

        if (ratioIndex >= 0) {
            mp = ratioImages[ratioIndex];
        }
        boolean bAddSlice = false;
        if (mp != null) {
            if (mp.getNSlices() > 1) {
                mp = null;
                bAddSlice = true;
            }
        }

        if (mp == null) {
            mp = new MimsPlus(this, image, props, false);
            if (bAddSlice) {
                ij.ImageStack stack = ratioImages[ratioIndex].getStack();
                stack.addSlice(null, mp.getProcessor(), num.getCurrentSlice());
                ratioImages[ratioIndex].setStack(null, stack);
                ratioImages[ratioIndex].setSlice(num.getCurrentSlice());
            } else {
                // find a slot to save it
                boolean bFound = false;
                for (int i = 0; i < maxMasses && !bFound; i++) {
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
            }
        }

        if (mp == null) {
            updateStatus("Error allocating ratio image");
            return null;
        }

        float[] rPixels = (float[]) mp.getProcessor().getPixels();
        float[] nPixels = (float[]) num.getProcessor().getPixels();
        float[] dPixels = (float[]) den.getProcessor().getPixels();

        float rMax = 0.0f;
        float rMin = 1000000.0f;
        for (int i = 0; i < rPixels.length; i++) {
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
        if (getMedianFilterRatios()) {
            Roi temproi = mp.getRoi();
            mp.killRoi();
            ij.plugin.filter.RankFilters rfilter = new ij.plugin.filter.RankFilters();
            double r = getMedianFilterRadius();
            rfilter.rank(mp.getProcessor(), r, rfilter.MEDIAN);
            rfilter = null;
            mp.setRoi(temproi);
        }

        if (show) {                      
            mp.show();
            
           // Set window location.
           int xloc = props.getXWindowLocation();
           int yloc = props.getYWindowLocation();           
           if (xloc > -1 & yloc > -1)
              mp.getWindow().setLocation(xloc, yloc);
            
            mp.updateAndDraw();
            ij.measure.Calibration cal = new ij.measure.Calibration(mp);
            mp.setCalibration(cal);
        } else {
            mp.updateAndRepaintWindow();
        }

        cbControl.addWindowtoList(mp);
        return mp;
    }

    public static String getImageHeader(Opener im) {
        //String str = "\nHeader: \n";
        //str += "Path: " + im.getImageFile().getAbsolutePath() + "/" + im.getName() + "\n";

        // WE HAVE TO DECIDE WHAT WE WANT.
        String[] names = im.getMassNames();


        String str = "\nHeader: \n";
        str += "Path: " + im.getImageFile().getAbsolutePath() + "\n";
        str += "Masses: ";
        for (int i = 0; i < im.getNMasses(); i++) {str += names[i] + " ";}
        str += "\n";
        str += "Pixels: " + im.getWidth() + "x" + im.getHeight() + "\n";
        /*
        str += "Raster (nm): " + im.getRaster() + "\n";
        str += "Duration (s): " + im.getDuration() + "\n";
        str += "Dwell time (ms/xy): " + im.getDwellTime() + "\n";
        str += "Stage Position: " + im.getPosition() + "\n";
        str += "Sample name: " + im.getSampleName() + "\n";
        str += "Sample date: " + im.getSampleDate() + "\n";
        str += "Sample hour: " + im.getSampleHour() + "\n";
        str += "Pixel width (nm): " + im.getPixelWidth() + "\n";
        str += "Pixel height (nm): " + im.getPixelHeight() + "\n";
        */
        str += "End header.\n\n";
        return str;
    }
    
    public void updateAllImages() {
        for (int i = 0; i < maxMasses; i++) {
            if (segImages[i] != null) {
                segImages[i].updateAndDraw();
                segImages[i].killRoi();
            }
            if (massImages[i] != null) {
                massImages[i].updateAndDraw();
                massImages[i].killRoi();
            }
            if (hsiImages[i] != null) {
                hsiImages[i].updateAndDraw();
                hsiImages[i].killRoi();
            }
            if (ratioImages[i] != null) {
                ratioImages[i].updateAndDraw();
                ratioImages[i].killRoi();
            }
        }

        for (int i = 0; i < maxMasses * 2; i++) {
            if (sumImages[i] != null) {
                sumImages[i].updateAndDraw();
                sumImages[i].killRoi();
            }
        }
    }

    public void recomputeAllRatio() {
        MimsPlus[] openRatio = this.getOpenRatioImages();
        for (int i = 0; i < openRatio.length; i++) {
            //median radius is global
                computeRatio(openRatio[i].getHSIProps(), true);
                openRatio[i].updateAndDraw();            
        }      
        cbControl.updateHistogram();
    }

    public void recomputeAllHSI() {
        MimsPlus[] openHSI = this.getOpenHSIImages();
        for (int i = 0; i < openHSI.length; i++) {
                computeHSI(hsiImages[i].getHSIProps());
                openHSI[i].updateAndDraw();            
        }        
    }        
    
    public void computeSum(String parentImageName){
       MimsPlus mp = getImageByName(parentImageName);
       computeSum(mp, true);
    }
    
    public void computeSum(SumProps sumProps) {
       
       // fraction for which we consider masses to be equals. (0.01 = 1%)       
       double tolerance = 0.01;                            
       int num_idx = -1;
       int den_idx = -1;
       MimsPlus mp = null;       
       
       // Generates a new sum image from a mass image.
       if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
          for (int i = 0; i < massImages.length; i++) {
             mp = massImages[i];
             if (mp == null) continue;
             
             // If the difference between the masses for the parentMass of the old sum image 
             // and the new mass image is than 1%, treat as same and generate a new sum image. 
             if (Math.abs(sumProps.getParentMass()-mp.getMassNumber()) < tolerance*sumProps.getParentMass()) {
                MimsPlus sp = computeSum(mp, true); 
                sp.getWindow().setLocation(sumProps.getXWindowLocation(), sumProps.getYWindowLocation());             
             }
          }             
       } 
       // Generates a new sum image from a ratio image.
       else if (sumProps.getSumType() == SumProps.RATIO_IMAGE)
          for (int i = 0; i < massImages.length; i++) {
             mp = massImages[i];
             if (mp == null) continue;                          
             
             // Difference between current mass values and those in SumProps.
             double num_diff = Math.abs(sumProps.getNumMass()-mp.getMassNumber());
             double den_diff = Math.abs(sumProps.getDenMass()-mp.getMassNumber());
             
             // If difference is less than 1% assign it to numerator (or denominator).
             if (num_diff < tolerance*sumProps.getNumMass())
                num_idx = i;
             else if (den_diff < tolerance*sumProps.getDenMass())
                den_idx = i;
             else 
                continue;
             
             // If suitable numerator and denominator mass images are found,
             // assign those indexes to a props object, generate an invisible 
             // ratio image, and then generate a sum image from that ratio image.
             if (num_idx > -1 && den_idx > -1) {
                HSIProps props = new HSIProps();
                props.setNumMass(num_idx);
                props.setDenMass(den_idx);        
                MimsPlus rp = computeRatio(props, false);
                MimsPlus sp = computeSum(rp, true);
                sp.getWindow().setLocation(sumProps.getXWindowLocation(), sumProps.getYWindowLocation());                           
             }                
          }          
    }
    
    public MimsPlus computeSum(MimsPlus mImage, boolean show) {
       
       ArrayList<Integer> sumlist = new ArrayList<Integer>();
       for (int i = 1; i <= mimsAction.getSize(); i++) {
          sumlist.add(i);
       }
       return computeSum(mImage, show, sumlist);
       
    }
    
    public MimsPlus computeSum(MimsPlus mImage, boolean show, ArrayList<Integer> sumlist) {
        boolean fail = true;
        
        if (mImage == null) {
            return null;
        }
                    
        int width = mImage.getWidth();
        int height = mImage.getHeight();        
        String sumName = "Sum : " + mImage.getShortTitle() + " : " + image.getImageFile().getName();
        int templength = mImage.getProcessor().getPixelCount();
        double[] sumPixels = new double[templength];
        short[] tempPixels = new short[templength];
        SumProps sumProps = null;

        int startSlice = mImage.getCurrentSlice();
        bUpdating = true;        
        
        if (mImage.getMimsType() == MimsPlus.MASS_IMAGE) {                                                         
            sumProps = new SumProps(mImage.getMassNumber());            
            Object[] o = mImage.getStack().getImageArray();
            for (int i = 0; i < sumlist.size(); i++) {
                if (sumlist.get(i) < 1 || sumlist.get(i) > mImage.getNSlices()) continue;                
                tempPixels = (short[])o[sumlist.get(i)-1];
                for (int j = 0; j < sumPixels.length; j++) {                 
                    sumPixels[j] += ((int) ( tempPixels[j] & 0xffff) );
                }
            }         
            fail = false;
        }

        MimsPlus nImage = null;
        MimsPlus dImage = null;
        if (mImage.getMimsType() == MimsPlus.RATIO_IMAGE) {
            sumProps = new SumProps(mImage.getNumerMassNumber(), mImage.getDenomMassNumber());
            int numMass = mImage.getNumMass();
            int denMass = mImage.getDenMass();

            // Get the sum images of the numerator and denominator.
            nImage = computeSum(massImages[numMass], false, sumlist);
            dImage = computeSum(massImages[denMass], false, sumlist);
            
            float[] numPixels = (float[]) nImage.getProcessor().getPixels();
            float[] denPixels = (float[]) dImage.getProcessor().getPixels();

            for (int i = 0; i < sumPixels.length; i++) {
                if (denPixels[i] != 0) {
                    sumPixels[i] = ratioScaleFactor * (numPixels[i] / denPixels[i]);
                } else {
                    sumPixels[i] = 0;
                }
            }

            fail = false;
        }
        mImage.setSlice(startSlice);
        this.bUpdating = false;
        
        if (!fail) {
            MimsPlus mp = new MimsPlus(this, width, height, sumPixels, sumName);
            mp.setSumProps(sumProps);
            mp.setNumeratorSum(nImage);
            mp.setDenominatorSum(dImage);

            if(show==false) return mp;

            // if showing find a slot to save it

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
                this.autoContrastImage(mp);
            }
            mp.updateAndDraw();

            cbControl.addWindowtoList(mp);
            return mp;
        } else {
            return null;
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
        if(props==null) return false;

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

        MimsPlus mp = null;
        if (hsiIndex != -1) {
            mp = hsiImages[hsiIndex];
        }
        boolean bShow = mp == null;

        if (!bShow) {
            HSIProps oldProps = mp.getHSIProcessor().getProps();
            HSIProcessor proc = new HSIProcessor(mp);
            proc.setProps(props);
            mp.setHSIProcessor(proc);
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
            mp = new MimsPlus(this, image, props, true);
            mp.setHSIProcessor(new HSIProcessor(mp));
            boolean bFound = false;
            for (int i = 0; i < maxMasses && !bFound; i++) {
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
           
           // Set window location.
           int xloc = props.getXWindowLocation();
           int yloc = props.getYWindowLocation();           
           if (xloc > -1 & yloc > -1)
              mp.getWindow().setLocation(xloc, yloc);
        }

        return true;
    }

    /**
     * Catch events such as changing the slice number of a stack
     * or drawing ROIs and if enabled,  update or synchronize all images
     * @param evt
     */
    @Override
    public synchronized void mimsStateChanged(MimsPlusEvent evt) {

        // Do not call updateStatus() here - causes a race condition..
        if (currentlyOpeningImages || bUpdating)
            return;
        bUpdating = true;

        // Sychronize stack displays.
        if (bSyncStack && evt.getAttribute() == MimsPlusEvent.ATTR_UPDATE_SLICE) {
            MimsPlus mp[] = this.getOpenMassImages();
            MimsPlus rp[] = this.getOpenRatioImages();
            MimsPlus hsi[] = this.getOpenHSIImages();

            // Set mass images.
            int nSlice = evt.getSlice();
            for (int i = 0; i < mp.length; i++) {
               mp[i].setSlice(nSlice);
            }                                                    

            // Update HSI image slice.
            for (int i = 0; i < hsi.length; i++) {                
                hsi[i].recomputeInternalImages();
                computeHSI(hsi[i].getHSIProps());
            }

            // Update ratio images.
            for (int i = 0; i < rp.length; i++) {                                                                     
              computeRatio(rp[i].getHSIProps(), true);
            }                            

            autocontrastAllImages();
            cbControl.updateHistogram();

        } else if (evt.getAttribute() == MimsPlusEvent.ATTR_IMAGE_CLOSED) {
            // If an image was closed by a window event,
            // dispose the corresponding reference.            
            boolean bNotFound = true;
            int i;
            for (i = 0; bNotFound && i < image.getNMasses(); i++) {
                if (massImages[i] == evt.getSource()) {
                    massImages[i].removeListener(this);
                    massImages[i] = null;
                    bNotFound = false;
                }
            }
            for (i = 0; bNotFound && i < maxMasses; i++) {
                if (sumImages[i] == evt.getSource()) {
                    sumImages[i].removeListener(this);
                    sumImages[i] = null;
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
            // Update all images with a selected ROI 
            // MOUSE_RELEASE catches drawing new ROIs             
            if (bSyncROIs) {
                int i;
                MimsPlus mp = (MimsPlus) evt.getSource();
                for (i = 0; i < image.getNMasses(); i++) {
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
                for (i = 0; i < sumImages.length; i++) {
                    if (sumImages[i] != mp && sumImages[i] != null) {
                        sumImages[i].setRoi(evt.getRoi());
                    }
                }
            }
            // Automatically appends a drawn ROI to the RoiManager
            // to improve work flow without extra mouse actions.             
            if (bAddROIs && evt.getAttribute() == MimsPlusEvent.ATTR_MOUSE_RELEASE) {
                ij.gui.Roi roi = evt.getRoi();
                if (roi != null && roi.getState() != Roi.CONSTRUCTING) {
                    MimsRoiManager rm = getRoiManager();
                    rm.add();
                    rm.showFrame();
                }
            }

        } else if (evt.getAttribute() == MimsPlusEvent.ATTR_ROI_MOVED) {
            MimsRoiManager rm = getRoiManager();
            rm.move();
        }

        bUpdating = false;

        // had to wait untill not changing....
        // System.out.println("mims state changed...");
        this.mimsStackEditing.resetTrueIndexLabel();
        this.mimsStackEditing.resetSpinners();
    }

   // This method returns the name of the main image file without the extension.
   public String getImageFilePrefix() {
      String filename = image.getImageFile().getName().toString();
      String prefix = filename.substring(0, filename.lastIndexOf("."));
      return prefix;
   }
   
   // Save a temporary backup of the action file.
   public File saveTempActionFile(){
      String tempBackupFileName = getImageFilePrefix() + tempActionFileString;
      tempActionFile = new File(image.getImageFile().getParent(), tempBackupFileName);
      getmimsAction().writeAction(tempActionFile);          
      return tempActionFile;
   }
   
   // Delete the temporary backup action file.
   public void deleteTempActionFile(){
      if (tempActionFile != null){
         if (tempActionFile.exists())
            tempActionFile.delete();
      }
   }
    
   private void initComponentsCustom() {

      // Open action.
      jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            try {
               openActionEvent(evt);
            } finally {
               setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
         }
      });
      
      // save action.
      jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            try {
               saveAction(evt);
            } finally {
               setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
         }
      });          
      
      // Save session.
      jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
             String fileName;
             try {
                 // User sets file prefix name
                 JFileChooser fc = new JFileChooser();
                 if (lastFolder != null) {
                     fc.setCurrentDirectory(new java.io.File(lastFolder));
                 }
                 int returnVal = fc.showSaveDialog(jTabbedPane1);                 
                 if (returnVal == JFileChooser.APPROVE_OPTION) {                     
                     fileName = fc.getSelectedFile().getAbsolutePath();
                     File file  = new File(fileName);
                     lastFolder = file.getParent();
                     setIJDefaultDir(lastFolder);
                     setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                     saveSession(fileName);                     
                 } else {
                     return;
                 }
             } finally {
                 setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
             }
         }
      });
   }
   
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jMenuItem9 = new javax.swing.JMenuItem();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        mainTextField = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openNewMenuItem = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        jMenuItem6 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenuItem11 = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JSeparator();
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
        jSeparator4 = new javax.swing.JSeparator();
        genStackMenuItem = new javax.swing.JMenuItem();
        TestMenuItem = new javax.swing.JMenuItem();

        jMenuItem9.setText("Export all images");

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
        openNewMenuItem.setToolTipText("Open a MIMS image.");
        openNewMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMIMSImageMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openNewMenuItem);
        openNewMenuItem.getAccessibleContext().setAccessibleDescription("Open a MIMS Image");

        jMenuItem6.setText("Save MIMS");
        fileMenu.add(jMenuItem6);

        fileMenu.add(jSeparator6);

        //jMenuItem1.setText("Save Action File");
        //fileMenu.add(jMenuItem1);

        //jMenuItem5.setText("Open Action File");
        //fileMenu.add(jMenuItem5);
        //fileMenu.add(jSeparator5);

      //jMenuItem11.setText("Export Stack As Nrrd Format");
      //jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
      //   public void actionPerformed(java.awt.event.ActionEvent evt) {
      //      jMenuItem11ActionPerformed(evt);
      //   }
      //});
      //fileMenu.add(jMenuItem11);

      //  jMenuItem8.setText("Export current image");
      //  fileMenu.add(jMenuItem8);

      //  jMenuItem10.setText("Export all images");
      //  fileMenu.add(jMenuItem10);
      //  fileMenu.add(jSeparator7);

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
        utilitiesMenu.add(jSeparator4);

        genStackMenuItem.setText("Generate Stack");
        genStackMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                genStackMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(genStackMenuItem);

        TestMenuItem.setText("Test");
        TestMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TestMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(TestMenuItem);

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
    }// </editor-fold>                        

    /**
     * restores any closed or modified massImages
     */
    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {                                           

        mimsStackEditing.untrack();
        int currentSlice = massImages[0].getCurrentSlice();

        // concatenate the remaining files.
        int x = mimsAction.getSize();
        for (int i = 1; i <= mimsAction.getSize(); i++) {
           massImages[0].setSlice(i);
           if (mimsAction.isDropped(i)) mimsStackEditing.insertSlice(i);
        }

        mimsStackEditing.resetTrueIndexLabel();
        mimsStackEditing.resetSpinners();

        massImages[0].setSlice(currentSlice);
    }

    public void setIsSum(boolean set) {
        isSum = set;
    }

    public boolean getIsSum() {
        return isSum;
    }

    public void setIsWindow(boolean set) {
        isWindow = set;
    }

    public boolean getIsWindow() {
        return isWindow;
    }

    public void setWindowRange(int range) {
        windowRange = range;
    }

    public int getWindowRange() {
        return windowRange;
    }
    
    public void setMedianFilterRatios(boolean set) {
        medianFilterRatios = set;
    }

    public boolean getMedianFilterRatios() {
        return medianFilterRatios;
    }

    public void autocontrastAllImages() {       
       // All mass images             
       MimsPlus mp[] = getOpenMassImages();
        for (int i = 0; i < mp.length; i++) {
            if (mp[i].getAutoContrastAdjust())
               autoContrastImage(mp[i]);
        }
        
        // All ratio images
        MimsPlus rp[] = getOpenRatioImages();
        for (int i = 0; i < rp.length; i++) {
           if (rp[i].getAutoContrastAdjust())
              autoContrastImage(rp[i]);
        }                
    }   
    
   // Custom contrasting code for ratio images.
   public void autocontrastNRIMS(MimsPlus img) {
      
      // Get the current image statistics.
      ImageStatistics imgStats = img.getStatistics();
      
      // Get the current image properties and override some fields.
      HSIProps props = img.getHSIProps();
      props.setMinRatio(java.lang.Math.max(0.0, imgStats.mean - (2 * imgStats.stdDev)));
      props.setMaxRatio(imgStats.mean + (imgStats.stdDev));
      
      // Generate new image.
      computeRatio(props, true);            
   }
   
   public void autoContrastImage(MimsPlus img) {
                  
      // Use Collins autocontrasting code for ratio images (and NOT medianizing).
      if (img.getMimsType() == MimsPlus.RATIO_IMAGE && !hsiControl.isMedianFilterSelected()) {
         autocontrastNRIMS(img);
      } 
      
      // Use Collins code for HSI images (and NOT medianizing). 
      else if (img.getMimsType() == MimsPlus.HSI_IMAGE && !hsiControl.isMedianFilterSelected()) {
         if (hsiControl.getRatioRange()) 
            hsiControl.update(true);
      } 
      
      // Anything else use imageJ autocontrasting. Have to reset everytime BEFORE
      // autoadjusting because imageJ autoadjust is iterative and would give
      // a different result everytime if reset was not done first.
      else {                     
         ContrastAdjuster ca = new ContrastAdjuster(img);
         
         // same as hitting reset button.
         ca.doReset = true;
         ca.doUpdate(img);                           
         
         // same as hitting auto button.
         ca.doAutoAdjust = true;
         ca.doUpdate(img);                           
      }
      
   }

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {                                           
      
       PrefFrame pf = new PrefFrame();
       pf.showFrame();
        
    }                                          

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {                                           
        ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
        wo.run("tile");
    }                                          

    private void openMIMSImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                                      
       
       // Get HSIProps for all open ratio images.
       MimsPlus[] rto = getOpenRatioImages();
       HSIProps[] rto_props = new HSIProps[rto.length]; 
       for (int i=0; i<rto.length; i++){
          rto_props[i] = rto[i].getHSIProps();
       }
       
       // Get HSIProps for all open hsi images.     
       MimsPlus[] hsi = getOpenHSIImages();
       HSIProps[] hsi_props = new HSIProps[hsi.length];
       for (int i=0; i<hsi.length; i++){
          hsi_props[i] = hsi[i].getHSIProps();
       }
       
       // Get SumProps for all open sum images.    
       MimsPlus[] sum = getOpenSumImages();
       SumProps[] sum_props = new SumProps[sum.length];
       for (int i=0; i<sum.length; i++){
          sum_props[i] = sum[i].getSumProps();          
       }
             
       // Load the new file.
       loadMIMSFile();
       
       // Generate all images that were previously open.
       restoreState(rto_props, hsi_props, sum_props);

       // Keep the HSIView GUI up to date.
       if (medianFilterRatios) {
           hsiControl.setIsMedianFiltered(true);
           hsiControl.setMedianFilterRadius(medianFilterRadius);
       }
       if (isSum) {
           hsiControl.setIsSum(true);
       }
       if (isWindow) {
           hsiControl.setIsWindow(true);
           hsiControl.setWindowRange(windowRange);
       }

}                                                     

    public void restoreState( HSIProps[] rto_props,  HSIProps[] hsi_props, SumProps[] sum_props){
       
       // Generate ratio images.
       for (int i=0; i<rto_props.length; i++){
          computeRatio(rto_props[i], true);
       }
       
       // Generate hsi images.
       for (int i=0; i<hsi_props.length; i++){
          computeHSI(hsi_props[i]);
          hsiControl.updateInternalImages();
       }
       
       MimsPlus[] openhsi = this.getOpenHSIImages();
       for (int i=0; i<openhsi.length; i++){
          openhsi[i].recomputeInternalImages();
       }

       // Generate sum images.
       for (int i=0; i<sum_props.length; i++){
          computeSum(sum_props[i]);       
       }
       
    }
    
    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {                                          
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

    }                                         

private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                             
    //System.exit(0);
    //TODO doesn't actually close...
    //deleteTempActionFile();
    this.close();
}                                            

private void sumAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                               
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
        this.computeSum(openmass[i], true);
    }
    for (int i = 0; i < openratio.length; i++) {
        this.computeSum(openratio[i], true);
    }
}                                              

private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                              

    String message = "OpenMIMS v0.7\n\n";
    message += "OpenMIMS was Developed at NRIMS, the National Resource\n";
    message += "for Imaging Mass Spectrometry.\n";
    message += "http://www.nrims.hms.harvard.edu/\n";
    message += "\nDeveloped by:\n Doug Benson, Collin Poczatek\n ";
    message += "Boris Epstein, Philipp Gormanns\n Stefan Reckow, ";
    message += "Rob Gonzales, Zeke Kaufman.";
    message += "\n\nOpenMIMS uses or depends upon:\n";
    message += " TurboReg:  http://bigwww.epfl.ch/thevenaz/turboreg/\n";
    message += " jFreeChart:  http://www.jfree.org/jfreechart/\n";
    message += " FileDrop:  http://iharder.sourceforge.net/current/java/filedrop/\n";

    javax.swing.JOptionPane pane = new javax.swing.JOptionPane(message);
    javax.swing.JDialog dialog = pane.createDialog(new javax.swing.JFrame(), "About OpenMIMS");

    dialog.setVisible(true);
}                                             

private void captureImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                                     
    //testing trying to grab screen pixels from image for rois and annotations

    // Captures the active image window and returns it as an ImagePlus.
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

}                                                    

private void importIMListMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                                     
    com.nrims.data.LoadImageList testLoad = new com.nrims.data.LoadImageList(this);
    boolean read;
    read = testLoad.openList();
    if (!read) {
        return;
    }

    testLoad.printList();
    testLoad.simpleIMImport();
    //this.mimsStackEditing.setConcatGUI(true);
}                                                    

private void closeAllRatioMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                                      
    for (int i = 0; i < ratioImages.length; i++) {
        if (ratioImages[i] != null) {
            ratioImages[i].close();
        }
    }
}                                                     

private void closeAllHSIMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                                    
    for (int i = 0; i < hsiImages.length; i++) {
        if (hsiImages[i] != null) {
            hsiImages[i].close();
        }
    }
}                                                   

private void closeAllSumMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                                    
    for (int i = 0; i < sumImages.length; i++) {
        if (sumImages[i] != null) {
            sumImages[i].close();
        }
    }
}                                                   

   // Saves a session.
   private void saveSession(String fileName) {

      // Initialize variables.
      File file = new File(fileName);
      String directory = file.getParent();
      String name = file.getName();
      FileOutputStream f_out;
      ObjectOutputStream obj_out;
      String objectFileName;

      try {

        // Save the original .im file to a new file of the .nrrd file type.
        String nrrdFileName = name+NRRD_EXTENSION;
        ImagePlus[] imp = getOpenMassImages();
		if (imp == null) return;
		Nrrd_Writer nw = new Nrrd_Writer(this);
        File dataFile = nw.save(imp, directory, nrrdFileName);
       
        // Save the ROI files to zip.
        String roisFileName = fileName+ROIS_EXTENSION;
        int[] indexes = getRoiManager().getAllIndexes();
        if (indexes.length > 0)
           getRoiManager().saveMultiple(indexes, roisFileName, false);

        // Contruct a unique name for each ratio image and save.
        MimsPlus ratio[] = getOpenRatioImages();
        if (ratio.length > 0){
        for (int i = 0; i < ratio.length; i++) {
           HSIProps hsiprops = ratio[i].getHSIProps();
           hsiprops.setDataFileName(dataFile.getName());
           int numIndex = hsiprops.getNumMass();
           int denIndex = hsiprops.getDenMass();
           int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
           int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
           objectFileName = fileName + "_m" + numMass + "_m" + denMass + RATIO_EXTENSION;
           f_out = new FileOutputStream(objectFileName);
           obj_out = new ObjectOutputStream(f_out);
           obj_out.writeObject(hsiprops);
        }}
        
        // Contruct a unique name for each hsi image and save.
        MimsPlus hsi[] = getOpenHSIImages();
        if (hsi.length > 0){
        for (int i = 0; i < hsi.length; i++) {
           HSIProps hsiprops = hsi[i].getHSIProps();
           hsiprops.setDataFileName(dataFile.getName());
           int numIndex = hsiprops.getNumMass();
           int denIndex = hsiprops.getDenMass();
           int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
           int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
           objectFileName = fileName + "_m" + numMass + "_m" + denMass + HSI_EXTENSION;
           f_out = new FileOutputStream(objectFileName);
           obj_out = new ObjectOutputStream(f_out);
           obj_out.writeObject(hsiprops);
        }}

        // Contruct a unique name for each sum image and save.
        MimsPlus sum[] = getOpenSumImages();
        if (sum.length > 0){
        for (int i = 0; i < sum.length; i++) {
           SumProps sumProps = sum[i].getSumProps();
           sumProps.setDataFileName(dataFile.getName());
           if (sumProps.getSumType() == SumProps.RATIO_IMAGE) {
              int numMass = Math.round(new Float(sumProps.getNumMass()));
              int denMass = Math.round(new Float(sumProps.getDenMass()));
              objectFileName = fileName + "_m" + numMass + "_m" + denMass + SUM_EXTENSION;
           } else if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
              int massNum = Math.round(new Float(sumProps.getParentMass()));
              objectFileName = fileName + "_m" + massNum + SUM_EXTENSION;
           } else {
              continue;
           }
           f_out = new FileOutputStream(objectFileName);
           obj_out = new ObjectOutputStream(f_out);
           obj_out.writeObject(sumProps);
        }}


      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   // Loads a previous session.
   private void openSession(ActionEvent ae) {
/*
      // instaitate some variables.
      int trueIndex = 1;
      ArrayList<Integer> deleteList = new ArrayList<Integer>();
      bUpdating = true;
      ZipEntry zipEntry = null;

      // User gets Zip file containg all .im files, action file, ROI files and HSIs. 
      JFileChooser fc = new JFileChooser();
       if (lastFolder != null) {
            fc.setCurrentDirectory(new java.io.File(lastFolder));
        }
      MIMSFileFilter ffilter = new MIMSFileFilter("zip");
      ffilter.setDescription("MIMS Session");
      fc.setFileFilter(ffilter);
      fc.setPreferredSize(new java.awt.Dimension(650, 500));
      if (fc.showOpenDialog(this) == JFileChooser.CANCEL_OPTION) {
         return;
      }
      lastFolder = fc.getSelectedFile().getParent();
      setIJDefaultDir(lastFolder);

      // Set the wait cursor and start opening session.
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      File selectedFile = fc.getSelectedFile();
      currentlyOpeningImages = true;

      // Begin process of reading zip file contents
      // and extracting files if necessary.
      try {

         // Find, then read action file straight from zip.
         ZipFile zipFile = new ZipFile(selectedFile);
         Enumeration zipEntries = zipFile.entries();
         while (zipEntries.hasMoreElements()) {
            zipEntry = (ZipEntry) zipEntries.nextElement();
            if (zipEntry.getName().endsWith(actionFileName))    
               break;
         }         
         if (zipEntry == null) {
            JOptionPane.showMessageDialog(this, "Zip file does not contain " + actionFileName,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
         }
         InputStream input = zipFile.getInputStream(zipEntry);
         InputStreamReader isr = new InputStreamReader(input);
         BufferedReader br = new BufferedReader(isr);

         // Read action file, and perform actions              
         String line;
         LinkedList actionRowList = new LinkedList();
         LinkedList fileList = new LinkedList();
         while ((line = br.readLine()) != null) {

            // Parse line.
            if (line.equals("")) {break;}                      
            String[] row = line.split("\t");

            // Add the actions to the actionList                 
            actionRowList.add(row);

            // Add files to the fileList (only if not already contained).
            if (!fileList.contains(row[5]))
               fileList.add(row[5]);
         }

         // Now extract all relevant .im files from zip.
         boolean firstImage = true;
         for (int i = 0; i < fileList.size(); i++) {

            // Make sure its exists in the zip file.
            String fileName = ((String)fileList.get(i));
            zipEntry = zipFile.getEntry(fileName);
            if (zipEntry == null) {
               System.out.println("Zip file does not contain " + fileName);
               return;
            }

            // If exists, extract to temp directory.
            File imageFile = extractFromZipfile(zipFile, zipEntry, null);
            if (imageFile == null || !imageFile.exists()) {
               System.out.println("Unable to extract " + fileName + " from " + zipFile.getName());
               return;
            }

            // Read main image file.
            if (firstImage)
               loadMIMSFile(imageFile.getAbsolutePath());
            else {
               UI tempui = new UI(imageFile.getAbsolutePath());
               mimsStackEditing.concatImages(false, false, tempui);
               openers.put(imageFile.getName(), tempui.getOpener());
               for (MimsPlus img : tempui.getMassImages()) {
                  if (img != null) {
                     img.setAllowClose(true);
                     img.close();
                  }
               }
            }

            firstImage = false;
         }

         // Loop over the action row list and perform actions.
         for (int i = 0; i < actionRowList.size(); i++) {

            // Get the action row.
            String[] actionRowString = (String[])actionRowList.get(i);

            // Get the display index that corresponds to the true index.
            int displayIndex = mimsAction.displayIndex(trueIndex);
            for (int j = 0; j < image.getNMasses(); j++) {
               massImages[j].setSlice(displayIndex);
            }

            // Set the XShift, YShift, dropped val, and image name for this slice.
            mimsStackEditing.XShiftSlice(displayIndex, Double.parseDouble(actionRowString[1]));
            mimsStackEditing.YShiftSlice(displayIndex, Double.parseDouble(actionRowString[2]));
            if (Integer.parseInt(actionRowString[3]) == 1) 
               deleteList.add(trueIndex);            
            mimsAction.setSliceImage(displayIndex, new String(actionRowString[5]));

            trueIndex++;
         }
         mimsStackEditing.removeSliceList(deleteList);

         // Load ROI list.
         MimsRoiManager rm = getRoiManager();
         rm.deleteAll();
         rm.openZip(selectedFile.getAbsolutePath());
         if (rm.getAllIndexes().length > 0)
            rm.showFrame();
                            
         // Load HSI images.              
         FileInputStream fis = new FileInputStream(selectedFile);
         ZipInputStream zis = new ZipInputStream(fis);
         zipEntry = null;
         while((zipEntry = zis.getNextEntry()) != null ) {
            if (zipEntry.getName().endsWith(hsiExtension)) {
               ObjectInputStream ois = new ObjectInputStream(zis);
               HSIProps hsiprops = (HSIProps)ois.readObject();                    
               computeHSI(hsiprops);
            } else if (zipEntry.getName().endsWith(ratioExtension)) {
               ObjectInputStream ois = new ObjectInputStream(zis);
               HSIProps hsiprops = (HSIProps)ois.readObject();                    
               computeRatio(hsiprops, true);
            } else if (zipEntry.getName().endsWith(sumExtension)) {
               ObjectInputStream ois = new ObjectInputStream(zis);
               String parentName = (String)ois.readObject();                    
               computeSum(parentName);
            }
         }
         zis.close();

      // TODO we need more refined Exception checking here
      } catch (Exception e) {
         e.printStackTrace();
      }

      // Set all images to the first slice.
      for (int j = 0; j < image.getNMasses(); j++) {
         massImages[j].setSlice(1);
      }

      currentlyOpeningImages = false;
      bUpdating = false;
 */
   }                                          

private void genStackMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                                 

    MimsPlus img;
    //grab current window and try to cast
    try {
        img = (MimsPlus) ij.WindowManager.getCurrentImage();
    } catch (ClassCastException e) {
        //if it's some random image and we can't cast just return
        return;
    }

    generateStack(img);

}                                                

private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {                                            
    Nrrd_Writer nw = new Nrrd_Writer(this);
    nw.run("");
}                                           

private void TestMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                             
    // TODO add your handling code here:

    //MimsPlus img = (MimsPlus) ij.WindowManager.getCurrentImage();
    //System.out.println(img.getTitleFileMass());

                                          
}                                           


   // Method for saving action file and writing backup action files.
   public void saveAction(java.awt.event.ActionEvent evt) {

      // Initialize variables.
      File selectedFile = null;

      // Query user where to save action file.
      JFileChooser fc = new JFileChooser(lastFolder);
      String fname = this.getImageFilePrefix();
      fname = fname + ".act";
      fc.setSelectedFile(new File(fname));

      MIMSFileFilter ffilter = new MIMSFileFilter("txt");
      ffilter.addExtension("act");
      ffilter.setDescription("MIMS Action");
      fc.setFileFilter(ffilter);

      while (true) {
         if (fc.showSaveDialog(this) == JFileChooser.CANCEL_OPTION) {
            return;
         }
         selectedFile = fc.getSelectedFile();

         // Update ImageJs the 'lastFolder' variable.
         lastFolder = selectedFile.getParent();
         setIJDefaultDir(lastFolder);

         // Check for overwriting any existing file.
         String actionFile = selectedFile.getName();
         if (selectedFile.exists()) {
            String[] options = {"Overwrite", "Cancel"};
            int value = JOptionPane.showOptionDialog(this, "File \"" + actionFile + "\" already exists!", null,
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (value == JOptionPane.NO_OPTION) {
               return;
            }

         }
         getmimsAction().writeAction(selectedFile);
         deleteTempActionFile();
         break;
      }
   }

   // Open action file.
   private void openActionEvent(java.awt.event.ActionEvent evt) {
      
      // Open JFileChooser and allow user to select action file.
      JFileChooser fc = new JFileChooser();
      if (lastFolder != null) {
         fc.setCurrentDirectory(new java.io.File(lastFolder));
      }
      fc.setPreferredSize(new java.awt.Dimension(650, 500));
      MIMSFileFilter ffilter = new MIMSFileFilter("txt");
      ffilter.addExtension("act");
      ffilter.setDescription("MIMS Action");
      fc.setFileFilter(ffilter);
      if (fc.showOpenDialog(this) == JFileChooser.CANCEL_OPTION) {
         return;
      }
            
      File actionFile = fc.getSelectedFile();
      lastFolder = fc.getSelectedFile().getParent();
      setIJDefaultDir(lastFolder);
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      
      openAction(actionFile);
   }
      
   public void openAction(File actionFile) {   
            
      // initialize variables
      int trueIndex = 1;
      ArrayList<Integer> deleteList = new ArrayList<Integer>();
                              
      // Get all open images so we can regenerate them with new data.
      
      // Get HSIProps for all open ratio images.
       MimsPlus[] rto = getOpenRatioImages();
       HSIProps[] rto_props = new HSIProps[rto.length]; 
       for (int i=0; i<rto.length; i++){
          rto_props[i] = rto[i].getHSIProps();
       }
       
       // Get HSIProps for all open hsi images.     
       MimsPlus[] hsi = getOpenHSIImages();
       HSIProps[] hsi_props = new HSIProps[hsi.length];
       for (int i=0; i<hsi.length; i++){
          hsi_props[i] = hsi[i].getHSIProps();
       }
       
       // Get SumProps for all open hsi images.    
       MimsPlus[] sum = getOpenSumImages();
       SumProps[] sum_props = new SumProps[sum.length];
       for (int i=0; i<sum.length; i++){
          sum_props[i] = sum[i].getSumProps();          
       }             
       // done with getting all open images.

      // more variable assignment and initialization.
      currentlyOpeningImages = true;
      bUpdating = true;      

      try {

         BufferedReader br = new BufferedReader(new FileReader(actionFile));

         // Read action file, and perform actions              
         String line;
         LinkedList actionRowList = new LinkedList();
         LinkedList fileList = new LinkedList();
         while ((line = br.readLine()) != null) {

            // Parse line.
            if (line.equals("")) {
               break;
            }
            String[] row = line.split("\t");

            // Add the actions to the actionList                 
            actionRowList.add(row);

            // Add files to the fileList (only if not already contained).
            if (!fileList.contains(row[5])) {
               fileList.add(row[5]);
            }
         }

         // Now extract all relevant .im files from zip.
         boolean firstImage = true;
         for (int i = 0; i < fileList.size(); i++) {

            // Make sure its exists in the current directory.
            String fileName = ((String) fileList.get(i));
            File imageFile = new File(actionFile.getParent(), fileName);
            if (!imageFile.exists()) {
               System.out.println("can not find image file " + fileName + " in current directory");
               return;
            }

            // Read main image file.
            if (firstImage) {
               loadMIMSFile(imageFile);
            } else {
               UI tempui = new UI(imageFile.getAbsolutePath());
               mimsStackEditing.concatImages(false, false, tempui);
               openers.put(imageFile.getName(), tempui.getOpener());
               for (MimsPlus img : tempui.getMassImages()) {
                  if (img != null) {
                     img.setAllowClose(true);
                     img.close();
                  }
               }
            }

            firstImage = false;
         }

         // Loop over the action row list and perform actions.
         for (int i = 0; i < actionRowList.size(); i++) {

            // Get the action row.
            String[] actionRowString = (String[]) actionRowList.get(i);

            // Get the display index that corresponds to the true index.
            int displayIndex = mimsAction.displayIndex(trueIndex);
            for (int j = 0; j < image.getNMasses(); j++) {
               massImages[j].setSlice(displayIndex);
            }

            // Set the XShift, YShift, dropped val, and image name for this slice.
            mimsStackEditing.XShiftSlice(displayIndex, Double.parseDouble(actionRowString[1]));
            mimsStackEditing.YShiftSlice(displayIndex, Double.parseDouble(actionRowString[2]));
            if (Integer.parseInt(actionRowString[3]) == 1) {
               deleteList.add(trueIndex);
            }
            mimsAction.setSliceImage(displayIndex, new String(actionRowString[5]));

            trueIndex++;
         }
         mimsStackEditing.removeSliceList(deleteList);

      // TODO we need more refined Exception checking here
      } catch (Exception e) {}
      
      //restoreState(rto_props, hsi_props, sum_props);

      // Set all images to the first slice.
      for (int j = 0; j < image.getNMasses(); j++) {
         massImages[j].setSlice(1);
      }

      bUpdating = false;
      currentlyOpeningImages = false;
   }

//generates a new ImagePlus that's a stack from a ratio or hsi
public void generateStack(MimsPlus img) {
    //do a few checks
    if(img==null)
        return;
    
    //need some reference image that's a stack
    if(this.massImages[0]==null)
        return;

    ImagePlus refimp = this.massImages[0];
    int currentslice = refimp.getSlice();

    //return is there's no stack
    if(refimp.getStackSize()==1)
        return;
    //return if it's not a computed image, ie ratio/hsi
    if( !(img.getMimsType()==img.RATIO_IMAGE || img.getMimsType()==img.HSI_IMAGE) )
        return;

    ij.ImageStack stack = img.getStack();
    java.awt.image.ColorModel cm = stack.getColorModel();
    ij.ImageStack ims = new ij.ImageStack(stack.getWidth(), stack.getHeight(), cm);
    int numImages = refimp.getStackSize();

    for (int i = 1; i <= numImages; i++) {
        refimp.setSlice(i);
        ims.addSlice(refimp.getStack().getSliceLabel(i), img.getProcessor().duplicate());
    }

    // Create new image
    ImagePlus newimp = new ImagePlus("Stack : "+img.getTitle(), ims);
    newimp.setCalibration(img.getCalibration());

    // Display this new stack
    newimp.show();
    newimp.setSlice(currentslice);
    refimp.setSlice(currentslice);

}


public void updateLineProfile(double[] newdata, String name, int width) {
    if(this.lineProfile==null) {
        return;
    } else {
        lineProfile.updateData(newdata, name, width);
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

    public MimsPlus[] getHSIImages() {
        return hsiImages;
    }

    public MimsPlus getHSIImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return hsiImages[i];
        }
        return null;
    }

    public MimsPlus getRatioImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return ratioImages[i];
        }
        return null;
    }

    public MimsPlus[] getSumImages() {
        return sumImages;
    }

    public MimsPlus getSumImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return sumImages[i];
        }
        return null;
    }

    // Returns only the open mass images as an array.
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
        for (i = 0 , nOpen = 0; i < maxMasses; i++) {
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

    public Opener getOpener() {
        return image;
    }   
    
    public Opener getFromOpenerList(String name){
        Object op = openers.get(name);
        return (Opener)openers.get(name);
    }
    
    public void addToOpenerList(String fileName, Opener opener) {
       openers.put(fileName, opener);
    }

    public void setActiveMimsPlus(MimsPlus mp) {
        for (int i = 0; i < maxMasses; i++) {
            if (mp == hsiImages[i]) {
                if(hsiImages[i].getHSIProps()!=null) { hsiControl.setHSIProps(hsiImages[i].getHSIProps()); }
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

    public double getMedianFilterRadius() {
        return this.medianFilterRadius;
    }

    public double setMedianFilterRadius(double r) {
        this.medianFilterRadius = r;
        return this.medianFilterRadius;
    }

    public String getLastFolder() {
        return lastFolder;
    }

    public void setIJDefaultDir(String dir) {
        ij.io.OpenDialog temp = new ij.io.OpenDialog("", "fubar");
        temp.setDefaultDirectory(dir);
        temp = null;
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

    // Variables declaration - do not modify                     
    private javax.swing.JMenuItem TestMenuItem;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenuItem captureImageMenuItem;
    private javax.swing.JMenuItem closeAllHSIMenuItem;
    private javax.swing.JMenuItem closeAllRatioMenuItem;
    private javax.swing.JMenuItem closeAllSumMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem genStackMenuItem;
    private javax.swing.JMenuItem importIMListMenuItem;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField mainTextField;
    private javax.swing.JMenuItem openNewMenuItem;
    private javax.swing.JMenuItem sumAllMenuItem;
    private javax.swing.JMenu utilitiesMenu;
    private javax.swing.JMenu viewMenu;
    // End of variables declaration                   

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

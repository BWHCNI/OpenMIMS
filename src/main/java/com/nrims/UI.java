/*
 * UI.java
 *
 * Created on May 1, 2006, 12:59 PM 
 */
package com.nrims;

import com.nrims.data.*;
import com.nrims.logging.OMLogger;
import com.nrims.managers.OpenerManager;
import com.nrims.managers.QSAcorrectionManager;
import com.nrims.managers.ConvertManager;
import com.nrims.managers.CompositeManager; //DJ: 08/07/2014
import com.nrims.managers.FilesManager;     //DJ: 08/20/2014
import com.nrims.unoplugin.UnoPlugin;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.process.ImageProcessor;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
//import java.nio.file.Files;
//import java.nio.file.LinkOption;
//import java.nio.file.Path;
//import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.*;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jfree.ui.ExtensionFileFilter;

//DJ: 10/20/2014
import java.util.Properties; // to be used to read the config files where html links are located 
import java.util.concurrent.ExecutionException;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker.StateValue;
import org.apache.commons.io.FilenameUtils;
                            //  for OpenMIMS Documentation as well as the Sample Data link + other possible links.
import org.opencv.videoio.VideoCapture;

/**
 * The main user interface of the NRIMS ImageJ plugin. A multi-tabbed window
 * with a file menu, the UI class serves as the central hub for all classes
 * involved with the user interface.
 */
public class UI extends PlugInJFrame implements WindowListener, MimsUpdateListener, PropertyChangeListener {

    public static final long serialVersionUID = 1;
    public static final String NRRD_EXTENSION = ".nrrd";
    public static final String NRRD_HEADER_EXTENSION = ".nhdr";
    public static final String MIMS_EXTENSION = ".im";
    public static final String ROIS_EXTENSION = ".rois.zip";
    public static final String ROI_EXTENSION = ".roi";
    public static final String RATIO_EXTENSION = ".ratio";
    public static final String RATIOS_EXTENSION = ".ratios.zip";
    public static final String HSI_EXTENSION = ".hsi";
    public static final String COMPOSITE_EXTENSION = ".comp";
    public static final String HSIS_EXTENSION = ".hsis.zip";
    public static final String SUM_EXTENSION = ".sum";
    public static final String SUMS_EXTENSION = ".sums.zip";
    public static final String SESSIONS_EXTENSION = ".session.zip";
    public static final String ACT_EXTIONSION = ".act";
    public static final String SAVE_IMAGE = "Save Images";
    public static final String SAVE_SESSION = "Save Session";
    
    public String operatingSystem;
    private String mimsVersion = "3.0.4";  

    public int maxMasses = 25;
    private double medianFilterRadius = 1;
    
    static public boolean runningInNetBeans = false;
    private boolean bSyncStack = true;
    private boolean bUpdating = false;
    private boolean onlyReadHeader = false;
    private boolean currentlyOpeningImages = false;
    private boolean bCloseOldWindows = true;
    private boolean medianFilterRatios = false;
    private boolean isSum = false;
    private boolean isWindow = false;
    private int windowRange = -1;
    private boolean isPercentTurnover = false;
    private boolean isRatio = true;
    private boolean[] bOpenMass = new boolean[maxMasses];
    private boolean silentMode = false;
    private boolean isDTCorrected = false;
    private boolean isQSACorrected = false;
    private float[] betas;
    private float fc_objective;

    private String lastFolder = null;
    public File tempActionFile;
    private HashMap openers = new HashMap();
    private HashMap metaData = new HashMap();

    private MimsPlus[] massImages = new MimsPlus[maxMasses];
    private MimsPlus[] ratioImages = new MimsPlus[maxMasses];
    private MimsPlus[] hsiImages = new MimsPlus[maxMasses];
    private MimsPlus[] segImages = new MimsPlus[maxMasses];
    private MimsPlus[] sumImages = new MimsPlus[2 * maxMasses];
    private MimsPlus[] compImages = new MimsPlus[2 * maxMasses];
    //private ArrayList<MimsPlus> nonMIMSImages = new ArrayList<MimsPlus>();
    private List<MimsPlus> nonMIMSImages = new ArrayList<MimsPlus>();

    
    // DJ - 07/30/2014
    //private ArrayList<MassProps> filtered_mass_props ;
    private List<MassProps> filtered_mass_props ;
   // private ArrayList<MimsPlus> closedWindowsList  = null;
    //private ArrayList<MassProps> closedWindowsList = null;
    private List<MassProps> closedWindowsList = null;
    private LinkedHashMap<String, Vector> filesProps = null;
    public boolean same_size = false;
   
    // 10/09/2014
    private ConvertManager htmlGenerator = null;
    
    // DJ: 08/20/2014
    private File previousFileOpened = null;
    private MassProps[] mass_props = null;
    private RatioProps[] rto_props = null;
    private HSIProps[] hsi_props = null;
    private SumProps[] sum_props = null;
    private CompositeProps[] composite_props = null;
    
    private FilesManager filesManager = null; // DJ:08/26/2014
    private String nameOfFileNowOpened = null; // DJ:08/26/2014
    
   private boolean usedForTables = false; // DJ:11/21/2014
    

    private MimsData mimsData = null;
    private MimsLog mimsLog = null;
    private MimsCBControl cbControl = new MimsCBControl(this);
    private MimsStackEditor mimsStackEditing = null;
    private MimsRoiManager2 roiManager = null;
    private MimsTomography mimsTomography = null;
    private MimsHSIView hsiControl = null;
    private CompositeManager compManager = null;
    private SegmentationForm segmentation = null;
    private QSAcorrectionManager qsam;
    private ReportGenerator rg = null;
    private javax.swing.JRadioButtonMenuItem[] viewMassMenuItems = null;
    private Opener image = null;
    private ij.ImageJ ijapp = null;
    private FileDrop mimsDrop;
    private File tempDir;

    protected MimsLineProfile lineProfile;
    protected MimsAction mimsAction = null;
    private imageNotes imgNotes;
    private PrefFrame prefs;
    private String revisionNumber = "1";
    public static Boolean single_instance_mode = false;
    public static UI ui = null;
    public boolean sessionOpened = false;
    // Task related variables.
    public SwingWorker task;
    boolean previousFileCanceled = true;
    private final static Logger OMLOGGER = OMLogger.getOMLogger(UI.class.getName());
    
    private Thread autosaveROIthread;
    
    //DJ: 10/20/2014 
    // to be used as a backup in case the config file for links doesn't exist or doesn't get read/parsed.
    String DEFAULT_DOCUMENTATION_LINK = "https://github.com/BWHCNI/OpenMIMS/wiki";
    String DEFAULT_SAMPLE_DATA_LINK   = "https://github.com/BWHCNI/OpenMIMS/wiki/Sample-Data";
    
    //DJ: 10/20/2014
    String documentationLink = DEFAULT_DOCUMENTATION_LINK; // to be actualized later on while parsing the "OpenMIMSLinks.cfg"
    String sampleDataLink    = DEFAULT_SAMPLE_DATA_LINK; // to be actualized later on while parsing the "OpenMIMSLinks.cfg"

    //DJ:10/20/2014
    private Properties defaultLinks; // for reading and parsing the "OpenMIMSLinks.cfg"

    public UI() {
        this(false);
    }

    /**
     * Creates a new instance of the OpenMIMS analysis interface.
     * 
     * @param silentMode true to use silent mode, false otherwise
     */
    public UI(boolean silentMode) {
        //NOTE: Trying to leave strictly UI related code in here, and remove the rest
        //As such, autosaveroi is now called in NRIMS_Plugin
        super("OpenMIMS");
        
        OMLOGGER.info(mimsVersion);

        // Is this to surpress what was going to stdout, 
        // that should have been going to stderr?
        this.silentMode = silentMode;

        OMLOGGER.fine("Ui id = " + System.identityHashCode(this));
        OMLOGGER.fine("java.version: " + System.getProperty("java.version"));
        OMLOGGER.fine("java.vendor: " + System.getProperty("java.vendor"));
        OMLOGGER.fine("java.vendor.url: " + System.getProperty("java.vendor.url"));
        OMLOGGER.fine("java.home: " + System.getProperty("java.home"));
        OMLOGGER.fine("os.arch: " + System.getProperty("os.arch"));
        OMLOGGER.fine("os.name: " + System.getProperty("os.name"));
        OMLOGGER.fine("os.version: " + System.getProperty("os.version"));
        OMLOGGER.fine("java.library.path: " + System.getProperty("java.library.path"));

        try {
            OMLOGGER.info("machine name = " + FileUtilities.getMachineName());
        } catch (Exception e) {
            OMLOGGER.info("Could not retrieve machine name");
        }
        revisionNumber = extractRevisionNumber();
        OMLOGGER.info("revisionNumber: " + revisionNumber);

        /*
         // Set look and feel to native OS - this has been giving us issues
         // as getting the "SystemLookAndFeel" sometimes results is very
         // different layouts for the HSIView tab. Leave commentoutr out for now.
         try {
         javax.swing.UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         javax.swing.SwingUtilities.updateComponentTreeUI(this);
         } catch (Exception e) {
         IJ.log("Error setting native Look and Feel:\n" + e.toString());
         }
         */
        
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.indexOf("mac")>=0) {
            operatingSystem = "MacOS";   // value of OS is "mac os x" under High Sierra
            Prefs.setIJMenuBar = true;
        } else if (OS.indexOf("nix")>=0) {
            operatingSystem = "Unix";
        } else if (OS.indexOf("linux")>=0) {
            operatingSystem = "linux";
        } else if (OS.indexOf("win")>=0) {
            operatingSystem = "windows";
        } else {
            operatingSystem = "unknown";
        }
        
        initComponents();
        initComponentsCustom();

        //read in preferences so values are gettable
        //by various tabs (ie mimsTomography, HSIView, etc.
        //when constructed further down.
        
        
        ijapp = IJ.getInstance();
        if (ijapp == null || (ijapp != null && !ijapp.isShowing())) {
            if (silentMode) {
                ijapp = new ij.ImageJ(ij.ImageJ.NO_SHOW);
            } else {
                ijapp = new ij.ImageJ();
            }
            setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        }

        //DJ: 11/19/2014
        prefs = new PrefFrame(this);
        
        String defaultLUT = prefs.getDefaultLUT();
        cbControl.setLUT(defaultLUT);
        
        boolean showDragDropItems = prefs.getShowDragDropMessage();
        this.showHideDragDropMessage(showDragDropItems);
        
        boolean showRoiManager = prefs.getShowRoiManager();
        this.showHideROIManager(showRoiManager);
        
     
        UnoPlugin.setNotesPath(prefs.getMyNotesPath());
        UnoPlugin.setOpenStatus(prefs.getMyNotesPath());

        //initialize empty image arrays
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
        //set location of window based on spawn location and size of IJ
        int xloc, yloc = 150;
        if (ijapp != null) {
            xloc = ijapp.getX();
            if (xloc + ijapp.getWidth() + this.getPreferredSize().width + 10
                    < Toolkit.getDefaultToolkit().getScreenSize().width) {
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
        this.ui = this;
        //create new interface for dropping files into OpenMIMS
        this.mimsDrop = new FileDrop(null, jTabbedPane1, new FileDrop.Listener() {
            public void filesDropped(File[] files) {
                if (files.length == 0) {
                    IJ.error("Unable to open file. Make sure file is not from a remote location.");
                    return;
                }
                File file = files[0];
                //if multiple files detected, check whether the user wants to stack them, open the first, or neither
                if (files.length > 1) {
                    Object[] options = {"Yes, stack the images",
                        "No, just open the first one",
                        "No, don't open anything"};
                    int n = JOptionPane.showOptionDialog(ui,
                            "Would you like to open a stacked version of these multiple images or just open the first one?",
                            "Multiple images detected",
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    if (n == JOptionPane.YES_OPTION) {
                        setLastFolder(file.getParentFile());
                        setIJDefaultDir(file.getParent());
                        boolean proceed = checkCurrentFileStatusBeforeOpening();  
                        if (proceed) {
                            openFileInBackground(FileUtilities.stackImages(files, ui));
                        }
                        return;
                    } else if (n == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                }
                setLastFolder(file.getParentFile());
                setIJDefaultDir(file.getParent());
                boolean proceed = checkCurrentFileStatusBeforeOpening();
                if (proceed) {
                    
                    MimsRoiManager2 roiManager = getRoiManager();
                    roiManager.selectAll();  // Do not show the ROI manager or ROIs unless user wants them.
                    roiManager.delete(false, false);
                    roiManager.close();
                    roiManager.setNeedsToBeSaved(false);
            
            
                    openFileInBackground(file);
                    
                    enableRestoreMimsMenuItem(true);
                    
                    //Start Auto save thread for ROI, if not already started.
                    startAutosaveROIthread(false);

                }
                // When openFileInBackground finishes loading the file, this listener will get
                // invoked in order to load ROI files, if present.
                task.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName().equalsIgnoreCase("progress")) {
                            int progress = task.getProgress();
                            if (progress == 0) {
                                if (getPreferences().getShowAutosaveROISdialog()) {                         
                                    loadROIfiles(file);                                  
                                    //getRoiManager().enableSaveButton(true);
                                } 
                                getRoiManager().enableSaveButton(true);
                            }
                        }
                    }
                });           
            }
        });
        
        String macrosPath = IJ.getDirectory("macros");
        if (macrosPath == null) {
            macrosPath = "lib/plugins/macros/";
        }
        OMLOGGER.info("Macros filepath: " + macrosPath);
        File file = new File(macrosPath + "/openmims_tools.fiji.ijm");
        if (file.exists()) {
            OMLOGGER.info("openmims_tools.fiji.ijm found, installing");
            IJ.run("Install...", "install=" + macrosPath + "/openmims_tools.fiji.ijm");
        } else {
            OMLOGGER.info("openmims_tools.fiji.ijm NOT found.");
            //This line --probably-- broke the "open_mims" bash script called from the
            //"transfer" script...
            //IJ.error("Error: openmims_tools.fiji.ijm does not exist. Please try updating.");
        }


        //StartupScript should be DEPRICATED/REMOVED
        //Better way to initialize state is via a script
        //need to research...
        // Create and start the thread
        //Thread thread = new StartupScript(this);
        //thread.start();
        //loadMIMSFile1(new File("/nrims/home3/cpl/JASON/LOSCALZO/EXP4/110324-c3_2_1-82Se-2x999_1_2_3_4_concat.nrrd"));

        // I added this listener because for some reason,
        // neither the windowClosing() method in this class, nor the
        // windowClosing method in PlugInJFrame is registering.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent winEvt) {
                if (IJ.getInstance() != null) {
                    prefs.savePreferences();
                }
                String macrosPath = IJ.getDirectory("macros");
                File file = new File(macrosPath + "/StartupMacros.fiji.ijm");
                if (file.exists()) {
                    System.out.println("OpenMIMS: restoring StartupMacros.fiji.ijm");
                    IJ.run("Install...", "install=" + macrosPath + "/StartupMacros.fiji.ijm");
                }
                closeCurrentImage();
                close();
            }
        });
        
        
        // DJ: 10/06/2014
        // try to read the "OpenMIMSLinks.cfg" config file that contain web links to (exp: Documentation, Sample Data, ...
        // the "OpenMIMSLinks.cfg" could be extended as desired as long as the parsing happens at this stage.
        try{
            defaultLinks = new Properties();
            //This would allow us to both:
            // 1) read the "OpenMIMSLinks.cfg" right here from eclipse IDE and
            // 2) within the  "OpenMIMS.jar" located in the fiji.app/plugins while it's running
            InputStream in = getClass().getResourceAsStream("/OpenMIMSLinks.cfg"); 
            
            //If in case the "OpenMIMSLinks.cfg" file is provided, we read/parse it
            // otherwise, we just the default links that were previously declared.
            if (in != null) {
                defaultLinks.load(in);
                //defaultLinks.list(System.out); //  prints out.
                in.close();

                documentationLink = defaultLinks.getProperty("DOCUMENTATION_LINK", DEFAULT_DOCUMENTATION_LINK);
                sampleDataLink = defaultLinks.getProperty("SAMPLE_DATA_LINK", DEFAULT_SAMPLE_DATA_LINK);

                // for debugging purposes:
                /*
                System.out.println("==> " + documentationLink);
                System.out.println("==> " + sampleDataLink);
                */
            }
            
        } catch(FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e_io){
            System.out.println(e_io);
        }
        
        boolean documentationWebPageExists = false;
        try {
            final java.net.URL documentationURL = new java.net.URL(documentationLink);
            java.net.HttpURLConnection huc = (java.net.HttpURLConnection) documentationURL.openConnection();
            huc.setRequestMethod("HEAD");
            
            int response = huc.getResponseCode();

            if (response == 200)
                documentationWebPageExists = true;
                

        } catch (MalformedURLException e) {
            //System.err.println(e);
        } catch (IOException io_e) {
            //System.out.println(io_e);
        }
        finally{
            if(documentationWebPageExists)
                System.out.println("ONLINE Documentation Web Page Exists");
           else
                System.out.println("ONLINE Documentation Web Page Does Not Exists");
        }
        // in here we check would have the local documentation pdf file to be shown \
        // instead of the online version because it simply doesn't exist.
        
        String javaVersion = System.getProperty("java.version");  // Will look something like this:  1.8.0_51-b16
        //String javaVersion = "1.5";
        if (javaVersion.startsWith("1.4") || javaVersion.startsWith("1.5")  || javaVersion.startsWith("1.6")) {
            JOptionPane.showMessageDialog(this,
                "OpenMIMS requires Java version 1.8 or above. \nYour machine is running version " +
                    javaVersion + "\n\nPlease upgrade to the latest version of Fiji, which contains the required Java version.",
                "Java version needs updating",
                JOptionPane.WARNING_MESSAGE); 
        }
        

        // DJ: 20/10/2014:
        // we produce a tip text whenever the mouse pointer hovers over the Documentation Button
        // as well as the Sample Data Button.
        docButton.setToolTipText(" OPEN LINK:  " + documentationLink);
        sampleDataButton.setToolTipText("OPEN LINK:  " + sampleDataLink);
        
    }
    
    public void startAutosaveROIthread(boolean interrupt) {
        if (interrupt) {
            //System.out.println("interrupting autosaveROIthread");
            autosaveROIthread.interrupt();
            //System.out.println("starting autosaveROIthread after interruption");
            autosaveROIthread = new Thread(new FileUtilities.AutoSaveROI(ui));
            autosaveROIthread.start();
        }
                    
                    
        if (autosaveROIthread == null) {
            //System.out.println("starting autosaveROIthread");
            autosaveROIthread = new Thread(new FileUtilities.AutoSaveROI(ui));
            autosaveROIthread.start();
        }
            
   
    }
    


    /**
     * Insertion status of the current MimsPlus object
     *
     * @param mp object to be inserted.
     * @return success/failure of insertion.
     */
    public boolean addToImagesList(MimsPlus mp) {
        int i = 0;
        int ii = 0;
        boolean inserted = false;
        int numBefore = 0;
        while (i < maxMasses) {
            if (mp.getMimsType() == MimsPlus.RATIO_IMAGE) {
                if (ratioImages[i] == null) {
                    inserted = true;
                    if (numBefore > 0) {
                        mp.setTitle("(" + numBefore + ") " + mp.getTitle());
                    }
                    ratioImages[i] = mp;
                    getCBControl().addWindowtoList(mp);
                    getMimsTomography().resetImageNamesList();
                    return true;
                } else if (ratioImages[i].getRatioProps().equals(mp.getRatioProps())) {
                    numBefore++;
                }

            }
            if (mp.getMimsType() == MimsPlus.HSI_IMAGE) {
                if (hsiImages[i] == null) {
                    inserted = true;
                    if (numBefore > 0) {
                        mp.setTitle("(" + numBefore + ") " + mp.getTitle());
                    }
                    hsiImages[i] = mp;
                    getMimsTomography().resetImageNamesList();
                    return true;
                } else if (hsiImages[i].getHSIProps().equals(mp.getHSIProps())) {
                    numBefore++;
                }
            }
            if (mp.getMimsType() == MimsPlus.SEG_IMAGE && segImages[i] == null) {
                inserted = true;
                segImages[i] = mp;
                return true;
            }
            i++;
        }

        // Sum and composite images has a larger array size.
        while (ii < 2 * maxMasses) {
            if (mp.getMimsType() == MimsPlus.SUM_IMAGE) {
                if (sumImages[ii] == null) {
                    inserted = true;
                    if (numBefore > 0) {
                        mp.setTitle("(" + numBefore + ") " + mp.getTitle());
                    }
                    sumImages[ii] = mp;
                    getCBControl().addWindowtoList(mp);
                    this.mimsTomography.resetImageNamesList();
                    return true;
                } else if (sumImages[ii].getSumProps().equals(mp.getSumProps())) {
                    numBefore++;
                }
            }
            if (mp.getMimsType() == MimsPlus.COMPOSITE_IMAGE) {
                if (compImages[ii] == null) {
                    inserted = true;
                    if (numBefore > 0) {
                        mp.setTitle("(" + numBefore + ") " + mp.getTitle());
                    }
                    compImages[ii] = mp;
                    return true;
                } else if (compImages[ii].getCompositeProps().equals(mp.getCompositeProps())) {
                    numBefore++;
                }
            }
            ii++;
        }
        if (!inserted) {
            System.out.println("Too many open images");
        }
        return inserted;
    }

    /**
     * Closes the current image and its associated set of windows if the mode is
     * set to close open windows.
     */
    public void closeCurrentImage() {
        if (getRoiManager() != null) {
            if (getRoiManager().isVisible()) {
                getRoiManager().close();
            }
        }
        for (int i = 0; i < maxMasses; i++) {
            if (segImages[i] != null) {
                segImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (segImages[i].getWindow() != null) {
                        ImageWindow ww = segImages[i].getWindow();
                        // If the user closes the displayed segmentation result,
                        // then the ImagePlus instance associated with the
                        // ImageWindow is null.
                        try {
                            ImagePlus imp = ww.getImagePlus();
                            if (imp != null) {
                                ww.close();
                            }
                        } catch (NullPointerException npe) {
                            System.out.println("Error closing segmentation result window");
                        }
                        //segImages[i].getWindow().close();
                    }
                    segImages[i] = null;
                }
            }
            if (massImages[i] != null) {
                massImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (massImages[i].getWindow() != null) {
                        massImages[i].getWindow().close();
                    }
                    massImages[i] = null;
                }
            }
            bOpenMass[i] = false;
            if (hsiImages[i] != null) {
                hsiImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (hsiImages[i].getWindow() != null) {
                        hsiImages[i].getWindow().close();
                    }
                    hsiImages[i] = null;
                }
            }
            if (ratioImages[i] != null) {
                ratioImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (ratioImages[i].getWindow() != null) {
                        ratioImages[i].getWindow().close();
                    }
                    ratioImages[i] = null;
                }
            }
        }

        for (int i = 0; i < maxMasses * 2; i++) {
            if (sumImages[i] != null) {
                sumImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (sumImages[i].getWindow() != null) {
                        sumImages[i].getWindow().close();
                    }
                    sumImages[i] = null;
                }
            }
            if (compImages[i] != null) {
                compImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (compImages[i].getWindow() != null) {
                        compImages[i].getWindow().close();
                    }
                    compImages[i] = null;
                }
            }
        }
        if (image != null) {
            image.close();
        }
    }

    /**
     * Brings up the graphical pane for selecting files to be opened.
     * 
     * @return true if the file was opened, false if not
     */
    public synchronized File loadMIMSFile() {

        // Bring up file chooser.
        MimsJFileChooser fc = new MimsJFileChooser(this);
        fc.setMultiSelectionEnabled(false);
        int returnVal = fc.showOpenDialog(this);

        // Open file or return null.
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            boolean opened = openFile(file, false);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (opened) {
                return file;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Opens a MIMS file in the background. Do not call this method if your code
     * does work with the file once opened, instead call openFile.
     *
     * @param file to be opened.
     */
    public void openFileInBackground(File file) {
        
        //----------------------------------------------------------------------
        // DJ: 08/19/2014 - Saving all the props of the "to-be-closed-file"
        if (mimsData != null) {
            String previousFileTitle = mimsData.getFilePathAndName();
            
            MassProps[] prevFile_massProps = new MassProps[ui.getOpenMassProps().length];
            prevFile_massProps = ui.getOpenMassProps();
            
            RatioProps[] prevFile_rtoProps = new RatioProps[ui.getOpenRatioProps().length];
            prevFile_rtoProps = ui.getOpenRatioProps();
            
            HSIProps[] prevFile_hsiProps = new HSIProps[ui.getOpenHSIProps().length];
            prevFile_hsiProps = ui.getOpenHSIProps();
            
            SumProps[] prevFile_sumProps = new SumProps[ui.getOpenSumProps().length];
            prevFile_sumProps = ui.getOpenSumProps();
            
            CompositeProps[] prevFile_compProps = new CompositeProps[ui.getOpenCompositeProps().length];
            prevFile_compProps = ui.getOpenCompositeProps();
            
            // composite Manager visibility
            boolean isCompositemanagerVisible = false;
            if(com.nrims.managers.CompositeManager.getInstance()!= null)
                isCompositemanagerVisible= com.nrims.managers.CompositeManager.getInstance().isVisible();
            
             // roiProps:
            boolean isROIVisble = roiManager.isVisible();
            Roi[] allROIs = new Roi[roiManager.getAllROIs().length];
            allROIs = roiManager.getAllROIs();
            
            HashMap<String, ArrayList<Integer[]>> locations = new HashMap<String, ArrayList<Integer[]>>();
            locations = roiManager.getLocations();
            
            //ArrayList<ROIgroup> groups = new ArrayList<ROIgroup>();
            List<ROIgroup> groups = new ArrayList<ROIgroup>();
            groups = roiManager.getGroups();
            
            //HashMap<String, String> groupsMap = new HashMap<String, String>();
            Map<String, ROIgroup> groupsMap = roiManager.getGroupMap();
            
            
            //ArrayList roiProps = new ArrayList();
            List roiProps = new ArrayList();
            roiProps.add(isROIVisble);
            roiProps.add(allROIs);
            roiProps.add(locations);
            roiProps.add(groups);
            roiProps.add(groupsMap);
            
            // populate the vector of all props for the previous file opened.
            Vector v = new Vector();
            v.add(prevFile_massProps);
            v.add(prevFile_rtoProps);
            v.add(prevFile_hsiProps);
            v.add(prevFile_sumProps);
            v.add(prevFile_compProps);
            v.add(isCompositemanagerVisible);
            v.add(roiProps);
            
            if(filesProps == null)
                filesProps = new LinkedHashMap<String, Vector>();
            
            if(filesProps.containsKey(previousFileTitle))
                filesProps.remove(previousFileTitle);
            
            filesProps.put(previousFileTitle, v);
            
            if(filesProps.containsKey(file.getPath()))
                filesProps.remove(file.getPath());
            
        }//---------------------------------------------------------------------
        nameOfFileNowOpened = file.getPath();
        
        if (currentlyOpeningImages) {
            return;
        }
        currentlyOpeningImages = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        stopButton.setEnabled(true);
        task = new FileOpenTask(file, this, false);
        task.addPropertyChangeListener(this);
        task.execute();  // Open in background, not inline
    }
    
    public boolean openFile(File file) {
        onlyReadHeader = false;
        return(openFile(file, onlyReadHeader));
    }

    /**
     * Opens file in a non-cancelable thread.
     *
     * @param file to be opened.
     * @param onlyReadHeader true if this method should only read the header of the file
     * 
     * @return true if the file was opened, false if not
     */
    public boolean openFile(File file, boolean onlyReadHeader) {
        this.onlyReadHeader = onlyReadHeader;
        //----------------------------------------------------------------------
        // DJ: 08/19/2014 - Saving all the props of the "to-be-closed-file".
        if (mimsData != null) {
            String previousFileTitle = mimsData.getFilePathAndName();
            
            MassProps[] prevFile_massProps = new MassProps[ui.getOpenMassProps().length];
            prevFile_massProps = ui.getOpenMassProps();
            
            RatioProps[] prevFile_rtoProps = new RatioProps[ui.getOpenRatioProps().length];
            prevFile_rtoProps = ui.getOpenRatioProps();
            
            HSIProps[] prevFile_hsiProps = new HSIProps[ui.getOpenHSIProps().length];
            prevFile_hsiProps = ui.getOpenHSIProps();
            
            SumProps[] prevFile_sumProps = new SumProps[ui.getOpenSumProps().length];
            prevFile_sumProps = ui.getOpenSumProps();
            
            CompositeProps[] prevFile_compProps = new CompositeProps[ui.getOpenCompositeProps().length];
            prevFile_compProps = ui.getOpenCompositeProps();
            
            // composite Manager visibility
            boolean isCompositemanagerVisible = false;
            if(com.nrims.managers.CompositeManager.getInstance()!= null)
                isCompositemanagerVisible= com.nrims.managers.CompositeManager.getInstance().isVisible();
            
            // roiProps:
            boolean isROIVisble = roiManager.isVisible();
            Roi[] allROIs = new Roi[roiManager.getAllROIs().length];
            allROIs = roiManager.getAllROIs();
            
            HashMap<String, ArrayList<Integer[]>> locations = new HashMap<String, ArrayList<Integer[]>>();
            locations = roiManager.getLocations();
            
            //ArrayList<ROIgroup> groups = new ArrayList<ROIgroup>();
            List<ROIgroup> groups = new ArrayList<ROIgroup>();
            groups = roiManager.getGroups();
            
            //HashMap<String, String> groupsMap = new HashMap<String, String>();
            Map<String, ROIgroup> groupsMap = roiManager.getGroupMap();
            
            //ArrayList roiProps = new ArrayList();
            List roiProps = new ArrayList();
            roiProps.add(isROIVisble);
            roiProps.add(allROIs);
            roiProps.add(locations);
            roiProps.add(groups);
            roiProps.add(groupsMap);
            
            // populate the vector of all props for the previous file opened.
            Vector v = new Vector();
            v.add(prevFile_massProps);
            v.add(prevFile_rtoProps);
            v.add(prevFile_hsiProps);
            v.add(prevFile_sumProps);
            v.add(prevFile_compProps);
            v.add(isCompositemanagerVisible);
            v.add(roiProps);
            
            if(filesProps == null)
                filesProps = new LinkedHashMap<String, Vector>();
            
            if(filesProps.containsKey(previousFileTitle))
                filesProps.remove(previousFileTitle);
            
            filesProps.put(previousFileTitle, v);
            
            if(filesProps.containsKey(file.getPath()))
                filesProps.remove(file.getPath());
            
        }//---------------------------------------------------------------------
        nameOfFileNowOpened = file.getPath();
        
        boolean opened = false;
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            FileOpenTask fileOpenTask = new FileOpenTask(file, this, onlyReadHeader);
            opened = fileOpenTask.doInBackground();                          
        } catch (Exception e) {
           e.printStackTrace(); 
        } finally {
            setCursor(null);

        }
        return opened;
    }
    
    /**
     * restores the windows positioning as well as the zoom and the hiding
     * properties of all windows based on the massProps[] given.
     *
     * @param mass_props an array of MassProps
     */
    public void applyWindowState(MassProps[] mass_props) {

        if (mass_props == null) {
            return;
        }
        //Change missing from svn->git migration
        if (mass_props.length == 0) {
            return;
        }
        //end
        try {
            MimsPlus[] imgs = getOpenMassImages();
            

            for (int i = 0; i < imgs.length; i++) {
                for (int y = 0; y < mass_props.length; y++) {
                    if (massesEqualityCheck(imgs[i].getMassValue(), mass_props[y].getMassValue(), 0.49)) {
                        if (imgs[i].getMassIndex() == mass_props[y].getMassIdx()) {
                            imgs[i].getWindow().setLocation(mass_props[y].getXWindowLocation() + 9, mass_props[y].getYWindowLocation() + 6);
                            imgs[i].getWindow().setVisible(mass_props[y].getVisibility());
                            applyZoom(imgs[i], mass_props[y].getMag());
                            imgs[i].setDisplayRange(mass_props[y].getMinLUT(), mass_props[y].getMaxLUT());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Applies the magnification factor stored
     * <code>zoom</code> to the specified image
     * <code>mp</code>
     * 
     * @param mp a <code>MimsPlus</code> instance
     * @param zoom zoom factor
     *
     */
    public void applyZoom(MimsPlus mp, double zoom) {

        if (zoom == 0.0 || zoom == 1.0) {
            return;
        }

        if (mp == null) {
            return;
        }

        if (mp.getCanvas() == null) {
            return;
        }

        if (mp.getCanvas().getMagnification() < zoom) {
            while (mp.getCanvas().getMagnification() < zoom) {
                mp.getCanvas().zoomIn(0, 0);
            }
        } else if (mp.getCanvas().getMagnification() > zoom) {
            while (mp.getCanvas().getMagnification() > zoom) {
                mp.getCanvas().zoomOut(0, 0);
            }
        }
    }

    public void removeComponentFromMimsDrop(Component comp) {
        if (comp != null) {
            mimsDrop.remove(comp);
        }
    }

    /**
     * Resets the "view" menu item to reflect the mass images in the current
     * data file.
     */
    public void resetViewMenu() {

        if (isSilentMode()) {
            return;
        }

        for (int i = 0; i < viewMassMenuItems.length; i++) {
            if (i < image.getNMasses()) {
                viewMassMenuItems[i].setText(image.getMassNames()[i]);
                viewMassMenuItems[i].setVisible(true);
                double massVal = massImages[i].getMassValue();
                Boolean isVisible = false;
             
                
                if(massImages[i].getWindow() != null){
                    isVisible = massImages[i].getWindow().isShowing();
                }
                else{
                    isVisible = false;
                     
                }
                
                if (isVisible) {
                    viewMassMenuItems[i].setSelected(true);
                } else {
                    viewMassMenuItems[i].setSelected(false);
                }
            } else {
                viewMassMenuItems[i].setText("foo");
                viewMassMenuItems[i].setVisible(false);
                viewMassMenuItems[i].setSelected(false);
            }
        }
    }

    /**
     * Initializes the view menu.
     */
    private void initializeViewMenu() {

        this.viewMassMenuItems = new javax.swing.JRadioButtonMenuItem[this.maxMasses];

        for (int i = 0; i < viewMassMenuItems.length; i++) {
            javax.swing.JRadioButtonMenuItem massRButton = new javax.swing.JRadioButtonMenuItem();

            if (i < image.getNMasses()) {
                massRButton.setVisible(true);
                massRButton.setSelected(true);
                massRButton.setText(image.getMassNames()[i]);
            } else {
                massRButton.setSelected(false);
                massRButton.setText("foo");
                massRButton.setVisible(false);
            }


            
            // This is a lambda version of the above commented-out lines
            massRButton.addActionListener( e -> viewMassChanged(e));
            
            viewMassMenuItems[i] = massRButton;

            this.viewMenu.add(massRButton);
        }
        
        // DJ: 10/03/2014:
        // we add the "show all" and "hide all" option for mass images.
        JSeparator sprtr = new javax.swing.JSeparator();
        this.viewMenu.add(sprtr);
        
        JMenuItem showAllMassImages = new JMenuItem("Show All Mass Images");
        
        showAllMassImages.addActionListener(event -> {
            if (getOpenMassImages() != null) {
                for (int i = 0; i < viewMassMenuItems.length; i++) {
                    ActionEvent evt = new ActionEvent(event.getSource(),
                            event.getID(),
                            viewMassMenuItems[i].getText());
                    viewMassMenuItems[i].setSelected(true);
                    viewMassChanged(evt);
                }
            }     
        });

        JMenuItem hideAllMassImages = new JMenuItem("Hide All Mass Images");
        hideAllMassImages.addActionListener(event -> {
            if (getOpenMassImages() != null) {
                for (int i = 0; i < viewMassMenuItems.length; i++) {
                    ActionEvent evt = new ActionEvent(event.getSource(),
                            event.getID(),
                            String.valueOf(viewMassMenuItems[i].getText()));
                    viewMassMenuItems[i].setSelected(false);
                    viewMassChanged(evt);
                }
            }    
        });
        
        this.viewMenu.add(showAllMassImages);
        this.viewMenu.add(hideAllMassImages);
    }

    // DJ:08/25/2014
    // adds props of the window that was closed to the closedWindowsList
    // in order to keep track of the positioning of all closed windows
    // so when we show them again, they would appear at their last position
    // when their were last time visible
    /**
     * adds props of the window that was closed to the closedWindowsList
     * @param massProps of the closed window.
     */
    public void addToClosedWindowsList(MassProps massProps){
        if (massProps != null) {
            if(closedWindowsList == null)
                closedWindowsList = new ArrayList<MassProps>();
        
            closedWindowsList.add(massProps);
        }
    }
    
    /**
     * Action method for the "view" menu items.
     */
    private void viewMassChanged(java.awt.event.ActionEvent evt) {
        
        //ArrayList<Integer> indices = new ArrayList<Integer>();
        List<Integer> indices = new ArrayList<Integer>();

        for (int i = 0; i < viewMassMenuItems.length; i++) {
            if (evt.getActionCommand().equals(viewMassMenuItems[i].getText())) {
                indices.add(i);
            }
        }
                            
        Iterator iter = indices.iterator();
        int index;
        while (iter.hasNext()) {
            index = (Integer)iter.next();
            
            if (massImages[index] == null)
                return;

            MimsPlus mp = massImages[index];

            if (viewMassMenuItems[index].isSelected() && !mp.isVisible()) {

                if(closedWindowsList != null){
                    for(int j=0 ; j<closedWindowsList.size(); j++){
                      if(massesEqualityCheck(closedWindowsList.get(j).getMassValue(), mp.getMassValue(), 0.49 )){
                            int index_diff = Math.abs(closedWindowsList.get(j).getMassIdx() - mp.getMassIndex());
                            /* DJ: 
                             * an IF statement to handle the case where we've got two groups or more
                             * of mass images in the same im/nrrd file.
                             * IMPORTANT NOTE: It is working for now but it needs better/clean 
                             * implementation.
                             * to be re-done using the grouping detection method that was recently
                             * implemented in UI.
                             */
                            if( index_diff < 3 ){ 
                                mp.show();
                                mp.getWindow().setLocation(
                                        new Point(
                                            closedWindowsList.get(j).getXWindowLocation(),
                                            closedWindowsList.get(j).getYWindowLocation())
                                        );
                                //System.out.println("at opening : (" + closedWindowsList.get(j).getXWindowLocation() +", "+ closedWindowsList.get(j).getYWindowLocation() + ")");            
                                mp.getWindow().setLocation(mp.getWindow().getX(), mp.getWindow().getY());
                                mp.setbIgnoreClose(true);
                                closedWindowsList.remove(j);
                                return;
                            }
                        }
                    }
                    mp.show();
                    mp.getWindow().setLocation(mp.getXYLoc());
                    mp.setbIgnoreClose(true);
                    return;
                }
                mp.show();
                mp.getWindow().setLocation(mp.getXYLoc());
                mp.setbIgnoreClose(true);

            } else if (!viewMassMenuItems[index].isSelected() && mp.isVisible()) {
                Point point = new Point(mp.getWindow().getLocation());

                MassProps windowMProps = new MassProps(mp.getMassIndex(), mp.getMassValue());
                //System.out.println("at closing : (" + point.x +", "+ point.y + ")");
                windowMProps.setXWindowLocation(point.x);
                windowMProps.setYWindowLocation(point.y);

                if(closedWindowsList == null){
                    closedWindowsList = new ArrayList<MassProps>();
                    closedWindowsList.add(windowMProps);
                }
                else{
                   if(!closedWindowsList.contains(windowMProps))
                       closedWindowsList.add(windowMProps);
                }
                mp.setXYLoc(point);
                mp.hide();
            }
        }
    }

    /**
     * The behavior for "closing" mass images, when closing is not allowed.
     * 
     * @param im a <code>MimsPlus</code> instance
     */
    public void massImageClosed(MimsPlus im) {
        for (int i = 0; i < massImages.length; i++) {
            if (massImages[i] != null) {
                if (massImages[i].equals(im)) {
                    viewMassMenuItems[i].setSelected(false);
                }
            }
        }
    }
    
    /**
     * Returns the index of the ratio image with numerator
     * <code>numIndex</code> and denominator
     * <code>denIndex</code>.
     *
     * @param numIndex numerator mass index.
     * @param denIndex denominator mass index.
     * @return index of ratio image, -1 of none exists.
     */
    public int getRatioImageIndex(int numIndex, int denIndex) {
        for (int i = 0; i < ratioImages.length; i++) {
            if (ratioImages[i] != null) {
                RatioProps rp = ratioImages[i].getRatioProps();
                if (rp.getNumMassIdx() == numIndex && rp.getDenMassIdx() == denIndex) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the index of the HSI image with numerator
     * <code>numIndex</code> and denominator
     * <code>denIndex</code>.
     *
     * @param numIndex numerator mass index.
     * @param denIndex denominator mass index.
     * @return index of ratio image, -1 of none exists.
     */
    public int getHsiImageIndex(int numIndex, int denIndex) {
        for (int i = 0; i < hsiImages.length; i++) {
            if (hsiImages[i] != null) {
                HSIProps hp = hsiImages[i].getHSIProps();
                if (hp.getNumMassIdx() == numIndex && hp.getDenMassIdx() == denIndex) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Extracts the revision number from the file
     * <code>buildnum.txt</code>
     *
     * @return a string containing the build number.
     */
    private String extractRevisionNumber() {
        String revision = "";
        try {
            InputStream build = getClass().getResourceAsStream("/buildnum.txt");
            InputStreamReader buildr = new InputStreamReader(build);
            BufferedReader br = new BufferedReader(buildr);
            revision = br.readLine();
            if (revision.contains(":")) {
                revision = revision.split(":")[1].trim();
            }
            br.close();
            buildr.close();
            build.close();
        } catch (Exception v) {
            //v.printStackTrace();
        }
        return revision;
    }

    // TODO: Fix Me
    /**
     * Opens a segmented image.
     *
     * @param segImage the data array (width x height x numPlanes).
     * @param description a simple description to be used as a title.
     * @param segImageHeight the height of the image in pixels.
     * @param segImageWidth the width of the image in pixels.
     */
    public void openSeg(int[] segImage, String description, int segImageHeight, int segImageWidth) {

        int npixels = segImageWidth * segImageHeight;
        if (segImage.length % npixels != 0) {
            return;
        }
        int nplanes = (int) Math.floor(segImage.length / npixels);

        //TODO: need to unify these, ie fix the multi-plane part
        if (nplanes > 1) {
            ImageStack stack = new ImageStack(segImageWidth, segImageHeight, nplanes);

            for (int offset = 0; offset < nplanes; offset++) {
                int[] pixels = new int[npixels];
                for (int i = 0; i < npixels; i++) {
                    pixels[i] = segImage[i + (npixels * offset)];
                }
                stack.setPixels(pixels, offset + 1);

            }
            ImagePlus img = new ImagePlus("seg", stack);
            img.show();
        } else {
            MimsPlus mp = new MimsPlus(this, segImageWidth, segImageHeight, segImage, description);
            mp.setHSIProcessor(new HSIProcessor(mp));
            boolean bShow = (mp == null);
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
            }
        }
    }

    /**
     * Updates all images and kills any active ROIs that might be on those
     * images.
     */
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
            if (compImages[i] != null) {
                compImages[i].updateAndDraw();
                compImages[i].killRoi();
            }
        }
    }

    public void recomputeAllImages() {
        recomputeAllHSI();
        recomputeAllRatio();
        //ArrayList<Integer> sumlist = new ArrayList<Integer>();
        List<Integer> sumlist = new ArrayList<Integer>();
        for (int i = 1; i <= ui.getMimsAction().getSize(); i++) {
            sumlist.add(i);
        }
        recomputeAllSum(sumlist);
        recomputeAllComposite();
    }

    /**
     * Updates the scroll bar size/placement.
     *
     * As far as I can tell the only way I can see to do update the scroll bar
     * is to update the slice position twice.
     */
    public void updateScrollbars() {
        int current_slice = massImages[0].getCurrentSlice();
        int num_planes = massImages[0].getNSlices();
        if (current_slice == num_planes) {
            massImages[0].setSlice(num_planes - 1);
        } else {
            massImages[0].setSlice(num_planes);
        }
        massImages[0].setSlice(current_slice);
    }

    /**
     * Recomputes all ratio images. This needs to be done whenever an action is
     * performed that changes the underlying data of the ratio image. For
     * example, using a median filter.
     */
    public void recomputeAllRatio() {
        MimsPlus[] openRatio = this.getOpenRatioImages();
        for (int i = 0; i < openRatio.length; i++) {
            openRatio[i].computeRatio();
            openRatio[i].updateAndDraw();
        }
        //cbControl.updateHistogram();
    }

    /**
     * Recomputes all HSI images. This needs to be done whenever an action is
     * performed that changes the underlying data of the HSI image. For example,
     * using a median filter.
     */
    public void recomputeAllHSI() {
        MimsPlus[] openHSI = this.getOpenHSIImages();
        for (int i = 0; i < openHSI.length; i++) {
            openHSI[i].computeHSI();
            openHSI[i].updateAndDraw();
        }
    }

    /**
     * Recomputes a composite image.
     *
     * @param img the parent image.
     */
    public void recomputeComposite(MimsPlus img) {
        MimsPlus[] openComp = this.getOpenCompositeImages();
        for (int i = 0; i < openComp.length; i++) {
            CompositeProps props = openComp[i].compProps;
            MimsPlus[] parentImgs = props.getImages(this);
            for (int j = 0; j < parentImgs.length; j++) {
                if (parentImgs[j] != null) {
                    if (img.equals(parentImgs[j])) {
                        openComp[i].computeComposite();
                        openComp[i].updateAndDraw();
                    }
                }
            }
        }
    }

    /**
     * Recomputes all composite images.
     */
    public void recomputeAllComposite() {
        MimsPlus[] openComp = this.getOpenCompositeImages();
        for (int i = 0; i < openComp.length; i++) {
            openComp[i].computeComposite();
            openComp[i].updateAndDraw();
        }
    }

    /**
     * Recomputes all sum images
     *
     * @param sumlist list of slices to add in sum
     */
    //public void recomputeAllSum(ArrayList<Integer> sumlist) {
    public void recomputeAllSum(List<Integer> sumlist) {
        MimsPlus[] openSum = this.getOpenSumImages();
        for (int i = 0; i < openSum.length; i++) {
            openSum[i].computeSum(sumlist);
            openSum[i].updateAndDraw();
        }
    }

    /**
     * Catch events such as changing the slice number of a stack or drawing ROIs
     * and if enabled, update or synchronize all images.
     *
     * @param evt a <code>MimsPlusEvent</code> reference
     */
    @Override
    public synchronized void mimsStateChanged(MimsPlusEvent evt) {

        // Do not call updateStatus() here - causes a race condition..
        if (currentlyOpeningImages || bUpdating) {
            return;
        }
        bUpdating = true;

        // Sychronize stack displays.
        if (bSyncStack && evt.getAttribute() == MimsPlusEvent.ATTR_UPDATE_SLICE) {
            MimsPlus mp[] = this.getOpenMassImages();
            MimsPlus rp[] = this.getOpenRatioImages();
            MimsPlus hsi[] = this.getOpenHSIImages();
            MimsPlus sum[] = this.getOpenSumImages();
            MimsPlus comp[] = this.getOpenCompositeImages();

            // Set mass images.
            int nSlice = evt.getSlice();
            boolean updateRatioHSI = evt.getUpdateRatioHSI();
            MimsPlus mplus = evt.getMimsPlus();
            for (int i = 0; i < mp.length; i++) {
                mp[i].setSlice(nSlice);
            }

            if (!isSum) {
                if (updateRatioHSI) {
                    if (mplus == null) {

                        // Update HSI image slice.
                        for (int i = 0; i < hsi.length; i++) {
                            hsi[i].computeHSI();
                        }

                        // Update ratio images.
                        for (int i = 0; i < rp.length; i++) {
                            rp[i].computeRatio();
                        }

                        // Update composite images.
                        for (int i = 0; i < comp.length; i++) {
                            comp[i].computeComposite();
                        }
                    } else {
                        if (mplus.getMimsType() == MimsPlus.RATIO_IMAGE) {
                            mplus.computeRatio();
                        } else if (mplus.getMimsType() == MimsPlus.HSI_IMAGE) {
                            mplus.computeHSI();
                        } else if (mplus.getMimsType() == MimsPlus.COMPOSITE_IMAGE) {
                            mplus.computeComposite();
                        }
                    }
                }
                // Update rois in sum images
                // This is questionable code.
                for (int i = 0; i < sum.length; i++) {
                    // For some reason 1 does not work... any other number does.
                    sum[i].setSlice(2);
                }
            }

            autocontrastAllImages();
            //cbControl.updateHistogram();
            roiManager.updateSpinners();
            bUpdating = false;
            this.mimsStackEditing.resetTrueIndexLabel();
            this.mimsStackEditing.resetSpinners();
            return;

        } else if (evt.getAttribute() == MimsPlusEvent.ATTR_SET_ROI
                || evt.getAttribute() == MimsPlusEvent.ATTR_MOUSE_RELEASE) {
            // Update all images with a selected ROI 
            // MOUSE_RELEASE catches drawing new ROIs             
            int i;
            if (evt.getRoi() != null) {
                evt.getRoi().setStrokeColor(Color.yellow);  // needed to highlight current ROI on all images
            }  
            // previous code did not highlight ShapeRoi objects
            
            // If we get here an no mims or nrrd file has been opened, but a non-mims image has, we
            // need to skip the stuff having to do with mims images, because image will be null
            // in that case.
            MimsPlus mp = (MimsPlus) evt.getSource();
            if (image != null) {
                for (i = 0; i < image.getNMasses(); i++) {
                    if (massImages[i] != mp && massImages[i] != null && bOpenMass[i]) {
                        massImages[i].setRoi(evt.getRoi());
                    }
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
            for (i = 0; i < compImages.length; i++) {
                if (compImages[i] != mp && compImages[i] != null) {
                    compImages[i].setRoi(evt.getRoi());
                }
            }
            for (i = 0; i < nonMIMSImages.size(); i++) {
                if (nonMIMSImages.get(i) != mp && nonMIMSImages.get(i) != null) {
                    nonMIMSImages.get(i).setRoi(evt.getRoi());
                }
            }
            // Automatically appends a drawn ROI to the RoiManager
            // to improve work flow without extra mouse actions.             
            if (evt.getAttribute() == MimsPlusEvent.ATTR_MOUSE_RELEASE) {
                ij.gui.Roi roi = evt.getRoi();
                if (roi != null && roi.getState() != Roi.CONSTRUCTING) {
                    MimsRoiManager2 rm = getRoiManager();
                    rm.add();
                    if (rm.isVisible() == false) {
                        rm.showFrame();
                    }
                }
            }

        } else if (evt.getAttribute() == MimsPlusEvent.ATTR_ROI_MOVED) {
            MimsRoiManager2 rm = getRoiManager();
            Roi roi = evt.getRoi();

            // Lines have to be treated specially.
            if (roi.isLine()) {
                rm.moveLine(rm.getRoiByName(roi.getName()), roi);
            }

            rm.move();
        }

        bUpdating = false;
        if (this.mimsStackEditing != null) {
            this.mimsStackEditing.resetTrueIndexLabel();
            this.mimsStackEditing.resetSpinners();
        }
    }

    /**
     * Determines the behavior when an image is closed. Essentially a
     * bookkeeping method that sets certain member variable to null when
     * corresponding window is closed.
     *
     * @param mp the image being closed.
     */
    public void imageClosed(MimsPlus mp) {
        int i;
        // TODO: add switch case statement
        for (i = 0; i < sumImages.length; i++) {
            if (sumImages[i] != null) {
                if (sumImages[i].equals(mp)) {
                    sumImages[i] = null;
                }
            }
        }
        for (i = 0; i < segImages.length; i++) {
            if (segImages[i] != null) {
                if (segImages[i].equals(mp)) {
                    segImages[i] = null;
                }
            }

        }
        for (i = 0; i < hsiImages.length; i++) {
            if (hsiImages[i] != null) {
                if (hsiImages[i].equals(mp)) {
                    hsiImages[i] = null;
                }
            }

        }
        for (i = 0; i < ratioImages.length; i++) {
            if (ratioImages[i] != null) {
                if (ratioImages[i].equals(mp)) {
                    ratioImages[i] = null;
                }
            }
        }
        for (i = 0; i < compImages.length; i++) {
            if (compImages[i] != null) {
                if (compImages[i].equals(mp)) // DJ: example: it's called when we close a composite image
                {
                    compImages[i] = null;
                }
            }
        }
    }

    /**
     * Returns the prefix of the current data files name. For example:
     * test_file.im = test_file
     *
     * @return prefix file name.
     */
    public String getImageFilePrefix() {
        if (image != null) {
            String filename = image.getImageFile().getName().toString();
            String prefix = filename.substring(0, filename.lastIndexOf("."));
            return prefix;
        } else {
            return "none";
        }
    }
    
     /**
     * Returns the extension of the current data files name. For example:
     * test_file.im = im
     *
     * @return prefix file name.
     */
    public String getImageFileExtension() {
        if (image != null) {
            String filename = image.getImageFile().getName().toString();
            String extension = filename.substring(filename.lastIndexOf("."));  
            // Include the period, because constants like NRRD_EXTENSION include it
            return extension;
        } else {
            return "none";
        }
    }

    /**
     * Returns the prefix of any file name. For example: /tmp/test_file.im =
     * /tmp/test_file
     *
     * @param fileName a file name string for which to find the prefix
     * @return prefix file name.
     */
    public String getFilePrefix(String fileName) {
        String prefix = fileName.substring(0, fileName.lastIndexOf("."));
        return prefix;
    }

    /**
     * Custom actions to be called AFTER initComponent.
     */
    private void initComponentsCustom() {
        this.imgNotes = new imageNotes();
        this.imgNotes.setVisible(false);
        this.testingMenu.setVisible(false);
        
//
//        boolean showDragDropItems = false;
//        this.dragDropMessagejLabel1.setVisible(showDragDropItems);
//        this.dragDropMessagejLabel2.setVisible(showDragDropItems);
//        this.dragDropMessagejLabel3.setVisible(showDragDropItems);
//        this.openPrefsjButton1.setVisible(showDragDropItems);
    }

    public void initComponentsTesting() {
        this.testingMenu.setVisible(true);
    }
    
    public void enableRestoreMimsMenuItem(boolean enable) {
        restoreMimsMenuItem.setEnabled(enable);
    }
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuItem9 = new javax.swing.JMenuItem();
        jPopupMenu1 = new javax.swing.JPopupMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        dragDropMessagejLabel1 = new javax.swing.JLabel();
        dragDropMessagejLabel2 = new javax.swing.JLabel();
        dragDropMessagejLabel3 = new javax.swing.JLabel();
        openPrefsjButton1 = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        jProgressBar1 = new javax.swing.JProgressBar();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openNewMenuItem = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        openNextMenuItem = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        saveMIMSjMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jMenuItem5 = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        preferencesMenuItem = new javax.swing.JMenuItem();
        restoreMimsMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        tileWindowsMenuItem = new javax.swing.JMenuItem();
        closeMenu = new javax.swing.JMenu();
        closeAllRatioMenuItem = new javax.swing.JMenuItem();
        closeAllHSIMenuItem = new javax.swing.JMenuItem();
        closeAllSumMenuItem = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        roiManagerMenuItem = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JSeparator();
        utilitiesMenu = new javax.swing.JMenu();
        generateReportMenuItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        openMyNotes_jMenuItem = new javax.swing.JMenuItem();
        openNewWriter = new javax.swing.JMenuItem();
        openNewDraw = new javax.swing.JMenuItem();
        openNewImpress = new javax.swing.JMenuItem();
        insertPicFrame = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        sumAllMenuItem = new javax.swing.JMenuItem();
        importIMListMenuItem = new javax.swing.JMenuItem();
        captureImageMenuItem = new javax.swing.JMenuItem();
        imageNotesMenuItem = new javax.swing.JMenuItem();
        RecomputeAllMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        batch2nrrdMenuItem = new javax.swing.JMenuItem();
        findMosaic = new javax.swing.JMenuItem();
        findStackFile = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        exportjMenu = new javax.swing.JMenu();
        jMenuItem7 = new javax.swing.JMenuItem();
        exportPNGjMenuItem = new javax.swing.JMenuItem();
        exportQVisMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem8 = new javax.swing.JMenuItem();
        genStackMenuItem = new javax.swing.JMenuItem();
        compositeMenuItem = new javax.swing.JMenuItem();
        correctionsMenu = new javax.swing.JMenu();
        DTCorrectionMenuItem = new javax.swing.JCheckBoxMenuItem();
        QSACorrectionMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        docButton = new javax.swing.JMenuItem();
        sampleDataButton = new javax.swing.JMenuItem();
        testingMenu = new javax.swing.JMenu();
        emptyTestMenuItem = new javax.swing.JMenuItem();

        jMenuItem9.setText("Export all images");

        jMenuItem2.setText("jMenuItem2");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("OpenMIMS");
        setName("NRIMSUI"); // NOI18N
        setPreferredSize(new java.awt.Dimension(778, 560));

        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        jPanel1.setName("Images"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(703, 428));

        dragDropMessagejLabel1.setText("Image files can be opened by dragging them here, or to any");

        dragDropMessagejLabel2.setText("of the tab panes when an image file is already open.");

        dragDropMessagejLabel3.setText("This message can be disabled in the preferences.");

        openPrefsjButton1.setText("Open Preferences");
        openPrefsjButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openPrefsActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(201, 201, 201)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(dragDropMessagejLabel2)
                            .add(dragDropMessagejLabel1)))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(220, 220, 220)
                        .add(dragDropMessagejLabel3))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(284, 284, 284)
                        .add(openPrefsjButton1)))
                .addContainerGap(170, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(89, 89, 89)
                .add(dragDropMessagejLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(dragDropMessagejLabel2)
                .add(34, 34, 34)
                .add(dragDropMessagejLabel3)
                .add(30, 30, 30)
                .add(openPrefsjButton1)
                .addContainerGap(208, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Images", jPanel1);

        stopButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stopsign.png"))); // NOI18N
        stopButton.setEnabled(false);
        stopButton.setIconTextGap(0);
        stopButton.setMaximumSize(new java.awt.Dimension(27, 27));
        stopButton.setMinimumSize(new java.awt.Dimension(27, 27));
        stopButton.setPreferredSize(new java.awt.Dimension(27, 27));
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        jProgressBar1.setBackground(new java.awt.Color(211, 215, 207));
        jProgressBar1.setString("");
        jProgressBar1.setStringPainted(true);

        fileMenu.setText("File");

        openNewMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openNewMenuItem.setMnemonic('o');
        openNewMenuItem.setText("Open MIMS Image...");
        openNewMenuItem.setToolTipText("Open a MIMS image from an existing .im file.");
        openNewMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMIMSImageMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openNewMenuItem);
        openNewMenuItem.getAccessibleContext().setAccessibleDescription("Open a MIMS Image");

        jMenuItem3.setText("Open Non-MIMS Image...");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenNonMIMSimageActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem3);

        openNextMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        openNextMenuItem.setText("Open Next");
        openNextMenuItem.setToolTipText("Open next MIMS image(.im/.nrrd) in folder of current image.");
        openNextMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openNextMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openNextMenuItem);

        jMenuItem1.setText(SAVE_IMAGE);
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMIMSjMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem1);

        saveMIMSjMenuItem.setText(SAVE_SESSION);
        saveMIMSjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMIMSjMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMIMSjMenuItem);
        fileMenu.add(jSeparator5);

        jMenuItem5.setText("Files Manager ...");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem5);
        fileMenu.add(jSeparator7);

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

        preferencesMenuItem.setText("Preferences...");
        preferencesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                preferencesMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(preferencesMenuItem);

        restoreMimsMenuItem.setText("Restore MIMS");
        restoreMimsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restoreMimsMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(restoreMimsMenuItem);

        jMenuBar1.add(editMenu);

        viewMenu.setText("View");

        tileWindowsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.ALT_MASK));
        tileWindowsMenuItem.setText("Tile Windows");
        tileWindowsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tileWindowsMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(tileWindowsMenuItem);

        closeMenu.setText("Close...");

        closeAllRatioMenuItem.setText("Close All Ratio Images");
        closeAllRatioMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeAllRatioMenuItemActionPerformed(evt);
            }
        });
        closeMenu.add(closeAllRatioMenuItem);

        closeAllHSIMenuItem.setText("Close All HSI Images");
        closeAllHSIMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeAllHSIMenuItemActionPerformed(evt);
            }
        });
        closeMenu.add(closeAllHSIMenuItem);

        closeAllSumMenuItem.setText("Close All Sum Images");
        closeAllSumMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeAllSumMenuItemActionPerformed(evt);
            }
        });
        closeMenu.add(closeAllSumMenuItem);

        viewMenu.add(closeMenu);
        viewMenu.add(jSeparator6);

        roiManagerMenuItem.setText("Roi Manager");
        roiManagerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                roiManagerMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(roiManagerMenuItem);
        viewMenu.add(jSeparator8);

        jMenuBar1.add(viewMenu);

        utilitiesMenu.setText("Utilities");

        generateReportMenuItem.setText("Generate Report");
        generateReportMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateReportMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(generateReportMenuItem);

        jMenu1.setText("LibreOffice");
        jMenu1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu1ActionPerformed(evt);
            }
        });

        openMyNotes_jMenuItem.setText("Open My Note's File");
        openMyNotes_jMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMyNotes_jMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(openMyNotes_jMenuItem);

        openNewWriter.setText("Open new writer doc");
        openNewWriter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openNewWriterActionPerformed(evt);
            }
        });
        jMenu1.add(openNewWriter);

        openNewDraw.setText("Open new draw doc");
        openNewDraw.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openNewDrawActionPerformed(evt);
            }
        });
        jMenu1.add(openNewDraw);

        openNewImpress.setText("Open new impress doc");
        openNewImpress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openNewImpressActionPerformed(evt);
            }
        });
        jMenu1.add(openNewImpress);

        insertPicFrame.setText("Insert Draw frame");
        insertPicFrame.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                insertPicFrameActionPerformed(evt);
            }
        });
        jMenu1.add(insertPicFrame);

        utilitiesMenu.add(jMenu1);
        utilitiesMenu.add(jSeparator1);

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

        imageNotesMenuItem.setText("Image Notes...");
        imageNotesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imageNotesMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(imageNotesMenuItem);

        RecomputeAllMenuItem.setText("Recompute All");
        RecomputeAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RecomputeAllMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(RecomputeAllMenuItem);
        utilitiesMenu.add(jSeparator3);

        batch2nrrdMenuItem.setText("Batch covert to nrrd");
        batch2nrrdMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                batch2nrrdMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(batch2nrrdMenuItem);

        findMosaic.setText("Find mosaic file");
        findMosaic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findMosaicActionPerformed(evt);
            }
        });
        utilitiesMenu.add(findMosaic);

        findStackFile.setText("Find stack file");
        findStackFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findStackFileActionPerformed(evt);
            }
        });
        utilitiesMenu.add(findStackFile);

        jMenuItem4.setText("Center of mass autotrack");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        utilitiesMenu.add(jMenuItem4);

        exportjMenu.setText("Export...");
        exportjMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportjMenuActionPerformed(evt);
            }
        });

        jMenuItem7.setText("Current Image");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        exportjMenu.add(jMenuItem7);

        exportPNGjMenuItem.setText("All Derived (png)");
        exportPNGjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPNGjMenuItemActionPerformed(evt);
            }
        });
        exportjMenu.add(exportPNGjMenuItem);

        exportQVisMenuItem.setText("HSI image 3D (qvis)");
        exportQVisMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportQVisMenuItemActionPerformed(evt);
            }
        });
        exportjMenu.add(exportQVisMenuItem);

        utilitiesMenu.add(exportjMenu);
        utilitiesMenu.add(jSeparator4);

        jMenuItem6.setText("Generate HTML");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        utilitiesMenu.add(jMenuItem6);

        jMenuItem8.setText("Generate Table(s)");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        utilitiesMenu.add(jMenuItem8);

        genStackMenuItem.setText("Generate Stack");
        genStackMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                genStackMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(genStackMenuItem);

        compositeMenuItem.setText("Composite");
        compositeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compositeMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(compositeMenuItem);

        jMenuBar1.add(utilitiesMenu);

        correctionsMenu.setText("Corrections");

        DTCorrectionMenuItem.setText("Apply dead time correction");
        DTCorrectionMenuItem.setEnabled(false);
        DTCorrectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DTCorrectionMenuItemActionPerformed(evt);
            }
        });
        correctionsMenu.add(DTCorrectionMenuItem);

        QSACorrectionMenuItem.setText("Apply QSA correction");
        QSACorrectionMenuItem.setEnabled(false);
        QSACorrectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                QSACorrectionMenuItemActionPerformed(evt);
            }
        });
        correctionsMenu.add(QSACorrectionMenuItem);

        jMenuBar1.add(correctionsMenu);

        helpMenu.setText("Help");

        aboutMenuItem.setText("About OpenMIMS");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        docButton.setText("Documentation");
        docButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                docButtonActionPerformed(evt);
            }
        });
        helpMenu.add(docButton);

        sampleDataButton.setText("Sample Data");
        sampleDataButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleDataButtonActionPerformed(evt);
            }
        });
        helpMenu.add(sampleDataButton);

        jMenuBar1.add(helpMenu);

        testingMenu.setText("Testing");

        emptyTestMenuItem.setText("warp test");
        emptyTestMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                emptyTestMenuItemActionPerformed(evt);
            }
        });
        testingMenu.add(emptyTestMenuItem);

        jMenuBar1.add(testingMenu);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(jProgressBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(stopButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
                .add(5, 5, 5)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jProgressBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(stopButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(16, 16, 16))
        );

        getAccessibleContext().setAccessibleDescription("NRIMS Analyais Module");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Restores the image to its unmodified state. Undoes and compression,
     * reinserts all deleted planes, and sets all translations back to zero.
     */
    private void restoreMimsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {

        int currentSlice = massImages[0].getCurrentSlice();

        mimsStackEditing.uncompressPlanes();

        // concatenate the remaining files.
        for (int i = 1; i <= mimsAction.getSize(); i++) {
            massImages[0].setSlice(i);
            if (mimsAction.isDropped(i)) {
                mimsStackEditing.insertSlice(i);
            }
        }
        mimsStackEditing.untrack();

        mimsStackEditing.resetTrueIndexLabel();
        mimsStackEditing.resetSpinners();

        massImages[0].setSlice(currentSlice);
    }

    /**
     * Sets isRatio to true, meaning data shown in ratio images are ratio values
     * and NOT percent turnover.
     *
     * @param selected <code>true</code> if generating ratio images with ratio
     * values, otherwise <code>false</code>.
     */
    public void setIsRatio(boolean selected) {
        isRatio = selected;
    }

    /**
     * Returns
     * <code>true</code> if generating ratio images with ratio values and NOT
     * percent turnover.
     *
     * @return boolean.
     */
    public boolean getIsRatio() {
        return isRatio;
    }

    /**
     * Sets isPercentTurnover to true, meaning data shown in ratio images are
     * percent turnover values and NOT ratio values.
     *
     * @param selected <code>true</code> if generating ratio images with percent
     * turnover values, otherwise <code>false</code>.
     */
    public void setIsPercentTurnover(boolean selected) {
        isPercentTurnover = selected;
    }

    /**
     * Returns
     * <code>true</code> if generating ratio images with percent turnover values
     * and NOT ratio values.
     *
     * @return boolean.
     */
    public boolean getIsPercentTurnover() {
        return isPercentTurnover;
    }

    /**
     * Sets the isSum member variable to
     * <code>set</code> telling the application if ratio and HSI images should
     * be generated as sum images, instead of plane by plane.
     *
     * @param set <code>true</code> if generating ratio image and HSI images as
     * sum images, otherwise false.
     */
    public void setIsSum(boolean set) {
        isSum = set;
    }

    /**
     * Sets the DTCorrected flag.
     *
     * @param isDTCorrected true to set the DT corrected flag, false to clear it
     */
    public void setIsDTCorrected(boolean isDTCorrected) {
        this.isDTCorrected = isDTCorrected;
    }

    /**
     * Sets the QSACorrected flag.
     *
     * @param isQSACorrected true to set the QSA corrected flag, false to clear it
     */
    public void setIsQSACorrected(boolean isQSACorrected) {
        this.isQSACorrected = isQSACorrected;
    }

    /**
     * Sets the beta QSA correction parameters.
     *
     * @param betas an array of floats for setting the values of the beta QSA parameters
     */
    public void setBetas(float[] betas) {
        this.betas = betas;
    }

    /**
     * Sets the FC Objective QSA correction parameter.
     *
     * @param fc_objective sets the value of the FC Objective QSA correction parameter
     */
    public void setFCObjective(float fc_objective) {
        this.fc_objective = fc_objective;
    }

    /**
     * Returns
     * <code>true</code> if the application is generating ratio and HSI images
     * as sum images.
     *
     * @return <code>true</code> if generating ratio images and HSI images as
     * sum images, otherwise <code>false</code>.
     */
    public boolean getIsSum() {
        return isSum;
    }

    /**
     * Sets the isWindow member variable to
     * <code>set</code> telling the application if ratio and HSI images should
     * be generated with a window of n planes.
     *
     * @param set <code>true</code> if generating ratio and HSI images with a
     * window of <code>n</code> planes.
     */
    public void setIsWindow(boolean set) {
        isWindow = set;
    }

    /**
     * Returns
     * <code>true</code> if the application is generating ratio and HSI images
     * with a window of
     * <code>n</code> planes.
     *
     * @return <code>true</code> if generating ratio and HSi images with a
     * window of <code>n</code> planes, otherwise <code>false</code>.
     */
    public boolean getIsWindow() {
        return isWindow;
    }

    /**
     * Used to set the number of planes to when generating ratio and HSI images
     * with a sliding window. Actual window size will be 2 times window range
     * plus 1 (The current window is currentplane - windowSize up to
     * currentplane + windowSize).
     *
     * @param range the "radius" of the sliding window.
     */
    public void setWindowRange(int range) {
        windowRange = range;
    }

    /**
     * Returns the size of the sliding window.
     *
     * @return size of sliding window.
     */
    public int getWindowRange() {
        return windowRange;
    }

    /**
     * Tells the application to use the median filter when generating ratio and
     * HSI images. All open ratio and HSI images will be regenerated.
     *
     * @param set <code>true</code> if computing ratio and HSI images with a
     * median filter, othewise <code>false</code>.
     */
    public void setMedianFilterRatios(boolean set) {
        medianFilterRatios = set;
    }

    /**
     * Returns
     * <code>true</code> if the application is using a median filter when
     * generating ratio and HSI images.
     *
     * @return <code>true</code> if using a median filter, * *
     * otherwise <code>false</code>.
     */
    public boolean getMedianFilterRatios() {
        return medianFilterRatios;
    }

    /**
     * Will perform and autocontrast on all open mass, ratio and sum images.
     */
    public void autocontrastAllImages() {
        // All mass images             
        MimsPlus mp[] = getOpenMassImages();
        for (int i = 0; i < mp.length; i++) {
            if (mp[i].getAutoContrastAdjust()) {
                autoContrastImage(mp[i]);
            }
        }

        // All ratio images
        MimsPlus rp[] = getOpenRatioImages();
        for (int i = 0; i < rp.length; i++) {
            if (rp[i].getAutoContrastAdjust()) {
                autoContrastImage(rp[i]);
            }
        }

        // All sum images
        MimsPlus sp[] = getOpenSumImages();
        for (int i = 0; i < sp.length; i++) {
            if (sp[i].getAutoContrastAdjust()) {
                autoContrastImage(sp[i]);
            }
        }
    }

    /**
     * Autocontrasts an images according to ImageJ's autocontrasting algorithm.
     *
     * @param imgs an array of images to autocontrast.
     */
    public void autoContrastImages(MimsPlus[] imgs) {
        for (int i = 0; i < imgs.length; i++) {
            if (imgs[i] != null) {
                autoContrastImage(imgs[i]);
            }
        }
    }

    /**
     * Autocontrast an image according to ImageJ's autocontrasting algorithm.
     *
     * @param img an image to autocontrast.
     */
    public void autoContrastImage(MimsPlus img) {
        ContrastAdjuster ca = new ContrastAdjuster(img);
        ca.doAutoAdjust = true;
        ca.doUpdate(img);
    }

    public void logException(Exception e) {
        IJ.log(e.toString());

        StackTraceElement[] trace = e.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            IJ.log(trace[i].toString());
        }
    }

    /**
     * An action method for the Edit>Preferences... menu item.
     */
    private void preferencesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_preferencesMenuItemActionPerformed

        if (this.prefs == null) {
            prefs = new PrefFrame(this);
        }
        prefs.showFrame();
        boolean showDragDropItems = prefs.getShowDragDropMessage();
        this.showHideDragDropMessage(showDragDropItems);
    }//GEN-LAST:event_preferencesMenuItemActionPerformed

    
    /**
     * Sets whether or not to show the drag and drop message on the blank
     * area of the window that is visible when OpenMIMS is first launched.
     * This gets called if the user changes the state of the check box labeled
     * "Show drag-drop message on startup" in the preferences dialog.
     *
     * @param showDragDropItems boolean to control visibility of drag and
     * drop message lines and controls.
     */
    public void showHideDragDropMessage(boolean showDragDropItems) {
        this.dragDropMessagejLabel1.setVisible(showDragDropItems);
        this.dragDropMessagejLabel2.setVisible(showDragDropItems);
        this.dragDropMessagejLabel3.setVisible(showDragDropItems);
        this.openPrefsjButton1.setVisible(showDragDropItems);        
    }
    
    public void showHideROIManager(boolean showROImanager) {
        if (showROImanager) {
            getRoiManager().viewManager();
        }
    }
    
    /**
     * Action method for the View>Tile Windows menu item.
     */
    private void tileWindowsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        //ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
        //wo.run("tile");

        tileWindows();
    }

    /**
     * Saves the current analysis session. If
     * <code>saveImageOnly</code> is set to
     * <code>true</code> than only the mass image data will be saved to a file
     * with name
     * <code>fileName</code> (always ending with .nrrd). If
     * <code>saveImageOnly</code> is set to
     * <code>false</code> than mass image data will be save ALONG with a roi zip
     * file containing all ROIs, all ratio, HSI and sum images. All files will
     * have the same prefix name, but will slightly differ in their endings as
     * well as their extensions.
     *
     * @param fileName the name of the file to be saved.
     * @param saveImageOnly <code>true</code> if saving only the mass image
     * data, <code>false</code> if saving Rois and derived images.
     * @return <code>true</code> if successful, otherwise <code>false</code>
     */
    public boolean saveSession(String fileName, boolean saveImageOnly) {

        // Initialize variables.
        File file = new File(fileName);
        String directory = file.getParent();
        String name = file.getName();
        String baseFileName;
        String onlyFileName;
        
        try {

            if (imgNotes != null) {
                getOpener().setNotes(imgNotes.getOutputFormatedText());
            }
            //Change missing from svn->git migration
            String[] pos = FileUtilities.validStackPositions(getOpenMassImages()[0]);
            getOpener().setStackPositions(pos);
            //end
            if (mimsAction.getIsTracked()) {
                double max_delta = mimsAction.getMaxDelta();
                DecimalFormat twoDForm = new DecimalFormat("#.##");
                insertMetaData(Opener.Max_Tracking_Delta, twoDForm.format(max_delta));
            }
            if (!metaData.isEmpty()) {
                getOpener().setMetaDataKeyValuePairs(metaData);
            }
            // Set DT correction flag.
            getOpener().setIsDTCorrected(isDTCorrected);

            // Set QSA correction flag.
            getOpener().setIsQSACorrected(isQSACorrected);

            // Set QSA correction parameters.
            if (isQSACorrected) {
                getOpener().setBetas(betas);
                getOpener().setFCObjective(fc_objective);
            }

            // Save the original .im file to a new file of the .nrrd file type.
            String nrrdFileName = name;
            if (!name.endsWith(NRRD_EXTENSION)) {
                nrrdFileName = name + NRRD_EXTENSION;
            }
            
            // Save the file.
            boolean imgModified = ui.getMimsAction().isImageModified();
            boolean notesModified = imgNotes.isModified();
            boolean interleaved = ui.getMimsAction().getIsInterleaved();
            
            // saveImageOnly is true if the user selected the menu option to
            // save an image, and false if he/she selected Save session.
            
            
            if (saveImageOnly || imgModified || interleaved || notesModified) {
                ImagePlus[] imp = getOpenMassImages();   
                if (imp == null) {
                    return false;
                }
                Nrrd_Writer nw = new Nrrd_Writer(this);
                File dataFile = nw.save(imp, directory, nrrdFileName);

                // Update the Opener object.
                image.close();
                image = new Nrrd_Reader(dataFile);
                openers.clear();
                openers.put(nrrdFileName, image);

                // Update the Action object.
                mimsAction = new MimsAction(image);

                // Update the Data tab
                mimsData = new MimsData(this, image);
                jTabbedPane1.setComponentAt(0, mimsData);
           
                // Update the image titles.             
                for (int i = 0; i < imp.length; i++) {
                    imp[i].setTitle((new MimsPlus(this, i)).getTitle());
                }
                baseFileName = getFilePrefix(dataFile.getAbsolutePath());
                onlyFileName = getFilePrefix(dataFile.getName());
            } else {  
                baseFileName = this.getLastFolder() + "/" + this.getImageFilePrefix();
                onlyFileName = this.getImageFilePrefix();
                if (!imgModified) {
                    JOptionPane.showConfirmDialog(null, "No images have been modified, so the image file will not be saved.\n"
                    + "However, any HSI, ratio, sum, or composite images will be saved in a separate .zip file.", 
                    "Unmodified Image", 
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE);
                }
            }
            if (saveImageOnly) {
                return true;
            }
            //save additional images (sum, ratio, etc) and rois
            FileUtilities.saveAdditionalData(baseFileName, onlyFileName, this);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Action method for File>Open Mims Image menu item. If opening
     */
    private void openMIMSImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {

        boolean proceed = checkCurrentFileStatusBeforeOpening();
        if (!proceed) {
            return;
        }
       
        // Bring up file chooser.
        MimsJFileChooser fc = new MimsJFileChooser(this);
        fc.setMultiSelectionEnabled(false);
        int returnVal = fc.showOpenDialog(this);

        // Open file or return null.
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            MimsRoiManager2 roiManager = getRoiManager();
            roiManager.selectAll();  // Do not show the ROI manager or ROIs unless user wants them.
            roiManager.delete(false, false);
            roiManager.close();
            openFileInBackground(file);
            enableRestoreMimsMenuItem(true); 
            roiManager.setNeedsToBeSaved(false);
            startAutosaveROIthread(false);   // false = do not interrupt
        }
        if (returnVal == JFileChooser.CANCEL_OPTION) {
            return;
        }
        
        task.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equalsIgnoreCase("progress")) {
                    int progress = task.getProgress();
                    if (progress == 0) {
                        if (getPreferences().getShowAutosaveROISdialog()) {                         
                            loadROIfiles(fc.getSelectedFile()); 
                        }  
                        System.out.println("enabling Save Button for ROI Manager");
                        roiManager.enableSaveButton(true);
                    }
                }
            }
        });        
    }
    
     /**
     * Load the most recent ROI file associated with the current image.
     *
     * @param selectedImageFile the image file for which we wish to retrieve ROI information
     */   
    private void loadROIfiles(File selectedImageFile) {
          
        getRoiManager().close();
                 
        //  If not already present, create a hidden tmp folder where autosaved data will be saved.
        String tmpDir = null;       
        if (operatingSystem.compareToIgnoreCase("windows") == 0) {
            tmpDir = "\\tmp";   
        } else {
            tmpDir = "/.tmp";   // some kind of Unix, such as Linux, MacOS, etc
        }
        
        List<File> roiFiles = new ArrayList<File>();    
        File[] files = null;        

        String parentDir = selectedImageFile.getParent();  // gets parent directory
        String tempDirStr = parentDir.concat(tmpDir);
        tempDir = new File(tempDirStr);
         
        String javaTempDir = System.getProperty("java.io.tmpdir");
        
        if (!tempDir.exists()) {

            System.out.println("UI:loadROIfiles: tempdir path is " + tempDir.getPath());
            
            //System.out.println("UI:loadROIfiles: tempdir absolute path is " + tempDir.getAbsolutePath());
            try {
                System.out.println("UI:loadROIfiles: tempdir canonical path is " + tempDir.getCanonicalPath());
            } catch (Exception e) {
                //System.out.println("Error attempting to create a .tmp directory");
            }
            
            if (!tempDir.mkdir()) {   // This is failing if we do not own the parent directory
                tempDir = new File(javaTempDir);
                System.out.println("Error attempting to create a .tmp subdirectory in the directory containing the image file.\n"
                        + "Using the java temporary folder " + tempDir + " instead.");               
            }
                                              
        } 
        //if (tempDir.exists()) {
            // If the tempDir is not writable, save the ROI files to the java temporary file location.
            // Previously determined like this:
            
            // On OSX, this is  /var/folders/_2/6c3ch7n109d_bbt_f59fh6fm0000gn/T/   or something similar
            // On Linux, it is /tmp
            // Windoze, it is \\tmp

            if (!tempDir.canWrite()) {
                tempDir = new File(javaTempDir);
                
                OMLOGGER.info("openMIMSImageMenuItemActionPerformed:  The default temporary directory (" +
                        tempDir + ") is read-only.  \n"
                        + "The java temporary directory  " + tempDir + " will be used instead.");
            }       
            
           // File[] files = new File(tempDir.getCanonicalPath()).listFiles();
            //If this pathname does not denote a directory, then listFiles() returns null.
                        
            boolean userSelectedNRRDfile = false;
            boolean userSelectedIMfile = false;
            String selectedImageFilesStr = selectedImageFile.getName();
            if (selectedImageFilesStr.endsWith(".nrrd")) {
                userSelectedNRRDfile = true;
            }
            if (selectedImageFilesStr.endsWith(".im")) {
                userSelectedIMfile = true;
            }
        
            try {
                files = new File(tempDir.getCanonicalPath()).listFiles(new FilenameFilter() { 
                    @Override public boolean accept(File dir, String name) { 
                        return name.endsWith(".rois.zip"); 
                    } 
                }); 
              
                // This for loop is ugly, but will work when we are considering just nrrd and im files.
                for (File roiFile : files) {
                    if (roiFile.isFile()) {
                        // Only add the ROI files that match the selected mim or nrd file name provided by the user.
                        String roiFileStr = roiFile.getName();
                        
                        if (roiFileStr.endsWith(".zip")) {
                            // Remove the .zip first, then the .nrrd or .im
                            roiFileStr = FilenameUtils.removeExtension(roiFileStr);
                            if (roiFileStr.endsWith("rois")) {
                                roiFileStr = FilenameUtils.removeExtension(roiFileStr); 
                            } else {
                              break; 
                            }                        
                        }

                        String selectedFilename = FilenameUtils.removeExtension(selectedImageFile.getName());
                        boolean useThisFile = false;                      
                        if (roiFileStr.startsWith(selectedFilename)) {
                            if (roiFileStr.contains("-nrrd") && userSelectedNRRDfile) {
                                useThisFile = true;
                            }
                            if (roiFileStr.contains("-im") && userSelectedIMfile) {
                                useThisFile = true;
                            }                      
                            if (useThisFile) {
                                roiFiles.add(roiFile);                                  
                            }
                        }
                    }
                }   
            } catch (IOException ioe) {
                System.out.println("UI.java:loadROIfiles: IOexception while loading ROI files.");
                ioe.printStackTrace();
            }
                        
            // Sort these so that the most recent file is the first one, even if the autosave 
            // interval is less than 1 minute.
            Collections.sort(roiFiles, new Comparator<File>() {
                public int compare(File o1, File o2) {
                    return Long.compare(o1.lastModified(), o2.lastModified());
                }
            });
            
            Collections.reverse(roiFiles);  // get most recent one first
            
            int option = 0;
            Object[] options = {"Load autosaved ROIs",
                "Don't load autosaved ROIs",
                "Delete autosaved ROIs"};
            // Are there any files in the .tmp directory?
            if (!roiFiles.isEmpty()) {     
                // Get the current autosave interval
                int autosaveInterval = ui.getPreferences().getAutoSaveInterval();
                // Ask user if he/she wants to load the existing ROIs  
                option = JOptionPane.showOptionDialog(this,
                    "Load existing ROIs? \nThe current ROI autosave interval is " + autosaveInterval + " seconds.\n"
                            + "This can be changed in Preferences.",
                    "ROIs exist in current directory",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);   // used to set which button is the default           
            } else {
                //nuttin'
            }
  
            if (option == 0) {   // Yes. load ROIs       
                if (roiFiles.size() > 0) {
                    //openFileInBackground(files[0]);     // messes up lastFolder somehow 
                    OMLOGGER.info("UI.java:loadROIfiles:  Loading ROI file " + roiFiles.get(0));              
                   
                    openFileInBackground(roiFiles.get(0));
                     
                    System.out.print("loading roi file ");
                    do { 
                        try {
                           System.out.print(".");
                           Thread.sleep(100);
                        } catch (InterruptedException ie) {
                           // do nothing
                        }
                    } while (!task.isDone());    
                    System.out.println("");
                }              
            }                              
            if (option == 1) {   // No, keep existing ROIs
                // Don't load any ROIs, but also do not delete them.
                // But do remove them from the ROI window, just not from disk
                //getRoiManager().clearRoiJList();
                getRoiManager().delete(false, false);
                getRoiManager().close();
            }
            if (option == 2) {  // Don't load ROIs, and delete existing ROI zip files
                if (roiFiles != null) {
                    for (File afile : roiFiles) {
                        if (afile.isFile()) {
                            afile.delete();
                        }
                    }
                }
            } 
        //} 
        
       // setLastFolder(tempDir.getParentFile());  // restore default folder
         
        String last = null; 
        try {
            last = tempDir.getParentFile().getCanonicalPath();
            ij.io.OpenDialog.setLastDirectory(last);
        } catch (IOException ioe) {
            System.out.println("IOException while trying to set path to last path used.");
        }
        setIJDefaultDir(last);
        this.setLastFolder(last);
                
        if (operatingSystem.compareToIgnoreCase("windows") == 0) {
            System.out.println("Trying to set hidden file attribute under Windows");
            try {
                hide(tempDir);
                //Path path = Paths.get(tempDir.getCanonicalPath());
                //Files.setAttribute(path, "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS); //< set hidden attribute
                // This does not work completely.   It does set something in the directory, such that when you get 
                // properties on it, the hidden check box is checked, but the directory is visible in the File Mangler
                // and you can copy things into the directory.
            } catch (Exception e) {
                System.out.println("Error attempting to set the hidden file attribute under Windows");
            }              
        } else {
            //System.out.println("Some version of OSX, MacOS, Linux, or other Unix. ");
        }
        
    }
   
     /**
     * Hide a file on Windows
     *
     */
    void hide(File src) throws InterruptedException, IOException {
        // win32 command line variant
        Process p = Runtime.getRuntime().exec("attrib +h " + src.getPath());
        p.waitFor(); // p.waitFor() important, so that the file really appears as hidden immediately after function exit.
    }
    
    
     /**
     * Returns the hidden temporary directory (tmp) that is created in the same directory as the last file opened. 
     *
     * @return String the tmp directory, or null if the directory is read-only or not owned by user.
     */
    public File getTempDir() {
        if (tempDir == null) {
            return null;
        }
        if (tempDir.canWrite()) {
            return tempDir;
        } else {
            return null;
        }
    }

    /**
     * check whether the numerator mass and denominator mass referred to by
     * indices are within the range of the ones supplied in arguments. Used in
     * restoreState to check HSI/Ratio/Sum-Ratio images indices/masses Depends
     * on no globals, can probably move to new helper class
     *
     * @param nidx numerator index to check
     * @param didx denominator index to check
     * @param tolerance range to be considered
     * @param numMass numerator mass to check against index
     * @param denMass denominator mass to check against index
     * @return true if within range, and false if not
     */
    public boolean withinMassRange(int nidx, int didx, double tolerance, double numMass, double denMass) {
        String[] names = this.getOpener().getMassNames();
        double numDiff, denDiff;
        double mindiff = Double.MAX_VALUE;
        if (nidx == -1 || didx == -1) {
            return false;
        }
        double nMass = Double.valueOf(names[nidx]);
        double dMass = Double.valueOf(names[didx]);
        numDiff = Math.abs(numMass - nMass);
        denDiff = Math.abs(denMass - dMass);
        if (numDiff < mindiff && numDiff < tolerance && denDiff < mindiff && denDiff < tolerance) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * check whether the mass referred to by an index are within the range of
     * one supplied in the argument. Used in restoreState to check Sum-Mass
     * Images indices/masses Depends on no globals, can probably move to new
     * helper class
     *
     * @param nidx index to check
     * @param tolerance range to be considered
     * @param numMass mass to check against index
     * @return true if within range, and false if not
     */
    public boolean withinMassRange(int nidx, double tolerance, double numMass) {
        String[] names = this.getOpener().getMassNames();
        double numDiff;
        double mindiff = Double.MAX_VALUE;
        if (nidx == -1) {
            return false;
        }
        double nMass = Double.valueOf(names[nidx]);
        numDiff = Math.abs(numMass - nMass);
        if (numDiff < mindiff && numDiff < tolerance) {
            return true;
        } else {
            return false;
        }
    }
    

    /**
     * DJ: 07/28/2014 Check whether two masses are close enough within the
     * tolerance range to be considered equal.
     *
     * @param mass1 first mass to consider.
     * @param mass2 second mass to consider.
     * @param tolerance range to be considered.
     * @return true if the two masses are within range.
     */
    public static boolean massesEqualityCheck(double mass1, double mass2, double tolerance) {

        double numDiff = Math.abs(mass1 - mass2);

        if (numDiff < tolerance) {
            return true;
        } else {
            return false;
        }
    }

    // DJ: 08/12/2014
    /**
     * gives back the number of groups where images are
     * categorized :
     *
     * @return int of number of groups, if no groups are detected, return 0
     */
    public static int getNumberOfGroups() {

        MimsPlus[] massImages = ui.getOpenMassImages();

        int current_collection_set = 0;
        int collectionsCount = 0;

        for (int i = 0; i < massImages.length; i++) {

            int indexOfOpenBrakets = massImages[i].getTitle().indexOf("(");
            int indexOfCloseBrakets = massImages[i].getTitle().indexOf(")");

            String collection = "";

            if (indexOfOpenBrakets != -1 && indexOfCloseBrakets != -1) {
                collection += massImages[i].getTitle().substring(indexOfOpenBrakets + 1, indexOfCloseBrakets);
                if (Integer.parseInt(collection) > current_collection_set) {
                    current_collection_set = Integer.parseInt(collection);
                    collectionsCount += 1;
                }
            }
        }

        return collectionsCount;
    }

    // DJ: 07/31/2014 To be used in MimsHSIView.java
    /**
     * Checks if numerator and denumerator are suitable to construct a ratio
     *
     * @param num string of the MASS-SYMBOL to be considered as numerator.
     * @param den string of the MASS-SYMBOL to be considered as denumerator.
     * @return true if the numerator and denumerator symbols are both suitable
     * to construct a mass ratio. False otherwise.
     */
    public static boolean validRatioChecker(String num, String den) {

        if(num == null || den == null ){
            return false;
        }
        if (num.length() != den.length()) {
            return false;       // no different lengths : 12C14N/12C
        }
        if (num.equals(den)) {
            return false;                    // no same num and den  : 12C/12C
        }
        if (num.equals("-") || den.equals("-")) {
            return false; // no zero involvemnt   : 0/12C  or  12C/0
        }

        boolean isValidRatio = true;

        ArrayList<Integer> num_coefs_list = new ArrayList<Integer>(); // to hold the numerators coeffecients: 12C14N -->  (12, 14)
        ArrayList<String> num_symbols_list = new ArrayList<String>();  // to hold the numerators symbols : 12C14N ------>  (C, N)

        ArrayList<Integer> den_coefs_list = new ArrayList<Integer>(); // to hold the denumerators coeffecients:12C14N --> (12, 14)
        ArrayList<String> den_symbols_list = new ArrayList<String>();  // to hold the dennumerators symbols : 12C14N ----> (C, N)

        String num_coef = "";
        String num_symbol = "";
        String den_coef = "";
        String den_symbol = "";

        //-----------------------------------------------------------------
        for (int i = 0; i < num.length(); i++) {
            char c = num.charAt(i);
            char next_c;
            if (i + 1 != num.length()) {  // to avoid the case of going over the length of the numerator string.
                next_c = num.charAt(i + 1);
            } else {
                next_c = '-'; // or any non-letter symbol.
            }
            if (Character.isDigit(c)) {
                num_coef += c;
                if (!Character.isDigit(next_c)) {
                    num_coefs_list.add(Integer.decode(num_coef));
                    num_coef = "";
                }
            } else if (Character.isLetter(c)) {
                num_symbol += c;
                if (!Character.isLetter(next_c)) {
                    num_symbols_list.add(num_symbol);
                    num_symbol = "";
                }

            } else {
                ;
            }
        }

        //-----------------------------------------------------------------
        for (int i = 0; i < den.length(); i++) {
            char c = den.charAt(i);
            char next_c;
            if (i + 1 != den.length()) {
                next_c = den.charAt(i + 1);
            } else {
                next_c = '-'; // or any non-letter symbol.
            }

            if (Character.isDigit(c)) {
                den_coef += c;
                if (!Character.isDigit(next_c)) {
                    den_coefs_list.add(Integer.decode(den_coef));
                    den_coef = "";
                }
            } else if (Character.isLetter(c)) {
                den_symbol += c;
                if (!Character.isLetter(next_c)) {
                    den_symbols_list.add(den_symbol);
                    den_symbol = "";
                }

            } else {
                ;
            }
        }

        // treat the "Se" seperately as a special case since for instance 77se/80se is fine     
        for (int i = 0; i < num_symbols_list.size(); i++) {

            // to make sure for "Se", we always get 80 in Denumerator as it's most abundant
            if (num_symbols_list.get(i).equals("Se") && den_symbols_list.get(i).equals("Se") && (den_coefs_list.get(i)).intValue() == 80) {
                return true;
            }

            // to make explicitly sure that any other "Se" ratios where the denumerator isn't 80 are not considered correct
            if (num_symbols_list.get(i).equals("Se") && den_symbols_list.get(i).equals("Se") && den_coefs_list.get(i).intValue() != 80) {
                return false;
            }

            // For the rest, first coeffieciant of numerator should always be greater than the first coeffecient of denumenator.
            // for instance: to avoid having 12C/13C
            if (num_coefs_list.get(0).intValue() < den_coefs_list.get(0)) {
                return false;
            }
        }

        for (int i = 0; i < num_symbols_list.size(); i++) {
            if (!(num_symbols_list.get(i)).equals(den_symbols_list.get(i)) // to avoid for instance 12C/31P.
                    || (num_coefs_list.get(i)).intValue() < (den_coefs_list.get(i)).intValue() // to avoid for instance: 12C14N/12C15N which should be the inverse.
                    ) {
                return false;
            }
        }


        return isValidRatio;
    }

    /**
     * Regenerates the ratio images, hsi images and sum images with the
     * properties specified in the arrays. Used when opening a new data file and
     * the user wishes to generate all the same derived images that were open
     * with the previous data file.
     *
     * @param mass_props array of <code>MassProps</code> objects.
     * @param rto_props array of <code>RatioProps</code> objects.
     * @param hsi_props array of <code>HSIProps</code> objects.
     * @param sum_props array of <code>SumProps</code> objects.
     * @param composite_props array of <code>CompositeProps</code> objects
     * @param same_size <code>true</code> restores magnification.
     * @param roiManagerVisible <code>true</code> region of interest.
     */
    public void restoreState(MassProps[] mass_props, RatioProps[] rto_props, HSIProps[] hsi_props, SumProps[] sum_props, CompositeProps[] composite_props, boolean same_size, boolean roiManagerVisible) {

        // just to tile the the new windows that were not present in the previous file
        // The states of the ones that were present in the previous file, will be handled by applyStateWindow(mass_props)
        
        
        /*
        ij.gui.GenericDialog gd = new ij.gui.GenericDialog("Options Chooser");
        String[] choices = {"ch 0", "ch 1", "ch 2", "ch 3", "ch 4", "ch 5", "ch 6", "ch 7"};
        boolean[] defChoices = {false, true, false, true, false, true, false, true};
        gd.addCheckboxGroup(4, 2, choices, defChoices);
        gd.validate();
        
        
        
        
        gd.showDialog();
        
        
        
        if(gd.wasOKed()){
            System.out.println("wasOked");
            System.out.println("ch 0  = " + ((java.awt.Checkbox) gd.getCheckboxes().elementAt(0)).getState());
            System.out.println("ch 1  = " + ((java.awt.Checkbox) gd.getCheckboxes().elementAt(1)).getState());
            System.out.println("ch 2  = " + ((java.awt.Checkbox) gd.getCheckboxes().elementAt(2)).getState());
            System.out.println("ch 3  = " + ((java.awt.Checkbox) gd.getCheckboxes().elementAt(3)).getState());

        }else
            System.out.println("wasCancelled");
        
        //ij.gui.YesNoCancelDialog dialogBox = new ij.gui.YesNoCancelDialog(this, "Options Chooser", "");
        //dialogBox.
        * 
        */
        
        
        
        /*
        System.out.println("================================");
        try {
            String line;
            Process p = Runtime.getRuntime().exec("ps -e");
            BufferedReader input =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                System.out.println(line); //<-- Parse data here.
            }
            input.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
        System.out.println("================================");
        */
        
        
        
        
        
        
        //MimsRoiManager2 rm2 = new MimsRoiManager2(this);
        //rm2.setVisible(true);
        
        
        //Change missing from svn->git migration
        //Do basic screen positioning/contrast
        tileWindows();
        autoContrastImages(getOpenMassImages());
        //Apply previous state, if it esists
        applyWindowState(mass_props);
        //end
        
        if (mass_props == null) {
            mass_props = new MassProps[0];
        }
        if (rto_props == null) {
            rto_props = new RatioProps[0];
        }
        if (hsi_props == null) {
            hsi_props = new HSIProps[0];
        }
        if (sum_props == null) {
            sum_props = new SumProps[0];
        }
        if (composite_props == null) {
            composite_props = new CompositeProps[0];
        }

        MimsPlus mp;
        
        // DJ : filtered_mass_props would hold just the props that their massvalues are actually 
        // IN the image file we've just opened and trying to restore its specs.
        filtered_mass_props = new ArrayList<MassProps>();

        for (int i = 0; i < image.getMassNames().length; i++) {
            for (int j = 0; j < mass_props.length; j++) {
                if (massesEqualityCheck(mass_props[j].getMassValue(), Double.parseDouble((image.getMassNames())[i]), 0.49)){
                    MassProps m = new MassProps(getOpenMassImages()[i].getMassIndex());
                    m.setMassValue(getOpenMassImages()[i].getMassValue());
                    filtered_mass_props.add(m);
                    break;                   
                }
            }
        }
        //-----------------------------------
        // Generate ratio images. DJ: 07/28/2014
        for (int i = 0; i < rto_props.length; i++) {

            boolean isExist_num = false, isExist_den = false;

            for (int y = 0; y < filtered_mass_props.size(); y++) {

                if (massesEqualityCheck(filtered_mass_props.get(y).getMassValue(), rto_props[i].getNumMassValue(), 0.49)) {
                    isExist_num = true;
                    rto_props[i].setNumMassValue(filtered_mass_props.get(y).getMassValue());

                    if (filtered_mass_props.get(y).getMassIdx() == rto_props[i].getNumMassIdx()) {
                        rto_props[i].setNumMassIdx(filtered_mass_props.get(y).getMassIdx());
                    } else {
                        int num_index = getClosestMassIndices(filtered_mass_props.get(y).getMassValue(), 0.49);
                        rto_props[i].setNumMassIdx(num_index);
                    }
                    break;
                }
            }
            for (int y = 0; y < filtered_mass_props.size(); y++) {
                if (massesEqualityCheck(filtered_mass_props.get(y).getMassValue(), rto_props[i].getDenMassValue(), 0.49)) {
                    isExist_den = true;
                    rto_props[i].setDenMassValue(filtered_mass_props.get(y).getMassValue());

                    if (filtered_mass_props.get(y).getMassIdx() == rto_props[i].getDenMassIdx()) {
                        rto_props[i].setDenMassIdx(filtered_mass_props.get(y).getMassIdx());
                    } else {
                        int den_index = getClosestMassIndices(filtered_mass_props.get(y).getMassValue(), 0.49);
                        rto_props[i].setDenMassIdx(den_index);
                    }
                    break;
                }
            }
           
            if (isExist_num && isExist_den) {
                if (!same_size) {
                    rto_props[i].setMag(1.0);
                }

                rto_props[i].setXWindowLocation(rto_props[i].getXWindowLocation() + 9); // DJ: to avoid images shiffting LEFT
                rto_props[i].setYWindowLocation(rto_props[i].getYWindowLocation() + 6); // DJ: to avoid images shiffting UP

                mp = new MimsPlus(this, rto_props[i]);
                mp.showWindow();
                mp.setDisplayRange(rto_props[i].getMinLUT(), rto_props[i].getMaxLUT());
            }
        }

        // Generate hsi images. DJ: 07/28/2014
        for (int i = 0; i < hsi_props.length; i++) {

            boolean isExist_num = false, isExist_den = false;

            for (int y = 0; y < filtered_mass_props.size(); y++) {

                if (massesEqualityCheck(filtered_mass_props.get(y).getMassValue(), hsi_props[i].getNumMassValue(), 0.49)) {
                    isExist_num = true;
                    hsi_props[i].setNumMassValue(filtered_mass_props.get(y).getMassValue());

                    if (filtered_mass_props.get(y).getMassIdx() == hsi_props[i].getNumMassIdx()) {
                        hsi_props[i].setNumMassIdx(filtered_mass_props.get(y).getMassIdx());
                    } else {
                        int num_index = getClosestMassIndices(filtered_mass_props.get(y).getMassValue(), 0.49);
                        hsi_props[i].setNumMassIdx(num_index);
                    }
                    if (i == 0)
                        break;    
                }
            }
            for (int y = 0; y < filtered_mass_props.size(); y++) {

                if (massesEqualityCheck(filtered_mass_props.get(y).getMassValue(), hsi_props[i].getDenMassValue(), 0.49)) {
                    isExist_den = true;
                    hsi_props[i].setDenMassValue(filtered_mass_props.get(y).getMassValue());

                    if (filtered_mass_props.get(y).getMassIdx() == hsi_props[i].getDenMassIdx()) {
                        hsi_props[i].setDenMassIdx(filtered_mass_props.get(y).getMassIdx());
                    } else {
                        int den_index = getClosestMassIndices(filtered_mass_props.get(y).getMassValue(), 0.49);
                        hsi_props[i].setDenMassIdx(den_index);
                    }
                    if (i == 0)
                        break;
                }
            }

            if (isExist_num && isExist_den) {
                if (!same_size) {
                    hsi_props[i].setMag(1.0);
                }

                hsi_props[i].setXWindowLocation(hsi_props[i].getXWindowLocation() + 9); // DJ: to avoid images shiffting LEFT
                hsi_props[i].setYWindowLocation(hsi_props[i].getYWindowLocation() + 6); // DJ: to avoid images shiffting UP

                mp = new MimsPlus(this, hsi_props[i]);
                mp.showWindow();
            }
        }

        // Generate sum images. DJ: 07/28/2014
        for (int i = 0; i < sum_props.length; i++) {

            if (sum_props[i].getSumType() == SumProps.RATIO_IMAGE) {

                boolean isExist_num = false, isExist_den = false;

                for (int y = 0; y < filtered_mass_props.size(); y++) {

                    if (massesEqualityCheck(filtered_mass_props.get(y).getMassValue(), sum_props[i].getNumMassValue(), 0.49)) {

                        isExist_num = true;
                        sum_props[i].setNumMassValue(filtered_mass_props.get(y).getMassValue());

                        if (filtered_mass_props.get(y).getMassIdx() == sum_props[i].getNumMassIdx()) {
                            sum_props[i].setNumMassIdx(filtered_mass_props.get(y).getMassIdx());
                        } else {
                            int num_index = getClosestMassIndices(filtered_mass_props.get(y).getMassValue(), 0.49);
                            sum_props[i].setDenMassIdx(num_index);

                        }
                        break;
                    }
                }
                for (int y = 0; y < filtered_mass_props.size(); y++) {
                    if (massesEqualityCheck(filtered_mass_props.get(y).getMassValue(), sum_props[i].getDenMassValue(), 0.49)) {

                        isExist_den = true;
                        sum_props[i].setDenMassValue(filtered_mass_props.get(y).getMassValue());

                        if (filtered_mass_props.get(y).getMassIdx() == sum_props[i].getDenMassIdx()) {
                            sum_props[i].setDenMassIdx(filtered_mass_props.get(y).getMassIdx());
                        } else {
                            int den_index = getClosestMassIndices(filtered_mass_props.get(y).getMassValue(), 0.49);
                            sum_props[i].setDenMassIdx(den_index);
                        }
                        break;
                    }
                }
                
                if (isExist_num && isExist_den) {
                    if (!same_size) {
                        sum_props[i].setMag(1.0);
                    }

                    sum_props[i].setXWindowLocation(sum_props[i].getXWindowLocation() + 10); // DJ: to avoid images shiffting LEFT
                    sum_props[i].setYWindowLocation(sum_props[i].getYWindowLocation() + 6); // DJ: to avoid images shiffting UP

                    mp = new MimsPlus(this, sum_props[i], null);
                    mp.showWindow();
                    mp.setDisplayRange(sum_props[i].getMinLUT(), sum_props[i].getMaxLUT());
                }

            } else if (sum_props[i].getSumType() == SumProps.MASS_IMAGE) {

                for (int z = 0; z < filtered_mass_props.size(); z++) {
                    if (massesEqualityCheck(filtered_mass_props.get(z).getMassValue(), sum_props[i].getParentMassValue(), 0.49)) {

                        sum_props[i].setParentMassValue(filtered_mass_props.get(z).getMassValue());

                        if (filtered_mass_props.get(z).getMassIdx() == sum_props[i].getParentMassIdx()) {
                            sum_props[i].setParentMassIdx(filtered_mass_props.get(z).getMassIdx());

                            if (!same_size) {
                                sum_props[i].setMag(1.0);
                            }
                            sum_props[i].setXWindowLocation(sum_props[i].getXWindowLocation() + 10); // DJ: to avoid images shiffting LEFT
                            sum_props[i].setYWindowLocation(sum_props[i].getYWindowLocation() + 6);  // DJ: to avoid images shiffting UP

                            mp = new MimsPlus(this, sum_props[i], null);
                            mp.showWindow();
                            mp.setDisplayRange(sum_props[i].getMinLUT(), sum_props[i].getMaxLUT());

                            break;
                            
                        } else if (filtered_mass_props.get(z).getMassIdx() != sum_props[i].getParentMassIdx()) {
                            int index = getClosestMassIndices(filtered_mass_props.get(z).getMassValue(), 0.49);
                            sum_props[i].setParentMassIdx(index);
                            sum_props[i].setXWindowLocation(sum_props[i].getXWindowLocation() + 10); // DJ: to avoid images shiffting LEFT
                            sum_props[i].setYWindowLocation(sum_props[i].getYWindowLocation() + 6);  // DJ: to avoid images shiffting UP
                            mp = new MimsPlus(this, sum_props[i], null);
                            mp.showWindow();
                            mp.setDisplayRange(sum_props[i].getMinLUT(), sum_props[i].getMaxLUT());
                            break;
                        }
                    }
                }
            }
        }


        //  DJ: 08/01/2014.
        // Generate composite images.
        for (int z = 0; z < composite_props.length; z++) {

            Object[] channels = new Object[4]; // 4 = four channels : RGBG : Red, Green, Blue, Gray.

            MimsPlus[] imgs = composite_props[z].getImages(ui);
            Object[] imgs_props = composite_props[z].getImageProps();

            boolean atLeastOneChannelExists = false;

            for (int i = 0; i < imgs.length; i++) {

                if (imgs[i] != null) {

                    if (imgs[i].getMimsType() == MimsPlus.RATIO_IMAGE) {
                        for (int y = 0; y < rto_props.length; y++) {
                            if (imgs[i].ratioProps.equalsThruMassValues(rto_props[y])) {
                                atLeastOneChannelExists = true;
                                channels[i] = imgs_props[i];
                            }
                        }
                    }

                    if (imgs[i].getMimsType() == MimsPlus.SUM_IMAGE) {
                        for (int y = 0; y < sum_props.length; y++) {
                            if (imgs[i].sumProps.equalsThruMassValues(sum_props[y])) {
                                atLeastOneChannelExists = true;
                                channels[i] = imgs_props[i];
                            }
                        }
                    }

                    if (imgs[i].getMimsType() == MimsPlus.MASS_IMAGE) {
                        for (int y = 0; y < filtered_mass_props.size(); y++) {
                            if (massesEqualityCheck(imgs[i].getMassValue(), filtered_mass_props.get(y).getMassValue(), 0.49)) {
                                atLeastOneChannelExists = true;
                                channels[i] = imgs_props[i];
                            }
                        }
                    }

                } // end of null check
            } // end of imgs LOOP

            if (atLeastOneChannelExists == true) {

                composite_props[z].setImageProps(channels); 
                composite_props[z].setXWindowLocation(composite_props[z].getXWindowLocation() + 10); // DJ: the "+10" to avoid images shifting LEFT
                composite_props[z].setYWindowLocation(composite_props[z].getYWindowLocation() + 6);  // DJ: the "+6" to avoid images shifting UP

                mp = new MimsPlus(this, composite_props[z]);
                mp.showWindow();

            }
        } // end of composite_props LOOP
        
        resetViewMenu(); //DJ: 08/18/2014
        getRoiManager().setVisible(roiManagerVisible);
        
        if(!usedForTables) {
            hsiControl.updateImage(true);  // removes old entries (but puts new ones back in
            // so when re-opening a file, extra entries get added by the next call.  Damn.
            //hsiControl.addShownRatiosToList(rto_props, hsi_props); //DJ: 08/14/2014
        }
    
    }
    
    public void setUsedForTable(boolean b){
        usedForTables = b;
    }

    /**
     * Determines the default name to assign ratio, HSi and sum images when
     * being saved. Generally they will all have the same prefix name followed
     * by some detail about the image along with an extension that corresponds
     * to the type of image.
     * <p>
     * For example, if the current open file is called test_file.nrrd and the
     * user wished to save all derived images, the following are possible names
     * of the saved images depending on its type:
     * <ul>
     * <li>test_file_m13.sum
     * <li>test_file_m13_m12.sum
     * <li>test_file_m13_m12.ratio
     * <li>test_file_m13_m12.hsi
     * </ul>
     *
     * @param img the image to be saved.
     * @return the default name of the file.
     */
    String getExportName(MimsPlus img) {
        String name = "";
        name += this.getImageFilePrefix();

        if (img.getMimsType() == MimsPlus.MASS_IMAGE) {
            int index = img.getMassIndex();
            int mass = Math.round(new Float(getOpener().getMassNames()[index]));
            name += "_m" + mass;
            return name;
        }

        if (img.getMimsType() == MimsPlus.RATIO_IMAGE) {
            RatioProps ratioprops = img.getRatioProps();
            int numIndex = ratioprops.getNumMassIdx();
            int denIndex = ratioprops.getDenMassIdx();
            int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
            int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
            name += "_m" + numMass + "_m" + denMass + "_ratio";
            return name;
        }

        if (img.getMimsType() == MimsPlus.HSI_IMAGE) {
            HSIProps hsiprops = img.getHSIProps();

            int numIndex = hsiprops.getNumMassIdx();
            int denIndex = hsiprops.getDenMassIdx();
            int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
            int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
            name += "_m" + numMass + "_m" + denMass + "_hsi";
            return name;
        }

        if (img.getMimsType() == MimsPlus.SUM_IMAGE) {
            SumProps sumProps = img.getSumProps();
            if (sumProps.getSumType() == SumProps.RATIO_IMAGE) {
                int numIndex = sumProps.getNumMassIdx();
                int denIndex = sumProps.getDenMassIdx();
                int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
                int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
                name += "_m" + numMass + "_m" + denMass + "_sum";
                return name;
            } else if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
                int parentIndex = sumProps.getParentMassIdx();
                int parentMass = Math.round(new Float(getOpener().getMassNames()[parentIndex]));
                name += "_m" + parentMass + "_sum";
                return name;
            }

        }

        if (img.getMimsType() == MimsPlus.SEG_IMAGE) {
            name += "_seg";
            return name;
        }

        if (img.getMimsType() == img.COMPOSITE_IMAGE) {
            name += "_comp";
            return name;
        }

        return name;
    }

    /**
     * Action method for changing tabs. Im not sure but I dont think this should
     * be needed. Possible future delete.
     */
    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
        //if (this.mimsTomography != null) {
        //    this.mimsTomography.resetImageNamesList();
        //}
        //if (this.mimsStackEditing != null) {
        //    this.mimsStackEditing.resetTrueIndexLabel();
        //    this.mimsStackEditing.resetSpinners();
        //}
    }//GEN-LAST:event_jTabbedPane1StateChanged

    /**
     * Action method File>Exit menu item. Closes the application.
     */
private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
    WindowEvent wev = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
    Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
}//GEN-LAST:event_exitMenuItemActionPerformed

    /**
     * Action method for Utilities>Sum all Open menu item. Generates sum images
     * for all open mass and ration images.
     */
private void sumAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sumAllMenuItemActionPerformed

    SumProps sumProps;
    MimsPlus[] openmass = this.getOpenMassImages();
    MimsPlus[] openratio = this.getOpenRatioImages();

    //clear all sum images
    for (int i = 0; i < maxMasses * 2; i++) {
        if (sumImages[i] != null) {
            sumImages[i].close();
            sumImages[i] = null;
        }
    }

    // Open a sum image for each mass image.
    for (int i = 0; i < openmass.length; i++) {
        try {
            sumProps = new SumProps(openmass[i].getMassIndex());
            sumProps.setXWindowLocation(openmass[i].getWindow().getLocationOnScreen().x + MimsPlus.X_OFFSET);
            sumProps.setYWindowLocation(openmass[i].getWindow().getLocationOnScreen().y + MimsPlus.Y_OFFSET);
            MimsPlus mp = new MimsPlus(this, sumProps, null);
            mp.showWindow();
        } catch (NullPointerException npe) {
            // Most likely the user tried to close a mass image hitting the X button
        }
    }

    // open a sum image for each ratio image.
    for (int i = 0; i < openratio.length; i++) {
        sumProps = new SumProps(openratio[i].getRatioProps().getNumMassIdx(), openratio[i].getRatioProps().getDenMassIdx());
        sumProps.setRatioScaleFactor(openratio[i].getRatioProps().getRatioScaleFactor());
        sumProps.setXWindowLocation(openratio[i].getWindow().getLocationOnScreen().x + MimsPlus.X_OFFSET);
        sumProps.setYWindowLocation(openratio[i].getWindow().getLocationOnScreen().y + MimsPlus.Y_OFFSET);
        MimsPlus mp = new MimsPlus(this, sumProps, null);
        mp.showWindow();
    }
}//GEN-LAST:event_sumAllMenuItemActionPerformed

    /**
     * Action method for File>About menu item. Displays basic information about
     * the Open Mims plugins.
     */
private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed

    String message = "OpenMIMS v" + mimsVersion + ", Jan 29, 2018 (rev: " + revisionNumber + ")";
    message += "\n\n";
    message += "http://www.nrims.hms.harvard.edu/";
    message += "\n\n";
    message += "OpenMIMS is a plugin for ImageJ, a public domain, Java-based\n";
    message += "image processing program developed at the NIH by Wayne Rasband.";
    message += "\n\n";
    message += "OpenMIMS is an open source software project that is funded through \n";
    message += "the NIH/NIBIB National Resource for Imaging Mass Spectrometry. \n";
    message += "Please use the following acknowledgment and send us references to \n";
    message += "any publications, presentations, or successful funding applications \n";
    message += "that make use of OpenMIMS software:";
    message += "\n\n";
    message += "    \"This work was made possible in part by the OpenMIMS software \n";
    message += "    whose development is funded by the NIH/NIBIB National Resource \n";
    message += "    for Imaging Mass Spectrometry, NIH/NIBIB 5P41 EB001974-10.\"";
    message += "\n\n";
    message += "Concept by:\n   Claude Lechene and Doug Benson (RIP)\n\n";
    
    message += "Developed by:\n   Doug Benson (RIP)\n";
    message += "   Collin Poczatek, Boris Epstein, Philipp Gormanns, Stefan Reckow,\n";
    message += "   Zeke Kaufman, Farah Kashem, William Ang, ";
    message += "Djamel-Eddine Sia,\n   and Walter Taylor.";
    message += "\n\n";
    message += "OpenMIMS has modified, uses, or depends upon: \n";
    message += "    ImageJ: http://rsbweb.nih.gov/ij/\n";
    message += "    Fiji: http://fiji.sc/Fiji\n";
    message += "    TurboReg:  http://bigwww.epfl.ch/thevenaz/turboreg/ \n";
    message += "    libSVM: http://www.csie.ntu.edu.tw/~cjlin/libsvm/ \n";
    message += "    NRRD file format: http://teem.sourceforge.net/nrrd/ \n";
    message += "    nrrd plugins: http://flybrain.stanford.edu/nrrd \n";
    message += "    jFreeChart:  http://www.jfree.org/jfreechart/ \n";
    message += "    FileDrop:  http://iharder.sourceforge.net/current/java/filedrop/ \n";
    message += "    Apache Commons: http://commons.apache.org/io/ \n";
    message += "    jRTF:  http://code.google.com/p/jrtf/ \n";
    message += "    jUnique: http://www.sauronsoftware.it/projects/junique/ \n";
    message += "\n\n";
    message += "Please cite OpenMIMS or any of the above projects when applicable.";

    javax.swing.JFrame frame = new javax.swing.JFrame("About OpenMIMS");
    frame.setSize(500, 700);

    javax.swing.JScrollPane scroll = new javax.swing.JScrollPane();
    frame.add(scroll);
    javax.swing.JTextArea area = new javax.swing.JTextArea();
    area.setEditable(false);
    area.append(message);

    area.setColumns(20);
    area.setRows(5);
    //this sets vert-scrollbar to top, calls to vert-scrollbar don't work
    area.setCaretPosition(0);
    
    scroll.setViewportView(area);
    int x = java.awt.MouseInfo.getPointerInfo().getLocation().x;
    int y = java.awt.MouseInfo.getPointerInfo().getLocation().y;
    frame.setLocation(x, y);
    frame.setVisible(true);

}//GEN-LAST:event_aboutMenuItemActionPerformed

    /**
     * Action method for Utilities>Capture Current Image menu item. Generates a
     * .png screen capture for the most recently clicked image.
     */
private void captureImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureImageMenuItemActionPerformed

    // Captures the active image window and returns it as an ImagePlus.
    MimsPlus imp = (MimsPlus) ij.WindowManager.getCurrentImage();
    if (imp == null) {
        IJ.noImage();
        return;
    }
     // If the image has been enlarged, the capture puts too many ROIs on the captured file, in 
     // some cases, and also in the wrong location.
     // A quick fix is to set the image size back to the  default size before the capture occurs.
    imp.restoreMag();

    Image img = getScreenCaptureCurrentImage();
    if (img == null) {
        return;
    }
    
    // Bring up JFileChooser and get file name.
    File file;
    MimsJFileChooser fc = new MimsJFileChooser(this);
    if (this.getImageFilePrefix() != null) {
        fc.setSelectedFile(new File(getExportName(imp) + ".png"));
    }
    MIMSFileFilter mff_png = new MIMSFileFilter("png");
    mff_png.setDescription("Snapshot image");
    fc.addChoosableFileFilter(mff_png);
    fc.setFileFilter(mff_png);
    int returnVal = fc.showSaveDialog(jTabbedPane1);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
        String fileName = fc.getSelectedFile().getAbsolutePath();
        file = new File(fileName);
    } else {
        return;
    }

    // Save file.
    ImageCanvas imageCanvas = massImages[0].getCanvas();
    Graphics g = imageCanvas.getGraphics();
    try {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ImagePlus imp2 = new ImagePlus(file.getName(), img);
        
        // Get ROIs from current image and apply to this MimPlus instance (imp)
        MimsRoiManager2 roiManager = ui.getRoiManager();
        Hashtable rois = roiManager.getROIs();
        javax.swing.JList list = roiManager.getList();
        //massImages[0].getRoi();
        Overlay overlay = new Overlay();
        // loop over the list of rois and add each to the overlay
        
        // Most of the rest of this code in this method was copied from the drawOverlay method of 
        // MimsCanvas.java, with some modification.
        int parentplane = 1;
        try {
            if (imp.getMimsType() == MimsPlus.MASS_IMAGE) {
                parentplane = imp.getCurrentSlice();
            } else if (imp.getMimsType() == MimsPlus.RATIO_IMAGE) {
                parentplane = ui.getMassImages()[imp.getRatioProps().getNumMassIdx()].getCurrentSlice();
            } else if (imp.getMimsType() == MimsPlus.HSI_IMAGE) {
                parentplane = ui.getMassImages()[imp.getHSIProps().getNumMassIdx()].getCurrentSlice();
            } else if (imp.getMimsType() == MimsPlus.SUM_IMAGE) {
                if (imp.getSumProps().getSumType() == SumProps.MASS_IMAGE) {
                    parentplane = ui.getMassImages()[imp.getSumProps().getParentMassIdx()].getCurrentSlice();
                } else if (imp.getSumProps().getSumType() == SumProps.RATIO_IMAGE) {
                    parentplane = ui.getMassImages()[imp.getSumProps().getNumMassIdx()].getCurrentSlice();
                }

            } // DJ: 08/06/2014 Added to handle composite images
            else if (imp.getMimsType() == MimsPlus.COMPOSITE_IMAGE) {
                for (int i = 0; i < imp.getCompositeProps().getImages(ui).length; i++) {

                    MimsPlus channel = (imp.getCompositeProps().getImages(ui))[i];

                    if (channel != null) {

                        if (channel.getMimsType() == MimsPlus.MASS_IMAGE) {
                            parentplane = channel.getCurrentSlice();
                            break;
                        } else if (channel.getMimsType() == MimsPlus.RATIO_IMAGE) {
                            parentplane = ui.getMassImages()[channel.getRatioProps().getNumMassIdx()].getCurrentSlice();
                            break;
                        } else if (channel.getMimsType() == MimsPlus.SUM_IMAGE) {
                            if (channel.getSumProps().getSumType() == SumProps.MASS_IMAGE) {
                                parentplane = ui.getMassImages()[channel.getSumProps().getParentMassIdx()].getCurrentSlice();
                                break;
                            } else if (channel.getSumProps().getSumType() == SumProps.RATIO_IMAGE) {
                                parentplane = ui.getMassImages()[channel.getSumProps().getNumMassIdx()].getCurrentSlice();
                                break;
                            }
                        }
                    }
                }
            }
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            // Do nothing, assume plane 1.
        }      
         // loop over the list of rois and add each to the overlay
        for (int id = 0; id < list.getModel().getSize(); id++) {
            String label = (list.getModel().getElementAt(id).toString());
            Roi roi = (Roi) rois.get(label);
                       
            roi.setImage(imp);
            Integer[] xy = roiManager.getRoiLocation(label, parentplane);
            if (xy != null && roi.getType() != Roi.LINE) {
                roi.setLocation(xy[0], xy[1]);
            }

            Color color;
            if (roiManager.isSelected(label)) {
                color = Color.GREEN;
            } else if (imp.getMimsType() == MimsPlus.HSI_IMAGE || 
                    imp.getMimsType() == MimsPlus.COMPOSITE_IMAGE || 
                    imp.getMimsType() == MimsPlus.SEG_IMAGE) {
                color = Color.WHITE;
            } else {
                color = Color.RED;              
            }
            roi.setStrokeColor(color);
            g.setColor(color);
            overlay.setLabelColor(color);
            
            overlay.setLabelFont(roiManager.getFont());

            // Draw the Label.
            String name = label;
            Rectangle r = roi.getBounds();              
            int x = imageCanvas.screenX(r.x + r.width / 2);  // a method of ImageCanvas.java
            int y = imageCanvas.screenY(r.y + r.height / 2);
            g.drawString(name, x, y);
         
            // Draw the Roi.
            roi.setStrokeWidth(0.0);
            roi.drawOverlay(g);
            overlay.add(roi);
        }
        // This is a bandaid, and a ratty one at that.   For some unknown reason, calling 
        // overlay.drawLabels(true) on Linux causes the ROI number label to be displayed twice.
        if (operatingSystem.compareToIgnoreCase("MacOS") == 0) {
            overlay.drawLabels(true);
        }
        //overlay.drawNames(true);                     
        imp2.setOverlay(overlay);
        
        FileSaver saver = new ij.io.FileSaver(imp2);
        saver.saveAsPng(file.getAbsolutePath());
        
    } catch (Exception e) {
        ij.IJ.error("Save Error", "Error saving file:" + e.getMessage());
        e.printStackTrace();
    } finally {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
}//GEN-LAST:event_captureImageMenuItemActionPerformed

    /**
     * Gets a screen capture for the current image.
     *
     * @return the AWT Image.
     */
    public Image getScreenCaptureCurrentImage() {
        MimsPlus imp = (MimsPlus) ij.WindowManager.getCurrentImage();
        final ImageWindow win = imp.getWindow();
        if (win == null) {
            return null;
        }
        //win.setVisible(false);
        //win.setVisible(true);
        //win.repaint();
        //win.toFront();
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        //win.toFront();
        Point loc = win.getLocation();
        ImageCanvas ic = win.getCanvas();
        ic.update(ic.getGraphics());

        Rectangle bounds = ic.getBounds();
        loc.x += bounds.x;
        loc.y += bounds.y;
        System.out.println("Printing at " + loc.x + ", " + loc.y);
        Rectangle r = new Rectangle(loc.x, loc.y, bounds.width, bounds.height);
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            IJ.error("Unable to capture image");
            return null;
        }
        robot.delay(100);
        Image img = robot.createScreenCapture(r);

        return img;
    }

    /**
     * Action method for the Utilities>Import .im List menu item. Loads a list
     * of images contained in a text file and concatenates them.
     */
private void importIMListMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importIMListMenuItemActionPerformed
    com.nrims.data.LoadImageList testLoad = new com.nrims.data.LoadImageList(this);
    boolean read;
    read = testLoad.openList();
    if (!read) {
        return;
    }

    testLoad.printList();
    testLoad.simpleIMImport();
}//GEN-LAST:event_importIMListMenuItemActionPerformed

    /**
     * Action method for the Utilities>Generate Stack menu item. Turns a ratio
     * or HSI image into a stack.
     */
private void genStackMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_genStackMenuItemActionPerformed

    MimsPlus img;
    //grab current window and try to cast
    try {
        img = (MimsPlus) ij.WindowManager.getCurrentImage();
    } catch (ClassCastException e) {
        //if it's some random image and we can't cast just return
        return;
    }

    ImageDataUtilities.generateStack(img, this);

}//GEN-LAST:event_genStackMenuItemActionPerformed

    /**
     * Action method Utilities>Test menu item. Reserved for testing code.
     */
    /**
     * Action method for File>Save Image and File>Save Session menu items.
     * Brings up a JFileChooser for saving the current image (or session).
     */
private void saveMIMSjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMIMSjMenuItemActionPerformed
    saveMIMS(evt);
    //TODO should add check if return false show error message
}//GEN-LAST:event_saveMIMSjMenuItemActionPerformed

    /**
     * Called by the saveMIMSjMenuItemActionPerformed method and can be used to
     * programmatically save the current image. Brings up a file chooser.
     */
    private boolean saveMIMS(java.awt.event.ActionEvent evt) {
        String fileName;
        try {
            // User sets file prefix name
            boolean saveImageOnly = true;
            boolean success = false;
            //Save only image, enter save from gui because evt != null
            if ((evt != null && evt.getActionCommand().equals(SAVE_IMAGE)) || evt == null) {
                saveImageOnly = true;
                System.out.println("Save image only.");
                MimsJFileChooser fc = new MimsJFileChooser(this);
                //Change missing from svn->git migration
                if (this.getImageFilePrefix() != null) {
                        fc.setSelectedFile(new java.io.File(this.getImageFilePrefix() + NRRD_EXTENSION));
                }
                //end
                int returnVal = fc.showSaveDialog(jTabbedPane1);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    fileName = fc.getSelectedFile().getAbsolutePath();
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    success = saveSession(fileName, saveImageOnly);
                    // DJ: 08/28/2014
                    ui.setTitle("OpenMIMS: " + image.getImageFile().getName().toString());
                   
                }

                //Save session, enter save from gui because evt != null
            } else if (evt != null && evt.getActionCommand().equals(SAVE_SESSION)) {
                saveImageOnly = false;
                System.out.println("Save session.");
                if (this.getMimsAction().isImageModified() || imgNotes.isModified()) {
                    MimsJFileChooser fc = new MimsJFileChooser(this);
                    if (this.getImageFilePrefix() != null) {
                        fc.setSelectedFile(new java.io.File(this.getImageFilePrefix() + NRRD_EXTENSION));
                    }

                    int returnVal = fc.showSaveDialog(jTabbedPane1);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        fileName = fc.getSelectedFile().getAbsolutePath();
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                        success = saveSession(fileName, saveImageOnly);
                        int i=1;
                    }
                } else {
                    String imageFileName = this.getImageFilePrefix();
                    String imageDir = this.getImageDir();
                    //String lastFolder = this.getLastFolder();                   
                    //fileName = new java.io.File(this.getImageFilePrefix() + NRRD_EXTENSION).getAbsolutePath();
                    String theFile = lastFolder + "/" + imageFileName + NRRD_EXTENSION;
                    fileName = new java.io.File(theFile).getAbsolutePath();
                    
                    success = saveSession(fileName, saveImageOnly);
                    int i=2;
                }
                // DJ: 08/28/2014
                ui.setTitle("OpenMIMS: " + image.getImageFile().getName().toString());
                
                //saveMIMS(null) called
                //only called from checkCurrentFileStatusBeforeOpening()
            }

            return success;
        } catch (Exception e) {
            if (!silentMode) {
                ij.IJ.error("Save Error", "Error saving file:" + e.getMessage());
            } else {
                e.printStackTrace();
            }
            return false;
        } finally {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Action method for the Utilities>Close>Close All Ratio Images. Closes all
     * ratio images.
     */
private void closeAllRatioMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllRatioMenuItemActionPerformed

    for (int i = 0; i < ratioImages.length; i++) {
        if (ratioImages[i] != null) {
            ratioImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllRatioMenuItemActionPerformed

    /**
     * Action method for the Utilities>Close>Close All HSI Images. Closes all
     * HSI images.
     */
private void closeAllHSIMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllHSIMenuItemActionPerformed

    for (int i = 0; i < hsiImages.length; i++) {
        if (hsiImages[i] != null) {
            hsiImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllHSIMenuItemActionPerformed

    /**
     * Action method for the Utilities>Close>Close All Sum Images. Closes all
     * sum images.
     */
private void closeAllSumMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllSumMenuItemActionPerformed

    for (int i = 0; i < sumImages.length; i++) {
        if (sumImages[i] != null) {
            sumImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllSumMenuItemActionPerformed
    //DJ: 10/22/2014
    public void exportCurrentlySelectedToPNG(){
        
        File file = image.getImageFile();
        System.out.println(file.getParent() + File.separator);
        String dir = file.getParent() + File.separator;
        
        MimsPlus selectedImage = (MimsPlus) ij.WindowManager.getCurrentImage();
        
        ij.io.FileSaver saver = new ij.io.FileSaver(ij.WindowManager.getCurrentImage());
        
        if(selectedImage.getMimsType() == MimsPlus.MASS_IMAGE){
            selectedImage.getWindow().toFront();
            Image screenShot =  ui.getScreenCaptureCurrentImage();
            saver = new ij.io.FileSaver(new ImagePlus(selectedImage.title, screenShot));
        }
        
        String name = getExportName(selectedImage) + ".png";
        File saveName = new File(dir + name);
        if (saveName.exists()) {
            for (int j = 1; j < 1000; j++) {
                name = getExportName(selectedImage) + "_" + j + ".png";
                saveName = new File(dir + name);
                if (!saveName.exists()) {
                    break;
                }
            }
        }
        String msg = "Folder Writting Permission Denied.\n Choose Another Folder ?";
        File folder = com.nrims.data.FileUtilities.checkWritePermissions(saveName, ui, msg);

        if(folder != null && folder.exists()){
          File fileToSave = new File(folder.getAbsolutePath() + "/" + name );
            saver.saveAsPng(fileToSave.getAbsolutePath());
        }
        
    }
    /**
     * Exports all open sum images, HSI images, ratio images and composite
     * images as .png files.
     */
    public void exportPNGs() {
        
        String msg = "Folder Writting Permission Denied.\n Choose Another Folder ?";
        
        File file = image.getImageFile();
        System.out.println(file.getParent() + File.separator);
        String dir = file.getParent() + File.separator;

        // DJ: 08/27/2014 TESTing
        MimsPlus[] mass = getOpenMassImages();
        for (int i = 0; i < mass.length; i++) {
            //ImagePlus img = (ImagePlus) mass[i];
            ij.WindowManager.setCurrentWindow(mass[i].getWindow());
            //mass[i].setActivated();
            mass[i].getWindow().toFront();
            Image screenShot =  ui.getScreenCaptureCurrentImage();
            ij.io.FileSaver saver = new ij.io.FileSaver(new ImagePlus(mass[i].title, screenShot));
            String name = getExportName(mass[i]) + ".png";
            File saveName = new File(dir + name);
            if (saveName.exists()) {
                for (int j = 1; j < 1000; j++) {
                    name = getExportName(mass[i]) + "_" + j + ".png";
                    saveName = new File(dir + name);
                    if (!saveName.exists()) {
                        break;
                    }
                }
            }
            
            // DJ: 10/24/2014
            File folder = com.nrims.data.FileUtilities.checkWritePermissions(saveName, ui, msg);
            if (folder != null && folder.exists()) {
                File fileToSave = new File(folder.getAbsolutePath() + "/" + name);
                saver.saveAsPng(fileToSave.getAbsolutePath());
            }else
                return;

        }    
        
        MimsPlus[] sum = getOpenSumImages();
        for (int i = 0; i < sum.length; i++) {
            ImagePlus img = (ImagePlus) sum[i];
            ij.io.FileSaver saver = new ij.io.FileSaver(img);
            String name = getExportName(sum[i]) + ".png";
            File saveName = new File(dir + name);
            if (saveName.exists()) {
                for (int j = 1; j < 1000; j++) {
                    name = getExportName(sum[i]) + "_" + j + ".png";
                    saveName = new File(dir + name);
                    if (!saveName.exists()) {
                        break;
                    }
                }
            }
            // DJ: 10/24/2014
            File folder = com.nrims.data.FileUtilities.checkWritePermissions(saveName, ui, msg);
            if (folder != null && folder.exists()) {
                File fileToSave = new File(folder.getAbsolutePath() + "/" + name);
                saver.saveAsPng(fileToSave.getAbsolutePath());
            } else
                return;
        }

        MimsPlus[] hsi = getOpenHSIImages();
        for (int i = 0; i < hsi.length; i++) {
            ImagePlus img = (ImagePlus) hsi[i];
            ij.io.FileSaver saver = new ij.io.FileSaver(img);
            String name = getExportName(hsi[i]) + ".png";
            File saveName = new File(dir + name);
            if (saveName.exists()) {
                for (int j = 1; j < 1000; j++) {
                    name = getExportName(hsi[i]) + "_" + j + ".png";
                    saveName = new File(dir + name);
                    if (!saveName.exists()) {
                        break;
                    }
                }
            }
            // DJ: 10/24/2014
            File folder = com.nrims.data.FileUtilities.checkWritePermissions(saveName, ui, msg);
            if (folder != null && folder.exists()) {
                File fileToSave = new File(folder.getAbsolutePath() + "/" + name);
                saver.saveAsPng(fileToSave.getAbsolutePath());
            } else
                return;
        }

        MimsPlus[] ratios = getOpenRatioImages();
        for (int i = 0; i < ratios.length; i++) {
            ImagePlus img = (ImagePlus) ratios[i];
            ij.io.FileSaver saver = new ij.io.FileSaver(img);
            String name = getExportName(ratios[i]) + ".png";
            File saveName = new File(dir + name);
            if (saveName.exists()) {
                for (int j = 1; j < 1000; j++) {
                    name = getExportName(ratios[i]) + "_" + j + ".png";
                    saveName = new File(dir + name);
                    if (!saveName.exists()) {
                        break;
                    }
                }
            }
            // DJ: 10/24/2014
            File folder = com.nrims.data.FileUtilities.checkWritePermissions(saveName, ui, msg);
            if (folder != null && folder.exists()) {
                File fileToSave = new File(folder.getAbsolutePath() + "/" + name);
                saver.saveAsPng(fileToSave.getAbsolutePath());
            } else
                return;
        }


        MimsPlus[] comp = getOpenCompositeImages();
        for (int i = 0; i < comp.length; i++) {
            ImagePlus img = (ImagePlus) comp[i];
            ij.io.FileSaver saver = new ij.io.FileSaver(img);
            String name = getExportName(comp[i]) + ".png";
            File saveName = new File(dir + name);
            if (saveName.exists()) {
                for (int j = 1; j < 1000; j++) {
                    name = getExportName(comp[i]) + "_" + j + ".png";
                    saveName = new File(dir + name);
                    if (!saveName.exists()) {
                        break;
                    }
                }
            }
            // DJ: 10/24/2014
            File folder = com.nrims.data.FileUtilities.checkWritePermissions(saveName, ui, msg);
            if (folder != null && folder.exists()) {
                File fileToSave = new File(folder.getAbsolutePath() + "/" + name);
                saver.saveAsPng(fileToSave.getAbsolutePath());
            } else
                return;
        }

    }

    /**
     * Action method for Utilities>Export>Export All Derived. Exports all
     * derived images and .png's.
     */
private void exportPNGjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPNGjMenuItemActionPerformed
    exportPNGs();
}//GEN-LAST:event_exportPNGjMenuItemActionPerformed

    /**
     * Action method for Utilities>Image Notes. Opens a text area for adding
     * notes.
     */
private void imageNotesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageNotesMenuItemActionPerformed
    this.imgNotes.setVisible(true);
}//GEN-LAST:event_imageNotesMenuItemActionPerformed

    /**
     * Action method for Utilities>Composite menu item. Shows the compisite
     * manager interface.
     */
private void compositeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compositeMenuItemActionPerformed
    //DJ: 08/14/2014
    // to avoid openning a new composite manager 
    // while another one still exists.

    if(com.nrims.managers.CompositeManager.getInstance() == null || 
       com.nrims.managers.CompositeManager.getInstance().isVisible() == false)
        cbControl.showCompositeManager();
    else
        this.compManager.toFront();
}//GEN-LAST:event_compositeMenuItemActionPerformed

    /**
     * Action method for Utilities>Debug menu item.
     */
    /**
     * Not used. Unable to delete. Stupid netbeans.
     */
private void exportHSI_RGBAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportHSI_RGBAActionPerformed
    // Empty
}//GEN-LAST:event_exportHSI_RGBAActionPerformed

    /**
     * Action method for View>Roi Manager menu item. Display the Roi Manager.
     */
private void roiManagerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_roiManagerMenuItemActionPerformed
    getRoiManager().viewManager();
}//GEN-LAST:event_roiManagerMenuItemActionPerformed

private void generateReportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateReportMenuItemActionPerformed
    openReportGenerator();
}//GEN-LAST:event_generateReportMenuItemActionPerformed

    /*
     * Method for opening report generator. If user closed or cancelled old report dialog,
     * this will report the old one. 
     */
    public void openReportGenerator() {
        if (rg != null) {
            rg.setVisible(true);
            return;
        } else {
            rg = new ReportGenerator(this);
            rg.setVisible(true);
        }
    }

private void batch2nrrdMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_batch2nrrdMenuItemActionPerformed
    ConvertManager cm = new ConvertManager(this, false);
    cm.setVisible(true);
    cm.selectFiles();
}//GEN-LAST:event_batch2nrrdMenuItemActionPerformed

//DJ: 08/21/2014
/**
 * Gets all the File's props as a Vector.
 * @param fileNameAndPath as a <code>String</code>
 * @return <code>Vector</Code> of all the file's props.
 */
public Vector getFileSettings(String fileNameAndPath){
    return filesProps.get(fileNameAndPath);
}

public void removeFileFromFileProps(String fileName){
    if(filesProps == null || filesProps.size() == 0)
        return;
    filesProps.remove(fileName);
}
//DJ: 08/21/2014
/**
 * gets the name of the last file that was fully opened.
 * @return <code>String</code> of the last file that was fully opened.
 */
public String getNameOfLastFileOpened(){
    String[] allFilesPaths = filesProps.keySet().toArray(new String[filesProps.keySet().size()]);
    return allFilesPaths[allFilesPaths.length-1];
}

private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
    
    if(filesManager != null)
        filesManager.dispose();
    
    
    
    if (com.nrims.managers.CompositeManager.getInstance() != null)
         com.nrims.managers.CompositeManager.getInstance().closeWindow();
    
    task.cancel(true);

    // so the next file loaded won't have the props of the last file cancelled
    if (mimsData == null) {} 
    else if (filesProps != null) {
       String[] allFilesPaths = filesProps.keySet().toArray(new String[filesProps.keySet().size()]);
       FilesManager filesManager = new FilesManager(ui, allFilesPaths, true, nameOfFileNowOpened);
      // filesManager.addImageFiles(allFilesPaths);
       filesManager.setVisible(true);
    }

}//GEN-LAST:event_stopButtonActionPerformed

private void DTCorrectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DTCorrectionMenuItemActionPerformed
    if (image == null) {
        return;
    }

    int start_slice = getOpenMassImages()[0].getCurrentSlice();

    if (image.isDTCorrected()) {
        return;
    } else {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // User sets file prefix name
        MimsJFileChooser fc = new MimsJFileChooser(this);
        fc.setDialogTitle("Save Corrected File As...");
        if (this.getImageFilePrefix() != null) {
            fc.setSelectedFile(new java.io.File(this.getImageFilePrefix() + NRRD_EXTENSION));
        }
        int returnVal = fc.showSaveDialog(jTabbedPane1);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String fileName = fc.getSelectedFile().getAbsolutePath();
            jProgressBar1.setString("Applying dead time correction...");
            applyDeadTimeCorrection(fileName);
            DTCorrectionMenuItem.setEnabled(false);
            jProgressBar1.setString("Dead time correction complete.");
            getOpenMassImages()[0].setSlice(start_slice);
            updateAllImages();
            ij.IJ.showMessage("Done");
        } else {
            DTCorrectionMenuItem.setSelected(false);
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

    }
}//GEN-LAST:event_DTCorrectionMenuItemActionPerformed

private void QSACorrectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_QSACorrectionMenuItemActionPerformed
    if (qsam == null) {
        qsam = new QSAcorrectionManager(this);
    }
    qsam.setVisible(true);
}//GEN-LAST:event_QSACorrectionMenuItemActionPerformed

private void emptyTestMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_emptyTestMenuItemActionPerformed
    //empty?
    //String foo = "{{\\b DATE: }2012/11/08 12:45\\par}";
    //System.out.println("foo = "+foo);

    com.nrims.experimental.warp warp = new com.nrims.experimental.warp();
    warp.cellWarp(this);
}//GEN-LAST:event_emptyTestMenuItemActionPerformed

private void exportQVisMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportQVisMenuItemActionPerformed

    MimsPlus img;
    try {
        img = (MimsPlus) ij.WindowManager.getCurrentImage();
    } catch (Exception e) {
        IJ.error("Error: current image not a MIMS image.");
        return;
    }

    if (img.getMimsType() != MimsPlus.HSI_IMAGE) {
        IJ.error("Error: current image not an HSI image.");
        return;
    }

    String empty = "";
    ij.gui.GenericDialog gd = new ij.gui.GenericDialog("Alpha min/max in counts");
    gd.addStringField("Alpha min:", empty, 20);
    gd.addStringField("Alpha max:", empty, 20);
    gd.showDialog();
    if (gd.wasCanceled()) {
        return;
    }
    String minstr = gd.getNextString();
    String maxstr = gd.getNextString();


    int minA = 0;
    int maxA = 0;

    //Do paramter/error checking
    try {
        minA = Integer.parseInt(minstr);
        maxA = Integer.parseInt(maxstr);
    } catch (Exception e) {
        IJ.error("Error: alpha min/max not integers.");
        return;
    }

    //Query user for filename
    MimsJFileChooser fc = new MimsJFileChooser(this);
    fc.setSelectedFile(new File(this.getLastFolder(), this.getImageFilePrefix() + ".dat"));
    MIMSFileFilter filter = new MIMSFileFilter("dat");
    filter.setDescription("QVis file");
    fc.addChoosableFileFilter(filter);
    fc.setFileFilter(filter);

    int returnVal = fc.showSaveDialog(this);
    if (returnVal == MimsJFileChooser.CANCEL_OPTION) {
        return;
    }

    File outFile = fc.getSelectedFile();

    String fileName = fc.getSelectedFile().getName();
    if (!fileName.endsWith(".dat")) {
        fileName = fileName + ".dat";
    }
    outFile = new java.io.File(outFile.getParentFile(), fileName);

    int startplane = this.getOpenMassImages()[0].getSlice();
    boolean success = com.nrims.data.exportQVis.exportHSI_RGBA(this, img, minA, maxA, outFile);
    if (!success) {
        IJ.error("Error: writing file failed.");
    }
    this.getOpenMassImages()[0].setSlice(startplane);
}//GEN-LAST:event_exportQVisMenuItemActionPerformed

    private void openNextMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openNextMenuItemActionPerformed
        openNext();
    }//GEN-LAST:event_openNextMenuItemActionPerformed

    private void RecomputeAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RecomputeAllMenuItemActionPerformed
        recomputeAllImages();
    }//GEN-LAST:event_RecomputeAllMenuItemActionPerformed

    private void openNewWriterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openNewWriterActionPerformed
        UnoPlugin.newDoc();
    }//GEN-LAST:event_openNewWriterActionPerformed

    private void insertPicFrameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_insertPicFrameActionPerformed
                                                                                                                                                                                                              if (image != null) {
            UnoPlugin.insertEmptyOLEObject(image.getImageFile().getName(), getImageDir() + File.separator);
        } else {
            UnoPlugin.insertEmptyOLEObject("", "");
        }
    }//GEN-LAST:event_insertPicFrameActionPerformed

    private void openNewDrawActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openNewDrawActionPerformed
        UnoPlugin.newDraw();
    }//GEN-LAST:event_openNewDrawActionPerformed

    private void openNewImpressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openNewImpressActionPerformed
        UnoPlugin.newImpress();
    }//GEN-LAST:event_openNewImpressActionPerformed

    private void findMosaicActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findMosaicActionPerformed
        File file = FileUtilities.getMosaic(image.getImageFile());
        if (file != null) {
            FileUtilities.openInNewUI(file, ui);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Could not find mosaic which contains this file",
                    "File not found",
                    JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_findMosaicActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        ImageDataUtilities.centerMassAutoTrack(ui);
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItemOpenNonMIMSimageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenNonMIMSimageActionPerformed
        MimsJFileChooser fc = new MimsJFileChooser(this);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Non-MIMS Images", "png", "tif", "jpg");
        fc.setFileFilter(filter);
        fc.setMultiSelectionEnabled(false);
        int returnVal = fc.showOpenDialog(this);

        // Open file or return null.
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            ImagePlus tiff = IJ.openImage(file.getAbsolutePath());
            MimsPlus newImage = new MimsPlus(this, MimsPlus.NON_MIMS_IMAGE, tiff.getProcessor());          
            newImage.setProcessor(tiff.getProcessor());  // a ByteProcessor
            newImage.addListener(ui);
            newImage.setTitle(file.getName());           
            nonMIMSImages.add(newImage);
            tiff.close();
            newImage.show();
        }
    }//GEN-LAST:event_jMenuItemOpenNonMIMSimageActionPerformed

    private void findStackFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findStackFileActionPerformed
        File file = FileUtilities.getStack(image.getImageFile());
        if (file != null) {
            FileUtilities.openInNewUI(file, ui);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Could not find stack which contains this file",
                    "File not found",
                    JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_findStackFileActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        String[] allFilesPaths = new String[0];
        if (filesProps != null) 
            allFilesPaths = filesProps.keySet().toArray(new String[filesProps.keySet().size()]);
            
        if(filesManager == null)
            filesManager = new FilesManager(ui, allFilesPaths, false, nameOfFileNowOpened);
        
        FilesManager.getInstance().activate_disactivate_components();
        FilesManager.getInstance().showWindow();
        
        
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed

        htmlGenerator = new ConvertManager(this, true);
        htmlGenerator.setVisible(true);
        htmlGenerator.selectFiles();
        
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void exportjMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportjMenuActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_exportjMenuActionPerformed

    private void jMenu1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu1ActionPerformed
        // TODO add your handling code here:
        java.awt.Desktop desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;
        try {
            if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                try {
                    java.net.URI uri = new java.net.URI(documentationLink);
                    desktop.browse(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_jMenu1ActionPerformed
    //DJ: 10/08/2014
    private void docButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_docButtonActionPerformed
        java.awt.Desktop desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;
        try {
            if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                try {
                    java.net.URI uri = new java.net.URI(documentationLink);
                    desktop.browse(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_docButtonActionPerformed
    
    //DJ: 10/08/2014
    private void sampleDataButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleDataButtonActionPerformed
        java.awt.Desktop desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;
        try {
            if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                try {
                    java.net.URI uri = new java.net.URI(sampleDataLink);
                    desktop.browse(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_sampleDataButtonActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        // TODO add your handling code here:
        exportCurrentlySelectedToPNG();
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    //DJ: 10/24/2014
    private void openMyNotes_jMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMyNotes_jMenuItemActionPerformed
        
        String notesPath = prefs.getMyNotesPath();
        //System.out.println("my note's path = " + notesPath);
        if(notesPath.isEmpty()){
            IJ.error("The note's document path is empty - check: Edit>Preferences ");
            return;
        }
        if (!notesPath.endsWith(".odt") && !notesPath.endsWith(".doc") && !notesPath.endsWith(".docx")) {
            IJ.error("Incorrect Document Format - check: Edit>Preferences ");
            return;
        } else{
            File noteFile = new File(notesPath);
            if(!noteFile.exists()){
                IJ.error("Note's file does not exist - check: Edit>Preferences ");
                return;
            }
            if(!noteFile.canRead()){
                IJ.error("Note's file read-access denied - check the file's permissions ");
                return;
            }
            else{
                UnoPlugin.setNotesPath(notesPath);
                UnoPlugin.openDoc(notesPath);
            }
        }
    }//GEN-LAST:event_openMyNotes_jMenuItemActionPerformed

    
    // DJ: Generate tables button
    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        System.out.println("Generate Tables Button Hit");
        
        File[] nrrdFiles = null;;
        File[] roiFiles = null;
        JFileChooser chooser;
        FileFilter filter;
        int returnVal;
        
        //-----------------------------------------------------
        // getting the nrrd files : 
        //-----------------------------------------------------
        chooser = new JFileChooser(ui.getLastFolder());
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("CHOOSE NRRD FILES");
        filter = new ExtensionFileFilter("nrrd", new String("nrrd"));
        chooser.setFileFilter(filter);
        
        returnVal= chooser.showOpenDialog(prefs);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            nrrdFiles = chooser.getSelectedFiles();
        } else {
            System.out.println("No Nrrd Files chosen --   exiting ...");
            return;
        }
        //-----------------------------------------------------
        // getting the roi files : 
        //-----------------------------------------------------
        chooser = new JFileChooser(ui.getLastFolder());
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("CHOOSE ROI FILES");
        filter = new ExtensionFileFilter("roi.zip", new String("rois.zip"));
        chooser.setFileFilter(filter);
        
        returnVal= chooser.showOpenDialog(prefs);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            roiFiles = chooser.getSelectedFiles();
        } else {
            System.out.println("No Roi Files chosen --   exiting ...");
            return;
        }
        //------------------------------------------------------
        // Check of images/rois counts correspondance : 
        //------------------------------------------------------
        if (nrrdFiles.length != roiFiles.length){
            System.out.println("Number of Image Files DOES NOT MATCH The Number of Roi Files ...Existing! ");
            return;
        } 
        
        //------------------------------------------------------
        // Get the Statistics
        //------------------------------------------------------
        
        //------------------------------------------------------
        // Get the Images:
        //------------------------------------------------------
        MimsPlus[] massImages = getOpenMassImages();
        MimsPlus[] ratioImages  = getOpenRatioImages();
        MimsPlus[] HSIImages    = getOpenHSIImages();
        MimsPlus[] sumImages = getOpenSumImages();

        UI newUI = new UI();
        newUI = getInstance();
        
        newUI.setVisible(true);
        
        newUI.openFile(nrrdFiles[0], false);
        newUI.openFile(roiFiles[0], false);
        
        int[] selectedStats = new int[2];
        selectedStats[0] = 0;
        selectedStats[0] = 1;
        
        int[] selectedImages = new int[2];
        selectedImages[0] = 0;
        selectedImages[0] = 1;
        
        
        newUI.getMimsTomography().setSelectedStatIndices(selectedStats);
        newUI.getMimsTomography().setSelectedImagesIndices(selectedImages);
        
       // newUI.mimsTomography.
        
        /*
        HSIProps[] hsiProps = new HSIProps[hsiImages.length];
        for(int indx = 0 ; indx < HSIImages.length ; indx++){
            hsiProps[indx] = HSIImages[indx].getHSIProps();
            System.out.println("hsi props = " + hsiProps[indx].getNumMassValue() + "/" + hsiProps[indx].getDenMassValue());
        }
        */
      
        SumProps[] sumProps = new SumProps[sumImages.length];
        for (int indx = 0; indx < sumImages.length; indx++) {
            sumProps[indx] = sumImages[indx].getSumProps();
            System.out.println("sum props = " + sumProps[0].getSumType());
        }
        
        //newUI.setUsedForTable(true);
        
        //System.out.println("===== before restore state:");
        
        
        
        //newUI.restoreState(null, null, null, sumProps, null, false, false);
        
        
        
        // System.out.println("===== After restore state:");
         
        
        
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void openPrefsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openPrefsActionPerformed
        // TODO add your handling code here:
        preferencesMenuItemActionPerformed(null);
        
    }//GEN-LAST:event_openPrefsActionPerformed

    
    
    /**
     * Applies a correction to the current image and writes the file to
     * fileName.
     *
     * @param fileName - the corrected file.
     * @return retVal <code>true</code> if applied correctly, otherwise <code>false</code>.
     */
    public boolean applyDeadTimeCorrection(String fileName) {

        com.nrims.data.massCorrection masscor = new com.nrims.data.massCorrection(this);
        boolean retVal = true;
        
        float dwell = 0f;
        try {
            //dwell time in sec. (stored as ms in file)
            dwell = Float.parseFloat(this.getOpener().getDwellTime()) / 1000;
            masscor.performDeadTimeCorr(this.getOpenMassImages(), dwell);
            isDTCorrected = true;
            //log what was done
            this.getMimsLog().Log("DT correction dwelltime (s) = " + dwell);
        } catch (NumberFormatException e) {
            if (!silentMode) {
                ij.IJ.error("Error", "Cannot get dwelltime from file header.");
            } else {
                e.printStackTrace();
            }
            retVal = false;
            return retVal;
        } catch (Exception e) {
            if (!silentMode) {
                ij.IJ.error("Error", "Error applying correction and/or saving file:" + e.getMessage());
            } else {
                e.printStackTrace();
            }
            retVal = false;
            return retVal;
        }

        retVal = saveSession(fileName, true);
 
        return retVal;
    }

    /**
     * Updates the line profile to reflect the data stored in
     * <code>newdata</code>.
     *
     * @param newdata the new data.
     * @param name the name to be on the legend.
     * @param width width of the line roi.
     * @param image a <code>MimsPlus</code> image instance
     */
    public void updateLineProfile(double[] newdata, String name, int width, MimsPlus image) {
        if (this.lineProfile == null) {
            return;
        } else {
            lineProfile.updateData(newdata, name, width, image);
        }
    }

    /**
     * Returns an instance of the RoiManager.
     *
     * @return an instance of the RoiManager.
     */
    public MimsRoiManager2 getRoiManager() {
        //roiManager = getRoiManager().getInstance();
        if (roiManager == null) {
            roiManager = new MimsRoiManager2(this);
        } else {
            //roiManager.cancelTimer();
            //roiManager.startTimer(1);
        }
        return roiManager;
    }
    public void resetRoiManager(){
        roiManager = null;
        roiManager = new MimsRoiManager2(this);
    }

    /**
     * Returns an instance of the ReportGenerator
     *
     * @return an instance of the ReportGenerator
     */
    public ReportGenerator getReportGenerator() {
        return rg;
    }

    /**
     * Sets the report generator
     *
     * @param gen the <code>ReportGenerator</code> instance
     */
    protected void setReportGenerator(ReportGenerator gen) {
        if (gen != null) {
            rg = gen;
        } else {
            rg = null;
        }
    }

    /**
     * Returns the directory of the image currently opened.
     *
     * @return the directory of the current image file.
     */
    public String getImageDir() {
        if (image == null) {
            // Image can be null if the user open a non-MIMS image file and then attempts to save
            // ROIs defined on that image.
            return lastFolder;
        }
        String path = image.getImageFile().getParent();
        return path;
    }
    
    /**
     * Returns true if image header was fixed.
     *
     * @return true if image header was fixed.
     */
    public boolean getImageHeaderWasFixed() {
        return image.getWasHeaderFixed();
    }
    
    /**
     * Returns true if image header was bad.
     *
     * @return true if image header was bad.
     */
    public boolean getIsHeaderBad() {
        return image.getIsHeaderBad();
    }

    /**
     * Returns all the mass images, regardless if they are open or not.
     *
     * @return MimsPlus array of mass images.
     */
    public MimsPlus[] getMassImages() {
        return massImages;
    }

    /**
     * Returns the mass image with index
     * <code>i</code>.
     *
     * @param i the index.
     * @return the mass image.
     */
    public MimsPlus getMassImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return massImages[i];
        }
        return null;
    }

    /**
     * Return the index of the mass that falls closest to massValue (and within
     * tolerance).
     *
     * Used in Converter.java. Should be deprecated.
     *
     * @param massValue the massValue.
     * @param tolerance the range of possible masses * *
     * from <code>massValue</code>.
     * @return the index
     */
    public int getClosestMassIndices(double massValue, double tolerance) {
        double massVal1, diff;
        double mindiff = Double.MAX_VALUE;
        int returnIdx = -1;
        
        if (tolerance <= 0.0) {
            return returnIdx;
        }

        String[] massNames = getOpener().getMassNames();
        for (int i = 0; i < massNames.length; i++) {
            massVal1 = (new Double(getOpener().getMassNames()[i])).doubleValue();
            diff = Math.abs(massValue - massVal1);          
            if (diff <= mindiff && diff < tolerance) {
                mindiff = diff;
                returnIdx = i;
            }
        }
        return returnIdx;
    }

    /**
     * Returns all mass indices with a mass value within of
     * <code>massValue</code> +/-
     * <code>tolerance</code>.
     *
     * @param massValue the massValue.
     * @param tolerance the range of possible masses * *
     * from <code>massValue</code>.
     * @return the indices
     */
    public int[] getMassIndices(double massValue, double tolerance) {
        double massVal1, diff;

        if (tolerance > 0.0) {
            // do nothing
        } else {
            return null;
        }

        MimsPlus[] mps = getOpenMassImages();
        ArrayList imageList = new ArrayList<MimsPlus>();
        for (int i = 0; i < mps.length; i++) {
            massVal1 = mps[i].getMassValue();
            diff = Math.abs(massValue - massVal1);
            if (diff < tolerance) {
                imageList.add(mps[i]);
            }
        }

        int[] indices = new int[imageList.size()];
        for (int i = 0; i < imageList.size(); i++) {
            indices[i] = ((MimsPlus) imageList.get(i)).getMassIndex();
        }

        return indices;
    }

    /**
     * Gets the mass value for mass image with index
     * <code>i</code>.
     *
     * @param i the index.
     * @return the mass value.
     */
    public double getMassValue(int i) {
        double mass = -1.0;
        try {
            mass = new Double(getOpener().getMassNames()[i]);
        } catch (Exception e) {
        }
        return mass;
    }

    /**
     * Get the description of the file in a single String.
     *
     * @return string of the description
     */
    public String getDescription() {
        return mimsData.getText();
    }

    /**
     * Gets the ratio image with index
     * <code>i</code>.
     *
     * @param i the index
     * @return the image.
     */
    public MimsPlus getRatioImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return ratioImages[i];
        }
        return null;
    }

    /**
     * Gets the HSI image with index
     * <code>i</code>.
     *
     * @param i the index
     * @return the image.
     */
    public MimsPlus getHSIImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return hsiImages[i];
        }
        return null;
    }

    /**
     * Returns all the open Mass, Sum, Ratio, hsi and composites
     *
     * @return array of images.
     */
    public MimsPlus[] getAllOpenImages() {
        MimsPlus[] massImages = getOpenMassImages();
        MimsPlus[] sumImages = getOpenSumImages();
        MimsPlus[] ratioImages = getOpenRatioImages();
        MimsPlus[] hsiImages = getOpenHSIImages();
        MimsPlus[] compImages = getOpenCompositeImages();

        MimsPlus[] allImages = new MimsPlus[massImages.length + sumImages.length + ratioImages.length + hsiImages.length + compImages.length];
        int i = 0;
        for (MimsPlus mp : massImages) {
            allImages[i] = mp;
            i++;
        }
        for (MimsPlus mp : sumImages) {
            allImages[i] = mp;
            i++;
        }
        for (MimsPlus mp : ratioImages) {
            allImages[i] = mp;
            i++;
        }
        for (MimsPlus mp : hsiImages) {
            allImages[i] = mp;
            i++;
        }
        for (MimsPlus mp : compImages) {
            allImages[i] = mp;
            i++;
        }

        return allImages;
    }

    /**
     * Returns the open mass images as an array.
     *
     * @return array of images.
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
        for (i = 0, nOpen = 0; i < massImages.length; i++) {
            if (massImages[i] != null && bOpenMass[i]) {
                mp[nOpen++] = massImages[i];
            }
        }
        return mp;
    }

    public MimsPlus[] getOpenNonMimsImages() {
        int i, nOpen = 0;
        for (i = 0; i < nonMIMSImages.size(); i++) {
            if (nonMIMSImages.get(i) != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0, nOpen = 0; i < nonMIMSImages.size(); i++) {
            if (nonMIMSImages.get(i) != null) {
                mp[nOpen++] = nonMIMSImages.get(i);
            }
        }
        return mp;
    }

    /**
     * Returns the open ratio images as an array.
     *
     * @return array of images.
     */
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
        for (i = 0, nOpen = 0; i < maxMasses; i++) {
            if (ratioImages[i] != null) {
                mp[nOpen++] = ratioImages[i];
            }
        }
        return mp;
    }

    /**
     * Returns the open composite images as an array.
     *
     * @return array of images.
     */
    public MimsPlus[] getOpenCompositeImages() {
        int i, nOpen = 0;
        for (i = 0; i < maxMasses; i++) {
            if (compImages[i] != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0, nOpen = 0; i < maxMasses; i++) {
            if (compImages[i] != null) {
                mp[nOpen++] = compImages[i];
            }
        }
        return mp;
    }

    /**
     * Add the key value pair to the metaData hashmap. Nulls not allowed as key
     * or value.
     *
     * @param key the key
     * @param value the value
     */
    public synchronized void insertMetaData(String key, String value) {
        metaData.put(key, value);
    }

    /**
     * Return the value associated with key.
     *
     * @param key the key
     * @return the value
     */
    public synchronized String getMetaDataFromKey(String key) {
        return (String) metaData.get(key);
    }

    /**
     * Return the map.
     *
     * @return the map
     */
    public HashMap getMetaData() {
        return metaData;
    }

    /**
     * Returns the open HSI images as an array.
     *
     * @return array of images.
     */
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
        for (i = 0, nOpen = 0; i < maxMasses; i++) {
            if (hsiImages[i] != null) {
                mp[nOpen++] = hsiImages[i];
            }
        }
        return mp;
    }

    /**
     * Returns the open segmented images as an array.
     *
     * @return array of images.
     */
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

    /**
     * Returns the open sum images as an array.
     *
     * @return array of images.
     */
    public MimsPlus[] getOpenSumImages() {
        int i, nOpen = 0;
        for (i = 0; i < 2 * maxMasses; i++) {
            if (sumImages[i] != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0, nOpen = 0; i < 2 * maxMasses; i++) {
            if (sumImages[i] != null) {
                mp[nOpen++] = sumImages[i];
            }
        }
        return mp;
    }

    /**
     * Returns the
     * <code>HSIProps</code> object for all open HSI images.
     *
     * @return array of <code>HSIProps</code> objects.
     */
    public HSIProps[] getOpenHSIProps() {
        MimsPlus[] hsi = getOpenHSIImages();
        HSIProps[] hsi_props = new HSIProps[hsi.length];
        for (int i = 0; i < hsi.length; i++) {
            hsi_props[i] = hsi[i].getHSIProps();
        }
        return hsi_props;
    }

    /**
     * Returns the
     * <code>RatioProps</code> object for all open ratio images.
     *
     * @return array of <code>RatioProps</code> objects.
     */
    public RatioProps[] getOpenRatioProps() {
        MimsPlus[] rto = getOpenRatioImages();
        RatioProps[] rto_props = new RatioProps[rto.length];
        for (int i = 0; i < rto.length; i++) {
            rto_props[i] = rto[i].getRatioProps();
        }
        return rto_props;
    }

    /**
     * Returns the
     * <code>SumProps</code> object for all open sum images.
     *
     * @return array of <code>SumProps</code> objects.
     */
    public SumProps[] getOpenSumProps() {
        MimsPlus[] sum = getOpenSumImages();
        SumProps[] sum_props = new SumProps[sum.length];
        for (int i = 0; i < sum.length; i++) {
            sum_props[i] = sum[i].getSumProps();
            sum_props[i].setXWindowLocation(sum[i].getWindow().getX());
            sum_props[i].setYWindowLocation(sum[i].getWindow().getY());
            if (sum_props[i].getSumType() == SumProps.RATIO_IMAGE) {
                sum_props[i].setNumMassValue(getMassValue(sum_props[i].getNumMassIdx()));
                sum_props[i].setDenMassValue(getMassValue(sum_props[i].getDenMassIdx()));
            } else if (sum_props[i].getSumType() == SumProps.MASS_IMAGE) {
                sum_props[i].setParentMassValue(getMassValue(sum_props[i].getParentMassIdx()));
            }

            //should these be set inside getprops?
            //maybe...
            sum_props[i].setMinLUT(sum[i].getDisplayRangeMin());
            sum_props[i].setMaxLUT(sum[i].getDisplayRangeMax());

            sum_props[i].setMag(sum[i].getCanvas().getMagnification());
        }
        return sum_props;
    }

    /**
     * Returns the
     * <code>MassProps</code> object for all open mass images.
     *
     * @return array of <code>MassProps</code> objects.
     */
    public MassProps[] getOpenMassProps() {

        MimsPlus[] mass = getOpenMassImages();
        MassProps[] mass_props = new MassProps[mass.length];

        for (int i = 0; i < mass.length; i++) {
            mass_props[i] = new MassProps(i);
            mass_props[i].setMassValue(mass[i].getMassValue());

            if (mass[i].isVisible()) {
                mass_props[i].setXWindowLocation(mass[i].getWindow().getX());
                mass_props[i].setYWindowLocation(mass[i].getWindow().getY());

            }

            //should these be set inside getprops?
            //maybe...
            mass_props[i].setMinLUT(mass[i].getDisplayRangeMin());
            mass_props[i].setMaxLUT(mass[i].getDisplayRangeMax());

            if (mass[i].isVisible()) {
                mass_props[i].setMag(mass[i].getCanvas().getMagnification());
            }

            mass_props[i].setVisibility(mass[i].isVisible()); // DJ: 07/29/2014

        }
        return mass_props;
    }

    // DJ : 08/01/2014
    /**
     * Returns the
     * <code>CompositeProps</code> object for all open composite images.
     *
     * @return array of <code>CompositeProps</code> objects.
     */
    public CompositeProps[] getOpenCompositeProps() {

        MimsPlus[] composite = getOpenCompositeImages();
        CompositeProps[] composite_props = new CompositeProps[composite.length];
        for (int i = 0; i < composite.length; i++) {
            composite_props[i] = composite[i].getCompositeProps();
        }
        return composite_props;
    }

    /**
     * Returns the mass, ratio, HSI or sum image with the name
     * <code>name</code>.
     *
     * @param name the name
     * @return an image.
     */
    public MimsPlus getImageByName(String name) {
        MimsPlus mp = null;
        MimsPlus[] tempimages;

        // Mass images.
        tempimages = getOpenMassImages();
        for (int i = 0; i < tempimages.length; i++) {
            if (name.equals(tempimages[i].getTitle())) {
                return tempimages[i];
            }
        }

        // Ratio images.
        tempimages = getOpenRatioImages();
        for (int i = 0; i < tempimages.length; i++) {
            if (name.equals(tempimages[i].getTitle())) {
                return tempimages[i];
            }
        }

        // Hsi images.
        tempimages = getOpenHSIImages();
        for (int i = 0; i < tempimages.length; i++) {
            if (name.equals(tempimages[i].getTitle())) {
                return tempimages[i];
            }
        }

        // Sum images.
        tempimages = getOpenSumImages();
        for (int i = 0; i < tempimages.length; i++) {
            if (name.equals(tempimages[i].getTitle())) {
                return tempimages[i];
            }
        }
        // DJ: 08/01/2014
        // Composite images.
        tempimages = getOpenCompositeImages();
        for (int i = 0; i < tempimages.length; i++) {
            if (name.equals(tempimages[i].getTitle())) {
                return tempimages[i];
            }
        }

        return mp;
    }

    /**
     * Sets if all mass images should croll through the stack in unison
     * (synched).
     *
     * @param bSync Set to <code>true</code> if mass images should scroll in
     * sych, otherwise <code>false</code>.
     */
    public void setSyncStack(boolean bSync) {
        bSyncStack = bSync;
    }

    /**
     * Returns a pointer to the
     * <code>MimsData</code> object which controls the "Data" tab.
     *
     * @return the <code>MimsData</code> object.
     */
    public MimsData getMimsData() {
        return mimsData;
    }

    /**
     * Returns a pointer to the
     * <code>MimsHSIView</code> object which controls the "Process" tab.
     *
     * @return the <code>MimsHSIView</code> object.
     */
    public MimsHSIView getHSIView() {
        return hsiControl;
    }

    /**
     * Returns a pointer to the
     * <code>MimsLog</code> object which controls the "MIMS Log" tab.
     *
     * @return the <code>MimsLog</code> object.
     */
    public MimsLog getMimsLog() {
        return mimsLog;
    }

    /**
     * Returns a pointer to the
     * <code>MimsCBControl</code> object which controls the "Contrast" tab.
     *
     * @return the <code>MimsCBControl</code> object.
     */
    public MimsCBControl getCBControl() {
        return cbControl;
    }
    //DJ:10/17/2014
    public int getMimsPlusImageContrast(MimsPlus mp){
        setActiveMimsPlus(mp);
        return cbControl.getContrastLevel();
    }
    //DJ:10/17/2014
    public int getMimsPlusImageBrightness(MimsPlus mp){
        setActiveMimsPlus(mp);
        return cbControl.getBrightnessLevel();
    }

    /**
     * Returns a pointer to the
     * <code>MimsStackEditor</code> object which controls the "Stack Editing"
     * tab.
     *
     * @return the <code>MimsStackEditor</code> object.
     */
    public MimsStackEditor getMimsStackEditing() {
        return mimsStackEditing;
    }

    /**
     * Returns a pointer to the
     * <code>MimsTomography</code> object which controls the "Tomography" tab.
     *
     * @return the <code>MimsTomography</code> object.
     */
    public MimsTomography getMimsTomography() {
        return mimsTomography;
    }

    /**
     * Returns a pointer to the
     * <code>SegmentationForm</code> object which controls the "Segmentation"
     * tab.
     *
     * @return the <code>MimsTomography</code> object.
     */
    public SegmentationForm getMimsSegmentation() {
        return segmentation;
    }

    public static UI getInstance() {
        return (UI) ui;
    }

    /**
     * Returns a pointer to the
     * <code>MimsAction</code> object. The
     * <code>MimsAction</code> object stores all the modifications made to the
     * current image (translations, plane deletions, etc).
     *
     * @return the <code>MimsAction</code> object.
     */
    public MimsAction getMimsAction() {
        return mimsAction;
    }

    /**
     * Sets the flag indicating the plugin is in the process of updating.
     *
     * @param bool <code>true</code> if updating, otherwise false.
     */
    public void setUpdating(boolean bool) {
        bUpdating = bool;
    }

    /**
     * Return
     * <code>true</code> if the plugin is in the process of opening an image.
     *
     * @return <code>true</code> if the plugin is in the process of opening an
     * image, otherwise <code>false</code>.
     */
    public boolean isOpening() {
        return currentlyOpeningImages;
    }

    /**
     * Return
     * <code>true</code> if the plugin is in silent mode.
     *
     * @return <code>true</code> if the plugin is in silent mode.
     */
    public boolean isSilentMode() {
        return silentMode;
    }

    /**
     * Gets the flag indicating if the plugin is in the process of updating.
     *
     * @return <code>true</code> if updating, otherwise false.
     */
    public boolean isUpdating() {
        return bUpdating;
    }

    /**
     * Returns a link the
     * <code>PrefFrame</code> object for referencing user preferences.
     *
     * @return the <code>PrefFrame</code> object.
     */
    public PrefFrame getPreferences() {
        return prefs;
    }

    /**
     * Returns a link the
     * <code>Opener</code> object for getting image data and metadata. The UI
     * class stores a list of opener objects in the case that the image was
     * concatenated or derived from multiple files. This method returns the
     * member variable
     * <code>image</code> which will correspond to the FIRST image opened.
     *
     * @return the <code>Opener</code> object.
     */
    public Opener getOpener() {
        return image;
    }

    /**
     * Gets
     * <code>Opener</code> with name
     * <code>name</code> from the list of opener objects.
     *
     * @param name the name of the opener object.
     * @return the <code>Opener</code>.
     */
    public Opener getFromOpenerList(String name) {
        return (Opener) openers.get(name);
    }

    /**
     * Adds the
     * <code>Opener</code> object with name
     * <code>name</code> to the list of openers.
     *
     * @param fileName name of file.
     * @param opener opener object.
     */
    public void addToOpenerList(String fileName, Opener opener) {
        openers.put(fileName, opener);
    }

    /**
     * Determines the behavior when an image window is made active.
     *
     * @param mp the image.
     */
    public void setActiveMimsPlus(MimsPlus mp) {
        if (mp == null) {
            return;
        }

        if (mp.getMimsType() == MimsPlus.HSI_IMAGE) {
            hsiControl.setCurrentImage(mp);
            hsiControl.setProps(mp.getHSIProcessor().getHSIProps());
        } else if (mp.getMimsType() == MimsPlus.RATIO_IMAGE) {
            hsiControl.setCurrentImage(mp);
            hsiControl.setProps(mp.getRatioProps());
        }
    }

    /**
     * Displays the String
     * <code>msg</code> in the status bar.
     *
     * @param msg the message to display.
     */
    public void updateStatus(String msg) {
        jProgressBar1.setString(msg);
    }

    /**
     * Sets the radius of the radius of the median filter.
     *
     * @param r the radius.
     */
    public void setMedianFilterRadius(double r) {
        this.medianFilterRadius = r;
    }

    /**
     * Gets the radius of the radius of the median filter.
     *
     * @return the radius.
     */
    public double getMedianFilterRadius() {
        return this.medianFilterRadius;
    }

    /**
     * N
     * Returns the directory of the last location used by the user for loading
     * or saving image data.
     *
     * @return the last folder.
     */
    public String getLastFolder() {
        return lastFolder;
    }

    /**
     * Set the directory of the last location used to retrieve data.
     *
     * @param path the last folder.
     */
    public void setLastFolder(String path) {
        if (path == null) {
            return;
        }

        File folder = new File(path);
        if (folder.exists() && folder.isDirectory()) {
            lastFolder = path;
        }
    }

    /**
     * Set the directory of the last location used to retrieve data.
     *
     * @param folder the last folder.N
     */
    public void setLastFolder(File folder) {
        if (folder.exists() && folder.isDirectory()) {
            setLastFolder(folder.getAbsolutePath());
        }
    }

    /**
     * Sets the ImageJ default directory so that when imageJ file choosers are
     * opened, they are pointing to the directory
     * <code>dir</code>.
     *
     * @param dir the directory.
     */
    public void setIJDefaultDir(String dir) {
        File defaultDir = new File(dir);
        if (!defaultDir.exists()) {
            return;
        }
        ij.io.OpenDialog temp = new ij.io.OpenDialog("", dir);
        temp.setDefaultDirectory(dir);
        temp = null;
    }

    /**
     * This is a copy of imageJ's WindowOrganizer.tileWindows() method.
     *
     */
    public void tileWindows() {
        //Change by DJ: 07/29/2014
        //XSTART changed from 4 to 40 to have more space on left: better visibility and lining
        final int XSTART = 40, GAP = 2;
        int tileY = prefs.getTileY();
        int YSTART = 80 + tileY;
        int titlebarHeight = IJ.isMacintosh() ? 40 : 20;

        Dimension screen = IJ.getScreenSize();
        int minWidth = Integer.MAX_VALUE;
        int minHeight = Integer.MAX_VALUE;
        boolean allSameSize = true;
        int width = 0, height = 0;
        double totalWidth = 0;
        double totalHeight = 0;
        MimsPlus[] windows = getOpenMassImages();
        double[] massVals = new double[windows.length];
        int[] win_ids = new int[windows.length];
        int n = 0;
        MimsPlus[] massImages = windows;
        ArrayList<MimsPlus> sortedMassImages = new ArrayList<MimsPlus>();
        MimsPlus[] sumImages = getOpenSumImages();
        MimsPlus[] ratioImages = getOpenRatioImages();
        MimsPlus[] hsiImages = getOpenHSIImages();
        MimsPlus[] compositeImages = getOpenCompositeImages(); //DJ : 08/01/2014
        n = 0;
        int row = 0;
        MimsPlus curZero = null;
        if (massImages[0].getMassValue() != 0) {
            sortedMassImages.add(massImages[0]);
        } else {
            curZero = massImages[0];
        }




        //DJ: 08/11/2014
        // handles the case where images are grouped (1)title ,(1)title,....,(2)title, (2)title...
        // they get sorted per group or collection
        
        int collectionsCount = getNumberOfGroups();

        if (collectionsCount != 0) {

            String[][] massIndex_vs_collectionSet = new String[massImages.length][];
            for (int i = 0; i < massImages.length; i++) {
                massIndex_vs_collectionSet[i] = new String[collectionsCount + 1];
            }

            for (int i = 0; i < massImages.length; i++) {
                int indexOfOpenBrakets = massImages[i].getTitle().indexOf("(");
                int indexOfCloseBrakets = massImages[i].getTitle().indexOf(")");
                String collection_set = massImages[i].getTitle().substring(indexOfOpenBrakets + 1, indexOfCloseBrakets);

                int j = Integer.parseInt(collection_set);
                massIndex_vs_collectionSet[i][j] = "checkMark";

            }

            for (int j = 0; j < massIndex_vs_collectionSet[1].length; j++) {

                for (int i = 1; i < massImages.length; i++) {
                    if ((massIndex_vs_collectionSet[i][j]) != null) {
                        if ((massIndex_vs_collectionSet[i][j]).equals("checkMark")) {
                            MimsPlus cur = massImages[i];
                            MimsPlus prev = massImages[i - 1];
                            //  if (prev.getMassValue() == 0 && i > 1) prev = massImages[i-2];
                            if (prev.getMassValue() > cur.getMassValue()) {
                                if (cur.getMassValue() != 0) {
                                    if (curZero != null) {
                                        sortedMassImages.add(curZero);
                                    }
                                    if (row == 0) {
                                        row = sortedMassImages.size();
                                    }
                                } else {
                                    curZero = cur;
                                }
                            }
                            if (cur.getMassValue() != 0) {
                                sortedMassImages.add(cur);
                            }
                            // if (i == massImages.length-1 && curZero != null) sortedMassImages.add(curZero);
                        }
                    }
                }
            }
           
            // traditional case where imgs are not grouped at all.  
        } else {
            for (int i = 1; i < massImages.length; i++) {
                MimsPlus cur = massImages[i];
                MimsPlus prev = massImages[i - 1];
                if (prev.getMassValue() == 0 && i > 1) {
                    prev = massImages[i - 2];
                }
                if (prev.getMassValue() > cur.getMassValue()) {
                    if (cur.getMassValue() != 0) {
                        if (curZero != null) {
                            sortedMassImages.add(curZero);
                        }
                        if (row == 0) {
                            row = sortedMassImages.size();
                        }
                    } else {
                        curZero = cur;
                    }
                }
                if (cur.getMassValue() != 0) {
                    sortedMassImages.add(cur);
                }
                if (i == massImages.length - 1 && curZero != null) {
                    sortedMassImages.add(curZero);
                }
            }
        }


        int[] wList = new int[sortedMassImages.size() + sumImages.length + ratioImages.length + hsiImages.length];
        int j = 0;
        //need to add all types of images into win_ids so they are tiled too
        for (int i = 0; i < sortedMassImages.size(); i++) {
            wList[j++] = sortedMassImages.get(i).getID();
        }
        for (MimsPlus mp : sumImages) {
            wList[j++] = mp.getID();
        }
        for (MimsPlus mp : ratioImages) {
            wList[j++] = mp.getID();
        }
        for (MimsPlus mp : hsiImages) {
            wList[j++] = mp.getID();
        }
        for (int i = 0; i < wList.length; i++) {
            //ImageWindow win = getWindow(wList[i]);
            ImageWindow win = null;
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp != null) {
                win = imp.getWindow();
            }
            if (win == null) {
                continue;
            }
            Dimension d = win.getSize();
            int w = d.width;
            int h = d.height + titlebarHeight;
            if (i == 0) {
                width = w;
                height = h;
            }
            if (w != width || h != height) {
                allSameSize = false;
            }
            if (w < minWidth) {
                minWidth = w;
            }
            if (h < minHeight) {
                minHeight = h;
            }
            totalWidth += w;
            totalHeight += h;
        }
        int nPics = wList.length;
        double averageWidth = totalWidth / nPics;
        double averageHeight = totalHeight / nPics;
        int tileWidth = (int) averageWidth;
        int tileHeight = (int) averageHeight;
        //IJ.write("tileWidth, tileHeight: "+tileWidth+" "+tileHeight);
        int hspace = screen.width - 2 * GAP;
        if (tileWidth > hspace) {
            tileWidth = hspace;
        }
        int vspace = screen.height - YSTART;
        if (tileHeight > vspace) {
            tileHeight = vspace;
        }
        int hloc, vloc;
        boolean theyFit;
        do {
            hloc = XSTART;
            vloc = YSTART;
            theyFit = true;
            int i = 0;
            do {
                i++;
                if (hloc + tileWidth > screen.width) {
                    hloc = XSTART;
                    vloc = vloc + tileHeight;
                    if (vloc + tileHeight > screen.height) {
                        theyFit = false;
                    }
                }
                hloc = hloc + tileWidth + GAP;
            } while (theyFit && (i < nPics));
            if (!theyFit) {
                tileWidth = (int) (tileWidth * 0.98 + 0.5);
                tileHeight = (int) (tileHeight * 0.98 + 0.5);
            }
        } while (!theyFit);
        int nColumns = (screen.width - XSTART) / (tileWidth + GAP);
        int nRows = nPics / nColumns;
        if ((nPics % nColumns) != 0) {
            nRows++;
        }
        hloc = XSTART;
        vloc = YSTART;
        int currentlyInRow = 0;
        for (int i = 0; i < nPics; i++) {
            if (hloc + tileWidth > screen.width || (row != 0 && currentlyInRow >= row)) {
                hloc = XSTART;
                vloc = vloc + tileHeight;
                currentlyInRow = 0;
            }
            ImageWindow win = null;
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp != null) {
                win = imp.getWindow();
            }
            if (win != null) {
                win.setLocation(hloc, vloc);
                ImageCanvas canvas = win.getCanvas();
                while (win.getSize().width * 0.85 >= tileWidth && canvas.getMagnification() > 0.03125) {
                    canvas.zoomOut(0, 0);
                }
                win.toFront();
            }
            currentlyInRow++;
            hloc += tileWidth + GAP;
        }
    }

    @Override
    public void run(String cmd) {
        OMLOGGER.info("UI.run");

        if (cmd != null) {
            OMLOGGER.info("Fiji args: " + cmd);
            if (cmd.equals("-t")) {
                initComponentsTesting();
            }
        }

        super.run("");
        setVisible(true);
    }

    /**
     * @param args the command line argumentsS
     */
    //NOT hit if starting plugin from ImageJ plugins menu
    //Program startup flow
    //
    //If being called from Netbeans or the main class is being called in an executable (runUI) then we will hit this function first
    //then we create the nrimsPlugin class, the construction of which does nothing but log some info
    //we then call run and pass it the arguments we recieved in main
    //run will parse the arguments and configure the UI globals
    //then it will create a new instance of UI and set it visible
    //it then checks if there are any arguments passed from ImageJ
    //then passes those to UI.run(), which only checks for the testing flag.
    public static void main(String args[]) {
        
        if (args.length > 0) {
            if (args[0].equals("runningInNetBeans")) {
                OMLOGGER.info("Running in debug mode.");
                runningInNetBeans = true;
            } else {
                OMLOGGER.info("NOT running in debug mode.");
            }
        }
        
        NRIMS_Plugin nrimsPlugin = new NRIMS_Plugin();
        nrimsPlugin.run(FileUtilities.joinArray(args));
        //to emulate hitting it from the gui, comment out the above and uncomment the below:
        //nrimsPlugin.run("");


    }

    /**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int progress = (Integer) evt.getNewValue();
            jProgressBar1.setValue(progress);
        } else if (evt.getNewValue() == StateValue.DONE) {
            //  Some java documentation recommends calling cancel on a completed Swingworker,
            // but this does not seem to change anything.
            //task.cancel(true);
        }
    }

    /**
     * Checks if the current file has been modified. If so, the user is prompted
     * to see if he/she wants to save.
     *
     * @return <code>true</code> if a save action is required, otherwise false.
     */
    private boolean checkCurrentFileStatusBeforeOpening() {
        if (mimsAction != null && (mimsAction.isImageModified() || imgNotes.isModified())) {
            int n = JOptionPane.showConfirmDialog(
                    this,
                    "The current file has been modified.\n\t\tDo you want to save the changes?\n",
                    "Warning",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (n == JOptionPane.NO_OPTION) {
                return true;
            }
            if (n == JOptionPane.YES_OPTION) {
                return saveMIMS(null);
            }
            if (n == JOptionPane.CANCEL_OPTION) {
                return false;
            }
        }
        return true;
    }

    public int getInterval() {
        if (prefs != null) {
            return 1000 * prefs.getAutoSaveInterval();
        } else {
            return 120000;
        }
    }

    /**
     * openNext(): called to open next file in same folder as current image.
     */
    public void openNext() {
        if (image != null) {
            String imageName = image.getImageFile().getName();
            String path = getLastFolder();
            String nextPath = FileUtilities.getNext(path, imageName, true);
            File nextFile = new File(nextPath);
            if (nextFile != null) {
                System.out.println(nextFile.getName());
                if (checkCurrentFileStatusBeforeOpening()) {
                    openFileInBackground(nextFile);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Unable to find next file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "No image loaded", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * The FileOpenTask will open image files either in the background or
     * inline. To open a file in the background, use the following code
     * sequence:
     *
     * FileOpenTask fileOpenTask = new FileOpenTask(file, ui);
     * fileOpenTask.addPropertyChangeListener(this); // for progressbar updates.
     * fileOpenTask.execute();
     *
     * To open a file inline, use the following code sequence:
     *
     * FileOpenTask fileOpenTask = new FileOpenTask(file, ui);
     * fileOpenTask.addPropertyChangeListener(this); // for progressbar updates.
     * fileOpenTask.doInBackground();
     *
     * It is counter intuitive the the doInBackground() method does NOT launch
     * the task in the background but that is the result of the naming
     * convention used by java.
     */
    public class FileOpenTask extends SwingWorker<Boolean, Void> {

        UI ui;
        File file;
        boolean onlyReadHeader = false;
        
 

        public FileOpenTask(File file, UI ui, boolean onlyReadHeader) {
            this.file = file;
            this.ui = ui;
            this.onlyReadHeader = onlyReadHeader;
            
            // DJ:08/13/2014: just a separator -for better visibility purposes only
            System.out.println("-------------------------------------------------"); 
            
            OMLOGGER.info("OpenMIMS: UI.FileOpenTask(): " + file.getAbsolutePath());
        }
        
        public FileOpenTask(File file, UI ui) {
            this(file, ui, false);
        }
        
        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Boolean doInBackground() {
            mass_props = getOpenMassProps();
            rto_props = getOpenRatioProps();
            hsi_props = getOpenHSIProps();
            sum_props = getOpenSumProps();
            
            // Roi Manager visible.
            boolean roiManagerVisible = getRoiManager().isVisible();
            
            composite_props = getOpenCompositeProps();  // DJ: 08/01/2014
            boolean success = doInBackground(mass_props, rto_props, 
                hsi_props, sum_props, composite_props,
                true, roiManagerVisible);
            
            
            return success;
            
            
            
        
            /*
                
            // Get previous image size.
            int old_width = 0;
            int old_height = 0;
            if (image != null) {
                old_width = image.getWidth();
                old_height = image.getHeight();
            }

            

            // Open the file.
            boolean opened = openFile(file);
            if (!opened) {
                //openProcessFailedOrCanceled(); 
                return false;
            }
            stopButton.setEnabled(false);

            boolean isRoiFile = (file.getAbsolutePath().endsWith(ROI_EXTENSION) || file.getAbsolutePath().endsWith(ROIS_EXTENSION));
            if (isRoiFile) {
                return true;
            }

            try {
                doneLoadingFile();
            } catch (Exception e) {
                e.printStackTrace();
            }


            // Get new image size.
            int new_width = image.getWidth();
            int new_height = image.getHeight();
            same_size = ((old_height == new_height) && (old_width == new_width));

            // Perform some checks to see if we wanna restore state.          
            boolean isImageFile = (file.getAbsolutePath().endsWith(NRRD_EXTENSION) || file.getAbsolutePath().endsWith(MIMS_EXTENSION));
            if (isImageFile) {
                restoreState(mass_props, rto_props, hsi_props, sum_props, composite_props, same_size, roiManagerVisible);
            }

            // Set up roi manager.
            MimsRoiManager rm = getRoiManager();
            if (rm != null) {
                rm.resetRoiLocationsLength();
            }

            // Autocontrast mass images.
            //autoContrastImages(getOpenMassImages());
            //TODO, remove/refactor
            //This overrides contrast of sum images set in restoreState()
            //autoContrastImages(getOpenSumImages());

            // Update notes gui
            if (image != null) {
                imgNotes.setOutputFormatedText(image.getNotes());
            }

            // Update hashmap
            if (image != null) {
                metaData = image.getMetaDataKeyValuePairs();
            }

            // Update dt correction flag
            if (image != null) {
                isDTCorrected = image.isDTCorrected();
            }
            DTCorrectionMenuItem.setSelected(isDTCorrected);
            DTCorrectionMenuItem.setEnabled(!isDTCorrected);

            // Update QSA correction flag
            if (image != null) {
                isQSACorrected = image.isQSACorrected();
            }
            QSACorrectionMenuItem.setSelected(isQSACorrected);
            QSACorrectionMenuItem.setEnabled(!isQSACorrected);
            
            



        return true;
        */
        }
        
        
        
        //DJ: 08/19/2014
        public Boolean doInBackground(MassProps[] mass_props, RatioProps[] rto_props, 
                HSIProps[] hsi_props, SumProps[] sum_props, CompositeProps[] composite_props,
                boolean same_size, boolean roiManagerVisible) {
            
            //----------------------------------------------------------------------
            // DJ: 09/24/2014 - Saving all the props of the "to-be-closed-file"
            if (mimsData != null) {
                String previousFileTitle = mimsData.getFilePathAndName();

                MassProps[] prevFile_massProps = new MassProps[ui.getOpenMassProps().length];
                prevFile_massProps = ui.getOpenMassProps();

                RatioProps[] prevFile_rtoProps = new RatioProps[ui.getOpenRatioProps().length];
                prevFile_rtoProps = ui.getOpenRatioProps();

                HSIProps[] prevFile_hsiProps = new HSIProps[ui.getOpenHSIProps().length];
                prevFile_hsiProps = ui.getOpenHSIProps();

                SumProps[] prevFile_sumProps = new SumProps[ui.getOpenSumProps().length];
                prevFile_sumProps = ui.getOpenSumProps();

                CompositeProps[] prevFile_compProps = new CompositeProps[ui.getOpenCompositeProps().length];
                prevFile_compProps = ui.getOpenCompositeProps();

                // composite Manager visibility
                boolean isCompositemanagerVisible = false;
                if (com.nrims.managers.CompositeManager.getInstance() != null) {
                    isCompositemanagerVisible = com.nrims.managers.CompositeManager.getInstance().isVisible();
                }

                // roiProps:
                boolean isROIVisble = roiManager.isVisible();
                Roi[] allROIs = new Roi[roiManager.getAllROIs().length];
                allROIs = roiManager.getAllROIs();

                HashMap<String, ArrayList<Integer[]>> locations = new HashMap<String, ArrayList<Integer[]>>();
                locations = roiManager.getLocations();

                //ArrayList<ROIgroup> groups = new ArrayList<ROIgroup>();
                List<ROIgroup> groups = new ArrayList<ROIgroup>();
                groups = roiManager.getGroups();

                //HashMap<String, String> groupsMap = new HashMap<String, String>();
                Map<String, ROIgroup> groupsMap = roiManager.getGroupMap();


                //ArrayList roiProps = new ArrayList();
                List roiProps = new ArrayList();

                roiProps.add(isROIVisble);
                roiProps.add(allROIs);
                roiProps.add(locations);
                roiProps.add(groups);
                roiProps.add(groupsMap);

                // populate the vector of all props for the previous file opened.
                Vector v = new Vector();
                v.add(prevFile_massProps);
                v.add(prevFile_rtoProps);
                v.add(prevFile_hsiProps);
                v.add(prevFile_sumProps);
                v.add(prevFile_compProps);
                v.add(isCompositemanagerVisible);
                v.add(roiProps);

                if (filesProps == null) {
                    filesProps = new LinkedHashMap<String, Vector>();
                }

                if (filesProps.containsKey(previousFileTitle)) {
                    filesProps.remove(previousFileTitle);
                }

                filesProps.put(previousFileTitle, v);

                if (filesProps.containsKey(file.getPath())) {
                    filesProps.remove(file.getPath());
                }

            }
            nameOfFileNowOpened = file.getPath();
        //---------------------------------------------------------------------
            
            // Get previous image size.
            int old_width = 0;
            int old_height = 0;
            if (image != null) {
                old_width = image.getWidth();
                old_height = image.getHeight();
            }

            boolean opened = openFile(file);
            if (!opened) 
                return false;
            
            stopButton.setEnabled(false);

            boolean isRoiFile = (file.getAbsolutePath().endsWith(ROI_EXTENSION) || file.getAbsolutePath().endsWith(ROIS_EXTENSION));
            if (isRoiFile) {
                return true;
            }

            try {
                doneLoadingFile();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Perform some checks to see if we wanna restore state.          
            boolean isImageFile = (file.getAbsolutePath().endsWith(NRRD_EXTENSION) || file.getAbsolutePath().endsWith(MIMS_EXTENSION));
            if (isImageFile && !onlyReadHeader) {
                restoreState(mass_props, rto_props, hsi_props, sum_props, composite_props, same_size, roiManagerVisible);
            }

            // Set up roi manager.
            MimsRoiManager2 rm = getRoiManager();
            if (rm != null) {
                rm.resetRoiLocationsLength();
            }

            // Update notes gui
            if (image != null) {
                imgNotes.setOutputFormatedText(image.getNotes());
                imgNotes.setIsModified(false);
            }

            // Update hashmap
            if (image != null) {
                metaData = image.getMetaDataKeyValuePairs();
            }

            // Update dt correction flag
            if (image != null) {
                isDTCorrected = image.isDTCorrected();
            }
            DTCorrectionMenuItem.setSelected(isDTCorrected);
            DTCorrectionMenuItem.setEnabled(!isDTCorrected);

            // Update QSA correction flag
            if (image != null) {
                isQSACorrected = image.isQSACorrected();
            }
            QSACorrectionMenuItem.setSelected(isQSACorrected);
            QSACorrectionMenuItem.setEnabled(!isQSACorrected);
            
            return true;
        }
        
        

        /**
         * Relegates behavior for opening any of the various MimsPlus file
         * types.
         *
         * @param file to be opened.
         * 
         * @return true if the file was successfully opened, false if the file could not be opened
         */
        public boolean openFile(File file) {
            long length = file.length();
            OMLOGGER.fine("Current file size: " + length);
            OMLOGGER.fine("Total JVM memory: " + Runtime.getRuntime().totalMemory());
            if (length > Runtime.getRuntime().totalMemory()) {
                OMLOGGER.fine("File size exceeds the allocated memory in JVM.");
            }

            boolean onlyShowDraggedFile = true;

            String fileName = file.getName();
            if (fileName.endsWith(NRRD_EXTENSION) || fileName.endsWith(MIMS_EXTENSION)
                    || fileName.endsWith(RATIO_EXTENSION) || fileName.endsWith(HSI_EXTENSION)
                    || fileName.endsWith(SUM_EXTENSION) || fileName.endsWith(ROIS_EXTENSION)
                    || fileName.endsWith(ROI_EXTENSION) || fileName.endsWith(NRRD_HEADER_EXTENSION)
                    || fileName.endsWith(RATIOS_EXTENSION) || fileName.endsWith(HSIS_EXTENSION)
                    || fileName.endsWith(SUMS_EXTENSION) || fileName.endsWith(SESSIONS_EXTENSION)) {
            } else {
                String fileType;
                int lastIndexOf = fileName.lastIndexOf(".");
                if (lastIndexOf >= 0) {
                    fileType = fileName.substring(fileName.lastIndexOf("."));
                } else {
                    fileType = fileName;
                }
                IJ.error("Unable to open files of type: " + fileType + " using this File menu command.\n"
                        + "Try using the \"Open Non-MIMS Image...\" File menu command. ");
                return false;
            }
            
            lastFolder = file.getParent();
            setIJDefaultDir(lastFolder);
            Object obj;
            try {
                if (file.getAbsolutePath().endsWith(NRRD_EXTENSION)
                        || file.getAbsolutePath().endsWith(MIMS_EXTENSION) || file.getAbsolutePath().endsWith(NRRD_HEADER_EXTENSION)) {
                    onlyShowDraggedFile = false;
                    if (!loadMIMSFileInBackground(file)) {
                        return false;
                    }
                } else if (file.getAbsolutePath().endsWith(RATIO_EXTENSION)) {
                    if ((obj = FileUtilities.readObjectFromXML(file)) instanceof RatioProps) {
                        RatioProps ratioprops = (RatioProps) obj;
                        File dataFile = new File(file.getParent(), ratioprops.getDataFileName());
                        if (!loadMIMSFileInBackground(dataFile)) {
                            return false;
                        }
                        doneLoadingFile();
                        sessionOpened = true;
                        MimsPlus mp = new MimsPlus(ui, ratioprops);
                        mp.showWindow();
                    }
                } else if (file.getAbsolutePath().endsWith(HSI_EXTENSION)) {
                    if ((obj = FileUtilities.readObjectFromXML(file)) instanceof HSIProps) {
                        HSIProps hsiprops = (HSIProps) obj;
                        File dataFile = new File(file.getParent(), hsiprops.getDataFileName());
                        if (!loadMIMSFileInBackground(dataFile)) {
                            return false;
                        }
                        doneLoadingFile();
                        sessionOpened = true;
                        MimsPlus mp = new MimsPlus(ui, hsiprops);
                        mp.showWindow();
                    }
                } else if (file.getAbsolutePath().endsWith(SUM_EXTENSION)) {
                    if ((obj = FileUtilities.readObjectFromXML(file)) instanceof SumProps) {
                        SumProps sumprops = (SumProps) obj;
                        File dataFile = new File(file.getParent(), sumprops.getDataFileName());
                        if (!loadMIMSFileInBackground(dataFile)) {
                            return false;
                        }
                        doneLoadingFile();
                        sessionOpened = true;
                        MimsPlus sp = new MimsPlus(ui, sumprops, null);
                        sp.showWindow();
                    }
                } else if (file.getAbsolutePath().endsWith(SESSIONS_EXTENSION)) {
                    onlyShowDraggedFile = false;
                    //get all xml objects contained within zip
                    //ArrayList entries = FileUtilities.openXMLfromZip(file);
                    List entries = FileUtilities.openXMLfromZip(file);
                    if (entries == null || entries.isEmpty()) {
                        JOptionPane.showMessageDialog(ui, ".session.zip is empty/corrupt", "File Read Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                    //sort the objects into props
                    MimsPlus sp;
                    //IMPORTANT: Composite props must be loaded last after all other images are loaded, as it may call
                    //upon other MimsPlus objects
                    //ArrayList<CompositeProps> compProps = new ArrayList<CompositeProps>();
                    List<CompositeProps> compProps = new ArrayList<CompositeProps>();

                    String filename = file.getName().toString();
                    String sessionname = filename.substring(0, filename.lastIndexOf(".session.zip")) + ".nrrd";
                    for (Object entry : entries) {
                        if (entry instanceof SumProps) {
                            SumProps sumprops = (SumProps) entry;
                            if (!sessionOpened) {
                                try {
                                    if (!loadMIMSFileInBackground(new File(file.getParent(), sumprops.getDataFileName()))) {
                                        return false;
                                    }
                                } catch (Exception e) {
                                    if (!loadMIMSFileInBackground(new File(file.getParent(), sessionname))) {
                                        return false;
                                    }
                                }
                                doneLoadingFile();
                                sessionOpened = true;
                            }
                            sp = new MimsPlus(ui, sumprops, null);
                            sp.showWindow();
                        } else if (entry instanceof RatioProps) {
                            RatioProps ratioprops = (RatioProps) entry;
                            if (!sessionOpened) {
                                try {
                                    if (!loadMIMSFileInBackground(new File(file.getParent(), ratioprops.getDataFileName()))) {
                                        return false;

                                    }
                                } catch (Exception e) {
                                    if (!loadMIMSFileInBackground(new File(file.getParent(), sessionname))) {
                                        return false;
                                    }
                                }
                                doneLoadingFile();
                                sessionOpened = true;
                            }
                            sp = new MimsPlus(ui, ratioprops);
                            sp.showWindow();
                        } else if (entry instanceof HSIProps) {
                            HSIProps hsiprops = (HSIProps) entry;
                            if (!sessionOpened) {
                                try {
                                    if (!loadMIMSFileInBackground(new File(file.getParent(), hsiprops.getDataFileName()))) {
                                        return false;

                                    }
                                } catch (Exception e) {
                                    if (!loadMIMSFileInBackground(new File(file.getParent(), sessionname))) {
                                        return false;
                                    }
                                }
                                doneLoadingFile();
                                sessionOpened = true;
                            }
                            sp = new MimsPlus(ui, hsiprops);
                            sp.showWindow();
                        } else if (entry instanceof HSIProps) {
                            HSIProps hsiprops = (HSIProps) entry;
                            if (!sessionOpened) {
                                try {
                                    if (!loadMIMSFileInBackground(new File(file.getParent(), hsiprops.getDataFileName()))) {
                                        return false;

                                    }
                                } catch (Exception e) {
                                    if (!loadMIMSFileInBackground(new File(file.getParent(), sessionname))) {
                                        return false;
                                    }
                                }
                                doneLoadingFile();
                                sessionOpened = true;
                            }
                            sp = new MimsPlus(ui, hsiprops);
                            sp.showWindow();
                        } else if (entry instanceof CompositeProps) {
                            if (!sessionOpened) {
                                try {
                                    if (!loadMIMSFileInBackground(new File(file.getParent(), ((CompositeProps) entry).getDataFileName()))) {
                                        return false;

                                    }
                                } catch (Exception e) {
                                    if (!loadMIMSFileInBackground(new File(file.getParent(), sessionname))) {
                                        return false;
                                    }
                                }
                                doneLoadingFile();
                                sessionOpened = true;
                            }
                            compProps.add((CompositeProps) entry);
                        }
                    }
                    for (CompositeProps entry : compProps) {
                        sp = new MimsPlus(ui, entry);
                        sp.showWindow();
                    }

                } else if (file.getAbsolutePath().endsWith(ROIS_EXTENSION)
                        || file.getAbsolutePath().endsWith(ROI_EXTENSION)) {
                    onlyShowDraggedFile = false;
                    getRoiManager().open(file.getAbsolutePath(), true);
                    updateAllImages();
                    getRoiManager().showFrame();
                }
            } catch (Exception e) {
                IJ.error("Failed to open " + file + ":" + e.getMessage() + "\n");
                e.printStackTrace();
                return false;
            }
            
            // Don't attempt to update scrollbars if we are just ready the header of a file.  Gives
            // a null point exception if you do.
            if (onlyReadHeader) {
                //System.out.println("Reading header only.");                            
            }  else {
               updateScrollbars();
            }
            
            if (onlyShowDraggedFile) {
                MimsPlus[] mps = getOpenMassImages();
                for (int i = 0; i < mps.length; i++) {
                    mps[i].hide();
                }
            }
            return true;
        }

        /**
         * Opens an image file in the .im or .nrrd file format.
         *
         * @param file absolute file path.
         * 
         * @return true if the file was successfully opened, false if the file could not be opened
         * @throws java.lang.NullPointerException thrown if the file does not exist
         */
        public synchronized boolean loadMIMSFileInBackground(File file) throws NullPointerException {
            if (!file.exists()) {
                throw new NullPointerException("File " + file.getAbsolutePath() + " does not exist!");
            }
            long startTime = System.nanoTime();
            boolean isIM = false;
            boolean isNRRD = false;
            int progress = 0;
            closeCurrentImage();
            
            //DJ: 12/05/2014
            getRoiManager().clearRoiJList();  //getRoiManager().roijlist.clearSelection();

            try {

                // Set up the Opener object depending on file type.
                if (file.getName().endsWith(MIMS_EXTENSION)) {
                    image = new Mims_Reader(file);
                    isIM = true;
                } else if (file.getName().endsWith(NRRD_EXTENSION)) {
                    image = new Nrrd_Reader(file);
                    isNRRD = true;
                } else if (file.getName().endsWith(NRRD_HEADER_EXTENSION)) {
                    image = new Nrrd_Reader(file);
                    isNRRD = true;
                } else {
                    return false;
                }

                // Make sure there is agreement between the file size and header.
                boolean checks_out = image.performFileSanityCheck();
                if (checks_out == false) {
                    if (ui.silentMode == true) {    // SilentMode allows the use of certain utility
                        // methods in the UI class.  Ideally, those utility methods might be moved to
                        // some other utility class.   Silent mode is NOT used for single file open or drag and drop,
                        // but is used for batch convert.
                        
                        // Mark this file as having a bad header
                        image.setIsHeaderBad(true);
                        OMLOGGER.warning("File has a bad header.");
                        // Mark file as having bad header and attempt to fix.  Keep array of info about fix status
                        // When all fixed have been processed, show user status of fixes and give user the option
                        // to invoke the OpenManager to manually fix the problem.
                        
                        // Attempt to fix bad header.
                        boolean headerWasFixed = image.fixBadHeader();
                        
                        if (headerWasFixed) {
                            OMLOGGER.warning("File has a bad header, but it was fixed.");
                            image.setWasHeaderFixed(headerWasFixed);
                        }
                    } else {
                        // Attempt to fix bad header
                        int numPlanes = 0;
                        int priorNumPlanes = image.getNImages();
                        boolean headerWasFixed = image.fixBadHeader();
                        if (headerWasFixed) {
                            numPlanes = image.getNImages();
                        }
                        // Show dialog that reports bad header information.
                        //OpenerManager opg = new OpenerManager(ui, image);
                        OpenerManager opg = new OpenerManager(ui, image, headerWasFixed);
                        opg.setModal(true);
                        opg.setLocation(ui.getLocation().x + 50, ui.getLocation().y + 50);
                        opg.setVisible(true);
                        if (opg.isOK() == false) {
                            return false;
                        }
                    }
                }
                
                if (onlyReadHeader) {
                    return true;
                }
                
                // Make sure we have enough memory.
                // This code has not been maintained for a long time and probably
                // contains some errors. At the very least the nImages of the
                // Opener object should be updated. Should be phased out or updated.
                int nMasses = image.getNMasses();
                int nImages = image.getNImages();
                long memRequired = ((long) nMasses) * ((long) image.getWidth()) * ((long) image.getHeight()) * 
                        ((long) 2) * ((long) nImages);
                long maxMemory = IJ.maxMemory() - (128000000);
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
                        return false;
                    }
                    nMasses = 0;
                    for (int i = 0; i < image.getNMasses(); i++) {
                        bOpenMass[i] = gd.getNextBoolean();
                        if (bOpenMass[i]) {
                            nMasses++;
                        }
                    }
                    nImages = (int) gd.getNextNumber();
                    memRequired = memRequired = ((long) nMasses) * ((long) image.getWidth()) * ((long) image.getHeight()) * ((long) 2) * ((long) nImages);
                }

                // Opens the first plane.
                int n = 0;
                int t = image.getNMasses() * (nImages);
                for (int i = 0; i < image.getNMasses(); i++) {
                    if (isCancelled()) {
                        return false;
                    }
                    progress = 100 * n++ / t;
                    setProgress(Math.min(progress, 100));
                    if (bOpenMass[i]) {
                        MimsPlus mp = new MimsPlus(ui, i);
                        mp.setAllowClose(false);
                        massImages[i] = mp;
                        if (mp != null) {
                            massImages[i].getProcessor().setMinAndMax(0, 0);
                            massImages[i].getProcessor().setPixels(image.getPixels(i));
                        }
                    }
                }
                updateStatus("1 of " + nImages);
                if (nImages <= 1) {
                    return true;
                }

                // Appends additional planes.
                //
                // Yes, this code is kinda ugly :(
                // and Yes, this code works :)
                for (int i = 1; i < nImages; i++) {
                    boolean stop = false;
                    if (isCancelled()) {
                        return false;
                    }
                    image.setStackIndex(i);
                    for (int mass = 0; mass < image.getNMasses(); mass++) {
                        progress = 100 * n++ / t;
                        setProgress(Math.min(progress, 100));
                        if (bOpenMass[mass]) {
                            if (stop) {
                                massImages[mass].appendBlankImage(i);
                            } else {
                                try {
                                    massImages[mass].appendImage(i);
                                } catch (IOException ioe) {
                                    if (mass == 0) {
                                        stop = true;
                                        break;
                                    } else if (isIM) {
                                        stop = true;
                                        massImages[mass].appendBlankImage(i);
                                    } else if (isNRRD) {
                                        massImages[mass].appendBlankImage(i);
                                    }
                                }
                            }
                        }
                    }
                    //here the divider ensures that the progress bar is updated no more than 10 times (thus why we divide by 10 and round)
                    int divider = Math.max(Math.round((nImages + 1) / 10), 1);
                    if ((i + 1) % divider == 0) {
                        updateStatus((i + 1) + " of " + nImages);
                    }
                    if (stop) {
                        image.setNImages(i + 1);
                        break;
                    }
                }
                long endTime = System.nanoTime();
                long duration = endTime - startTime;
               // System.out.println(duration);
            } catch (Exception e) {
                if (!ui.silentMode) {                 
                    IJ.error("Failed to open " + file + ":" + e.getMessage() + "\n" + "File may be corrupted.");
                    e.printStackTrace();
                } else {
                    e.printStackTrace();
                }
                return false;
            }
            return true;
        }

        @Override
        public void done() {
            setCursor(null); //turn off the wait cursor
            stopButton.setEnabled(false);
            setProgress(0);
            currentlyOpeningImages = false;
            //System.out.println("Just set currentlyOpeningImages to false");
            
            try {
                get();  // get() will raise an exception if task is terminated by an exception.
                //System.out.println("Done with FileOpenTask, after get");
            }  catch (final InterruptedException ex) {
                System.out.println("InterruptedException exception while calling get in done method of FileOpenTask!");
                ex.printStackTrace();
                throw new RuntimeException(ex);
            } catch (final ExecutionException ex) {
                System.out.println("ExecutionException exception while calling get in done method of FileOpenTask!");
                ex.printStackTrace();
                throw new RuntimeException(ex.getCause());
            }   

            System.out.println("FileOpenTask is finished.");
        }

        public void doneLoadingFile() throws Exception {
            if (!sessionOpened) {
                if (!onlyReadHeader) {
                    Toolkit.getDefaultToolkit().beep();
                }

                jProgressBar1.setValue(0);


                if (ui.single_instance_mode == true) {
                    java.awt.Point p = ui.getLocation();
                    java.awt.Point q = IJ.getInstance().getLocation();

                    OMLOGGER.finer("Ui screen location = " + p.getX() + "," + p.getY());
                    OMLOGGER.finer("Ui screen location = " + q.getX() + "," + q.getY());

                    ui.setVisible(false);
                    ui.setLocation(p);
                    ui.setVisible(true);
                    ui.setExtendedState(javax.swing.JFrame.NORMAL);

                    OMLOGGER.finer("ui location after: " + ui.getLocation());

                    IJ.getInstance().setVisible(false);
                    IJ.getInstance().setLocation(q);
                    IJ.getInstance().setVisible(true);
                    IJ.getInstance().setExtendedState(java.awt.Frame.NORMAL);

                }

                String[] names = image.getStackPositions();
                for (int i = 0; i < image.getNMasses(); i++) {
                    if (bOpenMass[i]) {
                        if (image.getNImages() > 1) {
                            if (names != null && names.length > 0) {
                                ImageStack stack = massImages[i].getImageStack();
                                for (int j = 0; j < names.length; j++) {
                                    stack.setSliceLabel(names[j], j + 1);
                                }
                                massImages[i].setStack(stack);
                            }

                            massImages[i].setIsStack(true);
                            massImages[i].setSlice(1);
                        }
                        if (isSilentMode() == false) {
                            massImages[i].show();
                            massImages[i].updateAndDraw();
                        }
                    }
                }

                for (int i = 0; i < image.getNMasses(); i++) {
                    if (bOpenMass[i]) {
                        massImages[i].addListener(ui);
                    } else {
                        massImages[i] = null;
                    }
                }


                //Lots of code. Seriously. Lots.

                try {
                    
                    // DJ: Fir testing and debugging purposes:
                    /*
                    System.out.println("total number of threads created: " + Thread.getAllStackTraces().keySet().size());
                    int nbRunning = 0;
                    for (Thread t : Thread.getAllStackTraces().keySet()) {
                        if (t.getState() == Thread.State.RUNNABLE) {
                            nbRunning++;
                        }
                    }
                    System.out.println("number of threads actually running: " + nbRunning);
                    */
                    
                    
                    //DJ: 10/03/2014
                    // Yield/wait for the other threads to finish and by then we start
                    // building the OpenMIMS Tabs.
                    // That way we prevent the throw of a nullpointerException.
                    // Note: yield is much better that sleep since sleep requires a 
                    // specific time.
                    Thread.yield();

                    //Thread.sleep(500);
                    
                    jTabbedPane1.setEnabled(true);
                    if (mimsData == null) {
                        initializeViewMenu();
                        mimsData = new com.nrims.MimsData(ui, image);
                        hsiControl = new MimsHSIView(ui);
                        mimsLog = new MimsLog();
                        mimsStackEditing = new MimsStackEditor(ui, image);
                        mimsTomography = new MimsTomography(ui);
                        mimsAction = new MimsAction(image);
                        segmentation = new SegmentationForm(ui);

                        jTabbedPane1.setComponentAt(0, mimsData);
                        jTabbedPane1.setTitleAt(0, "MIMS Data");
                        jTabbedPane1.add("Process", hsiControl);
                        jTabbedPane1.add("Contrast", cbControl);
                        jTabbedPane1.add("Stack Editing", mimsStackEditing);
                        jTabbedPane1.add("Tomography", mimsTomography);
                        jTabbedPane1.add("Segmentation", segmentation);
                        jTabbedPane1.add("MIMS Log", mimsLog);

                    } else {
                        resetViewMenu();
                        mimsData = new com.nrims.MimsData(ui, image);
                        cbControl = new MimsCBControl(ui);
                        mimsStackEditing = new MimsStackEditor(ui, image);
                        int[] indices = mimsTomography.getSelectedStatIndices();
                        mimsTomography = new MimsTomography(ui);
                        mimsTomography.setSelectedStatIndices(indices);
                        mimsAction = new MimsAction(image);
                        segmentation = new SegmentationForm(ui);

                        hsiControl = new MimsHSIView(ui);//DJ: 08/12/2014

                        jTabbedPane1.setComponentAt(0, mimsData);
                        jTabbedPane1.setTitleAt(0, "MIMS Data");
                        jTabbedPane1.setComponentAt(1, hsiControl);
                        jTabbedPane1.setComponentAt(2, cbControl);
                        jTabbedPane1.setComponentAt(3, mimsStackEditing);
                        jTabbedPane1.setComponentAt(4, mimsTomography);
                        jTabbedPane1.setComponentAt(5, segmentation);

                        mimsData.setMimsImage(image);
                        hsiControl.updateImage(false);
                    }

                    MimsHSIView.MimsRatioManager ratioManager = MimsHSIView.MimsRatioManager.getInstance();
                    if (ratioManager != null) {
                        ratioManager.closeWindow();
                        ratioManager = new MimsHSIView.MimsRatioManager(hsiControl, ui);
                        ratioManager.showFrame();
                    }

                    // DJ: 08/12/2014 re-open composite manager if it was open previously
                    compManager = CompositeManager.getInstance();
                    if (compManager != null && compManager.isVisible() == true) {
                        int x = compManager.getX();
                        int y = compManager.getY();
                        compManager.dispose();
                        compManager = new com.nrims.managers.CompositeManager(ui);
                        compManager.setVisible(true);
                        compManager.setLocation(new Point(x, y));

                    }
                    
                    // DJ: 08/26/2014 re-open composite manager if it was open previously
                    
                    
                    filesManager = FilesManager.getInstance();
                    String[] allFilesPaths = new String[0];
                    if (filesManager != null && filesManager.isVisible() == true) {
                        int x = filesManager.getX();
                        int y = filesManager.getY();
                        filesManager.dispose();
                        
                        if(filesProps != null && filesProps.size() != 0)
                            allFilesPaths = filesProps.keySet().toArray(new String[filesProps.keySet().size()]);
                        
                        filesManager = new FilesManager(ui, allFilesPaths, false, file.getPath());
                        filesManager.setVisible(true);

                    }
                    
                    

                    jTabbedPane1.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                            int selected = jTabbedPane1.getSelectedIndex();
                            if (selected == 2) {
                                cbControl.updateHistogram();
                                cbControl.updateWindowLUT();
                            }
                        }
                    });

                    mimsLog.Log("\n\nNew image: " + getImageFilePrefix());
                    mimsLog.Log(ImageDataUtilities.getImageHeader(image));

                    // Calculate theoretical duration
                    double duration = image.getCountTime() * (double) image.getNImages();
                    mimsLog.Log("Theoretical duration (s): " + Double.toString(duration));

                    mimsTomography.resetImageNamesList();
                    mimsStackEditing.resetSpinners();

                    openers.clear();
                    String fName = file.getName();
                    openers.put(fName, image);

                    // Add the windows to the combobox in CBControl.
                    MimsPlus[] mp = getOpenMassImages();
                    for (int i = 0; i < mp.length; i++) {
                        cbControl.addWindowtoList(mp[i]);
                    }
                    previousFileCanceled = false;
                    ui.setTitle("OpenMIMS: " + image.getImageFile().getName().toString());

                } catch (Exception e) {
                    throw new Exception(e);
                }

            } else {
                sessionOpened = false;
            }

        }

        private void openProcessFailedOrCanceled() {
            closeCurrentImage();
            jTabbedPane1.setEnabled(false);
            previousFileCanceled = true;
            currentlyOpeningImages = false;
        }
        
    }  // End inner class FileOpenTask 
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JCheckBoxMenuItem DTCorrectionMenuItem;
    public javax.swing.JCheckBoxMenuItem QSACorrectionMenuItem;
    private javax.swing.JMenuItem RecomputeAllMenuItem;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenuItem batch2nrrdMenuItem;
    private javax.swing.JMenuItem captureImageMenuItem;
    private javax.swing.JMenuItem closeAllHSIMenuItem;
    private javax.swing.JMenuItem closeAllRatioMenuItem;
    private javax.swing.JMenuItem closeAllSumMenuItem;
    private javax.swing.JMenu closeMenu;
    private javax.swing.JMenuItem compositeMenuItem;
    public javax.swing.JMenu correctionsMenu;
    private javax.swing.JMenuItem docButton;
    private javax.swing.JLabel dragDropMessagejLabel1;
    private javax.swing.JLabel dragDropMessagejLabel2;
    private javax.swing.JLabel dragDropMessagejLabel3;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem emptyTestMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem exportPNGjMenuItem;
    private javax.swing.JMenuItem exportQVisMenuItem;
    private javax.swing.JMenu exportjMenu;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem findMosaic;
    private javax.swing.JMenuItem findStackFile;
    private javax.swing.JMenuItem genStackMenuItem;
    private javax.swing.JMenuItem generateReportMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem imageNotesMenuItem;
    private javax.swing.JMenuItem importIMListMenuItem;
    private javax.swing.JMenuItem insertPicFrame;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JMenuItem openMyNotes_jMenuItem;
    private javax.swing.JMenuItem openNewDraw;
    private javax.swing.JMenuItem openNewImpress;
    private javax.swing.JMenuItem openNewMenuItem;
    private javax.swing.JMenuItem openNewWriter;
    private javax.swing.JMenuItem openNextMenuItem;
    private javax.swing.JButton openPrefsjButton1;
    private javax.swing.JMenuItem preferencesMenuItem;
    private javax.swing.JMenuItem restoreMimsMenuItem;
    private javax.swing.JMenuItem roiManagerMenuItem;
    private javax.swing.JMenuItem sampleDataButton;
    private javax.swing.JMenuItem saveMIMSjMenuItem;
    public javax.swing.JButton stopButton;
    private javax.swing.JMenuItem sumAllMenuItem;
    private javax.swing.JMenu testingMenu;
    private javax.swing.JMenuItem tileWindowsMenuItem;
    private javax.swing.JMenu utilitiesMenu;
    private javax.swing.JMenu viewMenu;
    // End of variables declaration//GEN-END:variables
}

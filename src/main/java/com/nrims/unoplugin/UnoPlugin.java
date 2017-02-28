/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.unoplugin;

/**
 *
 * @author wang2
 */

import com.sun.star.accessibility.AccessibleEventObject;
import ij.*;
import ooo.connector.BootstrapSocketConnector;
import ij.plugin.*;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import com.sun.star.accessibility.AccessibleRole;
import com.sun.star.awt.Point;
import com.sun.star.awt.Size;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.drawing.XShape;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.graphic.XGraphic;
import com.sun.star.graphic.XGraphicProvider;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.WrapTextMode;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.uno.XComponentContext;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XNameAccess;
import com.sun.star.lib.uno.adapter.ByteArrayToXInputStreamAdapter;
import com.sun.star.container.XNamed;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.accessibility.XAccessible;
import com.sun.star.accessibility.XAccessibleComponent;
import com.sun.star.accessibility.XAccessibleContext;
import com.sun.star.accessibility.XAccessibleEventListener;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XUnitConversion;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyChangeEvent;
import com.sun.star.beans.XPropertyChangeListener;
import com.sun.star.beans.XPropertySet;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.XIndexAccess;
import com.sun.star.document.DocumentEvent;
import com.sun.star.document.EventObject;
import com.sun.star.drawing.FillStyle;
import com.sun.star.drawing.XDrawPage;
import com.sun.star.drawing.XDrawPagesSupplier;
import com.sun.star.drawing.XShapeGroup;
import com.sun.star.drawing.XShapeGrouper;
import com.sun.star.drawing.XShapes;
import com.sun.star.embed.EmbedStates;
import com.sun.star.embed.XEmbeddedObject;
import com.sun.star.frame.FrameActionEvent;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFrameActionListener;
import com.sun.star.frame.XModel;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.NoSupportException;
import com.sun.star.lang.XEventListener;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XTextEmbeddedObjectsSupplier;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.Any;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;
import com.sun.star.util.MeasureUnit;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *
 * @author wang2
 */
public class UnoPlugin implements PlugIn{

    private XComponentContext context;
    private static XComponentContext localContext;
    private static String oooExeFolder;
    private static String OS; 
    private XMultiComponentFactory xMCF;
    private UnoPluginWindow unoPluginWindow;
    private ArrayList<ImageListenerPair> pairs = new ArrayList<ImageListenerPair>();
    public boolean dropToResize;
    public boolean fitToWindow;
    public boolean autoTile;
    private boolean setupByMims;
    private Thread t;
    private ArrayList<Integer> mimsImages = new ArrayList<Integer>();
    public static UnoPlugin unoPlugin;
    
    // DJ: 11/10/2014
    private static String previousEvent = "";
    //private static StringBuffer sSaveUrl = new StringBuffer("/nrims/home3/djsia/Desktop/backupTestFolder/");
    static boolean docChangesFlag;
    
    static NotesSaver notesSaver = null; //new NotesSaver(currentDocument, true, exec); 
    static Thread notesSaverThread = null;
    static final int SLEEP_TIME = 15;  // seconds 
    static boolean isNotesFileUnlocked = false;
    static String NotesPath = "";
    static boolean notesOpenedBefore = false;
    static String notesFileName = "";
    
    
    public UnoPlugin(){
        UnoPlugin.unoPlugin = this;
        dropToResize = true;
        fitToWindow = true;
        autoTile = true;
        String pluginPath = IJ.getDirectory("macros");
        File file = new File(pluginPath + "/dragdrop_tools.fiji.ijm");
        if (file.exists()) {
            IJ.run("Install...", "install=" + pluginPath + "/dragdrop_tools.fiji.ijm");
        } else {
            IJ.error("Error: dragdrop_tools.fiji.ijm does not exist. Please try updating.");
        }
        
        /*
         //DJ
         try {
             File notes = new File(NotesPath);
             org.apache.commons.io.FileUtils.touch(notes);
             isNotesFileUnlocked = true;

             System.out.println("Const 1 : notes file is unlocked");

             if (notesOpenedBefore == false) {
                 openDoc(NotesPath);
                 notesOpenedBefore = true;
             }

         } catch (java.io.IOException e) {
             isNotesFileUnlocked = false;
             System.out.println("Const 1 : notes file is locked");
         }
        */
        
        
        t = new Thread(new ListenerAdder());
        t.start();

    }
    public UnoPlugin(boolean mims){
        UnoPlugin.unoPlugin = this;
        setupByMims = mims;
        dropToResize = true;
        fitToWindow = true;
        autoTile = true;
        
 //   This shows how to list all JVM properties          
//          java.util.Properties props = System.getProperties();         
//          java.util.Enumeration e = props.propertyNames();
//          System.out.println("JVM Properties:");
//          while (e.hasMoreElements()) {
//             String k = (String) e.nextElement();
//             String v = props.getProperty(k);
//             System.out.println("   "+k+" = "+v);
//          }
      
        // trivial change for testing branching.
        
        OS = System.getProperty("os.name").toLowerCase();
        //We need to check whether or not the OS is a Mac here because on Macs, the juh.jar (the Java UNO Helper)
        //is not located in the same place as the program, so we need to specify where the program is
        if (OS.indexOf("mac") >= 0) {
             // do nothing on a Mac, because the path to the libreOffice executables is set elsewhere 
             // in this class.
            oooExeFolder = "/Applications/LibreOffice.app/Contents/MacOS";
        } else {
            try {
                addLibraryPath("/usr/lib/libreoffice/program/");
                // For future work, possibly:  Fiji does some of its own library path manipulation that we will
                // have to take into account.  The addLibraryPath method is a bit of a hack that seems to be
                // the best option Java developers have to add paths after launch of the VM, though nobody
                // seems to like it.   On both the Mac and Linux, this code will fail if LibreOffice is installed 
                // anywhere but the hard-coding paths specified above.
            } catch (Exception ee) {
                System.out.println("Unable to add libreoffice path in UnoPlugin.");
            } 
            try {
                localContext = Bootstrap.bootstrap();
            } catch (BootstrapException be) {
                System.out.println("BootstrapException in constructor of UnoPlugin");
            }
        }
        
        


        /*
        //DJ
        try {
            File notes = new File(NotesPath);
             org.apache.commons.io.FileUtils.touch(notes);
            isNotesFileUnlocked = true;
            
            System.out.println("Const 2 : notes file is unlocked");
            if(notesOpenedBefore == false){
                openDoc(NotesPath);
                notesOpenedBefore = true;
            }
            
        } catch (java.io.IOException e) {
            isNotesFileUnlocked = false;
            System.out.println("Const 2 : notes file is locked");
        }
        */

        
        t = new Thread(new ListenerAdder());
        t.start();
    }
    
    
    /**
    * Adds the specified path to the java library path
    *
    * @param pathToAdd the path to add
    * @throws Exception
    */
    public static void addLibraryPath(String pathToAdd) throws Exception{
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        //get array of paths
        final String[] paths = (String[])usrPathsField.get(null);

        //check if the path to add is already present
        for(String path : paths) {
            if(path.equals(pathToAdd)) {
                return;
            }
        }

        //add the new path
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length-1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }


    public static UnoPlugin getInstance(){
        return (UnoPlugin)unoPlugin;
    }
    public void addMimsImage(ImagePlus image){
        mimsImages.add(image.getID());
    }
    @Override
    public void run(String arg){
        unoPluginWindow = new UnoPluginWindow("Libreoffice DragDrop", this);
        unoPluginWindow.setVisible(true);
        WindowAdapter windowAdapter = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                if (!setupByMims){
                    t.interrupt();
                    destroyAllListeners();
                    
                    String pluginPath = IJ.getDirectory("macros");
                    IJ.run("Install...", "install=" + pluginPath + "/StartupMacros.fiji.ijm");
                    unoPluginWindow = null;
                }
            }
        };
        unoPluginWindow.addWindowListener(windowAdapter);
        unoPluginWindow.show();
                 
    }
    /**
     * Class to pair image and a listener together
     */
    public class ImageListenerPair{
        ImagePlus img;
        MouseListener ml;
        public ImageListenerPair(ImagePlus img, MouseListener ml){
            this.img = img;
            this.ml = ml;
            img.getWindow().getCanvas().addMouseListener(this.ml);
        }
        public void destroy(){
            if (img != null){
                if (img.getWindow() != null){
                    if (img.getWindow().getCanvas() != null){
                        img.getWindow().getCanvas().removeMouseListener(ml);
                    }
                }
            }
        }
    }
    /**
     * Thread to run in background attaching listeners to any new images;
     */
    public class ListenerAdder implements Runnable{
        public void run() {
            int count = ij.WindowManager.getImageCount();
            for (;;) {
                try {
                    if (ij.WindowManager.getImageCount() != count) {
                        count = ij.WindowManager.getImageCount();
                        destroyAllListeners();
                        setListeners();
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
    /**
     * Implementation of MouseListener to trigger image drop into Libreoffice
     */
    public class dragListener implements MouseListener{
        ImagePlus img;
        public dragListener(ImagePlus img){
            this.img = img;
        }
        @Override
        public void mousePressed(MouseEvent e) {
	}

        @Override
	public void mouseReleased(MouseEvent e) {
            if (IJ.getToolName().equals("Drag To Writer tool") && !mimsImages.contains(img.getID())) {
                dropImage(getScreenCaptureCurrentImage(), img.getTitle(), img.getTitle(), img.getTitle());
            }
	}
        @Override
        public void mouseExited(MouseEvent e) {}
        @Override
	public void mouseClicked(MouseEvent e) {}	
        @Override
	public void mouseEntered(MouseEvent e) {}
    }
    /**
     * Add listener to all non-OpenMims images
     */
    public void setListeners(){
        int[] ids = ij.WindowManager.getIDList();
        if (ids != null) {
            for (int i = 0; i < ids.length; i++) {
                ImagePlus img = ij.WindowManager.getImage(ids[i]);
                if (!mimsImages.contains(img.getID())) {
                    dragListener dl = new dragListener(img);
                    pairs.add(new ImageListenerPair(img, dl));
                }
            }
        }
    }
    /**
     * Delete all listener attacked to all images
     */
    public void destroyAllListeners(){
        for (int i = 0; i < pairs.size(); i++){
            pairs.get(i).destroy();
        }
        pairs = new ArrayList<ImageListenerPair>();
    }
    /**
     * Gets a screen capture for the current image.
     *
     * @return the AWT Image.
     */
    public static Image getScreenCaptureCurrentImage() {
        ImagePlus imp = ij.WindowManager.getCurrentImage();
        final ImageWindow win = imp.getWindow();
        if (win == null) {
            return null;
        }
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        java.awt.Point loc = win.getLocation();
        ImageCanvas ic = win.getCanvas();
        ic.update(ic.getGraphics());

        Rectangle bounds = ic.getBounds();
        loc.x += bounds.x;
        loc.y += bounds.y;
        Rectangle r = new Rectangle(loc.x, loc.y, bounds.width, bounds.height);
        Robot robot;
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
     * Method called to get current LibreOffice document. Gets currently opened
     * document, in the form of a XComponent
     *
     * @return XComponent of currently opened document, null if none is open
     */
    private XComponent getCurrentDocument() {
        try {
            String OS = System.getProperty("os.name").toLowerCase();
            //We need to check whether or not the OS is a Mac here because on Macs, the juh.jar (the Java UNO Helper)
            //is not located in the same place as the program, so we need to specify where the program is
            if (OS.indexOf("mac") >= 0) {
                String oooExeFolder = "/Applications/LibreOffice.app/Contents/MacOS";
                context = BootstrapSocketConnector.bootstrap(oooExeFolder);
            } else {
                context = Bootstrap.bootstrap();
            }
            xMCF = context.getServiceManager();
            Object oDesktop = xMCF.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", context);
            XDesktop desktop = (com.sun.star.frame.XDesktop) UnoRuntime.queryInterface(
                    com.sun.star.frame.XDesktop.class, oDesktop);
            //Get the document with focus here
            XComponent currentDocument = desktop.getCurrentComponent();
            
            return currentDocument;
        } catch (Exception e) {
            System.out.println("Failure to connect");
            e.printStackTrace(System.err);
        }
        return null;
    }
    
    
     private boolean isImpress(XComponent xComponent){
        XModel xModel = (XModel) UnoRuntime.queryInterface(XModel.class, xComponent);
        XServiceInfo xServiceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, xModel);
        if (xServiceInfo.supportsService("com.sun.star.presentation.PresentationDocument"))
            return true;
        else return false;
    }
     
     
     public void setDocChangesFlag(boolean flag){
         this.docChangesFlag = flag;
     }
     public boolean getDocChangesFlag(){
         return this.docChangesFlag;
     }
     
     public static void setNotesPath(String notesPath){
         NotesPath = notesPath;
     }
     
     public static void setOpenStatus(String notesPath){       
         //DJ
         NotesPath = notesPath;
         try {
             File notes = new File(NotesPath);
             org.apache.commons.io.FileUtils.touch(notes);
             isNotesFileUnlocked = true;

             System.out.println("notes file is unlocked");

            // if (notesOpenedBefore == true) {
             //    openDoc(NotesPath);
             //    notesOpenedBefore = true;
           //  }

         } catch (java.io.IOException e) {
             isNotesFileUnlocked = false;
             System.out.println("notes file is locked");
         }
         
       //  if (isNotesFileUnlocked == true) {
       //          openDoc(NotesPath);
       //          notesOpenedBefore = true;
       //      }
         
     }
     
     //DJ: 10/24/2014
    /**
     * Opens a writer document that already exists.
     *
     * @return true on success, false otherwise
     */
    public static boolean openDoc(String notesFilepath) {
        
        if(notesFilepath.isEmpty()){
            System.out.println("Notes File Path is Empty");
            return false;
        }
        
        notesFileName = notesFilepath.substring(notesFilepath.lastIndexOf('/')+1, notesFilepath.indexOf(".odt"));
        
        System.out.println("notes file name is :" + notesFileName);
        
        
       // System.out.println("My Note's Path Is: " + notesFilepath);
        try {
            XComponentContext localContext;
            String OS = System.getProperty("os.name").toLowerCase();
            //We need to check whether or not the OS is a Mac here because on Macs, the juh.jar (the Java UNO Helper)
            //is not located in the same place as the program, so we need to specify where the program is
            if (OS.indexOf("mac") >= 0) {
                String oooExeFolder = "/Applications/LibreOffice.app/Contents/MacOS";
                localContext = BootstrapSocketConnector.bootstrap(oooExeFolder);
            } else {
                localContext = Bootstrap.bootstrap();
            }
            XMultiComponentFactory xMCF = localContext.getServiceManager();
            Object oDesktop = xMCF.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", localContext);
            XDesktop desktop = (com.sun.star.frame.XDesktop) UnoRuntime.queryInterface(
                    com.sun.star.frame.XDesktop.class, oDesktop);

            
            XComponentLoader xComponentLoader = (XComponentLoader) UnoRuntime.queryInterface(
                    XComponentLoader.class, desktop);
            PropertyValue[] loadProps = new PropertyValue[0];
            
           
            // DJ: "_default" argument checks if the document is already opened.
            // if it is, it pushes it to be visible upfront
            // if it is not already opened, it just opens it using the provide url (first argument)
            
           
          //  if(notesOpenedBefore){
          //      xComponentLoader.loadComponentFromURL("file://"+notesFilepath, "_default", 0, loadProps);
         //       return true;
         //   }
                
            
            //XComponent currentDocument = xComponentLoader.loadComponentFromURL("file://"+notesFilepath, "_blank", 0, loadProps);
            final XComponent currentDocument = xComponentLoader.loadComponentFromURL("file://"+notesFilepath, "_default", 0, loadProps);
            
           
            
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, currentDocument);
            
           
            
            
            // check if the backup directory exists,
            // if it does not, we create a new one
            
            //get the parents directory where the notes file is located
            String notesParentsPath = notesFilepath.substring(0, notesFilepath.lastIndexOf('/')+1);
            //System.out.println("Parent directory is: " + notesParentsDirectory);
            
            // we make a hidden directory there.
            File backupDirectory = new File(notesParentsPath+".NotesBackupsFolder/");
            
            if(!backupDirectory.exists()){
                try{
                     backupDirectory.mkdir();
                } catch(SecurityException se) {
                    System.out.println(se);
                }
            }
            
            final StringBuffer sSaveUrl = new StringBuffer(backupDirectory.getAbsolutePath()+"/");
            
            System.out.println("NotesBackupsFolder ==> " + sSaveUrl.toString());
            final String notesPath = notesFilepath;
            
            //======================================================================================
            /*
            String[] allServives = xMCF.getAvailableServiceNames();  
            System.out.println("========== Service Names: ================");
            for(int idx=0; idx<allServives.length ; idx++){
                System.out.println(allServives[idx]);
            }
            System.out.println("==========================================");
            */
       
            // Step1 : We make a system file copy to  the notes' file the first time we open it.
         //   com.sun.star.beans.PropertyValue[] propertyValue =
         //       new com.sun.star.beans.PropertyValue[1];
           // propertyValue[0] = new com.sun.star.beans.PropertyValue();
            //propertyValue[0].Name = "Hidden";
            //propertyValue[0].Value = new Boolean(true);
            
            com.sun.star.frame.XStorable xStorable =
                UnoRuntime.queryInterface(
                com.sun.star.frame.XStorable.class, currentDocument );
            
            //StringBuffer sSaveUrl = new StringBuffer("file://");
            //StringBuffer sSaveUrl = new StringBuffer("");
            //sSaveUrl.append("/nrims/home3/djsia/Desktop/backupTestFolder/");
            
            com.sun.star.beans.PropertyValue[] propertyValue = new com.sun.star.beans.PropertyValue[1];
            propertyValue[0] = new com.sun.star.beans.PropertyValue();
            propertyValue[0].Name = "Hidden";
            propertyValue[0].Value = false;
            //propertyValue[1] = new com.sun.star.beans.PropertyValue();
            //propertyValue[1].Name = "FilterName";
            //propertyValue[1].Value = "StarOffice XML (Writer)";
            //propertyValue[1].Value = "StarWriter 4.0";
            
            
            // we make a system copy to the notes files when we first open it.
            DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
            java.util.Calendar cal = java.util.Calendar.getInstance();
            System.out.println(dateFormat.format(cal.getTime())); //2014/08/06 16:00:22
            
            File notesFile = new File(notesFilepath);
            String destinFilePath = sSaveUrl.toString() + notesFileName + "_"+dateFormat.format(cal.getTime())+ "_copy" + ".odt";
            File destFile = new File(destinFilePath);
            
            //System.out.println("notesFile: " + notesFile.getAbsolutePath());
            //System.out.println("destinFile: " + destFile.getAbsolutePath());
            
            org.apache.commons.io.FileUtils.copyFile(notesFile, destFile, false);
            
            /*
            try {
                notesFilepath = "file://" + sSaveUrl.toString()+"notes_"+dateFormat.format(cal.getTime())+".odt";
                //notesFilepath = sSaveUrl.toString()+"notes_"+ dateFormat.format(cal.getTime()) +".odt";
                System.out.println("initial backup file is: " + notesFilepath);
                xStorable.storeToURL(notesFilepath, propertyValue );
                
               // xStorable.storeToURL(OS, loadProps);
                
                notesCountTracker++;
            }catch (IOException e) {
                e.printStackTrace();
            }
            */
            
            //xStorable.storeToURL( sSaveUrl.toString(), propertyValue );

           System.out.println("\nINITIAL COPY SAVED ==>  " + notesFilepath);
           
           
            
            // newly added lines
            notesSaver = new NotesSaver(currentDocument, true, sSaveUrl); 
            notesSaverThread = new Thread(notesSaver);
            notesSaverThread.start();
            
            
            
            //====================================================================================
            
            
            
            //====================================================================================
            
            

            com.sun.star.document.XDocumentEventBroadcaster broadCaster  = (com.sun.star.document.XDocumentEventBroadcaster)
            UnoRuntime.queryInterface(com.sun.star.document.XDocumentEventBroadcaster.class, currentDocument); 
            // call to broadCaster.addDocumentEventListener(listner) is
            // below/after documentEventOccured overide
            
           
            
            com.sun.star.document.XDocumentEventListener listner = new com.sun.star.document.XDocumentEventListener() {
                //NotesSaver notesSaver = null; //= new NotesSaver(currentDocument, true, exec); 
                //Thread notesSaverThread = null;

                @Override
                public void documentEventOccured(com.sun.star.document.DocumentEvent de) {
                    
                    System.out.println(" ===============================>> EVENT IS : " + de.EventName);
                    
                    // at this level, we're inside an OLE object
                    
                    if(de.EventName.equals("OnUnfocus") ){
                        
                        
                        XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                                XTextDocument.class, currentDocument);

                        XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                                XMultiServiceFactory.class, xTextDocument);

                        XAccessible mXRoot = makeRoot(xMSF, xTextDocument);
                        XAccessibleContext xAccessibleRoot = mXRoot.getAccessibleContext();

                        //scope: xTextDocument -> ScrollPane -> Document
                        //get the scroll pane object
                        XAccessibleContext xAccessibleContext = getNextContext(xAccessibleRoot, 0);

                        //get the document object
                        xAccessibleContext = getNextContext(xAccessibleContext, 0);

                        int numChildren = xAccessibleContext.getAccessibleChildCount();

                        System.out.println("number of children = " + numChildren);

                        //loop through all the children of the document and find the text frames
                        for (int ii = 0; ii < numChildren; ii++) {
                            XAccessibleContext xChildAccessibleContext = getNextContext(xAccessibleContext, ii);

                            if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.EMBEDDED_OBJECT) {
                                
                                
                                System.out.println("Description: " + xChildAccessibleContext.getAccessibleDescription());
                                System.out.println("Name: " + xChildAccessibleContext.getAccessibleName());
                                
                         
                                if (xChildAccessibleContext.getAccessibleName().isEmpty() == false) {
                                    XComponent ole = getOLEE(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                                 
                                    com.sun.star.document.XDocumentEventBroadcaster bC = (com.sun.star.document.XDocumentEventBroadcaster) UnoRuntime.queryInterface(com.sun.star.document.XDocumentEventBroadcaster.class, ole);

                                    com.sun.star.document.XDocumentEventListener listner = new com.sun.star.document.XDocumentEventListener() {
                                        @Override
                                        public void documentEventOccured(DocumentEvent de) {

                                            if (de.EventName.equals("OnModifyChanged")) {
                                                System.out.println("Within OLE : " + de.EventName);
                                                docChangesFlag = true;
                                            } else if (de.EventName.equals("OnVisAreaChanged")) {
                                                System.out.println("Within OLE : " + de.EventName);
                                                docChangesFlag = true;
                                            }


                                            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                        }

                                        @Override
                                        public void disposing(com.sun.star.lang.EventObject eo) {
                                            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                        }
                                    };


                                    bC.addDocumentEventListener(listner);
                                }                       

                            }

                        }

                    }

                    
                    // DJ: the timer and scheduler were used for testing purposes only.
                    //java.util.Timer timer;// = new java.util.Timer();
                    //java.util.concurrent.ScheduledExecutorService exec;// = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
                    
                    // The case where we save the system file before the save action is performed. 
                    if(de.EventName.equals("OnSave") ){
                        
                        
                        previousEvent = "OnSave";
                        //System.out.println(" ===============================>> EVENT IS : " + de.EventName);
                        
                        //notesSaver.setFlag(false);
                        docChangesFlag = false;
                        System.out.println("docChangesFlagg = " + docChangesFlag);
                        
                         // we save a SYSTEM file copy before we perform the save because if it happens
                        // that we save a "bad ole", we at least have the most updated clean copy saved as a backup.
                        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        String backupFilePath = sSaveUrl.toString() + notesFileName + "_" + dateFormat.format(cal.getTime()) + "_copy" +  ".odt";
                        
                        Thread copierThread = new Thread(new NotesCopier(notesPath, backupFilePath));
                        copierThread.start();
                        
                        /*
                        previousEvent = "OnSave";
                       // System.out.println("SAVE EVENT");
                        
                       if(notesSaver != null && notesSaverThread != null){
                            
                            System.out.println("Stopping the thread...");
                            notesSaver.setFlag(false); // stops the thread.
                            notesSaverThread.setPriority(Thread.MAX_PRIORITY);
                            System.out.println("Thread stopped.");
                        }
                            
                        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        String backupFilePath = sSaveUrl.toString() + "notes_copy_" + dateFormat.format(cal.getTime()) + ".odt";

                        // we save a SYSTEM file copy before we perform the save because if it happens
                        // that we save a "bad ole", we at least have the most updated clean copy saved as a backup.
                        Thread copierThread = new Thread(new NotesCopier(notesPath, backupFilePath));
                        copierThread.start();
                        */ 
                        
                    }   
                    
                    else if (de.EventName.equals("OnSaveDone")){
                        previousEvent = "OnSaveDone";
                        docChangesFlag = false;
                        
                    }
                    
                    else if (de.EventName.equals("OnCopyToDone")){
                        docChangesFlag = false;
                        previousEvent = "OnCopyToDone";
                        //System.out.println("docChangesFlag = " + docChangesFlag);
                        
                        /*
                        DocumentEvent fakeDocEvent = new DocumentEvent(de.Source, "OnSaveDone", de.ViewController, de.Supplement);
                        System.out.println("fake event name is: " + fakeDocEvent.EventName);
                        this.documentEventOccured(fakeDocEvent);
                        */
                        
                    }
                    
                    else if (de.EventName.equals("OnTitleChanged")){
                        previousEvent = "OnTitleChanged";
                    }
                    
                    else if (de.EventName.equals("OnLayoutFinished")  && previousEvent.equals("OnSaveDone") == false ){
                        docChangesFlag = true;
                        previousEvent = "OnLayoutFinished";
                        System.out.println("docChangesFlag = " + docChangesFlag);
                    }
                    
                    else if(de.EventName.equals("OnModifyChanged") /* || de.EventName.equals("OnLayoutFinished")  /*&& !previousEvent.equals("OnSave")*/){
                        
                        
                        
                        if(previousEvent.equals("OnSave") || previousEvent.equals("OnTitleChanged") ){
                          //  System.out.println("\tPrevious event was a SAVE EVENT");
                            //previousEvent = "OnModifyChanged";
                        } else{
                            //System.out.println(" ===============================>> EVENT IS : " + de.EventName);
                            //notesSaver.setFlag(true);
                            docChangesFlag = true;
                            System.out.println("docChangesFlag = " + docChangesFlag + "inside ONMODIDYCHANGED/ONLAYOUTFINISHED/ELSE/PREV=" + previousEvent);
                        }
                        previousEvent = "OnModifyChanged";
                        
                    }
                    
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public void disposing(com.sun.star.lang.EventObject eo) {
                    System.out.println("document is about to be closed.");
                    notesOpenedBefore = false;
                    notesSaver.setFlag(false);
                    System.out.println("notes Opened = " + notesOpenedBefore);
                    /*
                    try {
                        notesSaverThread.join();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(UnoPlugin.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    */
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            };
            
            
            
            broadCaster.addDocumentEventListener(listner);

            
            
            /*
            com.sun.star.beans.PropertyValue[] propertyValue =
                new com.sun.star.beans.PropertyValue[1];
            propertyValue[0] = new com.sun.star.beans.PropertyValue();
            propertyValue[0].Name = "Hidden";
            propertyValue[0].Value = new Boolean(true);
            
            com.sun.star.frame.XStorable xStorable =
                UnoRuntime.queryInterface(
                com.sun.star.frame.XStorable.class, currentDocument );
            
            //StringBuffer sSaveUrl = new StringBuffer("file://");
            StringBuffer sSaveUrl = new StringBuffer("");
            sSaveUrl.append("/nrims/home3/djsia/Desktop/backupTestFolder/");
            */
            
            
            
            
            /*
            StringBuffer sSaveUrl0 = new StringBuffer("file://");
            sSaveUrl0.append("/nrims/home3/djsia/Desktop/backupTestFolder/notes0.odt");
            
            StringBuffer sSaveUrl1 = new StringBuffer("file://");
            sSaveUrl1.append("/nrims/home3/djsia/Desktop/backupTestFolder/notes1.odt");
            
            StringBuffer sSaveUrl2 = new StringBuffer("file://");
            sSaveUrl2.append("/nrims/home3/djsia/Desktop/backupTestFolder/notes2.odt");
            
            StringBuffer sSaveUrl3 = new StringBuffer("file://");
            sSaveUrl3.append("/nrims/home3/djsia/Desktop/backupTestFolder/notes3.odt");
            */
            
            /*
            propertyValue = new com.sun.star.beans.PropertyValue[ 2 ];
            propertyValue[0] = new com.sun.star.beans.PropertyValue();
            propertyValue[0].Name = "Overwrite";
            propertyValue[0].Value = new Boolean(true);
            propertyValue[1] = new com.sun.star.beans.PropertyValue();
            propertyValue[1].Name = "FilterName";
            propertyValue[1].Value = "StarOffice XML (Writer)";
            */
            
     //       try {
     //           
      //          Thread t = new Thread();
      //          int count = 0;
      //          while (true && count >=0 && count <= 9 ){
                    
        //            xStorable.storeToURL("file://" + sSaveUrl.toString()+"notes"+count+".odt", propertyValue );
                    
                    
      //              String backupFilePath = sSaveUrl.toString()+"notes"+count+".odt";
       //             String previousBackupFilePath;
      //              
      //              if(count == 0){
     //                   previousBackupFilePath = sSaveUrl.toString()+"notes"+9+".odt";
      //                  
       //             } else{
     //                   previousBackupFilePath = sSaveUrl.toString()+"notes"+(count-1)+".odt";
    //                }
  
    //                File backUpFile = new File(backupFilePath);
     //               File previousBackupFile = new File(previousBackupFilePath);
                    
                    /*
                    
                    byte[] aFile = null;
                    if(backUpFile.exists()){
                        System.out.println("back up file length = 0 " + backUpFile.length());
                        java.io.FileInputStream fileInputStream = null;
                        aFile = new byte[(int) backUpFile.length()];
                        try {
                            //convert file into array of bytes
                            fileInputStream = new java.io.FileInputStream(backUpFile);
                            fileInputStream.read(aFile);
                            fileInputStream.close();

                         //   for (int i = 0; i < aFile.length; i++) {
                         //       System.out.print((char) aFile[i]);
                         //   }

                            System.out.println("Done");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                    }
                    byte[] bFile = null;
                    if(previousBackupFile.exists()){
                        System.out.println("\n\nprevious back up file length = " + previousBackupFile.length());
                        java.io.FileInputStream fileInputStream = null;
                        bFile = new byte[(int) previousBackupFile.length()];
                        try {
                            //convert file into array of bytes
                            fileInputStream = new java.io.FileInputStream(previousBackupFile);
                            fileInputStream.read(bFile);
                            fileInputStream.close();

                            //for (int i = 0; i < bFile.length; i++) {
                            //    System.out.print((char) bFile[i]);
                           // }

                            System.out.println("Done");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    
                    if(aFile != null && bFile != null && aFile.length == bFile.length){
                        
                        System.out.println("Equal, countt is: " + aFile.length);
                        int countt = 0;
                        for(int i=0 ; i<aFile.length ; i++){
                            
                            if(aFile[i] == bFile[i]){
                                countt++;
                            }
                        }
                        System.out.println("count is = " + countt);
                    }
                    */
                    
    //                boolean AreFilesEqual;
   //                 if(previousBackupFile.exists()){
     //                   AreFilesEqual =
     //                           org.apache.commons.io.FileUtils.contentEquals(
     //                                           backUpFile, 
       //                                         previousBackupFile);
      //              }
      //              else{
      //                  AreFilesEqual = false;
       //             }
                    
                //    if(AreFilesEqual == true){
                //        backUpFile.delete();
                //        Thread.sleep(10000); // 10 seconds
   
                //    }
                //    else if(AreFilesEqual == false){
                //        if (count == 9) {
                 //           count = -1;
                //        }

                //        count++;
                 //       Thread.sleep(10); // 10 seconds = 10000
                //    } 
      //          }
                
     //       }catch (InterruptedException e) {
        //        e.printStackTrace();
     //       }
            
            //xStorable.storeToURL( sSaveUrl.toString(), propertyValue );

    //       System.out.println("\nDocument \"" + "file://"+notesFilepath + "\" saved under \"" +
     //                          sSaveUrl + "\"\n");
    //        
            notesOpenedBefore = true;
            return true;
        } catch (Exception e) {
            System.out.println("Failure to create new document");
            return false;
        }
    }
    /**
     * 
     */
    /**
     * Open a new writer document for taking notes.
     *
     * @return true on success, false otherwise
     */
    public static boolean newDoc() {
        try {
            if (OS.indexOf("mac") >= 0) {
                localContext = BootstrapSocketConnector.bootstrap(oooExeFolder);
            } else {
                localContext = Bootstrap.bootstrap();
            }

            XMultiComponentFactory xMCF = localContext.getServiceManager();
            Object oDesktop = xMCF.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", localContext);
            XDesktop desktop = (com.sun.star.frame.XDesktop) UnoRuntime.queryInterface(
                    com.sun.star.frame.XDesktop.class, oDesktop);
            XComponentLoader xComponentLoader = (XComponentLoader) UnoRuntime.queryInterface(
                    XComponentLoader.class, desktop);
            PropertyValue[] loadProps = new PropertyValue[0];
            XComponent currentDocument = xComponentLoader.loadComponentFromURL("private:factory/swriter", "_blank", 0, loadProps);
            return true;
        } catch (Exception e) {
            System.out.println("Failure to create new document");
            return false;
        }
    }
    public static boolean newDraw() {
        try {
            if (OS.indexOf("mac") >= 0) {
                localContext = BootstrapSocketConnector.bootstrap(oooExeFolder);
            } else {
                localContext = Bootstrap.bootstrap();
            }
            
            XMultiComponentFactory xMCF = localContext.getServiceManager();
            Object oDesktop = xMCF.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", localContext);
            XDesktop desktop = (com.sun.star.frame.XDesktop) UnoRuntime.queryInterface(
                    com.sun.star.frame.XDesktop.class, oDesktop);
            XComponentLoader xComponentLoader = (XComponentLoader) UnoRuntime.queryInterface(
                    XComponentLoader.class, desktop);
            PropertyValue[] loadProps = new PropertyValue[0];
            XComponent currentDocument = xComponentLoader.loadComponentFromURL("private:factory/sdraw", "_blank", 0, loadProps);
            return true;
        } catch (Exception e) {
            System.out.println("Failure to create new document");
            return false;
        }
    }
    public static boolean newImpress() {
        try {
            if (OS.indexOf("mac") >= 0) {
                localContext = BootstrapSocketConnector.bootstrap(oooExeFolder);
            } else {
                localContext = Bootstrap.bootstrap();
            }
            
            XMultiComponentFactory xMCF = localContext.getServiceManager();
            Object oDesktop = xMCF.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", localContext);
            XDesktop desktop = (com.sun.star.frame.XDesktop) UnoRuntime.queryInterface(
                    com.sun.star.frame.XDesktop.class, oDesktop);
            XComponentLoader xComponentLoader = (XComponentLoader) UnoRuntime.queryInterface(
                    XComponentLoader.class, desktop);
            PropertyValue[] loadProps = new PropertyValue[0];
            XComponent currentDocument = xComponentLoader.loadComponentFromURL("private:factory/simpress", "_blank", 0, loadProps);
            return true;
        } catch (Exception e) {
            System.out.println("Failure to create new document");
            return false;
        }
    }
    /**
     * Insert a new OLE object into the writer document to place images into
     */
    public static void insertEmptyOLEObject(String filename, String path) {
        
        //XComponent xComponent = null;
        try {
            if (OS.indexOf("mac") >= 0) {
                localContext = BootstrapSocketConnector.bootstrap(oooExeFolder);
            } else {
                localContext = Bootstrap.bootstrap();
            }
            
            XMultiComponentFactory xMCF = localContext.getServiceManager();
            Object oDesktop = xMCF.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", localContext);
            XDesktop desktop = (com.sun.star.frame.XDesktop) UnoRuntime.queryInterface(
                    com.sun.star.frame.XDesktop.class, oDesktop);
            XComponent currentDocument = desktop.getCurrentComponent();
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, currentDocument);
            //current document is not a writer
            if (xTextDocument != null) {
                XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                        XMultiServiceFactory.class, xTextDocument);
                XTextContent xt = (XTextContent) UnoRuntime.queryInterface(XTextContent.class,
                        xMSF.createInstance("com.sun.star.text.TextEmbeddedObject"));
                
                XPropertySet xps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xt);
                xps.setPropertyValue("CLSID", "4BAB8970-8A3B-45B3-991c-cbeeac6bd5e3");
               //xps.setPropertyValue("AnchorType", TextContentAnchorType.AS_CHARACTER);

                XModel xModel = (XModel)UnoRuntime.queryInterface(
                XModel.class, currentDocument);
                
                XController xController = xModel.getCurrentController();
                XTextViewCursorSupplier xViewCursorSupplier = (XTextViewCursorSupplier)UnoRuntime.queryInterface(
                XTextViewCursorSupplier.class, xController);

                XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor();
                XTextCursor cursor = xViewCursor;
                XPropertySet xpsCursor = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, cursor);
                XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, cursor);
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                String title = dateFormat.format(date) + "\n";
                if (filename != null && path != null){
                    title+=filename + "\n" + path + "\n";
                }
                xTextDocument.getText().insertString(xTextRange, title, false);
                xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, cursor);
                xTextDocument.getText().insertControlCharacter(xTextRange, com.sun.star.text.ControlCharacter.PARAGRAPH_BREAK, false);
                Point p = xViewCursor.getPosition();
                //xps.setPropertyValue("HoriOrientPosition", new Integer(p.X));
                //xps.setPropertyValue("VertOrientPosition", new Integer(p.Y));
                
                //cursor.gotoEnd(false);
                xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, cursor);
                xTextDocument.getText().insertTextContent(xTextRange, xt, false);
                //set the size of the Draw frame
                com.sun.star.document.XEmbeddedObjectSupplier2 xEOS2 = (com.sun.star.document.XEmbeddedObjectSupplier2) UnoRuntime.queryInterface(com.sun.star.document.XEmbeddedObjectSupplier2.class, xt);
                XEmbeddedObject xEmbeddedObject = xEOS2.getExtendedControlOverEmbeddedObject();
                Size aNewSize = new Size();
                aNewSize.Height = 20500;
                aNewSize.Width = 17250;
                xEmbeddedObject.setVisualAreaSize(xEOS2.getAspect(), aNewSize);
                //xpsCursor.setPropertyValue("BreakType", com.sun.star.style.BreakType.PAGE_AFTER); 
                //xTextDocument.getText().insertControlCharacter(xTextRange, com.sun.star.text.ControlCharacter.PARAGRAPH_BREAK, false);
                
                // DJ: 10/13/2014
                //return ((com.sun.star.document.XEmbeddedObjectSupplier) xEmbeddedObject).getEmbeddedObject();
            }
        } catch (Exception ex) {
            System.out.println("Could not insert OLE object");
            ex.printStackTrace(System.err);
        }
        //return xComponent;
    }
    
    
    
    
    
    //DJ: 10/13/2014:
    // "insertOLEObject" where te size is handled as an argument    /**
    /*
     
    public static XComponent insertEmptyOLEObject(String filename, String path, int width, int height) {
        
        XComponent xComponent = null;
        try {
             
            XComponentContext localContext;
            String OS = System.getProperty("os.name").toLowerCase();
            //We need to check whether or not the OS is a Mac here because on Macs, the juh.jar (the Java UNO Helper)
            //is not located in the same place as the program, so we need to specify where the program is
            if (OS.indexOf("mac") >= 0) {
                String oooExeFolder = "/Applications/LibreOffice.app/Contents/MacOS";
                localContext = BootstrapSocketConnector.bootstrap(oooExeFolder);
            } else {
                localContext = Bootstrap.bootstrap();
            }
            XMultiComponentFactory xMCF = localContext.getServiceManager();
            Object oDesktop = xMCF.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", localContext);
            XDesktop desktop = (com.sun.star.frame.XDesktop) UnoRuntime.queryInterface(
                    com.sun.star.frame.XDesktop.class, oDesktop);
            XComponent currentDocument = desktop.getCurrentComponent();
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, currentDocument);
            //current document is not a writer
            if (xTextDocument != null) {
                XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                        XMultiServiceFactory.class, xTextDocument);
                XTextContent xt = (XTextContent) UnoRuntime.queryInterface(XTextContent.class,
                        xMSF.createInstance("com.sun.star.text.TextEmbeddedObject"));
                XPropertySet xps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xt);
                xps.setPropertyValue("CLSID", "4BAB8970-8A3B-45B3-991c-cbeeac6bd5e3");
               //xps.setPropertyValue("AnchorType", TextContentAnchorType.AS_CHARACTER);

                XModel xModel = (XModel)UnoRuntime.queryInterface(
                XModel.class, currentDocument);
                
                XController xController = xModel.getCurrentController();
                XTextViewCursorSupplier xViewCursorSupplier = (XTextViewCursorSupplier)UnoRuntime.queryInterface(
                XTextViewCursorSupplier.class, xController);

                XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor();
                XTextCursor cursor = xViewCursor;
                XPropertySet xpsCursor = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, cursor);
                XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, cursor);
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                String title = dateFormat.format(date) + "\n";
                if (filename != null && path != null){
                    title+=filename + "\n" + path + "\n";
                }
                xTextDocument.getText().insertString(xTextRange, title, false);
                xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, cursor);
                xTextDocument.getText().insertControlCharacter(xTextRange, com.sun.star.text.ControlCharacter.PARAGRAPH_BREAK, false);
                Point p = xViewCursor.getPosition();
                //xps.setPropertyValue("HoriOrientPosition", new Integer(p.X));
                //xps.setPropertyValue("VertOrientPosition", new Integer(p.Y));
                
                //cursor.gotoEnd(false);
                xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, cursor);
                xTextDocument.getText().insertTextContent(xTextRange, xt, false);
                //set the size of the Draw frame
                com.sun.star.document.XEmbeddedObjectSupplier2 xEOS2 = (com.sun.star.document.XEmbeddedObjectSupplier2) UnoRuntime.queryInterface(com.sun.star.document.XEmbeddedObjectSupplier2.class, xt);
                XEmbeddedObject xEmbeddedObject = xEOS2.getExtendedControlOverEmbeddedObject();
                Size aNewSize = new Size(width, height);
                xEmbeddedObject.setVisualAreaSize(xEOS2.getAspect(), aNewSize);
                //xpsCursor.setPropertyValue("BreakType", com.sun.star.style.BreakType.PAGE_AFTER); 
                //xTextDocument.getText().insertControlCharacter(xTextRange, com.sun.star.text.ControlCharacter.PARAGRAPH_BREAK, false);
                
                // DJ: 10/13/2014
                return ((com.sun.star.document.XEmbeddedObjectSupplier) xEmbeddedObject).getEmbeddedObject();
            }
        } catch (Exception ex) {
            System.out.println("Could not insert OLE object");
            ex.printStackTrace(System.err);
        }
        return xComponent;
    }
    */
    
    //DJ: 10/13/2014:
    // inserts an OLE frame as well as an image at the same time.
    public void insertOLEAndImage(ImageInfo image, String filename, String path, int width, int height,Point pnt, Size z) {
        
        try {
            if (OS.indexOf("mac") >= 0) {
                localContext = BootstrapSocketConnector.bootstrap(oooExeFolder);
            } else {
                localContext = Bootstrap.bootstrap();
            }
            
            XMultiComponentFactory xMCF = localContext.getServiceManager();
            Object oDesktop = xMCF.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", localContext);
            XDesktop desktop = (com.sun.star.frame.XDesktop) UnoRuntime.queryInterface(
                    com.sun.star.frame.XDesktop.class, oDesktop);
            XComponent currentDocument = desktop.getCurrentComponent();
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, currentDocument);
            //current document is not a writer
            if (xTextDocument != null) {
                XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                        XMultiServiceFactory.class, xTextDocument);
                XTextContent xt = (XTextContent) UnoRuntime.queryInterface(XTextContent.class,
                        xMSF.createInstance("com.sun.star.text.TextEmbeddedObject"));
                
                XPropertySet xps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xt);
                xps.setPropertyValue("CLSID", "4BAB8970-8A3B-45B3-991c-cbeeac6bd5e3");
               //xps.setPropertyValue("AnchorType", TextContentAnchorType.AS_CHARACTER);

                XModel xModel = (XModel)UnoRuntime.queryInterface(
                XModel.class, xTextDocument);
                
                XController xController = xModel.getCurrentController();
                XTextViewCursorSupplier xViewCursorSupplier = (XTextViewCursorSupplier)UnoRuntime.queryInterface(
                XTextViewCursorSupplier.class, xController); 

                
                XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor() ; 
                
              
                
                XTextCursor cursor = xViewCursor; 
                
                
                
                //System.out.println("cursor before: (" + xViewCursor.getPosition().X + "," + xViewCursor.getPosition().Y + ")");
                //Dj:10/15/2014
                //Short s = new Short("2");
                
                
                //cursor.gotoStart(false); //works.
                
                //xViewCursor.gotoStart(false);
                //for(int i = 0; i < 2 ; i++){
                //    cursor.goLeft(s, false);
                //    xViewCursor.goLeft(s, false);
                //}
                
                /*
                Point point = xAccessibleComponent.getLocationOnScreen();
                Size size = xAccessibleComponent.getSize();
                java.awt.Point location = MouseInfo.getPointerInfo().getLocation();
                if (point.X + size.Width < location.getX()
                
                */
                
                /*
                //Point pnt1 = ((XTextViewCursor)Cursor.getDefaultCursor()).getPosition(); // throws
                //System.out.println("cursor after:  (" + ((XTextViewCursor)cursor).getPosition().X + "," + ((XTextViewCursor)cursor).getPosition().Y + ")");
                System.out.println("cursor after: (" + xViewCursor.getPosition().X + "," + xViewCursor.getPosition().Y + ")");
                System.out.println("(" +(pnt.X)+","+(pnt.Y)+")");
                System.out.println("mouse locati: (" + MouseInfo.getPointerInfo().getLocation().x + "," + MouseInfo.getPointerInfo().getLocation().y + ")");
                com.sun.star.awt.MouseEvent me = new com.sun.star.awt.MouseEvent();
                System.out.println("new mouse x = " + me.X);
                System.out.println("new mouse y = " + me.Y);
                
                XAccessibleComponent xac = (XAccessibleComponent)UnoRuntime.queryInterface(
                XAccessibleComponent.class, cursor);

                
                if(xac == null)
                    System.out.println("null  yo");
                else 
                    System.out.println("not null  yo");
                
                
                System.out.println("Image description: ");
                System.out.println(image.description);
                */
                
                
                //XTextRange xTextRanges = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, MouseInfo.getPointerInfo());
                //cursor.gotoRange(xTextRanges, false);
                
                
                
                XPropertySet xpsCursor = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, cursor);
                XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, cursor);
                DateFormat dateFormat = new SimpleDateFormat(" yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                String title = dateFormat.format(date) + "\n";
                
                //DJ: 10/15/2014 slightly modified for better visibility.
                title += image.getImageTitle();
                //if (filename != null && path != null){
                //        title+=" " + filename.substring(filename.indexOf(':')+1) + "\n" + path;    
                //}
                //System.out.println("Title is: " + title);
                
                xTextDocument.getText().insertString(xTextRange, title, false);
                
               
                
                // The three following lines to be uncommented:
                //xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, cursor);
                xTextDocument.getText().insertControlCharacter(xTextRange, com.sun.star.text.ControlCharacter.PARAGRAPH_BREAK, false);
                //Point p = xViewCursor.getPosition();
              
                //xps.setPropertyValue("HoriOrientPosition", new Integer(p.X)+5);
                //xps.setPropertyValue("VertOrientPosition", new Integer(p.Y)+5);
                
                //cursor.gotoEnd(true);
                
                // DJ: 10/15/2014 we might test for cursor position here and compare against the mouse position
                //XTextCursor curText = xTextDocument.getText().createTextCursor();
               // curText.gotoEnd(true);
                
                
                xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, cursor);
                xTextDocument.getText().insertTextContent(xTextRange, xt, false);
                //set the size of the Draw frame
                com.sun.star.document.XEmbeddedObjectSupplier2 xEOS2 = (com.sun.star.document.XEmbeddedObjectSupplier2) UnoRuntime.queryInterface(com.sun.star.document.XEmbeddedObjectSupplier2.class, xt);
                XEmbeddedObject xEmbeddedObject = xEOS2.getExtendedControlOverEmbeddedObject();
                Size aNewSize = new Size(width, height);
                xEmbeddedObject.setVisualAreaSize(xEOS2.getAspect(), aNewSize);
                //xpsCursor.setPropertyValue("BreakType", com.sun.star.style.BreakType.PAGE_AFTER); 
                //xTextDocument.getText().insertControlCharacter(xTextRange, com.sun.star.text.ControlCharacter.PARAGRAPH_BREAK, false);
                
                // DJ: 10/15/2014
                //xEmbeddedObject.update();
                XComponent ole = xEOS2.getEmbeddedObject();
                insertIntoOLE(image, currentDocument, ole);
                xTextDocument.getText().insertString(xTextRange, " NOTES: \n\n", false);
                // insertIntoDraw(ole, image);
            }
        } catch (Exception ex) {
            System.out.println("Could not insert OLE object");
            ex.printStackTrace(System.err);
        }
        
    }
    
    

    /**
     * Method to handle dropping images in LibreOffice. If the user drops
     * outside a text frame, nothing happens. If the user drops inside a text
     * frame, and over no images, a new image is inserted into the text frame If
     * the user drops inside a text frame and over an image, the existing image
     * is replaced with the new one, albeit with same size and position
     * @param i java.awt.image to be inserted
     * @param text caption for the image
     * @param title title for the image, under "Description..."
     * @param description description for the image, under "Description..."
     * @return true if succeeded, false if not
     */
    public boolean dropImage(Image i, String text, String title, String description) {
        if (title.equals("")) {
            title = "None";
        }
        if (description.equals("")) {
            description = "None";
        }
        ImageInfo image = new ImageInfo(i, text, title, description);
        XComponent currentDocument = getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        try {
            // Querying for the text interface
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, currentDocument);
            //current document is not a writer
            if (xTextDocument == null) {
                //check if an draw doc
                XDrawPage xDrawPage = getXDrawPage(currentDocument);
                if (xDrawPage != null) {
                    //System.out.println("Current document is a draw");
                    insertIntoDraw(currentDocument, image);
                }
            } else {
                //System.out.println("Current document is a writer");
                insertIntoWriter(image, currentDocument);
            }

        } catch (Exception e) {
            System.out.println("Error reading frames");
            e.printStackTrace(System.err);
            return false;
        }
        return true;
        
       //Original Implementation:
       /* 
        if (title.equals("")) {
            title = "None";
        }
        if (description.equals("")) {
            description = "None";
        }
        ImageInfo image = new ImageInfo(i, text, title, description);
        XComponent currentDocument = getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        try {
            // Querying for the text interface
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, currentDocument);
            //current document is not a writer
            if (xTextDocument == null) {
                //check if an draw doc
                XDrawPage xDrawPage = getXDrawPage(currentDocument);
                if (xDrawPage != null) {
                    //System.out.println("Current document is a draw");
                    insertIntoDraw(currentDocument, image);
                }
            } else {
                //System.out.println("Current document is a writer");
                insertIntoWriter(image, currentDocument);
            }

        } catch (Exception e) {
            System.out.println("Error reading frames");
            e.printStackTrace(System.err);
            return false;
        }
        return true;
        */
    }
    
    
     // DJ: 24/10/2014
     // another version of "dropImages" where a the 4th arg is a array of strings
     /**
     * Method to handle dropping images in LibreOffice. If the user drops
     * outside a text frame, nothing happens. If the user drops inside a text
     * frame, and over no images, a new image is inserted into the text frame If
     * the user drops inside a text frame and over an image, the existing image
     * is replaced with the new one, albeit with same size and position
     * @param i java.awt.image to be inserted
     * @param text caption for the image
     * @param title title for the image, under "Description..."
     * @param description description for the image, under "Description..."
     * @return true if succeeded, false if not
     */
    public boolean dropImage(Image i, String text, String title, String[] descriptions) {
        if (title.equals("")) {
            title = "None";
        }

        ImageInfo image = new ImageInfo(i, text, title, descriptions);
        XComponent currentDocument = getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        try {
            // Querying for the text interface
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, currentDocument);
            //current document is not a writer
            if (xTextDocument == null) {
                //check if an draw doc
                XDrawPage xDrawPage = getXDrawPage(currentDocument);
                if (xDrawPage != null) {
                    //System.out.println("Current document is a draw");
                    insertIntoDraw(currentDocument, image);
                }
            } else {
                //System.out.println("Current document is a writer");
                insertIntoWriter(image, currentDocument);
            }

        } catch (Exception e) {
            System.out.println("Error reading frames");
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }
    
     /**
     * Method to handle dropping graphs. 
     * This method is called when users in OpenMIMS right click a plot and select
     * "Insert in LibreOffice". This will then insert the given plot into the either the closest word or draw document.
     * @param i java.awt.image to be inserted
     * @param text caption for the image
     * @param title title for the image, under "Description..."
     * @param description description for the image, under "Description..."
     * @return true if succeeded, false if not
     */
    public boolean insertGraph(Image i, String text, String title, String description) {
        if (title.equals("")) {
            title = "None";
        }
        if (description.equals("")) {
            description = "None";
        }
        ImageInfo image = new ImageInfo(i, text, title, description);
        XComponent currentDocument = getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        try {
            // Querying for the text interface
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, currentDocument);
            //current document is not a writer
            if (xTextDocument == null) {
                //check if an draw doc
                XDrawPage xDrawPage = getXDrawPage(currentDocument);
                if (xDrawPage != null) {
                    //System.out.println("Current document is a draw");
                    insertClosestDraw(currentDocument, image);
                }
            } else {
                //System.out.println("Current document is a writer");
                insertClosestWriter(image, currentDocument);
            }

        } catch (Exception e) {
            System.out.println("Error reading frames");
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }
    private boolean insertClosestWriter(ImageInfo image, XComponent xComponent) {
                try {
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, xComponent);
            // Querying for the text service factory
            XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xTextDocument);
            XAccessible mXRoot = makeRoot(xMSF, xTextDocument);
            XAccessibleContext xAccessibleRoot = mXRoot.getAccessibleContext();

            //scope: xTextDocument -> ScrollPane -> Document
            //get the scroll pane object
            XAccessibleContext xAccessibleContext = getNextContext(xAccessibleRoot, 0);

            //get the document object
            xAccessibleContext = getNextContext(xAccessibleContext, 0);

            int numChildren = xAccessibleContext.getAccessibleChildCount();
            //loop through all the children of the document and find the text frames
            for (int i = 0; i < numChildren; i++) {
                XAccessibleContext xChildAccessibleContext = getNextContext(xAccessibleContext, i);
                if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.EMBEDDED_OBJECT && withinPage(xChildAccessibleContext)) {
                    //user is over an OLE embedded object
                    XComponent xcomponent = getOLE(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                    Size size = getOLEDimensions(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                    return insertDrawGraph(image, xcomponent, xChildAccessibleContext, xComponent, size);
                }
            }
            //if we hit here, this means we did not hit a text frame or OLE object
            if (withinRange(xAccessibleContext)) {
                XUnitConversion xUnitConversion = getXUnitConversion(xComponent);
                xAccessibleContext = getNextContext(xAccessibleContext, 0);
                XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                        XAccessibleComponent.class, xAccessibleContext);
                Point point = xAccessibleComponent.getLocationOnScreen();
                java.awt.Point location = MouseInfo.getPointerInfo().getLocation();
                image.p = xUnitConversion.convertPointToLogic(
                        new Point((int) Math.round(location.getX() - point.X), (int) Math.round(location.getY() - point.Y)),
                        MeasureUnit.MM_100TH);
                insertTextContent(image, null, xComponent, xAccessibleContext);
            }
        } catch (Exception e) {
            System.out.println("Error with accessibility api");
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }
        /**
     * Find where to insert into the writer document.
     * Also find if we need to copy an image's dimensions.
     * @param image image to insert
     * @param xComponent component of main window
     * @return true if succeeded, false if not
     */
    private boolean insertIntoWriter(ImageInfo image, XComponent xComponent) {
        try {
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, xComponent);
            // Querying for the text service factory
            XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xTextDocument);
            
            XAccessible mXRoot = makeRoot(xMSF, xTextDocument);
            
            XAccessibleContext xAccessibleRoot = mXRoot.getAccessibleContext();
            

            //scope: xTextDocument -> ScrollPane -> Document
            //get the scroll pane object
            XAccessibleContext xAccessibleContext = getNextContext(xAccessibleRoot, 0);
            
            

            //get the document object
            xAccessibleContext = getNextContext(xAccessibleContext, 0);
            
            

            int numChildren = xAccessibleContext.getAccessibleChildCount();
            //loop through all the children of the document and find the text frames
            for (int i = 0; i < numChildren; i++) {
                XAccessibleContext xChildAccessibleContext = getNextContext(xAccessibleContext, i);
                if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.TEXT_FRAME && withinRange(xChildAccessibleContext)) {
                    //loop through all images in text frame to see if we are over any of them
                    XTextFrame xTextFrame = getFrame(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                    
                   
                    if (dropToResize) {
                        numChildren = xChildAccessibleContext.getAccessibleChildCount();
                        for (int j = 0; j < numChildren; j++) {
                            xChildAccessibleContext = getNextContext(xAccessibleContext, j);
                            if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.GRAPHIC && withinRange(xChildAccessibleContext)) {
                                //if we are over the image, then we insert a new image scaled to the width of the one we're dropping on
                                XUnitConversion xUnitConversion = getXUnitConversion(xComponent);
                                
                                XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                                        XAccessibleComponent.class, xChildAccessibleContext);
                                Size size = xUnitConversion.convertSizeToLogic(xAccessibleComponent.getSize(), MeasureUnit.MM_100TH);
                                image.size.Width = size.Width;
                                j = numChildren;
                            }
                        }
                    }
                    return insertTextContent(image, xTextFrame, xComponent, xChildAccessibleContext);
                } else if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.EMBEDDED_OBJECT) {
                    //System.out.println("Over object");
                    if (withinRange(xChildAccessibleContext)){
                    //user is over an OLE embedded object
                    XComponent xcomponent = getOLE(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                    Size size = getOLEDimensions(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                    return insertDrawContent(image, xcomponent, xChildAccessibleContext, xComponent, size);
                    }
                }
            }

            //insertOLEAndImage(image, image.title, image.description, 17250, 12250);

            // working v1
            //insertOLEAndImage(image, image.title, "", 17250, 12250);
            // working v2
            
            xAccessibleContext = getNextContext(xAccessibleContext, 0);
            XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                    XAccessibleComponent.class, xAccessibleContext);
            Point point = xAccessibleComponent.getLocationOnScreen();
            insertOLEAndImage(image, image.title, "", 17250, 19550, point, xAccessibleComponent.getSize());

                
               
        } catch (Exception e) {
            System.out.println("Error with accessibility api");
            e.printStackTrace(System.err);
            return false;
        }
        return true;
        
        // DJ: this is the original implementation:
        /*
        try {
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, xComponent);
            // Querying for the text service factory
            XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xTextDocument);
            XAccessible mXRoot = makeRoot(xMSF, xTextDocument);
            XAccessibleContext xAccessibleRoot = mXRoot.getAccessibleContext();

            //scope: xTextDocument -> ScrollPane -> Document
            //get the scroll pane object
            XAccessibleContext xAccessibleContext = getNextContext(xAccessibleRoot, 0);

            //get the document object
            xAccessibleContext = getNextContext(xAccessibleContext, 0);

            int numChildren = xAccessibleContext.getAccessibleChildCount();
            
            
            
            //loop through all the children of the document and find the text frames
            for (int i = 0; i < numChildren; i++) {
                XAccessibleContext xChildAccessibleContext = getNextContext(xAccessibleContext, i);
                if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.TEXT_FRAME && withinRange(xChildAccessibleContext)) {
                    //loop through all images in text frame to see if we are over any of them
                    XTextFrame xTextFrame = getFrame(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                    if (dropToResize) {
                        numChildren = xChildAccessibleContext.getAccessibleChildCount();
                        for (int j = 0; j < numChildren; j++) {
                            xChildAccessibleContext = getNextContext(xAccessibleContext, j);
                            if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.GRAPHIC && withinRange(xChildAccessibleContext)) {
                                //if we are over the image, then we insert a new image scaled to the width of the one we're dropping on
                                XUnitConversion xUnitConversion = getXUnitConversion(xComponent);
                                
                                XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                                        XAccessibleComponent.class, xChildAccessibleContext);
                                Size size = xUnitConversion.convertSizeToLogic(xAccessibleComponent.getSize(), MeasureUnit.MM_100TH);
                                image.size.Width = size.Width;
                                j = numChildren;
                            }
                        }
                    }
                    return insertTextContent(image, xTextFrame, xComponent, xChildAccessibleContext);
                } else if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.EMBEDDED_OBJECT) {
                    //System.out.println("Over object");
                    if (withinRange(xChildAccessibleContext)){
                    //user is over an OLE embedded object
                    XComponent xcomponent = getOLE(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                    Size size = getOLEDimensions(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                    return insertDrawContent(image, xcomponent, xChildAccessibleContext, xComponent, size);
                    }
                }
            }
            //if we hit here, this means we did not hit a text frame or OLE object
            if (withinRange(xAccessibleContext)) {
                XUnitConversion xUnitConversion = getXUnitConversion(xComponent);
                xAccessibleContext = getNextContext(xAccessibleContext, 0);
                XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                        XAccessibleComponent.class, xAccessibleContext);
                Point point = xAccessibleComponent.getLocationOnScreen();
                java.awt.Point location = MouseInfo.getPointerInfo().getLocation();
                image.p = xUnitConversion.convertPointToLogic(
                        new Point((int) Math.round(location.getX() - point.X), (int) Math.round(location.getY() - point.Y)),
                        MeasureUnit.MM_100TH);
                insertTextContent(image, null, xComponent, xAccessibleContext);
            }
        } catch (Exception e) {
            System.out.println("Error with accessibility api");
            e.printStackTrace(System.err);
            return false;
        }
        return true;
        */
    }
        
    
    
    //DJ: 10/13/2014
    /**
     * Find where to insert into the writer document.
     * Also find if we need to copy an image's dimensions.
     * @param image image to insert
     * @param xComponent component of main window
     * @return true if succeeded, false if not
     */
    private boolean insertIntoOLE(ImageInfo image, XComponent xComponent, XComponent oleComponent) {
        
        try {
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, xComponent);
            // Querying for the text service factory
            XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xTextDocument);
            XAccessible mXRoot = makeRoot(xMSF, xTextDocument);
            XAccessibleContext xAccessibleRoot = mXRoot.getAccessibleContext();

            //scope: xTextDocument -> ScrollPane -> Document
            //get the scroll pane object
            XAccessibleContext xAccessibleContext = getNextContext(xAccessibleRoot, 0);

            //get the document object
            xAccessibleContext = getNextContext(xAccessibleContext, 0);

            int numChildren = xAccessibleContext.getAccessibleChildCount();
            
            
            
            //loop through all the children of the document and find the text frames
            for (int i = 0; i < numChildren; i++) {
                XAccessibleContext xChildAccessibleContext = getNextContext(xAccessibleContext, i);
                if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.EMBEDDED_OBJECT) {
                    
                    
                    XComponent xcomponent = getOLE(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                    
                    //System.out.println("here");
                    if(oleComponent.equals(xcomponent)){
                        //System.out.println("yes");
                        Size size = getOLEDimensions(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                        return insertDrawContent(image, xcomponent, xChildAccessibleContext, xComponent, size);
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("Error with accessibility api");
            e.printStackTrace(System.err);
            return false;
        }
        return true;
        
        
    }
        /**
     * Find where to insert into the draw document.
     * Also find if we need to copy an image's dimensions.
     * @param xComponent component of draw window
     * @param image image to insert 
     * @return true if succeeded, false if not
     */
    private boolean insertClosestDraw(XComponent xComponent, ImageInfo image) {
        try {
            XModel xModel = (XModel) UnoRuntime.queryInterface(XModel.class, xComponent);
            XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xModel);
            XAccessible mXRoot = makeRoot(xMSF, xModel);
            XAccessibleContext xAccessibleRoot = mXRoot.getAccessibleContext();
            //go into AccessibleRole 40 (panel)
            XAccessibleContext xAccessibleContext = getNextContext(xAccessibleRoot, 0);

            //go into AccessibleRole 51 (scroll pane)
            xAccessibleContext = getNextContext(xAccessibleContext, 0);

            //go into AccessibleRole 13 (document)
            xAccessibleContext = getNextContext(xAccessibleContext, 0);

            //check to see whether if in range of document
            if (withinPage(xAccessibleContext)) {
                insertDrawGraph(image, xComponent, xAccessibleRoot, null, null);
            }
        } catch (Exception e) {
            System.out.println("Error with accessibility api");
            e.printStackTrace(System.err);
            return false;
        }
        return true;

    }
    /**
     * Find where to insert into the draw document.
     * Also find if we need to copy an image's dimensions.
     * @param xComponent component of draw window
     * @param image image to insert 
     * @return true if succeeded, false if not
     */
    private boolean insertIntoDraw(XComponent xComponent, ImageInfo image) {
        try {
            XModel xModel = (XModel) UnoRuntime.queryInterface(XModel.class, xComponent);
            XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xModel);
            XAccessible mXRoot = makeRoot(xMSF, xModel);
            XAccessibleContext xAccessibleRoot = mXRoot.getAccessibleContext();
            //go into AccessibleRole 40 (panel)
            XAccessibleContext xAccessibleContext = getNextContext(xAccessibleRoot, 0);

            //go into AccessibleRole 51 (scroll pane)
            xAccessibleContext = getNextContext(xAccessibleContext, 0);

            //go into AccessibleRole 13 (document)
            xAccessibleContext = getNextContext(xAccessibleContext, 0);

            //check to see whether if in range of document
            if (withinRange(xAccessibleContext)) {
                int numChildren = xAccessibleContext.getAccessibleChildCount();
                //loop through all the children of the document
                if (dropToResize) {
                    for (int i = 0; i < numChildren; i++) {
                        XAccessibleContext xChildAccessibleContext = getNextContext(xAccessibleContext, i);
                        //if we are over an image and it has a description (so from OpenMIMS), adjust our height
                        if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.LIST_ITEM
                                && !xChildAccessibleContext.getAccessibleDescription().isEmpty()
                                && withinRange(xChildAccessibleContext)) {
                            XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                                    XAccessibleComponent.class, xChildAccessibleContext);
                            XUnitConversion xUnitConversion = getXUnitConversion(xComponent);
                            Size size = xUnitConversion.convertSizeToLogic(xAccessibleComponent.getSize(), MeasureUnit.MM_100TH);
                                image.size.Width = size.Width;
                            break;
                        }
                    }
                }
                XUnitConversion xUnitConversion = getXUnitConversion(xComponent);
                xAccessibleContext = getNextContext(xAccessibleContext, 0);
                XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                        XAccessibleComponent.class, xAccessibleContext);
                Point point = xAccessibleComponent.getLocationOnScreen();
                java.awt.Point location = MouseInfo.getPointerInfo().getLocation();
                image.p = xUnitConversion.convertPointToLogic(
                        new Point((int) Math.round(location.getX() - point.X), (int) Math.round(location.getY() - point.Y)),
                        MeasureUnit.MM_100TH);
                insertDrawContent(image, xComponent, xAccessibleRoot, null, null);
            }
        } catch (Exception e) {
            System.out.println("Error with accessibility api");
            e.printStackTrace(System.err);
            return false;
        }
        return true;

    }
    /**
     * Convert and insert image and relevant info into Writer doc
     * @param image image and info to insert
     * @param xTextFrame textframe to insert into, null if none
     * @param xComponent component of writer window
     * @param xAccessibleContext accessible context of what we are inserting into
     * @return true if succeeded, false if not
     */
    private boolean insertTextContent(ImageInfo image, XTextFrame xTextFrame, XComponent xComponent, XAccessibleContext xAccessibleContext) {
        XTextDocument xTextDocument;
        try {
            //create blank graphic in document
            xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, xComponent);
            Object graphic = createBlankGraphic(xTextDocument);

            //query for the interface XTextContent on the GraphicObject 
            image.xImage = (com.sun.star.text.XTextContent) UnoRuntime.queryInterface(
                    com.sun.star.text.XTextContent.class, graphic);

            //query for the properties of the graphic
            XUnitConversion xUnitConversion = getXUnitConversion(xComponent);
            Size size = new Size(image.image.getWidth(null), image.image.getHeight(null));
            size = xUnitConversion.convertSizeToLogic(size, MeasureUnit.MM_100TH);
            if (image.size.Width > 0) {
                //calculate the width and height
                double ratio = (double) image.size.Width / (double) size.Width;
                image.size.Height = (int) Math.round(ratio * size.Height);
            }else{
                image.size = size;
            }
            
            XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                    XAccessibleComponent.class, xAccessibleContext);
            Size windowSize = xUnitConversion.convertSizeToLogic(xAccessibleComponent.getSize(), MeasureUnit.MM_100TH);
            //if the image is greater than the width, then we scale it down the barely fit in the page
            if (fitToWindow) {
                if (image.size.Width > windowSize.Width) {
                    int ratio = image.size.Width;
                    image.size.Width = windowSize.Width - 1000;
                    ratio = image.size.Width / ratio;
                    image.size.Height = image.size.Height * ratio;
                }
                //if greater than height, do the same thing to descale it
                /*if (image.size.Height >= windowSize.Height) {
                    double ratio = image.size.Height;
                    image.size.Height = windowSize.Height - 2500;
                    ratio = image.size.Height / ratio;
                    image.size.Width = (int) Math.round(image.size.Width * ratio);
                }*/
            }
            //set the TextContent properties
            com.sun.star.beans.XPropertySet xPropSet = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                    com.sun.star.beans.XPropertySet.class, graphic);
            xPropSet.setPropertyValue("AnchorType", TextContentAnchorType.AT_FRAME);
            xPropSet.setPropertyValue("Width", image.size.Width);
            xPropSet.setPropertyValue("Height", image.size.Height);
            xPropSet.setPropertyValue("Graphic", convertImage(image.image));
            xPropSet.setPropertyValue("TextWrap", WrapTextMode.NONE);
            xPropSet.setPropertyValue("Title", image.title);
            xPropSet.setPropertyValue("Description", image.description);
        } catch (Exception exception) {
            System.out.println("Couldn't set image properties");
            exception.printStackTrace(System.err);
            return false;
        }
        //insert the content
        return insertImageIntoTextFrame(image, xTextFrame, xTextDocument);

    }
      /**
     * Inserts content and info into a draw page.
     * @param image image and info to insert
     * @param xComponent component of draw page
     * @param xAccessibleContext accessiblecontext of draw page
     * @param parentComponent parent component of draw page, if one exists
     * @return true if succeeded, false if not
     */
    private boolean insertDrawGraph(ImageInfo image, XComponent xComponent, XAccessibleContext xAccessibleContext, XComponent parentComponent, Size vSize) {
        Size size;
        Point point;
        XDrawPage xDrawPage = getXDrawPage(xComponent);
        XUnitConversion xUnitConversion;
        if (xDrawPage == null) {
            return false;
        }
        try {
            if (parentComponent == null) {
                xUnitConversion = getXUnitConversion(xComponent);
            } else {
                xUnitConversion = getXUnitConversion(parentComponent);
            }
            //create blank graphic in document
            Object graphic = createBlankGraphic(xComponent);

            //query for the interface XTextContent on the GraphicObject 
            image.xShape = (XShape) UnoRuntime.queryInterface(
                    XShape.class, graphic);

            //query for the properties of the graphic
            com.sun.star.beans.XPropertySet xPropSet = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                    com.sun.star.beans.XPropertySet.class, graphic);

            size = new Size(image.image.getWidth(null), image.image.getHeight(null));
            size = xUnitConversion.convertSizeToLogic(size, MeasureUnit.MM_100TH);
            if (image.size.Width > 0) {
                //calculate the width and height
                double ratio = (double) image.size.Width / (double) size.Width;
                image.size.Height = (int) Math.round(ratio * size.Height);
            }else{
                image.size = size;
            }
            XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                    XAccessibleComponent.class, xAccessibleContext);
            Size windowSize = xUnitConversion.convertSizeToLogic(xAccessibleComponent.getSize(), MeasureUnit.MM_100TH);
            if (vSize != null){
                windowSize = vSize;
            }
            //if the image is greater than the width, then we scale it down to fit in the page
            if (fitToWindow) {
                if (image.size.Width > windowSize.Width) {
                    double ratio = image.size.Width;
                    image.size.Width = windowSize.Width - 2500;
                    ratio = image.size.Width / ratio;
                    image.size.Height = (int) Math.round(image.size.Height * ratio);
                }
                //if greater than height, do the same thing to descale it
                if (image.size.Height >= windowSize.Height) {
                    double ratio = image.size.Height;
                    image.size.Height = windowSize.Height - 2500;
                    ratio = image.size.Height / ratio;
                    image.size.Width = (int) Math.round(image.size.Width * ratio);
                }
            }
           
            point = new Point();
            point.X = 0;
            point.Y = 0;
            //tile the images, and make sure they do not go beyond the limit of the window
            if (autoTile) {
                int curX;
                while ((curX = intersects(point, image.size, xDrawPage)) != 0) {
                    if (curX + image.size.Width + 200 < windowSize.Width) {
                        point.X = curX;
                    } else {
                        point.X = 0;
                        point.Y += (300);
                    }
                }
            }
            image.xShape.setPosition(point);
            if (fitToWindow) {
                //if greater than height, do the same thing to descale it
                if (image.size.Height + point.Y >= windowSize.Height && image.size.Height - point.Y > 1000) {
                    double ratio = image.size.Height;
                    image.size.Height = windowSize.Height - point.Y - 2500;
                    ratio = image.size.Height / ratio;
                    image.size.Width = (int) Math.round(image.size.Width * ratio);
                }
            }
            //point.X -= Math.round(image.size.Width/2);
            //point.Y -= Math.round(image.size.Height/2);
             image.xShape.setSize(image.size);
            xPropSet.setPropertyValue("Graphic", convertImage(image.image));
            //xPropSet.setPropertyValue("Title", image.title);
            //xPropSet.setPropertyValue("Description", image.description);
        } catch (Exception exception) {
            System.out.println("Couldn't set image properties");
            exception.printStackTrace(System.err);
            return false;
        }
        try {
            XMultiServiceFactory xDrawFactory =
                    (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xComponent);
            Object drawShape = xDrawFactory.createInstance("com.sun.star.drawing.TextShape");
            XShape xDrawShape = (XShape) UnoRuntime.queryInterface(XShape.class, drawShape);
            xDrawShape.setSize(new Size(image.size.Width, 1000));
            xDrawShape.setPosition(new Point(point.X, point.Y + image.size.Height));

            //add OpenMims Image
            xDrawPage.add(image.xShape);

            //get properties of text shape and modify them
            XPropertySet xShapeProps = (XPropertySet) UnoRuntime.queryInterface(
                    XPropertySet.class, drawShape);
            xShapeProps.setPropertyValue("TextAutoGrowHeight", true);
            xShapeProps.setPropertyValue("TextContourFrame", true);
            xShapeProps.setPropertyValue("FillStyle", FillStyle.NONE);
            xShapeProps.setPropertyValue("LineTransparence", 100);   
        } catch (Exception e) {
            System.out.println("Couldn't insert image");
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }
    /**
     * Inserts content and info into a draw page.
     * @param image image and info to insert
     * @param xComponent component of draw page
     * @param xAccessibleContext accessiblecontext of draw page
     * @param parentComponent parent component of draw page, if one exists
     * @return true if succeeded, false if not
     */
    private boolean insertDrawContent(ImageInfo image, XComponent xComponent, XAccessibleContext xAccessibleContext, XComponent parentComponent, Size vSize) {
        Size size;
        Point point;
        XDrawPage xDrawPage = getXDrawPage(xComponent); 
        
        //DJ: 10/15/2014 :  just for testing
        //System.out.println("Does draw have elements: " + xDrawPage.hasElements());
        
        
        XUnitConversion xUnitConversion;
        if (xDrawPage == null) {
            return false;
        }
        try {
            if (parentComponent == null) {
                xUnitConversion = getXUnitConversion(xComponent);
            } else {
                xUnitConversion = getXUnitConversion(parentComponent);
            }
            //create blank graphic in document
            Object graphic = createBlankGraphic(xComponent);

            //query for the interface XTextContent on the GraphicObject 
            image.xShape = (XShape) UnoRuntime.queryInterface(
                    XShape.class, graphic);

            //query for the properties of the graphic
            com.sun.star.beans.XPropertySet xPropSet = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                    com.sun.star.beans.XPropertySet.class, graphic);

            size = new Size(image.image.getWidth(null), image.image.getHeight(null));
            size = xUnitConversion.convertSizeToLogic(size, MeasureUnit.MM_100TH);
            Size images = new Size(2,2);
            Size imagesize = xUnitConversion.convertSizeToPixel(images, MeasureUnit.INCH);
            double imageRatio = (double) imagesize.Width/ (double) 256;
            if (image.size.Width > 0) {
                //calculate the width and height
                double ratio = (double) image.size.Width / (double) size.Width;
                image.size.Height = (int) Math.round(ratio * size.Height);
            }else{
                image.size = size;
                image.size.Width = (int) (imageRatio * image.size.Width);
                image.size.Height = (int) (imageRatio * image.size.Height);
            }
            XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                    XAccessibleComponent.class, xAccessibleContext);
            Size windowSize = xUnitConversion.convertSizeToLogic(xAccessibleComponent.getSize(), MeasureUnit.MM_100TH);
            if (vSize != null) {
                windowSize = vSize;
            }
            //if the image is greater than the width, then we scale it down to fit in the page
            if (fitToWindow) {
                if (image.size.Width > windowSize.Width) {
                    double ratio = image.size.Width;
                    image.size.Width = windowSize.Width - 2500;
                    ratio = image.size.Width / ratio;
                    image.size.Height = (int) Math.round(image.size.Height * ratio);
                }
                //if greater than height, do the same thing to descale it
                if (image.size.Height >= windowSize.Height) {
                    double ratio = image.size.Height;
                    image.size.Height = windowSize.Height - 2500;
                    ratio = image.size.Height / ratio;
                    image.size.Width = (int) Math.round(image.size.Width * ratio);
                }
            }
           
            point = new Point();
            point.X = 0;
            point.Y = 0;
            //tile the images, and make sure they do not go beyond the limit of the window
            if (isImpress(xComponent)){
                point = image.p;
            }else if (autoTile) {
                int curX;
                while ((curX = intersects(point, image.size, xDrawPage)) != 0) {
                    
                    if (curX + image.size.Width + 200 < 17250) {
                        point.X = curX;
                    } else {
                        point.X = 0;
                        point.Y += (300);
                    }
                }
            }
            image.xShape.setPosition(point);
            if (fitToWindow) {
                //if greater than height, do the same thing to descale it
                if (image.size.Height + point.Y >= windowSize.Height && image.size.Height - point.Y > 1000) {
                    double ratio = image.size.Height;
                    image.size.Height = windowSize.Height - point.Y - 2500;
                    ratio = image.size.Height / ratio;
                    image.size.Width = (int) Math.round(image.size.Width * ratio);
                }
            }
            //point.X -= Math.round(image.size.Width/2);
            //point.Y -= Math.round(image.size.Height/2);
             image.xShape.setSize(image.size);
            xPropSet.setPropertyValue("Graphic", convertImage(image.image));
            xPropSet.setPropertyValue("Title", image.title);
            xPropSet.setPropertyValue("Description", image.description);
        } catch (Exception exception) {
            System.out.println("Couldn't set image properties");
            exception.printStackTrace(System.err);
            return false;
        }
        try {
            XMultiServiceFactory xDrawFactory =
                    (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xComponent);
            Object drawShape = xDrawFactory.createInstance("com.sun.star.drawing.TextShape");
            XShape xDrawShape = (XShape) UnoRuntime.queryInterface(XShape.class, drawShape);
            
            // DJ: 10/31/2014
            if(image.imageType.equals("HSI_IMAGE")){
                xDrawShape.setSize(new Size(image.size.Width, 900));
            } else {
                xDrawShape.setSize(new Size(image.size.Width, 1000));
            }
            xDrawShape.setPosition(new Point(point.X, point.Y + image.size.Height));

            //add OpenMims Image
            xDrawPage.add(image.xShape);

            //get properties of text shape and modify them
            XPropertySet xShapeProps = (XPropertySet) UnoRuntime.queryInterface(
                    XPropertySet.class, drawShape);
            xShapeProps.setPropertyValue("TextAutoGrowHeight", true);
            xShapeProps.setPropertyValue("TextContourFrame", true);
            xShapeProps.setPropertyValue("FillStyle", FillStyle.NONE);
            xShapeProps.setPropertyValue("LineTransparence", 100);

            //add text shape
            xDrawPage.add(xDrawShape);

            //add text into text shape and set text size
            XText xShapeText = (XText) UnoRuntime.queryInterface(XText.class, drawShape);
            XTextCursor xTextCursor = xShapeText.createTextCursor();
            XTextRange xTextRange = xTextCursor.getStart();
            XPropertySet xTextProps = (XPropertySet) UnoRuntime.queryInterface(
                    XPropertySet.class, xTextRange);
            xTextProps.setPropertyValue("CharHeight", new Float(9)); //DJ intitially was 11
            
            //DJ: 10/15/2014 Slightly changed for visibility purposes:
            //xTextRange.setString(image.text); // original.
            
            //DJ: 10/24/2014
            String stringRange = image.text;
            if(image.imageType.equals("MASS_IMAGE") ){
                stringRange += " - Plane [#" + image.planeNumber + "]\n";
            } else if( image.imageType.equals("RATIO_IMAGE") && !image.planeNumber.equals("N/A") ) {
                stringRange += " - Plane [#" + image.planeNumber + "]\n";
            } else if( image.imageType.equals("HSI_IMAGE") && !image.planeNumber.equals("N/A") ){
                stringRange += " - Plane [#" + image.planeNumber + "]";
            }
            xTextRange.setString(stringRange);

            
                                 

            //get XShapes interface to group images
            XMultiServiceFactory xMultiServiceFactory = (XMultiServiceFactory) UnoRuntime.queryInterface(XMultiServiceFactory.class, xMCF);
            Object xObj = xMultiServiceFactory.createInstance("com.sun.star.drawing.ShapeCollection");
            XShapes xToGroup = (XShapes) UnoRuntime.queryInterface(XShapes.class, xObj);

            //add images to XShapes
            xToGroup.add(image.xShape);
            xToGroup.add(xDrawShape);

            //Group the shapes by using the XShapeGrouper
            XShapeGrouper xShapeGrouper = (XShapeGrouper) UnoRuntime.queryInterface(
                    XShapeGrouper.class, xDrawPage);
            XShapeGroup xShapeGroup = (XShapeGroup) xShapeGrouper.group(xToGroup);

            //set title and description of grouped image
            com.sun.star.beans.XPropertySet xPropSet = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                    com.sun.star.beans.XPropertySet.class, xShapeGroup);
            xPropSet.setPropertyValue("Title", image.title);
            xPropSet.setPropertyValue("Description", image.description);
            
            
            //DJ: 10/15/2014 :  just for testing
            //System.out.println("2- Does draw have elements: " + xDrawPage.hasElements());
           
            
        } catch (Exception e) {
            System.out.println("Couldn't insert image");
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }
    private boolean insertText(XComponent currentDocument, String s){

            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, currentDocument);
                        XModel xModel = (XModel)UnoRuntime.queryInterface(
                XModel.class, currentDocument);
 XController xController = xModel.getCurrentController();
                XTextViewCursorSupplier xViewCursorSupplier = (XTextViewCursorSupplier)UnoRuntime.queryInterface(
                XTextViewCursorSupplier.class, xController);

                XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor();
                XTextCursor cursor = xViewCursor;
                XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, cursor);
                xTextDocument.getText().insertString(xTextRange, s, false);
        return true;
    }
    /**
     * Method to insert a textframe and image together into a text document's textframe.
     * 
     * @param image image and info to insert
     * @param destination textframe to insert into
     * @param xTextDocument document to insert into
     * @return 
     */
    private boolean insertImageIntoTextFrame(ImageInfo image, XTextFrame destination, XTextDocument xTextDocument) {
        XTextFrame xTextFrame;
        XText xText;
        XTextCursor xTextCursor;
        XTextRange xTextRange;
        try {
            XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xTextDocument);
            //create a new text frame
            Object frame = xMSF.createInstance("com.sun.star.text.TextFrame");
            xTextFrame = (com.sun.star.text.XTextFrame) UnoRuntime.queryInterface(
                    com.sun.star.text.XTextFrame.class, frame);

            //set the dimensions of the new text frame
            XShape xTextFrameShape = (com.sun.star.drawing.XShape) UnoRuntime.queryInterface(
                    com.sun.star.drawing.XShape.class, frame);
            com.sun.star.awt.Size aSize = new com.sun.star.awt.Size();
            aSize.Height = image.size.Height;
            aSize.Width = image.size.Width;     
            xTextFrameShape.setSize(aSize);

            //Set the properties of the textframe
            int[] blank = new int[]{0, 0, 0, 0};
            com.sun.star.beans.XPropertySet xTFPS = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                    com.sun.star.beans.XPropertySet.class, xTextFrame);
            //remove the borders
            xTFPS.setPropertyValue("FrameIsAutomaticHeight", true);
            xTFPS.setPropertyValue("LeftBorder", blank);
            xTFPS.setPropertyValue("RightBorder", blank);
            xTFPS.setPropertyValue("TopBorder", blank);
            xTFPS.setPropertyValue("BottomBorder", blank);
            if (destination != null) {
                xTFPS.setPropertyValue("AnchorType",
                    com.sun.star.text.TextContentAnchorType.AT_FRAME);
                //insert the textframe
                xText = destination.getText();
                xTextCursor = xText.createTextCursor();
                xTextRange = xTextCursor.getStart();
                xText.insertTextContent(xTextRange, xTextFrame, true);
            } else {
                int x = image.p.X - (image.size.Width / 2);
                int y = image.p.Y - (image.size.Height / 2);
                xTFPS.setPropertyValue("AnchorType",
                    com.sun.star.text.TextContentAnchorType.AT_PARAGRAPH);
                xTFPS.setPropertyValue("VertOrient", com.sun.star.text.VertOrientation.NONE);
                xTFPS.setPropertyValue("HoriOrient", com.sun.star.text.HoriOrientation.NONE);
                xTFPS.setPropertyValue("HoriOrientRelation", com.sun.star.text.RelOrientation.PAGE_FRAME);
                xTFPS.setPropertyValue("VertOrientRelation", com.sun.star.text.RelOrientation.PAGE_FRAME);
                xTFPS.setPropertyValue("HoriOrientPosition", x);
                xTFPS.setPropertyValue("VertOrientPosition", y);
                xText = xTextDocument.getText();
                xTextCursor = xText.createTextCursor();
                xTextRange = xTextCursor.getStart();
                xText.insertTextContent(xTextRange, xTextFrame, true);
            }

            //insert the image into the textframe
            xText = xTextFrame.getText();
            xTextCursor = xText.createTextCursor();
            xTextRange = xTextCursor.getStart();
            xText.insertTextContent(xTextRange, image.xImage, true);

            //insert the caption
            xTextRange.setString(image.text);
        } catch (Exception exception) {
            System.out.println("Couldn't insert image");
            exception.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    /**
     * Find a named text frame within current Writer doc
     *
     * @param name the name of the text frame
     * @return XTextFrame interface
     */
    private static XTextFrame getFrame(String name, XTextDocument xTextDocument) {
        XTextFrame xTextFrame = null;
        try {
            //get the text frame supplier from the document
            XTextFramesSupplier xTextFrameSupplier =
                    (XTextFramesSupplier) UnoRuntime.queryInterface(
                    XTextFramesSupplier.class, xTextDocument);

            //get text frame objects
            XNameAccess xNameAccess = xTextFrameSupplier.getTextFrames();

            //query for the object with the desired name
            Object frame = xNameAccess.getByName(name);

            //get the XTextFrame interface
            xTextFrame = (XTextFrame) UnoRuntime.queryInterface(
                    com.sun.star.text.XTextFrame.class, frame);
        } catch (Exception e) {
            System.out.println("Could not find frame with name " + name);
            e.printStackTrace(System.err);
        }
        return xTextFrame;

    }
    /**
     * Retrieve an OLE Object from a text document.
     * @param name Name of the OLE Object
     * @param xTextDocument the text document we're looking in
     * @return the XComponent which represents the OLE
     */
    private XComponent getOLE(String name, XTextDocument xTextDocument) {
        XComponent xComponent = null;
        try {
            //get the text frame supplier from the document
            XTextEmbeddedObjectsSupplier xTextEmbeddedObjectsSupplier =
                    (XTextEmbeddedObjectsSupplier) UnoRuntime.queryInterface(
                    XTextEmbeddedObjectsSupplier.class, xTextDocument);

            //get text frame objects
            XNameAccess xNameAccess = xTextEmbeddedObjectsSupplier.getEmbeddedObjects();

            //query for the object with the desired name
            Object xTextEmbeddedObject = xNameAccess.getByName(name);
            XTextContent xTextContent = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, xTextEmbeddedObject);
            //get the XTextFrame interface
            com.sun.star.document.XEmbeddedObjectSupplier xEOS = (com.sun.star.document.XEmbeddedObjectSupplier) UnoRuntime.queryInterface(com.sun.star.document.XEmbeddedObjectSupplier.class, xTextContent);
            com.sun.star.lang.XComponent xModel = xEOS.getEmbeddedObject();
            return xModel;
        } catch (Exception e) {
            System.out.println("Could not find frame with name " + name);
            e.printStackTrace(System.err);
        }
        return xComponent;

    }
        //DJ: for testing purposes:
        static XComponent getOLEE(String name, XTextDocument xTextDocument) {
        XComponent xComponent = null;
        try {
            //get the text frame supplier from the document
            XTextEmbeddedObjectsSupplier xTextEmbeddedObjectsSupplier =
                    (XTextEmbeddedObjectsSupplier) UnoRuntime.queryInterface(
                    XTextEmbeddedObjectsSupplier.class, xTextDocument);

            //get text frame objects
            XNameAccess xNameAccess = xTextEmbeddedObjectsSupplier.getEmbeddedObjects();
            
            for(String s : xNameAccess.getElementNames()){
                System.out.println("Embeded object name is : " + s);
            }

            //query for the object with the desired name
            Object xTextEmbeddedObject = xNameAccess.getByName(name);
            XTextContent xTextContent = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, xTextEmbeddedObject);
            //get the XTextFrame interface
            com.sun.star.document.XEmbeddedObjectSupplier xEOS = (com.sun.star.document.XEmbeddedObjectSupplier) UnoRuntime.queryInterface(com.sun.star.document.XEmbeddedObjectSupplier.class, xTextContent);
            com.sun.star.lang.XComponent xModel = xEOS.getEmbeddedObject();
            return xModel;
        } catch (Exception e) {
            System.out.println("Could not find frame with name " + name);
            e.printStackTrace(System.err);
        }
        return xComponent;

    }
    
    private Size getOLEDimensions(String name, XTextDocument xTextDocument) {
        try {
            //get the text frame supplier from the document
            XTextEmbeddedObjectsSupplier xTextEmbeddedObjectsSupplier =
                    (XTextEmbeddedObjectsSupplier) UnoRuntime.queryInterface(
                    XTextEmbeddedObjectsSupplier.class, xTextDocument);

            //get text frame objects
            XNameAccess xNameAccess = xTextEmbeddedObjectsSupplier.getEmbeddedObjects();

            //query for the object with the desired name
            Object xTextEmbeddedObject = xNameAccess.getByName(name);
            XTextContent xt = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, xTextEmbeddedObject);
            //get the XTextFrame interface
            com.sun.star.document.XEmbeddedObjectSupplier2 xEOS2 = (com.sun.star.document.XEmbeddedObjectSupplier2) UnoRuntime.queryInterface(com.sun.star.document.XEmbeddedObjectSupplier2.class, xt);
            XEmbeddedObject xEmbeddedObject = xEOS2.getExtendedControlOverEmbeddedObject();
            return xEmbeddedObject.getVisualAreaSize(xEOS2.getAspect());
        } catch (Exception e) {
            System.out.println("Could not find frame with name " + name);
            e.printStackTrace(System.err);
        }
        return null;

    }

    /**
     * Convert an image into a XGraphic
     *
     * @param image the java.awt.image to convert
     * @return an XGraphic which can be placed into a XTextContent
     */
    private XGraphic convertImage(Image image) {
        XGraphic xGraphic = null;
        try {
            ByteArrayToXInputStreamAdapter xSource = new ByteArrayToXInputStreamAdapter(imageToByteArray(image));
            PropertyValue[] sourceProps = new PropertyValue[2];

            //specify the byte array source
            sourceProps[0] = new PropertyValue();
            sourceProps[0].Name = "InputStream";
            sourceProps[0].Value = xSource;

            //specify the image type
            sourceProps[1] = new PropertyValue();
            sourceProps[1].Name = "MimeType";
            sourceProps[1].Value = "image/png";

            //get the graphic object
            XGraphicProvider xGraphicProvider = (XGraphicProvider) UnoRuntime.queryInterface(
                    XGraphicProvider.class,
                    xMCF.createInstanceWithContext("com.sun.star.graphic.GraphicProvider", context));
            xGraphic = xGraphicProvider.queryGraphic(sourceProps);
        } catch (Exception e) {
            System.out.println("Failed to convert image into LibreOffice graphic");
            e.printStackTrace(System.err);
        }
        return xGraphic;
    }

    /**
     * Create a blank graphic for insertion
     *
     * @return Object representing a blank Graphic
     */
    private Object createBlankGraphic(XTextDocument xTextDocument) {
        Object graphic = null;
        try {
            //create unique name based on timestamp
            long unixTime = System.currentTimeMillis() / 1000L;
            XMultiServiceFactory docServiceFactory =
                    (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xTextDocument);
            graphic = docServiceFactory.createInstance("com.sun.star.text.TextGraphicObject");
            XNamed name = (XNamed) UnoRuntime.queryInterface(XNamed.class, graphic);
            name.setName("" + unixTime);
        } catch (Exception exception) {
            System.out.println("Could not create image");
            exception.printStackTrace(System.err);
        }
        return graphic;
    }

    /**
     * Create a graphic object on specified page
     *
     * @param xDrawPage
     * @return
     */
    private Object createBlankGraphic(XComponent xDrawPage) {
        Object graphic = null;
        try {
            //create unique name based on timestamp
            long unixTime = System.currentTimeMillis() / 1000L;
            XMultiServiceFactory docServiceFactory =
                    (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xDrawPage);
            graphic = docServiceFactory.createInstance("com.sun.star.drawing.GraphicObjectShape");
            XNamed name = (XNamed) UnoRuntime.queryInterface(XNamed.class, graphic);
            name.setName("" + unixTime);
        } catch (Exception exception) {
            System.out.println("Could not create image");
            exception.printStackTrace(System.err);
        }
        return graphic;
    }
    /**
     * Get current window from given xModel
     * @param msf
     * @param xModel
     * @return XWindow object
     */
    private static XWindow getCurrentWindow(
            XModel xModel) {
        return getWindow(xModel, false);
    }

    /**
     * Check if the mouse pointer is within range of particular component
     *
     * @param xAccessibleContext the context of particular component
     * @return true if within, false if not
     */
    private static boolean withinRange(XAccessibleContext xAccessibleContext) {
        //get the accessible component
        XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                XAccessibleComponent.class, xAccessibleContext);

        //get the bounds and check whether cursor is within it
        Point point = xAccessibleComponent.getLocationOnScreen();
        Size size = xAccessibleComponent.getSize();
        java.awt.Point location = MouseInfo.getPointerInfo().getLocation();
        if (point.X + size.Width < location.getX() || location.getX() < point.X || point.Y + size.Height < location.getY() || point.Y > location.getY()) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Check if element is within page range
     *
     * @param xAccessibleContext the context of particular component
     * @return true if within, false if not
     */
    private boolean withinPage(XAccessibleContext xAccessibleContext) {
        //get the accessible component
        XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                XAccessibleComponent.class, xAccessibleContext);
        //get the bounds and check whether the element is on the screen
        Point point = xAccessibleComponent.getLocationOnScreen();
        Size size = xAccessibleComponent.getSize();
        if (point.X > 0 && point.Y > 0) {
            return true;
        } else {
            return false;
        }
    }
    /**
     * Check to see if a rectangle intersects with any objects in the XDrawPage.
     * @param p the upper-left corner of the rectangle
     * @param s the dimensions of the rectangle
     * @param xDrawPage the XDrawPage containing the objects we want to check against
     * @return 
     */
    private int intersects(Point p, Size s, XDrawPage xDrawPage) {
        Rectangle rectangle = new Rectangle(p.X, p.Y, s.Width, s.Height);
        XShapes xShapes = (XShapes) UnoRuntime.queryInterface(XShapes.class, xDrawPage);
        for (int i = 0; i < xShapes.getCount(); i++) {
            try {
                XShape xShape = (XShape) UnoRuntime.queryInterface(XShape.class, xShapes.getByIndex(i));

                //get the bounds and check whether cursor is within it
                Point point = xShape.getPosition();
                Size size = xShape.getSize();
                Rectangle targetRectangle = new Rectangle(point.X, point.Y, size.Width, size.Height);
                if (rectangle.intersects(targetRectangle)) {
                    return point.X+size.Width + 200;
                }
            } catch (Exception e) {
                System.out.println("Exception caught");
                return -1;
            }

        }
        return 0;
    }

    private static XWindow getWindow(XModel xModel, boolean containerWindow) {
        XWindow xWindow = null;
        try {
            if (xModel == null) {
                System.out.println("invalid model (==null)");
            }
            XController xController = xModel.getCurrentController();
            if (xController == null) {
                System.out.println("can't get controller from model");
            }
            XFrame xFrame = xController.getFrame();
            if (xFrame == null) {
                System.out.println("can't get frame from controller");
            }
            if (containerWindow) {
                xWindow = xFrame.getContainerWindow();
            } else {
                xWindow = xFrame.getComponentWindow();
            }
            if (xWindow == null) {
                System.out.println("can't get window from frame");
            }
        } catch (Exception e) {
            System.out.println("caught exception while getting current window" + e);
        }
        return xWindow;
    }
    private static XAccessible getAccessibleObject(XInterface xObject) {
        XAccessible xAccessible = null;
        try {
            xAccessible = (XAccessible) UnoRuntime.queryInterface(
                    XAccessible.class, xObject);
        } catch (Exception e) {
            System.out.println("Caught exception while getting accessible object" + e);
            e.printStackTrace();
        }
        return xAccessible;
    }
    /**
     * Get the current drawpage of the given xComponent.
     * Can be used either on a draw document, impress document, or OLE object
     * @param xComponent
     * @return the XDrawPage that is currently displayed
     */
    private XDrawPage getXDrawPage(XComponent xComponent) {
        XDrawPage xDrawPage = null;
        try {
            XModel xModel = (XModel) UnoRuntime.queryInterface(XModel.class, xComponent);
            
                XController dddV = xModel.getCurrentController();
            if (dddV != null) {
                //this will work for draw and impress documents
                com.sun.star.beans.XPropertySet xTFPS = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                        com.sun.star.beans.XPropertySet.class, dddV);
                Any any = (Any) xTFPS.getPropertyValue("CurrentPage");
                xDrawPage = (XDrawPage) any.getObject();
            } else {
                //xModel.getCurrentController will fail if the XComponent belongs to an OLE object
                //so we need to treat it as a single page draw document
                XDrawPagesSupplier xDrawPagesSupplier = (XDrawPagesSupplier) UnoRuntime.queryInterface(
                        XDrawPagesSupplier.class, xComponent);
                if (xDrawPagesSupplier != null) {
                    Object drawPages = xDrawPagesSupplier.getDrawPages();
                    XIndexAccess xIndexedDrawPages = (XIndexAccess) UnoRuntime.queryInterface(
                            XIndexAccess.class, drawPages);
                    //get current draw page
                    Object drawPage = xIndexedDrawPages.getByIndex(0);
                    xDrawPage = (XDrawPage) UnoRuntime.queryInterface(XDrawPage.class, drawPage);
                }
            }
        } catch (Exception e) {
            System.out.println("Error trying to retrieve draw page" + e);
            e.printStackTrace(System.err);
        } finally {
            return xDrawPage;
        }
    }

    private static XAccessible makeRoot(XMultiServiceFactory msf, XModel aModel) {
        XWindow xWindow = getCurrentWindow(aModel);
        return getAccessibleObject(xWindow);
    }
    /**
     * Get the next AccessibleContext from the parent given and index given
     * @param xAccessibleContext the context whose child you want to retrieve
     * @param i the index of the child you want to retrieve
     * @return XAccessibleContext child of the parent
     */
    private static XAccessibleContext getNextContext(XAccessibleContext xAccessibleContext, int i) {
        try {
            XAccessible xAccessible = xAccessibleContext.getAccessibleChild(i);
            return xAccessible.getAccessibleContext();
        } catch (Exception e) {
             System.out.println("Error trying to retrieve draw page" + e);
            return null;
        }
    }

    /**
     * method to convert a java Image to a byte array representing a PNG image
     *
     * @param image desired image to convert
     * @return a byte array representing the given image
     */
    private byte[] imageToByteArray(Image image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedImage bimg = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
            bimg.createGraphics().drawImage(image, 0, 0, null);
            ImageIO.write(bimg, "png", baos);
            baos.flush();
            byte[] res = baos.toByteArray();
            baos.close();
            return res;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.out.println("Failure to convert image to byte array");
            return null;
        }
    }
    /**
     * Return the XUnitConversion object for the passed XComponent.
     * The XUnitConversion class is used to convert from the units of the document
     * to the pixels of the screen (Ex 10mm -> ??? pixels)
     * This will work regardless of screen dpi or the current zoom level of the doc
     * @param xComponent the component in whose context we wish to convert
     * @return the XUnitConversion class
     */
    private XUnitConversion getXUnitConversion(XComponent xComponent) {
        XUnitConversion xUnitConversion = null;
        try {
            XModel xModel = (XModel) UnoRuntime.queryInterface(XModel.class, xComponent);
            XWindow xWindow = getCurrentWindow(xModel);
            XWindowPeer xWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, xWindow);
            xUnitConversion = UnoRuntime.queryInterface(XUnitConversion.class, xWindowPeer);
        } catch (Exception e) {
            System.out.println("Error trying to get XUnitConversion" + e);
            e.printStackTrace(System.err);
        } finally {
            return xUnitConversion;
        }
    }
    /**
     * Helper class to help pass parameters through the various methods of the plugin
     */
    public class ImageInfo {
        
        private final int IMAGE_TYPE       = 0;
        private final int IMAGE_TITLE      = 1;
        private final int DISPLAY_MIN      = 2;
        private final int DISPLAY_MAX      = 3;
        private final int PLANE_NUMBER     = 4;
        private final int FILE_DESCRIPTION = 5;
        
        /*
        static final String MASS_IMAGE      = "MASS_IMAGE";
        static final String SUM_IMAGE       = "SUM_IMAGE";
        static final String RATIO_IMAGE     = "RATIO_IMAGE";
        static final String HSI_IMAGE       = "HSI_IMAGE";
        static final String COMPOSITE_IMAGE = "COMPOSITE_IMAGE";
        static final String NON_MIMSIMAGE   = "NON_MIMSIMAGE";
        */
        
        public Point p;
        public Image image;
        public XTextContent xImage;
        public XShape xShape;
        public Size size;
        public String text;
        public String title;
        public String description;
        
        //DJ: 10/17/2014
        public String imageType   = "";
        public String imageTitle  = "";
        public String displayMin  = "";
        public String displayMax  = "";
        public String planeNumber = "";
        
        public ImageInfo(Image i) {
            this.image = i;
        }
        
        public ImageInfo(Image i, String n, String t, String d) {
            this.image = i;
            this.text = n;
            this.title = t;
            
            this.description = d;
            
            size = new Size(0, 0);
            p = new Point(0, 0);
        }

        // DJ: 10/24/2014
        // an another version of the constructor where the 4th arg
        // is array of string instead of being just one string.
        // That way we could easily get each image property/description.
        public ImageInfo(Image i, String n, String t, String[] d) {
            this.image = i;
            this.text = n;
            this.title = t;
            
            //DJ: 10/24/2014
            this.imageType   = d[IMAGE_TYPE];
            this.imageTitle  = d[IMAGE_TITLE];
            this.displayMin  = d[DISPLAY_MIN];
            this.displayMax  = d[DISPLAY_MAX];
            this.planeNumber = d[PLANE_NUMBER];
            this.description = 
                    "Image Type: "   + imageType   + "\n" +
                    "Display Min: "  + displayMin  + "\n" +
                    "Display Max: "  + displayMax  + "\n" +
                    "Plane Number: " + planeNumber + "\n" +
                    d[FILE_DESCRIPTION];
            
            size = new Size(0, 0);
            p = new Point(0, 0);
        }
        
        public String getImageTitle(){
            return imageTitle;
        }
    }
    
    // DJ: 11/10/2014
    // To be used for backup purposes:
    private static class NotesSaver extends java.util.TimerTask{
        
        
        XComponent currentDocument = null;
        int count = -1;
        String notesFilepath = null;
        boolean flag = false;
        private NotesSaver notesSaver;
        java.util.Timer timer = new java.util.Timer();
        UnoPlugin unoPlugins;
        
        StringBuffer sSaveUrl = null;
        
        
        public NotesSaver(XComponent currDoc) {
            this.currentDocument = currDoc;
            this.unoPlugins = UnoPlugin.getInstance();
            //this.notesSaver = this;
        }
        
        public NotesSaver(XComponent currDoc, int counter) {
            this.currentDocument = currDoc;
            this.count = counter;
            this.unoPlugins = UnoPlugin.getInstance();
            //this.notesSaver = this;
        }

        
       public NotesSaver(XComponent currDoc, boolean flag, StringBuffer parentDir) {
            this.currentDocument = currDoc;
            this.flag = flag;
            this.unoPlugins = UnoPlugin.getInstance();
            this.sSaveUrl = parentDir;
            //this.notesSaver = this;
        }
        
        public void setFlag(boolean flag){
            this.flag = flag;
        }
        public boolean getFlag(){
            return this.flag;
        }
        public NotesSaver getinstance(){
            return this;
        }
        
        public void resetTime(){
            try {
                
                Thread.sleep((long) 30 * 1000);
            } catch (InterruptedException e) {
                System.out.println(e);
                //flag = false;
            }
        }

        @Override
        public void run() {
            
            
            //System.out.println("Scheduler called");

            while (flag) {
                
                System.out.println("Thread woke up");
                
                if (docChangesFlag) { // might add  || (!docChangesFlag && previousEvent.equals("OnModifyChanged"))
                    System.out.println("CH Flag +");
                    
                    System.out.println("Change happened within last 10 seconds ===> to save it");
                    
                   // try {
                        // we wait for 5 seconds before we do anything
                        // to be changed later on to be 10 minutes: 1000*60*10
                        //Thread.sleep((long) 5 * 1000);

                        /*
                        com.sun.star.beans.PropertyValue[] propertyValue =
                                new com.sun.star.beans.PropertyValue[1];
                        propertyValue[0] = new com.sun.star.beans.PropertyValue();
                        propertyValue[0].Name = "Hidden";
                        propertyValue[0].Value = new Boolean(true);
                        */

                        com.sun.star.frame.XStorable xStorable =
                                UnoRuntime.queryInterface(
                                com.sun.star.frame.XStorable.class, currentDocument);

                        //StringBuffer sSaveUrl = new StringBuffer("file://");
                        //StringBuffer sSaveUrl = new StringBuffer("");
                        //sSaveUrl.append("/nrims/home3/djsia/Desktop/backupTestFolder/");

                         com.sun.star.beans.PropertyValue[] propertyValue = new com.sun.star.beans.PropertyValue[1];
                        propertyValue[0] = new com.sun.star.beans.PropertyValue();
                        propertyValue[0].Name = "Overwrite";
                        propertyValue[0].Value = true;
                        /*
                        propertyValue[1] = new com.sun.star.beans.PropertyValue();
                        propertyValue[1].Name = "FilterName";
                        propertyValue[1].Value = "StarOffice XML (Writer)";
                        */

                        try {

                            DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            notesFilepath = "file://" + sSaveUrl.toString() + notesFileName + "_" + dateFormat.format(cal.getTime()) + ".odt";
                            xStorable.storeToURL(notesFilepath, propertyValue);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //xStorable.storeToURL( sSaveUrl.toString(), propertyValue );

                        System.out.println("\nNOTES SAVED ==> " + notesFilepath + "\n");

                        docChangesFlag = false; 
                        
                        //this.flag = false;
                } else {
                    System.out.println("CH Flag -");
                }
                try {
                    Thread.sleep((long) SLEEP_TIME * 1000);  // in milliseconds
                } catch (InterruptedException e) {
                    System.out.println(e);
                    //       flag = false;
                }

            }
        }
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    
    // DJ: 11/10/2014
    // To be used for backup purposes:
    private static class NotesCopier implements Runnable{
        
        String sourcePath = "";
        String destinationPath = "";
        private volatile boolean running = true;
        
        public NotesCopier(String srcPath, String dstPath) {
            this.sourcePath = srcPath;
            this.destinationPath = dstPath;
        }
        // the new proper way to stop a thread is by setting upa avariable for check
        public void terminate() {
            running = false;
        }
        
        @Override
        public void run() {
            while (running) {
                try {
                    File sourceFile = new File(this.sourcePath);
                    File destinFile = new File(this.destinationPath);
                    //System.out.println("notes file is : " + sourceFile.getAbsolutePath());
                    //System.out.println("backupFile file is : " + destinFile.getAbsolutePath());
                    //org.apache.commons.io.FileUtils.copyFile(sourceFile, destinFile);
                    
                    
                    // The "false" argument means that we don't preserve the original file date but rather have the
                    // time where the actual coying happens.
                    org.apache.commons.io.FileUtils.copyFile(sourceFile, destinFile, false);
                    
                    System.out.println("\nNOTES BACKEDUP BEFORE THE FINAL SAVE ==> " + destinFile + "\n");
                    running = false;


                } catch (java.io.IOException ex) {
                    Logger.getLogger(UnoPlugin.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
           //System.out.println("copy file process done");
        }
    }
    
}
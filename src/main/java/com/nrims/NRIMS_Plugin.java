package com.nrims;

/*
 * NRIMS_Plugin.java
 *
 * Created on May 1, 2006, 12:34 PM
 *
 * @author Douglas Benson
 */
import com.nrims.data.FileUtilities;
import com.nrims.logging.OMLogger;
import ij.plugin.PlugIn;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import it.sauronsoftware.junique.MessageHandler;
import java.awt.EventQueue;
import java.io.File;
import java.util.logging.Logger;
//Program startup flow
//
//If being called from Netbeans or the main class is being called in an executable (runUI) then we will hit main first
//which creates the nrimsPlugin class, the construction of which does nothing but log some info
//it calls nrimsPlugin.run() and passes it the arguments recieved in main
//alternatively, nrimsPlugin.run() is called directly by the ImageJ instance, and no arguments are passed
//run will parse the arguments and configure the UI globals
//then it will create a new instance of UI and set it visible
//it then checks if there are any arguments passed from ImageJ
//then passes those to UI.run(), which only checks for the testing flag.

public class NRIMS_Plugin implements PlugIn {

    private final static Logger OMLOGGER = OMLogger.getOMLogger(NRIMS_Plugin.class.getName());
    private static boolean isTesting = false;
    /*
     * Private stings for option parsing
     */
    private static final String IMFILE_OPTION = "-imfile";
    private static final String SINGLE_INSTANCE_OPTION = "-single_instance";
    private static final String FIJI_RUN_COMMAND = "run(\"Open MIMS Image\"";
    private static final String IMAGEJ_RUN_COMMAND = "-run";
    private static String im_file_path = null;

    /**
     * Creates a new instance of NRIMS_Plugin Analysis Module.
     */
    public NRIMS_Plugin() {
        OMLOGGER.info("");
        OMLOGGER.info("NRIMS constructor: id=" + System.identityHashCode(this));
        //Originally we constructed the UI here, but due to the combining of the main UI method and NRIMS_Plugin,
        //we need to move the UI contruction AFTER the reading of arguments no matter the case, in order that
        //the custom tools be installed
    }

    /**
     * Opens the GUI.
     */
    @Override
    public void run(String arg) {
        String options = ij.Macro.getOptions();

        if (options != null || arg != null) {//if there are args, we are being called from main
            if (arg != null && options == null) {
                options = arg;
            }
            OMLOGGER.info("args: " + options);
            String[] args = FileUtilities.splitArgs(options.trim());
            Boolean skip_next = false;
            for (int i = 0; i < args.length; i++) {
                OMLOGGER.fine(args[i]);
                if (args[i] == null) {
                    continue;
                }
                if (!skip_next) {
                    // Testing should work inside and outside IDE.
                    if (args[i].equals("-t")) {
                        OMLOGGER.fine("Testing mode");
                        isTesting = true;
                        /* Debuging locale issues with MimsJTable,
                         * eg "1.23" vs "1,23". No real issues in OpenMIMS
                         * but downstream issues, eg in Excel...
                         * needs imports
                         * java.util.Locale and javax.swing.JComponent
                         */
                        //JComponent.setDefaultLocale(Locale.US);
                        //JComponent.setDefaultLocale(Locale.GERMANY);

                    }
                    // Development doesn't work outside IDE
                    if (args[i].equals("-d")) {
                        System.getProperties().setProperty("plugins.dir", "lib/plugins");
                        //It appears unnessecary to explicity set macros.dir, 
                        //gets set to lib/plugins/macros when instance of ij created?
                        //calls to getProperty in UI() constructor have non null values
                        //
                        //System.getProperties().setProperty("macros.dir", "lib/plugins/macros");
                        OMLOGGER.fine("plugins.dir = " + System.getProperty("plugins.dir"));
                    }
                    if (args[i].startsWith("-ijpath") && i + 1 < args.length) {
                        //Prefs.setHomeDir(args[i+1]);
                        skip_next = true;
                    }
                    // NO LONGER SUPPORTED
                    // The use of the "-imFile" flag is no longer supported or required.
                    if ((args[i].equals(IMFILE_OPTION)) && i + 1 < args.length) {
                        im_file_path = args[i + 1];
                        skip_next = true;
                    }
                    if (args[i].startsWith(FIJI_RUN_COMMAND)) {
                        int q1 = args[i].indexOf("\"");
                        int q2 = args[i].indexOf("\"", q1 + 1);
                        int q3 = args[i].indexOf("\"", q2 + 1);
                        int q4 = args[i].indexOf("\"", q3 + 1);
                        if (q3 > 0 && q4 > 0) {
                            im_file_path = args[i].substring(q3 + 1, q4);
                        }
                    }
                    if (args[i].startsWith(IMAGEJ_RUN_COMMAND)) {
                        skip_next = true;
                    }
                    if ((args[i].equals(SINGLE_INSTANCE_OPTION))) {
                        OMLOGGER.fine("Single instance mode");
                        ui.single_instance_mode = true;
                    }
                    if ((new File(args[i])).exists()) {
                        im_file_path = args[i];
                    }
                } else {
                    skip_next = false;
                }
            }
            String id = UI.class.getName();
            boolean already_open = false;
            if (ui.single_instance_mode) {
                try {
                    JUnique.acquireLock(id, new MessageHandler() {
                        @Override
                        public String handle(String message) {
                            if (ui != null) {
                                UI thisui = ui;
                                thisui.setVisible(true);
                                thisui.toFront();
                                thisui.openFileInBackground(new File(message));
                            }
                            return null;
                        }
                    });
                } catch (AlreadyLockedException e) {
                    already_open = true;
                }
            }
            if (ui.single_instance_mode && already_open) {
                JUnique.sendMessage(id, im_file_path);
                ij.IJ.getInstance().quit();
                return;
            } else {
                ui = new UI();
                OMLOGGER.fine("im_file_path = " + im_file_path);
                if (im_file_path != null) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            String temp_path = im_file_path;
                            File[] files_arr = new File[1];
                            ui.setVisible(true);
                            files_arr[0] = new File(temp_path);
                            File file_to_open = files_arr[0];
                            ui.openFileInBackground(file_to_open);
                        }
                    });
                } else {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (isTesting) {
                                ui.initComponentsTesting();
                            }
                            ui.setVisible(true);
                        }
                    });
                }
            }
        } else {
            //UI will be constructed regardless, but either after parsing args or immeadiately if none
            ui = new UI();
        }
        if (ui != null) {//need to check whether or not ui is null, which can happen when a single instance mode is already open
//            //Start Auto save thread for ROI
//            Thread t = new Thread(new FileUtilities.AutoSaveROI(ui));
//            t.start();

            OMLOGGER.info("NRIMS.run");
            OMLOGGER.info(Thread.currentThread().getName());
            //NOTE: I've commented out the below because upon further inspection, ui.run doesn't do anything useful
            /*
             options = ij.Macro.getOptions();
             OMLOGGER.info("options: " + options);
             if (options != null) {
             options = options.trim();
             System.out.println("options=" + options + ",");
             }

             ui.run(options);
             */
            if (ui.isVisible() == false) {
                ui.setVisible(true);
            }
        }
    }

    //Change missing from svn->git migration
    //TODO, was this added for scripting access to UI?
    public static UI getUI() {
        return ui;
    }
    //end

    private static com.nrims.UI ui = null;
}

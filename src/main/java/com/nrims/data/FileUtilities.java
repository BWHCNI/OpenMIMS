/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.data;

import com.nrims.CompositeProps;
import com.nrims.HSIProps;
import com.nrims.MassProps;
import com.nrims.MimsAction;
import com.nrims.MimsJFileChooser;
import com.nrims.MimsPlus;
import com.nrims.MimsStackEditor;
import com.nrims.RatioProps;
import com.nrims.SumProps;
import com.nrims.UI;
import static com.nrims.UI.HSI_EXTENSION;
import static com.nrims.UI.MIMS_EXTENSION;
import static com.nrims.UI.NRRD_EXTENSION;
import static com.nrims.UI.RATIO_EXTENSION;
import static com.nrims.UI.ROIS_EXTENSION;
import static com.nrims.UI.SESSIONS_EXTENSION;
import static com.nrims.UI.SUM_EXTENSION;
import static com.nrims.UI.COMPOSITE_EXTENSION;
import ij.ImageStack;
import ij.gui.Roi;
import java.beans.DefaultPersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.jfree.data.xy.XYDataset;

/**
 *
 * @author wang2
 */
public class FileUtilities {

    /**
     * getNext: modified version of getNext function from NextImageOpener plugin in ImageJ, modified for OpenMIMS
     *
     * @param path the path to an image file
     * @param imageName image name
     * @param forward if true, makes the candidate the index of the next file 
     * @return fullpath to next item e.g. home/user/nextfile.nrrd
     */
    public static String getNext(String path, String imageName, boolean forward) {
        File dir = new File(path);
        if (!dir.isDirectory()) {
            return null;
        }
        String[] names = dir.list();
        ij.util.StringSorter.sort(names);
        int thisfile = -1;
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(imageName)) {
                thisfile = i;
                break;
            }
        }
        //System.out.println("OpenNext.thisfile:" + thisfile);
        if (thisfile == -1) {
            return null;// can't find current image
        }
        // make candidate the index of the next file
        int candidate = thisfile + 1;
        if (!forward) {
            candidate = thisfile - 1;
        }
        if (candidate < 0) {
            candidate = names.length - 1;
        }
        if (candidate == names.length) {
            candidate = 0;
        }
        // keep on going until an image file is found or we get back to beginning
        while (candidate != thisfile) {
            String nextPath = path + "/" + names[candidate];
            //System.out.println("OpenNext: "+ candidate + "  " + names[candidate]);
            File nextFile = new File(nextPath);
            boolean canOpen = true;
            if (names[candidate].startsWith(".") || nextFile.isDirectory()) {
                canOpen = false;
            }
            if (canOpen) {
                String fileName = nextFile.getName();
                if (fileName.endsWith(NRRD_EXTENSION) || fileName.endsWith(MIMS_EXTENSION)) {
                } else {
                    canOpen = false;
                }
            }
            if (canOpen) {
                return nextPath;
            } else {// increment again
                if (forward) {
                    candidate = candidate + 1;
                } else {
                    candidate = candidate - 1;
                }
                if (candidate < 0) {
                    candidate = names.length - 1;
                }
                if (candidate == names.length) {
                    candidate = 0;
                }
            }

        }
        return null;
    }

    /**
     * Helper function for reading .sum/.ratio/.hsi files from xml.
     *
     * @param file the file to read
     * @return the object read from the file
     */
    public static Object readObjectFromXML(File file) {
        Object obj = null;
        try {
            XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)));
            obj = xmlDecoder.readObject();
            xmlDecoder.close();
        } catch (Exception e) {
            try {
                FileInputStream f_in = new FileInputStream(file);
                ObjectInputStream obj_in = new ObjectInputStream(f_in);
                obj = obj_in.readObject();
            } catch (Exception ex) {
                obj = null;
            }
        } finally {
            return obj;
        }
    }

    /**
     * Takes a props object and adds it to a zip. Used in Save Session. depends on no globals, can probably move to new
     * helper class
     *
     * @param zos the ZipOutputStream for the final zip
     * @param toWrite the props object to be written
     * @param filenames the filenames of similar props objects
     * @param extension the extension for the props object
     * @param numName mass string for the numerator (or if a sum of a mass image, the parent)
     * @param denName mass string for the numerator (or if a sum of a mass image, -1)
     * @param filename the name of the .nrrd file
     * @param i index of the object in filenames
     * @return true if save succeeded, or false if an error was thrown
     */
    public static boolean saveToXML(ZipOutputStream zos, Object toWrite, String[] filenames, String extension, String numName, String denName, String filename, int i) {
        try {
            int numMass = Math.round(new Float(numName));
            int denMass = Math.round(new Float(denName));
            if (denMass != -1) {
                filenames[i] = filename + "_m" + numMass + "_m" + denMass;
            } else {
                filenames[i] = filename + "_m" + numMass;
            }
            int numBefore = 0;
            for (int j = 0; j < i; j++) {
                if (filenames[i].equals(filenames[j])) {
                    numBefore++;
                }
            }
            String post = "";
            if (numBefore > 0) {
                post = "(" + numBefore + ")";
            }
            zos.putNextEntry(new ZipEntry(filenames[i] + post + extension));
            XMLEncoder e = new XMLEncoder(zos);
            //need to modify persistance delegate to deal with constructor in SumProps which takes parameters
            e.setPersistenceDelegate(CompositeProps.class, new DefaultPersistenceDelegate(new String[]{"imageProps"}));
            e.setPersistenceDelegate(MassProps.class, new DefaultPersistenceDelegate(new String[]{"massIdx"}));
            e.setPersistenceDelegate(RatioProps.class, new DefaultPersistenceDelegate(new String[]{"numMassIdx", "denMassIdx"}));
            e.setPersistenceDelegate(HSIProps.class, new DefaultPersistenceDelegate(new String[]{"numMassIdx", "denMassIdx"}));
            e.setPersistenceDelegate(SumProps.class, new DefaultPersistenceDelegate(new String[]{"parentMassIdx", "numMassIdx", "denMassIdx"}));
            e.writeObject(toWrite);
            e.flush();
            //need to append "</java>" to the end because flushing doesn't "complete" the file like close does
            //but we cannot close or else the entire ZipOutputstream closes
            DataOutputStream d_out = new DataOutputStream(zos);
            d_out.writeBytes("</java>");
            d_out.flush();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Given a zip file, extracts XML files from within and converts to Objects. Used in opening .session.zip files
     * Depends on no globals, can probably move to new helper class
     *
     * @param file absolute file path
     * @return array list containing Objects.
     */
    public static ArrayList openXMLfromZip(File file) {
        ArrayList entries = new ArrayList();
        Object obj;
        try {
            ZipFile z_file = new ZipFile(file);
            ZipInputStream z_in = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry;
            XMLDecoder xmlDecoder;
            while ((entry = z_in.getNextEntry()) != null) {
                xmlDecoder = new XMLDecoder(z_file.getInputStream(entry));
                obj = xmlDecoder.readObject();
                entries.add(obj);
                xmlDecoder.close();
            }
            z_in.close();
        } catch (Exception e) {
            return null;
        } finally {
            return entries;
        }
    }

    /**
     * AutoSaveROI is the thread which is responsible for autosaving the ROI's
     */
    public static class AutoSaveROI implements Runnable {

        UI ui;

        public AutoSaveROI(UI ui) {
            this.ui = ui;
        }

        public void run() {
            for (;;) {
                try {
                    // Save the ROI files to zip.
                    //String roisFileName = System.getProperty("java.io.tmpdir") + "/" + ui.getImageFilePrefix();
                    String type;
                    String extension = ui.getImageFileExtension();
                    if (extension.compareTo(NRRD_EXTENSION) == 0) {
                        type = NRRD_EXTENSION;

                    } else {
                        type = MIMS_EXTENSION;
                    }
                    String fileType = null;
                    if (type.startsWith(".")) {
                        fileType = type.substring(1);
                    }
                    
                    File roisTempDir = ui.getTempDir();
                    // roisTempDir is sometimes not writable.  This should be dealt with elsewhere.
                    if (roisTempDir != null) {
                        String roisFileName = roisTempDir.toString() + System.getProperty("file.separator") +
                                ui.getImageFilePrefix() + "-" + fileType;
                        Roi[] rois = ui.getRoiManager().getAllROIs();
                        if (rois.length > 0 && ui.getRoiManager().needsToBeSaved()) {
                            checkSave(roisFileName + ROIS_EXTENSION, roisFileName, 1);
                            ui.getRoiManager().saveMultiple(rois, roisFileName + ROIS_EXTENSION, false);
                            ui.getRoiManager().setNeedsToBeSaved(false);                       
                            System.out.println("autosaved rois to filename " + roisFileName + ROIS_EXTENSION);
                            LocalTime currentTime = LocalTime.now();   // 13:02:40.317
                            String hrsec = currentTime.truncatedTo(ChronoUnit.SECONDS).toString();
                            DateTimeFormatter USFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                            LocalTime lt = LocalTime.parse(hrsec, USFormatter);
                            String lastAutosave = new String("Last autosave " + lt);
                            ui.getRoiManager().showAutoSaveLabel(lastAutosave);                          
                            //threadMessage("Autosaved at "+ roisFileName + ROIS_EXTENSION);
                        } else {
                            //threadMessage("Nothing to autosave");
                        }
                    }
                    Thread.sleep(ui.getInterval());
                } catch (InterruptedException e) {
                    //ui.threadMessage("Autosave thread interrupted");
                    break;
                }
            }
        }
    }

    /**
     * Recursively check for files with the same name as specified and save those other files as other names. Used in
     * autosave ROI to keep a backlog of autosaves Ex. my_rois.rois, my_rois(1).rois, my_rois(2).rois
     *
     * @param toSave full path and filename of filename you want to use
     * @param filename only the filename you want to use
     * @param n how many duplicates you have encountered so far
     * @return new name of the file that doesn't conflict with any others
     */
    public static boolean checkSave(String toSave, String filename, int n) {
        File file = new File(toSave);
        String newFilename = filename + "(" + n + ")" + ROIS_EXTENSION;
        // File (or directory) with new name
        File file2 = new File(newFilename);
        if (file2.exists() && n < 10) {
            checkSave(newFilename, filename, n + 1);
        }
        if (file.exists()) {
            boolean success = file.renameTo(file2);
            if (success) {
                file = new File(toSave);
                file.delete();
            }
        }
        return true;
    }

    public static String getMachineName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    /**
     * Returns the prefix of any file name. For example: /tmp/test_file.im = /tmp/test_file
     *
     * @param fileName the file name for which to return a prefix
     * @return prefix file name.
     */
    public static String getFilePrefix(String fileName) {
        String prefix = fileName.substring(0, fileName.lastIndexOf("."));
        return prefix;
    }

    /**
     * Method to save all additional data such as sum images, ratios images, rois, etc.
     *
     * @param baseFileName the filename and path of the .nrrd or .im file e.g. /home/user/myfile.nrrd
     * @param onlyFileName the filename of the .nrrd or .im file without the extension or folders e.g. "myfile"
     * @param ui the <code>UI</code> instance
     * @return true if succeeded, false if not
     */
    public static boolean saveAdditionalData(String baseFileName, String onlyFileName, UI ui) {
        String dataFileName = onlyFileName + NRRD_EXTENSION;
        File sessionFile = null;
        Opener image = ui.getOpener();
        Roi[] rois = ui.getRoiManager().getAllROIs();
        MimsPlus ratio[] = ui.getOpenRatioImages();
        MimsPlus hsi[] = ui.getOpenHSIImages();
        MimsPlus sum[] = ui.getOpenSumImages();
        MimsPlus comp[] = ui.getOpenCompositeImages();
        String[] names = image.getMassNames();
        // Save the ROI files to zip.
        if (rois.length > 0) {
            System.out.println("ROIS exist, going to open window");
            
            String type;
            String extension = ui.getImageFileExtension();
            if (extension.compareTo(NRRD_EXTENSION) == 0) {
                type = NRRD_EXTENSION;

            } else {
                type = MIMS_EXTENSION;
            }
            String fileType = null;
            if (type.startsWith(".")) {
                fileType = type.substring(1);
            }
            File roisTempDir = ui.getTempDir();
            String roisFileName = roisTempDir.toString() + System.getProperty("file.separator") +
                    ui.getImageFilePrefix() + "-" + fileType;                                   
                                    
            if ((sessionFile = checkForExistingFiles(ROIS_EXTENSION, baseFileName, "Mims roi zips", ui)) != null) {    
                ui.getRoiManager().saveMultiple(rois, roisFileName + ROIS_EXTENSION, false);
                //baseFileName = getFilePrefix(getFilePrefix(sessionFile.getAbsolutePath()));
                //ui.getRoiManager().saveMultiple(rois, baseFileName + ROIS_EXTENSION, false);
            } else {
                System.out.println("Saving roi.zip canceled.");
            }
        }
        //serialize all special images to XML
        if (ratio.length + hsi.length + sum.length + comp.length > 0) {
            /* Get the desired save name of the session.zip file. By default it is filename.session.zip, but 
             * the file prompt window will always appear so the user can change it. If another filename.session.zip
             * exists, then the default name will be filename(1).session.zip, etc.
             */
            if ((sessionFile = checkForExistingFiles(SESSIONS_EXTENSION, baseFileName, "Mims session files", ui)) != null) {
                baseFileName = getFilePrefix(getFilePrefix(sessionFile.getAbsolutePath()));
                onlyFileName = getFilePrefix(getFilePrefix(sessionFile.getName()));
            } else {
                System.out.println("Saving session.zip canceled.");
            }
            try {
                ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(baseFileName + SESSIONS_EXTENSION)));
                // Contruct a unique name for each ratio image and save.
                if (ratio.length > 0) {
                    String[] filenames = new String[ratio.length];
                    for (int i = 0; i < ratio.length; i++) {
                        RatioProps ratioprops = ratio[i].getRatioProps();
                        ratioprops.setDataFileName(dataFileName);
                        if (!FileUtilities.saveToXML(zos, ratioprops, filenames,
                                RATIO_EXTENSION, names[ratioprops.getNumMassIdx()], names[ratioprops.getDenMassIdx()], onlyFileName, i)) {
                            return false;
                        }
                    }
                }

                // Contruct a unique name for each hsi image and save.
                if (hsi.length > 0) {
                    String[] filenames = new String[hsi.length];
                    for (int i = 0; i < hsi.length; i++) {
                        HSIProps hsiprops = hsi[i].getHSIProps();
                        hsiprops.setDataFileName(dataFileName);
                        if (!FileUtilities.saveToXML(zos, hsiprops, filenames,
                                HSI_EXTENSION, names[hsiprops.getNumMassIdx()], names[hsiprops.getDenMassIdx()], onlyFileName, i)) {
                            return false;
                        }
                    }
                }
                //Construct a unique name of each composite image and save
                if (comp.length > 0) {
                    String[] filenames = new String[comp.length];
                    for (int i = 0; i < comp.length; i++) {
                        CompositeProps compprops = comp[i].getCompositeProps();
                        compprops.setDataFileName(dataFileName);
                        if (!FileUtilities.saveToXML(zos, compprops, filenames,
                                COMPOSITE_EXTENSION, i + "", "-1", onlyFileName, i)) {
                            return false;
                        }
                    }
                }
                // Contruct a unique name for each sum image and save.
                if (sum.length > 0) {
                    String[] filenames = new String[sum.length];
                    for (int i = 0; i < sum.length; i++) {
                        SumProps sumProps = sum[i].getSumProps();
                        sumProps.setDataFileName(dataFileName);
                        if (sumProps.getSumType() == 1) {
                            if (!FileUtilities.saveToXML(zos, sumProps, filenames,
                                    SUM_EXTENSION, names[sumProps.getNumMassIdx()], names[sumProps.getDenMassIdx()], onlyFileName, i)) {
                                return false;
                            }
                        } else if (sumProps.getSumType() == 0) {
                            if (!FileUtilities.saveToXML(zos, sumProps, filenames,
                                    SUM_EXTENSION, names[sumProps.getParentMassIdx()], "-1", onlyFileName, i)) {
                                return false;
                            }
                        } else {
                            continue;
                        }
                    }
                }
                //close the zip file
                zos.flush();
                zos.close();
            } catch (Exception e) {
                System.out.println("Error saving session file:" + e);
                return false;
            }
        } else {
            JOptionPane.showConfirmDialog(null, "A session zip file will not be saved, since no \n"
                + "HSI, ratio, sum, or composite images have been created.  ", 
                "No new data to save", 
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE);
        }
        return true;
    }

    /**
     * checks within folder of filename if filename exists. Depends on no globals, can probably move to new helper class
     *
     * @param extension which extension we want to save ass
     * @param filename the filename we want to save as (Ex. "/tmp/test_file"
     * @param description description of the type of file (Ex. "Mims session file")
     * @return a File representing the new file (which has not been created yet)
     */
    private static File checkForExistingFiles(String extension, String filename, String description, UI ui) {
        String baseFileName = filename;
        File f = new File(baseFileName + extension);
        int counter = 0;
        if (f.exists()) {
            while (f.exists()) {
                counter++;
                f = new File(baseFileName + "(" + counter + ")" + extension);
            }
            baseFileName += "(" + counter + ")";
            MimsJFileChooser fc = new MimsJFileChooser(ui);
            MIMSFileFilter session = new MIMSFileFilter(extension.substring(1));
            session.setDescription(description);
            fc.setFileFilter(session);
            fc.setSelectedFile(new java.io.File(baseFileName + extension));

            int returnVal = fc.showSaveDialog(ui);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                if (extension == SESSIONS_EXTENSION || extension == ROIS_EXTENSION) {
                    baseFileName = getFilePrefix(getFilePrefix(fc.getSelectedFile().getAbsolutePath()));
                } else {
                    baseFileName = getFilePrefix(fc.getSelectedFile().getAbsolutePath());
                }
            } else {
                return null;
            }
            f = new File(baseFileName + extension);
        }
        return f;
    }

    public static String joinArray(String[] args) {
        String output = "";
        for (int i = 0; i < args.length; i++) {
            if (i != 0) {
                output += " ";
            }
            output += args[i];
        }
        return output;
    }

    /**
     * Method to parse arguments passed in startup of OpenMIMS
     *
     * @param args a string of arguments to be parsed
     * @return a string of parsed arguments
     */
    public static String[] splitArgs(String args) {
        String SINGLE_INSTANCE_OPTION = "-single_instance";
        String FIJI_RUN_COMMAND = "run(\"Open MIMS Image\"";
        String IMAGEJ_RUN_COMMAND = "-run";
        String im_file_path = null;
        ArrayList<String> optArgs = new ArrayList<String>();
        String[] splitArgs = args.split(" ");
        String leftover = "";
        //In case the file has spaces in the name, we need to check all invalid flags and their following combinations
        for (int i = 0; i < splitArgs.length; i++) {
            String checkArg = leftover + splitArgs[i];
            if (checkArg.equals("-t") || checkArg.equals("-d") || checkArg.equals(SINGLE_INSTANCE_OPTION) || (new File(checkArg)).exists()) {
                optArgs.add(checkArg);
                leftover = "";
            } else if (checkArg.startsWith("-ijpath") && i + 1 < splitArgs.length) {
                optArgs.add(checkArg);
                i++;
                leftover = "";
            } else if (checkArg.startsWith(IMAGEJ_RUN_COMMAND)) {
                optArgs.add(checkArg);
                i++;
                leftover = "";
            } else //if the flag is not valid, there could be the possibility that it is a filename, so we save it
            {
                if (leftover.equals("")) {
                    leftover = splitArgs[i] + " ";
                } else {
                    leftover = leftover + " " + splitArgs[i] + " ";
                }
            }
        }
        return (String[]) optArgs.toArray(new String[0]);
    }

    /**
     * Helper method to eliminate extra null values in MimsPlus arrays. Useful for the arrays returned by
     * getMassImages(), etc.
     *
     * @param massImages an array of <code>MimsPlus</code> instances
     * @return array containing only the MimsPlus
     */
    public static MimsPlus[] slimImageArray(MimsPlus[] massImages) {
        ArrayList<MimsPlus> images = new ArrayList<MimsPlus>();
        for (int i = 0; i < massImages.length; i++) {
            if (massImages[i] != null) {
                images.add(massImages[i]);
            }
        }
        return (MimsPlus[]) images.toArray(new MimsPlus[0]);
    }

    /**
     * Helper method to check the permissions of a given directory/directory file is contained in. Useful for when you
     * are trying to save something. If the folder is not writable, then the function prompts the user to select a new
     * directory
     *
     * @param folder the directory whose write permission are in question, or file to check it's parent directory
     * @param ui main UI window, needed to display error messages
     * @param msg customized message for the user to describe what you're using the folder for.
     * @return the parent folder if one can be written to, or null if the users opts out of choosing one.
     */
    public static File checkWritePermissions(File folder, UI ui, String msg) {
        if (!folder.isDirectory()) {
            folder = folder.getParentFile();
        }
        while (!folder.canWrite()) {
            int n = JOptionPane.showConfirmDialog(
                    ui, msg,
                    "No write permissions",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (n == JOptionPane.NO_OPTION) {
                return null;
            }
            if (n == JOptionPane.YES_OPTION) {
                JFileChooser chooser = new JFileChooser(folder);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Select target directory");
                int returnVal = chooser.showOpenDialog(ui);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    folder = chooser.getSelectedFile();
                } else {
                    return null;
                }
            }
            if (n == JOptionPane.CANCEL_OPTION) {
                return null;
            }
        }
        return folder;
    }

    /**
     * Used to sum a list of images, then concatenate the sums. Useful for series of images. Images will be stacked in
     * order of array
     *
     * @param files the files you want to stack
     * @param originalUI the <code>UI</code> reference
     * @return the saved file in which the stack of images is located
     */
    public static File stackImages(File[] files, UI originalUI) {
        UI ui = new UI();
        File targetDirectory;
        File tempDirectory;
        String originalName = "";
        ArrayList<File> tempFiles = new ArrayList<File>();
        String[] names = new String[files.length];
        if (files.length > 0) {
            originalName = getFilePrefix(files[0].getName());
            targetDirectory = checkWritePermissions(files[0].getParentFile(), originalUI,
                    "Current user lacks write permissions to save in current folder. Choose another folder?");
            if (targetDirectory == null) {
                return null;
            }
            tempDirectory = checkWritePermissions(new File(System.getProperty("java.io.tmpdir")), originalUI,
                    "Current user lacks write permissions to write temporary stack work to system tmp folder. Choose another folder to do work in?");
            if (tempDirectory == null) {
                return null;
            }
            for (int i = 0; i < files.length; i++) {
                File imFile = files[i];
                names[i] = imFile.getName();
                if (ui.openFile(imFile)) {
                    String name = getFilePrefix(imFile.getName());
                    MimsStackEditor mimStack = ui.getMimsStackEditing();
                    MimsPlus[] images = slimImageArray(ui.getMassImages());
                    int blockSize = images[0].getNSlices();
                    //compress all mass images in file into sum images
                    Boolean done = mimStack.compressPlanes(blockSize);
                    massCorrection massCorr = new massCorrection(ui);
                    //force all images to be float in order for them to be compatible for concatenation
                    massCorr.forceFloatImages(images);
                    if (done) {
                        //save the resulting sum images into a temporary file
                        Nrrd_Writer nw = new Nrrd_Writer(ui);
                        File dataFile = nw.save(images, tempDirectory.getPath(), "comp_" + name + ".nrrd");
                        tempFiles.add(dataFile);
                    }
                }
            }
            //open up the first file to do our work in
            ui.openFile(tempFiles.get(0), false);
            MimsPlus[] images;

            for (int i = 0; i < tempFiles.size(); i++) {
                File tempFile = tempFiles.get(i);
                images = slimImageArray(ui.getMassImages());
                if (i != 0) {
                    Opener image = ui.getOpener();
                    UI tempUi = new UI();
                    tempUi.openFile(tempFile, false);
                    Opener tempImage = tempUi.getOpener();

                    MimsStackEditor mimStack = ui.getMimsStackEditing();
                    if (ui.getOpener().getNMasses() == tempImage.getNMasses()) {
                        if (mimStack.sameResolution(image, tempImage)) {
                            if (mimStack.sameSpotSize(image, tempImage)) {
                                //Concatenate the images if all conditions are met
                                mimStack.concatImages(false, tempUi);
                            } else {
                                JOptionPane.showMessageDialog(originalUI,
                                        "Images do not have the same spot size.",
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                return null;
                            }
                        } else {
                            JOptionPane.showMessageDialog(originalUI,
                                    "Images are not the same resolution.",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                    } else {
                        JOptionPane.showMessageDialog(originalUI,
                                "Files have different number of masses.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                    //close all temporary images and kill the tempUI
                    MimsPlus[] tempImages = tempUi.getOpenMassImages();
                    for (int j = 0; j < tempImages.length; j++) {
                        if (tempImages[j] != null) {
                            tempImages[j].setAllowClose(true);
                            tempImages[j].close();
                        }
                    }
                    tempUi = null;
                    tempFile.delete();
                    ui.getMimsData().setHasStack(true);
                }
            }
            images = slimImageArray(ui.getMassImages());
            for (int j = 0; j < images.length; j++) {
                ImageStack imageStack = images[j].getImageStack();
                for (int i = 0; i < names.length; i++) {
                    imageStack.setSliceLabel(names[i], i + 1);
                    images[j].setStack(imageStack);
                }

            }
            ui.getOpener().setStackPositions(names);
            Nrrd_Writer nw = new Nrrd_Writer(ui);
            images = slimImageArray(ui.getMassImages());
            ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
            ij.WindowManager.repaintImageWindows();
            tempFiles.get(0).delete();
            //save the file and return it
            File file = nw.save(images, targetDirectory.getPath(), "stack_" + originalName + ".nrrd");
            for (int j = 0; j < images.length; j++) {
                if (images[j] != null) {
                    images[j].setAllowClose(true);
                    images[j].close();
                }
            }
            ui = null;
            return file;
        }
        return null;
    }

    /**
     * Get the mosaic file in which the argument file is contained within. Note: searches the direct parent directories
     * and all .nrrd files directly within those directories. It will not recursively search the sibling/uncle
     * directories.
     *
     * @param file a mosaic file reference
     * @return the mosaic file if found, or null if not found.
     */
    public static File getMosaic(File file) {
        File parent = file.getParentFile();
        while (parent != null) {
            File[] siblings = parent.listFiles();
            for (File sibling : siblings) {
                if (sibling.getPath().endsWith(NRRD_EXTENSION)) {
                    try {
                        HashMap<String, String> headerInfo = FileUtilities.getHeaderInfo(sibling);
                        if (headerInfo.containsKey("mims_tile_positions") && headerInfo.get("mims_tile_positions").contains(file.getName())) {
                            return sibling;
                        }
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
            parent = parent.getParentFile();
        }
        return null;
    }

    /**
     * Open a file in a new UI window
     *
     * @param file the file to be opened
     * @param ui the <code>UI</code> instance
     */
    public static void openInNewUI(File file, UI ui) {
        UI ui_new = new UI();
        ui_new.setLocation(ui.getLocation().x + 35, ui.getLocation().y + 35);
        ui_new.setVisible(true);
        ui_new.openFile(file);

        // DJ: 07/24/2014 - "ui.getOpenMassProps()" added due to the new mass_props arg we added to restoreState
        // DJ: 08/01/2014 - "ui.getOpenCompositeProps()" added due to the new composite_props arg we added to restoreState
        ui_new.restoreState(ui.getOpenMassProps(), ui.getOpenRatioProps(), ui.getOpenHSIProps(), ui.getOpenSumProps(), ui.getOpenCompositeProps(), false, false);

        ui_new.getHSIView().useSum(ui.getIsSum());
        ui_new.getHSIView().medianize(ui.getMedianFilterRatios(), ui.getMedianFilterRadius());
    }

    /**
     * Pull the header info from a .nrrd file into a HashMap.
     *
     * @param file a .nrrd file instance
     * @return headerInfo a HashMap containing the header information for the file.
     * @throws IOException if an error occurred while reading the file
     */
    public static HashMap<String, String> getHeaderInfo(File file) throws IOException {
        HashMap<String, String> headerInfo = new HashMap<String, String>();

        // Need RAF in order to ensure that we know file offset.
        RandomAccessFile in = new RandomAccessFile(file.getAbsolutePath(), "r");

        // Initialize some strings.
        String thisLine, noteType, noteValue, noteValuelc;
        int lineskip = 0;
        String commentString = "";
        // Parse the header file, until reach an empty line.
        while (true) {
            thisLine = in.readLine();
            if (thisLine == null || thisLine.equals("")) {
                break;
            }
            if (thisLine.indexOf("#") == 0) {
                commentString += thisLine.substring(1) + "\n";
            } else if (thisLine.contains(":")) {
                String[] keyValuePair = thisLine.split("/:(.+)?/");
                String key = thisLine.substring(0, thisLine.indexOf(":")).trim().toLowerCase();
                String value = thisLine.substring(thisLine.indexOf(":") + 1).trim();
                if (value.indexOf("=") == 0) {
                    value = value.substring(1).trim();
                }
                headerInfo.put(key, value);
            }
        }
        headerInfo.put("comments", commentString);
        return headerInfo;
    }

    /*public static ArrayList<Object> openExperiment(File file){
        ArrayList entries = new ArrayList();
        Object obj;
        try {
            XMLDecoder xmlDecoder;
            xmlDecoder = new XMLDecoder(new FileInputStream(file));
            obj = xmlDecoder.readObject();
            Integer length = (Integer) obj;
            for (int i = 0; i < length; i++){
                obj = xmlDecoder.readObject();
                entries.add(obj);
            }
            xmlDecoder.close();
        } catch (Exception e) {
            return null;
        } finally {
            return entries;
        }
    }
    public static boolean saveExperiment(String baseFileName, String onlyFileName, UI ui) {
        String dataFileName = ui.getImageFilePrefix() + NRRD_EXTENSION;
        File sessionFile = null;
        Opener image = ui.getOpener();
        MimsPlus ratio[] = ui.getOpenRatioImages();
        MimsPlus hsi[] = ui.getOpenHSIImages();
        MimsPlus sum[] = ui.getOpenSumImages();
        String[] names = image.getMassNames();
        Integer length = ratio.length + hsi.length + sum.length;
        if (ratio.length + hsi.length + sum.length > 0) {
                        try {
                // Contruct a unique name for each ratio image and save into a ratios.zip file
                BufferedOutputStream zos = new BufferedOutputStream(new FileOutputStream(baseFileName + SESSIONS_EXTENSION));
                XMLEncoder out = new XMLEncoder(zos);
                //write how many objects we are writing to the file
                out.writeObject(length);
                
            //need to modify persistance delegate to deal with constructor in SumProps which takes parameters
                if (ratio.length > 0) {
                    out.setPersistenceDelegate(RatioProps.class, new DefaultPersistenceDelegate(new String[]{"numMassIdx", "denMassIdx"}));
                    for (int i = 0; i < ratio.length; i++) {
                        RatioProps ratioprops = ratio[i].getRatioProps();
                        ratioprops.setDataFileName(dataFileName);
                        out.writeObject(ratioprops);
                    }
                }
                // Contruct a unique name for each hsi image and save.
                if (hsi.length > 0) {
                    out.setPersistenceDelegate(RatioProps.class, new DefaultPersistenceDelegate(new String[]{"numMassIdx", "denMassIdx"}));
                    for (int i = 0; i < hsi.length; i++) {
                        HSIProps hsiprops = hsi[i].getHSIProps();
                        hsiprops.setDataFileName(dataFileName);
                        out.writeObject(hsiprops);
                    }
                }
                // Contruct a unique name for each sum image and save.
                if (sum.length > 0) {
                    for (int i = 0; i < sum.length; i++) {
                        SumProps sumProps = sum[i].getSumProps();
                        sumProps.setDataFileName(dataFileName);
                        if (sumProps.getSumType() == 1) {
                            out.setPersistenceDelegate(RatioProps.class, new DefaultPersistenceDelegate(new String[]{"numMassIdx", "denMassIdx"}));
                            out.writeObject(sumProps);
                        } else if (sumProps.getSumType() == 0) {
                            out.setPersistenceDelegate(RatioProps.class, new DefaultPersistenceDelegate(new String[]{"parentMassIdx"}));
                            out.writeObject(sumProps);
                        } else {
                            continue;
                        }
                    }
                }
                out.close();
            } catch (Exception e) {
                System.out.println("Error saving experiment settings");
                return false;
            }
        }
        return true;
    }*/
    private static void addFilesRecursively(File dir, String name, Collection<File> all) {
        if (all.size() > 0) {
            return;
        }
        final File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.getName().matches(name)) {
                    all.add(child);
                    return;
                }
                addFilesRecursively(child, name, all);
            }
        }
    }

    /**
     * Get the file from which the current slice of a generated stack originates from.
     *
     * @param massimage the image whose current slice is desired file we wish to find
     * @param ui the <code>UI</code> instance
     * @return the file which the slice originates from.
     */
    public static File getSliceFile(MimsPlus massimage, UI ui) {
        File file = null;
        int sliceIndex = massimage.getCurrentSlice();
        String sliceName = ui.getOpener().getStackPositions()[sliceIndex - 1];
        final Collection<File> all = new ArrayList<File>();
        addFilesRecursively(new File(ui.getImageDir()), sliceName, all);
        if (all.isEmpty()) {
            return null;
        } else {
            return ((File) all.toArray()[0]);
        }
    }

    /**
     * Get the mosaic file in which the argument file is contained within. Note: searches the direct parent directories
     * and all .nrrd files directly within those directories. It will not recursively search the sibling/uncle
     * directories.
     *
     * @param file the file
     * @return the mosaic file if found, or null if not found.
     */
    public static File getStack(File file) {
        File parent = file.getParentFile();
        while (parent != null) {
            File[] siblings = parent.listFiles();
            for (File sibling : siblings) {
                if (sibling.getPath().endsWith(NRRD_EXTENSION)) {
                    try {
                        HashMap<String, String> headerInfo = FileUtilities.getHeaderInfo(sibling);
                        if (headerInfo.containsKey("mims_stack_positions") && headerInfo.get("mims_stack_positions").contains(file.getName())) {
                            return sibling;
                        }
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
            parent = parent.getParentFile();
        }
        return null;
    }

    /**
     * Take an XYDataset and convert it to a two dimensional array.
     *
     * @param data
     * @return two dimensional Object array
     */
    public static Object[][] convertXYDatasetToArray(XYDataset data) {
        int numSeries = data.getSeriesCount();
        int maxCount = 0;
        for (int i = 0; i < numSeries; i++) {
            if (data.getItemCount(i) > maxCount) {
                maxCount = data.getItemCount(i);
            }
        }
        Object[][] exportedData = new Object[maxCount][numSeries + 1];
        for (int i = 0; i < maxCount; i++) {
            Object[] row = new Object[numSeries + 1];
            row[0] = i + 1;
            for (int j = 1; j < numSeries + 1; j++) {
                if (i >= data.getItemCount(j - 1)) {
                    row[j] = 0;
                } else {
                    row[j] = data.getYValue(j - 1, i);
                }
            }
            exportedData[i] = row;
        }
        return exportedData;
    }

    //Change missing from svn->git migration
    public static String[] validStackPositions(MimsPlus image) {
        ArrayList<String> output = new ArrayList();
        ImageStack imageStack = image.getImageStack();
        for (int i = 0; i < imageStack.getSize(); i++) {
            output.add(imageStack.getSliceLabel(i + 1));
        }
        return output.toArray(new String[output.size()]);
    }

    /**
     * UNUSED, This may be the wrong place to put this....
     */
    /**
     * Return a string of "stack positions" that includes only the names of files that are currently in the stack, ie
     * deleted planes are not included. String is formatted to be placed directly in a nrrd header as the value of key
     * 'mims_stack_positions'.
     *
     * @param ui the <code>UI</code> instance
     * @param op an <code>Opener</code> reference
     * @return string of stack positions with those of deleted planes removed.
     */
    public static String trimStackPositions(UI ui, Opener op) {
        //ArrayList filenames = new ArrayList(String);
        String[] filenames = op.getStackPositions();
        MimsAction action = ui.getMimsAction();
        // int size = action.get().size();
        String raw = "";
        String trimmed = "";
        try {
            for (int i = 0; i < filenames.length; i++) {
                raw += filenames[i];
                if (!ui.getMimsAction().isDropped(i)) {
                    trimmed += filenames[i] + ";";
                }
            }
        } catch (Exception e) {
            //Catch all exceptions and return a non-empty error string.
            //This string will be written to the header.
            System.out.println(e.toString());
            return "Error-exception_trimming_stack_positions;";
        }

        System.out.println("raw: " + raw);
        System.out.println("trimmed: " + raw);

        return trimmed;
    }
    //end
}

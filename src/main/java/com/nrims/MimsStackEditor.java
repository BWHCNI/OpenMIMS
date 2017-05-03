package com.nrims;

import com.nrims.data.Opener;
import com.nrims.data.ImageDataUtilities;
import com.nrims.logging.OMLogger;

import ij.*;
import ij.io.FileInfo;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.StackProcessor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
//import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JLabel;
import org.apache.commons.io.FileUtils;

/**
 * The MimsStackEditor class creates the "Stack Editing" tab on the main UI window. It contains all the functionality
 * for manipulating images, including, but not limited to, applying translations, deleting planes, inserting planes, and
 * concatenating images. The AutoTrack algorithm can also be launched from this interface, as well as the the
 * functionality for generating "Sum" images.
 *
 * @author zkaufman
 */
public class MimsStackEditor extends javax.swing.JPanel {
    private final static Logger OMLOGGER = OMLogger.getOMLogger(NRIMS_Plugin.class.getName());
    public static final long serialVersionUID = 1;
    public static final int OK = 2;
    public static final int CANCEL = 3;
    public static final int WORKING = 4;
    public static final int DONE = 5;
    public int STATE = -1;
    public int THREAD_STATE = -1;
    private UI ui = null;
    private Opener image = null;
    private int numberMasses = -1;
    private MimsPlus[] images = null;
    private boolean holdupdate = false;
    public AutoTrackManager atManager;
    public ArrayList<Integer> includeList = new ArrayList<Integer>();
    public int startSlice = -1;
    private MimsJTable table;

    /**
     * The MimsStackEditor constructor.
     *
     * @param ui a pointer to the main UI class.
     * @param im a pointer to the Opener object.
     */
    public MimsStackEditor(UI ui, Opener im) {
        this.ui = ui;
        this.image = im;

        initComponents();
        initComponentsCustom();

        images = ui.getMassImages();
        numberMasses = image.getNMasses();

    }

    /**
     * Some basic setup of interface componenets.
     */
    private void initComponentsCustom() {

        //Note from Farah: These are double, but no translations occur except with integer number
        translateXSpinner.setModel(new javax.swing.SpinnerNumberModel(0.0d, -999.0d, 999.0d, 1.0d));
        translateYSpinner.setModel(new javax.swing.SpinnerNumberModel(0.0d, -999.0d, 999.0d, 1.0d));

        // Remove components (jspinners) from the area
        // in which a user can drag and drop a file.
        Component[] comps = {deleteListTextField, reinsertListTextField, translateXSpinner,
            translateYSpinner, compressTextField, sumTextField};
        for (Component comp : comps) {
            ui.removeComponentFromMimsDrop(comp);
        }
    }

    /**
     * Removes the planes contained within remList.
     *
     * @param remList a list of indices.
     * @return a string containing the list of removed planes (for debugging purposes).
     */
    public String removeSliceList(ArrayList<Integer> remList) {
        String liststr = "";
        int length = remList.size();
        int count = 0;
        for (int i = 0; i < length; i++) {
            removeSlice(remList.get(i) - count);
            count++;
            liststr += remList.get(i);
            if (i < (length - 1)) {
                liststr += ", ";
            }
        }
        return liststr;
    }

    /**
     * Removes the plane with the specified index.
     *
     * @param plane the plane to remove.
     */
    public void removeSlice(int plane) {
        int currentSlice = images[0].getCurrentSlice();
        for (int k = 0; k <= (numberMasses - 1); k++) {
            ImageStack is = new ImageStack();
            is = images[k].getStack();
            is.deleteSlice(plane);
            images[k].setStack(null, is);
            images[k].updateAndRepaintWindow();
        }
        this.ui.mimsAction.dropPlane(plane);
        images[0].setSlice(currentSlice);
    }

    /**
     * Shifts the current plane of ALL images by the amount <code>xval</code> in the x-direction.
     *
     * @param xval the amount to shift the plane.
     */
    private void XShiftSlice(double xval) {
        for (int k = 0; k <= (numberMasses - 1); k++) {
            this.images[k].killRoi();
            this.images[k].getProcessor().translate(xval, 0.0);
            images[k].updateAndDraw();
            ui.mimsAction.setIsTracked(true);
        }
    }

    /**
     * Shifts the current plane of ALL images by the amount <code>yval</code> in the y-direction.
     *
     * @param yval the amount to shift the plane.
     */
    private void YShiftSlice(double yval) {
        for (int k = 0; k <= (numberMasses - 1); k++) {
            this.images[k].killRoi();
            this.images[k].getProcessor().translate(0.0, yval);
            images[k].updateAndDraw();
            ui.mimsAction.setIsTracked(true);
        }
    }

    /**
     * Sets the mean translation of a block to zero.
     *
     * @param plane block number.
     */
    private void setBlockTranslationToZero(int plane) {

        int[] planes = ui.mimsAction.getPlaneNumbersFromBlockNumber(plane);
        int openerIndex;
        String openerName;
        Opener op;

        Object[][] pixels = new Object[numberMasses][planes.length];

        double xMean = ui.mimsAction.getXShift(plane);
        double yMean = ui.mimsAction.getYShift(plane);
        // Apply the translations.
        for (int k = 0; k <= (numberMasses - 1); k++) {
            for (int i = 0; i < planes.length; i++) {
                openerIndex = ui.mimsAction.getOpenerIndex(planes[i] - 1);
                openerName = ui.mimsAction.getOpenerName(planes[i] - 1);
                op = ui.getFromOpenerList(openerName);
                op.setStackIndex(openerIndex);
                try {
                    pixels[k][i] = op.getPixels(k);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                ImageProcessor ip = null;
                if (op.getFileType() == FileInfo.GRAY16_UNSIGNED) {
                    ip = new ShortProcessor(images[0].getWidth(), images[0].getHeight());//, (short[])pixels[k][i], null);
                } else if (op.getFileType() == FileInfo.GRAY32_FLOAT || op.getFileType() == FileInfo.GRAY32_UNSIGNED) {
                    ip = new FloatProcessor(images[0].getWidth(), images[0].getHeight());//, (float[])pixels[k][i], null);
                }
                ip.setPixels(pixels[k][i]);
                double xCurrent = ui.mimsAction.xyTranslationList.get(planes[i] - 1)[0];
                double yCurrent = ui.mimsAction.xyTranslationList.get(planes[i] - 1)[1];

                double xTrans = xCurrent - xMean;
                double yTrans = yCurrent - yMean;
                ip.translate(xTrans, yTrans);
                pixels[k][i] = ip.getPixels();
            }
        }

        int pixelLength = ((Object[]) pixels[0][0]).length;
        Object sumPixels[][] = new Object[numberMasses][pixelLength];

        // Sum the pixels.
        for (int k = 0; k <= (numberMasses - 1); k++) {
            for (int i = 0; i < planes.length; i++) {
                for (int j = 0; j < pixelLength; j++) {
                    if (images[k].getProcessor() instanceof ShortProcessor) {
                        short tempVal = ((Short) sumPixels[k][j]).shortValue();
                        short[] temppixels = (short[]) pixels[k][i];
                        sumPixels[k][j] = tempVal + temppixels[j];
                    } else if (images[k].getProcessor() instanceof FloatProcessor) {
                        float tempVal = ((Float) sumPixels[k][j]).floatValue();
                        float[] temppixels = (float[]) pixels[k][i];
                        sumPixels[k][j] = tempVal + temppixels[j];
                    } else {
                        sumPixels[k][j] = 0;
                    }
                }
            }
            images[k].setSlice(plane);
            images[k].getProcessor().setPixels(sumPixels[k]);
        }
    }

    /**
     * Restores a block to the minimum translation.
     *
     * @param plane block number.
     * @return the amount shifted
     */
    //public double[] restoreBlock(int plane) {
    public double[] restoreBlock(int plane) {

        double xval = (Double) translateXSpinner.getValue();
        double yval = (Double) translateYSpinner.getValue();
        double actx = ui.mimsAction.getXShift(plane);
        double acty = ui.mimsAction.getYShift(plane);
        double deltaX = xval - actx;
        double deltaY = yval - acty;

        int[] planes = ui.mimsAction.getPlaneNumbersFromBlockNumber(plane);
        int openerIndex;
        String openerName;
        Opener op;

        Object[][] pixels = new Object[numberMasses][planes.length];

        // Get the smallest translation?  among planes?  masses?
        double minXval = 0.0;
        double minYval = 0.0;
        double xMean = ui.mimsAction.getXShift(plane);
        double yMean = ui.mimsAction.getYShift(plane);

        for (int i = 0; i < planes.length; i++) {
            double xCurrent = ui.mimsAction.xyTranslationList.get(planes[i] - 1)[0];
            double yCurrent = ui.mimsAction.xyTranslationList.get(planes[i] - 1)[1];
            double xTrans = xCurrent - xMean;
            double yTrans = yCurrent - yMean;
            if (xTrans < minXval) {
                minXval = xTrans;
            }
            if (yTrans < minYval) {
                minYval = yTrans;
            }
        }

        // Apply the translations.
        for (int k = 0; k <= (numberMasses - 1); k++) {
            for (int i = 0; i < planes.length; i++) {
                openerIndex = ui.mimsAction.getOpenerIndex(planes[i] - 1);
                openerName = ui.mimsAction.getOpenerName(planes[i] - 1);
                op = ui.getFromOpenerList(openerName);
                op.setStackIndex(openerIndex);
                try {
                    pixels[k][i] = op.getPixels(k);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                ImageProcessor ip = null;
                if (op.getFileType() == FileInfo.GRAY16_UNSIGNED) {
                    ip = new ShortProcessor(images[0].getWidth(), images[0].getHeight());
                } else if (op.getFileType() == FileInfo.GRAY32_FLOAT || op.getFileType() == FileInfo.GRAY32_UNSIGNED) {
                    ip = new FloatProcessor(images[0].getWidth(), images[0].getHeight());
                }
                ip.setPixels(pixels[k][i]);   // Set pixels read from disk in processor

                double xCurrent = ui.mimsAction.xyTranslationList.get(planes[i] - 1)[0];
                double yCurrent = ui.mimsAction.xyTranslationList.get(planes[i] - 1)[1];
                double xTrans = xCurrent + deltaX;
                double yTrans = yCurrent + deltaY;

                ip.translate(xTrans, yTrans);
                pixels[k][i] = ip.getPixels();
            }
        }

        int pixelLength = 0;
        if (pixels[0][0] instanceof short[]) {
            pixelLength = ((short[]) pixels[0][0]).length;
        } else if (pixels[0][0] instanceof float[]) {
            pixelLength = ((float[]) pixels[0][0]).length;
        }

        Object[][] sumPixels = new Object[numberMasses][pixelLength];
        // Sum the pixels.
        for (int k = 0; k <= (numberMasses - 1); k++) {
            for (int i = 0; i < planes.length; i++) {
                for (int j = 0; j < pixelLength; j++) {
                    if (sumPixels[k][j] == null && images[k].getProcessor() instanceof ShortProcessor) {
                        short zero = 0;
                        sumPixels[k][j] = zero;
                    }
                    if (sumPixels[k][j] == null && images[k].getProcessor() instanceof FloatProcessor) {
                        float zero = 0;
                        sumPixels[k][j] = zero;
                    }
                    if (images[k].getProcessor() instanceof ShortProcessor) {
                        short tempVal = ((Short) sumPixels[k][j]).shortValue();
                        short[] temppixels = (short[]) pixels[k][i];
                        sumPixels[k][j] = (short) (tempVal + temppixels[j]);
                    } else if (images[k].getProcessor() instanceof FloatProcessor) {
                        float tempVal = ((Float) sumPixels[k][j]).floatValue();
                        float[] temppixels = (float[]) pixels[k][i];
                        sumPixels[k][j] = (float) tempVal + temppixels[j];
                    } else {
                        sumPixels[k][j] = 0;
                    }
                }
            }

            images[k].setSlice(plane);

            if (images[k].getProcessor() instanceof ShortProcessor) {
                short[] shortPixels = new short[pixelLength];
                for (int j = 0; j < pixelLength; j++) {
                    shortPixels[j] = ((Short) sumPixels[k][j]).shortValue();
                }
                int restoreIndex = ui.mimsAction.trueIndex(plane);
                images[k].getStack().setPixels(shortPixels, restoreIndex);
            } else if (images[k].getProcessor() instanceof FloatProcessor) {
                float[] floatPixels = new float[pixelLength];
                for (int j = 0; j < pixelLength; j++) {
                    floatPixels[j] = ((Float) sumPixels[k][j]).floatValue();
                }
                //images[k].getProcessor().setPixels(floatPixels);
                int restoreIndex = ui.mimsAction.trueIndex(plane);
                images[k].getStack().setPixels(floatPixels, restoreIndex);
            }
        }

        double[] returnVal = {minXval, minYval};
        return returnVal;
    }  // end restoreBlock

    /**
     * Restores a slice (or block) from the original data so that a new translation can be applied.
     *
     * @param plane the plane (aka slice) to be restored
     * 
     * @return [0.0 0.0] if restoring a slice, [x y] if restoring a block.
     */
    public double[] restoreSlice(int plane) {

        if (ui.mimsAction.getIsCompressed()) {
            double[] returnVal = restoreBlock(plane);
            return returnVal;
        }

        int restoreIndex = ui.mimsAction.trueIndex(plane);
        try {

            int openerIndex = ui.mimsAction.getOpenerIndex(restoreIndex - 1);
            String openerName = ui.mimsAction.getOpenerName(restoreIndex - 1);
            Opener op = ui.getFromOpenerList(openerName);

            op.setStackIndex(openerIndex);
            for (int k = 0; k <= (numberMasses - 1); k++) {
                images[k].setSlice(plane);
                images[k].getStack().setPixels(op.getPixels(k), restoreIndex);
                images[k].updateAndDraw();
            }
        } catch (Exception e) {
            System.err.println("Error re-reading plane " + restoreIndex);
            System.err.println(e.toString());
            e.printStackTrace();
        }

        double[] returnVal = {0.0, 0.0};
        return returnVal;
    }


    /**
     * Inserts a previously deleted plane back into the stack.
     *
     * @param plane the true plane.
     */
    public void insertSlice(int plane) {

        if (!ui.mimsAction.isDropped(plane)) {
            System.out.println("already there...");
            return;
        }
        // Get some properties about the image we are trying to restore.
        int restoreIndex = ui.mimsAction.displayIndex(plane);
        int displaysize = images[0].getNSlices();
        System.out.println("added image: " + restoreIndex);
        try {
            if (restoreIndex < displaysize) {
                int openerIndex = ui.mimsAction.getOpenerIndex(plane - 1);
                String openerName = ui.mimsAction.getOpenerName(plane - 1);
                Opener op = ui.getFromOpenerList(openerName);
                op.setStackIndex(openerIndex);
                for (int i = 0; i < op.getNMasses(); i++) {
                    images[i].setSlice(restoreIndex);
                    images[i].getStack().addSlice("", images[i].getProcessor(), restoreIndex);
                    images[i].getProcessor().setPixels(op.getPixels(i));
                    images[i].updateAndDraw();
                }
            }
            if (restoreIndex >= displaysize) {
                this.holdupdate = true;
                int openerIndex = ui.mimsAction.getOpenerIndex(plane - 1);
                String openerName = ui.mimsAction.getOpenerName(plane - 1);
                Opener op = ui.getFromOpenerList(openerName);
                op.setStackIndex(openerIndex);
                for (int i = 0; i < op.getNMasses(); i++) {
                    images[i].setSlice(displaysize);
                    ImageStack is = new ImageStack();
                    is = images[i].getImageStack();
                    is.addSlice("", op.getPixels(i));
                    images[i].setStack(null, is);
                    images[i].updateAndRepaintWindow();
                }
            }
            images[0].setSlice(restoreIndex);
            ui.mimsAction.undropPlane(plane);
            XShiftSlice(ui.mimsAction.getXShift(restoreIndex));
            YShiftSlice(ui.mimsAction.getYShift(restoreIndex));

        } catch (Exception e) {
            System.err.println("Error re-reading plane " + restoreIndex + "from file.");
            System.err.println(e.toString());
            e.printStackTrace();
        }
        this.holdupdate = false;
        this.resetTrueIndexLabel();
    }

    /**
     * Set the user selected options on to the tempImage. The tempImage is then returned (modified by the options) and
     * ready to be tracked.
     *
     * @param tempImage an Image.
     * @param options a set of options specifying modifications to the tempImage.
     * @return a modified tempImage.
     */
    private ImagePlus setOptions(ImagePlus tempImage, String options) {

        // Crop image to size of roi.
        if (options.contains("roi")) {
            tempImage = cropImage(tempImage);
        } else {
            ImageStack tempStack = tempImage.getImageStack();
            ImageProcessor tempProcessor = tempImage.getProcessor();
            StackProcessor stackproc = new StackProcessor(tempStack, tempProcessor);
            tempImage.setStack("cropped", stackproc.crop(0, 0, tempImage.getProcessor().getWidth(), tempImage.getProcessor().getHeight()));
        }

        // Enhance tracking image contrast.
        if (options.contains("normalize") || options.contains("equalize")) {
            WindowManager.setTempCurrentImage(tempImage);
            String tmpstring = "";
            if (options.contains("normalize")) {
                tmpstring += "normalize ";
            }
            if (options.contains("equalize")) {
                tmpstring += "equalize ";
            }
            IJ.run("Enhance Contrast", "saturated=0.5 " + tmpstring + " normalize_all");
        }
        if (options.contains("medianize")) {
            WindowManager.setTempCurrentImage(tempImage);
            int originalSliceNum = tempImage.getSlice();
            for (int i=1; i<=tempImage.getNSlices(); i++) {
                tempImage.setSlice(i);
                IJ.run("Median...", "radius=2");
            }
            tempImage.setSlice(originalSliceNum);
        }

        return tempImage;
    }

    /**
     * Get the user selected options for autotracking.
     *
     * @return the options as a string.
     */
    private String getOptions() {

        String options = " ";
        if (atManager.sub.isSelected()) {
            options += "roi ";
        }

        //check for roi
        if (options.contains("roi")) {
            MimsRoiManager2 roimanager = ui.getRoiManager();
            if (roimanager == null) {
                JOptionPane.showMessageDialog(atManager,
                        "No ROI selected.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                //ij.IJ.error("Error", "No ROI selected.");
                return null;
            }
            ij.gui.Roi roi = roimanager.getRoi();
            if (roi == null) {
                JOptionPane.showMessageDialog(atManager,
                        "No ROI selected.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                //ij.IJ.error("Error", "No ROI selected.");
                return null;
            }
        }

        if (atManager.norm.isSelected()) {
            options += "normalize ";
        }
        if (atManager.eq.isSelected()) {
            options += "equalize ";
        }
        if (atManager.med.isSelected()) {
            options += "medianize ";
        }
        if (atManager.show.isSelected()) {
            options += "show ";
        }

        return options;
    }

    /**
     * shifts all slices within a range the same x and y
     *
     * @param xShift   x amount to shift
     * @param yShift  y amount to shift
     * @param startSlice  starting slice for the shift
     * @param endSlice  ending slice for the shift
     */
    public void translateStack(int xShift, int yShift, int startSlice, int endSlice) {

        MimsAction action = ui.mimsAction;

        for (int i = startSlice; i <= endSlice; i++) {
            double actx = action.getXShift(i);
            double acty = action.getYShift(i);
            boolean redraw = ((xShift * actx < 0) || (yShift * acty < 0));
            double deltaX = xShift;
            double deltaY = yShift;

            if (!holdupdate && (!ui.isUpdating())) {
                if (redraw) {
                    double[] xy = restore(i);
                    deltaX = actx + xShift + xy[0];
                    deltaY = acty + yShift + xy[1];

                    if (xy[0] != 0.0 && xy[1] != 0.0) {
                        //TODO: Test this case. This if block is just a reminder for Farah.
                        System.out.println("We need to test to make sure this works properly.");
                    }
                }

                for (int k = 0; k < numberMasses; k++) {
                    images[k].getStack().getProcessor(i).translate(deltaX, deltaY);
                }
            }

            action.setShiftX(i, actx + xShift);
            action.setShiftY(i, acty + yShift);
            action.setIsTracked(true);
        }
    }

    public void translatePlane(double deltax, double deltay) {
        int plane = images[0].getCurrentSlice();
        double actx = ui.mimsAction.getXShift(plane);
        double acty = ui.mimsAction.getYShift(plane);

        boolean redraw = ((deltax * actx < 0) || (deltay * acty < 0));

        if (!holdupdate && !ui.isUpdating()) {
            if (redraw) {
                double[] xy = restore(plane);
                this.XShiftSlice(actx + deltax + xy[0]);
                this.YShiftSlice(acty + deltay + xy[1]);
            } else {
                this.XShiftSlice(deltax);
                this.YShiftSlice(deltay);
            }
            ui.mimsAction.setShiftX(plane, actx + deltax);
            ui.mimsAction.setShiftY(plane, acty + deltay);
        }
    }

    /**
     * This method is used by translateStack -- restores image without going through each slice visually. Repeat of
     * restoreSlice and restoreBlock
     *
     * @param plane number
     */
    private double[] restore(int plane) {

        if (ui.mimsAction.getIsCompressed()) {
            //This is from restoreBlock

            System.out.println("SHOULD NOT COME HERE");

            int[] planes = ui.mimsAction.getPlaneNumbersFromBlockNumber(plane);
            int openerIndex;
            Opener op;

            Object[][] pixels = new Object[numberMasses][planes.length];

            // Get the smallest translation
            double minXval = 0.0;
            double minYval = 0.0;
            double xMean = ui.mimsAction.getXShift(plane);
            double yMean = ui.mimsAction.getYShift(plane);
            for (int k = 0; k <= (numberMasses - 1); k++) {
                for (int i = 0; i < planes.length; i++) {
                    double xCurrent = ui.mimsAction.xyTranslationList.get(planes[i] - 1)[0];
                    double yCurrent = ui.mimsAction.xyTranslationList.get(planes[i] - 1)[1];
                    double xTrans = xCurrent - xMean;
                    double yTrans = yCurrent - yMean;
                    if (xTrans < minXval) {
                        minXval = xTrans;
                    }
                    if (yTrans < minYval) {
                        minYval = yTrans;
                    }
                }
            }

            // Apply the translations.
            for (int k = 0; k <= (numberMasses - 1); k++) {
                for (int i = 0; i < planes.length; i++) {
                    openerIndex = ui.mimsAction.getOpenerIndex(planes[i] - 1);
                    op = ui.getFromOpenerList(ui.mimsAction.getOpenerName(planes[i] - 1));
                    op.setStackIndex(openerIndex);

                    try {
                        pixels[k][i] = op.getPixels(k);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }

                    ImageProcessor ip = null;
                    if (op.getFileType() == FileInfo.GRAY16_UNSIGNED) {
                        ip = new ShortProcessor(images[0].getWidth(), images[0].getHeight());
                    } else if (op.getFileType() == FileInfo.GRAY32_FLOAT || op.getFileType() == FileInfo.GRAY32_UNSIGNED) {
                        ip = new FloatProcessor(images[0].getWidth(), images[0].getHeight());
                    }
                    ip.setPixels(pixels[k][i]);
                    double xCurrent = ui.mimsAction.xyTranslationList.get(planes[i] - 1)[0];
                    double yCurrent = ui.mimsAction.xyTranslationList.get(planes[i] - 1)[1];

                    double xTrans = xCurrent - xMean - minXval;
                    double yTrans = yCurrent - yMean - minYval;
                    ip.translate(xTrans, yTrans);
                    pixels[k][i] = ip.getPixels();
                }
            }

            int pixelLength = 0;
            if (pixels[0][0] instanceof short[]) {
                pixelLength = ((short[]) pixels[0][0]).length;
            } else if (pixels[0][0] instanceof float[]) {
                pixelLength = ((float[]) pixels[0][0]).length;
            }

            Object sumPixels[][] = new Object[numberMasses][pixelLength];

            // Sum the pixels.
            for (int k = 0; k <= (numberMasses - 1); k++) {
                for (int i = 0; i < planes.length; i++) {
                    for (int j = 0; j < pixelLength; j++) {
                        if (sumPixels[k][j] == null && images[k].getProcessor() instanceof ShortProcessor) {
                            short zero = 0;
                            sumPixels[k][j] = zero;
                        }
                        if (sumPixels[k][j] == null && images[k].getProcessor() instanceof FloatProcessor) {
                            float zero = 0;
                            sumPixels[k][j] = zero;
                        }
                        if (images[k].getProcessor() instanceof ShortProcessor) {
                            short tempVal = ((Short) sumPixels[k][j]).shortValue();
                            short[] temppixels = (short[]) pixels[k][i];
                            sumPixels[k][j] = (short) (tempVal + temppixels[j]);
                        } else if (images[k].getProcessor() instanceof FloatProcessor) {
                            float tempVal = ((Float) sumPixels[k][j]).floatValue();
                            float[] temppixels = (float[]) pixels[k][i];
                            sumPixels[k][j] = (float) (tempVal + temppixels[j]);
                        } else {
                            sumPixels[k][j] = 0;
                        }
                    }

                    if (images[k].getProcessor() instanceof ShortProcessor) {
                        short[] shortPixels = new short[pixelLength];
                        for (int j = 0; j < pixelLength; j++) {
                            shortPixels[j] = ((Short) sumPixels[k][j]).shortValue();
                        }
                        images[k].getStack().setPixels(shortPixels[k], plane);
                    } else if (images[k].getProcessor() instanceof FloatProcessor) {
                        float[] floatPixels = new float[pixelLength];
                        for (int j = 0; j < pixelLength; j++) {
                            floatPixels[j] = ((Float) sumPixels[k][j]).floatValue();
                        }
                        images[k].getStack().setPixels(floatPixels[k], plane);
                    }
                }
            }
            double[] returnVal = {minXval, minYval};
            return returnVal;
        } else {
            //this is the edited version from restoreSlice
            int restoreIndex = ui.mimsAction.trueIndex(plane);
            try {

                int openerIndex = ui.mimsAction.getOpenerIndex(restoreIndex - 1);
                Opener op = ui.getFromOpenerList(ui.mimsAction.getOpenerName(restoreIndex - 1));

                op.setStackIndex(openerIndex);

                for (int k = 0; k < numberMasses; k++) {

                    /*NOTE: images[k].getProcessor() and images[k].getStack().getProcessor(plane) where plane imageStack
                    * your current plane do not return the same object. When you're setting the pixels for the stack,
                    * call images[k].getStack().setPixels(object, plane) and NOT! images[k].getStack().getProcessor().setPixels(obj)
                    * 
                    * ImageJ --> :(
                     */
                    images[k].getStack().setPixels(op.getPixels(k), plane);
                }
            } catch (Exception e) {
                System.err.println("Error re-reading plane " + restoreIndex);
                System.err.println(e.toString());
                e.printStackTrace();
            }

            double[] returnVal = {0.0, 0.0};
            return returnVal;
        }
    }

    /**
     * Applies a set of translations to the images.
     *
     * @param translations the list of translations.
     * @param includeList the list of planes to which the translations apply.
     */
    public void applyTranslations(double[][] translations, ArrayList<Integer> includeList) {

        double deltax, deltay;
        int plane;

        for (int i = 0; i < translations.length; i++) {

            if (STATE == CANCEL) {
                return;
            }

            plane = includeList.get(i);
            //possible loss of precision...
            //notice the negative....
            deltax = (-1.0) * translations[i][0];
            deltay = (-1.0) * translations[i][1];
            images[0].setSlice(plane);
            double actx = ui.mimsAction.getXShift(plane);
            double acty = ui.mimsAction.getYShift(plane);

            boolean redraw = ((deltax * actx < 0) || (deltay * acty < 0));

            if (!holdupdate && (!ui.isUpdating())) {
                if (redraw) {
                    double[] xy = restoreSlice(plane);
                    XShiftSlice(actx + deltax + xy[0]);
                    YShiftSlice(acty + deltay + xy[1]);
                } else {
                    XShiftSlice(deltax);
                    YShiftSlice(deltay);
                }
                ui.mimsAction.setShiftX(plane, actx + deltax);
                ui.mimsAction.setShiftY(plane, acty + deltay);
            }
        }
    }

    /**
     * Concatenates another image to the current image.
     *
     * @param pre <code>true</code> if prepending, otherwise <code>false</code> (appending).
     * @param tempui a UI object, required for access to the Opener and the mass images.
     */
    public void concatImages(boolean pre, UI tempui) {

        ui.setUpdating(true);

        Opener tempImage = tempui.getOpener();
        MimsPlus[] tempimage = tempui.getMassImages();
        ImageStack[] tempstacks = new ImageStack[numberMasses];

        for (int i = 0; i < image.getNMasses(); i++) {
            if (images[i] != null) {
                images[i].setIsStack(true);
            }
        }

        //increase action size
        ui.mimsAction.addPlanes(pre, tempimage[0].getNSlices(), tempImage);

        // Get the stacks for the original data. These
        // are the stack we are going to (pre)append to.
        for (int i = 0; i <= (numberMasses - 1); i++) {
            tempstacks[i] = images[i].getStack();
        }

        // (Pre)append slices, include labels.
        if (pre) {
            if (tempImage.getBitsPerPixel() == 2) {
                for (int mass = 0; mass < numberMasses; mass++) {
                    ShortProcessor sp = new ShortProcessor(tempImage.getWidth(), tempImage.getHeight());

                    for (int i = tempImage.getNImages(); i >= 1; i--) {
                        tempImage.setStackIndex(i - 1);
                        try {
                            sp.setPixels(tempImage.getPixels(mass));
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                        tempstacks[mass].addSlice(tempimage[mass].getTitle(), sp, 0);
                    }
                    images[mass].setStack(null, tempstacks[mass]);
                }
            } else if (tempImage.getBitsPerPixel() == 4) {
                for (int mass = 0; mass < numberMasses; mass++) {
                    FloatProcessor sp = new FloatProcessor(tempImage.getWidth(), tempImage.getHeight());

                    for (int i = tempImage.getNImages(); i >= 1; i--) {
                        tempImage.setStackIndex(i - 1);
                        try {
                            sp.setPixels(tempImage.getPixels(mass));
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                        tempstacks[mass].addSlice(tempimage[mass].getTitle(), sp, 0);
                    }
                    images[mass].setStack(null, tempstacks[mass]);
                }
            }

        } else if (tempImage.getBitsPerPixel() == 2) {
            for (int mass = 0; mass < numberMasses; mass++) {
                ShortProcessor sp = new ShortProcessor(tempImage.getWidth(), tempImage.getHeight());
                for (int i = 1; i <= tempImage.getNImages(); i++) {
                    tempImage.setStackIndex(i - 1);
                    try {
                        sp.setPixels(tempImage.getPixels(mass));
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    tempstacks[mass].addSlice(tempimage[mass].getTitle(), sp);
                }
                images[mass].setStack(null, tempstacks[mass]);
            }
        } else if (tempImage.getBitsPerPixel() == 4) {
            System.out.println("Concating");
            for (int mass = 0; mass < numberMasses; mass++) {
                FloatProcessor sp = new FloatProcessor(tempImage.getWidth(), tempImage.getHeight());
                for (int i = 1; i <= tempImage.getNImages(); i++) {
                    tempImage.setStackIndex(i - 1);
                    try {
                        sp.setPixels(tempImage.getPixels(mass));
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    tempstacks[mass].addSlice(tempimage[mass].getTitle(), sp);
                }
                images[mass].setStack(null, tempstacks[mass]);
            }
        }

        // Update images.
        for (int i = 0; i <= (numberMasses - 1); i++) {
            this.images[i].updateImage();
        }

        // Add log entry.
        ui.getMimsLog().Log("New size: " + images[0].getNSlices() + " planes");

        // Clean up residual images.
        for (int i = 0; i < tempimage.length; i++) {
            if (tempimage[i] != null) {
                tempimage[i].setAllowClose(true);
                tempimage[i].close();
                tempimage[i] = null;
            }
        }

        ui.addToOpenerList(tempui.getOpener().getImageFile().getName(), tempui.getOpener());
        ui.setUpdating(false);
    }

    /**
     * Checks if the size of the images for two different Opener objects is the same. (Required for concatenating)
     *
     * @param im Opener object 1.
     * @param ij Opener object 2.
     * @return <code>true</code> if sizes are the same, otherwise <code>false</code>.
     */
    public boolean sameResolution(Opener im, Opener ij) {
        return ((im.getWidth() == ij.getWidth()) && (im.getHeight() == ij.getHeight()));
    }

    /**
     * Checks if the spot size of the images for two different Opener objects is the same. (Required for concatenating)
     * different
     *
     * @param im Opener object 1.
     * @param ij Opener object 2.
     * @return <code>true</code> if spot sizes are the same, otherwise <code>false</code>.
     */
    public boolean sameSpotSize(Opener im, Opener ij) {
        return ((im.getPixelWidth() == ij.getPixelWidth()) && (im.getPixelHeight() == ij.getPixelHeight()));
    }

    /**
     * A static method that takes a String in the form "2,4,8-25,45..." and converts it to a list of integers.
     *
     * @param liststr the string (for example "2,4,8-25,45...").
     * @param lb the lower bound of the list. (Usually 1 for image stacks)
     * @param ub the upper bound of the list.
     * @return an ArrayList of integers.
     */
    public static ArrayList<Integer> parseList(String liststr, int lb, int ub) {
        ArrayList<Integer> deletelist = new ArrayList<Integer>();
        ArrayList<Integer> checklist = new ArrayList<Integer>();
        boolean badlist = false;

        check:
        try {
            if (liststr.equals("")) {
                badlist = true;
                break check;
            }
            liststr = liststr.replaceAll("[^0-9,-]", "");
            String[] splitstr = liststr.split(",");
            int l = splitstr.length;

            for (int i = 0; i < l; i++) {

                if (!splitstr[i].contains("-")) {
                    deletelist.add(Integer.parseInt(splitstr[i]));
                } else {
                    String[] resplitstr = splitstr[i].split("-");

                    if (resplitstr.length > 2) {
                        ij.IJ.error("List Error", "Malformed range in list.");
                        break check;
                    }

                    int low = Integer.parseInt(resplitstr[0]);
                    int high = Integer.parseInt(resplitstr[1]);

                    if (low >= high) {
                        ij.IJ.error("List Error", "Malformed range bounds in list.");
                        break check;
                    }

                    for (int j = low; j <= high; j++) {
                        deletelist.add(j);
                    }
                }
            }
            java.util.Collections.sort(deletelist);

            int length = deletelist.size();

            for (int i = 0; i < length; i++) {
                if (deletelist.get(i) > ub || deletelist.get(i) < lb) {
                    badlist = true;
                    ij.IJ.error("List Error", "Out of range element in list.");
                    break check;
                }
            }

            for (int i = 0; i < length; i++) {
                int plane = deletelist.get(i);
                if (!checklist.contains(plane)) {
                    checklist.add(plane);
                }
            }

        } catch (Exception e) {
            ij.IJ.error("List Error", "Exception, malformed list.");
            badlist = true;
        }

        if (badlist) {
            ArrayList<Integer> bad = new ArrayList<Integer>(0);
            return bad;
        } else {
            return checklist;
        }
    }

    /**
     * Untracks the ImageStack. Sets all translations to zero.
     */
    public void untrack() {

        double xval = 0.0;
        double yval = 0.0;

        for (int plane = 1; plane <= images[0].getNSlices(); plane++) {
            if (ui.mimsAction.getIsCompressed()) {
                setBlockTranslationToZero(plane);
            } else {
                restoreSlice(plane);
            }
            this.XShiftSlice(xval);
            this.YShiftSlice(yval);
            ui.mimsAction.setShiftX(plane, xval);
            ui.mimsAction.setShiftY(plane, yval);
        }
        ui.mimsAction.setIsTracked(false);
        ui.getMimsLog().Log("Untracked.");
    }

    /**
     * Uncompresses the planes, if they were compressed into blocks.
     */
    public void uncompressPlanes() {

        if (!ui.getMimsAction().getIsCompressed()) {
            return;
        }

        ui.getMimsAction().setIsCompressed(false);

        MimsAction ma = ui.getMimsAction();
        int nPlanes = ma.getSizeMinusNumberDropped();

        for (int i = 0; i < numberMasses; i++) {
            ImageStack is = new ImageStack(image.getWidth(), image.getHeight());
            ImageProcessor ip = null;
            for (int plane = 1; plane <= nPlanes; plane++) {
                int tplane = ma.trueIndex(plane);
                String openerName = ui.mimsAction.getOpenerName(tplane - 1);
                Opener op = ui.getFromOpenerList(openerName);
                int imageIndex = ma.getOpenerIndex(tplane - 1);
                op.setStackIndex(imageIndex);
                try {
                    if (ui.getOpener().getFileType() == FileInfo.GRAY16_UNSIGNED) {
                        ip = new ShortProcessor(image.getWidth(), image.getHeight());
                    } else if (ui.getOpener().getFileType() == FileInfo.GRAY32_FLOAT || ui.getOpener().getFileType() == FileInfo.GRAY32_UNSIGNED) {
                        ip = new FloatProcessor(image.getWidth(), image.getHeight());
                    }
                    ip.setPixels(op.getPixels(i));
                    double x = ma.getXShift(plane);
                    double y = ma.getYShift(plane);
                    ip.translate(x, y);
                    is.addSlice(openerName, ip);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            images[i].setStack(null, is);
        }

        // Enable the deleting and reinserting of images.
        deleteListButton.setEnabled(true);
        deleteListTextField.setEnabled(true);
        reinsertButton.setEnabled(true);
        reinsertListTextField.setEnabled(true);

        this.resetTrueIndexLabel();
        this.resetSpinners();
    }

    /**
     * Compresses the planes to blocks. If the 16-bit limit for a ShortProcessor is exceeded, the ImageProcessors are
     * converted to FloatProcessors.
     *
     * @param blockSize number of planes to compress into a block.
     * @return <code>true</code> if successful, otherwise <code>false</code>.
     */
    public boolean compressPlanes(int blockSize) {

        // initializing stuff.
        int nmasses = image.getNMasses();
        boolean is16Bit = true;
        int width = images[0].getWidth();
        int height = images[0].getHeight();

        // Set up the stacks.
        int size = images[0].getNSlices();
        ImageStack[] imageStack = new ImageStack[nmasses];   
        for (int mindex = 0; mindex < nmasses; mindex++) {
            ImageStack iss = new ImageStack(width, height);
            imageStack[mindex] = iss;
        }

        // Determine if we are going to exceed the 16bit limit.
        int idx = 0;
        int size_i = (size + blockSize - 1) / blockSize;  // This is number of blocks, not slices/block
        MimsPlus[][] mp = new MimsPlus[nmasses][size_i];
        for (int i = 1; i <= size; i = i + blockSize) {

            // Create a sum list of individual images to be in the block.
            ArrayList sumlist = new ArrayList<Integer>();
            for (int j = i; j <= i + blockSize - 1; j++) {
                if (j > 0 && j <= images[0].getNSlices()) {
                    sumlist.add(j);
                }
            }

            // Generate the sum image for the block.
            for (int mindex = 0; mindex < nmasses; mindex++) {
                SumProps sumProps = new SumProps(images[mindex].getMassIndex());
                mp[mindex][idx] = new MimsPlus(ui, sumProps, sumlist);
                mp[mindex][idx].setTitle(sumlist.get(0) + " - " + sumlist.get(sumlist.size() - 1));

                // Check for bit size.
                double m = mp[mindex][idx].getProcessor().getMax();
                if (m > Short.MAX_VALUE - 1) {
                    is16Bit = false;
                }
            }
            idx++;
        }
        int info = mp[0].length;  // number of compressed frames
        for (int i = 0; i < mp[0].length; i++) {
            for (int mindex = 0; mindex < nmasses; mindex++) {

                // Build up the stacks.
                ImageProcessor ip = null;
                if (is16Bit) {
                    float[] floatArray = (float[]) mp[mindex][i].getProcessor().getPixels();
                    int len = floatArray.length;
                    short[] shortArray = new short[len];
                    for (int j = 0; j < len; j++) {
                        shortArray[j] = ((Float) floatArray[j]).shortValue();
                    }
                    ip = new ShortProcessor(width, height);
                    ip.setPixels(shortArray);
                } else {
                    ip = new FloatProcessor(width, height);
                    ip.setPixels(mp[mindex][i].getProcessor().getPixels());
                }
                imageStack[mindex].addSlice(mp[mindex][i].getTitle(), ip);
            }
        }

        //a little cleanup
        //multiple calls to setSlice are to get image scroll bars triggered right
        for (int mindex = 0; mindex < nmasses; mindex++) {
            images[mindex].setStack(null, imageStack[mindex]);
            if (images[mindex].getNSlices() > 1) {
                images[mindex].setIsStack(true);
                images[mindex].setSlice(1);
                images[mindex].setSlice(images[mindex].getNSlices());
                images[mindex].setSlice(1);
            } else {
                images[mindex].setIsStack(false);
            }
            images[mindex].updateAndDraw();
        }
        return true;
    }
    
    /**
     * Interleaves the masses.  Multiple series get converted to a single series, and for masses
     * present in multiple series have their planes added together.   The total number of planes 
     * is the same for all image stacks, but images present in one series but not in another have 
     * planes of zeros added to compensate.
     *
     * @param blockSize blocksize
     * @return number of interleaved masses
     */
    public int interleave(int blockSize) {

        ui.setUpdating(true);
        int numMasses = image.getNMasses();
        String[] massNames = image.getMassNames();
 
        // Create a set of the masses, with no duplicates, then sort numerically, ascending
        SortedSet<String> massesSet = new TreeSet<String>(new Comparator<String>() {
            public int compare(String a, String b) {
                return Float.valueOf(a).compareTo(Float.valueOf(b));
            }
        });

        for (String name : massNames) {           
            massesSet.add(name);
        }
        
        // down below, need an array of the mass names instead of an ArrayList
        String[] massNamesArray = new String[massesSet.size()];
        Iterator mnIter = massesSet.iterator();
        int index = 0;
        while (mnIter.hasNext()) {
            String name = (String)mnIter.next();
            massNamesArray[index] = name;
            index++;
        }
        
        // Find number of series
        int seriesSize = ImageDataUtilities.getSeriesSize(image);
        int numSeries = numMasses/seriesSize;

        int[] numOccurences = new int[massesSet.size()];   
        int[][] pos = new int[massesSet.size()][numSeries];
        
        // Create an array containing the number of occurrences of each unique mass
        Iterator iter = massesSet.iterator();
        index = 0;
        while (iter.hasNext()) {
            //for each unique mass, how many times does it occur in the list of masses
            String mass = (String)iter.next();
            int count = 0;
            int massNamesCount = 0;
            for (String aMass : massNames) { 
                if (aMass.compareTo(mass) == 0) {
                    pos[index][count] = massNamesCount;
                    count++;  
                }
                massNamesCount++;
            }        
            numOccurences[index] = count;
            index++;
        }
          
        //String[] massSymbols = image.getMassSymbols();   // e.g. 12C14N     May not need these.
        
        // Find unique masses.
        String[] unique = new HashSet<String>(Arrays.asList(massNames)).toArray(new String[0]);
        // sort by size ascending
        Arrays.sort(unique, new FloatComparator());  // already have this in massesSet
        int numUniqueMasses = unique.length;        
        
        boolean is16Bit = true;
        int width = images[0].getWidth();
        int height = images[0].getHeight();   

        // Set up the stacks.
        int size = images[0].getNSlices();  // This is number of blocks, not slices/block
        // For uncompressed stacks, number of blocks is the same as number of slices.
        ImageStack[] interleavedStacks = new ImageStack[numUniqueMasses];  // ImageStack is an array of imageJ objects
        for (int mindex = 0; mindex < numUniqueMasses; mindex++) {
            ImageStack iss = new ImageStack(width, height);
            interleavedStacks[mindex] = iss;   
        }
        
        // Check to see whether the pixels are 16 bit or not.
        for (int massNum = 0; massNum < images.length; massNum++) {
            if (images[massNum] == null) {
                break;
            }
            if (images[massNum].getFileInfo().getBytesPerPixel() > 2) {
                is16Bit = false;
                break;
            }
        }
           
        int idx = 0;
        int numSlices = (size + blockSize - 1) / blockSize;   // number of planes or slices
        int numInterleavedSlices = numSeries * numSlices;
        int numSlicesToBeAdded = numInterleavedSlices - numSlices;
        MimsPlus[][] outputMimsPlus = new MimsPlus[numUniqueMasses][numInterleavedSlices];      
 
        // Iterate through the massesAndPositions object.  For each mass, retrieve the mass and the positions
        // in the images array where stack of that mass occurr. For each stack of that mass, add slices
        // (or zero padding).  
        iter = massesSet.iterator();  
        int massNumber = 0;                       
        while (iter.hasNext()) {  
            ImageStack[] sourceStackList = new ImageStack[numSeries];
            String mass = (String)iter.next();
            //OMLOGGER.info("mass number " + massNumber + "    " + mass);
            int[] positions = pos[massNumber];   // Places in images array where a stack of this mass can be found
            for (int i=0; i< positions.length; i++) {
                if ((i!=0) && (positions[i] == 0)) {
                    //sourceStackList[i] = images[positions[0]].getImageStack();  // leave it null
                    int iii = 1;  // placeholder
                } else {
                    sourceStackList[i] = images[positions[i]].getImageStack();
                }              
            }

            // interleave stack in sourceStackList and put into interleavedStacks
            // you can add slices to an imageStack like this:
            //          imageStack[mindex].addSlice(mp[mindex][i].getTitle(), ip);
            //  So slices are stored in ImageProcessor objects.
            int slicePosition = 0;
            for (int i=0; i<numSlices; i++) {
                //OMLOGGER.info("     slice number " + i);
                boolean zeroPad;

                for (int j=0; j<positions.length; j++) {
                    //OMLOGGER.info("         stack number " + j);
                    if ((j!=0) && (positions[j] == 0)) { 
                        zeroPad = true;
                    } else {
                        zeroPad = false;
                    }
                    if (zeroPad) {
                        // add zero padding slice
                        ImageProcessor ip = null;
                        
                        if (is16Bit) {
                            int len = 0;
                            ImageProcessor proc = sourceStackList[0].getProcessor(i+1);
                            proc.getPixels();
                            float[] floatArray;
                            short[] shortArray;
                            int[] intArray;
                            if (proc.getPixels() instanceof float[]) {  
                                floatArray = (float[]) proc.getPixels();
                                len = floatArray.length;
                            } else if (proc.getPixels() instanceof short[]) {
                                shortArray = (short[]) proc.getPixels();
                                len = shortArray.length;
                            } else if (proc.getPixels() instanceof int[]) {
                                intArray = (int[]) proc.getPixels();
                                len = intArray.length;
                            }
                            shortArray = new short[len];
                            for (int jj = 0; jj < len; jj++) {
                                shortArray[jj] = 0;
                            }   
                            ip = new ShortProcessor(width, height);
                            ip.setPixels(shortArray);
                        } else {
                            ip = new FloatProcessor(width, height);
                            ip.setPixels(sourceStackList[0].getProcessor(1).getPixels());
                        }
                        interleavedStacks[massNumber].addSlice(images[positions[0]].getTitle(), ip);
                    } else {
                        ImageProcessor processor = sourceStackList[j].getProcessor(i+1);  // 1-based for some reason
                        // insert it
                        String sliceLabel = sourceStackList[j].getSliceLabel(i+1);
                        // addSlice will add to the beginning of the stack if last param is zero!
                        //OMLOGGER.info("         adding slice at position " + slicePosition);
                        interleavedStacks[massNumber].addSlice(sliceLabel, processor, slicePosition);                         
                    }
                    slicePosition++;
                    //OMLOGGER.info("         index = " + index);
                }
            } 
            //OMLOGGER.info("         ***** setting stack = " + massNumber);
            //OMLOGGER.info("         ***** stack has  = " + sourceStackList[0].getSize() + " slices");
            massNumber++;  
        }
        
        // Create an array of the titles and libreTitles and update them in the MimsPlus instances.
        String[] titles = new String[numUniqueMasses];
        String[] libreTitles = new String[numUniqueMasses];
        for (massNumber=0; massNumber<numUniqueMasses; massNumber++) {
            int titlePos = pos[massNumber][0];
            String title = images[titlePos].getTitle();
            titles[massNumber] = title;
            images[massNumber].setLocalTitle(title);  // may not be necessary
            
            String libreTitle = ImageDataUtilities.formatLibreTitle(titlePos, ui.getOpener(), 
                    images[massNumber]);
            images[massNumber].setLibreTitle(libreTitle);
            
            
        }              
        // Copy interleaved stacks to the images
        for (massNumber=0; massNumber<numUniqueMasses; massNumber++) {      
            images[massNumber].setStack(null, interleavedStacks[massNumber]);       
        }     
        for (massNumber=0; massNumber<numUniqueMasses; massNumber++) {  
            images[massNumber].setTitle(titles[massNumber]);   // sets title in ImagePlus, but not in MimsPlus
            images[massNumber].updateImage();           
        }   
        int numImages = images.length;
        for (int i=massNumber; i<numImages; i++) {         
            if (images[i] == null) {
                break;
            } else {
                //OMLOGGER.info("         ---- closing image = " + i);
                images[i].setAllowClose(true);
                images[i].close();
                if (i >= numUniqueMasses) {
                    images[i] = null;  // Remove unused images
                }
            }
        }      
        ui.mimsAction.setIsInterleaved(true);         
        
        Opener opener = ui.getOpener();  // Opener is an NrrdReader instance
        opener.setNImages(numInterleavedSlices);
        opener.setNMasses(numUniqueMasses);
        opener.setMassNames(massNamesArray);   // Set proper mass names in opener.  
        ui.resetViewMenu();  // Update the View menu to reflect the different mass names.
        
        ui.setUpdating(false);
        return numUniqueMasses;
    }  // end interleave

    
    
    /**
     * Resets the spinners to reflect the values of the current plane. To be used when performing operations that affect
     * the currently displayed plane (for example, uncompress).
     */
    protected void resetSpinners() {
        if (this.images != null && (!holdupdate) && (images[0] != null) && (!ui.isUpdating())) {
            if (THREAD_STATE == this.WORKING) {
                return;
            }
            holdupdate = true;
            int plane = images[0].getCurrentSlice();
            double xval = ui.mimsAction.getXShift(plane);
            double yval = ui.mimsAction.getYShift(plane);
            this.translateXSpinner.setValue(xval);
            this.translateYSpinner.setValue(yval);
            holdupdate = false;
        }
    }

    /**
     * Sets the label displaying the "true index" of plane currently displayed. To be used when performing operations
     * that affect the currently displayed plane (for example, uncompress).
     */
    protected void resetTrueIndexLabel() {

        if (this.images != null && (!holdupdate) && (images[0] != null)) {
            String label = "True index: ";
            int currentSlice = this.images[0].getCurrentSlice();
            //System.out.println("******  current slice is " + currentSlice);
            int trueIndex = ui.mimsAction.trueIndex(currentSlice);  // array index out of bounds
            //System.out.println("******  trueIndex is " + trueIndex);
            label = label + java.lang.Integer.toString(trueIndex);
           // p = this.images[0].getCurrentSlice();
            label = label + "   Display index: " + java.lang.Integer.toString(currentSlice);

            this.trueIndexLabel.setText(label);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        concatButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        deleteListTextField = new javax.swing.JTextField();
        deleteListButton = new javax.swing.JButton();
        trueIndexLabel = new javax.swing.JLabel();
        reinsertButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        reinsertListTextField = new javax.swing.JTextField();
        displayActionButton = new javax.swing.JButton();
        translateXSpinner = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        translateYSpinner = new javax.swing.JSpinner();
        autoTrackButton = new javax.swing.JButton();
        untrackButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        sumButton = new javax.swing.JButton();
        compressButton = new javax.swing.JButton();
        compressTextField = new javax.swing.JTextField();
        sumTextField = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        interleaveButton = new javax.swing.JButton();

        concatButton.setText("Concatenate");
        concatButton.setToolTipText("Prepend or append another image or image stack to the current image set.");
        concatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                concatButtonActionPerformed(evt);
            }
        });

        jLabel5.setText("Delete List (eg: 2,4,8-25,45...)");

        deleteListTextField.setToolTipText("List of planes to delete in the image.");

        deleteListButton.setText("Delete");
        deleteListButton.setToolTipText("Delete the planes listed above.");
        deleteListButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteListButtonActionPerformed(evt);
            }
        });

        trueIndexLabel.setText("True Index: 999   Display Index: 999");

        reinsertButton.setText("Reinsert");
        reinsertButton.setToolTipText("Reinsert the planes listing above.");
        reinsertButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reinsertButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Reinsert List (True plane numbers)");

        reinsertListTextField.setToolTipText("List of previously deleted planes to be reinserted.");

        displayActionButton.setText("Display Action");
        displayActionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayActionButtonActionPerformed(evt);
            }
        });

        translateXSpinner.setToolTipText("Move the image in the X direction by this amount.");
        translateXSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                translateXSpinnerStateChanged(evt);
            }
        });

        jLabel2.setText("Translate X");

        jLabel3.setText("Translate Y");

        translateYSpinner.setToolTipText("Move the image in the Y direction by this amount.");
        translateYSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                translateYSpinnerStateChanged(evt);
            }
        });

        autoTrackButton.setText("Autotrack");
        autoTrackButton.setToolTipText("Using the currently-selected mass, register the image automatically.");
        autoTrackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoTrackButtonActionPerformed(evt);
            }
        });

        untrackButton.setText("Untrack");
        untrackButton.setToolTipText("Reset all translation to zero.");
        untrackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                untrackButtonActionPerformed(evt);
            }
        });

        sumButton.setText("Sum");
        sumButton.setToolTipText("Generate a sum image from whichever mass image or ratio image was most recently clicked.");
        sumButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sumButtonActionPerformed(evt);
            }
        });

        compressButton.setText("Compress");
        compressButton.setToolTipText("Compresses the images into blocks containing the number of planes indicated by the text field to the right of this button.");
        compressButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compressButtonActionPerformed(evt);
            }
        });

        compressTextField.setToolTipText("<html>Enter a positive integer here to specifiy the number of planes that are compressed into blocks by the Compress button.<br>If you leave this field blank and press the Compress button, all images will be compressed into a single plane.</html>");

        sumTextField.setToolTipText("The range of planes to be summed.  Leave blank to sum the entire image.");

        jButton1.setText("Uncompress");
        jButton1.setToolTipText("");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(deleteListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel5)
                                .addComponent(trueIndexLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(deleteListButton, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(reinsertListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(reinsertButton)
                                    .addComponent(jLabel1))
                                .addGap(87, 87, 87)))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(translateXSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel2))
                                .addGap(8, 8, 8))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 121, Short.MAX_VALUE)
                                    .addComponent(compressButton, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                                    .addComponent(sumButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(23, 23, 23))
                            .addComponent(translateYSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(sumTextField, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(compressTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(displayActionButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(concatButton)
                        .addGap(77, 77, 77)
                        .addComponent(autoTrackButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(untrackButton)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(53, 53, 53)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(translateXSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(translateYSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(trueIndexLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteListButton))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addComponent(jLabel4)))
                .addGap(50, 50, 50)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(compressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(compressButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(reinsertListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(reinsertButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sumTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sumButton))
                .addGap(46, 46, 46)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(displayActionButton)
                    .addComponent(concatButton)
                    .addComponent(autoTrackButton)
                    .addComponent(untrackButton))
                .addContainerGap())
        );

        interleaveButton.setText("Interleave");
        interleaveButton.setToolTipText("<html>Combines planes from image stacks that have the same mass, and creates<br> separate image stacks when mass is changed during electrostatic peak switching.</html>");
        interleaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                interleaveButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(163, 163, 163)
                .addComponent(interleaveButton)
                .addContainerGap(445, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(37, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(398, 398, 398)
                .addComponent(interleaveButton)
                .addContainerGap(16, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(36, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * The action method for the "Concatenate" button.
     */
    private void concatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_concatButtonActionPerformed
        UI tempUi = new UI();
        tempUi.setLastFolder(ui.getLastFolder());
        tempUi.loadMIMSFile();
        Opener tempImage = tempUi.getOpener();
        if (tempImage == null) {
            return; // if the FileChooser dialog was canceled
        }
        if (ui.getOpener().getNMasses() == tempImage.getNMasses()) {
            if (sameResolution(image, tempImage)) {
                if (sameSpotSize(image, tempImage)) {
                    Object[] options = {"Append images", "Prepend images", "Cancel"};
                    int value = JOptionPane.showOptionDialog(this, tempUi.getImageFilePrefix(), "Concatenate",
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
                    if (value != JOptionPane.CANCEL_OPTION) {
                        // store action to reapply it after restoring
                        // (shallow copy in mimsAction is enough as 'restoreMims' creates a new 'actionList' object
                        concatImages(value != JOptionPane.YES_OPTION, tempUi);
                        ui.getRoiManager().updateRoiLocations(value != JOptionPane.YES_OPTION);
                    }
                } else {
                    IJ.error("Images do not have the same spot size.");
                }
            } else {
                IJ.error("Images are not the same resolution.");
            }
        } else {
            IJ.error("Two images with the same\nnumber of masses must be open.");
        }

        // close the temporary windows
        for (MimsPlus image : tempUi.getMassImages()) {
            if (image != null) {
                image.setAllowClose(true);
                image.close();
            }
        }
        tempUi = null;

        ui.getMimsData().setHasStack(true);

        ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
        ij.WindowManager.repaintImageWindows();

    }//GEN-LAST:event_concatButtonActionPerformed

    /**
     * The action method for the "delete" button. Deletes certain planes from the stack.
     */
    private void deleteListButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteListButtonActionPerformed
        String liststr = deleteListTextField.getText();
        ArrayList<Integer> checklist = null;
        if (liststr != null && !liststr.equals("")) {
            checklist = parseList(liststr, 1, images[0].getStackSize());
        } else {
            MimsTomography tomo = ui.getMimsTomography();
            //need to check whether both MimTomography and MimsJTable are initialized, else create a new table
            if (tomo != null) {
                MimsJTable tomo_table = tomo.getTable();
                if (tomo_table != null) {
                    checklist = tomo_table.getSelectedImageRows();
                } else if (table == null) {
                    createTable(false);
                } else {
                    checklist = table.getSelectedImageRows();
                    table.close();
                    table = null;
                }
            } else if (table == null) {
                createTable(false);
            } else {
                checklist = table.getSelectedImageRows();
                table.close();
                table = null;
            }
        }
        if (checklist != null) {
            if (!checklist.isEmpty()) {
                liststr = removeSliceList(checklist);
                ui.getMimsLog().Log("Deleted list: " + liststr);
                ui.getMimsLog().Log("New size: " + images[0].getNSlices() + " planes");
                MimsTomography tomo = ui.getMimsTomography();
                if (tomo != null) {
                    MimsJTable tomo_table = tomo.getTable();
                    if (tomo_table != null) {
                        tomo_table.close();
                    }
                }
            }
        }

        ui.updateScrollbars();
        this.resetTrueIndexLabel();
        this.resetSpinners();
        deleteListTextField.setText("");
    }//GEN-LAST:event_deleteListButtonActionPerformed

    /**
     * The action method for the "reinsert" button. Reinserts previously deleted planes back into the stack.
     */
    private void reinsertButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reinsertButtonActionPerformed
        int current = images[0].getCurrentSlice();
        String liststr = reinsertListTextField.getText();
        int trueSize = ui.mimsAction.getSize();
        ArrayList<Integer> checklist = parseList(liststr, 1, trueSize);
        int length = checklist.size();
        if (length == 0) {
            if (table == null) {
                createTable(true);
            } else {
                checklist = table.getSelectedSlices();
                table.close();
                table = null;
            }
        }
        length = checklist.size();
        for (int i = 0; i < length; i++) {
            this.insertSlice(checklist.get(i));
        }
        images[0].setSlice(current);
        this.resetTrueIndexLabel();
        this.resetSpinners();
        reinsertListTextField.setText("");

    }//GEN-LAST:event_reinsertButtonActionPerformed

    /**
     * Action method for the "Display Action" button. Displays the contents of MimsAction to a table for examination.
     */
    private void displayActionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayActionButtonActionPerformed
        ij.text.TextWindow actionWindow = new ij.text.TextWindow("Current Action State", "plane\tx\ty\tdrop\timage index\timage", "", 300, 400);

        int n = ui.mimsAction.getSize();
        String tempstr = "";
        for (int i = 1; i <= n; i++) {
            tempstr = ui.mimsAction.getActionRow(i);
            actionWindow.append(tempstr);
        }
    }//GEN-LAST:event_displayActionButtonActionPerformed

    /**
     * Action method for the "Translate X" jspinner. Applies translations a translations in the x-direction for the
     * current plane.
     */
    private void translateXSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_translateXSpinnerStateChanged
        int plane = images[0].getCurrentSlice();

        double xval = (Double) translateXSpinner.getValue();
        double actx = ui.mimsAction.getXShift(plane);
        double deltax = xval - actx;
        boolean redraw = (deltax * actx < 0);

        if (!holdupdate && (actx != xval) && (!ui.isUpdating())) {
            if (redraw) {
                double[] xy = this.restoreSlice(plane); 
                this.XShiftSlice(xval + xy[0]);
            } else {
                this.XShiftSlice(deltax);
            }
            ui.mimsAction.setShiftX(plane, xval);
        }
    }//GEN-LAST:event_translateXSpinnerStateChanged

    /**
     * Action method for the "Translate Y" jspinner. Applies translations a translations in the y-direction for the
     * current plane.
     */
    private void translateYSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_translateYSpinnerStateChanged
        int plane = images[0].getCurrentSlice();

        double yval = (Double) translateYSpinner.getValue();
        double acty = ui.mimsAction.getYShift(plane);
        double deltay = yval - acty;
        boolean redraw = (deltay * acty < 0);

        if (!holdupdate && (acty != yval) && (!ui.isUpdating())) {
            if (redraw) {
                double[] xy = this.restoreSlice(plane);
                this.YShiftSlice(yval + xy[1]);
            } else {
                this.YShiftSlice(deltay);
            }
            ui.mimsAction.setShiftY(plane, yval);
        }
    }//GEN-LAST:event_translateYSpinnerStateChanged

    /**
     * Action method for the "Autotrack" button. Launches the TrackManager to auto-track the image.
     */
    private void autoTrackButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoTrackButtonActionPerformed
        if (atManager == null) {
            showTrackManager();
        } else {
            atManager.showFrame();
        }
    }//GEN-LAST:event_autoTrackButtonActionPerformed

    /**
     * Action method for the "Untrack" button. Sets all translations to zero.
     */
    private void untrackButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_untrackButtonActionPerformed

        untrack();
    }//GEN-LAST:event_untrackButtonActionPerformed

    /**
     * Launches the Autotrack manager and displays the frame.
     */
    public void showTrackManager() {
        MimsPlus currentImage = (MimsPlus) WindowManager.getCurrentImage();
        if (currentImage.getMimsType() != MimsPlus.MASS_IMAGE) {
            ij.IJ.showMessage("Please select a mass image for tracking.");
            return;
        }
        atManager = new AutoTrackManager(currentImage);
        atManager.showFrame();
    }

    /**
     * Action method for the "Sum" button. Creates a sum image for the most recently selected image.
     */
    private void sumButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sumButtonActionPerformed
        // Get the window title.
        String name = WindowManager.getCurrentImage().getTitle();
        String sumTextFieldString = sumTextField.getText().trim();

        // initialize varaibles.
        SumProps sumProps = null;
        MimsPlus mp = ui.getImageByName(name);
        if (mp == null) {
            return;
        }
        MimsPlus sp;

        // Generate a SumProps object
        if (mp.getMimsType() == MimsPlus.MASS_IMAGE) {
            int parentIdx = mp.getMassIndex();
            if (parentIdx > -1) {
                sumProps = new SumProps(parentIdx);
            }
        } else if (mp.getMimsType() == MimsPlus.RATIO_IMAGE) {
            RatioProps rp = mp.getRatioProps();
            int numIdx = rp.getNumMassIdx();
            int denIdx = rp.getDenMassIdx();
            if (numIdx > -1 && denIdx > -1) {
                sumProps = new SumProps(numIdx, denIdx);
            }
            sumProps.setRatioScaleFactor(mp.getRatioProps().getRatioScaleFactor());
        } else {
            return;
        }
        sumProps.setXWindowLocation(mp.getWindow().getLocationOnScreen().x + MimsPlus.X_OFFSET);
        sumProps.setYWindowLocation(mp.getWindow().getLocationOnScreen().y + MimsPlus.Y_OFFSET);

        // Get list from field box.
        if (sumTextFieldString.isEmpty()) {
            sp = new MimsPlus(ui, sumProps, null);
        } else {
            ArrayList<Integer> sumlist = parseList(sumTextFieldString, 1, ui.mimsAction.getSize());
            if (sumlist.size() == 0) {
                return;
            }
            sp = new MimsPlus(ui, sumProps, sumlist);
        }

        // Show sum image.
        sp.showWindow();
    }//GEN-LAST:event_sumButtonActionPerformed

    /**
     * Action method for the "Compress" button. Compresses the images into blocks of a specified size.
     */
    private void compressButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compressButtonActionPerformed

        // Get the block size from the text box.
        String comptext = compressTextField.getText();
        if (comptext.trim().length() == 0) {
            comptext = Integer.toString(ui.getMimsAction().getSizeMinusNumberDropped());
        }
        int blockSize = 1;
        try {
            blockSize = Integer.parseInt(comptext);
            if (blockSize < 1) {
                throw new Exception();
            }
        } catch (Exception e) {
            ij.IJ.error("<html>Invalid compression value.<br>"
                    + "The number of planes to compress into a block must be a non-zero, positive integer.<br>"
                    + "Or leave this field blank to compress all planes into a single plane.</html>");
            compressTextField.setText("");
            return;
        }

        // Do the compression.
        boolean done = compressPlanes(blockSize);
        ui.getMimsAction().setIsCompressed(done);

        // Do some autocontrasting stuff.
        if (done) {
            ui.getMimsAction().setBlockSize(blockSize);
            deleteListButton.setEnabled(false);
            deleteListTextField.setEnabled(false);
            reinsertButton.setEnabled(false);
            reinsertListTextField.setEnabled(false);
            int nmasses = image.getNMasses();
            for (int mindex = 0; mindex < nmasses; mindex++) {
                images[mindex].setSlice(1);
                images[mindex].updateAndDraw();
                //images[massNumber].updateAndRepaintWindow();
                ui.autoContrastImage(images[mindex]);
            }
        }

        compressTextField.setText("");
        ui.getMimsLog().Log("Compressed with blocksize: " + blockSize);
    }//GEN-LAST:event_compressButtonActionPerformed

    /**
     * Uncompresses the image so that all planes are restored.
     */
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        uncompressPlanes();
    }//GEN-LAST:event_jButton1ActionPerformed

    
   private static void copyFileUsingApacheCommonsIO(File source, File dest)

	        throws IOException {

	    FileUtils.copyFile(source, dest);

	} 
    
    private void interleaveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_interleaveButtonActionPerformed
 
        // Check to see if this file has already been interleaved
        Opener opener1 = ui.getOpener();
        File inputFile = opener1.getImageFile();
        String filename = inputFile.getName();
        if (filename.contains("interleaved")) {
            // Interleaving an already-interleaved file does not do anything, but let the user do anyway.
            int n = JOptionPane.showConfirmDialog(
                this,
                "Based on the filename (" + filename + "), \nthis file may have already been interleaved.\n\nTry to interleave anyway? \n",
                "Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (n == JOptionPane.NO_OPTION) {
                return;
            } 
        }
        
        // Do the interleaving.
        MimsAction mimsAction = ui.getMimsAction();

        int blockSize = mimsAction.getBlockSize();
        int numInterleavedMasses = this.interleave(blockSize);
        int numInterleavedSlices = images[0].getNSlices();

        Opener opener = ui.getOpener();
        // Do some autocontrasting stuff.
        if (numInterleavedMasses > 0) {
            mimsAction.setIsInterleaved(true);
            mimsAction.setBlockSize(blockSize);
            deleteListButton.setEnabled(false);
            deleteListTextField.setEnabled(false);
            reinsertButton.setEnabled(false);
            reinsertListTextField.setEnabled(false);
            //int nmasses = image.getNMasses();
            String[] symbols = new String[numInterleavedMasses];
            for (int massIndex = 0; massIndex < numInterleavedMasses; massIndex++) {
                images[massIndex].setSlice(1);
                images[massIndex].updateAndDraw();
                ui.autoContrastImage(images[massIndex]);
                
                //String[] symbols = opener.getMassSymbols();
                String title = images[massIndex].getLibreTitle();
                String title2 = title.substring(0, title.indexOf("("));
                symbols[massIndex] = title2;
            }
            opener.setMassSymbols(symbols);
            opener.setNMasses(numInterleavedMasses);

        }  else {
            mimsAction.setIsInterleaved(false);
        }
        //numberMasses = numInterleavedMasses;
        ui.enableRestoreMimsMenuItem(false);
           
        //Opener opener = ui.getOpener();
        File oldFile = opener.getImageFile();     
        String oldFilePath = oldFile.getAbsolutePath();
        String newFilePath;
        ///  Users/taylorwrt/MIMS-data/peak-switching-images/160606-c8-9850-LF127-KO-p2_1.im
        //  /Users/taylorwrt/MIMS-data/peak-switching-interleaved.nrrdages/160606-c8-9850-LF127-KO-p2_1.im
        if (oldFilePath.contains("interleaved")) {
            newFilePath = oldFilePath;
        } else {
            if (oldFilePath.endsWith(".im")) {
                // first param of replaceFirst has to be a regular expression
                newFilePath = oldFilePath.replaceFirst("[.]im", "-interleaved.nrrd");  
            } else {
                newFilePath = oldFilePath.replaceFirst(".nrrd", "-interleaved.nrrd");
            }
        }
        
        File fileToSave = new File(newFilePath);
        
//        int n = JOptionPane.showConfirmDialog(
//                this,
//                "Do you want to save this interleaved data?\nIf so, it will be saved in a file called \n" + fileToSave + "\n",
//                "Warning",
//                JOptionPane.YES_NO_OPTION,
//                JOptionPane.QUESTION_MESSAGE);
//
//        if (n == JOptionPane.NO_OPTION) {
//            interleaveButton.setFocusable(false);
//            return;
//        }
        
        String fileToSavePath = null;
        if (fileToSave.exists()) {
            
            int n = JOptionPane.showConfirmDialog(
                this,
                "The file " + fileToSave  + "\nalready exists.  Overwrite the file? \n",
                "Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (n == JOptionPane.NO_OPTION) {
                // add an file number before the .nrrd
                String prefix = newFilePath.substring(0, newFilePath.lastIndexOf("."));
                String extension = ".nrrd";
                String format = prefix + "%02d" + extension;
               // File f = null;
                for (int i = 1; i < 100; i++) {
                    fileToSave = new File(String.format(format, i));
                    if (!fileToSave.exists()) { 
                        break;
                    }
                }
                try {
                    fileToSavePath = fileToSave.getCanonicalPath();
                    System.out.println(fileToSave.getCanonicalPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                JOptionPane.showMessageDialog(this,
                    "The file will be written to a new file called \n" + fileToSavePath,
                    "File Overwrite message",
                    JOptionPane.PLAIN_MESSAGE);
                
                //interleaveButton.setFocusable(false);
                //return;
            }
            if (n == JOptionPane.YES_OPTION) {
                if (!newFilePath.contentEquals(oldFilePath)) {
                    fileToSave.delete();
                }
            }
        } else {          
            try {
                fileToSavePath = fileToSave.getCanonicalPath();
                System.out.println(fileToSave.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            JOptionPane.showMessageDialog(this,
                "The file will be written to a new file called \n" + fileToSavePath,
                "Information",
                JOptionPane.PLAIN_MESSAGE);
        }
       
        try {
            // If the newFilePath and oldFilePath are the same (and overwrite situation, the 
            // copyFileUsingApacheCommonsIO call will throw an exception, so we need to do some
            // shenangians first.
            if (newFilePath.contentEquals(oldFilePath)) {   // only compares file names, not content of file
                // Save new stuff to a tmp file, delete the original, then copy the tmp file back to original file name
                // copy to a temp file, delete old file, then copy temp to old path
                String tempFilePath = oldFilePath.replaceFirst(".nrrd", ".tmp");
                File tempFile = new File(tempFilePath);
                //Files.copy(oldFile.toPath(), tempFile.toPath());  // uses nio
                copyFileUsingApacheCommonsIO(oldFile, tempFile);
                oldFile.delete();
                //Files.copy(tempFile.toPath(), fileToSave.toPath());  // uses nio
                copyFileUsingApacheCommonsIO(tempFile, fileToSave);
                tempFile.delete();

            } else {
            //Files.copy(oldFile.toPath(), fileToSave.toPath());  // uses nio
                copyFileUsingApacheCommonsIO(oldFile, fileToSave);   // oldFile has been interleaved
            }
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(this, ioe.getMessage(), "Error while attempting to create interleaved file.", JOptionPane.ERROR_MESSAGE);
            ioe.printStackTrace();
        }

        ui.updateAllImages();       
        numberMasses = numInterleavedMasses;
        
        // In some cases, if we do not update the number of images in HSIView, an ArrayIndexOutOfBounds 
        // exception gets raised.
        ui.getHSIView().updateImage(true);  
                  
        ui.saveSession(fileToSave.getAbsolutePath(), false); 
        
        //interleaveButton.setSelected(false);
        interleaveButton.setFocusable(false);
           
        ui.getMimsLog().Log("Interleaving completed, resulting blocksize (number of planes) is " + numInterleavedSlices);
    }//GEN-LAST:event_interleaveButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton autoTrackButton;
    private javax.swing.JButton compressButton;
    private javax.swing.JTextField compressTextField;
    private javax.swing.JButton concatButton;
    private javax.swing.JButton deleteListButton;
    private javax.swing.JTextField deleteListTextField;
    private javax.swing.JButton displayActionButton;
    private javax.swing.JButton interleaveButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton reinsertButton;
    private javax.swing.JTextField reinsertListTextField;
    private javax.swing.JButton sumButton;
    private javax.swing.JTextField sumTextField;
    private javax.swing.JSpinner translateXSpinner;
    private javax.swing.JSpinner translateYSpinner;
    private javax.swing.JLabel trueIndexLabel;
    private javax.swing.JButton untrackButton;
    // End of variables declaration//GEN-END:variables

    //Getters for spinners
    public javax.swing.JSpinner getTranslateX() {
        return translateXSpinner;
    }

    public javax.swing.JSpinner getTranslateY() {
        return translateYSpinner;
    }

    /**
     * Crops the image to the Roi selected in RoiManager.
     *
     * @param img the image.
     * @return the cropped image.
     */
    public ImagePlus cropImage(ImagePlus img) {

        ImageStack tempStack = img.getImageStack();

        MimsRoiManager2 roimanager = ui.getRoiManager();
        if (roimanager == null) {
            return null;
        }
        ij.gui.Roi roi = roimanager.getRoi();
        if (roi == null) {
            return null;
        }
        ImageProcessor tempProcessor = null;
        ImageProcessor mask = roi.getMask();
        if (mask == null) {
            tempProcessor = img.getProcessor();
        } else {
            ImagePlus tempImg = img.duplicate();
            img.killRoi();
            tempProcessor = tempImg.getProcessor();
            tempImg.killRoi();
            tempStack = tempImg.getStack();
            int stackSize = tempStack.getSize();
            for (int i = 1; i <= stackSize; i++) {
                tempProcessor = tempStack.getProcessor(i);
                tempProcessor.setValue(0);
                tempProcessor.fillOutside(roi);
            }
        }
        ij.process.StackProcessor stackproc = new ij.process.StackProcessor(tempStack, tempProcessor);
        //int width = roi.getBoundingRect().width;
        int width = roi.getBounds().width;
        int height = roi.getBounds().height;
        int x = roi.getBounds().x;
        int y = roi.getBounds().y;
        img.getProcessor().setRoi(roi);
        img.setStack("cropped", stackproc.crop(x, y, width, height));
        img.killRoi();

        return img;
    }

    /**
     * Returns a subStack of the passed image containing only those planes included in the includeList.
     *
     * @param img the parent image.
     * @param includeList a list of indices to include.
     * @return an image containing only those planes specified in includeList.
     */
    public ImagePlus getSubStack(ImagePlus img, ArrayList<Integer> includeList) {
        ImageStack imgStack = img.getImageStack();
        ImageStack tempStack = new ImageStack(img.getWidth(), img.getHeight());
        for (int i = 0; i < imgStack.getSize(); i++) {
            if (includeList.contains(i + 1)) {
                tempStack.addSlice(Integer.toString(i + 1), imgStack.getProcessor(i + 1));
            }
        }
        ImagePlus tempImg = new ImagePlus("img", tempStack);
        return tempImg;
    }

    /**
     * Applies the translations contained within trans to the current image.
     *
     * @param trans an array of translation values.
     */
    public void notifyComplete(double[][] trans) {

        boolean nullTrans = false;
        atManager.cancelButton.setEnabled(false);
        if (trans == null) {
            nullTrans = true;
        } else {
            applyTranslations(trans, includeList);
        }
        atManager.cancelButton.setEnabled(true);

        THREAD_STATE = DONE;
        ui.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        atManager.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        atManager.closeWindow();
        if (startSlice > -1) {
            images[0].setSlice(startSlice);
        }

        if (nullTrans == true && STATE == MimsStackEditor.OK) {
            System.out.println("translations is null");
        }
    }

    public void createTable(boolean insert) {
        // initialize variables.
        table = new MimsJTable(ui);
        // Determine planes.
        ArrayList<Integer> planes = new ArrayList<Integer>();
        if (insert == false) {
            for (int i = 1; i <= ui.getOpenMassImages()[0].getNSlices(); i++) {
                planes.add(Integer.valueOf(i));
            }
        } else {
            planes = ui.mimsAction.getDroppedList();
            Collections.sort(planes);
        }
        int numPlanes = planes.size();
        Object[][] data = new Object[numPlanes][7];
        for (int i = 0; i < numPlanes; i++) {
            ArrayList ar = ui.mimsAction.getActionList(i + 1);
            data[i][0] = planes.get(i);
            if (insert) {
                data[i][1] = planes.get(i);
            } else {
                data[i][1] = ui.mimsAction.trueIndex(i + 1);
            }
            data[i][2] = ar.get(0);
            data[i][3] = ar.get(1);
            data[i][4] = ar.get(2);
            data[i][5] = ar.get(3);
            data[i][6] = ar.get(4);
        }
        table = new MimsJTable(ui);
        table.setImages(ui.getOpenMassImages());
        String[] columnNames = {"Slice", "True Index", "x", "y", "drop", "image index", "file"};
        table.createCustomTable(data, columnNames);
        table.showFrame();
    }

    /**
     * The AutotrackManager class displays a relatively simple graphical interface allowing the user to specify certain
     * criteria for the autotracking algorithm. Things like using tracking only within a particular ROI or
     * "normailizing" the image can have mean slightly different results generated by the tracking algorithm.
     */
    public class AutoTrackManager extends com.nrims.PlugInJFrame implements ActionListener {

        Frame instance;
        ButtonGroup buttonGroup = new ButtonGroup();
        JTextField txtField = new JTextField();
        JLabel imageLabel;
        JRadioButton all;
        JRadioButton some;
        JRadioButton sub;
        JRadioButton norm;
        JRadioButton eq;
        JRadioButton med;
        JRadioButton show;
        JButton cancelButton;
        JButton okButton;
        MimsPlus currentImage;

        /**
         * The AutoTrackManager constructor assembles and displays the GUI.
         * 
         * @param currentImage a <code>MimsPlus</code> instance
         */
        public AutoTrackManager(MimsPlus currentImage) {
            super("Auto Track Manager");
            if (instance != null) {
                instance.toFront();
                return;
            }
            this.currentImage = currentImage;
            instance = this;

            // Setup radiobutton panel.
            JPanel jPanel = new JPanel();
            jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));

            String imagename = WindowManager.getCurrentImage().getTitle();
            imageLabel = new JLabel("Image:   " + imagename);
            jPanel.add(imageLabel);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            // Radio buttons.
            all = new JRadioButton("Autotrack all images.");
            all.setActionCommand("All");
            all.addActionListener(this);
            all.setSelected(true);

            some = new JRadioButton("Autotrack subset of images. (eg: 2,4,8-25,45...)");
            some.setActionCommand("Subset");
            some.addActionListener(this);
            txtField.setEditable(false);

            sub = new JRadioButton("Use subregion (Roi)");
            sub.setSelected(false);

            norm = new JRadioButton("Normalize tracking image");
            norm.setSelected(true);
            eq = new JRadioButton("Equalize tracking image");
            eq.setSelected(true);
            med = new JRadioButton("Medianize tracking image");
            med.setSelected(true);
            show = new JRadioButton("Show temp image");
            show.setSelected(false);

            buttonGroup.add(all);
            buttonGroup.add(some);

            // Add to container.
            jPanel.add(all);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(some);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(txtField);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(sub);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(norm);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(eq);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(med);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(show);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            // Set up "OK" and "Cancel" buttons.
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            cancelButton = new JButton("Cancel");
            cancelButton.setActionCommand("Cancel");
            cancelButton.addActionListener(this);
            okButton = new JButton("OK");
            okButton.setActionCommand("OK");
            okButton.addActionListener(this);
            buttonPanel.add(cancelButton);
            buttonPanel.add(okButton);

            // Add elements.
            setLayout(new BorderLayout());
            add(jPanel, BorderLayout.PAGE_START);
            add(buttonPanel, BorderLayout.PAGE_END);
            setSize(new Dimension(375, 385));
        }

        /**
         * Implements all required action events methods.
         */
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Subset")) {
                txtField.setEditable(true);
            } else if (e.getActionCommand().equals("All")) {
                txtField.setEditable(false);
            } else if (e.getActionCommand().equals("Cancel")) {
                STATE = CANCEL;
                THREAD_STATE = DONE;
                closeWindow();
                if (startSlice > -1) {
                    images[0].setSlice(startSlice);
                }
            } else if (e.getActionCommand().equals("OK")) {
                STATE = OK;              
                includeList.clear();

                ui.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                atManager.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                try {

                    // Get optional parameters for auto tracking algorithm.
                    String options = getOptions();
                    if (options == null) {
                        STATE = CANCEL;
                        notifyComplete(null);
                        return;
                    }

                    // Get the list of included images and create substack.
                    startSlice = currentImage.getCurrentSlice();
                    if (atManager.getSelection(atManager.buttonGroup).getActionCommand().equals("Subset")) {
                        includeList = parseList(atManager.txtField.getText(), 1, ui.mimsAction.getSize());
                    } else {
                        for (int i = 0; i < images[0].getNSlices(); i++) {
                            includeList.add(i, i + 1);   
                        }
                    }

                    ImagePlus tempImage = getSubStack(currentImage, includeList);
                    
                    tempImage.setSlice(startSlice);

                    // Set options and apply them.
                    tempImage = setOptions(tempImage, options);

                    // Call Autotrack algorithm.
                    THREAD_STATE = WORKING;
                    AutoTrack temptrack = new AutoTrack(ui, tempImage);
                    if (options.contains("show")) {
                        tempImage.show();
                    }
                    temptrack.setIncludeList(includeList);
                    Thread thread = new Thread(temptrack);
                    thread.start();

                    //tempImage.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    notifyComplete(null);
                }
            }
        }

        public void updateImage(MimsPlus img) {
            if (img == null) {
                return;
            }
            this.currentImage = img;
            this.imageLabel.setText("Image:   " + img.getTitle());
        }

        /**
         * Shows the frame.
         */
        public void showFrame() {
            setLocation(400, 400);
            setVisible(true);
            toFront();
            setExtendedState(NORMAL);
        }

        /**
         * Closes the frame.
         */
        public void closeWindow() {
            super.close();
            instance = null;
            this.setVisible(false);
        }

        /**
         * Returns a reference to the MimsRatioManager or null if not open.
         * 
         * @return instance An <code>AutoTraceManager</code> instance
         */
        public AutoTrackManager getInstance() {
            return (AutoTrackManager) instance;
        }

        /**
         * Returns all numbers between min and max NOT in listA.
         * 
         * @param listA An integer ArrayList to reverse
         * @param min minimum index to arraylist of numbers to return
         * @param max maximum index of arraylist of numbers to return
         * 
         * @return listB
         */
        public ArrayList<Integer> getInverseList(ArrayList<Integer> listA, int min, int max) {
            ArrayList<Integer> listB = new ArrayList<Integer>();

            for (int i = min; i <= max; i++) {
                if (!listA.contains(i)) {
                    listB.add(i);
                }
            }

            return listB;
        }

        /**
         * This method returns the selected radio button in a button group.
         * 
         * @param group A <code>ButtonGroup</code> instance
         * 
         * @return b A radiotButton instance, or null if the group has no elements.
         */
        private JRadioButton getSelection(ButtonGroup group) {
            for (Enumeration e = group.getElements(); e.hasMoreElements();) {
                JRadioButton b = (JRadioButton) e.nextElement();
                if (b.getModel() == group.getSelection()) {
                    return b;
                }
            }
            return null;
        }
    }
    
    private class FloatComparator implements Comparator {
        public int compare(Object o1, Object o2) {

            float v1 = Float.valueOf(o1.toString());
            float v2 = Float.valueOf(o2.toString());
            return Float.compare(v1, v2);
            
        }
    }

}

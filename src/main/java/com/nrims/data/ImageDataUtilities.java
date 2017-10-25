/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.data;

import com.nrims.MimsJTable;
import com.nrims.MimsPlus;
import com.nrims.MimsRoiManager2;
import com.nrims.UI;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.process.ImageStatistics;
import org.jfree.data.xy.XYDataset;

/**
 *
 * @author wang2
 */
public class ImageDataUtilities {

    /**
     * Determine how large the series size of a mass spec image is. Particularly useful for 
     * determining whether or not an image is peak switching.
     *
     * @param op image to examine
     * @return the size of the series
     */
    public static int getSeriesSize(Opener op) {
        String[] massNames = op.getMassNames();

        //Special case of single mass image files
        if (op.getNMasses() == 1) {
            return 1;
        }

        int row = 0;
        for (int i = 1; i < massNames.length; i++) {
            Double cur = new Double(massNames[i]);
            Double prev = new Double(massNames[i - 1]);
            if (prev == 0 && i > 1) {
                prev = new Double(massNames[i - 2]);
            }
            if (prev > cur) {
                /*
                 * Explanation: The mass images are ordered in size of mass (save for zero which is always in the sixth position, when it exists)
                 * within each series. When peak switching occurs, there is a drop in the mass in the image sequence.
                 * Ex (m22 m23 m24 m25) ( m22 m23 ... etc)
                 * We use the this to determine the series size
                 */
                //check if cur is zero (in which case we may not be at the end of the row)
                //also check if row has already been set
                if (cur != 0 && row == 0) {
                    row = i;
                }
            }
            if (row == 0 && i + 1 == massNames.length) {
                row = i + 1;
            }
        }
        return row;
    }

    /**
     * Determines which "series" the mass image at index is from. If electric peak-switching was not used, should all
     * ways return 0. An example, if electric peak-switching was used, 4 detectors were used, and the peaks switch once,
     * then: - there will be 8 mass images - this method returns 0 for images at indices 0,1,2,3 - this method returns 1
     * for images at indices 4,5,6,7
     *
     * @param index index of the mass image
     * @param op an <code>Opener</code> reference
     * @return index of the series the mass image is from (bad grammar and all)
     */
    public static int determineSeries(int index, Opener op) {
        int series = getSeriesSize(op);
        return ((index - (index % series)) / series);
    }

    /**
     * Determine if the passed image is a peakswitching image
     *
     * @param op an <code>Opener</code> reference
     * @return true if peak switching, false if not
     */
    public static boolean isPeakSwitching(Opener op) {
        int series = getSeriesSize(op);
        if (series == op.getNMasses()) {
            return false;
        } else {
            return true;
        }

    }

    /**
     * Gets the information stored in the header of the image file and returns it as a string. Currently used as debug
     * data output only.
     *
     * @param im a pointer to the <code>Opener</code>.
     * @return a String containing the metadata.
     */
    public static String getImageHeader(Opener im) {

        // WE HAVE TO DECIDE WHAT WE WANT.
        String[] names = im.getMassNames();
        String[] symbols = im.getMassSymbols();

        String str = "\nHeader: \n";

        str += "Path: " + im.getImageFile().getAbsolutePath() + "\n";
        str += "Masses: ";
        for (int i = 0; i < im.getNMasses(); i++) {
            str += names[i] + " ";
        }
        str += "\n";

        str += "Symbols: ";
        if (symbols != null) {
            for (int i = 0; i < im.getNMasses(); i++) {
                str += symbols[i] + " ";
            }
        }

        str += "\n";

        str += "Pixels: " + im.getWidth() + "x" + im.getHeight() + "\n";

        str += "Raster (nm): " + im.getRaster() + "\n";
        str += "Duration (s): " + im.getDuration() + "\n";
        str += "Dwell time (ms/xy): " + im.getDwellTime() + "\n";
        str += "Stage Position: " + im.getPosition() + "\n";
        str += "Z Position: " + im.getZPosition() + "\n";

        str += "Sample date: " + im.getSampleDate() + "\n";
        str += "Sample hour: " + im.getSampleHour() + "\n";

        str += "Pixel width (nm): " + im.getPixelWidth() + "\n";
        str += "Pixel height (nm): " + im.getPixelHeight() + "\n";

        str += "Dead time Corrected: " + im.isDTCorrected() + "\n";
        str += "QSA Corrected: " + im.isQSACorrected() + "\n";
        if (im.isQSACorrected()) {
            if (im.getBetas() != null) {
                str += "\tBetas: ";
                for (int i = 0; i < im.getBetas().length; i++) {
                    str += im.getBetas()[i];
                    if (i < im.getBetas().length - 1) {
                        str += ", ";
                    }
                }
                str += "\n";
            }

            if (im.getFCObjective() > 0) {
                str += "\tFC Objective: " + im.getFCObjective() + "\n";
            }

        }

        //DJ: 10/13/2014
        if (im.getPrimCurrentT0() != null) {
            str += "FCP Current T0   : " + im.getPrimCurrentT0() + "\n";
        }

        if (im.getPrimCurrentTEnd() != null) {
            str += "FCP Current END : " + im.getPrimCurrentTEnd() + "\n";
        }

        if (im.getD1Pos() != null) {
            str += "D1 Pos : " + im.getD1Pos() + "\n";
        }

        if (im.getESPos() != null) {
            str += "ES Pos : " + im.getESPos() + "\n";
        }

        if (im.getASPos() != null) {
            str += "AS Pos : " + im.getASPos() + "\n";
        }

        if (im.getNPrimL1() != null) {
            str += "Prim L1 : " + im.getNPrimL1() + "\n";
        }

        if (im.getNPrimL0() != null) {
            str += "Prim L0 : " + im.getNPrimL0() + "\n";
        }

        if (im.getNCsHv() != null) {
            str += "CS HV   : " + im.getNCsHv() + "\n";
        }

        str += "End header.\n\n";
        return str;
    }

    /**
     * Method to return title for a single image based on formatString in preferences
     *
     * @param index the index of the image/parent of image you want title for
     * @param extension whether or not to include the file extension in the name
     * @param formatString a string used to determine how the title will be formatted
     * @param image an <code>Opener</code> reference
     * @return a formatted title string according to user preferences
     */
    public static String formatTitle(int index, boolean extension, String formatString, Opener image) {
        char[] formatArray = formatString.toCharArray();
        String curString = "";
        String name = image.getImageFile().getName().toString();
        String[] symbols = image.getMassSymbols();
        for (int i = 0; i < formatArray.length; i++) {
            char curChar = formatArray[i];
            if (curChar == 'M') {
                curString += String.valueOf(image.getMassNames()[index]);
            } else if (curChar == 'F') {
                if (!extension) {
                    curString += name.substring(0, name.lastIndexOf("."));
                } else {
                    curString += name;
                }
            } else if (curChar == 'S') {
                if (image.getMassSymbols() != null) {
                    curString += String.valueOf(symbols[index]);
                }
            } else if (i > 0 && (String.valueOf(curChar).equals("]") || String.valueOf(curChar).equals(")")) && symbols == null
                    && formatArray[i - 1] == 'S') {
                //dont add closing parentheses if there are no symbols, or else we just get empty symbols (Ex m22[]:filename.nrrd)
            } else if (i - 1 < formatArray.length && (String.valueOf(curChar).equals("[") || String.valueOf(curChar).equals("(")) && symbols == null
                    && formatArray[i + 1] == 'S') {
                //dont add opening parentheses if there are no symbols, or else we just get empty symbols (Ex m22[]:filename.nrrd)
            } else {
                curString += String.valueOf(curChar);
            }
        }
        int numBefore;
        if (isPeakSwitching(image)) {
            numBefore = determineSeries(index, image) + 1;
            curString = "(" + numBefore + ") " + curString;

        }
        return curString;
    }

    public static String formatLibreTitle(int index, Opener image, MimsPlus mp) {
        String curString = "";
        String[] symbols = image.getMassSymbols();
        if (mp.getMimsType() == MimsPlus.SUM_IMAGE) {
            curString += "Sum ";
        }
        int numBefore;

        if (image.getMassSymbols() != null) {
            curString += String.valueOf(symbols[index]);
        } else {
            curString += String.valueOf(image.getMassNames()[index]);
        }
        if (isPeakSwitching(image)) {
            numBefore = determineSeries(index, image) + 1;
            curString += "(" + numBefore + ") ";

        }
        return curString;
    }

    public static String formatLibreTitle(int numIndex, int denIndex, Opener image, MimsPlus mp) {
        String curString = "";
        String[] names = image.getMassNames();
        String[] symbols = image.getMassSymbols();
        if (mp.getMimsType() == MimsPlus.HSI_IMAGE) {
            curString += "HSI ";
        } else if (mp.getMimsType() == MimsPlus.RATIO_IMAGE) {
            curString += "Ratio ";
        }
        int numBefore;
        if (isPeakSwitching(image)) {
            numBefore = determineSeries(numIndex, image) + 1;
            curString += "(" + numBefore + ") ";

        }
        if (image.getMassSymbols() != null) {
            curString += String.valueOf(symbols[numIndex]);
            if (denIndex < image.getNMasses()) {
                curString += "/" + String.valueOf(symbols[denIndex]);
            }

        } else {
            curString += String.valueOf(names[numIndex]) + "/";
            if (denIndex < image.getNMasses()) {
                curString += String.valueOf(names[denIndex]);
            } else {
                curString += "1";
            }
        }
        return curString;
    }

    /**
     * Method to return title for a double image (ie ratio, hsi) based on formatString in preferences
     *
     * @param numIndex index of the numerator
     * @param denIndex index of the denominator
     * @param extension whether or not to include the file extension in the name
     * @param formatString a string used to determine how the title will be formatted
     * @param image an <code>Opener</code> reference
     * @return a formatted title string according to user preferences
     */
    public static String formatTitle(int numIndex, int denIndex, boolean extension, String formatString, Opener image) {
        char[] formatArray = formatString.toCharArray();
        String[] names = image.getMassNames();
        String[] symbols = image.getMassSymbols();
        String curString = "";
        String name = image.getImageFile().getName().toString();
        int series;
        boolean isPeakSwitching = isPeakSwitching(image);
        for (int i = 0; i < formatArray.length; i++) {
            char curChar = formatArray[i];
            if (curChar == 'M') {
                if (isPeakSwitching) {
                    series = determineSeries(numIndex, image) + 1;
                    curString = "(" + (series) + ")" + curString;
                }
                curString += String.valueOf(names[numIndex]) + "/";
                if (denIndex < image.getNMasses()) {
                    if (isPeakSwitching) {
                        series = determineSeries(denIndex, image) + 1;
                        curString = "(" + (series) + ")" + curString;
                    }
                    curString += String.valueOf(names[denIndex]);
                } else {
                    curString += "1";
                }
            } else if (curChar == 'F') {
                if (!extension) {
                    curString += name.substring(0, name.lastIndexOf("."));
                } else {
                    curString += name;
                }
            } else if (curChar == 'S') {
                if (symbols != null) {
                    curString += String.valueOf(symbols[numIndex]);
                    if (denIndex < image.getNMasses()) {
                        curString += "/" + String.valueOf(symbols[denIndex]);
                    }
                }
            } else if (i > 0 && (String.valueOf(curChar).equals("]") || String.valueOf(curChar).equals(")")) && symbols == null
                    && formatArray[i - 1] == 'S') {
                //dont add closing parentheses if there are no symbols, or else we just get empty symbols (Ex m22[]:filename.nrrd)
            } else if (i - 1 < formatArray.length && (String.valueOf(curChar).equals("[") || String.valueOf(curChar).equals("(")) && symbols == null
                    && formatArray[i + 1] == 'S') {
                //dont add opening parentheses if there are no symbols, or else we just get empty symbols (Ex m22[]:filename.nrrd)
            } else {
                curString += String.valueOf(curChar);
            }
        }
        return curString;
    }

    /**
     * Generates a new MimsPlus image that is a stack. Whereas ratio image and HSI images are single plane images by
     * design, this method will turn it into a scrollable stack.
     *
     * @param img the image (ratio or HSI images only)
     * @param ui a reference to the <code>UI</code> instance
     */
    public static void generateStack(MimsPlus img, UI ui) {
        //do a few checks
        if (img == null) {
            return;
        }

        //need some reference image that's a stack
        if (ui.getMassImages()[0] == null) {
            return;
        }

        ImagePlus refimp = ui.getMassImages()[0];
        int currentslice = refimp.getSlice();

        //return is there's no stack
        if (refimp.getStackSize() == 1) {
            return;
        }
        //return if it's not a computed image, ie ratio/hsi
        if (!(img.getMimsType() == MimsPlus.RATIO_IMAGE || img.getMimsType() == MimsPlus.HSI_IMAGE)) {
            return;
        }

        ij.ImageStack stack = img.getStack();
        java.awt.image.ColorModel cm = stack.getColorModel();
        ij.ImageStack ims = new ij.ImageStack(stack.getWidth(), stack.getHeight(), cm);
        int numImages = refimp.getStackSize();

        for (int i = 1; i <= numImages; i++) {
            refimp.setSlice(i);
            if (img.getMimsType() == MimsPlus.HSI_IMAGE) {
                while (img.getHSIProcessor().isRunning()) {
                }
            }

            ims.addSlice(refimp.getStack().getSliceLabel(i), img.getProcessor().duplicate());
        }

        // Create new image
        ImagePlus newimp = new ImagePlus("Stack : " + img.getTitle(), ims);
        newimp.setCalibration(img.getCalibration());

        // Display this new stack
        newimp.show();
        newimp.setSlice(currentslice);
        refimp.setSlice(currentslice);

    }

    public static double[][] xyDatasetToTableData(XYDataset dataset) {
        int maxCount = 0;
        for (int j = 0; j < dataset.getSeriesCount(); j++) {
            if (maxCount < dataset.getItemCount(j)) {
                maxCount = dataset.getItemCount(j);
            }
        }
        double[][] data = new double[dataset.getSeriesCount() + 1][maxCount];
        for (int j = 1; j <= dataset.getSeriesCount(); j++) {
            for (int i = 0; i < dataset.getItemCount(j); i++) {
                if (j == 1) {
                    data[0][i] = dataset.getXValue(j - 1, i);
                }
                data[j][i] = dataset.getYValue(j - 1, i);
            }
        }
        return data;
    }

    /**
     *
     * @param ui the value of ui
     */
    public static void centerMassAutoTrack(UI ui) {
        MimsPlus image = (MimsPlus) WindowManager.getCurrentImage();
        MimsRoiManager2 roiManager = ui.getRoiManager();
        Roi roi = roiManager.getRoi();
        Integer[] xy = ui.getRoiManager().getRoiLocation(roi.getName(), 1);
        roi.setLocation(xy[0], xy[1]);
        image.setRoi(roi);
        image.setSlice(1);
        ImageStatistics stats = image.getStatistics(MimsJTable.mOptions);
        double xcenter = stats.xCenterOfMass;
        double ycenter = stats.yCenterOfMass;
        for (int i = 2; i <= image.getNSlices(); i++) {
            xy = ui.getRoiManager().getRoiLocation(roi.getName(), i);
            roi.setLocation(xy[0], xy[1]);
            image.setSlice(i);
            image.setRoi(roi);
            ImageStatistics sliceStats = image.getStatistics(MimsJTable.mOptions);
            double slicexcenter = sliceStats.xCenterOfMass;
            double sliceycenter = sliceStats.yCenterOfMass;
            int deltax = (int) (xcenter - slicexcenter);
            int deltay = (int) (ycenter - sliceycenter);
            MimsPlus[] translateimages = ui.getOpenMassImages();
            ui.getMimsStackEditing().translateStack(deltax, deltay, i, i);
        }
    }
}

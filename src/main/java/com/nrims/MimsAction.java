package com.nrims;

import com.nrims.data.Opener;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * MimsAction.java
 *
 *
 * The MimsAction class stores information regarding the 'state' of the image. State changes can be in the form of x and
 * y translations, dropped planes, compressed planes, block size if compressed, etc.
 *
 */
public class MimsAction implements Cloneable {

    public ArrayList<double[]> xyTranslationList;
    private ArrayList<Integer> droppedList;
    private ArrayList<Integer> imageIndex;
    private ArrayList<String> imageList;
    double zeros[] = {0.0, 0.0};
    private boolean isCompressed = false;
    private boolean isInterleaved = false;
    private boolean isTracked = false;
    private int blockSize = 1;

    public MimsAction(Opener im) {
        resetAction(im);
    }

    /**
     * Initializes all the member variables to the correct size.
     *
     * @param im the opener object.
     */
    private void resetAction(Opener im) {

        // Size of the stack.
        int size = im.getNImages();

        // Set size of member variables.
        xyTranslationList = new ArrayList<double[]>();
        droppedList = new ArrayList<Integer>();
        imageIndex = new ArrayList<Integer>();
        imageList = new ArrayList<String>();

        // Initialize member variables.
        for (int i = 0; i < size; i++) {
            xyTranslationList.add(zeros);
            droppedList.add(0);
            imageIndex.add(i);
            imageList.add(im.getImageFile().getName());
        }
    }

    /**
     * Increases the size of member variables. To be used when planes are appended (or prepended) to the existing image.
     *
     * @param pre boolean <code>true</code> if prepended, <code>false</code> if appending.
     * @param n number of planes being added.
     * @param op a link to the Opener object of the image.
     */
    public void addPlanes(boolean pre, int n, Opener op) {
        int origSize = imageList.size();
        int startIndex;
        if (pre) {
            startIndex = 0;
        } else {
            startIndex = origSize;
        }

        // Add planes to the action-ArrayList.
        int openerPlaneNum = 0;
        for (int i = startIndex; i < n + startIndex; i++) {
            xyTranslationList.add(i, zeros);
            droppedList.add(i, 0);
            imageIndex.add(i, openerPlaneNum);
            imageList.add(i, op.getImageFile().getName());
            openerPlaneNum++;
        }

    }

    /**
     * Returns a String of tab delimited data for a given plane. Used for display purposes only.
     *
     * @param plane the true plane index.
     *
     * @return A string of tab delimited data. Example:
     * <p>
     * "p:1	15.0	20.0	0	0	test_file.im"
     * <p>
     * 1=plane, 15=x-translation, 20=y-translation, 0=dropped value (1 if dropped, 0 otherwise), 0=index of plane in the
     * Opener object, "test_file.im"=name of image
     */
    public String getActionRow(int plane) {
        int idx = plane - 1;
        return "p:" + plane
                + "\t" + roundTwoDecimals((Double) (xyTranslationList.get(idx)[0]))
                + "\t" + roundTwoDecimals((Double) (xyTranslationList.get(idx)[1]))
                + "\t" + droppedList.get(idx)
                + "\t" + imageIndex.get(idx)
                + "\t" + imageList.get(idx);
    }

    public ArrayList getActionList(int plane) {
        int idx = plane - 1;
        ArrayList ar = new ArrayList();
        ar.add(roundTwoDecimals((Double) (xyTranslationList.get(idx)[0])));
        ar.add(roundTwoDecimals((Double) (xyTranslationList.get(idx)[1])));
        ar.add(droppedList.get(idx));
        ar.add(imageIndex.get(idx));
        ar.add(imageList.get(idx));
        return ar;
    }

    /**
     * Gets the total size of the currently open image (dropped images included).
     *
     * @return size of the current image.
     */
    public int getSize() {
        return this.imageList.size();
    }

    public ArrayList<Integer> getDroppedList() {
        ArrayList<Integer> dropped = new ArrayList<Integer>();
        for (int i = 0; i < droppedList.size(); i++) {
            if (droppedList.get(i).intValue() == 1) {
                dropped.add(Integer.valueOf(i + 1));
            }
        }
        return dropped;
    }

    /**
     * Marks a plane as being dropped (assigns a dropped value of 1 to the current plane).
     *
     * @param displayIndex the planes display index.
     */
    public void dropPlane(int displayIndex) {
        int index = trueIndex(displayIndex);
        droppedList.set(index - 1, 1);
    }

    /**
     * Marks a plane as being included (assigns a value of 0 to the current plane).
     *
     * @param trueIndex the true index of the plane.
     */
    public void undropPlane(int trueIndex) {
        droppedList.set(trueIndex - 1, 0);
    }

    /**
     * Apply the given offset (in the x-direction) to the given plane. If image is compressed, apply offset to all
     * planes within that block.
     *
     * @param displayPlane the planes display index.
     * @param offset the offset value.
     */
    public void setShiftX(int displayPlane, double offset) {
        int[] planes = new int[1];
        int tplane = trueIndex(displayPlane);
        planes[0] = tplane;
        double meanTranslation = getXShift(displayPlane);

        if (isCompressed) {
            planes = getPlaneNumbersFromBlockNumber(displayPlane);
        }

        for (int i = 0; i < planes.length; i++) {
            tplane = planes[i];
            double y = xyTranslationList.get(tplane - 1)[1];
            double x = xyTranslationList.get(tplane - 1)[0];
            double xy[] = {offset, y};
            if (isCompressed) {
                double diff = offset - meanTranslation;
                xy[0] = x + diff;
            }
            xyTranslationList.set(tplane - 1, xy);
        }
    }

    /**
     * Apply the given offset (in the y-direction) to the given plane. If image is compressed, apply offset to all
     * planes within that block.
     *
     * @param displayPlane the planes display index.
     * @param offset the offset value.
     */
    public void setShiftY(int displayPlane, double offset) {
        int[] planes = new int[1];
        int tplane = trueIndex(displayPlane);
        planes[0] = tplane;
        double meanTranslation = getYShift(displayPlane);

        if (isCompressed) {
            planes = getPlaneNumbersFromBlockNumber(displayPlane);
        }

        for (int i = 0; i < planes.length; i++) {
            tplane = planes[i];
            double y = xyTranslationList.get(tplane - 1)[1];
            double x = xyTranslationList.get(tplane - 1)[0];
            double xy[] = {x, offset};
            if (isCompressed) {
                double diff = offset - meanTranslation;
                xy[1] = y + diff;
            }
            xyTranslationList.set(tplane - 1, xy);
        }
    }

    /**
     * Gets the size of the currently open image. This is the total size, as return by {@link #getSize() getSize} minus
     * the number of dropped planes. This method could possibly be replaced by a call to MimsPlus getNPlanes method.
     *
     * @return size of the current image (accounts for dropped planes).
     */
    public int getSizeMinusNumberDropped() {
        int nPlanes = 0;
        for (int i = 1; i <= getSize(); i++) {
            if (!isDropped(i)) {
                nPlanes++;
            }
        }
        return nPlanes;
    }

    /**
     * Returns the sequence of plane numbers that make up a given block. Only needs to be used when the current image
     * has been compressed into blocks.
     *
     * @param blockNumber the blockNumber (plane number).
     * @return a series of ints (plane numbers) that make up a block.
     */
    public int[] getPlaneNumbersFromBlockNumber(int blockNumber) {

        if (!isCompressed) {
            int[] planes = new int[1];
            planes[0] = blockNumber;
            return planes;
        }

        // Start and End 'display' planes of the blockNumber.
        int blockStart = ((blockNumber - 1) * blockSize) + 1;
        int blockEnd = (blockNumber * blockSize);
        if (blockEnd > getSizeMinusNumberDropped()) {
            blockEnd = getSizeMinusNumberDropped();
        }

        // Assemble the array.
        ArrayList<Integer> planesArray = new ArrayList<Integer>();
        for (int i = blockStart; i <= blockEnd; i++) {
            int tplane = trueIndex(i);
            if (tplane <= getSize()) {
                planesArray.add(tplane);
            }
        }

        // Convert to int[].
        int[] planes = new int[planesArray.size()];
        for (int i = 0; i < planesArray.size(); i++) {
            planes[i] = planesArray.get(i);
        }

        return planes;
    }

    /**
     * Returns the x-translation for this plane. If compressed than returns the mean of the planes in the block.
     *
     * @param displayPlane the planes display index.
     * @return x-translation value.
     */
    public double getXShift(int displayPlane) {

        int[] planes = new int[1];
        int tplane = trueIndex(displayPlane);
        planes[0] = tplane;

        if (isCompressed) {
            planes = getPlaneNumbersFromBlockNumber(displayPlane);
        }

        double sumX = 0;
        for (int i = 0; i < planes.length; i++) {
            tplane = planes[i];
            sumX += xyTranslationList.get(tplane - 1)[0];
        }
        double xval = sumX / planes.length;
        return xval;
    }

    /**
     * Returns the y-translation for this plane. If compressed than returns the mean of the planes in the block.
     *
     * @param displayPlane the planes display index.
     * @return y-translation value.
     */
    public double getYShift(int displayPlane) {
        int[] planes = new int[1];
        int tplane = trueIndex(displayPlane);
        planes[0] = tplane;

        if (isCompressed) {
            planes = getPlaneNumbersFromBlockNumber(displayPlane);
        }

        double sumY = 0;
        for (int i = 0; i < planes.length; i++) {
            tplane = planes[i];
            sumY += xyTranslationList.get(tplane - 1)[1];
        }
        double yval = sumY / planes.length;
        return yval;
    }

    /**
     * Returns the true index for a plane. If an image has a total of 2 planes, but plane 1 has been dropped, than only
     * 1 plane is visible. In this case, the true index for the first plane is 2.
     *
     * @param dispIndex the true index of the plane.
     * @return true plane index
     */
    public int trueIndex(int dispIndex) {
        int index = 0;
        int zeros = 0;
        while (zeros < dispIndex) {
            if (droppedList.get(index) == 0) {
                zeros++;
            }
            index++;
        }
        return index;
    }

    /**
     * Returns the display index for a plane. If an image has a total of 2 planes, but plane 1 has been dropped, than
     * only one plane is visible. In this case, the display index for the first plane is 1.
     *
     * @param tIndex the planes display index.
     * @return display plane index
     */
    public int displayIndex(int tIndex) {
        int zeros = 0;
        int i = 0;
        while (i < tIndex) {
            if (droppedList.get(i) == 0) {
                zeros++;
            }
            i++;
        }
        if (droppedList.get(tIndex - 1) == 0) {
            return zeros;
        } else {
            return zeros + 1;
        }
    }

    /**
     * Returns <code>true</code> if dropped, returns <code>false</code> if not dropped.
     *
     * @param tIndex the true index of the plane.
     * @return boolean
     */
    public boolean isDropped(int tIndex) {
        if (droppedList.get(tIndex - 1) == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the imageList member variable. The imageList member variable stores the name of the image file from which
     * the plane was read. If only 1 image file was used (no concatenation of files) than all entries will be the same.
     *
     * @return an ArrayList of image file names.
     */
    public ArrayList<String> getImageList() {
        return imageList;
    }

    /**
     * Returns <code>true</code> if image is compressed into blocks, otherwise <code>false</code>.
     *
     * @return boolean
     */
    public boolean getIsCompressed() {
        return isCompressed;
    }

    /**
     * Set to <code>true</code> if compressing the image into blocks.
     *
     * @param compressed <code>true</code> if compressing the image into blocks, otherwise <code>false</code>.
     */
    public void setIsCompressed(boolean compressed) {
        isCompressed = compressed;
    }
    
    /**
     * Set to <code>true</code> if images have been interleaved.
     *
     * @param interleaved <code>true</code> if interleaving has occurred, otherwise <code>false</code>.
     */
    public void setIsInterleaved(boolean interleaved) {
        isInterleaved = interleaved;
    }
    
    /**
     * Returns <code>true</code> if images have been interleaved/code>.
     *
     * @return boolean
     */
    public boolean getIsInterleaved() {
        return isInterleaved;
    }

    /**
     * Returns <code>true</code> if image has been tracked, otherwise <code>false</code>.
     *
     * @return boolean
     */
    public boolean getIsTracked() {
        return isTracked;
    }

    /**
     * Set to <code>true</code> if image has been tracked.
     *
     * @param tracked <code>true</code> if tracked, otherwise <code>false</code>.
     */
    public void setIsTracked(boolean tracked) {
        isTracked = tracked;
    }

    /**
     * Returns the number of images that makes up a block.
     *
     * @return int
     */
    public int getBlockSize() {
        return blockSize;
    }

    /**
     * Sets the number of images that makes up a block.
     *
     * @param size the size of a block
     */
    public void setBlockSize(int size) {
        blockSize = size;
    }

    /**
     * Returns the index within the Opener that the <code>plane</code> comes from.
     *
     * @param plane the true index of the plane.
     */
    public int getOpenerIndex(int plane) {
        return imageIndex.get(plane);
    }

    /**
     * Returns maximum delta translation.
     *
     * @return max delta
     */
    public double getMaxDelta() {
        double max_delta = 0.0;
        if (getIsTracked()) {
            int dispIndx = 1;
            double[][] trans = new double[getSizeMinusNumberDropped()][2];
            for (int i = 0; i < getSize(); i++) {
                if (!isDropped(i + 1)) {
                    trans[dispIndx - 1][0] = getXShift(dispIndx);
                    trans[dispIndx - 1][1] = getYShift(dispIndx);
                    dispIndx++;
                }
            }
            max_delta = AutoTrack.calcMaxDelta(AutoTrack.calcOffset(trans));
        }
        return max_delta;
    }

    /**
     * Returns the name of the Opener object that the <code>plane</code> comes from. If single image file, values for
     * all planes will be the same. If concatenated, than not all planes will be equal.
     *
     * @param plane the true index of the plane.
     * @return the name of the Opener, as a String.
     */
    public String getOpenerName(int plane) {
        return imageList.get(plane);
    }

    /**
     * Returns a <code>double</code> formatted to have 2 decimal places.
     *
     * @param d double value.
     * @return <code>double</code> rounded to 2 decimal places.
     */
    double roundTwoDecimals(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(d));
    }

    /**
     * Returns true if the Action has been modified.
     *
     * @return <code>true</code> if the Action has been modified, otherwise <code>false</code>
     */
    public boolean isImageModified() {

        // Check if any translation was applied,
        // Or any deletion,
        // Or any concatination.
        String originalImage = imageList.get(0);
        for (int i = 0; i < xyTranslationList.size(); i++) {
            if (xyTranslationList.get(i)[0] != 0.0
                    || xyTranslationList.get(i)[1] != 0.0) {
                return true;
            }
            if (droppedList.get(i) != 0) {
                return true;
            }
            if (!imageList.get(i).matches(originalImage)) {
                return true;
            }
        }

        return false;
    }
}

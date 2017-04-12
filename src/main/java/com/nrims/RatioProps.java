package com.nrims;

/**
 * The RatioProps class is required to generate a ratio image and it also serves to store values related to how the
 * image displayed. This class can be written to disk as an object and later loaded by the OpenMIMS plugin to
 * automatically regenerate the image as it was displayed at the time of saving.
 *
 * @author zkaufman
 */
public class RatioProps implements java.io.Serializable {

    //-----------------------------
    static final long serialVersionUID = 2L;
    //-----------------------------
    // DO NOT! Change variable order/type
    // DO NOT! Delete variables
    private int numMassIdx, denMassIdx;
    private double numMassValue, denMassValue;
    private int xloc, yloc;
    private String dataFileName;
    private double ratioScaleFactor;
    //------------------------------
    //End of v2
    private double minLUT;
    private double maxLUT;
    private double mag = 1.0;
    private int numThreshold;
    private int denThreshold;

    /**
     * Create a RatioProps object with given numerator and denominator mass indexes.
     *
     * @param numerator index of numerator
     * @param denominator index of denominator
     */
    public RatioProps(int numerator, int denominator) {
        numMassIdx = numerator;
        denMassIdx = denominator;

        // Default values.
        xloc = -1;
        yloc = -1;
        numMassValue = -1.0;
        denMassValue = -1.0;
        ratioScaleFactor = -1.0;
        minLUT = 0.0;
        maxLUT = 1.0;
        numThreshold = 0;
        denThreshold = 0;
    }

    /**
     * Two <code>RatioProps</code> objects are equal if numerator and denominator are the same.
     *
     * @param rp a <code>RatioProps</code> object.
     * @return <code>true</code> if <code>this</code> and <code>rp</code> are equal.
     */
    public boolean equals(RatioProps rp) {
        if (rp.getNumMassIdx() == numMassIdx && rp.getDenMassIdx() == denMassIdx) {
            return true;
        } else {
            return false;
        }
    }

    // DJ : 08/04/2014
    /**
     * Two <code>RatioProps</code> objects are equal if the numerator's and the denominator's massVales are
     * consecutively the same to the corresponding RatioProp provided.
     *
     * @param rp a <code>RatioProps</code> object.
     * @return <code>true</code> if <code>this</code> and <code>rp</code> are equal.
     */
    public boolean equalsThruMassValues(RatioProps rp) {
        if (rp.getNumMassValue() == numMassValue && rp.getDenMassValue() == denMassValue) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Overwrites the index of the numerator mass set in the constructor.
     *
     * @param numerator mass index (e.g. 0,1,2).
     */
    public void setNumMassIdx(int numerator) {
        this.numMassIdx = numerator;
    }

    /**
     * Gets the index of the mass image of the numerator.
     *
     * @return int
     */
    public int getNumMassIdx() {
        return numMassIdx;
    }

    /**
     * Overwrites the index of the denominator mass set in the constructor.
     *
     * @param denomator mass index (e.g. 0,1,2).
     */
    public void setDenMassIdx(int denomator) {
        this.denMassIdx = denomator;
    }

    /**
     * Gets the index of the mass image of the denominator.
     *
     * @return int
     */
    public int getDenMassIdx() {
        return denMassIdx;
    }

    /**
     * Sets the mass value of the numerator.
     *
     * @param d mass value (e.g. 13.01).
     */
    public void setNumMassValue(double d) {
        this.numMassValue = d;
    }

    /**
     * Gets the mass value of the numerator.
     *
     * @return double mass value (e.g. 13.01).
     */
    public double getNumMassValue() {
        return this.numMassValue;
    }

    /**
     * Sets the mass value of the denominator.
     *
     * @param d mass value (e.g. 13.01).
     */
    public void setDenMassValue(double d) {
        this.denMassValue = d;
    }

    /**
     * Gets the mass value of the denominator.
     *
     * @return double mass value (e.g. 13.01).
     */
    public double getDenMassValue() {
        return this.denMassValue;
    }

    /**
     * Sets the x-value for the window location.
     *
     * @param x x value for the window location
     */
    public void setXWindowLocation(int x) {
        this.xloc = x;
    }

    /**
     * Gets the x-value for the window location.
     *
     * @return int
     */
    public int getXWindowLocation() {
        return this.xloc;
    }

    /**
     * Sets the y-value for the window location.
     *
     * @param y y-value.
     */
    public void setYWindowLocation(int y) {
        this.yloc = y;
    }

    /**
     * Gets the y-value for the window location.
     *
     * @return int y-value.
     */
    public int getYWindowLocation() {
        return this.yloc;
    }

    /**
     * Sets the data file associated with <code>this</code> class. To be used so that when <code>RatioProps</code>
     * objects are saved and then reopend, a pointer exists to the data file used to create the image. It is assumed
     * that the data file exists in the same directory as the <code>RatioProps</code> object being opened, therfore only
     * the name is required.
     *
     * @param fileName file name (no directory)
     */
    public void setDataFileName(String fileName) {
        dataFileName = fileName;
    }

    /**
     * Gets the name of the data file so that images can be regenerated. It is assumed that the data file exists in the
     * same directory as the <code>RatioProps</code> object being opened, therfore only the name is required.
     *
     * @return String file name (no directory)
     */
    public String getDataFileName() {
        return dataFileName;
    }

    /**
     * Sets the scale factor.
     *
     * @param s scale factor.
     */
    public void setRatioScaleFactor(double s) {
        this.ratioScaleFactor = s;
    }

    /**
     * Gets the scale factor.
     *
     * @return double
     */
    public double getRatioScaleFactor() {
        return this.ratioScaleFactor;
    }

    /**
     * Sets the minimum value of the LUT. Used for contrasting purposes.
     *
     * @param min minimum LUT value.
     */
    public void setMinLUT(double min) {
        this.minLUT = min;
    }

    /**
     * Gets the minimum value of the LUT. Used for contrasting purposes.
     *
     * @return double
     */
    public double getMinLUT() {
        return this.minLUT;
    }

    /**
     * Sets the maximum value of the LUT. Used for contrasting purposes.
     *
     * @param max maximum LUT value.
     */
    public void setMaxLUT(double max) {
        this.maxLUT = max;
    }

    /**
     * Gets the maximum value of the LUT. Used for contrasting purposes.
     *
     * @return double
     */
    public double getMaxLUT() {
        return this.maxLUT;
    }

    /**
     * Sets the threshold of the number of counts required for a valid measurement (numerator).
     *
     * @param numThresh int counts required.
     */
    public void setNumThreshold(int numThresh) {
        this.numThreshold = numThresh;
    }

    /**
     * Gets the numerator threshold setting.
     *
     * @return int
     */
    public int getNumThreshold() {
        return this.numThreshold;
    }

    /**
     * Sets the threshold of the number of counts required for a valid measurement (denominator).
     *
     * @param denThresh int counts required
     */
    public void setDenThreshold(int denThresh) {
        this.denThreshold = denThresh;
    }

    /**
     * Get the denominator threshold setting.
     *
     * @return int
     */
    public int getDenThreshold() {
        return this.denThreshold;
    }

    /**
     * Sets the magnification level.
     *
     * @param m magnification level.
     */
    public void setMag(double m) {
        this.mag = m;
    }

    /**
     * Gets the magnification level
     *
     * @return double
     */
    public double getMag() {
        return this.mag;
    }
}

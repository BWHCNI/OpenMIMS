package com.nrims;

/**
 * The HSIProps class is required to generate an HSI image. It also serves to store values related to how the image
 * calculated and displayed. This class can be written to disk as an object and later loaded by the OpenMIMS plugin to
 * automatically regenerate the image as it was displayed at the time of saving.
 *
 * @author zkaufman
 *
 */
public class HSIProps implements java.io.Serializable {
    //-----------------------------
    static final long serialVersionUID = 2L;
    //-----------------------------
    // DO NOT! Change variable order/type
    // DO NOT! Delete variables
    private int numMassIdx;
    private int denMassIdx;
    private double numMassValue, denMassValue;
    private double maxRatio;
    private double minRatio;
    private int minNum;
    private int minDen;
    private int maxRGB;
    private int minRGB;
    private int transparency;
    private int label;
    private double ratioScaleFactor;
    private boolean transform;
    private float referenceRatio;
    private float backgroundRatio;
    private int xloc, yloc;
    private String dataFileName;
    //--------------------------------------
    //End of v2

    private double mag = 1.0;

    /**
     * Creates a new instance of HSIProps
     */
    private HSIProps() {
    }

    /**
     * Create an HSIProps object with given numerator and denominator mass indexes.
     *
     * @param numerator index of numerator
     * @param denominator index of denominator
     */
    public HSIProps(int numerator, int denominator) {

        numMassIdx = numerator;
        denMassIdx = denominator;
        maxRatio = 100;
        minRatio = 37;
        minNum = 0;
        minDen = 0;
        maxRGB = 255;
        minRGB = 0;
        transparency = 2;
        label = 2;
        ratioScaleFactor = -1.0;

        //TODO: this is for testing and should be fixed
        transform = false;
        referenceRatio = (float) 37 / (float) 10000;
        backgroundRatio = (float) 129 / (float) 10000;
        xloc = -1;
        yloc = -1;
        numMassValue = -1.0;
        denMassValue = -1.0;
        dataFileName = null;
        //---------------------------------

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
     * @return denominator mass index.
     */
    public int getDenMassIdx() {
        return denMassIdx;
    }

    /**
     * Sets the numerator mass value.
     *
     * @param d mass value (e.g. 13.01)
     */
    public void setNumMassValue(double d) {
        this.numMassValue = d;
    }

    /**
     * Gets the numerator mass value.
     *
     * @return numerator mass value (e.g. 13.01)
     */
    public double getNumMassValue() {
        return this.numMassValue;
    }

    /**
     * Sets the denominator mass value.
     *
     * @param d mass value (e.g. 13.01)
     */
    public void setDenMassValue(double d) {
        this.denMassValue = d;
    }

    /**
     * Gets the denominator mass value.
     *
     * @return mass value (e.g. 13.01)
     */
    public double getDenMassValue() {
        return this.denMassValue;
    }

    /**
     * Sets the threshold of the number of counts required for a valid measurement (numerator).
     *
     * @param numThresh int counts required.
     */
    public void setNumThreshold(int numThresh) {
        this.minNum = numThresh;
    }

    /**
     * Gets the numerator threshold setting.
     *
     * @return int
     */
    public int getNumThreshold() {
        return this.minNum;
    }

    /**
     * Sets the threshold of the number of counts required for a valid measurement (denominator).
     *
     * @param denThresh int counts required
     */
    public void setDenThreshold(int denThresh) {
        this.minDen = denThresh;
    }

    /**
     * Gets the denominator threshold setting.
     *
     * @return int
     */
    public int getDenThreshold() {
        return this.minDen;
    }

    /**
     * Sets the maximum value of the colorbar.
     *
     * @param d max colorbar value.
     */
    public void setMaxRatio(double d) {
        maxRatio = d;
    }

    /**
     * Gets the maximum value of the colorbar.
     *
     * @return max colorbar value.
     */
    public double getMaxRatio() {
        return maxRatio;
    }

    /**
     * Sets the mimimum value of the colorbar.
     *
     * @param d minimum colorbar value.
     */
    public void setMinRatio(double d) {
        minRatio = d;
    }

    /**
     * Gets the minimum value of the colorbar.
     *
     * @return minimum colorbar value.
     */
    public double getMinRatio() {
        return minRatio;
    }

    /**
     * Sets the maximum RGB value.
     *
     * @param n maximum RGB value (0-255)
     */
    public void setMaxRGB(int n) {
        maxRGB = n;
    }

    /**
     * Gets the maximum RGB value.
     *
     * @return the maximum RGB value (0-255)
     */
    public int getMaxRGB() {
        return maxRGB;
    }

    /**
     * Sets the minimum RGB value.
     *
     * @param n minimum RGB value.
     */
    public void setMinRGB(int n) {
        minRGB = n;
    }

    /**
     * Gets the minimum RGB value.
     *
     * @return minimum RGB value.
     */
    public int getMinRGB() {
        return minRGB;
    }

    /**
     * Sets the transparency setting. Actual transparency (numerator, denominator, min max numerator denominator) is
     * determined by the index in the jcombobox in the HSIView class.
     *
     * @param n transparency index.
     */
    public void setTransparency(int n) {
        transparency = n;
    }

    /**
     * Gets the transparency setting.
     *
     * @return transparency index.
     */
    public int getTransparency() {
        return transparency;
    }

    /**
     * Sets the labeling method (none, colorbar, labels and colorbar). Actual labeling method is derived from the index
     * passed and the jcombobox in the HSIView class.
     *
     * @param n the label method index.
     */
    public void setLabelMethod(int n) {
        label = n;
    }

    /**
     * Gets the labeling method index
     *
     * @return the label method index.
     */
    public int getLabelMethod() {
        return label;
    }

    /**
     * Sets the scale factor of the underlying ratio image.
     *
     * @param s scale factor
     */
    public void setRatioScaleFactor(double s) {
        this.ratioScaleFactor = s;
    }

    /**
     * Gets the scale factor of the underlying ratio image.
     *
     * @return the scale factor
     */
    public double getRatioScaleFactor() {
        return this.ratioScaleFactor;
    }

    /**
     * Sets the x-value for the window location.
     *
     * @param x  window x location
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
     * @param y  window y location
     */
    public void setYWindowLocation(int y) {
        this.yloc = y;
    }

    /**
     * Gets the y-value for the window location.
     *
     * @return int
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
     * @return the magnification level.
     */
    public double getMag() {
        return this.mag;
    }

    /**
     * Sets this class' properties from another class
     *
     * @param props the HSIProps instance
     */
    public void setProps(HSIProps props) {
        numMassIdx = props.getNumMassIdx();
        denMassIdx = props.getDenMassIdx();
        minNum = props.getNumThreshold();
        minDen = props.getDenThreshold();
        maxRatio = props.getMaxRatio();
        minRatio = props.getMinRatio();
        maxRGB = props.getMaxRGB();
        minRGB = props.getMinRGB();
        transparency = props.getTransparency();
        label = props.getLabelMethod();
        xloc = props.getXWindowLocation();
        yloc = props.getYWindowLocation();
        mag = props.getMag();
    }

    /**
     * Sets the HSI props passed as an argument to this class properties
     *
     * @param props
     */
    private void getProps(HSIProps props) {
        props.setNumMassIdx(numMassIdx);
        props.setDenMassIdx(denMassIdx);
        props.setNumThreshold(minNum);
        props.setDenThreshold(minDen);
        props.setMaxRatio(maxRatio);
        props.setMinRatio(minRatio);
        props.setMaxRGB(maxRGB);
        props.setMinRGB(minRGB);
        props.setTransparency(transparency);
        props.setLabelMethod(label);
        props.setXWindowLocation(xloc);
        props.setYWindowLocation(yloc);
        props.setRatioScaleFactor(ratioScaleFactor);
        props.setMag(mag);
    }

    /**
     * Returns an HSIProps object with all the same properties as this class.
     *
     * @return an HSIProps object
     */
    public HSIProps clone() {
        HSIProps props = new HSIProps();
        getProps(props);
        return props;
    }

    /**
     * Tests if this object is equal to the passed parameter. Two HSIProps objects are equal if numerator and
     * denominator are the same.
     *
     * @param rp the HSIProps instance
     * @return <code>true</code> if num and den are the same, otherwise <code>false</code>.
     */
    public boolean equals(HSIProps rp) {
        if (rp.getNumMassIdx() == numMassIdx && rp.getDenMassIdx() == denMassIdx) {
            return true;
        } else {
            return false;
        }
    }

    // DJ: 08/04/2014
    /**
     * Tests if this object is equal massValue-wise to the passed parameter Two HSIProps objects are equal if numerator
     * and denominator are the same massValue-wise.
     *
     * @param rp return param?
     * @return <code>true</code> if num and den are the same, otherwise <code>false</code>.
     */
    public boolean equalsThruMassValues(HSIProps rp) {
        if (rp.getNumMassValue() == numMassValue && rp.getDenMassValue() == denMassValue) {
            return true;
        } else {
            return false;
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims;

/**
 *
 * @author wang2
 */
public class MassProps implements java.io.Serializable {

    //-----------------------------
    static final long serialVersionUID = 2L;
    //-----------------------------
    // DO NOT! Change variable order/type
    // DO NOT! Delete variables

    private int massIdx;
    private double massValue;
    private int xloc, yloc;
    private String dataFileName;
    private double ratioScaleFactor;
    //--------------------------------
    //End of v2
    private double mag = 1.0;
    private double minLUT;
    private double maxLUT;
    private int currentSlice;
    private boolean hidden;

    /**
     * Initiates a MassProps object.
     *
     * @param massIndex the index of the mass image.
     */
    public MassProps(int massIndex) {
        this.massIdx = massIndex;

        // Default values.
        xloc = -1;
        yloc = -1;
        ratioScaleFactor = -1.0;
    }

    // DJ: 08/05/2014
    // to be used in compositeManager.
    // so composite_props would rely on massValues instead of massindexes
    // to avoid getting wrong values.
    // COULD BE USED AS NEEDED.
    /**
     * Initiates a MassProps object.
     *
     * @param massIndexthe of the mass image
     * @param massValue of the mass image
     */
    public MassProps(int massIndex, double massValue) {
        this.massIdx = massIndex;
        this.massValue = massValue;

        // Default values.
        xloc = -1;
        yloc = -1;
        ratioScaleFactor = -1.0;
    }

    /**
     * Two <code>MassProps</code> objects are equal if numerator and denominator are the same, in the case of a sum of a
     * ratio image. Or if the parent mass index is the same, in the case of a sum of a mass image.
     *
     * @param sp a <code>MassProps</code> object.
     * @return <code>true</code> if <code>this</code> and <code>sp</code> are equal.
     */
    public boolean equals(MassProps sp) {
        if (sp.getMassIdx() == massIdx) {
            return true;
        } else {
            return false;
        }
    }

    // DJ: 08/04/2014
    /**
     * Checks equality of two <code>MassProps</code> through their mass values.
     *
     * @param sp a <code>MassProps</code> object.
     * @return <code>true</code> if <code>this</code> and <code>sp</code> are equal.
     */
    public boolean equalsThruMassValues(MassProps sp) {
        if (sp.getMassValue() == massValue) {
            return true;
        } else {
            return false;
        }
    }

    public boolean equalsThruMassValues(double mass) {
        if (mass == massValue) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the scale factor used to create the image.
     *
     * @param rsf scale factor (default 10,000).
     */
    public void setRatioScaleFactor(double rsf) {
        ratioScaleFactor = rsf;
    }

    /**
     * Gets the scale factor used to create the image.
     *
     * @return the scale factor.
     */
    public double getRatioScaleFactor() {
        return ratioScaleFactor;
    }

    /**
     * Overwrites the index of the mass set in the constructor.
     *
     * @param numerator mass index (e.g. 0,1,2).
     */
    public void setMassIdx(int massIdx) {
        this.massIdx = massIdx;
    }

    /**
     * Gets the index of the mass.
     *
     * @return the index of the image.
     */
    public int getMassIdx() {
        return massIdx;
    }

    /**
     * Sets the mass value of the mass image.
     *
     * @param d mass of parent image.
     */
    public void setMassValue(double d) {
        massValue = d;
    }

    /**
     * Gets the mass of the mass image.
     *
     * @return mass of parent image.
     */
    public double getMassValue() {
        return massValue;
    }

    /**
     * Sets the x-value of the window location.
     *
     * @param x x-value of the window location.
     */
    public void setXWindowLocation(int x) {
        this.xloc = x;
    }

    /**
     * Gets the x-value of the window location.
     *
     * @return x-value of window location.
     */
    public int getXWindowLocation() {
        return this.xloc;
    }

    /**
     * Sets the y-value of the window location.
     *
     * @param y y-value of the window location.
     */
    public void setYWindowLocation(int y) {
        this.yloc = y;
    }

    /**
     * Gets the y-value of the window location.
     *
     * @return y-value of window location.
     */
    public int getYWindowLocation() {
        return this.yloc;
    }

    // set, get visibility: DJ 07/29/2014.
    /**
     * Sets the hidden property of the window.
     *
     * @param isHidden true if hidden, false otherwise
     */
    public void setVisibility(boolean isHidden) {
        this.hidden = isHidden;
    }

    /**
     * Gets the hidden property of the window as boolean.
     *
     * @return true if hidden or false otherwise.
     */
    public boolean getVisibility() {
        return this.hidden;
    }

    /**
     * Sets the name of the data file from which this image was derived.
     *
     * @param fileName name of file (name only, do not include directory).
     */
    public void setDataFileName(String fileName) {
        dataFileName = fileName;
    }

    /**
     * Gets the name of the data file form which this image was derived.
     *
     * @return name of file (name only, does not include a directory).
     */
    public String getDataFileName() {
        return dataFileName;
    }

    /**
     * Sets the magnification factor.
     *
     * @param m magnification factor.
     */
    public void setMag(double m) {
        this.mag = m;
    }

    /**
     * Gets the magnification factor.
     *
     * @return magnification factor.
     */
    public double getMag() {
        return this.mag;
    }

    /**
     * Sets the minimum value of the lut, for display purposes only.
     *
     * @param min lut minimum value.
     */
    public void setMinLUT(double min) {
        this.minLUT = min;
    }

    /**
     * Gets the minimum value of the lut, for display purposes only.
     *
     * @return minimum lut value.
     */
    public double getMinLUT() {
        return this.minLUT;
    }

    /**
     * Sets the maximum value of the lut, for diaply purposes only.
     *
     * @param max lut maximum value.
     */
    public void setMaxLUT(double max) {
        this.maxLUT = max;
    }

    /**
     * Gets the maximum value of the lut, for display purposes only.
     *
     * @return maximum lut value.
     */
    public double getMaxLUT() {
        return this.maxLUT;
    }

    public void setCurrentSlice(int slice) {
        this.currentSlice = slice;
    }

    public int getCurrentSlice() {
        return this.currentSlice;
    }

}

package com.nrims;

import java.io.Serializable;

/**
 * A container class for storing properties needed to generate a Composite image.
 *
 * @author cpoczatek
 */
public class CompositeProps implements Serializable {

    //-----------------------------
    static final long serialVersionUID = 2L;
    //-----------------------------
    // DO NOT! Change variable order/type
    // DO NOT! Delete variables
    private Object[] imageProps;
    private String dataFileName;
    //------------------------------
    //End of v2

    // DJ:08/04/2014   
    private int xloc, yloc;
    private double mag = 1.0;

    /**
     * Instantiates a CompositeProps object with images <code>imgs</code>.
     *
     * @param imgs set of images used to create the composite image.
     */
    public CompositeProps(Object[] imgs) {
        this.imageProps = imgs;

        // DJ: 08/04/2014  Default values.
        this.xloc = -1;
        this.yloc = -1;
    }

    // DJ: 08/04/2014  Modified to use eqaulity check thru mass values instead of mass indexes.
    /**
     * Two <code>CompositeProps</code> objects are equal if the <code>MimsPlus</code> objects that make them up are
     * equal.
     *
     * @param cp a <code>CompositeProps</code> object.
     * @return <code>true</code> if <code>this</code> and <code>cp</code> are equal.
     */
    public boolean equals(CompositeProps cp) {

        // If lengths are different then obviously they are not equal.
        Object[] cps = cp.getImageProps();
        if (cps.length != imageProps.length) {
            return false;
        }

        // If the contents of the images array of the two objects differ
        // then the two objects are considered different, even if the
        // contents are the same, but in a different order.
        for (int i = 0; i < imageProps.length; i++) {
            //if neither is null, check if images are equal
            if (cps[i] != null && imageProps[i] != null) {

                //---------------------------------------------------------------
                if (imageProps[i] instanceof RatioProps) {
                    if (!(cps[i] instanceof RatioProps)) {
                        return false;
                    }
                    if ((cps[i] instanceof RatioProps) && !((RatioProps) cps[i]).equalsThruMassValues((RatioProps) imageProps[i])) {
                        return false;
                    }
                }
                //---------------------------------------------------------------
                if (imageProps[i] instanceof HSIProps) {
                    if (!(cps[i] instanceof HSIProps)) {
                        return false;
                    }
                    if ((cps[i] instanceof HSIProps) && !((HSIProps) cps[i]).equalsThruMassValues((HSIProps) imageProps[i])) {
                        return false;
                    }
                }
                //---------------------------------------------------------------
                if (imageProps[i] instanceof SumProps) {

                    if (!(cps[i] instanceof SumProps)) {
                        return false;
                    }
                    if ((cps[i] instanceof SumProps) && ((SumProps) (imageProps[i])).getSumType() != ((SumProps) cps[i]).getSumType()) {
                        return false;
                    }
                    if ((cps[i] instanceof SumProps) && ((SumProps) (imageProps[i])).getSumType() == ((SumProps) cps[i]).getSumType()
                            && ((SumProps) cps[i]).equalsThruMassValues((SumProps) imageProps[i]) == false) {
                        return false;
                    }
                }
                //---------------------------------------------------------------
                if (imageProps[i] instanceof MassProps) {
                    if (!(cps[i] instanceof MassProps)) {
                        return false;
                    }
                    if ((cps[i] instanceof MassProps) && !((MassProps) cps[i]).equalsThruMassValues((MassProps) imageProps[i])) {
                        return false;
                    }
                }
            }
            //if one is null and the other not they are unequal
            if ((cps[i] != null && imageProps[i] == null) || (cps[i] == null && imageProps[i] != null)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sets the images to be used for generating the composite image.
     *
     * @param imgs set of images used to create the composite image.
     */
    public void setImageProps(Object[] imgs) {
        this.imageProps = imgs;
    }

    public Object[] getImageProps() {
        return this.imageProps;
    }

    /**
     * Gets the images used to generate the composite image.
     *
     * @param ui reference to the UI.
     * @return the array of images used to create the composite image.
     */
    public MimsPlus[] getImages(UI ui) {

        MimsPlus[] images = new MimsPlus[imageProps.length];
        MimsPlus[] massImages = ui.getOpenMassImages();
        MimsPlus[] sumImages = ui.getOpenSumImages();
        MimsPlus[] ratioImages = ui.getOpenRatioImages();
        MimsPlus[] hsiImages = ui.getOpenHSIImages();

        for (int i = 0; i < imageProps.length; i++) {
            if (imageProps[i] != null) {

                if (imageProps[i] instanceof RatioProps) {
                    for (int j = 0; j < ratioImages.length; j++) {
                        // DJ: 08/04/2014
                        // check for equality thru masses and not thru indexes.
                        // if (ratioImages[j].getRatioProps().equals((RatioProps) imageProps[i])){  // commented out by DJ
                        if (ratioImages[j].getRatioProps().equalsThruMassValues((RatioProps) imageProps[i])) {
                            images[i] = ratioImages[j];
                        }
                    }
                }
                if (imageProps[i] instanceof HSIProps) {
                    for (int j = 0; j < hsiImages.length; j++) {
                        // DJ: 08/04/2014
                        // check for equality thru masses and not thru indexes.
                        // if (hsiImages[j].getHSIProps().equals((HSIProps) imageProps[i])) { // commented out by DJ
                        if (hsiImages[j].getHSIProps().equalsThruMassValues((HSIProps) imageProps[i])) {
                            images[i] = hsiImages[j];
                        }
                    }
                }
                if (imageProps[i] instanceof SumProps) {
                    for (int j = 0; j < sumImages.length; j++) {

                        // DJ: 08/04/2014
                        // check for equality thru masses and not thru indexes.
                        // if (sumImages[j].getSumProps().equals((SumProps) imageProps[i])){ // commented out by DJ 
                        if (sumImages[j].getSumProps().equalsThruMassValues((SumProps) imageProps[i])) {
                            images[i] = sumImages[j];
                        }
                    }
                }
                if (imageProps[i] instanceof MassProps) {
                    for (int j = 0; j < massImages.length; j++) {
                        MassProps massProp = (MassProps) imageProps[i];

                        // DJ: 08/04/2014
                        // check for equality thru masses and not thru indexes.
                        //if (massImages[j].getMassIndex() == massProp.getMassIdx()){  // commented out by DJ
                        if (UI.massesEqualityCheck(massImages[j].getMassValue(), massProp.getMassValue(), 0.49)) {
                            images[i] = massImages[j];
                        }
                    }
                }
            } // end of not null check
        }
        return images;
    }

    /**
     * Sets the name of the data file from which this image was derived.
     *
     * @param fileName name of file (name only, do not include directory).
     */
    public void setDataFileName(String fileName) {
        dataFileName = fileName;
    }

    public String getDataFileName() {
        return dataFileName;
    }

    // DJ: 08/04/2014  ALL WINDOW/MAG SET/GET STUFF
    /**
     * Sets the x-value for the window location.
     *
     * @param x x coordinate of window
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
     * Sets the magnification factor.
     *
     * @param m magnification factor.
     */
    public void setMag(double m) {
        this.mag = m;
    }

}

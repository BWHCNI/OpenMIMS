/*
 * HSIProps.java
 *
 * Created on May 4, 2006, 11:19 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.nrims;

public class HSIProps implements java.io.Serializable {

    private int numMassIdx ;
    private int denMassIdx ;
    private double maxRatio ;
    private double minRatio ;
    private int minNum ;
    private int minDen ;
    private int maxRGB ;
    private int minRGB ;
    private int transparency ;
    private int label ;
    private double ratioScaleFactor;
    private boolean transform;
    private float referenceRatio;
    private float backgroundRatio;
    private int xloc, yloc;
    private String dataFileName;
    
    /** Creates a new instance of HSIProps */
    public HSIProps() {}

    public HSIProps(int numerator, int denominator) {

        numMassIdx = numerator;
        denMassIdx = denominator;
        maxRatio = 1.0 ;
        minRatio = 0.01 ;
        minNum = 3 ;
        minDen = 3 ;
        maxRGB = 255 ;
        minRGB = 0 ;
        transparency = 0 ;
        label = 0 ;
        ratioScaleFactor = 10000;

        //TODO: this is for testing and should be fixed
        transform = false;
        referenceRatio = (float)37/(float)10000;
        backgroundRatio = (float)129/(float)10000;
        xloc = -1;
        yloc = -1;
        dataFileName = null;
    }
    public void setNumMassIdx(int n) { numMassIdx = n ; }
    public int getNumMassIdx() { return numMassIdx ; }
    public void setDenMassIdx(int n) { denMassIdx = n ; }
    public int getDenMassIdx() { return denMassIdx ; }
    public void setMinNum(int n) { minNum = n ; }
    public int getMinNum() { return minNum ; }
    public void setMinDen(int n) { minDen = n ; }
    public int getMinDen() { return minDen ; }
    public void setMaxRatio(double d) { maxRatio = d ; }
    public double getMaxRatio() { return maxRatio ; }
    public void setMinRatio(double d) { minRatio = d ; }
    public double getMinRatio() { return minRatio ; }
    public void setMaxRGB(int n) { maxRGB = n ; }
    public int getMaxRGB() { return maxRGB ; }
    public void setMinRGB(int n) { minRGB = n ; }
    public int getMinRGB() { return minRGB ; }
    public void setTransparency(int n) { transparency = n ; }
    public int getTransparency() { return transparency ; }
    public void setLabelMethod(int n) { label = n ; }
    public int getLabelMethod() { return label ; }
    public void setRatioScaleFactor(double s) { this.ratioScaleFactor = s; }
    public double getRatioScaleFactor() { return this.ratioScaleFactor; }
    public void setXWindowLocation(int x) { this.xloc = x; }
    public int getXWindowLocation() { return this.xloc; }
    public void setYWindowLocation(int y) { this.yloc = y; }
    public int getYWindowLocation() { return this.yloc; }     
    public void setTransform(boolean trans) { this.transform = trans; }
    public boolean getTransform() { return this.transform; }
    public void setReferenceRatio(float ref) { this.referenceRatio = ref; }
    public float getReferenceRatio() { return this.referenceRatio; }
    public void setBackgroundRatio(float bg) { this.backgroundRatio = bg; }
    public float getBackgroundRatio() { return this.backgroundRatio; }
    public void setDataFileName(String fileName) { dataFileName = fileName;}
    public String getDatFileName() { return dataFileName; }
    /**
     * Set this class' properties from another class
     */
    public void setProps(HSIProps props) {
        numMassIdx = props.getNumMassIdx();
        denMassIdx = props.getDenMassIdx();
        minNum = props.getMinNum();
        minDen = props.getMinDen();
        maxRatio = props.getMaxRatio();
        minRatio = props.getMinRatio();
        maxRGB = props.getMaxRGB();
        minRGB = props.getMinRGB();
        transparency = props.getTransparency();
        label = props.getLabelMethod();
        transform = props.getTransform();
        referenceRatio = props.getReferenceRatio();
        backgroundRatio = props.getBackgroundRatio();
        xloc = props.getXWindowLocation();
        yloc = props.getYWindowLocation();
    }
   
    /**
     * Sets the HSI props passed as an argument to this class properties 
     */
    //Why is this getter setting?
    public void getProps(HSIProps props) {
        props.setNumMassIdx(numMassIdx);
        props.setDenMassIdx(denMassIdx);
        props.setMinNum(minNum);
        props.setMinDen(minDen);
        props.setMaxRatio(maxRatio);
        props.setMinRatio(minRatio);
        props.setMaxRGB(maxRGB);
        props.setMinRGB(minRGB);
        props.setTransparency(transparency);
        props.setLabelMethod(label);
        props.setTransform(transform);
        props.setReferenceRatio(referenceRatio);
        props.setBackgroundRatio(backgroundRatio);
        props.setXWindowLocation(xloc);
        props.setYWindowLocation(yloc);
    }
    
    public HSIProps clone() {
        HSIProps props = new HSIProps();
        getProps(props);
        return props ;
    }
    
   // Two props objects are equal if numerator and denominator are the same.
   public boolean equals(HSIProps rp) {
      if (rp.getNumMassIdx() == numMassIdx && rp.getDenMassIdx() == denMassIdx)
         return true;
      else
         return false;
   }
}

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
    
    /** Creates a new instance of HSIProps */
    public HSIProps() {
        numMass = 0 ;
        denMass = 1 ;
        maxRatio = 1.0 ;
        minRatio = 0.01 ;
        minNum = 3 ;
        minDen = 3 ;
        maxRGB = 255 ;
        minRGB = 0 ;
        transparency = 0 ;
        label = 0 ;
        ratioScaleFactor = 10000;
        isSum = false;
        isWindow = false;
        windowSize = 0;
        transform = false;
        referenceRatio = (float)37/(float)10000;
        backgroundRatio = (float)129/(float)10000;
        xloc = -1;
        yloc = -1;
    }
    public void setNumMass(int n) { numMass = n ; }
    public int getNumMass() { return numMass ; }
    public void setDenMass(int n) { denMass = n ; }
    public int getDenMass() { return denMass ; }
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
    public void setRatioScaleFactor(int s) { this.ratioScaleFactor = s; }
    public int getRatioScaleFactor() { return this.ratioScaleFactor; }
    public void setXWindowLocation(int x) { this.xloc = x; }
    public int getXWindowLocation() { return this.xloc; }
    public void setYWindowLocation(int y) { this.yloc = y; }
    public int getYWindowLocation() { return this.yloc; }    
    public void setIsWindow(boolean isWindow) {this.isWindow = isWindow; }
    public boolean getIsWindow() { return this.isWindow; }
    public void setWindowSize(int size) { this.windowSize = size; }
    public int getWindowSize() { return this.windowSize; }
    
    public void setIsSum(boolean isSum) { this.isSum = isSum; }
    public boolean getIsSum() { return this.isSum; }
    
    public void setTransform(boolean trans) { this.transform = trans; }
    public boolean getTransform() { return this.transform; }

    public void setReferenceRatio(float ref) { this.referenceRatio = ref; }
    public float getReferenceRatio() { return this.referenceRatio; }

    public void setBackgroundRatio(float bg) { this.backgroundRatio = bg; }
    public float getBackgroundRatio() { return this.backgroundRatio; }
    /**
     * Set this class' properties from another class
     */
    public void setProps(HSIProps props) {
        numMass = props.getNumMass();
        denMass = props.getDenMass();
        minNum = props.getMinNum();
        minDen = props.getMinDen();
        maxRatio = props.getMaxRatio();
        minRatio = props.getMinRatio();
        maxRGB = props.getMaxRGB();
        minRGB = props.getMinRGB();
        transparency = props.getTransparency();
        label = props.getLabelMethod();
        isSum = props.getIsSum();
        isWindow = props.getIsWindow();
        windowSize = props.getWindowSize();
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
        props.setNumMass(numMass);
        props.setDenMass(denMass);
        props.setMinNum(minNum);
        props.setMinDen(minDen);
        props.setMaxRatio(maxRatio);
        props.setMinRatio(minRatio);
        props.setMaxRGB(maxRGB);
        props.setMinRGB(minRGB);
        props.setTransparency(transparency);
        props.setLabelMethod(label);
        props.setIsSum(isSum);
        props.setIsWindow(isWindow);
        props.setWindowSize(windowSize);
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
    
    public boolean equal(HSIProps props) {
        return
                props.getDenMass() == denMass
            &&  props.getNumMass() == numMass
            &&  props.getMinDen() == minDen
            &&  props.getMinNum() == minNum
            &&  props.getMaxRatio() == maxRatio
            &&  props.getMinRatio() == minRatio
            &&  props.getMaxRGB() == maxRGB
            &&  props.getMinRGB() == minRGB
            &&  props.getTransparency() == transparency
            &&  props.getLabelMethod() == label
            &&  props.getIsSum() == isSum
            &&  props.getTransform() == transform
            &&  props.getReferenceRatio() == referenceRatio
            &&  props.getBackgroundRatio() == backgroundRatio;
    }
    
    private int numMass ;
    private int denMass ;
    private double maxRatio ;
    private double minRatio ;
    private int minNum ;
    private int minDen ;
    private int maxRGB ;
    private int minRGB ;
    private int transparency ;
    private int label ;
    private int ratioScaleFactor;
    private int windowSize;
    private boolean isSum;
    private boolean isWindow;
    private boolean transform;
    private float referenceRatio;
    private float backgroundRatio;
    private int xloc, yloc;
}

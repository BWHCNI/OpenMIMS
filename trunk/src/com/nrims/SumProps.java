package com.nrims;

public class SumProps implements java.io.Serializable {
      
    //-----------------------------
    static final long serialVersionUID = 2L;
    //-----------------------------
    // DO NOT! Change variable order/type
    // DO NOT! Delete variables
    static final int MASS_IMAGE = 0;
    static final int RATIO_IMAGE = 1;
      
   private int parentMassIdx, numMassIdx, denMassIdx;
   private double parentMassValue, numMassValue, denMassValue;
   private int sumType;
   private int xloc, yloc;
   private String dataFileName;
   private double ratioScaleFactor;
   //--------------------------------
   //End of v2
    
    // Use for Mass Images.
    public SumProps(int massIndex) {
       this.parentMassIdx = massIndex;
       this.sumType = MASS_IMAGE;

       // Default values.
       xloc = -1;
       yloc = -1;
       numMassValue = -1.0;
       denMassValue = -1.0;
       ratioScaleFactor = -1.0;
    }        
    
    // Use for Ratio Images.
    public SumProps(int numIndex, int denIndex) {
       this.numMassIdx = numIndex;
       this.denMassIdx = denIndex;
       this.sumType = RATIO_IMAGE;

       // Default values.
       xloc = -1;
       yloc = -1;
       numMassValue = -1.0;
       denMassValue = -1.0;
       ratioScaleFactor = -1.0;
    }
          
    // The mass index of the Numerator.
    public void setNumMassIdx(int i) { numMassIdx = i; }
    public int getNumMassIdx() { return numMassIdx; }
    
    // The mass of the Numerator.
    public void setNumMassValue(double d) { numMassValue = d; }
    public double getNumMassValue() { return numMassValue; }

    // The mass index of the Denominator.
    public void setDenMassIdx(int i) { denMassIdx = i; }
    public int getDenMassIdx() { return denMassIdx; }
    
    // The mass of the Denominator.
    public void setDenMassValue(double d) { denMassValue = d; }
    public double getDenMassValue() { return denMassValue; }

    // The ratio scale factor
    public void setRatioScaleFactor(double rsf) { ratioScaleFactor = rsf; }
    public double getRatioScaleFactor() { return ratioScaleFactor; }

    // The mass number of the Parent Mass.
    public void setParentMassIdx(int i) { parentMassIdx = i; }
    public int getParentMassIdx() { return parentMassIdx; }

    // The mass of the Parent Mass.
    public void setParentMassValue(double d) { parentMassValue = d; }
    public double getParentMassValue() { return parentMassValue; }

    // The window location
    public void setXWindowLocation(int x) { this.xloc = x; }
    public int getXWindowLocation() { return this.xloc; }
    public void setYWindowLocation(int y) { this.yloc = y; }
    public int getYWindowLocation() { return this.yloc; }

    // The original data file.
    public void setDataFileName(String fileName) { dataFileName = fileName;}
    public String getDataFileName() { return dataFileName; }

    // Type of sum image (either Mass or Ratio).
    public int getSumType() { return sumType; }    
}

package com.nrims;

public class SumProps implements java.io.Serializable {
      
   static final int MASS_IMAGE = 0;
   static final int RATIO_IMAGE = 1;
      
   private int parentMassIdx, numMassIdx, denMassIdx;
   private double parentMass, numMass, denMass;
   private double ratioScaleFactor = -1;
   private int sumType;
   private int xloc, yloc;
   private String dataFileName;
    
    // Use for Mass Images.
    public SumProps(int massIndex) {
       this.parentMassIdx = massIndex;
       this.sumType = MASS_IMAGE;
    }        
    
    // Use for Ratio Images.
    public SumProps(int numIndex, int denIndex) {
       this.numMassIdx = numIndex;
       this.denMassIdx = denIndex;
       this.sumType = RATIO_IMAGE;
    }
          
    // The mass index of the Numerator.
    public void setNumMassIdx(int i) { numMassIdx = i; }
    public int getNumMassIdx() { return numMassIdx; }
    
    // The mass of the Numerator.
    public void setNumMass(double d) { numMass = d; }
    public double getNumMass() { return numMass; }

    // The mass index of the Denominator.
    public void setDenMassIdx(int i) { denMassIdx = i; }
    public int getDenMassIdx() { return denMassIdx; }
    
    // The mass of the Denominator.
    public void setDenMass(double d) { denMass = d; }
    public double getDenMass() { return denMass; }

    // The ratio scale factor
    public void setRatioScaleFactor(double rsf) { ratioScaleFactor = rsf; }
    public double getRatioScaleFactor() { return ratioScaleFactor; }

    // The mass number of the Parent Mass.
    public void setParentMassIdx(int i) { parentMassIdx = i; }
    public int getParentMassIdx() { return parentMassIdx; }

    // The mass of the Parent Mass.
    public void setParentMass(double d) { parentMass = d; }
    public double getParentMass() { return parentMass; }

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

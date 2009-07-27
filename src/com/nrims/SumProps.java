package com.nrims;

import java.io.File;

public class SumProps implements java.io.Serializable {
      
   static final int MASS_IMAGE = 0;
   static final int RATIO_IMAGE = 1;
      
   double numMass, denMass, parentMass;
   private int sumType;
   private int xloc = -1;
   private int yloc = -1;
   private String dataFileName = null;
    
    // Use for Mass Images.
    public SumProps(double mass) {
       this.parentMass = mass;
       this.sumType = MASS_IMAGE;
    }        
    
    // Use for Ratio Images.
    public SumProps(double numerator, double denominator) {
       this.numMass = numerator;
       this.denMass = denominator;
       this.sumType = RATIO_IMAGE;
    }
    
    // Window parameters.
    public void setXWindowLocation(int x) { xloc = x; }
    public int getXWindowLocation() { return xloc; }    
    public void setYWindowLocation(int y) { yloc = y; }
    public int getYWindowLocation() { return yloc; }
        
    // The mass number of the Numerator in the Ratio Image used to generate the sum image.
    public void setNumMass(double d) { numMass = d; }
    public double getNumMass() { return numMass; }
    
    // The mass number of the Denominator in the Ratio Image used to generate the sum image.
    public void setDenMass(double d) { denMass = d; }
    public double getDenMass() { return denMass; }
    
    // The mass number of the Mass Image used to generate the the sum image.
    public void setParentMass(double d) { parentMass = d; }
    public double getParentMass() { return parentMass; }

    // Set the original data file used to generate image.
    public void setDataFileName(String fileName) { dataFileName = fileName; }
    public String getDataFileName() { return dataFileName; }
    
    // The type of image this sum image came from (either Mass or Ratio)
    public int getSumType() { return sumType; }    
}

package com.nrims;

// Ratio properties.
public class RatioProps implements java.io.Serializable {
    //-----------------------------
    static final long serialVersionUID = 2L;
    //-----------------------------
    // DO NOT! Change variable order/type
    // DO NOT! Delete variables
    private int numMassIdx,  denMassIdx;
    private double numMassValue,  denMassValue;
    private int xloc,  yloc;
    private String dataFileName;
    private double ratioScaleFactor;
    //------------------------------
    //End of v2
    private double minLUT;
    private double maxLUT;


   // Create an empty ratio props object.
   public RatioProps(){}

   // Create a ratio props object with given numerator and denominator mass indexes.
   public RatioProps(int numerator, int denominator) {
       numMassIdx = numerator ;
       denMassIdx = denominator ;

       // Default values.
       xloc = -1;
       yloc = -1;
       numMassValue = -1.0;
       denMassValue = -1.0;
       ratioScaleFactor = -1.0;
       //-----------
       minLUT = 0.0;
       maxLUT = 1.0;
   }

   // Two props objects are equal if numerator and denominator are the same.
   public boolean equals(RatioProps rp) {
      if (rp.getNumMassIdx() == numMassIdx && rp.getDenMassIdx() == denMassIdx)
         return true;
      else
         return false;
   }

   // Getters and Setters.
   public int getNumMassIdx() { return numMassIdx ; }
   public int getDenMassIdx() { return denMassIdx ; }
   public void setNumMassValue(double d) { this.numMassValue = d; }
   public double getNumMassValue() { return this.numMassValue; }
   public void setDenMassValue(double d) { this.denMassValue = d; }
   public double getDenMassValue() { return this.denMassValue; }
   public void setXWindowLocation(int x) { this.xloc = x; }
   public int getXWindowLocation() { return this.xloc; }
   public void setYWindowLocation(int y) { this.yloc = y; }
   public int getYWindowLocation() { return this.yloc; }
   public void setDataFileName(String fileName) { dataFileName = fileName;}
   public String getDataFileName() { return dataFileName; }
   public void setRatioScaleFactor(double s) { this.ratioScaleFactor = s; }
   public double getRatioScaleFactor() { return this.ratioScaleFactor; }

   public void setMinLUT(double min) { this.minLUT = min; }
   public double getMinLUT() { return this.minLUT; }
   public void setMaxLUT(double max) { this.maxLUT = max; }
   public double getMaxLUT() { return this.maxLUT; }
}





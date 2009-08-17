package com.nrims;

// Ratio properties.
public class RatioProps implements java.io.Serializable {
   private int numMassIdx, denMassIdx;
   private int xloc, yloc;
   private String dataFileName;
   private double ratioScaleFactor = -1;

   // Create an empty ratio props object.
   public RatioProps(){}

   // Create a ratio props object with given numerator and denominator mass indexes.
   public RatioProps(int numerator, int denominator) {
       numMassIdx = numerator ;
       denMassIdx = denominator ;
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

   public void setXWindowLocation(int x) { this.xloc = x; }
   public int getXWindowLocation() { return this.xloc; }
   public void setYWindowLocation(int y) { this.yloc = y; }
   public int getYWindowLocation() { return this.yloc; }
   public void setDataFileName(String fileName) { dataFileName = fileName;}
   public String getDataFileName() { return dataFileName; }
   public void setRatioScaleFactor(double s) { this.ratioScaleFactor = s; }
   public double getRatioScaleFactor() { return this.ratioScaleFactor; }
}





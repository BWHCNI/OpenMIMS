package com.nrims;

// Ratio properties.
public class RatioProps implements java.io.Serializable {
   private double numMass, denMass;
   private int numMassIdx, denMassIdx;
   private int xloc, yloc;
   private String dataFileName;
   private int ratioScaleFactor;

   // Create an empty ratio props object.
   public RatioProps(){}

   // Create a ratio props object with given numerator and denominator mass indexes.
   public RatioProps(int numerator, int denominator) {
       numMassIdx = numerator ;
       denMassIdx = denominator ;
   }

   // Getters and Setters.
   public void setNumMass(double d) { numMass = d ; }
   public double getNumMass() { return numMass ; }
   public void setDenMass(double d) { denMass = d ; }
   public double getDenMass() { return denMass ; }
   public void setNumMassIdx(int i) { numMassIdx = i ; }
   public int getNumMassIdx() { return numMassIdx ; }
   public void setDenMassIdx(int i) { denMassIdx = i ; }
   public int getDenMassIdx() { return denMassIdx ; }
   public void setXWindowLocation(int x) { this.xloc = x; }
   public int getXWindowLocation() { return this.xloc; }
   public void setYWindowLocation(int y) { this.yloc = y; }
   public int getYWindowLocation() { return this.yloc; }
   public void setDataFileName(String fileName) { dataFileName = fileName;}
   public String getDataFileName() { return dataFileName; }
   public void setRatioScaleFactor(int s) { this.ratioScaleFactor = s; }
   public int getRatioScaleFactor() { return this.ratioScaleFactor; }
}





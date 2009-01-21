package com.nrims;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;

/**
 * @author zkaufman
 */
public class MimsRoi extends Roi {
   int plane;
   int imageID;
   
   public MimsRoi(Roi roi){      
      super(roi.getBoundingRect().x, roi.getBoundingRect().y, 
              roi.getBoundingRect().width, roi.getBoundingRect().height);
      ImagePlus imp = WindowManager.getCurrentImage();                                      
      if (imp != null) {
          this.plane = imp.getCurrentSlice();
          this.imageID = imp.getID();
      }
      
   }
   
   public MimsRoi(int startX, int startY, int width, int height){
      super(startX, startY, width, height);
      ImagePlus imp = WindowManager.getCurrentImage();       
      if (imp != null) {
          this.plane = imp.getCurrentSlice();
          this.imageID = imp.getID();
      }
   }
   
   // This class extends the ImageJ Roi class because we would like to keep track 
   // of the plane/slice number and the imageID for which the ROI was drawn. 
   public MimsRoi(int startX, int startY, int width, int height, int plane, int imageID){
      super(startX, startY, width, height);      
      this.plane = plane;
      this.imageID = imageID;      
   }
   
   // The plane number is the plane/slice which the ROI was drawn.
   public int getPlaneNumber(){
      return this.plane;
   }
   
   public void setPlaneNumber(int plane){
      this.plane = plane;
   }
   
   // The imageID for which the ROI was drawn.
   public int getImageID(){
      return this.imageID;
   }
   
   public void setImageID(int imageID){
      this.imageID = imageID;
   }
  

}

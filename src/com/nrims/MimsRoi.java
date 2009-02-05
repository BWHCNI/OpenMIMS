package com.nrims;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Roi;

/**
 * @author zkaufman
 */
public class MimsRoi {
   
   Roi roi;
   int plane;
   int imageID;  
   String windowtitle;
   ImageWindow imagewindow;
   
   
   // This class extends the ImageJ Roi class. Allows
   // us to keep track of the plane/slice number 
   // and the imageID for which the ROI was drawn. 
   public MimsRoi (Roi roi, ImagePlus imp){      
      this.roi = roi;
      if (imp != null) {          
          this.plane = imp.getCurrentSlice();
          this.imageID = imp.getID();
      }      
   }  
   
   public MimsRoi (Roi roi, int plane, int imageID){      
      this.roi = roi;                                               
      this.plane = plane;
      this.imageID = imageID;     
   }  
   
   // Checks the plane and imageID for the Roi and compares it to
   // the current plane and imageID to see if it should be displayed.
   // Also must check user parameters ui.getSyncROIsAcrossPlanes 
   // and ui.getSyncROIsAcrossMasses.
   public boolean show(UI ui, int currentPlane, int currentImageID) {
      boolean planeMatch = (this.plane == currentPlane);
      boolean imageIDMatch = (this.imageID == currentImageID);      
      if ((ui.getSyncROIsAcrossPlanes() && ui.getSyncROIsAcrossMasses()) || (planeMatch && imageIDMatch) ||
                    (ui.getSyncROIsAcrossPlanes() && imageIDMatch) || (ui.getSyncROIsAcrossMasses() && planeMatch)) {
         return true;
      }
      return false;
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
   
   // The roi. 
   public Roi getRoi(){
      return this.roi;
   }
   
   private void setRoi(Roi roi){
      this.roi = roi;
   }
   
   // The window the roi was created in.
   public ImageWindow getImageWindow(){
      return this.imagewindow;
   }
   
   public void setImageWindow(ImageWindow imagewin){
      this.imagewindow = imagewin;
   }
}

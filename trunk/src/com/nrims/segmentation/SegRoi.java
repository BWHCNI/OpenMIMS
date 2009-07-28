/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims.segmentation;

import ij.gui.Roi;
import ij.gui.ShapeRoi;

/**
 *
 * @author sreckow
 */
   public class SegRoi{               
        private Roi roi;
        private ShapeRoi shapeRoi;    
        private String className;
        private int ID;
     
        public SegRoi(Roi roi, String className, int ID) {            
            this.className = className;
            this.ID = ID;
            setRoi(roi,null);
        }                

        public String getClassName() {
            return className;
        }

        public int getID(){
            return ID;
        }        

        public Roi getRoi() {
            return roi;
        }

        public void setRoi(Roi roi){
            setRoi(roi, null);            
        }

        public void setRoi(Roi roi, ShapeRoi shape){
            this.roi = roi;
            this.shapeRoi = shape;
            roi.setName(String.valueOf(ID));
        }

        public ShapeRoi getShapeRoi(){
            if(shapeRoi==null) shapeRoi = new ShapeRoi(roi);
            return shapeRoi;
        }
    }

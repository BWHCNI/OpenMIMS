package com.nrims;

import com.nrims.data.Opener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class MimsAction implements Cloneable {
    
    private double[][] xyTranslationList;
    private int[] sliceNumber;
    private int[] droppedList;
    private int[] imageIndex;
    private String[] imageList;

    public MimsAction(UI ui, Opener im) {
        resetAction(ui, im);
    }

    public void resetAction(UI ui, Opener im) {                    
                                     
        // Size of the stack.
        int size = ui.getMassImage(0).getNSlices();
        
        // Set size of member variables.
        xyTranslationList = new double[size][2];
        sliceNumber = new int[size];
        droppedList = new int[size];
        imageIndex  = new int[size];
        imageList = new String[size];
        
        // Initialize member variables.
        for (int i = 0; i < size; i++) {
           sliceNumber[i] = i+1;
           xyTranslationList[i][0] = 0.0;
           xyTranslationList[i][1] = 0.0;
           droppedList[i] = 0;
           imageIndex[i] = i;            
           imageList[i] = im.getImageFile().getName();
        }
    }

    public void addPlanes(boolean pre, int n, Opener op) {
        int origSize = sliceNumber.length;
        int startIndex;
        if (pre) {
            startIndex = 0;
        } else {
            startIndex = origSize;
        }       

        // Add planes to the action-ArrayList.
        int openerPlaneNum = 0;
        for (int i = startIndex; i < n + startIndex; i++) {           
           sliceNumber[i] = i+1;
           xyTranslationList[i][0] = 0.0;
           xyTranslationList[i][1] = 0.0;
           droppedList[i] = 0;
           imageIndex[i] = openerPlaneNum;            
           imageList[i] = op.getImageFile().getName();
           openerPlaneNum++;
        }

        // renumber original planes, if new ones were prepended
        if (pre) {
            for (int i = origSize; i < origSize + n; i++) {
               sliceNumber[i] = i+1;                
            }
        }        
    }

    public String getActionRow(int plane) {
        int idx = plane-1;
        return "p:" + sliceNumber[idx] + 
               "\t" + roundTwoDecimals(xyTranslationList[idx][0]) + 
               "\t" + roundTwoDecimals(xyTranslationList[idx][1]) + 
               "\t" + droppedList[idx] + 
               "\t" + imageIndex[idx] + 
               "\t" + imageList[idx];
    }

    public int getSize() {
        return this.imageList.length;
    }

    public void dropPlane(int displayIndex) {
        int index = trueIndex(displayIndex);
        droppedList[index - 1] = 1;
    }

    public void undropPlane(int trueIndex) {
        droppedList[trueIndex - 1] = 0;
    }

    public void setShiftX(int plane, double offset) {
        int tplane = this.trueIndex(plane);
        xyTranslationList[tplane-1][0] = offset;                    
    }

    public void setShiftY(int plane, double offset) {
        int tplane = this.trueIndex(plane);
        xyTranslationList[tplane-1][1] = offset;              
    }

    public double getXShift(int plane) {
        int tplane = this.trueIndex(plane);
        return xyTranslationList[tplane-1][0];
    }

    public double getYShift(int plane) {
        int tplane = this.trueIndex(plane);
        return xyTranslationList[tplane-1][1];
    }

    public int trueIndex(int dispIndex) {
        int index = 0;
        int zeros = 0;
        while (zeros < dispIndex) {
            if (droppedList[index] == 0) {
                zeros++;
            }
            index++;
        }
        return index;
    }

    public int displayIndex(int tIndex) {
        int zeros = 0;
        int i = 0;
        while (i < tIndex) {
            if (droppedList[i] == 0) {
                zeros++;
            }
            i++;
        }
        if (droppedList[tIndex - 1] == 0) {
            return zeros;
        } else {
            return zeros + 1;
        }
    }

    public int isDropped(int tIndex) {
        return (droppedList[tIndex - 1]);
    }

    public String[] getImageList(){
       return imageList;
    }
    
    public int getOpenerIndex(int plane) {       
       return imageIndex[plane];       
    }
    
    public String getOpenerName(int plane) {
       return imageList[plane];
    }
    
    @Override
    public Object clone() {
        // only a shallow copy is returned!!
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public boolean writeAction(String filename) {
        BufferedWriter bw = null;       
        
        if (sliceNumber.length != imageList.length){
           System.out.println("internal decrepancy between image list and action list.");
           return false;
        }
        
        try {
            bw = new BufferedWriter(new FileWriter(filename));

            // write image state
            for (int i = 1; i <= sliceNumber.length; i++) {
                bw.append(getActionRow(i));
                bw.newLine();
            }
            bw.close();
            return true;
        } catch (IOException e) {
            System.out.println(e.getStackTrace().toString());
            return false;
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    // Ignore.
                    return false;
                }
            }
        }
    }

   void setSliceImage(int plane, String file) {
        int tplane = trueIndex(plane);
        if (!imageList[tplane - 1].equals(file)) {
            imageList[tplane - 1] = file;
        }   
   }
   
   double roundTwoDecimals(double d) {
        	DecimalFormat twoDForm = new DecimalFormat("#.##");
		return Double.valueOf(twoDForm.format(d));
   }
}

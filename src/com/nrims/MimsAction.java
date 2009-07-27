package com.nrims;

import com.nrims.data.Opener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MimsAction implements Cloneable {
    
    private ArrayList<double[]> xyTranslationList;
    private ArrayList<Integer> droppedList;
    private ArrayList<Integer> imageIndex;
    private ArrayList<String> imageList;
    double zeros[] = {0.0, 0.0};

    public MimsAction(UI ui, Opener im) {
        resetAction(ui, im);
    }

    public void resetAction(UI ui, Opener im) {                    
                                     
        // Size of the stack.
        //int size = ui.getMassImage(0).getNSlices();
        int size = im.getNImages();

        // Set size of member variables.
        xyTranslationList = new ArrayList<double[]>();
        droppedList = new ArrayList<Integer>();
        imageIndex = new ArrayList<Integer>();
        imageList = new ArrayList<String>();
        
        // Initialize member variables.
        for (int i = 0; i < size; i++) {         
           xyTranslationList.add(zeros);           
           droppedList.add(0);
           imageIndex.add(i);            
           imageList.add(im.getImageFile().getName());
        }
    }

    public void addPlanes(boolean pre, int n, Opener op) {
        int origSize = imageList.size();
        int startIndex;
        if (pre) {
            startIndex = 0;
        } else {
            startIndex = origSize;
        }       

        // Add planes to the action-ArrayList.
        int openerPlaneNum = 0;
        for (int i = startIndex; i < n + startIndex; i++) {           
           xyTranslationList.add(i, zeros);           
           droppedList.add(i, 0);
           imageIndex.add(i, openerPlaneNum);            
           imageList.add(i, op.getImageFile().getName());
           openerPlaneNum++;
        }
       
    }

    public String getActionRow(int plane) {
        int idx = plane-1;
        return "p:" + plane +
               "\t" + roundTwoDecimals((Double)(xyTranslationList.get(idx)[0])) + 
               "\t" + roundTwoDecimals((Double)(xyTranslationList.get(idx)[1])) +
               "\t" + droppedList.get(idx) + 
               "\t" + imageIndex.get(idx) + 
               "\t" + imageList.get(idx);
    }

    public int getSize() {
        return this.imageList.size();
    }

    public void dropPlane(int displayIndex) {
        int index = trueIndex(displayIndex);
        droppedList.set(index - 1, 1);
    }

    public void undropPlane(int trueIndex) {
        droppedList.set(trueIndex - 1, 0);
    }

    public void setShiftX(int plane, double offset) {
        int tplane = this.trueIndex(plane);
        double y = getXShift(plane);
        double xy[] = {offset, y};
        xyTranslationList.set(tplane-1, xy);                    
    }

    public void setShiftY(int plane, double offset) {
        int tplane = this.trueIndex(plane);
        double x = getXShift(plane);
        double xy[] = {x, offset};
        xyTranslationList.set(tplane-1, xy);    }

    public double getXShift(int plane) {
        int tplane = this.trueIndex(plane);
        return (Double)(xyTranslationList.get(tplane-1)[0]);
    }

    public double getYShift(int plane) {
        int tplane = this.trueIndex(plane);
        return (Double)(xyTranslationList.get(tplane-1)[1]);
    }

    public int trueIndex(int dispIndex) {
        int index = 0;
        int zeros = 0;
        while (zeros < dispIndex) {
            if (droppedList.get(index) == 0) {
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
            if (droppedList.get(i) == 0) {
                zeros++;
            }
            i++;
        }
        if (droppedList.get(tIndex - 1) == 0) {
            return zeros;
        } else {
            return zeros + 1;
        }
    }

    public boolean isDropped(int tIndex) {
        if (droppedList.get(tIndex-1) == 1)
            return true;
        else
            return false;
    }

    public String[] getImageList(){
       String imageListString[] = new String[imageList.size()];
       for (int i=0; i<imageListString.length; i++)
          imageListString[i] = imageList.get(i);
       return imageListString;
    }
    
    public int getOpenerIndex(int plane) {       
       return imageIndex.get(plane);       
    }
    
    public String getOpenerName(int plane) {
       return imageList.get(plane);
    }
    
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public File writeAction(File file) {
       
        // initialize variable.
        BufferedWriter bw = null;             
        
        try {
            bw = new BufferedWriter(new FileWriter(file));

            // write image state
            for (int i = 1; i <= imageList.size(); i++) {
                bw.append(getActionRow(i));
                bw.newLine();
            }
            bw.close();
            return file.getAbsoluteFile();
        } catch (IOException e) {
            System.out.println(e.getStackTrace().toString());
            return null;
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    return null;
                }
            }
        }
    }

   void setSliceImage(int plane, String file) {
        int tplane = trueIndex(plane);
        if (!imageList.get(tplane - 1).equals(file)) {
            imageList.set(tplane - 1, file);
        }   
   }
   
   double roundTwoDecimals(double d) {
        	DecimalFormat twoDForm = new DecimalFormat("#.##");
		return Double.valueOf(twoDForm.format(d));
   }
}

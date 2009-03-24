package com.nrims;

import com.nrims.data.Opener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MimsAction implements Cloneable {
    
    // actionList and imageList have one entry per slice in the stack.
    private ArrayList<int[]> actionList;
    private ArrayList<String> imageList;

    public MimsAction(UI ui, Opener im) {
        resetAction(ui, im);
    }

    public void resetAction(UI ui, Opener im) {                    

        actionList = new ArrayList<int[]>();
        imageList = new ArrayList<String>();
        int tempsize = ui.getMassImage(0).getNSlices();

        // index [0] = slice number
        // index [1] = X offset
        // index [2] = Y offset
        // index [3] = 0 or 1 (kept or dropped)
        // index [4] = image index
        for (int i = 0; i < tempsize; i++) {
            int[] temparray = {i + 1, 0, 0, 0, i};
            actionList.add(temparray);
            imageList.add(im.getImageFile().getName());
        }
    }

    public void addPlanes(boolean pre, int n, Opener op) {
        int origSize = actionList.size();
        int startIndex;
        if (pre) {
            startIndex = 0;
        } else {
            startIndex = origSize;
        }       

        // Add planes to the action-ArrayList.
        int openerPlaneNum = 0;
        for (int i = startIndex; i < n + startIndex; i++) {
            int[] temparray = {i + 1, 0, 0, 0, openerPlaneNum};            
            actionList.add(i, temparray);
            imageList.add(i, op.getImageFile().getName());
            openerPlaneNum++;
        }

        // renumber original planes, if new ones were prepended
        if (pre) {
            for (int i = origSize; i < origSize + n; i++) {
                actionList.get(i)[0] = i + 1;
            }
        }
        System.out.println("act size=" + actionList.size());
    }

    public String getActionRow(int plane) {
        int[] row = actionList.get(plane - 1);
        String imageName = imageList.get(plane - 1);
        return "p:" + row[0] + "\t" + row[1] + "\t" + row[2] + "\t" + row[3] + "\t" + row[4] + "\t" + imageName;
    }

    public int getSize() {
        return this.actionList.size();
    }

    public void dropPlane(int displayIndex) {
        int index = trueIndex(displayIndex);
        actionList.get(index - 1)[3] = 1;
    }

    public void undropPlane(int trueIndex) {
        actionList.get(trueIndex - 1)[3] = 0;
    }

    public void setShiftX(int plane, int offset) {
        int tplane = this.trueIndex(plane);
        if (actionList.get(tplane - 1)[1] != offset) {
            actionList.get(tplane - 1)[1] = offset;
        }
    }

    public void setShiftY(int plane, int offset) {
        int tplane = this.trueIndex(plane);
        if (actionList.get(tplane - 1)[2] != offset) {
            actionList.get(tplane - 1)[2] = offset;
        }
    }

    public void nudgeX(int plane, int offset) {
        int tplane = this.trueIndex(plane);
        actionList.get(tplane - 1)[1] = actionList.get(tplane - 1)[1] + offset;
    }

    public void nudgeY(int plane, int offset) {
        int tplane = this.trueIndex(plane);
        actionList.get(tplane - 1)[2] = actionList.get(tplane - 1)[2] + offset;
    }

    public int getXShift(int plane) {
        int tplane = this.trueIndex(plane);
        return actionList.get(tplane - 1)[1];
    }

    public int getYShift(int plane) {
        int tplane = this.trueIndex(plane);
        return actionList.get(tplane - 1)[2];
    }

    public int trueIndex(int dispIndex) {
        int index = 0;
        int zeros = 0;
        while (zeros < dispIndex) {
            if (actionList.get(index)[3] == 0) {
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
            if (actionList.get(i)[3] == 0) {
                zeros++;
            }
            i++;
        }
        if (actionList.get(tIndex - 1)[3] == 0) {
            return zeros;
        } else {
            return zeros + 1;
        }
    }

    public int isDropped(int tIndex) {
        return (actionList.get(tIndex - 1)[3]);
    }

    public ArrayList<String> getImageList(){
       return imageList;
    }
    
    public void addImage(boolean pre, Opener im) {
       
       String imageName = im.getName();       
       
       // Keep plane dependant list up to date.
        if (pre) {
           imageList.add(0, imageName);
        } else {
           imageList.add(imageName);
        }
    }
    
    public int getOpenerIndex(int plane) {       
       return actionList.get(plane)[4];       
    }
    
    public String getOpenerName(int plane) {
       return imageList.get(plane);
    }
    
    @Override
    public Object clone() {
        // only a shallow copy is retrurned!!
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public boolean writeAction(String filename) {
        BufferedWriter bw = null;
        
        int imageSize = imageList.size();
        int actionSize = actionList.size();
        
        if (imageSize != actionSize){
           System.out.println("internal decrepancy between image list and action list.");
           return false;
        }
        
        try {
            bw = new BufferedWriter(new FileWriter(filename));

            // write image state
            for (int i = 1; i <= actionSize; i++) {
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
        if (!imageList.get(tplane - 1).equals(file)) {
            imageList.set(tplane - 1, file);
        }   
   }
}

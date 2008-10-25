package com.nrims;

import com.nrims.data.Opener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author cpoczatek
 * @author <a href="mailto:rob.gonzalez@gmail.com">Rob Gonzalez</a>
 */
// TODO why is this Coneable?
public class MimsAction implements Cloneable {

    private ArrayList<Opener> images;
    private java.util.ArrayList<int[]> actionList;

    public MimsAction(UI ui, Opener im) {
        resetAction(ui, im);
    }

    public void resetAction(UI ui, Opener im) {
        this.images = new ArrayList<Opener>();
        images.add(im);

        this.actionList = new ArrayList<int[]>();
        int tempsize = ui.getMassImage(0).getNSlices();

        for (int i = 0; i < tempsize; i++) {
            int[] temparray = {i + 1, 0, 0, 0};
            actionList.add(temparray);
        }
    }

    public void printAction() {
        int n = this.actionList.size();
        int[] row = new int[4];
        for (int i = 0; i < n; i++) {
            row = actionList.get(i);
            System.out.println("p:" + row[0] + " " + row[1] + " " + row[2] + " " + row[3]);
        }

    }

    public void addPlanes(boolean pre, int n) {
        int origSize = actionList.size();
        int startIndex;
        if (pre) {
            startIndex = 0;
        } else {
            startIndex = origSize;
        }

        // add planes to the action-ArrayList
        for (int i = startIndex; i < n + startIndex; i++) {
            int[] temparray = {i + 1, 0, 0, 0};
            System.out.println("i=" + (i + 1));
            actionList.add(i, temparray);
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
        return "p:" + row[0] + "\t" + row[1] + "\t" + row[2] + "\t" + row[3];
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

    public ArrayList<Opener> getImages() {
        return images;
    }

    public void addImage(boolean pre, Opener im) {
        if (pre) {
            images.add(0, im);
        } else {
            images.add(im);
        }
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

    public static boolean writeAction(MimsAction action, String filename) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(filename));

            // write header containing .im filenames
            for (Opener im : action.getImages()) {
                bw.append(im.getName());
                bw.newLine();
            }
            bw.newLine();

            // write image state
            for (int i = 1; i <= action.getSize(); i++) {
                bw.append(action.getActionRow(i));
                bw.newLine();
            }
            bw.close();
            return true;
        } catch (IOException e) {
            System.out.println(e.getStackTrace().toString());
            // TODO write to log file?
            // TODO should we honestly silently ignore this?
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
}

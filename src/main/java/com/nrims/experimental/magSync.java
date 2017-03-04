/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.experimental;

import com.nrims.MimsPlus;
import com.nrims.UI;
import ij.gui.ImageWindow;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author cpoczatek
 */

/*
 * 
 * This is mostly uneeded. Command IJ -> Analyze -> Tools -> Syncronize Windows 
 * performs a very similar function.
 * 
 * Consider deletion....
 * 
 * changes in MimsPlus are:
 * at end of mouse clicked method
 *          if (this.getCanvas() != null && Toolbar.getToolId() == Toolbar.MAGNIFIER) {
 * and at end of if{}
 * com.nrims.experimental.magSync.magSyncTo(this, e);
 */
public class magSync {

    public static void magSyncTo(MimsPlus img, MouseEvent e) {
        UI ui = img.getUI();
        double mag = img.getCanvas().getMagnification();
        ///?
        int x = img.getCanvas().offScreenX(e.getPoint().x);
        int y = img.getCanvas().offScreenY(e.getPoint().y);
        int w = img.getWindow().getWidth();
        int h = img.getWindow().getHeight();

        ArrayList imglist = new ArrayList();
        imglist.addAll(Arrays.asList(ui.getOpenMassImages()));
        imglist.addAll(Arrays.asList(ui.getOpenRatioImages()));
        imglist.addAll(Arrays.asList(ui.getOpenHSIImages()));
        imglist.addAll(Arrays.asList(ui.getOpenSumImages()));
        imglist.addAll(Arrays.asList(ui.getOpenCompositeImages()));

        for (int i = 0; i < imglist.size(); i++) {
            if (!img.equals((MimsPlus) imglist.get(i)) && ((MimsPlus) imglist.get(i)).getCanvas() != null) {
                ((MimsPlus) imglist.get(i)).getCanvas().setMagnification(mag);
                ((MimsPlus) imglist.get(i)).getCanvas().setSize(Math.min(w, 1000 - x), Math.min(h, 1000 - y));
                //bs 1000 value
                //((MimsPlus)imglist.get(i)).getWindow().setSize(Math.min(w, 1000-x), Math.min(h, 1000-y));
                ((MimsPlus) imglist.get(i)).getWindow().validate();

                ((MimsPlus) imglist.get(i)).updateAndDraw();
            }

        }
    }
}

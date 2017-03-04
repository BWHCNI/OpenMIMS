/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.experimental;

import ij.gui.Roi;
import ij.gui.ShapeRoi;
import com.nrims.*;

/**
 *
 * @author cpoczatek
 */
public class coreyEdge {

    private com.nrims.UI ui;

    public coreyEdge(com.nrims.UI ui) {
        this.ui = ui;
    }

    public void prl(String foo) {
        System.out.println(foo);
    }

    public void pr(String foo) {
        System.out.print(foo);
    }

    public void gentable() {
        //get ratio, should be only one...
        MimsPlus ratio = ui.getRatioImage(0);
        if (ratio == null) {
            return;
        }

        //get mass26
        MimsPlus m26 = ui.getMassImage(2);
        prl("m26: " + m26.getTitle());

        //get rm
        com.nrims.MimsRoiManager2 rm = ui.getRoiManager();

        //stack size, should be 256
        int zsize = ui.getMassImage(0).getStackSize();
        prl("zsize: " + zsize);

        //plane loop, small test
        for (int p = 1; p <= zsize; p++) {
            //set plane
            m26.setSlice(p);

            ShapeRoi edge = getEdgeROI(p);
            edge.setName("edge-" + p);
            rm.add(edge);
            ratio.setRoi(edge);

            //double[] pix = ratio.getRoiPixels();
            //rm.add(edge);
            //No!, have to do the bounding box loop + checks
            //to get x,y position info
            /*
            pr("{");
            for(int i=0; i< pix.length; i++) {
                if(pix[i]!=0) {
                    pr(pix[i]+"");
                    if(i!=(pix.length-1))
                        pr(", ");
                }
                
            }
            prl("}, ");
             */
            java.awt.Rectangle br = edge.getBounds();

            pr("{");
            String line = "";
            for (int x = br.x; x < (br.x + br.width); x++) {
                for (int y = br.y; y < (br.y + br.height); y++) {
                    if (edge.contains(x, y)) {
                        float pix = ratio.getProcessor().getPixelValue(x, y);
                        if (pix != 0) {
                            line = line + "{" + x + "," + y + "," + pix + "},";
                        }
                    }
                }
            }
            //drop trailing comma
            if (line.length() > 1) {
                line = line.substring(0, line.length() - 1);
            }
            pr(line);
            pr("}");
            prl("");

        }

    }

    public ShapeRoi getEdgeROI(int plane) {
        //get rm
        com.nrims.MimsRoiManager2 rm = ui.getRoiManager();
        //get 3 core rois, hard coded, so sue me
        Roi c1 = rm.getRoiByName("1-1");
        Roi c2 = rm.getRoiByName("2-1");
        Roi c3 = rm.getRoiByName("3-1");

        //need to get/set locations...damn
        Integer[] c1l = rm.getRoiLocation("1-1", plane);
        Integer[] c2l = rm.getRoiLocation("2-1", plane);
        Integer[] c3l = rm.getRoiLocation("3-1", plane);
        c1.setLocation(c1l[0], c1l[1]);
        c2.setLocation(c2l[0], c2l[1]);
        c3.setLocation(c3l[0], c3l[1]);

        //create shape roi of cores
        ShapeRoi core = new ShapeRoi(c1);
        core.or(new ShapeRoi(c2));
        core.or(new ShapeRoi(c3));

        //get 3 "edge" rois, hard coded, so sue me
        Roi e1 = rm.getRoiByName("1-e");
        Roi e2 = rm.getRoiByName("2-e");
        Roi e3 = rm.getRoiByName("3-e");

        //need to get/set locations...damn
        Integer[] e1l = rm.getRoiLocation("1-e", plane);
        Integer[] e2l = rm.getRoiLocation("2-e", plane);
        Integer[] e3l = rm.getRoiLocation("3-e", plane);
        e1.setLocation(e1l[0], e1l[1]);
        e2.setLocation(e2l[0], e2l[1]);
        e3.setLocation(e3l[0], e3l[1]);

        //create shaperoi of edges
        ShapeRoi edge = new ShapeRoi(e1);
        edge.or(new ShapeRoi(e2));
        edge.or(new ShapeRoi(e3));
        //subtract core
        edge.not(core);

        return edge;
    }

    public void test() {
        //get ratio, should be only one...
        MimsPlus ratio = ui.getRatioImage(0);
        if (ratio == null) {
            return;
        }

        //get mass26
        MimsPlus m26 = ui.getMassImage(2);
        prl("m26: " + m26.getTitle());

        //get rm
        com.nrims.MimsRoiManager2 rm = ui.getRoiManager();

        //stack size, should be 256
        int zsize = ui.getMassImage(0).getStackSize();
        prl("zsize: " + zsize);

        //get 3 core rois, hard coded, so sue me
        Roi c1 = rm.getRoiByName("1-1");
        Roi c2 = rm.getRoiByName("2-1");
        Roi c3 = rm.getRoiByName("3-1");

        //print areas
        m26.setRoi(c1);
        prl("c1 area: " + m26.getRoiPixels().length);
        m26.killRoi();
        m26.setRoi(c2);
        prl("c2 area: " + m26.getRoiPixels().length);
        m26.killRoi();
        m26.setRoi(c3);
        prl("c3 area: " + m26.getRoiPixels().length);
        m26.killRoi();

        //create shape roi of cores
        ShapeRoi core = new ShapeRoi(c1);
        core.or(new ShapeRoi(c2));
        core.or(new ShapeRoi(c3));

        //print area
        m26.setRoi(core);
        prl("core area: " + m26.getRoiPixels().length);
        m26.killRoi();

        //get 3 "edge" rois, hard coded, so sue me
        Roi e1 = rm.getRoiByName("1-e");
        Roi e2 = rm.getRoiByName("2-e");
        Roi e3 = rm.getRoiByName("3-e");

        ShapeRoi edge = new ShapeRoi(e1);
        edge.or(new ShapeRoi(e2));
        edge.or(new ShapeRoi(e3));

        //print area
        m26.setRoi(edge);
        prl("edge area: " + m26.getRoiPixels().length);
        m26.killRoi();

        //subtract core
        edge.not(core);

        //print area again
        m26.setRoi(edge);
        prl("edge area: " + m26.getRoiPixels().length);
        m26.killRoi();

        ratio.setRoi(edge);
        double[] pix = ratio.getRoiPixels();

        for (int i = 0; i < pix.length; i++) {
            pr(pix[i] + ", ");
        }
        prl("");

        //rm.add(edge);
    }

    public void test2() {
        //get ratio, should be only one...
        MimsPlus ratio = ui.getRatioImage(0);
        if (ratio == null) {
            return;
        }

        //get mass26
        MimsPlus m26 = ui.getMassImage(2);
        prl("m26: " + m26.getTitle());

        //get rm
        com.nrims.MimsRoiManager2 rm = ui.getRoiManager();

        //stack size, should be 256
        int zsize = ui.getMassImage(0).getStackSize();
        prl("zsize: " + zsize);

        //get 3 core rois, hard coded, so sue me
        Roi c1 = rm.getRoiByName("1-1");
        Roi c2 = rm.getRoiByName("2-1");
        Roi c3 = rm.getRoiByName("3-1");

        //plane loop, small test
        for (int p = 1; p <= zsize; p++) {
            m26.setSlice(p);

            Integer[] c1l = rm.getRoiLocation("1-1", p);
            Integer[] c2l = rm.getRoiLocation("2-1", p);
            Integer[] c3l = rm.getRoiLocation("3-1", p);
            c1.setLocation(c1l[0], c1l[1]);
            c2.setLocation(c2l[0], c2l[1]);
            c3.setLocation(c3l[0], c3l[1]);

            ratio.setRoi(c3);
            double[] pix = ratio.getRoiPixels();

            pr("{");
            String line = "";
            for (int i = 0; i < pix.length; i++) {
                if (pix[i] != 0) {
                    line = line + pix[i] + ",";
                }
            }

            //drop trailing comma
            if (line.length() > 1) {
                line = line.substring(0, line.length() - 1);
            }
            pr(line);
            pr("}");
            prl("");

        }
    }
}

/*
 * These ROI location methods were needed to set things up for the code in this class...
 * All from UI.
 * LOOK AT MimsRoiManager.java.bak in experimental!
 *
 * private void copyLocMenuItemActionPerformed(java.awt.event.ActionEvent evt) {

    Roi[] rois = roiManager.getSelectedROIs();

    System.out.println("-----------------------------");
    for(int i=0; i< rois.length; i++) {
        System.out.println("roi: "+rois[i].getName());
    }
    System.out.println("-----------------------------");

    String from = "";
    String to = "";
    ij.gui.GenericDialog gd = new ij.gui.GenericDialog("Copy Roi Locations");
    gd.addStringField("From Roi:", "", 20);
    gd.addStringField("To Roi:", "", 20);
    gd.showDialog();
    if (gd.wasCanceled()) {
        return;
    }
    from = gd.getNextString();
    to = gd.getNextString();

    //check for input
    if(from.equals("") || to.equals("")) {
        ij.IJ.error("Roi not entered");
        return;
    }

    Roi fromRoi = roiManager.getRoiByName(from);
    Roi toRoi = roiManager.getRoiByName(to);

    //check if rois found
    if( (fromRoi == null) || (toRoi == null)) {
        ij.IJ.error("Roi not found");
        return;
    }

    roiManager.copyROILocation(fromRoi, toRoi);

}

private void shiftLocMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
    // TODO add your handling code here:

    Roi roi =roiManager.getRoi();
    if(roi==null) return;
    String Xstring = "";
    String Ystring = "";
    ij.gui.GenericDialog gd = new ij.gui.GenericDialog("Shift Roi Locations");
    gd.addMessage("Roi: " + roi.getName());
    gd.addStringField("delta X:", "", 20);
    gd.addStringField("delta Y:", "", 20);
    gd.showDialog();
    if (gd.wasCanceled()) {
        return;
    }
    Xstring = gd.getNextString();
    Ystring = gd.getNextString();

    int xshift = Integer.parseInt(Xstring);
    int yshift = Integer.parseInt(Ystring);

    roiManager.shiftRoiLocations(roi, xshift, yshift);

}


 private void coreyEdgeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {

    com.nrims.experimental.coreyEdge ce = new com.nrims.experimental.coreyEdge(this);
    //ce.gentable();
    ce.test2();
}
*/

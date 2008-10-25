/*
 * mimsTomography.java
 *
 * Created on December 20, 2007, 3:00 PM
 */

package com.nrims;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;
import java.util.Vector;
import java.awt.*;


/**
 *
 * @author  cpoczatek
 */
public class MimsTomography extends javax.swing.JPanel {
    
    /** Creates new form mimsTomography */
    public MimsTomography(com.nrims.UI ui, com.nrims.data.Opener im) {
        initComponents();
        this.ui = ui ;
        this.image = im;
        this.images = ui.getMassImages();
        numberMasses = image.nMasses();
        imagestacks = new ImageStack[numberMasses];
        rp = ui.getOpenRatioImages();
                
        for (int i=0; i<=(numberMasses-1); i++) {
            imagestacks[i]=this.images[i].getStack();
        }
        
        
        //some swing component cleanup, whee...
        lowerSlider.setMaximum(image.nImages());
        upperSlider.setMaximum(image.nImages());
        upperSlider.setValue(upperSlider.getMaximum());
        
        lowerSlider.setMajorTickSpacing((int)(lowerSlider.getMaximum()/8)+1);
        upperSlider.setMajorTickSpacing((int)(upperSlider.getMaximum()/8)+1);
        
        jList2.setModel(new javax.swing.AbstractListModel() {
            String[] strings = image.getMassNames();
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        
        plotcolors = new java.util.Vector<java.awt.Color>();
        for(int i=0;i<20;i++)
            plotcolors.add(randColor());
        
        
    }
    
    public double getIndividualStat(ImageStatistics stats, String statname) {
        double st;
        
        if(statname.equals("area"))
            return stats.area;
        if(statname.equals("mean"))
            return stats.mean;
        if(statname.equals("stddev"))
            return stats.stdDev;
        if(statname.equals("mode"))
            return stats.mode;
        if(statname.equals("min"))
            return stats.min;
        if(statname.equals("max"))
            return stats.max;
        if(statname.equals("xcentroid"))
            return stats.xCentroid;
        if(statname.equals("ycentroid"))
            return stats.yCentroid;
        if(statname.equals("xcentermass"))
            return stats.xCenterOfMass;
        if(statname.equals("ycentermass"))
            return stats.yCenterOfMass;
        if(statname.equals("roix"))
            return stats.roiX;
        if(statname.equals("roiy"))
            return stats.roiY;
        if(statname.equals("roiwidth"))
            return stats.roiWidth;
        if(statname.equals("roiheight"))
            return stats.roiHeight;
        if(statname.equals("major"))
            return stats.major;
        if(statname.equals("minor"))
            return stats.minor;
        if(statname.equals("angle"))
            return stats.angle;
        if(statname.equals("feret"))
            return stats.FERET;
        if(statname.equals("sum"))
            return (stats.pixelCount*stats.mean);
        if(statname.equals("median"))
            return stats.median;
        if(statname.equals("kurtosis"))
            return stats.kurtosis;
        if(statname.equals("areafraction"))
            return stats.AREA_FRACTION;
        if(statname.equals("perimeter"))
            return stats.PERIMETER;
        
        return -999;
    }
    
    public double[] getPlotData(ij.gui.Roi roi, String statname, int mass, int min, int max) {
        
        double[] statistics = new double[max-min+1];
        ImageStatistics tempstats = null;
        if(roi!=null)
            if(mass<numberMasses)
                images[mass].setRoi(roi);
            else
                images[0].setRoi(roi);
        
        
        System.out.println("Mass #: " + mass + "ROI: " + roi.getName());
        
        if(mass<numberMasses) {
            for(int i=min; i<=max; i++) {
                images[mass].setSlice(i);
                tempstats = ImageStatistics.getStatistics(images[mass].getProcessor(), 0, images[mass].getCalibration());
                statistics[i-min] = getIndividualStat(tempstats, statname);
            }
        } else {
            for(int i=min; i<=max; i++) {
                images[0].setSlice(i);
                tempstats = ImageStatistics.getStatistics(rp[mass-numberMasses].getProcessor(), 0, rp[mass-numberMasses].getCalibration() );
                statistics[i-min] = getIndividualStat(tempstats, statname);
            }
        }
        
        return statistics;
    }
    
    public ij.gui.Plot generatePlot(Roi[] rois, String title, String[] stats, int[] masses, int min, int max) {
        
        double[] tempdata = getPlotData(rois[0], stats[0], masses[0], min, max);
        Vector<double[]> vect = new Vector<double[]>();
        vect.add(tempdata);
        Plot tempplot = new Plot(title, "plane", squishStrings(stats, "-"), getRange(min, max), tempdata);
        
        tempplot.setColor(plotcolors.get(0));
        
        for(int k=0; k<masses.length; k++) {
            for(int i=0; i<rois.length; i++) {
                for(int j=0; j<stats.length; j++) {
                    tempdata = getPlotData(rois[i], stats[j], masses[k], min, max);
                    vect.add(tempdata);
                }
            }
        }
        
        System.out.println("vector size: "+vect.size());
        
        double ymin = findMin(vect);
        double ymax = findMax(vect);
        System.out.println("min: "+ymin+" max: "+ymax);
        
        tempplot.setLimits(min, max, 0.85*ymin, 1.15*ymax);
        
        while(plotcolors.size() < vect.size()) {
            plotcolors.add(randColor());
            System.out.println("coloradded*******");
        }
        
        for(int i=1; i<vect.size(); i++) {
            System.out.println("grab: "+i);
            tempplot.setColor(plotcolors.get(i));
            
            tempplot.addPoints(getRange(min, max), (double[])(vect.get(i)), 2);
        }
        //reset color of original points...
        tempplot.setColor(plotcolors.get(0));
        return tempplot;
    }
    
    public double[] getRange(int min, int max) {
        double[] temp = new double[max-min+1];
        for(int i=0; i <= (max-min); i++)
            temp[i]=i+min;
        return temp;
    }
    
    public String squishStrings(String[] strings, String spacer) {
        String temp = spacer;
        for(int i=0; i<strings.length; i++)
            temp = temp + strings[i] + spacer;
        return temp;
    }
    
    public double findMin(Vector v) {
        double min = Integer.MAX_VALUE;
        int l = ((double[])v.get(0)).length;
        
        if(!v.isEmpty()) {
        for(int i=0; i<v.size(); i++) {
            double[] d = (double[])(v.get(i));
            for(int j=0; j<l; j++) {
                if(d[j] < min)
                    min = d[j];
            }
        }
        }
        
        return min;
    }
    
    public double findMax(Vector v) {
        double max = Integer.MIN_VALUE;
        int l = ((double[])v.get(0)).length;
        
        if(!v.isEmpty()) {
        for(int i=0; i<v.size(); i++) {
            double[] d = (double[])(v.get(i));
            for(int j=0; j<l; j++) {
                if(d[j] > max)
                    max = d[j];
            }
        }
        }
        
        return max;
    }
    
    public java.awt.Color randColor() {
        java.util.Random r = new java.util.Random();
        
        java.awt.Color c = new java.awt.Color(25+r.nextInt(206), 25+r.nextInt(206), 25+r.nextInt(206));
        
        return c;
    }
    
     
     /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lowerSlider = new javax.swing.JSlider();
        upperSlider = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jScrollPane2 = new javax.swing.JScrollPane();
        jList2 = new javax.swing.JList();
        jLabel4 = new javax.swing.JLabel();
        plotButton = new javax.swing.JButton();

        setToolTipText("");

        lowerSlider.setFont(new java.awt.Font("Dialog", 0, 10));
        lowerSlider.setMinimum(1);
        lowerSlider.setPaintLabels(true);
        lowerSlider.setPaintTicks(true);
        lowerSlider.setValue(1);
        lowerSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                lowerSliderStateChanged(evt);
            }
        });

        upperSlider.setFont(new java.awt.Font("Dialog", 0, 10));
        upperSlider.setMinimum(1);
        upperSlider.setPaintLabels(true);
        upperSlider.setPaintTicks(true);
        upperSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                upperSliderStateChanged(evt);
            }
        });

        jLabel1.setText("Lower limit");

        jLabel2.setText("Upper limit");

        jLabel3.setText("Statistics to plot");

        jList1.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "sum", "mean", "stddev", "min", "max", "mode", "area", "xcentroid", "ycentroid", "xcentermass", "ycentermass", "roix", "roiy", "roiwidth", "roiheight", "major", "minor", "angle", "feret", "median", "kurtosis", "areafraction", "perimeter" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(jList1);

        jScrollPane2.setViewportView(jList2);

        jLabel4.setText("Masses");

        plotButton.setText("Plot");
        plotButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                plotButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(30, 30, 30)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addGap(28, 28, 28)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1)
                                .addComponent(lowerSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel2)
                                .addComponent(upperSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(plotButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(10, 10, 10)
                        .addComponent(lowerSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(11, 11, 11)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(upperSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4))
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE))))
                .addGap(18, 18, 18)
                .addComponent(plotButton)
                .addContainerGap(69, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    private void lowerSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_lowerSliderStateChanged
        // TODO add your handling code here:
        int upperval = upperSlider.getValue();
        jLabel1.setText("Lower limit: "+lowerSlider.getValue());
        if (!upperSlider.getValueIsAdjusting() ) {
            if((upperval<=lowerSlider.getValue())) {
                upperSlider.setValue(java.lang.Math.min(upperSlider.getMaximum(),lowerSlider.getValue()+1));
            }
        }
    }//GEN-LAST:event_lowerSliderStateChanged

    private void upperSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_upperSliderStateChanged
        // TODO add your handling code here:
        int lowerval = lowerSlider.getValue();
        jLabel2.setText("Upper limit: "+upperSlider.getValue());
        if (!lowerSlider.getValueIsAdjusting() ) {
            if((lowerval>=upperSlider.getValue())) {
                lowerSlider.setValue(java.lang.Math.max(lowerSlider.getMinimum(),upperSlider.getValue()-1));
            }
        }
    }//GEN-LAST:event_upperSliderStateChanged

    private void plotButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotButtonActionPerformed
        // TODO add your handling code here:
        System.out.println("Button!");
        int currentPlane = images[0].getSlice();
        
        MimsRoiManager rm = ui.getRoiManager();
        rm.showFrame();
        ij.gui.Roi[] rois;
        java.awt.List rlist = rm.getList() ;
                
        int[] roiIndexes = rlist.getSelectedIndexes();
        
        if(!rm.getROIs().isEmpty()) {
            rois = new ij.gui.Roi[roiIndexes.length];
            for(int i = 0 ; i < roiIndexes.length; i++ ) {
                rois[i] = (ij.gui.Roi)rm.getROIs().get(rlist.getItem(roiIndexes[i]));
            }
        }            
        else rois = new ij.gui.Roi[0];
        
        
        if ( !(jList1.getSelectedIndex()==-1 || jList2.getSelectedIndex()==-1 || rois.length==0) ) {
            Object[] objs = new Object[jList1.getSelectedValues().length];
            objs = jList1.getSelectedValues();
            String[] statnames = new String[objs.length];
            for(int i=0; i<objs.length; i++) {
                statnames[i]=(String)objs[i];
            }
            
            int[] masses = jList2.getSelectedIndices();
        
            plot = generatePlot(rois, image.getName(), statnames, masses, lowerSlider.getValue(), upperSlider.getValue());
           
            if (plotWindow==null || plotWindow.isClosed()) {
            	plotWindow = plot.show();
                //plotWindow.setResizable(false);
                System.out.println("resizable?  "+plotWindow.isResizable());
                
            } else {
                plotWindow.drawPlot(plot);
                //plotWindow.setResizable(false);
                System.out.println("resizable?  "+plotWindow.isResizable());
            }
        } else {
            ij.IJ.error("Tomography Error", "You must select at least one ROI, statistic, and mass.");
        }
        
        images[0].setSlice(currentPlane);
        
        if(plotWindow != null) plotWindow.setResizable(false);
        //System.out.println("resizable?  "+plotWindow.isResizable());
        
        System.out.println("No More Button!");
    }//GEN-LAST:event_plotButtonActionPerformed
    
    public void resetBounds() {            
        if (images[0] != null){// to prevent exceptions when no images open 
        lowerSlider.setMaximum(images[0].getImageStackSize());
        upperSlider.setMaximum(images[0].getImageStackSize());
        upperSlider.setValue(upperSlider.getMaximum());
        
        lowerSlider.setMajorTickSpacing((int)(lowerSlider.getMaximum()/8)+1);
        upperSlider.setMajorTickSpacing((int)(upperSlider.getMaximum()/8)+1);
        
        lowerSlider.setLabelTable( lowerSlider.createStandardLabels((int)(lowerSlider.getMaximum()/8)+1) );
        upperSlider.setLabelTable( upperSlider.createStandardLabels((int)(upperSlider.getMaximum()/8)+1) );
    
        }
    }
    public void resetImageNamesList() {
        
        rp = ui.getOpenRatioImages();
        System.out.println("rp length: "+rp.length); 
                        
        java.util.ArrayList<String> strings = new java.util.ArrayList<String>();
        String[] tempstrings = image.getMassNames();
        
        for(int j=0; j<numberMasses; j++) {
            strings.add(tempstrings[j]);
        }
        
        for(int i=0; i<rp.length; i++) {
            String foo;
            foo = rp[i].getTitle();
            foo = foo.substring(foo.indexOf("_m")+1);
            foo = foo.replaceAll("_", "/");
            foo = foo.replaceAll("m", "");
            strings.add(foo);
        }
        
        final String[] str = new String[strings.size()];
        for(int k=0; k<str.length; k++)
            str[k]=strings.get(k);
        
        jList2.setModel(new javax.swing.AbstractListModel() {
            public int getSize() { return str.length; }
            public Object getElementAt(int i) { return str[i]; }
        });
        
        
    }
    
    private java.util.Vector<java.awt.Color> plotcolors;
    private Plot plot;
    private PlotWindow plotWindow;
    
        
    private com.nrims.UI ui;
    private com.nrims.data.Opener image;
    private int numberMasses;
    private MimsPlus[] images;
    private ImageStack[] imagestacks;
    private ij.process.ImageStatistics imagestats;
    private MimsPlus[] rp;
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JList jList1;
    private javax.swing.JList jList2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSlider lowerSlider;
    private javax.swing.JButton plotButton;
    private javax.swing.JSlider upperSlider;
    // End of variables declaration//GEN-END:variables
    

}

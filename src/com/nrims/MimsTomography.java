package com.nrims;

/*
 * mimsTomography.java
 *
 * Created on December 20, 2007, 3:00 PM
 */

import com.nrims.data.Opener;
import ij.gui.Roi;
import java.util.ArrayList;

/**
 * @author  cpoczatek
 */
public class MimsTomography extends javax.swing.JPanel {

    private UI ui;
    private Opener image;
    MimsJFreeChart tomoChart = null;
    MimsJTable table = null;
    
    /** Creates new form mimsTomography */
    public MimsTomography(UI ui) {
        System.out.println("MimsTomography constructor");
        initComponents();
        this.ui = ui;
        this.image = ui.getOpener();
        tomoChart = null;

        imageJList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = image.getMassNames();
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });        
    }
    
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      jLabel3 = new javax.swing.JLabel();
      jScrollPane1 = new javax.swing.JScrollPane();
      statJList = new javax.swing.JList();
      jScrollPane2 = new javax.swing.JScrollPane();
      imageJList = new javax.swing.JList();
      jLabel4 = new javax.swing.JLabel();
      plotButton = new javax.swing.JButton();
      appendCheckBox = new javax.swing.JCheckBox();
      jButton1 = new javax.swing.JButton();
      jTextField1 = new javax.swing.JTextField();
      jLabel5 = new javax.swing.JLabel();
      jCheckBox1 = new javax.swing.JCheckBox();

      setToolTipText("");

      jLabel3.setText("Statistics to plot");

      statJList.setModel(new javax.swing.AbstractListModel() {
         String[] strings = { "sum", "mean", "stddev", "min", "max", "mode", "area", "xcentroid", "ycentroid", "xcentermass", "ycentermass", "roix", "roiy", "roiwidth", "roiheight", "major", "minor", "angle", "feret", "median", "kurtosis", "areafraction", "perimeter" };
         public int getSize() { return strings.length; }
         public Object getElementAt(int i) { return strings[i]; }
      });
      jScrollPane1.setViewportView(statJList);

      jScrollPane2.setViewportView(imageJList);

      jLabel4.setText("Masses");

      plotButton.setText("Plot");
      plotButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            plotButtonActionPerformed(evt);
         }
      });

      appendCheckBox.setText("Append");

      jButton1.setText("Measure");
      jButton1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton1ActionPerformed(evt);
         }
      });

      jLabel5.setText("Image List (eg: 2,4,8-25,45...)");

      jCheckBox1.setText("Current plane only");

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
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(jLabel5)
                     .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jCheckBox1)))
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                     .addComponent(plotButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(jButton1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(appendCheckBox)))
            .addGap(28, 28, 28))
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
               .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(jLabel3)
                     .addComponent(jLabel4))
                  .addGap(10, 10, 10)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                     .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)))
               .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                  .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(jCheckBox1)))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addGap(18, 18, 18)
                  .addComponent(plotButton)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(jButton1))
               .addGroup(layout.createSequentialGroup()
                  .addGap(34, 34, 34)
                  .addComponent(appendCheckBox)))
            .addContainerGap(39, Short.MAX_VALUE))
      );
   }// </editor-fold>//GEN-END:initComponents

    private void plotButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotButtonActionPerformed

       // initialize variables.
       MimsRoiManager rm = ui.getRoiManager();
       rm.showFrame();

       if (!appendCheckBox.isSelected() || tomoChart == null) {
          tomoChart = new MimsJFreeChart(ui);
       }

       // Determine planes.
       ArrayList planes = getPlanes();
       if (planes.size() >= 1) {
          tomoChart.setPlanes(planes);
       } else {
          System.out.println("Undetermined planes");
          return;
       }

       // Get selected stats.
       String[] statnames = getStatNames();
       if (statnames.length >= 1) {
          tomoChart.setStats(statnames);
       } else {
          System.out.println("No stats selected");
          return;
       }

       // Get selected rois.
       Roi[] rois = rm.getSelectedROIs();
       if (rois.length >= 1) {
          tomoChart.setRois(rois);
       } else {
          System.out.println("No rois selected");
          return;
       }

       // images
       MimsPlus[] images = getImages();
       if (images.length >= 1) {
          tomoChart.setImages(images);
       } else {
          System.out.println("No images selected");
          return;
       }

       tomoChart.createFrame(appendCheckBox.isSelected());                               
    }//GEN-LAST:event_plotButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

       // initialize variables.
       MimsRoiManager rm = ui.getRoiManager();
       rm.showFrame();

       if (!appendCheckBox.isSelected() || table == null) {
          table = new MimsJTable(ui);
       }

       // Determine planes.
       ArrayList planes = getPlanes();
       if (planes.size() >= 1) {
          table.setPlanes(planes);
       } else {
          System.out.println("Undetermined planes");
          return;
       }

       // Get selected stats.
       String[] statnames = getStatNames();
       if (statnames.length >= 1) {
          table.setStats(statnames);
       } else {
          System.out.println("No stats selected");
          return;
       }

       // Get selected rois.
       Roi[] rois = rm.getSelectedROIs();
       if (rois.length >= 1) {
          table.setRois(rois);
       } else {
          System.out.println("No rois selected");
          return;
       }

       // images
       MimsPlus[] images = getImages();
       if (images.length >= 1) {
          table.setImages(images);
       } else {
          System.out.println("No images selected");
          return;
       }
       
       table.createTable(appendCheckBox.isSelected());
       table.showFrame();
    }//GEN-LAST:event_jButton1ActionPerformed
    
    public void resetImageNamesList() {
        
        MimsPlus[] rp = ui.getOpenRatioImages();
                        
        java.util.ArrayList<String> strings = new java.util.ArrayList<String>();
        String[] tempstrings = ui.getOpener().getMassNames();
        
        for(int j=0; j<tempstrings.length; j++) {
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
        
        imageJList.setModel(new javax.swing.AbstractListModel() {
            public int getSize() { return str.length; }
            public Object getElementAt(int i) { return str[i]; }
        });
        
        
    }
    
    private MimsPlus[] getImages(){
       
       // Get selected images.
       int[] num = imageJList.getSelectedIndices();
       
       // Get all open mass and ratio images.
       MimsPlus[] mp = ui.getOpenMassImages();
       MimsPlus[] rp = ui.getOpenRatioImages();
       
       // Build list of images.
       MimsPlus[] images = new MimsPlus[num.length];
       for (int i = 0; i < num.length; i++) {
          if (i < mp.length) {
             images[i] = mp[num[i]];
          } else if (i >= mp.length && i < mp.length + rp.length) {
             images[i] = rp[num[i]-mp.length];
          }
       }
       
       return images;
    }
    
    private String[] getStatNames(){
       Object[] objs = new Object[statJList.getSelectedValues().length];
       objs = statJList.getSelectedValues();
       String[] statnames = new String[objs.length];
       for (int i = 0; i < objs.length; i++) {
          statnames[i] = (String) objs[i];
       }
       return statnames;
    }

    private ArrayList<Integer> getPlanes(){

       // initialize
       ArrayList planes = new ArrayList<Integer>();

       // Get text.
       String list = jTextField1.getText().trim();

       // Parse text, generat list.
       if (jCheckBox1.isSelected()) {
          planes.add(ui.getOpenMassImages()[0].getCurrentSlice());
       } else if (list.matches("") || list.length() == 0) {
          for(int i = 1; i <= ui.getmimsAction().getSize(); i++) {
             planes.add(i);
          }
       } else {
          planes = MimsStackEditing.parseList(jTextField1.getText(), 1, ui.getmimsAction().getSize());
       }

       return planes;
    }
 
   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JCheckBox appendCheckBox;
   private javax.swing.JList imageJList;
   private javax.swing.JButton jButton1;
   private javax.swing.JCheckBox jCheckBox1;
   private javax.swing.JLabel jLabel3;
   private javax.swing.JLabel jLabel4;
   private javax.swing.JLabel jLabel5;
   private javax.swing.JScrollPane jScrollPane1;
   private javax.swing.JScrollPane jScrollPane2;
   private javax.swing.JTextField jTextField1;
   private javax.swing.JButton plotButton;
   private javax.swing.JList statJList;
   // End of variables declaration//GEN-END:variables
}

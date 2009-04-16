/*
 * mimsRoiControl.java
 *
 * Created on May 3, 2006, 9:57 AM
 */

package com.nrims;

import ij.*;
import com.nrims.UI;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;


public class MimsRoiControl extends javax.swing.JPanel {
    
    public MimsRoiControl(UI ui) {
        this.ui = ui ;
        measure = new Measure(ui);
        initComponents();
        setupHistogram();

        jCheckBox2.setSelected(ui.getSyncROIs());
        jCheckBox3.setSelected(ui.getAddROIs());
        jCheckBox5.setSelected(ui.getSyncROIsAcrossPlanes());
        jTextField1.setText(ui.getOpener().getImageFile().getName()+".txt");
        measure.setName(jTextField1.getText());
    }
    
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      jCheckBox2 = new javax.swing.JCheckBox();
      jCheckBox3 = new javax.swing.JCheckBox();
      jButton1 = new javax.swing.JButton();
      jButton2 = new javax.swing.JButton();
      jButton3 = new javax.swing.JButton();
      jButton4 = new javax.swing.JButton();
      jCheckBox4 = new javax.swing.JCheckBox();
      jLabel1 = new javax.swing.JLabel();
      jTextField1 = new javax.swing.JTextField();
      jButton5 = new javax.swing.JButton();
      jCheckBox5 = new javax.swing.JCheckBox();
      histogramjPanel = new javax.swing.JPanel();
      histogramUpdatejCheckBox = new javax.swing.JCheckBox();
      profilejButton = new javax.swing.JButton();

      setToolTipText("Drawing ROIs automatically adds to RoiManager");

      jCheckBox2.setSelected(true);
      jCheckBox2.setText("Synchronize ROIs across all masses");
      jCheckBox2.setToolTipText("Display ROI in all images");
      jCheckBox2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
      jCheckBox2.setMargin(new java.awt.Insets(0, 0, 0, 0));
      jCheckBox2.addItemListener(new java.awt.event.ItemListener() {
         public void itemStateChanged(java.awt.event.ItemEvent evt) {
            jCheckBox2ItemStateChanged(evt);
         }
      });

      jCheckBox3.setSelected(true);
      jCheckBox3.setText("Add ROIs");
      jCheckBox3.setToolTipText("Automatically add ROs to the RoiManager");
      jCheckBox3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
      jCheckBox3.setMargin(new java.awt.Insets(0, 0, 0, 0));
      jCheckBox3.addItemListener(new java.awt.event.ItemListener() {
         public void itemStateChanged(java.awt.event.ItemEvent evt) {
            jCheckBox3ItemStateChanged(evt);
         }
      });

      jButton1.setText("RoiManager");
      jButton1.setToolTipText("Launch RoiManager");
      jButton1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton1ActionPerformed(evt);
         }
      });

      jButton2.setText("Measure All Rois");
      jButton2.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton2ActionPerformed(evt);
         }
      });

      jButton3.setText("Data Options...");
      jButton3.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton3ActionPerformed(evt);
         }
      });

      jButton4.setText("Source images...");
      jButton4.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton4ActionPerformed(evt);
         }
      });

      jCheckBox4.setText("Append to table");
      jCheckBox4.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
      jCheckBox4.setMargin(new java.awt.Insets(0, 0, 0, 0));

      jLabel1.setText("Table Name");

      jTextField1.setText("NRIMS.txt");
      jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyPressed(java.awt.event.KeyEvent evt) {
            jTextField1KeyPressed(evt);
         }
      });

      jButton5.setText("Measure Sum Images");
      jButton5.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton5ActionPerformed(evt);
         }
      });

      jCheckBox5.setSelected(true);
      jCheckBox5.setText("Synchronize ROIs across all planes ");
      jCheckBox5.setToolTipText("Display ROI in all images");
      jCheckBox5.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
      jCheckBox5.setMargin(new java.awt.Insets(0, 0, 0, 0));
      jCheckBox5.addItemListener(new java.awt.event.ItemListener() {
         public void itemStateChanged(java.awt.event.ItemEvent evt) {
            jCheckBox5ItemStateChanged(evt);
         }
      });

      histogramjPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
      histogramjPanel.setMaximumSize(new java.awt.Dimension(300, 250));
      histogramjPanel.setMinimumSize(new java.awt.Dimension(300, 250));
      histogramjPanel.setPreferredSize(new java.awt.Dimension(350, 250));

      org.jdesktop.layout.GroupLayout histogramjPanelLayout = new org.jdesktop.layout.GroupLayout(histogramjPanel);
      histogramjPanel.setLayout(histogramjPanelLayout);
      histogramjPanelLayout.setHorizontalGroup(
         histogramjPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(0, 348, Short.MAX_VALUE)
      );
      histogramjPanelLayout.setVerticalGroup(
         histogramjPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(0, 248, Short.MAX_VALUE)
      );

      histogramUpdatejCheckBox.setText("Autoupdate Histogram");

      profilejButton.setText("Dynamic Profile");
      profilejButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            profilejButtonActionPerformed(evt);
         }
      });

      org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(layout.createSequentialGroup()
            .addContainerGap()
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(layout.createSequentialGroup()
                  .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                     .add(layout.createSequentialGroup()
                        .add(jCheckBox3)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jButton1))
                     .add(jCheckBox5)
                     .add(jCheckBox2)
                     .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, jButton3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, jButton4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                     .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, jButton3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, jButton4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                  .add(40, 40, 40)
                  .add(histogramjPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
               .add(jCheckBox4)
               .add(jCheckBox5)
               .add(jCheckBox2)
               .add(layout.createSequentialGroup()
                  .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                     .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 144, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                     .add(layout.createSequentialGroup()
                        .add(jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 138, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 166, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                  .add(43, 43, 43)
                  .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                     .add(profilejButton)
                     .add(histogramUpdatejCheckBox))))
            .add(139, 139, 139))
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(layout.createSequentialGroup()
            .add(12, 12, 12)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
               .add(org.jdesktop.layout.GroupLayout.LEADING, histogramjPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                  .add(jCheckBox5)
                  .add(18, 18, 18)
                  .add(jCheckBox2)
                  .add(16, 16, 16)
                  .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(jCheckBox3)
                     .add(jButton1))
                  .add(51, 51, 51)
                  .add(jButton4)
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                  .add(jButton3)))
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(layout.createSequentialGroup()
                  .add(12, 12, 12)
                  .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(jLabel1)
                     .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(jButton2)
                     .add(jButton5))
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(jCheckBox4))
               .add(layout.createSequentialGroup()
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                  .add(histogramUpdatejCheckBox)
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(profilejButton)))
            .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );
   }// </editor-fold>//GEN-END:initComponents

    private void setupHistogram() {
        // Create arbitrary dataset
        HistogramDataset dataset = new HistogramDataset();
        double[] values = new double[100];
        for (int i = 0; i < 100; i++) {
            values[i] = 10*i;
        }
        dataset.addSeries("H1", values, 100);
        // Create chart using the ChartFactory
        chart = ChartFactory.createHistogram(
                "",
                null,
                null,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        
        XYPlot plot = (XYPlot) chart.getPlot();
        
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardXYBarPainter());


        chartPanel = new ChartPanel(chart);
        chartPanel.setSize(350, 250);
        histogramjPanel.add(chartPanel);
    }
    
    public void updateHistogram(double[] pixelvalues, String label, boolean forceupdate) {
       if(pixelvalues == null) {
          return;
       } else if (pixelvalues.length == 0) {
          return;
       }
       if (forceupdate || histogramUpdatejCheckBox.isSelected()) {
          HistogramDataset dataset = new HistogramDataset();

          dataset.addSeries(label, pixelvalues, 100);

          org.jfree.chart.plot.XYPlot plot = (XYPlot) chart.getPlot();
          plot.setDataset(dataset);

          chart.fireChartChanged();
       }
    }
    
    private void jTextField1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyPressed
        if(evt.getKeyChar() == '\t' || evt.getKeyChar() == '\n')
            measure.setName(jTextField1.getText());
    }//GEN-LAST:event_jTextField1KeyPressed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        measure.getSourceOptions();
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        measure.getDataOptions();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        // If not appending reset the data in the table
        if (!jCheckBox4.isSelected()) {
            measure.reset();
            measure.setResize(true);
        } else {
            measure.setResize(false);
        }
        
        if (jCheckBox5.isSelected()) {
            measure.generateStackTable();
        } else {
            measure.generateRoiTable();
        //ij.WindowManager.getFrame(measure.getName()).setSize(450, 300);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        ui.getRoiManager().showFrame();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jCheckBox3ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBox3ItemStateChanged
        ui.setAddROIs(jCheckBox3.isSelected());
    }//GEN-LAST:event_jCheckBox3ItemStateChanged

    private void jCheckBox2ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBox2ItemStateChanged
        ui.setSyncROIs(jCheckBox2.isSelected());
    }//GEN-LAST:event_jCheckBox2ItemStateChanged

private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
// TODO add your handling code here:
    // If not appending reset the data in the table
    if(jCheckBox4.isSelected() == false ) measure.reset();
    measure.measureSums();
    
    if (ij.WindowManager.getFrame(measure.getName()) != null) {
        ij.WindowManager.getFrame(measure.getName()).setSize(450, 300);
        ij.WindowManager.getFrame(measure.getName());
    }
}//GEN-LAST:event_jButton5ActionPerformed

private void jCheckBox5ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBox5ItemStateChanged
// TODO add your handling code here:   
   ui.setSyncROIsAcrossPlanes(jCheckBox5.isSelected());
   WindowManager.getCurrentImage().updateAndRepaintWindow();   
   //WindowManager.repaintImageWindows();
}//GEN-LAST:event_jCheckBox5ItemStateChanged

private void profilejButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_profilejButtonActionPerformed
// TODO add your handling code here:
    
    if (ui.lineProfile == null) {
        ui.lineProfile = new MimsLineProfile();
        double[] foo = new double[100];
        for (int i = 0; i < 100; i++) {
            foo[i] = 10;
        }
        ui.updateLineProfile(foo, "line", 1);
    } else {
        ui.lineProfile.setVisible(true);
    }
 
}//GEN-LAST:event_profilejButtonActionPerformed

public Measure getMeasure(){
   return measure;
}

public boolean append(){
   return jCheckBox4.isSelected();
}

public void setROIsSynchedAcrossPlanes(boolean setSynched) {
   jCheckBox5.setSelected(setSynched);
}
   
    private com.nrims.UI ui = null ;
    private com.nrims.Measure measure = null ;
    
    private JFreeChart chart;
    private ChartPanel chartPanel;
    
   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JCheckBox histogramUpdatejCheckBox;
   private javax.swing.JPanel histogramjPanel;
   private javax.swing.JButton jButton1;
   private javax.swing.JButton jButton2;
   private javax.swing.JButton jButton3;
   private javax.swing.JButton jButton4;
   private javax.swing.JButton jButton5;
   private javax.swing.JCheckBox jCheckBox2;
   private javax.swing.JCheckBox jCheckBox3;
   private javax.swing.JCheckBox jCheckBox4;
   private javax.swing.JCheckBox jCheckBox5;
   private javax.swing.JLabel jLabel1;
   private javax.swing.JTextField jTextField1;
   private javax.swing.JButton profilejButton;
   // End of variables declaration//GEN-END:variables
    
}

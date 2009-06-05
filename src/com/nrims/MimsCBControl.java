/*
 * MimsCBControl.java
 *
 * Created on February 10, 2009, 5:19 PM
 */

package com.nrims;

import ij.WindowManager;
import ij.plugin.LutLoader;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Hashtable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPolygonAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.ui.Layer;

public class MimsCBControl extends javax.swing.JPanel {
   
   Hashtable windows = new Hashtable();
   UI ui;   
   private JFreeChart chart;
   private ChartPanel chartPanel;
   XYPolygonAnnotation a;
   
    public MimsCBControl(UI ui) {
       this.ui = ui;       
       initComponents();
       jLabel1.setText("");
       Dimension d = new Dimension(350, 200);
       jPanel1.setMinimumSize(d); jPanel1.setPreferredSize(d); jPanel1.setMaximumSize(d);
       jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder());
       setupHistogram();
    }
    
    private void setupHistogram() {

        // Create chart using the ChartFactory.
        chart = ChartFactory.createHistogram("", null, null, null, PlotOrientation.VERTICAL, true, true, false);
        chart.setBackgroundPaint(this.getBackground());
        chart.removeLegend();
        
        // Set the renderer.
        XYPlot plot = (XYPlot) chart.getPlot();                                       
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardXYBarPainter());
        
        chartPanel = new ChartPanel(chart);
        chartPanel.setSize(350, 225); 
        jPanel1.add(chartPanel);
    }
    
   // Call this method whenever you want to update the histogram.
   // Histogram updates to reflect title selected in combobox.
   public void updateHistogram(){ 
      
      // Dont bother updating histogram if tab not even visible.
      if (!this.isVisible())
         return;       
      
      // Initialize imp variable.
      MimsPlus imp = null;   
      
      // Get the title of the window currently selected in the combobox.
      // Then get the window associated with that title. 
      String title = (String)jComboBox1.getSelectedItem();                  
      if (title != null) 
         imp = (MimsPlus)windows.get(title); 
      
      // Not sure why but sometimes images have NULL image processors.
      // Cant update histogram if it does not have one.
      ImageProcessor ip = imp.getProcessor();
      if (ip == null) return;
      
      // Update the sliders.
      contrastAdjuster1.update(imp);                              

      // Get Pixel values.
      int i = 0;
      int width = imp.getWidth();
      int height = imp.getHeight();
      int nbins = 256;
      double[] pixels = new double[width * height];
      double pixelVal, maxVal = 0.0;
      for (int x = 0; x < width; x++) {
         for (int y = 0; y < height; y++) {
            pixelVal = ip.getPixelValue(x, y);
            pixels[i] = pixelVal;
            if (maxVal < pixelVal) {
               maxVal = pixelVal;
            }
            i++;
         }
      }

      // return if no data avaialable
      if (pixels == null)
         return;      
      if (pixels.length == 0)
         return;
            
      
      // Setup and plot histogram.
      HistogramDataset dataset = new HistogramDataset();
      dataset.addSeries("", pixels, nbins);            
      org.jfree.chart.plot.XYPlot plot = (XYPlot) chart.getPlot();
      plot.setDataset(dataset);                 
      
      // Show contrast window.
      updateContrastWindow();
      
      // Final plot adjustmetns.
      plot.getDomainAxis().setRange(0, maxVal);
      plot.setDomainGridlinesVisible(false);                 
      chart.fireChartChanged();     
   }
   
   // Update the contrast window range displayed.
   public void updateContrastWindow(){
      
      org.jfree.chart.plot.XYPlot plot = (XYPlot) chart.getPlot();
      
      // Polygon bounds.
      int x1 = (new Double(contrastAdjuster1.min)).intValue();
      int x2 = (new Double(contrastAdjuster1.min)).intValue();   
      int x3 = (new Double(contrastAdjuster1.max)).intValue();
      int x4 = (new Double(contrastAdjuster1.max)).intValue();    
      
      int y1 = -1;
      int y2 = (new Double(plot.getRangeAxis().getUpperBound())).intValue()+1;
      int y3 = (new Double(plot.getRangeAxis().getUpperBound())).intValue()+1;
      int y4 = -1;     
      
      // Displays polygon.
      a = new XYPolygonAnnotation(new double[] {x1, y1, x2, y2, x3, y3, x4, y4}, 
              null, new Color(100, 100, 155, 70), new Color(200, 200, 255, 100));            
      XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
      renderer.removeAnnotations();
      renderer.addAnnotation(a, Layer.BACKGROUND);          
   }
                 
   // set the windowlist jComboBox
   public void setWindowlistCombobox(String title){
      if (windows.containsKey(title)) {
         jComboBox1.setSelectedItem(title);
      }      
   }   
   
   // Adds the window's title to the combobox list.
   public void addWindowtoList(MimsPlus imp){
      String title = imp.getTitle();
      if (!windows.containsKey(title)) {
         jComboBox1.addItem(title);
         windows.put(title, imp);
      }
      if (jComboBox1.getItemCount() == 1){
         jComboBox1.setSelectedItem(title);         
      } 
   }   
   
   // Removes the window's title from combobox list.
   public void removeWindowfromList(MimsPlus imp){
      String title = imp.getTitle();
      if (windows.containsKey(title)) {
         jComboBox1.removeItem(title);
         windows.remove(title);
      }
   }
   
   // Is auto-contrast radiobutton selected.
   public boolean autoContrastRadioButtonIsSelected(){   
      return jRadioButton1.isSelected();
   }
   
   public ContrastAdjuster getContrastAdjuster(){
      return contrastAdjuster1;
   }
   
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      contrastAdjuster1 = new com.nrims.ContrastAdjuster(ui);
      jLabel1 = new javax.swing.JLabel();
      jComboBox1 = new javax.swing.JComboBox();
      jLabel2 = new javax.swing.JLabel();
      jRadioButton1 = new javax.swing.JRadioButton();
      jComboBox2 = new javax.swing.JComboBox();
      jLabel5 = new javax.swing.JLabel();
      jPanel1 = new javax.swing.JPanel();

      jLabel1.setText("Contrast / Brightness");

      jComboBox1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jComboBox1ActionPerformed(evt);
         }
      });

      jLabel2.setText("Window :");

      jRadioButton1.setText("Auto adjust");
      jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jRadioButton1ActionPerformed(evt);
         }
      });

      jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Grays", "Fire", "Ice", "Spectrum", "3-3-2 RGB", "Red", "Green", "Blue", "Cyan", "Magenta", "Yellow", "Red/Green", "Invert LUT" }));
      jComboBox2.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jComboBox2ActionPerformed(evt);
         }
      });

      jLabel5.setText("Lookup Table :");

      jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

      javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
      jPanel1.setLayout(jPanel1Layout);
      jPanel1Layout.setHorizontalGroup(
         jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 302, Short.MAX_VALUE)
      );
      jPanel1Layout.setVerticalGroup(
         jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 203, Short.MAX_VALUE)
      );

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
               .addGroup(layout.createSequentialGroup()
                  .addGap(81, 81, 81)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jRadioButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)))
                  .addGap(83, 83, 83)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel5)))
               .addGroup(layout.createSequentialGroup()
                  .addGap(57, 57, 57)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                     .addComponent(contrastAdjuster1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel1))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addGap(28, 28, 28)
            .addComponent(jLabel1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addComponent(contrastAdjuster1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(33, 33, 33)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jLabel2)
               .addComponent(jRadioButton1)
               .addComponent(jLabel5))
            .addGap(4, 4, 4)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
   MimsPlus imp = null;   
      
   // Get the title of the window currently selected in the combobox.
   // Then get the window associated with that title. 
   String title = (String)jComboBox1.getSelectedItem();                  
   if (title != null) 
      imp = (MimsPlus)windows.get(title);      

   // Select autocontrasting radio button...
   if (imp != null) {
      jRadioButton1.setSelected(imp.getAutoContrastAdjust());
      updateHistogram();           
   }
}//GEN-LAST:event_jComboBox1ActionPerformed

private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
   MimsPlus imp = null;   
      
   // Get the title of the window currently selected in the combobox.
   // Then get the window associated with that title. 
   String title = (String)jComboBox1.getSelectedItem();                  
   if (title != null) 
      imp = (MimsPlus)windows.get(title);
   imp.setAutoContrastAdjust(jRadioButton1.isSelected());
   
   // If setting to true, do autocontrasting.
   if (jRadioButton1.isSelected()) {
         ui.autoContrastImage(imp);
   }   
   updateHistogram();
}//GEN-LAST:event_jRadioButton1ActionPerformed

private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
   
   // Get the selected LUT  
   String label = (String)jComboBox2.getSelectedItem();
   
   // Get the selected window.
   String title = (String)jComboBox1.getSelectedItem();  
   MimsPlus imp = null;
   if (title != null) 
      imp = (MimsPlus)windows.get(title);    
   if (imp != null)
      WindowManager.setCurrentWindow(imp.getWindow());
  
   // Manipulate the string
   if (label.equals("Red/Green"))
      label = "redgreen";
   else if (label.equals("Invert LUT"))
      label = "invert";
   else if (label.equals("3-3-2 RGB"))
      label = "3-3-2 RGB";
   else
      label = label.toLowerCase();
      
   // Change the LUT
   LutLoader ll = new LutLoader();
   ll.run(label);
                    
}//GEN-LAST:event_jComboBox2ActionPerformed

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private com.nrims.ContrastAdjuster contrastAdjuster1;
   private javax.swing.JComboBox jComboBox1;
   private javax.swing.JComboBox jComboBox2;
   private javax.swing.JLabel jLabel1;
   private javax.swing.JLabel jLabel2;
   private javax.swing.JLabel jLabel5;
   private javax.swing.JPanel jPanel1;
   private javax.swing.JRadioButton jRadioButton1;
   // End of variables declaration//GEN-END:variables
}
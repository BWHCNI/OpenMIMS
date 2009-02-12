/*
 * MimsCBControl.java
 *
 * Created on February 10, 2009, 5:19 PM
 */

package com.nrims;

import ij.ImagePlus;
import java.util.Hashtable;

/* @author zkaufman */
public class MimsCBControl extends javax.swing.JPanel {
   
   Hashtable windows = new Hashtable();

    public MimsCBControl() {
        initComponents();               
    }

    @SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      contrastAdjuster1 = new com.nrims.ContrastAdjuster();
      jLabel1 = new javax.swing.JLabel();
      jComboBox1 = new javax.swing.JComboBox();
      jLabel2 = new javax.swing.JLabel();
      jRadioButton1 = new javax.swing.JRadioButton();

      jLabel1.setText("Contrast / Brightness");

      jComboBox1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jComboBox1ActionPerformed(evt);
         }
      });

      jLabel2.setText("Window :");

      jRadioButton1.setText("Auto adjust");

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addGap(48, 48, 48)
            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addGap(12, 12, 12)
                  .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addGap(91, 91, 91)
                  .addComponent(jRadioButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addComponent(contrastAdjuster1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addGap(53, 53, 53)))
            .addContainerGap(106, Short.MAX_VALUE))
         .addGroup(layout.createSequentialGroup()
            .addGap(191, 191, 191)
            .addComponent(jLabel1)
            .addContainerGap(416, Short.MAX_VALUE))
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap(56, Short.MAX_VALUE)
            .addComponent(jLabel1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(contrastAdjuster1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                  .addComponent(jLabel2)
                  .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addComponent(jRadioButton1))
            .addGap(23, 23, 23))
      );
   }// </editor-fold>//GEN-END:initComponents

private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
   updateHistogram();           
}//GEN-LAST:event_jComboBox1ActionPerformed

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private com.nrims.ContrastAdjuster contrastAdjuster1;
   private javax.swing.JComboBox jComboBox1;
   private javax.swing.JLabel jLabel1;
   private javax.swing.JLabel jLabel2;
   private javax.swing.JRadioButton jRadioButton1;
   // End of variables declaration//GEN-END:variables
   
   // Call this method whenever you want to update the histogram.
   // Histogram updates to reflect title selected in combobox.
   public void updateHistogram(){      
      
      // Initialize imp variable.
      ImagePlus imp = null;   
      
      // Get the title of the window currently selected in the combobox.
      // Then get the window associated with that title. 
      String title = (String)jComboBox1.getSelectedItem();                  
      if (title != null) 
         imp = (ImagePlus)windows.get(title);              
      
      // Update the histogram for the image in that window.
      contrastAdjuster1.update(imp);
      
      // Autoadjust, if selected.
      if (jRadioButton1.isSelected()) {
         contrastAdjuster1.doReset = true; // no need to reset...
         contrastAdjuster1.doUpdate(); 
      }
      
   }
   
   // Adds window title to list in combobox
   public void addWindowtoList(ImagePlus imp){
      String title = imp.getTitle();
      if (!windows.containsKey(title)) {
         jComboBox1.addItem(title);
         windows.put(title, imp);
      }
      if (jComboBox1.getItemCount() == 1){
         jComboBox1.setSelectedItem(title);
      }
   }   
   
   // Removes window title from list in combobox
   public void removeWindowfromList(ImagePlus imp){
      String title = imp.getTitle();
      if (windows.containsKey(title)) {
         jComboBox1.removeItem(title);
         windows.remove(title);
      }
   }
}
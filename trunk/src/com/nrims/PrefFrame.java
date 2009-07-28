
package com.nrims;

import ij.Prefs;

public class PrefFrame extends PlugInJFrame {
   
   boolean includeHSI = true;
   boolean includeSum = true;
   boolean includeMass = false;
   boolean includeRatio = false;
   int scaleFactor = 10000;
   final String PREFS_KEY="openmims.";

   public PrefFrame() {   
      super("Preferences");
      readPreferences();       
      initComponents();
   }
       
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      jLabel1 = new javax.swing.JLabel();
      HSIcheckbox = new javax.swing.JCheckBox();
      sumCheckbox = new javax.swing.JCheckBox();
      massCheckbox = new javax.swing.JCheckBox();
      ratioCheckbox = new javax.swing.JCheckBox();
      jLabel2 = new javax.swing.JLabel();
      scaleFactorTextbox = new javax.swing.JTextField();
      jButton1 = new javax.swing.JButton();
      jButton2 = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

      jLabel1.setText("When exporting images:");

      HSIcheckbox.setSelected(includeHSI);
      HSIcheckbox.setText("include HSI images");

      sumCheckbox.setSelected(includeSum);
      sumCheckbox.setText("include sum images");

      massCheckbox.setSelected(includeMass);
      massCheckbox.setText("include mass images");

      ratioCheckbox.setSelected(includeRatio);
      ratioCheckbox.setText("include ratio images");

      jLabel2.setText("Ratio scale factor:");

      scaleFactorTextbox.setText((new Integer(scaleFactor)).toString());

      jButton1.setText("Cancel");
      jButton1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton1ActionPerformed(evt);
         }
      });

      jButton2.setText("Save");
      jButton2.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton2ActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addGap(181, 181, 181)
                  .addComponent(jButton1)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 69, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addContainerGap()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(jLabel1)
                     .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                           .addComponent(sumCheckbox)
                           .addComponent(massCheckbox)
                           .addComponent(HSIcheckbox)
                           .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                              .addComponent(scaleFactorTextbox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                              .addComponent(ratioCheckbox))))
                     .addComponent(jLabel2))))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel1)
            .addGap(8, 8, 8)
            .addComponent(HSIcheckbox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(sumCheckbox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(massCheckbox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(ratioCheckbox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jLabel2)
               .addComponent(scaleFactorTextbox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 109, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jButton2)
               .addComponent(jButton1))
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
   savePreferences();
}//GEN-LAST:event_jButton2ActionPerformed

private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
   close();
}//GEN-LAST:event_jButton1ActionPerformed

    void readPreferences() {       
       includeHSI   = (boolean)Prefs.get(PREFS_KEY+"includeHSI", includeHSI);
       includeSum   = (boolean)Prefs.get(PREFS_KEY+"includeSum", includeSum);
       includeMass  = (boolean)Prefs.get(PREFS_KEY+"includeMass", includeMass);
       includeRatio = (boolean)Prefs.get(PREFS_KEY+"includeRatio", includeRatio);
       scaleFactor  = (int)Prefs.get(PREFS_KEY+"ratioScaleFactor", scaleFactor);                   
    }
    
    void savePreferences() {       
       includeHSI   = HSIcheckbox.isSelected();
       includeSum   = sumCheckbox.isSelected();
       includeMass  = massCheckbox.isSelected();
       includeRatio = ratioCheckbox.isSelected();
       try {
          scaleFactor = new Integer(scaleFactorTextbox.getText());
       } catch (Exception e) {}
       
       Prefs.set(PREFS_KEY+"includeHSI", includeHSI);
       Prefs.set(PREFS_KEY+"includeSum", includeSum);
       Prefs.set(PREFS_KEY+"includeMass", includeMass);
       Prefs.set(PREFS_KEY+"includeRatio", includeRatio);       
       Prefs.set(PREFS_KEY+"ratioScaleFactor", scaleFactor);
       Prefs.savePreferences();              
       close();
    }    
    
    public void showFrame() {
        setVisible(true);
        toFront();
        setExtendedState(NORMAL);
    }
    
    public void close() {
       setVisible(false);
    }

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JCheckBox HSIcheckbox;
   private javax.swing.JButton jButton1;
   private javax.swing.JButton jButton2;
   private javax.swing.JLabel jLabel1;
   private javax.swing.JLabel jLabel2;
   private javax.swing.JCheckBox massCheckbox;
   private javax.swing.JCheckBox ratioCheckbox;
   private javax.swing.JTextField scaleFactorTextbox;
   private javax.swing.JCheckBox sumCheckbox;
   // End of variables declaration//GEN-END:variables
}

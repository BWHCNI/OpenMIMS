package com.nrims.experimental;

import com.nrims.PlugInJFrame;
import com.nrims.UI;
import ij.IJ;
import ij.Prefs;

/**
 * This class creates a user preferences interface. It opens as a frame and allows user to enter parameters and settings
 * that control the applications behavior. It is built upon the ImageJ Preferences class. These settings are stored in
 * the ImageJ preferences file unually located in ~HOME_DIR/.imagej/IJ_Prefs.txt. All Open_Mims settings are preceded
 * with the "openmims." string.
 *
 * @author cpoczatek
 */
public class PrefFrame extends PlugInJFrame {

    boolean includeHSI = true;
    boolean includeSum = true;
    boolean includeMass = false;
    boolean includeRatio = false;
    double scaleFactor = 10000;
    double ratioSpan = 1.5;
    boolean ratioReciprocals = false;
    boolean showAutosaveROISdialog = true;
    boolean enableHighDPIonLinux = false;
    UI ui;
    float reference = (float) 0.0130;
    float background = (float) 0.0037;
    String numerators = "";
    String denominators = "";
    double massDiff = 0.5;
    int numDecimalPlaces = 2;
    int tileY = 0;
    int autoSaveInterval = 120;
    String formatString = "M[S]:F";

    //DJ: 10/24/2014
    String myNotesPath = "";

    final String PREFS_KEY = "openmims.";

    /**
     * Instantiates the class and creates the frame.
     * 
     * @param ui a reference to the <code>ui</code> instance.
     */
    public PrefFrame(UI ui) {
        super("Preferences");
        this.ui = ui;
        readPreferences();
        initComponents();
        initComponentsCustom();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        HSIcheckbox = new javax.swing.JCheckBox();
        sumCheckbox = new javax.swing.JCheckBox();
        massCheckbox = new javax.swing.JCheckBox();
        ratioCheckbox = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        scaleFactorTextbox = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        ratioSpanTextbox = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        ratioReciprocalsCheckBox = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        backgroundTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        referenceTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        numDecimalPlacesSpinner = new javax.swing.JSpinner();
        tileYTextField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        AutoSaveIntervalTextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        formatStringTextField = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        myNotes_jLabel = new javax.swing.JLabel();
        myNotes_jTextField1 = new javax.swing.JTextField();
        showAutosaveDialogCheckBox = new javax.swing.JCheckBox();
        enableHighDPIonLinuxCheckBox = new javax.swing.JCheckBox();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/nrims/experimental/Bundle"); // NOI18N
        jLabel5.setText(bundle.getString("PrefFrame.jLabel5.text")); // NOI18N

        jLabel6.setText(bundle.getString("PrefFrame.jLabel6.text")); // NOI18N

        jLabel1.setText(bundle.getString("PrefFrame.jLabel1.text")); // NOI18N

        HSIcheckbox.setText(bundle.getString("PrefFrame.HSIcheckbox.text")); // NOI18N

        sumCheckbox.setText(bundle.getString("PrefFrame.sumCheckbox.text")); // NOI18N

        massCheckbox.setText(bundle.getString("PrefFrame.massCheckbox.text")); // NOI18N

        ratioCheckbox.setText(bundle.getString("PrefFrame.ratioCheckbox.text")); // NOI18N

        jLabel2.setText(bundle.getString("PrefFrame.jLabel2.text")); // NOI18N

        jButton1.setText(bundle.getString("PrefFrame.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText(bundle.getString("PrefFrame.jButton2.text")); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel3.setText(bundle.getString("PrefFrame.jLabel3.text")); // NOI18N

        ratioReciprocalsCheckBox.setText(bundle.getString("PrefFrame.ratioReciprocalsCheckBox.text")); // NOI18N

        jLabel4.setText(bundle.getString("PrefFrame.jLabel4.text")); // NOI18N

        jLabel7.setText(bundle.getString("PrefFrame.jLabel7.text")); // NOI18N

        jLabel8.setText(bundle.getString("PrefFrame.jLabel8.text")); // NOI18N

        numDecimalPlacesSpinner.setModel(new javax.swing.SpinnerNumberModel(2, 1, 9, 1));

        jLabel9.setText(bundle.getString("PrefFrame.jLabel9.text")); // NOI18N

        jLabel10.setText(bundle.getString("PrefFrame.jLabel10.text")); // NOI18N

        jLabel11.setText(bundle.getString("PrefFrame.jLabel11.text")); // NOI18N

        myNotes_jLabel.setText(bundle.getString("PrefFrame.myNotes_jLabel.text")); // NOI18N

        showAutosaveDialogCheckBox.setSelected(true);
        showAutosaveDialogCheckBox.setText(bundle.getString("PrefFrame.showAutosaveDialogCheckBox.text")); // NOI18N
        showAutosaveDialogCheckBox.setToolTipText(bundle.getString("PrefFrame.showAutosaveDialogCheckBox.toolTipText")); // NOI18N
        showAutosaveDialogCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAutosaveDialogCheckBoxActionPerformed(evt);
            }
        });

        enableHighDPIonLinuxCheckBox.setText(bundle.getString("PrefFrame.enableHighDPIonLinuxCheckBox.text")); // NOI18N
        enableHighDPIonLinuxCheckBox.setToolTipText(bundle.getString("PrefFrame.enableHighDPIonLinuxCheckBox.toolTipText")); // NOI18N

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
                        .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(AutoSaveIntervalTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE)
                            .addComponent(tileYTextField))
                        .addGap(113, 113, 113))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(myNotes_jLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(myNotes_jTextField1))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel2)
                                            .addComponent(jLabel3))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(ratioSpanTextbox, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(scaleFactorTextbox, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(sumCheckbox)
                                            .addComponent(massCheckbox)
                                            .addComponent(HSIcheckbox)
                                            .addComponent(ratioCheckbox)
                                            .addComponent(ratioReciprocalsCheckBox)))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel4)
                                            .addComponent(jLabel7))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(referenceTextField)
                                            .addComponent(backgroundTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel8)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(numDecimalPlacesSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel11)
                                        .addGap(28, 28, 28)
                                        .addComponent(formatStringTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(showAutosaveDialogCheckBox)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(enableHighDPIonLinuxCheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(ratioSpanTextbox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ratioReciprocalsCheckBox)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(backgroundTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(referenceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(numDecimalPlacesSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tileYTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AutoSaveIntervalTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(formatStringTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(myNotes_jLabel)
                    .addComponent(myNotes_jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(enableHighDPIonLinuxCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showAutosaveDialogCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(jButton1))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Action method for the "save" button.
     */
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        savePreferences();
    }//GEN-LAST:event_jButton2ActionPerformed

    /**
     * Action method for the "cancel" button.
     */
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        close();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void showAutosaveDialogCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAutosaveDialogCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_showAutosaveDialogCheckBoxActionPerformed

    private void initComponentsCustom() {

        this.HSIcheckbox.setSelected(includeHSI);
        this.ratioCheckbox.setSelected(includeRatio);
        this.massCheckbox.setSelected(includeMass);
        this.sumCheckbox.setSelected(includeSum);

        this.scaleFactorTextbox.setText(new Double(scaleFactor).toString());
        this.ratioSpanTextbox.setText(new Double(ratioSpan).toString());
        this.ratioReciprocalsCheckBox.setSelected(ratioReciprocals);
        this.showAutosaveDialogCheckBox.setSelected(showAutosaveROISdialog);
        this.backgroundTextField.setText(new Float(background).toString());
        this.referenceTextField.setText(new Float(reference).toString());
        this.numDecimalPlacesSpinner.setValue(numDecimalPlaces);
        this.tileYTextField.setText(new Integer(tileY).toString());
        this.AutoSaveIntervalTextField.setText(new Integer(autoSaveInterval).toString());
        this.formatStringTextField.setText(formatString);

        //DJ: 10/24/2014
        this.myNotes_jTextField1.setText(myNotesPath);
    }

    /**
     * Reads the preferences and sets member variables accordingly.
     */
    private void readPreferences() {
        includeHSI = (boolean) Prefs.get(PREFS_KEY + "includeHSI", includeHSI);
        includeSum = (boolean) Prefs.get(PREFS_KEY + "includeSum", includeSum);
        includeMass = (boolean) Prefs.get(PREFS_KEY + "includeMass", includeMass);
        includeRatio = (boolean) Prefs.get(PREFS_KEY + "includeRatio", includeRatio);
        scaleFactor = (double) Prefs.get(PREFS_KEY + "ratioScaleFactor", scaleFactor);
        ratioSpan = (double) Prefs.get(PREFS_KEY + "ratioSpan", ratioSpan);
        ratioReciprocals = (boolean) Prefs.get(PREFS_KEY + "ratioReciprocals", ratioReciprocals);
        showAutosaveROISdialog = (boolean) Prefs.get(PREFS_KEY + "showAutosaveROISdialog", showAutosaveROISdialog);
        enableHighDPIonLinux = (boolean) Prefs.get(PREFS_KEY + "enableHighDPIonLinux", enableHighDPIonLinux);      
        reference = (float) Prefs.get(PREFS_KEY + "reference", reference);
        background = (float) Prefs.get(PREFS_KEY + "background", background);
        numerators = Prefs.get(PREFS_KEY + "numerators", numerators);
        denominators = Prefs.get(PREFS_KEY + "denominators", denominators);
        numDecimalPlaces = (int) Prefs.get(PREFS_KEY + "numDecimalPlaces", numDecimalPlaces);
        tileY = (int) Prefs.get(PREFS_KEY + "tileY", tileY);
        autoSaveInterval = (int) Prefs.get(PREFS_KEY + "autoSaveInterval", autoSaveInterval);
        formatString = Prefs.get(PREFS_KEY + "formatString", formatString);

        //DJ: 10/24/2014
        myNotesPath = Prefs.get(PREFS_KEY + "myNotesPath", myNotesPath);
    }

    /**
     * Saves the preferences file.
     */
    public void savePreferences() {
        includeHSI = HSIcheckbox.isSelected();
        includeSum = sumCheckbox.isSelected();
        includeMass = massCheckbox.isSelected();
        includeRatio = ratioCheckbox.isSelected();
        ratioReciprocals = ratioReciprocalsCheckBox.isSelected();
        showAutosaveROISdialog = showAutosaveDialogCheckBox.isSelected();
        enableHighDPIonLinux = enableHighDPIonLinuxCheckBox.isSelected();

        try {
            scaleFactor = new Double(scaleFactorTextbox.getText());
        } catch (Exception e) {
            IJ.error("Malformed \"scale factor\" value.");
            return;
        }

        if (ui.getHSIView() != null) {
            ui.getHSIView().setRatioScaleFactor(scaleFactor);
        }

        try {
            ratioSpan = new Double(ratioSpanTextbox.getText());
        } catch (Exception e) {
            IJ.error("Malformed \"ratio span\" value.");
            return;
        }

        try {
            background = new Float(backgroundTextField.getText());
        } catch (Exception e) {
            IJ.error("Malformed \"background\" value.");
            return;
        }

        try {
            reference = new Float(referenceTextField.getText());
        } catch (Exception e) {
            IJ.error("Malformed \"reference\" value.");
            return;
        }

        try {
            numDecimalPlaces = (Integer) numDecimalPlacesSpinner.getValue();
        } catch (Exception e) {
            IJ.error("Malformed \"numDecimalPlaces\" value.");
            return;
        }

        try {
            tileY = new Integer(tileYTextField.getText());
        } catch (Exception e) {
            IJ.error("Malformed \"Tile Y Offset\" value.");
            return;
        }
        try {
            autoSaveInterval = new Integer(AutoSaveIntervalTextField.getText());
        } catch (Exception e) {
            IJ.error("Malformed \"Autosave Interval\" value.");
            return;
        }
        try {
            formatString = formatStringTextField.getText();
        } catch (Exception e) {
            IJ.error("Malformed \"formatString\" value.");
            return;
        }
        //DJ: 10/24/2014
        try {
            myNotesPath = myNotes_jTextField1.getText();
        } catch (Exception e) {
            IJ.error("Malformed \"myNotesPath\" value.");
            return;
        }
        Prefs.set(PREFS_KEY + "includeHSI", includeHSI);
        Prefs.set(PREFS_KEY + "includeSum", includeSum);
        Prefs.set(PREFS_KEY + "includeMass", includeMass);
        Prefs.set(PREFS_KEY + "includeRatio", includeRatio);
        Prefs.set(PREFS_KEY + "ratioScaleFactor", scaleFactor);
        Prefs.set(PREFS_KEY + "ratioSpan", ratioSpan);
        Prefs.set(PREFS_KEY + "ratioReciprocals", ratioReciprocals);
        Prefs.set(PREFS_KEY + "showAutosaveROISdialog", showAutosaveROISdialog);
        Prefs.set(PREFS_KEY + "enableHighDPIonLinux", enableHighDPIonLinux);
        Prefs.set(PREFS_KEY + "background", background);
        Prefs.set(PREFS_KEY + "reference", reference);
        Prefs.set(PREFS_KEY + "numerators", numerators);
        Prefs.set(PREFS_KEY + "denominators", denominators);
        Prefs.set(PREFS_KEY + "numDecimalPlaces", numDecimalPlaces);
        Prefs.set(PREFS_KEY + "tileY", tileY);
        Prefs.set(PREFS_KEY + "autoSaveInterval", autoSaveInterval);
        Prefs.set(PREFS_KEY + "formatString", formatString);
        //DJ: 10/24/2014
        Prefs.set(PREFS_KEY + "myNotesPath", myNotesPath);
        Prefs.savePreferences();
        close();
    }

    /**
     * Shows the frame.
     */
    public void showFrame() {
        initComponentsCustom();
        setVisible(true);
        toFront();
        setExtendedState(NORMAL);
    }

    @Override
    public void close() {
        setVisible(false);
    }

    /**
     * Include HSI images when exporting images?
     *
     * @return boolean.
     */
    boolean getincludeHSI() {
        return includeHSI;
    }

    /**
     * Include Sum images when exporting images?
     *
     * @return boolean.
     */
    boolean getincludeSum() {
        return includeSum;
    }

    /**
     * Include Mass images when exporting images?
     *
     * @return boolean.
     */
    boolean getincludeMass() {
        return includeMass;
    }

    /**
     * Include Ratio images when exporting images?
     *
     * @return boolean.
     */
    boolean getincludeRatio() {
        return includeRatio;
    }

    /**
     * Gets the scale factor.
     *
     * @return the scale factor.
     */
    double getscaleFactor() {
        return scaleFactor;
    }

    /**
     * Gets the difference allowed between atomic numbers in order to show ratio images in the list.
     *
     * @return double
     */
    double getRatioSpan() {
        return ratioSpan;
    }

    /**
     * Gets the difference in mass allowed between atomic numbers for ratio images saved by the user.
     *
     * @return double
     */
    double getMassDiff() {
        return massDiff;
    }

    /**
     * Include reciprocals in the ratio image list (13/12 and 12/13).
     *
     * @return boolean
     */
    boolean getRatioReciprocals() {
        return ratioReciprocals;
    }
    
            
    /**
     * Returns whether autosave ROIs dialog when loading images files is enabled or not.
     *
     * @return boolean
     */
    boolean getShowAutosaveROISdialog() {
        return showAutosaveROISdialog;
    }
    
     /**
     * Gets state of enabling highDPI display on Linux
     *
     * @return boolean
     */
    boolean getEnableHighDPIonLinux() {
        return enableHighDPIonLinux;
    }
    

    /**
     * Get the background ratio reference.
     *
     * @return the background ratio
     */
    float getBackgroundRatio() {
        return background;
    }

    /**
     * Get the source ratio reference.
     *
     * @return the background ratio
     */
    float getReferenceRatio() {
        return reference;
    }

    /**
     * Get the number of decimal places to use in tables.
     *
     * @return the number of decimal places.
     */
    int getNumDecimalPlaces() {
        return numDecimalPlaces;
    }

    /**
     * Get the y position of where tiling starts
     *
     * @return the number of decimal places.
     */
    int getTileY() {
        return tileY;
    }

    /**
     * Get the y position of where tiling starts
     *
     * @return the number of decimal places.
     */
    int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    /**
     * Get the format string used to determine format of image titles
     *
     * @return the formatString
     */
    public String getFormatString() {
        return formatString;
    }

    //Dj: 10/24/2014
    /**
     * Get the Notes path string used to determine the user's notes file
     *
     * @return
     */
    String getMyNotesPath() {
        return myNotesPath;
    }

    /**
     * Get the list of ratio images preferred by the user.
     */
    String[] getNumerators() {
        String[] numStrArray = new String[0];
        if (numerators != null) {
            numStrArray = numerators.split(",");
        }
        return numStrArray;
    }

    /**
     * Get the list of ratio images preferred by the user.
     */
    String[] getDenominators() {
        String[] denStrArray = new String[0];
        if (denominators != null) {
            denStrArray = denominators.split(",");
        }
        return denStrArray;
    }

    /**
     * Add a ratio image with numerator num and denominator den to the list of default ratio images.
     */
    void addRatioImage(double num, double den) {
        if (numerators == null || numerators.length() == 0 || numerators.length() != denominators.length()) {
            numerators = "";
            denominators = "";
        } else {
            numerators += ",";
            denominators += ",";
        }
        numerators += Double.toString(num);
        denominators += Double.toString(den);
    }

    /**
     * Add a ratio image with numerator num and denominator den to the list of default ratio images.
     */
    void addRatioImage(String num, String den) {

        Double numValue, denValue;
        try {
            numValue = Double.parseDouble(num);
            denValue = Double.parseDouble(den);
        } catch (NumberFormatException nfe) {
            return;
        }
        addRatioImage(numValue, denValue);
    }

    /**
     * Remove a ratio image with numerator num, and denominator den, to the list of default ratio images.
     */
    void removeRatioImage(double num, double den) {
        if (numerators == null || numerators.length() == 0) {
            return;
        }
        String[] numStrArray = numerators.split(",");
        String[] denStrArray = denominators.split(",");
        if (numStrArray.length != denStrArray.length) {
            return;
        }

        // Clear the numerator and denominator list and 
        // add back those ratio images that are NOT to be removed.
        numerators = "";
        denominators = "";
        Double numValue, denValue, numDiff, denDiff;
        for (int i = 0; i < numStrArray.length; i++) {

            try {
                numValue = Double.parseDouble(numStrArray[i]);
                denValue = Double.parseDouble(denStrArray[i]);
            } catch (NumberFormatException nfe) {
                continue;
            }

            numDiff = Math.abs(num - numValue);
            denDiff = Math.abs(den - denValue);
            System.out.println();
            System.out.println("numValue = " + numValue);
            System.out.println("numDiff = " + numDiff + ", denDiff = " + denDiff);
            System.out.println(numDiff < massDiff && denDiff < massDiff);
            if (numDiff > massDiff && denDiff > massDiff) {
                addRatioImage(numValue, denValue);
            }
        }
        System.out.println();
        System.out.println("numerators = " + numerators);

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField AutoSaveIntervalTextField;
    private javax.swing.JCheckBox HSIcheckbox;
    private javax.swing.JTextField backgroundTextField;
    private javax.swing.JCheckBox enableHighDPIonLinuxCheckBox;
    private javax.swing.JTextField formatStringTextField;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JCheckBox massCheckbox;
    private javax.swing.JLabel myNotes_jLabel;
    private javax.swing.JTextField myNotes_jTextField1;
    private javax.swing.JSpinner numDecimalPlacesSpinner;
    private javax.swing.JCheckBox ratioCheckbox;
    private javax.swing.JCheckBox ratioReciprocalsCheckBox;
    private javax.swing.JTextField ratioSpanTextbox;
    private javax.swing.JTextField referenceTextField;
    private javax.swing.JTextField scaleFactorTextbox;
    private javax.swing.JCheckBox showAutosaveDialogCheckBox;
    private javax.swing.JCheckBox sumCheckbox;
    private javax.swing.JTextField tileYTextField;
    // End of variables declaration//GEN-END:variables
}

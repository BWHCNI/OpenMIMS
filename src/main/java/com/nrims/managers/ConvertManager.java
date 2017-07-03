/*
 * ConvertManager.java
 *
 * Created on Jul 5, 2011, 3:54:47 PM
 */
package com.nrims.managers;

import com.nrims.Converter;
import com.nrims.Converter.FileHeaderCheckStatus;
import com.nrims.MimsJFileChooser;
import com.nrims.MimsPlus;
import com.nrims.UI;
import ij.IJ;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.ListIterator;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import org.jfree.ui.ExtensionFileFilter;

/**
 *
 * @author zkaufman
 */
public class ConvertManager extends JFrame implements PropertyChangeListener {

    UI ui;
    static Color[] colors = {Color.BLUE, Color.GRAY, Color.RED};

    File[] files;
    ArrayList<String> fileNames = new ArrayList<String>();
    Converter converter;
    File configFile;

    String html_string = "Yo";
    String mass = "";
    static JFrame instance;
    private boolean onlyReadHeader = false;
    ArrayList<FileHeaderCheckStatus> fileStatusList;
    ComboBoxRenderer renderer;

    // DJ
    // to indicate whether this class 
    // is used to generate html web page or just to convert IMs and NRRDs.
    boolean isHTML; // true if this converter is used to genrate html
    // false if it is used to convert to nrrd only.

    /**
     * Creates new form convertManager
     * 
     * @param ui a reference to the <code>ui</code> instance.
     * @param isHTML true if this class should be an HTML generator.
     */
    public ConvertManager(UI ui, boolean isHTML) {

        initComponents();
        fileListComboBox.setForeground(Color.BLUE);
        renderer = new ComboBoxRenderer(fileListComboBox);
        jLabelFilesHadBadHeaders.setVisible(false);
        jLabelFilesHadBadHeaders.setText("<html>Some files had bad headers.<br>Click on the list above</html>");
        massComboBox.setVisible(false);

        config_file_label.setForeground(Color.BLUE);

        this.isHTML = isHTML;

        // DJ: Implementation decisions/thoughts:
        // We decided to include this double functionality of this class due to the job similarity 
        // between what this class ititially peroforms and what we actually want from the Html Generator.
        // So making the fusion between the two in the same class was the best option as of now.
        // So here are the two cases:
        // The case where this class is used as an Html Gererator instead of the basic "ConvertManger".
        if (isHTML == true) {
            this.setTitle("HTML GENERATOR");
            config_file_label.setText(" ");

            choose_config_Button.setEnabled(false);
            config_file_label.setEnabled(false);

            allOpenedImages_radioButton.setSelected(true);

            //openMimsSpecs_radioButton.setToolTipText("HSI/MASS IMAGES selected at the Tomography Tab");
            allOpenedImages_radioButton.setToolTipText("All the images that are shown on the screen.");
            configFile_radioButton.setToolTipText("config file of type \".cgf\" that has specs to collect");

            /*
            //DJ: 10/27/2014 : just a thought:
            // in case there is no image that is currently open,
            // we force the user to use a config.cfg file because there is no way 
            // to get the information otherwise.
            if(ui.getHSIView() == null){
                allOpenedImages_radioButton.setSelected(false);
                allOpenedImages_radioButton.setEnabled(false);
                configFile_radioButton.setSelected(true);
            } 
             */
        } // The case where this class is used as a "ConvertManger" instead of an "Html Gererator"
        else {
            this.setTitle("Convert Manager");
            //this.remove(openMimsSpecs_radioButton);
            this.remove(allOpenedImages_radioButton);
            this.remove(configFile_radioButton);
            this.remove(choose_config_Button);
            this.remove(config_file_label);

            setSize((new Double(getSize().getWidth())).intValue(),
                    (new Double(getSize().getHeight())).intValue() - 70);

        }

        this.ui = ui;
        fileStatusList = new ArrayList<com.nrims.Converter.FileHeaderCheckStatus>();

        setLocation(ui.getLocation().x + 50, ui.getLocation().y + 50);

        // DJ
        // so ehen the "X" closing logo button is hit, just this manager
        // gets closed and not the OpenMIMS plugin as a whole.
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        instance = this;
    }

    // DJ : 09/19/2014 : 
    public static ConvertManager getInstance() {
        return (ConvertManager) instance;
    }

    // DJ : 09/19/2014 : 
    public void setHtmlString(String html) {
        html_string = html;
    }

    // DJ : 09/19/2014 : 
    public String getHtmlString() {
        return html_string;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        trackCheckBox = new javax.swing.JCheckBox();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        fileListComboBox = new javax.swing.JComboBox();
        selectFilesButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        selectFilesButton1 = new javax.swing.JButton();
        configFile_radioButton = new javax.swing.JRadioButton();
        config_file_label = new javax.swing.JLabel();
        choose_config_Button = new javax.swing.JButton();
        allOpenedImages_radioButton = new javax.swing.JRadioButton();
        jLabelFilesHadBadHeaders = new javax.swing.JLabel();
        massComboBox = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        trackCheckBox.setText("Auto track");
        trackCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trackCheckBoxActionPerformed(evt);
            }
        });

        okButton.setText("OK");
        okButton.setSelected(true);
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        fileListComboBox.setToolTipText("<html>Blue:  files with good headers.  <br>Green:  files with bad headers that were fixed upon reading them.   <br>Red:  Files with bad headers that could not be fixed.</html>");
        fileListComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                fileListComboBoxItemStateChanged(evt);
            }
        });

        selectFilesButton.setText("Select files...");
        selectFilesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectFilesButtonActionPerformed(evt);
            }
        });

        progressBar.setStringPainted(true);

        selectFilesButton1.setText("delete selected");
        selectFilesButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectFilesButton1ActionPerformed(evt);
            }
        });

        buttonGroup1.add(configFile_radioButton);
        configFile_radioButton.setText("Choose a \"CONFIG\" file");
        configFile_radioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configFile_radioButtonActionPerformed(evt);
            }
        });

        config_file_label.setText("config_file_label");

        choose_config_Button.setText("Select CONFIG file");
        choose_config_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                choose_config_ButtonActionPerformed(evt);
            }
        });

        buttonGroup1.add(allOpenedImages_radioButton);
        allOpenedImages_radioButton.setText("Use all the actual opened images");

        jLabelFilesHadBadHeaders.setForeground(new java.awt.Color(255, 0, 0));
        jLabelFilesHadBadHeaders.setText("Some files had bad headers.  Check the list.");

        massComboBox.setToolTipText("");
        massComboBox.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        massComboBox.setRequestFocusEnabled(false);
        massComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                massComboBoxItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(selectFilesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(selectFilesButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(cancelButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(progressBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(trackCheckBox)
                                .addGap(18, 18, 18)
                                .addComponent(massComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelFilesHadBadHeaders))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addComponent(config_file_label))
                            .addComponent(allOpenedImages_radioButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(configFile_radioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(choose_config_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(fileListComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 304, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(selectFilesButton)
                    .addComponent(selectFilesButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fileListComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(trackCheckBox)
                    .addComponent(jLabelFilesHadBadHeaders)
                    .addComponent(massComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(allOpenedImages_radioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(configFile_radioButton)
                    .addComponent(choose_config_Button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(config_file_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addGap(18, 18, 18)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void selectFilesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectFilesButtonActionPerformed
        trackCheckBox.setSelected(false);  // This prevents tracking upon user selection of a new list of files.
        // If this is checked and the user selects new files, the code attempts not only to check their headers,
        // but also tries to track them, which causes errors for some files, thus preventing the update of the files
        // list.
        massComboBox.setVisible(false);
        selectFiles();
    }//GEN-LAST:event_selectFilesButtonActionPerformed

    public void selectFiles() {
        MimsJFileChooser mjfc = new MimsJFileChooser(ui);
        mjfc.setMultiSelectionEnabled(true);
        mjfc.setPreferredSize(new java.awt.Dimension(650, 500));
        int returnVal = mjfc.showOpenDialog(this);

        fileNames.clear();
        fileListComboBox.removeAllItems();
        jLabelFilesHadBadHeaders.setVisible(false);

        // Open file or return null.
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File currentDirectory = mjfc.getCurrentDirectory();

            // First see if the current user has ownership of this directory.
            //String osName = System.getProperty("os.name").toLowerCase();
            String osName = System.getProperties().getProperty("os.name");

            String userName = System.getProperty("user.name");
            String userID = System.getProperties().getProperty("user.name");

            if (osName.toLowerCase().contains("windows")) {
                //userName = new com.sun.security.auth.module.NTSystem().getName();
                userName = System.getenv().get("USERNAME");
            } else if (osName.toLowerCase().contains("linux")) {
                //userName = new com.sun.security.auth.module.UnixSystem().getUsername();
            } else if (osName.contains("solaris") || osName.contains("sunos")) {
                //userName = new com.sun.security.auth.module.SolarisSystem().getUsername();
            } else if (osName.contains("Mac OS X")) {
                //userName = new com.sun.security.auth.module.UnixSystem().getUsername();
            }
            
        //System.out.println("user name: " + userName);
        //System.out.println("operating system:  " + osName);
            
//             The try block below works well to find the owner of a file or directory, but it 
//             require nio, which is unavailable in Java 6, which we have to use until Fiji
//             decides to join the 21st century (use Java 7 or above).  Finding the onwer 
//             without nio is very kludgy, so ror now, we will just depend on directories
//             that the user does not own being read-only.
            try {
                // Path thePath = FileSystems.getDefault().getPath("logs", "access.log");
                Path thePath = currentDirectory.toPath();   //toPath
                UserPrincipal owner = java.nio.file.Files.getOwner(thePath);
              
                owner = java.nio.file.Files.getOwner(thePath);  // on OSX, all show same
               
                // Since for now we have to target Java 1.6, it may not be possible to get 
                // the owner, because the nio stuff above for getting the owner is not
                // available in Java 1.6.   For now, just depend on writability of the directory.
                
                
                String ownerStr = owner.toString();
                ownerStr = owner.getName();
                if (!ownerStr.contains(userID)) {
                    JOptionPane.showMessageDialog(this,
                            "You cannot use files in this directory because you are not the owner of the directory.",
                            "Not directory owner",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "OpenMIMS attempted to find the directory owner, but failed.",
                        " Error retrieving directory owner",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // User has (might have) ownership of the directory, now see if it is read-only             
            if (mjfc.isCurrentDirReadOnly()) {

                String currentDirStr = currentDirectory.getPath();
                //ij.IJ.error("The directory " + currentDirStr + " is read-only.");

                Object[] options = {"Make dir writable and continue",
                    "Select different directory"};

                int buttonNum = JOptionPane.showOptionDialog(this,
                        "The directory " + currentDirStr + " is read-only\n and/or you may not be the owner.",
                        "Selected directory is read-only",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[1]);

                if (buttonNum == JOptionPane.NO_OPTION) {
                    return;
                } else {
                    try {
                        if (!currentDirectory.setWritable(true, true)) {
                            JOptionPane.showMessageDialog(this,
                                    "OpenMIMS attempted to make the current directory writeable, but failed, \n"
                                    + "possibly because you are not the owner or the directory.\n\n"
                                    + "You will have to make the directory writeable manually.",
                                    "setWritable Error",
                                    JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    } catch (SecurityException se) {
                        JOptionPane.showMessageDialog(this,
                                "OpenMIMS attempted to make the current directory writeable, but failed. /n"
                                + "You will have to make the directory writeable manually.",
                                "Security Exception Error",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            files = mjfc.getSelectedFiles();
        } else {
            return;
        }

        converter = new Converter(false, false, trackCheckBox.isSelected(), mass, null, "", "");
        for (File file : files) {
            boolean isFile = file.isFile();
            boolean contains = fileNames.contains(file.getAbsolutePath());
            if (isFile && (contains == false)) {
                fileListComboBox.addItem(file.getName());
                fileNames.add(file.getAbsolutePath());
            }
        }
        fileListComboBox.setSelectedIndex(0);  // Fires event so the mass combobox appears initially.

        // Check file headers.
        onlyReadHeader = true;
        converter.setFiles(fileNames, onlyReadHeader);
        converter.addPropertyChangeListener(this);   // having this here screws up file reading somehow
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        okButton.setEnabled(false);
        converter.execute();

    }
//    
//   private static Object userInfoProvider;
//
//   private static Method userNameGetter;
//
//    public static String getUserName() {
//        if (userInfoProvider == null) {
//            //logger.error("cannot reliably obtain the current user name from the operating system "
//             //   + "(could not instantiate a user information provider)");
//            return System.getProperty("user.name", "unknown");
//        }
//        try { // ... to call 'userInfoProvider'.'userNameGetter'() and cast the result to a String...
//            return (String) userNameGetter.invoke(userInfoProvider);
//        } catch (Throwable e) {
//            //logger.error("cannot reliably obtain the current user name from the operating system", e);
//            return System.getProperty("user.name", "unknown");
//        }
//    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed

        if (converter != null) {
            converter.proceed(false);
        }
        setCursor(null);
        close();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        // Return if file list is empty (user did not select files).
        // or if all files were deleted from the Combo Box. 
        if (files == null || fileListComboBox.getModel().getSize() == 0) {
            ij.IJ.error("No files selected");
            setCursor(null);
            okButton.setEnabled(true);
            return;
        }

        // Make sure user enters valid mass.
        if (trackCheckBox.isSelected()) {
            try {
                //String mass = massTextField.getText();
                //double massString = new Double(massTextField.getText());
            } catch (Exception e) {
                //ij.IJ.error("\"" + massTextField.getText() + "\"" + " is not a valid mass value.");
                setCursor(null);
                okButton.setEnabled(true);
                return;
            }
        }

        okButton.setEnabled(true); //DJ: changed to "false"; originally was "true"
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        //=======================================================================

        // DJ:
        // In case we are using this converterManager as an HTML generator. 
        if (this.isHTML == true) {

            JFileChooser chooser = new JFileChooser(ui.getLastFolder());
            FileFilter filter = new ExtensionFileFilter("html", new String("html"));
            chooser.setFileFilter(filter);

            int returnVal = chooser.showSaveDialog(this);

            if (returnVal == JFileChooser.CANCEL_OPTION) {
                chooser.setVisible(false);
                this.setEnabled(true);
                this.setVisible(true);
            }
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();

                // We check if the html file name ends with ".html".
                // if it doesn't, we add the extension ".html"
                String fileExtension = selectedFile.getAbsolutePath().substring(selectedFile.getAbsolutePath().length() - 5);
                if (fileExtension.equals(".html") == false) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + ".html");
                }

                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                /*
                String[] HSIsArray =             new String[0];
                String[] numThreshArray =        new String[0];
                String[] denThreshArray =        new String[0];
                String[] ratioScaleFactorArray = new String[0];
                String[] maxRGBArray =           new String[0];
                String[] minRGBArray =           new String[0];
                 */
                //=======================================================================================

                /*
                // we get the selected HSIs at the HSIView ( the Process Tab when you run OpenMIMS )
                if (ui.getHSIView() != null && ui.getHSIView().getSelectedRatios() != null) {

                    String[] massNames = ui.getOpener().getMassNames();
                    String[] selectedRatios = ui.getHSIView().getSelectedRatios();

                    //System.out.println("selected ratios' length is: " + selectedRatios.length);

                    HSIsArray = new String[selectedRatios.length];
                    numThreshArray = new String[selectedRatios.length];
                    denThreshArray = new String[selectedRatios.length];
                    ratioScaleFactorArray = new String[selectedRatios.length];
                    maxRGBArray = new String[selectedRatios.length];
                    minRGBArray = new String[selectedRatios.length];

                    //----- GET THE NUM THRESHHOLD  --------
                    int numThresh = ui.getHSIView().getNumThresh();
                    //System.out.println(" numThresh = " + numThresh);

                    //----- GET THE DEN THRESHHOLD   --------
                    int denThresh = ui.getHSIView().getDenThresh();
                    //System.out.println(" denThresh = " + denThresh);

                    //----- GET THE RATIO SCALE FACTOR --------
                    int ratioScaleFactor = (int)(Math.round(ui.getHSIView().getRatioScaleFactor()));
                    //System.out.println(" Ratio Scale Factor = " + ratioScaleFactor);

                    //----- GET THE MAXIMUM RGB FACTOR --------
                    int maxRGB = ui.getHSIView().getMaxRGB();
                    //System.out.println(" maxRGB = " + maxRGB);

                    //----- GET THE MINIMUM RGB FACTOR --------
                    int minRGB = ui.getHSIView().getMinRGB();
                    //System.out.println(" minRGB = " + minRGB);


                    //----- GET THE HSIsArray --------
                    for (int i = 0; i < selectedRatios.length; i++) {

                        String[] num_den = selectedRatios[i].split(":");

                        Double num_massValue = Double.parseDouble( massNames[Integer.parseInt(num_den[0])]);
                        Double den_massValue = Double.parseDouble( massNames[Integer.parseInt(num_den[1])]);

                        long num_massValueRounded = Math.round(num_massValue);
                        long den_massValueRounded = Math.round(den_massValue);

                        String HSI = Long.toString(num_massValueRounded) + "/" + Long.toString(den_massValueRounded);

                        HSIsArray[i] = HSI;
                        numThreshArray[i] = String.valueOf(numThresh);
                        denThreshArray[i] = String.valueOf(denThresh);
                        ratioScaleFactorArray[i] = String.valueOf(ratioScaleFactor);
                        maxRGBArray[i] = String.valueOf(maxRGB);
                        minRGBArray[i] = String.valueOf(minRGB);
                    }
                }
                 */
                //=======================================================================================
                // TO UNCOMMENT THIS AND HAVE IT WORKING THE RADIO BUTTONS SHOULD BE RESTORED AS WELL
                /*
                // we get the selected HSIs at the mimsTomography level ( the 5th tab of OpenMIMS )
                if (ui.getMimsTomography() != null && ui.getMimsTomography().getSelectedHSIImages() != null) {

                    String[] massNames = ui.getOpener().getMassNames();
                    String[] selectedHSIs = new String[ui.getMimsTomography().getSelectedHSIImages().size()];
                    ui.getMimsTomography().getSelectedHSIImages().toArray(selectedHSIs);

                    HSIsArray = new String[selectedHSIs.length];
                    numThreshArray = new String[selectedHSIs.length];
                    denThreshArray = new String[selectedHSIs.length];
                    ratioScaleFactorArray = new String[selectedHSIs.length];
                    maxRGBArray = new String[selectedHSIs.length];
                    minRGBArray = new String[selectedHSIs.length];

                    //----- Collect all the props we need for each HSI image.
                    for (int i = 0; i < selectedHSIs.length; i++) {
                        for(MimsPlus openedHSIimage: ui.getOpenHSIImages()){
                            if(selectedHSIs[i].equals(openedHSIimage.getRoundedTitle())){
                                String title = selectedHSIs[i].substring(selectedHSIs[i].indexOf(" ") + 1);
                                HSIsArray[i]             = title;
                                numThreshArray[i]        = Integer.toString(openedHSIimage.getHSIProcessor().getHSIProps().getNumThreshold());
                                denThreshArray[i]        = Integer.toString(openedHSIimage.getHSIProcessor().getHSIProps().getDenThreshold());
                                ratioScaleFactorArray[i] = Integer.toString((new Double(openedHSIimage.getHSIProcessor().getHSIProps().getRatioScaleFactor())).intValue());
                                maxRGBArray[i]           = Integer.toString((new Double(openedHSIimage.getHSIProcessor().getHSIProps().getMaxRatio())).intValue());
                                minRGBArray[i]           = Integer.toString((new Double(openedHSIimage.getHSIProcessor().getHSIProps().getMinRatio())).intValue());
                            }
                        }
                    }
                } 

                // USING THE SPECS COLLECTED FROM OPENMIMS
                if (openMimsSpecs_radioButton.isSelected()) {

                    String temp_folder_path = System.getProperty("java.io.tmpdir") + "/OpenMIMS_HTMLGEN_" + String.valueOf(System.currentTimeMillis());
                    File directory = new File(temp_folder_path);

                    // make directory in the system's temporary folder
                    boolean isMKDIR = directory.mkdir();

                    if (isMKDIR == false) {
                        System.out.println("Could't make a directory for the png file..EXITING...");
                        return;
                    }
                    // Initialize and run Converter object.
                    converter = new Converter(false, false, trackCheckBox.isSelected(), mass, null, "", "");
                    converter.setFiles(fileNames);
                    // We prepare/setup the props that the converter need in order
                    // to generate the html file.
                    converter.specsForHTMLThruSelectedImages(
                            selectedFile.getAbsolutePath(),
                            temp_folder_path,
                            HSIsArray,
                            numThreshArray, denThreshArray,
                            ratioScaleFactorArray,
                            maxRGBArray, minRGBArray);
                    converter.addPropertyChangeListener(this);
                    converter.execute();

                }
               
                //=======================================================================================
                 */
                if (allOpenedImages_radioButton.isSelected()) {

                    //MimsPlus[] openedMassImages = ui.getOpenMassImages();
                    MimsPlus[] openedSumImages = ui.getOpenSumImages();
                    MimsPlus[] openedRatioImages = ui.getOpenRatioImages();
                    MimsPlus[] openedHSIImages = ui.getOpenHSIImages();
                    MimsPlus[] openedCompositeImages = ui.getOpenCompositeImages();

                    /*
                    ArrayList<Integer> massImages      = new ArrayList<Integer>();
                    ArrayList<Integer> sumImages       = new ArrayList<Integer>();
                    ArrayList<Integer> ratioImages     = new ArrayList<Integer>();
                    ArrayList<Integer> hsiImages       = new ArrayList<Integer>();
                    ArrayList<Integer> compositeImages = new ArrayList<Integer>();

                    // we collect the mass images that have just one plane.
                    for(MimsPlus massImage: openedMassImages){
                        if(massImage.getSlice() == 1){
                            massImages.add(new Integer((int) Math.round(massImage.getMassValue())));
                        }
                    }
                    for(MimsPlus sumImage: openedSumImages){
                            sumImages.add(new Integer((int) Math.round(sumImage.getMassValue())));
                    }
                    for(MimsPlus ratioImage: openedRatioImages){
                            // to be populated
                    }
                    for(MimsPlus hsiImage: openedHSIImages){
                            // to be populated
                    }
                    for(MimsPlus compositeImage: openedCompositeImages){
                            // to be populated
                    }
                     */
                    //-----------------------------------------------------------
                    // Handling SUM images:
                    //-----------------------------------------------------------
                    int number_of_SUMs = openedSumImages.length;
                    String[] sumArray = new String[number_of_SUMs];
                    for (int i = 0; i < openedSumImages.length; i++) {
                        MimsPlus openedSumImage = openedSumImages[i];
                        String title = openedSumImage.getTitle().substring(openedSumImage.getTitle().indexOf(':') + 2, openedSumImage.getTitle().indexOf('['));
                        sumArray[i] = title;
                    }

                    //-----------------------------------------------------------
                    // Handling Ratio images:
                    //-----------------------------------------------------------
                    int number_of_Ratios = openedRatioImages.length;
                    String[] RatiosArray = new String[number_of_Ratios];
                    String[] ratioNumThreshArray = new String[number_of_Ratios];
                    String[] ratioDenThreshArray = new String[number_of_Ratios];
                    String[] r_ratioScaleFactorArray = new String[number_of_Ratios];

                    for (int i = 0; i < openedRatioImages.length; i++) {
                        MimsPlus openedRatioImage = openedRatioImages[i];
                        // title should be like 82.36/80.02
                        String title = openedRatioImage.getTitle().substring(0, openedRatioImage.getTitle().indexOf('['));
                        RatiosArray[i] = title;
                        openedRatioImage.getRatioProps().getNumThreshold();
                        ratioNumThreshArray[i] = Integer.toString(openedRatioImage.getRatioProps().getNumThreshold());
                        ratioDenThreshArray[i] = Integer.toString(openedRatioImage.getRatioProps().getDenThreshold());
                        r_ratioScaleFactorArray[i] = Integer.toString((new Double(openedRatioImage.getRatioProps().getRatioScaleFactor())).intValue());
                    }

                    //-----------------------------------------------------------
                    // Handling HSI images:
                    //-----------------------------------------------------------
                    int number_of_HSIs = openedHSIImages.length;
                    String[] HSIsArray = new String[number_of_HSIs];
                    String[] numThreshArray = new String[number_of_HSIs];
                    String[] denThreshArray = new String[number_of_HSIs];
                    String[] ratioScaleFactorArray = new String[number_of_HSIs];
                    String[] maxRGBArray = new String[number_of_HSIs];
                    String[] minRGBArray = new String[number_of_HSIs];

                    for (int i = 0; i < openedHSIImages.length; i++) {
                        MimsPlus openedHSIimage = openedHSIImages[i];
                        // title should be like 82.36/80.02
                        String title = openedHSIimage.getTitle().substring(openedHSIimage.getTitle().indexOf(':') + 2, openedHSIimage.getTitle().indexOf('['));
                        HSIsArray[i] = title;
                        numThreshArray[i] = Double.toString(openedHSIimage.getHSIProcessor().getHSIProps().getMaxRatio());
                        denThreshArray[i] = Double.toString(openedHSIimage.getHSIProcessor().getHSIProps().getMinRatio());
                        ratioScaleFactorArray[i] = Integer.toString((new Double(openedHSIimage.getHSIProcessor().getHSIProps().getRatioScaleFactor())).intValue());
                        maxRGBArray[i] = Integer.toString((new Double(openedHSIimage.getHSIProcessor().getHSIProps().getMaxRGB())).intValue());
                        minRGBArray[i] = Integer.toString((new Double(openedHSIimage.getHSIProcessor().getHSIProps().getMinRGB())).intValue());
                    }

                    //-----------------------------------------------------------
                    // Handling Composite images:
                    //-----------------------------------------------------------
                    int number_of_composites = openedCompositeImages.length;
                    String[] compositesArray = new String[number_of_composites];

                    String temp_folder_path = System.getProperty("java.io.tmpdir") + "/OpenMIMS_HTMLGEN_" + String.valueOf(System.currentTimeMillis());
                    File directory = new File(temp_folder_path);

                    // make directory in the system's temporary folder
                    boolean isMKDIR = directory.mkdir();

                    if (isMKDIR == false) {
                        System.out.println("Could't make a directory for the png file..EXITING...");
                        return;
                    }
                    // Initialize and run Converter object.
                    converter = new Converter(false, false, trackCheckBox.isSelected(), mass, null, "", "");
                    converter.setFiles(fileNames, false);
                    // We prepare/setup the props that the converter need in order
                    // to generate the html file.

                    converter.setForHtml(selectedFile.getAbsolutePath(), temp_folder_path);
                    converter.sumImageSpecsForHTML(sumArray);
                    converter.ratioImageSpecsForHTML(
                            RatiosArray,
                            ratioNumThreshArray,
                            ratioDenThreshArray,
                            r_ratioScaleFactorArray);
                    converter.hsiImageSpecsForHTML(
                            HSIsArray,
                            numThreshArray, denThreshArray,
                            ratioScaleFactorArray,
                            maxRGBArray, minRGBArray);

                    converter.addPropertyChangeListener(this);
                    converter.execute();

                } // USING SPECS IN THE CONFIG FILE
                else // IF THE CONFIG FILE IS VALID
                 if (configFile.getName().endsWith(".cfg") || configFile.getName().endsWith(".CFG")) {
                        // Initialize and run Converter object.
//                            configFile.getAbsolutePath(), "", "");
                        converter = new Converter(true, false, trackCheckBox.isSelected(), mass,
                                configFile.getAbsolutePath(), "", "");
                        converter.setFiles(fileNames, false);

                        // We indicate the html file thst the user have chosen to generate.
                        converter.specsForHtmlThruConfigFile(selectedFile.getAbsolutePath());

                        converter.addPropertyChangeListener(this);
                        converter.execute();

                    } // IN CASE THE CONFIG FILE IS NOT VALID
                    else {
                        IJ.error("CONFIG FILE ERROR MESSAGE",
                                "THE CONFIG FILE CHOSEN IS NOT VALID, PLEASE CHOOSE A VALID ONE.");
                    }
            }
            return;
        }

        // Initialize and run Converter object.  Do not attempt to read files that had been previously marked as
        // corrupted.
        int numStatus = fileStatusList.size();
        int numFiles = fileNames.size();
        if (numStatus != numFiles) {
            IJ.error("Unmated number of files and status message",
                    ".");
        }
        ListIterator<String> listIterator = fileNames.listIterator();
        ListIterator<FileHeaderCheckStatus> iter = fileStatusList.listIterator();
        while (listIterator.hasNext()) {
            String item = listIterator.next();
            //System.out.println(item);
            FileHeaderCheckStatus status = iter.next();
            if (status.openFailed || ((status.wasHeaderBad) && (!status.wasHeaderFixed))) {
                listIterator.remove();
                iter.remove();
            }
        }

        converter = new Converter(false, false, trackCheckBox.isSelected(), mass, null, "", "");
        onlyReadHeader = false;
        converter.setFiles(fileNames, onlyReadHeader);
        converter.addPropertyChangeListener(this);
        converter.execute();

    }//GEN-LAST:event_okButtonActionPerformed

    private void trackCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trackCheckBoxActionPerformed
        if (trackCheckBox.isSelected()) {
            populateMassListComboBox();
        } else {
            //massTextField.setEditable(false);
            massComboBox.setVisible(false);
        }
    }//GEN-LAST:event_trackCheckBoxActionPerformed

    private void selectFilesButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectFilesButton1ActionPerformed
        // DJ:
        int indx = fileListComboBox.getSelectedIndex();
        if (indx == -1) {
            return;
        }
        fileListComboBox.removeItemAt(indx);
        fileNames.remove(indx); // DJ
    }//GEN-LAST:event_selectFilesButton1ActionPerformed

    private void configFile_radioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configFile_radioButtonActionPerformed
        // TODO add your handling code here:

        choose_config_Button.setEnabled(true);
        config_file_label.setEnabled(true);
    }//GEN-LAST:event_configFile_radioButtonActionPerformed

    private void choose_config_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_choose_config_ButtonActionPerformed
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser(".");

        // At this point, we just accept the config files type: "cfg",
        FileFilter filter = new ExtensionFileFilter("cfg", new String("cfg"));
        chooser.setFileFilter(filter);

        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnVal = chooser.showOpenDialog(this);

        if (returnVal == JFileChooser.CANCEL_OPTION) {
            chooser.setVisible(false);
            this.setEnabled(true);
            this.setVisible(true);
        }
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            configFile = chooser.getSelectedFile();
            config_file_label.setText(configFile.getName());
        }

    }//GEN-LAST:event_choose_config_ButtonActionPerformed

    private void populateMassListComboBox() {
        massComboBox.setMaximumRowCount(20);   // We don't need no stinking scrollbars.
        FileHeaderCheckStatus status = null;
        int selectedIndex = fileListComboBox.getSelectedIndex();
        //massTextField.setEditable(true);
        if (fileListComboBox.getItemCount() > 0) {
            if ((converter != null) && (trackCheckBox.isSelected())) {
                fileStatusList = converter.getFileOpenStatusList();
                if (fileStatusList.size() != 0) {
                    status = fileStatusList.get(selectedIndex);
                    ArrayList<String> names = status.getImageNames();
                    ArrayList<String> symbols = status.getImageSymbols();
                    ArrayList<String> series = status.getImageSeries();

                    ListIterator<String> namesIter = names.listIterator();
                    ListIterator<String> symbolsIter = symbols.listIterator();
                    ListIterator<String> seriesIter = series.listIterator();
                    ArrayList<String> masses = new ArrayList<String>(names.size());
                    massComboBox.removeAllItems();
                    while (namesIter.hasNext()) {
                        String seriesStr = seriesIter.next();
                        String nameStr = namesIter.next();
                        String symbolStr = symbolsIter.next();
                        String mass = "(" + seriesStr + ")  " + nameStr + " " + symbolStr;
                        massComboBox.addItem(mass);
                        masses.add(mass);
                    }
                } else {
                    System.out.println("dammit.  fileStatusList is empty.");
                }

                if (trackCheckBox.isSelected()) {
                    massComboBox.setVisible(true);
                }
            }
        }
    }

    private void fileListComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_fileListComboBoxItemStateChanged
        // This gets fired twice.  Once for deselecting old item, and once for the newly selected item.
        if (evt.getStateChange() == evt.DESELECTED) {
//            if (trackCheckBox.isSelected()) {
//                populateMassListComboBox();  
//            }
        }
        if (evt.getStateChange() == evt.SELECTED) {
            if (trackCheckBox.isSelected()) {
                populateMassListComboBox();
            }
        } else if (evt.getStateChange() == evt.DESELECTED) {
            int i = 1;
        }

    }//GEN-LAST:event_fileListComboBoxItemStateChanged

    private void massComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_massComboBoxItemStateChanged
        // TODO add your handling code here:
        if (evt.getStateChange() == evt.SELECTED) {
            String massStr = (String) evt.getItem();
            int start = massStr.indexOf("  ") + 2;  // +2 to skip over the two spaces
            int end = massStr.indexOf(" ", start);
            mass = massStr.substring(start, end);
        } else if (evt.getStateChange() == evt.DESELECTED) {
            // Don't need to do anything on deselect  
        }
    }//GEN-LAST:event_massComboBoxItemStateChanged

    /**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
        } else if (evt.getPropertyName().matches("state") && evt.getNewValue().toString().matches("DONE")) {
            setCursor(null);
            boolean enableOKButton = false;
            if (onlyReadHeader) {
                // Get file status for all files, and don't call close, which will get rid of the dialog.
                fileStatusList = converter.getFileOpenStatusList();
                //System.out.println("fileStatusList size is " + fileStatusList.size());
                int numItems = fileStatusList.size();
                Color[] colors = new Color[numItems];
                boolean badHeaders = false;
                for (int i = 0; i < numItems; i++) {
                    if (!(fileStatusList.get(i).wasHeaderBad) && !(fileStatusList.get(i).wasHeaderFixed)) {
                        colors[i] = Color.BLUE;
                    } else if ((fileStatusList.get(i).wasHeaderBad) && (fileStatusList.get(i).wasHeaderFixed)) {
                        colors[i] = Color.GREEN;
                        badHeaders = true;
                    } else if ((fileStatusList.get(i).wasHeaderBad) && !(fileStatusList.get(i).wasHeaderFixed)) {
                        colors[i] = Color.RED;
                        badHeaders = true;
                    } else if (fileStatusList.get(i).openFailed) {
                        colors[i] = Color.RED;
                    }
                }
                if (badHeaders) {
                    jLabelFilesHadBadHeaders.setVisible(true);
                }

                String[] filesStr = new String[numItems];
                ListIterator<String> iterator = fileNames.listIterator();
                int i = 0;
                while (iterator.hasNext()) {
                    String theFile = iterator.next();
                    filesStr[i] = theFile;
                    i++;
                }

                renderer.setStrings(filesStr);
                renderer.setColors(colors);
                fileListComboBox.setRenderer(renderer);
                // Added a code block here to check if any good files have been selected.  If none, diable 
                // the OK button.
                ListIterator<FileHeaderCheckStatus> iter = fileStatusList.listIterator();
                while (iter.hasNext()) {
                    FileHeaderCheckStatus status = iter.next();
                    if (status.openFailed || ((status.wasHeaderBad) && (!status.wasHeaderFixed))) {
                    } else {
                        enableOKButton = true;
                    }
                }

            } else {
                okButton.setEnabled(true);
                close();
                //converter.cancel(true);
            }
            okButton.setEnabled(enableOKButton);
        }

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton allOpenedImages_radioButton;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton choose_config_Button;
    private javax.swing.JRadioButton configFile_radioButton;
    private javax.swing.JLabel config_file_label;
    private javax.swing.JComboBox fileListComboBox;
    private javax.swing.JLabel jLabelFilesHadBadHeaders;
    private javax.swing.JComboBox massComboBox;
    private javax.swing.JButton okButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton selectFilesButton;
    private javax.swing.JButton selectFilesButton1;
    private javax.swing.JCheckBox trackCheckBox;
    // End of variables declaration//GEN-END:variables

    private void close() {
        setVisible(false);
    }
}

class ComboBoxRenderer extends JPanel implements ListCellRenderer {

    private static final long serialVersionUID = -1L;
    private Color[] colors;
    private String[] strings;

    JPanel textPanel;
    JLabel text;

    public ComboBoxRenderer(JComboBox combo) {
        textPanel = new JPanel();
        textPanel.add(this);
        text = new JLabel();
        text.setOpaque(true);
        text.setFont(combo.getFont());
        textPanel.add(text);
    }

    public void setColors(Color[] col) {
        colors = col;
    }

    public void setStrings(String[] str) {
        strings = str;
    }

    public Color[] getColors() {
        return colors;
    }

    public String[] getStrings() {
        return strings;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {

        if (isSelected) {
            setBackground(list.getSelectionBackground());
        } else {
            setBackground(Color.WHITE);
        }

        text.setBackground(getBackground());

        text.setText(value.toString());
        if (index > -1) {
            text.setForeground(colors[index]);
        }
        return text;
    }
}

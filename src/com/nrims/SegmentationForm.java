package com.nrims;

import com.nrims.segmentation.*;
import ij.gui.Roi;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 *
 * @author  sreckow
 */
public class SegmentationForm extends javax.swing.JPanel implements java.beans.PropertyChangeListener {

    private static final int NO_TASK = -1,  TRAIN_TASK = 0,  PRED_TASK = 1,  TRAINFEAT_TASK = 2,  PREDFEAT_TASK = 3,  ROI_TASK = 4,  EXPORT_TASK = 5;

    // TODO
    // ### HARD CODED ###
    private final int[][] RATIOS = {{1, 0}, {3, 2}}; // ratio images 13/12 and 27/26
    public static Hashtable<String, String[]> implementedEngines;
    // #######
    private int activeTask;
    private SegmentationEngine activeEngine;
    private String activeClass;
    private SegUtils segUtil;
    private MimsRoiManager roiManager;
    private UI mimsUi;
    // members for current model setup
    private ClassManager trainClasses;
    private boolean[] massImageFeatures;
    private boolean[] ratioImageFeatures;
    private int[] localFeatures;
    private int colorImageIndex;
    private SegmentationProperties properties;
//    private String modelFile;

    // members for current segmentation
    private ClassManager predClasses;
    private byte[] classification;
    private int[] classColors;
    private String[] classNames;

    public SegmentationForm(UI ui) {
        initComponents();
        loadPredictionButton.setVisible(false);
        savePredictionButton.setVisible(false);
        exportButton.setVisible(false);

        mimsUi = ui;
        roiManager = mimsUi.getRoiManager();

        progressBar.setMinimum(0);
        progressBar.setMaximum(100);

        // TO DO
        // ## HARD CODED ### this should be read from a config file
        implementedEngines = new Hashtable<String, String[]>();
        implementedEngines.put("lib-SVM", new String[]{"SVM.SvmEngine", "SVM.SVMSetupDialog"});
        // #########

        resetForm();
    }

    public void resetForm() {
        activeClass = "";
        workLabel.setText("");
        trainClasses = new ClassManager();
        predClasses = new ClassManager();
        classesCombo.removeAllItems();
        activeTask = NO_TASK;

        // reset current model setup
        properties = new SegmentationProperties();
        properties.setValueOf(SegmentationEngine.ENGINE, "lib-SVM");
        massImageFeatures = new boolean[]{true, false, true, true};
        ratioImageFeatures = new boolean[]{false, true};
        localFeatures = new int[]{1, 1, 1};
        colorImageIndex = 3;
        // modelFile = "";

        // reset current classification
        classification = new byte[0];
        classColors = new int[0];
        classNames = new String[0];

        minAreaText.setText("100");
        updateControls();  // reset controls
        updateModelInfo(); // reset model info
    }

    private MimsPlus[] getImages() {
        ArrayList<MimsPlus> images = new ArrayList<MimsPlus>();

        // get mass images
        for (int i = 0; i < massImageFeatures.length; i++) {
            if (massImageFeatures[i]) {
                images.add(mimsUi.getMassImage(i));
            }
        }

        // get ratio images

        for (int i = 0; i < ratioImageFeatures.length; i++) {
            if (ratioImageFeatures[i]) {
                int num = RATIOS[i][0];
                int den = RATIOS[i][1];
                int index = mimsUi.getRatioImageIndex(num, den);
                if (index == -1) {
                    // create ratio image
                    HSIProps props = new HSIProps();
                    props.setNumMass(num);
                    props.setDenMass(den);
                    mimsUi.computeRatio(props, true);
                    index = mimsUi.getRatioImageIndex(num, den);
                    if (index == -1) {
                        System.out.println("Error computing ratio image!");
                        return null;
                    }
                }
                images.add(mimsUi.getRatioImage(index));
            }
        }
        return images.toArray(new MimsPlus[images.size()]);
    }

    private void startTraining(ArrayList<ArrayList<ArrayList<Double>>> trainData) {
        // a new engine instance is needed for each execution as SwingWorker objects are non-reusable
        try {
            String engineName = implementedEngines.get(properties.getValueOf(SegmentationEngine.ENGINE))[0];
            Class engineClass = Class.forName(engineName);
            Class[] params = {Integer.TYPE, ArrayList.class, SegmentationProperties.class};
            java.lang.reflect.Constructor con = engineClass.getConstructor(params);
            activeEngine = (SegmentationEngine) con.newInstance(SegmentationEngine.TRAIN_TYPE, trainData, properties);
            activeEngine.addPropertyChangeListener(this);
            activeTask = TRAIN_TASK;
            activeEngine.execute();
        } catch (Exception e) {
            System.out.println("Error: engine could not be instantiated!");
            e.printStackTrace();
        }
    }

    private void startPrediction(ArrayList<ArrayList<ArrayList<Double>>> predData) {
        // a new engine instance is needed for each execution as SwingWorker objects are non-reusable
        try {
            String engineName = implementedEngines.get(properties.getValueOf(SegmentationEngine.ENGINE))[0];
            Class engineClass = Class.forName(engineName);
            Class[] params = {Integer.TYPE, ArrayList.class, SegmentationProperties.class};
            java.lang.reflect.Constructor con = engineClass.getConstructor(params);
            activeEngine = (SegmentationEngine) con.newInstance(SegmentationEngine.PRED_TYPE, predData, properties);
            activeEngine.addPropertyChangeListener(this);
            activeTask = PRED_TASK;
            activeEngine.execute();
        } catch (Exception e) {
            System.out.println("Error: engine could not be instantiated!");
            e.printStackTrace();
        }
    }

    public void updateControls() {
        boolean working = (segUtil != null && !segUtil.isDone() || activeEngine != null && !activeEngine.isDone());
        boolean trained = properties.getValueOf(SegmentationEngine.MODEL) != null;
        boolean predicted = classification != null && classification.length > 0;
        boolean trainSet = trainClasses != null && trainClasses.getClasses().length > 0;

        resetButton.setEnabled(!working);
        modifyButton.setEnabled(!working);
        loadModelButton.setEnabled(!working);
        saveModelButton.setEnabled(!working);
        trainButton.setEnabled(!working && trainSet);
        predictButton.setEnabled(!working && trained);
        roiButton.setEnabled(!working && predicted);
        displayButton.setEnabled(!working && predicted);
//        savePredictionButton.setEnabled(!working && predicted);
//        loadPredictionButton.setEnabled(!working);
       
    }

    private boolean fillBox() {
        classesCombo.removeAllItems();
        for (String className : predClasses.getClasses()) {
            classesCombo.addItem(className);
        }
        return classesCombo.getItemCount() > 0;
    }

    private void setActiveClass(String className) {
        // remove ROIs from the RoiManager, if required
        if (roiManager.jlist.getModel().getSize() > 0) {
            roiManager.selectAll();
            roiManager.delete();
            roiManager.getImage().setRoi((Roi) null);
        }
        // set the current class and add ROIs to the roiManager
        activeClass = className;
        for (SegRoi segRoi : predClasses.getRois(activeClass)) {
            roiManager.getImage().setRoi(segRoi.getRoi());
            roiManager.add();
        }
    }

    private void updateModelInfo() {
        String desc = "";
        desc += "ENGINE\n";
        desc += "name: " + properties.getValueOf(SegmentationEngine.ENGINE) + "\n";
        if (properties.getValueOf(SegmentationEngine.MODEL) == null) {
            desc += "status: untrained\n";
        } else {
            desc += "status: trained\n";
        }

        desc += "\nCLASSES\n";
        for (String classname : trainClasses.getClasses()) {
            desc += classname + "\n";
        }

        desc += "\nIMAGES\n";
        for (int i = 0; i < massImageFeatures.length; i++) {
            if (massImageFeatures[i]) {
                desc += mimsUi.getOpener().getMassName(i) + "\n";
            }
        }
        for (int i = 0; i < ratioImageFeatures.length; i++) {
            if (ratioImageFeatures[i]) {
                desc += mimsUi.getOpener().getMassName(RATIOS[i][0]) + "/" + mimsUi.getOpener().getMassName(RATIOS[i][1]) + "\n";
            }
        }
        desc += "\nLOCAL FEATURES\n";
        if (localFeatures[0] == 1) {
            desc += "neighborhood statistics\n";
        }
        if (localFeatures[1] == 1) {
            desc += "approximated gradient\n";
        }
        if (localFeatures[0] == 1 || localFeatures[1] == 1) {
            desc += "neighborhood size: " + localFeatures[2] + "\n";
        }

        descriptionText.setText(desc);
        descriptionText.setCaretPosition(0);
    }

    public void setClassColors(int[] classColors) {
        this.classColors = classColors;
    }

    public void setClassNames(String[] classNames) {
        this.classNames = classNames;
    }

    public void setTrainClasses(ClassManager classes) {
        this.trainClasses = classes;
    }

    public void setPredClasses(ClassManager classes) {
        this.predClasses = classes;
        fillBox();
    }

    public void setClassification(byte[] classification) {
        this.classification = classification;
    }

    public void setProperties(SegmentationProperties properties) {
        this.properties = properties;
        updateModelInfo();
    }

    public SegmentationProperties getProperties() {

        return this.properties;
    }

    public void setLocalFeatures(int[] localFeatures) {
        this.localFeatures = localFeatures;
    }

    public void setColorImageIndex(int colorImageIndex) {
        this.colorImageIndex = colorImageIndex;
    }

    public void setMassImageFeatures(boolean[] massImageFeatures) {
        this.massImageFeatures = massImageFeatures;
    }

    public void setRatioImageFeatures(boolean[] ratioImageFeatures) {
        this.ratioImageFeatures = ratioImageFeatures;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        modelPanel = new javax.swing.JPanel();
        modifyButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        descriptionText = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        trainButton = new javax.swing.JButton();
        exportButton = new javax.swing.JButton();
        predictionPanel = new javax.swing.JPanel();
        predictButton = new javax.swing.JButton();
        roiButton = new javax.swing.JButton();
        displayButton = new javax.swing.JButton();
        savePredictionButton = new javax.swing.JButton();
        loadPredictionButton = new javax.swing.JButton();
        classesCombo = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        minAreaText = new javax.swing.JTextField();
        progressBar = new javax.swing.JProgressBar();
        workLabel = new javax.swing.JLabel();
        resetButton = new javax.swing.JButton();
        loadModelButton = new javax.swing.JButton();
        saveModelButton = new javax.swing.JButton();

        modelPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Model Configuration", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        modifyButton.setText("Config");
        modifyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modifyButtonActionPerformed(evt);
            }
        });

        descriptionText.setColumns(20);
        descriptionText.setEditable(false);
        descriptionText.setRows(5);
        jScrollPane1.setViewportView(descriptionText);

        jLabel2.setText("Model description");

        trainButton.setText("Train");
        trainButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trainButtonActionPerformed(evt);
            }
        });

        exportButton.setText("Export");
        exportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout modelPanelLayout = new javax.swing.GroupLayout(modelPanel);
        modelPanel.setLayout(modelPanelLayout);
        modelPanelLayout.setHorizontalGroup(
            modelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modelPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(modelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(modelPanelLayout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(modelPanelLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(70, 70, 70))
                    .addGroup(modelPanelLayout.createSequentialGroup()
                        .addComponent(trainButton, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(modifyButton, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(exportButton, javax.swing.GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        modelPanelLayout.setVerticalGroup(
            modelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modelPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(modelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(trainButton)
                    .addComponent(modifyButton)
                    .addComponent(exportButton))
                .addContainerGap())
        );

        predictionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Prediction", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        predictButton.setText("Predict");
        predictButton.setPreferredSize(new java.awt.Dimension(57, 23));
        predictButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                predictButtonActionPerformed(evt);
            }
        });

        roiButton.setText("ROIs");
        roiButton.setEnabled(false);
        roiButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                roiButtonActionPerformed(evt);
            }
        });

        displayButton.setText("Display");
        displayButton.setActionCommand("loadROIs");
        displayButton.setEnabled(false);
        displayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayButtonActionPerformed(evt);
            }
        });

        savePredictionButton.setText("Save");
        savePredictionButton.setEnabled(false);
        savePredictionButton.setRequestFocusEnabled(false);
        savePredictionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                savePredictionButtonActionPerformed(evt);
            }
        });

        loadPredictionButton.setText("Load");
        loadPredictionButton.setEnabled(false);
        loadPredictionButton.setRequestFocusEnabled(false);
        loadPredictionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadPredictionButtonActionPerformed(evt);
            }
        });

        classesCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                classesComboItemStateChanged(evt);
            }
        });

        jLabel1.setText("current class");

        jLabel3.setText("min area");

        javax.swing.GroupLayout predictionPanelLayout = new javax.swing.GroupLayout(predictionPanel);
        predictionPanel.setLayout(predictionPanelLayout);
        predictionPanelLayout.setHorizontalGroup(
            predictionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, predictionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(predictionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(classesCombo, 0, 249, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, predictionPanelLayout.createSequentialGroup()
                        .addComponent(predictButton, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(displayButton)
                        .addGap(18, 18, 18)
                        .addComponent(roiButton, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(predictionPanelLayout.createSequentialGroup()
                        .addGroup(predictionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(savePredictionButton, javax.swing.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)
                            .addComponent(loadPredictionButton, javax.swing.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE))
                        .addGap(26, 26, 26)
                        .addGroup(predictionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(minAreaText, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        predictionPanelLayout.setVerticalGroup(
            predictionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(predictionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(predictionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(predictButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(displayButton)
                    .addComponent(roiButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(predictionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(predictionPanelLayout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(minAreaText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(predictionPanelLayout.createSequentialGroup()
                        .addComponent(loadPredictionButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(savePredictionButton)))
                .addGap(53, 53, 53)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(classesCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        workLabel.setText("working");

        resetButton.setText("Reset");
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });

        loadModelButton.setText("Load");
        loadModelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadModelButtonActionPerformed(evt);
            }
        });

        saveModelButton.setText("Save");
        saveModelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveModelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(loadModelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(saveModelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(resetButton, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(modelPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(workLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                                .addGap(85, 85, 85))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(predictionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {loadModelButton, resetButton, saveModelButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(predictionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(modelPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(59, 59, 59)
                .addComponent(workLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(loadModelButton)
                        .addComponent(saveModelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(resetButton))
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void trainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trainButtonActionPerformed
        // TODO warn if current model is already trained
        MimsPlus[] images = getImages();
        activeTask = TRAINFEAT_TASK;
        segUtil = SegUtils.initPrepSeg(images, colorImageIndex, localFeatures, trainClasses);
        segUtil.addPropertyChangeListener(this);
        segUtil.execute();
    }//GEN-LAST:event_trainButtonActionPerformed

    private void classesComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_classesComboItemStateChanged
        if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
            String className = (String) classesCombo.getSelectedItem();
            if (!className.equals(activeClass)) {
                setActiveClass(className);
            }
        }
}//GEN-LAST:event_classesComboItemStateChanged

    private void modifyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modifyButtonActionPerformed



        SegmentationSetupForm setup = new SegmentationSetupForm(mimsUi, this, roiManager, trainClasses, massImageFeatures,
                ratioImageFeatures, localFeatures, colorImageIndex, properties, mimsUi.getOpener().getMassNames());
        mimsUi.setState(java.awt.Frame.ICONIFIED);
        setup.setVisible(true);
}//GEN-LAST:event_modifyButtonActionPerformed

    private void saveModelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveModelButtonActionPerformed
        String defaultPath = mimsUi.getOpener().getImageFile().getParent() + System.getProperty("file.separator") + "model.MODEL.zip";
        JFileChooser fc = new JFileChooser(defaultPath);
        fc.setSelectedFile(new java.io.File(defaultPath));
        while (true) {
            if (fc.showSaveDialog(this) == JFileChooser.CANCEL_OPTION) {
                break;
            }
            String filePath = fc.getSelectedFile().getPath();
            String fileName = fc.getSelectedFile().getName();
            if (new java.io.File(filePath).exists()) {
                String[] options = {"Overwrite", "Cancel"};
                int value = JOptionPane.showOptionDialog(this, "File \"" + fileName + "\" already exists!", null,
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                if (value == JOptionPane.NO_OPTION) {
                    continue;
                }
            }
            //SegFileUtils.saveModel(filePath, trainClasses, classColors, properties,
            //           massImageFeatures, ratioImageFeatures, localFeatures);
            SegFileUtils.save(filePath, properties, trainClasses, classColors, colorImageIndex, massImageFeatures, ratioImageFeatures,
                    localFeatures, classification, predClasses);
            break;
        }
    }//GEN-LAST:event_saveModelButtonActionPerformed

    private void loadModelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadModelButtonActionPerformed
        String defaultPath = mimsUi.getOpener().getImageFile().getParent() + System.getProperty("file.separator") + ".";
        JFileChooser fc = new JFileChooser(defaultPath);
        fc.setSelectedFile(new java.io.File(defaultPath));
        if (fc.showOpenDialog(this) != JFileChooser.CANCEL_OPTION) {
            resetForm();
//            SegFileUtils.loadModel(fc.getSelectedFile().getPath(), this);
//            updateModelInfo();
//            modelFile = fc.getSelectedFile().getPath();
//            trained = (properties.getValueOf(SegmentationProperties.MODEL) != null);
//            updateControls();

            SegFileUtils.load(fc.getSelectedFile().getPath(), this);
            updateModelInfo();
            updateControls();
        }
    }//GEN-LAST:event_loadModelButtonActionPerformed

    private void predictButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_predictButtonActionPerformed
        MimsPlus[] images = getImages();
        activeTask = PREDFEAT_TASK;
        segUtil = SegUtils.initPrepSeg(images, colorImageIndex, localFeatures);
        segUtil.addPropertyChangeListener(this);
        segUtil.execute();
    }//GEN-LAST:event_predictButtonActionPerformed

    private void savePredictionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_savePredictionButtonActionPerformed
//        String defaultPath = mimsUi.getOpener().getImageFile().getParent() + System.getProperty("file.separator") + "segResult.zip";
//        JFileChooser fc = new JFileChooser(defaultPath);
//        fc.setSelectedFile(new java.io.File(defaultPath));
//        while(true){
//            if(fc.showSaveDialog(this) == JFileChooser.CANCEL_OPTION ) break;
//            String filePath = fc.getSelectedFile().getPath();
//            String fileName = fc.getSelectedFile().getName();
//            if(new java.io.File(filePath).exists()){
//                String[] options = {"Overwrite","Cancel"};
//                int value = JOptionPane.showOptionDialog(this,"File \"" + fileName + "\" already exists!",null,
//                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,null,options, options[1]);
//                if(value == JOptionPane.NO_OPTION) continue;
//            }
//            SegFileUtils.savePrediction(filePath, predClasses, classification, modelFile);
//            break;
//        }
    }//GEN-LAST:event_savePredictionButtonActionPerformed

    private void loadPredictionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadPredictionButtonActionPerformed
//        String defaultPath = mimsUi.getOpener().getImageFile().getParent() + System.getProperty("file.separator") + ".";
//        JFileChooser fc = new JFileChooser(defaultPath);
//        fc.setSelectedFile(new java.io.File(defaultPath));
//        if(fc.showOpenDialog(this) != JFileChooser.CANCEL_OPTION ){
//            resetForm();
//            SegFileUtils.loadPrediction(fc.getSelectedFile().getPath(), this);
//            updateModelInfo();
//            modelFile = fc.getSelectedFile().getPath();
//            predicted = true;
//            trained = (properties.getValueOf(SegmentationProperties.MODEL) != null);
//            updateControls();
//        }
    }//GEN-LAST:event_loadPredictionButtonActionPerformed

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonActionPerformed
        int value = JOptionPane.showConfirmDialog(this, "Reset form to default settings?", "Confirm Reset",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (value == JOptionPane.OK_OPTION) {
            resetForm();
        }
}//GEN-LAST:event_resetButtonActionPerformed

    private void displayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayButtonActionPerformed
        int[] pixels = new int[classification.length];
        for (int i = 0; i < pixels.length; i++) {
            // get the color value for each pixel according to its assigned class
            pixels[i] = classColors[classification[i]];
        }
        mimsUi.openSeg(pixels, "Segmentation result:", mimsUi.getOpener().getHeight(), mimsUi.getOpener().getWidth());
    }//GEN-LAST:event_displayButtonActionPerformed

    private void roiButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_roiButtonActionPerformed
        activeTask = ROI_TASK;
        // TODO
        // height & width are taken from the current mims image,
        // but should be taken from (and thus stored with) the classification
        int minArea;
        try {
            minArea = Integer.parseInt(minAreaText.getText());
            if (minArea <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please specify a positive integer value for \"min area\"!", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        segUtil = SegUtils.initCalcRoi(mimsUi.getOpener().getHeight(), mimsUi.getOpener().getWidth(), classNames, classification, minArea, (int) Double.POSITIVE_INFINITY);
        segUtil.addPropertyChangeListener(this);
        segUtil.execute();
    }//GEN-LAST:event_roiButtonActionPerformed

    private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
        MimsPlus[] images = getImages();
        activeTask = EXPORT_TASK;
        segUtil = SegUtils.initPrepSeg(images, colorImageIndex, localFeatures, trainClasses);
        segUtil.addPropertyChangeListener(this);
        segUtil.execute();
}//GEN-LAST:event_exportButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox classesCombo;
    private javax.swing.JTextArea descriptionText;
    private javax.swing.JButton displayButton;
    private javax.swing.JButton exportButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton loadModelButton;
    private javax.swing.JButton loadPredictionButton;
    private javax.swing.JTextField minAreaText;
    private javax.swing.JPanel modelPanel;
    private javax.swing.JButton modifyButton;
    private javax.swing.JButton predictButton;
    private javax.swing.JPanel predictionPanel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton resetButton;
    private javax.swing.JButton roiButton;
    private javax.swing.JButton saveModelButton;
    private javax.swing.JButton savePredictionButton;
    private javax.swing.JButton trainButton;
    private javax.swing.JLabel workLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void propertyChange(java.beans.PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
        } else if ("state".equals(evt.getPropertyName())) {
            switch (activeTask) {
                case TRAIN_TASK:
                    if (evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.STARTED)) {
                        workLabel.setText("training model...");
                        progressBar.setValue(activeEngine.getProgress());
                    } else if (evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.DONE)) {
                        workLabel.setText("done");
                        progressBar.setValue(0);
                        activeTask = NO_TASK;
                        if (activeEngine.getSuccess()) {
                            // TODO display model info
                            setProperties(activeEngine.getProperties()); //also updates model info
                        }
                    }
                    break;
                case PRED_TASK:
                    if (evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.STARTED)) {
                        workLabel.setText("predicting...");
                        progressBar.setValue(activeEngine.getProgress());
                    } else if (evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.DONE)) {
                        activeTask = NO_TASK;
                        workLabel.setText("done");
                        progressBar.setValue(0);
                        if (activeEngine.getSuccess() && activeEngine.getClassification() != null) {
                            byte[] tmpClassification = activeEngine.getClassification();
                            classification = tmpClassification;
                        } else {
                            // TODO error handling
                            workLabel.setText("Error: segmentation failed!");
                        }
                    }
                    break;
                case TRAINFEAT_TASK:
                    if (evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.STARTED)) {
                        workLabel.setText("extracting features for training...");
                        progressBar.setValue(segUtil.getProgress());
                    } else if (evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.DONE)) {
                        activeTask = NO_TASK;
                        if (segUtil.getSuccess()) {
                            workLabel.setText("done");
                            progressBar.setValue(0);
                            ArrayList<ArrayList<ArrayList<Double>>> trainData = segUtil.getData();
                            classColors = segUtil.getClassColors();
                            startTraining(trainData);
                        } else { // TODO error handling
                            workLabel.setText("Error: feature extraction failed!");
                        }
                    }
                    break;
                case PREDFEAT_TASK:
                    if (evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.STARTED)) {
                        workLabel.setText("extracting features for prediction...");
                        progressBar.setValue(segUtil.getProgress());
                    } else if (evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.DONE)) {
                        activeTask = NO_TASK;
                        if (segUtil.getSuccess()) {
                            workLabel.setText("done");
                            progressBar.setValue(0);
                            ArrayList<ArrayList<ArrayList<Double>>> predData = segUtil.getData();
                            startPrediction(predData);
                        } else { // TODO error handling
                            workLabel.setText("Error: feature extraction failed!");
                        }
                    }
                    break;
                case ROI_TASK:
                    if (evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.STARTED)) {
                        workLabel.setText("calculating ROIs...");
                        progressBar.setValue(segUtil.getProgress());
                    } else if (evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.DONE)) {
                        activeTask = NO_TASK;
                        ClassManager cm = segUtil.getClasses();
                        if (segUtil.getSuccess() && cm != null && cm.getClasses().length > 0) {
                            workLabel.setText("done");
                            progressBar.setValue(0);
                            predClasses = cm;
                            fillBox();
                            setActiveClass(predClasses.getClasses()[0]); // set first class as active class
                        } else { // TODO error handling
                            workLabel.setText("calculating ROIs failed!");
                        }
                    }
                    break;
            }
        }
        updateControls();
    }
}

package com.nrims;

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
    private SegmentationEngine activeEngine;
    private ClassManager activeClasses;
    private ClassManager trainClasses;
    private ClassManager predClasses;        
    private String activeClass;
    private int activeTask;
    private SegUtils segUtil;
    private MimsRoiManager roiManager;    
    private Hashtable<String,String> implementedEngines;
    private UI mimsUi;
    
    // members describing current segmentation
    private byte[] classification;
    private int[] classColors;
    private String[] classNames;
    private int height;
    private int width;
    
    private static final int NONE_TASK = -1, SEG_TASK = 0, FEAT_TASK = 1, ROI_TASK = 2; 
           
    public SegmentationForm(UI ui) {
        initComponents();
        mimsUi = ui;
        roiManager = mimsUi.getRoiManager();
        buttonGroup = new javax.swing.ButtonGroup();        
        buttonGroup.add(trainRButton);
        buttonGroup.add(predRButton);
        
        implementedEngines = new Hashtable<String,String>();
        implementedEngines.put("lib-SVM", "SVM.SvmEngine");
        for(String engine:implementedEngines.keySet()) enginesCombo.addItem(engine);
        
        resetForm();
    }
    
    private boolean fillBox(){
        classesCombo.removeAllItems();        
        for(String className : activeClasses.getClasses()){
            classesCombo.addItem(className);
        }
        return classesCombo.getItemCount()>0;
    }
    
    private void resetForm(){
        activeClass = "";
        trainRButton.doClick();
        predRButton.setEnabled(false);
        workLabel.setText("");
        activeClasses = trainClasses = new ClassManager();
        predClasses = new ClassManager();        
        classesCombo.removeAllItems();
        runButton.setEnabled(false);
        displayButton.setEnabled(false);
        roiButton.setEnabled(false);
        activeTask = NONE_TASK;
        
        // reset current classification
        classification = new byte[0];
        classColors = new int[0];
        classNames = new String[0];
        height = 0;
        width = 0;
    }
    
    private void setActiveClass(String className){
        // remove ROIs from the RoiManager, if required
        if(roiManager.list.getItemCount()>0){
            roiManager.selectAll();
            roiManager.delete(false);
            roiManager.getImage().setRoi((Roi)null);
        }
        // set the current class and add ROIs to the roiManager
        activeClass = className;
        for(SegRoi segRoi : activeClasses.getRois(activeClass)){
            roiManager.getImage().setRoi(segRoi.getRoi());        
            roiManager.add(false);
        }
    }
    
    public void propertyChange(java.beans.PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
        }else if ("state".equals(evt.getPropertyName())) {
            switch(activeTask){
                case SEG_TASK:
                    if(evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.STARTED)){
                        // setup progress bar
                        runButton.setText("cancel");
                        workLabel.setText("running segmentation...");                        
                        progressBar.setMinimum(activeEngine.getMinWork());
                        progressBar.setMaximum(activeEngine.getMaxWork());
                        progressBar.setValue(activeEngine.getMinWork());
                    }else if(evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.DONE)){
                        activeTask = NONE_TASK;
                        runButton.setText("run");
                        this.runButton.setText("run");
                        progressBar.setValue(100);
                        byte[] tmpClassification = activeEngine.getClassification();
                        if(tmpClassification.length > 0){
                            workLabel.setText("done");
                            classification = tmpClassification;
                            displayButton.setEnabled(true);
                            roiButton.setEnabled(true);
                        }else{
                            // TODO error handling
                            System.out.println("Error: segmentation failed!");
                            this.runButton.setText("run");
                            progressBar.setValue(0);
                            workLabel.setText("");
                        }
                    }
                    break;
                case FEAT_TASK:
                    if(evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.STARTED)){
                        workLabel.setText("extracting features...");
                        progressBar.setMinimum(0);
                        progressBar.setMaximum(100);
                        progressBar.setValue(0);
                    }else if(evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.DONE)){
                        activeTask = NONE_TASK;
                        workLabel.setText("done");
                        progressBar.setValue(100);
                        ArrayList<ArrayList<ArrayList<Double>>> trainData = segUtil.getTrainData();
                        ArrayList<ArrayList<ArrayList<Double>>> predData  = segUtil.getPredData();
                        classColors = segUtil.getClassColors();
                        startSegmentation(trainData, predData);                        
                    }
                    break;
                case ROI_TASK:
                    if(evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.STARTED)){
                        workLabel.setText("calculating ROIs...");
                        progressBar.setMinimum(0);
                        progressBar.setMaximum(100);
                        progressBar.setValue(0);
                    }else if(evt.getNewValue().equals(javax.swing.SwingWorker.StateValue.DONE)){
                        activeTask = NONE_TASK;
                        progressBar.setValue(100);
                        ClassManager cm = segUtil.getPredClasses();
                        if (cm != null && cm.getClasses().length > 0){
                            workLabel.setText("done");
                            predClasses = activeClasses = cm;
                            predRButton.setEnabled(true);
                            predRButton.doClick();
                            fillBox();
                        }else{
                            String errorText = "calculating ROIs failed!";
                            workLabel.setText(errorText);
                            System.out.println("Error: " + errorText);
                        }                        
                    }                    
                    break;
            }
        } 
    }
    
    private void startSegmentation(ArrayList<ArrayList<ArrayList<Double>>> trainData, ArrayList<ArrayList<ArrayList<Double>>> predData){
        // a new engine instance is needed for each execution as SwingWorker objects are non-reusable        
        try {
            Class engineClass = Class.forName(implementedEngines.get(enginesCombo.getSelectedItem()));
            Class[] params = {java.util.ArrayList.class,java.util.ArrayList.class,String[].class,java.util.Properties.class};
            java.lang.reflect.Constructor con = engineClass.getConstructor(params);                                                                         
            activeEngine = (SegmentationEngine)con.newInstance(trainData,predData,new String[0],null);             
            activeEngine.addPropertyChangeListener(this);
            activeTask = SEG_TASK;
            activeEngine.execute();            
        } catch (Exception e) {
            System.out.println("Error: engine could not be instantiated!");
            e.printStackTrace();
        }
}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        addButton = new javax.swing.JButton();
        renameButton = new javax.swing.JButton();
        classesCombo = new javax.swing.JComboBox();
        minSizeSpinner = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        minSizeLabel = new javax.swing.JLabel();
        trainRButton = new javax.swing.JRadioButton();
        predRButton = new javax.swing.JRadioButton();
        syncButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        enginesCombo = new javax.swing.JComboBox();
        runButton = new javax.swing.JButton();
        displayButton = new javax.swing.JButton();
        roiButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        workLabel = new javax.swing.JLabel();
        saveButton = new javax.swing.JButton();
        loadButton = new javax.swing.JButton();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Class Manager", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        addButton.setText("add");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        renameButton.setText("rename");
        renameButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                renameButtonActionPerformed(evt);
            }
        });

        classesCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                classesComboItemStateChanged(evt);
            }
        });

        minSizeSpinner.setEnabled(false);

        jLabel1.setText("current class");

        minSizeLabel.setText("min size");
        minSizeLabel.setEnabled(false);

        trainRButton.setText("training");
        trainRButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trainRButtonActionPerformed(evt);
            }
        });

        predRButton.setText("prediction");
        predRButton.setEnabled(false);
        predRButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                predRButtonActionPerformed(evt);
            }
        });

        syncButton.setText("sync");
        syncButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                syncButtonActionPerformed(evt);
            }
        });

        removeButton.setText("remove");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(trainRButton)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1)))
                        .addGap(21, 21, 21)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(classesCombo, 0, 163, Short.MAX_VALUE)
                            .addComponent(predRButton)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(syncButton, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                            .addComponent(addButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(removeButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(renameButton))
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(minSizeSpinner)
                                .addComponent(minSizeLabel)))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(classesCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(trainRButton)
                    .addComponent(predRButton))
                .addGap(39, 39, 39)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(removeButton)
                    .addComponent(renameButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(syncButton)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(minSizeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(minSizeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Segmentation", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jButton10.setText("features");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        jButton11.setText("setup");

        runButton.setText("run");
        runButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runButtonActionPerformed(evt);
            }
        });

        displayButton.setText("display");
        displayButton.setEnabled(false);
        displayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayButtonActionPerformed(evt);
            }
        });

        roiButton.setText("ROIs");
        roiButton.setActionCommand("loadROIs");
        roiButton.setEnabled(false);
        roiButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                roiButtonActionPerformed(evt);
            }
        });

        workLabel.setText("working");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(workLabel)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(enginesCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jButton11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(runButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(displayButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(roiButton, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(enginesCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jButton11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton10)))
                .addGap(24, 24, 24)
                .addComponent(runButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(displayButton)
                    .addComponent(roiButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 17, Short.MAX_VALUE)
                .addComponent(workLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        saveButton.setText("save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        loadButton.setText("load");
        loadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButtonActionPerformed(evt);
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
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(saveButton)
                        .addGap(18, 18, 18)
                        .addComponent(loadButton)))
                .addContainerGap(64, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(loadButton)
                    .addComponent(saveButton))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

private void trainRButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trainRButtonActionPerformed
    if(trainClasses == activeClasses) return;    
    activeClasses = trainClasses;
    if (fillBox()) setActiveClass((String)classesCombo.getItemAt(0));
}//GEN-LAST:event_trainRButtonActionPerformed

private void predRButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_predRButtonActionPerformed
    if(predClasses == activeClasses) return;
    activeClasses = predClasses;
    if (fillBox()) setActiveClass((String)classesCombo.getItemAt(0));
}//GEN-LAST:event_predRButtonActionPerformed

private void runButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runButtonActionPerformed
    if (activeTask == SEG_TASK){
        activeEngine.cancel(true);
        // TODO: better check whether thread was really canceled before allowing another run
        runButton.setText("run(!)");
    }else if (activeTask == FEAT_TASK){
        // TODO: implement canceling of feature extraction (by suspending thread)
        segUtil.cancel(true);
        // TODO: better check whether thread was really canceled before allowing another run
        runButton.setText("run(!)");
    }else{
        // HARD-CODED: feature selection (use mass images 0,2,3 , no ratio image, image 2 for coloring)
        java.util.ArrayList images = new java.util.ArrayList();
        //images.add(0);
        images.add(2);
        images.add(3);
        java.util.ArrayList<int[]> ratioImages = new java.util.ArrayList<int[]>();
        ratioImages.add(new int[]{3,2});
        int colorImage = 2;
        int features = 0;
        // HARD-CODED
        this.height=mimsUi.getMimsImage().getHeight();
        this.width = mimsUi.getMimsImage().getWidth();
        classNames = trainClasses.getClasses();
        activeTask = FEAT_TASK;        
        segUtil = SegUtils.initPrepSeg(mimsUi, trainClasses, images, ratioImages, colorImage, features);
        segUtil.addPropertyChangeListener(this);
        segUtil.execute();
    }
}//GEN-LAST:event_runButtonActionPerformed

private void roiButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_roiButtonActionPerformed
    activeTask = ROI_TASK;
    segUtil = SegUtils.initCalcRoi(height, width, classNames, classification, 100, (int)Double.POSITIVE_INFINITY);
    segUtil.addPropertyChangeListener(this);
    segUtil.execute();    
}//GEN-LAST:event_roiButtonActionPerformed

private void classesComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_classesComboItemStateChanged
    if(evt.getStateChange()==java.awt.event.ItemEvent.SELECTED){
        String className = (String)classesCombo.getSelectedItem();
        if(!className.equals(activeClass)) setActiveClass(className);
    }
}//GEN-LAST:event_classesComboItemStateChanged

private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
    String className = (String)JOptionPane.showInputDialog(this,"Name for new class:","Edit class",JOptionPane.PLAIN_MESSAGE,null,null,"");
    if(className != null && className.length() > 0 && activeClasses.addClass(className)){        
        fillBox();                  // display the new list of class names
        classesCombo.setSelectedItem(className);
        if(!runButton.isEnabled()) runButton.setEnabled(true);
    }
}//GEN-LAST:event_addButtonActionPerformed

private void renameButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renameButtonActionPerformed
    if(activeClass.equals("")) return;
    String newName = (String)JOptionPane.showInputDialog(this,"Rename class to:","Edit class",JOptionPane.PLAIN_MESSAGE,null,null,activeClass);
    if(newName != null && newName.length() > 0 && activeClasses.renameClass(activeClass, newName)){
        // display the new list of class names
        fillBox();
        classesCombo.setSelectedItem(newName);
    }
}//GEN-LAST:event_renameButtonActionPerformed

private void syncButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_syncButtonActionPerformed
// TODO add your handling code here:
    Hashtable rois = roiManager.getROIs();
    activeClasses.removeClass(activeClass);
    activeClasses.addClass(activeClass);
    for(Object key : rois.keySet()){
        activeClasses.addRoi(activeClass, (Roi)rois.get(key));
    }
    setActiveClass(activeClass);
}//GEN-LAST:event_syncButtonActionPerformed

private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
// TODO add your handling code here:
    FeaturesDialog features = new FeaturesDialog(mimsUi, true, mimsUi, new boolean[]{true,false,true,true}, null);
    features.setVisible(true);
}//GEN-LAST:event_jButton10ActionPerformed

private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
    String defaultPath = mimsUi.getMimsImage().getImageFile().getParent() + System.getProperty("file.separator") + "segResult.zip";
    JFileChooser fc = new JFileChooser(defaultPath);       
    fc.setSelectedFile(new java.io.File(defaultPath));
    while(true){
        if(fc.showSaveDialog(this) == JFileChooser.CANCEL_OPTION ) break;    
        String filePath = fc.getSelectedFile().getPath();
        String fileName = fc.getSelectedFile().getName();
        if(new java.io.File(filePath).exists()){
            String[] options = {"Overwrite","Cancel"};
            int value = JOptionPane.showOptionDialog(this,"File \"" + fileName + "\" already exists!",null,
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,null,options, options[1]);                      
            if(value == JOptionPane.NO_OPTION) continue;            
        }
        SegUtils.saveSegmentation(filePath, trainClasses, predClasses, classNames, classification, classColors);
        break;
    }
}//GEN-LAST:event_saveButtonActionPerformed

private void loadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButtonActionPerformed
// TODO add your handling code here:
    String defaultPath = mimsUi.getMimsImage().getImageFile().getParent() + System.getProperty("file.separator") + ".";
    JFileChooser fc = new JFileChooser(defaultPath);
    fc.setSelectedFile(new java.io.File(defaultPath));
    if(fc.showOpenDialog(this) != JFileChooser.CANCEL_OPTION ){
        resetForm();
        SegUtils loadedData = SegUtils.loadSegmentation(fc.getSelectedFile().getPath());
        
        trainClasses = loadedData.getTrainClasses();
        // if there are training sets available, display them and enable segmentation
        if(trainClasses.getClasses().length > 1){
            runButton.setEnabled(true);
            trainRButton.doClick();
        }
        
        predClasses = loadedData.getPredClasses();
        // if there are predicted sets available, display them (priority over training sets       
        if(predClasses.getClasses().length > 1){
            predRButton.setEnabled(true);
            predRButton.doClick();
        }
        
        classification = loadedData.getClassification();
        // if there is classification data available, enable display and ROI-calculation        
        if(classification.length > 1){
            displayButton.setEnabled(true);
            roiButton.setEnabled(true);
        }
        
        // assuming we have only square dimensions
        width = height = (int)Math.sqrt(classification.length);
        
        classNames = loadedData.getClassNames();
        classColors = loadedData.getClassColors();
    }    
}//GEN-LAST:event_loadButtonActionPerformed

private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
   if (activeClass.equals(""))return;
   int ok = JOptionPane.showConfirmDialog(this, "Remove class " +activeClass + "?", "Edit class", JOptionPane.OK_OPTION);
   if(ok == JOptionPane.OK_OPTION && activeClasses.removeClass(activeClass)){
       // remove the class from the class names array
//       classNames.remove(activeClass);
       int index = classesCombo.getSelectedIndex();
       fillBox();
       if(classesCombo.getItemCount() > 0) classesCombo.setSelectedIndex(index);
       else if(runButton.isEnabled()) runButton.setEnabled(false);
    }
}//GEN-LAST:event_removeButtonActionPerformed

private void displayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayButtonActionPerformed
    int[] pixels = new int[classification.length];
    for (int i=0; i<pixels.length;i++){
        // get the color value for each pixel according to its assigned class
        pixels[i] = classColors[classification[i]];        
    }    
    mimsUi.openSeg(pixels, "Segmentation result", height, width);
}//GEN-LAST:event_displayButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JComboBox classesCombo;
    private javax.swing.JButton displayButton;
    private javax.swing.JComboBox enginesCombo;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JButton loadButton;
    private javax.swing.JLabel minSizeLabel;
    private javax.swing.JSpinner minSizeSpinner;
    private javax.swing.JRadioButton predRButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton removeButton;
    private javax.swing.JButton renameButton;
    private javax.swing.JButton roiButton;
    private javax.swing.JButton runButton;
    private javax.swing.JButton saveButton;
    private javax.swing.JButton syncButton;
    private javax.swing.JRadioButton trainRButton;
    private javax.swing.JLabel workLabel;
    // End of variables declaration//GEN-END:variables
    private javax.swing.ButtonGroup buttonGroup;
}

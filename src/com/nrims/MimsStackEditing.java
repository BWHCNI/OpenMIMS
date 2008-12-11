package com.nrims;

import com.nrims.data.Opener;
import ij.*;
import java.io.*;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 *
 * @author  cpoczatek
 */
public class MimsStackEditing extends javax.swing.JPanel {

    public static final long serialVersionUID = 1;

    //Philipp method for demo
    //needs to be deleted
    public void setConcatGUI(boolean status) {
        if (status) {
            this.reinsertButton.setEnabled(false);
            this.reinsertListTextField.setEnabled(false);
            this.concatButton.setEnabled(false);
            this.untrackButton.setEnabled(false);
            this.translateXSpinner.setEnabled(false);
            this.translateYSpinner.setEnabled(false);
        } else {
            this.reinsertButton.setEnabled(true);
            this.reinsertListTextField.setEnabled(true);
            this.concatButton.setEnabled(true);
            this.untrackButton.setEnabled(true);
            this.translateXSpinner.setEnabled(true);
            this.translateYSpinner.setEnabled(true);
            autoTrackButton.setEnabled(true);
        }
    }
    //Philipp method for demo

    /** Creates new form mimsStackEditing
     * @param ui ??
     * @param im ??
     */
    public MimsStackEditing(UI ui, Opener im) {

        initComponents();


        this.ui = ui;
        this.image = im;

        this.images = ui.getMassImages();
        numberMasses = image.nMasses();
        imagestacks = new ImageStack[numberMasses];

        resetImageStacks();

        ui.getmimsLog().Log("New image: " + image.getName() + "\n" + getImageHeader(image));
    }

    public void resetImageStacks() {
        for (int i = 0; i < numberMasses; i++) {
            imagestacks[i] = this.images[i].getStack();
        }
    }

    public String removeSliceList(ArrayList<Integer> remList) {
        String liststr = "";
        int length = remList.size();
        int count = 0;
        for (int i = 0; i < length; i++) {
            removeSlice(remList.get(i) - count);
            count++;
            liststr += remList.get(i);
            if (i < (length - 1)) {
                liststr += ", ";
            }
        }

        return liststr;
    }

    public void removeSlice(int plane) {

        for (int k = 0; k <= (numberMasses - 1); k++) {

            imagestacks[k].deleteSlice(plane);

            this.images[k].setStack(null, imagestacks[k]);
        }
        this.ui.mimsAction.dropPlane(plane);
    }

    public void XShiftSlice(int plane, int xval) {
        for (int k = 0; k <= (numberMasses - 1); k++) {
            this.images[k].getProcessor().translate(xval, 0, true);
            images[k].updateAndDraw();
        }
        ui.mimsAction.setShiftX(plane, xval);
    }

    public void YShiftSlice(int plane, int yval) {
        for (int k = 0; k <= (numberMasses - 1); k++) {
            this.images[k].getProcessor().translate(0, yval, true);
            images[k].updateAndDraw();
        }
        ui.mimsAction.setShiftY(plane, yval);
    }

    public void XShiftSlice(int plane, int xval, int foo) {
        for (int k = 0; k <= (numberMasses - 1); k++) {
            this.images[k].getProcessor().translate(xval, 0, true);
            images[k].updateAndDraw();
        }
        ui.mimsAction.nudgeX(plane, xval);
    }

    public void YShiftSlice(int plane, int yval, int foo) {
        for (int k = 0; k <= (numberMasses - 1); k++) {
            this.images[k].getProcessor().translate(0, yval, true);
            images[k].updateAndDraw();
        }
        ui.mimsAction.nudgeY(plane, yval);
    }

    public void restoreSlice(int plane) {
        int restoreIndex = ui.mimsAction.trueIndex(plane);
        try {
            image.setStackIndex(restoreIndex - 1);
            for (int k = 0; k <= (numberMasses - 1); k++) {
                image.readPixels(k);
                images[k].setSlice(plane);
                images[k].getProcessor().setPixels(image.getPixels(k));
                images[k].updateAndDraw();
            }
        } catch (Exception e) {
            System.err.println("Error re-reading plane " + restoreIndex);
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }

    public void insertSlice(int plane) {

        System.out.println("inside insertSlice...");

        if (ui.mimsAction.isDropped(plane) == 0) {
            System.out.println("already there...");
            return;
        }

        //int n = ui.mimsAction.getSize();

        int restoreIndex = ui.mimsAction.displayIndex(plane);
        int displaysize = images[0].getNSlices();
        System.out.println("try to add at: " + restoreIndex);
        try {
            if (restoreIndex < displaysize) {
                int currentPlane = images[0].getSlice();

                image.setStackIndex(plane - 1);
                for (int i = 0; i < image.nMasses(); i++) {
                    images[i].setSlice(restoreIndex);
                    imagestacks[i].addSlice("", images[i].getProcessor(), restoreIndex);
                    images[i].getProcessor().setPixels(image.getPixels(i));
                    images[i].updateAndDraw();
                }
                ui.mimsAction.undropPlane(plane);
                images[0].setSlice(currentPlane);
            }
            if (restoreIndex >= displaysize) {

                this.holdupdate = true;

                int currentPlane = images[0].getSlice();
                image.setStackIndex(plane - 1);

                for (int i = 0; i < image.nMasses(); i++) {
                    images[i].setSlice(displaysize);
                    imagestacks[i].addSlice("", images[i].getProcessor());
                    images[i].setStack(null, imagestacks[i]);
                    images[i].setSlice(restoreIndex);
                    images[i].getProcessor().setPixels(image.getPixels(i));
                    images[i].updateAndDraw();
                }

                ui.mimsAction.undropPlane(plane);
                images[0].setSlice(currentPlane);
            }

        } catch (Exception e) {
            System.err.println("Error re-reading plane " + restoreIndex + "from file.");
            System.err.println(e.toString());
            e.printStackTrace();
        }
        this.holdupdate = false;
        this.resetTrueIndexLabel();
    }

    public void concatImages(boolean pre, UI tempui) {

        MimsPlus[] tempimage = tempui.getMassImages();
        ImageStack[] tempstacks = new ImageStack[numberMasses];

        ui.mimsAction.addPlanes(pre, tempimage[0].getNSlices());

        for (int i = 0; i <= (numberMasses - 1); i++) {
            tempstacks[i] = tempimage[i].getStack();
        }

        if (pre) {
            for (int mass = 0; mass <= (numberMasses - 1); mass++) {
                for (int i = tempstacks[mass].getSize(); i >= 1; i--) {
                    imagestacks[mass].addSlice("", tempstacks[mass].getProcessor(i), 0);
                }
            }
            ui.getmimsLog().Log("Concat: " + tempui.getMimsImage().getName() + " + " + image.getName());
        } else {
            for (int mass = 0; mass <= (numberMasses - 1); mass++) {
                for (int i = 1; i <= tempstacks[mass].getSize(); i++) {
                    imagestacks[mass].addSlice("", tempstacks[mass].getProcessor(i));
                }
            }
            ui.getmimsLog().Log("Concat: " + image.getName() + " + " + tempui.getMimsImage().getName());
        }


        for (int i = 0; i <= (numberMasses - 1); i++) {
            this.images[i].setStack(null, imagestacks[i]);
            this.images[i].updateImage();
        }

        ui.mimsAction.addImage(pre, tempui.getMimsImage());

        ui.getmimsLog().Log("New size: " + images[0].getNSlices() + " planes");

        // disable all functions       
        //for(Component comp : this.getComponents()){
        //    comp.setEnabled(false);
        //}

        // disable certain functions
        setConcatGUI(true);

    }

    public static String getImageHeader(Opener im) {
        String str = "\nHeader: \n";
        str += "Path: " + im.getImageFile().getAbsolutePath() + "/" + im.getName() + "\n";

        str += "Masses: ";
        for (int i = 0; i < im.nMasses(); i++) {
            str += im.getMassName(i) + " ";
        }
        str += "\n";
        str += "Pixels: " + im.getWidth() + "x" + im.getHeight() + "\n";
        str += "Duration: " + im.getDuration() + "\n";
        str += "Dwell time: " + im.getDwellTime() + "\n";
        str += "Position: " + im.getPosition() + "\n";
        str += "Sample name: " + im.getSampleName() + "\n";
        str += "Sample date: " + im.getSampleDate() + "\n";
        str += "Sample hour: " + im.getSampleHour() + "\n";
        str += "Pixel width: " + im.getPixelWidth() + "\n";
        str += "Pixel height: " + im.getPixelHeight() + "\n";
        str += "End header.\n\n";
        return str;
    }

    public boolean sameResolution(Opener im, Opener ij) {
        return ((im.getWidth() == ij.getWidth()) && (im.getHeight() == ij.getHeight()));
    }

    public boolean sameSpotSize(Opener im, Opener ij) {
        return ((im.getPixelWidth() == ij.getPixelWidth()) && (im.getPixelHeight() == ij.getPixelHeight()));
    }

    public void rawExport(String path) {
        try {
            new java.io.File(path).mkdir();
            String[] names = image.getMassNames();

            for (int i = 0; i < numberMasses; i++) {
                new java.io.File(path + names[i] + "/").mkdir();
            }

            FileWriter fstream = new FileWriter(path + "header.txt");
            BufferedWriter output = new BufferedWriter(fstream);
            output.write(getImageHeader(this.image));
            output.close();

            for (int i = 0; i < numberMasses; i++) {
                String temppath = path + names[i] + "/";
                int numslices = this.image.nImages();
                for (int j = 1; j <= numslices; j++) {
                    fstream = new FileWriter(temppath + j + ".txt");
                    output = new BufferedWriter(fstream);
                    int[][] pixels = this.imagestacks[i].getProcessor(j).getIntArray();

                    for (int k = 0; k < pixels.length; k++) {
                        for (int s = 0; s < pixels[k].length - 1; s++) {
                            output.write(pixels[k][s] + ", ");
                        }
                        output.write(pixels[k][pixels[k].length - 1] + "");
                        output.write("\n");
                    }
                    output.flush();
                    fstream.flush();
                    fstream.close();
                }
            }

        } catch (Exception e) {
            ij.IJ.log("Error: " + e.getMessage());
        }


    }

    public ArrayList<Integer> parseList(String liststr, int lb, int ub) {
        ArrayList<Integer> deletelist = new ArrayList<Integer>();
        ArrayList<Integer> checklist = new ArrayList<Integer>();
        boolean badlist = false;

        check:
        try {
            if (liststr.equals("")) {
                badlist = true;
                break check;
            }
            liststr = liststr.replaceAll("[^0-9,-]", "");
            String[] splitstr = liststr.split(",");
            int l = splitstr.length;

            for (int i = 0; i < l; i++) {

                if (!splitstr[i].contains("-")) {
                    deletelist.add(Integer.parseInt(splitstr[i]));
                } else {
                    String[] resplitstr = splitstr[i].split("-");

                    if (resplitstr.length > 2) {
                        ij.IJ.error("List Error", "Malformed range in list.");
                        break check;
                    }

                    int low = Integer.parseInt(resplitstr[0]);
                    int high = Integer.parseInt(resplitstr[1]);

                    if (low >= high) {
                        ij.IJ.error("List Error", "Malformed range bounds in list.");
                        break check;
                    }

                    for (int j = low; j <= high; j++) {
                        deletelist.add(j);
                    }
                }
            }
            java.util.Collections.sort(deletelist);

            int length = deletelist.size();

            for (int i = 0; i < length; i++) {
                if (deletelist.get(i) > ub || deletelist.get(i) < lb) {
                    badlist = true;
                    ij.IJ.error("List Error", "Out of range element in list.");
                    break check;
                }
            }

            for (int i = 0; i < length; i++) {
                int plane = deletelist.get(i);
                if (!checklist.contains(plane)) {
                    checklist.add(plane);
                }
            }


        } catch (Exception e) {
            ij.IJ.error("List Error", "Exception, malformed delete list.");
            badlist = true;
        }


        if (badlist) {
            ArrayList<Integer> bad = new ArrayList<Integer>(0);
            return bad;
        } else {
            return checklist;
        }
    }

    public void applyAction(MimsAction action) {
        int trueIndex = 1;
        for (Opener im : action.getImages()) {    // iterate through all .im files contained in the action
            for (int i = 0; i < im.nImages(); i++) {  // iterate through all planes of the current .im file
                for (int k = 0; k < image.nMasses(); k++) {     // set the current slice
                    images[k].setSlice(trueIndex);
                }
                int displayIndex = action.displayIndex(trueIndex);
                XShiftSlice(displayIndex, action.getXShift(displayIndex));
                YShiftSlice(displayIndex, action.getYShift(displayIndex));
                if (action.isDropped(trueIndex) == 1) {
                    removeSlice(displayIndex);
                }
                trueIndex++;
            }
        }
    }
    private UI ui = null;
    private Opener image = null;
    private int numberMasses = -1;
    private MimsPlus[] images = null;
    private ImageStack[] imagestacks = null;
    private boolean holdupdate = false;

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jSeparator1 = new javax.swing.JSeparator();
        concatButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        deleteListTextField = new javax.swing.JTextField();
        deleteListButton = new javax.swing.JButton();
        rawExportButton = new javax.swing.JButton();
        trueIndexLabel = new javax.swing.JLabel();
        reinsertButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        reinsertListTextField = new javax.swing.JTextField();
        displayActionButton = new javax.swing.JButton();
        translateXSpinner = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        translateYSpinner = new javax.swing.JSpinner();
        autoTrackButton = new javax.swing.JButton();
        untrackButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        saveActionButton = new javax.swing.JButton();
        sumButton = new javax.swing.JButton();

        concatButton.setText("Concatenate");
        concatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                concatButtonActionPerformed(evt);
            }
        });

        jLabel5.setText("Delete List (eg: 2,4,8-25,45...)");

        deleteListButton.setText("Delete");
        deleteListButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteListButtonActionPerformed(evt);
            }
        });

        rawExportButton.setText("Raw Export");
        rawExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawExportButtonActionPerformed(evt);
            }
        });

        trueIndexLabel.setText("True Index: 999   Display Index: 999");

        reinsertButton.setText("Reinsert");
        reinsertButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reinsertButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Reinsert List (True plane numbers)");

        displayActionButton.setText("Display Action");
        displayActionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayActionButtonActionPerformed(evt);
            }
        });

        translateXSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));
        translateXSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                translateXSpinnerStateChanged(evt);
            }
        });

        jLabel2.setText("Translate X");

        jLabel3.setText("Translate Y");

        translateYSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));
        translateYSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                translateYSpinnerStateChanged(evt);
            }
        });

        autoTrackButton.setText("Autotrack");
        autoTrackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoTrackButtonActionPerformed(evt);
            }
        });

        untrackButton.setText("Untrack");
        untrackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                untrackButtonActionPerformed(evt);
            }
        });

        saveActionButton.setText("Save Action");
        saveActionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionButtonActionPerformed(evt);
            }
        });

        sumButton.setText("Sum");
        sumButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sumButtonActionPerformed(evt);
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
                            .addComponent(rawExportButton)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(displayActionButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(saveActionButton)))
                        .addGap(84, 84, 84))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(deleteListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel5)
                                .addComponent(deleteListButton, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(trueIndexLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 309, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addGap(16, 16, 16))
                        .addComponent(reinsertListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(reinsertButton)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(jLabel4))
                            .addComponent(concatButton)
                            .addComponent(sumButton))
                        .addGap(57, 57, 57)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel2))
                            .addComponent(translateYSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                            .addComponent(translateXSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE))
                        .addGap(51, 51, 51))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(autoTrackButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(untrackButton)
                        .addContainerGap(143, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(trueIndexLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteListButton)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(reinsertListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(reinsertButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(translateXSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sumButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(translateYSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(concatButton))))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(rawExportButton)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(displayActionButton)
                            .addComponent(untrackButton)
                            .addComponent(saveActionButton)
                            .addComponent(autoTrackButton))
                        .addGap(31, 31, 31)))
                .addGap(36, 36, 36))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void deleteListButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteListButtonActionPerformed
// TODO add your handling code here:

        String liststr = deleteListTextField.getText();
        ArrayList<Integer> checklist = parseList(liststr, 1, images[0].getStackSize());

        if (checklist.size() != 0) {
            liststr = removeSliceList(checklist);
            ui.getmimsLog().Log("Deleted list: " + liststr);
            ui.getmimsLog().Log("New size: " + images[0].getNSlices() + " planes");
        }

        this.resetTrueIndexLabel();
        this.resetSpinners();
        deleteListTextField.setText("");
}//GEN-LAST:event_deleteListButtonActionPerformed

    private void concatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_concatButtonActionPerformed
        UI tempUi = new UI(null); //loadMims here
        Opener tempImage = tempUi.getMimsImage();
        if (tempImage == null) {
            return; // if the FileChooser dialog was canceled
        }
        if (ui.getMimsImage().nMasses() == tempImage.nMasses()) {
            if (sameResolution(image, tempImage)) {
                if (sameSpotSize(image, tempImage)) {
                    Object[] options = {"Append images", "Prepend images", "Cancel"};
                    int value = JOptionPane.showOptionDialog(this, tempImage.getName(), "Concatenate",
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
                    if (value != JOptionPane.CANCEL_OPTION) {
                        // store action to reapply it after restoring
                        // (shallow copy in mimsAction is enough as 'restoreMims' creates a new 'actionList' object
                        //mimsAction action = (mimsAction)ui.getmimsAction().clone();
                        //ui.restoreMims();
                        concatImages(value != JOptionPane.YES_OPTION, tempUi);
                    //applyAction(action);
//                        for(int k=0; k<image.nMasses(); k++) {     // display the first slice
//                            images[k].setSlice(1);
//                        }
                    }
                } else {
                    IJ.error("Images do not have the same spot size.");
                }
            } else {
                IJ.error("Images are not the same resolution.");
            }
        } else {
            IJ.error("Two images with the same\nnumber of masses must be open.");
        }

        // close the temporary windows
        for (MimsPlus image : tempUi.getMassImages()) {
            if (image != null) {
                image.setAllowClose(true);
                image.close();

            }
        }

        ui.getmimsTomography().resetBounds();
        ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
        //wo.run("tile");
        ui.updateStatus("");
        ij.WindowManager.repaintImageWindows();

//        mimsPlus[] finalImages =this.ui.getMassImages();
//        
//        for (int p =0; p<finalImages.length;p++){
//            this.ui.getMassImage(p).setAllowClose(false);
//        }

//            jLabel4.setText(tempmims.getName());
//            ui.getmimsLog().Log("Additional image: "+tempmims.getName()+"\n"+getImageHeader(tempmims));
//            tempui.updateStatus("");        
//        }
//        else{            
//            jLabel4.setText("");
//        }

//        if(gd.wasCanceled()) return ;        
//        int[] windowIDList = ij.WindowManager.getIDList();
//        
//        if (tempui != null && tempmims != null){
////            ui.restoreMims();
//            tempui.restoreMims();
//                if(ui.getMimsImage().nMasses() == tempmims.nMasses()){
//                if (sameResolution(image,tempmims)) {
//                    if (sameSpotSize(image,tempmims)) {
//                        concatOpenImages(prepend);
//                        ui.getmimsTomography().resetBounds();
////                        for (int i=(windowIDList.length-numberMasses); i<windowIDList.length; i++) 
////                            ij.WindowManager.getImage(windowIDList[i]).close();
//                        
//                        ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
//                        wo.run("tile");
//                    } else {
//                        IJ.error("Images do not have the same spot size.");
//                    }   
//                } else {
//                    IJ.error("Images are not the same resolution.");
//                }
//            } else {
//                IJ.error("Two images with the same\nnumber of masses must be open.");
//            }
//        }
//        
//        ui.updateStatus("");
//        ij.WindowManager.repaintImageWindows();
}//GEN-LAST:event_concatButtonActionPerformed

    private void rawExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawExportButtonActionPerformed
        // TODO add your handling code here:
        final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle("Export");
        fc.setApproveButtonText("Export");
        fc.setFileSelectionMode(fc.DIRECTORIES_ONLY);

        int returnVal = fc.showOpenDialog(this);

        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getAbsolutePath() + "/" + this.image.getName() + "_raw/";
            rawExport(path);
            ui.getmimsLog().Log("File Exported To: " + path);
        } else {
            ui.getmimsLog().Log("Export Canceled");
        }
}//GEN-LAST:event_rawExportButtonActionPerformed

    private void reinsertButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reinsertButtonActionPerformed
        // TODO add your handling code here:
        int current = images[0].getCurrentSlice();
        String liststr = reinsertListTextField.getText();
        int trueSize = ui.mimsAction.getSize();
        ArrayList<Integer> checklist = parseList(liststr, 1, trueSize);
        int length = checklist.size();

        System.out.println("insert button...");
        System.out.println("length = " + length);

        for (int i = 0; i < length; i++) {
            System.out.println("i = " + i);
            this.insertSlice(checklist.get(i));
        }

        images[0].setSlice(current);
        this.resetTrueIndexLabel();
        this.resetSpinners();
        reinsertListTextField.setText("");
}//GEN-LAST:event_reinsertButtonActionPerformed

    private void displayActionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayActionButtonActionPerformed
        // TODO add your handling code here:
        ij.text.TextWindow actionWindow = new ij.text.TextWindow("Current Action State", "plane\tx\ty\tdrop", "", 300, 400);

        int n = ui.mimsAction.getSize();
        String tempstr = "";
        for (int i = 1; i <= n; i++) {
            tempstr = ui.mimsAction.getActionRow(i);
            actionWindow.append(tempstr);
        }
    }//GEN-LAST:event_displayActionButtonActionPerformed

    private void translateXSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_translateXSpinnerStateChanged
        // TODO add your handling code here:
        int plane = images[0].getCurrentSlice();

        int xval = (Integer) translateXSpinner.getValue();
        int yval = (Integer) translateYSpinner.getValue();

        int actx = ui.mimsAction.getXShift(plane);
        int acty = ui.mimsAction.getYShift(plane);

        int deltax = xval - actx;
        int deltay = yval - acty;

        boolean redraw = ((deltax * actx < 0) || (deltay * acty < 0));

        if (!holdupdate && ((actx != xval) || (acty != yval)) && (!ui.isUpdating())) {
            if (redraw) {
                this.restoreSlice(plane);
                this.XShiftSlice(plane, xval);
                this.YShiftSlice(plane, yval);
            } else {
                this.XShiftSlice(plane, deltax);
                this.YShiftSlice(plane, deltay);
            }
            ui.mimsAction.setShiftX(plane, xval);
            ui.mimsAction.setShiftY(plane, yval);
        }
    }//GEN-LAST:event_translateXSpinnerStateChanged

    private void translateYSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_translateYSpinnerStateChanged
        // TODO add your handling code here:
        int plane = images[0].getCurrentSlice();

        int xval = (Integer) translateXSpinner.getValue();
        int yval = (Integer) translateYSpinner.getValue();

        int actx = ui.mimsAction.getXShift(plane);
        int acty = ui.mimsAction.getYShift(plane);

        int deltax = xval - actx;
        int deltay = yval - acty;

        boolean redraw = ((deltax * actx < 0) || (deltay * acty < 0));

        if (!holdupdate && ((actx != xval) || (acty != yval)) && (!ui.isUpdating())) {
            if (redraw) {
                this.restoreSlice(plane);
                this.XShiftSlice(plane, xval);
                this.YShiftSlice(plane, yval);
            } else {
                this.XShiftSlice(plane, deltax);
                this.YShiftSlice(plane, deltay);
            }
            ui.mimsAction.setShiftX(plane, xval);
            ui.mimsAction.setShiftY(plane, yval);
        //System.out.println("ychanged "+ foo++);
        }
    }//GEN-LAST:event_translateYSpinnerStateChanged

    private void autoTrackButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoTrackButtonActionPerformed
        //ugly....
        
        ImagePlus tempImage = WindowManager.getCurrentImage();
        ij.process.ImageProcessor tempProcessor = tempImage.getProcessor();
        int startPlane = images[0].getSlice();
        ij.ImageStack tempStack = new ij.ImageStack(tempImage.getWidth(), tempImage.getHeight());
        //not setting roi's to deselect, simply deselecting from list
        ui.getRoiManager().select(-1);
        String massname = tempImage.getTitle();
        massname = massname.substring(massname.length() - 6, massname.length());

        for (int i = 1; i <= images[0].getStackSize(); i++) {
            images[0].setSlice(i);
            tempImage = WindowManager.getCurrentImage();
            tempProcessor = tempImage.getProcessor();
            tempStack.addSlice(tempImage.getTitle(), tempProcessor);
        }


        tempImage.setSlice(0);
        ImagePlus img = new ij.ImagePlus("", tempStack);

        //the waiting
        if (img == null) {
            System.err.println("The img is null; aborting.");
        } else if (ui == null) {
            System.err.println("The UI is null; aborting.");
        }
        AutoTrack temptrack = new AutoTrack(ui);
        double[][] translations = temptrack.track(img);

        if (translations == null) {
            throw new NullPointerException("translations is null: AutoTrack has failed.");
        }

        int xval, yval;
        int actx = 0;
        int acty = 0;

        int deltax = 0;
        int deltay = 0;
        int plane;

        boolean redraw;

        for (int i = 0; i < translations.length; i++) {
            plane = i + 1;
            //possible loss of precision...
            //notice the negative....
            xval = (-1) * (int) java.lang.Math.round(translations[i][0]);
            yval = (-1) * (int) java.lang.Math.round(translations[i][1]);
            images[0].setSlice(plane);
            this.XShiftSlice(i + 1, xval, 1);
            this.YShiftSlice(i + 1, yval, 1);

        }

        //clean up
        images[0].setSlice(startPlane);
        tempImage = null;
        tempProcessor = null;
        tempStack = null;
        img.close();
        img = null;
        if (!this.reinsertButton.isEnabled()) {
            autoTrackButton.setEnabled(false);
        }
        ui.getmimsLog().Log("Autotracked on the " + massname + " images.");
}//GEN-LAST:event_autoTrackButtonActionPerformed

    private void untrackButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_untrackButtonActionPerformed
        // TODO add your handling code here:
        int xval = 0;
        int yval = 0;

        int actx = 0;
        int acty = 0;

        int deltax = 0;
        int deltay = 0;

        boolean redraw;

        for (int plane = 1; plane <= images[0].getStackSize(); plane++) {
            xval = 0;
            yval = 0;

            actx = ui.mimsAction.getXShift(plane);
            acty = ui.mimsAction.getYShift(plane);

            deltax = xval - actx;
            deltay = yval - acty;

            redraw = ((deltax * actx < 0) || (deltay * acty < 0));

            if (!holdupdate && ((actx != xval) || (acty != yval)) && (!ui.isUpdating())) {
                if (redraw) {
                    this.restoreSlice(plane);
                    this.XShiftSlice(plane, xval);
                    this.YShiftSlice(plane, yval);
                } else {
                    this.XShiftSlice(plane, deltax);
                    this.YShiftSlice(plane, deltay);
                }
                ui.mimsAction.setShiftX(plane, xval);
                ui.mimsAction.setShiftY(plane, yval);
            //System.out.println("ychanged "+ foo++);
            }
        }
        autoTrackButton.setEnabled(true);
        ui.getmimsLog().Log("Untracked.");
}//GEN-LAST:event_untrackButtonActionPerformed

private void saveActionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveActionButtonActionPerformed
    String defaultPath = image.getImageFile().getParent().toString() + System.getProperty("file.separator") + "action.txt";
    JFileChooser fc = new JFileChooser(defaultPath);
    fc.setSelectedFile(new File(defaultPath));
    while (true) {
        if (fc.showSaveDialog(this) == JFileChooser.CANCEL_OPTION) {
            break;
        }
        String actionFilePath = fc.getSelectedFile().getPath();
        String actionFile = fc.getSelectedFile().getName();
        if (new File(actionFilePath).exists()) {
            String[] options = {"Overwrite", "Cancel"};
            int value = JOptionPane.showOptionDialog(this, "File \"" + actionFile + "\" already exists!", null,
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (value == JOptionPane.NO_OPTION) {
                continue;
            }

        }
        MimsAction.writeAction(ui.getmimsAction(), actionFilePath);
        break;
    }
}//GEN-LAST:event_saveActionButtonActionPerformed

private void sumButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sumButtonActionPerformed
// TODO add your handling code here:
    
    String name = WindowManager.getCurrentImage().getTitle();
    
    ui.computeSum(ui.getImageByName(name));
}//GEN-LAST:event_sumButtonActionPerformed

    protected void restoreAllPlanes() {
        this.holdupdate = true;
        for (int restoreIndex = 1; restoreIndex <= image.nImages(); restoreIndex++) {
            try {
                int currentPlane = images[0].getSlice();
                image.setStackIndex(restoreIndex - 1);
                for (int i = 0; i < image.nMasses(); i++) {
                    image.readPixels(i);  //really "bad" don't use????
                    images[i].setSlice(restoreIndex);
                    images[i].getProcessor().setPixels(image.getPixels(i));
                    images[i].updateAndDraw();
                }
                images[0].setSlice(currentPlane);

            } catch (Exception e) {
                System.err.println("Error re-reading plane " + restoreIndex);
                System.err.println(e.toString());
                e.printStackTrace();
            }

        //pritnt entire actionlist
        //test this.ui.mimsAction.dropPlane(2);
        //this.ui.mimsAction.printAction();

        //for(int i=1; i<=10; i++) {
        //    System.out.println("vect: " + (i-1) + " pl: " + i + " dr: " + ui.mimsAction.isDropped(i) + " disp: " + ui.mimsAction.displayIndex(i) );
        //}
        }
        ui.getmimsAction().resetAction(ui, image);
        this.holdupdate = false;
    }

    protected void resetSpinners() {
        if (this.images != null && (!holdupdate) && (images[0] != null) && (!ui.isUpdating())) {
            //System.out.println("resetspinners ");
            holdupdate = true;
            int plane = images[0].getCurrentSlice();
            int xval = ui.mimsAction.getXShift(plane);
            int yval = ui.mimsAction.getYShift(plane);
            this.translateXSpinner.setValue(xval);
            this.translateYSpinner.setValue(yval);
            holdupdate = false;
        //System.out.println("resetspinners "+ foo++);
        }
    }

    protected void resetTrueIndexLabel() {

        if (this.images != null && (!holdupdate) && (images[0] != null)) {
            String label = "True index: ";
            int p = ui.mimsAction.trueIndex(this.images[0].getCurrentSlice());

            label = label + java.lang.Integer.toString(p);
            p = this.images[0].getCurrentSlice();
            label = label + "   Display index: " + java.lang.Integer.toString(p);

            this.trueIndexLabel.setText(label);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton autoTrackButton;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton concatButton;
    private javax.swing.JButton deleteListButton;
    private javax.swing.JTextField deleteListTextField;
    private javax.swing.JButton displayActionButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JButton rawExportButton;
    private javax.swing.JButton reinsertButton;
    private javax.swing.JTextField reinsertListTextField;
    private javax.swing.JButton saveActionButton;
    private javax.swing.JButton sumButton;
    private javax.swing.JSpinner translateXSpinner;
    private javax.swing.JSpinner translateYSpinner;
    private javax.swing.JLabel trueIndexLabel;
    private javax.swing.JButton untrackButton;
    // End of variables declaration//GEN-END:variables
}

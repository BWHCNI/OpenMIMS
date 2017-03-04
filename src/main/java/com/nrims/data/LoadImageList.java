/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.data;

import com.nrims.MimsJFileChooser;
import com.nrims.UI;
import com.nrims.MimsPlus;

import ij.IJ;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

/**
 *
 * @author cpoczatek
 */
public class LoadImageList {

    private com.nrims.UI ui;
    private ArrayList<String> imageList;
    private String workingDirectory;
    private String listFile;

    public LoadImageList(com.nrims.UI ui) {
        this.ui = ui;
        imageList = new ArrayList<String>();

    }

    public boolean openList() {
        MimsJFileChooser fc = new MimsJFileChooser(ui);

        fc.setPreferredSize(new java.awt.Dimension(650, 500));

        if (fc.showOpenDialog(ui) == MimsJFileChooser.CANCEL_OPTION) {
            return false;
        }

        listFile = fc.getSelectedFile().getName();
        this.workingDirectory = fc.getSelectedFile().getParent();
        File file = new File(workingDirectory, listFile);

        if (file.exists()) {
            return readList(file);
        } else {
            IJ.error("Error locating: \n \n \t " + file.getAbsolutePath());
            return false;
        }
    }

    public boolean readList(File listFile) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(listFile));
            String line;

            while ((line = br.readLine()) != null) {
                if (line.equals("") || line.equals("LIST") || line.equals("#")) {
                    continue;
                }
                imageList.add(line);
            }

            return true;
            // TODO we need more refined Exception checking here
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkList(String dir) {
        //check files exist?
        String file;
        boolean test = true;
        for (int i = 0; i < imageList.size(); i++) {
            file = dir + imageList.get(i);
            if (!new File(file).exists()) {
                test = false;
                System.out.println("Error: file in list not there.");
            }
        }

        return test;
    }

    /* this is super simple and should be temporary
     * extend or remove when ui can have multiple openers
     */
    public void simpleIMImport() {
        try {

            File file = new File(workingDirectory, imageList.get(0));
            this.ui.openFile(file, false);
            MimsPlus[] massImages = this.ui.getMassImages();
            int nMasses = this.ui.getOpener().getNMasses();

            for (int i = 0; i < nMasses; i++) {
                if (massImages[i] != null) {
                    massImages[i].setIsStack(true);
                }
            }

            String[] names = new String[nMasses];
            for (int i = 0; i < nMasses; i++) {
                if (massImages[i] != null) {
                    String oldname = massImages[i].getTitle();
                    String newname = oldname.substring(0, oldname.indexOf(" "));
                    newname += " : " + listFile;
                    massImages[i].setTitle(newname);
                    names[i] = newname;
                }
            }

            File imFile;
            for (int i = 1; i < imageList.size(); i++) {
                imFile = new File(workingDirectory, imageList.get(i));
                UI tempUi = new UI();
                boolean opened = tempUi.openFile(imFile);
                if (opened) {
                    this.ui.getMimsStackEditing().concatImages(false, tempUi);
                    for (MimsPlus image : tempUi.getMassImages()) {
                        if (image != null) {
                            image.setAllowClose(true);
                            image.close();
                        }
                    }
                    tempUi = null;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printList() {
        System.out.println("Working dir: " + workingDirectory);
        for (int i = 0; i < imageList.size(); i++) {
            System.out.println(imageList.get(i));
        }
    }

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims.data;

import javax.swing.JFileChooser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import ij.io.*;

/**
 *
 * @author cpoczatek
 */

public class LoadImageList {
    
    private com.nrims.UI ui;
    private ArrayList<String> imageList;
    private String workingDirectory;
    
    public LoadImageList(com.nrims.UI ui) {
        this.ui = ui;
        imageList = new ArrayList<String>();
    }
    
    public void openList() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(ui) == JFileChooser.CANCEL_OPTION) {
            return;
        }
        String listFile = fc.getSelectedFile().getPath();
        this.workingDirectory  = fc.getSelectedFile().getParent();
         
        readList(listFile);
    }
    
    public void readList(String listFile) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(listFile));
            String line;
            
            while ((line = br.readLine()) != null) {
                if (line.equals("") || line.equals("LIST") || line.equals("#")) {
                    continue;
                }
                imageList.add(line);
            }
        // TODO we need more refined Exception checking here
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean checkList(String dir) {
        //check files exist?
        String file;
        boolean test = true;
        for(int i=0;i<imageList.size(); i++) {
            file = dir + imageList.get(i);
            if (!new File(file).exists() ){
                test = false;
                System.out.println("Error: file in list not there.");
            }
        }
        
        return test;
    }
    
    public void dumbImport(int series) {
        //ij.plugin.Raw raw = new ij.plugin.Raw();
        
        ij.ImageStack[] teststacks = new ij.ImageStack[series];
        
        String arg = ""; 
        int stackindex;
        
        for (int i = 0; i < imageList.size(); i++) {
            
            stackindex = i % series;
            
            arg = workingDirectory + "/" + imageList.get(i);
            
            File f = new File(arg);
            FileInfo fi = new FileInfo();
            fi.fileFormat = fi.RAW;
            fi.fileName = imageList.get(i);
            fi.directory = workingDirectory;

            //hard coded parameters for images
            fi.width = 256;
            fi.height = 256;
            fi.offset = 512;
            fi.nImages = 1;
            fi.gapBetweenImages = 0;
            fi.intelByteOrder = true;
            fi.whiteIsZero = false;
            fi.fileType = FileInfo.GRAY16_UNSIGNED;
            //open dialog
            FileOpener fo = new FileOpener(fi);
            
            ij.ImagePlus iplus = fo.open(true);
            ij.process.ImageProcessor p = iplus.getProcessor();
            
            teststacks[stackindex].addSlice(fi.fileName, p);
        }
        
        //for(int i=0; i<series; i++) {
          //  teststacks[i].
        //}
    }
    
    public void printList() {
        System.out.println("Working dir: " + workingDirectory);
        for(int i=0;i<imageList.size(); i++) {
            System.out.println(imageList.get(i));
        }
    }
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 *
 * @author sreckow
 */
public class ClassManager {
//    private Hashtable<String,ArrayList<SegRoi>> classes;
//    private Hashtable<String, Integer> roiCounts;
    private ArrayList<SegClass> classes;
            
    public ClassManager() {
//        classes = new Hashtable<String, ArrayList<SegRoi>>();
//        roiCounts = new Hashtable<String, Integer>();
        classes = new ArrayList<SegClass>();
    }
    
    public boolean addClass(String className){
        SegClass segClass = getSegClass(className);
        if(segClass != null)return false;
        else{
            classes.add(new SegClass(className));
            return true;
        }
//        if(classes.containsKey(className)) return false;
//        else{
//            classes.put(className, new ArrayList<SegRoi>());
//            roiCounts.put(className, 0);
//            return true;
//        }
    }
    
    public boolean removeClass(String className){
        SegClass segClass = getSegClass(className);
        if(segClass == null)return false;
        else{
            classes.remove(segClass);
            return true;
        }
//        boolean removed = (classes.remove(className) != null);
//        roiCounts.remove(className);
//        return removed;
    }
    
    public boolean addRoi(String className, Roi roi){
        SegClass segClass = getSegClass(className);
        if(segClass == null)return false;
        else{
            segClass.addRoi(roi);
            return true;
        }
//        if(!classes.containsKey(className)) return false;
//        else{
//            roiCounts.put(className, roiCounts.get(className)+1);
//            classes.get(className).add(new SegRoi(roi, className, (byte)-1, roiCounts.get(className)));
//            return true;
//        }
    }
    
    public String[] getClasses(){
        String[] names = new String[classes.size()];
        for(int i=0; i< names.length; i++) names[i] = classes.get(i).name;
        return names;
//        String[] names = new String[classes.size()];
//        return classes.keySet().toArray(names);
    }
    
    public SegRoi[] getRois(String className){     
        SegClass segClass = getSegClass(className);
        if(segClass == null) return new SegRoi[0];        
        else {
            SegRoi[] rois = new SegRoi[0];//segClass.rois.size()];
            return segClass.rois.toArray(rois);
        }
//        SegRoi[] rois = new SegRoi[classes.get(className).size()];
//        return classes.get(className).toArray(rois);
    }
   
    public boolean renameClass(String oldName, String newName){
        SegClass segClass = getSegClass(oldName);
        if(segClass == null) return false;
        else if(getSegClass(newName) != null) return false;
            else{
                segClass.name = newName;
                return true;
            }
//        if(!classes.containsKey(oldName) || classes.containsKey(newName)) return false;
//        else{
//            ArrayList<SegRoi> tmp = classes.remove(oldName);
//            classes.put(newName, tmp);
//            return true;
//        }        
    }
    
    private SegClass getSegClass(String className){
        for(SegClass segClass: classes) if (segClass.name.equals(className)) return segClass;
        return null;
    }
    
    private class SegClass{
        private String name;
        private ArrayList<SegRoi> rois;
        
        public SegClass(String name){
            this.name = name;
            rois = new ArrayList<SegRoi>();
        }
        
        public void addRoi(Roi roi){
            rois.add(new SegRoi(roi,name,rois.size()));
        }        
    }
//    public static ClassManager loadROIs(String filename) throws FileNotFoundException, IOException{
//        ClassManager cm = new ClassManager();
//        BufferedReader br = new BufferedReader(new FileReader(filename));
//        String line;
//        while((line = br.readLine())!= null){
//            String[] data = line.split("\t");
//            String className = data[0];
//            int npoints = Integer.parseInt(data[1]);
//            String[] xData = data[2].split(",");
//            String[] yData = data[3].split(",");
//            int[] xpoints = new int[npoints];
//            int[] ypoints = new int[npoints];
//            for(int i=0; i<npoints; i++){
//                xpoints[i] = Integer.parseInt(xData[i]);
//                ypoints[i] = Integer.parseInt(yData[i]);
//            }
//            cm.addClass(className); // does nothing, if class already exists
//            cm.addRoi(className, new PolygonRoi(xpoints, ypoints, npoints, Roi.FREEROI));
//        }
//        return cm;
//    }
//    
//    public static boolean saveROIs(ClassManager cm, String filename) throws IOException{       
//        BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
//        for(String className : cm.getClasses()){
//            for(SegRoi roi : cm.getRois(className)){
//                int npoints = roi.getRoi().getPolygon().npoints;
//                bw.append(className + "\t");
//                bw.append(npoints + "\t");
//                int[] xpoints = roi.getRoi().getPolygon().xpoints;
//                int[] ypoints = roi.getRoi().getPolygon().ypoints;
//                for(int i=0; i< npoints; i++){
//                    bw.append(String.valueOf(xpoints[i]) + ",");
//                }
//                bw.append("\t");
//                for(int i=0; i< npoints; i++){
//                    bw.append(String.valueOf(ypoints[i]) + ",");
//                }
//                bw.append("\n");
//            }
//        }
//        bw.close();
//        return true;
//    }
    
//    // inner class SegRoi which stores information about ROIs that were derived from a segmentation
//    private static class SegRoi{               
//        private Roi roi;
//        private ShapeRoi shapeRoi;    
//        private String className;
//        private String ID;
//        private int size;
//
//        public SegRoi(Roi roi, String className, String ID) {
//            this.roi = roi;
//            this.className = className;
//            this.ID = ID;
//        }                
//
//        public String getClassName() {
//            return className;
//        }
//
//        public String getID(){
//            return ID;
//        }
//
//        public Roi getRoi() {
//            return roi;
//        }
//
//        public void setRoi(Roi roi){
//            setRoi(roi, null);
//        }
//
//        public void setRoi(Roi roi, ShapeRoi shape){
//            this.roi = roi;
//            this.shapeRoi = shape;
//        }
//
//        public ShapeRoi getShapeRoi(){
//            if(shapeRoi==null) shapeRoi = new ShapeRoi(roi);
//            return shapeRoi;
//        }
//    }
}

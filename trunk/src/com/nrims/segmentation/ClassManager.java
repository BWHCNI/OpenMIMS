package com.nrims.segmentation;

import ij.gui.Roi;
import java.util.ArrayList;

/**
 *
 * @author sreckow
 */
public class ClassManager implements Cloneable {
    private ArrayList<SegClass> classes;
            
    public ClassManager() {
        classes = new ArrayList<SegClass>();
    }
    
    public boolean addClass(String className){
        SegClass segClass = getSegClass(className);
        if(segClass != null)return false;
        else{
            classes.add(new SegClass(className));
            return true;
        }
    }
    
    public boolean removeClass(String className){
        SegClass segClass = getSegClass(className);
        if(segClass == null)return false;
        else{
            classes.remove(segClass);
            return true;
        }
    }
    
    public boolean addRoi(String className, Roi roi){
        SegClass segClass = getSegClass(className);
        if(segClass == null)return false;
        else{
            segClass.addRoi(roi);
            return true;
        }
    }
    
    public String[] getClasses(){
        String[] names = new String[classes.size()];
        for(int i=0; i< names.length; i++) names[i] = classes.get(i).name;
        return names;
    }
    
    public SegRoi[] getRois(String className){     
        SegClass segClass = getSegClass(className);
        if(segClass == null) return new SegRoi[0];        
        else {
            SegRoi[] rois = new SegRoi[0];//segClass.rois.size()];
            return segClass.rois.toArray(rois);
        }
    }
   
    public boolean renameClass(String oldName, String newName){
        SegClass segClass = getSegClass(oldName);
        if(segClass == null || oldName.equals(newName) || getSegClass(newName) != null) return false;
        else{
            segClass.name = newName;
            return true;
        }
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
            rois.add(new SegRoi(roi,name,rois.size()+1));
        }        
    }

    @Override
    public Object clone(){
       ClassManager newObject = new ClassManager();
        for(String segClass : this.getClasses()){
            newObject.addClass(segClass);
            for(SegRoi segRoi : this.getRois(segClass)){
                newObject.addRoi(segClass, segRoi.getRoi());
            }
        }
        return newObject;
    }
}

package com.nrims.segmentation;

import java.util.Hashtable;

/**
 *
 * @author reckow
 */
public class SegmentationProperties implements java.io.Serializable {

static final long serialVersionUID = -2057325729625438428L;

    public enum PropertyType{
        BOOLEAN, DOUBLE, INTEGER, STRING
    }

    private Hashtable<String, Object> properties;
//    private Hashtable<String, PropertyType> types;
//    private Hashtable<String, Object> defaults;
//    private Hashtable<String, Object[]> options;
//    private Hashtable<String, Object[]> model;

    public SegmentationProperties(){
        properties = new Hashtable<String, Object>();
//        types      = new Hashtable<String, PropertyType>();
//        defaults   = new Hashtable<String, Object>();
//        options    = new Hashtable<String, Object[]>();
//        model      = new Hashtable<String, Object[]>();

//        initDefaults();
    }

    public void setValueOf(String key, Object value){
        properties.put(key, value);
    }

    public Object getValueOf(String key){
        return properties.get(key);
    }

/*
    private void initDefaults(){
        // TO DO read this from a configuration file
        defaults.put(SegmentationEngine.ENGINE, "lib-SVM");
        options.put(SegmentationEngine.ENGINE, new Object[]{"lib-SVM"});
        types.put(SegmentationEngine.ENGINE, PropertyType.STRING);
    }

    public PropertyType getTypeOf(String key){
        return types.get(key);
    }

    public Object getDefaultOf(String key){
        return defaults.get(key);
    }

    public Object[] getOptionsOf(String key){
        return options.get(key);
    }

    public boolean setOptionOf(String key, Object[] values){

    options.put(key, values);
    return true;
    }

    public boolean setTypeOf(String key, PropertyType type){
        types.put(key, type);
        return true;
    }

    public boolean removeValueOf(String key){
        if(!properties.containsKey(key)){return false;}
        properties.remove(key);
        return true;
    }

    public boolean removeOptionOf(String key){
        if(!options.containsKey(key)){return false;}
        options.remove(key);
        return true;
    }

    public boolean removeTypeOf(String key){
        if(!types.contains(key)){return false;}
        types.remove(key);
    return false;
    }

    public boolean setModelOf(String key, Object[] value){
        if(model.contains(key)){return false;}
        else{
            model.put(key, value);
            return true;
        }
    }
    public Object getModelOf(String key){

        return model.get(key);
    }
  }
 **/
}
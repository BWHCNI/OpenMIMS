package com.nrims.segmentation;

import java.util.ArrayList;
import javax.swing.SwingWorker;

/**
 *
 * @author sreckow
 */
public abstract class SegmentationEngine extends SwingWorker<Boolean,Void>{   
    private final SegmentationProperties props;    
    private final int type;
    private final ArrayList<ArrayList<ArrayList<Double>>> data;    
    private boolean success = false;
    private byte[] classification;

    public static final int TRAIN_TYPE = 0, PRED_TYPE = 1;

    // use these values as keys for storing the engine name and the model object in the SegmentationProperties object
    // additional entries can be defined by each SegmentationEngine implementation as needed
    public static final String
            ENGINE      = "engine", // name of the segmentation engine class (e.g. lib-SVM)
            MODEL       = "model";  // contains the model object (each segEngine implementation has to know how to deal

    public SegmentationEngine(int type, ArrayList<ArrayList<ArrayList<Double>>> data, SegmentationProperties props){
        this.type = type;
        this.props = props;
        this.data = data;
    }

    /**
     * This method is called automatically before the engine is executed and is supposed to check on the
     * parameters in the properties object. Default values should be set here, if necessary.
     * @param props the <code>SegmentationProperties</code> object that should be checked
     */
    protected abstract void checkParams(SegmentationProperties props);

    /**
     * This method is called automatically when an engine is executed that was initialized with <code>type==TRAIN</code>
     * @return <code>true</code>, if training succeeded and <code>false</code> otherwise
     * @throws java.lang.Exception any exception should be thrown so it can be caught outside the SwingWorker-Thread
     */
    protected abstract boolean train() throws Exception;

    /**
     * This method is called automatically when an engine is executed that was initialized with <code>type==PRED</code>
     * @return <code>true</code>, if prediction succeeded and <code>false</code> otherwise
     * @throws java.lang.Exception any exception should be thrown so it can be caught outside the SwingWorker-Thread
     */
    protected abstract boolean predict() throws Exception;

    public ArrayList<ArrayList<ArrayList<Double>>> getData(){
        return data;
    }

    public SegmentationProperties getProperties(){
        return props;
    }

    public byte[] getClassification(){
        return classification;
    }

    public void setClassification(byte[] classification){
        this.classification = classification;
    }

    public boolean getSuccess() {
        return success;
    }
  
    @Override
    protected Boolean doInBackground() throws Exception{
        checkParams(props);
        switch(type){
            case TRAIN_TYPE:
                return train();
            case PRED_TYPE:
                return predict();
            default:
                return false;
        }
    }

    @Override
    protected void done() {
        try {
            success = get();
        } catch (Exception ex) {
            System.out.println("error in segmentation engine!\n");
            ex.printStackTrace();
        }      
    }   
}

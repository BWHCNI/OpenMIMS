/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 *
 * @author sreckow
 */
public abstract class SegmentationEngine extends SwingWorker<byte[],Void>{
    protected final ArrayList<ArrayList<ArrayList<Double>>> trainingData;
    protected final ArrayList<ArrayList<ArrayList<Double>>> testData;    
    protected final Properties props;
    protected final String[] classNames;
    protected byte[] classification;
    protected int minWork = 0;
    protected int maxWork = 100;

    
    public SegmentationEngine(ArrayList<ArrayList<ArrayList<Double>>> trainingData,ArrayList<ArrayList<ArrayList<Double>>> testData, String[] classNames, Properties props){
        this.trainingData = trainingData;
        this.testData = testData;
        this.classNames = classNames;
        this.props = props;
        this.classification = new byte[0];
    }         
    
    public byte[] getClassification(){
        return classification;
    }
     
    public int getMaxWork() {
        return maxWork;
    }

    public int getMinWork() {
        return minWork;
    }

    @Override
    protected void done() {
        try {
            classification = get();
        } catch (InterruptedException ex) {
            Logger.getLogger(SegmentationEngine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(SegmentationEngine.class.getName()).log(Level.SEVERE, null, ex);
        }      
    }   
}

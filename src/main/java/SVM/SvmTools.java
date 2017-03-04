/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package SVM;

import com.nrims.segmentation.SegmentationProperties;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author gormanns
 */
public class SvmTools {

    

    //Create flag in UI to set the Plugin into validation mode
     /*To-Do: Load image
     *       Extract data-->ExtractFeatures from SegUtils.extractFeatures
     *          This requires the SegUtils.initPrepSeg
      *         MimsPlus[] images = getImages();
                segUtil = SegUtils.initPrepSeg(images, colorImageIndex, localFeatures, trainClasses);
      * 
     *       Run shuffle method
     *       Use different subsssets to run SVM=> For loop and use SVMengine
             
     */
    
    /*Maybe integrate  this feature subset generation at a point during SVM run...but thats gonna be messy*/


   
    public static void doubleCV(double[] trSetSize, List<List<List<Double>>> data, SegmentationProperties props,String outpath){

SegmentationProperties tempProps =  props;
        for(int t=0;t<trSetSize.length;t++){
        

        List<List<List<Double>>> newTrainData= new ArrayList<List<List<Double>>>();
        List<List<List<Double>>> newTestData= new ArrayList<List<List<Double>>>();
        for(int i=0;i<data.size();i++){

            List<List<Double>> tempData= data.get(i);
            java.util.Collections.shuffle(tempData);
          //ArrayList<ArrayList<Double>> subTempData= data.get(i);


           List<List<Double>> classTrainData = new ArrayList<List<Double>>();
           for(int j=0; j<new Double(Math.floor(tempData.size()*trSetSize[t])).intValue();j++){


               classTrainData.add(tempData.get(j));
           }


            newTrainData.add(i,classTrainData);

            
            
           List<List<Double>> classTestData = new ArrayList<List<Double>>();
           for(int j=new Double(Math.floor(tempData.size()*trSetSize[t])).intValue()+1; j<tempData.size();j++){
           
           
               classTestData.add(tempData.get(j));
           }
           newTestData.add(i, classTestData);
        }

        SvmEngine svmV= new SvmEngine(1, newTrainData, tempProps);
        svmV.checkParams(tempProps);
        try{
            svmV.train();
        
        }catch(Exception e){

            System.out.println("Error in DoubleCV");
        }
        tempProps = svmV.getProperties();
        svmV = new SvmEngine(1, newTestData, tempProps);

        try{
            svmV.predict();
            ArrayList<String> original =svmV.convertData();
            byte[] result=svmV.getClassification();
            BufferedWriter bfw= new BufferedWriter(new FileWriter(new File(outpath+"_"+trSetSize[t])));

            int correct=0;
            for(int a=0;a<result.length;a++){
                //System.out.println(new Integer(original.get(a).substring(0, 1)));
                int first=new Integer(original.get(a).substring(0, 1));
                int second=result[a];
                bfw.append(first+"\t"+second+"\n");
                if(first==second){correct+=1;}
            }
            bfw.append("Accurracy:"+(double)correct/result.length);
            bfw.close();
        }catch(Exception e){

            System.out.println("Error in DoubleCV");
        }
    }
    }
}

package SVM;

import SVM.libsvm.svm_model;
import com.nrims.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sreckow
 */
public class SvmEngine extends com.nrims.SegmentationEngine{

    public ArrayList<String> convTrainingData;
    public ArrayList<String> convTestData;
    
    public SvmEngine(ArrayList<ArrayList<ArrayList<Double>>> trainingData,  ArrayList<ArrayList<ArrayList<Double>>> testData, String[] names, Properties props){
       
        super(trainingData, testData, names, props);
        this.convTrainingData = new ArrayList<String>();
        this.convTestData = new ArrayList<String>();
        
        //this. description = "lib-SVM";
    }
    public void writeData(){
        
        
        for (int u =0; u < this.trainingData.size();u++){
            
                Iterator pointIT = trainingData.get(u).iterator();
                while (pointIT.hasNext()){
                
                    //Double[] currentFeatures = (Double[]) pointIT.next();
                    Iterator featureIT = ((ArrayList) pointIT.next()).iterator();
                    String res= u+"";
                    int i=0;
                    while (featureIT.hasNext())
                    {// for (int i =0; i<currentFeatures.length; i++){
                    
                        res += " "+ (i+1) +":"+ (Double)featureIT.next();
                        i++;
                    }
                    this.convTrainingData.add(res);
//                    bfw.append(res);
//                    bfw.append("\n");
                }
            }
        
        for (int u =0; u < this.testData.size();u++){
            
                Iterator pointIT = testData.get(u).iterator();
                while (pointIT.hasNext()){
                
                    //Double[] currentFeatures = (Double[]) pointIT.next();
                    Iterator featureIT = ((ArrayList) pointIT.next()).iterator();
                    String res= 99+"";
                    int i=0;
                    while (featureIT.hasNext())
                    {// for (int i =0; i<currentFeatures.length; i++){
                    
                        res += " "+ (i+1) +":"+ (Double)featureIT.next();
                        i++;
                    }
                    this.convTestData.add(res);
//                    bfw.append(res);
//                    bfw.append("\n");
                }
            }
//            bfw.close();
//        
        
        

//            
//            
//            BufferedWriter bfw = new BufferedWriter(new FileWriter(new File("train")));
//            
//            for (int u =0; u < this.trainingData.size();u++){
//            
//                Iterator pointIT = trainingData.get(u).iterator();
//                while (pointIT.hasNext()){
//                
//                    //Double[] currentFeatures = (Double[]) pointIT.next();
//                    Iterator featureIT = ((ArrayList) pointIT.next()).iterator();
//                    String res= u+1+"";
//                    int i=0;
//                    while (featureIT.hasNext())
//                    {// for (int i =0; i<currentFeatures.length; i++){
//                    
//                        res += " "+ (i+1) +":"+ (Double)featureIT.next();
//                        i++;
//                    }
//                    bfw.append(res);
//                    bfw.append("\n");
//                }
//            }
//            bfw.close();
//            
//            
//            
//            bfw = new BufferedWriter(new FileWriter(new File("test_1")));
//            for (int u =0; u < this.testData.size();u++){
//            
//                
//                Iterator pointIT = testData.get(u).iterator();
//                
//                
//                
//                while (pointIT.hasNext()){
//                
//                    //Double[] currentFeatures = (Double[]) pointIT.next();
//                    Iterator featureIT = ((ArrayList) pointIT.next()).iterator();
//                    String res= u+1+"";
//                    int i=0;
//                    while (featureIT.hasNext())
//                    {// for (int i =0; i<currentFeatures.length; i++){
//                    
//                        res += " "+ (i+1) +":"+ (Double)featureIT.next();
//                        i++;
//                    }
//                    bfw.append(res);
//                    bfw.append("\n");
//                }
//            }
//            bfw.close();
//            
//        } catch (IOException ex) {
//            
//            Logger.getLogger(SvmEngine.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
    @Override
    @SuppressWarnings("empty-statement")
    
    protected byte[] doInBackground() throws Exception {
        this.setProgress(10);
        this.writeData();
        svm_scale sc = new svm_scale();
        String[] scaleSetup = {"-s","params","train"};
        ArrayList<String> tempTrainData =sc.run(scaleSetup, this.convTrainingData);
        this.setProgress(25);
        svm_train st = new svm_train();
        String[] trainSetup= {"train.scale", "train.model"};
        svm_model model = st.run(trainSetup, tempTrainData);
        
        this.setProgress(50);
        sc = new svm_scale();
        String[] scaleSetupTest = {"-r","params","test_1"};
        sc.setDatatype(false);
        ArrayList<String> tempTestData = sc.run(scaleSetupTest, this.convTestData);
        this.setProgress(75);
        svm_predict sp = new svm_predict();
        String[] predictSetup = {"test.scale", "train.model", "test.predict"};
        ArrayList<String> prediction = sp.predict(tempTestData, model,0);
        this.setProgress(95);
        byte[] classMap = createOutput(prediction);

        Runtime.getRuntime().exec("rm params");

       
        return classMap;// new com.nrims.SegmentationResult(classMap, classNames, 512, 512);
    }

    private byte[] createOutput(ArrayList<String> data){            
        byte[] pixels = new byte[data.size()];
        Iterator dataIT = data.iterator();
        int i=0;
        while(dataIT.hasNext()){
            pixels[i] =new Byte ((((String) dataIT.next()).split("\\.")[0]));
            //pixels[i]=a.getBytes()[0];
            i++;
        }
        return pixels;
//        ArrayList<Byte> classesTmp = new ArrayList<Byte>();
//        try{
//            BufferedReader br = new BufferedReader(new FileReader(filename));
//            String line;
//            int index = 0;
//            while((line = br.readLine())!= null){                    
//                line = line.trim();
//                if (line.length() == 0) continue;
//                Byte b = new Byte(line.substring(0, 1));
//                pixels[index++] = b;
//                if(!classesTmp.contains(b))classesTmp.add(b);                    
//            }
//            return pixels;
//        }catch(Exception e){
//            e.printStackTrace();
//            return null;
//        }
    }

}

package com.nrims.segmentation;

import com.nrims.SegmentationForm;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author reckow
 */
public class SegFileUtils {

    // constants used for writing the .zip file
    private static final String FEATURE_FILE_HEADER = "#V2";
    private static final String RATIOS       = "ratios = ";
    private static final String MASSES      = "masses = ";
    private static final String COLOR_INDEX   = "color_index = ";
    private static final String TRAINPREFIX = "train";
    private static final String PREDPREFIX  = "pred";
    private static final String CLASSFILE   = "class";
    private static final String DESCFILE    = "desc";
    private static final String FEATFILE    = "feat";
    private static final String PROPFILE    = "prop";
    private static final String SEPARATOR   = "/";      // there's no portable way of writing .zip files, so we
                                                        // consistently use forward slash as file separator

    public static void save(String filename, SegmentationProperties props, ClassManager train, int[] classColors, int colorImageIndex,
        boolean[] massImageFeatures, ArrayList<String> ratioImages, int[] localFeatures, byte[] classification, ClassManager pred){
        try{
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(filename));
            DataOutputStream out = new DataOutputStream(zos);
            BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(out));

            // write properties
            zos.putNextEntry(new ZipEntry(PROPFILE));
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(out);
            oos.writeObject(props);

            // write train classes, description and class colors (if available)
            writeClassManager(train, zos, TRAINPREFIX);
            zos.putNextEntry(new ZipEntry(DESCFILE));
            String[] names = train.getClasses();
            for(int i=0; i<names.length; i++){
                String line = names[i] + "\t";
                if (classColors!= null && i <classColors.length) line += classColors[i];
                line += "\n";
                bwr.write(line);
                bwr.flush();
            }

            // write features
            writeFeatures(zos, colorImageIndex, massImageFeatures, ratioImages, localFeatures);
            //boolean[] ratioImageFeature = new boolean[]{true, false, true};
            //writeFeatures(zos, colorImageIndex, massImageFeatures, ratioImageFeature, localFeatures);

            // write classification (if available)
            if(classification != null && classification.length > 0){
                zos.putNextEntry(new ZipEntry(CLASSFILE));
                zos.write(classification);
            }

            //write predicted classes (if available)
            if(pred != null && pred.getClasses().length > 0){
                writeClassManager(pred, zos, PREDPREFIX);
            }

            out.close();
            zos.close();
        }catch(java.io.IOException e){
            System.out.println("Error saving segmentation result!\n" + e.toString());
            return;
        }
    }

    public static void load(String filename, SegmentationForm form){
        ArrayList<String> classNames = new ArrayList<String>();
        ArrayList<Integer> classColors_tmp = new ArrayList<Integer>();
        int colorImageIndex = -1;
        ClassManager trainClasses = new ClassManager();
        boolean[] massImageFeatures = new boolean[4];
        boolean[] ratioImageFeatures = new boolean[3];
        int[] localFeatures = new int[3];
        SegmentationProperties properties = new SegmentationProperties();
        ClassManager predClasses = new ClassManager();
        byte[] classification = new byte[0];

        try {
            ZipInputStream in = new ZipInputStream(new FileInputStream(filename));
            ZipEntry entry = in.getNextEntry();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while (entry!=null) {
                String name[] = entry.getName().split(SEPARATOR);
                if (name[0].equals(TRAINPREFIX)){
                    // load train classes
                    loadRoi(in, name[1], name[2], trainClasses);
                }else if(name[0].equals(DESCFILE)){
                    // load description and class colors (if available)
                    String line;
                    while((line = br.readLine())!=null){
                        String[] data = line.split("\t");
                        classNames.add(data[0]);
                        if(data.length>1) classColors_tmp.add(Integer.parseInt(data[1]));
                    }
                }else if(name[0].equals(FEATFILE)){
                    // load features (see comments in the "writeFeatures" method for details)
                    String line = br.readLine();
                    byte[] bytes = line.getBytes();
                    if (line.matches(FEATURE_FILE_HEADER)) {

                       // Get color index.
                       line = br.readLine(); String colorImageIndexString = line.substring(line.lastIndexOf(" ")+1);
                       colorImageIndex = new Integer(colorImageIndexString).intValue();

                       // Get mass
                       String[] tfs = br.readLine().substring(MASSES.length()).trim().split(" ");
                       String[] rts = br.readLine().substring(RATIOS.length()).trim().split(",");
                       while((line = br.readLine())!=null) {
                          if (line.matches(SegmentationForm.FEATURE_NAMES[0]))
                             localFeatures[0] = 1;
                          else if (line.matches(SegmentationForm.FEATURE_NAMES[1]))
                             localFeatures[1] = 1;
                          else if (line.startsWith(SegmentationForm.FEATURE_NAMES[2]))
                             localFeatures[2] = new Integer(line.substring(line.lastIndexOf(" ")+1)).intValue();
                       }

                       // mass image features.
                       massImageFeatures = new boolean[tfs.length];
                       for (int i = 0; i < tfs.length; i++) {
                          massImageFeatures[i] = new Boolean(tfs[i]);
                       }

                       // ratio image feature.
                       ratioImageFeatures = new boolean[]{false, false, false};
                       for (int i = 0; i < rts.length; i++) {
                          if (rts[i].matches("1 0"))
                             ratioImageFeatures[0] = true;
                          else if (rts[i].matches("3 2"))
                             ratioImageFeatures[1] = true;
                          else if (rts[i].length() > 1)
                             ratioImageFeatures[2] = true;
                       }
                    } else {
                        //version 1 case
                       /*original
                       colorImageIndex = in.read();
                       for(int i=0; i<massImageFeatures.length; i++)  massImageFeatures[i]  = (in.read()==1);
                       for(int i=0; i<2; i++) ratioImageFeatures[i] = (in.read()==1);
                       for(int i=0; i<localFeatures.length; i++)      localFeatures[i]      = in.read();
                        */
                        int offset = 0;
                        colorImageIndex = bytes[offset];
                        offset++;
                        for (int i = 0; i < massImageFeatures.length; i++) {
                            massImageFeatures[i] = (bytes[offset] == 1);
                            offset++;
                        }
                        for (int i = 0; i < 2; i++) {
                            ratioImageFeatures[i] = (bytes[offset] == 1);
                            offset++;
                        }
                        for (int i = 0; i < localFeatures.length; i++) {
                            localFeatures[i] = bytes[offset];
                            offset++;
                        }
                        
                    }
                }else if(name[0].equals(PROPFILE)){
                    // load properties
                    java.io.ObjectInputStream ois = new java.io.ObjectInputStream(in);
                    properties = (SegmentationProperties) ois.readObject();
                }else if (name[0].equals(CLASSFILE)){
                    // load classification
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0){
                        byte[] tmp = new byte[classification.length + len];
                        System.arraycopy(classification, 0, tmp ,0, classification.length);
                        System.arraycopy(buf, 0, tmp, classification.length, len);
                        classification = tmp;
                    }
                }else if (name[0].equals(PREDPREFIX)){
                    // load predicted classes
                    loadRoi(in, name[1], name[2], predClasses);
                }

                entry = in.getNextEntry();
            }

            int[] classColors = new int[classColors_tmp.size()];
            for(int i=0; i<classColors.length; i++) classColors[i] = classColors_tmp.get(i);

            form.setClassColors(classColors);
            form.setClassNames(classNames.toArray(new String[classNames.size()]));
            form.setColorImageIndex(colorImageIndex);
            form.setProperties(properties);
            form.setRatioImageFeatures(ratioImageFeatures);
            form.setMassImageFeatures(massImageFeatures);
            form.setLocalFeatures(localFeatures);
            form.setTrainClasses(trainClasses);
            form.setPredClasses(predClasses);
            form.setClassification(classification);
        } catch (Exception e) {
            System.out.println("Error loading model!\n" + e.toString());
        }
    }

//    public static void saveModel(String fileName, ClassManager train, int[] classColors, int colorImageIndex, SegmentationProperties props,
//        boolean[] massImageFeatures, boolean[] ratioImageFeatures, int[] localFeatures){
//        try{
//             // add MODEL.zip extension if missing
//            if(!fileName.endsWith("MODEL.zip")) fileName = fileName + MODELEXT;
//            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fileName));
//            DataOutputStream out = new DataOutputStream(zos);
//            BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(out));
//
//            // write class info
//            writeClassManager(train, zos, TRAINPREFIX);
//            zos.putNextEntry(new ZipEntry(DESCFILE));
//            String[] names = train.getClasses();
//            for(int i=0; i<names.length; i++){
//                String line = names[i] + "\t";
//                if (classColors!= null && i <classColors.length) line += classColors[i];
//                line += "\n";
//                bwr.write(line);
//                bwr.flush();
//            }
//
//            // write features
//            writeFeatures(zos, colorImageIndex, massImageFeatures, ratioImageFeatures, localFeatures);
//
//            // write setup
//            zos.putNextEntry(new ZipEntry(PROPFILE));
//            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(out);
//            oos.writeObject(props);
//            out.close();
//        }catch(java.io.IOException e){
//            System.out.println("Error saving segmentation result!\n" + e.toString());
//            return;
//        }
//    }
//
//    public static void savePrediction(String fileName, ClassManager pred, byte[] classification, String modelFile){
//        byte[] model = new byte[0];
//        try{    // try to read model file
//            java.io.InputStream in;
//            if(modelFile.endsWith(MODELEXT)) in = new FileInputStream(modelFile);
//            else{   // this is supposedly a zip file containing the .MODEL.zip file
//                ZipInputStream zin = new ZipInputStream(new FileInputStream(modelFile));
//                ZipEntry entry = zin.getNextEntry();
//                while (entry!=null){
//                    if(entry.getName().endsWith(MODELEXT)) in = zin; break;
//                }
//                throw new java.io.IOException();
//            }
//            byte[] buf = new byte[1024];
//            int len;
//            while ((len = in.read(buf)) > 0){
//                byte[] tmp = new byte[model.length + len];
//                System.arraycopy(model, 0, tmp ,0, model.length);
//                System.arraycopy(buf, 0, tmp, model.length, len);
//                model = tmp;
//            }
//        }catch(java.io.IOException e){
//            System.out.println("Could not read model file!\nPrediction will be saved without model information!\n" + e.toString());
//        }
//
//        try{
//            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fileName));
//            DataOutputStream out = new DataOutputStream(zos);
//
//            // write class info
//            writeClassManager(pred, zos, PREDPREFIX);
//
//            // write classification
//            zos.putNextEntry(new ZipEntry(CLASSFILE));
//            zos.write(classification);
//
//            // write model
//            if(model.length>0){
//                zos.putNextEntry(new ZipEntry(modelFile));
//                zos.write(model);
//            }
//
//            out.close();
//        }catch(java.io.IOException e){
//            System.out.println("Error saving prediction!\n" + e.toString());
//            return;
//        }
//    }

    private static void writeClassManager(ClassManager classes, ZipOutputStream zos, String prefix) throws IOException{
        RoiEncoder re = new RoiEncoder(zos);
        for(String className : classes.getClasses()){
            for(SegRoi segRoi: classes.getRois(className)){
                String label = prefix + SEPARATOR + className + SEPARATOR + segRoi.getID();
                zos.putNextEntry(new ZipEntry(label));
                re.write(segRoi.getRoi());
            }
        }
    }

    private static void writeFeatures(ZipOutputStream zos, int colorImageIndex, boolean[] massImageFeatures, ArrayList<String> ratioImages, int[] localFeatures) throws IOException{

      String[] featureNames = SegmentationForm.FEATURE_NAMES;
      byte[] ms = MASSES.getBytes();
      byte[] rs = RATIOS.getBytes();
      byte[] nl = new String("\n").getBytes();
      byte[] sp = new String(" ").getBytes();
      byte[] cm = new String(",").getBytes();
      byte[] tr = new String("true").getBytes();
      byte[] fl = new String("false").getBytes();

      // Begin to write file.
      zos.putNextEntry(new ZipEntry(FEATFILE));

      // Write the header.
      zos.write(FEATURE_FILE_HEADER.getBytes());
      zos.write(nl);

      // Write color index.
      zos.write(COLOR_INDEX.getBytes());
      zos.write(new Integer(colorImageIndex).toString().getBytes());
      zos.write(nl);

      // Write the masses being used.
      // E.g. true false true false false true true
      //        0    1    2     3    4     5    6
      zos.write(ms);
      for (boolean b : massImageFeatures){
         if(b) zos.write(tr);
         else  zos.write(fl);
         zos.write(sp);
      }
      zos.write(nl);

      // Write the numerator-denominator pair of ratio images being used.
      // E.g.
      //    1 0, 3 2, 6 5
      zos.write(rs);
      int i = 0;
      for (String b : ratioImages){
         if (i!=0) zos.write(cm);
         zos.write(b.getBytes());
         i++;
      }
      zos.write(nl);

      // Write the features and parameters being used.
      // E.g.
      //    Neighborhood
      //    Gradient
      //    Radius 5
      int j = 0;
      for (int b : localFeatures){
         if (b > 0) {
         switch (j) {
            case 0:
               zos.write(featureNames[j].getBytes());
               break;
            case 1:
               zos.write(featureNames[j].getBytes());
               break;
            case 2:
               zos.write(featureNames[j].getBytes());
               zos.write(sp);
               zos.write((new Integer(b)).toString().getBytes());
               break;
         }
         zos.write(nl);
         }
         j++;
      }
      zos.write(nl);
      zos.closeEntry();
    }

//    public static void loadModel(String modelFile, SegmentationForm form){
//        try {
//            FileInputStream fs = new FileInputStream(modelFile);
//            loadModel(fs, form);
//            fs.close();
//        } catch (IOException e) {
//            System.out.println("Error loading model!\n" + e.toString());
//        }
//    }
//
//    // the submitted stream is not closed; this has to be handled outside this method
//    public static void loadModel(java.io.InputStream model, SegmentationForm form){
//        ArrayList<String> classNames = new ArrayList<String>();
//        ArrayList<Integer> classColors_tmp = new ArrayList<Integer>();
//        ClassManager trainClasses = new ClassManager();
//        boolean[] massImageFeatures = new boolean[4];
//        boolean[] ratioImageFeatures = new boolean[2];
//        int[] localFeatures = new int[3];
//        SegmentationProperties properties = new SegmentationProperties();
//
//        try {
//            ZipInputStream in = new ZipInputStream(model);
//            ZipEntry entry = in.getNextEntry();
//            BufferedReader br = new BufferedReader(new InputStreamReader(in));
//            while (entry!=null) {
//                String name[] = entry.getName().split(SEPARATOR);
//                if (name[0].equals(TRAINPREFIX))  loadRoi(in, name[1], name[2], trainClasses);
//                else if(name[0].equals(DESCFILE)){
//                    String line;
//                    while((line = br.readLine())!=null){
//                        String[] data = line.split("\t");
//                        classNames.add(data[0]);
//                        if(data.length>1) classColors_tmp.add(Integer.parseInt(data[1]));
//                    }
//                }else if(name[0].equals(FEATFILE)){
//                    for(int i=0; i<massImageFeatures.length; i++)  massImageFeatures[i]  = (in.read()==1);
//                    for(int i=0; i<ratioImageFeatures.length; i++) ratioImageFeatures[i] = (in.read()==1);
//                    for(int i=0; i<localFeatures.length; i++)      localFeatures[i]      = in.read();
//                }else if(name[0].equals(PROPFILE)){
//                    java.io.ObjectInputStream ois = new java.io.ObjectInputStream(in);
//                    properties = (SegmentationProperties) ois.readObject();
//                }
//                entry = in.getNextEntry();
//            }
//
//            int[] classColors = new int[classColors_tmp.size()];
//            for(int i=0; i<classColors.length; i++) classColors[i] = classColors_tmp.get(i);
//
//            form.setClassColors(classColors);
//            form.setClassNames(classNames.toArray(new String[classNames.size()]));
//            form.setProperties(properties);
//            form.setRatioImageFeatures(ratioImageFeatures);
//            form.setMassImageFeatures(massImageFeatures);
//            form.setLocalFeatures(localFeatures);
//            form.setTrainClasses(trainClasses);
//        } catch (Exception e) {
//            System.out.println("Error loading model!\n" + e.toString());
//        }
//    }
//
//    public static void loadPrediction(String filename, SegmentationForm form){
//        ClassManager predClasses = new ClassManager();
//        byte[] classification = new byte[0];
//
//        try {
//            ZipInputStream in = new ZipInputStream(new FileInputStream(filename));
//            ZipEntry entry = in.getNextEntry();
//            while (entry!=null) {
//                String name[] = entry.getName().split(SEPARATOR);
//                if (name[0].equals(PREDPREFIX))  loadRoi(in, name[1], name[2], predClasses);
//                else if (name[0].equals(CLASSFILE)){
//                    byte[] buf = new byte[1024];
//                    int len;
//                    while ((len = in.read(buf)) > 0){
//                        byte[] tmp = new byte[classification.length + len];
//                        System.arraycopy(classification, 0, tmp ,0, classification.length);
//                        System.arraycopy(buf, 0, tmp, classification.length, len);
//                        classification = tmp;
//                    }
//                }else if(name[0].endsWith(MODELEXT)){
//                    loadModel(in, form);
//                }
//                entry = in.getNextEntry();
//            }
//            in.close();
//
//            form.setPredClasses(predClasses);
//            form.setClassification(classification);
//        } catch (Exception e) {
//            System.out.println("Error loading model!\n" + e.toString());
//        }
//    }

    private static void loadRoi(ZipInputStream in, String className, String ID, ClassManager classes) throws IOException{
        classes.addClass(className);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        byte[] bytes = out.toByteArray();
        RoiDecoder rd = new RoiDecoder(bytes, ID);
        Roi roi = rd.getRoi();
        if (roi!=null) classes.addRoi(className, roi);
    }
}

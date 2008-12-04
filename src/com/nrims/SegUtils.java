package com.nrims;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author sreckow
 */
public class SegUtils extends javax.swing.SwingWorker<Boolean,Void>{    
    // constants used for writing the .zip file
    private static final String TRAINPREFIX = "train";
    private static final String PREDPREFIX  = "pred";
    private static final String CLASSFILE   = "class";
    private static final String DESCFILE    = "desc";
    private static final String SEPARATOR   = "/";      // there's no portable way of writing .zip files, so we
                                                        // consistently use forward slash as file separator    
    private static final int PREPSEG_TYPE = 0;
    private static final int CALCROI_TYPE = 1;

    private boolean success = false;

    // input members
    private UI ui;    
    private int type;    
    private ArrayList<Integer> massImages;
    private ArrayList<int[]> ratioImages;
    private int colorImage;
    private int features;
    private int height;
    private int width;        
    private int minSize;
    private int maxSize;
    
    // output members
    private ClassManager trainClasses;
    private ArrayList<ArrayList<ArrayList<Double>>> trainData;
    private ClassManager predClasses;
    private ArrayList<ArrayList<ArrayList<Double>>> predData;    
    private int[] classColors;    
    private String[] classNames;
    private byte[] classification;
        
    private SegUtils(){
        // initialize the output members
        trainData = new ArrayList<ArrayList<ArrayList<Double>>>();
        trainClasses = new ClassManager();
        predData = new ArrayList<ArrayList<ArrayList<Double>>>();        
        predClasses = new ClassManager();
        classColors = new int[0];
        classNames = new String[0];
        classification = new byte[0];
    }
    
    public static SegUtils initPrepSeg(UI ui, ClassManager trainClasses, ArrayList<Integer> massImages, ArrayList<int[]> ratioImages, int colorImage, int features){
        SegUtils s = new SegUtils();
        s.type = PREPSEG_TYPE;
        s.ui = ui;        
        s.trainClasses = trainClasses;
        s.massImages = massImages;
        s.ratioImages = ratioImages;
        s.colorImage = colorImage;
        s.features = features;
        return s;
    }
    
    public static SegUtils initCalcRoi(int height, int width, String[] classNames, byte[] classification, int minSize, int maxSize){
        SegUtils s = new SegUtils();
        s.type = CALCROI_TYPE;
        s.height = height;
        s.width = width;
        s.classNames = classNames;
        s.classification = classification;
        s.minSize = minSize;
        s.maxSize = maxSize;
        return s;
    }
    
    private void prepareSegmentation(){        
        MimsPlus[] images = getImages();
        ArrayList<ArrayList<int[]>> dataPoints;        
        
        // extract features for training
        dataPoints = getDataPoints();
        double[] classMeans = new double[dataPoints.size()];
        trainData = extractFeatures(dataPoints, images, classMeans);
        // get colors
        classColors = createClassColors(classMeans);
        
        // extract features for prediction
        dataPoints = new ArrayList<ArrayList<int[]>>(1);
        // create "dummy ROI which corresponds to the whole image
        ij.gui.Roi testRoi = new ij.gui.Roi(0,0,ui.getMimsImage().getWidth(), ui.getMimsImage().getHeight());
        dataPoints.add(getDataPoints(testRoi));
        predData = extractFeatures(dataPoints, images, classMeans);               
    }
    
    private MimsPlus[] getImages(){
        MimsPlus[] images = new MimsPlus[massImages.size()+ratioImages.size()];
        int cnt = 0;
        // we assume here that the mass images and their pixel data are available
        for(int massIndex : massImages){
            images[cnt++] = ui.getMassImage(massIndex);
        }
        // we assume here that the ratio images and their pixel data are available
        for(int[] ratio: ratioImages){
            int num = ratio[0];
            int den = ratio[1];
            int ratioIndex = ui.getRatioImageIndex(num,den);
            if(ratioIndex > -1) images[cnt++] = ui.getRatioImage(ratioIndex);
            else{
                // this can not be done in the SwingWorker thread as it involves GUI operation !!

//                // create ratio image
//                HSIProps props = new HSIProps();
//                props.setNumMass(num);
//                props.setDenMass(den);
//                ui.computeRatio(props);
//                ratioIndex = ui.getRatioImageIndex(num,den);
//                if(ratioIndex > -1) images[cnt++] = ui.getRatioImage(ratioIndex);
//                else{

                    System.out.println("Error: ratio image not available!");
//                }
            }
        }        
        return images;
    }
    
    // get data points (pixel coordinates) for the specified ROIs
    private static ArrayList<int[]> getDataPoints(Roi roi){
        ArrayList<int[]> dataPoints = new ArrayList<int[]>();
        int roiY = roi.getBounds().y;
        int roiX = roi.getBounds().x;
        int roiHeight = roi.getBounds().height;
        int roiWidth = roi.getBounds().width;
        // iterate through ROI bounding box
        for (int y = roiY; y < (roiY + roiHeight); y++) {
            for (int x = roiX; x < (roiX + roiWidth); x++) {
                if(roi.contains(x, y)){
                    dataPoints.add(new int[]{x,y});
                }
            }                    
        }
        return dataPoints;
    }
    
    // get data points for all ROIs of all classes
    private ArrayList<ArrayList<int[]>> getDataPoints(){
        ArrayList<ArrayList<int[]>> dataPoints = new ArrayList<ArrayList<int[]>>(trainClasses.getClasses().length);
        for(String className : trainClasses.getClasses()){
            ArrayList<int[]> classDataPoints = new ArrayList<int[]>();
            for(SegRoi segRoi : trainClasses.getRois(className)){
                classDataPoints.addAll(getDataPoints(segRoi.getRoi()));
            }            
            dataPoints.add(classDataPoints);
        }
        return dataPoints;
    }       
            
    private ArrayList<ArrayList<ArrayList<Double>>> extractFeatures (ArrayList<ArrayList<int[]>> dataPoints, MimsPlus[] images, double[] classMeans){
        ArrayList<ArrayList<ArrayList<Double>>> dataTable = new ArrayList<ArrayList<ArrayList<Double>>>(dataPoints.size());        
        
        for(int classIndex = 0; classIndex<dataPoints.size(); classIndex++){
            ArrayList<ArrayList<Double>> classData = new ArrayList<ArrayList<Double>>();
            for(int dataPointIndex=0; dataPointIndex<dataPoints.get(classIndex).size();dataPointIndex++){    
                classData.add(new ArrayList<Double>());
            }
            dataTable.add(classData);
        }
        
        int neighborhood = 1;
        
        // iteratate through source images
        for (int i = 0; i < images.length; i++) {
            ij.process.ImageProcessor ip = images[i].getProcessor(); 
            height = ip.getHeight();
            width = ip.getWidth();
            double[][] imageData = new double[height][width];
            if(ip instanceof ij.process.ShortProcessor){
                for(int y=0; y<height; y++){
                    for(int x=0; x<width; x++){
                        imageData[x][y] = ip.get(x,y);
                    }
                }
            }else if(ip instanceof ij.process.FloatProcessor){
                for(int y=0; y<height; y++){
                    for(int x=0; x<width; x++){
                        imageData[x][y] = Float.intBitsToFloat(ip.get(x,y));
                    }
                }
            }else{
                System.out.println("Error: unknown pixel type");
                return new ArrayList<ArrayList<ArrayList<Double>>>(0);
            }
            
            // iterate through all data points and collect features from the current image data
            for(int classIndex = 0; classIndex<dataPoints.size(); classIndex++){
                ArrayList<ArrayList<Double>> classData = dataTable.get(classIndex);
                for(int dataPointIndex=0; dataPointIndex<dataPoints.get(classIndex).size();dataPointIndex++){ 
                    ArrayList<Double> featureVector = classData.get(dataPointIndex);
                    int x = dataPoints.get(classIndex).get(dataPointIndex)[0];
                    int y = dataPoints.get(classIndex).get(dataPointIndex)[1];
                    
                    // intensity (or ratio) of this data point (pixel)
                    double value = imageData[x][y];                    
                    
                    // iterate through neighborhood (<neighborhood> neighbors + current data point)
                    double sum = 0;
                    double sum2 = 0;
                    int n = 0;
                    double grad_x = 0;
                    double grad_y = 0;
                    double grad_n = 0;
                    for(int ny = y==0?0:y-neighborhood; ny<height && ny<=y+neighborhood; ny++){
                        for(int nx = x==0?0:x-neighborhood; nx<width && nx<=x+neighborhood; nx++){
                            sum  += imageData[nx][ny];
                            sum2 += imageData[nx][ny]*imageData[nx][ny];
                            n++;

                            // compute discrete gradient approximation for this neighbor
                            // approx. gradient of function f at point i calculated as
                            // grad{f(i)} = (f(i+1)-f(i-1))/2
                            if(ny>0 && ny<height-1 && nx>0 && nx<width-1){                                    
                                double y_1 = imageData[nx][ny-1];
                                double y_2 = imageData[nx][ny+1];
                                grad_y += (y_2 - y_1)/2.0;
                                double x_1 = imageData[nx-1][ny];
                                double x_2 = imageData[nx+1][ny];
                                grad_x += (x_2 - x_1)/2.0;
                                grad_n++;
                            }                                
                        }
                    }
                    // subtract current data point from sum (does not add to neighborhood)
                    sum -= value;
                    sum2 -= value*value;
                    n--;
                    // calculate local gradient approximation (average gradient vector)
                    grad_y = grad_y/grad_n;
                    grad_x = grad_x/grad_n;

                    // add requested features
                    featureVector.add(value);   // intensity/ratio
                    featureVector.add(sum/n);   // mean value of neigbors
                    featureVector.add((n*sum2 - sum*sum) / (n*(n-1))); // variance of neighbors
                    featureVector.add(Math.sqrt(grad_y*grad_y + grad_x*grad_x)); // norm of gradient   
                    
                    if(colorImage == i) classMeans[classIndex]+=value;
                }
            }
        }
        for(int classIndex = 0; classIndex<dataPoints.size(); classIndex++){
            classMeans[classIndex] /= dataPoints.get(classIndex).size();
        }
        return dataTable;
    }
    
    private static int[] createClassColors(double[] classMeans){
        int nClasses = classMeans.length;
        int[] classColors = new int[nClasses];     
        ArrayList<Double> sorted = new ArrayList<Double>();
        for(double d : classMeans) sorted.add(d);
        java.util.Collections.sort(sorted);       
        
        int delta = 65535/(nClasses-1);
        float[][] tables = HSIProcessor.getHsiTables();
        for( int i=0 ; i<nClasses ; i++ )
        {
            int colorIndex = sorted.indexOf(classMeans[i]);
            int iratio = colorIndex*(int)delta;
            int r = (int)(tables[0][iratio] * 255.0 )  ;
            int g = (int)(tables[1][iratio] * 255.0 )  ;
            int b = (int)(tables[2][iratio] * 255.0 )  ;
            classColors[i] = ((r&0xff)<<16) + ((g&0xff)<<8) + (b&0xff) ;
        }
        return classColors;
    }
    
    private void calcROIs(){
        // setup values to report progress
        int progress = 0;
        int step = 100/(classNames.length*2);
        
        // create an image from the classification result in order to find ROIs
        ByteProcessor ip = new ByteProcessor(width,height);
        ip.setPixels(classification);
        ImagePlus classMap = new ImagePlus("",ip);
        predClasses = new ClassManager();        
        
        // find all ROIs using particle analyzer
        for(byte classID=0; classID<classNames.length; classID++){                        
            classMap.getProcessor().setThreshold(classID, classID, ImageProcessor.BLACK_AND_WHITE_LUT);
            int options = ParticleAnalyzer.ADD_TO_MANAGER;
            ParticleAnalyzer pa = new ParticleAnalyzer(options, 0, Analyzer.getResultsTable(), minSize, maxSize);
            pa.analyze(classMap);
            RoiManager rm = (RoiManager)WindowManager.getFrame("ROI Manager");
            predClasses.addClass(classNames[classID]);
            if(rm== null) continue;                        
            for(Roi roi : rm.getRoisAsArray()) predClasses.addRoi(classNames[classID], roi);
            rm.close();
            progress += step;
            setProgress(progress);
        }
            
        // handle overlapping ROIs        
        long start = System.currentTimeMillis();
        int comparisons = 0;
        int intersections = 0;            
        int subtractions = 0;
        int overlaps = 0;
            
        for(int classI=0; classI<predClasses.getClasses().length; classI++){ // iterate through all classes
            SegRoi[] rois = predClasses.getRois(classNames[classI]);         // get all ROIs from one class
            for(int classJ=classI+1; classJ<predClasses.getClasses().length; classJ++){ 
                SegRoi[] newRois = predClasses.getRois(classNames[classJ]);  // get all ROIs from another class    
                
//                System.out.println(newRois.length);
//                System.out.println(rois.length);
//                int n=0;
                for(SegRoi newSegRoi : newRois){                                        
//                    System.out.println(n++);
                    for(SegRoi segRoi : rois){ // check for overlap with all other ROIs
//                        if(segRoi.getClassName().equals(newSegRoi.getClassName())) continue; // 1st filter: different classes?
                        comparisons++;
                                                     
                        // the ROI objects that might need updating
                        Roi roi    = segRoi.getRoi();
                        Roi newRoi = newSegRoi.getRoi();                        
                        if(newRoi.getBounds().intersects(roi.getBounds())){ // 2nd filter: do bounding rectangles intersect?
                            intersections++;
                            
                            // the corresponding ShapeRoi objects used for the set operations
                            // ShapeRoi objects are cloned before each set operation as the caller gets altered
                            ShapeRoi roiShape    = (ShapeRoi)segRoi.getShapeRoi().clone(); // A
                            ShapeRoi newRoiShape;                                          // B
                            
                            // calculate A intersects B = C
                            ShapeRoi intersection = roiShape.and(newSegRoi.getShapeRoi());
                            //Roi intersection = tryConvert(roiShape);
                            if(tryConvert(intersection)==null) continue;    // 3rd filter: is intersection non-empty?
                                       
                            subtractions++;
                            
                            roiShape    = (ShapeRoi)segRoi.getShapeRoi().clone();
                            newRoiShape = (ShapeRoi)newSegRoi.getShapeRoi().clone();
                                                      
                            // subtract the intersection from both ROIs to eliminate overlaps
                            roiShape.not(intersection);                 // calculate A - C (= A - B) = D
                            newRoiShape.not(intersection);              // calculate B - C (= B - A) = E
                            // the updated ROIs lacking any overlaps
                            Roi roiUpdated = tryConvert(roiShape);                            
                            Roi newRoiUpdated = tryConvert(newRoiShape);
                            
                            // 4th filter: did both subtractions result in a non-empty set
                            if(roiUpdated==null){
                                try{
                                 newSegRoi.setRoi(newRoiUpdated, newRoiShape);  // B = E (A entirely contained in B)
                                 continue;
                                }catch(Exception e){
                                    e.printStackTrace();
                                }
                            }
                            if(newRoiUpdated==null){
                                 segRoi.setRoi(roiUpdated, roiShape);        // A = D (B entirely contained in A)
                                 continue;
                            }
                                                        
                            overlaps++;
                            
//                            System.out.print(segRoi.getClassName() + "_" + segRoi.getID());
//                            System.out.print("\t");
//                            System.out.print(newSegRoi.getClassName() + "_" + newSegRoi.getID());
//                            System.out.print("\t");
//                            System.out.print(intersection.getLength());
//                            System.out.println();
                            
//                            String ID = String.valueOf(overlaps);
//                            showRois.add(new SegRoi(roi, segRoi.getClassName() + "orig", ID)); // original A
//                            showRois.add(new SegRoi(newRoi, newSegRoi.getClassName() + "orig", ID)); // original B
//                            showRois.add(new SegRoi(intersection, "inter", ID)); // A - B
//                            showRois.add(new SegRoi((ShapeRoi)roiShape.clone(), segRoi.getClassName() + "-", ID)); // A - B
//                            showRois.add(new SegRoi((ShapeRoi)newRoiShape.clone(), newSegRoi.getClassName() + "-", ID)); // B - A
                            
                            // as this is a partial overlap, we need to add back overlapping points to their
                            // corresponding area
                            ShapeRoi addToRoi;
                            boolean roiAdded = false;
                            boolean newRoiAdded = false;
                            Rectangle mbr = intersection.getBounds();
                            // walk through the bounding rectangle of the overlap and check class membership of each point
                            for(int y=mbr.y; y<mbr.y+mbr.height; y++){
                                for(int x=mbr.x; x<mbr.x+mbr.width; x++){
                                    if(!intersection.contains(x, y)) continue;
                                        
                                    int tmpClassID = classMap.getProcessor().getPixel(x, y);
                                    addToRoi = null;
                                    if(tmpClassID == classI){
                                        addToRoi = roiShape;
                                        roiAdded = true;
                                    }else if(tmpClassID == classJ){
                                        addToRoi = newRoiShape;
                                        newRoiAdded = true;
                                    }
                                                                        
                                    if(addToRoi !=null){
                                        // we have a match, so add the point to its corresponding ShapeRoi
                                        GeneralPath newSubpath = new GeneralPath();
                                        newSubpath.moveTo(x, y);
                                        newSubpath.lineTo(x+1, y);
                                        newSubpath.lineTo(x+1, y+1);
                                        newSubpath.lineTo(x, y+1);
                                        newSubpath.closePath();
                                        // set A = D OR C_A      /      B = E OR C_B 
                                        addToRoi.or(new ShapeRoi(newSubpath));  
                                    }
                                }
                            }
                            // try to convert each ShapeRoi that got points added back to a non-composite ROI
//                            if(roiAdded) roiUpdated = tryConvert(roiShape);
                                //showRois.add(segRoi);
//                            if(newRoiAdded) newRoiUpdated = tryConvert(newRoiShape);
                                //showRois.add(newSegRoi);
                            
                            // update the original ROI objects
//                            segRoi.setRoi(roiUpdated, roiShape);
//                            newSegRoi.setRoi(newRoiUpdated, newRoiShape);
                            segRoi.setRoi(roiShape, roiShape);
                            newSegRoi.setRoi(newRoiShape, newRoiShape);
                        }
                    }
                }
            }
            progress += step;
            setProgress(progress);
        }
        long stop = System.currentTimeMillis();
//        System.out.println("Time:" + (stop - start)/1000);
//        System.out.println("comparisons:" + comparisons);
//        System.out.println("intersections:" + intersections);
//        System.out.println("subtractions:" + subtractions);
//        System.out.println("overlaps:" + overlaps);        
    }
    
    // helper method trying to convert a ShapeRoi object into a single Roi
    // returns null, if the shape corresponds to no ROI
    private static Roi tryConvert(ShapeRoi shapeRoi){
        if(shapeRoi.getLength() > 0 ) return shapeRoi;
        else return null;
//        Roi[] roisTmp = shapeRoi.getRois();
//        if(roisTmp.length > 0){
//            int type = roisTmp[0].getType();
//            if (roisTmp.length==1 && (type==Roi.POLYGON||type==Roi.FREEROI)){
//                return roisTmp[0];                        
//            }else{
//                return shapeRoi;
//            }     
//        }else{
//            return null;
//        }
    }
    
    public static void saveSegmentation(String fileName, ClassManager train, ClassManager pred, String[] classNames, byte[] classification, int[] classColors){
        try{
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fileName));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(out));
            writeClassManager(train, zos, TRAINPREFIX);
            writeClassManager(pred, zos, PREDPREFIX);
            zos.putNextEntry(new ZipEntry(CLASSFILE));
            zos.write(classification);
            zos.putNextEntry(new ZipEntry(DESCFILE));            
            for(int i=0; i<classNames.length; i++){                
                String line = classNames[i] + "\t" + classColors[i] + "\n";
                bwr.write(line);
                bwr.flush();
            }
            out.close();
        }catch(java.io.IOException e){
            System.out.println("Error saving segmentation result!\n" + e.toString());
            return;
        }
    }
    
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
    
    public static SegUtils loadSegmentation(String fileName){
        SegUtils s = new SegUtils();
        ArrayList<String> tmpClassNames = new ArrayList<String>();
        ArrayList<Integer> tmpClassColors = new ArrayList<Integer>();
        try {                 
            ZipInputStream in = new ZipInputStream(new FileInputStream(fileName));            
            ZipEntry entry = in.getNextEntry(); 
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while (entry!=null) {
                String name[] = entry.getName().split(SEPARATOR);                
                if      (name[0].equals(TRAINPREFIX)) loadRoi(in, name[1], name[2], s.trainClasses);
                else if (name[0].equals(PREDPREFIX))  loadRoi(in, name[1], name[2], s.predClasses);
                else if (name[0].equals(CLASSFILE)){
                    byte[] buf = new byte[1024]; 
                    int len; 
                    while ((len = in.read(buf)) > 0){                        
                        byte[] tmp = new byte[s.classification.length + len];
                        System.arraycopy(s.classification, 0, tmp ,0, s.classification.length);
                        System.arraycopy(buf, 0, tmp, s.classification.length, len);
                        s.classification = tmp;
                    }
                }else if(name[0].equals(DESCFILE)){
                    String line;
                    while((line = br.readLine())!=null){
                        String[] data = line.split("\t");
                        tmpClassNames.add(data[0]);
                        tmpClassColors.add(Integer.parseInt(data[1]));
                    }                    
                }
                entry = in.getNextEntry(); 
            }
            in.close(); 
            s.classNames = tmpClassNames.toArray(s.classNames);
            s.classColors = new int[tmpClassColors.size()];
            for(int i=0; i<tmpClassColors.size(); i++){
                s.classColors[i] = tmpClassColors.get(i);
            }
        } catch (IOException e) {return null;} 
        return s;
    }
    
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

    public int[] getClassColors() {
        return classColors;
    }

    public ClassManager getPredClasses() {
        return predClasses;
    }

    public ArrayList<ArrayList<ArrayList<Double>>> getPredData() {
        return predData;
    }

    public ClassManager getTrainClasses() {
        return trainClasses;
    }
        
    public ArrayList<ArrayList<ArrayList<Double>>> getTrainData() {
        return trainData;
    }
    
    public String[] getClassNames() {
        return classNames;
    }     
    
    public byte[] getClassification() {
        return classification;
    }

    public boolean getSuccess(){
        return success;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        try{
            switch(type){
                case PREPSEG_TYPE:
                    prepareSegmentation();
                    return true;
                case CALCROI_TYPE:
                    calcROIs();
                    return true;
                default:
                    return false;
            }
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void done() {
        try{
            success = get();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
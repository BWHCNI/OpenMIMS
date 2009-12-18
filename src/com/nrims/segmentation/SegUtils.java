package com.nrims.segmentation;

import com.nrims.*;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

/**
 *
 * @author sreckow
 */
public class SegUtils extends javax.swing.SwingWorker<Boolean,Void>{     
    private static final int PREPSEG_TYPE = 0;
    private static final int CALCROI_TYPE = 1;

    private boolean success = false;

    private int type;    
    private MimsPlus[] images;
    private int colorImage;
    private int[] features;
    private int height;
    private int width;        
    private int minSize;
    private int maxSize;    
    private ClassManager classes;
    private ArrayList<ArrayList<ArrayList<Double>>> data;
    private int[] classColors;    
    private String[] classNames;
    private byte[] classification;
        
    private SegUtils(){
        data = new ArrayList<ArrayList<ArrayList<Double>>>();
        classes = new ClassManager();
        classColors = new int[0];
        classNames = new String[0];
        classification = new byte[0];
    }
    
    public static SegUtils initPrepSeg(MimsPlus[] images, int colorImage, int[] features , ClassManager classes){
        SegUtils s = new SegUtils();
        s.type = PREPSEG_TYPE;
        s.classes = classes;
        s.images = images;
        s.colorImage = colorImage;
        s.features = features;
        return s;
    }

    public static SegUtils initPrepSeg(MimsPlus[] images, int colorImage, int[] features){
        return SegUtils.initPrepSeg(images, colorImage, features, null);
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

    // get coordinates of specified region(s), extract the features and the class color
    // sets <data> and <classColors>




    public void prepareSegmentation(){
        // extract features for training
        ArrayList<ArrayList<int[]>> dataPoints;
        // if a classManager is available it is used for data point extraction (typically for training)
        // otherwise, a dummy ROI of maximum width and height is created to extract all data points (typically for prediction)
        if(classes != null) dataPoints = getDataPoints(classes);
        else{
            ij.gui.Roi dummyRoi = new ij.gui.Roi(0, 0, images[0].getWidth(), images[0].getHeight());
            dataPoints = new ArrayList<ArrayList<int[]>>();
            int depth = getDataZSize(images);
            dataPoints.add(getDataPointsZ(dummyRoi, depth));
        }        
        double[] classMeans = new double[dataPoints.size()];
        //this is a stupid check, needs to be better
        if(dataPoints.get(0).get(0).length==2) {
            data = extractFeatures(dataPoints, images, classMeans);
        } else {
            data = extractFeaturesZ(dataPoints, images, classMeans);
        }
        // get colors
        classColors = createClassColors(classMeans); 
    }   

    /* **************************
        private void prepareSegmentation(){
        // extract features for training
        ArrayList<ArrayList<int[]>> dataPoints;
        // if a classManager is available it is used for data point extraction (typically for training)
        // otherwise, a dummy ROI of maximum width and height is created to extract all data points (typically for prediction)
        if(classes != null) {
            dataPoints = getDataPoints(classes);
            double[] classMeans = new double[dataPoints.size()];
            data = extractFeatures(dataPoints, images, classMeans);
            // get colors
            classColors = createClassColors(classMeans);
        }
        else{
            ij.gui.Roi dummyRoi = new ij.gui.Roi(0, 0, images[0].getWidth(), images[0].getHeight());
            int depth = getDataZSize(images);
            dataPoints = new ArrayList<ArrayList<int[]>>();
            dataPoints.add(getDataPointsZ(dummyRoi, depth));
            double[] classMeans = new double[dataPoints.size()];
            data = extractFeaturesZ(dataPoints, images, classMeans);
            // get colors
            classColors = createClassColors(classMeans);
        }

    }   */


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

        private static ArrayList<int[]> getDataPointsZ(Roi roi, int depth){
            ArrayList<int[]> dataPoints = new ArrayList<int[]>();

            System.out.println("getDataPointsZ called at depth: "+depth);

            int roiY = roi.getBounds().y;
            int roiX = roi.getBounds().x;
            int roiHeight = roi.getBounds().height;
            int roiWidth = roi.getBounds().width;
            // iterate through ROI bounding box
            for (int z = 0; z < depth; z++) {

                System.out.println("Depth: "+z);

                for (int y = roiY; y < (roiY + roiHeight); y++) {
                    for (int x = roiX; x < (roiX + roiWidth); x++) {
                        if (roi.contains(x, y)) {
                            dataPoints.add(new int[]{x, y, z});
                        }
                    }
                }
            }
            System.out.println("getDataPointsZ dataPoint.size = " + dataPoints.size());
            return dataPoints;
    }
    
    // get data points for all ROIs of all classes
    private ArrayList<ArrayList<int[]>> getDataPoints(ClassManager classes){
        ArrayList<ArrayList<int[]>> dataPoints = new ArrayList<ArrayList<int[]>>(classes.getClasses().length);
        for(String className : classes.getClasses()){
            ArrayList<int[]> classDataPoints = new ArrayList<int[]>();
            for(SegRoi segRoi : classes.getRois(className)){
                classDataPoints.addAll(getDataPoints(segRoi.getRoi()));
            }            
            dataPoints.add(classDataPoints);
        }
        return dataPoints;
    }       

    private int getDataZSize(MimsPlus[] imgs) {
        //Is assuming that all mass images have same z size.
        //Which should allways be true.

        int size = 1;
        boolean hasmass = false;

        for(int i = 0; i< imgs.length; i++) {
            if(imgs[i].getMimsType() == imgs[i].MASS_IMAGE) {
                hasmass = true;
                size = imgs[i].getNSlices();
                break;
            }
        }

        if(!hasmass && imgs[0].getMimsType()==imgs[0].RATIO_IMAGE) {
            size = imgs[0].getNumeratorImage().getNSlices();
        }

        return size;
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

        // use neighborhood of specified size only if neigborhood or gradient feature are selected
        // features[0] : neigborhood; features[1] = gradient; features[2] = neigborhood size
        int neighborhood = (features[0]==1 || features[1]==1) ? features[2] : 0;

        // iteratate through source images
        for (int i = 0; i < images.length; i++) {
            ij.process.ImageProcessor ip = images[i].getProcessor(); 
            int height = ip.getHeight();
            int width = ip.getWidth();
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
                for(int dataPointIndex=0; dataPointIndex<dataPoints.get(classIndex).size(); dataPointIndex++){
                    ArrayList<Double> featureVector = classData.get(dataPointIndex);
                    int x = dataPoints.get(classIndex).get(dataPointIndex)[0];
                    int y = dataPoints.get(classIndex).get(dataPointIndex)[1];
                    
                    // intensity (or ratio) of this data point (pixel)
                    double value = imageData[x][y];                    
                    
                    // iterate through neighborhood (#neighbors + current data point)
                    double sum = 0;
                    double sum2 = 0;
                    int n = 0;
                    double grad_x = 0;
                    double grad_y = 0;
                    double grad_n = 0;
                    for(    int ny = y-neighborhood<0?0:y-neighborhood; ny<height && ny<=y+neighborhood; ny++){
                        for(int nx = x-neighborhood<0?0:x-neighborhood; nx<width  && nx<=x+neighborhood; nx++){
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
                    if(features[0]==1)  featureVector.add(sum/n);   // mean value of neigbors
                    if(features[0]==1)  featureVector.add((n*sum2 - sum*sum) / (n*(n-1))); // variance of neighbors
                    if(features[1]==1)  featureVector.add(Math.sqrt(grad_y*grad_y + grad_x*grad_x)); // norm of gradient
                    
                    if(colorImage == i) classMeans[classIndex]+=value;
                }
            }
        }
        for(int classIndex = 0; classIndex<dataPoints.size(); classIndex++){
            classMeans[classIndex] /= dataPoints.get(classIndex).size();
        }
        return dataTable;
    }



        //-NOT- computing features using a z radius
        //Simply extracting features serially
        //Should change
        public ArrayList<ArrayList<ArrayList<Double>>> extractFeaturesZ (ArrayList<ArrayList<int[]>> dataPoints, MimsPlus[] images, double[] classMeans){
        ArrayList<ArrayList<ArrayList<Double>>> dataTable = new ArrayList<ArrayList<ArrayList<Double>>>(dataPoints.size());

        for(int classIndex = 0; classIndex<dataPoints.size(); classIndex++){
            ArrayList<ArrayList<Double>> classData = new ArrayList<ArrayList<Double>>();
            for( int dataPointIndex=0; dataPointIndex<dataPoints.get(classIndex).size();dataPointIndex++){
                classData.add(new ArrayList<Double>());
            }
            dataTable.add(classData);
        }

        // use neighborhood of specified size only if neigborhood or gradient feature are selected
        // features[0] : neigborhood; features[1] = gradient; features[2] = neigborhood size
        int neighborhood = (features[0]==1 || features[1]==1) ? features[2] : 0;

        // iteratate through source images
        for (int i = 0; i < images.length; i++) {

            //generate pixel arrays correctly
            ij.process.ImageProcessor ip = images[i].getProcessor();
            int height = ip.getHeight();
            int width = ip.getWidth();
            int depth = this.getDataZSize(images);

            double[][][] imageData = new double[height][width][depth];

            //mass image case
            if(ip instanceof ij.process.ShortProcessor){
                for (int z = 0; z < depth; z++) {
                    if(depth>1)
                        ip = images[i].getStack().getProcessor(z+1);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            imageData[x][y][z] = ip.get(x, y);
                        }
                    }
                }
            }
            //ratio image case
            //ratio images aren't stacks...
            else if(ip instanceof ij.process.FloatProcessor){
                for (int z = 0; z < depth; z++) {
                    if(depth>1) {
                        //surprisingly it works.  No concurency badness?
                        images[i].getNumeratorImage().setSlice(z+1);
                        ip = images[i].getProcessor();
                    }
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            imageData[x][y][z] = Float.intBitsToFloat(ip.get(x, y));
                        }
                    }
                }
            }else{
                System.out.println("Error: unknown pixel type");
                return new ArrayList<ArrayList<ArrayList<Double>>>(0);
            }


            // iterate through all data points and collect features from the current image data
            for(int classIndex = 0; classIndex<dataPoints.size(); classIndex++){
                ArrayList<ArrayList<Double>> classData = dataTable.get(classIndex);

                for(int dataPointIndex=0; dataPointIndex<dataPoints.get(classIndex).size(); dataPointIndex++){
                    ArrayList<Double> featureVector = classData.get(dataPointIndex);
                    int x = dataPoints.get(classIndex).get(dataPointIndex)[0];
                    int y = dataPoints.get(classIndex).get(dataPointIndex)[1];
                    int z = dataPoints.get(classIndex).get(dataPointIndex)[2];

                    // intensity (or ratio) of this data point (pixel)
                    double value = imageData[x][y][z];

                    // iterate through neighborhood (#neighbors + current data point)
                    double sum = 0;
                    double sum2 = 0;
                    int n = 0;
                    double grad_x = 0;
                    double grad_y = 0;
                    double grad_n = 0;
                    for(    int ny = y-neighborhood<0?0:y-neighborhood; ny<height && ny<=y+neighborhood; ny++){
                        for(int nx = x-neighborhood<0?0:x-neighborhood; nx<width  && nx<=x+neighborhood; nx++){
                            sum  += imageData[nx][ny][z];
                            sum2 += imageData[nx][ny][z]*imageData[nx][ny][z];
                            n++;

                            // compute discrete gradient approximation for this neighbor
                            // approx. gradient of function f at point i calculated as
                            // grad{f(i)} = (f(i+1)-f(i-1))/2
                            if(ny>0 && ny<height-1 && nx>0 && nx<width-1){
                                double y_1 = imageData[nx][ny-1][z];
                                double y_2 = imageData[nx][ny+1][z];
                                grad_y += (y_2 - y_1)/2.0;
                                double x_1 = imageData[nx-1][ny][z];
                                double x_2 = imageData[nx+1][ny][z];
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
                    if(features[0]==1)  featureVector.add(sum/n);   // mean value of neigbors
                    if(features[0]==1)  featureVector.add((n*sum2 - sum*sum) / (n*(n-1))); // variance of neighbors
                    if(features[1]==1)  featureVector.add(Math.sqrt(grad_y*grad_y + grad_x*grad_x)); // norm of gradient

                    //-------
                    if (dataPointIndex % 10000 == 0) {
                        System.out.println("dataPointIndex = " + dataPointIndex);
                        System.out.println("featureVector.size() = " + featureVector.size());
                    }
                    //-------
                    if(colorImage == i) classMeans[classIndex]+=value;
                }
            }
        }
        for(int classIndex = 0; classIndex<dataPoints.size(); classIndex++){
            classMeans[classIndex] /= dataPoints.get(classIndex).size();
        }
        return dataTable;
    }



    //-NOT- computing features using a z radius
    //Simply extracting features serially
    //Should change
    public void exportFeaturesZ(MimsPlus[] images, java.io.BufferedWriter buffwriter) {

        ArrayList<ArrayList<int[]>> dataPoints = new ArrayList<ArrayList<int[]>>();
        ij.gui.Roi dummyRoi = new ij.gui.Roi(0, 0, images[0].getWidth(), images[0].getHeight());
        dataPoints.add(getDataPointsZ(dummyRoi, getDataZSize(images)));

        // use neighborhood of specified size only if neigborhood or gradient feature are selected
        // features[0] : neigborhood; features[1] = gradient; features[2] = neigborhood size
        int neighborhood = (features[0] == 1 || features[1] == 1) ? features[2] : 0;
        int currentplane = -1;

        //generate pixel arrays correctly
        ij.process.ImageProcessor ip = images[0].getProcessor();
        int height = ip.getHeight();
        int width = ip.getWidth();
        int depth = this.getDataZSize(images);
        int zdepth = 1;
        double[][][][] imageData = new double[images.length][height][width][zdepth];
        
        ArrayList<ArrayList<Double>> featurebuffer = new ArrayList<ArrayList<Double>>();

        //debugging vars
        int grabpix = 0;
        int write = 0;

        for (int dataPointIndex = 0; dataPointIndex < dataPoints.get(0).size(); dataPointIndex++) {

            ArrayList<Double> featureVector = new ArrayList<Double>();
            featureVector.add(((double)dataPointIndex));
            // iteratate through source images
            //moved compared to extractFeatures
            //was depedent on refencing an ArrayList in memory
            //to build feature vectors of correct length
            //NEEDS cleanup....
                        
            for (int i = 0; i < images.length; i++) {
                               
                int dataz =  dataPoints.get(0).get(dataPointIndex)[2];


                //grab pixel data only if z value hase changed
                if (dataz != currentplane) {
                    grabpix++;
                    System.out.println("grabbed pix "+grabpix+" times");
                    //mass image case
                    if (images[i].getMimsType()==MimsPlus.MASS_IMAGE) {

                        if (depth > 1) {
                            ip = images[i].getStack().getProcessor(dataz + 1);
                        }
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                imageData[i][x][y][0] = ip.get(x, y);
                            }
                        }

                    } //ratio image case
                    //ratio images aren't stacks...
                    else if (images[i].getMimsType()==MimsPlus.RATIO_IMAGE) {

                        if (depth > 1) {
                            //surprisingly it works.  No concurency badness?
                            images[i].getNumeratorImage().setSlice(dataz + 1);
                            ip = images[i].getProcessor();
                        }
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                imageData[i][x][y][0] = Float.intBitsToFloat(ip.get(x, y));
                            }
                        }

                    } else {
                        System.out.println("Error: unknown pixel type");
                        return;
                    }
                    currentplane = dataz;
                }

                int x = dataPoints.get(0).get(dataPointIndex)[0];
                int y = dataPoints.get(0).get(dataPointIndex)[1];
                int z = 0;

                // intensity (or ratio) of this data point (pixel)
                double value = imageData[i][x][y][0];

                // iterate through neighborhood (#neighbors + current data point)
                double sum = 0;
                double sum2 = 0;
                int n = 0;
                double grad_x = 0;
                double grad_y = 0;
                double grad_n = 0;
                for (int ny = y - neighborhood < 0 ? 0 : y - neighborhood; ny < height && ny <= y + neighborhood; ny++) {
                    for (int nx = x - neighborhood < 0 ? 0 : x - neighborhood; nx < width && nx <= x + neighborhood; nx++) {
                        sum += imageData[i][nx][ny][z];
                        sum2 += imageData[i][nx][ny][z] * imageData[i][nx][ny][z];
                        n++;

                        // compute discrete gradient approximation for this neighbor
                        // approx. gradient of function f at point i calculated as
                        // grad{f(i)} = (f(i+1)-f(i-1))/2
                        if (ny > 0 && ny < height - 1 && nx > 0 && nx < width - 1) {
                            double y_1 = imageData[i][nx][ny - 1][z];
                            double y_2 = imageData[i][nx][ny + 1][z];
                            grad_y += (y_2 - y_1) / 2.0;
                            double x_1 = imageData[i][nx - 1][ny][z];
                            double x_2 = imageData[i][nx + 1][ny][z];
                            grad_x += (x_2 - x_1) / 2.0;
                            grad_n++;
                        }
                    }
                }
                // subtract current data point from sum (does not add to neighborhood)
                sum -= value;
                sum2 -= value * value;
                n--;
                // calculate local gradient approximation (average gradient vector)
                grad_y = grad_y / grad_n;
                grad_x = grad_x / grad_n;

                // add requested features
                featureVector.add(value);   // intensity/ratio
                if (features[0] == 1) {
                    featureVector.add(sum / n);   // mean value of neigbors
                }
                if (features[0] == 1) {
                    featureVector.add((n * sum2 - sum * sum) / (n * (n - 1))); // variance of neighbors
                }
                if (features[1] == 1) {
                    featureVector.add(Math.sqrt(grad_y * grad_y + grad_x * grad_x)); // norm of gradient
                }
                //-------
                if (dataPointIndex % 10000 == 0) {
                    System.out.println("dataPointIndex = " + dataPointIndex);
                    System.out.println("featureVector.size() = " + featureVector.size());
                }

            }

            featurebuffer.add(featureVector);

            if (featurebuffer.size() >= 100000) {
                write++;
                System.out.println("Writing feature buffer: "+write);
                for (int i = 0; i < featurebuffer.size(); i++) {
                    //write feature vector
                    java.util.Iterator featureIT = featurebuffer.get(i).iterator();
                    long ind = ((Double)featureIT.next()).longValue();

                    String res = ind + "";
                    int f = 0;
                    while (featureIT.hasNext()) {
                        res += " " + (f + 1) + ":" + (Double) featureIT.next();
                        f++;
                    }
                    try {
                        buffwriter.append(res);
                        buffwriter.newLine();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                featurebuffer.clear();
            }
        }

        //final write/clear of buffer
        if (!featurebuffer.isEmpty()) {
                write++;
                System.out.println("Final writing feature buffer: "+write);
                for (int i = 0; i < featurebuffer.size(); i++) {
                    //write feature vector
                    java.util.Iterator featureIT = featurebuffer.get(i).iterator();
                    long ind = ((Double)featureIT.next()).longValue();

                    String res = ind + "";
                    int f = 0;
                    while (featureIT.hasNext()) {
                        res += " " + (f + 1) + ":" + (Double) featureIT.next();
                        f++;
                    }
                    try {
                        buffwriter.append(res);
                        buffwriter.newLine();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                featurebuffer.clear();
            }

    }


    private static int[] createClassColors(double[] classMeans){
        int nClasses = classMeans.length;
        int[] classColors = new int[nClasses];     
        ArrayList<Double> sorted = new ArrayList<Double>();
        for(double d : classMeans) sorted.add(d);
        java.util.Collections.sort(sorted);       

        int delta;
        if(nClasses>1) delta = 65535/(nClasses-1);
        else           delta = 0;
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
        int step = 100/(classNames.length*2+1);
        
        // create an image from the classification result in order to find ROIs
        ByteProcessor ip = new ByteProcessor(width,height);

        if((width*height)!=classification.length) return;

        ip.setPixels(classification);
        ImagePlus classMap = new ImagePlus("",ip);
        classes = new ClassManager();
        
        // find all ROIs using particle analyzer
        for(byte classID=0; classID<classNames.length; classID++){                        
            classMap.getProcessor().setThreshold(classID, classID, ImageProcessor.BLACK_AND_WHITE_LUT);
            int options = ParticleAnalyzer.ADD_TO_MANAGER;
            ParticleAnalyzer pa = new ParticleAnalyzer(options, 0, Analyzer.getResultsTable(), minSize, maxSize);
            pa.analyze(classMap);
            RoiManager rm = (RoiManager)WindowManager.getFrame("ROI Manager");
            classes.addClass(classNames[classID]);
            if(rm== null) continue;                        
            for(Roi roi : rm.getRoisAsArray()) classes.addRoi(classNames[classID], roi);
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
            
        for(int classI=0; classI<classes.getClasses().length; classI++){ // iterate through all classes
            SegRoi[] rois = classes.getRois(classNames[classI]);         // get all ROIs from one class
            for(int classJ=classI+1; classJ<classes.getClasses().length; classJ++){
                SegRoi[] newRois = classes.getRois(classNames[classJ]);  // get all ROIs from another class
                
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



    public int[] getClassColors() {
        return classColors;
    }

    public ClassManager getClasses() {
        return classes;
    }

    public ArrayList<ArrayList<ArrayList<Double>>> getData() {
        return data;
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
/*
 * Opener.java
 *
 * Created on May 1, 2006, 12:09 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */


package com.nrims;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;   
import java.text.DecimalFormat ;
import java.awt.Color ;
import java.awt.Frame ;
import java.applet.Applet ;

/**
 *
 * @author Douglas Benson
 */
public class Opener {
    
    /** Creates a new instance of Opener */
    public Opener() {
    }

    private String           fullpath;
    private File             file ;
    private RandomAccessFile in;
    private int              verbose = 0 ;
    private int              width, height, nMasses, nImages ;
    private HeaderImage      ihdr ;
    private DefAnalysis      dhdr ;
    private TabMass[]        tabMass ;
    private MaskImage        maskIm ;
    private int              currentMass = 0 ;
    private int              currentIndex = 0 ;
    private com.nrims.UI     ui = null ;
    
    private MassImage []     massImages ;
    public static final int  MIN_FILE_SIZE = 150828 ;
    public static final int  IHDR_SIZE = 84 ;


    final String getChar(int n) throws IOException {
        String rstr = new String() ;
        byte [] buf = new byte[n] ;
        int nr = in.read(buf);
        for(int i=0;i<n && buf[i] != 0; i++) rstr += (char)buf[i] ;
        return rstr ;
    }
        
    protected void resetPixels() {
        for(int i = 0 ; i < nMasses ; i++ ) 
            massImages[i].setPixels(null);
    }
/**
 * Reads the pixel data 
 */
    public void readPixels(int index) throws Exception {

        int i, j;
        int pixelsPerImage = width * height ;
        int bytesPerMass = pixelsPerImage * 2 ;
        double sum, sum2 ;
        double var, np = (double)pixelsPerImage ;

// Assumes file is positioned at the beginning of the data 

        short [] spixels = new short[pixelsPerImage];
        massImages[index].setPixels(spixels);
        // long [] histogram = new long[65536];
        // massImages[index].setHistogram(histogram);

        int minGL = 65535 ;
        int maxGL = 0 ;

        // i : short counter
        // n : image counter
		
        long offset = dhdr.header_size  + currentIndex * nMasses * bytesPerMass ;
        if(index > 0) offset += index * bytesPerMass ;
        in.seek(offset) ;

        sum = 0.0F ;
        sum2 = 0.0F ;
        int gl =-1 ;
        i = index+1 ;
        if(ui != null) {
            if(nImages > 1) 
                ui.updateStatus("Reading image " + (currentIndex+1) + " mass " + i + " of " + nMasses) ;
            else
                ui.updateStatus("Reading mass " + i + " of " + nMasses );
        }

/*       
        for(i=0; i < pixelsPerImage ; i++ ) {
            gl = in.readShort() & 0xffff ;
            // if(gl < 0) gl = 65536 + gl ;
            sum += (double)gl ;
            sum2 += (double)(gl * gl) ;
            spixels[i] = (short)gl ;
            histogram[gl] += 1 ;
            if(gl < minGL) minGL = gl ;
            if(gl > maxGL) maxGL = gl ;
        }
 */
        byte [] bArray = new byte[bytesPerMass];
        in.read(bArray);

        for(i=0,j=0;i<pixelsPerImage;i++,j+=2) {
            int b1 = bArray[j] & 0xff ;
            int b2 = bArray[j+1] & 0xff ;
            gl = (b2+(b1<<8));
            sum += (double)gl ;
            sum2 += (double)(gl * gl) ;
            spixels[i] = (short)gl ;
            // histogram[gl] += 1 ;
            if(gl < minGL) minGL = gl ;
            if(gl > maxGL) maxGL = gl ;           
        }
        
        massImages[index].setMeanGL(sum/np) ;
        var = (sum2 - (sum*sum/np) ) / (np -1.0) ;
        if(sum > 0) massImages[index].setStdDev(Math.sqrt(var)) ;
        else massImages[index].setStdDev(0.0);

    }

/**
 * Reads the DefAnalysis structure from the SIMS file header.
 */
    private void readDefAnalysis( DefAnalysis dhdr ) throws Exception {
        if(dhdr == null) throw new NullPointerException() ;
        dhdr.release = in.readInt() ;
        dhdr.analysis_type = in.readInt() ;
        dhdr.header_size = in.readInt() ;
        int noused = in.readInt() ;
        dhdr.data_included = in.readInt() ;
        dhdr.sple_pos_x = in.readInt() ;
        dhdr.sple_pos_y = in.readInt() ;
        dhdr.analysis_name = getChar(32);
        dhdr.username = getChar(16);
        dhdr.sample_name = getChar(16);
        dhdr.date = getChar(16);
        dhdr.hour = getChar(16);

        if(this.verbose > 2){
            System.out.println("readDefAnalysis OK");
            System.out.println("dhdr.release:" + dhdr.release );
            System.out.println("dhdr.analysis_type:" + dhdr.analysis_type );
            System.out.println("dhdr.header_size:" + dhdr.header_size );
            System.out.println("dhdr.data_included:" + dhdr.data_included );
            System.out.println("dhdr.sple_pos_x:" + dhdr.sple_pos_x );
            System.out.println("dhdr.sple_pos_y:" + dhdr.sple_pos_y );
            System.out.println("dhdr.analysis_name:" + dhdr.analysis_name );
            System.out.println("dhdr.username:" + dhdr.username );
            System.out.println("dhdr.sample_name:" + dhdr.sample_name );
            System.out.println("dhdr.date:" + dhdr.date );
            System.out.println("dhdr.hour:" + dhdr.hour );
        }
    }

/**
 * Reads the AutoCal structure from the SIMS file header.
 */
    private void readAutoCal(AutoCal ac) throws IOException {
        ac.mass = getChar(64);
        ac.begin = in.readInt() ;
        ac.period = in.readInt() ;
    }

/**
 * Reads a Table element structure from the SIMS file header.
 */
    private void readTabelts(Tabelts te) throws IOException {
        te.num_elt = in.readInt() ;
        te.num_isotop = in.readInt() ;
        te.quantity = in.readInt() ;
    }

/**
 * Reads a PolyAtomic element/structure from the SIMS file header.
 */
    private void readPolyAtomic(PolyAtomic pa) throws IOException {
        pa.flag_numeric = in.readInt() ;
        pa.numeric_value = in.readInt() ;
        pa.nb_elts = in.readInt() ;
        pa.nb_charges = in.readInt() ;      
        pa.charge = in.readInt();
        String unused = getChar(64);
        pa.tabelts = new Tabelts[5];
        for(int i = 0 ; i < 5 ; i++) {
            pa.tabelts[i] = new Tabelts();
            readTabelts(pa.tabelts[i]) ;
        }
    }

/**
 * Reads a SigRef element/structure from the SIMS file header.
 */
    private void readSigRef(SigRef sr) throws IOException {
        sr.polyatomic = new PolyAtomic();
        readPolyAtomic(sr.polyatomic) ;
        sr.detector = in.readInt() ;
        sr.offset = in.readInt() ;
        sr.quantity = in.readInt() ;
    }

/**
 * Reads and returns a Mask_im structure from the SIMS file header.
 */
    private void readMaskIm(MaskImage mask) throws Exception {
        if(mask == null) throw new NullPointerException();
        mask.filename = getChar(16) ;
        mask.analysis_duration = in.readInt() ;
        mask.cycle_number = in.readInt() ;
        mask.scantype = in.readInt() ;
        mask.magnification = in.readShort() ;
        mask.sizetype = in.readShort() ;
        mask.size_detector = in.readShort() ;
        short unused = in.readShort() ;
        mask.beam_blanking = in.readInt() ;
        mask.sputtering = in.readInt() ;
        mask.sputtering_duration = in.readInt() ;
        mask.auto_calib_in_analysis = in.readInt() ;

        if(this.verbose > 2){
            System.out.println("mask.filename:"+mask.filename);
            System.out.println("mask.analysis_duration:"+
                    mask.analysis_duration);
            System.out.println("mask.cycle_number:"+mask.cycle_number);
            System.out.println("mask.scantype:"+mask.scantype);
            System.out.println("mask.magnification:"+mask.magnification);
            System.out.println("mask.sizetype:"+mask.sizetype);
            System.out.println("mask.size_detector:"+mask.size_detector);
            System.out.println("mask.beam_blanking:"+mask.beam_blanking);
            System.out.println("mask.sputtering:"+mask.sputtering);
            System.out.println("mask.sputtering_duration:"
                    +mask.sputtering_duration);
            System.out.println("mask.auto_calib:"+mask.auto_calib_in_analysis);
        }

        mask.autocal = new AutoCal();
        readAutoCal(mask.autocal) ;
        int sig_ref_int = in.readInt() ;
        mask.sig_ref = new SigRef();
        readSigRef(mask.sig_ref) ;
        nMasses = in.readInt() ;
        if(this.verbose > 2) System.out.println("mask.nMasses:"+nMasses);

//	Read the Tab_mass *tab_mass[10]..
        int tab_mass_ptr ;
        for(int i = 0 ; i < 10 ; i++){
            tab_mass_ptr = in.readInt() ;
            if(this.verbose > 2) System.out.println("mask.tmp:"+tab_mass_ptr);
        }
    }

/**
 * Reads and returns a TabMass structure from the SIMS file header.
 */
    private void readTabMass(TabMass tab) throws Exception {
        if(tab == null) throw new NullPointerException() ;
        int unused = in.readInt() ;
        int unuseds2 = in.readInt() ;
        tab.mass_amu = in.readDouble() ;
        tab.matrix_or_trace = in.readInt() ;
        tab.detector = in.readInt() ;
        tab.waiting_time = in.readDouble() ;
        tab.counting_time = in.readDouble() ;
        tab.offset = in.readInt() ;
        tab.mag_field = in.readInt() ;

        if(this.verbose > 2){
            System.out.println("TabMass.mass_amu:"+tab.mass_amu);
            System.out.println("TabMass.matrix_or_trace:"+tab.matrix_or_trace);
            System.out.println("TabMass.detector:"+tab.detector);
            System.out.println("TabMass.waiting_time:"+tab.waiting_time);
            System.out.println("TabMass.counting_time:"+tab.counting_time);
            System.out.println("TabMass.offset:"+tab.offset);
            System.out.println("TabMass.mag_field:"+tab.mag_field);
        }
        tab.polyatomic = new PolyAtomic();
        readPolyAtomic(tab.polyatomic) ;
    }

/**
 * Formats a double precision to a string
 */
    private String DecimalToStr( double v, int fraction )
    {
        DecimalFormat df = new DecimalFormat("0.00");
        if(fraction != 2) {
            String format ;
            format = "0" ;
            if(fraction > 0) format += "." ;
            for(int i = 0 ; i < fraction ; i++) format += "0";
            df.applyPattern(format) ;
        }
        return df.format(v) ;
    }

/**
 * Reads and returns the HeaderImage structure from a SIMS image file
 */
    private void readHeaderImage( HeaderImage ihdr ) throws Exception {
        if(ihdr == null) throw new NullPointerException();
        ihdr.size_self = in.readInt() ;
        ihdr.type = in.readShort() ;
        ihdr.w = in.readShort() ;
        this.width = ihdr.w ;
        ihdr.h = in.readShort() ;
        this.height = ihdr.h ;
        ihdr.d = in.readShort() ;
        ihdr.n = in.readShort() ;
	nImages = ihdr.z = in.readShort() ;
        if(nImages < 1) nImages = 1 ;      
        ihdr.raster = in.readInt() ;
        ihdr.nickname = getChar(64);

        if(this.verbose > 1){
            System.out.println("readHeaderImage OK");
            System.out.println("ihdr.d:" + ihdr.d );
            System.out.println("ihdr.n:" + ihdr.n );
            System.out.println("ihdr.w:" + ihdr.w );
            System.out.println("ihdr.type:" + ihdr.type );
            System.out.println("ihdr.z:" + ihdr.z );
        }
    }

/**
 * Reads and returns the header data from a SIMS image file
 * Creates the DefAnalysis and HeaderImage subclass's for this class.
 */
    private void readHeader() throws Exception {
  
        this.dhdr = new DefAnalysis() ;
        readDefAnalysis(dhdr) ;
        this.maskIm = new MaskImage() ;
        readMaskIm(this.maskIm) ;
        if(this.nMasses <= 0) throw new Exception("Error reading MIMS file.") ;
        
        massImages = new MassImage[nMasses];
        for(int i = 0 ; i < nMasses ; i++ ) {
            massImages[i] = new MassImage();
        }
        this.tabMass = new TabMass[this.nMasses] ;

        for(int i = 0 ; i < this.nMasses ; i++ ){
            TabMass tm = new TabMass();
            massImages[i].setTabMass(tm);
            readTabMass(tm);
            massImages[i].setName(DecimalToStr(tm.mass_amu,2));
        }

        long offset = dhdr.header_size - IHDR_SIZE;
        in.seek(offset) ;

        this.ihdr = new HeaderImage() ;
        readHeaderImage(ihdr) ;
 
    }

/**
 * Checks the given imagename and returns true of this is a SIMS image.
 */
    private boolean FileOk( String imagename )
    {
        if(imagename == null) return false;
        if ( (file = new File(imagename) ) == null ) return false ;
        if(!file.exists()) return false ;
        if(file.length() < Opener.MIN_FILE_SIZE ) return false ;
        if(!file.isFile()) return false ;
        if(!file.canRead()) return false ;
        if(file.isDirectory()) return false ;
        return true ;
    }

/**
 * Opens and reads a SIMS image from an image file.
 */
    public Opener (com.nrims.UI ui, String imagename) throws Exception
    {
        this.ui = ui ;
        this.verbose = ui.getDebug() ? 1 : 0 ;
        if(imagename == null){ throw new NullPointerException(); }
        if(!FileOk(imagename)){ file = null ; throw new Exception("File Error"); }
        try { in = new RandomAccessFile(file, "r") ; }
        catch( IOException e) { throw new Exception("Access Error"); }
        readHeader();
        currentIndex = 0 ;
        currentMass = 0 ;
        readPixels(0);
        this.fullpath = imagename ;
    }

/**
 * returns the name of the SIMS image file
 */

    public String getName() { 
        String name = file.getName() ;
        int extIndex = name.lastIndexOf(".im");
        return name.substring(0,extIndex);
    }
    public String getDirectory() { return file.getParent(); }
    public String getFullPath() { return fullpath ; }
        
/**
 * returns a string with the mean,standard deviation 
 * min and max graylevels of a SIMS image.
 */
    public String getStats(int index) throws Exception {
            if(nMasses <= 0) throw new Exception("No images") ;
            if(index < 0 || index >= nMasses) throw new Exception("No images") ;
            if(massImages[index].getPixels() == null)  readPixels(index);
            double n = (double)(this.width * this.height) ;
            String statString = "Mean " +DecimalToStr(massImages[index].getMeanGL(),2)
                +" +/-  "+DecimalToStr(massImages[index].getStdDev(),2)
                +" Min: "+ massImages[index].getMinGL()
                +" Max: "+ massImages[index].getMaxGL() ;
            return statString ;
    }

    public String getCounts(int index) throws Exception {
        
        if(nMasses <= 0) throw new Exception("No images") ;
        if(index < 0 || index >= nMasses) throw new Exception("No images") ;
        if(massImages[index].getPixels() == null) readPixels(index);
        double n = (double)(this.width * this.height) ;

        String statString = DecimalToStr(massImages[index].getMeanGL(),2)
                +" +/-  "+DecimalToStr(massImages[index].getStdDev(),2)
                +" [ "+ massImages[index].getMinGL()
                +" - "+ massImages[index].getMaxGL() 
                                    + " ] " ;

        return statString ;
    }
/**
 * returns the Statistics string for the currently selected index.
 */
    public String getStats() throws Exception { return getStats(this.currentMass); }

/**
 * returns the number of "masses" in this SIMS image file.
 */
    public int nImages() { return this.nImages ; }
    public int nMasses() { return this.nMasses ; }

/**
 * returns the width of the images in pixels.
 */
    public int getWidth() { return this.width ; }

/**
 * returns the height of the images in pixels.
 */
    public int getHeight() { return this.height ; }

/**
 * returns the number of bits per pixel. Always 16.
 */
    public int getBits() { return 16 ; }

/**
 * returns the mass in AMU for image at the given index.
 */
    public double getMass(int index)
    {
        if(index >= 0 && index <=  nMasses){
                return this.tabMass[index].mass_amu ;
        }
        return -1.0 ;
    }

/**
 * returns a String of the mass in AMU for image at the given index.
 */
    public String getMassName(int index)
    {
        if(nMasses == 0) return "" ;
        if(index >= 0 && index <=  nMasses){
                return massImages[index].getName();
        }
        return "" ;
    }

    public String [] getMassNames() {
        String [] names = new String[nMasses] ;
        for(int i = 0 ; i < nMasses ; i++ ) {
            names[i] = massImages[i].getName();
        }
        return names ;
    }
    
    public void setDebug(int nLevel)
    {
        this.verbose = nLevel ;
    }

/**
 * returns the graylevel of the image at index and location x,y
 */

    public short[] getPixels(int index) throws Exception {
        if(index < 0 || index >= this.nMasses) throw new Exception("Index Error");
        if(massImages[index].getPixels() == null)  readPixels(index);
        return massImages[index].getPixels();
    }

/**
 * returns the graylevel of the image at index and location x,y
 */

    public int getPixel(int index, int x, int y) throws Exception
    {
        if(index < 0 || index >= this.nMasses)  throw new Exception("Index Error");
        if( massImages[index].getPixels() == null ) readPixels(index);
        short [] spixels = massImages[index].getPixels();
        int offset = x + y * width ;
        if( offset < 0 || offset > spixels.length -1 ) throw new Exception("Out of Bounds");
        return (int) spixels[offset];
    }

/**
 * returns the graylevel of the image at current index and location x,y
 */
    public int getPixel(int x, int y) throws Exception {
        return this.getPixel(this.currentMass, x, y);
    }

/**
 * returns the minimum graylevel in the image at the given index
 */
    public int getMin(int index) throws Exception {
        if(index < 0 || index >= this.nMasses) throw new Exception("Index Error"); 
        if( massImages[index].getPixels() == null ) readPixels(index);
        return massImages[index].getMinGL() ;
    }

/**
 * returns the maximum graylevel in the image at the given index
 */
    public int getMax(int index) throws Exception {
        if(index < 0 || index >= this.nMasses)  throw new Exception("Index Error"); ;
        if( massImages[index].getPixels() == null ) readPixels(index);
        return massImages[index].getMaxGL() ;
    }

/**
 * returns the current index in a stack or multiple time point series
 * indices are between at zero and nImages() - 1
 */
    public int getStackIndex() { return this.currentIndex  ; }

/**
 * returns the current Mass.
 * masses are between at zero and nMasses - 1
 */
    public int getMassIndex() { return this.currentMass  ; }

/**
 * sets the current mass index.
 * indices are 0 for the first mass up to nMasses()-1.
 */
    public void setMassIndex(int nMass) throws Exception {
        if(nMass < 0 || nMass >= this.nMasses) throw new Exception();
        if(this.currentMass == nMass) return ;
        this.currentMass = nMass ;
        if(ui != null) ui.updateStatus("Current Mass:"+ (nMass+1)) ;    
    }

/**
 * sets the current image index.
 * indices are 0 for the first image up to nImages()-1.
 * each image is composed of up to nMasses() sub images
 */
    public void setStackIndex(int index) throws Exception {
        if(index < 0 || index >= this.nImages) throw new Exception() ;
        if( this.currentIndex == index ) return ;
        resetPixels();
        this.currentIndex = index ;
    }

/**
 * returns the sple_pos_x,sple_pos_y entries from the SIMS image header
 */
    public String getPosition(){
        if(this.dhdr == null) return null;
        String pos = this.dhdr.sple_pos_x + "," + this.dhdr.sple_pos_y ;
        return pos ;
    }

/**
 * returns the date entry from the SIMS image header
 */
    public String getSampleDate(){
        if(this.dhdr == null) return null;
        if(this.dhdr.sample_name == null) return new String(" ");
        return this.dhdr.date ;
    }

/**
 * returns the hour entry from the SIMS image header
 */
    public String getSampleHour(){
        if(this.dhdr == null) return null;
        if(this.dhdr.sample_name == null) return new String(" ");
        return this.dhdr.hour ;
    }

/**
 * returns the username entry from the SIMS image header
 */
    public String getUserName()
    {
        if(this.dhdr == null) return null;
        if(this.dhdr.sample_name == null) return new String(" ");
        return this.dhdr.username ;
    }

/**
 * returns the samplename entry from the SIMS image header
 */
    public String getSampleName()
    {
        if(this.dhdr == null) return null;
        if(this.dhdr.sample_name == null) return new String(" ");
        return this.dhdr.sample_name ;
    }

/**
 * returns the raster entry from the SIMS image header
 */
    public int getRaster()
    {
        if(this.ihdr == null) return 0;
        return this.ihdr.raster ;
    }

/**
 * returns the dwelltime per pixel in milliseconds
 */
    public String getDwellTime()
    {
        if(this.maskIm == null || this.ihdr == null) return new String(" ");
        double duration = (double)this.maskIm.analysis_duration ;
        double size = (double)(this.ihdr.w * this.ihdr.h) ;
        if(size == 0) return new String(" ") ;
        duration = 1000.0 * duration / size ;
        String dtime = DecimalToStr(duration,3);
        return dtime ;
    }

/**
 * returns the nickname from the SIMS header
 */
    public String getNickName()
    {
        if(this.ihdr == null) return new String(" ");
        return this.ihdr.nickname ;
    }


/**
 * returns the analysis_duration from the SIMS header
 */
    public String getDuration()
    {
        if(this.maskIm == null) return null;
        double duration = (double)this.maskIm.analysis_duration ;
        String dtime = DecimalToStr(duration,3);
        return dtime ;
    }

/**
 * returns the pixel Width from the SIMS header
 */
    public float getPixelWidth()
    {
        float pw = 1.0f ;
        if(this.ihdr != null) 
                pw = (float) this.ihdr.raster / (float)this.ihdr.w ;
        return pw ;
    }

/**
 * returns the pixel Height from the SIMS header
 */
    public float getPixelHeight()
    {
        float ph = 1.0f ;
        if(this.ihdr != null) 
                ph = (float) this.ihdr.raster / (float)this.ihdr.h ;
        return ph ;
    }

    public com.nrims.UI getUI() { return ui ; }
    
    public String getInfo() {
        String info = "" ;
        try {
            if(massImages[currentMass].getPixels() == null) readPixels(currentMass);
            info += "MIMSFile=" + fullpath + "\n";
            info += "Mass" + massImages[currentMass].getName() + "\n";
            info += "MinCounts=" + massImages[currentMass].getMinGL() + "\n";
            info += "MaxCounts=" + massImages[currentMass].getMaxGL() + "\n" ;
            info += "Mean=" +massImages[currentMass].getMeanGL() + "\n";
            info += "StdDev=" + massImages[currentMass].getStdDev() + "\n";
            int index = currentMass+1;
            info += "MassIndex=" + index + "\n";
            info += "TotalMasses=" + nMasses() + "\n";
            if(nImages() > 1) {
                info += "Section=" + (currentIndex+1) + "\n";
                info += "TotalSections=" + nImages() + "\n" ;
            }
            if(tabMass[currentMass] != null) info += tabMass[currentMass].getInfo();
            if(ihdr != null) info += ihdr.getInfo() ;
            if(dhdr != null) info += dhdr.getInfo() ;
            if(maskIm != null) info += maskIm.getInfo();
            return info ;
        }
        catch(Exception x) { return info ; }
    }
    
/**
 * returns the hour and date entries from the SIMS image header
 */

/**
 * prints to the standard output the name, mass and statistics for
 * a series of SIMS images given as arguments
 */
    public static void main(String[] args)
    {
        if( args.length < 1 ){
                System.out.println("Usage: Opener <image>");       
                System.exit(0);
        }else{
          for(int argc=0; argc<args.length; argc++)
          {
              try
              {
                    Opener image = new Opener(null, args[argc]) ;
                    for(int n = 0; n < image.nMasses(); n++){
                            System.out.println("Image("+n+" of "+image.nMasses()
                                    + ") Mass: " + image.getMassName(n) 
                                    + " [" + image.getMass(n) + "]");
                            System.out.println("  Stats:" + image.getStats(n));
                    }
              }
              catch(Exception x)
              {
                      System.out.println("Can't open " + args[argc] ) ;
              }
          }
        }
    } 

    private class MassImage {
        
        private short[]        spixels ;
        // private long []        histogram  ;
        private TabMass        tabMass ;
        private String         massName ;
        private int            minGL ;
        private int            maxGL ;
        private double         meanGL ;
        private double         sdGL ;
        public void setPixels(short [] pixels) { spixels = pixels ; }
        public short[] getPixels() { return spixels ; }
        // public void setHistogram (long [] hist) { histogram = hist ; }
        // public long[]  getHistogram() { return histogram ; }
        public void setTabMass( TabMass tm ) { tabMass = tm ; }
        public TabMass getTabMass() { return tabMass ; }
        public void setName(String name) { massName = name ; }
        public String getName() { return massName ; }
        public void setMinGL(int gl) { minGL = gl ; }
        public int getMinGL() { return minGL ; }
        public void setMaxGL(int gl) { maxGL = gl ; }
        public int getMaxGL() { return maxGL ; }
        public void setMeanGL(double mean) { meanGL = mean ; }
        public double getMeanGL() { return meanGL ; }
        public void setStdDev(double sd) { sdGL = sd ; }
        public double getStdDev() { return sdGL ; }
        
    }
/**
 * defines a structure for saving the HeaderImage data
 */
    private class HeaderImage
    {
        int size_self ;
        short type ;
        short w ;
        short h ;
        short d;
        short n ;
        short z ;
        int raster ;
        String nickname ;
        public String getInfo() {
            String info = "";
            if(type != 1) info += "Header.Type="+type+"\n";
            if(w != 0) info += "Header.Width="+w+"\n";
            if(h != 0) info += "Header.Height="+h+"\n";
            if(d != 0) info += "Header.BytesPerPixel="+d+"\n";
            if(n != 0) info += "Header.masses="+n+"\n";
            if(z != 0) info += "Header.Sections="+z+"\n";
            if(raster != 0) info += "Header.Raster="+raster+"\n";
            if(nickname.length()> 0) info += "Header.Nickname="+nickname+"\n";
            return info ;
        }
    }
    private class AutoCal {
        String mass ;
        int begin ;
        int period ;
        public String getInfo() {
            String info = "" ;
            if(mass.length() > 0) info += "AutoCal.mass="+mass+"\n";
            if(begin != 0) info += "AutoCal.begin="+begin+"\n";
            if(period != 0) info += "AutoCal.period="+period+"\n";
            return info ;
        }
    }
    private class SigRef {
        PolyAtomic polyatomic ;
        int detector ;
        int offset ;
        int quantity ;
        public String getInfo() {
            String info = "" ;
            if(detector != 0) info += "SigRef.detector="+detector+"\n";
            if(offset != 0) info += "SigRef.offset="+offset+"\n";
            if(quantity != 0) info += "SigRef.quantity="+quantity+"\n";
            if(polyatomic != null) {
                String pInfo = polyatomic.getInfo();
                if(pInfo.length() > 0) {
                    info += "SigReg.polyatomic={\n";
                    info += polyatomic.getInfo();
                    info += "}\n";
                }
            }
            return info ;
        }
    }

    private class Tabelts {
        int num_elt ;
        int num_isotop ;
        int quantity ;
        
        public String getInfo() {
            String info = "" ;
            if(num_elt != 0) info += "Tabelts.num_elt="+num_elt+"\n";
            if(num_isotop != 0) info += "Tabelts.num_isotop="+num_isotop+"\n";
            if(quantity != 0) info += "Tabelts.quantity="+quantity+"\n";
            return info ;
        }
    }
    
    private class PolyAtomic {
        int flag_numeric ;
        int numeric_value ;
        int nb_elts ;
        int nb_charges ;
        int charge ;
        Tabelts tabelts[] ;
        public String getInfo() {
            String info = "" ;
            if(flag_numeric != 0) info = "PolyAtomic.flag_numeric="+flag_numeric+"\n";
            if(numeric_value != 0) info += "PolyAtomic.numeric_value="+numeric_value+"\n";
            if(nb_elts != 0) info += "PolyAtomic.nb_elts="+nb_elts+"\n";
            if(nb_charges != 0) info += "PolyAtomic.nb_charges="+nb_charges+"\n";
            if(charge != 0) info += "PolyAtomic.charge="+charge+"\n";
            if(tabelts != null ) {
                for(int i = 0 ; i < tabelts.length; i++ ) {
                    if(tabelts[i] != null) {
                        String tInfo = tabelts[i].getInfo();
                        if(tInfo.length() > 0) {
                            info += "PolyAtomic.tabelts["+i+"].={\n";
                            info += tInfo ;
                            info += "}\n";
                        }
                    }
                }
            }
            return info ;
        }
    }
 
/**
 * defines a structure for saving the DefAnalysis data
 */
    private class DefAnalysis
    {
        int release ;
        int analysis_type;
        int header_size ;
        int data_included ;
        int sple_pos_x ;
        int sple_pos_y ;
        String analysis_name ;
        String username ;
        String sample_name ;
        String date ;
        String hour ;
        public String getInfo() {
            String info = "" ;
            if(release != 0) info = "Analysis.Release="+release+"\n";
            if(analysis_type != 0) info += "Analysis.Type="+analysis_type+"\n";
            if(header_size != 0) info += "Analysis.Headersize="+header_size+"\n";
            if(sple_pos_x != 0) info += "Analysis.PosistionX="+sple_pos_x+"\n";
            if(sple_pos_y != 0) info += "Analysis.PositionY="+sple_pos_y+"\n";
            // if(data_included != 0) info += "Analysis.data_included="+data_included+"\n";
            if(analysis_name.length() > 0) info += "Analysis.Name="+analysis_name+"\n";
            if(username.length() > 0) info += "Analysis.Username="+username+"\n";
            if(sample_name.length() > 0) info += "Analysis.Samplename="+sample_name+"\n";
            if(date.length() > 0) info += "Analysis.Date="+date+"\n";
            if(hour.length() > 0) info += "Analysis.Hour="+hour+"\n";
            return info ;
        }
    }

/**
 * defines a structure for saving the Mask_im data
 */
    private class MaskImage
    {
        String filename;
        int analysis_duration;
        int cycle_number ;
        int scantype ;
        short magnification ;
        short sizetype ;
        short size_detector ;
        int beam_blanking ;
        int sputtering ;
        int sputtering_duration ;
        int auto_calib_in_analysis;
        SigRef sig_ref ;
        int nb_mass ;
        AutoCal autocal ;
        
        public String getInfo() {
            String info = "";
            if(filename.length() > 0) info += "MaskImage.Filename="+filename+"\n";
            if(analysis_duration != 0) info += "MaskImage.Duration="+analysis_duration+"\n";
            if(cycle_number != 0) info += "MaskImage.Cycle="+cycle_number+"\n";
            if(scantype != 0) info += "MaskImage.Scantype="+scantype+"\n";
            if(magnification != 0) info += "MaskImage.Magnification="+magnification+"\n";
            if(sizetype != 0) info += "MaskImage.Sizetype="+sizetype+"\n";
            if(size_detector != 0) info += "MaskImage.SizeDetector="+size_detector+"\n";
            if(beam_blanking != 0) info += "MaskImage.BeamBlanking="+beam_blanking+"\n";
            if(sputtering != 0) info += "MaskImage.Sputtering="+sputtering+"\n";
            if(sputtering_duration != 0) info += "MaskImage.SputteringDuration="+sputtering_duration+"\n";
            if(auto_calib_in_analysis != 0) info += "MaskImage.AutoCalibration="+auto_calib_in_analysis+"\n";
            if(nb_mass != 0) info += "MaskImage.NbMass="+nb_mass+"\n";
            if(sig_ref != null) {
                String sInfo = sig_ref.getInfo();
                if(sInfo.length() > 0) {
                    info += "MaskImage.SigRef={\n";
                    info += sInfo;
                    info += "}\n";
                }
            }
            if(autocal != null) {
                String aInfo = autocal.getInfo();
                if(aInfo.length() > 0) {
                    info += "MasImage.AutoCal={\n";
                    info += aInfo;
                    info += "}\n";
                }
            }
            return info ;
        }
    }

/**
 * defines a structure for saving the TabMass data
 */
    private class TabMass
    {
        double mass_amu ;
        int matrix_or_trace ;
        int detector ;
        double waiting_time ;
        double counting_time ;
        int offset ;
        int mag_field ;
        PolyAtomic polyatomic ;
        
        public String getInfo() {
            String info = "" ;
            if(mass_amu != 0.0) info += "TabMass.AMU="+mass_amu+"\n";
            if(matrix_or_trace != 0) info += "TabMass.matrix_or_trace="+matrix_or_trace+"\n";
            if(detector != 0) info += "TabMass.Detector="+detector+"\n";
            if(waiting_time != 0) info += "TabMass.WaitingTime="+waiting_time+"\n";
            if(counting_time != 0) info += "TabMass.CountingTime="+counting_time+"\n";
            if(offset != 0) info += "TabMass.Offset="+offset+"\n";
            if(mag_field != 0) info += "TabMass.Magfield="+mag_field+"\n";
            if(polyatomic != null) {
                String pInfo = polyatomic.getInfo();
                if(pInfo.length() > 0) {
                    info += "TabMass.PolyAtomic={\n";
                    info += pInfo;
                    info += "}\n";
                }
            }
            return info ;
        }
    }
}

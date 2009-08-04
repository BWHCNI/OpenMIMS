package com.nrims;
import ij.IJ ;
import ij.gui.* ;
import java.awt.Rectangle;
import java.awt.event.WindowEvent ;
import java.awt.event.WindowListener ;
import java.awt.event.MouseListener ;
import java.awt.event.MouseMotionListener ;
import java.awt.event.MouseEvent ;
import javax.swing.event.EventListenerList;
/**
 * extends ImagePlus with methods to synchronize display of multiple stacks
 * and drawing ROIs in each windows
 */
public class MimsPlus extends ij.ImagePlus implements WindowListener, MouseListener, MouseMotionListener {
    
    static final int MASS_IMAGE = 0 ;
    static final int RATIO_IMAGE = 1 ;
    static final int HSI_IMAGE  =  2 ;
    static final int SEG_IMAGE = 3 ;
    static final int SUM_IMAGE = 4 ;

    //TODO: these are going to get deleted
    MimsPlus ratioMims;
    MimsPlus numeratorSum;
    MimsPlus denominatorSum;
    //replaced by these
    MimsPlus internalRatio;
    MimsPlus internalNumerator;
    MimsPlus internalDenominator;


    public double massNumber;
    public double denomMassNumber;
    public double numerMassNumber;
    public double scaleFactor;
    public SumProps sumProps = null;
    private String parentImageName = null;
    private boolean allowClose =true;
    private boolean bIgnoreClose = false ;
    boolean bMoving = false;
    boolean autoAdjustContrast = false;
    static boolean bStateChanging = false ;
    private int nMass = 0 ;
    private int nType = 0 ;
    private int nRatioNum = 0 ;
    private int nRatioDen = 0 ;
    private int x1, x2, y1, y2, w1, w2, h1, h2;
    private boolean bIsStack = false ;
    private HSIProcessor hsiProcessor = null ;
    private com.nrims.data.Opener srcImage ;
    private com.nrims.UI ui = null;
    private EventListenerList fStateListeners = null ;

    /** Creates a new instance of mimsPlus */
    public MimsPlus() {
        super();
        fStateListeners = new EventListenerList() ;
    }
    
    // Use for mass images.
    public MimsPlus(UI ui, com.nrims.data.Opener image, int index ) {
        super();
        srcImage = image ;
        this.ui = ui;
        nMass = index ;
        nType = MASS_IMAGE ;
        massNumber = new Double(image.getMassNames()[index]);
        
        try {
            image.setStackIndex(0);
            ij.process.ImageProcessor ip = new ij.process.ShortProcessor(
                image.getWidth(),
                image.getHeight(),
                image.getPixels(index),
                null);
/*
            float[] foo = ip.getCalibrationTable();
            for(int i =0; i<foo.length; i++) {
                System.out.println("CalTable " + i + " = " +foo[i]);
            }
*/
            massNumber = new Double(image.getMassNames()[index]);
            String title = "m" + massNumber + " : " + ui.getImageFilePrefix();
            setProcessor(title, ip);
            getProcessor().setMinAndMax(0, 65535);
            fStateListeners = new EventListenerList() ;
            //setProperty("Info", srcImage.getInfo());
            bIgnoreClose = true;            
        } catch (Exception x) { IJ.log(x.toString());}
    }
    
    // Use for segmented images.
    public MimsPlus(UI ui,int width, int height, int[] pixels, String name) {
        super();
        this.ui=ui;
        nType = SEG_IMAGE ;
           
        try {
            ij.process.ImageProcessor ipp = new ij.process.ColorProcessor(
                width,
                height,
                pixels);
        
            setProcessor(name, ipp);
            fStateListeners = new EventListenerList() ;
        } catch (Exception x) { IJ.log(x.toString());}
    }
    
    // This contructor is used for making SUM images.
    public MimsPlus(UI ui,int width, int height, double[] pixels, String name) {
           super();
           this.ui=ui;
           nType = SUM_IMAGE ;
           
           try {
               ij.process.FloatProcessor ipp = new ij.process.FloatProcessor(width, height, pixels);
               String title = name;
               setProcessor(title, ipp);
               fStateListeners = new EventListenerList() ;
        } catch (Exception x) { IJ.log(x.toString());}
    }    
    
    // Use for ratio and hsi images.
    public MimsPlus(UI ui, com.nrims.data.Opener image, HSIProps props, boolean bIsHSI ) {
        super();
        try {
           srcImage = image ;
           this.ui = ui;
           nMass = -1 ;
           int numIndex = props.getNumMass() ;
           int denIndex = props.getDenMass() ;
           nType = bIsHSI ? HSI_IMAGE : RATIO_IMAGE ;
           String title = ui.getImageFilePrefix();
           nRatioNum = props.getNumMass() ;
           nRatioDen = props.getDenMass() ;
           scaleFactor = props.getRatioScaleFactor();
           System.out.println("set MimsPlus.scaleFactor = " + props.getRatioScaleFactor());
           numerMassNumber = new Double(image.getMassNames()[numIndex]);
           denomMassNumber = new Double(image.getMassNames()[denIndex]);
           
           if(bIsHSI) {
               int height = image.getHeight() ;
               if( props.getLabelMethod() > 0) {
                    height += 16;
                }
               int [] rgbPixels = new int[image.getWidth()*height];
               ij.process.ImageProcessor ip = new ij.process.ColorProcessor(
                   image.getWidth(),
                   height,
                   rgbPixels);
               title = "HSI : m" + srcImage.getMassNames()[numIndex]+"/m"+ srcImage.getMassNames()[denIndex] + " : " + title;
               setProcessor(title, ip);
               getProcessor().setMinAndMax(0, 255); // default display range
               fStateListeners = new EventListenerList() ;
               
               // For HSI images make a copy of a ratio image without 
               // showing it. This is useful for getting data associated 
               // with line plot medianization.             
               ratioMims = ui.computeRatio(props, false);

               //TODO fix me
               if(ui.getIsSum()) {
                   this.initializeHSISum(props);
               }

            }
            else {
               float [] fPixels = new float[image.getWidth()*image.getHeight()];
               ij.process.ImageProcessor ip = new ij.process.FloatProcessor(
                   image.getWidth(),
                   image.getHeight(),
                   fPixels,
                   null );
               title = "m" + srcImage.getMassNames()[numIndex]+"/m"+ srcImage.getMassNames()[denIndex] + " : " + title;
               setProcessor(title, ip);
               getProcessor().setMinAndMax(0, 1.0); // default display range
               fStateListeners = new EventListenerList() ;

               if(ui.getIsSum()) {
                   this.initializeHSISum(props);
               }
            }          
            /*
            info += "Numerator=" + srcImage.getMassName(numIndex)+"\n";
            info += "Denominator=" + srcImage.getMassName(denIndex)+"\n";
            info += srcImage.getInfo();
            setProperty("Info", info) ;
            */
        }
        catch(Exception x) { IJ.log(x.toString()); }
    }
    
    public void setSize(int w, int h) {
        if(nType == HSI_IMAGE) {
               int [] rgbPixels = new int[w*h];
               ij.process.ImageProcessor ip = new ij.process.ColorProcessor(
                   w,h,rgbPixels);
               setProcessor(null, ip);
        }
        else if(nType == RATIO_IMAGE) {
              float [] fPixels = new float[w*h];
               ij.process.ImageProcessor ip = new ij.process.FloatProcessor(
                   w, h, fPixels, null );
               setProcessor(null,ip);
        }
        else {
            short [] spixels = new short[w*h];
            ij.process.ImageProcessor ip = new ij.process.ShortProcessor(
                w,h,spixels,null);
            setProcessor(null, ip);
        }
    }
    
    //not hit from MimsStackEditing.concatImages()
    //only used when opening a multiplane image
    public void appendImage(int nImage) throws Exception {
        if(srcImage == null) {
            throw new Exception("No image opened?");
        }
        if(nImage >= srcImage.getNImages()) {
            throw new Exception("Out of Range");
        }
        ij.ImageStack stack = getStack();
        srcImage.setStackIndex(nImage);
        stack.addSlice(null,srcImage.getPixels(nMass));
        setStack(null,stack);
        setSlice(nImage+1);
        //setProperty("Info", srcImage.getInfo());
        bIgnoreClose = true ;
        bIsStack = true ;
       
    }
    
    public com.nrims.data.Opener getOpener() { return srcImage ; }
    public int getMimsMassIndex() { return nMass ; }
    public int getMimsType() { return nType ; }

    public SumProps getSumProps() {
       
        // Set window location parameters.
        sumProps.setXWindowLocation(this.getWindow().getX());
        sumProps.setYWindowLocation(this.getWindow().getY());
        
        return sumProps;               
    } 
    
    public HSIProps getHSIProps() { 
        HSIProps props = new HSIProps();
        
        if ( getHSIProcessor() != null ) 
           props = getHSIProcessor().getProps();            
        else
           props = new HSIProps();
        
        if(nType == RATIO_IMAGE || nType == HSI_IMAGE) {
           
           // If ratio, set numerator and denominator masses.
           props.setNumMass(nRatioNum);
           props.setDenMass(nRatioDen);
           props.setRatioScaleFactor(scaleFactor);
                
           // Unlikee an HSIimage, the props of a ratio image 
           // CAN NOT be changed. Therefore we get the default values
           // and overwrite the minRatio and maxRatio with the current 
           // min and max used by ImageJ to render the image.                                 
           props.setMaxRatio(this.getProcessor().getMax());
           props.setMinRatio(this.getProcessor().getMin());
                              
        }       
        
        // Set window location parameters.
        if(this.getWindow()!=null) {
            props.setXWindowLocation(this.getWindow().getX());
            props.setYWindowLocation(this.getWindow().getY());
        }

        return props; 
    }
    
    public int getNumMass() { return nRatioNum ; }
    public int getDenMass() { return nRatioDen ; }


    public float[] medianizeInternalRatio(double radius) {

        if( this.internalRatio != null ) {

                ij.plugin.filter.RankFilters rfilter = new ij.plugin.filter.RankFilters();
                rfilter.rank(this.internalRatio.getProcessor(), radius, rfilter.MEDIAN);
                
                return ( (float[]) this.internalRatio.getProcessor().getPixels() );
        }

        //should never hit
        return null;
    }

    @Override
    public String getShortTitle() {
        String tempstring = this.getTitle();
        int colonindex = tempstring.indexOf(":");
        if(colonindex>0) return tempstring.substring(0, colonindex-1);
        else return "";
    }

    //get an image title better for saving
    //DOES NOT WORK YET...
    public String getTitleFileMass() {
        String tempstring = this.getTitle();
        String newname = this.getOpener().getImageFile().getName();
        
        //int colonindex = tempstring.indexOf(":");
        //newname = tempstring.substring(colonindex+1,tempstring.length());
        
        newname += "_" + this.getShortTitle().replace("/", "-");
        //System.out.println(newname);
        return newname;
    }
    
    //returns the rounded mass value(s) of an image, eg "26" or "27/26"
    public String getRoundedTitle() {
        if (this.getMimsType() == this.MASS_IMAGE) {
            String tempstring = this.getTitle();
            int colonindex = tempstring.indexOf(":");
            tempstring = tempstring.substring(1, colonindex-1);
            int massint = java.lang.Math.round(Float.parseFloat(tempstring));
            return Integer.toString(massint);
        }
        if (this.getMimsType() == this.RATIO_IMAGE) {
            String tempstring = this.getTitle();
            int colonindex = tempstring.indexOf(":");
            int slashindex = tempstring.indexOf("/");
            String neumstring = tempstring.substring(1,slashindex);
            String denstring = tempstring.substring(slashindex+2, colonindex-1);
            int nint = java.lang.Math.round(Float.parseFloat(neumstring));
            int dint = java.lang.Math.round(Float.parseFloat(denstring));
            return Integer.toString(nint)+"/"+Integer.toString(dint);
        }
        return "0";
    }

    public void setbIgnoreClose(boolean b) {
        this.bIgnoreClose = b;
    }

    @Override
    public void show() {
        ij.gui.ImageWindow win = getWindow() ;
        super.show() ;
        if(win == null && getWindow() != null) {
            if(!(getWindow().getCanvas() instanceof MimsCanvas )) {
                if (getStackSize() > 1) {
                    new StackWindow(this, new MimsCanvas(this, ui));
                } else {
                    new ImageWindow(this, new MimsCanvas(this, ui));
                }
            }
            getWindow().addWindowListener(this);
            getWindow().getCanvas().addMouseListener(this);
            getWindow().getCanvas().addMouseMotionListener(this);            
            if(ui.getDebug()) {
                ui.updateStatus("mimsPlus::show() addWindowListener " + getWindow().toString());
            }
        }
    }
    
    @Override
    public void setRoi(ij.gui.Roi roi) {
        if(roi == null) super.killRoi();
        else super.setRoi(roi);
        stateChanged(roi,MimsPlusEvent.ATTR_SET_ROI);
    }

    @Override
    public void windowClosing(WindowEvent e) {
//        System.out.println("STOP");
//        this.bIgnoreClose =false;
//        this.
        
    }
    @Override
    public void windowClosed(WindowEvent e) {
        // When opening a stack, we get a close event 
        // from the original ImageWindow, and ignore this event
        if(bIgnoreClose) {
            if(ui.getDebug()) {
                ui.updateStatus("Ignoring close..");
            }
            bIgnoreClose = false ;
            if(ui.getDebug()) {
                ui.updateStatus("Event window = " + e.getWindow().toString() + " mimsPlus = " + getWindow().toString());
            }
            return ;
        }
        if(ui.getDebug()) {
            ui.updateStatus("mimsPlus listener window closed");
        }
        stateChanged(0,MimsPlusEvent.ATTR_IMAGE_CLOSED);
        ui.getCBControl().removeWindowfromList(this);
    }
    public void windowStateChanged(WindowEvent e) {}
    @Override
    public void windowDeactivated(WindowEvent e) {}
    @Override
    public void windowActivated(WindowEvent e) {
        ui.setActiveMimsPlus(this); 
        ui.getCBControl().setWindowlistCombobox(getTitle()); 
    }
    @Override
    public void windowDeiconified(WindowEvent e) {}
    @Override
    public void windowIconified(WindowEvent e) {}
    @Override
    public void windowOpened(WindowEvent e) {
        if(ui.getDebug()) {
            ui.updateStatus("mimsPlus window opened");
        }
        WindowListener [] wl = null;
        if( getWindow() != null ) {
            wl = getWindow().getWindowListeners();
        } else { return; }
        boolean bFound = false ;
        int i ;
        for(i = 0 ; i < wl.length ; i++ ) {
            if(wl[i] == this) {
                bFound = true;
            }
        }
        if(!bFound) {
            getWindow().addWindowListener(this);
        }
        bFound = false ;
        MouseListener [] ml = getWindow().getCanvas().getMouseListeners();
        for(i=0;i<ml.length;i++) {
            if(ml[i] == this) {
                bFound = true;
            }
        }
        if(!bFound) {
            getWindow().getCanvas().addMouseListener(this);
            getWindow().getCanvas().addMouseMotionListener(this);
        }
        if(ui.getDebug()) {
            ui.updateStatus("mimsPlus::windowOpened listener installed");
        }
    }
    @Override
    public void mouseExited(MouseEvent e){ ui.updateStatus(" "); }
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) { 
      
      if(getRoi() != null) {                  
         
         // Set the moving flag so we know if user is attempting to move a roi.
         // Line Rois have to be treated differently because their state is never MOVING .       
         int roiState = getRoi().getState();
         int roiType = getRoi().getType();
         
         if (roiState == Roi.MOVING) bMoving = true;
         else if (roiType == Roi.LINE && roiState == Roi.MOVING_HANDLE) bMoving = true;
         else bMoving = false;
         
         // Highlight the roi in the jlist that the user is selecting
         if (roi.getName() != null) {
            int i = ui.getRoiManager().getIndex(roi.getName());
            ui.getRoiManager().select(i);
         }
         
         // Get the location so that if the user simply declicks without 
         // moving, a duplicate roi is not created at the same location.
         Rectangle r = getRoi().getBounds();
         x1 = r.x; y1 = r.y; w1 = r.width; h1 = r.height;         
         
      }
    }
    @Override
    public void mouseClicked(MouseEvent e) {

         float[] pix;
         if (this.nType == HSI_IMAGE ) {
            internalRatio.setRoi(getRoi());
            pix = (float[])internalRatio.getProcessor().getPixels();
            internalRatio.killRoi();
         } else if(this.nType == RATIO_IMAGE || this.nType == SUM_IMAGE) {
             pix = (float[])getProcessor().getPixels();
         } else {
            short[] spix = (short[])getProcessor().getPixels();
            pix = new float[spix.length];
            for(int i = 0; i < spix.length; i++)
                pix[i] = (new Short(spix[i])).floatValue();
         }
         if (pix != null) {
            double[] dpix = new double[pix.length];
            for (int i = 0; i < pix.length; i++) {
                dpix[i] = (new Float(pix[i])).doubleValue();
            }
            ui.getRoiControl().updateHistogram(dpix, getShortTitle(), true);

            String stats = "";
            stats += this.getShortTitle() + ": ";
            stats += "mean = " +  IJ.d2s(this.getStatistics().mean, 2) + " ";
            stats += "sd = " +  IJ.d2s(this.getStatistics().stdDev, 2);
            ui.updateStatus(stats);
         }

    }
    /**
     * Catch drawing ROIs to enable updating other images with the same ROI
     * @param e MouseEvent
     */
    @Override
    public void mouseReleased(MouseEvent e) {
      if (bStateChanging) {
         return;
      }
      if (bMoving) {
        Roi thisroi = getRoi();   
        
        // Prevent duplicate roi at same location
        Rectangle r = thisroi.getBounds();
        x2 = r.x; y2 = r.y; w2 = r.width; h2 = r.height;         
        if (x1 == x2 && y1 == y2 && w1 == w2 && h1 == h2) return;
        
        stateChanged(getRoi(), MimsPlusEvent.ATTR_ROI_MOVED);
       
        ui.getRoiManager().resetSpinners(thisroi);
         
        bMoving = false;
        return;
      }

      switch (Toolbar.getToolId()) {
         case Toolbar.RECTANGLE:
         case Toolbar.OVAL:
         case Toolbar.LINE:
         case Toolbar.FREELINE:
         case Toolbar.FREEROI:
         case Toolbar.POINT:
         case Toolbar.POLYGON:
         case Toolbar.POLYLINE:
            stateChanged(getRoi(), MimsPlusEvent.ATTR_MOUSE_RELEASE);
            break;
         case Toolbar.WAND:
            if (getRoi() != null) {
               stateChanged(getRoi(), MimsPlusEvent.ATTR_MOUSE_RELEASE);
            }
            break;
      }            
   }
    
    //rollover pixel value code
    public void mouseMoved(MouseEvent e) {

        if(this.ui.isOpening()) {
            return;
        }
        
        int x = (int) e.getPoint().getX();
        int y = (int) e.getPoint().getY();
        int mX = getWindow().getCanvas().offScreenX(x);
        int mY = getWindow().getCanvas().offScreenY(y);        
        String msg = "" + mX + "," + mY ;
        if(isStack()) {
            msg += "," + getCurrentSlice() + " = ";
        }
        else {
            msg += " = ";
        }
        if(this.nType == HSI_IMAGE && (this.internalDenominator!=null && this.internalNumerator!=null) ) {
            float ngl = internalNumerator.getProcessor().getPixelValue(mX, mY);
            float dgl = internalDenominator.getProcessor().getPixelValue(mX, mY);
            double ratio = internalRatio.getProcessor().getPixelValue(mX, mY);

            msg += "S (" + (int)ngl + " / " + (int)dgl + ") = " + IJ.d2s(ratio, 4);
        } 
        else if(this.nType == RATIO_IMAGE && (this.internalDenominator!=null && this.internalNumerator!=null) ) {
           float ngl = internalNumerator.getProcessor().getPixelValue(mX, mY);
            float dgl = internalDenominator.getProcessor().getPixelValue(mX, mY);
            //double ratio = internalRatio.getProcessor().getPixelValue(mX, mY);
            float ratio = this.getProcessor().getPixelValue(mX, mY);
            msg += "S (" + (int)ngl + " / " + (int)dgl + ") = " + IJ.d2s(ratio, 4); 
        }
        /*else if(this.nType == RATIO_IMAGE) {
            int n = getHSIProps().getNumMass();
            int d = getHSIProps().getDenMass();
            MimsPlus [] ml = ui.getMassImages() ;
            if(ml.length > n && ml.length > d && ml[n] != null && ml[d] != null ) {
                int [] ngl = ml[n].getPixel(mX,mY);
                int [] dgl = ml[d].getPixel(mX,mY);
                float r = this.getProcessor().getPixelValue(mX, mY);
                msg += "S (" + ngl[0] + " / " + dgl[0] + ") = " + IJ.d2s(r,4);
            }         
        }*/
        else if(this.nType == SUM_IMAGE) {
            float ngl, dgl;
            if (numeratorSum != null && denominatorSum != null) {
               ngl = numeratorSum.getProcessor().getPixelValue(mX, mY);
               dgl = denominatorSum.getProcessor().getPixelValue(mX, mY);
               msg += " S (" + ngl + " / " + dgl + ") = ";
            }
            int[] gl = getPixel(mX, mY);
            float s = Float.intBitsToFloat(gl[0]);
            msg += IJ.d2s(s,0);
        }
        else {
            MimsPlus[] ml = ui.getOpenMassImages() ;
            for(int i = 0 ; i < ml.length ; i++ ) {
                int [] gl = ml[i].getPixel(mX,mY);
                msg += gl[0] ;
                if( i+1 < ml.length ) {
                    msg += ", ";
                }
            }
        }
            
        int displayDigits = 2;
        //should be in preferences
        boolean insideRoi = false;
        java.util.Hashtable rois = ui.getRoiManager().getROIs();        
        for(Object key:rois.keySet()) {
            Roi roi = (Roi)rois.get(key);
            int slice=1;
            if(this.getMimsType()==this.HSI_IMAGE || this.getMimsType()==this.RATIO_IMAGE) {
                int neum = getHSIProps().getNumMass();
                slice = ui.getMassImage(neum).getCurrentSlice();
            } else {
                slice = getCurrentSlice();
            }
            
            boolean linecheck = false;
            int c = -1;
            if( (roi.getType() == roi.LINE) || (roi.getType() == roi.POLYLINE) || (roi.getType() == roi.FREELINE) ) {
                c = roi.isHandle(x, y);
                if(c != -1) linecheck=true;
                
            }
            
            if(roi.contains(mX, mY) || linecheck) {
               if (ui.getSyncROIsAcrossPlanes() || ui.getRoiManager().getSliceNumber(key.toString()) == slice) {
                  insideRoi = true;
                                    
                  // Update message.
                  ij.process.ImageStatistics stats;                 
                  if(this.getMimsType()==HSI_IMAGE && internalRatio!=null) {
                      internalRatio.setRoi(roi);
                     stats = internalRatio.getStatistics();
                      internalRatio.killRoi();
                  } else {
                     stats = this.getStatistics();
                  }
                  msg += "\t ROI " + roi.getName() + ": A=" + IJ.d2s(stats.area, 0) + ", M=" + IJ.d2s(stats.mean, displayDigits) + ", Sd=" + IJ.d2s(stats.stdDev, displayDigits);
                  
                  // Set Roi to yellow.
                  if(ui.activeRoi != roi){                   
                    ui.activeRoi = roi;
                    setRoi(roi);
                  }
                  
                  updateHistogram(true);
                  updateLineProfile();           
                  
                  break;
               } // End - if (ui.getSyncROIsAcrossPlanes() 
            } // End - if(contains)
        } // End - for(Object key:rois.keySet())
        if (ui.activeRoi != null && !insideRoi){
           ui.activeRoi = null;
           setRoi((Roi)null);
        } 
        ui.updateStatus(msg);
    }
    
    @Override
    // Display statistics while dragging or creating ROIs.
    public void mouseDragged(MouseEvent e) {
      
       // get mouse poistion
       int x = (int) e.getPoint().getX();
       int y = (int) e.getPoint().getY();
       int mX = getWindow().getCanvas().offScreenX(x);
       int mY = getWindow().getCanvas().offScreenY(y); 
       String msg = "" + mX + "," + mY ;

       // precision
       int displayDigits = 2;
       
       // Get the ROI, (the area in yellow).
      Roi roi = getRoi();

      // Display stats in the message bar.
      if (roi != null) {
         ij.process.ImageStatistics stats = this.getStatistics();
         if (this.getMimsType() == MimsPlus.HSI_IMAGE) {
            msg += "\t ROI " + roi.getName() + ": A=" + IJ.d2s(stats.area, 0);
         } else {
            msg += "\t ROI " + roi.getName() + ": A=" + IJ.d2s(stats.area, 0) + ", M=" + IJ.d2s(stats.mean, displayDigits) + ", Sd=" + IJ.d2s(stats.stdDev, displayDigits);
         }

         ui.updateStatus(msg);

         // Set Roi to yellow.
         if (ui.activeRoi != roi) {
            ui.activeRoi = roi;
            setRoi(roi);
         }        
         
         updateHistogram(false);
         updateLineProfile();

      }
   }
    
    public void updateHistogram(boolean force) {      
      
      // Update histogram (area Rois only).      
      if ((roi.getType() == roi.FREEROI) || (roi.getType() == roi.OVAL) ||
          (roi.getType() == roi.POLYGON) || (roi.getType() == roi.RECTANGLE)) {
         String label = this.getShortTitle() + " ROI: " + roi.getName();
         double[] roiPix;
         if (this.nType == HSI_IMAGE) {
            internalRatio.setRoi(getRoi());
            roiPix = internalRatio.getRoiPixels();
            internalRatio.killRoi();
         } else {
            roiPix = this.getRoiPixels();
         }
         if (roiPix != null) {
            ui.getRoiControl().updateHistogram(roiPix, label, force);
         }
      }

   }
    
   public void updateLineProfile() {

      // Line profiles for ratio images and HSI images should be identical.
      if ((roi.getType() == roi.LINE) || (roi.getType() == roi.POLYLINE) || (roi.getType() == roi.FREELINE)) {
         if (this.nType == HSI_IMAGE) {
            internalRatio.setRoi(getRoi());
            ij.gui.ProfilePlot profileP = new ij.gui.ProfilePlot(internalRatio);
            internalRatio.killRoi();
            ui.updateLineProfile(profileP.getProfile(), this.getShortTitle() + " : " + roi.getName(), this.getProcessor().getLineWidth());
         } else {
            ij.gui.ProfilePlot profileP = new ij.gui.ProfilePlot(this);
            ui.updateLineProfile(profileP.getProfile(), this.getShortTitle() + " : " + roi.getName(), this.getProcessor().getLineWidth());
         }
      }

   }

    // TODO - needs to more easily handle HSI images
    public double[] getRoiPixels() {
        if (this.getRoi()==null) return null;
               
        Rectangle rect = roi.getBoundingRect();
        ij.process.ImageProcessor imp = this.getProcessor();
        ij.process.ImageStatistics stats = this.getStatistics();
        
        byte[] mask = imp.getMaskArray();
        int i, mi;
        
        if (mask == null) {
            double[] pixels = new double[rect.width * rect.height];
            i = 0;

            for (int y = rect.y; y < (rect.y + rect.height); y++) {
                for (int x = rect.x; x < (rect.x + rect.width); x++) {
                    pixels[i] = imp.getPixelValue(x, y);
                    i++;
                }
            }
            //System.out.println("pixels.length: " + pixels.length);
            return pixels;
        } else {
            java.util.ArrayList<Double> pixellist = new java.util.ArrayList<Double>();
            for (int y = rect.y, my = 0; y < (rect.y + rect.height); y++, my++) {
                i = y * width + rect.x;
                mi = my * rect.width;
                for (int x = rect.x; x < (rect.x + rect.width); x++) { 
                   
                    // I had to add this line because oval Rois were generating
                    // an OutOfBounds exception when being dragged off the canvas.
                    if (mi >= mask.length) break;
                    
                    // mask should never be null here.
                    if (mask == null || mask[mi++] != 0) {
                        pixellist.add((double)imp.getPixelValue(x, y));
                    }
                    i++;
                }
            }
            double[] foo = new double[pixellist.size()];
            for(int j =0; j< foo.length; j++) 
                foo[j] = pixellist.get(j);
            return foo;
        }
    }

    public void initializeHSISum(HSIProps props) {
          numeratorSum = ui.computeSum(ui.getMassImage(props.getNumMass()), false);
          denominatorSum = ui.computeSum(ui.getMassImage(props.getDenMass()), false);        
    }

    public void recomputeInternalImages() {

        // Only to be called on HSI images.
        if(nType != HSI_IMAGE && nType != RATIO_IMAGE ) return;

        // Get the properties.
        HSIProps props = this.getHSIProps();

        // Get the numerator and denominator mass images.
        MimsPlus parentNum = ui.getMassImage( props.getNumMass() );
        MimsPlus parentDen = ui.getMassImage( props.getDenMass() );

        // Setup list for sliding window, entire image, or single plane.
        java.util.ArrayList<Integer> list = new java.util.ArrayList<Integer>();
        int windowSize = ui.getWindowRange();
        int currentplane = parentNum.getSlice();               
            if (!ui.getIsSum() && !ui.getIsWindow()) {
                list.add(currentplane);

            } else if (ui.getIsSum()) {
                for (int i = 1; i <= parentNum.getNSlices(); i++) {
                    list.add(i);
                }
            } else if (ui.getIsWindow()) {
                int lb = currentplane - windowSize;
                int ub = currentplane + windowSize;
                for (int i = lb; i <= ub; i++) {
                    list.add(i);
                }
            }

        // Compute the sum of the numerator and denominator mass images.
        this.internalNumerator = ui.computeSum(parentNum, false, list);
        this.internalDenominator = ui.computeSum(parentDen, false, list);

        // Fill in the data.
        int npixels = this.internalNumerator.getProcessor().getPixelCount();        
        float[] rpixels = new float[npixels];
        float[] numpixels = (float[]) internalNumerator.getProcessor().getPixels();
        float[] denpixels = (float[]) internalDenominator.getProcessor().getPixels();
        float rMax = 0.0f;
        float rMin = 1000000.0f;
        float sf = (float) props.getRatioScaleFactor();
        for(int i = 0; i < rpixels.length; i++) {
            if (numpixels[i] >= 0 && denpixels[i] > 0) {
                rpixels[i] =  sf*(numpixels[i]/denpixels[i]);
                if (rpixels[i] > rMax) {
                    rMax = rpixels[i];
                } else if (rpixels[i] < rMin) {
                    rMin = rpixels[i];
                }
            } else {
                rpixels[i] = 0.0f;
            }
        }

        // Do some other stuff.
        if( internalRatio == null ) {
            internalRatio = new MimsPlus();
        }
        if( internalRatio.getProcessor() != null ) {
            internalRatio.getProcessor().setPixels(rpixels);
        } else {
            int procwidth = internalNumerator.getProcessor().getWidth();
            int procheight = internalNumerator.getProcessor().getHeight();
            ij.process.FloatProcessor tempProc = new ij.process.FloatProcessor(procwidth, procheight);
            tempProc.setPixels(rpixels);
            internalRatio.setProcessor("", tempProc);
        }

        // Medianize if needed.
        if(ui.getMedianFilterRatios()) {
            medianizeInternalRatio(ui.getMedianFilterRadius());
        }

        // Other stuff.
        internalNumerator.ui = ui;
        internalDenominator.ui = ui;
        internalRatio.ui = ui;
    }


        //copied and modified from ui.computeSum()
        public synchronized float[] returnSum() {
        boolean fail = true;

        int templength = this.getProcessor().getPixelCount();
        float[] sumPixels = new float[templength];
        short[] tempPixels = new short[templength];

        int startSlice = this.getCurrentSlice();

        if (this.getMimsType() == MimsPlus.MASS_IMAGE) {
            for (int i = 1; i <= this.getImageStackSize(); i++) {
                this.setSlice(i);
                tempPixels = (short[]) this.getProcessor().getPixels();
                for (int j = 0; j < sumPixels.length; j++) {
                    sumPixels[j] += tempPixels[j];
                }
            }
            this.setSlice(startSlice);
            fail = false;
        }

        if (this.getMimsType() == MimsPlus.RATIO_IMAGE) {
            int numMass = this.getNumMass();
            int denMass = this.getDenMass();
            MimsPlus nImage = ui.getMassImage(numMass);
            MimsPlus dImage = ui.getMassImage(denMass);
            float[] numPixels = new float[templength];
            float[] denPixels = new float[templength];

            startSlice = nImage.getCurrentSlice();

            for (int i = 1; i <= nImage.getImageStackSize(); i++) {
                nImage.setSlice(i);
                tempPixels = (short[]) nImage.getProcessor().getPixels();
                for (int j = 0; j < numPixels.length; j++) {
                    numPixels[j] += tempPixels[j];
                }
            }
            for (int i = 1; i <= dImage.getImageStackSize(); i++) {
                dImage.setSlice(i);
                tempPixels = (short[]) dImage.getProcessor().getPixels();
                for (int j = 0; j < denPixels.length; j++) {
                    denPixels[j] += tempPixels[j];
                }
            }
            float sf = (float) getHSIProps().getRatioScaleFactor();
            for (int i = 0; i < sumPixels.length; i++) {
                if (denPixels[i] != 0) {
                    sumPixels[i] = sf * (numPixels[i] / denPixels[i]);
                } else {
                    sumPixels[i] = 0;
                }
            }

            nImage.setSlice(startSlice);

            fail = false;
        }

        if (!fail) {
            return sumPixels;
        } else {
            return null;
        }
    }

    public MimsPlus getInternalRatio() {
        return this.internalRatio;
    }

    public MimsPlus getInternalNumerator() {
        return this.internalNumerator;
    }

    public MimsPlus getInternalDenominator() {
        return this.internalDenominator;
    }

    public MimsPlus getNumeratorSum() {
        return this.numeratorSum;
    }

    public void setNumeratorSum(MimsPlus mp) {
        this.numeratorSum = mp;
    }

    public MimsPlus getDenominatorSum() {
        return this.denominatorSum;
    }

    public void setDenominatorSum(MimsPlus mp) {
        this.denominatorSum = mp;
    }
    
    public void addListener( MimsUpdateListener inListener ) {
         fStateListeners.add(MimsUpdateListener.class, inListener );
    }
    
    public void removeListener(MimsUpdateListener inListener ) {
         fStateListeners.remove(MimsUpdateListener.class, inListener );
    }
    
    /**
     * extends setSlice to notify listeners when the frame updates
     * enabling synchronization with other windows
     */
    
    private void stateChanged(int slice, int attr) {
        bStateChanging = true ;
        MimsPlusEvent event = new MimsPlusEvent(this, slice, attr); 
        Object[] listeners = fStateListeners.getListenerList();
        for(int i=listeners.length-2; i >= 0; i -= 2){
            if(listeners[i] == MimsUpdateListener.class ){
                    ((MimsUpdateListener)listeners[i+1])
					.mimsStateChanged(event);
            }
        }
        bStateChanging = false ;
    }
    
    private void stateChanged(ij.gui.Roi roi, int attr) {
        MimsPlusEvent event = new MimsPlusEvent(this, roi, attr); 
        Object[] listeners = fStateListeners.getListenerList();
        for(int i=listeners.length-2; i >= 0; i -= 2){
            if(listeners[i] == MimsUpdateListener.class ){
                    ((MimsUpdateListener)listeners[i+1])
					.mimsStateChanged(event);
            }
        }
    }
    
    public synchronized void setSlice(int index) {
        if(getCurrentSlice() == index) {
            return;
        }
        super.setSlice(index);
        if(bStateChanging) {
            return;
        }
        stateChanged(index,MimsPlusEvent.ATTR_UPDATE_SLICE);       
    }
    public void setAllowClose(boolean allowClose){
        this.allowClose = allowClose;
    }        
    @Override
    public void close () {
        if(allowClose) {
            super.close();
        } else {
            ui.massImageClosed(this);
            this.hide();
        }
    }
    public void setHSIProcessor( HSIProcessor processor ) { this.hsiProcessor = processor ; }
    public HSIProcessor getHSIProcessor() { return hsiProcessor ; }
    
    public void setSumProps(SumProps props) { this.sumProps = props; }
       
    public void setParentImageName(String name) { this.parentImageName = name; }
    public String getParentImageName() { return parentImageName ; }    
    
    public void setAutoContrastAdjust( boolean auto ) { this.autoAdjustContrast = auto ; }
    public boolean getAutoContrastAdjust() { return autoAdjustContrast ; }
    
    public boolean isStack() { return bIsStack ; }
    public void setIsStack(boolean isS) { bIsStack = isS; }
    
    public double getMassNumber() {return massNumber;}
    public double getDenomMassNumber() {return denomMassNumber;}
    public double getNumerMassNumber() {return numerMassNumber;}
    
    public UI getUI() { return ui ; }

}

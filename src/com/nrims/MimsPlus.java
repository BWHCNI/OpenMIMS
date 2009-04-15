package com.nrims;
import ij.IJ ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.* ;
//import ij.process.*;
import java.awt.Rectangle;
import java.awt.event.WindowEvent ;
import java.awt.event.WindowListener ;
import java.awt.event.MouseListener ;
import java.awt.event.MouseMotionListener ;
import java.awt.event.MouseEvent ;
import java.util.Calendar;
import javax.swing.event.EventListenerList;
/**
 * extends ImagePlus with methods to synchronize display of multiple stacks
 * and drawing ROIs in each windows
 * 
 */
public class MimsPlus extends ij.ImagePlus implements WindowListener, MouseListener, MouseMotionListener {
    
    static final int MASS_IMAGE = 0 ;
    static final int RATIO_IMAGE = 1 ;
    static final int HSI_IMAGE  =  2 ;
    static final int SEG_IMAGE = 3 ;
    static final int SUM_IMAGE = 4 ;
    
    MimsPlus ratioMims;
    
    /** Creates a new instance of mimsPlus */
    public MimsPlus() {
        super();
        fStateListeners = new EventListenerList() ;
    }
    
    public MimsPlus(com.nrims.data.Opener image, int index ) {
        super();
        srcImage = image ;
        ui = srcImage.getUI();
        nMass = index ;
        nType = MASS_IMAGE ;
        
        try {
            image.setMassIndex(index);
            image.setStackIndex(0);
            ij.process.ImageProcessor ip = new ij.process.ShortProcessor(
                image.getWidth(),
                image.getHeight(),
                image.getPixels(index),
                null);
            String title = "m" + image.getMassName(index) + " : " + image.getName();
            setProcessor(title, ip);
            getProcessor().setMinAndMax(0, 65535); // default display range
            fStateListeners = new EventListenerList() ;
            setProperty("Info", srcImage.getInfo());
            //added, was only set in appendImage(), only called
            //for multi-plane images
            bIgnoreClose = true;
            //------
        } catch (Exception x) { IJ.log(x.toString());}
    }
    
    public MimsPlus(UI ui,int width, int height, int[] pixels, String name) {
        super();
        this.ui=ui;
           // srcImage = image ;
           //ui = srcImage.getUI();
          // nMass = -1;
        nType = SEG_IMAGE ;
           
        try {
           // image.setMassIndex(0);
           // image.setStackIndex(0);
            ij.process.ImageProcessor ipp = new ij.process.ColorProcessor(
                width,
                height,
                pixels);
        
            setProcessor(name, ipp);
          //     getProcessor().setMinAndMax(0, 65535); // default display range
            fStateListeners = new EventListenerList() ;
        } catch (Exception x) { IJ.log(x.toString());}
    }
    
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
    
    public MimsPlus( com.nrims.data.Opener image, HSIProps props, boolean bIsHSI ) {
        super();
        try {
           srcImage = image ;
           ui = srcImage.getUI();
           nMass = -1 ;
           int numIndex = props.getNumMass() ;
           int denIndex = props.getDenMass() ;
           nType = bIsHSI ? HSI_IMAGE : RATIO_IMAGE ;
           String title = image.getName();
           String info = "";
           nRatioNum = props.getNumMass() ;
           nRatioDen = props.getDenMass() ;
           
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
               title = "HSI m" + srcImage.getMassName(numIndex)+":m"+ srcImage.getMassName(denIndex) + " : " + title;
               setProcessor(title, ip);
               getProcessor().setMinAndMax(0, 255); // default display range
               fStateListeners = new EventListenerList() ;
               info += "Type=HSI\n";
               
               // For HSI images make a copy of a ratio image without 
               // showing it. This is useful for getting data associated 
               // with line plot medianization.             
               ratioMims = ui.computeRatio(props, false);
            }
            else {
               float [] fPixels = new float[image.getWidth()*image.getHeight()];
               ij.process.ImageProcessor ip = new ij.process.FloatProcessor(
                   image.getWidth(),
                   image.getHeight(),
                   fPixels,
                   null );
               title = "m" + srcImage.getMassName(numIndex)+"/m"+ srcImage.getMassName(denIndex) + " : " + title;
               info += "Type=Ratio";
               setProcessor(title, ip);
               getProcessor().setMinAndMax(0, 1.0); // default display range
               fStateListeners = new EventListenerList() ;
            }

            info += "Numerator=" + srcImage.getMassName(numIndex)+"\n";
            info += "Denominator=" + srcImage.getMassName(denIndex)+"\n";
            info += srcImage.getInfo();
            setProperty("Info", info) ;           
            
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
        if(nImage >= srcImage.nImages()) {
            throw new Exception("Out of Range");
        }
        ij.ImageStack stack = getStack();
        srcImage.setStackIndex(nImage);
        srcImage.setMassIndex(nMass);
        stack.addSlice(null,srcImage.getPixels(nMass));
        setStack(null,stack);
        setSlice(nImage+1);
        setProperty("Info", srcImage.getInfo());
        bIgnoreClose = true ;
        bIsStack = true ;
       
    }
    
    public com.nrims.data.Opener getOpener() { return srcImage ; }
    public int getMimsMassIndex() { return nMass ; }
    public int getMimsType() { return nType ; }

    public HSIProps getHSIProps() { 
        if(nType == HSI_IMAGE) {
            if( getHSIProcessor() != null ) {
                return getHSIProcessor().getProps();
            }
        }
        else if(nType == RATIO_IMAGE) {
            if( getHSIProcessor() != null ) {
                return getHSIProcessor().getProps();
            }
            else {
                HSIProps props = new HSIProps() ;
                props.setNumMass(nRatioNum);
                props.setDenMass(nRatioDen);
                
                // Unlikee an HSIimage, the props of a ratio image 
                // CAN NOT be changed. Therefore we get the default values
                // and overwrite the minRatio and maxRatio with the current 
                // min and max used by ImageJ to render the image.                                 
                props.setMaxRatio(this.getProcessor().getMax());
                props.setMinRatio(this.getProcessor().getMin());
                
                return props ;
            }
        }
        return null ; 
    }
    
    public int getNumMass() { return nRatioNum ; }
    public int getDenMass() { return nRatioDen ; }
    
    @Override
    public String getShortTitle() {
        String tempstring = this.getTitle();
        int colonindex = tempstring.indexOf(":");
        //System.out.println("asdf="+tempstring.substring(0, colonindex-1));
        return tempstring.substring(0, colonindex-1);
    }
    
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
        WindowListener [] wl = getWindow().getWindowListeners();
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
    public void mouseClicked(MouseEvent e) {}    
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
        if(this.nType == HSI_IMAGE) { 
            int n = getHSIProps().getNumMass();
            int d = getHSIProps().getDenMass();
            MimsPlus [] ml = ui.getMassImages() ;
            if(ml.length > n && ml.length > d && ml[n] != null && ml[d] != null) {
                int [] ngl = ml[n].getPixel(mX,mY);
                int [] dgl = ml[d].getPixel(mX,mY);
                double ratio = 0.0 ;
                if( dgl[0] > 0 ) {
                    ratio = ui.getRatioScaleFactor()*((double) ngl[0] / (double) dgl[0]);
                }
                msg += "S (" + ngl[0] + " / " + dgl[0] + ") = " + IJ.d2s(ratio,4);
            }
        } 
        else if(this.nType == RATIO_IMAGE) {
            int n = getHSIProps().getNumMass();
            int d = getHSIProps().getDenMass();
            MimsPlus [] ml = ui.getMassImages() ;
            if(ml.length > n && ml.length > d && ml[n] != null && ml[d] != null ) {
                int [] ngl = ml[n].getPixel(mX,mY);
                int [] dgl = ml[d].getPixel(mX,mY);
                int [] rgl = getPixel(mX,mY);
                float r = Float.intBitsToFloat(rgl[0]);
                msg += "S (" + ngl[0] + " / " + dgl[0] + ") = " + IJ.d2s(r,4);
            }         
        }
        else if(this.nType == SUM_IMAGE) {
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
                  ij.process.ImageStatistics stats = this.getStatistics();

                  // Update message.
                  if(this.getMimsType()==this.HSI_IMAGE) {
                      msg += "\t ROI " + roi.getName() + ": A=" + IJ.d2s(stats.area, 0);
                  } else {
                      msg += "\t ROI " + roi.getName() + ": A=" + IJ.d2s(stats.area, 0) + ", M=" + IJ.d2s(stats.mean, displayDigits) + ", Sd=" + IJ.d2s(stats.stdDev, displayDigits);
                  }
                  
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
            ratioMims.setRoi(getRoi());
            roiPix = ratioMims.getRoiPixels();
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
            ratioMims.setRoi(getRoi());
            ij.gui.ProfilePlot profileP = new ij.gui.ProfilePlot(ratioMims);
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
                   
                    // Z.K. I had to add this line because oval Rois were generating
                    // an OutOfBounds exception when being dragged off the screen.
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
        if(allowClose) {super.close();}
    }
    public void setHSIProcessor( HSIProcessor processor ) { this.hsiProcessor = processor ; }
    public HSIProcessor getHSIProcessor() { return hsiProcessor ; }
    
    public void setAutoContrastAdjust( boolean auto ) { this.autoAdjustContrast = auto ; }
    public boolean getAutoContrastAdjust() { return autoAdjustContrast ; }
    
    public boolean isStack() { return bIsStack ; }
    public void setIsStack(boolean isS) { bIsStack = isS; }
    public UI getUI() { return ui ; }
    
    private boolean allowClose =true;
    private boolean bIgnoreClose = false ;
    boolean bMoving = false;
    boolean autoAdjustContrast = false;
    static boolean bStateChanging = false ;
    private boolean bWindowListenerInstalled = false ;
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
}

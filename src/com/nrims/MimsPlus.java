package com.nrims;
import ij.IJ ;
import ij.gui.* ;
//import ij.process.*;
import java.awt.event.WindowEvent ;
import java.awt.event.WindowListener ;
import java.awt.event.MouseListener ;
import java.awt.event.MouseMotionListener ;
import java.awt.event.MouseEvent ;
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
    
    public com.nrims.data.Opener getMimsImage() { return srcImage ; }
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
    }
    public void windowStateChanged(WindowEvent e) {}
    @Override
    public void windowDeactivated(WindowEvent e) {}
    @Override
    public void windowActivated(WindowEvent e) {
        ui.setActiveMimsPlus(this);
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
      // Highlight the selected ROI in the ROI list
      if(getRoi() != null) {
         
         // Set the moving flag so we know if 
         // user is attempting to move a roi.
         if (getRoi().getState() == Roi.MOVING) bMoving = true;
         else bMoving = false;
         
         // Highlight the roi in the jlist that the user is selecting
         if (roi.getName() != null) {
            int i = ui.getRoiManager().getIndex(roi.getName());
            ui.getRoiManager().select(i);
         }
         
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
        stateChanged(getRoi(), MimsPlusEvent.ATTR_ROI_MOVED);
        bMoving = false;
        return;
      }

      switch (Toolbar.getToolId()) {
         case Toolbar.RECTANGLE:
         case Toolbar.OVAL:
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
                //ui.updateStatus(msg + ngl[0] + " / " + dgl[0] + " = " + IJ.d2s(ratio,4) ) ;
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
                //ui.updateStatus(msg + ngl[0] + " / " + dgl[0] + " = " + IJ.d2s(r,4) ) ;
                msg += "S (" + ngl[0] + " / " + dgl[0] + ") = " + IJ.d2s(r,4);
            }         
        }
        else if(this.nType == SUM_IMAGE) {
            int[] gl = this.getPixel(mX, mY);
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
        for(Object key:rois.keySet()){
            Roi roi = (Roi)rois.get(key);
            if(roi.contains(mX, mY)){
                insideRoi = true;
                ij.process.ImageStatistics stats = this.getStatistics();
                msg += "\t ROI " + roi.getName() + ": A=" + IJ.d2s(stats.area, 0) + ", M=" + IJ.d2s(stats.mean, displayDigits) + ", Sd=" + IJ.d2s(stats.stdDev*stats.stdDev, displayDigits);                
                roi.setInstanceColor(java.awt.Color.yellow);
                if(ui.activeRoi != roi){
//                        //ij.process.ImageProcessor mask = ui.activeRoi==null?null:ui.activeRoi.getMask();
//                        ip.reset();
//                        ip.snapshot();
//                        setRoi(roi);
//                        ip.setValue(0);
//                        ip.fill(roi.getMask());
//                        setProcessor(null,ip);                    
                    ui.activeRoi = roi;
                    setRoi(roi);
                    //ui.mimsStateChanged(new mimsPlusEvent(this, roi, mimsPlusEvent.ATTR_SET_ROI));
                }
                break;
            }
        }
        if (ui.activeRoi != null && !insideRoi){
//                ip.reset();
            ui.activeRoi = null;
            setRoi((Roi)null);
            //ui.mimsStateChanged(new mimsPlusEvent(this, null, mimsPlusEvent.ATTR_SET_ROI));
        }
        
        ui.updateStatus(msg);
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {}
    
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
    
    public boolean isStack() { return bIsStack ; }
    public UI getUI() { return ui ; }
    
    private boolean allowClose =true;
    private boolean bIgnoreClose = false ;
    boolean bMoving = false;
    static boolean bStateChanging = false ;
    private boolean bWindowListenerInstalled = false ;
    private int nMass = 0 ;
    private int nType = 0 ;
    private int nRatioNum = 0 ;
    private int nRatioDen = 0 ;
    private boolean bIsStack = false ;
    private HSIProcessor hsiProcessor = null ;
    private com.nrims.data.Opener srcImage ;
    private com.nrims.UI ui = null;
    private EventListenerList fStateListeners = null ;
}

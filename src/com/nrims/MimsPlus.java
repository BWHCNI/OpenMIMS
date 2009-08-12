package com.nrims;

import com.nrims.data.Opener;

import ij.IJ ;
import ij.gui.* ;
import ij.plugin.filter.RankFilters;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.awt.event.WindowEvent ;
import java.awt.event.WindowListener ;
import java.awt.event.MouseListener ;
import java.awt.event.MouseMotionListener ;
import java.awt.event.MouseEvent ;
import java.util.ArrayList;
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

    // Internal images for test data display.
    public MimsPlus internalRatio;
    public MimsPlus internalNumerator;
    public MimsPlus internalDenominator;

    // Window position.
    private int xloc = -1;
    private int yloc = -1;

    // Props objects.
    public SumProps sumProps = null;
    public RatioProps ratioProps = null;
    public HSIProps hsiProps = null;

    public String title = "";
    private boolean allowClose =true;
    private boolean bIgnoreClose = false ;
    boolean bMoving = false;
    boolean autoAdjustContrast = false;
    static boolean bStateChanging = false ;
    private int massIndex = 0 ;
    private int nType = 0 ;
    private int x1, x2, y1, y2, w1, w2, h1, h2;
    private boolean bIsStack = false ;
    private HSIProcessor hsiProcessor = null ;
    private com.nrims.UI ui = null;
    private EventListenerList fStateListeners = null ;

    /** Creates a new instance of mimsPlus */
    public MimsPlus(UI ui) {
        super();
        this.ui = ui;
        fStateListeners = new EventListenerList() ;
    }
    
    // Use for mass images.
    public MimsPlus(UI ui, int index ) {
        super();
        this.ui = ui;
        this.massIndex = index ;
        this.nType = MASS_IMAGE;

        // Get a copy of the opener and setup image parameters.
        Opener op = ui.getOpener();
        int width = op.getWidth();
        int height = op.getHeight();
        short[] pixels = new short[width * height];
        op.setStackIndex(0);

        // Set processor.
        ij.process.ImageProcessor ip = new ij.process.ShortProcessor(width, height, pixels, null);
        Double massNumber = new Double(op.getMassNames()[index]);
        String title = "m" + massNumber + " : " + ui.getImageFilePrefix();
        setProcessor(title, ip);
        getProcessor().setMinAndMax(0, 65535);           
        fStateListeners = new EventListenerList() ;
    }
    
    // Use for segmented images.
    public MimsPlus(UI ui,int width, int height, int[] pixels, String name) {
        super();
        this.ui=ui;
        this.nType = SEG_IMAGE ;
           
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
   public MimsPlus(UI ui, SumProps sumProps, ArrayList<Integer> sumlist) {
      super();
      this.ui = ui;
      this.sumProps = sumProps;
      this.nType = SUM_IMAGE;

      // Setup image.
      Opener op = ui.getOpener();
      int width = op.getWidth();
      int height = op.getHeight();
      double sumPixels[] = new double[width*height];
      ImageProcessor ipp = new FloatProcessor(width, height, sumPixels);
      String title = "Sum : ";
      if (sumProps.getSumType() == SumProps.MASS_IMAGE)
         title += "m" + op.getMassNames()[sumProps.getParentMassIdx()] + " : " + op.getImageFile().getName();
      else if (sumProps.getSumType() == SumProps.RATIO_IMAGE)
         title += "m" + op.getMassNames()[sumProps.getNumMassIdx()] + "/m" + op.getMassNames()[sumProps.getDenMassIdx()] + " : " + op.getImageFile().getName();
      setProcessor(title, ipp);
      fStateListeners = new EventListenerList();
      
      // Setup sumlist.
      if (sumlist == null) {
         sumlist = new ArrayList<Integer>();
         for(int i = 1; i <= ui.getOpener().getNImages(); i++)
            sumlist.add(i);
      }
      
      // Compute pixels values.
      computeSum(sumlist);
   }

   // Use for ratio images.
   public MimsPlus(UI ui, RatioProps props) {
      super();
      this.ui = ui;
      this.ratioProps = props;
      this.nType = RATIO_IMAGE;

      // Setup image.
      Opener op = ui.getOpener();
      int width = op.getWidth();
      int height = op.getHeight();
      float[] pixels = new float[width * height];
      ImageProcessor ip = new FloatProcessor(width, height, pixels, null);
      ip.setMinAndMax(0, 1.0);
      String numName = ui.getOpener().getMassNames()[props.getNumMassIdx()];
      String denName = ui.getOpener().getMassNames()[props.getDenMassIdx()];
      title = "m" + numName + "/m" + denName + " : " + ui.getImageFilePrefix();
      setProcessor(title, ip);
      fStateListeners = new EventListenerList();

      // Compute pixel values.
      computeRatio();
    }
    
    // Use hsi images.
    public MimsPlus(UI ui, HSIProps props) {       
      super();
      this.ui = ui;
      this.hsiProps = props;
      this.nType = HSI_IMAGE;

      setupHSIImage(props);
    }

    public void setupHSIImage(HSIProps props) {

      // Set props incase changes
      hsiProps = props;

      // Setup image.
      Opener op = ui.getOpener();
      int width = op.getWidth();
      int height = op.getHeight();
      if(props.getLabelMethod() > 0) {
         height += 16;
      }
      int [] rgbPixels = new int[width*height];
      ImageProcessor ip = new ColorProcessor(width, height, rgbPixels);
      String numName = ui.getOpener().getMassNames()[props.getNumMassIdx()];
      String denName = ui.getOpener().getMassNames()[props.getDenMassIdx()];
      title = "HSI : m" + numName +"/m"+ denName + " : " + ui.getImageFilePrefix();
      setProcessor(title, ip);
      getProcessor().setMinAndMax(0, 255);
      fStateListeners = new EventListenerList() ;

      // Fill in pixel values.
      computeHSI();
    }

   public synchronized boolean computeHSI() {

      // Set up internal images for data display.
      internalRatio = new MimsPlus(ui, new RatioProps(hsiProps.getNumMassIdx(), hsiProps.getDenMassIdx()));
      internalNumerator = internalRatio.internalNumerator;
      internalDenominator = internalRatio.internalDenominator;
      setHSIProcessor(new HSIProcessor(this));
      try {
         getHSIProcessor().setProps(hsiProps);
      } catch (Exception e) {
         ui.updateStatus("Failed computing HSI image");
      }
      return true;
   }

    public synchronized void computeRatio() {

       // Get numerator and denominator mass indexes.
       int numIndex = ratioProps.getNumMassIdx();
       int denIndex = ratioProps.getDenMassIdx();

       if (numIndex > ui.getOpener().getNMasses() - 1 || denIndex > ui.getOpener().getNMasses())
          return;

        // Get the numerator and denominator mass images.
        MimsPlus parentNum = ui.getMassImage( numIndex );
        MimsPlus parentDen = ui.getMassImage( denIndex );

        // Setup list for sliding window, entire image, or single plane.
        java.util.ArrayList<Integer> list = new java.util.ArrayList<Integer>();        
        int currentplane = parentNum.getCurrentSlice();
        if (ui.getIsSum()) {
           for (int i = 1; i <= parentNum.getNSlices(); i++) {
              list.add(i);
           }
        } else if (ui.getIsWindow()) {
           int windowSize = ui.getWindowRange();
           int lb = currentplane - windowSize;
           int ub = currentplane + windowSize;
           for (int i = lb; i <= ub; i++) {
              list.add(i);
           }
        } else {
           list.add(currentplane);
        }

        // Compute the sum of the numerator and denominator mass images.
        SumProps numProps = new SumProps(numIndex);
        SumProps denProps = new SumProps(denIndex);
        internalNumerator = new MimsPlus(ui, numProps, list);
        internalDenominator = new MimsPlus(ui, denProps, list);

        // Fill in the data.
        float[] nPixels = (float[]) internalNumerator.getProcessor().getPixels();
        float[] dPixels = (float[]) internalDenominator.getProcessor().getPixels();
        float[] rPixels = new float[getWidth() * getHeight()];
        float rMax = 0.0f;
        float rMin = 1000000.0f;
        for (int i = 0; i < rPixels.length; i++) {
          rPixels[i] = ui.getRatioScaleFactor() * ((float) nPixels[i] / (float) dPixels[i]);
          if (rPixels[i] > rMax) {
             rMax = rPixels[i];
          } else if (rPixels[i] < rMin) {
             rMin = rPixels[i];
          }
       }

       // Set processor.
       ImageProcessor ip = new FloatProcessor(getWidth(), getHeight(), rPixels, null);
       ip.setMinAndMax(getProcessor().getMin(), getProcessor().getMax());
       setProcessor(title, ip);
    }

    public synchronized void computeSum(ArrayList<Integer> sumlist) {

       // initialize variables.
       double[] sumPixels = null;
       int parentIdx, numIdx, denIdx;
       MimsPlus parentImage, numImage, denImage;

       // Sum-mass image.
       if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
          parentIdx = sumProps.getParentMassIdx();
          parentImage = ui.getMassImage(parentIdx);

          int templength = parentImage.getProcessor().getPixelCount();
          sumPixels = new double[templength];
          short[] tempPixels = new short[templength];

          Object[] o = parentImage.getStack().getImageArray();
            for (int i = 0; i < sumlist.size(); i++) {
                if (sumlist.get(i) < 1 || sumlist.get(i) > ui.getOpener().getNImages()) continue;
                tempPixels = (short[])o[sumlist.get(i)-1];
                for (int j = 0; j < sumPixels.length; j++) {
                    sumPixels[j] += ((int) ( tempPixels[j] & 0xffff) );
                }
            }

       }
       // Sum-ratio image
       else if (sumProps.getSumType() == SumProps.RATIO_IMAGE) {
          numIdx = sumProps.getNumMassIdx();
          denIdx = sumProps.getDenMassIdx();

          SumProps nProps = new SumProps(numIdx);
          SumProps dProps = new SumProps(denIdx);

          UI tempui = ui;
          tempui.setMedianFilterRatios(false);

          numImage = new MimsPlus(tempui, nProps, sumlist);
          denImage = new MimsPlus(tempui, dProps, sumlist);
          internalNumerator = numImage;
          internalDenominator = denImage;

          float[] numPixels = (float[]) numImage.getProcessor().getPixels();
          float[] denPixels = (float[]) denImage.getProcessor().getPixels();
          sumPixels = new double[numImage.getProcessor().getPixelCount()];

          for (int i = 0; i < sumPixels.length; i++) {
             if (denPixels[i] != 0) {
                 sumPixels[i] = ui.getRatioScaleFactor() * (numPixels[i] / denPixels[i]);
             } else {
                 sumPixels[i] = 0;
             }
          }
       }

       // Set processor.
       ImageProcessor ip = new FloatProcessor(getWidth(), getHeight(), sumPixels);
       setProcessor(title, ip);

       // Do median filter if set to true.
       if (ui.getMedianFilterRatios()) {
          Roi temproi = getRoi();
          killRoi();
          RankFilters rfilter = new RankFilters();
          double r = ui.getMedianFilterRadius();
          rfilter.rank(getProcessor(), r, RankFilters.MEDIAN);
          rfilter = null;
          setRoi(temproi);
       }
    }

    public void showWindow(){

       show();

       // Set window location.
       if (xloc > -1 & yloc > -1)
          getWindow().setLocation(xloc, yloc);

       // Add image to list og images in UI.
       ui.addToImagesList(this);

       // Autocontrast image by default.
       ui.autoContrastImage(this);
    }


   @Override
    public int getWidth() {
      if (getProcessor() != null)
         return getProcessor().getWidth();
      else
         return ui.getOpener().getWidth();
    }

   @Override
    public int getHeight() {
      if (getProcessor() != null)
         return getProcessor().getHeight();
      else
         return ui.getOpener().getHeight();
    }
    
    //not hit from MimsStackEditing.concatImages()
    //only used when opening a multiplane image
    public void appendImage(int nImage) throws Exception {
        if(ui.getOpener() == null) {
            throw new Exception("No image opened?");
        }
        if(nImage >= ui.getOpener().getNImages()) {
            throw new Exception("Out of Range");
        }
        ij.ImageStack stack = getStack();
        ui.getOpener().setStackIndex(nImage);
        stack.addSlice(null,ui.getOpener().getPixels(massIndex));
        setStack(null,stack);
        setSlice(nImage+1);
        //setProperty("Info", srcImage.getInfo());
        bIgnoreClose = true ;
        bIsStack = true ;       
    }

    @Override
    public String getShortTitle() {
        String tempstring = this.getTitle();
        int colonindex = tempstring.indexOf(":");
        if(colonindex>0) return tempstring.substring(0, colonindex-1);
        else return "";
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

    public boolean equals(MimsPlus mp){

       if (mp.getMimsType() != getMimsType())
          return false;

       if (mp.getMimsType() == MASS_IMAGE){
          if (mp.getMassIndex() == getMassIndex())
             return true;
       } else if (mp.getMimsType() == RATIO_IMAGE) {
          if (mp.getRatioProps().equals(getRatioProps())){
             return true;
          }
       } else if (mp.getMimsType() == SUM_IMAGE) {
          if (mp.getSumProps().equals(getSumProps())) {
             return true;
          }
       } else if (mp.getMimsType() == HSI_IMAGE) {
          if (mp.getHSIProps().equals(getHSIProps())) {
             return true;
          }
       } else if (mp.getMimsType() == SEG_IMAGE) {
          // TODO: Not sure what to add here
       }

       return false;
    }
    
    @Override
    public void setRoi(ij.gui.Roi roi) {
        if(roi == null) super.killRoi();
        else super.setRoi(roi);
        stateChanged(roi,MimsPlusEvent.ATTR_SET_ROI);
    }

    @Override
    public void windowClosing(WindowEvent e) {
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
        ui.imageClosed(this);
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
        else if(this.nType == RATIO_IMAGE) {
            int n = getRatioProps().getNumMassIdx();
            int d = getRatioProps().getDenMassIdx();
            MimsPlus [] ml = ui.getMassImages() ;
            if(ml.length > n && ml.length > d && ml[n] != null && ml[d] != null ) {
                int [] ngl = ml[n].getPixel(mX,mY);
                int [] dgl = ml[d].getPixel(mX,mY);
                float r = this.getProcessor().getPixelValue(mX, mY);
                msg += "S (" + ngl[0] + " / " + dgl[0] + ") = " + IJ.d2s(r,4);
            }         
        }
        else if(this.nType == SUM_IMAGE) {
            float ngl, dgl;
            if (internalNumerator != null && internalDenominator != null) {
               ngl = internalNumerator.getProcessor().getPixelValue(mX, mY);
               dgl = internalDenominator.getProcessor().getPixelValue(mX, mY);
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
                int neum = getHSIProps().getNumMassIdx();
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
            this.hide();
            ui.massImageClosed(this);
        }
    }
    public void setHSIProcessor( HSIProcessor processor ) { this.hsiProcessor = processor ; }
    public HSIProcessor getHSIProcessor() { return hsiProcessor ; }
    
    public void setAutoContrastAdjust( boolean auto ) { this.autoAdjustContrast = auto ; }
    public boolean getAutoContrastAdjust() { return autoAdjustContrast ; }
    
    public boolean isStack() { return bIsStack ; }
    public void setIsStack(boolean isS) { bIsStack = isS; }

    public int getMassIndex() { return massIndex; }
    public int getMimsType() { return nType ; }
    public SumProps getSumProps() { return sumProps; }
    public RatioProps getRatioProps() { return ratioProps; }
    public HSIProps getHSIProps() { return getHSIProcessor().getHSIProps(); }
    public UI getUI() { return ui ; }

}

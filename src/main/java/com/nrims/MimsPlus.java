package com.nrims;

import com.nrims.data.ImageDataUtilities;
import com.nrims.data.Opener;
import com.nrims.unoplugin.UnoPlugin;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.filter.RankFilters;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import java.awt.Dimension;

import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import javax.swing.DefaultListModel;
import javax.swing.event.EventListenerList;

/**
 * Extends ImagePlus with methods to synchronize display of multiple stacks and drawing ROIs in each windows
 */
public class MimsPlus extends ImagePlus implements WindowListener, MouseListener,
        MouseMotionListener, MouseWheelListener {

    /* Public constants */
    static final public int MASS_IMAGE = 0;
    static final public int RATIO_IMAGE = 1;
    static final public int HSI_IMAGE = 2;
    static final public int SEG_IMAGE = 3;
    static final public int SUM_IMAGE = 4;
    static final public int COMPOSITE_IMAGE = 5;
    static final public int NON_MIMS_IMAGE = 6;  // e.g., tiff
    static final public int X_OFFSET = 5; // originally 15
    static final public int Y_OFFSET = 40;
    // Internal images for test data display.
    public MimsPlus internalRatio_filtered;
    public MimsPlus internalRatio;
    public MimsPlus internalNumerator;
    public MimsPlus internalDenominator;
    // Window position.
    private int xloc = -1;
    private int yloc = -1;
    private double mag = 1.0;
    // Props objects.
    public MassProps massProps = null;
    public SumProps sumProps = null;
    public RatioProps ratioProps = null;
    public HSIProps hsiProps = null;
    public CompositeProps compProps = null;
    // Lut
    public String lut = "Grays";
    // Other member variables.
    public String title = "";
    public String libreTitle = "";
    private boolean allowClose = true;
    private boolean bIgnoreClose = false;
    boolean bMoving = false;
    boolean autoAdjustContrast = false;
    static boolean bStateChanging = false;
    private int massIndex = 0;
    private double massValue = -999.0;
    private int nType = 0;
    private int x1, x2, y1, y2, w1, w2, h1, h2;
    private boolean bIsStack = false;
    private HSIProcessor hsiProcessor = null;
    private CompositeProcessor compProcessor = null;
    private com.nrims.UI ui = null;
    private EventListenerList fStateListeners = null;
    private Overlay graphOverlay = new Overlay();
    //Mouse coordinates for use with OpenMIMS too
    private int startX;
    private int startY;
    //These two hold after a mousepress
    private int pressX;
    private int pressY;
    private UnoPlugin mimsUno;

    /**
     * Generic constructor
     *
     * @param ui user interface to be used in the MimsPlus object
     */
    public MimsPlus(UI ui) {
        super();
        this.ui = ui;
        fStateListeners = new EventListenerList();
        setupDragDrop();
    }
    
    /**
     * Constructor to use for non-MIMS images.
     *
     * @param ui user interface to be used in the MimsPlus object
     * @param imageType x
     * @param ipp an <code>ImageProcessor</code> instance
     */
    public MimsPlus(UI ui, int imageType, ImageProcessor ipp) {
        super();
        this.ui = ui;
        this.nType = NON_MIMS_IMAGE;
        setProcessor(title, ipp);
        
        
        fStateListeners = new EventListenerList();
        setupDragDrop();
    }

    /**
     * Constructor to use for mass images.
     *
     * @param ui a <code>UI</code> instance
     * @param index mass index
     */
    public MimsPlus(UI ui, int index) {
        super();
        this.ui = ui;
        this.massIndex = index;

        this.nType = MASS_IMAGE;

        // Get a copy of the opener and setup image parameters.
        Opener op = ui.getOpener();
        int w = op.getWidth();
        int h = op.getHeight();
        op.setStackIndex(0);
        this.massValue = Double.parseDouble(op.getMassNames()[index]);

        // Set processor.
        ij.process.ImageProcessor ipp = null;
        if (op.getFileType() == FileInfo.GRAY16_UNSIGNED) {
            short[] pixels = new short[w * h];
            ipp = new ij.process.ShortProcessor(w, h, pixels, null);
        } else if (op.getFileType() == FileInfo.GRAY32_FLOAT) {
            float[] pixels = new float[w * h];
            ipp = new ij.process.FloatProcessor(w, h, pixels, null);
        } else if (op.getFileType() == FileInfo.GRAY32_UNSIGNED) {
            int[] pixels = new int[w * h];
            ipp = new ij.process.FloatProcessor(w, h, pixels);
        }
        //String titleString = "m" + ui.getTitleStringSymbol(index) + ": " + ui.getImageFilePrefix();
        title = ImageDataUtilities.formatTitle(index, true, ui.getPreferences().getFormatString(),
                ui.getOpener());
        libreTitle = ImageDataUtilities.formatLibreTitle(index, ui.getOpener(), this);
        setProcessor(title, ipp);

        fStateListeners = new EventListenerList();
        setupDragDrop();
    }
    
    public void setLibreTitle(String lt) {
        libreTitle = lt;
    }
    
    public String getLibreTitle() {
        return libreTitle;
    }

    /**
     * Constructor to use for segmented images.
     *
     * @param ui a <code>UI</code> instance
     * @param width image width
     * @param height image height
     * @param pixels an integer array of pixels
     * @param name name
     */
    public MimsPlus(UI ui, int width, int height, int[] pixels, String name) {
        super();
        this.ui = ui;
        this.nType = SEG_IMAGE;

        try {
            ij.process.ImageProcessor ipp = new ij.process.ColorProcessor(
                    width,
                    height,
                    pixels);

            setProcessor(name, ipp);
            fStateListeners = new EventListenerList();
        } catch (Exception x) {
            IJ.log(x.toString());
        }
        setupDragDrop();
    }

    /**
     * Constructor for sum images.
     *
     * @param ui a <code>UI</code> instance
     * @param sumProps a <code>SumProps</code> instance
     * @param sumlist a list of Integer objects
     */
    //public MimsPlus(UI ui, SumProps sumProps, ArrayList<Integer> sumlist) {
    public MimsPlus(UI ui, SumProps sumProps, List<Integer> sumlist) {
        super();
        this.ui = ui;
        this.sumProps = sumProps;
        this.nType = SUM_IMAGE;
        this.xloc = sumProps.getXWindowLocation();
        this.yloc = sumProps.getYWindowLocation();
        this.mag = sumProps.getMag();

        // Setup image.
        Opener op = ui.getOpener();
        int w = op.getWidth();
        int h = op.getHeight();
        double sumPixels[] = new double[w * h];
        ImageProcessor ipp = new FloatProcessor(w, h, sumPixels);
        title = "Sum : ";
        libreTitle = "";
        if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
            if (sumProps.getParentMassIdx() == ui.getOpenMassImages().length) {
                title += "1";
                libreTitle += "1";
            } else {
                title += ImageDataUtilities.formatTitle(sumProps.getParentMassIdx(), false,
                        ui.getPreferences().getFormatString(), ui.getOpener());
                libreTitle += ImageDataUtilities.formatLibreTitle(sumProps.getParentMassIdx(), ui.getOpener(), this);
            }
        } else if (sumProps.getSumType() == SumProps.RATIO_IMAGE) {
            title += ImageDataUtilities.formatTitle(sumProps.getNumMassIdx(), sumProps.getDenMassIdx(),
                    false, ui.getPreferences().getFormatString(), ui.getOpener());
            libreTitle += ImageDataUtilities.formatLibreTitle(sumProps.getNumMassIdx(),
                    sumProps.getDenMassIdx(), ui.getOpener(), this);
        }
        setProcessor(title, ipp);
        fStateListeners = new EventListenerList();
        //before listeners were added in compute...
        addListener(ui);

        // Setup sumlist.
        if (sumlist == null) {
            sumlist = new ArrayList<Integer>();
            for (int i = 1; i <= ui.getMimsAction().getSize(); i++) {
                sumlist.add(i);
            }
        }

        // Compute pixels values.
        computeSum(sumlist);
        setupDragDrop();
    }

    /**
     * Constructor for ratio images.
     *
     * @param ui a <code>UI</code> instance
     * @param props a <code>RatioProps</code> object
     */
    public MimsPlus(UI ui, RatioProps props) {
        this(ui, props, false);
        setupDragDrop();
    }

    /**
     * Constructor for ratio images.
     *
     * @param ui a <code>UI</code> instance
     * @param props a <code>RatioProps</code> object
     * @param forInternalRatio boolean
     */
    public MimsPlus(UI ui, RatioProps props, boolean forInternalRatio) {
        super();
        this.ui = ui;
        this.ratioProps = props;
        this.nType = RATIO_IMAGE;
        this.xloc = props.getXWindowLocation();
        this.yloc = props.getYWindowLocation();
        this.mag = props.getMag();

        // Setup image.
        Opener op = ui.getOpener();
        int w = op.getWidth();
        int h = op.getHeight();
        float[] pixels = new float[w * h];
        ImageProcessor ipp = new FloatProcessor(w, h, pixels, null);
        ipp.setMinAndMax(0, 1.0);
        String numName, denName;
        title += ImageDataUtilities.formatTitle(ratioProps.getNumMassIdx(), ratioProps.getDenMassIdx(),
                false, ui.getPreferences().getFormatString(), ui.getOpener());
        libreTitle += ImageDataUtilities.formatLibreTitle(ratioProps.getNumMassIdx(),
                ratioProps.getDenMassIdx(), ui.getOpener(), this);
        setProcessor(title, ipp);
        fStateListeners = new EventListenerList();
        //before listeners were added in compute...
        addListener(ui);

        // Compute pixel values.
        computeRatio(forInternalRatio);
        setupDragDrop();
    }

    /**
     * Constructor for composite images.
     *
     * @param ui a <code>UI</code> instance
     * @param compprops a <code>CompositeProps</code> instance
     */
    public MimsPlus(UI ui, CompositeProps compprops) {
        super();
        this.ui = ui;
        this.compProps = compprops;
        this.xloc = compprops.getXWindowLocation(); // DJ
        this.yloc = compprops.getYWindowLocation(); // DJ 
        this.nType = MimsPlus.COMPOSITE_IMAGE;
        fStateListeners = new EventListenerList();
        addListener(ui);
        setupCompositeImage(compprops);
        setupDragDrop();
    }

    /**
     * Constructor for HSI images.
     *
     * @param ui a <code>UI</code> instance
     * @param props an <code>HSIProps</code> instance
     */
    public MimsPlus(UI ui, HSIProps props) {
        super();
        this.ui = ui;
        this.hsiProps = props;
        this.nType = HSI_IMAGE;
        this.xloc = props.getXWindowLocation();
        this.yloc = props.getYWindowLocation();
        this.mag = props.getMag();

        setupHSIImage(props);
        setupDragDrop();
    }
    
     /**
     * Sets the title.  This is not the same title as the superclass keeps.
     *
     * @param aTitle a title for this image
     */
    public void setLocalTitle(String aTitle) {
        this.title = aTitle;
    }
    
    

    public void setupDragDrop() {
        mimsUno = UnoPlugin.getInstance();
        if (mimsUno == null) {
            mimsUno = new UnoPlugin(true);
        }
        mimsUno.addMimsImage(this);
    }

    /**
     * Initialization for composite image graphics.
     *
     * @param compprops a <code>CompositeProps</code> object
     */
    public void setupCompositeImage(CompositeProps compprops) {
        compProps = compprops;
        MimsPlus[] imgs = compprops.getImages(ui);

        Opener op = ui.getOpener();
        int w = op.getWidth();
        int h = op.getHeight();
        int[] rgbPixels = new int[w * h];
        ImageProcessor ip = new ColorProcessor(w, h, rgbPixels);
        title = ui.getImageFilePrefix();
        for (int i = 0; i < imgs.length; i++) {
            if (imgs[i] != null) {
                title += "_" + imgs[i].getRoundedTitle().replace(" ", "-");
            } else {
                title += "_n";
            }
        }
        title += "_comp";
        setProcessor(title, ip);

        //fill in pixels
        computeComposite();
    }

    /**
     * Composite image readiness status
     *
     * @return status (true for success, false for failure)
     */
    public synchronized boolean computeComposite() {
        setCompositeProcessor(new CompositeProcessor(this, ui));
        try {
            getCompositeProcessor().setProps(compProps);
        } catch (Exception e) {
            ui.updateStatus("Failed computing Composite image");
        }
        return true;
    }

    /**
     * Sets up HSI image
     *
     * @param props HSI properties to use
     */
    public void setupHSIImage(HSIProps props) {

        // Set props incase changes
        hsiProps = props;

        // Setup image.
        Opener op = ui.getOpener();
        int w = op.getWidth();
        int h = op.getHeight();
        if (props.getLabelMethod() > 0) {
            h += 16;
        }
        int[] rgbPixels = new int[w * h];
        String numName;
        String denName;
        ImageProcessor ipp = new ColorProcessor(w, h, rgbPixels);
        title = "HSI : " + ImageDataUtilities.formatTitle(hsiProps.getNumMassIdx(), hsiProps.getDenMassIdx(),
                false, ui.getPreferences().getFormatString(), ui.getOpener());
        libreTitle = ImageDataUtilities.formatLibreTitle(hsiProps.getNumMassIdx(), hsiProps.getDenMassIdx(),
                ui.getOpener(), this);
        setProcessor(title, ipp);
        getProcessor().setMinAndMax(0, 255);
        fStateListeners = new EventListenerList();
        addListener(ui);

        // Fill in pixel values.
        computeHSI();
    }

    /**
     * Compute HSI image; return true for success
     *
     * @return true for success, false for failure
     */
    public synchronized boolean computeHSI() {

        // Set up internal images for data display.
        RatioProps rProps = new RatioProps(hsiProps.getNumMassIdx(), hsiProps.getDenMassIdx());
        rProps.setRatioScaleFactor(hsiProps.getRatioScaleFactor());
        rProps.setNumThreshold(hsiProps.getNumThreshold());
        rProps.setDenThreshold(hsiProps.getDenThreshold());
        MimsPlus mp_filtered = new MimsPlus(ui, rProps, false);
        MimsPlus mp_raw = new MimsPlus(ui, rProps, true);
        internalRatio = mp_raw;
        internalNumerator = internalRatio.internalNumerator;
        internalDenominator = internalRatio.internalDenominator;
        internalRatio_filtered = mp_filtered;
        setHSIProcessor(new HSIProcessor(this));
        try {
            getHSIProcessor().setProps(hsiProps);
        } catch (Exception e) {
            ui.updateStatus("Failed computing HSI image");
        }
        return true;
    }

    /**
     * Computes ratios values.
     */
    public synchronized void computeRatio() {
        computeRatio(false);
    }

    /**
     * Computes ratios values.
     *
     * @param forHSI
     */
    private synchronized void computeRatio(boolean forInternalRatio) {

        // Get numerator and denominator mass indexes.
        int numIndex = ratioProps.getNumMassIdx();
        int denIndex = ratioProps.getDenMassIdx();

        if (numIndex > ui.getOpener().getNMasses() - 1 || denIndex > ui.getOpener().getNMasses()) {
            return;
        }

        // Get the numerator and denominator mass images.
        MimsPlus parentNum = ui.getMassImage(numIndex);
        MimsPlus parentDen = ui.getMassImage(denIndex);

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
        float rSF = ui.getHSIView().getRatioScaleFactor();
        int numThreshold = ratioProps.getNumThreshold();
        int denThreshold = ratioProps.getDenThreshold();

        if (this.ratioProps.getRatioScaleFactor() > 0) {
            rSF = ((Double) this.ratioProps.getRatioScaleFactor()).floatValue();
        }

        // If we are dealing with a ratio image whose numerator or denominator
        // is "1", we DO NOT need to multiply the ratio by the ratioscalefactor.
        if (denIndex == ui.getOpenMassImages().length) {
            rSF = (float) 1.0;
        }

        for (int i = 0; i < rPixels.length; i++) {
            if (dPixels[i] != 0 && nPixels[i] > numThreshold && dPixels[i] > denThreshold) {
                rPixels[i] = rSF * ((float) nPixels[i] / (float) dPixels[i]);
            } else {
                rPixels[i] = 0;
            }
            if (rPixels[i] > rMax) {
                rMax = rPixels[i];
            } else if (rPixels[i] < rMin) {
                rMin = rPixels[i];
            }
        }

        if (ui.getIsPercentTurnover()) {
            float reference = ui.getPreferences().getReferenceRatio();
            float background = ui.getPreferences().getBackgroundRatio();
            rPixels = HSIProcessor.turnoverTransform(rPixels, reference, background,
                    (float) (ratioProps.getRatioScaleFactor()));
        }

        // Set processor.
        ImageProcessor ipp = new FloatProcessor(getWidth(), getHeight(), rPixels, getProcessor().getColorModel());
        ipp.setMinAndMax(ratioProps.getMinLUT(), ratioProps.getMaxLUT());
        if (forInternalRatio) {
            internalRatio = null;
        } else {
            MimsPlus mp_raw = new MimsPlus(ui, ratioProps, true);
            internalRatio = mp_raw;
        }

        // Do median filter if set to true.
        if (ui.getMedianFilterRatios() && !forInternalRatio) {
            Roi temproi = getRoi();
            killRoi();
            RankFilters rfilter = new RankFilters();
            double r = ui.getHSIView().getMedianRadius();
            rfilter.rank(ipp, r, RankFilters.MEDIAN);
            rfilter = null;
            setRoi(temproi);
        }
        setProcessor(title, ipp);
    }

    /**
     * Computes sum values.
     *
     * @param sumlist a list of Integer objects
     */
    //public synchronized void computeSum(ArrayList<Integer> sumlist) {
    public synchronized void computeSum(List<Integer> sumlist) {


        // initialize variables.
        double[] sumPixels = null;
        int parentIdx, numIdx, denIdx;
        MimsPlus parentImage, numImage, denImage;

        // Sum-mass image.
        if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
            parentIdx = sumProps.getParentMassIdx();
            parentImage = ui.getMassImage(parentIdx);
            if (parentIdx == ui.getOpenMassImages().length) {
                int w = getWidth();
                int h = getHeight();
                int area = w * h;
                sumPixels = new double[area];
                for (int i = 0; i < area; i++) {
                    sumPixels[i] = 1;
                }
            } else {
                int templength = parentImage.getProcessor().getPixelCount();
                sumPixels = new double[templength];

                Object[] o = parentImage.getStack().getImageArray();
                int numSlices = parentImage.getNSlices();
                if (o[0] instanceof float[]) {
                    float[] tempPixels = new float[templength];
                    for (int i = 0; i < sumlist.size(); i++) {
                        if (sumlist.get(i) < 1 || sumlist.get(i) > numSlices) {
                            continue;
                        }
                        tempPixels = (float[]) o[sumlist.get(i) - 1];
                        for (int j = 0; j < sumPixels.length; j++) {
                            sumPixels[j] += ((int) (tempPixels[j]));
                        }
                    }
                } else if (o[0] instanceof short[]) {
                    short[] tempPixels = new short[templength];
                    for (int i = 0; i < sumlist.size(); i++) {
                        if (sumlist.get(i) < 1 || sumlist.get(i) > numSlices) {
                            continue;
                        }
                        tempPixels = (short[]) o[sumlist.get(i) - 1];
                        for (int j = 0; j < sumPixels.length; j++) {
                            sumPixels[j] += ((int) (tempPixels[j] & 0xffff));
                        }
                    }
                }
            }
        } // Sum-ratio image
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

            float rSF = ui.getHSIView().getRatioScaleFactor();
            if (this.sumProps.getRatioScaleFactor() > 0) {
                rSF = ((Double) this.sumProps.getRatioScaleFactor()).floatValue();
            }
            if (denIdx == ui.getOpenMassImages().length) {
                rSF = (float) 1.0;
            }
            for (int i = 0; i < sumPixels.length; i++) {
                if (denPixels[i] != 0) {
                    sumPixels[i] = rSF * (numPixels[i] / denPixels[i]);
                } else {
                    sumPixels[i] = 0;
                }
            }
        }

        // Set processor.
        ImageProcessor ip = new FloatProcessor(getWidth(), getHeight(), sumPixels);
        setProcessor(title, ip);

    }

    public void showWindow() {
        showWindow(true);
    }

    /**
     * Shows the current window.
     * 
     * @param forceAutoContrast a boolean that determines whether or not to autocontrast the image.
     */
    public void showWindow(boolean forceAutoContrast) {

        show();

        // Set window location.
        if (xloc > -1 & yloc > -1) {
            getWindow().setLocation(xloc, yloc);
        }

        // Add image to list og images in UI.
        ui.addToImagesList(this);

        // Autocontrast image by default.
        if ((this.getMimsType() == MimsPlus.MASS_IMAGE) || (this.getMimsType() == MimsPlus.RATIO_IMAGE)
                || (this.getMimsType() == MimsPlus.SUM_IMAGE)) {
            if (forceAutoContrast) {
                ui.autoContrastImage(this);

            }
        }

        this.restoreMag();

    }

    /**
     * Restores previous magnification.
     */
    public void restoreMag() {
        if (this.getCanvas() == null) {
            return;
        }

        double z = this.getCanvas().getMagnification();

        if (this.getCanvas().getMagnification() < mag) {
            while (this.getCanvas().getMagnification() < mag) {
                this.getCanvas().zoomIn(0, 0);
            }
        }

        if (this.getCanvas().getMagnification() > mag) {
            while (this.getCanvas().getMagnification() > mag) {
                this.getCanvas().zoomOut(0, 0);
            }
        }

    }

    /**
     * Returns the width of the image in pixels.
     *
     * @return the width in pixels.
     */
    @Override
    public int getWidth() {
        if (getProcessor() != null) {
            return getProcessor().getWidth();
        } else {
            return ui.getOpener().getWidth();
        }
    }

    /**
     * Returns the height of the image in pixels.
     *
     * @return the height in pixels.
     */
    @Override
    public int getHeight() {
        if (getProcessor() != null) {
            return getProcessor().getHeight();
        } else {
            return ui.getOpener().getHeight();
        }
    }

    /**
     * Appends image to stack. Not hit from MimsStackEditing.concatImages(). Only used when opening a multiplane image
     * file.
     *
     * @param nImage number of images
     * @throws Exception an exception thrown if no image was opened or the number of images was out of range
     */
    public void appendImage(int nImage) throws Exception {
        if (ui.getOpener() == null) {
            throw new Exception("No image opened?");
        }
        if (nImage >= ui.getOpener().getNImages()) {
            throw new Exception("Out of Range");
        }
        ij.ImageStack stack = getStack();
        ui.getOpener().setStackIndex(nImage);
        stack.addSlice(null, ui.getOpener().getPixels(massIndex));
        setStack(null, stack);
        setSlice(nImage + 1);
        //setProperty("Info", srcImage.getInfo());
        bIgnoreClose = true;
        bIsStack = true;
    }

    /**
     * This method is required for those instances where the header was incorrect or the file did 
     * not contain all of the data. For example, lets say the file should contain 40 planes (for 4 
     * masses) but the last plane of the last two masses is missing. The plugin requires that all 
     * masses contain the same number of planes, so in this case we want to append two blank images 
     * to the end of the last two masses, thereby ensuring all masses have the same number of
     * planes.
     * 
     * @param nImage number of images
     */
    public void appendBlankImage(int nImage) {

        ij.process.ImageProcessor ipp = null;
        Opener op = ui.getOpener();
        if (op.getFileType() == FileInfo.GRAY16_UNSIGNED) {
            short[] pixels = new short[getWidth() * getHeight()];
            ipp = new ij.process.ShortProcessor(getWidth(), getHeight(), pixels, null);
        } else if (op.getFileType() == FileInfo.GRAY32_FLOAT) {
            float[] pixels = new float[getWidth() * getHeight()];
            ipp = new ij.process.FloatProcessor(getWidth(), getHeight(), pixels, null);
        } else if (op.getFileType() == FileInfo.GRAY32_UNSIGNED) {
            int[] pixels = new int[getWidth() * getHeight()];
            ipp = new ij.process.FloatProcessor(getWidth(), getHeight(), pixels);
        }
        ij.ImageStack stack = getStack();
        stack.addSlice(null, ipp);
        setStack(null, stack);
        setSlice(nImage + 1);
        bIgnoreClose = true;
        bIsStack = true;
    }

    /**
     * Returns a short title in the form "m13.01" or "m26.05".
     *
     * @return The title (e.g. "m13.01").
     */
    @Override
    public String getShortTitle() {
        String tempstring = this.getTitle();
        int colonindex = tempstring.indexOf(":");
        if (getMimsType() == SUM_IMAGE) {
            colonindex = tempstring.indexOf(":", colonindex + 1);
        }
        if (colonindex > 0) {
            return tempstring.substring(0, colonindex - 1);
        } else {
            return "";
        }
    }

    /**
     * Similar to {@link #getShortTitle() getShortTitle} but uses the rounded mass and excludes the "m".
     *
     * @return Text string containing the rounded mass value. For example:
     * <ul>
     * <li>"13" for Mass images.
     * <li>"ratio 13/12" for Ratio images.
     * <li>"hsi 13/12" for HSI images.
     * <li>"sum 13" for Sum images.
     * <li>"0" by default.
     * </ul>
     */
    public String getRoundedTitle() {
        return getRoundedTitle(false);
    }

    /**
     * Similar to {@link #getShortTitle() getShortTitle} but uses the rounded mass and excludes the "m".
     *
     * @return Text string containing the rounded mass value. For example:
     * <ul>
     * <li>"13" for Mass images.
     * <li>"ratio 13/12" for Ratio images.
     * <li>"hsi 13/12" for HSI images.
     * <li>"sum 13" for Sum images.
     * <li>"0" by default.
     * </ul>
     * 
     * @param forTableOrPlot a boolean that determines the form of the <code>roundedTitle</code>
     */
    public String getRoundedTitle(boolean forTableOrPlot) {

        String roundedTitle = "";
        try {
            if (this.getMimsType() == MimsPlus.MASS_IMAGE) {
                int mass_idx = getMassIndex();
                double mass_d = ui.getMassValue(mass_idx);
                long num_l = java.lang.Math.round(mass_d);
                roundedTitle = Long.toString(num_l);
            }
            if (this.getMimsType() == MimsPlus.HSI_IMAGE) {
                HSIProps hprops = getHSIProps();
                double num_d = ui.getMassValue(hprops.getNumMassIdx());
                double den_d = ui.getMassValue(hprops.getDenMassIdx());
                long num_l = java.lang.Math.round(num_d);
                long den_l = java.lang.Math.round(den_d);
                roundedTitle = "HSI " + Long.toString(num_l) + "/" + Long.toString(den_l);
            }
            if (this.getMimsType() == MimsPlus.RATIO_IMAGE) {
                RatioProps rprops = getRatioProps();
                double num_d = ui.getMassValue(rprops.getNumMassIdx());
                double den_d = ui.getMassValue(rprops.getDenMassIdx());
                long num_l = java.lang.Math.round(num_d);
                long den_l = java.lang.Math.round(den_d);
                roundedTitle = "ratio " + Long.toString(num_l) + "/" + Long.toString(den_l);
            }
            if (this.getMimsType() == MimsPlus.SUM_IMAGE) {
                SumProps props = this.getSumProps();
                if (props.getSumType() == SumProps.MASS_IMAGE) {
                    int mass_idx = props.getParentMassIdx();
                    if (forTableOrPlot) {
                        roundedTitle = "sm" + ui.getMassImage(mass_idx).getRoundedTitle();
                    } else {
                        roundedTitle = "sum " + ui.getMassImage(mass_idx).getRoundedTitle();
                    }
                } else if (props.getSumType() == SumProps.RATIO_IMAGE) {
                    double num_d = ui.getMassValue(props.getNumMassIdx());
                    double den_d = ui.getMassValue(props.getDenMassIdx());
                    long num_l = java.lang.Math.round(num_d);
                    long den_l = java.lang.Math.round(den_d);
                    if (forTableOrPlot) {
                        roundedTitle = " sm" + Long.toString(num_l) + "/" + Long.toString(den_l);
                    } else {
                        roundedTitle = "sum " + Long.toString(num_l) + "/" + Long.toString(den_l);
                    }
                }
            }
        } catch (Exception e) {
            roundedTitle = "0";
        }

        return roundedTitle;
    }

    //DJ: 11/14/2014
    public String getTitleForTables() {
        return this.title.substring(this.title.indexOf('[') + 1, this.title.indexOf(']'));
    }

    /**
     * Returns extended title information with properties Mass: contrast range "(min - max)" + 
     * "slice number" HSI: Ratio range "(min - max)" Ratio: "(min - max)"
     *
     * @return propTitle the formatted property title
     */
    public String getPropsTitle() {
        //move elsewhere?
        DecimalFormat formatter = new DecimalFormat("0.0");

        String propTitle = "";
        try {
            if (this.getMimsType() == MimsPlus.MASS_IMAGE) {
                double max = this.getDisplayRangeMax();
                double min = this.getDisplayRangeMin();
                propTitle += " (" + min + " - " + max + ") " + this.getCurrentSlice() + "/" + this.getStackSize();
            } else if (this.getMimsType() == MimsPlus.HSI_IMAGE) {
                HSIProps hprops = getHSIProps();
                double max = hprops.getMaxRatio();
                double min = hprops.getMinRatio();
                propTitle += " (" + min + " - " + max + ")";
                //TODO?
                //need if not summed plane #s
            } else if (this.getMimsType() == MimsPlus.RATIO_IMAGE) {
                double max = this.getDisplayRangeMax();
                double min = this.getDisplayRangeMin();
                propTitle += " (" + formatter.format((Number) min) + " - " + formatter.format((Number) max) + ")";
                //TODO?
                //need if not summed plane #s
            } else if (this.getMimsType() == MimsPlus.SUM_IMAGE) {
                double max = this.getDisplayRangeMax();
                double min = this.getDisplayRangeMin();
                propTitle += " (" + min + " - " + max + ")";
            }
        } catch (Exception e) {
            propTitle = "0";
        }

        return propTitle;
    }

    /**
     * Returns numerator image if such exists.
     *
     * @return numerator image
     */
    public MimsPlus getNumeratorImage() {
        if (this.getMimsType() == MimsPlus.RATIO_IMAGE) {
            return this.ui.getMassImage(this.getRatioProps().getNumMassIdx());
        } else if (this.getMimsType() == MimsPlus.HSI_IMAGE) {
            return this.ui.getMassImage(this.getHSIProps().getNumMassIdx());
        } else {
            return null;
        }
    }

    /**
     * Returns denominator image if such exists.
     *
     * @return denominator image
     */
    public MimsPlus getDenominatorImage() {
        if (this.getMimsType() == MimsPlus.RATIO_IMAGE) {
            return this.ui.getMassImage(this.getRatioProps().getDenMassIdx());
        } else if (this.getMimsType() == MimsPlus.HSI_IMAGE) {
            return this.ui.getMassImage(this.getHSIProps().getDenMassIdx());
        } else {
            return null;
        }
    }

    /**
     * Sets the "ignore close" flag to the value of b
     *
     * @param b "ignore close" value
     */
    public void setbIgnoreClose(boolean b) {
        this.bIgnoreClose = b;
    }

    /**
     * Gets current location
     *
     * @return the Point object containing the current location
     */
    public java.awt.Point getXYLoc() {
        return new java.awt.Point(this.xloc, this.yloc);
    }

    /**
     * Sets current window location
     * 
     * @param p points at which to set the current window location
     */
    public void setXYLoc(java.awt.Point p) {
        this.xloc = p.x;
        this.yloc = p.y;
    }

    /**
     * Shows the window and add various mouse mouse listeners.
     */
    @Override
    public void show() {
        try {
            if (getStackSize() > 1) {
                // DJ: 08/06/2014
                StackWindow sw = new StackWindow(this, new MimsCanvas(this, ui));

                sw.addWindowListener(this);
                sw.getCanvas().addMouseListener(this);
                sw.getCanvas().addMouseMotionListener(this);
                sw.getCanvas().addMouseWheelListener(this);
                sw.setVisible(true);

            } else {
                // DJ: 08/06/2014
                ImageWindow iw = new ImageWindow(this, new MimsCanvas(this, ui));

                iw.addWindowListener(this);
                iw.getCanvas().addMouseListener(this);
                iw.getCanvas().addMouseMotionListener(this);
                iw.getCanvas().addMouseWheelListener(this);
                iw.setVisible(true);
            }

        } catch (Exception e) {
            System.out.println("DJ- Exception detected at MimsPlus.show() :  " + e);
        } finally {
            this.unlock();
        }

    }

    /**
     * Compares the input MimsPlus object (mp) to the current class.
     *
     * @param mp a <code>MimsPlus</code> object
     * @return true if a match, false otherwise
     */
    public boolean equals(MimsPlus mp) {

        if (mp.getMimsType() != getMimsType()) {
            return false;
        }

        if (mp.getMimsType() == MASS_IMAGE) {
            if (mp.getMassIndex() == getMassIndex()) {
                return true;
            }
        } else if (mp.getMimsType() == RATIO_IMAGE) {
            if (mp.getRatioProps().equals(getRatioProps())) {
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
        } else if (mp.getMimsType() == COMPOSITE_IMAGE) {
            if (mp.getCompositeProps().equals(this.getCompositeProps())) {
                return true;
            }
        } else if (mp.getMimsType() == SEG_IMAGE) {
            // TODO: Not sure what to add here
        }

        return false;
    }

    /**
     * This method sets the image to actually be the image enclosed by the Roi.
     *
     * @param roi a ROI object
     */
    @Override
    public void setRoi(ij.gui.Roi roi) {
        if (roi == null) {
            super.killRoi();
        } else {
            super.setRoi(roi);
        }
        stateChanged(roi, MimsPlusEvent.ATTR_SET_ROI);
    }

    @Override
    public void windowClosing(WindowEvent e) {

        if (getWindow() != null) {   // DJ: 08/07/2014 : the "IF" is added to avoid nullexception
            // when we get a valid window but totally blank and we try to close it
            java.awt.Point p = this.getWindow().getLocation();
            this.setXYLoc(p);

            // to be used when updating the ui.viewMassChanged
            // to keep it updated on the window location before it closes.
            if (this.nType == MASS_IMAGE) {
                MassProps windowMP = new MassProps(this.getMassIndex(), this.getMassValue());
                windowMP.setXWindowLocation(p.x + 9);
                windowMP.setYWindowLocation(p.y + 8);
                ui.addToClosedWindowsList(windowMP);
            }

        }
        //this.xloc = p.x;
        //this.yloc = p.y;

    }

    @Override
    public void windowClosed(WindowEvent e) {
        // When opening a stack, we get a close event
        // from the original ImageWindow, and ignore this event
        if (bIgnoreClose) {
            bIgnoreClose = false;
            return;
        }
        //ui.imageClosed(this);
        //ui.getCBControl().removeWindowfromList(this);
    }

    public void windowStateChanged(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
        //??? should this be handled in setActiveMimsPlus instead?
        ui.setActiveMimsPlus(this);
        ui.getCBControl().setWindowlistCombobox(getTitle());
        String defaultLUT = ui.getPreferences().getDefaultLUT();
        //ui.getCBControl().setLUT(lut);
        ui.getCBControl().setLUT(defaultLUT);
        if (ui.getMimsStackEditing() == null) {
            return;
        }
        MimsStackEditor.AutoTrackManager am = ui.getMimsStackEditing().atManager;
        if (am != null) {
            am.updateImage(this);
        }
        //??? can/should add reports?

        MimsRoiManager2 rm = ui.getRoiManager();
        if (rm == null) {
            return;
        }

        MimsRoiManager2.ParticlesManager pm = rm.getParticlesManager();
        MimsRoiManager2.SquaresManager sm = rm.getSquaresManager();

        if (pm != null) {
            pm.resetImage(this);
        }
        if (sm != null) {
            sm.resetImage(this);
        }
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {

        if (getWindow() != null) {    // DJ 08/06/2014

            java.awt.Window w = getWindow();
            if (w == null) {
                return;
            }
            WindowListener[] wl = w.getWindowListeners();
            boolean bFound = false;
            int i;
            for (i = 0; i < wl.length; i++) {
                if (wl[i] == this) {
                    bFound = true;
                }
            }
            if (!bFound) {
                getWindow().addWindowListener(this);
            }
            bFound = false;
            MouseListener[] ml = getWindow().getCanvas().getMouseListeners();
            for (i = 0; i < ml.length; i++) {
                if (ml[i] == this) {
                    bFound = true;
                }
            }
            if (!bFound) {
                getWindow().getCanvas().addMouseListener(this);
                getWindow().getCanvas().addMouseMotionListener(this);
            }
            
            String defaultLUT = ui.getPreferences().getDefaultLUT();
            //ui.getCBControl().setLUT(lut);
            ui.getCBControl().setLUT(defaultLUT);
            
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        ui.updateStatus(" ");
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (getWindow() != null) {    // DJ 08/06/2014

            if (IJ.getToolName().equals("OpenMIMS tool") && (this.nType == MimsPlus.MASS_IMAGE)) {
                startX = getWindow().getCanvas().offScreenX((int) e.getPoint().getX());
                startY = getWindow().getCanvas().offScreenY((int) e.getPoint().getY());
                pressX = startX;
                pressY = startY;
                return;
            }
        }
        
        
        boolean isROInull = true;

        // This delay very hokey, but the best I can do at the moment.  Previously,
        // as one zoomed in, it got harder and harder for one to create an ROI.
        // This can be solved (badly), by introducing a delay here that is
        // proportional to the level of zoom for an image window.  This may
        // have something to do with the time it takes the mouse to traverse
        // very large pixels during a drag.  This delay may prevent the 
        // system from thinking the next drag position is in the same pixel
        // as the starting pixel.  A better solution might be found in an
        // examination of the mouseMoved method below.  (WRT, Mar 8, 2018)
        ImageCanvas imageCanvas = getWindow().getCanvas();
        double percentZoom = imageCanvas.getMagnification();   // Tells how far in you have zoomed      
        try {
           Thread.sleep((long) (percentZoom * 30));
        } catch (InterruptedException ee) {
        }

        // getRoi does not seem ever to return null...
        if (getRoi() != null) {
            isROInull = false;
            // Set the moving flag so we know if user is attempting to move a roi.
            // Line Rois have to be treated differently because their state is never MOVING .
            int roiState = getRoi().getState();
            int roiType = getRoi().getType();

            if (roiState == Roi.MOVING) {
                bMoving = true;
            } else if (roiType == Roi.LINE && roiState == Roi.MOVING_HANDLE) {
                bMoving = true;
            } else {
                bMoving = false;
            }

            // Highlight the roi in the jlist that the user is selecting
            if (roi.getName() != null) {
                int i = ui.getRoiManager().getIndex(roi.getName());
                if (!(ij.IJ.controlKeyDown())) {
                    ui.getRoiManager().select(i);
                }
                if (ij.IJ.controlKeyDown()) {
                    bStateChanging = true;
                    ui.getRoiManager().selectAdd(i);
                }
            }

            // Get the location so that if the user simply declicks without
            // moving, a duplicate roi is not created at the same location.
            Rectangle r = getRoi().getBounds();
            x1 = r.x;
            y1 = r.y;
            w1 = r.width;
            h1 = r.height;

        } else if (!isROInull && (ij.IJ.shiftKeyDown() && ij.IJ.controlKeyDown())) {
            this.killRoi();
            bMoving = false;
        } 
        
        boolean shift = ij.IJ.shiftKeyDown();
        boolean control = ij.IJ.controlKeyDown();
        if (!isROInull && shift && !control) {
            this.killRoi();
            bMoving = false;
            ui.getRoiManager().delete(false, true);   // true for prompt
            
        }
        bStateChanging = false;

    }

    @Override
    public void mouseClicked(MouseEvent e) {

        if (bStateChanging) {
            return;
        }

        float[] pix;
        if (this.nType == HSI_IMAGE || this.nType == RATIO_IMAGE) {
            internalRatio.setRoi(getRoi());
            pix = (float[]) internalRatio.getProcessor().getPixels();
            internalRatio.killRoi();
        } else if (this.nType == SUM_IMAGE) {
            pix = (float[]) getProcessor().getPixels();
        } else if ((this.nType == SEG_IMAGE) || (this.nType == COMPOSITE_IMAGE)) {
            return;
        } else if (getProcessor() instanceof ShortProcessor) {
            short[] spix = (short[]) getProcessor().getPixels();
            pix = new float[spix.length];
            for (int i = 0; i < spix.length; i++) {
                pix[i] = (new Short(spix[i])).floatValue();
            }
        } else if (getProcessor() instanceof ByteProcessor) {
            return;
            //ImageProcessor ip = getProcessor();
            //pix = (float[]) ip.getPixels();
        } else {
            pix = (float[]) getProcessor().getPixels();
        }
        if (pix != null) {
            double[] dpix = new double[pix.length];
            for (int i = 0; i < pix.length; i++) {
                dpix[i] = (new Float(pix[i])).doubleValue();
            }
            ui.getMimsTomography().updateHistogram(dpix, getShortTitle(), true);

            //TODO: this should be somewhere else
            if (this.nType == HSI_IMAGE || this.nType == RATIO_IMAGE) {
                String stats = "";
                stats += this.getShortTitle() + ": ";
                stats += "mean = " + IJ.d2s(this.internalRatio.getStatistics().mean, 2) + " ";
                stats += "sd = " + IJ.d2s(this.internalRatio.getStatistics().stdDev, 2);
                ui.updateStatus(stats);
            } else {
                String stats = "";
                stats += this.getShortTitle() + ": ";
                stats += "mean = " + IJ.d2s(this.getStatistics().mean, 2) + " ";
                stats += "sd = " + IJ.d2s(this.getStatistics().stdDev, 2);
                ui.updateStatus(stats);
            }
        }

        //check magnification and update
        //ignoring tool type to start
        if (this.getCanvas() != null) {
            double lmag = this.getCanvas().getMagnification();
            if (this.getMimsType() == MimsPlus.HSI_IMAGE) {
                this.getHSIProps().setMag(lmag);
            }
            if (this.getMimsType() == MimsPlus.RATIO_IMAGE) {
                this.getRatioProps().setMag(lmag);
            }
            if (this.getMimsType() == MimsPlus.SUM_IMAGE) {
                this.getSumProps().setMag(lmag);
            }
            if (this.getMimsType() == MimsPlus.COMPOSITE_IMAGE) { // DJ 08/06/2014
                this.getCompositeProps().setMag(lmag);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        if (getWindow() != null) {  // DJ: 08/06/2014

            if (!IJ.getToolName().equals("Drag To Writer tool")) {
                if (bStateChanging) {
                    return;
                }
                if (bMoving) {
                    Roi thisroi = getRoi();
                    if (thisroi == null) {
                        return;
                    }
                    // Prevent duplicate roi at same location
                    Rectangle r = thisroi.getBounds();
                    x2 = r.x;
                    y2 = r.y;
                    w2 = r.width;
                    h2 = r.height;
                    if (x1 == x2 && y1 == y2 && w1 == w2 && h1 == h2) {
                        return;
                    }

                    stateChanged(getRoi(), MimsPlusEvent.ATTR_ROI_MOVED);

                    ui.getRoiManager().resetSpinners(thisroi);
                    updateHistogram(true);
                    bMoving = false;
                    return;
                }

                if (IJ.getToolName().equals("OpenMIMS tool") && (this.nType == MimsPlus.MASS_IMAGE) && e.isShiftDown()) {
                    int endX = getWindow().getCanvas().offScreenX((int) e.getPoint().getX());
                    int endY = getWindow().getCanvas().offScreenY((int) e.getPoint().getY());
                    int deltaX = endX - pressX;
                    int deltaY = endY - pressY;

                    int current = getCurrentSlice() + 1;
                    int total = getNSlices();

                    ui.getMimsStackEditing().translateStack(deltaX, deltaY, current, total);
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
            } else {
                System.out.println("release captured");

                //DJ:10/17/2014
                //DO NOT CHANGE NEITHER THE STRINGS NOR THEIR ORDER - THEY GET PARSED AT THE UNOPLUGIN.JAVA
                String[] allDescriptions = new String[6];

                String imageType = "";
                String displayMin = "";
                String displayMax = "";
                String planeNumber = "";
                String fileDescription = ui.getDescription();

                switch (this.getMimsType()) {
                    case MimsPlus.MASS_IMAGE:
                        imageType += "MASS_IMAGE";
                        displayMin += (new Double(this.getDisplayRangeMin())).intValue();
                        displayMax += (new Double(this.getDisplayRangeMax())).intValue();
                        planeNumber += this.getSlice();
                        break;

                    case MimsPlus.SUM_IMAGE:
                        imageType += "SUM_IMAGE";
                        displayMin += (new Double(this.getDisplayRangeMin())).intValue();
                        displayMax += (new Double(this.getDisplayRangeMax())).intValue();
                        planeNumber += "N/A";
                        break;

                    case MimsPlus.RATIO_IMAGE:
                        imageType += "RATIO_IMAGE";
                        displayMin += (new Double(this.getDisplayRangeMin())).intValue();
                        displayMax += (new Double(this.getDisplayRangeMax())).intValue();
                        if (ui.getIsSum()) {
                            planeNumber += "N/A";
                        } else {
                            planeNumber += this.getNumeratorImage().getSlice();
                        }

                        break;

                    case MimsPlus.HSI_IMAGE:
                        imageType += "HSI_IMAGE";
                        displayMin += "N/A";
                        displayMax += "N/A";
                        if (ui.getIsSum()) {
                            planeNumber += "N/A";
                        } else {
                            planeNumber += this.getNumeratorImage().getSlice();
                        }

                        break;

                    case MimsPlus.COMPOSITE_IMAGE:
                        imageType += "COMPOSITE_IMAGE";
                        displayMin += (new Double(this.getDisplayRangeMin())).intValue();
                        displayMax += (new Double(this.getDisplayRangeMax())).intValue();
                        planeNumber += "N/A";
                        break;

                    default:
                        imageType += "NON_MIMSIMAGE";
                        displayMin += (new Double(this.getDisplayRangeMin())).intValue();
                        displayMax += (new Double(this.getDisplayRangeMax())).intValue();
                        planeNumber += "N/A";
                }

                allDescriptions[0] = imageType;
                allDescriptions[1] = ui.getOpener().getImageFile().getName();
                allDescriptions[2] = displayMin;
                allDescriptions[3] = displayMax;
                allDescriptions[4] = planeNumber;
                allDescriptions[5] = fileDescription;

                //mimsUno.dropImage(ui.getScreenCaptureCurrentImage(), libreTitle, title, ui.getDescription());
                mimsUno.dropImage(ui.getScreenCaptureCurrentImage(), libreTitle, title, allDescriptions);
            }

        }
    }

    /**
     * Handles a mouse move event. A fairly in depth method that displays data in the status bar of the application. The
     * data that is displayed is dependant on the type of image (Mass, HSI, Sum, etc.) This method also controls various
     * aspects of ROI behavior regarding mouse events.
     *
     * @param e mouse move event
     */
    public void mouseMoved(MouseEvent e) {
        
        if (this.ui.isOpening()) {
            return;
        }

        if (getWindow() != null) {   // DJ:  08/06/2014

            // Get the X and Y position of the mouse.
            int x = (int) e.getPoint().getX();
            int y = (int) e.getPoint().getY();
            int mX = getWindow().getCanvas().offScreenX(x);
            int mY = getWindow().getCanvas().offScreenY(y);
            String msg = "" + mX + "," + mY;

            // Get the slice number.
            int cslice = getCurrentSlice();
            String cstring = Integer.toString(cslice);
            boolean stacktest = isStack();
            if (this.nType == RATIO_IMAGE || this.nType == HSI_IMAGE) {
                stacktest = stacktest || this.getNumeratorImage().isStack();
                if (stacktest) {
                    if (ui.getIsSum()) {
                        //if it's a sum HSI/ratio show the whole range of z values
                        cstring = "[1-" + this.getNumeratorImage().getStackSize() + "]";
                    } else if (ui.getIsWindow() && (ui.getWindowRange() > 0)) {
                        //if there's a non-zero radius window used show the range
                        //covered by the window, need to check edges too
                        int r = ui.getWindowRange();
                        int min = java.lang.Math.max(1, this.getNumeratorImage().getSlice() - r);
                        int max = java.lang.Math.min(this.getNumeratorImage().getSlice() + r,
                                this.getNumeratorImage().getStackSize());
                        cstring = "[" + min + "-" + max + "]";
                    } else {
                        cstring = "" + this.getNumeratorImage().getCurrentSlice();
                    }
                }
            }
            if (stacktest) {
                msg += "," + cstring + " = ";
            } else {
                msg += " = ";
            }

            //TODO, do something sensible for composite images....
            // Get pixel data for the mouse location.
            if ((this.nType == RATIO_IMAGE || this.nType == HSI_IMAGE) && (this.internalDenominator != null
                    && this.internalNumerator != null)) {
                float ngl = internalNumerator.getProcessor().getPixelValue(mX, mY);
                float dgl = internalDenominator.getProcessor().getPixelValue(mX, mY);
                double ratio = internalRatio.getProcessor().getPixelValue(mX, mY);
                msg += "S (" + (int) ngl + " / " + (int) dgl + ") = " + IJ.d2s(ratio, 2);
                if (ui.getMedianFilterRatios()) {
                    double medianizedValue;
                    if (this.nType == HSI_IMAGE) {
                        medianizedValue = internalRatio_filtered.getProcessor().getPixelValue(mX, mY);
                    } else {
                        medianizedValue = getProcessor().getPixelValue(mX, mY);
                    }
                    msg += " -med-> " + IJ.d2s(medianizedValue);
                }
            } else if (this.nType == SUM_IMAGE) {
                float ngl, dgl;
                if (internalNumerator != null && internalDenominator != null) {
                    ngl = internalNumerator.getProcessor().getPixelValue(mX, mY);
                    dgl = internalDenominator.getProcessor().getPixelValue(mX, mY);
                    msg += " S (" + ngl + " / " + dgl + ") = ";
                }
                int[] gl = getPixel(mX, mY);
                float s = Float.intBitsToFloat(gl[0]);
                msg += IJ.d2s(s, 0);
            } else {
                MimsPlus[] ml = ui.getOpenMassImages();
                int mindex = this.getMassIndex();
                for (int i = 0; i < ml.length; i++) {
                    if (i == mindex) {
                        //use "[]" to highlight current mass
                        msg += "[" + ml[i].getValueAsString(mX, mY) + "]";
                    } else {
                        msg += ml[i].getValueAsString(mX, mY);
                    }
                    if (i + 1 < ml.length) {
                        msg += ", ";
                    }
                }
            }

            // Loop over all Rois, determine which one to highlight.
            int displayDigits = 2;
            Hashtable rois = ui.getRoiManager().getROIs();

            Roi smallestRoi = null;
            double smallestRoiArea = 0.0;
            ij.process.ImageStatistics stats = null;
            ij.process.ImageStatistics smallestRoiStats = null;
            ij.process.ImageStatistics numeratorStats = null;
            ij.process.ImageStatistics denominatorStats = null;

            for (Object key : rois.keySet()) {
                Roi loopRoi = (Roi) rois.get(key);
                loopRoi.setImage(this);

                if (!((DefaultListModel) ui.getRoiManager().getList().getModel()).contains(loopRoi.getName())) {
                    continue;
                }

                boolean linecheck = false;
                int c = -1;
                if ((loopRoi.getType() == Roi.LINE) || (loopRoi.getType() == Roi.POLYLINE)
                        || (loopRoi.getType() == Roi.FREELINE)) {
                    // I have no idea why, but the isHandle() method for Line Rois
                    // only works with true x, y not image x, y. This apparently
                    // is only true for Line Rois.
                    c = loopRoi.isHandle(x, y);
                    if (c != -1) {
                        linecheck = true;
                    }

                    if (loopRoi.getType() == Roi.FREELINE && loopRoi.contains(mX, mY)) {
                        linecheck = true;
                    }
                }

                if (loopRoi.contains(mX, mY) || linecheck) {

                    if (linecheck) {
                        loopRoi.setStrokeWidth(this.getProcessor().getLineWidth());
                    }

                    if ((this.getMimsType() == RATIO_IMAGE || this.getMimsType() == HSI_IMAGE)
                            && internalRatio != null) {
                        internalRatio.setRoi(loopRoi);
                        stats = internalRatio.getStatistics();
                        internalRatio.killRoi();
                    } else {
                        setRoi(loopRoi);
                        stats = this.getStatistics();
                        killRoi();
                    }

                    // Set as smallest Roi that the mouse is within and save stats
                    if (smallestRoi == null) {
                        smallestRoi = loopRoi;
                        smallestRoiStats = stats;
                        smallestRoiArea = smallestRoiStats.area;
                        if (linecheck) {
                            smallestRoiArea = 0;
                        }
                    } else if (stats.area < smallestRoiArea || linecheck) {
                        smallestRoi = loopRoi;
                        smallestRoiArea = stats.area;
                        smallestRoiStats = stats;
                        if (linecheck) {
                            smallestRoiArea = 0;
                        }
                    }
                }
            }

            setRoi(smallestRoi);
            if (smallestRoi != null) {
                //get numerator and denominator stats
                if ((this.getMimsType() == HSI_IMAGE || this.getMimsType() == RATIO_IMAGE)
                        && internalNumerator != null && internalDenominator != null) {
                    internalNumerator.setRoi(smallestRoi);
                    internalDenominator.setRoi(smallestRoi);
                    numeratorStats = internalNumerator.getStatistics();
                    denominatorStats = internalDenominator.getStatistics();
                    internalNumerator.killRoi();
                    internalDenominator.killRoi();
                }

                double sf = 1.0;
                if (this.getMimsType() == HSI_IMAGE) {
                    sf = this.getHSIProps().getRatioScaleFactor();
                }
                if (this.getMimsType() == RATIO_IMAGE) {
                    sf = this.getRatioProps().getRatioScaleFactor();
                }

                //set image roi for vizualization
                //smallestRoi.setInstanceColor(java.awt.Color.YELLOW);  // deprecated
                smallestRoi.setStrokeColor(java.awt.Color.YELLOW);
                if (roi.getType() == Roi.LINE || roi.getType() == Roi.FREELINE || roi.getType() == Roi.POLYLINE) {
                    msg += "\t ROI " + roi.getName() + ": L = " + IJ.d2s(roi.getLength(), 0);
                } else {
                    msg += "\t ROI " + roi.getName()
                            + ": A = " + IJ.d2s(smallestRoiStats.area, 0)
                            + ", M=" + IJ.d2s(smallestRoiStats.mean, displayDigits)
                            + ", Sd=" + IJ.d2s(smallestRoiStats.stdDev, displayDigits);
                }
                updateHistogram(true);
                updateLineProfile();
                if ((this.getMimsType() == HSI_IMAGE || this.getMimsType() == RATIO_IMAGE) && numeratorStats != null && denominatorStats != null) {
                    double ratio_means = sf * (numeratorStats.mean / denominatorStats.mean);
                    if (this.getMimsType() == HSI_IMAGE && ui.getIsPercentTurnover()) {
                        float reference = ui.getPreferences().getReferenceRatio();
                        float background = ui.getPreferences().getBackgroundRatio();
                        float ratio_means_fl = HSIProcessor.turnoverTransform((float) ratio_means, reference, background, (float) sf);
                        ratio_means = (double) ratio_means_fl;
                    }
                    msg += ", N/D=" + IJ.d2s(ratio_means, displayDigits);
                }
            }
            ui.updateStatus(msg);
        }// DJ: end of getWindow() != null condition check
    }

    private String getValueAsString(int x, int y) {
        if (win != null && win instanceof PlotWindow) {
            return "";
        }
        Calibration cal = getCalibration();
        int[] v = getPixel(x, y);
        int type = getType();
        switch (type) {
            case GRAY8:
            case GRAY16:
            case COLOR_256:
                if (type == COLOR_256) {
                    if (cal.getCValue(v[3]) == v[3]) // not calibrated
                    {
                        return (", index=" + v[3] + "," + v[0] + "," + v[1] + "," + v[2]);
                    } else {
                        v[0] = v[3];
                    }
                }
                double cValue = cal.getCValue(v[0]);
                if (cValue == v[0]) {
                    return ("" + v[0]);
                } else {
                    return ("" + IJ.d2s(cValue) + " (" + v[0] + ")");
                }
            case GRAY32:
                DecimalFormat dec = new DecimalFormat();
                dec.setMaximumFractionDigits(0);
                return ("" + dec.format(Float.intBitsToFloat(v[0])));
            case COLOR_RGB:
                return ("" + v[0] + "," + v[1] + "," + v[2]);
            default:
                return ("");
        }
    }
    
    @Override
    // Display statistics while dragging or creating ROIs.
    public void mouseDragged(MouseEvent e) {
      
        if (getWindow() != null) {  // DJ: 08/06/2014

            if (!IJ.getToolName().equals("Drag To Writer tool")) {
                if (IJ.getToolName().equals("OpenMIMS tool") && (this.nType == MimsPlus.MASS_IMAGE)) {
                    //Get spinners
                    javax.swing.JSpinner xSpin = ui.getMimsStackEditing().getTranslateX();
                    javax.swing.JSpinner ySpin = ui.getMimsStackEditing().getTranslateY();

                    //Get mouse position
                    int x = (int) e.getPoint().getX();
                    int y = (int) e.getPoint().getY();
                    int mX = getWindow().getCanvas().offScreenX(x);
                    int mY = getWindow().getCanvas().offScreenY(y);

                    //Calculate deltas
                    int xDelta = mX - startX;
                    int yDelta = mY - startY;

                    //Spinner numbers
                    Double xVal = (Double) xSpin.getModel().getValue() + xDelta;
                    Double yVal = (Double) ySpin.getModel().getValue() + yDelta;

                    //Update spinners
                    xSpin.getModel().setValue(xVal);
                    ySpin.getModel().setValue(yVal);

                    //Update start points
                    startX = mX;
                    startY = mY;
                    return;
                }
                //cannot hit return below, confused as to why this is here
                if (Toolbar.getBrushSize() != 0 && Toolbar.getToolId() == Toolbar.OVAL) {
                    return;
                }

                // get mouse poistion
                int x = (int) e.getPoint().getX();
                int y = (int) e.getPoint().getY();
                int mX = getWindow().getCanvas().offScreenX(x);
                int mY = getWindow().getCanvas().offScreenY(y);
                String msg = "" + mX + "," + mY;

                int cslice = getCurrentSlice();
                String cstring = Integer.toString(cslice);
                boolean stacktest = isStack();
                if (this.nType == RATIO_IMAGE || this.nType == HSI_IMAGE) {
                    stacktest = stacktest || this.getNumeratorImage().isStack();
                    if (stacktest) {
                        if (ui.getIsSum()) {
                            //if it's a sum HSI/ratio show the whole range of z values
                            cstring = "[1-" + this.getNumeratorImage().getStackSize() + "]";
                        } else if (ui.getIsWindow() && (ui.getWindowRange() > 0)) {
                            //if there's a non-zero radius window used show the range
                            //covered by the window, need to check edges too
                            int r = ui.getWindowRange();
                            int min = java.lang.Math.max(1, this.getNumeratorImage().getSlice() - r);
                            int max = java.lang.Math.min(this.getNumeratorImage().getSlice() + r,
                                    this.getNumeratorImage().getStackSize());
                            cstring = "[" + min + "-" + max + "]";
                        } else {
                            cstring = "" + this.getNumeratorImage().getCurrentSlice();
                        }
                    }
                }
                if (stacktest) {
                    msg += "," + cstring + " = ";
                } else {
                    msg += " = ";
                }

                if ((this.nType == RATIO_IMAGE || this.nType == HSI_IMAGE)
                        && (this.internalDenominator != null && this.internalNumerator != null)) {
                    float ngl = internalNumerator.getProcessor().getPixelValue(mX, mY);
                    float dgl = internalDenominator.getProcessor().getPixelValue(mX, mY);
                    double ratio = internalRatio.getProcessor().getPixelValue(mX, mY);
                    String opstring = "";
                    if (ui.getMedianFilterRatios()) {
                        opstring = "-med->";
                    } else {
                        opstring = "=";
                    }
                    msg += "S (" + (int) ngl + " / " + (int) dgl + ") " + opstring + " " + IJ.d2s(ratio, 4);
                } else if (this.nType == SUM_IMAGE) {
                    float ngl, dgl;
                    if (internalNumerator != null && internalDenominator != null) {
                        ngl = internalNumerator.getProcessor().getPixelValue(mX, mY);
                        dgl = internalDenominator.getProcessor().getPixelValue(mX, mY);
                        msg += " S (" + ngl + " / " + dgl + ") = ";
                    }
                    int[] gl = getPixel(mX, mY);
                    float s = Float.intBitsToFloat(gl[0]);
                    msg += IJ.d2s(s, 0);
                } else {
                    MimsPlus[] ml = ui.getOpenMassImages();
                    for (int i = 0; i < ml.length; i++) {
                        int[] gl = ml[i].getPixel(mX, mY);
                        msg += gl[0];
                        if (i + 1 < ml.length) {
                            msg += ", ";
                        }
                    }
                }

                // precision
                int displayDigits = 3;

                // Get the ROI, (the area in yellow).
                Roi lroi = getRoi();

                double sf = 1.0;
                if (this.getMimsType() == HSI_IMAGE) {
                    sf = this.getHSIProps().getRatioScaleFactor();
                }
                if (this.getMimsType() == RATIO_IMAGE) {
                    sf = this.getRatioProps().getRatioScaleFactor();
                }

                // Display stats in the message bar.
                if (lroi != null) {
                    ij.process.ImageStatistics stats = this.getStatistics();
                    ij.process.ImageStatistics numeratorStats = null;
                    ij.process.ImageStatistics denominatorStats = null;

                    if (this.getMimsType() == MimsPlus.HSI_IMAGE || this.getMimsType() == MimsPlus.RATIO_IMAGE) {
                        this.internalRatio.setRoi(lroi);
                        stats = this.internalRatio.getStatistics();
                        msg += "\t ROI " + lroi.getName() + ": A=" + IJ.d2s(stats.area, 0)
                                + ", M=" + IJ.d2s(stats.mean, displayDigits) + ", Sd=" + IJ.d2s(stats.stdDev, displayDigits);
                    } else {
                        msg += "\t ROI " + lroi.getName() + ": A=" + IJ.d2s(stats.area, 0)
                                + ", M=" + IJ.d2s(stats.mean, displayDigits) + ", Sd=" + IJ.d2s(stats.stdDev, displayDigits);
                    }

                    //get numerator denominator stats
                    if ((this.getMimsType() == HSI_IMAGE || this.getMimsType() == RATIO_IMAGE)
                            && internalNumerator != null && internalDenominator != null) {
                        internalNumerator.setRoi(lroi);
                        numeratorStats = internalNumerator.getStatistics();
                        internalNumerator.killRoi();

                        internalDenominator.setRoi(lroi);
                        denominatorStats = internalDenominator.getStatistics();
                        internalDenominator.killRoi();

                        double ratio_means = sf * numeratorStats.mean / denominatorStats.mean;
                        msg += ", N/D=" + IJ.d2s(ratio_means, displayDigits);
                    }
                    ui.updateStatus(msg);

                    setRoi(lroi);

                    updateHistogram(false);
                    updateLineProfile();

                }
            }
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

        if (getWindow() != null) {  // DJ: 08/06/2014

            int plane = 1;
            int size = 1;
            MimsPlus mp = null;
            if (IJ.controlKeyDown()) {
                this.getWindow().mouseWheelMoved(e);
                return;
            }

            if (this.nType == MimsPlus.MASS_IMAGE) {
                this.getWindow().mouseWheelMoved(e);
                return;
            } else if (this.nType == MimsPlus.HSI_IMAGE) {
                mp = ui.getMassImage(this.getHSIProps().getNumMassIdx());
                plane = mp.getSlice();
                size = mp.getStackSize();
            } else if (this.nType == MimsPlus.RATIO_IMAGE) {
                mp = ui.getMassImage(this.getRatioProps().getNumMassIdx());
                plane = mp.getSlice();
                size = mp.getStackSize();
            } else if (this.nType == MimsPlus.SUM_IMAGE) {
                return;
            }

            if (mp == null) {
                return;
            }

            int d = e.getWheelRotation();
            if (((plane + d) <= size) && ((plane + d) >= 1)) {
                mp.setSlice(plane + d);
            }

        }
    }

    /**
     * Updates histogram values.
     *
     * @param force force update (true or false)
     */
    private void updateHistogram(boolean force) {
        if (roi == null) {
            return;
        }
        // Update histogram (area Rois only).
        if ((roi.getType() == Roi.FREEROI) || (roi.getType() == Roi.OVAL)
                || (roi.getType() == Roi.POLYGON) || (roi.getType() == Roi.RECTANGLE)) {
            int imageLabel = ui.getRoiManager().getIndex(roi.getName()) + 1;
            String label = getShortTitle() + " Roi: (" + imageLabel + ")";
            double[] roiPix;
            if (this.nType == HSI_IMAGE || this.nType == RATIO_IMAGE) {
                internalRatio.setRoi(getRoi());
                roiPix = internalRatio.getRoiPixels();
                internalRatio.killRoi();
            } else {
                roiPix = this.getRoiPixels();
            }
            if (roiPix != null) {
                MimsTomography mt = ui.getMimsTomography();
                // We don't do tomography on non-mims images unless a mims image is open,
                // and MimsTomography is null if no mims image is open.
                if (mt != null) {
                    mt.updateHistogram(roiPix, label, force);
                }
            }
        }

    }

    /**
     * Update image line profile. Line profiles for ratio images and HSI images should be identical.
     */
    private void updateLineProfile() {
        if (roi == null) {
            return;
        }

        if (!roi.isLine()) {
            return;
        }

        ij.gui.ProfilePlot profileP = null;
        // Line profiles for ratio images and HSI images should be identical.
        if (this.nType == HSI_IMAGE || this.nType == RATIO_IMAGE) {
            internalRatio.setRoi(getRoi());
            profileP = new ij.gui.ProfilePlot(internalRatio);
            internalRatio.killRoi();
        } else {
            profileP = new ij.gui.ProfilePlot(this);
        }
        ui.updateLineProfile(profileP.getProfile(), this.getShortTitle() + " : "
                + roi.getName(), this.getProcessor().getLineWidth(), this);

    }

    /**
     * Obtain ROI pixel values.
     *
     * @return Array of pixel values
     */
    public double[] getRoiPixels() {
        if (this.getRoi() == null) {
            return null;
        }

        // Rectangle rect = roi.getBoundingRect();   // deprecated.  Replace with getBounds
        Rectangle rect = roi.getBounds();
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
                    if (mi >= mask.length) {
                        break;
                    }

                    // mask should never be null here.
                    if (mask == null || mask[mi++] != 0) {
                        pixellist.add((double) imp.getPixelValue(x, y));
                    }
                    i++;
                }
            }
            double[] foo = new double[pixellist.size()];
            for (int j = 0; j < foo.length; j++) {
                foo[j] = pixellist.get(j);
            }
            return foo;
        }
    }

    /**
     * Adds listener tp the object.
     *
     * @param inListener Listener to add
     */
    public void addListener(MimsUpdateListener inListener) {
        fStateListeners.add(MimsUpdateListener.class, inListener);
    }

    /**
     * Removes listener from the object.
     *
     * @param inListener Listener to remove
     */
    public void removeListener(MimsUpdateListener inListener) {
        fStateListeners.remove(MimsUpdateListener.class, inListener);
    }

    /**
     * Set LUT
     *
     * @param label Label for LUT
     */
    public void setLut(String label) {
        lut = label;
    }

    /**
     * extends setSlice to notify listeners when the frame updates enabling synchronization with other windows
     *
     * @param slice
     * @param attr
     */
    private void stateChanged(int slice, int attr) {
        bStateChanging = true;
        MimsPlusEvent event = new MimsPlusEvent(this, slice, attr);
        //System.out.println("in stateChanged, slice is " + slice);
        Object[] listeners = fStateListeners.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == MimsUpdateListener.class) {
                ((MimsUpdateListener) listeners[i + 1])
                        .mimsStateChanged(event);
            }
        }
        bStateChanging = false;
    }

    private void stateChanged(int slice, int attr, boolean updateRatioHSI) {
        bStateChanging = true;
        MimsPlusEvent event = new MimsPlusEvent(this, slice, attr, updateRatioHSI);
        Object[] listeners = fStateListeners.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == MimsUpdateListener.class) {
                ((MimsUpdateListener) listeners[i + 1])
                        .mimsStateChanged(event);
            }
        }
        bStateChanging = false;
    }

    private void stateChanged(int slice, int attr, MimsPlus mplus) {
        bStateChanging = true;
        MimsPlusEvent event = new MimsPlusEvent(this, slice, attr, mplus);
        Object[] listeners = fStateListeners.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == MimsUpdateListener.class) {
                ((MimsUpdateListener) listeners[i + 1])
                        .mimsStateChanged(event);
            }
        }
        bStateChanging = false;
    }

    private void stateChanged(ij.gui.Roi roi, int attr) {
        MimsPlusEvent event = new MimsPlusEvent(this, roi, attr);
        Object[] listeners = fStateListeners.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == MimsUpdateListener.class) {
                ((MimsUpdateListener) listeners[i + 1])
                        .mimsStateChanged(event);
            }
        }
    }

    /**
     * Sets the slice.
     *
     * @param index the plane number (starts with 1).
     */
    @Override
    public synchronized void setSlice(int index) {
        if (getCurrentSlice() == index) {
            return;
        }
        super.setSlice(index);
        if (bStateChanging) {
            return;
        }
        stateChanged(index, MimsPlusEvent.ATTR_UPDATE_SLICE);
    }

    /**
     * Set slice as current
     *
     * @param index the index of the current slice
     * @param updateRatioHSI  a boolean not used at this time
     */
    public synchronized void setSlice(int index, boolean updateRatioHSI) {
        if (getCurrentSlice() == index) {
            return;
        }
        super.setSlice(index);
        if (bStateChanging) {
            return;
        }
        stateChanged(index, MimsPlusEvent.ATTR_UPDATE_SLICE, false);
    }

    /**
     * Set slice as current
     *
     * @param index the index of the slice
     * @param mplus the <code>MimsPlus</code> instance
     */
    public synchronized void setSlice(int index, MimsPlus mplus) {
        if (this.getCurrentSlice() == index) {
            return;
        }
        super.setSlice(index);
        if (bStateChanging) {
            return;
        }
        stateChanged(index, MimsPlusEvent.ATTR_UPDATE_SLICE, mplus);
    }

    /**
     * Set the allow close flag
     *
     * @param allowClose  boolean allowClose flag.
     */
    public void setAllowClose(boolean allowClose) {
        this.allowClose = allowClose;
    }

    /**
     *
     */
    @Override
    public void close() {
        while (this.lockSilently() == false) {
        }
        try {
            if (allowClose) {

                ui.imageClosed(this);   // calls ui.imageClosed()
                ui.getCBControl().removeWindowfromList(this);
                MimsTomography mt = ui.getMimsTomography();
                if (mt != null) {
                    mt.resetImageNamesList();
                }
                super.close();
            } else {
                this.hide();
                ui.massImageClosed(this);
            }
        } finally {
            this.unlock();
        }
    }

    /**
     * Set HSI processor
     *
     * @param processor an <code>HSIProcessor</code> instance
     */
    public void setHSIProcessor(HSIProcessor processor) {
        this.hsiProcessor = processor;
    }

    /**
     *
     * @return HSI processor
     */
    public HSIProcessor getHSIProcessor() {
        return hsiProcessor;
    }

    /**
     * Sets Composite processor
     *
     * @param processor a <code>CompositeProcessor</code> instance
     */
    public void setCompositeProcessor(CompositeProcessor processor) {
        this.compProcessor = processor;
    }

    /**
     *
     * @return composite processor
     */
    public CompositeProcessor getCompositeProcessor() {
        return compProcessor;
    }

    /**
     * Sets auto contrast adjust flag (true/false)
     *
     * @param auto auto contrast adjust flag
     */
    public void setAutoContrastAdjust(boolean auto) {
        this.autoAdjustContrast = auto;
    }

    /**
     *
     * @return auto contrast adjust flag
     */
    public boolean getAutoContrastAdjust() {
        return autoAdjustContrast;
    }

    /**
     *
     * @return "is stack" flag
     */
    public boolean isStack() {
        return bIsStack;
    }

    /**
     * Sets "is stack" flag
     *
     * @param isS boolean to determine whether or not to set the stack flag
     */
    public void setIsStack(boolean isS) {
        bIsStack = isS;
    }

    /**
     *
     * @return mass index
     */
    public int getMassIndex() {
        return massIndex;
    }

    /**
     *
     * @return mass value
     */
    public double getMassValue() {
        return massValue;
    }

    /**
     *
     * @return MIMS type
     */
    public int getMimsType() {
        return nType;
    }

    /**
     *
     * @return sum properties
     */
    public SumProps getSumProps() {
        if (getWindow() != null && isVisible()) {       // DJ: 06/06/2014
            sumProps.setXWindowLocation(getWindow().getX());
            sumProps.setYWindowLocation(getWindow().getY());
            sumProps.setMag(getCanvas().getMagnification());
        }

        if (sumProps.getSumType() == SumProps.RATIO_IMAGE) {
            sumProps.setNumMassValue(ui.getMassValue(sumProps.getNumMassIdx()));
            sumProps.setDenMassValue(ui.getMassValue(sumProps.getDenMassIdx()));
        } else if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
            sumProps.setParentMassValue(ui.getMassValue(sumProps.getParentMassIdx()));
        }

        sumProps.setMinLUT(getDisplayRangeMin());
        sumProps.setMaxLUT(getDisplayRangeMax());

        return sumProps;

    }

    /**
     *
     * @return ratio properties
     */
    public RatioProps getRatioProps() {
        if (getWindow() != null && isVisible()) {       // DJ: 06/06/2014
            ratioProps.setXWindowLocation(getWindow().getX());
            ratioProps.setYWindowLocation(getWindow().getY());
            ratioProps.setMag(getCanvas().getMagnification());
        }
        ratioProps.setNumMassValue(ui.getMassValue(ratioProps.getNumMassIdx()));
        ratioProps.setDenMassValue(ui.getMassValue(ratioProps.getDenMassIdx()));
        ratioProps.setMinLUT(getDisplayRangeMin());
        ratioProps.setMaxLUT(getDisplayRangeMax());
        return ratioProps;
    }

    /**
     *
     * @return HSI properties
     */
    public HSIProps getHSIProps() {
        hsiProps = getHSIProcessor().getHSIProps();
        if (getWindow() != null && isVisible()) {     // DJ: 06/06/2014
            hsiProps.setXWindowLocation(getWindow().getX());
            hsiProps.setYWindowLocation(getWindow().getY());
            hsiProps.setMag(getCanvas().getMagnification());
        }
        hsiProps.setNumMassValue(ui.getMassValue(hsiProps.getNumMassIdx()));
        hsiProps.setDenMassValue(ui.getMassValue(hsiProps.getDenMassIdx()));
        return hsiProps;
    }

    /**
     *
     * @return ratio properties
     */
    public CompositeProps getCompositeProps() {
        compProps = getCompositeProcessor().getProps();
        // DJ: 08/04/2014
        if (getWindow() != null && isVisible()) {
            compProps.setXWindowLocation(getWindow().getX());
            compProps.setYWindowLocation(getWindow().getY());
            compProps.setMag(getCanvas().getMagnification());
        }

        return compProps;
    }

    /**
     *
     * @return UI object
     */
    public UI getUI() {
        return ui;
    }

    /**
     * Set
     *
     * @return graphOverlay
     */
    public Overlay getGraphOverlay() {
        return graphOverlay;
    }

}

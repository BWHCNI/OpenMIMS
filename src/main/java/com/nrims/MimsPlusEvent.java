package com.nrims;

/**
 * A class for triggering events based on certain actions performed on a MimsPlus window.
 *
 * @author Douglas Benson
 */
public class MimsPlusEvent extends java.util.EventObject {

    static final int ATTR_UPDATE_SLICE = 1;
    static final int ATTR_IMAGE_CLOSED = 2;
    static final int ATTR_SET_ROI = 3;
    static final int ATTR_MOUSE_RELEASE = 4;
    static final int ATTR_ROI_MOVED = 5;

    /**
     * Creates a new instance of mimsPlusEvent
     * 
     * @param mimsImage a <code>MimsPlus</code> object
     * @param slice slice number
     * @param attribute a changeAttribute number
     */
    public MimsPlusEvent(MimsPlus mimsImage, int slice, int attribute) {
        super(mimsImage);
        this.nSlice = slice;
        this.changeAttribute = attribute;
    }

    public MimsPlusEvent(MimsPlus mimsImage, int slice, int attribute, boolean updateRatioHSI) {
        super(mimsImage);
        this.updateRatioHSI = updateRatioHSI;
        this.nSlice = slice;
        this.changeAttribute = attribute;
    }

    public MimsPlusEvent(MimsPlus mimsImage, int slice, int attribute, MimsPlus mplus) {
        super(mimsImage);
        this.mplus = mplus;
        this.nSlice = slice;
        this.changeAttribute = attribute;
    }

    public MimsPlusEvent(MimsPlus mimsImage, ij.gui.Roi roi, int attribute) {
        super(mimsImage);
        this.changeAttribute = attribute;
        this.roi = roi;
    }

    public int getAttribute() {
        return changeAttribute;
    }

    public int getSlice() {
        return nSlice;
    }

    public ij.gui.Roi getRoi() {
        return roi;
    }

    public boolean getUpdateRatioHSI() {
        return updateRatioHSI;
    }

    public MimsPlus getMimsPlus() {
        return mplus;
    }

    private boolean updateRatioHSI = true;
    private MimsPlus mplus = null;
    private int nSlice;
    private int changeAttribute;
    private ij.gui.Roi roi = null;
}

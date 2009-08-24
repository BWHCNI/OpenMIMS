/*
 * mimsPlusEvent.java
 *
 * Created on May 3, 2006, 12:47 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.nrims;

/**
 *
 * @author Douglas Benson
 */
public class MimsPlusEvent extends java.util.EventObject {
    static final int ATTR_UPDATE_SLICE = 1 ;
    static final int ATTR_IMAGE_CLOSED = 2 ;
    static final int ATTR_SET_ROI = 3 ;
    static final int ATTR_MOUSE_RELEASE = 4 ;
    static final int ATTR_ROI_MOVED = 5;
    
    /** Creates a new instance of mimsPlusEvent */
    public MimsPlusEvent( MimsPlus mimsImage, int slice, int attribute ) {
        super(mimsImage);
        this.nSlice = slice ;
        this.changeAttribute = attribute ;
    }

    public MimsPlusEvent( MimsPlus mimsImage, ij.gui.Roi roi, int attribute ) {
        super(mimsImage);
        this.changeAttribute = attribute ;
        this.roi = roi ;
    }
    
    public int getAttribute() { return changeAttribute; }
    public int getSlice() { return nSlice; }
    public ij.gui.Roi getRoi() { return roi ; }
    
    private int nSlice ;
    private int changeAttribute ;
    private ij.gui.Roi roi = null ;
}

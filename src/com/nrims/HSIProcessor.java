/*
 * HSIProcessor.java
 *
 * Created on May 4, 2006, 10:28 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.nrims;

import ij.plugin.* ;
import ij.* ;
import ij.io.* ;
import ij.io.FileInfo ;
import ij.gui.* ;
import ij.process.* ;
import ij.measure.Calibration ;

import java.awt.* ;
import java.awt.event.* ;
import java.io.*;
import javax.swing.*;
import javax.swing.JTextArea;
/**
 *
 * @author Douglas Benson
 */
public class HSIProcessor implements Runnable {
    
    /**
     * Creates a new instance of HSIProcessor
     */
    public HSIProcessor(MimsPlus hsiImage ) {
        this.hsiImage = hsiImage ;
        this.medianize = this.hsiImage.getUI().getMedianFilterRatios();
        compute_hsi_table() ;
    }
    
    public void finalize() {
        hsiImage = null ;
        hsiProps = null ;
    }
   
    private MimsPlus hsiImage = null ;
    private HSIProps hsiProps = null ;
    private Thread fThread = null ;

    private static float[] rTable = null ;
    private static float[] gTable = null ;
    private static float[] bTable = null ;

    private static final double S6_6 = Math.sqrt(6.0) / 6.0 ;
    private static final double S6_3 = Math.sqrt(6.0) / 3.0 ;
    private static final double S6_2 = Math.sqrt(6.0) / 2.0 ;
    private static final double FULLSCALE = 65535.0 / (2.0 * Math.PI);
    private static final int MAXNUMDEN	=  0 ; 
    private static final int NUMERATOR	=  1 ; 
    private static final int DENOMINATOR =  2 ; 
    private static final int MINNUMDEN	=  3 ; 
    private static final int MEANNUMDEN	=  4 ; 
    private static final int SUMNUMDEN	=  5 ; 
    private static final int RMSNUMDEN	=  6 ; 
    
    private int numSlice = 1 ;
    private int denSlice = 1 ;

    private boolean medianize;

    public void setProps(HSIProps props) {
        if(hsiImage == null) return ;
        
        MimsPlus numerator = hsiImage.getUI().getMassImage(props.getNumMass());
        MimsPlus denominator = hsiImage.getUI().getMassImage(props.getDenMass());
        if(numerator == null || denominator == null) return ;
        // Need to catch cases where the props are the same but the slice changed
        boolean bNeedsUpdate = false ;
        int nSlice = numerator.getCurrentSlice() ;
        int dSlice = denominator.getCurrentSlice() ;
        if(nSlice != numSlice || dSlice != denSlice ) bNeedsUpdate = true ;
        numSlice = nSlice ;
        denSlice = dSlice ;
        if(hsiProps == null) hsiProps = props.clone();
        else if( !bNeedsUpdate && hsiProps.equal(props)) {
             if(hsiImage.getUI().getDebug())
                hsiImage.getUI().updateStatus("HSIProcessor: no change redraw..");
            hsiImage.updateAndDraw() ;
            return ;
        }
        else
            hsiProps.setProps(props);
        start();
    }
    
    public HSIProps getProps() { return hsiProps ; }
    
    private synchronized void start() {
        if(fThread != null) {
            if(hsiImage.getUI().getDebug())
                hsiImage.getUI().updateStatus("HSIProcessor: stop and restart");
            stop();
        }
        fThread = new Thread(this);
        fThread.setPriority(fThread.NORM_PRIORITY);
        fThread.setContextClassLoader(
                Thread.currentThread().getContextClassLoader());
        if(hsiImage.getUI().getDebug())
                hsiImage.getUI().updateStatus("HSIProcessor: start");
        try { fThread.start();}
        catch( IllegalThreadStateException x){ IJ.log(x.toString()); }
    }
    
    private void stop() {
        if(fThread != null) {
            fThread.interrupt();
            fThread = null ;
        }
    }

    public boolean isRunning() {
        if(fThread == null) return false ;
        return fThread.isAlive() ;
    }
    
    public void run( ) {
        
        MimsPlus numerator = null , denominator = null ;
        
        try {
            if( hsiImage == null ) { fThread = null ; return ; }
        
            while( hsiImage.lockSilently() == false ) {
                if(fThread == null || fThread.interrupted()) {
                    return ;
                }
            }
            
            MimsPlus [] ml = hsiImage.getUI().getMassImages() ;
            numerator = ml[hsiProps.getNumMass()];
            if( numerator == null ) { 
                fThread = null ;
                hsiImage.unlock();
                return ;
            }

            denominator = ml[hsiProps.getDenMass()];
            if( denominator == null ) { 
                fThread = null ;
                hsiImage.unlock();
                return ;
            }
            
            while( numerator.lockSilently() == false ){
                if(fThread == null || fThread.interrupted()) {
                    hsiImage.unlock();
                    return ;
                }
            }
            while( denominator.lockSilently() == false ) {
                if(fThread == null || fThread.interrupted()) {
                    hsiImage.unlock();
                    numerator.unlock();
                    return ;
                }
            }
               
            short [] numPixels = (short []) numerator.getProcessor().getPixels() ;
            short [] denPixels = (short []) denominator.getProcessor().getPixels() ;
            int [] hsiPixels = (int []) hsiImage.getProcessor().getPixels() ;
            int rgbMax = hsiProps.getMaxRGB() ;
            int rgbMin = hsiProps.getMinRGB() ;
            if(rgbMax == rgbMin) rgbMax++ ;
            double rgbGain = 255.0 / (double)(rgbMax-rgbMin);

            double numMax = numerator.getProcessor().getMax() ;
            double numMin = numerator.getProcessor().getMin() ;
            double numGain = numMax > numMin ? 255.0 / ( numMax - numMin ) : 1.0 ;

            double denMax = denominator.getProcessor().getMax() ;
            double denMin = denominator.getProcessor().getMin() ;
            double denGain = denMax > denMin ? 255.0 / ( denMax - denMin ) : 1.0 ;
            double maxRatio = hsiProps.getMaxRatio() ;
            double minRatio = hsiProps.getMinRatio() ;
            int showLabels = hsiProps.getLabelMethod() ;
            
            if(maxRatio <= 0.0) maxRatio = 1.0 ;
            float rScale = 65535.0F / (float)maxRatio ;
            int numThreshold = hsiProps.getMinNum() ;
            int denThreshold = hsiProps.getMinDen() ;
            int transparency = hsiProps.getTransparency() ;

            //TODO
            //attempt at medianization, need to make better...

            float ratioPixels[] = new float[numPixels.length];
            MimsPlus internalRatio = this.hsiImage.getInternalRatio();
            /*
            float[] medPixels = ratioPixels;
            if (this.hsiImage.getUI().getMedianFilterRatios() && ( internalRatio != null ) ) {
                ij.plugin.filter.RankFilters rfilter = new ij.plugin.filter.RankFilters();
                double r = this.hsiImage.getUI().getHSIView().getMedianRadius();
                rfilter.rank(internalRatio.getProcessor(), r, rfilter.MEDIAN);
                medPixels = (float[])internalRatio.getProcessor().getPixels();
            } else if(internalRatio != null) {
                medPixels = (float[])internalRatio.getProcessor().getPixels();
            }  else {
                for (int i = 0; i < ratioPixels.length; i++) {
                    if (denPixels[i] != 0) {
                        medPixels[i] = ((float) numPixels[i]) / ((float) denPixels[i]);
                    } else {
                        medPixels[i] = 0; //doesn't get looked at because of thresholds
                    }
                }
            }
            */
            //not the right way to do this
             for (int i = 0; i < ratioPixels.length; i++) {
                if (denPixels[i] != 0) {
                    ratioPixels[i] = hsiProps.getRatioScaleFactor()*((float) numPixels[i]) / ((float) denPixels[i]);
                } else {
                    ratioPixels[i] = 0; //doesn't get looked at because of thresholds
                }
            }
            float[] medPixels = ratioPixels;
            if (medianize && internalRatio != null) {
                ij.process.FloatProcessor tempProc = new ij.process.FloatProcessor(internalRatio.getProcessor().getWidth(),internalRatio.getProcessor().getHeight()); //change
                tempProc.setPixels(ratioPixels);
                ij.plugin.filter.RankFilters rfilter = new ij.plugin.filter.RankFilters();
                double r = this.hsiImage.getUI().getHSIView().getMedianRadius();
                rfilter.rank(tempProc, r, rfilter.MEDIAN);
                
                medPixels = (float[]) tempProc.getPixels();
                internalRatio.getProcessor().setPixels(medPixels);
            }

            if (!medianize && internalRatio != null) {
                internalRatio.getProcessor().setPixels(ratioPixels);
            }

            for(int offset = 0 ; offset < numPixels.length && fThread != null ; offset++ ) {
                
                int numValue =  (int) ( numPixels[offset] ) ;
                int denValue =  (int) ( denPixels[offset] ) ;
            
                if( numValue > numThreshold && denValue > denThreshold ){
            
                    //original
                    //float ratio = hsiProps.getRatioScaleFactor()*((float)numValue / (float)denValue );
                    float ratio = medPixels[offset];
                    
                    int numOut = (int)(numGain * (float)( numValue - (int)numMin )) ;
                    int denOut = (int)(denGain * (float)( denValue - (int)denMin )) ;

                    int outValue, r,g,b;
                    
                    switch(transparency) {
                        default:
                        case MAXNUMDEN:
                                outValue = numOut > denOut ? numOut : denOut ;
                                break ;
                        case NUMERATOR:
                                outValue = numOut  ;
                                break ;
                        case DENOMINATOR:
                                outValue = denOut  ;
                                break ;
                        case MINNUMDEN:
                                outValue = numOut < denOut ? numOut : denOut  ;
                                break ;
                        case MEANNUMDEN:
                                outValue = ( numOut + denOut) / 2 ;
                                break ;
                        case SUMNUMDEN:
                                outValue =  numOut + denOut ;
                                break ;
                        case RMSNUMDEN:
                                outValue = (int)Math.sqrt( numOut*numOut 
                                                                + denOut*denOut) ;
                                break ;
                    }

                    outValue = (int) ((double)(outValue - rgbMin) * rgbGain) ;
                    if(outValue < 0) outValue = 0 ;
                    else if(outValue > 255) outValue = 255 ;
                   
                    int iratio = 0 ;
                    if( ratio > minRatio) {
                        if( ratio < maxRatio ) {
                            iratio = (int)( (ratio-minRatio) * rScale ) ;
                            if(iratio < 0) iratio = 0 ;
                            else if(iratio > 65535) iratio = 65535 ;                           
                        }
                        else iratio = 65535 ;
                    }
                    
                    r = (int)(rTable[iratio] * outValue ) << 16 ;
                    g = (int)(gTable[iratio] * outValue ) << 8  ;
                    b = (int)(bTable[iratio] * outValue ) ;

                    hsiPixels[offset] = r + g + b ;

                }
                else{
                    hsiPixels[offset] = 0 ;
                }
                
                if(fThread == null || fThread.interrupted()) {
                    fThread = null ;
                    hsiImage.unlock() ;
                    denominator.unlock();
                    numerator.unlock();
                    return ;
                }
            }
  
            if( numPixels.length != hsiPixels.length ) {

                double dScale = 65535.0F / maxRatio ;
                double dRatio = minRatio ;
                double dDelta = ( maxRatio - minRatio ) 
                                / (double) hsiImage.getWidth() ;

                for( int x=0 ; x<hsiImage.getWidth() ; x++ )
                {
                    int iratio = (int)(dRatio * dScale ) ;
                    int r = (int)(rTable[iratio] * 255.0 )  ;
                    int g = (int)(gTable[iratio] * 255.0 )  ;
                    int b = (int)(bTable[iratio] * 255.0 )  ;

                    dRatio += dDelta ;

                    int offset, y ;
                    for(offset = numPixels.length + x, y = 0 ;
                            y < 16 ;
                            y++, offset += hsiImage.getWidth() )
                    {
                            hsiPixels[offset] = ((r&0xff)<<16) + ((g&0xff)<<8) + (b&0xff) ;
                    }
                }

                if(showLabels > 1) 	// Add the labels..
                {
                    hsiImage.getProcessor().setColor(Color.white);

                    hsiImage.getProcessor().moveTo( 0, hsiImage.getHeight() - 16) ;
                    String label = IJ.d2s(minRatio) ;
                    hsiImage.getProcessor().drawString( label ) ;
                    hsiImage.getProcessor().moveTo( 
                                                    hsiImage.getWidth()/2 - 12,
                                                    hsiImage.getHeight() - 16 ) ;
                    label = IJ.d2s(((maxRatio-minRatio)/2 + minRatio),2) ;
                    hsiImage.getProcessor().drawString( label ) ;

                    hsiImage.getProcessor().moveTo( 
                                                    hsiImage.getWidth() - 24,
                                                    hsiImage.getHeight() - 16) ;
                    label = IJ.d2s(maxRatio,2) ;
                    hsiImage.getProcessor().drawString( label ) ;

                }
            }

            denominator.unlock();
            numerator.unlock();
            hsiImage.unlock();
            hsiImage.updateAndRepaintWindow();          
            fThread = null ;
            
        }
        catch(Exception x) {
            hsiImage.unlock();
            if(denominator != null) denominator.unlock();
            if(numerator != null ) numerator.unlock();
            fThread = null ;
            IJ.log(x.toString());
            x.printStackTrace();
        }
              
    }

    private static float[] ratio_to_rgb( int ratio ){
        float [] rgb = new float[3] ;
        double i0, i1, i2, o0, o1, o2 ;
        double g2h ;

        i0 = 160.0 ;
        i1 = ratio ;
        i2 = 128.0 ;
        g2h = ( i1 / FULLSCALE ) - Math.PI ;
        i1 = i2 * Math.cos(g2h) ;
        i2 *= Math.sin(g2h);
        o0 = i0 - i1 * S6_6 + i2 * S6_2 ;
        o1 = i0 - i1 * S6_6 - i2 * S6_2 ;
        o2 = i0 + i1 * S6_3 ;
        if(o0 < 0) o0 = 0 ;
        if(o0 > 255.0) o0 = 255.0 ;
        if(o1 < 0) o1 = 0 ;
        if(o1 > 255.0) o1 = 255.0 ;
        if(o2 < 0) o2 = 0 ;
        if(o2 > 255.0) o2 = 255.0 ;

        if(ratio < 16384 ) rgb[0] = (float)o0 * (float)ratio / 4177920.0F ;
        else rgb[0] = (float)o0 / 255.0F ;
        rgb[1] = (float)o2 / 255.0F ;
        rgb[2] = (float)o1 / 255.0F ;
        return rgb ;
    }

//    private static void compute_hsi_table() {
//        if(rTable == null || gTable == null || bTable == null ) {
//            if(rTable == null) rTable = new float[65536] ;
//            if(gTable == null) gTable = new float[65536] ;
//            if(bTable == null) bTable = new float[65536] ;
//            for( int i = 0 ; i < 65536 ; i++ ){
//                    float [] rgb = ratio_to_rgb(i) ;
//                    rTable[i] = rgb[0] ;
//                    gTable[i] = rgb[1] ;
//                    bTable[i] = rgb[2] ;
//            }
//        }
//    }
    
        private static void compute_hsi_table() {
        if(rTable == null || gTable == null || bTable == null ) {
            float[][] tables = getHsiTables();
            rTable = tables[0];
            gTable = tables[1];
            bTable = tables[2];
        }
    }
    
    public static float[][] getHsiTables(){
        float[] rTable = new float[65536] ;
        float[] gTable = new float[65536] ;
        float[] bTable = new float[65536] ;
        for( int i = 0 ; i < 65536 ; i++ ){
                float [] rgb = ratio_to_rgb(i) ;
                rTable[i] = rgb[0] ;
                gTable[i] = rgb[1] ;
                bTable[i] = rgb[2] ;
        }
        return new float[][]{rTable,gTable,bTable};
    }
}

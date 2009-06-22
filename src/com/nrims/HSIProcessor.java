/*
 * HSIProcessor.java
 *
 * Created on May 4, 2006, 10:28 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.nrims;

import ij.* ;
import java.awt.* ;
import java.util.ArrayList;

public class HSIProcessor implements Runnable {
    
    /**
     * Creates a new instance of HSIProcessor
     */
    public HSIProcessor(MimsPlus hsiImage) {
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
    private boolean isSum = false;
    private boolean isWindow = false;
    private int windowSize = 0;

    private float[] fixedNumPixels = null;
    private float[] fixedDenPixels = null;

    public void setProps(HSIProps props) {
        if(hsiImage == null) return ;
        
        MimsPlus numerator = hsiImage.getUI().getMassImage(props.getNumMass());
        MimsPlus denominator = hsiImage.getUI().getMassImage(props.getDenMass());
        if(numerator == null || denominator == null) return ;
        isSum = props.getIsSum();
        isWindow = props.getIsWindow();
        windowSize = props.getWindowSize();
        
        if(isSum&&( hsiImage.getNumeratorSum()==null || hsiImage.getDenominatorSum()==null)) {
                    hsiImage.initializeHSISum(props);
        }
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
        
       // initialize stuff.
        MimsPlus numerator = null , denominator = null, ratio = null ;
        MimsPlus [] ml = hsiImage.getUI().getMassImages() ;
        
        try {
            if( hsiImage == null ) { fThread = null ; return ; }
        
            // Thread stuff.
            while( hsiImage.lockSilently() == false ) {
                if(fThread == null || fThread.interrupted()) {
                    return ;
                }
            }
            
            // Get numerator information.            
            numerator = hsiImage.internalNumerator;
            if( numerator == null ) { 
                fThread = null ;
                hsiImage.unlock();
                return ;
            }

            // Get denominator information.
            denominator = hsiImage.internalDenominator;
            if( denominator == null ) { 
                fThread = null ;
                hsiImage.unlock();
                return ;
            }

            ratio = hsiImage.internalRatio;
 /*
            if( ratio == null ) {
                fThread = null ;
                hsiImage.unlock();
                return ;
            }
*/
            // More threading stuff...
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

            /*
            //need thread stuff?
            while( ratio.lockSilently() == false ) {
                if(fThread == null || fThread.interrupted()) {
                    hsiImage.unlock();
                    numerator.unlock();
                    denominator.unlock();
                    return ;
                }
            }
*/
            float[] numPixels = (float[]) numerator.getProcessor().getPixels();
            float[] denPixels = (float[]) denominator.getProcessor().getPixels();
            float[] ratioPixels = (float[]) ratio.getProcessor().getPixels();
            double numMax = numerator.getProcessor().getMax();
            double numMin =numerator.getProcessor().getMin();
            double denMax = denominator.getProcessor().getMax();
            double denMin = denominator.getProcessor().getMin();



            /* DELETE
            // Get numerator and denominator pixels.
            short[] tempnumPixels = (short[]) numerator.getProcessor().getPixels();
            short[] tempdenPixels = (short[]) denominator.getProcessor().getPixels();
            
            // Convert from shorts to floats.
            float[] numPixels = new float[tempnumPixels.length];
            float[] denPixels = new float[tempdenPixels.length];
            for(int i = 0; i<numPixels.length; i++) numPixels[i] =  tempnumPixels[i] & 0xffff ;
            for(int i = 0; i<denPixels.length; i++) denPixels[i] =  tempdenPixels[i] & 0xffff ;

            // initializing...
            float fixedNumMax = 0;
            float fixedNumMin = java.lang.Float.MAX_VALUE;
            float fixedDenMax = 0;
            float fixedDenMin = java.lang.Float.MAX_VALUE;



            // If sum image, get data. No need to update later because it is static.
            if (isSum || isWindow) {

                if (isSum && (fixedNumPixels == null || fixedDenPixels == null)) {
                    // Get the numerator sum and denominator sum. Only need to do this once.
                    fixedNumPixels = (float[]) hsiImage.getNumeratorSum().getProcessor().getPixels();
                    fixedDenPixels = (float[]) hsiImage.getDenominatorSum().getProcessor().getPixels();
                }

                // Window need updating.
                if (isWindow) {
                    try {

                        // The numerator sum and denominator sum for each update.
                        UI ui = hsiImage.getUI();
                        int currentSlice = ui.getMassImage(0).getCurrentSlice();
                        //windowSize = hsiProps.getWindowSize();
                        int lb = currentSlice - windowSize;
                        int ub = currentSlice + windowSize;
                        ArrayList<Integer> list = new ArrayList<Integer>();
                        for (int i = lb; i <= ub; i++) {
                            list.add(i);
                        }
                        fixedNumPixels = (float[]) ui.computeSum(ui.getMassImage(hsiProps.getNumMass()), false, list).getProcessor().getPixels();
                        fixedDenPixels = (float[]) ui.computeSum(ui.getMassImage(hsiProps.getDenMass()), false, list).getProcessor().getPixels();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                //need to scale and cast to short for transparency etc...
                for (int i = 0; i < fixedNumPixels.length; i++) {
                    if (fixedNumPixels[i] > fixedNumMax) {
                        fixedNumMax = fixedNumPixels[i];
                    }
                    if (fixedNumPixels[i] < fixedNumMin) {
                        fixedNumMin = fixedNumPixels[i];
                    }
                }
                for (int i = 0; i < fixedDenPixels.length; i++) {
                    if (fixedDenPixels[i] > fixedDenMax) {
                        fixedDenMax = fixedDenPixels[i];
                    }
                    if (fixedDenPixels[i] < fixedDenMin) {
                        fixedDenMin = fixedDenPixels[i];
                    }
                }

                for (int i = 0; i < fixedNumPixels.length; i++) {
                    numPixels[i] = fixedNumPixels[i];
                }
                for (int i = 0; i < fixedNumPixels.length; i++) {
                    denPixels[i] = fixedDenPixels[i];
                }

            }

            */

            int [] hsiPixels = (int []) hsiImage.getProcessor().getPixels() ;
            int rgbMax = hsiProps.getMaxRGB() ;
            int rgbMin = hsiProps.getMinRGB() ;
            if(rgbMax == rgbMin) rgbMax++ ;
            double rgbGain = 255.0 / (double)(rgbMax-rgbMin);

            /*
            double numMax = 0.0;
            double numMin = 0.0;
            if(!isSum ) {
                numMax = numerator.getProcessor().getMax() ;
                numMin = numerator.getProcessor().getMin() ;
            } else {
            //if ( isSum ) {
                numMax = hsiImage.getNumeratorSum().getProcessor().getMax();
                numMin = hsiImage.getNumeratorSum().getProcessor().getMin();
            }
/*            if( isWindow) {
                numMax = fixedNumMax;
                numMin = fixedNumMin;
            }

            

            double denMax = 0.0;
            double denMin = 0.0;
            if(!isSum ) {
                denMax = denominator.getProcessor().getMax() ;
                denMin = denominator.getProcessor().getMin() ;
            } else {
            //if ( isSum) {
                denMax = hsiImage.getDenominatorSum().getProcessor().getMax();
                denMin = hsiImage.getDenominatorSum().getProcessor().getMin();
            }
   /*         if (isWindow) {
                denMax = fixedNumMax;
                denMin = fixedNumMin;
            }

*/
            double numGain = numMax > numMin ? 255.0 / ( numMax - numMin ) : 1.0 ;
            double denGain = denMax > denMin ? 255.0 / ( denMax - denMin ) : 1.0 ;
            double maxRatio = hsiProps.getMaxRatio() ;
            double minRatio = hsiProps.getMinRatio() ;
            int showLabels = hsiProps.getLabelMethod() ;
            
            if(maxRatio <= 0.0) maxRatio = 1.0 ;
            double rScale = 65535.0 / (maxRatio -minRatio);
            int numThreshold = hsiProps.getMinNum() ;
            int denThreshold = hsiProps.getMinDen() ;
            int transparency = hsiProps.getTransparency() ;

            /*
            //float ratioPixels[] = new float[numPixels.length];
            //MimsPlus internalRatio = this.hsiImage.getInternalRatio();

            //not the right way to do this
             if (isSum || isWindow) {
                for (int i = 0; i < ratioPixels.length; i++) {
                    if (denPixels[i] != 0) {
                       ratioPixels[i] = hsiProps.getRatioScaleFactor() * ( fixedNumPixels[i]) / (fixedDenPixels[i]);
                        
                    } else {
                        ratioPixels[i] = 0;
                    }
                }
            } else {
                for (int i = 0; i < ratioPixels.length; i++) {
                    if (denPixels[i] != 0) {
                        ratioPixels[i] = hsiProps.getRatioScaleFactor() * ((float) numPixels[i]) / ((float) denPixels[i]);
                    } else {
                        ratioPixels[i] = 0;
                    }
                }
            }
            */

            //Place holder for transformed pixels...
            float[] transformedPixels = ratioPixels;
            if(transformedPixels == null) {
                transformedPixels = ratioPixels;
            }
            //if using non-ratio values, ie percent turnover
            if(hsiProps.getTransform()) {
                transformedPixels = this.turnoverTransform(transformedPixels, hsiProps.getReferenceRatio(), hsiProps.getBackgroundRatio(), hsiProps.getRatioScaleFactor());
           }

            /*
            if (medianize && internalRatio != null) {
                double r = this.hsiImage.getUI().getHSIView().getMedianRadius();
                transformedPixels = hsiImage.medianizeInternalRatio(r);

                //old
                /*
                  ij.process.FloatProcessor tempProc = new ij.process.FloatProcessor(internalRatio.getProcessor().getWidth(),internalRatio.getProcessor().getHeight()); //change
                tempProc.setPixels(ratioPixels);
                ij.plugin.filter.RankFilters rfilter = new ij.plugin.filter.RankFilters();
                double r = this.hsiImage.getUI().getHSIView().getMedianRadius();
                rfilter.rank(tempProc, r, rfilter.MEDIAN);

                transformedPixels = (float[]) tempProc.getPixels();
                internalRatio.getProcessor().setPixels(transformedPixels);
                //

            }

            if (!medianize && internalRatio != null) {
                internalRatio.getProcessor().setPixels(ratioPixels);
            }

            
            //if using non-ratio values, ie percent turnover
           // if(hsiProps.getTransform()) {
           //     transformedPixels = this.turnoverTransform(transformedPixels, hsiProps.getReferenceRatio(), hsiProps.getBackgroundRatio(), hsiProps.getRatioScaleFactor());
           // }

            */


            //scale num/den/ratio values to 16bit
            //do we really need to do this?


            //Conversion of num/den/ratio values to RGB
            //These values are scaled to 16bit
            for(int offset = 0 ; offset < numPixels.length && fThread != null ; offset++ ) {


                float numValue = numPixels[offset];
                float denValue = denPixels[offset];

                if( numValue > numThreshold && denValue > denThreshold ){
            
                    //original
                    //float ratio = hsiProps.getRatioScaleFactor()*((float)numValue / (float)denValue );
                    float ratioval = transformedPixels[offset];
                    
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
                    if( ratioval > minRatio) {
                        if( ratioval < maxRatio ) {
                            iratio = (int)( (ratioval-minRatio) * rScale ) ;
                            if(iratio < 0) iratio = 0 ;
                            else if(iratio > 65535) iratio = 65535 ;                           
                        }
                        else iratio = 65535 ;
                    } else iratio = 0;
                    
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
            //Scale bar colors
            if( numPixels.length != hsiPixels.length ) {

                double dScale = 65535.0F;
                double dRatio = 0.0;;
                double dDelta = ( dScale )
                                / (double) hsiImage.getWidth() ;

                for( int x=0 ; x<hsiImage.getWidth() ; x++ )
                {
                    int iratio = (int)dRatio ;
                    if(iratio<0) { iratio=0; } else if(iratio>65535) { iratio = 65535; }
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
                /* ORIGINAL, but incorrect?
                 * Why was this trying to do anything?
                 * The scale bar should never change, just the labels
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
                    */
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

    public float[] turnoverTransform(float[] ratiopixels, float ref, float bg, int sf) {
        float[] tpixels = new float[ratiopixels.length];
        for(int i =0; i< tpixels.length; i++) {
            tpixels[i] = turnoverTransform(ratiopixels[i], ref, bg, sf);
        }
        return tpixels;
    }

    public float turnoverTransform(float ratio, float ref, float bg, int sf) {
        float output=0;
        if(bg==ref) return output;
        float runscaled = ratio/sf;
        
        output = ( (runscaled-bg) / (ref-bg) )*( (1+ref) / (1+runscaled) );
        output = output * sf;
        return output;
    }

    //TODO: delete this crap
    //broken run() code
    public void doNotRun( ) {

       // initialize stuff.
        MimsPlus numerator = null , denominator = null ;
        MimsPlus [] ml = hsiImage.getUI().getMassImages() ;

        try {
            if( hsiImage == null ) { fThread = null ; return ; }

            // Thread stuff.
            while( hsiImage.lockSilently() == false ) {
                if(fThread == null || fThread.interrupted()) {
                    return ;
                }
            }

            // Get numerator information.
            numerator = ml[hsiProps.getNumMass()];
            if( numerator == null ) {
                fThread = null ;
                hsiImage.unlock();
                return ;
            }

            // Get denominator information.
            denominator = ml[hsiProps.getDenMass()];
            if( denominator == null ) {
                fThread = null ;
                hsiImage.unlock();
                return ;
            }

            // More threading stuff...
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

            // Get numerator and denominator pixels.
            short[] tempnumPixels = (short[]) numerator.getProcessor().getPixels();
            short[] tempdenPixels = (short[]) denominator.getProcessor().getPixels();

            // Convert from shorts to floats.
            float[] numPixels = new float[tempnumPixels.length];
            float[] denPixels = new float[tempdenPixels.length];
            for(int i = 0; i<numPixels.length; i++) numPixels[i] =  tempnumPixels[i] & 0xffff ;
            for(int i = 0; i<denPixels.length; i++) denPixels[i] =  tempdenPixels[i] & 0xffff ;

            // initializing...
            float fixedNumMax = 0;
            float fixedNumMin = java.lang.Float.MAX_VALUE;
            float fixedDenMax = 0;
            float fixedDenMin = java.lang.Float.MAX_VALUE;

            // If sum image, get data. No need to update later because it is static.
            if (isSum || isWindow) {

                if (isSum && (fixedNumPixels == null || fixedDenPixels == null)) {
                    // Get the numerator sum and denominator sum. Only need to do this once.
                    fixedNumPixels = (float[]) hsiImage.getNumeratorSum().getProcessor().getPixels();
                    fixedDenPixels = (float[]) hsiImage.getDenominatorSum().getProcessor().getPixels();
                }

                // Window need updating.
                if (isWindow) {
                    try {

                        // The numerator sum and denominator sum for each update.
                        UI ui = hsiImage.getUI();
                        int currentSlice = ui.getMassImage(0).getCurrentSlice();
                        //windowSize = hsiProps.getWindowSize();
                        int lb = currentSlice - windowSize;
                        int ub = currentSlice + windowSize;
                        ArrayList<Integer> list = new ArrayList<Integer>();
                        for (int i = lb; i <= ub; i++) {
                            list.add(i);
                        }
                        fixedNumPixels = (float[]) ui.computeSum(ui.getMassImage(hsiProps.getNumMass()), false, list).getProcessor().getPixels();
                        fixedDenPixels = (float[]) ui.computeSum(ui.getMassImage(hsiProps.getDenMass()), false, list).getProcessor().getPixels();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                //need to scale and cast to short for transparency etc...
                for (int i = 0; i < fixedNumPixels.length; i++) {
                    if (fixedNumPixels[i] > fixedNumMax) {
                        fixedNumMax = fixedNumPixels[i];
                    }
                    if (fixedNumPixels[i] < fixedNumMin) {
                        fixedNumMin = fixedNumPixels[i];
                    }
                }
                for (int i = 0; i < fixedDenPixels.length; i++) {
                    if (fixedDenPixels[i] > fixedDenMax) {
                        fixedDenMax = fixedDenPixels[i];
                    }
                    if (fixedDenPixels[i] < fixedDenMin) {
                        fixedDenMin = fixedDenPixels[i];
                    }
                }

                for (int i = 0; i < fixedNumPixels.length; i++) {
                    numPixels[i] = fixedNumPixels[i];
                }
                for (int i = 0; i < fixedNumPixels.length; i++) {
                    denPixels[i] = fixedDenPixels[i];
                }

            }

            int [] hsiPixels = (int []) hsiImage.getProcessor().getPixels() ;
            int rgbMax = hsiProps.getMaxRGB() ;
            int rgbMin = hsiProps.getMinRGB() ;
            if(rgbMax == rgbMin) rgbMax++ ;
            double rgbGain = 255.0 / (double)(rgbMax-rgbMin);

            double numMax = 0.0;
            double numMin = 0.0;
            if(!isSum ) {
                numMax = numerator.getProcessor().getMax() ;
                numMin = numerator.getProcessor().getMin() ;
            } else {
            //if ( isSum ) {
                numMax = hsiImage.getNumeratorSum().getProcessor().getMax();
                numMin = hsiImage.getNumeratorSum().getProcessor().getMin();
            }
/*            if( isWindow) {
                numMax = fixedNumMax;
                numMin = fixedNumMin;
            }
*/
            double numGain = numMax > numMin ? 255.0 / ( numMax - numMin ) : 1.0 ;

            double denMax = 0.0;
            double denMin = 0.0;
            if(!isSum ) {
                denMax = denominator.getProcessor().getMax() ;
                denMin = denominator.getProcessor().getMin() ;
            } else {
            //if ( isSum) {
                denMax = hsiImage.getDenominatorSum().getProcessor().getMax();
                denMin = hsiImage.getDenominatorSum().getProcessor().getMin();
            }
   /*         if (isWindow) {
                denMax = fixedNumMax;
                denMin = fixedNumMin;
            }
*/

            double denGain = denMax > denMin ? 255.0 / ( denMax - denMin ) : 1.0 ;
            double maxRatio = hsiProps.getMaxRatio() ;
            double minRatio = hsiProps.getMinRatio() ;
            int showLabels = hsiProps.getLabelMethod() ;

            if(maxRatio <= 0.0) maxRatio = 1.0 ;
            double rScale = 65535.0 / (maxRatio -minRatio);
            int numThreshold = hsiProps.getMinNum() ;
            int denThreshold = hsiProps.getMinDen() ;
            int transparency = hsiProps.getTransparency() ;

            float ratioPixels[] = new float[numPixels.length];
            MimsPlus internalRatio = this.hsiImage.getInternalRatio();

            //not the right way to do this
             if (isSum || isWindow) {
                for (int i = 0; i < ratioPixels.length; i++) {
                    if (denPixels[i] != 0) {
                       ratioPixels[i] = hsiProps.getRatioScaleFactor() * ( fixedNumPixels[i]) / (fixedDenPixels[i]);

                    } else {
                        ratioPixels[i] = 0;
                    }
                }
            } else {
                for (int i = 0; i < ratioPixels.length; i++) {
                    if (denPixels[i] != 0) {
                        ratioPixels[i] = hsiProps.getRatioScaleFactor() * ((float) numPixels[i]) / ((float) denPixels[i]);
                    } else {
                        ratioPixels[i] = 0;
                    }
                }
            }

            //Place holder for transformed pixels...
            float[] transformedPixels = ratioPixels;

            if (medianize && internalRatio != null) {
                double r = this.hsiImage.getUI().getHSIView().getMedianRadius();
                transformedPixels = hsiImage.medianizeInternalRatio(r);

                //old
                /*
                  ij.process.FloatProcessor tempProc = new ij.process.FloatProcessor(internalRatio.getProcessor().getWidth(),internalRatio.getProcessor().getHeight()); //change
                tempProc.setPixels(ratioPixels);
                ij.plugin.filter.RankFilters rfilter = new ij.plugin.filter.RankFilters();
                double r = this.hsiImage.getUI().getHSIView().getMedianRadius();
                rfilter.rank(tempProc, r, rfilter.MEDIAN);

                transformedPixels = (float[]) tempProc.getPixels();
                internalRatio.getProcessor().setPixels(transformedPixels);
                 */

            }

            if (!medianize && internalRatio != null) {
                internalRatio.getProcessor().setPixels(ratioPixels);
            }

            if(transformedPixels == null) {
                transformedPixels = ratioPixels;
            }

            //if using non-ratio values, ie percent turnover
           // if(hsiProps.getTransform()) {
           //     transformedPixels = this.turnoverTransform(transformedPixels, hsiProps.getReferenceRatio(), hsiProps.getBackgroundRatio(), hsiProps.getRatioScaleFactor());
           // }




            //Conversion of num/den/ratio values to RGB
            //These values are scaled to 16bit
            for(int offset = 0 ; offset < numPixels.length && fThread != null ; offset++ ) {


                float numValue = numPixels[offset];
                float denValue = denPixels[offset];

                if( numValue > numThreshold && denValue > denThreshold ){

                    //original
                    //float ratio = hsiProps.getRatioScaleFactor()*((float)numValue / (float)denValue );
                    float ratio = transformedPixels[offset];

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
                    } else iratio = 0;

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
            //Scale bar colors
            if( numPixels.length != hsiPixels.length ) {

                double dScale = 65535.0F;
                double dRatio = 0.0;;
                double dDelta = ( dScale )
                                / (double) hsiImage.getWidth() ;

                for( int x=0 ; x<hsiImage.getWidth() ; x++ )
                {
                    int iratio = (int)dRatio ;
                    if(iratio<0) { iratio=0; } else if(iratio>65535) { iratio = 65535; }
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
                /* ORIGINAL, but incorrect?
                 * Why was this trying to do anything?
                 * The scale bar should never change, just the labels
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
                    */
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


}

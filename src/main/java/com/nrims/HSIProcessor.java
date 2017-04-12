package com.nrims;

import ij.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A worker class that generates the actual values for how the pixels are calculated and how the image displayed.
 * Although the pixels are generated using standard techniques for calculating HSI values, the HSI images in the
 * MimsPlus plugin also store ratio (or percent turnover) values on a pixel by pixel basis. The user decides if the
 * background data (displayed in the status bar when the mouse is moved) is calculated in terms of a ratio value or
 * percent turnover. Regardless of the background data, the rgb values per pixel will be the same. For more information
 * on HSI images, see:
 *
 * http://en.wikipedia.org/wiki/HSI_color_space
 *
 * @author cpoczatek
 */
public class HSIProcessor implements Runnable {

    /**
     * Creates a new instance of HSIProcessor
     * 
     * @param hsiImage  MimsPlus instance for the HSI image.
     */
    public HSIProcessor(MimsPlus hsiImage) {
        this.hsiImage = hsiImage;
        compute_hsi_table();
    }

    public void finalize() {
        hsiImage = null;
        hsiProps = null;
    }

    private MimsPlus hsiImage = null;
    private HSIProps hsiProps = null;
    private Thread fThread = null;

    private static float[] rTable = null;
    private static float[] gTable = null;
    private static float[] bTable = null;

    private static final double S6_6 = Math.sqrt(6.0) / 6.0;
    private static final double S6_3 = Math.sqrt(6.0) / 3.0;
    private static final double S6_2 = Math.sqrt(6.0) / 2.0;
    private static final double FULLSCALE = 65535.0 / (2.0 * Math.PI);
    private static final int MAXNUMDEN = 0;
    private static final int NUMERATOR = 1;
    private static final int DENOMINATOR = 2;
    private static final int MINNUMDEN = 3;
    private static final int MEANNUMDEN = 4;
    private static final int SUMNUMDEN = 5;
    private static final int RMSNUMDEN = 6;

    /**
     * Sets the <code>HSIProps</code> object for this processor.
     *
     * @param props and <code>HSIProps</code> object.
     */
    public void setProps(HSIProps props) {
        if (hsiImage == null) {
            return;
        }

        MimsPlus numerator = hsiImage.getUI().getMassImage(props.getNumMassIdx());
        MimsPlus denominator = hsiImage.getUI().getMassImage(props.getDenMassIdx());
        if (numerator == null || denominator == null) {
            return;
        }
        hsiProps = props;
        start();
    }

    /**
     * Gets the <code>HSIProps</code> object for this processor.
     *
     * @return the <code>HSIProps</code> object.
     */
    public HSIProps getHSIProps() {
        return hsiProps;
    }

    /**
     * Kicks off the thread.
     */
    private synchronized void start() {
        if (fThread != null) {
            stop();
        }
        try {
            fThread = new Thread(this);
            fThread.setPriority(fThread.NORM_PRIORITY);
            fThread.setContextClassLoader(
                    Thread.currentThread().getContextClassLoader());
            try {
                fThread.start();
            } catch (IllegalThreadStateException x) {
                IJ.log(x.toString());
            }
        } catch (NullPointerException xn) {
        }
    }

    /**
     * Stops the thread.
     */
    private void stop() {
        if (fThread != null) {
            fThread.interrupt();
            fThread = null;
        }
    }

    /**
     * Returns <code>true</code> if the thread is still running.
     *
     * @return <code>true</code> if the thread is still running, otherwise <code>false</code>.
     */
    public boolean isRunning() {
        if (fThread == null) {
            return false;
        }
        return fThread.isAlive();
    }

    /**
     * The worker method that determines the actual HSI values for the image.
     */
    public void run() {

        // initialize stuff.
        MimsPlus numerator = null, denominator = null, ratio = null;
        MimsPlus[] ml = hsiImage.getUI().getMassImages();

        try {
            if (hsiImage == null) {
                fThread = null;
                return;
            }

            // Thread stuff.
            while (hsiImage.lockSilently() == false) {
                if (fThread == null || fThread.isInterrupted()) {
                    return;
                }
            }

            // Get numerator information.            
            numerator = hsiImage.internalNumerator;
            if (numerator == null) {
                fThread = null;
                hsiImage.unlock();
                return;
            }

            // Get denominator information.
            denominator = hsiImage.internalDenominator;
            if (denominator == null) {
                fThread = null;
                hsiImage.unlock();
                return;
            }

            ratio = hsiImage.internalRatio_filtered;

            // More threading stuff...
            while (numerator.lockSilently() == false) {
                if (fThread == null || fThread.isInterrupted()) {
                    hsiImage.unlock();
                    return;
                }
            }
            while (denominator.lockSilently() == false) {
                if (fThread == null || fThread.isInterrupted()) {
                    hsiImage.unlock();
                    numerator.unlock();
                    return;
                }
            }

            float[] numPixels = (float[]) numerator.getProcessor().getPixels();
            float[] denPixels = (float[]) denominator.getProcessor().getPixels();
            float[] ratioPixels = (float[]) ratio.getProcessor().getPixels();
            double numMax = numerator.getProcessor().getMax();
            double numMin = numerator.getProcessor().getMin();
            double denMax = denominator.getProcessor().getMax();
            double denMin = denominator.getProcessor().getMin();

            int[] hsiPixels;
            try {
                hsiPixels = (int[]) hsiImage.getProcessor().getPixels();
            } catch (Exception e) {
                return;
            }

            int rgbMax = hsiProps.getMaxRGB();
            int rgbMin = hsiProps.getMinRGB();
            if (rgbMax == rgbMin) {
                rgbMax++;
            }
            double rgbGain = 255.0 / (double) (rgbMax - rgbMin);

            double numGain = numMax > numMin ? 255.0 / (numMax - numMin) : 1.0;
            double denGain = denMax > denMin ? 255.0 / (denMax - denMin) : 1.0;
            double maxRatio = hsiProps.getMaxRatio();
            double minRatio = hsiProps.getMinRatio();
            int showLabels = hsiProps.getLabelMethod();

            if (maxRatio <= 0.0) {
                maxRatio = 1.0;
            }
            double rScale = 65535.0 / (maxRatio - minRatio);
            int numThreshold = hsiProps.getNumThreshold();
            int denThreshold = hsiProps.getDenThreshold();
            int transparency = hsiProps.getTransparency();

            //Place holder for transformed pixels...
            float[] transformedPixels = ratioPixels;

            for (int offset = 0; offset < numPixels.length && fThread != null; offset++) {

                float numValue = numPixels[offset];
                float denValue = denPixels[offset];

                if (numValue > numThreshold && denValue > denThreshold) {

                    //original
                    //float ratio = hsiProps.getRatioScaleFactor()*((float)numValue / (float)denValue );
                    float ratioval = transformedPixels[offset];

                    int numOut = (int) (numGain * (float) (numValue - (int) numMin));
                    int denOut = (int) (denGain * (float) (denValue - (int) denMin));

                    int outValue, r, g, b;

                    switch (transparency) {
                        default:
                        case MAXNUMDEN:
                            outValue = numOut > denOut ? numOut : denOut;
                            break;
                        case NUMERATOR:
                            outValue = numOut;
                            break;
                        case DENOMINATOR:
                            outValue = denOut;
                            break;
                        case MINNUMDEN:
                            outValue = numOut < denOut ? numOut : denOut;
                            break;
                        case MEANNUMDEN:
                            outValue = (numOut + denOut) / 2;
                            break;
                        case SUMNUMDEN:
                            outValue = numOut + denOut;
                            break;
                        case RMSNUMDEN:
                            outValue = (int) Math.sqrt(numOut * numOut
                                    + denOut * denOut);
                            break;
                    }

                    outValue = (int) ((double) (outValue - rgbMin) * rgbGain);
                    if (outValue < 0) {
                        outValue = 0;
                    } else if (outValue > 255) {
                        outValue = 255;
                    }

                    int iratio = 0;
                    if (ratioval > minRatio) {
                        if (ratioval < maxRatio) {
                            iratio = (int) ((ratioval - minRatio) * rScale);
                            if (iratio < 0) {
                                iratio = 0;
                            } else if (iratio > 65535) {
                                iratio = 65535;
                            }
                        } else {
                            iratio = 65535;
                        }
                    } else {
                        iratio = 0;
                    }

                    r = (int) (rTable[iratio] * outValue) << 16;
                    g = (int) (gTable[iratio] * outValue) << 8;
                    b = (int) (bTable[iratio] * outValue);

                    hsiPixels[offset] = r + g + b;

                } else {
                    hsiPixels[offset] = 0;
                }
                // System.out.print(hsiPixels[offset] + " ");
                if (fThread == null || fThread.isInterrupted()) {
                    fThread = null;
                    hsiImage.unlock();
                    denominator.unlock();
                    numerator.unlock();
                    return;
                }
            }
            //Scale bar colors
            if (numPixels.length != hsiPixels.length) {
                double dScale = 65535.0F;
                double dRatio = 0.0;;
                double dDelta = (dScale) / (double) hsiImage.getWidth();

                for (int x = 0; x < hsiImage.getWidth(); x++) {
                    int iratio = (int) dRatio;
                    if (iratio < 0) {
                        iratio = 0;
                    } else if (iratio > 65535) {
                        iratio = 65535;
                    }
                    int r = (int) (rTable[iratio] * 255.0);
                    int g = (int) (gTable[iratio] * 255.0);
                    int b = (int) (bTable[iratio] * 255.0);

                    dRatio += dDelta;

                    int offset, y;
                    for (offset = numPixels.length + x, y = 0;
                            y < 16;
                            y++, offset += hsiImage.getWidth()) {
                        hsiPixels[offset] = ((r & 0xff) << 16) + ((g & 0xff) << 8) + (b & 0xff);
                    }
                }

                if (showLabels > 1) { // Add the labels..
                    hsiImage.getProcessor().setColor(Color.WHITE);

                    // Min label.
                    hsiImage.getProcessor().moveTo(0, hsiImage.getHeight());
                    String label = IJ.d2s(minRatio, 0);
                    hsiImage.getProcessor().drawString(label);

                    //Add center label.
                    label = IJ.d2s(minRatio + ((maxRatio - minRatio) / 2), 0);
                    int offset = label.length() * 4;
                    hsiImage.getProcessor().moveTo(java.lang.Math.round(hsiImage.getWidth() / 2 - offset), hsiImage.getHeight());
                    hsiImage.getProcessor().drawString(label);

                    // Max label.
                    label = IJ.d2s(maxRatio, 0);
                    offset = label.length() * 8;
                    hsiImage.getProcessor().moveTo(hsiImage.getWidth() - offset, hsiImage.getHeight());
                    hsiImage.getProcessor().drawString(label);

                }
            }

            denominator.unlock();
            numerator.unlock();
            hsiImage.unlock();
            hsiImage.updateAndRepaintWindow();
            fThread = null;

        } catch (Exception x) {
            hsiImage.unlock();
            if (denominator != null) {
                denominator.unlock();
            }
            if (numerator != null) {
                numerator.unlock();
            }
            fThread = null;
            IJ.log(x.toString());
            x.printStackTrace();
        }

    }

    /**
     * Converts a ratio value to an rgb array.
     */
    private static float[] ratio_to_rgb(int ratio) {
        float[] rgb = new float[3];
        double i0, i1, i2, o0, o1, o2;
        double g2h;

        i0 = 160.0;
        i1 = ratio;
        i2 = 128.0;
        g2h = (i1 / FULLSCALE) - Math.PI;
        i1 = i2 * Math.cos(g2h);
        i2 *= Math.sin(g2h);
        o0 = i0 - i1 * S6_6 + i2 * S6_2;
        o1 = i0 - i1 * S6_6 - i2 * S6_2;
        o2 = i0 + i1 * S6_3;
        if (o0 < 0) {
            o0 = 0;
        }
        if (o0 > 255.0) {
            o0 = 255.0;
        }
        if (o1 < 0) {
            o1 = 0;
        }
        if (o1 > 255.0) {
            o1 = 255.0;
        }
        if (o2 < 0) {
            o2 = 0;
        }
        if (o2 > 255.0) {
            o2 = 255.0;
        }

        if (ratio < 16384) {
            rgb[0] = (float) o0 * (float) ratio / 4177920.0F;
        } else {
            rgb[0] = (float) o0 / 255.0F;
        }
        rgb[1] = (float) o2 / 255.0F;
        rgb[2] = (float) o1 / 255.0F;
        return rgb;
    }

    private static void compute_hsi_table() {
        if (rTable == null || gTable == null || bTable == null) {
            float[][] tables = getHsiTables();
            rTable = tables[0];
            gTable = tables[1];
            bTable = tables[2];
        }
    }

    public static float[][] getHsiTables() {
        float[] rTable = new float[65536];
        float[] gTable = new float[65536];
        float[] bTable = new float[65536];
        for (int i = 0; i < 65536; i++) {
            float[] rgb = ratio_to_rgb(i);
            rTable[i] = rgb[0];
            gTable[i] = rgb[1];
            bTable[i] = rgb[2];
        }
        return new float[][]{rTable, gTable, bTable};
    }

    /**
     * Converts an array of ratio values to an array of "percent turnover" values. Percent turnover is a function of the
     * ratio value, two reference points and the scale factor.
     * <ul>
     * <li>The first reference point is the ratio value at 100% turnover.
     * <li>The second reference point is the ratio value of the background.
     * </ul>
     * Currently, the values for the two reference points is hard-coded. In the near future they will be changed to be
     * user preferences.
     *
     * @param ratiopixels the array of ratio values.
     * @param ref the reference ratio value for 100% turnover.
     * @param bg the ratio value for the background.
     * @param sf the scale factor used when calculating the ratio values.
     * @return an array of "percent turnover".
     */
    public static float[] turnoverTransform(float[] ratiopixels, float ref, float bg, float sf) {
        float[] tpixels = new float[ratiopixels.length];
        for (int i = 0; i < tpixels.length; i++) {
            tpixels[i] = turnoverTransform(ratiopixels[i], ref, bg, sf);
        }
        return tpixels;
    }

    /**
     * Converts a ratio value to a "percent turnover" value. Percent turnover is a function of the ratio value, two
     * reference points and the scale factor.
     * <ul>
     * <li>The first reference point is the ratio value at 100% turnover.
     * <li>The second reference point is the ratio value of the background.
     * </ul>
     * Currently, the values for the two reference points is hard-coded. In the near future they will be changed to be
     * user preferences.
     *
     * @param ratio the ratio value.
     * @param ref the reference ratio value for 100% turnover.
     * @param bg the ratio value for the background.
     * @param sf the scale factor used when calculating the ratio value.
     * @return the "percent turnover".
     */
    public static float turnoverTransform(float ratio, float ref, float bg, float sf) {
        float output = 0;
        if (bg == ref) {
            return output;
        }
        float runscaled = ratio / sf;

        output = ((runscaled - bg) / (ref - bg)) * ((1 + ref) / (1 + runscaled));
        output = output * 100;
        return output;
    }

    /**
     * Converts a percent turnover value to a ratio value. Ratio value is a function of the percent turnover value, two
     * reference points and the scale factor.
     * <ul>
     * <li>The first reference point is the ratio value at 100% turnover.
     * <li>The second reference point is the ratio value of the background.
     * </ul>
     * Currently, the values for the two reference points is hard-coded. In the near future they will be changed to be
     * user preferences.
     *
     * @param turnover the percent turnover value.
     * @param ref the reference ratio value for 100% turnover.
     * @param bg the ratio value for the background.
     * @param sf the scale factor used to calculate the ratio values.
     * @return the ratio value.
     */
    public static float ratioTransform(float turnover, float ref, float bg, float sf) {
        float ratio = 0;
        float negOne = (float) -1.0000;
        float oneHundred = (float) 100.0;
        if (bg == ref) {
            return ratio;
        }
        float decimal = (float) turnover / oneHundred;

        ratio = (negOne * ref * bg + decimal * bg - bg - ref * decimal) / (decimal * ref - ref - bg * decimal - 1);
        ratio = Math.round(ratio * sf);
        return ratio;
    }

    // DJ: 08/06/2014
    // For testing purposes only.
    public void waitForThreadToFinish() {
        try {
            if (this.isRunning()) {
                this.fThread.join();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(HSIProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

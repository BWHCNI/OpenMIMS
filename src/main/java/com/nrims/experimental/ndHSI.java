/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.experimental;

import com.nrims.HSIProcessor;
import com.nrims.HSIProps;
import com.nrims.MimsPlus;
import com.nrims.UI;
import ij.process.FloatProcessor;
import ij.ImagePlus;
import ij.plugin.LutLoader;

/**
 *
 * @author cpoczatek
 */

/*
 * Example menu item code
 * com.nrims.experimental.ndHSI nd = new com.nrims.experimental.ndHSI();
 *  nd.generate_ndHSI();
 */
public class ndHSI {

    public void generate_ndHSI() {

        MimsPlus img;

        try {
            img = (MimsPlus) ij.WindowManager.getCurrentImage();
        } catch (Exception e) {
            return;
        }

        if (img.getMimsType() != MimsPlus.HSI_IMAGE) {
            return;
        }

        HSIProps props = img.getHSIProps();
        MimsPlus denimg = img.internalDenominator;
        MimsPlus numimg = img.internalNumerator;

        float[] denpixf = (float[]) denimg.getProcessor().getPixels();
        float[] numpixf = (float[]) numimg.getProcessor().getPixels();

        double[] denpixd = new double[denpixf.length];
        for (int i = 0; i < denpixd.length; i++) {
            denpixd[i] = (double) denpixf[i];
        }
        double[] numpixd = new double[numpixf.length];
        for (int i = 0; i < numpixd.length; i++) {
            numpixd[i] = (double) numpixf[i];
        }

        FloatProcessor densmooth = new FloatProcessor(denimg.getWidth(), denimg.getHeight(), denpixd);
        densmooth.smooth();
        denpixf = (float[]) densmooth.getPixels();
        FloatProcessor numsmooth = new FloatProcessor(numimg.getWidth(), numimg.getHeight(), numpixd);
        numsmooth.smooth();
        numpixf = (float[]) numsmooth.getPixels();

        double[] ndpix = new double[denpixd.length];
        for (int i = 0; i < ndpix.length; i++) {
            ndpix[i] = props.getRatioScaleFactor() * ((double) numpixf[i]) / ((double) denpixf[i]);
        }

        FloatProcessor ndproc = new FloatProcessor(denimg.getWidth(), denimg.getHeight(), ndpix);

        ImagePlus newHSI = new ImagePlus("test", ndproc);
        newHSI.show();

        //LutLoader ll = new LutLoader();
        ij.WindowManager.setCurrentWindow(newHSI.getWindow());
        //ll.run();
        newHSI.setDisplayRange(props.getMinRatio(), props.getMaxRatio());
        newHSI.updateAndDraw();
        //newHSI.

    }

    //incomplete and useless....
    public int[] getNeighborhoodPixels(MimsPlus img, int x, int y, double radius) {
        int[] pix = {0};
        if (radius == 0.5) {
            pix = new int[5];

        } else if (radius == 1.0) {
            pix = new int[9];
            int i = 0;
            for (int ix = x - 1; ix <= x + 1; ix++) {
                for (int iy = y - 1; iy <= y + 1; iy++) {

                }
            }

        }

        return pix;
    }
}

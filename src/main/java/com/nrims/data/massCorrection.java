/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.data;

import com.nrims.MimsPlus;
import com.nrims.SumProps;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.ImageStack;
import java.util.ArrayList;
import java.lang.Math;

/**
 *
 * @author cpoczatek
 */
public class massCorrection {

    private com.nrims.UI ui;
    static final float dt = 44 * (float) Math.pow(10, -9);
    static final float csc = (float) ((1 / 1.6) * Math.pow(10, 7));

    /**
     * Generic constructor
     *
     * @param ui the <code>UI</code> instance
     */
    public massCorrection(com.nrims.UI ui) {
        this.ui = ui;
    }

    /**
     * Performs deadtime correction on all mass images passed using dwelltime read from file header. WARNING: THIS
     * CHANGES DATA.
     *
     * @param massimgs an array of <code>MimsPlus</code> instances
     * @param dwelltime ?
     */
    public void performDeadTimeCorr(com.nrims.MimsPlus[] massimgs, float dwelltime) {

        //Assure float images
        if (!(massCorrection.check32bit(massimgs))) {
            this.forceFloatImages(massimgs);
        }

        int nplanes = massimgs[0].getNSlices();
        int nmasses = massimgs.length;

        //loop over all masses
        for (int m = 0; m < nmasses; m++) {
            for (int p = 0; p < nplanes; p++) {
                massimgs[m].setSlice(p + 1);
                float[] pix = (float[]) massimgs[m].getProcessor().getPixels();
                float[] newpix = new float[pix.length];

                //compute new pix
                for (int i = 0; i < pix.length; i++) {
                    newpix[i] = dtCorrect(pix[i], dwelltime);
                }

                massimgs[m].getProcessor().setPixels(newpix);
            }
        }

    }

    /**
     * Equation for dt corrected counts of a given pixel.
     *
     * @param counts pixel counts
     * @param dwelltime dwelltime
     * @return corCounts corrected counts for a given pixel
     */
    public static float dtCorrect(float counts, float dwelltime) {

        float corCounts = counts / (1 - (counts * dt) / (dwelltime));
        //return the corrected mass counts
        return corCounts;
    }

    /**
     * Perform QSA correction. Forces float image conversion and dt correction first.
     *
     * @param massimgs an array of <code>MimsPlus</code> instances
     * @param beta beta
     * @param dwelltime dwelltime
     * @param FCObj  descriptionTodo
     */
    public void performQSACorr(com.nrims.MimsPlus[] massimgs, float[] beta, float dwelltime, float FCObj) {
        //Assure float images
        if (!(massCorrection.check32bit(massimgs))) {
            this.forceFloatImages(massimgs);
        }

        //Do dt correction
        if (!ui.getOpener().isDTCorrected()) {
            this.performDeadTimeCorr(massimgs, dwelltime);
        }

        int nplanes = massimgs[0].getNSlices();
        int nmasses = massimgs.length;

        //loop over all masses
        for (int m = 0; m < nmasses; m++) {
            for (int p = 0; p < nplanes; p++) {
                massimgs[m].setSlice(p + 1);
                float[] pix = (float[]) massimgs[m].getProcessor().getPixels();
                float[] newpix = new float[pix.length];

                //compute new pix
                for (int i = 0; i < pix.length; i++) {
                    newpix[i] = QSACorrect(pix[i], beta[m], dwelltime, FCObj);
                }

                massimgs[m].getProcessor().setPixels(newpix);
            }
        }

    }

    /**
     * QSA correct individual dt corrected pixel
     *
     * @param dtcounts descriptionTodo
     * @param beta descriptionTodo
     * @param dwelltime descriptionTodo
     * @param FCObj descriptionTodo
     * @return  descriptionTodo
     */
    public static float QSACorrect(float dtcounts, float beta, float dwelltime, float FCObj) {
        float qsacorr = dtcounts * (1 + beta * yieldCorr(dtcounts, dwelltime, FCObj));
        return qsacorr;
    }

    /**
     * Correct the ion yield based on primary beam current
     *
     * @param dtcounts descriptionTodo
     * @param dwelltime descriptionTodo
     * @param FCObj descriptionTodo
     * @return  descriptionTodo
     */
    public static float yieldCorr(float dtcounts, float dwelltime, float FCObj) {
        float yieldcorr = yieldExperimental(dtcounts, dwelltime, FCObj) / (1 - (float) (0.5 * yieldExperimental(dtcounts, dwelltime, FCObj)));
        return yieldcorr;
    }

    /**
     * Return experimental ion yield
     *
     * @param dtcounts descriptionTodo
     * @param dwelltime descriptionTodo
     * @param FCObj descriptionTodo
     * @return  descriptionTodo
     */
    public static float yieldExperimental(float dtcounts, float dwelltime, float FCObj) {
        float yieldexp = dtcounts / CsNumber(dwelltime, FCObj);
        return yieldexp;
    }

    /**
     * Return scaled Cs number based on primary beam current
     *
     * @param dwelltime descriptionTodo
     * @param FCobj descriptionTodo
     * @return  descriptionTodo
     */
    public static float CsNumber(float dwelltime, float FCobj) {
        float csn = (dwelltime * FCobj * csc);
        return csn;
    }

    /**
     * Checks if -all- mass images are float images.
     *
     * @param massimgs descriptionTodo
     * @return  descriptionTodo
     */
    public static boolean check32bit(com.nrims.MimsPlus[] massimgs) {
        boolean is32b = true;
        for (int i = 0; i < massimgs.length; i++) {
            if (!(massimgs[i].getType() == MimsPlus.GRAY32)) {
                is32b = false;
            }
        }
        return is32b;
    }

    /**
     * Forces the conversion of passed mass images to 32bit. Needed to avoid loss of precision before doing corrections.
     *
     * @param massimgs descriptionTodo
     */
    public void forceFloatImages(com.nrims.MimsPlus[] massimgs) {
        int nplanes = massimgs[0].getNSlices();
        int nmasses = massimgs.length;
        MimsPlus[][] cp = new MimsPlus[nmasses][nplanes];
        int width = massimgs[0].getWidth();
        int height = massimgs[0].getHeight();

        // Set up the stacks.
        ImageStack[] is = new ImageStack[nmasses];
        for (int mindex = 0; mindex < nmasses; mindex++) {
            ImageStack iss = new ImageStack(width, height);
            is[mindex] = iss;
        }

        for (int idx = 1; idx <= nplanes; idx++) {

            ArrayList sumlist = new ArrayList<Integer>();
            sumlist.add(idx);
            // Generate the "sum" image for the plane
            for (int mindex = 0; mindex < nmasses; mindex++) {
                SumProps sumProps = new SumProps(ui.getMassImage(mindex).getMassIndex());
                cp[mindex][idx - 1] = new MimsPlus(ui, sumProps, sumlist);
            }
        }
        for (int i = 0; i < cp[0].length; i++) {
            for (int mindex = 0; mindex < nmasses; mindex++) {
                ImageProcessor ip = null;
                ip = new FloatProcessor(width, height);
                ip.setPixels(cp[mindex][i].getProcessor().getPixels());

                is[mindex].addSlice(null, ip);
            }
        }

        //set the stacks to new 32bit stacks
        for (int mindex = 0; mindex < nmasses; mindex++) {
            massimgs[mindex].setStack(null, is[mindex]);
        }
    }
}


/*
 * Example menuitem action methods:
 *
 *
private void dtMenuItemActionPerformed(java.awt.event.ActionEvent evt) {

    com.nrims.experimental.massCorrection masscor = new com.nrims.experimental.massCorrection(this);
    float dwell = 0;
    try {
        //dwell time in sec. (stored as ms in file)
        dwell = Float.parseFloat(this.getOpener().getDwellTime()) / 1000;
    } catch (Exception e) {
        ij.IJ.error("Error", "Cannot get dwelltime from file header.");
        return;
    }
    masscor.performDeadTimeCorr(this.getOpenMassImages(), dwell);
    //log what was done
    this.getMimsLog().Log("DT correction dwelltime (s) = "+dwell);
}

private void qsaMenuItemActionPerformed(java.awt.event.ActionEvent evt) {

    //generate a simple dialog to pass parrameters: beta[], FCO
    String betastring = "";
    String FCObjstrign = "";
    ij.gui.GenericDialog gd = new ij.gui.GenericDialog("QSA Correction");
    gd.addStringField("Betas:", betastring, 20);
    gd.addStringField("FC Objective (pA):", FCObjstrign, 20);
    gd.addMessage("Using dwelltime (ms): " + this.getOpener().getDwellTime());
    gd.showDialog();
    if (gd.wasCanceled()) {
        return;
    }
    betastring = gd.getNextString();
    FCObjstrign = gd.getNextString();
    String[] betasplit = betastring.split(",");
    if(betasplit.length != this.getOpenMassImages().length) {
        ij.IJ.error("Error", "Incorrect number of betas defined.");
        return;
    }

    //convert to float
    //and grab dwell time from opener metadata
    float[] betas = new float[betasplit.length];
    float FCObj = 0;
    float dwell = 0;
    try {
        for (int i = 0; i < betas.length; i++) {
            betas[i] = Float.parseFloat(betasplit[i]);
        }

        dwell = Float.parseFloat(this.getOpener().getDwellTime())/1000;
        FCObj = Float.parseFloat(FCObjstrign);

    } catch (Exception e) {
        ij.IJ.error("Error", "Mal-formed parameters (eg not a number).");
        return;
    }

    com.nrims.experimental.massCorrection masscor = new com.nrims.experimental.massCorrection(this);
    masscor.performQSACorr(this.getOpenMassImages(), betas, dwell, FCObj);

    //log what was done
    this.getMimsLog().Log("QSA correction \ndwelltime (s) = " + dwell +"\nbetas = " + betastring + "\nFCObj (pA) = " + FCObj + "\n");
}
 *
 *
 *
 *
 *
 *
 */

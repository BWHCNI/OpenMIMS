package com.nrims;

import ij.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A processor for generating Composite images.
 *
 * @author cpoczatek
 */
public class CompositeProcessor implements Runnable {

    /**
     * Creates a new instance of HSIProcessor.
     *
     * @param compImage the MimsPlus image for which the composite image is based.
     * @param ui reference to the UI.
     */
    public CompositeProcessor(MimsPlus compImage, UI ui) {
        this.compImage = compImage;
        this.ui = ui;
    }

    public void finalize() {
        compImage = null;
        compProps = null;
    }

    private MimsPlus compImage = null;
    private CompositeProps compProps = null;
    private Thread fThread = null;
    private UI ui;

    /**
     * Sets the <code>CompositeProps</code> object for this composite image.
     *
     * @param props the <code>CompositeProps</code> object for this composite image.
     */
    public void setProps(CompositeProps props) {
        if (compImage == null) {
            return;
        }

        compProps = props;
        start();

    }

    /**
     * Get the <code>CompositeProps</code> object for this composite image.
     *
     * @return the <code>CompositeProps</code> object for this composite image.
     */
    public CompositeProps getProps() {
        return compProps;
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
            fThread.setPriority(Thread.NORM_PRIORITY); // DJ : thread to Thread for static 
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
     * Generates the actual image.
     */
    public void run() {

        try {
            int[] compPixels;

            try {
                compPixels = (int[]) compImage.getProcessor().getPixels();
            } catch (Exception e) {
                return;
            }

            MimsPlus[] images = this.getProps().getImages(ui);
            int width = compImage.getWidth();
            int height = compImage.getHeight();

            int offset = 0;
            for (int y = 0; y < height && fThread != null; y++) {
                for (int x = 0; x < width && fThread != null; x++) {
                    int r = 0, g = 0, b = 0;

                    //8 bit grayscale value
                    if (images[0] != null) {
                        r = getPixelLUT(images[0], x, y);
                    }
                    if (images[1] != null) {
                        g = getPixelLUT(images[1], x, y);
                    }
                    if (images[2] != null) {
                        b = getPixelLUT(images[2], x, y);
                    }
                    if (images[3] != null) {
                        r = java.lang.Math.min(255, r + getPixelLUT(images[3], x, y));
                        g = java.lang.Math.min(255, g + getPixelLUT(images[3], x, y));
                        b = java.lang.Math.min(255, b + getPixelLUT(images[3], x, y));
                    }
                    //bit shifts
                    r = r << 16;
                    g = g << 8;
                    compPixels[offset] = r + g + b;

                    // System.out.print(hsiPixels[offset] + " ");
                    if (fThread == null || fThread.isInterrupted()) {
                        fThread = null;
                        compImage.unlock();
                        return;
                    }
                    offset++;
                }
            }
            compImage.unlock();
            compImage.updateAndRepaintWindow();
            fThread = null;
        } catch (Exception x) {
            compImage.unlock();
            fThread = null;
            IJ.log(x.toString());
            x.printStackTrace();
        }
    }

    /**
     * @param img MimsPlus instance
     * @param x  x position
     * @param y y position
     * @return val
     */
    public int getPixelLUT(MimsPlus img, int x, int y) {
        if (img == null) {
            return 0;
        }
        int val = 0;

        double min = img.getDisplayRangeMin();
        double max = img.getDisplayRangeMax();
        double range = max - min;
        float pix = img.getProcessor().getPixelValue(x, y);

        if (pix <= min) {
            return 0;
        } else if (pix >= max) {
            return 255;
        }

        val = java.lang.Math.round((float) ((pix - min) / range) * 255);

        return val;
    }

    // DJ: 08/06/2014
    // Just for testig purposes.
    public void waitForThreadToFinish() {
        try {
            this.fThread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(CompositeProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

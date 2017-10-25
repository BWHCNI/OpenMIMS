package com.nrims.segmentation;

import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Wand;
import ij.process.ByteProcessor;
import ij.process.ByteStatistics;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * Customized implementation of a particle analyzer, which takes an recursive approach to find particles within
 * particles. Only 4-connected wand mode gives consistent results! Thus, diagonally connected ROIs of the same class are
 * still not joined.
 *
 * Requires a binary ByteProcessor image!
 *
 * @author reckow
 */
public class NrimsParticleAnalyzer {

    private ImageProcessor ip;
    private int width;
    private int height;
    private byte[] pixels;
    private int wandMode;
    private int roiType;
    private double background;
    private double foreground;
    private double maskColor = 255;
    private int minSize;

//    private ImagePlus vis; // used to visualize the found particles (only for debugging)
    public NrimsParticleAnalyzer(ImagePlus imp, double foreground, double background, int minSize) {
        ip = imp.getProcessor();
        byte[] initialMask = new byte[ip.getPixelCount()];
        for (int i = 0; i < initialMask.length; i++) {
            initialMask[i] = (byte) maskColor;
        }
        ip.setMask(new ByteProcessor(ip.getWidth(), ip.getHeight(), initialMask, null));
        width = ip.getWidth();
        height = ip.getHeight();
        pixels = null;
        this.foreground = foreground;
        this.background = background;
        this.minSize = minSize;

        if (ip instanceof ByteProcessor) {
            pixels = (byte[]) ip.getPixels();
        }
        wandMode = Wand.FOUR_CONNECTED;
        roiType = Wand.allPoints() ? Roi.FREEROI : Roi.TRACED_ROI;

        //ImagePlus vis = new ImagePlus("vis", ip.duplicate());
        //vis.show();
    }

    public void finalize() {
//        vis.close();
    }

    public ArrayList<Roi> analyze() {
        ShapeRoi boundingRoi = new ShapeRoi(new Roi(0, 0, width, height));
        return find(boundingRoi, foreground, false, -1);
    }

    /**
     * Recursively finds and traces particles within the specified roi. Recursion alternates between finding a particle
     * (findHoles == false) and finding holes within that particle (findHoles == true). Holes are finally subtracted
     * from the surrounding roi.
     *
     * @param boundingRoi the roi to be searched for particles
     * @param level the color value of the particles class
     * @param findHoles true if searching for holes and false otherwise
     * @param depth depth of recursion (for monitoring the algorithm)
     * @return list of all found particles of at least minSize
     */
    private ArrayList<Roi> find(ShapeRoi boundingRoi, double level, boolean findHoles, int depth) {
        ArrayList<Roi> foundParticles = new ArrayList<Roi>();
        Rectangle bounds = boundingRoi.getBounds();
        ImageProcessor mask = boundingRoi.getMask();
        double nextLevel = findHoles ? foreground : background;

        depth++;

        for (int y = bounds.y; y < (bounds.y + bounds.height); y++) {
            int offset = y * width;
            for (int x = bounds.x; x < (bounds.x + bounds.width); x++) {
                // mask is checked to be sure we're moving within the bounding roi
                // -> avoids the extremely inefficient boundingRoi.contains(x,y)
                if (mask.get(x - bounds.x, y - bounds.y) == maskColor && (pixels[offset + x] & 255) == level) {

                    // here we have found a pixel of the correct color
                    ShapeRoi foundParticle = trace(x, y, level); // trace the area (only 4-connected wand mode guarantees
                    // we're always staying within the bounding ROI!)
//                  System.out.println("found " + foundParticle + "\tat depth " + depth + "   in " + boundingRoi); // debug info
                    ip.setRoi(foundParticle);       // set ROI for calculating area statistics
                    if (getPixelCount() >= minSize) {   // only further process area if it has a sufficient size
                        boundingRoi.not(new ShapeRoi(foundParticle)); // remove the area from its containing ROI

                        // further search within area:
                        // if this was a particle, this will remove holes from it
                        // if this was a hole, this will search for additional particles inside this holes
                        foundParticles.addAll(find(foundParticle, nextLevel, !findHoles, depth));
                    }
                    if (findHoles) {
                        ip.setValue(foreground);
                    } else {
                        ip.setValue(background);
                    }
                    ip.fill(foundParticle); // fill the found area with its complementary color, so it's not found later again

//                    vis.setProcessor("", ip.duplicate()); // just for visualization
                }
            }
        }
        if (findHoles) {
            foundParticles.add(boundingRoi);
        }
        return foundParticles;
    }

    private ShapeRoi trace(int x, int y, double level) {
        Wand wand = new Wand(ip);
        wand.autoOutline(x, y, level, level, wandMode);
        return new ShapeRoi(new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, roiType));
    }

    private int getPixelCount() {
        ByteStatistics stats = new ByteStatistics(ip, ij.measure.Measurements.AREA, null);
        //ColorStatistics stats = new ColorStatistics(ip, ij.measure.Measurements.AREA, null);
        return stats.pixelCount;
    }
}

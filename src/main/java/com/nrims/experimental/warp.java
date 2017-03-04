package com.nrims.experimental;

import ij.*;
import ij.process.*;
import com.nrims.*;

/**
 *
 * @author cpoczatek Test class for z warping. Needs much work...
 */
public class warp {

    public void yeastWarp(UI ui) {

        /*
         * sort of working yeast shifting
         * hard coded nonsense, don't expect this to work
         */
        ImagePlus seg = WindowManager.getImage("m26_med3_75-1200.tif");
        System.out.println("shift image: " + seg.getTitle());
        ij.process.ByteProcessor segproc = (ij.process.ByteProcessor) seg.getProcessor();

        int width = ui.getMassImage(0).getProcessor().getWidth();
        int height = ui.getMassImage(0).getProcessor().getHeight();
        int depth = ui.getMassImage(0).getStackSize();
        //stack indexed starting at 1;
        ui.getMassImage(0).setSlice(1);
        short[][] pixels = new short[width][height];
        int[][] shiftpix = new int[width][depth];

        int v = 0;
        //get pixel shift array from shift image
        for (int z = depth; z > 1; z--) {
            seg.setSlice(z);
            segproc = (ij.process.ByteProcessor) seg.getProcessor();
            if (z == 500) {
                System.out.println("foo");
            }

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    v = (int) segproc.getPixel(x, y);
                    if ((v != 0) && (shiftpix[x][y] == 0)) {
                        //System.out.println("hit:"+x+"-"+y+"-"+z);
                        shiftpix[x][y] = z;
                    }
                }
            }
        }

        ShortProcessor shiftproc = new ShortProcessor(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                shiftproc.set(x, y, shiftpix[x][y]);
            }
        }
        ImagePlus shift = new ImagePlus("shift", shiftproc);
        shift.show();

        //if(true) return;
        //create new stack for shifted images
        //only 1 image this.getOpenMassImages().length
        ImageStack[] stack = new ImageStack[1];

        //for(int i = 0; i<stack.length; i++){ stack[i] = new ImageStack(width, height); }
        //fill stacks with empty planes to the correct length
        //for (int mindex = 0; mindex < stack.length; mindex++) {
        //  for (int sindex = 1; sindex <= depth; sindex++) {
        //    stack[mindex].addSlice("", new ShortProcessor(width, height));
        //}
        //}
        ShortProcessor proc;
        for (int mindex = 0; mindex < ui.getOpenMassImages().length; mindex++) {

            for (int i = 0; i < stack.length; i++) {
                stack[i] = new ImageStack(width, height);
            }

            //fill stacks with empty planes to the correct length
            for (int i = 0; i < stack.length; i++) {
                for (int sindex = 1; sindex <= depth; sindex++) {
                    stack[i].addSlice("", new ShortProcessor(width, height));
                }
            }

            for (int sindex = 1; sindex <= depth; sindex++) {

                //getMassImage(mindex).setSlice(sindex);
                proc = (ShortProcessor) ui.getMassImage(mindex).getStack().getProcessor(sindex);

                //get mass image pixels
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        pixels[x][y] = (short) proc.getPixel(x, y);
                    }
                }

                //decide where those pixels go...
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {

                        //skip 0 depth locations
                        if (shiftpix[x][y] == 0) {
                            continue;
                        }

                        int zindex = sindex + (int) java.lang.Math.floor((depth - shiftpix[x][y]) / 2);

                        if ((zindex > 0) && (zindex < depth)) {
                            stack[0].getProcessor(zindex).putPixel(x, y, pixels[x][y]);
                        }

                        if (x == 150 && y == 150) {
                            System.out.println("m: " + mindex + " x: " + x + " y:" + y + " z: " + sindex + "->" + zindex + " v:" + proc.getPixel(x, y));
                        }

                    }//y
                }//x

            }//slice

            //ImagePlus tempimg =  new ImagePlus("m-"+mindex, stack[mindex]);
            //ij
            //create images and show
            ImagePlus img = new ImagePlus("m-" + mindex, stack[0]);

            img.show();

            System.out.println("saving:   /tmp/" + img.getTitle() + ".raw");
            new ij.io.FileSaver(img).saveAsRawStack("/tmp/" + img.getTitle() + ".raw");
            img.close();
            img = null;
            stack[0] = null;

        }//mass

        //end of yeast shift
    }

    //
    //
    //
    //
    //sort of working cell shifting
    public void cellWarp(UI ui) {

        System.out.println("called com.nrims.experimental.warp.cellWarp()");
        if (false) {
            return;
        }

        //blah
        ImagePlus maskImage = WindowManager.getCurrentImage();

        ByteProcessor maskproc = (ByteProcessor) maskImage.getProcessor();
        FloatProcessor proc;
        int width = maskproc.getWidth();
        int height = maskproc.getHeight();

        /*
         * All x,y,z refer to original data volume axes
         * when using about data volumes!
         */

 /*
         * Generate shift values from m12 mask...
         */
        float[][] movepixels = new float[256][256];
        for (int p = 0; p < maskImage.getStackSize(); p++) {
            maskImage.setSlice(p + 1);
            maskproc = (ByteProcessor) maskImage.getProcessor();

            for (int x = 0; x < width; x++) {
                int count = 0;
                boolean tripped = false;
                for (int y = 0; y < height; y++) {
                    count++;
                    float pix = maskproc.getPixelValue(x, y);
                    if (pix > 0) {
                        tripped = true;
                    }
                    if (pix == 0 && tripped) {
                        break;
                    }
                }
                if (!tripped) {
                    count = 0;
                }
                movepixels[x][p] = count;
            }
        }

        ImagePlus mask = new ImagePlus("mask", new FloatProcessor(movepixels));
        mask.show();
        if (true) {
            return;
        }

        int mwidth = ui.getMassImage(0).getProcessor().getWidth();
        int mheight = ui.getMassImage(0).getProcessor().getHeight();
        int mdepth = ui.getMassImage(0).getStackSize();
        float[][] pixels = new float[mwidth][mheight];
        float scalefactor = 10 / 10; //mheight / 130;
        int zmax = 130; //mheight;
        float afmshift = (float) 115.0;

        ImageStack[] stack = new ImageStack[4];
        for (int i = 0; i < 4; i++) {
            stack[i] = new ImageStack(mwidth, mheight);
        }

        //loop over masses
        for (int mindex = 0; mindex < 4; mindex++) {

            int minshift = 9999;
            int maxshift = 0;
            //loop over y (original axis, swapped with z)
            for (int y = 1; y <= mdepth; y++) {
                //int y = 93;
                ui.getMassImage(mindex).setSlice(y);
                proc = (FloatProcessor) ui.getMassImage(mindex).getProcessor();

                //copy pixels
                for (int x = 0; x < mwidth; x++) {
                    for (int z = 0; z < mheight; z++) {
                        pixels[x][z] = (float) proc.getPixelValue(x, z);
                    }
                }

                float[][] shiftpixels = new float[mwidth][mheight];

                for (int x = 0; x < mwidth; x++) {
                    //get z shift and scale
                    int shift;

                    shift = zmax - (int) movepixels[x][y - 1];

                    if (shift < minshift) {
                        minshift = shift;
                    }
                    if (shift > maxshift) {
                        maxshift = shift;
                    }

                    if (shift < 0) {
                        shift = 0;
                    }
                    if (shift > zmax) {
                        shift = zmax;
                    }

                    for (int z = 0; z < mheight; z++) {

                        float pixval = 0;

                        if ((z + shift >= 0) && (z + shift < mheight)) {
                            //pixval = pixels[x][z + shift];
                            shiftpixels[x][z + shift] = pixels[x][z];
                        }

                    } //z loop

                } //x loop

                FloatProcessor shiftproc = new FloatProcessor(shiftpixels);
                stack[mindex].addSlice("", shiftproc);
                //System.out.println("added slice: " + y + "to mass:" + mindex);
                if (mindex == 0) {
                    System.out.println("min: " + minshift + " max: " + maxshift);
                    minshift = 9999;
                    maxshift = 0;
                }
            } //y loop

        } //mass loop

        ImagePlus[] imgs = new ImagePlus[4];
        for (int i = 0; i < 4; i++) {
            imgs[i] = new ImagePlus("m-" + i, stack[i]);
            imgs[i].show();
        }
    }
}

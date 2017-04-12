/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.data;

import com.nrims.HSIProcessor;
import com.nrims.HSIProps;
import com.nrims.MimsPlus;
import java.io.File;

/**
 *
 * @author cpoczatek
 */
public class exportQVis {

    /**
     * Write a QVis RGBA version of an HSI image
     *
     * @param ui the <code>UI</code> instance
     * @param img HSI image to be converted
     * @param minA min of alpha range in counts
     * @param maxA max of alpha range in counts
     * @param outFile file for .dat header
     * 
     * @return true if the QVis RGBA version of the HSIfile was successfully written, false if file is of the wrong type or there was an exception while reading the file
     */
    public static boolean exportHSI_RGBA(com.nrims.UI ui, com.nrims.MimsPlus img, int minA, int maxA, File outFile) {

        if (img.getMimsType() != MimsPlus.HSI_IMAGE) {
            return false;
        }

        HSIProps props = img.getHSIProps();
        MimsPlus denimg = ui.getMassImage(props.getDenMassIdx());
        float[][] hsitables = HSIProcessor.getHsiTables();
        int r, g, b;
        double rScale = 65535.0 / (props.getMaxRatio() - props.getMinRatio());

        float[] pix = (float[]) img.internalRatio_filtered.getProcessor().getPixels();

        float[] denpix = (float[]) img.internalDenominator.getProcessor().getPixels();

        byte[][] plane_rgba = new byte[pix.length][4];

        int numIndex = props.getNumMassIdx();
        int denIndex = props.getDenMassIdx();
        int numMass = Math.round(new Float(ui.getOpener().getMassNames()[numIndex]));
        int denMass = Math.round(new Float(ui.getOpener().getMassNames()[denIndex]));

        java.io.FileOutputStream out = null;
        //testing each channel
        /*
        java.io.FileOutputStream outr = null;
        java.io.FileOutputStream outg = null;
        java.io.FileOutputStream outb = null;
        java.io.FileOutputStream outa = null;
         */
        String dir = outFile.getParent();

        String fileprefix = outFile.getName();
        fileprefix = fileprefix.substring(0, fileprefix.lastIndexOf("."));

        //File rawFile = new File(outFile.getParentFile(), fileprefix + ".raw");
        //stupid rgb max min crap
        //needs to change to not be unitless
        int rgbMax = props.getMaxRGB();
        int rgbMin = props.getMinRGB();
        if (rgbMax == rgbMin) {
            rgbMax++;
        }

        try {
            //write rgba data
            out = new java.io.FileOutputStream(dir + File.separator + fileprefix + ".raw");

            //testing each channel
            /*
            outr = new java.io.FileOutputStream(dir + java.io.File.separator + "r.raw");
            outg = new java.io.FileOutputStream(dir + java.io.File.separator + "g.raw");
            outb = new java.io.FileOutputStream(dir + java.io.File.separator + "b.raw");
            outa = new java.io.FileOutputStream(dir + java.io.File.separator + "a.raw");
             */
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        int nImages = denimg.getStackSize();
        for (int j = 1; j <= nImages; j++) {
            img.setSlice(j, img);
            //Don't call setSlice on internal images!
            //denimg.setSlice(j, true);

            pix = (float[]) img.internalRatio_filtered.getProcessor().getPixels();
            denpix = (float[]) img.internalDenominator.getProcessor().getPixels();

            for (int i = 0; i < pix.length; i++) {
                float ratioval = pix[i];
                //kludge
                if (ratioval < props.getMinRatio()) {
                    ratioval = (float) props.getMinRatio();
                }

                int iratio = 0;
                if (ratioval > props.getMinRatio()) {
                    if (ratioval < props.getMaxRatio()) {
                        iratio = (int) ((ratioval - props.getMinRatio()) * rScale);
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

                r = (int) (hsitables[0][iratio] * 255) << 16;
                g = (int) (hsitables[1][iratio] * 255) << 8;
                b = (int) (hsitables[2][iratio] * 255);

                plane_rgba[i][0] = (byte) (hsitables[0][iratio] * 255);
                plane_rgba[i][1] = (byte) (hsitables[1][iratio] * 255);
                plane_rgba[i][2] = (byte) (hsitables[2][iratio] * 255);
                if ((plane_rgba[i][0] > 255) || (plane_rgba[i][1] > 255) || (plane_rgba[i][2] > 255)) {
                    System.out.println("Error: " + r + "," + g + "," + b);
                }

                int min = minA;
                int max = maxA;

                double alpha = java.lang.Math.max(denpix[i], min);
                alpha = java.lang.Math.min(alpha, max);
                alpha = (alpha - min) / (max - min);

                //kludge to do ratio value alpha-ing
                //the kludge it burns
                alpha = java.lang.Math.max(ratioval, min);
                alpha = java.lang.Math.min(alpha, max);
                alpha = (alpha - min) / (max - min);

//WTF is this?
/*                if(denpix[i] < 5000.0) {
                    alpha = (double)0.0;
                }
                 */
                if (j == 50) {
                    System.out.println("pix: " + i + " alpha: " + alpha);
                }

                plane_rgba[i][3] = (byte) (255 * alpha);

            }

            try {
                for (int i = 0; i < plane_rgba.length; i++) {
                    out.write(plane_rgba[i]);

                    //testing each channel
                    /*
                    outr.write(plane_rgba[i][0]);
                    outg.write(plane_rgba[i][1]);
                    outb.write(plane_rgba[i][2]);
                    outa.write(plane_rgba[i][3]);
                     */
                }
                out.flush();

                //testing each channel
                /*
                outr.flush();
                outg.flush();
                outb.flush();
                outa.flush();
                 */
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }
        try {
            out.close();

            //testing each channel
            /*
            outr.close();
            outg.close();
            outb.close();
            outa.close();
             */
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        java.io.BufferedWriter bw = null;
        try {
            //write header file
            bw = new java.io.BufferedWriter(new java.io.FileWriter(outFile));
            /*
             * Example header file content
             * 
            ObjectFileName: 090826-3-4_PT-4x999_concat_rgba.raw
            TaggedFileName: ---
            Resolution: 256 256 449
            SliceThickness: 1 1 1
            Format: UCHAR4
            NbrTags: 0
            ObjectType: TEXTURE_VOLUME_OBJECT
            ObjectModel: RGBA
            GridType: EQUIDISTANT
            # some comment
            # some other comment
             */
            int x = denimg.getWidth();
            int y = denimg.getHeight();
            int z = denimg.getStackSize();
            String rname = fileprefix + ".raw";

            bw.write("ObjectFileName: " + rname + "\n");
            bw.write("TaggedFileName: ---" + "\n");
            bw.write("Resolution: " + x + " " + y + " " + z + "\n");
            bw.write("SliceThickness: 1 1 1" + "\n");
            bw.write("Format: UCHAR4\n");
            bw.write("NbrTags: 0\n");
            bw.write("ObjectType: TEXTURE_VOLUME_OBJECT\n");
            bw.write("ObjectModel: RGBA\n");
            bw.write("GridType: EQUIDISTANT\n");

            //write some metadata as comments
            bw.write("# data_file: " + ui.getOpener().getImageFile().getName() + "\n");
            bw.write("# ratio_min: " + props.getMinRatio() + "\n");
            bw.write("# ratio_max: " + props.getMaxRatio() + "\n");
            bw.write("# alpha_min: " + minA + "\n");
            bw.write("# alpha_max: " + maxA + "\n");
            bw.write("# medianized: " + ui.getMedianFilterRatios() + "\n");
            bw.write("# med_radius: " + ui.getHSIView().getMedianRadius() + "\n");
            bw.write("# window: " + ui.getIsWindow() + "\n");
            bw.write("# window_radius: " + ui.getWindowRange() + "\n");

            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

}

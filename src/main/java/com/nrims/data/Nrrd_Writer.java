package com.nrims.data;

// Nrrd_Writer
// -----------
// ImageJ plugin to save a file in Gordon Kindlmann's NRRD 
// or 'nearly raw raster data' format, a simple format which handles
// coordinate systems and data types in a very general way
// See http://teem.sourceforge.net/nrrd/
// and http://flybrain.stanford.edu/nrrd/
// (c) Gregory Jefferis 2007
// Department of Zoology, University of Cambridge
// jefferis@gmail.com
// All rights reserved
// Source code released under Lesser Gnu Public License v2
import com.nrims.UI;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.ImageWriter;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Nrrd_Writer {

    private static Opener op;
    private static final String plugInName = "Nrrd Writer";
    private static final String noImages = plugInName + "...\n" + "No images are open.";
    private static final String supportedTypes
            = plugInName + "..." + "Supported types:\n\n"
            + "32-bit Grayscale float : FLOAT\n"
            + "(32-bit Grayscale integer) : LONG\n"
            + "16-bit Grayscale integer: INT\n"
            + "(16-bit Grayscale unsigned integer) : UINT\n"
            + "8-bit Grayscale : BYTE\n"
            + "8-bit Colour LUT (converted to greyscale): BYTE\n";

    public static final int NRRD_VERSION = 4;
    private String imgTypeString = null;
    String nrrdEncoding = "raw";
    static final String defaultNrrdCentering = "node";

    // Constructor.
    public Nrrd_Writer(UI ui) {
        op = ui.getOpener();
    }

    public Nrrd_Writer(Opener opener) {
        op = opener;
    }

    //Change missing from svn->git migration
    public Opener getOpener() {
        return op;
    }
    //end

    public File save(ImagePlus[] imp, String directory, String file) {
        if (imp == null) {
            IJ.showMessage(noImages);
            return null;
        }

        // Get FileInfo for each image although only the one is really needed for the header.
        int numMasses = op.getNMasses();
        FileInfo[] fi = new FileInfo[op.getNMasses()];
        for (int i = 0; i < fi.length; i++) {
            fi[i] = imp[i].getFileInfo();
        }

        // Make sure that we can save this kind of image
        if (imgTypeString == null) {
            imgTypeString = imgType(fi[0].fileType);
            if (imgTypeString.equals("unsupported")) {
                IJ.showMessage(supportedTypes);
                return null;
            }
        }

        // Set the fileName stored in the file info record to the
        // file name that was passed in or chosen in the dialog box
        fi[0].fileName = file;
        fi[0].directory = directory;

        // Get calibration for each image.
        Calibration cal = imp[0].getCalibration();

        // Actually write out the image
        File returnFile = null;
        try {
            returnFile = writeImage(fi, cal);
        } catch (IOException e) {
            IJ.error("An error occured writing the file.\n \n" + e);
            IJ.showStatus("");
        }

        return returnFile;
    }

    File writeImage(FileInfo[] fi, Calibration cal) throws IOException {

        File file = new File(fi[0].directory, fi[0].fileName);

        // Setup output stream.
        FileOutputStream out = new FileOutputStream(file);

        // First write out the full header
        Writer bw = new BufferedWriter(new OutputStreamWriter(out));
        bw.write(makeHeader(fi[0], cal));

        // Write Mims specific fields.
        bw.write(getMimsKeyValuePairs());

        // Write out any other metadata.
        bw.write(getMetaDataKeyValuePairs() + "\n");

        // Flush rather than close
        bw.flush();

        // Then the image data
        ImageWriter[] writer = new ImageWriter[fi.length];
        for (int i = 0; i < writer.length; i++) {
            writer[i] = new ImageWriter(fi[i]);
            try {
                writer[i].write(out);
            } catch (ClassCastException cce) {
                System.out.println("ClassCastException writing image number " + i);
                cce.printStackTrace();
            }
        }

        out.close();

        //TODO, this shouldn't be here
        IJ.showStatus("Saved " + fi[0].fileName);
        return file;
    }

    public static String makeHeader(FileInfo fi, Calibration cal) {
        // NB You can add further fields to this basic header but 
        // You MUST add your own blank line at the end
        StringWriter out = new StringWriter();

        out.write("NRRD000" + NRRD_VERSION + "\n");
        out.write("# Created by OpenMIMS at " + (new Date()) + "\n");

        // Data type.
        out.write("type: " + imgType(fi.fileType) + "\n");

        // Encoding.
        out.write("encoding: " + getEncoding(fi) + "\n");

        // Endian.
        if (fi.intelByteOrder) {
            out.write("endian: little\n");
        } else {
            out.write("endian: big\n");
        }

        // Dimension.
        int dimension = 4;
        out.write("dimension: " + dimension + "\n");

        // Sizes.
        out.write("sizes: " + fi.width + " " + fi.height + " " + fi.nImages + " " + op.getNMasses() + "\n");

        // Kinds.
        out.write("kinds: space space space list\n");

        // Calibration.
        if (cal != null) {
            out.write("spacings: " + cal.pixelWidth + " " + cal.pixelHeight + " " + cal.pixelDepth + " NaN\n");
        }

        // Centers.
        out.write("centers: node node node node\n");

        // Units.
        String units;
        if (cal != null) {
            units = cal.getUnit();
        } else {
            units = fi.unit;
        }
        if (units.equals("�m")) {
            units = "microns";
        }
        if (!units.equals("")) {
            out.write("units: \"pixel\" \"pixel\" \"pixel\" \"pixel\"\n");
        }

        // Axis.
        // Only write axis mins if origin info has at least one non-zero element.
        if (cal != null && (cal.xOrigin != 0 || cal.yOrigin != 0 || cal.zOrigin != 0)) {
            out.write(dimmedLine("axis mins", dimension, (cal.xOrigin * cal.pixelWidth) + "",
                    (cal.yOrigin * cal.pixelHeight) + "", (cal.zOrigin * cal.pixelDepth) + ""));
        }

        return out.toString();
    }

    // Write Mims specific key/value pairs.
    private String getMimsKeyValuePairs() {

        // initialize variables.
        StringWriter out = new StringWriter();
        String line;

        // Mass numbers
        line = Opener.Mims_mass_numbers + Opener.Nrrd_separator;
        for (int i = 0; i < op.getNMasses(); i++) {
            line += op.getMassNames()[i] + " ";
        }
        out.write(line + "\n");

        // Mass symbols
        if (op.getMassSymbols() != null) {
            line = Opener.Mims_mass_symbols + Opener.Nrrd_separator;
            for (int i = 0; i < op.getNMasses(); i++) {
                line += op.getMassSymbols()[i] + " ";
            }
            out.write(line + "\n");
        }

        // Position
        out.write(Opener.Mims_position + Opener.Nrrd_separator + op.getPosition() + "\n");

        // Date
        out.write(Opener.Mims_date + Opener.Nrrd_separator + op.getSampleDate() + "\n");

        // Hour
        out.write(Opener.Mims_hour + Opener.Nrrd_separator + op.getSampleHour() + "\n");

        // Username
        out.write(Opener.Mims_user_name + Opener.Nrrd_separator + op.getUserName() + "\n");

        // Sample name
        out.write(Opener.Mims_sample_name + Opener.Nrrd_separator + op.getSampleName() + "\n");

        // Z position
        out.write(Opener.Mims_z_position + Opener.Nrrd_separator + op.getZPosition() + "\n");

        // Dwell time
        out.write(Opener.Mims_dwell_time + Opener.Nrrd_separator + op.getDwellTime() + "\n");

        // Count time
        out.write(Opener.Mims_count_time + Opener.Nrrd_separator + op.getCountTime() + "\n");

        // Duration
        out.write(Opener.Mims_duration + Opener.Nrrd_separator + op.getDuration() + "\n");

        // Raster
        out.write(Opener.Mims_raster + Opener.Nrrd_separator + op.getRaster() + "\n");

        // Pixel width
        out.write(Opener.Mims_pixel_width + Opener.Nrrd_separator + op.getPixelWidth() + "\n");

        // Pixel height
        out.write(Opener.Mims_pixel_height + Opener.Nrrd_separator + op.getPixelHeight() + "\n");

        // Deadtime correction
        out.write(Opener.Mims_dt_correction_applied + Opener.Nrrd_separator + op.isDTCorrected() + "\n");

        // QSA correction
        out.write(Opener.Mims_QSA_correction_applied + Opener.Nrrd_separator + op.isQSACorrected() + "\n");

        // BField
        if (op.getBField() != null) {
            out.write(Opener.Mims_BField + Opener.Nrrd_separator + op.getBField() + "\n");
        }

        // PrimCurrentT0
        if (op.getPrimCurrentT0() != null) {
            out.write(Opener.Mims_PrimCurrentT0 + Opener.Nrrd_separator + op.getPrimCurrentT0() + "\n");
        }

        // PrimCurrentTEnd
        if (op.getPrimCurrentTEnd() != null) {
            out.write(Opener.Mims_PrimCurrentTEnd + Opener.Nrrd_separator + op.getPrimCurrentTEnd() + "\n");
        }

        // D1Pos
        if (op.getD1Pos() != null) {
            out.write(Opener.Mims_D1Pos + Opener.Nrrd_separator + op.getD1Pos() + "\n");
        }

        // pszComment
        if (op.getpszComment() != null) {
            out.write(Opener.Mims_pszComment + Opener.Nrrd_separator + op.getpszComment() + "\n");
        }

        // Radius
        if (op.getRadius() != null) {
            out.write(Opener.Mims_Radius + Opener.Nrrd_separator + op.getRadius() + "\n");
        }

        // ESPos
        if (op.getESPos() != null) {
            out.write(Opener.Mims_ESPos + Opener.Nrrd_separator + op.getESPos() + "\n");
        }

        // ASPos
        if (op.getASPos() != null) {
            out.write(Opener.Mims_ASPos + Opener.Nrrd_separator + op.getASPos() + "\n");
        }

        // QSA correction parameters
        if (op.isQSACorrected()) {

            // Beta correction
            out.write(Opener.Mims_QSA_betas + Opener.Nrrd_separator);
            for (int i = 0; i < op.getBetas().length; i++) {
                out.write(Float.toString(op.getBetas()[i]));
                if (i < op.getBetas().length - 1) {
                    out.write(", ");
                }
            }
            out.write("\n");

            // FC Objective parameter.
            out.write(Opener.Mims_QSA_FC_Obj + Opener.Nrrd_separator + op.getFCObjective() + "\n");

        }
        //write slice labels
        if (op.getStackPositions() != null && op.getStackPositions().length > 0) {
            String[] names = op.getStackPositions();
            String output = "";
            for (int i = 0; i < names.length; i++) {
                output += names[i] + ";";
            }
            out.write(Opener.Mims_stack_positions + Opener.Nrrd_separator + output + "\n");
        }

        // Image Notes
        String notes = op.getNotes();
        //this is redundant but
        //double checking that file is written with no \n's
        notes = outputFormatNotes(notes);
        out.write(Opener.Mims_notes + Opener.Nrrd_separator + notes + "\n");

        return out.toString();
    }

    // Write Mims specific key/value pairs.
    private String getMetaDataKeyValuePairs() {

        // initialize variables.
        StringWriter out = new StringWriter();

        Map<String, String> sortedMap = new TreeMap<String, String>(op.getMetaDataKeyValuePairs());
        for (String key : sortedMap.keySet()) {
            out.write(key + Opener.Nrrd_separator + sortedMap.get(key) + "\n");
        }

        return out.toString();
    }

    public static String imgType(int fiType) {
        switch (fiType) {
            case FileInfo.GRAY32_FLOAT:
                return "float";
            case FileInfo.GRAY32_INT:
                return "int32";
            case FileInfo.GRAY32_UNSIGNED:
                return "uint32";
            case FileInfo.GRAY16_SIGNED:
                return "int16";
            case FileInfo.GRAY16_UNSIGNED:
                return "uint16";

            case FileInfo.COLOR8:
            case FileInfo.GRAY8:
                return "uint8";
            default:
                return "unsupported";
        }
    }

    public static String getEncoding(FileInfo fi) {
        NrrdFileInfo nfi;

        if (IJ.debugMode) {
            IJ.log("fi :" + fi);
        }

        try {
            nfi = (NrrdFileInfo) fi;
            if (IJ.debugMode) {
                IJ.log("nfi :" + nfi);
            }
            if (nfi.encoding != null && !nfi.encoding.equals("")) {
                return (nfi.encoding);
            }
        } catch (Exception e) {
        }

        switch (fi.compression) {
            case NrrdFileInfo.GZIP:
                return ("gzip");
            case NrrdFileInfo.BZIP2:
                return null;
            default:
                break;
        }
        // These aren't yet supported
        switch (fi.fileFormat) {
            case NrrdFileInfo.NRRD_TEXT:
            case NrrdFileInfo.NRRD_HEX:
                return (null);
            default:
                break;
        }
        // The default!
        return "raw";
    }

    private static String dimmedLine(String tag, int dimension, String x1, String x2, String x3) {
        String rval = null;
        if (dimension == 2) {
            rval = tag + ": " + x1 + " " + x2 + "\n";
        } else if (dimension == 3) {
            rval = tag + ": " + x1 + " " + x2 + " " + x3 + "\n";
        }
        return rval;
    }

    public String outputFormatNotes(String s) {
        String temp = s;
        temp = temp.replaceAll("(\r)|(\f)", "\n");
        return temp.replaceAll("\n", NrrdFileInfo.newlineReplacement);
    }

}

class NrrdFileInfo extends FileInfo {

    public int[] sizes;
    public String encoding = "";
    public String[] centers = null;
    public String[] massNames, massSymbols;
    public String duration, position, sampleDate, sampleHour,
            userName, dwellTime, sampleName;
    public String notes = "";
    public String raster;
    public String zposition;
    public int dimension, nMasses;
    public float pixel_width;
    public float pixel_height;
    public boolean dt_correction_applied = false;
    public boolean QSA_correction_applied = false;
    public float[] betas;
    public float fc_objective;
    public boolean isPrototype = false;
    public String[] tilePositions = null;
    public String[] stackPositions = null;
    public HashMap metadata = new HashMap();
    public double countTime;

    // These field were specifically selected by analysts as having importance,
    // but are not required to make the application work.
    public String BField;          // Magnetic field in DAC
    public String PrimCurrentT0;   // Primary current at t = 0 in pA
    public String PrimCurrentTEnd; // Primary current at t = End in pA
    public String D1Pos;           // D1 position
    public String pszComment;       // Comment
    public String Radius;	        // Trolley radius
    public String ESPos;           // Entrance slit position
    public String ASPos;           // Aperture slit position

    // DJ: 10/13/2014
    public String PrimL1;
    public String PrimL0;
    public String CsHv;

    // Additional compression modes for fi.compression
    public static final int GZIP = 1001;
    public static final int ZLIB = 1002;
    public static final int BZIP2 = 1003;

    // Additional file formats for fi.fileFormat
    public static final int NRRD = 1001;
    public static final int NRRD_TEXT = 1002;
    public static final int NRRD_HEX = 1003;

    //For notes field
    public static final String newlineReplacement = "&/&/&";
}

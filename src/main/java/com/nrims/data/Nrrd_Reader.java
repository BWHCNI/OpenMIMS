package com.nrims.data;

// Nrrd_Reader
// -----------
// (c) Gregory Jefferis 2007
// Department of Zoology, University of Cambridge
// jefferis@gmail.com
// All rights reserved
// Source code released under Lesser Gnu Public License v2
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

/**
 * ImageJ plugin to read a file in Gordon Kindlmann's NRRD or 'nearly raw raster data' format, a simple format which
 * handles coordinate systems and data types in a very general way. See
 * <A HREF="http://teem.sourceforge.net/nrrd">http://teem.sourceforge.net/nrrd</A>
 * and
 * <A HREF="http://flybrain.stanford.edu/nrrd">http://flybrain.stanford.edu/nrrd</A>
 */
public class Nrrd_Reader implements Opener {

    public final String uint8Types = "uchar, unsigned char, uint8, uint8_t";
    public final String int16Types = "short, short int, signed short, signed short int, int16, int16_t";
    public final String uint16Types = "ushort, unsigned short, unsigned short int, uint16, uint16_t";
    public final String int32Types = "int, signed int, int32, int32_t";
    public final String uint32Types = "uint, unsigned int, uint32, uint32_t";

    private File file = null;
    private int currentIndex = 0;
    private NrrdFileInfo fileInfo = null;
    private boolean header = false;
    private short bitSize = 0;

    public Nrrd_Reader(File imageFile) throws IOException {

        // Make sure file exists.
        this.file = imageFile;
        if (!file.exists()) {
            throw new NullPointerException("File " + imageFile + " does not exist.");
        }

        // Read the header.
        try {
            fileInfo = getHeaderInfo();
        } catch (IOException io) {
            System.out.println("Error reading file " + file.getAbsolutePath());
        }

        if (fileInfo.massNames == null) {
            fileInfo.massNames = new String[fileInfo.nMasses];
            for (int i = 0; i < fileInfo.nMasses; i++) {
                fileInfo.massNames[i] = Integer.toString(i);
            }
        } else if (fileInfo.nMasses != fileInfo.massNames.length) {
            System.out.print("Error! Number of masses (" + fileInfo.nMasses + ") does not equal "
                    + "number of mass names referenced: " + fileInfo.massNames);
            System.out.println();
            return;
        }

    }

    // Reads header and gets metadata.
    private NrrdFileInfo getHeaderInfo() throws IOException {

        if (IJ.debugMode) {
            IJ.log("Entering Nrrd_Reader.readHeader():");
        }

        // Setup file header.
        fileInfo = new NrrdFileInfo();
        fileInfo.directory = file.getParent();
        fileInfo.fileName = file.getName();

        // Need RAF in order to ensure that we know file offset.
        RandomAccessFile in = new RandomAccessFile(file.getAbsolutePath(), "r");

        // Initialize some strings.
        String thisLine, noteType, noteValue, noteValuelc;
        fileInfo.fileFormat = FileInfo.RAW;
        int lineskip = 0;

        // Parse the header file, until reach an empty line.
        while (true) {
            thisLine = in.readLine();
            if (thisLine == null || thisLine.equals("")) {
                fileInfo.longOffset = in.getFilePointer();
                break;
            }

            if (thisLine.indexOf("#") == 0) {
                continue; // ignore comments
            }
            // Get the key/value pair
            noteType = getFieldPart(thisLine, 0);
            String originalNoteType = noteType; //keep case for notes
            noteType = noteType.toLowerCase(); // case irrelevant
            noteValue = getFieldPart(thisLine, 1);
            noteValuelc = noteValue.toLowerCase();

            if (IJ.debugMode) {
                IJ.log("NoteType:" + noteType + ", noteValue:" + noteValue);
            }

            if (noteType.equals("dimension")) {
                fileInfo.dimension = Integer.valueOf(noteValue).intValue();
                //???????? add back dimension check?
                //if(fileInfo.dimension>3) throw new IOException("Nrrd_Reader: Dimension>3 not yet implemented!");
            }

            if (noteType.equals("sizes")) {
                fileInfo.sizes = new int[fileInfo.dimension];
                for (int i = 0; i < fileInfo.dimension; i++) {
                    fileInfo.sizes[i] = Integer.valueOf(getSubField(thisLine, i)).intValue();
                    if (i == 0) {
                        fileInfo.width = fileInfo.sizes[0];
                    }
                    if (i == 1) {
                        fileInfo.height = fileInfo.sizes[1];
                    }
                    if (i == 2) {
                        fileInfo.nImages = fileInfo.sizes[2];
                    }
                    if (i == 3) {
                        fileInfo.nMasses = fileInfo.sizes[3];
                    }
                }
            }

            if (noteType.equals("spacings")) {
                double[] spacings = new double[fileInfo.dimension];
                for (int i = 0; i < fileInfo.dimension; i++) {
                    // TOFIX - this order of allocations is not a given!
                    //spacings[i]=Double.valueOf(getSubField(thisLine,i)).doubleValue();
                    //if(i==0) spatialCal.pixelWidth=spacings[0];
                    //if(i==1) spatialCal.pixelHeight=spacings[1];
                    //if(i==2) spatialCal.pixelDepth=spacings[2];
                }
            }

            if (noteType.equals("centers") || noteType.equals("centerings")) {
                fileInfo.centers = new String[fileInfo.dimension];
                for (int i = 0; i < fileInfo.dimension; i++) {
                    // TOFIX - this order of allocations is not a given!
                    fileInfo.centers[i] = getSubField(thisLine, i);
                }
            }

            if (noteType.equals("axis mins") || noteType.equals("axismins")) {
                double[] axismins = new double[fileInfo.dimension];
                for (int i = 0; i < fileInfo.dimension; i++) {
                    // TOFIX - this order of allocations is not a given!
                    // NB xOrigin are in pixels, whereas axismins are of course
                    // in units; these are converted later
                    //axismins[i]=Double.valueOf(getSubField(thisLine,i)).doubleValue();
                    //if(i==0) spatialCal.xOrigin=axismins[0];
                    //if(i==1) spatialCal.yOrigin=axismins[1];
                    //if(i==2) spatialCal.zOrigin=axismins[2];
                }
            }

            if (noteType.equals("type")) {
                if (uint8Types.indexOf(noteValuelc) >= 0) {
                    fileInfo.fileType = FileInfo.GRAY8;
                } //16 bit signed/unsigned checks were flipped?
                else if (uint16Types.indexOf(noteValuelc) >= 0) {
                    fileInfo.fileType = FileInfo.GRAY16_UNSIGNED;
                    bitSize = 2;
                } else if (int16Types.indexOf(noteValuelc) >= 0) {
                    fileInfo.fileType = FileInfo.GRAY16_SIGNED;
                } else if (uint32Types.indexOf(noteValuelc) >= 0) {
                    fileInfo.fileType = FileInfo.GRAY32_UNSIGNED;
                } else if (int32Types.indexOf(noteValuelc) >= 0) {
                    fileInfo.fileType = FileInfo.GRAY32_INT;
                } else if (noteValuelc.equals("float")) {
                    fileInfo.fileType = FileInfo.GRAY32_FLOAT;
                    bitSize = 4;
                } else if (noteValuelc.equals("double")) {
                    fileInfo.fileType = FileInfo.GRAY64_FLOAT;
                } else {
                    throw new IOException("Unimplemented data type =" + noteValue);
                }
            }

            if (noteType.equals("byte skip") || noteType.equals("byteskip")) {
                fileInfo.longOffset = Long.valueOf(noteValue).longValue();
            }

            if (noteType.equals("endian")) {
                if (noteValuelc.equals("little")) {
                    fileInfo.intelByteOrder = true;
                } else {
                    fileInfo.intelByteOrder = false;
                }
            }

            if (noteType.equals("encoding")) {
                if (noteValuelc.equals("gz")) {
                    noteValuelc = "gzip";
                }
                fileInfo.encoding = noteValuelc;
            }

            if (noteType.equals("data file")) {
                header = true;
                fileInfo = setDataFile(fileInfo, noteValue);
            }

            if (noteType.equals("line skip")) {
                lineskip = Integer.valueOf(noteValue).intValue();
            }

            // All the following are Mims specific headers.
            int i = noteType.indexOf(Opener.Nrrd_separator);
            int j = Opener.Nrrd_separator.length();
            int k = i + j;
            String key = null;
            if (i >= 0) {
                key = noteType.substring(0, i);
            }
            String value = noteType.substring(k);
            String originalvalue = originalNoteType.substring(k);
            if (value == null) {
                value = "";
            }

            if (thisLine.startsWith(Opener.Mims_mass_numbers)) {
                fileInfo.massNames = noteType.substring(i + Opener.Nrrd_separator.length()).split(" ");
            } else if (thisLine.startsWith(Opener.Mims_mass_symbols)) {
                fileInfo.massSymbols = originalNoteType.substring(i + Opener.Nrrd_separator.length()).split(" ");
            } else if (thisLine.startsWith(Opener.Mims_date)) {
                fileInfo.sampleDate = value;
            } else if (thisLine.startsWith(Opener.Mims_duration)) {
                fileInfo.duration = value;
            } else if (thisLine.startsWith(Opener.Mims_dwell_time)) {
                fileInfo.dwellTime = value;
            } else if (thisLine.startsWith(Opener.Mims_hour)) {
                fileInfo.sampleHour = value;
            } else if (thisLine.startsWith(Opener.Mims_position)) {
                fileInfo.position = value;
            } else if (thisLine.startsWith(Opener.Mims_z_position)) {
                fileInfo.zposition = value;
            } else if (thisLine.startsWith(Opener.Mims_sample_name)) {
                fileInfo.sampleName = value;
            } else if (thisLine.startsWith(Opener.Mims_user_name)) {
                fileInfo.userName = value;
            } else if (thisLine.startsWith(Opener.Mims_tile_positions)) {
                fileInfo.tilePositions = originalNoteType.substring(i + Opener.Nrrd_separator.length()).split(";");
            } else if (thisLine.startsWith(Opener.Mims_stack_positions)) {
                fileInfo.stackPositions = originalNoteType.substring(i + Opener.Nrrd_separator.length()).split(";");
            } else if (thisLine.startsWith(Opener.Mims_raster)) {
                fileInfo.raster = value;
            } else if (thisLine.startsWith(Opener.Mims_BField)) {
                fileInfo.BField = value;
            } else if (thisLine.startsWith(Opener.Mims_pszComment)) {
                fileInfo.pszComment = value;
            } else if (thisLine.startsWith(Opener.Mims_PrimCurrentT0)) {
                fileInfo.PrimCurrentT0 = value;
            } else if (thisLine.startsWith(Opener.Mims_PrimCurrentTEnd)) {
                fileInfo.PrimCurrentTEnd = value;
            } else if (thisLine.startsWith(Opener.Mims_ESPos)) {
                fileInfo.ESPos = value;
            } else if (thisLine.startsWith(Opener.Mims_ASPos)) {
                fileInfo.ASPos = value;
            } else if (thisLine.startsWith(Opener.Mims_D1Pos)) {
                fileInfo.D1Pos = value;
            } else if (thisLine.startsWith(Opener.Mims_Radius)) {
                fileInfo.Radius = value;
            } else if (thisLine.startsWith(Opener.Mims_count_time)) {
                try {
                    Double ct = new Double(value);
                    fileInfo.countTime = ct.doubleValue();
                } catch (Exception e) {
                    fileInfo.countTime = (new Double(-1.0)).doubleValue();
                }
            } else if (thisLine.startsWith(Opener.Mims_pixel_height)) {
                try {
                    Float fl = new Float(value);
                    fileInfo.pixel_height = fl.floatValue();
                } catch (Exception e) {
                    fileInfo.pixelHeight = (new Float(-1.0)).floatValue();
                }
            } else if (thisLine.startsWith(Opener.Mims_pixel_width)) {
                try {
                    Float fl = new Float(value);
                    fileInfo.pixel_width = fl.floatValue();
                } catch (Exception e) {
                    fileInfo.pixelWidth = (new Float(-1.0)).floatValue();
                }
            } else if (thisLine.startsWith(Opener.Mims_dt_correction_applied)) {
                try {
                    fileInfo.dt_correction_applied = Boolean.parseBoolean(value);
                } catch (Exception e) {
                    fileInfo.dt_correction_applied = false;
                }
            } else if (thisLine.startsWith(Opener.Mims_prototype)) {
                try {
                    fileInfo.isPrototype = Boolean.parseBoolean(value);
                } catch (Exception e) {
                    fileInfo.isPrototype = false;
                }
            } else if (thisLine.startsWith(Opener.Mims_QSA_correction_applied)) {
                try {
                    fileInfo.QSA_correction_applied = Boolean.parseBoolean(value);
                } catch (Exception e) {
                    fileInfo.QSA_correction_applied = false;
                }
            } else if (fileInfo.QSA_correction_applied && thisLine.startsWith(Opener.Mims_QSA_FC_Obj)) {
                try {
                    Float fl = new Float(value);
                    fileInfo.fc_objective = fl.floatValue();
                } catch (Exception e) {
                }
            } else if (fileInfo.QSA_correction_applied && thisLine.startsWith(Opener.Mims_QSA_betas)) {
                try {
                    String[] qsa_string_vals = value.split(",");
                    float[] qsa_vals = new float[qsa_string_vals.length];
                    for (int ii = 0; ii < qsa_string_vals.length; ii++) {
                        qsa_vals[ii] = new Float(qsa_string_vals[ii]);
                    }
                    fileInfo.betas = qsa_vals;
                } catch (Exception e) {
                }
            } else if (thisLine.startsWith(Opener.Mims_notes)) {
                fileInfo.notes = originalvalue;
            } else if (key != null) {
                fileInfo.metadata.put(key, originalvalue);
            }

        }

        if (header) {
            RandomAccessFile datain = new RandomAccessFile(new File(fileInfo.directory, fileInfo.fileName), "r");
            for (int i = 0; i < lineskip; i++) {
                thisLine = datain.readLine();
            }
            fileInfo.longOffset = datain.getFilePointer();
        }

        return (fileInfo);
    }

    // Gets the field name from the header.
    String getFieldPart(String str, int fieldIndex) {
        str = str.trim(); // trim the string
        String[] fieldParts = str.split(":\\s+");
        if (fieldParts.length < 2) {
            return (fieldParts[0]);
        }
        if (fieldIndex == 0) {
            return fieldParts[0];
        } else {
            return fieldParts[1];
        }
    }

    private NrrdFileInfo setDataFile(NrrdFileInfo fi, String noteValue) {
        File datafile = new File(noteValue);
        if (datafile.exists()) {
            fi.directory = datafile.getParent();
            fi.fileName = datafile.getName();
            return fi;
        } else {
            datafile = new File(fi.directory, noteValue);
        }

        if (datafile.exists()) {
            fi.directory = datafile.getParent();
            fi.fileName = datafile.getName();
            return fi;
        } else {
            IJ.error("FileOpener", "File " + noteValue + " does not exist. \n "
                    + "File " + fi.directory + System.getProperty("file.separator") + noteValue + " does not exist.");
        }
        return fi;
    }

    String getSubField(String str, int fieldIndex) {
        String fieldDescriptor = getFieldPart(str, 1);
        fieldDescriptor = fieldDescriptor.trim(); // trim the string

        if (IJ.debugMode) {
            IJ.log("fieldDescriptor = " + fieldDescriptor + "; fieldIndex = " + fieldIndex);
        }

        String[] fields_values = fieldDescriptor.split("\\s+");

        if (fieldIndex >= fields_values.length) {
            return "";
        } else {
            String rval = fields_values[fieldIndex];
            if (rval.startsWith("\"")) {
                rval = rval.substring(1);
            }
            if (rval.endsWith("\"")) {
                rval = rval.substring(0, rval.length() - 1);
            }
            return rval;
        }
    }

    public Object getPixels(int index) throws IndexOutOfBoundsException, IOException {

        // Set up a temporary header to read the pixels from the file.
        NrrdFileInfo fi_clone = (NrrdFileInfo) fileInfo.clone();

        // Calculate offset
        //cast to long needed to avoid int overflow
        long offset = fi_clone.longOffset
                + // move down header
                ((long) bitSize * (long) index * (long) fi_clone.width * (long) fi_clone.height * (long) fi_clone.nImages)
                + // move down to correct channel
                ((long) bitSize * (long) fi_clone.width * (long) fi_clone.height * (long) currentIndex); // move down to correct image within that channel
        fi_clone.longOffset = offset;
        fi_clone.nImages = 1; // only going to read 1 image.
        FileOpener fo = new FileOpener(fi_clone);

        // Get image from file.
        ImagePlus imp = fo.open(false);
        if (imp == null) {
            throw new IOException();
        }

        Object pixels;
        if (fileInfo.fileType == FileInfo.GRAY16_UNSIGNED) {
            pixels = (short[]) imp.getProcessor().getPixels();
        } else if (fileInfo.fileType == FileInfo.GRAY32_FLOAT) {
            pixels = (float[]) imp.getProcessor().getPixels();
        } else {
            pixels = null;
        }

        return pixels;
    }

    public void close() {
    }

    public void setStackIndex(int index) throws IndexOutOfBoundsException {
        this.currentIndex = index;
    }

    public File getImageFile() {
        return file;
    }

    public int getNMasses() {
        return fileInfo.nMasses;
    }

    public int getNImages() {
        return fileInfo.nImages;
    }

    public int getPreviousNImages() {
        return fileInfo.nImages;
    }

    public int getWidth() {
        return fileInfo.width;
    }

    public int getHeight() {
        return fileInfo.height;
    }

    public String[] getMassNames() {
        return fileInfo.massNames;
    }
    
    public void setMassNames(String[] names) {
        fileInfo.massNames = names;
    }

    public String[] getMassSymbols() {
        return fileInfo.massSymbols;
    }
    
    public void setMassSymbols(String[] symbols) {
        fileInfo.massSymbols = symbols;
    }

    public float getPixelWidth() {
        return fileInfo.pixel_width;
    }

    public float getPixelHeight() {
        return fileInfo.pixel_height;
    }

    public String getSampleDate() {
        return fileInfo.sampleDate;
    }

    public String getSampleHour() {
        return fileInfo.sampleHour;
    }

    public String getSampleName() {
        return fileInfo.sampleName;
    }

    public String getUserName() {
        return fileInfo.userName;
    }

    public String getPosition() {
        return fileInfo.position;
    }

    public String getZPosition() {
        return fileInfo.zposition;
    }

    public String getRaster() {
        return fileInfo.raster;
    }

    public String getDwellTime() {
        return fileInfo.dwellTime;
    }

    public double getCountTime() {
        return fileInfo.countTime;
    }

    public String getDuration() {
        return fileInfo.duration;
    }

    public String getNotes() {
        return fileInfo.notes;
    }

    public void setNotes(String notes) {
        fileInfo.notes = notes;
    }

    public int getFileType() {
        return fileInfo.fileType;
    }

    public boolean isDTCorrected() {
        return fileInfo.dt_correction_applied;
    }

    public void setIsDTCorrected(boolean isCorrected) {
        fileInfo.dt_correction_applied = isCorrected;
    }

    public boolean isQSACorrected() {
        return fileInfo.QSA_correction_applied;
    }

    public void setIsQSACorrected(boolean isCorrected) {
        fileInfo.QSA_correction_applied = isCorrected;
    }

    public void setBetas(float[] betas) {
        fileInfo.betas = betas;
    }

    public void setFCObjective(float fc_objective) {
        fileInfo.fc_objective = fc_objective;
    }

    public float[] getBetas() {
        return fileInfo.betas;
    }

    public float getFCObjective() {
        return fileInfo.fc_objective;
    }

    public String[] getTilePositions() {
        return fileInfo.tilePositions;
    }

    public String[] getStackPositions() {
        return fileInfo.stackPositions;
    }

    public void setStackPositions(String[] names) {
        fileInfo.stackPositions = names;
    }

    public boolean isPrototype() {
        return fileInfo.isPrototype;
    }

    public HashMap getMetaDataKeyValuePairs() {
        return fileInfo.metadata;
    }

    public void setMetaDataKeyValuePairs(HashMap metaData) {
        fileInfo.metadata = metaData;
    }

    /**
     * Performs a check to see if the actual file size is in agreement with what the file size should be indicated by
     * the header.
     *
     * @return <code>true</code> if in agreement, otherwise <code>false</code>.
     */
    public boolean performFileSanityCheck() {
        long header_size = fileInfo.longOffset;
        int pixels_per_plane = getWidth() * getHeight();
        int num_planes = getNImages();
        int num_masses = getNMasses();
        int bytes = bitSize;

        long theoretical_file_size = (((long) pixels_per_plane) * ((long) num_planes) * ((long) num_masses) * ((long) bytes)) + header_size;
        long file_size = file.length();

        if (theoretical_file_size == file_size) {
            return true;
        } else {
            return false;
        }
    }

    public boolean fixBadHeader() {
        return true;
    }

    public boolean getWasHeaderFixed() {
        return false;
    }

    public boolean getIsHeaderBad() {
        return false;
    }

    public void setIsHeaderBad(boolean bad) {

    }

    public void setWasHeaderFixed(boolean fixed) {

    }

    public long getHeaderSize() {
        return fileInfo.longOffset;
    }

    public short getBitsPerPixel() {
        return bitSize;
    }

    public void setWidth(int width) {
        fileInfo.width = width;
    }

    public void setHeight(int height) {
        fileInfo.height = height;
    }

    public void setNMasses(int nmasses) {
        fileInfo.nMasses = nmasses;
    }

    public void setNImages(int nimages) {
        fileInfo.nImages = nimages;
    }

    public void setBitsPerPixel(short bitsperpixel) {
        bitSize = bitsperpixel;
    }

    public String getBField() {
        return fileInfo.BField;
    }

    public String getpszComment() {
        return fileInfo.pszComment;
    }

    public String getPrimCurrentT0() {
        return fileInfo.PrimCurrentT0;
    }

    public String getPrimCurrentTEnd() {
        return fileInfo.PrimCurrentTEnd;
    }

    public String getESPos() {
        return fileInfo.ESPos;
    }

    public String getASPos() {
        return fileInfo.ASPos;
    }

    public String getD1Pos() {
        return fileInfo.D1Pos;
    }

    public String getRadius() {
        return fileInfo.Radius;
    }

    // DJ: 10/13/2014: "fileInfo" to be enhanced to handle:
    // PrimL1, PrimL2, and CsHv
    // DJ: 10/13/2014
    public String getNPrimL1() {
        return fileInfo.PrimL1;
    }
    // DJ: 10/13/2014

    public String getNPrimL0() {
        return fileInfo.PrimL0;
    }
    // DJ: 10/13/2014

    public String getNCsHv() {
        return fileInfo.CsHv;
    }
}

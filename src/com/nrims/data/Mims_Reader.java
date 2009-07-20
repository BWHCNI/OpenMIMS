package com.nrims.data;

import com.nrims.*;
import java.io.*;
import java.text.DecimalFormat;

/**
 * Class responsible for opening MIMS files.
 *
**/
public class Mims_Reader implements Opener {

    /**
     * The number of bits per image pixel.  This is currently always 16.
     */
    public static final int BITS_PER_PIXEL = 16;
    private File file = null;
    private RandomAccessFile in;
    private int verbose = 0;
    private int width,  height,  nMasses,  nImages;
    private HeaderImage ihdr;
    private DefAnalysis dhdr;
    private TabMass[] tabMass;
    private MaskImage maskIm;
    private int currentMass = 0;
    private int currentIndex = 0;
    private UI ui = null;
    private MassImage[] massImages;
    public static final int IHDR_SIZE = 84;

    private Mims_Reader() {}

    /**
     * @return the image file which which this Opener is interfacing.
     */
    public File getImageFile() {
        return file;
    }

    final String getChar(int n) throws IOException {
        String rstr = new String();
        byte[] buf = new byte[n];
        int nr = in.read(buf);
        for (int i = 0; i < n && buf[i] != 0; i++) {
            rstr += (char) buf[i];
        }
        return rstr;
    }

    /**
     * Sets all the mass image pixels to null.
     */
    private void resetPixels() {
        for (int i = 0; i < nMasses; i++) {
            massImages[i].setPixels(null);
        }
    }

    /**
     * Ensures that the given index is valid (e.g. if it's >= 0 and <= nMasses).
     * @param index image mass index.
     * @throws IndexOutOfBoundsException if the given image mass index is invalid.
     */
    private void checkMassIndex(int index) {
        if (nMasses <= 0) {
            throw new IndexOutOfBoundsException("No images loaded, so mass index <" + index + "> is invalid.");
        } else if (index < 0 || index >= nMasses) {
            throw new IndexOutOfBoundsException("Given mass index <" + index + "> is out of range of the total number of masses <" + nMasses + ">.");
        }
    }

    /**
     * Ensures that the given index is valid (e.g. if it's >= 0 and <= nImages).
     * @param index image index.
     * @throws IndexOutOfBoundsException if the given image index is invalid.
     */
    private void checkImageIndex(int index) {
        if (nImages <= 0) {
            throw new IndexOutOfBoundsException("No images loaded, so image index <" + index + "> is invalid.");
        } else if (index < 0 || index >= nImages) {
            throw new IndexOutOfBoundsException("Given image index <" + index + "> is out of range of the total number of imags <" + nImages + ">.");
        }
    }

    /**
     * Reads the pixel data from the given mass image index.
     * @param index image mass index.
     * @throws IndexOutOfBoundsException if the given image mass index is invalid.
     * @throws IOException If there is an error reading in the pixel data.
     */
    public void readPixels(int index) throws IndexOutOfBoundsException, IOException {

        checkMassIndex(index);

        int i, j;
        int pixelsPerImage = width * height;
        int bytesPerMass = pixelsPerImage * 2;
        double sum, sum2;
        double var, np = (double) pixelsPerImage;

// Assumes file is positioned at the beginning of the data

        short[] spixels = new short[pixelsPerImage];
        massImages[index].setPixels(spixels);
        // long [] histogram = new long[65536];
        // massImages[index].setHistogram(histogram);

        int minGL = 65535;
        int maxGL = 0;

        // i : short counter
        // n : image counter

        long offset = dhdr.header_size + currentIndex * nMasses * bytesPerMass;
        if (index > 0) {
            offset += index * bytesPerMass;
        }
        in.seek(offset);

        sum = 0.0F;
        sum2 = 0.0F;
        int gl = -1;
        i = index + 1;
        if (ui != null) {
            if (nImages > 1) {
                ui.updateStatus("Reading image " + (currentIndex + 1) + " mass " + i + " of " + nMasses);
            } else {
                ui.updateStatus("Reading mass " + i + " of " + nMasses);
            }
        }

        /*
        for(i=0; i < pixelsPerImage ; i++ ) {
        gl = in.readShort() & 0xffff ;
        // if(gl < 0) gl = 65536 + gl ;
        sum += (double)gl ;
        sum2 += (double)(gl * gl) ;
        spixels[i] = (short)gl ;
        histogram[gl] += 1 ;
        if(gl < minGL) minGL = gl ;
        if(gl > maxGL) maxGL = gl ;
        }
         */
        byte[] bArray = new byte[bytesPerMass];
        in.read(bArray);

        for (i = 0, j=0; i < pixelsPerImage; i++, j += 2) {
            int b1 = bArray[j] & 0xff;
            int b2 = bArray[j + 1] & 0xff;
            gl = (b2 + (b1 << 8));
            sum += (double) gl;
            sum2 += (double) (gl * gl);
            spixels[i] = (short) gl;
            // histogram[gl] += 1 ;
            if (gl < minGL) {
                minGL = gl;
            }
            if (gl > maxGL) {
                maxGL = gl;
            }
        }

        massImages[index].setMeanGL(sum / np);
        var = (sum2 - (sum * sum / np)) / (np - 1.0);
        if (sum > 0) {
            massImages[index].setStdDev(Math.sqrt(var));
        } else {
            massImages[index].setStdDev(0.0);
        }
    }

    /**
     * Reads the DefAnalysis structure from the SIMS file header.
     * @throws NullPointerException if the given DefAnalysis is null.
     * @throws IOException if there's an error reading in the DefAnalysis.
     */
    private void readDefAnalysis(DefAnalysis dhdr) throws NullPointerException, IOException {
        if (dhdr == null) {
            throw new NullPointerException("DefAnalysis cannot be null in readDefAnalyais.");
        }

        dhdr.release = in.readInt();
        dhdr.analysis_type = in.readInt();
        dhdr.header_size = in.readInt();
        int noused = in.readInt();
        dhdr.data_included = in.readInt();
        dhdr.sple_pos_x = in.readInt();
        dhdr.sple_pos_y = in.readInt();
        dhdr.analysis_name = getChar(32);
        dhdr.username = getChar(16);
        dhdr.sample_name = getChar(16);
        dhdr.date = getChar(16);
        dhdr.hour = getChar(16);

        if (this.verbose > 2) {
            System.out.println("readDefAnalysis OK");
            System.out.println("dhdr.release:" + dhdr.release);
            System.out.println("dhdr.analysis_type:" + dhdr.analysis_type);
            System.out.println("dhdr.header_size:" + dhdr.header_size);
            System.out.println("dhdr.data_included:" + dhdr.data_included);
            System.out.println("dhdr.sple_pos_x:" + dhdr.sple_pos_x);
            System.out.println("dhdr.sple_pos_y:" + dhdr.sple_pos_y);
            System.out.println("dhdr.analysis_name:" + dhdr.analysis_name);
            System.out.println("dhdr.username:" + dhdr.username);
            System.out.println("dhdr.sample_name:" + dhdr.sample_name);
            System.out.println("dhdr.date:" + dhdr.date);
            System.out.println("dhdr.hour:" + dhdr.hour);
        }
    }

    /**
     * Reads the AutoCal structure from the SIMS file header.
     */
    private void readAutoCal(AutoCal ac) throws IOException {
        ac.mass = getChar(64);
        ac.begin = in.readInt();
        ac.period = in.readInt();
    }

    /**
     * Reads a Table element structure from the SIMS file header.
     */
    private void readTabelts(Tabelts te) throws IOException {
        te.num_elt = in.readInt();
        te.num_isotop = in.readInt();
        te.quantity = in.readInt();
    }

    /**
     * Reads a PolyAtomic element/structure from the SIMS file header.
     */
    private void readPolyAtomic(PolyAtomic pa) throws IOException {
        pa.flag_numeric = in.readInt();
        pa.numeric_value = in.readInt();
        pa.nb_elts = in.readInt();
        pa.nb_charges = in.readInt();
        pa.charge = in.readInt();
        String unused = getChar(64);
        pa.tabelts = new Tabelts[5];
        for (int i = 0; i < 5; i++) {
            pa.tabelts[i] = new Tabelts();
            readTabelts(pa.tabelts[i]);
        }
    }

    /**
     * Reads a SigRef element/structure from the SIMS file header.
     */
    private void readSigRef(SigRef sr) throws IOException {
        sr.polyatomic = new PolyAtomic();
        readPolyAtomic(sr.polyatomic);
        sr.detector = in.readInt();
        sr.offset = in.readInt();
        sr.quantity = in.readInt();
    }

    /**
     * Reads and returns a Mask_im structure from the SIMS file header.
     */
    private void readMaskIm(MaskImage mask) throws NullPointerException, IOException {
        if (mask == null) {
            throw new NullPointerException("MaskImage cannot be null in readMaskIm.");
        }

        mask.filename = getChar(16);
        mask.analysis_duration = in.readInt();
        mask.cycle_number = in.readInt();
        mask.scantype = in.readInt();
        mask.magnification = in.readShort();
        mask.sizetype = in.readShort();
        mask.size_detector = in.readShort();
        short unused = in.readShort();
        mask.beam_blanking = in.readInt();
        mask.sputtering = in.readInt();
        mask.sputtering_duration = in.readInt();
        mask.auto_calib_in_analysis = in.readInt();

        if (this.verbose > 2) {
            System.out.println("mask.filename:" + mask.filename);
            System.out.println("mask.analysis_duration:" +
                    mask.analysis_duration);
            System.out.println("mask.cycle_number:" + mask.cycle_number);
            System.out.println("mask.scantype:" + mask.scantype);
            System.out.println("mask.magnification:" + mask.magnification);
            System.out.println("mask.sizetype:" + mask.sizetype);
            System.out.println("mask.size_detector:" + mask.size_detector);
            System.out.println("mask.beam_blanking:" + mask.beam_blanking);
            System.out.println("mask.sputtering:" + mask.sputtering);
            System.out.println("mask.sputtering_duration:" + mask.sputtering_duration);
            System.out.println("mask.auto_calib:" + mask.auto_calib_in_analysis);
        }

        mask.autocal = new AutoCal();
        readAutoCal(mask.autocal);
        int sig_ref_int = in.readInt();
        mask.sig_ref = new SigRef();
        readSigRef(mask.sig_ref);
        nMasses = in.readInt();
        if (this.verbose > 2) {
            System.out.println("mask.nMasses:" + nMasses);//	Read the Tab_mass *tab_mass[10]..
        }
        int tab_mass_ptr;
        for (int i = 0; i < 10; i++) {
            tab_mass_ptr = in.readInt();
            if (this.verbose > 2) {
                System.out.println("mask.tmp:" + tab_mass_ptr);
            }
        }
    }

    /**
     * Reads and returns a TabMass structure from the SIMS file header.
     * @throws NullPointerException if the given TabMass is null.
     * @throws IOException if the TabMass cannot be read in.
     */
    private void readTabMass(TabMass tab) throws NullPointerException, IOException {
        if (tab == null) {
            throw new NullPointerException();
        }
        int unused = in.readInt();
        int unuseds2 = in.readInt();
        tab.mass_amu = in.readDouble();
        tab.matrix_or_trace = in.readInt();
        tab.detector = in.readInt();
        tab.waiting_time = in.readDouble();
        tab.counting_time = in.readDouble();
        tab.offset = in.readInt();
        tab.mag_field = in.readInt();

        if (this.verbose > 2) {
            System.out.println("TabMass.mass_amu:" + tab.mass_amu);
            System.out.println("TabMass.matrix_or_trace:" + tab.matrix_or_trace);
            System.out.println("TabMass.detector:" + tab.detector);
            System.out.println("TabMass.waiting_time:" + tab.waiting_time);
            System.out.println("TabMass.counting_time:" + tab.counting_time);
            System.out.println("TabMass.offset:" + tab.offset);
            System.out.println("TabMass.mag_field:" + tab.mag_field);
        }
        tab.polyatomic = new PolyAtomic();
        readPolyAtomic(tab.polyatomic);
    }

    /**
     * Formats a double precision to a string
     */
    private String DecimalToStr(double v, int fraction) {
        DecimalFormat df = new DecimalFormat("0.00");
        if (fraction != 2) {
            String format;
            format = "0";
            if (fraction > 0) {
                format += ".";
            }
            for (int i = 0; i < fraction; i++) {
                format += "0";
            }
            df.applyPattern(format);
        }
        return df.format(v);
    }

    /**
     * Reads and returns the HeaderImage structure from a SIMS image file
     * @throws NullPointerException if the given HeaderImage is null.
     * @throws IOException if there's an error reading in the HeaderImage.
     */
    private void readHeaderImage(HeaderImage ihdr) throws NullPointerException, IOException {
        if (ihdr == null) {
            throw new NullPointerException();
        }
        ihdr.size_self = in.readInt();
        ihdr.type = in.readShort();
        ihdr.w = in.readShort();
        this.width = ihdr.w;
        ihdr.h = in.readShort();
        this.height = ihdr.h;
        ihdr.d = in.readShort();
        ihdr.n = in.readShort();
        nImages = ihdr.z = in.readShort();
        if (nImages < 1) {
            nImages = 1;
        }
        ihdr.raster = in.readInt();
        ihdr.nickname = getChar(64);

        if (this.verbose > 1) {
            System.out.println("readHeaderImage OK");
            System.out.println("ihdr.d:" + ihdr.d);
            System.out.println("ihdr.n:" + ihdr.n);
            System.out.println("ihdr.w:" + ihdr.w);
            System.out.println("ihdr.type:" + ihdr.type);
            System.out.println("ihdr.z:" + ihdr.z);
        }
    }

    /**
     * Reads and returns the header data from a SIMS image file
     * Creates the DefAnalysis and HeaderImage subclass's for this class.
     * @throws NullPointerExeption rethrown from sub-readers.
     * @throws IOException if there is an error reading in the header.
     */
    private void readHeader() throws NullPointerException, IOException {

        this.dhdr = new DefAnalysis();
        readDefAnalysis(dhdr);
        this.maskIm = new MaskImage();
        readMaskIm(this.maskIm);

        if (this.nMasses <= 0) {
            throw new IOException("Error reading MIMS file.  Zero image masses read in.");
        }

        massImages = new MassImage[nMasses];
        for (int i = 0; i < nMasses; i++) {
            massImages[i] = new MassImage();
        }
        this.tabMass = new TabMass[this.nMasses];

        for (int i = 0; i < this.nMasses; i++) {
            TabMass tm = new TabMass();
            massImages[i].setTabMass(tm);
            readTabMass(tm);
            massImages[i].setName(DecimalToStr(tm.mass_amu, 2));
        }

        long offset = dhdr.header_size - IHDR_SIZE;
        in.seek(offset);

        this.ihdr = new HeaderImage();
        readHeaderImage(ihdr);

    }

    /**
     * Opens and reads a SIMS image from an image file.
     * @param ui MIMS UI into which the MIMS file will be loaded.
     * @param imageFileName filename of the image to be loaded.
     * @throws IOException if there is a problem reading the image file
     * @throws NullPointerException if the imagename is null or empty.
     */
    public Mims_Reader(com.nrims.UI ui, String imageFileName) throws IOException, NullPointerException {
        if (imageFileName == null || imageFileName.length() == 0) {
            throw new NullPointerException("Can't create an Opener with no valid imagename");
        }

        this.ui = ui;
        this.verbose = ui.getDebug() ? 1 : 0;

        this.file = new File(imageFileName);
        in = new RandomAccessFile(file, "r");

        readHeader();
        currentIndex = 0;
        currentMass = 0;
        readPixels(0);
    }

    /**
     * @return the name of the SIMS image file
     */
    public String getName() {
        String name = file.getName();
        int extIndex = name.lastIndexOf(".im");
        return name.substring(0, extIndex);
    }

    /**
     * This function returns a string whose purpose is to be displayed in the stat line.
     * @param index image mass index.
     * @return a string with the mean, standard deviation min and max graylevels of a SIMS image.
     * @throws IndexOutOfBoundsException if the given index is invalid.
     * @throws IOException if there is an error reading in the pixel data.
     */
    public String getStats(int index) throws IndexOutOfBoundsException, IOException {
        checkMassIndex(index);

        if (massImages[index].getPixels() == null) {
            readPixels(index);
        }

        return "Mean " + DecimalToStr(massImages[index].getMeanGL(), 2) + " +/-  " + DecimalToStr(massImages[index].getStdDev(), 2) + " Min: " + massImages[index].getMinGL() + " Max: " + massImages[index].getMaxGL();
    }

    /**
     * TODO figure out what this returns
     * @param index image mass index.
     * @return ??
     * @throws IndexOutOfBoundsException if the given image mass index is invalid.
     * @throws IOException if there's a problem reading the given image mass index pixel data.
     */
    public String getCounts(int index) throws IndexOutOfBoundsException, IOException {
        checkMassIndex(index);

        if (massImages[index].getPixels() == null) {
            readPixels(index);
        }
        double n = (double) (this.width * this.height);

        String statString = DecimalToStr(massImages[index].getMeanGL(), 2) + " +/-  " + DecimalToStr(massImages[index].getStdDev(), 2) + " [ " + massImages[index].getMinGL() + " - " + massImages[index].getMaxGL() + " ] ";

        return statString;
    }

    /**
     * @return the Statistics string for the currently selected index.
     * @throws IndexOutOfBoundsException rethrown.
     * @throws IOException rethrown.
     */
    public String getStats() throws IndexOutOfBoundsException, IOException {
        return getStats(this.currentMass);
    }

    /**
     * @return the number of "masses" in this SIMS image file.
     */
    public int getNImages() {
        return this.nImages;
    }

    /**
     * @return the total number of image masses.
     */
    public int getNMasses() {
        return this.nMasses;
    }

    /**
     * @return the width of the images in pixels.
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the height of the images in pixels.
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @param index index of image.
     * @return the mass in AMU for image at the given index.
     * @throws IndexOutOfBoundsException if the index is out of range of valid indices.
     */
    public double getMass(int index) {
        if (nMasses <= 0) {
            throw new IndexOutOfBoundsException("No images loaded, so index <" + index + "> is invalid.");
        } else if (index < 0 || index > nMasses) {
            throw new IndexOutOfBoundsException("Given index <" + index + "> is out of range of the total number of masses <" + nMasses + ">.");
        }
        return this.tabMass[index].mass_amu;
    }

    /**
     * @param index image mass index.
     * @return a String of the mass in AMU for image at the given index.
     */
    public String getMassName(int index) {
        if (nMasses == 0) {
            return "";
        }
        if (index >= 0 && index <= nMasses) {
            return massImages[index].getName();
        }
        return "";
    }

    public String[] getMassNames() {
        String[] names = new String[nMasses];
        for (int i = 0; i < nMasses; i++) {
            names[i] = massImages[i].getName();
        }
        return names;
    }

    public void setDebug(int nLevel) {
        this.verbose = nLevel;
    }

    /**
     * @param index image mass index.
     * @return the graylevel of the image at index and location x,y
     * @throws IndexOutOfBoundsException if the given image mass index is invalid.
     * @throws IOException if the given image mass index pixels cannot be loaded.
     */
    public short[] getPixels(int index) {
        checkMassIndex(index);

        if (massImages[index].getPixels() == null) {
            try {
               readPixels(index);
            } catch(Exception e){e.printStackTrace();}
        }
        return massImages[index].getPixels();
    }

    /**
     * @param index image mass index.
     * @param x x coordinate of the desired pixel in the given image mass.
     * @param y y coordinate of the desired pixel in the given image mass.
     * @return the graylevel of the image at index and location x,y
     * @throws IndexOutOfBoundsException if the given image mass index is invalid, or if the given coordinates are invalid for that image mass index.
     * @throws IOException if the image mass information cannot be read in.
     */
    public int getPixel(int index, int x, int y) throws IndexOutOfBoundsException, IOException {
        checkMassIndex(index);
        if (massImages[index].getPixels() == null) {
            readPixels(index);
        }
        short[] spixels = massImages[index].getPixels();
        int offset = x + y * width;
        if (offset < 0 || offset > spixels.length - 1) {
            throw new IndexOutOfBoundsException("Coordinates ("+x+","+y+") are invalid for image mass index <" + index + ">.");
        }
        return (int) spixels[offset];
    }

    /**
     * The pixel data for the current image mass.
     * @param x x coordinate of the desired pixel in the given image mass.
     * @param y y coordinate of the desired pixel in the given image mass.
     * @return the graylevel of the image at current index and location x,y
     * @throws IndexOutOfBoundsException if the given coordinates are invalid.
     * @throws IOException if there is an error reading in the image information.
     */
    public int getPixel(int x, int y) throws IndexOutOfBoundsException, IOException {
        return this.getPixel(this.currentMass, x, y);
    }

    /**
     * @param index image mass index.
     * @return the minimum graylevel in the image at the given index
     * @throws IndexOutOfBoundsException if the given image mass index is invalid.
     * @throws IOException if there is an error reading in the image information.
     */
    public int getMin(int index) throws IndexOutOfBoundsException, IOException {
        checkMassIndex(index);
        if (massImages[index].getPixels() == null) {
            readPixels(index);
        }
        return massImages[index].getMinGL();
    }

    /**
     * @param index image mass index.
     * @return the maximum graylevel in the image at the given index
     * @throws IndexOutOfBoundsException if the given image mass index is invalid.
     * @throws IOException if there is an error reading in image data.
     */
    public int getMax(int index) throws IndexOutOfBoundsException, IOException {
        checkMassIndex(index);

        if (massImages[index].getPixels() == null) {
            readPixels(index);
        }
        return massImages[index].getMaxGL();
    }

    /**
     * @return the current index in a stack or multiple time point series indices are between at zero and nImages() - 1
     */
    public int getStackIndex() {
        return this.currentIndex;
    }

    /**
     * @return  the current mass. Masses are between at zero and nMasses - 1
     */
    public int getMassIndex() {
        return this.currentMass;
    }

    /**
     * Sets the current mass index.
     * @param index image mass index.
     */
    public void setMassIndex(int index) {
        checkMassIndex(index);

        if (this.currentMass == index) {
            return;
        }
        this.currentMass = index;
        if (ui != null) {
            ui.updateStatus("Current Mass:" + (index + 1));
        }
    }

    /**
     * sets the current image index.
     * @param index image mass index.
     * @throws IndexOutOfBoundsException if the given index is invalid.
     */
    public void setStackIndex(int index) throws IndexOutOfBoundsException {
        checkImageIndex(index);

        if (this.currentIndex == index) {
            return;
        }
        // TODO why are we resetting the pixels here?
        resetPixels();
        this.currentIndex = index;
    }

    /**
     * @return the sple_pos_x,sple_pos_y entries from the SIMS image header
     */
    public String getPosition() {
        if (this.dhdr == null) {
            return null;
        }
        String pos = this.dhdr.sple_pos_x + "," + this.dhdr.sple_pos_y;
        return pos;
    }

    /**
     * @return the date entry from the SIMS image header
     */
    public String getSampleDate() {
        if (this.dhdr == null) {
            return null;
        }
        if (this.dhdr.sample_name == null) {
            return new String(" ");
        }
        return this.dhdr.date;
    }

    /**
     * @return the hour entry from the SIMS image header
     */
    public String getSampleHour() {
        if (this.dhdr == null) {
            return null;
        }
        if (this.dhdr.sample_name == null) {
            return new String(" ");
        }
        return this.dhdr.hour;
    }

    /**
     * @return the username entry from the SIMS image header
     */
    public String getUserName() {
        if (this.dhdr == null) {
            return null;
        }
        if (this.dhdr.sample_name == null) {
            return new String(" ");
        }
        return this.dhdr.username;
    }

    /**
     * @return the samplename entry from the SIMS image header
     */
    public String getSampleName() {
        if (this.dhdr == null) {
            return null;
        }
        if (this.dhdr.sample_name == null) {
            return new String(" ");
        }
        return this.dhdr.sample_name;
    }

    /**
     * @return the raster entry from the SIMS image header
     */
    public int getRaster() {
        if (this.ihdr == null) {
            return 0;
        }
        return this.ihdr.raster;
    }

    /**
     * @return the dwelltime per pixel in milliseconds
     */
    public String getDwellTime() {
        if (this.maskIm == null || this.ihdr == null) {
            return new String(" ");
        }
        double ctime = getCountTimeD();
        double size = (double) (this.ihdr.w * this.ihdr.h);
        if (size == 0) {
            return new String(" ");
        }
        double dwelltime = 1000 * ctime/(size);
        String dtime = DecimalToStr(dwelltime, 3);
        return dtime;
    }
    /**
     * @return the counttime units are second per plane
     */
    public String getCountTime() {
        String ctime = "";
        if(this.massImages[0]!=null) {
            TabMass temptab = massImages[0].getTabMass();
            ctime = Double.toString(temptab.counting_time);
        }

        return ctime;
    }

    public double getCountTimeD() {
       if(this.massImages[0]!=null) {
            TabMass temptab = massImages[0].getTabMass();
            return temptab.counting_time;
        }

        return 0.0;
    }

    /**
     * @return the nickname from the SIMS header
     */
    public String getNickName() {
        if (this.ihdr == null) {
            return new String(" ");
        }
        return this.ihdr.nickname;
    }

    /**
     * @return the analysis_duration from the SIMS header
     */
    public double getDurationD() {
        if (this.maskIm == null) {
            return 0.0;
        }
        double duration = (double) this.maskIm.analysis_duration;
        return duration;
    }

    public String getDuration() {
        if (this.maskIm == null) {
            return null;
        }
        double duration = (double) this.maskIm.analysis_duration;
        String dtime = DecimalToStr(duration, 3);
        return dtime;
    }

    /**
     * @return the pixel Width from the SIMS header
     */
    public float getPixelWidth() {
        float pw = 1.0f;
        if (this.ihdr != null) {
            pw = (float) this.ihdr.raster / (float) this.ihdr.w;
        }
        return pw;
    }

    /**
     * @return the pixel Height from the SIMS header
     */
    public float getPixelHeight() {
        float ph = 1.0f;
        if (this.ihdr != null) {
            ph = (float) this.ihdr.raster / (float) this.ihdr.h;
        }
        return ph;
    }

    public String getInfo() {
        String info = "";
        try {
            if (massImages[currentMass].getPixels() == null) {
                readPixels(currentMass);
            }
            info += "MIMSFile=" + this.file.getCanonicalPath() + "\n";
            info += "Mass" + massImages[currentMass].getName() + "\n";
            info += "MinCounts=" + massImages[currentMass].getMinGL() + "\n";
            info += "MaxCounts=" + massImages[currentMass].getMaxGL() + "\n";
            info += "Mean=" + massImages[currentMass].getMeanGL() + "\n";
            info += "StdDev=" + massImages[currentMass].getStdDev() + "\n";
            int index = currentMass + 1;
            info += "MassIndex=" + index + "\n";
            info += "TotalMasses=" + getNMasses() + "\n";
            if (getNImages() > 1) {
                info += "Section=" + (currentIndex + 1) + "\n";
                info += "TotalSections=" + getNImages() + "\n";
            }
            if (tabMass[currentMass] != null) {
                info += tabMass[currentMass].getInfo();
            }
            if (ihdr != null) {
                info += ihdr.getInfo();
            }
            if (dhdr != null) {
                info += dhdr.getInfo();
            }
            if (maskIm != null) {
                info += maskIm.getInfo();
            }
            return info;
        } catch (Exception x) {
            return info;
        }
    }

    /**
     * Prints to the standard output the name, mass, and statistics for a series of SIMS images given as arguments.
     *
     * @param args command line arguments given to the opener.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: Opener <image>");
            System.exit(0);
        } else {
            for (int argc = 0; argc < args.length; argc++) {
                try {
                    Mims_Reader image = new Mims_Reader(null, args[argc]);
                    for (int n = 0; n < image.getNMasses(); n++) {
                        System.out.println("Image(" + n + " of " + image.getNMasses() + ") Mass: " + image.getMassName(n) + " [" + image.getMass(n) + "]");
                        System.out.println("  Stats:" + image.getStats(n));
                    }
                } catch (Exception e) {
                    System.err.println("Can't open " + args[argc] + ":\n" + e.getStackTrace());
                    // Exit with exception code 1.
                    System.exit(1);
                }
            }
        }
    }
}

    
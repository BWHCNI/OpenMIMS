package com.nrims.data;

import java.io.*;

/**
 * Class responsible for opening MIMS files.
 * 
**/
public interface Opener {

    // Mims specific fields.
    public static final String Nrrd_seperator = ":=";
    public static final String Mims_mass_numbers = "Mims_mass_numbers";
    public static final String Mims_position = "Mims_position";
    public static final String Mims_date = "Mims_date";
    public static final String Mims_hour = "Mims_hour";
    public static final String Mims_user_name = "Mims_user_name";
    public static final String Mims_sample_name = "Mims_sample_name";
    public static final String Mims_dwell_time = "Mims_dwell_time";
    public static final String Mims_count_time = "Mims_count_time";
    public static final String Mims_duration = "Mims_duration";
    public static final String Mims_raster = "Mims_raster";
    public static final String Mims_pixel_width = "Mims_pixel_width";
    public static final String Mims_pixel_height = "Mims_pixel_height";
    public static final String Mims_notes = "Mims_notes";

    // Worker functions.
    public File getImageFile();

    public int getNMasses();

    public int getNImages();

    public int getWidth();

    public int getHeight();   

    public short[] getPixels(int index) throws IndexOutOfBoundsException, IOException;

    public float getPixelWidth();

    public float getPixelHeight();

    public void setStackIndex(int currentIndex);

    // Metadata
    public String getPosition();
    public String[] getMassNames();
    public String getSampleDate();
    public String getSampleHour();
    public String getUserName();
    public String getSampleName();
    public String getDwellTime();
    public String getCountTime();
    public String getDuration();
    public int getRaster();
    public String getNotes();

    //Setable metadata
    public void setNotes(String notes);

    /**
     * defines a structure for saving the HeaderImage data
     */
    class HeaderImage {

        int size_self;
        short type;
        short w;
        short h;
        short d;
        short n;
        short z;
        int raster;
        String nickname;

        public String getInfo() {
            String info = "";
            if (type != 1) {
                info += "Header.Type=" + type + "\n";
            }
            if (w != 0) {
                info += "Header.Width=" + w + "\n";
            }
            if (h != 0) {
                info += "Header.Height=" + h + "\n";
            }
            if (d != 0) {
                info += "Header.BytesPerPixel=" + d + "\n";
            }
            if (n != 0) {
                info += "Header.masses=" + n + "\n";
            }
            if (z != 0) {
                info += "Header.Sections=" + z + "\n";
            }
            if (raster != 0) {
                info += "Header.Raster=" + raster + "\n";
            }
            if (nickname.length() > 0) {
                info += "Header.Nickname=" + nickname + "\n";
            }
            return info;
        }
    }

    class AutoCal {

        String mass;
        int begin;
        int period;

        public String getInfo() {
            String info = "";
            if (mass.length() > 0) {
                info += "AutoCal.mass=" + mass + "\n";
            }
            if (begin != 0) {
                info += "AutoCal.begin=" + begin + "\n";
            }
            if (period != 0) {
                info += "AutoCal.period=" + period + "\n";
            }
            return info;
        }
    }

    class SigRef {

        PolyAtomic polyatomic;
        int detector;
        int offset;
        int quantity;

        public String getInfo() {
            String info = "";
            if (detector != 0) {
                info += "SigRef.detector=" + detector + "\n";
            }
            if (offset != 0) {
                info += "SigRef.offset=" + offset + "\n";
            }
            if (quantity != 0) {
                info += "SigRef.quantity=" + quantity + "\n";
            }
            if (polyatomic != null) {
                String pInfo = polyatomic.getInfo();
                if (pInfo.length() > 0) {
                    info += "SigReg.polyatomic={\n";
                    info += polyatomic.getInfo();
                    info += "}\n";
                }
            }
            return info;
        }
    }

    class Tabelts {

        int num_elt;
        int num_isotop;
        int quantity;

        public String getInfo() {
            String info = "";
            if (num_elt != 0) {
                info += "Tabelts.num_elt=" + num_elt + "\n";
            }
            if (num_isotop != 0) {
                info += "Tabelts.num_isotop=" + num_isotop + "\n";
            }
            if (quantity != 0) {
                info += "Tabelts.quantity=" + quantity + "\n";
            }
            return info;
        }
    }

    class PolyAtomic {

        int flag_numeric;
        int numeric_value;
        int nb_elts;
        int nb_charges;
        int charge;
        Tabelts tabelts[];

        public String getInfo() {
            String info = "";
            if (flag_numeric != 0) {
                info = "PolyAtomic.flag_numeric=" + flag_numeric + "\n";
            }
            if (numeric_value != 0) {
                info += "PolyAtomic.numeric_value=" + numeric_value + "\n";
            }
            if (nb_elts != 0) {
                info += "PolyAtomic.nb_elts=" + nb_elts + "\n";
            }
            if (nb_charges != 0) {
                info += "PolyAtomic.nb_charges=" + nb_charges + "\n";
            }
            if (charge != 0) {
                info += "PolyAtomic.charge=" + charge + "\n";
            }
            if (tabelts != null) {
                for (int i = 0; i < tabelts.length; i++) {
                    if (tabelts[i] != null) {
                        String tInfo = tabelts[i].getInfo();
                        if (tInfo.length() > 0) {
                            info += "PolyAtomic.tabelts[" + i + "].={\n";
                            info += tInfo;
                            info += "}\n";
                        }
                    }
                }
            }
            return info;
        }
    }

    /**
     * defines a structure for saving the DefAnalysis data
     */
    class DefAnalysis {

        int release;
        int analysis_type;
        int header_size;
        int data_included;
        int sple_pos_x;
        int sple_pos_y;
        String analysis_name;
        String username;
        String sample_name;
        String date;
        String hour;

        public String getInfo() {
            String info = "";
            if (release != 0) {
                info = "Analysis.Release=" + release + "\n";
            }
            if (analysis_type != 0) {
                info += "Analysis.Type=" + analysis_type + "\n";
            }
            if (header_size != 0) {
                info += "Analysis.Headersize=" + header_size + "\n";
            }
            if (sple_pos_x != 0) {
                info += "Analysis.PosistionX=" + sple_pos_x + "\n";
            }
            if (sple_pos_y != 0) {
                info += "Analysis.PositionY=" + sple_pos_y + "\n";
            // if(data_included != 0) info += "Analysis.data_included="+data_included+"\n";
            }
            if (analysis_name.length() > 0) {
                info += "Analysis.Name=" + analysis_name + "\n";
            }
            if (username.length() > 0) {
                info += "Analysis.Username=" + username + "\n";
            }
            if (sample_name.length() > 0) {
                info += "Analysis.Samplename=" + sample_name + "\n";
            }
            if (date.length() > 0) {
                info += "Analysis.Date=" + date + "\n";
            }
            if (hour.length() > 0) {
                info += "Analysis.Hour=" + hour + "\n";
            }
            return info;
        }
    }

    /**
     * defines a structure for saving the Mask_im data
     */
    class MaskImage {

        String filename;
        int analysis_duration;
        int cycle_number;
        int scantype;
        short magnification;
        short sizetype;
        short size_detector;
        int beam_blanking;
        int sputtering;
        int sputtering_duration;
        int auto_calib_in_analysis;
        SigRef sig_ref;
        int nb_mass;
        AutoCal autocal;

        public String getInfo() {
            String info = "";
            if (filename.length() > 0) {
                info += "MaskImage.Filename=" + filename + "\n";
            }
            if (analysis_duration != 0) {
                info += "MaskImage.Duration=" + analysis_duration + "\n";
            }
            if (cycle_number != 0) {
                info += "MaskImage.Cycle=" + cycle_number + "\n";
            }
            if (scantype != 0) {
                info += "MaskImage.Scantype=" + scantype + "\n";
            }
            if (magnification != 0) {
                info += "MaskImage.Magnification=" + magnification + "\n";
            }
            if (sizetype != 0) {
                info += "MaskImage.Sizetype=" + sizetype + "\n";
            }
            if (size_detector != 0) {
                info += "MaskImage.SizeDetector=" + size_detector + "\n";
            }
            if (beam_blanking != 0) {
                info += "MaskImage.BeamBlanking=" + beam_blanking + "\n";
            }
            if (sputtering != 0) {
                info += "MaskImage.Sputtering=" + sputtering + "\n";
            }
            if (sputtering_duration != 0) {
                info += "MaskImage.SputteringDuration=" + sputtering_duration + "\n";
            }
            if (auto_calib_in_analysis != 0) {
                info += "MaskImage.AutoCalibration=" + auto_calib_in_analysis + "\n";
            }
            if (nb_mass != 0) {
                info += "MaskImage.NbMass=" + nb_mass + "\n";
            }
            if (sig_ref != null) {
                String sInfo = sig_ref.getInfo();
                if (sInfo.length() > 0) {
                    info += "MaskImage.SigRef={\n";
                    info += sInfo;
                    info += "}\n";
                }
            }
            if (autocal != null) {
                String aInfo = autocal.getInfo();
                if (aInfo.length() > 0) {
                    info += "MasImage.AutoCal={\n";
                    info += aInfo;
                    info += "}\n";
                }
            }
            return info;
        }
    }

    /**
     * defines a structure for saving the TabMass data
     */
    class TabMass {

        double mass_amu;
        int matrix_or_trace;
        int detector;
        double waiting_time;
        double counting_time;
        int offset;
        int mag_field;
        PolyAtomic polyatomic;

        public String getInfo() {
            String info = "";
            if (mass_amu != 0.0) {
                info += "TabMass.AMU=" + mass_amu + "\n";
            }
            if (matrix_or_trace != 0) {
                info += "TabMass.matrix_or_trace=" + matrix_or_trace + "\n";
            }
            if (detector != 0) {
                info += "TabMass.Detector=" + detector + "\n";
            }
            if (waiting_time != 0) {
                info += "TabMass.WaitingTime=" + waiting_time + "\n";
            }
            if (counting_time != 0) {
                info += "TabMass.CountingTime=" + counting_time + "\n";
            }
            if (offset != 0) {
                info += "TabMass.Offset=" + offset + "\n";
            }
            if (mag_field != 0) {
                info += "TabMass.Magfield=" + mag_field + "\n";
            }
            if (polyatomic != null) {
                String pInfo = polyatomic.getInfo();
                if (pInfo.length() > 0) {
                    info += "TabMass.PolyAtomic={\n";
                    info += pInfo;
                    info += "}\n";
                }
            }
            return info;
        }
    }

}
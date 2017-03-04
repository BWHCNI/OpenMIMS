package com.nrims.data;

import java.io.*;
import java.util.HashMap;

/**
 * Class responsible for opening MIMS files.
 *
 *
 */
public interface Opener {

    // Mims specific fields.
    public static final String Nrrd_separator = ":=";
    public static final String Mims_mass_numbers = "Mims_mass_numbers";
    public static final String Mims_mass_symbols = "Mims_mass_symbols";
    public static final String Mims_position = "Mims_position";
    public static final String Mims_date = "Mims_date";
    public static final String Mims_hour = "Mims_hour";
    public static final String Mims_user_name = "Mims_user_name";
    public static final String Mims_sample_name = "Mims_sample_name";
    public static final String Mims_z_position = "Mims_z_position";
    public static final String Mims_dwell_time = "Mims_dwell_time";
    public static final String Mims_count_time = "Mims_count_time";
    public static final String Mims_duration = "Mims_duration";
    public static final String Mims_raster = "Mims_raster";
    public static final String Mims_pixel_width = "Mims_pixel_width";
    public static final String Mims_pixel_height = "Mims_pixel_height";
    public static final String Mims_notes = "Mims_notes";
    public static final String Mims_dt_correction_applied = "Mims_deadtime_correction_applied";
    public static final String Mims_QSA_correction_applied = "Mims_QSA_correction_applied";
    public static final String Mims_QSA_betas = "Mims_QSA_betas";
    public static final String Mims_QSA_FC_Obj = "Mims_QSA_FC_Obj";
    public static final String Mims_prototype = "Mims_prototype";
    public static final String Mims_tile_positions = "Mims_tile_positions";
    public static final String Mims_stack_positions = "Mims_stack_positions";
    public static final String Mims_BField = "Mims_BField";
    public static final String Mims_pszComment = "Mims_pszComment";
    public static final String Mims_PrimCurrentT0 = "Mims_PrimCurrentT0";
    public static final String Mims_PrimCurrentTEnd = "Mims_PrimCurrentTEnd";
    public static final String Mims_ESPos = "Mims_ESPos";
    public static final String Mims_ASPos = "Mims_ASPos";
    public static final String Mims_D1Pos = "Mims_D1Pos";
    public static final String Mims_Radius = "Mims_Radius";

    //DJ: 10/13/2014
    public static final String Mims_PrimL1 = "Mims_PrimL1";
    public static final String Mims_PrimL0 = "Mims_PrimL0";
    public static final String Mims_CsHv = "Mims_CsHv";

    // Field that do not define parameters about the image
    // but are still userful. Not found in original IM header.
    // In theory, Mims_notes, Mims_tile_positions
    // and QSA and deadtime parameters probably belong here.
    public static final String Max_Tracking_Delta = "max_tracking_delta";

    // Worker functions.
    public File getImageFile();

    public int getNMasses();

    public int getNImages();

    public int getPreviousNImages();

    public int getWidth();

    public int getHeight();

    public Object getPixels(int index) throws IndexOutOfBoundsException, IOException;

    public float getPixelWidth();

    public float getPixelHeight();

    public void setStackIndex(int currentIndex);

    public void close();

    public boolean performFileSanityCheck();

    public boolean getIsHeaderBad();

    public void setIsHeaderBad(boolean headerState);

    public boolean getWasHeaderFixed();

    public void setWasHeaderFixed(boolean headerFixedState);

    public boolean fixBadHeader();

    public void setWidth(int width);

    public void setHeight(int height);

    public void setNMasses(int nmasses);

    public void setNImages(int nimages);

    public void setBitsPerPixel(short bitperpixel);

    // Metadata
    public String getPosition();

    public String[] getMassNames();
    
    public void setMassNames(String[] names);

    public String[] getMassSymbols();
    
    public void setMassSymbols(String[] symbols);

    public String getSampleDate();

    public String getSampleHour();

    public String getUserName();

    public String getSampleName();

    public String getDwellTime();

    public double getCountTime();

    public String getDuration();

    public String getRaster();

    public String getNotes();

    public String getZPosition();

    public int getFileType();

    public long getHeaderSize();

    public short getBitsPerPixel();

    public boolean isDTCorrected();

    public boolean isQSACorrected();

    public float[] getBetas();

    public float getFCObjective();

    public boolean isPrototype();

    public String[] getTilePositions();

    public String[] getStackPositions();

    public HashMap getMetaDataKeyValuePairs();

    public String getBField();

    public String getpszComment();

    public String getPrimCurrentT0();

    public String getPrimCurrentTEnd();

    public String getESPos();

    public String getASPos();

    public String getD1Pos();

    public String getRadius();

    public String getNPrimL1(); //DJ: 10/13/2014

    public String getNPrimL0(); //DJ: 10/13/2014

    public String getNCsHv();   //DJ: 10/13/2014

    //Setable metadata
    public void setStackPositions(String[] names);

    public void setNotes(String notes);

    public void setIsDTCorrected(boolean isDTCorrected);

    public void setIsQSACorrected(boolean isQSACorrected);

    public void setBetas(float[] betas);

    public void setFCObjective(float fc_objective);

    public void setMetaDataKeyValuePairs(HashMap metaData);

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
        String charge;
        String massLabel;
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
            //if (charge != 0) {
            //    info += "PolyAtomic.charge=" + charge + "\n";
            //}
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
        int sample_type;
        int data_included;
        int sple_pos_x;
        int sple_pos_y;
        int pos_z;
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
            if (pos_z != 0) {
                info += "Analysis.PosistionZ=" + pos_z + "\n";
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
     * defines a structure for saving the Mask_ls data
     */
    class MaskSampleStageImage {

        String filename;            // File name
        int analysis_duration;      // Analysis duration in mn
        int type;                   // (0=Sample scan/1=Beam Scan/2=Image Scan)
        int nb_zones;               // Number of step
        int step_unit_x;            // X step unit
        int step_unit_y;            // Y step unit
        int step_reel_d;            // Distance between two points
        double wt_int_zones;        // Waiting time between two points
        int nNbCycle;               // Number of cycles
        int beam_blanking;          // Beam blanking (0=No/1=Yes)
        int pulverisation;          // Pulverisation (0=No/1=Yes)
        int pulve_duration;         // Pulverisation duration in s
        int auto_cal_in_anal;       // (0=No/1=Yes)
        AutoCal autocal;            // Param mass calibration
        int hv_sple_control;        // (0=No/1=Yes)
        HvControl hvcontrol;           // Param Hv Control
        int sig_reference;          // (0=No/1=Yes)
        SigRef sig_ref;             // Param signal reference
        int nb_mass;                // Number of masses

        public String getInfo() {

            String info = "";
            info += "MaskImage.Filename=" + filename + "\n";
            info += "MaskImage.Duration=" + analysis_duration + "\n";
            info += "MaskImage.NumberSteps=" + nb_zones + "\n";
            info += "MaskImage.StepX=" + step_unit_x + "\n";
            info += "MaskImage.StepY=" + step_unit_y + "\n";
            info += "MaskImage.StepDistance=" + step_reel_d + "\n";
            info += "MaskImage.WaitTime=" + wt_int_zones + "\n";
            info += "MaskImage.NumCycles=" + nNbCycle + "\n";
            info += "MaskImage.BeamBlanking=" + beam_blanking + "\n";
            info += "MaskImage.Sputtering=" + pulverisation + "\n";
            info += "MaskImage.SputteringDuration=" + pulve_duration + "\n";
            info += "MaskImage.AutoCalibration=" + auto_cal_in_anal + "\n";
            info += "MaskImage.HvControl=" + hv_sple_control + "\n";
            info += "MaskImage.SigReference=" + sig_reference + "\n";
            info += "MaskImage.NumMasses=" + nb_mass + "\n";

            if (autocal != null) {
                String aInfo = autocal.getInfo();
                if (aInfo.length() > 0) {
                    info += "MasImage.AutoCal={\n";
                    info += aInfo;
                    info += "}\n";
                }
            }

            if (hvcontrol != null) {
                String hInfo = hvcontrol.getInfo();
                if (hInfo.length() > 0) {
                    info += "MaskImage.HvControl={\n";
                    info += hInfo;
                    info += "}\n";
                }
            }

            if (sig_ref != null) {
                String sInfo = sig_ref.getInfo();
                if (sInfo.length() > 0) {
                    info += "MaskImage.SigRef={\n";
                    info += sInfo;
                    info += "}\n";
                }
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
        int sig_reference;
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

    class HvControl {

        String mass;
        /* mass reference included in tab_mass*/
        int debut;
        /* control start - in cycle */
        int period;
        /* control period - in cycle */
        double borne_inf;
        /* low limit - in volt */
        double borne_sup;
        /* high limit - in volt */
        double pas;
        /* step - in volt */
        int largeur_bp;
        /* width bande passante - en eV */
        double count_time;

        /* sec */
        public String getInfo() {
            String info = "";
            info += "HvCont.Mass=" + mass + "\n";
            info += "HvCont.Start=" + debut + "\n";
            info += "HvCont.Period=" + period + "\n";
            info += "HvCont.VoltHigh=" + borne_inf + "\n";
            info += "HvCont.VoltLow=" + borne_sup + "\n";
            info += "HvCont.Step=" + pas + "\n";
            info += "HvCont.WidthBandPass=" + largeur_bp + "\n";
            info += "HvCont.CountTime=" + count_time + "\n";
            return info;
        }

    }

    class CalCond {

        int n_delta;
        int np_delta;
        int tps_comptage;
        int nb_cycles;
        double no_used2;
        double cal_ref_mass;
        String symbol;
    }

    class PolyList {

        String structname;
        int nb_poly;
        PolyAtomic polyatomic;
    }

    class MaskNano {

        int nVersion;
        int nRegulationMode;
        int nMode;
        int nGrainMode;
        int nSemiGraphicMode;
        int nDeltaX;
        int nDeltaY;
        int nNX_max;
        int nNY_max;
        int nNX_low;
        int nNX_high;
        int nNY_low;
        int nNY_high;
        int nNX_lowB;
        int nNX_highB;
        int nNY_lowB;
        int nNY_highB;
        int nType_detector;
        int nElectron_scan;
        int nScanning_mode;
        int nBlanking_comptage;
        int nCheckAvailable;
        int nCheckStart;
        int nCheckFrequency;
        int nNbBField;
        int nPrintRed;
        TabBFieldNano BFieldTab;
        SecIonBeamNano horizontalSibcParam, verticalSibcParam;
        int nSibcBfieldInd;
        int nSibcSelected;
        EnergyNano AecParam;
        int nAecBFieldInd;
        int nAecSelected;
        int nAecFrequency;
        int nE0SCenterBFieldInd;
        E0SNano E0SCenterParam;
        int nE0SCenterSelected;
        int nAecWt;
        int nPreSputRaster;
        int nE0pOffsetForCent;
        int nE0sCenterNbPoints;
        int nBaselineNbMeas;
        double dBaselinePdOffset;
        int nBaselineFrequency;
        String zDummy;
    }

    class TabBFieldNano {

        int nFlagBFieldSelected;
        int nBField;
        int nWT;
        int nCTperPixel;
        double dCTperPoint;
        int nComputed;
        int nE0xOffset;
        int nQVal;
        int nLF4Val;
        int nHexVal;
        int nNbFrame;
        double noused;
        TabTrolleyNano mTrolleyTab;
        PHDTrolleyNano PhdTrolleyTab;
    }

    class PHDTrolleyNano {

        int nIsUsedForPhdScan;
        int nStartDacThr;
        int nDacStep;
        int nPointNumber;
        int nCountTime;
        int nNbscan;
    }

    class TabTrolleyNano {

        String pszSymbol;
        double dAMU;
        double dRadius;
        int nNegPlate;
        int nPosPlate;
        int nDetector;
        int nOutSlit;
        int nFlagTrolleyValidad;
        int nNum;
        int nPicNum;
        int nRefPicNum;
        double dPolarizationVal;
        double dStartVoltage;
        int nStartDacPlate1;
        int nStartDacPlate2;
        int nDacStep;
        int nPointNumber;
        int nCountTime;
        int nIsUsedForBaseline;
        double d50PercentWidth;
        int nEdgesMethod;
        int nApcCountTime;
        int nIsUsedForSecIonBeamCentering;
        int nUnitCorrection;
        double deflectionVal;
        int nIsUsedForEnergycentering;
        int nIsUsedForE0Centering;
    }

    class E0SNano {

        int nDetId;
        int nStartDac;
        int nDacStep;
        int nE0SCentCountTime;
        double dStartValue;
        double d80PercentWidth;
    }

    class AnalyticalParamNano {

        String pszNomStruct;
        int nRelease;
        int nIsN50Large;
        int notused1;
        int notused2;
        String pszComment;
        ApPrimaryNano prim;
        ApSecondaryNano seco;
    }

    class ApPrimaryNano {

        String pszIon;
        /* Source */
        int nPrimCurrentT0;
        /* Primary current at t = 0 in pA */
        int nPrimCurrentTEnd;
        /* Primary current at t = End in pA */
        int nPrimLDuo;
        /* LDuo in V */
        int nPrimL1;
        /* L1 in V */
        int nDduoPos;
        /* Dduo Position : 0(not used)/1/2/3/4 */
        int nDduoTabValue;
        /* Positions values*/
        int nD0Pos;
        /* D0 Position : 0(not used)/1/2/3/4 */
        int nD0TabValue;
        /* Positions values */
        int nD1Pos;
        /* D1 Position : 0(not used)/1/2/3/4 */
        int nD1TabValue;
        /* Positions values */
        double dRaster;
        /* Raster size in um */
        double dOct45;
        /* Octopole 45 in V */
        double dOct90;
        /* Octopole 90 in V */
        double dPrimaryFoc;
        /* E0P in V */
        String pszAnalChamberPres;
        /* Analysis chamber pressure in torr*/
 /*------------------------------- RELEASE 3 -------------------------------*/
        int nPrimL0;
        /* L0 in V */
 /*------------------------------- RELEASE 4 -------------------------------*/
        int nCsHv;
        /* Cs Hv in V */
        int nDuoHv;
        /* Duo Hv in V */
        int nUnusedTab;
        /* Unused*/

    }

    class ApSecondaryNano {

        double dHVSample;
        /* E0W in V */
        int nESPos;
        /* Entrance Slit Position :  0(not used)/1/2/3/4/5 */
        int nESTabWidthValue;
        /* Entrance Slit Positions Width */
        int nESTabHeightValue;
        /* Entrance Slit Positions Height */
        int nASPos;
        /* Aperture Slit Position :  0(not used)/1/2/3/4/5 */
        int nASTabWidthValue;
        /* Aperture Slit positions Width */
        int nASTabHeightValue;
        /* Aperture Slit positions Height */
        double dEnrjSPosValue;
        /* Energy Slit Position */
        double dEnrjSWidthValue;
        /* Energy Slit Width */
        int nExSFCPos;
        /* Exit Slit FC Position :1/2/3 */
        int nExSFCType;
        /* Exit Slit FC type : 0 Normal/1 Large */
        int nExSFCTabWidthValue;
        /* Exit Slit FC positions Width : [ Type ][ Pos ] */
        int nExSFCTabHeightValue;
        /* Exit Slit FC positions Height : [ Type ][ Pos ] */
        int nExSEM1Pos;
        /* Exit Slit EM1 Position :1/2/3 */
        int nExSEM1Type;
        /* Exit Sd ../aplit EM1 Type : 0 Normal/1 Large*/
        int nExSEM1TabWidthValue;
        /* Exit Slit EM1 positions Width : [ Type ][ Pos ] */
        int nExSEM1TabHeightValue;
        /* Exit Slit EM1 positions Height : [ Type ][ Pos ] */
        int nExSEM2Pos;
        /* Exit Slit EM2 Position :1/2/3 */
        int nExSEM2Type;
        /* Exit Slit EM2 Type : 0 Normal/1 Large*/
        int nExSEM2TabWidthValue;
        /* Exit Slit EM2 positions Width : [ Type ][ Pos ] */
        int nExSEM2TabHeightValue;
        /* Exit Slit EM2 positions Height : [ Type ][ Pos ] */
        int nExSEM3Pos;
        /* Exit Slit EM3 Position :1/2/3 */
        int nExSEM3Type;
        /* Exit Slit EM3 Type : 0 Normal/1 Large*/
        int nExSEM3TabWidthValue;
        /* Exit Slit EM3 positions Width : [ Type ][ Pos ] */
        int nExSEM3TabHeightValue;
        /* Exit Slit EM3 positions Height : [ Type ][ Pos ] */
        int nExSEM4Pos;
        /* Exit Slit EM4 Position :1/2/3 */
        int nExSEM4Type;
        /* Exit Slit EM4 Type : 0 Normal/1 Large*/
        int nExSEM4TabWidthValue;
        /* Exit Slit EM4 positions Width : [ Type ][ Pos ] */
        int nExSEM4TabHeightValue;
        /* Exit Slit EM4 positions Height : [ Type ][ Pos ] */
        int nExSEM5Pos;
        /* Exit Slit EM5 Position :1/2/3 */
        int nExSEM5Type;
        /* Exit Slit EM5 Type : 0 Normal/1 Large*/
        int nExSEM5TabWidthValue;
        /* Exit Slit EM5 positions Width : [ Type ][ Pos ] */
        int nExSEM5TabHeightValue;
        /* Exit Slit EM5 positions Height : [ Type ][ Pos ] */
        double dExSLDWidhtPos;
        /* Exit Slit LD slit vernier position */
        double dExSLDWidhtValueA;
        /* Exit Slit LD coefficient A */
        double dExSLDWidhtValueB;
        /* Exit Slit LD coefficient B */
        double dSecondaryFoc;
        /* E0S in V */
        String pszMultiColChamPres;
        /* Multicollection chamber */
        int nFCsPosBackground;
        /* FCS Positive Background */
        int nFCsNegBackground;
        /* FCS Negative Background */
        double dEM1Yield;
        /* EM1 Yield */
        int nEM1Background;
        /* EM1 Background */
        int nEM1DeadTime;
        /* EM1 Dead Time */
        double dEM2Yield;
        /* EM2 Yield */
        int nEM2Background;
        /* EM2 Background */
        int nEM2DeadTime;
        /* EM2 Dead Time */
        double dEM3Yield;
        /* EM3 Yield */
        int nEM3Background;
        /* EM3 Background */
        int nEM3DeadTime;
        /* EM3 Dead Time */
        double dEM4Yield;
        /* EM4 Yield */
        int nEM4Background;
        /* EM4 Background */
        int nEM4DeadTime;
        /* EM4 Dead Time */
        double dEM5Yield;
        /* EM5 Yield */
        int nEM5Background;
        /* EM5 Background */
        int nEM5DeadTime;
        /* EM5 Dead Time */
        double dLDYield;
        /* LD Yield */
        int nLDBackground;
        /* LD Background */
        int nLDDeadTime;
        /* LD Dead Time */
        int nExSEM4BPos;
        /* Exit Slit EM4B Position :1/2/3 */
        int nExSEM4BType;
        /* Exit Slit EM4B Type : 0 Normal / 1 Large */
        int nExSEM4BTabWidthValue;
        /* Exit Slit EM4B positions Width : [ Type ] [ Pos ] */
        int nExSEM4BTabHeightValue;/* Exit Slit EM4B positions Height : [ Type ] [ Pos ] */
        double dEM4BYield;
        /* EM4B Yield */
        int nEM4BBackground;
        /* EM4B Background */
        int nEM4BDeadTime;
        /* EM4B Dead Time */
        int nUnusedTab;
        /* Unused */

    }

    /**
     * Contains the Secondary Ion Beam Centering parameter information.(size = 40 bytes)
     */
    class SecIonBeamNano {

        int m_nDetId;
        /* Detector Id (-1 If none)*/
        int m_nStartDac;
        /* Start DAC */
        int m_nDacStep;
        /* Dac step */
        int m_nUnused1;
        /* For Byte Alignement */

 /* For Automatic Beam Centering */
        double m_dStartValue;
        /* Start value */
        double m_d50PerCentWidth;
        /* 50% Width in Volts */
        int m_nAbcCountTime;
        /* Counting Time per point multiple of 10ms */
        int m_nUnused2;
        /* For Byte Alignement */

    }

    /**
     * Contains the Energy Centering parameter information.(size = 40 bytes)
     */
    class EnergyNano {

        /* Energy Acquisition */
        int m_nDetId;
        /* Detector Id (-1 If none)*/
        int m_nStartDac;
        /* Start DAC */
        int m_nDacStep;
        /* Dac step */
        int m_nUnused1;
        /* For Byte Alignement */

 /* For Automatic Centering */
        double m_dStartValue;
        /* Start value */
        double m_dDelta;
        /* Delta between max and 10% in Volts */
        int m_nAecCountTime;
        /* Counting Time per point multiple of 10ms */
        int m_nUnused2;
        /* For Byte Alignement */

    }

    /**
     * Contains analytical parameters information.
     */
    class AnalysisParamNano {

        String pszNomStruct;
        /* Structure name */
        int nRelease;
        /* Data release version */
        int nIsN50Large;
        /* N50 Large Flag 0=Standard/1=Large */
        int nUnused2;
        /* Unused */
        int nUnused3;
        /* Unused */
        String pszComment;
        /* Comment */
        ApPrimaryNano prim;
        /* Primary Analytical Parameters */
        ApSecondaryNano seco;
        /* Secondary Analytical Parameters */

    }

    class DefAnalysisBis {

        int magic;
        /* Magic number = MAGIC_DEFB=2306 */
        int release;
        /* Version = RELEASE_DEFB=1 */
        String filename;
        /* file name */
        String matrice;
        /* matrix name */
        int sigref_mode;
        /* 0: manual 1: auto */
        int sigref_nbptsdm;
        /* sigref auto nbpts deltam */
        int sigref_nbdm;
        /* sigref auto nb deltam */
        int sigref_ct_scan;
        /* scanning count tim x 0,1sec */
        int sigref_ct_meas;
        /* measuring count tim sec */
        int sigref_tps_pulve;
        /* time beam=ON during sigref */
        int eps_recentrage;
        /* EPS Centering 0=NO/1=YES */
        int eps_flag;
        /* EPS 0=NO/ 1=YES */
        DefEps eps;
        /* IMS peak switching */
        int sple_rot_flag;
        /* sample rotation 0=NO/ 1=YES */
        int sple_rot_speed;
        /* rot speed tr/mn */
        int sple_rot_acq_sync;
        /* 0:no 1:yes */
        String sample_name;
        /* sample name */
        String experience;
        /* experience name */
        String method;
        /* method name */
        String no_used;

    }

    /**
     * Contains new analytical parameters information.
     */
    class AnalysisParam {

        /*------------------------------- RELEASE 5 -------------------------------*/
        String pszNomStruct;
        /* Structure name Anal_param_nano_bis *//* Det 6 */
        int nExSEM6Pos;
        /* Exit Slit EM6 Position :1/2/3 */
        int nExSEM6Type;
        /* Exit Slit EM6 Type : 0 Normal / 1 Large */
        int nExSEM6TabWidthValue;
        /* Exit Slit EM6 positions Width : [ Type ] [ Pos ] */
        int nExSEM6TabHeightValue;
        /* Height : [ Type ] [ Pos ] */
        double dEM6Yield;
        /* EM6 Yield */
        int nEM6Background;
        /* EM6 Background */
        int nEM6DeadTime;
        /* EM6 Dead Time */

 /* Det 7 */
        int nExSEM7Pos;
        /* Exit Slit EM7 Position :1/2/3 */
        int nExSEM7Type;
        /* Exit Slit EM7 Type : 0 Normal / 1 Large */
        int nExSEM7TabWidthValue;
        /* Exit Slit EM7 positions Width : [ Type ] [ Pos ] */
        int nExSEM7TabHeightValue;
        /* Height : [ Type ] [ Pos ] */
        double dEM7Yield;
        /* EM7 Yield */
        int nEM7Background;
        /* EM7 Background */
        int nEM7DeadTime;
        /* EM7 Dead Time */

 /* Exit Slit XLarge */
 /* XLarge Exit Slit EM1 positions Width & Height : [ Pos ] */
        int nXlExSEM1TabWidthValue;
        int nXlExSEM1TabHeightValue;
        /* XLarge Exit Slit EM2 positions Width & Height : [ Pos ] */
        int nXlExSEM2TabWidthValue;
        int nXlExSEM2TabHeightValue;
        /* XLarge Exit Slit EM3 positions Width & Height : [ Pos ] */
        int nXlExSEM3TabWidthValue;
        int nXlExSEM3TabHeightValue;
        /* XLarge Exit Slit EM4 positions Width & Height : [ Pos ] */
        int nXlExSEM4TabWidthValue;
        int nXlExSEM4TabHeightValue;
        /* XLarge Exit Slit EM5 positions Width & Height : [ Pos ] */
        int nXlExSEM5TabWidthValue;
        int nXlExSEM5TabHeightValue;
        /* XLarge Exit Slit EM6 positions Width & Height : [ Pos ] */
        int nXlExSEM6TabWidthValue;
        int nXlExSEM6TabHeightValue;
        /* XLarge Exit Slit EM7 positions Width & Height : [ Pos ] */
        int nXlExSEM7TabWidthValue;
        int nXlExSEM7TabHeightValue;

        /* Pre sput Preset */
        ApPresetSlit PreSputPresetSlit;
        ApPresetLens PreSputPresetLens;

        /* Acq Preset */
        ApPresetSlit AcqPresetSlit;
        ApPresetLens AcqPresetLens;

        /*------------------------------- RELEASE 6 -------------------------------*/
        double dEMTICYield;
        /* EMTIC Yield */
        int nEMTICBackground;
        /* EMTIC Background */
        int nEMTICDeadTime;
        /* EMTIC Dead Time */
        int nFC1PosBackground;
        /* FC1 Positive Background */
        int nFC1NegBackground;
        /* FC1 Negative Background */
        int nFC2PosBackground;
        /* FC2 Positive Background */
        int nFC2NegBackground;
        /* FC2 Negative Background */
        int nFC3PosBackground;
        /* FC3 Positive Background */
        int nFC3NegBackground;
        /* FC3 Negative Background */
        int nFC4PosBackground;
        /* FC4 Positive Background */
        int nFC4NegBackground;
        /* FC4 Negative Background */
        int nFC5PosBackground;
        /* FC5 Positive Background */
        int nFC5NegBackground;
        /* FC5 Negative Background */
        int nFC6PosBackground;
        /* FC6 Positive Background */
        int nFC6NegBackground;
        /* FC6 Negative Background */
        int nFC7PosBackground;
        /* FC7 Positive Background */
        int nFC7NegBackground;
        /* FC7 Negative Background */
        int nDet1Type;
        /* Det type (0=EM/1=FC) */
        int nDet2Type;
        /* Det type (0=EM/1=FC) */
        int nDet3Type;
        /* Det type (0=EM/1=FC) */
        int nDet4Type;
        /* Det type (0=EM/1=FC) */
        int nDet5Type;
        /* Det type (0=EM/1=FC) */
        int nDet6Type;
        /* Det type (0=EM/1=FC) */
        int nDet7Type;
        /* Det type (0=EM/1=FC) */
        int nUnusedTab;

    }

    /**
     * Contains IMS EPS definition information.
     */
    class DefEps {

        int central_energy;
        int field;
        /* central mass b field */
        PolyAtomic central_mass;
        /* central mass */
        PolyAtomic reference_mass;
        /* reference mass */
        double tens_tube;
        /* of the reference mass */
        double max_var_tens_tube;

    }

    /**
     * Contains Preset lens definition.
     */
    class ApPresetLens {

        ApPresetDef PresetInfo;
        /* Preset definition */
        ApParamPreset ParamTab;
        /* Preset param */

    }

    /**
     * Contains Preset Slit definition.
     */
    class ApPresetSlit {

        ApPresetDef PresetInfo;
        /* Preset definition */
        ApParamPreset ParamTab;
        /* Preset param */

    }

    /**
     * Contains parameters for Preset information.
     */
    class ApParamPreset {

        int nId;
        /* Parameter Id (par_dfp.h) */
        int nValue;
        /* Parameter DAC value */
        String szName;
        /* Parameter name */

    }

    /**
     * Contains Preset definition.
     */
    class ApPresetDef {

        String szFileName;
        /* ISF File name */
        String szName;
        /* Preset name */
        String szDateCalib;
        /* Preset name */
        int nIsSelected;
        /* Selection flag */
        int nNbParam;
        /* Nb param preset */

    }

}

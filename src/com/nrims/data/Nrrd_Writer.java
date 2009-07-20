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

// v0.1 2007-04-02
// - First functional version can write single channel image (stack)
// to raw/gzip encoded monolithic nrrd file
// - Writes key spatial calibration information	including
//   spacings, centers, units, axis mins

// TODO
// - Support for multichannel images, time data
// - option to write a detached header instead of detached nrrd file

// NB this class can be used to create detached headers for other file types
// See 

import com.nrims.UI;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.io.ImageWriter;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

import java.io.*;
import java.util.Date;
                          
public class Nrrd_Writer implements PlugIn {

    static UI ui;
    
    public Nrrd_Writer(UI ui) {
       this.ui=ui;
    }

	private static final String plugInName = "Nrrd Writer";
	private static final String noImages = plugInName+"...\n"+ "No images are open.";
	private static final String supportedTypes =
		plugInName+"..." + "Supported types:\n\n" +
				"32-bit Grayscale float : FLOAT\n" +
				"(32-bit Grayscale integer) : LONG\n" +
				"16-bit Grayscale integer: INT\n" +
				"(16-bit Grayscale unsigned integer) : UINT\n"+
				"8-bit Grayscale : BYTE\n"+
				"8-bit Colour LUT (converted to greyscale): BYTE\n";
				
	public static final int NRRD_VERSION = 4;	
	private String imgTypeString=null;	
	String nrrdEncoding="raw";
	// See http://teem.sourceforge.net/nrrd/format.html#centers
	static final String defaultNrrdCentering="node";	
						
	public void run(String arg) {
		//ImagePlus imp = WindowManager.getCurrentImage();
        ImagePlus[] imp = ui.getOpenMassImages();

		if (imp == null) {
			IJ.showMessage(noImages);
			return;
		}

		String name = arg;
		if (arg == null || arg.equals("")) {
			name = ui.getImageFilePrefix();
		}
		
		SaveDialog sd = new SaveDialog(plugInName+"...", name, ".nrrd");
		String file = sd.getFileName();
		if (file == null) return;
		String directory = sd.getDirectory();
		save(imp, directory, file);
	}

	public void save(ImagePlus[] imp, String directory, String file) {
		if (imp == null) {
			IJ.showMessage(noImages);
			return;
		}

        // Get FileInfo for each image although only the one is rfeally needed for the header.
		FileInfo[] fi = new FileInfo[ui.getOpener().getNMasses()];
        for (int i = 0; i < fi.length; i++) {
            fi[i] = imp[i].getFileInfo();
        }

		// Make sure that we can save this kind of image
		if(imgTypeString==null) {
			imgTypeString=imgType(fi[0].fileType);
			if (imgTypeString.equals("unsupported")) {
				IJ.showMessage(supportedTypes);
				return;
			}
		}		
		// Set the fileName stored in the file info record to the
		// file name that was passed in or chosen in the dialog box
		fi[0].fileName=file;
		fi[0].directory=directory;

        // Get calibration for each image.
        Calibration[] cal = new Calibration[ui.getOpener().getNMasses()];
        for (int i = 0; i < fi.length; i++) {
            cal[i] = imp[i].getCalibration();
        }
		
		// Actually write out the image
		try {
			writeImage(fi, cal);
		} catch (IOException e) {
			IJ.error("An error occured writing the file.\n \n" + e);
			IJ.showStatus("");
		}
	}
	void writeImage(FileInfo[] fis, Calibration[] cals) throws IOException {

        FileInfo fi = fis[0];
        Calibration cal = cals[0];

		FileOutputStream out = new FileOutputStream(new File(fi.directory, fi.fileName));

        // First write out the full header
		Writer bw = new BufferedWriter(new OutputStreamWriter(out));
		bw.write(makeHeader(fi,cal));
		
        // Write Mims specific fields.
        bw.write(getMimsKeyValuePairs()+"\n");

        // Flush rather than close
		bw.flush();		

		// Then the image data
		ImageWriter[] writer = new ImageWriter[fis.length];
        for (int i = 0; i < writer.length; i++) {
            writer[i] = new ImageWriter(fis[i]);
            writer[i].write(out);
        }

        out.close();
			
		IJ.showStatus("Saved "+ fi.fileName);
	}
		
	public static String makeHeader(FileInfo fi, Calibration cal) {
		// NB You can add further fields to this basic header but 
		// You MUST add your own blank line at the end
		StringWriter out=new StringWriter();
		
		out.write("NRRD000"+NRRD_VERSION+"\n");
		out.write("# Created by Nrrd_Writer at "+(new Date())+"\n");

		// Fetch and write the data type
		out.write("type: "+imgType(fi.fileType)+"\n");
		// Fetch and write the encoding
		out.write("encoding: "+getEncoding(fi)+"\n");
		
		if(fi.intelByteOrder) out.write("endian: little\n");
		else out.write("endian: big\n");
		
		int dimension=(fi.nImages==1)?2:3;
        dimension=4;

        // Diension.
        // out.write("sizes: "+fi.width+" "+fi.height+" "+fi.nImages+" "+ui.getOpener().getNMasses()+"\n");
		out.write("dimension: "+dimension+"\n");
		
        // Sizes.
        //out.write(dimmedLine("sizes",dimension,fi.width+"",fi.height+"",fi.nImages+""));
        out.write("sizes: "+fi.width+" "+fi.height+" "+fi.nImages+" "+ui.getOpener().getNMasses()+"\n");

        // Kinds.
        out.write("kinds: space space space list\n");

        // Calibration.
        if(cal!=null)
			//out.write(dimmedLine("spacings",dimension,cal.pixelWidth+"",cal.pixelHeight+"",cal.pixelDepth+""));
            out.write("spacings: "+cal.pixelWidth+" "+cal.pixelHeight+" "+cal.pixelDepth+" NaN\n");
		
        // Centers.
        // GJ: It's my understanding that ImageJ operates on a 'node' basis
		// See http://teem.sourceforge.net/nrrd/format.html#centers
		//out.write(dimmedLine("centers",dimension,defaultNrrdCentering,defaultNrrdCentering,"node"));
        out.write("centers: node node node node\n");

        // Units.
		String units;
		if(cal!=null) units=cal.getUnit();
		else units=fi.unit;
		if(units.equals("�m")) units="microns";
		//if(!units.equals("")) out.write(dimmedQuotedLine("units",dimension,units,units,units));
        if(!units.equals("")) out.write("units: \"pixel\" \"pixel\" \"pixel\" \"pixel\"\n");

        // Axis.
		// Only write axis mins if origin info has at least one non-zero
		// element
		if(cal!=null && (cal.xOrigin!=0 || cal.yOrigin!=0 || cal.zOrigin!=0) ) {
			out.write(dimmedLine("axis mins",dimension,(cal.xOrigin*cal.pixelWidth)+"",
								 (cal.yOrigin*cal.pixelHeight)+"",(cal.zOrigin*cal.pixelDepth)+""));
		}       

		return out.toString();
    }

    private String getMimsKeyValuePairs() {
       StringWriter out=new StringWriter();

       // Write Mims specific key/value pairs.

       // Mass numbers
       String line = "MIMS_mass_numbers:=";
       for (int i=0; i<ui.getOpener().getNMasses(); i++)
          line += ui.getOpener().getMassNames()[i]+" ";
       out.write(line+"\n");

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
		
		if (IJ.debugMode) IJ.log("fi :"+fi);
		
		try {
			nfi=(NrrdFileInfo) fi;
			if (IJ.debugMode) IJ.log("nfi :"+nfi);
			if(nfi.encoding!=null && !nfi.encoding.equals("")) return (nfi.encoding);
		} catch (Exception e) { }
		
		switch(fi.compression) {
			case NrrdFileInfo.GZIP: return("gzip");
			case NrrdFileInfo.BZIP2: return null;
			default:
			break;
		}
		// These aren't yet supported
		switch(fi.fileFormat) {
			case NrrdFileInfo.NRRD_TEXT:
			case NrrdFileInfo.NRRD_HEX:
			return(null);
			default:
			break;
		}
		// The default!
		return "raw";
	}
			
	private static String dimmedQuotedLine(String tag,int dimension,String x1,String x2,String x3) {
		x1="\""+x1+"\"";
		x2="\""+x2+"\"";
		x3="\""+x3+"\"";
		return dimmedLine(tag, dimension,x1, x2, x3);
	}
	
	private static String dimmedLine(String tag,int dimension,String x1,String x2,String x3) {
		String rval=null;
		if(dimension==2) rval=tag+": "+x1+" "+x2+"\n";
		else if(dimension==3) rval=tag+": "+x1+" "+x2+" "+x3+"\n";
		return rval;
	}

}
class NrrdFileInfo extends FileInfo {
	public int dimension=0;
	public int[] sizes;
    public int nMasses=1;
	public String encoding="";
	public String[] centers=null;
    public String[] massNames;
	
	// Additional compression modes for fi.compression
	public static final int GZIP = 1001;
	public static final int ZLIB = 1002;
	public static final int BZIP2 = 1003;
	
	// Additional file formats for fi.fileFormat
	public static final int NRRD = 1001;
	public static final int NRRD_TEXT = 1002;
	public static final int NRRD_HEX = 1003;

}

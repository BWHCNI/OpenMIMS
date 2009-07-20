package com.nrims.data;

// Nrrd_Reader
// -----------

// (c) Gregory Jefferis 2007
// Department of Zoology, University of Cambridge
// jefferis@gmail.com
// All rights reserved
// Source code released under Lesser Gnu Public License v2

// TODO
// - Support for multichannel images
//   (problem is how to figure out they are multichannel in the absence of 
//   other info - not strictly required by nrrd format)
// - time datasets
// - line skip (only byte skip at present)
// - calculating spacing information from axis mins/cell info

// Compiling:
// You must compile Nrrd_Writer.java first because this plugin
// depends on the NrrdFileInfo class declared in that file

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

/**
 * ImageJ plugin to read a file in Gordon Kindlmann's NRRD 
 * or 'nearly raw raster data' format, a simple format which handles
 * coordinate systems and data types in a very general way.
 * See <A HREF="http://teem.sourceforge.net/nrrd">http://teem.sourceforge.net/nrrd</A>
 * and <A HREF="http://flybrain.stanford.edu/nrrd">http://flybrain.stanford.edu/nrrd</A>
 */

public class Nrrd_Reader extends ImagePlus implements Opener {

    public Nrrd_Reader(String[] imageFileNames) {
        run(imageFileNames);
    }

	public final String uint8Types="uchar, unsigned char, uint8, uint8_t";
	public final String int16Types="short, short int, signed short, signed short int, int16, int16_t";
	public final String uint16Types="ushort, unsigned short, unsigned short int, uint16, uint16_t";
	public final String int32Types="int, signed int, int32, int32_t";
	public final String uint32Types="uint, unsigned int, uint32, uint32_t";
    public final String MIMS_mass_numbers="MIMS_mass_numbers";
	private String notes = "";
	
	private boolean detachedHeader=false;
	public String headerPath=null;
	public String imagePath=null;
	public String imageName=null;

    private File file = null;
    private RandomAccessFile in;
    private long headerOffset;
    private int verbose = 0;
    private int width,  height,  nMasses,  nImages;
    private String[] massNames;
    private HeaderImage ihdr;
    private DefAnalysis dhdr;
    private TabMass[] tabMass;
    private MaskImage maskIm;
    private int currentMass = 0;
    private int currentIndex = 0;
    private ImagePlus[] imp;
	
	public void run(String imageFileNames[]) {

        String directory = "", name = imageFileNames[0];
        System.out.println(imageFileNames[0]);

        file = new File(name);
        directory = file.getParent();
        name = file.getName();

        String[] fnames = new String[imageFileNames.length];
        for(int i = 0; i < fnames.length; i++) {
            File tempfile = new File(imageFileNames[i]);
            fnames[i] = tempfile.getName();
        }
        
        //String[] test = {"m26test_file-16b-plugin.nrrd", "m27test_file-16b-plugin.nrrd"};
        //String testdir = "/nrims/home3/cpoczatek/test_images/nrrd_test/";
        if(fnames.length==1) {
            imp = load(directory, fnames[0]);
        } else {
            imp = load(directory, fnames);
        }
        //imp = load(testdir, test);

        System.out.println("imp = "+imp.toString());
		if (imp==null) return;  // failed to load the file		

        for (int i = 0; i < imp.length; i++) {
            if (imageName != null) {
                // set the name of the image to the name found inside the load method
                // TOFIX - what should the name be?  There could be several
                // image files referenced in a single detached .nhdr
                setStack(imageName, imp[i].getStack());
            } else {
                setStack(name, imp[i].getStack());
            }

            if (!notes.equals("")) {
                setProperty("Info", notes);
            }
            // bring over the calibration information as well
            copyScale(imp[i]);
        }
	}

	public ImagePlus[] load(String directory, String fileName) {

		if (!directory.endsWith(File.separator)) directory += File.separator;
		if ((fileName == null) || (fileName == "")) return null;
		
		
		NrrdFileInfo fi;
		try {
			fi=getHeaderInfo(directory, fileName);
            width=fi.width;
            height=fi.height;
            nImages=fi.nImages;
            nMasses=fi.nMasses;
            massNames=fi.massNames;
		}
		catch (IOException e) { 
			IJ.write("readHeader: "+ e.getMessage()); 
			return null;
		}
		if (IJ.debugMode) IJ.log("fi:"+fi);
		
		IJ.showStatus("Loading Nrrd File: " + directory + fileName);
		
		imp = new ImagePlus[nMasses];
        //????????
        


	    FileOpener fo = new FileOpener(fi);
        long newOffset = 0;
        headerOffset = fi.longOffset;
        for (int massindex = 0; massindex < this.nMasses; massindex++) {
            newOffset = headerOffset + 2 * massindex * (fi.width * fi.height * fi.nImages);
            fi.longOffset = newOffset;
            imp[massindex] = fo.open(false);
        }

		if(imp==null) return null;
		
		// Copy over the spatial scale info which we found in readHeader
		// nb the first we don't just overwrite the current calibration 
		// because this may have density calibration for signed images 
		
        Calibration cal = imp[0].getCalibration();
		Calibration spatialCal = this.getCalibration();
		cal.pixelWidth=spatialCal.pixelWidth;
		cal.pixelHeight=spatialCal.pixelHeight;        
		cal.pixelDepth=spatialCal.pixelDepth;
		cal.setUnit(spatialCal.getUnit());
		cal.xOrigin=spatialCal.xOrigin;		
		cal.yOrigin=spatialCal.yOrigin;		
		cal.zOrigin=spatialCal.zOrigin;
		try{
            imp[0].setCalibration(cal);
        } catch(Exception e) {
            System.out.println(e.toString());
        }

		return imp; 
	} 


    //load nrrds from multiple files
    //needs various checks
    public ImagePlus[] load(String directory, String[] fileNames) {

		if (!directory.endsWith(File.separator)) directory += File.separator;
		if ((fileNames == null) || (fileNames[0] == "")) return null;
		int numFiles = fileNames.length;
        nMasses=numFiles;

		NrrdFileInfo[] fi = new NrrdFileInfo[numFiles];
		try {
            for(int i = 0; i < numFiles; i++) {
                fi[i]=getHeaderInfo(directory, fileNames[i]);
                width=fi[i].width;
                height=fi[i].height;
                nImages=fi[i].nImages;
            //nMasses=fi.nMasses;
            //???????
            }
		}
		catch (IOException e) {
			IJ.write("readHeader: "+ e.getMessage());
			return null;
		}
		if (IJ.debugMode) IJ.log("fi:"+fi);

        //??????????
		IJ.showStatus("Loading Nrrd File: " + directory + fileNames[0]);

		imp = new ImagePlus[numFiles];
        //????????


        for (int i = 0; i < numFiles; i++) {
            FileOpener fo = new FileOpener(fi[i]);
            imp[i] = fo.open(false);
            if (imp[i] == null) {
                return null;
            }

            // Copy over the spatial scale info which we found in readHeader
            // nb the first we don't just overwrite the current calibration
            // because this may have density calibration for signed images

            Calibration cal = imp[i].getCalibration();
            Calibration spatialCal = this.getCalibration();
            cal.pixelWidth = spatialCal.pixelWidth;
            cal.pixelHeight = spatialCal.pixelHeight;
            cal.pixelDepth = spatialCal.pixelDepth;
            cal.setUnit(spatialCal.getUnit());
            cal.xOrigin = spatialCal.xOrigin;
            cal.yOrigin = spatialCal.yOrigin;
            cal.zOrigin = spatialCal.zOrigin;
            try {
                imp[i].setCalibration(cal);
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
		return imp;
	}

	public NrrdFileInfo getHeaderInfo( String directory, String fileName ) throws IOException {

		if (IJ.debugMode) IJ.log("Entering Nrrd_Reader.readHeader():");
		NrrdFileInfo fi = new NrrdFileInfo();
		fi.directory=directory; fi.fileName=fileName;
		Calibration spatialCal = this.getCalibration();
		
		// NB Need RAF in order to ensure that we know file offset
		RandomAccessFile input = new RandomAccessFile(fi.directory+fi.fileName,"r");

		String thisLine,noteType,noteValue, noteValuelc;

        
		fi.fileType = FileInfo.GRAY16_UNSIGNED; // just assume this for the mo
		spatialCal.setUnit("micron");  // just assume this for the mo
        fi.nImages = 1;               // just assume this for the mo
		fi.fileFormat = FileInfo.RAW;
		

		// parse the header file, until reach an empty line//	boolean keepReading=true;
		while(true) {
			thisLine=input.readLine();
			if(thisLine==null || thisLine.equals("")) {
				if(!detachedHeader) fi.longOffset = input.getFilePointer();
				break;
			}		
			notes+=thisLine+"\n";

			if(thisLine.indexOf("#")==0) continue; // ignore comments

			noteType=getFieldPart(thisLine,0).toLowerCase(); // case irrelevant
			noteValue=getFieldPart(thisLine,1);
			noteValuelc=noteValue.toLowerCase();
			String firstNoteValue=getSubField(thisLine,0);
//			String firstNoteValuelc=firstNoteValue.toLowerCase();

			if (IJ.debugMode) IJ.log("NoteType:"+noteType+", noteValue:"+noteValue);

			if (noteType.equals("data file")||noteType.equals("datafile")) {
				// This is a detached header file
				// There are 3 kinds of specification for the data files
				// 	1.	data file: <filename>
				//	2.	data file: <format> <min> <max> <step> [<subdim>]
				//	3.	data file: LIST [<subdim>]
				if(firstNoteValue.equals("LIST")) {
					// TOFIX - type 3
					throw new IOException("Nrrd_Reader: not yet able to handle datafile: LIST specifications");
				} else if(!getSubField(thisLine,1).equals("")) {
					// TOFIX - type 2
					throw new IOException("Nrrd_Reader: not yet able to handle datafile: sprintf file specifications");
				} else {
					// Type 1 specification
					File imageFile;
					// Relative or absolute
					if(noteValue.indexOf("/")==0) {
						// absolute
						imageFile=new File(noteValue);
						// TOFIX could also check local directory if absolute path given
						// but dir does not exist
					} else {
						//IJ.log("fi.directory = "+fi.directory);					
						imageFile=new File(fi.directory,noteValue);
					}
					//IJ.log("image file ="+imageFile);

					if(imageFile.exists()) {
						fi.directory=imageFile.getParent();
						fi.fileName=imageFile.getName();
						imagePath=imageFile.getPath();
						detachedHeader=true;
					} else {
						throw new IOException("Unable to find image file ="+imageFile.getPath());
					}
				}										
			}

			if (noteType.equals("dimension")) {
				fi.dimension=Integer.valueOf(noteValue).intValue();
				//???????? add back dimension check?
                //if(fi.dimension>3) throw new IOException("Nrrd_Reader: Dimension>3 not yet implemented!");
			}
			if (noteType.equals("sizes")) {
				fi.sizes=new int[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					fi.sizes[i]=Integer.valueOf(getSubField(thisLine,i)).intValue();
					if(i==0) fi.width=fi.sizes[0];
					if(i==1) fi.height=fi.sizes[1];
					if(i==2) fi.nImages=fi.sizes[2];
                    if(i==3) fi.nMasses=fi.sizes[3];
				}
			}

			if (noteType.equals("units")) spatialCal.setUnit(firstNoteValue);
			if (noteType.equals("spacings")) {
				double[] spacings=new double[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					// TOFIX - this order of allocations is not a given!
					spacings[i]=Double.valueOf(getSubField(thisLine,i)).doubleValue();
					if(i==0) spatialCal.pixelWidth=spacings[0];
					if(i==1) spatialCal.pixelHeight=spacings[1];
					if(i==2) spatialCal.pixelDepth=spacings[2];
				}
			}
			if (noteType.equals("centers") || noteType.equals("centerings")) {
				fi.centers=new String[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					// TOFIX - this order of allocations is not a given!
					fi.centers[i]=getSubField(thisLine,i);
				}
			}

			if (noteType.equals("axis mins") || noteType.equals("axismins")) {
				double[] axismins=new double[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					// TOFIX - this order of allocations is not a given!
					// NB xOrigin are in pixels, whereas axismins are of course
					// in units; these are converted later
					axismins[i]=Double.valueOf(getSubField(thisLine,i)).doubleValue();
					if(i==0) spatialCal.xOrigin=axismins[0];
					if(i==1) spatialCal.yOrigin=axismins[1];
					if(i==2) spatialCal.zOrigin=axismins[2];
				}
			}
            
			if (noteType.equals("type")) {
				if (uint8Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY8;
				}
                //???????????????????
                //16 bit signed/unsigned checks were flipped?
                else if(uint16Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY16_UNSIGNED;
				} else if(int16Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY16_SIGNED;
				} else if(uint32Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY32_UNSIGNED;
				} else if(int32Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY32_INT;
				} else if(noteValuelc.equals("float")) {
					fi.fileType=FileInfo.GRAY32_FLOAT;
				} else if(noteValuelc.equals("double")) {
					fi.fileType=FileInfo.GRAY64_FLOAT;
				} else {
					throw new IOException("Unimplemented data type ="+noteValue);
				}
			}

			if (noteType.equals("byte skip")||noteType.equals("byteskip"))
                fi.longOffset=Long.valueOf(noteValue).longValue();
			if (noteType.equals("endian")) {
				if(noteValuelc.equals("little")) {
					fi.intelByteOrder = true;
				} else {
					fi.intelByteOrder = false;
				}
			}

			if (noteType.equals("encoding")) {
				if(noteValuelc.equals("gz")) noteValuelc="gzip";
				fi.encoding=noteValuelc;
			}

            if (thisLine.startsWith(MIMS_mass_numbers)) {
                int i = noteType.indexOf("=");
                fi.massNames=noteType.substring(i+1).split(" ");
            }
		}


		// Fix axis mins, converting them to pixels
		// if clause is to guard against cases where there is no spatial
		// calibration info leading to Inf
		if(spatialCal.pixelWidth!=0) spatialCal.xOrigin=spatialCal.xOrigin/spatialCal.pixelWidth;
		if(spatialCal.pixelHeight!=0) spatialCal.yOrigin=spatialCal.yOrigin/spatialCal.pixelHeight;
		if(spatialCal.pixelDepth!=0) spatialCal.zOrigin=spatialCal.zOrigin/spatialCal.pixelDepth;

		// Axis min will be the centre of the first pixel if this a "cell" nrrd
		// or at the (top, front, left) if this is a "node" nrrd.
		// ImageJ works on a node basis - that is it treats each voxel as a
		// cube located at its top left corner (or more accurately I think the 
		// corner closer to the coordinate origin); however the image extent
		// displayed is the "bounds" ie spacing*n
		// So to convert a cell based nrrd to a node based ImagePlus, need to
		// shift origin by 1/2 voxel dims in each dimension
		// Since the nrrd specified origin would have been the centre of the
		// voxel we need to SUBTRACT 1/2 voxel dims for ImageJ 
		// See http://teem.sourceforge.net/nrrd/format.html#centers

		if(fi.centers!=null) {
			if(fi.centers[0].equals("cell")) spatialCal.xOrigin-=spatialCal.pixelWidth/2;
			if(fi.centers[1].equals("cell")) spatialCal.yOrigin-=spatialCal.pixelHeight/2;
			if(fi.dimension>2 && fi.centers[2].equals("cell")) spatialCal.zOrigin-=spatialCal.pixelDepth/2;
		}

		if(!detachedHeader) fi.longOffset = input.getFilePointer();
		input.close();
		this.setCalibration(spatialCal);

		return (fi);
	}

	// This gets a space delimited field from a nrrd string
	// of the form
	// a long name: space delimited values
	// but note only works with Java >=1.4 Ithink


	String getFieldPart(String str, int fieldIndex) {
		str=str.trim(); // trim the string
		String[] fieldParts=str.split(":\\s+");
		if(fieldParts.length<2) return(fieldParts[0]);
		//IJ.log("field = "+fieldParts[0]+"; value = "+fieldParts[1]+"; fieldIndex = "+fieldIndex);

		if(fieldIndex==0) return fieldParts[0];
		else return fieldParts[1];
	}
	String getSubField(String str, int fieldIndex) {
		String fieldDescriptor=getFieldPart(str,1);
		fieldDescriptor=fieldDescriptor.trim(); // trim the string

		if (IJ.debugMode) IJ.log("fieldDescriptor = "+fieldDescriptor+"; fieldIndex = "+fieldIndex);

		String[] fields_values=fieldDescriptor.split("\\s+");

		if (fieldIndex>=fields_values.length) {
			return "";
		} else {
			String rval=fields_values[fieldIndex];
			if(rval.startsWith("\"")) rval=rval.substring(1);
			if(rval.endsWith("\"")) rval=rval.substring(0, rval.length()-1);
			return rval;
		}
	}

    @Override
    public File getImageFile() {
        return file;
    }

    @Override
    public int getNMasses() {
        return nMasses;
    }

    @Override
    public int getNImages() {
        return nImages;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public String[] getMassNames() {
        return massNames;
    }

    @Override
    public short[] getPixels(int index) throws IndexOutOfBoundsException, IOException {
        //System.out.println("Nrrd_Reader.getPixels( " + index + " )");
        ImageProcessor sp = imp[index].getProcessor();
        int[] pixels = new int[width*height];
        int i=0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pp = sp.getPixel(x, y);
               pixels[i] = sp.getPixel(x, y);
               i++;
            }
        }
 //the above isn't doing anything
//???????????
   
        return (short[])imp[index].getProcessor().getPixels();
    }

    @Override
    public float getPixelWidth() {
        return (new Float(-1.0)).floatValue();
    }

    @Override
    public float getPixelHeight() {
        return (new Float(-1.0)).floatValue();
    }

    @Override
    public void readPixels(int index) throws IndexOutOfBoundsException, IOException {
        
    }

    @Override
    public void setMassIndex(int index) {
        currentMass = index;
    }

    @Override
    public void setStackIndex(int currentIndex) {
        //keeping symetry
        //the +1 is because of ImageStacks stating with index 1 and not 0
        //the same method in Mims_Reader starts with index 0
        for(int i=0; i< imp.length; i++)
            imp[i].setSlice(currentIndex+1);
    }

}

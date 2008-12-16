package com.nrims;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.*;
import ij.util.Tools;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.plugin.frame.*;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/** 
 * This plugin replaces the Analyze/Tools/ROI Manager command. 
 * Intent to enable interaction with CustomCanvas to show all rois..
 */
public class MimsRoiManager extends PlugInJFrame implements ListSelectionListener, ActionListener, 
                                                            MouseListener {

	static final int BUTTONS = 10;
	JPanel panel;
	static Frame instance;
        JList jlist;
        DefaultListModel listModel;        
	Hashtable rois = new Hashtable();
	Roi roiCopy;
	boolean canceled;
	boolean macro;
	boolean ignoreInterrupts;
	JPopupMenu pm;
	JButton moreButton;
        JCheckBox cbShowAll ;

	public MimsRoiManager() {
		super("MIMS ROI Manager");
		if (instance!=null) {
			instance.toFront();
			return;
		}
		instance = this;
		ImageJ ij = IJ.getInstance();
 		addKeyListener(ij);
 		addMouseListener(this);
		WindowManager.addWindow(this);              
		setLayout(new BorderLayout());            
                
                //JList stuff - for ROIs		               
                listModel = new DefaultListModel();
                listModel.addElement("012345678901234567");                
                jlist = new JList(listModel);  
                jlist.addKeyListener(ij);
                JScrollPane scrollpane = new JScrollPane(jlist);                
                scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                jlist.getSelectionModel().addListSelectionListener(this);//Same as addItemListener                
                scrollpane.setPreferredSize(new Dimension(150, 225));    
                scrollpane.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(5, 5, 5, 5),
                        BorderFactory.createLineBorder(Color.BLACK)));                
                add(scrollpane, BorderLayout.WEST);
                
                //Button Panel
                panel = new JPanel();                
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));             		
                panel.setLayout(new FlowLayout());                
                panel.setPreferredSize(new Dimension(200, 300));                
		addButton("Delete");
		addButton("Rename");
		addButton("Open");
		addButton("Save");
		addButton("Measure");
		addButton("Deselect");
		addButton("More>>");				
		addPopupMenu();
                addCheckbox("Show All", true);
                add(panel, BorderLayout.CENTER);		
		pack();
                listModel.remove(0);                
		GUI.center(this);
	}
	
        void addCheckbox(String label, boolean bEnabled ) {
            JCheckBox cb = new JCheckBox(label);
            cb.setPreferredSize(new Dimension(90, 30));
            cb.setMaximumSize(cb.getPreferredSize());
            cb.setMinimumSize(cb.getPreferredSize());
            if(label.equals("Show All")) cbShowAll = cb ;
            cb.setSelected(bEnabled);
            cb.addActionListener(this);
            panel.add(cb);
        }
        
	void addButton(String label) {
                JButton b = new JButton(label);
                b.setPreferredSize(new Dimension(90, 30));
                b.setMaximumSize(b.getPreferredSize());
                b.setMinimumSize(b.getPreferredSize());
		b.addActionListener(this);
		b.addKeyListener(IJ.getInstance());
 		b.addMouseListener(this);
 		if (label.equals("More>>")) moreButton = b;
                                panel.add(b);
	}

	void addPopupMenu() {
                pm=new JPopupMenu();
		addPopupItem("Combine");
		addPopupItem("Split");
                addPopupItem("Add [t]");
		add(pm);
	}

	void addPopupItem(String s) {
                JMenuItem mi=new JMenuItem(s);
		mi.addActionListener(this);
		pm.add(mi);
	}
	
	public void actionPerformed(ActionEvent e) {
               
		int modifiers = e.getModifiers();
		boolean altKeyDown = (modifiers&ActionEvent.ALT_MASK)!=0 || IJ.altKeyDown();
		boolean shiftKeyDown = (modifiers&ActionEvent.SHIFT_MASK)!=0 || IJ.shiftKeyDown();
		IJ.setKeyUp(KeyEvent.VK_ALT);
		IJ.setKeyUp(KeyEvent.VK_SHIFT);
		String label = e.getActionCommand();
		if (label==null)
			return;
		String command = label;
                if (command.equals("Add [t]"))
			add(shiftKeyDown, altKeyDown);
		else if (command.equals("Delete"))
			delete(false);
		else if (command.equals("Rename"))
			rename(null);
		else if (command.equals("Open"))
			open(null);
		else if (command.equals("Save"))
			save(null);
		else if (command.equals("Measure"))
			measure();
		else if (command.equals("Draw"))
			draw();
		else if (command.equals("Deselect"))
			select(-1);
                else if (command.equals("Show All"))
                        showall();
		else if (command.equals("More>>")) {
			Point ploc = panel.getLocation();
			Point bloc = moreButton.getLocation();
			pm.show(this, ploc.x, bloc.y);
		} 
                else if (command.equals("Combine"))
			combine();
		else if (command.equals("Split"))
			split();
	}
        
        public void valueChanged(ListSelectionEvent e) { 
           
           // DO NOTHING!!  Wait till we are done switching         
           if (!e.getValueIsAdjusting()) return;
           
           int[] indices = jlist.getSelectedIndices();          
           if (indices.length==0) return; 
           
           int index = indices[indices.length-1];
	   if (index<0) index = 0;
           if (WindowManager.getCurrentImage()!=null) {
              restore(index, true);
              if (Recorder.record) Recorder.record("mimsRoiManager", "Select", index);
           }           
        }                                                               
        
        void showall() {           
           if(getImage() != null)
              getImage().updateAndRepaintWindow();              
        }
	
	void add(boolean shiftKeyDown, boolean altKeyDown) {
		if (shiftKeyDown)
			addAndDraw(altKeyDown);
		else if (altKeyDown)
			add(true);
		else
			add(false);
	}

	boolean add(boolean promptForName) {
		ImagePlus imp = getImage();
		if (imp==null)
			return false;
		Roi roi = imp.getRoi();
		if (roi==null) {
			error("The active image does not have a selection.");
			return false;
		}
		String name = roi.getName();
		if (isStandardName(name))
			name = null;
		String label = name!=null?name:getLabel(imp, roi);
		if (promptForName)
			label = promptForName(label);
		else
			label = getUniqueName(label);
		if (label==null) return false;
                listModel.addElement(label);
		roi.setName(label);
		roiCopy = (Roi)roi.clone();
		Calibration cal = imp.getCalibration();
		if (cal.xOrigin!=0.0 || cal.yOrigin!=0.0) {
			Rectangle r = roiCopy.getBounds();
			roiCopy.setLocation(r.x-(int)cal.xOrigin, r.y-(int)cal.yOrigin);
		}
		rois.put(label, roiCopy);
		if (Recorder.record) Recorder.record("mimsRoiManager", "Add");
		return true;
	}
	
	boolean isStandardName(String name) {
		if (name==null) return false;
		boolean isStandard = false;
		int len = name.length();
		if (len>=14 && name.charAt(4)=='-' && name.charAt(9)=='-' )
			isStandard = true;
		else if (len>=9 && name.charAt(4)=='-')
			isStandard = true;
		return isStandard;
	}
	
	String getLabel(ImagePlus imp, Roi roi) {
		Rectangle r = roi.getBounds();
		int xc = r.x + r.width/2;
		int yc = r.y + r.height/2;
		if (xc<0) xc = 0;
		if (yc<0) yc = 0;
		int digits = 4;
		String xs = "" + xc;
		if (xs.length()>digits) digits = xs.length();
		String ys = "" + yc;
		if (ys.length()>digits) digits = ys.length();
		xs = "000" + xc;
		ys = "000" + yc;
		String label = ys.substring(ys.length()-digits) + "-" + xs.substring(xs.length()-digits);
		if (imp.getStackSize()>1) {
			String zs = "000" + imp.getCurrentSlice();
			label = zs.substring(zs.length()-digits) + "-" + label;
		}
		return label;
	}

	void addAndDraw(boolean altKeyDown) {
		if (altKeyDown) {
			if (!add(true)) return;
		} else if (!add(false))
			return;
		ImagePlus imp = WindowManager.getCurrentImage();
		Undo.setup(Undo.COMPOUND_FILTER, imp);
		IJ.run("Draw");
		Undo.setup(Undo.COMPOUND_FILTER_DONE, imp);
		if (Recorder.record) Recorder.record("mimsRoiManager", "Add & Draw");
	}
	
	boolean delete(boolean replacing) {
                int count = listModel.getSize();
		if (count==0)
			return error("The list is empty.");
                int index[] = jlist.getSelectedIndices();
		if (index.length==0 || (replacing&&count>1)) {
			String msg = "Delete all items on the list?";
			if (replacing)
				msg = "Replace items on the list?";
			canceled = false;
			if (!IJ.macroRunning() && !macro) {
				YesNoCancelDialog d = new YesNoCancelDialog(this, "MIMS ROI Manager", msg);
				if (d.cancelPressed())
					{canceled = true; return false;}
				if (!d.yesPressed()) return false;
			}
			index = getAllIndexes();
		}
		for (int i=count-1; i>=0; i--) {
			boolean delete = false;
			for (int j=0; j<index.length; j++) {
				if (index[j]==i)
					delete = true;
			}
			if (delete) {
                                rois.remove(listModel.get(i));
				listModel.remove(i);                                
			}
		}
		if (Recorder.record) Recorder.record("mimsRoiManager", "Delete");
		return true;
	}

	boolean rename(String name2) {
                int index = jlist.getSelectedIndex();
		if (index<0)
			return error("Exactly one item in the list must be selected.");
                String name = listModel.get(index).toString();
		if (name2==null) name2 = promptForName(name);
		if (name2==null) return false;
		Roi roi = (Roi)rois.get(name);
		rois.remove(name);
		roi.setName(name2);
		rois.put(name2, roi);
                listModel.set(index, name2);
		jlist.setSelectedIndex(index);
		if (Recorder.record) Recorder.record("mimsRoiManager", "Rename", name2);
		return true;
	}
	
	String promptForName(String name) {
		GenericDialog gd = new GenericDialog("MIMS ROI Manager");
		gd.addStringField("Rename As:", name, 20);
		gd.showDialog();
		if (gd.wasCanceled())
			return null;
		String name2 = gd.getNextString();
		name2 = getUniqueName(name2);
		return name2;
	}

	boolean restore(int index, boolean setSlice) {
                String label = listModel.get(index).toString();                                
		Roi roi = (Roi)rois.get(label);
		ImagePlus imp = getImage();
		if (imp==null || roi==null)
			return false;
        if (setSlice) {
            int slice = getSliceNumber(label);
            if (slice>=1 && slice<=imp.getStackSize())
                imp.setSlice(slice);
        }
        Roi roi2 = (Roi)roi.clone();
		Calibration cal = imp.getCalibration();
		if (cal.xOrigin!=0.0 || cal.yOrigin!=0.0) {
			Rectangle r = roi2.getBounds();
			roi2.setLocation(r.x+(int)cal.xOrigin, r.y+(int)cal.yOrigin);
		}
		imp.setRoi(roi2);
		return true;
	}
	
	int getSliceNumber(String label) {
		int slice = -1;
		if (label.length()>4 && label.charAt(4)=='-' && label.length()>=14)
			slice = (int)Tools.parseDouble(label.substring(0,4),-1);
		return slice;
	}
	
	void open(String path) {
		Macro.setOptions(null);
		String name = null;
		if (path==null) {
			OpenDialog od = new OpenDialog("Open Selection(s)...", "");
			String directory = od.getDirectory();
			name = od.getFileName();
			if (name==null)
				return;
			path = directory + name;
		}
		if (Recorder.record) Recorder.record("mimsRoiManager", "Open", path);
		if (path.endsWith(".zip")) {
			openZip(path);
			return;
		}
		ij.io.Opener o = new ij.io.Opener();
		if (name==null) name = o.getName(path);
		Roi roi = o.openRoi(path);
		if (roi!=null) {
			if (name.endsWith(".roi"))
				name = name.substring(0, name.length()-4);
			name = getUniqueName(name);
                        listModel.addElement(name);
			rois.put(name, roi);
		}		
	}
	
	// Modified on 2005/11/15 by Ulrik Stervbo to only read .roi files and to not empty the current list
	void openZip(String path) { 
		ZipInputStream in = null; 
		ByteArrayOutputStream out; 
		int nRois = 0; 
		try { 
			in = new ZipInputStream(new FileInputStream(path)); 
			byte[] buf = new byte[1024]; 
			int len; 
			ZipEntry entry = in.getNextEntry(); 
			while (entry!=null) { 
				String name = entry.getName(); 
				if (name.endsWith(".roi")) { 
					out = new ByteArrayOutputStream(); 
					while ((len = in.read(buf)) > 0) 
						out.write(buf, 0, len); 
					out.close(); 
					byte[] bytes = out.toByteArray(); 
					RoiDecoder rd = new RoiDecoder(bytes, name); 
					Roi roi = rd.getRoi(); 
					if (roi!=null) { 
						name = name.substring(0, name.length()-4); 
						name = getUniqueName(name); 
                                                listModel.addElement(name); 
						rois.put(name, roi); 
						nRois++;
					} 
				} 
				entry = in.getNextEntry(); 
			} 
			in.close(); 
		} catch (IOException e) {error(e.toString());} 
		if(nRois==0)
				error("This ZIP archive does not appear to contain \".roi\" files");
	} 


	String getUniqueName(String name) {
			String name2 = name;
			int n = 1;
			Roi roi2 = (Roi)rois.get(name2);
			while (roi2!=null) {
				roi2 = (Roi)rois.get(name2);
				if (roi2!=null) {
					int lastDash = name2.lastIndexOf("-");
					if (lastDash!=-1 && name2.length()-lastDash<5)
						name2 = name2.substring(0, lastDash);
					name2 = name2+"-"+n;
					n++;
				}
				roi2 = (Roi)rois.get(name2);
			}
			return name2;
	}
	
	boolean save(String name) {
                if (listModel.size()==0)
			return error("The selection list is empty.");
                int[] indexes = jlist.getSelectedIndices();
		if (indexes.length==0)
			indexes = getAllIndexes();
		if (indexes.length>1)
			return saveMultiple(indexes, name, true );
                String listname = listModel.get(indexes[0]).toString();
		if(name == null) name =  listname ;
		else name += "_" + listname ;

		Macro.setOptions(null);
		SaveDialog sd = new SaveDialog("Save Selection...", name, ".roi");
		String name2 = sd.getFileName();
		if (name2 == null)
			return false;
		String dir = sd.getDirectory();
		Roi roi = (Roi)rois.get(name);
		rois.remove(listname);
		if (!name2.endsWith(".roi")) name2 = name2+".roi";
		String newName = name2.substring(0, name2.length()-4);
		rois.put(newName, roi);
		roi.setName(newName);
                listModel.set(indexes[0], newName);
		RoiEncoder re = new RoiEncoder(dir+name2);
		try {
			re.write(roi);
		} catch (IOException e) {
			IJ.error("MIMS ROI Manager", e.getMessage());
		}
		return true;
	}

	boolean saveMultiple(int[] indexes, String path, boolean bPrompt) {
		Macro.setOptions(null);
		if (path==null || bPrompt) {
			SaveDialog sd = new SaveDialog("Save ROIs...",
							path == null ? "RoiSet" : path,
							".zip");
			String name = sd.getFileName();
			if (name == null)
				return false;
			if (!(name.endsWith(".zip") || name.endsWith(".ZIP")))
				name = name + ".zip";
			String dir = sd.getDirectory();
			path = dir+name;
		}
		try {
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
			RoiEncoder re = new RoiEncoder(out);
			for (int i=0; i<indexes.length; i++) {
                                String label = listModel.get(indexes[i]).toString();
				Roi roi = (Roi)rois.get(label);
				if (!label.endsWith(".roi")) label += ".roi";
        		zos.putNextEntry(new ZipEntry(label));
				re.write(roi);
				out.flush();
			}
			out.close();
		}
		catch (IOException e) {
			error(""+e);
			return false;
		}
		if (Recorder.record) Recorder.record("mimsRoiManager", "Save", path);
		return true;
	}
		
	boolean measure() {
		ImagePlus imp = getImage();
		if (imp==null)
			return false;
                int[] indexes = jlist.getSelectedIndices();
		if (indexes.length==0)
			indexes = getAllIndexes();
        if (indexes.length==0) return false;

		int nLines = 0;
		for (int i=0; i<indexes.length; i++) {
                        String label = listModel.get(indexes[i]).toString();
			Roi roi = (Roi)rois.get(label);
			if (roi.isLine()) nLines++;
		}
		if (nLines>0 && nLines!=indexes.length) {
			error("All items must be areas or all must be lines.");
			return false;
		}
						
		int nSlices = 1;
                String label = listModel.get(indexes[0]).toString();
		if (getSliceNumber(label)==-1 || indexes.length==1) {
			int setup = IJ.setupDialog(imp, 0);
			if (setup==PlugInFilter.DONE)
				return false;
			nSlices = setup==PlugInFilter.DOES_STACKS?imp.getStackSize():1;
		}
		int currentSlice = imp.getCurrentSlice();
		for (int slice=1; slice<=nSlices; slice++) {
			if (nSlices>1) imp.setSlice(slice);
			for (int i=0; i<indexes.length; i++) {
				if (restore(indexes[i], nSlices==1))
					IJ.run("Measure");
				else
					break;
			}
		}
		imp.setSlice(currentSlice);
		if (indexes.length>1)
			IJ.run("Select None");
		if (Recorder.record) Recorder.record("mimsRoiManager", "Measure");
		return true;
	}	

	boolean draw() {
                int[] indexes = jlist.getSelectedIndices();
		if (indexes.length==0)
			indexes = getAllIndexes();
		ImagePlus imp = WindowManager.getCurrentImage();
		Undo.setup(Undo.COMPOUND_FILTER, imp);
		for (int i=0; i<indexes.length; i++) {
			if (restore(indexes[i], true)) {
				IJ.run("Draw");
				IJ.run("Select None");
			} else
				break;
		}
		Undo.setup(Undo.COMPOUND_FILTER_DONE, imp);
		if (Recorder.record) Recorder.record("mimsRoiManager", "Draw");
		return true;
	}

	void combine() {
		ImagePlus imp = getImage();
		if (imp==null) return;
                int[] indexes = jlist.getSelectedIndices();
		if (indexes.length==1) {
			error("More than one item must be selected, or none");
			return;
		}
		if (indexes.length==0)
			indexes = getAllIndexes();
		ShapeRoi s1=null, s2=null;
		for (int i=0; i<indexes.length; i++) {
                        Roi roi = (Roi)rois.get(listModel.get(indexes[i]).toString());
			if (roi.isLine() || roi.getType()==Roi.POINT)
				continue;
			Calibration cal = imp.getCalibration();
			if (cal.xOrigin!=0.0 || cal.yOrigin!=0.0) {
				roi = (Roi)roi.clone();
				Rectangle r = roi.getBounds();
				roi.setLocation(r.x+(int)cal.xOrigin, r.y+(int)cal.yOrigin);
			}
			if (s1==null) {
				if (roi instanceof ShapeRoi)
					s1 = (ShapeRoi)roi;
				else
					s1 = new ShapeRoi(roi);
				if (s1==null) return;
			} else {
				if (roi instanceof ShapeRoi)
					s2 = (ShapeRoi)roi;
				else
					s2 = new ShapeRoi(roi);
				if (s2==null) continue;
				if (roi.isArea())
					s1.or(s2);
			}
		}
		if (s1!=null)
			imp.setRoi(s1);
		if (Recorder.record) Recorder.record("mimsRoiManager", "Combine");
	}

	void split() {
		ImagePlus imp = getImage();
		if (imp==null) return;
		Roi roi = imp.getRoi();
		if (roi==null || roi.getType()!=Roi.COMPOSITE) {
			error("Image with composite selection required");
			return;
		}
		Roi[] rois = ((ShapeRoi)roi).getRois();
		for (int i=0; i<rois.length; i++) {
			imp.setRoi(rois[i]);
			add(false);
		}
	}

	int[] getAllIndexes() {
                int count = listModel.size();
		int[] indexes = new int[count];
		for (int i=0; i<count; i++)
			indexes[i] = i;
		return indexes;
	}
		
	ImagePlus getImage() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null) {
			error("There are no images open.");
			return null;
		} else
			return imp;
	}

	boolean error(String msg) {
		new MessageDialog(this, "MIMS ROI Manager", msg);
		Macro.abort();
		return false;
	}
	
        @Override
	public void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID()==WindowEvent.WINDOW_CLOSING) {
//			instance = null;	
		}
		ignoreInterrupts = false;
	}
	
	/** Returns a reference to the MIMS ROI Manager
		or null if it is not open. */
	public static MimsRoiManager getInstance() {
		return (MimsRoiManager)instance;
	}

	/** Returns the ROI Hashtable. */
	public Hashtable getROIs() {
		return rois;
	}

	/** Returns the selection list. */
	public JList getList() {
		return jlist;
	}
		
	/** Returns the name of the selection with the specified index.
		Can be called from a macro using
		<pre>call("ij.plugin.frame.RoiManager.getName", index)</pre>
		Returns "null" if the Roi Manager is not open or index is
		out of range.
	*/
	public static String getName(String index) {
		int i = (int)Tools.parseDouble(index, -1);
		MimsRoiManager instance = getInstance();                
                if (instance!=null && i>=0 && i<instance.listModel.size())       	 	
                   return  instance.listModel.get(i).toString();
		else
			return "null";
	}

	/** Executes the MIMS ROI Manager "Add", "Add & Draw", "Update", "Delete", "Measure", "Draw",
		"Deselect", "Select All", "Combine" or "Split" command. Returns false if <code>cmd</code> 
		is not one of these strings. */
	public boolean runCommand(String cmd) {
		cmd = cmd.toLowerCase();
                System.out.println(cmd);
		macro = true;
		boolean ok = true;
		if (cmd.equals("add"))
			add(IJ.shiftKeyDown(), IJ.altKeyDown());
		else
			ok = false;
		macro = false;
		return ok;
	}
	
	public void select(int index) {
                int n = listModel.size();
		if (index<0) {                               
                   jlist.clearSelection();
                   return;
		}
                
                // Dont know why this is being done... but whatever.
                if (jlist.getSelectionMode() != ListSelectionModel.SINGLE_SELECTION)
                   jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                
                
		if (index<n) {
                        jlist.setSelectedIndex(index);
			restore(index, true);	
			if (!Interpreter.isBatchMode())
				IJ.wait(10);
		}
                
                jlist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}
	
	public void select(int index, boolean shiftKeyDown, boolean altKeyDown) {
		if (!(shiftKeyDown||altKeyDown))
			select(index);
		ImagePlus imp = IJ.getImage();
		if (imp==null) return;
		Roi previousRoi = imp.getRoi();
		if (previousRoi==null)
			{select(index); return;}
		Roi.previousRoi = (Roi)previousRoi.clone();
                String label = listModel.get(index).toString();
		Roi roi = (Roi)rois.get(label);
		if (roi!=null) {
			roi.setImage(imp);
			roi.update(shiftKeyDown, altKeyDown);
		}
	}
	
	void selectAll() {
		boolean allSelected = true;
                int count = listModel.size();
		for (int i=0; i<count; i++) {
                        if (!jlist.isSelectedIndex(i));
				allSelected = false;
		}
		if (allSelected)
                     	select(-1);
		else {                   
                   for (int i=0; i<count; i++)
                                  if (!jlist.isSelectedIndex(i)) jlist.setSelectedIndex(i);
		}
	}

    /** Overrides PlugInFrame.close(). */
    @Override
    public void close() {
//    	super.close();
//    	instance = null;
        this.setVisible(false);
    }
    
    public void mousePressed (MouseEvent e) {
        int x=e.getX(), y=e.getY();
        if (e.isPopupTrigger() || e.isMetaDown())
                pm.show(e.getComponent(),x,y);
    }

    public void mouseReleased (MouseEvent e) {}
    public void mouseClicked (MouseEvent e) {}
    public void mouseEntered (MouseEvent e) {}
    public void mouseExited (MouseEvent e) {}

    public void setShowAll(boolean bEnabled) {
        cbShowAll.setSelected(bEnabled);
    }

    public boolean getShowAll() {
        boolean bEnabled = cbShowAll.isSelected();
        return bEnabled;
    }
    
    public void showFrame(){        
        setVisible(true);
        toFront();
        setExtendedState(NORMAL);
    }
}


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
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/** 
 * This plugin replaces the Analyze/Tools/ROI Manager command. 
 * Intent to enable interaction with CustomCanvas to show all rois..
 */
public class MimsRoiManager extends PlugInJFrame implements ListSelectionListener, ActionListener,
        MouseListener {

    JPanel panel;
    static Frame instance;
    JList jlist;
    DefaultListModel listModel;
    Hashtable rois = new Hashtable();
    boolean canceled;
    boolean macro;
    boolean ignoreInterrupts;
    JPopupMenu pm;
    JButton moreButton;
    JCheckBox cbShowAll;
    JSpinner xPosSpinner, yPosSpinner, widthSpinner, heightSpinner;
    JLabel xLabel, yLabel, wLabel, hLabel;
    boolean holdUpdate = false;
    private UI ui = null;
    private com.nrims.data.Opener image = null;
    private String savedpath = "";
    boolean previouslySaved = false;
    Measure scratch;
    HashMap locations = new HashMap<String, ArrayList<Integer[]>>();
    
    public MimsRoiManager(UI ui, com.nrims.data.Opener im) {
        super("MIMS ROI Manager");

        this.ui = ui;
        this.image = im;

        if (instance != null) {
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
               
        //order of these calls determines position...
        setupPosLabels();
        setupPosSpinners();
        setupSizeLabels();
        setupSizeSpinners();
        
        add(panel, BorderLayout.CENTER);
        pack();
        listModel.remove(0);
        GUI.center(this);
    }

    void setupPosSpinners() {
        xPosSpinner = new JSpinner();
        yPosSpinner = new JSpinner();

        xPosSpinner.setPreferredSize(new Dimension(90, 30));
        yPosSpinner.setPreferredSize(new Dimension(90, 30));

        xPosSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));
        yPosSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));

        xPosSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                posSpinnerStateChanged(evt);
            }
        });
        yPosSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                posSpinnerStateChanged(evt);
            }
        });

        panel.add(xPosSpinner);
        panel.add(yPosSpinner);
    }

    void setupSizeSpinners() {
        widthSpinner = new JSpinner();
        heightSpinner = new JSpinner();

        widthSpinner.setPreferredSize(new Dimension(90, 30));
        heightSpinner.setPreferredSize(new Dimension(90, 30));

        widthSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));
        heightSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));

        widthSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                hwSpinnerStateChanged(evt);
            }
        });
        heightSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                hwSpinnerStateChanged(evt);
            }
        });

        panel.add(widthSpinner);
        panel.add(heightSpinner);

    }

    void setupPosLabels() {
        xLabel = new JLabel("X Pos.");
        yLabel = new JLabel("Y Pos.");
        xLabel.setPreferredSize(new Dimension(90, 20));
        yLabel.setPreferredSize(new Dimension(90, 20));

        panel.add(xLabel);
        panel.add(yLabel);
    }

    void setupSizeLabels() {
        wLabel = new JLabel("Width");
        hLabel = new JLabel("Height");
        wLabel.setPreferredSize(new Dimension(90, 20));
        hLabel.setPreferredSize(new Dimension(90, 20));

        panel.add(wLabel);
        panel.add(hLabel);
    }

   void updateRoiLocations(boolean prepend) {

      // Loop over rios.
      for (Object key : locations.keySet()) {

         // Get roi location size.
         ArrayList<Integer[]> xylist = (ArrayList<Integer[]>) locations.get(key);
         
         // Current image size
         MimsPlus mp = ui.getOpenMassImages()[0];
         int size_new = mp.getNSlices();

         // Difference in sizes
         int size_orig = xylist.size();         
         int size_diff = size_new - size_orig;
         
         // If prepending use FIRST element.
         // If appending use LAST element.
         Integer[] xy = new Integer[2];
         if (prepend) {
            xy = xylist.get(0);
         } else {
            xy = xylist.get(xylist.size()-1);
         }

         // Create prepend/append array.
         ArrayList<Integer[]> xylist_new = new ArrayList<Integer[]>();
         for (int i = 0; i < size_diff; i++) {
            xylist_new.add(i, xy);
         }

         // Combine lists.
         if (prepend) {
            xylist_new.addAll(xylist);
            locations.put(key, xylist_new);
         } else {
            xylist.addAll(xylist_new);
            locations.put(key, xylist);
         }
      }
   }

   void updateRoiLocations() {

      // Loop over rios.
      for (Object key : locations.keySet()) {

         // Get roi location size.
         ArrayList<Integer[]> xylist = (ArrayList<Integer[]>) locations.get(key);

         // Current image size
         MimsPlus mp = ui.getOpenMassImages()[0];
         int size_new = mp.getNSlices();

         // Difference in sizes
         int size_orig = xylist.size();
         int size_diff = size_orig - size_new;
         if(size_diff<0) return;

         // Create prepend/append array.
         for (int i = 0; i < size_orig - size_diff; i++) {
            xylist.remove(xylist.size()-1);
         }
      }
   }

   public void resetRoiLocationsLength() {
        int img_size = 0;
        int locations_size = 0;
       for (Object key : locations.keySet()) {
        // Get roi location size.
         ArrayList<Integer[]> xylist = (ArrayList<Integer[]>) locations.get(key);
         locations_size = xylist.size();
         // Current image size
         MimsPlus mp = ui.getOpenMassImages()[0];
         img_size = mp.getNSlices();
         break;
       }

       int diff = locations_size - img_size;
       if(diff < 0) {
           updateRoiLocations(false);
       } else if(diff > 0) {
           updateRoiLocations();
       }
   }

   void updateSpinners() {

      String label = "";
      Roi roi = null;
      ArrayList xylist;
      Integer[] xy = new Integer[2];

      if (jlist.getSelectedIndices().length != 1) {
            return;
      } else {
            label = jlist.getSelectedValue().toString();
      }

      if (!label.matches(""))
         xylist = (ArrayList<Integer[]>)locations.get(label);
      else
         return;

      if (xylist != null)
         xy = (Integer[])xylist.get(ui.getOpenMassImages()[0].getCurrentSlice()-1);
      else
         return;

      if (xy != null) {
         holdUpdate = true;
         xPosSpinner.setValue(xy[0]);
         yPosSpinner.setValue(xy[1]);
         holdUpdate = false;
      } else {
         return;
      }
      
   }

    private void posSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {
        String label = "";

        if (holdUpdate) {
            return;
        }
        if (jlist.getSelectedIndices().length != 1) {
            error("Exactly one item in the list must be selected.");
            return;
        } else {
            label = jlist.getSelectedValue().toString();
        }
        
        // Make sure we have an image
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        
      int plane = imp.getCurrentSlice();
      ArrayList xylist = (ArrayList<Integer[]>)locations.get(label);
      xylist.set(plane-1, new Integer[] {(Integer) xPosSpinner.getValue(), (Integer) yPosSpinner.getValue()});
      locations.put(label, xylist);

      // For display purposes.
      Roi roi = (Roi)rois.get(label);
      Roi temproi = (Roi) roi.clone();
      temproi.setLocation((Integer) xPosSpinner.getValue(), (Integer) yPosSpinner.getValue());
      imp.setRoi(temproi);
            
      updatePlots(false);

    }

    private void hwSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {
        
        String label = "";
       
        if (holdUpdate) return;
        
        if (jlist.getSelectedIndices().length != 1) {
           error("Exactly one item in the list must be selected.");
           return;
        } else {
           label = jlist.getSelectedValue().toString();
        }

        // Make sure we have an image
        ImagePlus imp = getImage();
        if (imp == null) return;
        
        // Make sure we have a ROI
        Roi oldroi = (Roi)rois.get(label);
        if (oldroi == null) return;
        
        // There is no setWidth or  setHeight method for a ROI
        // so essentially we have to create a new one, setRoi
        // will delete the old one from the rois hashtable.
        Roi newroi = null;
        java.awt.Rectangle rect = oldroi.getBoundingRect();        
        
        // Dont do anything if the values are changing 
        // only because user selected a different ROI.        
        if (oldroi.getType() == ij.gui.Roi.RECTANGLE) {                
            newroi = new ij.gui.Roi(rect.x, rect.y, (Integer) widthSpinner.getValue(), (Integer) heightSpinner.getValue(), imp);                                
        } else if (oldroi.getType() == ij.gui.Roi.OVAL) {
            newroi = new ij.gui.OvalRoi(rect.x, rect.y, (Integer) widthSpinner.getValue(), (Integer) heightSpinner.getValue());
        } else {
           return;
        }
        
        //we give it the old name so that setRoi will
        // know which original roi to delete.
        newroi.setName(oldroi.getName());
        move(imp, newroi);
        updatePlots(false);
    }

    void addCheckbox(String label, boolean bEnabled) {
        JCheckBox cb = new JCheckBox(label);
        cb.setPreferredSize(new Dimension(90, 30));
        cb.setMaximumSize(cb.getPreferredSize());
        cb.setMinimumSize(cb.getPreferredSize());
        if (label.equals("Show All")) {
            cbShowAll = cb;
        }
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
        if (label.equals("More>>")) {
            moreButton = b;
        }
        panel.add(b);
    }

    void addPopupMenu() {
        pm = new JPopupMenu();
        addPopupItem("Duplicate");
        addPopupItem("Combine");
        addPopupItem("Split");
        addPopupItem("Add [t]");
        addPopupItem("Save As");
        add(pm);
    }

    void addPopupItem(String s) {
        JMenuItem mi = new JMenuItem(s);
        mi.addActionListener(this);
        pm.add(mi);
    }

    public void actionPerformed(ActionEvent e) {

        int modifiers = e.getModifiers();
        boolean altKeyDown = (modifiers & ActionEvent.ALT_MASK) != 0 || IJ.altKeyDown();
        boolean shiftKeyDown = (modifiers & ActionEvent.SHIFT_MASK) != 0 || IJ.shiftKeyDown();
        IJ.setKeyUp(KeyEvent.VK_ALT);
        IJ.setKeyUp(KeyEvent.VK_SHIFT);
        String label = e.getActionCommand();
        if (label == null) {
            return;
        }
        String command = label;
        if (command.equals("Add [t]")) {
            add();
        } else if (command.equals("Delete")) {
            delete();
        } else if (command.equals("Rename")) {
            rename(null);
        } else if (command.equals("Open")) {
            open(null);
        } else if (command.equals("Save")) {
            //save(null); //opens with defaul imagej path
            String path = ui.getImageDir();
            save(path);
        } else if (command.equals("Measure")) {
            measure();
        } else if (command.equals("Deselect")) {
            select(-1);
        } else if (command.equals("Show All")) {
            showall();
        } else if (command.equals("More>>")) {
            Point ploc = panel.getLocation();
            Point bloc = moreButton.getLocation();
            pm.show(this, ploc.x, bloc.y);
        } else if (command.equals("Duplicate")) {
            duplicate();
        } else if (command.equals("Combine")) {
            combine();
        } else if (command.equals("Split")) {
            split();
        } else if (command.equals("Save As")) {
            String path = ui.getImageDir();
            previouslySaved = false;
            save(path);
        }
    }

    public void valueChanged(ListSelectionEvent e) {       
        
        // DO NOTHING!!  Wait till we are done switching         
        if (!e.getValueIsAdjusting()) return;
               
        boolean setSlice = false;
        holdUpdate = true;

        int[] indices = jlist.getSelectedIndices();
        if (indices.length == 0) return;

        // Select ROI in the window
        int index = indices[indices.length - 1];
        if (index < 0) index = 0;        
        if (ui.getSyncROIsAcrossPlanes()) setSlice = false;
        else setSlice = true;
        
        restore(index, setSlice);

        // Do spinner stuff
        if (indices.length == 1) {
            ImagePlus imp = getImage();
            if (imp == null) return;
            Roi roi = imp.getRoi();
            resetSpinners(roi);
            updatePlots(true);
        } else {
            disablespinners();
        }      
        
        holdUpdate = false;
    }

    public void resetSpinners(Roi roi) {
       
       if (roi == null) return;
       holdUpdate = true;
       
       // get the type of ROI we are dealing with
       int roiType = roi.getType();
       
       // Not sure if all ROIs have a width-height value that can be adjusted... test
       if (roiType == Roi.RECTANGLE || roiType == Roi.OVAL) {
          enablespinners();
          java.awt.Rectangle rect = roi.getBoundingRect();
          xPosSpinner.setValue(rect.x);
          yPosSpinner.setValue(rect.y);
          wLabel.setText("Width");
          hLabel.setText("Height");
          widthSpinner.setValue(rect.width);
          heightSpinner.setValue(rect.height);           
       } else if (roiType == Roi.POLYGON || roiType == Roi.FREEROI) {
          enablePosSpinners();
          disableSizeSpinners();
          java.awt.Rectangle rect = roi.getBoundingRect();   
          xPosSpinner.setValue(rect.x); 
          yPosSpinner.setValue(rect.y); 
       } else if (roiType == Roi.LINE || roiType == Roi.POLYLINE || roiType == Roi.FREELINE) {
           enablePosSpinners();
           disableSizeSpinners();
           
           //widthSpinner.setEnabled(true);
           //heightSpinner.setEnabled(false);
           
           java.awt.Rectangle rect = roi.getBoundingRect();
           xPosSpinner.setValue(rect.x);
           yPosSpinner.setValue(rect.y);
           
           //ij.gui.Line lineroi = (Line) roi;
           //widthSpinner.setValue(lineroi.getWidth());
       }
       else {
          disablespinners();
       }       
       holdUpdate = false;
    }
    
    void enablespinners() {       
       xPosSpinner.setEnabled(true);
       yPosSpinner.setEnabled(true);
       widthSpinner.setEnabled(true);
       heightSpinner.setEnabled(true);
    }
    
    void disablespinners() {
       xPosSpinner.setEnabled(false);
       yPosSpinner.setEnabled(false);
       widthSpinner.setEnabled(false);
       heightSpinner.setEnabled(false);
    }
    
    void enablePosSpinners() {
        xPosSpinner.setEnabled(true);
        yPosSpinner.setEnabled(true);
    }

    void disablePosSpinners() {
        xPosSpinner.setEnabled(false);
        yPosSpinner.setEnabled(false);
    }

    void enableSizeSpinners() {
        widthSpinner.setEnabled(true);
        heightSpinner.setEnabled(true);
    }

    void disableSizeSpinners() {
        widthSpinner.setEnabled(false);
        heightSpinner.setEnabled(false);
    }
    
    void showall() {
        if (getImage() != null) {
            ui.updateAllImages();
        }
    }
    
   void setRoi(ImagePlus imp, Roi roi) {               
      
      // ROI old name - based on its old bounding rect
      String label = roi.getName();

      // ROI new name - based on its new bounding rect
      //String newName = getLabel(imp, roi);
      //newName = getUniqueName(newName);
      //if (newName != null) roi.setName(newName);
      //else return;
      
      // update name in the jlist
      //int i = getIndex(oldName);
      //if (i < 0) return;
      //listModel.set(i, newName);

      // update rois hashtable with new ROI            
      //rois.remove(oldName);
      //rois.put(newName, roi);

      Rectangle rec = roi.getBounds();      

      int plane = imp.getCurrentSlice();
      ArrayList xylist = (ArrayList<Integer[]>)locations.get(label);
      xylist.set(plane-1, new Integer[] {rec.x, rec.y});
      locations.put(label, xylist);

      imp.setRoi(roi);
      
      imp.updateAndRepaintWindow();
   }
   
   boolean move(ImagePlus imp, Roi roi) {
      if (imp == null) return false;               
      if (roi == null) return false;
      
       setRoi(imp, roi);
       
       // Debug
       if (Recorder.record) {
         Recorder.record("mimsRoiManager", "Move");
      }        
      return true;      
   }

    boolean move(int flag) {
        // Get the image and the roi
        ImagePlus imp = getImage();
        Roi roi = imp.getRoi();
        boolean b = true;

        if( flag == MimsPlusEvent.ATTR_ROI_MOVED) {
            b = move(imp, roi);
        }

        if( flag == MimsPlusEvent.ATTR_ROI_MOVED_ALL) {
            String label = roi.getName();
            Rectangle rec = roi.getBounds();
            ArrayList xylist = (ArrayList<Integer[]>)locations.get(label);

            for(int p = 1; p <= imp.getStackSize(); p ++) {
                xylist.set(p-1, new Integer[] {rec.x, rec.y});
            }
            locations.put(label, xylist);
        }

        return b;
    }    
    
    boolean add() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }
        Roi roi = imp.getRoi();
        if (roi == null) {
            error("The active image does not have a selection.");
            return false;
        }
        String name = roi.getName();
        if (isStandardName(name)) {
            name = null;
        }
        String label = name != null ? name : getLabel(imp, roi);
        label = getUniqueName(label);
        if (label == null) {
            return false;
        }
        listModel.addElement(label);
        roi.setName(label);
        Calibration cal = imp.getCalibration();
        Rectangle r = roi.getBounds();
        if (cal.xOrigin != 0.0 || cal.yOrigin != 0.0) {            
            roi.setLocation(r.x - (int) cal.xOrigin, r.y - (int) cal.yOrigin);
        }
        
        // Create positions arraylist.
        int stacksize = ui.getOpenMassImages()[1].getStackSize();
        ArrayList xypositions = new ArrayList<Integer[]>();
        Integer[] xy = new Integer[2];
        for (int i = 0; i < stacksize; i++) {
           xy = new Integer[] {r.x, r.y};
           xypositions.add(i, xy);
        }
        locations.put(label, xypositions);

        // Add roi to list.
        rois.put(label, roi);

        return true;
    }

    boolean isStandardName(String name) {
        if (name == null) {
            return false;
        }
        boolean isStandard = false;
        int len = name.length();
        if (len >= 14 && name.charAt(4) == '-' && name.charAt(9) == '-') {
            isStandard = true;
        } else if (len >= 9 && name.charAt(4) == '-') {
            isStandard = true;
        }
        return isStandard;
    }

    String getLabel(ImagePlus imp, Roi roi) {
        Rectangle r = roi.getBounds();
        int xc = r.x + r.width / 2;
        int yc = r.y + r.height / 2;
        if (xc < 0) {
            xc = 0;
        }
        if (yc < 0) {
            yc = 0;
        }
        int digits = 4;
        String xs = "" + xc;
        if (xs.length() > digits) {
            digits = xs.length();
        }
        String ys = "" + yc;
        if (ys.length() > digits) {
            digits = ys.length();
        }
        xs = "000" + xc;
        ys = "000" + yc;
        String label = ys.substring(ys.length() - digits) + "-" + xs.substring(xs.length() - digits);
/*  Cludgy...  TODO: why is this passed an imageplus and not mimsplus?
        if (imp.getStackSize() > 1) {
            String zs = "000" + imp.getCurrentSlice();
            label = zs.substring(zs.length() - digits) + "-" + label;
        }
*/
        MimsPlus mimsp = ui.getMassImages()[0];
        if(mimsp == null) { return label; }
        
        if (mimsp.getStackSize() > 1) {
            String zs = "000" + mimsp.getCurrentSlice();
            label = zs.substring(zs.length() - digits) + "-" + label;
        }
     
        return label;
    }
    
    void deleteAll(){
       if (listModel.getSize() > 0) {
          jlist.setSelectedIndices(getAllIndexes());
          
          delete();
       }
    }

    public HashMap getRoiLocations() {
       return locations;
    }

    public Integer[] getRoiLocation(String label, int plane) {
       int index = ui.getmimsAction().trueIndex(plane);
       ArrayList<Integer[]> xylist = (ArrayList<Integer[]>)locations.get(label);
       if (xylist == null)
          return null;
       else {
          return xylist.get(index-1);
       }          
    }

    boolean delete() {
        int count = listModel.getSize();
        if (count == 0) {
            return error("The list is empty.");
        }
        int index[] = jlist.getSelectedIndices();
        if (index.length == 0) {
            String msg = "Delete all items on the list?";
            canceled = false;
            if (!IJ.macroRunning() && !macro) {
                YesNoCancelDialog d = new YesNoCancelDialog(this, "MIMS ROI Manager", msg);
                if (d.cancelPressed()) {
                    canceled = true;
                    return false;
                }
                if (!d.yesPressed()) {
                    return false;
                }
            }
            index = getAllIndexes();
            //if clearing the whole list assume
            //you're working with a "new" file
            this.previouslySaved = false;
            this.savedpath = "";
            this.resetTitle();
            
        }
        for (int i = count - 1; i >= 0; i--) {
            boolean delete = false;
            for (int j = 0; j < index.length; j++) {
                if (index[j] == i) {
                    delete = true;
                }
            }
            if (delete) {
                locations.remove(listModel.get(i));
                rois.remove(listModel.get(i));
                listModel.remove(i);
            }
        }
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Delete");
        }
        
        ui.updateAllImages();
                     
        return true;
    }

    boolean rename(String name2) {
        int index = jlist.getSelectedIndex();
        if (index < 0) {
            return error("Exactly one item in the list must be selected.");
        }
        String name = listModel.get(index).toString();
        if (name2 == null) {
            name2 = promptForName(name);
        }
        if (name2 == null) {
            return false;
        }
        Roi roi = (Roi) rois.get(name);
        rois.remove(name);
        roi.setName(name2);
        rois.put(name2, roi);
        locations.put(name2, locations.get(name));
        locations.remove(name);
        listModel.set(index, name2);
        jlist.setSelectedIndex(index);
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Rename", name2);
        }
        return true;
    }

    String promptForName(String name) {
        GenericDialog gd = new GenericDialog("MIMS ROI Manager");
        gd.addStringField("Rename As:", name, 20);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return null;
        }
        String name2 = gd.getNextString();
        name2 = getUniqueName(name2);
        return name2;
    }

    boolean restore(int index, boolean setSlice) {
        String label = listModel.get(index).toString();
        Roi roi = (Roi) rois.get(label);
        MimsPlus imp;
        try{
            imp = (MimsPlus)getImage();
        } catch(ClassCastException e) {
            imp = ui.getOpenMassImages()[0];
        }
        if (imp == null || roi == null) {
            return false;
        }
        
        if (setSlice) {
            int slice = getSliceNumber(label);
            if (slice >= 1 && slice <= imp.getStackSize()) {
                imp.setSlice(slice);
            }
        }       
        
        // Set the selected roi to yellow
        roi.setInstanceColor(java.awt.Color.yellow);
        imp.setRoi(roi);              
        
        return true;
    }

    int getSliceNumber(String label) {
        int slice = -1;
        if (label.length() > 4 && label.charAt(4) == '-' && label.length() >= 14) {
            slice = (int) Tools.parseDouble(label.substring(0, 4), -1);
        }
        return slice;
    }

    void open(String path) {
        ImagePlus imp = getImage();
        if (imp == null) return;
        Macro.setOptions(null);
        String name = null;
        if (path == null) {
            OpenDialog od = new OpenDialog("Open Selection(s)...", ui.getImageDir(), "");
            String directory = od.getDirectory();
            name = od.getFileName();
            if (name == null) {
                return;
            }
            path = directory + name;
        }
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Open", path);
        }
        if (path.endsWith(".zip")) {
            openZip(path);
            return;
        }
        ij.io.Opener o = new ij.io.Opener();
        if (name == null) {
            name = o.getName(path);
        }
        Roi roi = o.openRoi(path);
        if (roi != null) {
            if (name.endsWith(".roi")) {
                name = name.substring(0, name.length() - 4);
            }
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
            while (entry != null) {
                String name = entry.getName();
                if (name.endsWith(".roi")) {
                    out = new ByteArrayOutputStream();
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                    byte[] bytes = out.toByteArray();
                    RoiDecoder rd = new RoiDecoder(bytes, name);
                    Roi roi = rd.getRoi();
                    if (roi != null) {
                        name = name.substring(0, name.length() - 4);
                        name = getUniqueName(name);
                        listModel.addElement(name);
                        rois.put(name, roi);
                        getImage().setRoi(roi);
                        nRois++;
                    }
                }
                if (name.endsWith(".pos")) {
                    ObjectInputStream ois = new ObjectInputStream(in);
                    HashMap temp_loc = new HashMap<String, ArrayList<Integer[]>>();
                    try {
                        temp_loc = (HashMap<String, ArrayList<Integer[]>>)ois.readObject();
                        this.locations = temp_loc;
                    } catch(ClassNotFoundException e) {
                        error(e.toString());
                        System.out.println(e.toString());
                    }
                }

                entry = in.getNextEntry();
            }
            in.close();
            savedpath = path;
            previouslySaved = true;
            resetTitle();
        } catch (IOException e) {
            error(e.toString());
            System.out.println(e.toString());
        }
        if (nRois == 0) {
            error("This ZIP archive does not appear to contain \".roi\" files");
        }
    }

    String getUniqueName(String name) {
        String name2 = name;
        int n = 1;
        Roi roi2 = (Roi) rois.get(name2);
        while (roi2 != null) {
            roi2 = (Roi) rois.get(name2);
            if (roi2 != null) {
                int lastDash = name2.lastIndexOf("-");
                if (lastDash != -1 && name2.length() - lastDash < 5) {
                    name2 = name2.substring(0, lastDash);
                }
                name2 = name2 + "-" + n;
                n++;
            }
            roi2 = (Roi) rois.get(name2);
        }
        return name2;
    }

    boolean save(String name) {
        if (listModel.size() == 0) {
            return error("The selection list is empty.");
        }
        int[] indexes = jlist.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        
        return saveMultiple(indexes, name, true);            

        /* Allways save a .zip since we're saving positions as well.
        //only called if one roi selected...
        String path = name;
        name = null;
        String listname = listModel.get(indexes[0]).toString();
        if (name == null) {
            name = listname;
        } else {
            name += "_" + listname;
        }
        Macro.setOptions(null);
        SaveDialog sd = new SaveDialog("Save Selection...", path, name, ".roi");
        String name2 = sd.getFileName();
        if (name2 == null) {
            return false;
        }
        String dir = sd.getDirectory();
        Roi roi = (Roi) rois.get(name);
        rois.remove(listname);
        if (!name2.endsWith(".roi")) {
            name2 = name2 + ".roi";
        }
        String newName = name2.substring(0, name2.length() - 4);
        rois.put(newName, roi);
        roi.setName(newName);
        listModel.set(indexes[0], newName);
        RoiEncoder re = new RoiEncoder(dir + name2);
        try {
            re.write(roi);
        } catch (IOException e) {
            IJ.error("MIMS ROI Manager", e.getMessage());
            System.out.println(e.toString());
        }
        return true;
        */
    }

    boolean saveMultiple(int[] indexes, String path, boolean bPrompt) {
        Macro.setOptions(null);
        if (bPrompt) {
            String defaultname = ui.getImageFilePrefix();
            defaultname += "_rois.zip";
            SaveDialog sd = new SaveDialog("Save ROIs...", path,
                    defaultname,
                    ".zip");
            String name = sd.getFileName();
            if (name == null) {
                return false;
            }
            if (!(name.endsWith(".zip") || name.endsWith(".ZIP"))) {
                name = name + ".zip";
            }
            String dir = sd.getDirectory();
            path = (new File(dir, name)).getAbsolutePath();
        }
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            RoiEncoder re = new RoiEncoder(out);
            for (int i = 0; i < indexes.length; i++) {
                String label = listModel.get(indexes[i]).toString();
                Roi roi = (Roi) rois.get(label);
                if (!label.endsWith(".roi")) {
                    label += ".roi";
                }
                zos.putNextEntry(new ZipEntry(label));
                re.write(roi);
                out.flush();
            }

            //save locations hash
            String label = "locations.pos";
            zos.putNextEntry(new ZipEntry(label));
            ObjectOutputStream obj_out = new ObjectOutputStream(zos);
            obj_out.writeObject(locations);
            obj_out.flush();

            out.close();
            savedpath = path;
            previouslySaved = true;
            resetTitle();
        } catch (IOException e) {
            error("" + e);
            System.out.println(e.toString());
            return false;
        }
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Save", path);
        }
        return true;
    }
    private void resetTitle() {
        String name = savedpath.substring(savedpath.lastIndexOf("/")+1, savedpath.length());
        this.setTitle("MIMS ROI Manager: "+name);
    }
    
    void measure() {
               
       Measure measure = ui.getRoiControl().getMeasure(); 
       if (scratch == null) scratch = new Measure(ui);       
       scratch.setOptionsTheSame(measure);
               
        // If not appending reset the data in the table
        if (ui.getRoiControl().append()) {
           scratch.setResize(false);            
        } else {
           scratch.reset();
           scratch.setResize(true); 
        }
       
        MimsPlus[] imp = new MimsPlus[1];
        imp[0] = (MimsPlus)getImage();
        if (imp[0].getMimsType() == MimsPlus.MASS_IMAGE || imp[0].getMimsType() == MimsPlus.RATIO_IMAGE)
           scratch.generateRoiTable(imp, true);       
    }
    
    boolean measure_old() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }
        int[] indexes = jlist.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        if (indexes.length == 0) {
            return false;
        }
        int nLines = 0;
        for (int i = 0; i < indexes.length; i++) {
            String label = listModel.get(indexes[i]).toString();
            Roi roi = (Roi) rois.get(label);
            if (roi.isLine()) {
                nLines++;
            }
        }
        if (nLines > 0 && nLines != indexes.length) {
            error("All items must be areas or all must be lines.");
            return false;
        }

        int nSlices = 1;
        String label = listModel.get(indexes[0]).toString();
        if (getSliceNumber(label) == -1 || indexes.length == 1) {
            int setup = IJ.setupDialog(imp, 0);
            if (setup == PlugInFilter.DONE) {
                return false;
            }
            nSlices = setup == PlugInFilter.DOES_STACKS ? imp.getStackSize() : 1;
        }
        int currentSlice = imp.getCurrentSlice();
        for (int slice = 1; slice <= nSlices; slice++) {
            if (nSlices > 1) {
                imp.setSlice(slice);
            }
            for (int i = 0; i < indexes.length; i++) {
                if (restore(indexes[i], nSlices == 1)) {
                    IJ.run("Measure");
                } else {
                    break;
                }
            }
        }
        imp.setSlice(currentSlice);
        if (indexes.length > 1) {
            IJ.run("Select None");
        }
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Measure");
        }
        return true;
    }

    void duplicate() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }

        Roi roi = imp.getRoi();

        if (roi == null) {
            error("The active image does not have a selection.");
            return;
        }

        Roi roi2 = (Roi)roi.clone();

        String name = roi2.getName();
        if (isStandardName(name)) {
            name = null;
        }
        String label = name != null ? name : getLabel(imp, roi);
        label = getUniqueName(label);
        if (label == null) {
            return;
        }
        listModel.addElement(label);
        roi2.setName(label);
        Calibration cal = imp.getCalibration();
        if (cal.xOrigin != 0.0 || cal.yOrigin != 0.0) {
            Rectangle r = roi2.getBounds();
            roi2.setLocation(r.x - (int) cal.xOrigin, r.y - (int) cal.yOrigin);
        }
        rois.put(label, roi2);
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Add");
        }
        return;

    }

    void combine() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        int[] indexes = jlist.getSelectedIndices();
        if (indexes.length == 1) {
            error("More than one item must be selected, or none");
            return;
        }
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        ShapeRoi s1 = null, s2 = null;
        for (int i = 0; i < indexes.length; i++) {
            Roi roi = (Roi) rois.get(listModel.get(indexes[i]).toString());
            if (roi.isLine() || roi.getType() == Roi.POINT) {
                continue;
            }
            Calibration cal = imp.getCalibration();
            if (cal.xOrigin != 0.0 || cal.yOrigin != 0.0) {
                roi = (Roi) roi.clone();
                Rectangle r = roi.getBounds();
                roi.setLocation(r.x + (int) cal.xOrigin, r.y + (int) cal.yOrigin);
            }
            if (s1 == null) {
                if (roi instanceof ShapeRoi) {
                    s1 = (ShapeRoi) roi;
                } else {
                    s1 = new ShapeRoi(roi);
                }
                if (s1 == null) {
                    return;
                }
            } else {
                if (roi instanceof ShapeRoi) {
                    s2 = (ShapeRoi) roi;
                } else {
                    s2 = new ShapeRoi(roi);
                }
                if (s2 == null) {
                    continue;
                }
                if (roi.isArea()) {
                    s1.or(s2);
                }
            }
        }
        if (s1 != null) {
            imp.setRoi(s1);
        }
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Combine");
        }
    }

    void split() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        Roi roi = imp.getRoi();
        if (roi == null || roi.getType() != Roi.COMPOSITE) {
            error("Image with composite selection required");
            return;
        }
        Roi[] rois = ((ShapeRoi) roi).getRois();
        for (int i = 0; i < rois.length; i++) {
            imp.setRoi(rois[i]);
            add();
        }
    }

    int[] getAllIndexes() {
        int count = listModel.size();
        int[] indexes = new int[count];
        for (int i = 0; i < count; i++) {
            indexes[i] = i;
        }
        return indexes;
    }

    ImagePlus getImage() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            error("There are no images open.");
            return null;
        } else {
            return imp;
        }
    }

    public Roi getRoi() {
        int[] indexes = jlist.getSelectedIndices();

        if (indexes.length == 0) {
            return null;
        }

        String label = listModel.get(indexes[0]).toString();
        Roi roi = (Roi) rois.get(label);
        return roi;
    }

    void updatePlots(boolean force) {
        MimsPlus imp;
        try{
            imp = (MimsPlus)this.getImage();
        } catch(ClassCastException e) {
            return;
        }
        if(imp == null) return;
        
        Roi roi = imp.getRoi();
        if(roi == null) return;
        double[] roipix = imp.getRoiPixels();
        
        if ((roi.getType() == roi.LINE) || (roi.getType() == roi.POLYLINE) || (roi.getType() == roi.FREELINE)) {
            ij.gui.ProfilePlot profileP = new ij.gui.ProfilePlot(imp);
            ui.updateLineProfile(profileP.getProfile(), imp.getShortTitle() + " : " + roi.getName(), imp.getProcessor().getLineWidth());
        } else {
            String label = imp.getShortTitle() + " ROI: " + roi.getName();
            ui.getRoiControl().updateHistogram(roipix, label, force);
        }
    }
    
    boolean error(String msg) {
        new MessageDialog(this, "MIMS ROI Manager", msg);
        Macro.abort();
        return false;
    }

    @Override
    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
//			instance = null;	
        }
        ignoreInterrupts = false;
    }

    /** Returns a reference to the MIMS ROI Manager
    or null if it is not open. */
    public static MimsRoiManager getInstance() {
        return (MimsRoiManager) instance;
    }

    /** Returns the ROI Hashtable. */
    public Hashtable getROIs() {
        return rois;
    }

    /** Return roi from hash by name */
    public Roi getRoiByName(String name) {
        return (Roi)rois.get(name);
    }

    /** Gets selected ROIs. */
    public Roi[] getSelectedROIs() {

       // initialize variables.
       Roi roi;
       Roi[] rois;

       // get selected indexes.
       int[] roiIndexes = jlist.getSelectedIndices();
       if (roiIndexes.length == 0) {
          rois = new Roi[0];
       } else {
          rois = new ij.gui.Roi[roiIndexes.length];
          for (int i = 0; i < roiIndexes.length; i++) {
             roi = (ij.gui.Roi) getROIs().get(jlist.getModel().getElementAt(roiIndexes[i]));
             //rois[i] = (Roi) roi.clone();
             //rois[i].setName("r" + Integer.toString(roiIndexes[i] + 1));
             rois[i] = roi;
          }
       }

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
        int i = (int) Tools.parseDouble(index, -1);
        MimsRoiManager instance = getInstance();
        if (instance != null && i >= 0 && i < instance.listModel.size()) {
            return instance.listModel.get(i).toString();
        } else {
            return "null";
        }
    }
    
    /* 
    Call this method to find what index the specified 
    ROI label has in the jlist.  
    */
   public int getIndex(String label) {      
      int count = listModel.getSize();
      for (int i = 0; i <= count - 1; i++) {
         String value = listModel.get(i).toString();
         if (value.equals(label)) return i;
      }
      return -1;
   }

    public void select(int index) {
        int n = listModel.size();
        if (index < 0) {
           jlist.clearSelection();
           return;
        } else if (index > -1 && index < n) {
           jlist.setSelectedIndex(index);    
        }
        
        
        String label = jlist.getSelectedValue().toString();
                
        // Make sure we have a ROI
        Roi roi = (Roi)rois.get(label);
        if (roi == null) return;
        else resetSpinners(roi);
        
        // Dont know why this is being done... commenting out for now. 
        /*
        if (jlist.getSelectionMode() != ListSelectionModel.SINGLE_SELECTION) {
            jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        if (index < n) {
            jlist.setSelectedIndex(index);
            restore(index, true);
            if (!Interpreter.isBatchMode()) {
                IJ.wait(10);
            }
        }
        jlist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);                        
        */
    }
    
    // Programatically selects all items in the list 
    void selectAll() {
        int len = jlist.getModel().getSize();
        if (len <= 0) {
            return;
        } else {
            jlist.setSelectionInterval(0, len - 1);
        }
    }

    /** Overrides PlugInFrame.close(). */
    @Override
    public void close() {
//    	super.close();
//    	instance = null;
        this.setVisible(false);
    }

    public void mousePressed(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        if (e.isPopupTrigger() || e.isMetaDown()) {
            pm.show(e.getComponent(), x, y);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void setShowAll(boolean bEnabled) {
        cbShowAll.setSelected(bEnabled);
    }

    public boolean getShowAll() {
        boolean bEnabled = cbShowAll.isSelected();
        return bEnabled;
    }

    public void showFrame() {
        setVisible(true);
        toFront();
        setExtendedState(NORMAL);
    }
}


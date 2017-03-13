package com.nrims;

import com.nrims.data.MIMSFileFilter;
import com.nrims.logging.OMLogger;
//import com.sun.xml.internal.bind.v2.runtime.unmarshaller.TagName;  // commented by DJ: 12/08/2014
import ij.IJ;
import ij.gui.Roi;
import ij.process.ImageStatistics;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;


/**
 * MimsJTable class creates a frame containing a <code>JTable</code>. This class is used to generate frame that contain
 * data, usually statistical data associated with images.
 *
 * @author zkaufman
 */
public class MimsJTable {

    UI gui;
    JTable table;
    String[] stats;
    MimsPlus images[];
    //int originalImagesTypes[];   // DJ
    Roi[] rois;
    //ArrayList planes;
    List planes;

    JFrame frame;
    JPopupMenu popupMenu;
    int selectedColumn;

    static String DEFAULT_TABLE_NAME = "_data.csv";
    static String DELETE_COLUMN_CMD = "Delete Column";
    static String AREA = "area";
    static String GROUP = "group";
    static String FILENAME = "file";
    static String ROIGROUP = "Roi group";
    static String ROITAGS = "Roi tags";                   //DJ: 12/08/2014
    static String ROINAME = "Roi name";
    static String SLICE = "Slice";
    static String TRUE = "True Index";
    static String[] SUM_IMAGE_MANDOTORY_COLUMNS = {FILENAME, ROIGROUP, ROITAGS, ROINAME};      //DJ: 12/08/2014
    static String[] ROIMANAGER_MANDATORY_COLUMNS = {ROINAME, ROIGROUP, ROITAGS, SLICE};                         // DJ: 12/08/2014
    private final static Logger OMLOGGER = OMLogger.getOMLogger(MimsJTable.class.getName());

    public static final int mOptions = ImageStatistics.AREA + ImageStatistics.MEAN + ImageStatistics.STD_DEV
            + ImageStatistics.MODE + ImageStatistics.MIN_MAX + ImageStatistics.CENTROID
            + ImageStatistics.CENTER_OF_MASS + ImageStatistics.PERIMETER + ImageStatistics.LIMIT
            + ImageStatistics.RECT + ImageStatistics.LABELS + ImageStatistics.ELLIPSE
            + ImageStatistics.INVERT_Y + ImageStatistics.CIRCULARITY + ImageStatistics.SHAPE_DESCRIPTORS
            + ImageStatistics.INTEGRATED_DENSITY + ImageStatistics.MEDIAN
            + ImageStatistics.SKEWNESS + ImageStatistics.KURTOSIS + ImageStatistics.SLICE
            + ImageStatistics.STACK_POSITION + ImageStatistics.SCIENTIFIC_NOTATION;

    public MimsJTable(UI ui) {
        this.gui = ui;
    }

    /**
     * Used by the RoiManager "measure" button to generate a table. When used in this way, only statistics for the
     * currently selected image will be shown.
     *
     * @param appendData <code>true</code> if appending a plot to an existing frame, otherwise <code>false</code>.
     */
    public void createRoiTable(boolean appendData) {

        // Get the data and column headers.
        Object[][] data = getRoiDataSet();
        String[] columnNames = getRoiManagerColumnNames();

        // Generate and display table.
        if (appendData && ableToAppendData(columnNames)) {
            appendDataToTable(data, columnNames);
        } else {
            displayTable(data, columnNames);
        }
    }

    /**
     * Generates a table for Sum images. Because sum images only have one plane, a sum table will differ from the
     * default table in that each row corresponds to an ROI (rather than to a plane).
     *
     * @param appendData <code>true</code> if appending a plot to an existing frame, otherwise <code>false</code>.
     */
    public void createSumTable(boolean appendData) {

        // Get the data and column headers.
        Object[][] data = getSumImageDataSet();
        String[] columnNames = getSumImageColumnNames();

        // Generate and display table.
        if (appendData && ableToAppendData(columnNames)) {
            appendDataToTable(data, columnNames);
        } else {
            displayTable(data, columnNames);
        }
    }
    
    /**
     * Generates a table for images. Each row corresponds to a plane and each column to a statisitical field (or meta
     * data String).
     *
     * @param appendData <code>true</code> if appending a plot to an existing frame, otherwise <code>false</code>.
     */
    public void createTable(boolean appendData) {

        // Get data.
        Object[][] data = getDataSet();
        String[] columnNames = getColumnNames();

        // Generate and display table.
        if (appendData && ableToAppendData(columnNames)) {
            appendDataToTable(data, columnNames);
        } else {
            displayTable(data, columnNames);
        }
    }

    /**
     * Create a table with custom data and column names.
     *
     * @param data data to be displayed in table
     * @param columnNames column header names
     */
    public void createCustomTable(Object[][] data, String[] columnNames) {
        displayTable(data, columnNames);
    }

    /**
     * Generates a table for listing Roi info (name and group) and their pixel values. Correct order is required and
     * ArrayLists must have the same length. Output table will look something like the following:
     *
     * Group  | Name  | Pixel Value
     * ------------------------------
     * group1 | name1 | pixel value 1
     * group1 | name1 | pixel value 2
     * group1 | name2 | pixel value 3
     * group2 | name1 | pixel value 1
     * group2 | name2 | pixel value 2
     * group3 | name1 | pixel value 1
     * group3 | name2 | pixel value 2
     * group3 | name3 | pixel value 3
     * group3 | name3 | pixel value 4
     *
     * @param groups ArrayList of roi names. Repeats expected.
     * @param groups ArrayList of groups. Repeats expected.
     * @param groups ArrayList of pixel values.
     */
   // void createPixelTable(String file, ArrayList<String> names, ArrayList<String> groups, ArrayList<Double> values) {
    void createPixelTable(String file, List<String> names, List<String> groups, List<Double> values) {

        // Input checks.
        if (names == null || groups == null || values == null) {
            return;
        }
        if (names.isEmpty() || groups.isEmpty() || values.isEmpty()) {
            return;
        }
        if ((groups.size() != values.size()) || (names.size() != values.size())) {
            return;
        }

        // Get data.
        Object[][] data = new Object[values.size()][4];
        String group, name = "";
        for (int i = 0; i < values.size(); i++) {
            name = (String) names.get(i);
            group = (String) groups.get(i);
            if (group == null) {
                group = "null";
            }
            if (name == null) {
                name = "null";
            }
            if (group.trim().length() == 0) {
                group = "null";
            }
            if (name.trim().length() == 0) {
                name = "null";
            }
            data[i][0] = file;
            data[i][1] = group;
            data[i][2] = name;
            data[i][3] = (Double) values.get(i);
        }
        String[] columnNames = {FILENAME, ROIGROUP, ROINAME, "Pixel value"};

        displayTable(data, columnNames);
    }

    //void createPixelTableNumDen(String file, ArrayList<String> names, ArrayList<String> groups, ArrayList<Double> values, ArrayList<Double> numValues, ArrayList<Double> denValues) {
    void createPixelTableNumDen(String file, List<String> names, List<String> groups, List<Double> values, List<Double> numValues, List<Double> denValues) {

        // Input checks.
        if (names == null || groups == null || values == null) {
            return;
        }
        if (names.isEmpty() || groups.isEmpty() || values.isEmpty()) {
            return;
        }
        if ((groups.size() != values.size()) || (names.size() != values.size())) {
            return;
        }

        // Get data.
        Object[][] data = new Object[values.size()][6];
        String group, name = "";
        for (int i = 0; i < values.size(); i++) {
            name = (String) names.get(i);
            group = (String) groups.get(i);
            if (group == null) {
                group = "null";
            }
            if (name == null) {
                name = "null";
            }
            if (group.trim().length() == 0) {
                group = "null";
            }
            if (name.trim().length() == 0) {
                name = "null";
            }
            data[i][0] = file;
            data[i][1] = group;
            data[i][2] = name;
            data[i][3] = (Double) values.get(i);
            data[i][4] = (Double) numValues.get(i);
            data[i][5] = (Double) denValues.get(i);
        }
        String[] columnNames = {FILENAME, ROIGROUP, ROINAME, "Med. pixel value", "Num pixel value", "Den pixel value"};

        displayTable(data, columnNames);
    }

    // DJ: 12/08/2014 :  added tags to the table
    /**
     * Generates a table for listing Roi info (name and group) and their pixel values. Correct order is required and
     * ArrayLists must have the same length. Output table will look something like the following:
     *
     * Group  |  Tags   |  Name  | Pixel Value
     * ------------------------------
     * group1 | t1, ...  |  name1 | pixel value 1
     * group1 | t1, ...  |  name1 | pixel value 2
     * group1 | t1, ...  |  name2 | pixel value 3
     * group2 | t1, ...  |  name1 | pixel value 1
     * group2 | t1, ...  |  name2 | pixel value 2
     * group3 | t1, ...  |  name1 | pixel value 1
     * group3 | t1, ...  |  name2 | pixel value 2
     * group3 | t1, ...  |  name3 | pixel value 3
     * group3 | t1, ...  |  name3 | pixel value 4
     *
     * @param groups ArrayList of roi names. Repeats expected.
     * @param groups ArrayList of groups. Repeats expected.
     * @param groups ArrayList of pixel values.
     */
    //void createPixelTable(String file, ArrayList<String> names, ArrayList<String> groups, ArrayList<String> tags, ArrayList<Double> values) {
    void createPixelTable(String file, List<String> names, List<String> groups, List<String> tags, List<Double> values) {

        if (tags == null) {
            System.out.println("in creatPixelTable: tags arraylist passed is NULL ");
        } else {
            System.out.println("in creatPixelTable: tags arraylist passed is FINE ");
        }

        // Input checks.
        if (names == null || groups == null || values == null) {
            return;
        }
        if (names.isEmpty() || groups.isEmpty() || values.isEmpty()) {
            return;
        }
        if ((groups.size() != values.size()) || (names.size() != values.size())) {
            return;
        }
        //DJ: 12/08/2014
        if ((tags.size() != values.size()) || (names.size() != values.size())) {
            return;
        }

        // Get data.
        Object[][] data = new Object[values.size()][5];  // DJ: 12/08/2014  : ARG was 4 initially
        String group, name, commaSeparatedTags = "";  // DJ: 12/08/2014
        for (int i = 0; i < values.size(); i++) {
            name = (String) names.get(i);
            group = (String) groups.get(i);
            commaSeparatedTags = (String) tags.get(i);  // DJ: 12/08/2014
            if (group == null) {
                group = "null";
            }
            if (commaSeparatedTags == null) { 
                commaSeparatedTags = "null";  // DJ: 12/08/2014
            }
            if (name == null) {
                name = "null";
            }
            if (group.trim().length() == 0) {
                group = "null";
            }
            if (commaSeparatedTags.trim().length() == 0) { 
                commaSeparatedTags = "null";  // DJ: 12/08/2014
            }
            if (name.trim().length() == 0) {
                name = "null";
            }
            data[i][0] = file;
            data[i][1] = group;
            data[i][2] = commaSeparatedTags;                   // DJ: 12/08/2014
            data[i][3] = name;
            data[i][4] = (Double) values.get(i);
        }
        String[] columnNames = {FILENAME, ROIGROUP, ROITAGS, ROINAME, "Pixel value"};

        displayTable(data, columnNames);
    }

    // DJ: 12/08/2014  added tags to the table.
    //void createPixelTableNumDen(String file, ArrayList<String> names, ArrayList<String> groups, ArrayList<String> tags, ArrayList<Double> values, ArrayList<Double> numValues, ArrayList<Double> denValues) {
    void createPixelTableNumDen(String file, List<String> names, List<String> groups, List<String> tags, List<Double> values, List<Double> numValues, List<Double> denValues) {

        if (tags == null) {
            System.out.println("in creatPixelTableNUMDEN: tags arraylist passed is NULL ");
        } else {
            System.out.println("in creatPixelTableNUMDEN: tags arraylist passed is FINE ");
        }
        // Input checks.
        if (names == null || groups == null || values == null) {
            return;
        }
        if (names.isEmpty() || groups.isEmpty() || values.isEmpty()) {
            return;
        }
        if ((groups.size() != values.size()) || (names.size() != values.size())) {
            return;
        }
        if ((tags.size() != values.size()) || (names.size() != values.size())) // DJ: 12/08/2014
        {
            return;
        }

        // Get data.
        Object[][] data = new Object[values.size()][7];    // DJ: 12/08/2014 : initially the argument was 6
        String group, name, commaSeparatedTags = "";   // DJ: 12/08/2014 
        for (int i = 0; i < values.size(); i++) {
            name = (String) names.get(i);
            group = (String) groups.get(i);
            commaSeparatedTags = (String) tags.get(i);  // DJ: 12/08/2014
            if (group == null) {
                group = "null";
            }
            if (commaSeparatedTags == null) {
                commaSeparatedTags = "null";   // DJ: 12/08/2014
            }
            if (name == null) {
                name = "null";
            }
            if (group.trim().length() == 0) {
                group = "null";
            }
            if (commaSeparatedTags.trim().length() == 0) {
                commaSeparatedTags = "null";   // DJ: 12/08/2014
            }
            if (name.trim().length() == 0) {
                name = "null";
            }
            data[i][0] = file;
            data[i][1] = group;
            data[i][2] = commaSeparatedTags;                       // DJ: 12/08/2014
            data[i][3] = name;
            data[i][4] = (Double) values.get(i);
            data[i][5] = (Double) numValues.get(i);
            data[i][6] = (Double) denValues.get(i);
        }

        // DJ: 12/08/2014 : ROITAGS added
        //Change missing from svn->git migration
        //String[] columnNames = {FILENAME, ROIGROUP, ROITAGS, ROINAME, "Med. pixel value", "Num pixel value", "Den pixel value"};
        String[] columnNames = {FILENAME, ROIGROUP, ROITAGS, ROINAME, "Ratio pixel value", "Num pixel value", "Den pixel value"};
        //end

        displayTable(data, columnNames);
    }

    /**
     * Does the actual displaying of the table and frame.
     */
    private void displayTable(Object[][] data, String[] columnNames) {

        // Create table and set column width.
        MyDefaultTableModel tm = new MyDefaultTableModel(data, columnNames);
        table = new JTable(tm);
        table = autoResizeColWidth();
        table.setAutoCreateRowSorter(true);
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(true);
        setColumnRenderer();

        // Set the popup menu.
        popupMenu = new JPopupMenu();
        JMenuItem popupmenuItem = new JMenuItem(DELETE_COLUMN_CMD);
        popupmenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeColumnAndData();
            }
        });
        popupMenu.add(popupmenuItem);
        MouseListener popupListener = new PopupListener();
        table.getTableHeader().addMouseListener(popupListener);
        table.addMouseListener(popupListener);

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Create the menu bar.
        JMenuBar menuBar = new JMenuBar();
        JMenu menu;
        JMenuItem menuItem;
        menu = new JMenu("File");
        menuBar.add(menu);

        // Save as menut item.
        menuItem = new JMenuItem("Save as...");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt);
            }
        });
        menu.add(menuItem);

        // Generate report menu item.
        menuItem = new JMenuItem("Generate report");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateReport();
            }
        });
        menu.add(menuItem);

        // Generate frame.
        String title = gui.getImageFilePrefix();
        for (int i = 0; i < images.length; i++) {
            title += " : " + images[i].getShortTitle();
        }
        frame = new JFrame(title);
        frame.setJMenuBar(menuBar);
        frame.setContentPane(scrollPane);
        frame.pack();
    }

    /**
     * Determines the best way to go about appending data.
     */
    private void appendDataToTable(Object[][] data, String[] columnNames) {
        TableModel tm = table.getModel();
        MyDefaultTableModel model = (MyDefaultTableModel) tm;
        for (int i = 0; i < data.length; i++) {
            model.addRow(data[i]);
            model.setColumnIdentifiers(columnNames);
        }
        autoResizeColWidth();

        // Update title.
        String title = gui.getImageFilePrefix();
        for (int i = 0; i < images.length; i++) {
            title += " : " + images[i].getShortTitle();
        }

        frame.setTitle(title);
    }

    /**
     * Determines if it is possible to append data.
     */
    private boolean ableToAppendData(String[] columnNames) {

        // Cant append if any of the following conditions are satisfied.
        if (frame == null) {
            return false;
        } else if (!frame.isVisible()) {
            return false;
        } else if (table == null) {
            return false;
        } else if (!tableColumnsMatch(columnNames)) {
            return false;
        }

        return true;
    }

    /**
     * Use this method when getting data for single planes. Produces a differently formated table than getDataSet().
     */
    private Object[][] getRoiDataSet() {

        // Image check.
        if (images.length != 1) {
            return null;
        }

        // Slice check.
        if (planes.size() != 1) {
            return null;
        }

        // Roi check.
        if (rois.length == 0) {
            return null;
        }

        // initialize data.
        int nRows = rois.length;
        int nCols = ROIMANAGER_MANDATORY_COLUMNS.length + stats.length;
        Object data[][] = new Object[nRows][nCols];
        int plane = (Integer) planes.get(0);
        MimsPlus image = images[0];
        Roi roi;
        String stat;

        // Set the plane.
        if (image.getMimsType() == MimsPlus.HSI_IMAGE) {
            image = image.internalRatio;
        }
        if (image.getMimsType() == MimsPlus.MASS_IMAGE) {
            image.setSlice(plane, false);
        } else if (image.getMimsType() == MimsPlus.RATIO_IMAGE) {
            image.setSlice(plane, image);
        }

        // Fill in "mandatory" fields... ususally non-numeric file/Roi information.
        for (int row = 0; row < rois.length; row++) {
            roi = rois[row];
            for (int col = 0; col < ROIMANAGER_MANDATORY_COLUMNS.length; col++) {
                stat = ROIMANAGER_MANDATORY_COLUMNS[col];
                if (stat.equals(ROIGROUP)) {
                    ROIgroup group = gui.getRoiManager().getRoiGroup(roi.getName());
                    if (group == null) {
                        group = null;
                    }
                    data[row][col] = group.getGroupName();
                } else if (stat.equals(ROINAME)) {
                    data[row][col] = roi.getName();
                } else if (stat.equals(SLICE)) {
                    data[row][col] = (new Integer(images[0].getCurrentSlice())).toString();
                } else {
                    data[row][col] = "null";
                }
            }
        }

        // Fill in the data.
        ImageStatistics imageStats = null;
        for (int row = 0; row < rois.length; row++) {
            roi = rois[row];
            int colnum = SUM_IMAGE_MANDOTORY_COLUMNS.length;

            // Set the ROI location.
            Integer[] xy = gui.getRoiManager().getRoiLocation(rois[row].getName(), plane);
            rois[row].setLocation(xy[0], xy[1]);
            image.setRoi(rois[row]);
            //  Rectangle rect = image.getProcessor().getRoi();
            //int area = rect.width*rect.height;
            // OMLOGGER.fine("Roi " + rois[row].getName() + ": " + area);
            //NOTE:For some reason when I calculate the area the same way as ImageJ does in ImageStatistics (http://rsb.info.nih.gov/ij/developer/source/ij/process/FloatStatistics.java.html)
            //I get a value of 1 even when getSingleStat gives an area of 0 as it should be
            //Therefore I assume that anything with an area of 1 will have no area.
            //if (area <= 1){
            //OMLOGGER.fine("Roi " + rois[row].getName() + " has zero area and is invalid.");
            // }
            for (int col = 0; col < stats.length; col++) {
                stat = stats[col];
                // "Group" is a mandatory row, so ignore if user selected it.
                if (stat.startsWith(GROUP)) {
                    continue;
                }
                // if (area > 1){
                data[row][colnum] = MimsJFreeChart.getSingleStat(image, stat, gui);
                // }else{
                //    data[row][colnum] = 0;
                //}
                colnum++;
            }
        }

        return data;
    }

    /**
     * Use this method when getting data for single planes. Produces a differently formated table than getDataSet().
     */
    private Object[][] getSumImageDataSet() {

        // Roi check.
        if (rois.length == 0) {
            return null;
        }

        // initialize data.
        int nRows = rois.length;
        int nCols = (SUM_IMAGE_MANDOTORY_COLUMNS.length + stats.length) * images.length;
        Object data[][] = new Object[nRows][nCols];
        Roi roi;
        MimsPlus image;
        String stat;

        // Fill in "mandatory" fields... ususally non-numeric file/Roi information.
        for (int row = 0; row < rois.length; row++) {
            roi = rois[row];
            for (int col = 0; col < SUM_IMAGE_MANDOTORY_COLUMNS.length; col++) {
                stat = SUM_IMAGE_MANDOTORY_COLUMNS[col];
                if (stat.equals(FILENAME)) {
                    data[row][col] = gui.getOpener().getImageFile().getName();
                } else if (stat.equals(ROIGROUP)) { 
                    ROIgroup group = gui.getRoiManager().getRoiGroup(roi.getName());
                    if (group == null) {
                        group = null;
                    }
                    data[row][col] = group;
                } // DJ: 12/08/2014
                else if (stat.equals(ROITAGS)) {
                    String commaSeparatedTags = "";
                    //ArrayList<String> tags = gui.getRoiManager().getRoiTags(roi.getName());
                    List<String> tags = gui.getRoiManager().getRoiTags(roi.getName());
                    if (tags == null) {
                        commaSeparatedTags += "null";
                    } else {
                        for (int ii = 0; ii < tags.size(); ii++) {
                            System.out.println(tags.get(ii));
                            if (ii == tags.size() - 1) {
                                commaSeparatedTags += tags.get(ii);
                            } else {
                                commaSeparatedTags += tags.get(ii) + " ,";
                            }
                        }
                    }
                    data[row][col] = commaSeparatedTags;
                } else if (stat.equals(ROINAME)) {
                    data[row][col] = roi.getName();
                } else {
                    data[row][col] = "null";
                }
            }
        }

        // Fill in rest of data... statistics.
        for (int row = 0; row < rois.length; row++) {
            roi = rois[row];
            int colnum = SUM_IMAGE_MANDOTORY_COLUMNS.length;

            for (int col1 = 0; col1 < images.length; col1++) {
                image = images[col1];

                // Sum images, by definition, are only 1 plane. Since Rois
                // can have different location on different planes, we will
                // choose the location for the currently displayed slice.
                int plane = gui.getMassImages()[0].getCurrentSlice();
                Integer[] xy = gui.getRoiManager().getRoiLocation(roi.getName(), plane);
                roi.setLocation(xy[0], xy[1]);
                image.setRoi(roi);

                for (int col2 = 0; col2 < stats.length; col2++) {
                    stat = stats[col2];

                    // "Group" is a mandatory row, so ignore if user selected it.
                    if (stat.startsWith(GROUP)) {
                        continue;
                    }

                    // Some stats we only want to put in once, like "area".
                    if (col1 == 0) {
                        data[row][colnum] = MimsJFreeChart.getSingleStat(image, stat, gui);
                    } else {
                        if (stat.equals(AREA)) {
                            continue;
                        } else {
                            data[row][colnum] = MimsJFreeChart.getSingleStat(image, stat, gui);
                        }
                    }
                    colnum++;
                }
            }
        }

        return data;
    }

    /**
     * Use this method when getting data for multiple planes.
     */
    private Object[][] getDataSet() {

        // initialize variables.
        int currentSlice = gui.getOpenMassImages()[0].getCurrentSlice();
        Object[][] data = new Object[planes.size()][rois.length * images.length * stats.length + 1];

        // Fill in "slice" field.
        for (int ii = 0; ii < planes.size(); ii++) {
            data[ii][0] = (Integer) planes.get(ii);
        }

        // Fill in data.
        for (int ii = 0; ii < planes.size(); ii++) {
            int col = 1;
            int plane = (Integer) planes.get(ii);
            for (int j = 0; j < images.length; j++) {
                MimsPlus image = images[j];
                if (image.getMimsType() == MimsPlus.MASS_IMAGE) {
                    image.setSlice(plane, false);
                } else if (image.getMimsType() == MimsPlus.RATIO_IMAGE) {
                    image.setSlice(plane, image);
                }
                for (int i = 0; i < rois.length; i++) {

                    Integer[] xy = gui.getRoiManager().getRoiLocation(rois[i].getName(), plane);
                    rois[i].setLocation(xy[0], xy[1]);
                    image.setRoi(rois[i]);

                    for (int k = 0; k < stats.length; k++) {

                        if (j == 0) {
                            if (stats[k].startsWith(GROUP)) {
                                ROIgroup group = gui.getRoiManager().getRoiGroup(rois[i].getName());
                                if (group == null) {
                                    //group = "null";
                                }
                                data[ii][col] = group;
                            } else {
                                data[ii][col] = MimsJFreeChart.getSingleStat(image, stats[k], gui);
                            }
                        } else {
                            if ((stats[k].startsWith(GROUP) || stats[k].equalsIgnoreCase(AREA))) {
                                continue;
                            }
                            else {
                                data[ii][col] = MimsJFreeChart.getSingleStat(image, stats[k], gui);
                            }
                        }
                        col++;
                    }
                }
            }
        }

        gui.getOpenMassImages()[0].setSlice(currentSlice);

        return data;
    }

    /**
     * Returns the column names.
     */
    public String[] getColumnNames() {

        //DJ: 11/14/2014
        // initialze variables.
        ArrayList<String> columnNamesArray = new ArrayList<String>();
        String header = "";
        columnNamesArray.add(SLICE);
        String tableOnly = "(table only)";

        // Generate header based on image, roi, stat.
        int col = 1;
        for (int j = 0; j < images.length; j++) {
            for (int i = 0; i < rois.length; i++) {
                for (int k = 0; k < stats.length; k++) {
                    String stat = stats[k];
                    if (j == 0) {
                        if (stats[k].startsWith(GROUP)) {
                            stat = GROUP;
                        }
                    } else if ((stats[k].startsWith(GROUP) || stats[k].equalsIgnoreCase(AREA))) {
                        continue;
                    }

                    header = images[j].getTitleForTables() + " ";

                    if (images[j].getMimsType() == MimsPlus.SUM_IMAGE) {
                        header = images[j].getTitleForTables() + " SUM ";

                    }
                    /*
                // DJ
                else if (images[j].getMimsType() == MimsPlus.RATIO_IMAGE) {
                 
                    if(originalImagesTypes != null){
                        if(originalImagesTypes[j] == MimsPlus.HSI_IMAGE){
                            header = images[j].getTitleForTables() + " HSI ";
                        }
                    }
                }
                     */

                    header += System.getProperty("line.separator") + stat + " ROI " + rois[i].getName();

                    /*
               String prefix = "_";
               if (images[j].getMimsType() == MimsPlus.MASS_IMAGE || images[j].getMimsType() == MimsPlus.RATIO_IMAGE)
                  prefix = "_m";
                     */
                    //header = images[j].getTitleForTables() + "\n" + stat;
                    System.out.println(header + "\n");

                    //header = stat + prefix + images[j].getRoundedTitle(true) + "_r" + rois[i].getName();
                    if (stat.matches(AREA) || stat.matches(GROUP)) {
                        header = stat + " ROI " + rois[i].getName();
                    }
                    columnNamesArray.add(header);
                    col++;
                }
            }
        }

        // Fill in columnNames array.
        String[] columnNames = new String[columnNamesArray.size()];
        for (int i = 0; i < columnNames.length; i++) {
            columnNames[i] = columnNamesArray.get(i);
        }

        return columnNames;

        /*
       // ORIGINAL IMPLEMENTATION:
      // initialze variables.
      ArrayList<String> columnNamesArray = new ArrayList<String>();
      String header = "";
      columnNamesArray.add(SLICE);
      String tableOnly = "(table only)";

      // Generate header based on image, roi, stat.
      int col = 1;
      for (int j = 0; j < images.length; j++) {
         for (int i = 0; i < rois.length; i++) {
            for (int k = 0; k < stats.length; k++) {
               String stat = stats[k];
               if (j == 0) {
                  if (stats[k].startsWith(GROUP))
                     stat = GROUP;
               } else {
                  if ((stats[k].startsWith(GROUP) || stats[k].equalsIgnoreCase(AREA)))
                     continue;
               }  
               String prefix = "_";
               if (images[j].getType() == MimsPlus.MASS_IMAGE || images[j].getType() == MimsPlus.RATIO_IMAGE)
                  prefix = "_m";
               header = stat + prefix + images[j].getRoundedTitle(true) + "_r" + rois[i].getName();
               if (stat.matches(AREA) || stat.matches(GROUP))
                  header = stat + "_r" + rois[i].getName();
               columnNamesArray.add(header);
               col++;
            }
         }
      }

      // Fill in columnNames array.
      String[] columnNames = new String[columnNamesArray.size()];
      for (int i = 0; i < columnNames.length; i++) {
         columnNames[i] = columnNamesArray.get(i);
      }

      return columnNames;
         */
    }

    /**
     * Setup column headers for sum image table.
     */
    private String[] getSumImageColumnNames() {

        // initialze variables.
        ArrayList<String> columnNamesArray = new ArrayList<String>();
        MimsPlus image;
        String stat;

        // Mandatory columns first.
        for (int col = 0; col < SUM_IMAGE_MANDOTORY_COLUMNS.length; col++) {
            String header = SUM_IMAGE_MANDOTORY_COLUMNS[col];
            if (header.equals(FILENAME)) {
                columnNamesArray.add(FILENAME);
            } else if (header.equals(ROIGROUP)) {
                columnNamesArray.add(ROIGROUP);
            } else if (header.equals(ROITAGS)) {
                columnNamesArray.add(ROITAGS);  // DJ: 12/08/2014
            } else if (header.equals(ROINAME)) {
                columnNamesArray.add(ROINAME);
            } else {
                columnNamesArray.add("null");
            }
        }

        // Data column headers.
        for (int col1 = 0; col1 < images.length; col1++) {
            image = images[col1];
            for (int col2 = 0; col2 < stats.length; col2++) {
                stat = stats[col2];
                //String label = stat + " " + image.getRoundedTitle(true);  // commented out by DJ: 11/14/2014
                String label = image.getTitleForTables() + " " + stat;

                /*
            // DJ: 11/14/2014
             if (image.getMimsType() == MimsPlus.RATIO_IMAGE) {
                 if (originalImagesTypes != null) {
                     if (originalImagesTypes[col1] == MimsPlus.HSI_IMAGE) {
                         label = images[col1].getTitleForTables() + " HSI ";
                     }
                 }
             }
                 */
                if (stat.startsWith(GROUP)) {
                    continue;
                }

                if (col1 > 0 && stat.equals(AREA)) {
                    continue;
                } else if (col1 == 0 && stat.equals(AREA)) {
                    columnNamesArray.add(AREA);
                } else {
                    columnNamesArray.add(label);
                }
            }
        }

        // Assemble column headers.
        String[] columnNames = new String[columnNamesArray.size()];
        for (int i = 0; i < columnNames.length; i++) {
            columnNames[i] = columnNamesArray.get(i);
        }

        return columnNames;
    }

    /**
     * Setup column headers for sum image table.
     */
    private String[] getRoiManagerColumnNames() {

        // Fill in preliminary mandatory columns headers.
        ArrayList<String> columnNamesArray = new ArrayList<String>();
        int colnum = 0;
        for (int i = 0; i < ROIMANAGER_MANDATORY_COLUMNS.length; i++) {
            columnNamesArray.add(ROIMANAGER_MANDATORY_COLUMNS[i]);
            colnum++;
        }

        // Fill in headers for stats.
        for (int i = 0; i < stats.length; i++) {
            if (stats[i].startsWith(GROUP)) {
                continue;
            }
            columnNamesArray.add(stats[i]);
            colnum++;
        }

        // Assemble column headers.
        String[] columnNames = new String[columnNamesArray.size()];
        for (int i = 0; i < columnNames.length; i++) {
            columnNames[i] = columnNamesArray.get(i);
        }

        // Return.
        return columnNames;
    }

    /**
     * Determines the behavior of the "Save" action.
     */
    private void saveActionPerformed(ActionEvent evt) {
        MimsJFileChooser fc = new MimsJFileChooser(gui);
        MIMSFileFilter mff_txt = new MIMSFileFilter("txt");
        mff_txt.setDescription("Text file");
        fc.addChoosableFileFilter(mff_txt);
        fc.setFileFilter(mff_txt);
        MIMSFileFilter mff_csv = new MIMSFileFilter("csv");
        mff_csv.setDescription("Comma seperated value");
        fc.addChoosableFileFilter(mff_csv);
        fc.setFileFilter(mff_csv);
        fc.setPreferredSize(new java.awt.Dimension(650, 500));
        String lastFolder = gui.getLastFolder();

        try {
            fc.setSelectedFile(new File(lastFolder, gui.getImageFilePrefix() + DEFAULT_TABLE_NAME));

            int returnVal = fc.showSaveDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                writeData(fc.getSelectedFile());
            } else {
                return;
            }
        } catch (Exception e) {
            ij.IJ.error("Save Error", "Error saving file.");
        } finally {
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Writes the actual data.
     */
    public void writeData(File file) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(file));

            // Write column headers
            for (int i = 0; i < table.getColumnCount(); i++) {
                out.print('"' + table.getColumnName(i) + '"');
                if (i < table.getColumnCount() - 1) {
                    out.print(",");
                }
            }
            out.println();

            // Write data
            String value = "";
            for (int i = 0; i < table.getRowCount(); i++) {
                for (int j = 0; j < table.getColumnCount(); j++) {
                    Object objVal = table.getValueAt(i, j);
                    if (value == null) {
                        value = "null";
                    } else {
                        value = objVal.toString();
                    }
                    if ((objVal instanceof String && !table.getColumnName(j).equals(new String("Slice")))
                            || table.getColumnName(j).equals(ROINAME)
                            || table.getColumnName(j).equals(ROIGROUP)
                            || table.getColumnName(j).equals(ROITAGS)) {    // DJ: 12/08/2014
                        out.print('"' + value + '"');
                    } else {
                        out.print(value);
                    }
                    if (j < table.getColumnCount() - 1) {
                        out.print(",");
                    }
                }
                out.println();
            }

            // Close file
            out.close();

        } catch (IOException e) {
            IJ.error("Unable to write data. Possible permissions error.");
        }
    }

    /**
     * Determines if the number of columns is the same.
     */
    private boolean tableColumnsMatch(String[] columnNames) {

        int numCol1 = columnNames.length;
        int numCol2 = ((DefaultTableModel) table.getModel()).getColumnCount();

        if (numCol1 != numCol2) {
            return false;
        }

        return true;
    }

    /**
     * Display the reportGenerator object for generating user reports.
     */
    public void generateReport() {
        ReportGenerator rg = new ReportGenerator(gui, this);
        rg.setVisible(true);
    }

    /**
     * Gets the scroll pane containing the table.
     */
    public JTable getJTable() {
        return table;
    }

    /**
     * Sets the images to be included in the table.
     *
     * @param images a set of MimsPlus images.
     */
    public void setImages(MimsPlus[] images) {
        this.images = images;
    }

    /*
   // DJ
   public void setOriginalImagesTypes(int[] mimsTypes){      
      this.originalImagesTypes = mimsTypes;
   }
     */
    /**
     * Sets the statistics to be included in the table.
     *
     * @param stats a set of statistics.
     */
    public void setStats(String[] stats) {
        this.stats = stats;
    }

    /**
     * Sets the ROIs to be included in the table.
     *
     * @param rois a set of ROIs.
     */
    public void setRois(Roi[] rois) {
        this.rois = rois;
    }

    /**
     * Displays the frame (with table).
     */
    public void showFrame() {
        if (frame != null) {
            frame.setVisible(true);
            frame.toFront();
        }
    }

    /**
     * Nulls the table and sets the frame to not visible.
     */
    public void close() {
        table = null;
        if (frame != null) {
            frame.setVisible(false);
        }
    }

    /**
     * Get the currently selected rows indices
     *
     * @return an arraylist containing the indices of all selected rows, or null if no table
     */
    public ArrayList<Integer> getSelectedImageRows() {
        if (table != null) {
            int[] rowsArray = table.getSelectedRows();
            ArrayList<Integer> rows = new ArrayList<Integer>();
            for (int value : rowsArray) {
                rows.add(Integer.valueOf(value + 1));
            }
            return rows;
        } else {
            return null;
        }
    }

    /**
     * Get the currently selected rows 'slice' value
     *
     * @return an arraylist containing the indices of all selected rows, or null if no table
     */
    public ArrayList<Integer> getSelectedSlices() {
        if (table != null) {
            int[] rowsArray = table.getSelectedRows();
            ArrayList<Integer> rows = new ArrayList<Integer>();
            for (int value : rowsArray) {
                rows.add((Integer) table.getValueAt(value, 0));
            }
            return rows;
        } else {
            return null;
        }
    }

    /**
     * Sets the planes to be included in the table.
     *
     * @param planes an arraylist of planes.
     */
    //public void setPlanes(ArrayList planes) {
    public void setPlanes(List planes) {

        this.planes = planes;
    }

    /**
     * Adjust the size of the columns correctly.
     *
     * @param table the JTable.
     * @param model the table model.
     */
    private JTable autoResizeColWidth() {

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int margin = 5;
        int minWidth = 75;

        for (int i = 0; i < table.getColumnCount(); i++) {
            int vColIndex = i;
            DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
            TableColumn col = colModel.getColumn(vColIndex);
            int width = 0;

            // Get width of column header
            TableCellRenderer renderer = col.getHeaderRenderer();

            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }

            Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);

            width = comp.getPreferredSize().width;

            // Get maximum width of column data
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, vColIndex);
                comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false,
                        r, vColIndex);
                width = Math.max(width, comp.getPreferredSize().width);
            }

            // Add margin
            width += 2 * margin;
            if (width < minWidth) {
                width = minWidth;
            }

            // Set the width
            col.setPreferredWidth(width);
        }

        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(
                SwingConstants.CENTER);

        return table;
    }

    // Removes the selectedCulmn from the table and the associated
    // call data from the table model.
    public void removeColumnAndData() {
        MyDefaultTableModel model = (MyDefaultTableModel) table.getModel();
        TableColumn col = table.getColumnModel().getColumn(selectedColumn);
        int columnModelIndex = col.getModelIndex();
        Vector data = model.getDataVector();
        Vector colIds = model.getColumnIdentifiers();

        // Remove the column from the table
        table.removeColumn(col);

        // Remove the column header from the table model
        colIds.removeElementAt(columnModelIndex);

        // Remove the column data
        for (int r = 0; r < data.size(); r++) {
            Vector row = (Vector) data.get(r);
            row.removeElementAt(columnModelIndex);
        }
        model.setDataVector(data, colIds);

        // Correct the model indices in the TableColumn objects
        // by decrementing those indices that follow the deleted column
        Enumeration colEnum = table.getColumnModel().getColumns();
        for (; colEnum.hasMoreElements();) {
            TableColumn c = (TableColumn) colEnum.nextElement();
            if (c.getModelIndex() >= columnModelIndex) {
                c.setModelIndex(c.getModelIndex() - 1);
            }
        }
        model.fireTableStructureChanged();
        autoResizeColWidth();
    }

    class PopupListener implements MouseListener {

        public void mousePressed(MouseEvent e) {
            showPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }

        private void showPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                JTable source;
                if (e.getSource() instanceof JTableHeader) {
                    source = ((JTableHeader) e.getSource()).getTable();
                } else if (e.getSource() instanceof JTable) {
                    source = (JTable) e.getSource();
                } else {
                    return;
                }
                int row = source.rowAtPoint(e.getPoint());
                int column = source.columnAtPoint(e.getPoint());
                selectedColumn = column;
                if (!source.isRowSelected(row)) {
                    source.changeSelection(row, column, false, false);
                }
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }

    /**
     * Sets the renderers for the various columns.
     */
    public void setColumnRenderer() {
        //table.setRowHeight(0, table.getRowHeight(0)*2);
        //table.get
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(new OpenMIMSTableFormatRenderer());
        }
    }

    /**
     * A custom renderer that display a Number to two decimal places.
     */
    class OpenMIMSTableFormatRenderer extends DefaultTableCellRenderer {

        private DecimalFormat formatter = new DecimalFormat("0.00");
        private DecimalFormat wholeNumberFormatter = new DecimalFormat("##");

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // First format the cell value as required
            formatter.setMaximumFractionDigits(gui.getPreferences().getNumDecimalPlaces());
            formatter.setMinimumFractionDigits(gui.getPreferences().getNumDecimalPlaces());

            String colName = table.getColumnName(column);
            if (colName.startsWith(GROUP) || colName.matches(FILENAME) || colName.matches(TRUE)
                    || colName.matches(ROIGROUP) || colName.matches(ROINAME) || colName.matches(SLICE)
                    || colName.matches(ROITAGS)) {              //DJ: 12/08/2014
                setHorizontalAlignment(JLabel.LEFT);
            } else if (colName.startsWith(AREA)) {
                value = wholeNumberFormatter.format((Number) value);
            } else {

                value = formatter.format((Number) value);
                /*
             Object[] objects = {"Hello", System.getProperty("line.separator"), "There"};
             java.text.MessageFormat mFormater = new java.text.MessageFormat("{0}{1}{2}{1}");
             value = mFormater.format(objects);
             System.out.println(value);
                 */
            }

            // And pass it on to parent class
            return super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
        }
    }
    /*
   public class LineWrapCellRenderer  extends JTextArea implements TableCellRenderer {

       private DecimalFormat formatter = new DecimalFormat("0.00");
       private DecimalFormat wholeNumberFormatter = new DecimalFormat("##");
       
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // First format the cell value as required
         formatter.setMaximumFractionDigits(gui.getPreferences().getNumDecimalPlaces());
         formatter.setMinimumFractionDigits(gui.getPreferences().getNumDecimalPlaces());

         String colName = table.getColumnName(column);
         if (colName.startsWith(GROUP) || colName.matches(FILENAME) || colName.matches(TRUE) ||
             colName.matches(ROIGROUP) || colName.matches(ROINAME)  || colName.matches(SLICE)) {
             setHorizontalAlignment(JLabel.LEFT);
         } else if (colName.startsWith(AREA)) {
             value = wholeNumberFormatter.format((Number) value);
         } else {
             
             value = formatter.format((Number) value);
             /*
             Object[] objects = {"Hello", System.getProperty("line.separator"), "There"};
             java.text.MessageFormat mFormater = new java.text.MessageFormat("{0}{1}{2}{1}");
             value = mFormater.format(objects);
             System.out.println(value);
             
         }

         // And pass it on to parent class
         return super.getTableCellRendererComponent(
                 table, value, isSelected, hasFocus, row, column);
      }
       
   }
     */

}

// This subclass adds a method to retrieve the columnIdentifiers
// which is needed to implement the removal of
// column data from the table model
class MyDefaultTableModel extends DefaultTableModel {

    public MyDefaultTableModel(Object rowData[][], Object columnNames[]) {
        super(rowData, columnNames);
    }

    public Vector getColumnIdentifiers() {
        return columnIdentifiers;
    }

    public Class getColumnClass(int c) {
        Object obj = getValueAt(0, c);
        // If the user is trying to create a table (in the Tomography tab) from
        // a mosaic image, and no group has been created, the getValueAt method 
        // returns null, and this prevents the table from being created.
        // In this case, just return Object.class to prevent the exception.
        if (obj != null) {
            return getValueAt(0, c).getClass();
        } else {
            return Object.class;
        }
    }
}

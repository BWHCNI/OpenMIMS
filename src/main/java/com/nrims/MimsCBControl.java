package com.nrims;

import com.nrims.plot.MimsChartFactory;
import com.nrims.plot.MimsChartPanel;
import com.nrims.plot.MimsXYPlot;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.plugin.LutLoader;
import ij.process.ImageProcessor;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Hashtable;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPolygonAnnotation;
import org.jfree.chart.plot.PlotOrientation;

import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.ui.Layer;

/**
 * The MimsCBControl class creates the "Contrast" tabbed panel. It is a GUI element used for controlling parameters
 * related to the Brightness and Contrast settings of displayed images. It also displays a histogram of pixel values for
 * specific images and also contains elements for controlling which LUT is being used by the plugin.
 *
 * @author zkaufman
 */
public class MimsCBControl extends javax.swing.JPanel {

    Hashtable windows = new Hashtable();
    UI ui;
    private JFreeChart chart;
    private MimsChartPanel chartPanel;
    boolean holdUpdate = false;
    private File lutDir;
    private String[] ijLutNames = new String[]{"Grays", "Fire", "Ice", "Spectrum", "Red", "Green", "Blue", "Cyan", "Magenta", "Yellow", "Red/Green", "Invert LUT"};
    private ArrayList<String> ijLutNameArray = new ArrayList<String>();
    public com.nrims.managers.CompositeManager compManager;

    /**
     * Constructor for MimsCBControl. A pointer to UI is required.
     *
     * @param ui a pointer to the UI object.
     */
    public MimsCBControl(UI ui) {
        this.ui = ui;
        initComponents();
        jLabel1.setText("");
        Dimension d = new Dimension(350, 200);
        jPanel1.setMinimumSize(d);
        jPanel1.setPreferredSize(d);
        jPanel1.setMaximumSize(d);
        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        setupLutComboBox();
        setupHistogram();
    }

    /**
     * Sets the LUT to a specific string value. Must match one of the entries in the jComboBox oj LUT names.
     *
     * @param lut name of lut.
     */
    void setLUT(String lut) {
        holdUpdate = true;
        jComboBox2.setSelectedItem(lut);
        holdUpdate = false;
    }

    /**
     * Sets up the list of entries in the jComboBox based on default LUTs and those in the imagej lut directory.
     */
    private void setupLutComboBox() {

        // Get all the lut files.
        lutDir = new File(Prefs.getHomeDir(), "luts");
        File[] lutFiles = new File[0];
        if (lutDir.exists()) {
            lutFiles = lutDir.listFiles(new LutFileFilter());
        }

        // Assemple IJ luts and file luts into one String[] object.
        int i = 0;
        String[] lutNames = new String[ijLutNames.length + lutFiles.length];
        for (String ijLutName : ijLutNames) {
            ijLutNameArray.add(ijLutName);
            lutNames[i] = ijLutName;
            i++;
        }
        for (File lutFile : lutFiles) {
            lutNames[i] = lutFile.getName().substring(0, lutFile.getName().indexOf(".lut"));
            i++;
        }

        // Construct the combobox.
        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(lutNames));
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

    }

    /**
     * Sets up the histogram that displays image pixel data.
     */
    private void setupHistogram() {

        // Create chart using the ChartFactory.
        chart = MimsChartFactory.createMimsHistogram("", "Pixel Value", "", null, PlotOrientation.VERTICAL, true, true, false);
        chart.setBackgroundPaint(this.getBackground());
        chart.removeLegend();

        // Set the renderer.
        MimsXYPlot plot = (MimsXYPlot) chart.getPlot();
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardXYBarPainter());

        // Listen for key pressed events.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED && thisIsVisible() && ui.isActive()) {
                    chartPanel.keyPressed(e);
                }
                return false;
            }
        });

        // Movable range and domain.
        plot.setDomainPannable(true);
        plot.setRangePannable(true);

        chartPanel = new MimsChartPanel(chart);
        chartPanel.setSize(350, 225);
        jPanel1.add(chartPanel);
    }

    /**
     * Call this method whenever you want to update the histogram. For example, changing planes, applying offsets, etc.
     * Histogram updates to reflect the image whose title is selected in combobox.
     */
    public void updateHistogram() {

        // Dont bother updating histogram if tab not even visible, save resources.
        if (!this.isVisible()) {
            return;
        }

        // Initialize imp variable.
        MimsPlus imp = null;

        // Get the title of the window currently selected in the combobox.
        // Then get the window associated with that title. 
        String title = (String) jComboBox1.getSelectedItem();
        if (title != null) {
            imp = (MimsPlus) windows.get(title);
        }

        // Not sure why but sometimes images have NULL image processors.
        // Cant update histogram if it does not have one.
        if (imp == null) {
            return;
        }
        ImageProcessor ip = imp.getProcessor();
        if (ip == null) {
            return;
        }

        // Update the sliders.
        contrastAdjuster1.update(imp);

        // Get Pixel values.
        int i = 0;
        int width = imp.getWidth();
        int height = imp.getHeight();
        int nbins = 256;
        double[] pixels = new double[width * height];
        double pixelVal, maxVal = 0.0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixelVal = ip.getPixelValue(x, y);
                pixels[i] = pixelVal;
                if (maxVal < pixelVal) {
                    maxVal = pixelVal;
                }
                i++;
            }
        }

        // return if no data avaialable
        if (pixels == null) {
            return;
        }
        if (pixels.length == 0) {
            return;
        }

        // Setup and plot histogram.
        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("", pixels, nbins);
        MimsXYPlot plot = (MimsXYPlot) chart.getPlot();
        plot.setDataset(dataset);

        // Show contrast window.
        updateContrastWindow();

        // Final plot adjustments.   maxVal cannot be zero, or JFreeChart will generate this error:
        // IllegalArgumentException: A positive range length is required: Range[0.0,0.0]
        if (maxVal == 0) {
            maxVal = 0.00001;
        }
        plot.getDomainAxis().setRange(0, maxVal);
        plot.setDomainGridlinesVisible(false);
        chart.fireChartChanged();
    }

    /**
     * Updates the window range displayed in the histogram representing the max and min brightness.
     */
    public void updateContrastWindow() {

        MimsXYPlot plot = (MimsXYPlot) chart.getPlot();

        // Polygon bounds.      
        double x1 = contrastAdjuster1.min;
        double x2 = contrastAdjuster1.min;
        double x3 = contrastAdjuster1.max;
        double x4 = contrastAdjuster1.max;

        double y1 = -99999999;
        double y2 = 99999999;
        double y3 = 99999999;
        double y4 = -99999999;

        // Displays polygon.
        XYPolygonAnnotation a = new XYPolygonAnnotation(new double[]{x1, y1, x2, y2, x3, y3, x4, y4},
                new BasicStroke(1), new Color(125, 125, 175, 100), new Color(125, 125, 175, 100));
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.removeAnnotations();
        renderer.addAnnotation(a, Layer.BACKGROUND);
    }

    /**
     * Sets the title selected in the combobox. This method is used to set the combobox to be the most recently
     * activated window.
     *
     * @param title the title of the window.
     */
    public void setWindowlistCombobox(String title) {
        if (windows.containsKey(title)) {
            jComboBox1.setSelectedItem(title);
        }
    }

    /**
     * Adds a window name to the list of entries in the combobox. Newly created ratio images (and possibly sum images)
     * should be added to the list, and removed from the list when destroyed.
     *
     * @param imp the image object.
     */
    public void addWindowtoList(MimsPlus imp) {
        String title = imp.getTitle();
        if (!windows.containsKey(title)) {
            jComboBox1.addItem(title);
            windows.put(title, imp);
        }
        if (jComboBox1.getItemCount() == 1) {
            jComboBox1.setSelectedItem(title);
        }
    }

    /**
     * Removes an image windows title from combobox list. Should be used whenever a window is closed (or no longer
     * selectable).
     *
     * @param imp the image object to remove.
     */
    public void removeWindowfromList(MimsPlus imp) {
        String title = imp.getTitle();
        if (windows.containsKey(title)) {
            jComboBox1.removeItem(title);
            windows.remove(title);
        }
    }

    /**
     * Determines the layout of the user interface.
     */
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        contrastAdjuster1 = new com.nrims.ContrastAdjuster(ui);
        jLabel1 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jRadioButton1 = new javax.swing.JRadioButton();
        jComboBox2 = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();

        jLabel1.setText("Contrast / Brightness");

        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel2.setText("Window :");

        jRadioButton1.setText("Auto adjust");
        jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });

        jLabel5.setText("Lookup Table :");

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 302, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 203, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(layout.createSequentialGroup()
                                        .addGap(81, 81, 81)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGroup(layout.createSequentialGroup()
                                                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(jRadioButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGap(83, 83, 83)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel5)))
                                .addGroup(layout.createSequentialGroup()
                                        .addGap(57, 57, 57)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addComponent(contrastAdjuster1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel1))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(contrastAdjuster1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(33, 33, 33)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel2)
                                .addComponent(jRadioButton1)
                                .addComponent(jLabel5))
                        .addGap(4, 4, 4)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Controls the behavior when an item in jComboBox1 (the list of images) is selected.
     */
    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        MimsPlus imp = null;

        // Get the title of the window currently selected in the combobox.
        // Then get the window associated with that title. 
        String title = (String) jComboBox1.getSelectedItem();
        if (title != null) {
            imp = (MimsPlus) windows.get(title);
        }

        // Select autocontrasting radio button...
        if (imp != null) {
            jRadioButton1.setSelected(imp.getAutoContrastAdjust());
            updateHistogram();
        }
    }//GEN-LAST:event_jComboBox1ActionPerformed

    /**
     * Controls the behavior when jRadioButton1 (the autocontrast button) is selected.
     */
    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        MimsPlus imp = null;

        // Get the title of the window currently selected in the combobox.
        // Then get the window associated with that title. 
        String title = (String) jComboBox1.getSelectedItem();
        if (title != null) {
            imp = (MimsPlus) windows.get(title);
        }
        imp.setAutoContrastAdjust(jRadioButton1.isSelected());

        // If setting to true, do autocontrasting.
        if (jRadioButton1.isSelected()) {
            ui.autoContrastImage(imp);
        }
        updateHistogram();
    }//GEN-LAST:event_jRadioButton1ActionPerformed

    /**
     * Controls the behavior when jComboBox2 (the list of LUTs) is selected.
     */
    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed

        if (holdUpdate) {
            return;
        }

        // Get the selected LUT  
        String label = (String) jComboBox2.getSelectedItem();
        boolean ijlut = false;

        // Manipulate the string
        LutLoader ll = new LutLoader();
        if (ijLutNameArray.contains(label)) {
            ijlut = true;
            if (label.equals("Red/Green")) {
                label = "redgreen";
            } else if (label.equals("Invert LUT")) {
                label = "invert";
            } else {
                label = label.toLowerCase();
            }
        }

        applyLutToAllWindows(label, ijlut);
    }//GEN-LAST:event_jComboBox2ActionPerformed

    /**
     * Applies the selected LUT to all possible windows.
     */
    private void applyLutToAllWindows(String lutlabel, boolean ijlut) {

        LutLoader ll = new LutLoader();
        ImagePlus current_imp = WindowManager.getCurrentImage();

        MimsPlus[] massImages = ui.getOpenMassImages();
        MimsPlus[] ratioImages = ui.getOpenRatioImages();
        MimsPlus[] sumImages = ui.getOpenSumImages();

        // Apply to mass images.
        for (MimsPlus mp : massImages) {
            WindowManager.setCurrentWindow(mp.getWindow());
            if (ijlut) {
                ll.run(lutlabel);
            } else {
                IJ.open((new File(lutDir, lutlabel + ".lut")).getAbsolutePath());
            }
            mp.setLut(lutlabel);
        }

        // Apply to ratio images.      
        for (MimsPlus mp : ratioImages) {
            WindowManager.setCurrentWindow(mp.getWindow());
            if (ijlut) {
                ll.run(lutlabel);
            } else {
                IJ.open((new File(lutDir, lutlabel + ".lut")).getAbsolutePath());
            }
            mp.setLut(lutlabel);
        }

        // Apply to sum images.      
        for (MimsPlus mp : sumImages) {
            WindowManager.setCurrentWindow(mp.getWindow());
            if (ijlut) {
                ll.run(lutlabel);
            } else {
                IJ.open((new File(lutDir, lutlabel + ".lut")).getAbsolutePath());
            }
            mp.setLut(lutlabel);
        }

        WindowManager.setTempCurrentImage(current_imp);
    }

    /**
     * Shows the composite manager user interface.
     */
    public void showCompositeManager() {
        if (this.compManager == null) {
            compManager = new com.nrims.managers.CompositeManager(ui);
        }
        compManager.setVisible(true);
    }

    private boolean thisIsVisible() {
        return this.isVisible();
    }

    //DJ:10/17/2014
    public int getContrastLevel() {
        return contrastAdjuster1.getImageContrastValue();
    }
    //DJ:10/17/2014

    public int getBrightnessLevel() {
        return contrastAdjuster1.getImageBrightnessValue();
    }

    private com.nrims.ContrastAdjuster contrastAdjuster1;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton jRadioButton1;
    // End of variables declaration//GEN-END:variables

}

class LutFileFilter implements FileFilter {

    public boolean accept(File pathname) {
        return (pathname.getAbsolutePath().endsWith(".lut"));
    }
}

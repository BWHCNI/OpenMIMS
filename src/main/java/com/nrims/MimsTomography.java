package com.nrims;

import com.nrims.data.Opener;
import com.nrims.plot.MimsChartFactory;
import com.nrims.plot.MimsChartPanel;
import com.nrims.plot.MimsXYPlot;
import ij.IJ;
import ij.gui.Overlay;

import ij.gui.Roi;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListCellRenderer;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;

/**
 * The MimsTomography class creates the "Tomography" tab on the main UI window. It contains all the functionality needed
 * for generating statistics for ROIs and images. Statistics can be displayed either as a plot or as a table for export.
 * The "Tomography" tab also contains a histogram of pixel values for the "active" (most recently clicked) image.
 *
 * @author cpoczatek
 */
public class MimsTomography extends javax.swing.JPanel {

    private UI ui;
    private Opener image;
    Overlay overlay = new Overlay();
    MimsJTable table = null;
    private JFreeChart chart;
    private MimsChartPanel chartPanel;

    /**
     * The MimsTomography constructor. Assembles the "Tomography" tab.
     *
     * @param ui a pointer to the main UI class.
     */
    public MimsTomography(UI ui) {
        this.ui = ui;
        this.image = ui.getOpener();

        initComponents();
        initComponentsCustom();
        setupHistogram();
    }

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      jLabel3 = new javax.swing.JLabel();
      jScrollPane1 = new javax.swing.JScrollPane();
      statJList = new javax.swing.JList();
      jScrollPane2 = new javax.swing.JScrollPane();
      imageJList = new javax.swing.JList();
      jLabel4 = new javax.swing.JLabel();
      plotButton = new javax.swing.JButton();
      appendCheckBox = new javax.swing.JCheckBox();
      jButton1 = new javax.swing.JButton();
      jTextField1 = new javax.swing.JTextField();
      jLabel5 = new javax.swing.JLabel();
      currentPlaneCheckBox = new javax.swing.JCheckBox();
      jSeparator1 = new javax.swing.JSeparator();
      histogramjPanel = new javax.swing.JPanel();
      histogramUpdatejCheckBox = new javax.swing.JCheckBox();
      profilejButton = new javax.swing.JButton();

      setToolTipText("");

      jLabel3.setText("Statistics to plot");

      statJList.setModel(new javax.swing.AbstractListModel() {
         String[] strings = { "mean", "stddev", "median", "N/D", "min", "max", "sum", "mode", "area", "group (table only)", "xcentroid", "ycentroid", "xcentermass", "ycentermass", "roix", "roiy", "roiwidth", "roiheight", "major", "minor", "angle", "kurtosis"};
         public int getSize() { return strings.length; }
         public Object getElementAt(int i) { return strings[i]; }
      });
      jScrollPane1.setViewportView(statJList);

      jScrollPane2.setViewportView(imageJList);

      jLabel4.setText("Masses");

      plotButton.setText("Plot");
      plotButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            plotButtonActionPerformed(evt);
         }
      });

      appendCheckBox.setText("Append");

      jButton1.setText("Table");
      jButton1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton1ActionPerformed(evt);
         }
      });

      jLabel5.setText("Planes (eg: 2,4,8-25,45...)");

      currentPlaneCheckBox.setText("Current plane only");

      jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

      histogramjPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

      javax.swing.GroupLayout histogramjPanelLayout = new javax.swing.GroupLayout(histogramjPanel);
      histogramjPanel.setLayout(histogramjPanelLayout);
      histogramjPanelLayout.setHorizontalGroup(
         histogramjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 364, Short.MAX_VALUE)
      );
      histogramjPanelLayout.setVerticalGroup(
         histogramjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 258, Short.MAX_VALUE)
      );

      histogramUpdatejCheckBox.setText("AutoUpdate Histogram");

      profilejButton.setText("Line Profile");
      profilejButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            profilejButtonActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addComponent(jLabel3))
                     .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                     .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel5)
                     .addComponent(currentPlaneCheckBox)
                     .addGroup(layout.createSequentialGroup()
                        .addGap(37, 37, 37)
                        .addComponent(jLabel4))
                     .addGroup(layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(jTextField1))))
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(plotButton, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(appendCheckBox)))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(histogramUpdatejCheckBox)
               .addComponent(histogramjPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addComponent(profilejButton, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 377, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addGap(22, 22, 22)
                  .addComponent(histogramjPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(histogramUpdatejCheckBox)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(profilejButton))
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(jLabel4)
                     .addComponent(jLabel3))
                  .addGap(10, 10, 10)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(currentPlaneCheckBox))
                     .addComponent(jScrollPane1))
                  .addGap(20, 20, 20)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(plotButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                     .addGroup(layout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(appendCheckBox)))))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

    private void initComponentsCustom() {
        jLabel3.setText("Statistics");
        imageJList.setModel(new DefaultListModel());
        imageJList.setCellRenderer(new ImageListRenderer());

        // Remove components (jspinners) from the area
        // in which a user can drag and drop a file.
        Component[] comps = {jTextField1};
        for (Component comp : comps) {
            ui.removeComponentFromMimsDrop(comp);
        }
    }

    /**
     * Sets up the histogram that charts pixel values for images.
     */
    private void setupHistogram() {
        // Create arbitrary dataset
        HistogramDataset dataset = new HistogramDataset();

        // Create chart using the ChartFactory
        chart = MimsChartFactory.createMimsHistogram("", "Pixel Value", "", dataset, PlotOrientation.VERTICAL, true, true, false);
        chart.setBackgroundPaint(this.getBackground());

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
        chartPanel.setSize(350, 250);
        histogramjPanel.add(chartPanel);
    }

    /**
     * Updates the histogram to reflect the data contained within <code>pixelvalues</code>.
     *
     * @param pixelvalues the pixel values to be histogramed.
     * @param label the histogram title, usually the name of the image.
     * @param forceupdate forces the update to occur.
     */
    public void updateHistogram(double[] pixelvalues, String label, boolean forceupdate) {
        if (pixelvalues == null) {
            return;
        } else if (pixelvalues.length == 0) {
            return;
        }
        if (forceupdate || histogramUpdatejCheckBox.isSelected()) {
            HistogramDataset dataset = new HistogramDataset();

            dataset.addSeries(label, pixelvalues, 100);

            MimsXYPlot plot = (MimsXYPlot) chart.getPlot();
            plot.setDataset(dataset);

            chart.fireChartChanged();
        }
    }

    /**
     * The action method for the "Plot" button. Generates a plot containing ROI statistical information.
     */
    private void plotButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotButtonActionPerformed

        MimsJFreeChart tomoChart = null;
        // Initialize chart.
        if (!appendCheckBox.isSelected() || tomoChart == null) {
            tomoChart = new MimsJFreeChart(ui);
        }

        // Determine planes.
        ArrayList planes = getPlanes();
        if (planes.size() >= 1) {
            tomoChart.setPlanes(planes);
        } else {
            System.out.println("Undetermined planes");
            return;
        }

        // Get selected stats.
        // Get selected rois.
        MimsRoiManager2 rm = ui.getRoiManager();
        Roi[] rois = rm.getSelectedROIs();
        int numLine = 0;
        if (rois.length == 0) {
            rois = rm.getAllROIsInList();
        }
        if (rois.length >= 1) {
            for (Roi roi : rois) {
                if (roi.getType() == Roi.LINE || roi.getType() == Roi.FREELINE || roi.getType() == roi.POLYLINE) {
                    numLine++;
                }
            }
            if (numLine > 0 && numLine != rois.length) {
                JOptionPane.showMessageDialog(ui,
                        "You cannot mix line and area rois in graphs.",
                        "Plotting options conflict",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            tomoChart.setRois(rois);
        } else {
            IJ.error("No rois selected");
            return;
        }
        boolean mean = false;
        String[] statnames = getStatNames();
        for (String statname : statnames) {
            if (statname == "mean") {
                if (statnames.length > 1 && numLine > 0) {
                    mean = true;
                    JOptionPane.showMessageDialog(ui,
                            "Because you have selected mean while using line ROIs, only mean will be plotted.",
                            "Plotting options conflict",
                            JOptionPane.WARNING_MESSAGE);
                } else if (numLine > 0 && statnames.length == 1) {
                    mean = true;
                }
            }
        }
        if (statnames.length >= 1) {
            tomoChart.setStats(statnames);
        } else {
            System.out.println("No stats selected");
            return;
        }
        // images
        MimsPlus[] images = getImages();
        if (images.length >= 1) {
            MimsPlus[] imagesToSend = new MimsPlus[images.length];
            for (int i = 0; i < images.length; i++) {
                imagesToSend[i] = images[i];
            }
            tomoChart.setImages(imagesToSend);
        } else {
            System.out.println("No images selected");
            return;
        }

        int currentPlane = ui.getMassImage(0).getSlice();
        tomoChart.plotData(appendCheckBox.isSelected(), mean);
        tomoChart.setTitle(ui.getImageFilePrefix());
        // Fast forward stack by one slice if we are appending current plane.
        if ((currentPlaneCheckBox.isSelected()) && ((currentPlane + 1) <= ui.getMassImage(0).getStackSize())) {
            ui.getMassImage(0).setSlice(currentPlane + 1);
        }
    }//GEN-LAST:event_plotButtonActionPerformed

    /**
     * Action method for the "Table" button. Generates a table containing statistical information. The format of the
     * table depends on the type of image being generated... Sum images will contain 1 row per ROI whereas images with
     * depth will contain 1 row per plane. If it is a single plane image than the output will be similar to that of a
     * Sum image.
     */
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        // DJ: 11/21/2014 : moved all the implementation that was in here to generateTable method
        // since we need to call generateTable method from else where as well.
        generateTable();
    }//GEN-LAST:event_jButton1ActionPerformed

    public void generateTable() {
        // initialize variables.
        MimsRoiManager2 rm = ui.getRoiManager();

        if (!appendCheckBox.isSelected() || table == null) {
            table = new MimsJTable(ui);
        }

        // Determine planes.
        ArrayList planes = getPlanes();
        if (planes.size() >= 1) {
            table.setPlanes(planes);
        } else {
            System.out.println("Undetermined planes");
            return;
        }

        // Get selected stats.
        String[] statnames = getStatNames();
        if (statnames.length >= 1) {
            table.setStats(statnames);
        } else {
            System.out.println("No stats selected");
            return;
        }

        // Get selected rois.
        Roi[] rois = rm.getSelectedROIs();
        if (rois.length == 0) {
            rois = rm.getAllROIsInList();
        }
        if (rois.length >= 1) {
            table.setRois(rois);
        } else {
            System.out.println("No rois selected");
            return;
        }

        // Get selected images.
        MimsPlus[] images = getImages();
        if (images.length >= 1) {
            MimsPlus[] imagesToSend = new MimsPlus[images.length];
            //int[] originalMimsTypesToSend = new int[images.length]; //DJ: 11/14/2014
            for (int i = 0; i < images.length; i++) {
                //originalMimsTypesToSend[i] = images[i].getMimsType();  //DJ: 11/14/2014
                if (images[i].getMimsType() == MimsPlus.HSI_IMAGE || images[i].getMimsType() == MimsPlus.RATIO_IMAGE) {
                    imagesToSend[i] = images[i].internalRatio;
                } else {
                    imagesToSend[i] = images[i];
                }
            }
            table.setImages(imagesToSend);
            //table.setOriginalImagesTypes(originalMimsTypesToSend); // DJ
        } else {
            System.out.println("No images selected");
            return;
        }

        // Decide if we are going to make a depth table or sum style table.
        boolean createDepthTable = false;
        int numplanes = ui.getOpenMassImages()[0].getStackSize();
        boolean sumRadioButtonChecked = ui.getHSIView().isUseSumSelected();
        for (MimsPlus mp : images) {
            int m_type = mp.getMimsType();
            // Mass images
            if (m_type == MimsPlus.MASS_IMAGE) {
                if (numplanes > 1) {
                    createDepthTable = true;
                    break;
                }
            }
            // Ratio and HSI images.
            if (m_type == MimsPlus.RATIO_IMAGE || m_type == MimsPlus.HSI_IMAGE) {
                if (numplanes > 1 && sumRadioButtonChecked == false) {
                    createDepthTable = true;
                    break;
                }
            }
        }

        // Generate Sum table.
        if (createDepthTable) {
            table.createTable(appendCheckBox.isSelected());
        } else {
            table.createSumTable(appendCheckBox.isSelected());
        }
        table.showFrame();
    }

    /**
     * Action method for the "Line Profile" button. generates a line plot representing pixel values along a line ROI.
     */
    private void profilejButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_profilejButtonActionPerformed
        if (ui.lineProfile == null) {
            ui.lineProfile = new MimsLineProfile(ui);
            double[] foo = new double[100];
            for (int i = 0; i < 100; i++) {
                foo[i] = 10;
            }
            ui.updateLineProfile(foo, "line", 1, null);
        } else {
            ui.lineProfile.setVisible(true);
        }
    }//GEN-LAST:event_profilejButtonActionPerformed

    /**
     * Updates the list of images to include all open mass images, ratio images, and sum images.
     */
    public void resetImageNamesList() {

        // Get all the open images we want in the list.
        MimsPlus[] rp = ui.getOpenRatioImages();
        MimsPlus[] mp = ui.getOpenMassImages();
        MimsPlus[] sp = ui.getOpenSumImages();
        MimsPlus[] hp = ui.getOpenHSIImages();

        // Build array of image names.
        java.util.ArrayList<MimsPlus> images = new java.util.ArrayList<MimsPlus>();
        images.addAll(Arrays.asList(mp));
        images.addAll(Arrays.asList(rp));
        images.addAll(Arrays.asList(sp));
        images.addAll(Arrays.asList(hp));

        // Insert into list.
        final MimsPlus[] img = new MimsPlus[images.size()];
        for (int k = 0; k < images.size(); k++) {
            img[k] = images.get(k);
        }

        imageJList.setModel(new javax.swing.AbstractListModel() {

            public int getSize() {
                return img.length;
            }

            public Object getElementAt(int i) {
                return img[i];
            }
        });
    }

    /**
     * Gets the table created by MimTomography
     *
     * @return current table (or null if none)
     */
    public MimsJTable getTable() {
        return table;
    }

    /**
     * Gets the images selected by the user in the jlist.
     *
     * @return an array of MimsPlus images.
     */
    private MimsPlus[] getImages() {

        // Get selected images.
        int[] num = imageJList.getSelectedIndices();

        // Build list of images.
        MimsPlus[] images = new MimsPlus[num.length];
        for (int i = 0; i < num.length; i++) {
            images[i] = (MimsPlus) imageJList.getModel().getElementAt(num[i]);
        }

        return images;
    }

    //DJ: 09/26/2014
    // "82/80", "13/12", ...
    public ArrayList<String> getSelectedHSIImages() {

        // Get selected images.
        int[] num = imageJList.getSelectedIndices();
        ArrayList<String> selectedHSIs = new ArrayList<String>();

        for (int i = 0; i < num.length; i++) {
            MimsPlus mp = (MimsPlus) imageJList.getModel().getElementAt(num[i]);
            if (mp.getMimsType() == MimsPlus.HSI_IMAGE) {
                String title = mp.getRoundedTitle();
                // title = title.substring(title.indexOf(" ") + 1);
                selectedHSIs.add(title);
            }
        }

        return selectedHSIs;
    }

    /**
     * Sets the statistics to be highlighted.
     *
     * @return the int array of indices selected in the statistics list.
     */
    public int[] getSelectedStatIndices() {
        int[] indices = statJList.getSelectedIndices();
        return indices;
    }

    /**
     * Sets the statistics to be highlighted.
     *
     * @param indices the indices to be selected.
     */
    public void setSelectedStatIndices(int[] indices) {
        statJList.setSelectedIndices(indices);
    }

    //DJ
    public void setSelectedImagesIndices(int[] indices) {
        imageJList.setSelectedIndices(indices);
    }

    /**
     * Gets the statistics selected by the user in the jlist.
     *
     * @return an array of statistic names.
     */
    public String[] getStatNames() {

        // initialize array and get selected statistics.
        // Object[] objs = new Object[statJList.getSelectedValues().length];
        // objs = statJList.getSelectedValues();
        List<String> selectednames = statJList.getSelectedValuesList();

        // If no statistics selected, use Area, Mean, and StdDev by default.
        String[] statnames;
        //if (objs.length == 0) {
        if (selectednames.size() == 0) {
            statnames = new String[3];
            statnames[0] = "area";
            statnames[1] = "mean";
            statnames[2] = "stddev";
        } else {
            statnames = new String[selectednames.size()];
            for (int i = 0; i < selectednames.size(); i++) {
                statnames[i] = selectednames.get(i);
            }
        }

        return statnames;
    }

    // DJ: 08/13/2014
    // Mainly created to be used in Jython scripting
    /**
     * Gets all the Stats Names
     *
     * @return array of strings of all stats names.
     */
    public String[] getAllStatNames() {

        int numOfComponents = statJList.getModel().getSize();

        String[] statNames = new String[numOfComponents];

        for (int i = 0; i < numOfComponents; i++) {
            statNames[i] = (String) (statJList.getModel().getElementAt(i));
        }

        return statNames;
    }

    /**
     * Gets the planes entered by the user in the textfield.
     *
     * @return an array list of plane numbers (all by default).
     */
    public ArrayList<Integer> getPlanes() {

        // initialize
        ArrayList planes = new ArrayList<Integer>();

        // Get text.
        String list = jTextField1.getText().trim();

        // Parse text, generat list.
        if (currentPlaneCheckBox.isSelected()) {
            planes.add(ui.getOpenMassImages()[0].getCurrentSlice());
        } else if (list.matches("") || list.length() == 0) {
            for (int i = 1; i <= ui.getOpenMassImages()[0].getNSlices(); i++) {
                planes.add(i);
            }
        } else {
            planes = MimsStackEditor.parseList(jTextField1.getText(), 1, ui.getOpenMassImages()[0].getNSlices());
        }

        return planes;
    }

    private boolean thisIsVisible() {
        return this.isVisible();
    }

    /**
     * The imageListRenderer class is controls how entries in the "images" jlist are rendered. A Mimsplus object
     * occupies the field but this class gets its very mass number and displays that string.
     */
    class ImageListRenderer extends JLabel implements ListCellRenderer {

        public ImageListRenderer() {
            setOpaque(true);
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(CENTER);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            // Extract simple title from value.
            MimsPlus image = (MimsPlus) value;
            String label = image.getRoundedTitle();
            setText(label);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JCheckBox appendCheckBox;
   private javax.swing.JCheckBox currentPlaneCheckBox;
   private javax.swing.JCheckBox histogramUpdatejCheckBox;
   private javax.swing.JPanel histogramjPanel;
   private javax.swing.JList imageJList;
   private javax.swing.JButton jButton1;
   private javax.swing.JLabel jLabel3;
   private javax.swing.JLabel jLabel4;
   private javax.swing.JLabel jLabel5;
   private javax.swing.JScrollPane jScrollPane1;
   private javax.swing.JScrollPane jScrollPane2;
   private javax.swing.JSeparator jSeparator1;
   private javax.swing.JTextField jTextField1;
   private javax.swing.JButton plotButton;
   private javax.swing.JButton profilejButton;
   private javax.swing.JList statJList;
   // End of variables declaration//GEN-END:variables
}

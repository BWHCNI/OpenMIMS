package com.nrims;

import com.nrims.plot.MimsChartFactory;
import com.nrims.plot.MimsChartPanel;
import com.nrims.plot.MimsXYPlot;
import ij.IJ;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import java.awt.Color;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Polygon;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.util.ResourceBundleWrapper;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ExtensionFileFilter;

/**
 * The MimsLineProfile class creates a line plot for line ROIs. The y-axis represents pixel value (of the current image)
 * and x-axis represents length along the line.
 *
 * @author cpoczatek
 */
public class MimsLineProfile extends JFrame {

    private JFreeChart chart;
    private JLabel coords;
    private MimsChartPanel chartPanel;
    private int linewidth = 1;
    private UI ui;
    private double curX;
    private String name;
    private MimsPlus image;
    private int pointX = -1;
    private int pointY = -1;

    public MimsLineProfile(final UI ui) {
        super("Dynamic Line Profile");
        this.setDefaultCloseOperation(this.DISPOSE_ON_CLOSE);
        this.ui = ui;
        image = null;
        XYDataset dataset = createDataset();
        chart = createChart(dataset);
        curX = 0;
        ((MimsXYPlot) chart.getPlot()).setLineProfile(this);
        chartPanel = new MimsChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        coords = new JLabel("", SwingConstants.LEFT);
        chartPanel.add(coords);

        JPopupMenu menu = chartPanel.getPopupMenu();
        JMenuItem menuItem = new javax.swing.JMenuItem("Display text");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displayProfileData();
            }
        });

        menu.add(menuItem, 2);
        setContentPane(chartPanel);

        // Add menu item for showing/hiding crosshairs.
        JMenuItem xhairs = new JMenuItem("Show/Hide Crosshairs");
        xhairs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showHideCrossHairs(chartPanel);
            }
        });
        chartPanel.getPopupMenu().addSeparator();
        chartPanel.getPopupMenu().add(xhairs);
        JMenuItem pointhairs = new JMenuItem("Add point roi at crosshairs");
        pointhairs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (pointX > 0 && pointY > 0) {
                    ui.getRoiManager().add(new PointRoi(pointX, pointY));
                    ui.updateAllImages();
                }
            }
        });
        chartPanel.getPopupMenu().add(pointhairs);
        // Replace Save As... menu item.
        chartPanel.getPopupMenu().remove(3);
        JMenuItem saveas = new JMenuItem("Save as...");
        saveas.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveAs();
            }
        });
        chartPanel.getPopupMenu().add(saveas, 3);

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(new KeyEventDispatcher() {
                    public boolean dispatchKeyEvent(KeyEvent e) {
                        if (e.getID() == KeyEvent.KEY_PRESSED && thisHasFocus()) {
                            chartPanel.keyPressed(e);
                        }
                        return false;
                    }
                });

        this.pack();
        this.setVisible(true);
    }

    /**
     * Add current corresponding crosshair point to all image overlays
     *
     * @return coordinates of corresponding image
     */
    public int[] addToOverlay() {
        if (image != null) {
            MimsXYPlot plot = (MimsXYPlot) chart.getPlot();
            System.out.println(plot.getDomainCrosshairValue());
            if (plot.isDomainCrosshairVisible() && curX != plot.getDomainCrosshairValue() && name != null) {
                curX = plot.getDomainCrosshairValue();
                Roi roi = ui.getRoiManager().getRoiByName(name);
                pointX = -1;
                pointY = -1;
                if (roi != null && roi.isLine()) {
                    if (roi.getType() == Roi.LINE) {
                        Line line = (Line) roi;
                        double ratio = plot.getDomainCrosshairValue() / line.getLength();
                        Polygon points = line.getPoints();
                        int[] xpoints = points.xpoints;
                        int[] ypoints = points.ypoints;
                        double xvec = (xpoints[0] - xpoints[1]) * ratio;
                        double yvec = (ypoints[0] - ypoints[1]) * ratio;
                        pointX = (int) (xpoints[0] - xvec);
                        pointY = (int) (ypoints[0] - yvec);
                    } else if (roi.getType() == Roi.FREELINE || roi.getType() == Roi.POLYLINE) {
                        Polygon points = roi.getPolygon();
                        int[] xpoints = points.xpoints;
                        int[] ypoints = points.ypoints;
                        double distanceTraveled = 0;
                        pointX = 0;
                        pointY = 0;
                        for (int i = 0; i < xpoints.length - 1; i++) {
                            double distance = Math.pow((Math.pow((double) (xpoints[i] - xpoints[i + 1]), 2) + Math.pow((double) (ypoints[i] - ypoints[i + 1]), 2)), 0.5);
                            if (distanceTraveled + distance > curX) {
                                double needToTravel = curX - distanceTraveled;
                                double ratio = needToTravel / distance;
                                double xvec = (xpoints[i] - xpoints[i + 1]) * ratio;
                                double yvec = (ypoints[i] - ypoints[i + 1]) * ratio;
                                pointX = (int) (xpoints[i] - xvec);
                                pointY = (int) (ypoints[i] - yvec);
                                i = xpoints.length;
                            } else {
                                distanceTraveled += distance;
                            }
                        }
                    }
                    int[] coords = {pointX, pointY};
                    Ellipse2D shape = new Ellipse2D.Float(pointX - 2, pointY - 2, 4, 4);
                    Roi shaperoi = new ShapeRoi(shape);
                    shaperoi.setName(name);
                    MimsPlus[] openImages = ui.getAllOpenImages();
                    //for (MimsPlus image : images) {
                    for (MimsPlus image : openImages) {
                        Overlay overlay = image.getGraphOverlay();
                        int indexm = overlay.getIndex(roi.getName());
                        if (indexm > -1) {
                            overlay.remove(indexm);
                        }
                        overlay.add(shaperoi);
                        overlay.setFillColor(java.awt.Color.yellow);
                        image.setOverlay(overlay);
                    }
                    return coords;
                }
            }
        }
        return null;
    }

    public void removeOverlay() {
        if (image != null && name != null) {
            MimsPlus[] openImages = ui.getAllOpenImages();
            //for (MimsPlus image : images) {
            for (MimsPlus image : openImages) {
                Overlay overlay = image.getGraphOverlay();

                int indexm = overlay.getIndex(name);
                if (indexm > -1) {
                    overlay.remove(indexm);
                }
                image.setOverlay(overlay);
            }
        }
    }

    public void showHideCrossHairs(MimsChartPanel chartpanel) {
        Plot plot = chartpanel.getChart().getPlot();
        if (!(plot instanceof MimsXYPlot)) {
            return;
        }

        // Show/Hide XHairs
        MimsXYPlot xyplot = (MimsXYPlot) plot;
        xyplot.setDomainCrosshairVisible(!xyplot.isDomainCrosshairVisible());
        xyplot.setRangeCrosshairVisible(!xyplot.isRangeCrosshairVisible());
        if (!xyplot.isDomainCrosshairVisible()) {
            removeOverlay();
        }
        xyplot.showXHairLabel(xyplot.isDomainCrosshairVisible() || xyplot.isDomainCrosshairVisible());
    }

    /**
     * Creates a sample dataset.
     *
     * @return a sample dataset.
     */
    private XYDataset createDataset() {

        XYSeries series1 = new XYSeries("N");
        series1.add(1.0, 2.0);
        series1.add(2.0, 2.0);
        series1.add(3.0, 2.0);
        series1.add(4.0, 2.0);
        series1.add(5.0, 2.0);
        series1.add(6.0, 2.0);
        series1.add(7.0, 2.0);
        series1.add(8.0, 2.0);

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);

        return dataset;

    }

    /**
     * Creates a chart.
     *
     * @param dataset the data for the chart.
     *
     * @return a chart.
     */
    private JFreeChart createChart(final XYDataset dataset) {

        // create the chart...
        final JFreeChart chart = MimsChartFactory.createMimsXYLineChart(
                "", // chart title
                "L", // x axis label
                "P", // y axis label
                dataset, // data
                PlotOrientation.VERTICAL,
                true, // include legend
                true, // tooltips
                false // urls
        );

        // get a reference to the plot for further customisation...
        MimsXYPlot plot = (MimsXYPlot) chart.getPlot();

        // Set colors.
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        // Movable range and domain.
        plot.setDomainPannable(true);
        plot.setRangePannable(true);

        // Allow crosshairs to 'focus' in on a given point.
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        // change the auto tick unit selection to integer units only...        
        plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        plot.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        return chart;

    }

    /**
     * Updates the data displayed in the chart.
     *
     * @param newdata the data for the chart.
     * @param name name of ROI.
     * @param width the line width.
     */
    public void updateData(double[] newdata, String name, int width, MimsPlus image) {
        if (newdata == null) {
            return;
        }
        removeOverlay();
        this.name = name.substring(name.lastIndexOf(":") + 2);
        this.image = image;
        linewidth = width;
        XYSeries series = new XYSeries(name + " w: " + width);
        for (int i = 0; i < newdata.length; i++) {
            series.add(i, newdata[i]);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        MimsXYPlot plot = (MimsXYPlot) chart.getPlot();
        plot.setDataset(dataset);

        chart.fireChartChanged();
    }

    /**
     * Creates a table displaying the data contained in the plot.
     */
    private void displayProfileData() {
        MimsXYPlot plot = (MimsXYPlot) chart.getPlot();
        XYDataset data = plot.getDataset();

        ij.measure.ResultsTable table = new ij.measure.ResultsTable();
        table.setHeading(1, "Position");   // Deprecated. Replaced by addValue(String,double) and setValue(String,int,double)
        //table.addValue("1", "Posiiton");
        table.setHeading(2, plot.getLegendItems().get(0).getLabel() + " : width " + linewidth);
        //table.incrementCounter();

        //end of table bug?
        for (int i = 0; i < data.getItemCount(0); i++) {
            table.incrementCounter();
            table.addValue(1, data.getXValue(0, i));
            table.addValue(2, data.getYValue(0, i));

        }

        table.show("");
    }

    private boolean thisHasFocus() {
        return this.hasFocus();
    }

    /**
     * Saveas the chart as a .png
     */
    public void saveAs() {
        MimsJFileChooser fileChooser = new MimsJFileChooser(ui);
        fileChooser.setSelectedFile(new File(ui.getLastFolder(), ui.getImageFilePrefix() + ".png"));
        ResourceBundle localizationResources = ResourceBundleWrapper.getBundle("org.jfree.chart.LocalizationBundle");
        ExtensionFileFilter filter = new ExtensionFileFilter(
                localizationResources.getString("PNG_Image_Files"), ".png");
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        int option = fileChooser.showSaveDialog(chartPanel);
        if (option == MimsJFileChooser.APPROVE_OPTION) {
            String filename = fileChooser.getSelectedFile().getPath();
            if (!filename.endsWith(".png")) {
                filename = filename + ".png";
            }
            try {
                ChartUtilities.saveChartAsPNG(new File(filename), chartPanel.getChart(), getWidth(), getHeight());
            } catch (IOException ioe) {
                IJ.error("Unable to save file.\n\n" + ioe.toString());
            }
        }
    }

    public void windowClosing(WindowEvent e) {
        removeOverlay();
    }
}

package com.nrims;

import ij.gui.*;
import ij.process.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.ui.NumberCellRenderer;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JFrame;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.entity.XYItemEntity;

public class MimsJFreeChart implements ChartMouseListener {

   String[] stats;
   MimsPlus images[];
   Roi[] rois;
   ArrayList planes;
   MimsJFreeChart jchart;
   Object e;
   private com.nrims.UI ui;
   private chartFrame chartframe;
   private mimsPanel chartpanel;
   private int currentSeriesInt = -1;
   private String currentSeriesName = null;
   private ArrayList<Integer> hiddenSeries = new ArrayList<Integer>();

   public MimsJFreeChart(UI ui) {
      this.ui = ui;
   }

   public MimsJFreeChart(MimsJFreeChart jchart) {
      this.jchart = jchart;
   }

   public void createFrame(boolean appendResults) {

      // Cant append if frame doesnt exist
      if (chartframe == null) {
         appendResults = false;
      } else if (!chartframe.isVisible()) {
         appendResults = false;
      }

      // if appending
      if (appendResults && chartframe != null) {
         XYDataset tempdata = getDataset();
         int numSeries = chartpanel.getChart().getXYPlot().getDatasetCount();

         XYSeriesCollection originaldata = (XYSeriesCollection) chartpanel.getChart().getXYPlot().getDataset();
         XYSeriesCollection newdata = (XYSeriesCollection) getDataset();

         addToDataSet(newdata, originaldata);
         tempdata = missingSeries(newdata, originaldata);

         XYSeriesCollection foo = (XYSeriesCollection) tempdata;
         int n = foo.getSeriesCount();
         for (int i = 0; i < n; i++) {
            originaldata.addSeries(foo.getSeries(i));
         }

         nullMissingPoints((XYSeriesCollection) chartpanel.getChart().getXYPlot().getDataset());

      } else if (rois.length == 0 || images.length == 0 || stats.length == 0) {
         return;
      } else {
         chartframe = new chartFrame("Plot");
         XYDataset tempdata = getDataset();
         chartframe.addData(tempdata);
         chartframe.pack();
         RefineryUtilities.centerFrameOnScreen(chartframe);
         chartframe.setVisible(true);
         chartpanel.addChartMouseListener(this);

         KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(new KeyEventDispatcher() {
                public boolean dispatchKeyEvent(KeyEvent e) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        chartpanel.keyPressed(e);
                   }
                    return false;
                }
            });


      }
   }

   // Add any series with a new key in newdata to olddata.
   public boolean addToDataSet(XYSeriesCollection newdata, XYSeriesCollection olddata) {

      boolean hasnewseries = false;
      // Loop over newdata.
      for (int nindex = 0; nindex < newdata.getSeriesCount(); nindex++) {
         XYSeries newseries = newdata.getSeries(nindex);
         String newname = (String) newseries.getKey();
         // Check if olddata has series with same key.
         XYSeries oldseries = null;
         try {
            oldseries = olddata.getSeries(newname);
         } catch (org.jfree.data.UnknownKeyException e) {
            hasnewseries = true;
            continue;
         }
         if (oldseries != null) {

            for (int n = 0; n < newseries.getItemCount(); n++) {
               // Remove possible {x,null} pairs.
               double xval = (Double) newseries.getX(n);
               int pos = oldseries.indexOf(xval);
               if ((pos > -1) && (oldseries.getY(pos) == null)) {
                  oldseries.remove(pos);
               }
               oldseries.add(newseries.getDataItem(n));
            }
         }
      }
      return hasnewseries;
   }

   // Return any series from newdata with a key missing from olddata.
   public XYSeriesCollection missingSeries(XYSeriesCollection newdata, XYSeriesCollection olddata) {
      XYSeriesCollection returncollection = new XYSeriesCollection();

      // Loop over newdata.
      for (int nindex = 0; nindex < newdata.getSeriesCount(); nindex++) {
         XYSeries newseries = newdata.getSeries(nindex);
         String newname = (String) newseries.getKey();
         // Check if olddata has series with same key.
         XYSeries oldseries = null;
         try {
            oldseries = olddata.getSeries(newname);
         } catch (org.jfree.data.UnknownKeyException e) {
            returncollection.addSeries(newseries);
            continue;
         }
      }

      return returncollection;
   }

   // Adds the pair {x, null} to any series in data that is missing {x, y}.
   // Changes contents of data.
   public void nullMissingPoints(XYSeriesCollection data) {
      for (int nindex = 0; nindex < data.getSeriesCount(); nindex++) {
         XYSeries series = data.getSeries(nindex);
         double min = series.getMinX();
         double max = series.getMaxX();
         double span = (max - min) + 1;
         for (int xindex = (int) min; xindex <= (int) max; xindex++) {
            double xval = (double) xindex;
            int pos = series.indexOf(xval);
            if (pos < 0) {
               series.add(xval, null);
            }
         }
      }
   }

   // This method will generate a set of plots for a given set of: rois, stats, images.
   public XYDataset getDataset() {

      // Initialize some variables
      XYSeriesCollection dataset = new XYSeriesCollection();
      XYSeries series[][][] = new XYSeries[rois.length][images.length][stats.length];
      String seriesname[][][] = new String[rois.length][images.length][stats.length];
      ImageStatistics tempstats = null;
      int currentSlice = ui.getOpenMassImages()[0].getCurrentSlice();

      // Image loop
      for (int j = 0; j < images.length; j++) {
         MimsPlus image = images[j];

         // Plane loop
         for (int ii = 0; ii < planes.size(); ii++) {
            int plane = (Integer) planes.get(ii);
            if (image.getMimsType() == MimsPlus.MASS_IMAGE) {
               ui.getOpenMassImages()[0].setSlice(plane, false);
            } else if (image.getMimsType() == MimsPlus.RATIO_IMAGE) {
               ui.getOpenMassImages()[0].setSlice(plane, image);
            }

            // Roi loop
            for (int i = 0; i < rois.length; i++) {

               // Stat loop
               for (int k = 0; k < stats.length; k++) {

                  // Generate a name for the dataset.
                  if (seriesname[i][j][k] == null) {
                     seriesname[i][j][k] = image.getShortTitle();
                     seriesname[i][j][k] = seriesname[i][j][k] + " " + stats[k] + " \n" + "r" + (ui.getRoiManager().getIndex(rois[i].getName()) + 1);
                  }

                  // Add data to the series.
                  if (series[i][j][k] == null) {
                     series[i][j][k] = new XYSeries(seriesname[i][j][k]);
                  }

                  // Get the stats.
                  Integer[] xy = ui.getRoiManager().getRoiLocation(rois[i].getName(), plane);
                  rois[i].setLocation(xy[0], xy[1]);
                  image.setRoi(rois[i]);
                  tempstats = image.getStatistics();
                  series[i][j][k].add(((Integer) planes.get(ii)).intValue(), getSingleStat(tempstats, stats[k]));

               } // End of Stat
            } // End of Roi
         } // End of Plane
      } // End of Image

      // Populate the final data structure.
      for (int i = 0; i < rois.length; i++) {
         for (int j = 0; j < images.length; j++) {
            for (int k = 0; k < stats.length; k++) {
               dataset.addSeries(series[i][j][k]);
            }
         }
      }

      ui.getOpenMassImages()[0].setSlice(currentSlice);

      return dataset;
   }

   // Listens for mouse click events.
   public void chartMouseClicked(ChartMouseEvent event) {

      ChartEntity e = event.getEntity();
      if (e != null) {
         this.e = e;

         // Spanwn a new thread to handle graphical events.
         Runnable runnable = new BasicThread2();        
         Thread thread = new Thread(runnable);        
         thread.start();
      }
   }

   // Keep the currentSeriesInt and the currentSeriesName up to date.
   public void chartMouseMoved(ChartMouseEvent event) {

      ChartEntity entity = event.getEntity();
      String seriesName = null;
      int seriesInt = getSeriesInt(entity);

         if (seriesInt > -1) {
            currentSeriesInt = seriesInt;
            seriesName = getSeriesName(entity);
            if (seriesName != null)
               currentSeriesName = seriesName;
         } else {
            currentSeriesInt = -1;
            currentSeriesName = null;
         }

      }

   // Get the series index associcated with the chartEntity.
   public int getSeriesInt(ChartEntity chartEntity) {
      int seriesKey = -1;
      if (chartEntity instanceof LegendItemEntity) {
         LegendItemEntity entity = (LegendItemEntity) chartEntity;
         Comparable seriesName = entity.getSeriesKey();

         // Get the plot.
         XYPlot plot = (XYPlot) chartpanel.getChart().getPlot();
         XYDataset dataset = plot.getDataset();

         // Loop over all series, find matching one.
         for (int i = 0; i < dataset.getSeriesCount(); i++) {
            if (dataset.getSeriesKey(i) == seriesName) {
               seriesKey = i;
               break;
            }
         }
      }  else if (chartEntity instanceof XYItemEntity) {
         XYItemEntity entity = (XYItemEntity) chartEntity;
         seriesKey = entity.getSeriesIndex();
      }
      return seriesKey;
   }

   // Get the series name associcated with the chartEntity.
   public String getSeriesName(ChartEntity chartEntity) {
      String seriesName = null;
      if (chartEntity instanceof LegendItemEntity) {
         LegendItemEntity entity = (LegendItemEntity) chartEntity;
         seriesName = (String)entity.getSeriesKey();
      }  else if (chartEntity instanceof XYItemEntity) {
         XYItemEntity entity = (XYItemEntity) chartEntity;
         XYPlot plot = (XYPlot) chartpanel.getChart().getPlot();
         XYDataset dataset = plot.getDataset();
         seriesName = (String)dataset.getSeriesKey(entity.getSeriesIndex());
      }
      return seriesName;
   }

   // Handle graphical events spawned from mouse clicks of the plots.
   class BasicThread2 implements Runnable {

      // This method is called when the thread runs.
      public void run() {
         Comparable seriesKey;
         if (e instanceof LegendItemEntity) {
            LegendItemEntity entity = (LegendItemEntity) e;
            seriesKey = entity.getSeriesKey();
            highlightSeries(seriesKey);
         } 
      }

      // Highlight the line that corresponds to the legend item clicked.
      public void highlightSeries(Comparable seriesKey) {

         // Get the plot.
         XYPlot plot = (XYPlot) chartpanel.getChart().getPlot();
         XYDataset dataset = plot.getDataset();

         // Get the renderer.
         XYItemRenderer renderer = plot.getRenderer();

         // Loop over all series, highlighting the one that matches seriesKey.
         for (int i = 0; i < dataset.getSeriesCount(); i++) {

            // Set the size of the big and small stroke.
            BasicStroke wideStroke = new BasicStroke(3.0f);
            BasicStroke thinStroke = new BasicStroke(1.0f);
            
            if (dataset.getSeriesKey(i).equals(seriesKey)) {
               try {
                  renderer.setSeriesStroke(i, wideStroke);
                  Thread.sleep(300);
                  renderer.setSeriesStroke(i, thinStroke);
                  Thread.sleep(300);
                  renderer.setSeriesStroke(i, wideStroke);
                  Thread.sleep(300);
                  renderer.setSeriesStroke(i, thinStroke);
               } catch (InterruptedException ex) {
                  Logger.getLogger(MimsJFreeChart.class.getName()).log(Level.SEVERE, null, ex);
               }
               break;
            }
         }
      }
   }

   
   static class BasicTableModel extends AbstractTableModel implements TableModel {

      private Object[][] data;

      // Creates a new demo table model.
      public BasicTableModel(int rows) {
         this.data = new Object[rows][3];
      }

      // Returns the number of columns.
      public int getColumnCount() {
         return 3;
      }

      // Returns the row count.
      public int getRowCount() {
         return 1;
      }

      // Returns the value at the specified cell in the table.
      public Object getValueAt(int row, int column) {
         return this.data[row][column];
      }

      // Sets the value at the specified cell.         
      public void setValueAt(Object value, int row, int column) {
         this.data[row][column] = value;
         fireTableDataChanged();
      }

      // Returns the column name.
      public String getColumnName(int column) {
         switch (column) {
            case 0:
               return "Series:";
            case 1:
               return "X:";
            case 2:
               return "Y:";
         }
         return null;
      }
   }

   public class mimsPanel extends ChartPanel implements ChartProgressListener {

      JFreeChart chart;
      int colorIdx = 0;
      Color[] colorList = {Color.BLACK, Color.BLUE, Color.CYAN, Color.GRAY, Color.GREEN,
                           Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.WHITE,
                           Color.YELLOW};

      public mimsPanel(JFreeChart chart) {
         super(chart);
         this.chart = chart;
      }

      @Override
      public void chartProgress(ChartProgressEvent event) {
         super.chartProgress(event);
         if (chartpanel != null) {
            JFreeChart chart = chartpanel.getChart();
            if (chart != null) {
               XYPlot plot = (XYPlot) chart.getPlot();
               double x = plot.getDomainCrosshairValue();
               double y = plot.getRangeCrosshairValue();

               if (chartframe != null) {
                  String name = findMatchingSeriesName(x, y);
                  chartframe.getTableModel().setValueAt(name, 0, 0);
                  chartframe.getTableModel().setValueAt(x, 0, 1);
                  chartframe.getTableModel().setValueAt(y, 0, 2);
               }
            }
         }
      }

      public String findMatchingSeriesName(double x, double y) {
         String name = "";
         XYPlot plot = (XYPlot) chartpanel.getChart().getPlot();
         XYDataset dataset = plot.getDataset();
         int n = dataset.getSeriesCount();
         int xint = (int) java.lang.Math.round(x);
         xint = xint - 1;
         if (xint < 0 || xint >= dataset.getItemCount(0)) {
            return "";
         }

         double sx, sy;
         for (int i = 0; i < n; i++) {
            sx = dataset.getX(i, xint).doubleValue();
            if (dataset.getY(i, xint) == null) {
               return "";
            }
            sy = dataset.getY(i, xint).doubleValue();
            if (sy == y) {
               if (i >= plot.getLegendItems().getItemCount())
                  name = "";
               else
                  name = plot.getLegendItems().get(i).getLabel();
            }
         }

         return name;
      }

      public JFreeChart getChart() {
         return this.chart;
      }

   public void keyPressed(KeyEvent e) {

      // Get the plot.
      XYPlot plot = (XYPlot) chartpanel.getChart().getPlot();
      XYDataset dataset = plot.getDataset();

      // Change color.
      if (e.getKeyChar()=='c') {
         
         // Get the series.
         if (currentSeriesInt < 0)
            return;

         // Set the color.
         XYItemRenderer renderer = plot.getRenderer();
         renderer.setSeriesPaint(currentSeriesInt, colorList[colorIdx]);
         colorIdx++;
         if (colorIdx >= colorList.length)
            colorIdx = 0;

      // Hide the plot.
      } else if (e.getKeyChar()=='h') {
            
         // If hiding a series that has crosshairs on it than we must reset crosshair location.
         Object crosshairSeries = chartframe.model.getValueAt(0,0);
         if (crosshairSeries != null && currentSeriesName != null) {
            String crosshairSeriesName = crosshairSeries.toString();
            if (crosshairSeriesName.matches(currentSeriesName)) {
               plot.setDomainCrosshairValue(0.0);
               plot.setRangeCrosshairValue(0.0);
            }
         }

         // Get the series and hide.
         if (currentSeriesInt < 0 && hiddenSeries.isEmpty()) {
            return;
         } else if (currentSeriesInt < 0 && !hiddenSeries.isEmpty()) {
            int seriesInt = (Integer) hiddenSeries.get(0);
            hiddenSeries.remove(0);
            XYItemRenderer renderer = plot.getRenderer();
            renderer.setSeriesVisible(seriesInt, true);
         } else {
            XYItemRenderer renderer = plot.getRenderer();
            renderer.setSeriesVisible(currentSeriesInt, false);
            hiddenSeries.add(currentSeriesInt);
         }

      // Reset zoom.
      } else if (e.getKeyChar() == 'r') {

         // Auto range.
         plot.getDomainAxis().setAutoRange(true);
         plot.getRangeAxis().setAutoRange(true);
      }
   }

   }
   
   public class chartFrame extends JFrame {

      public chartFrame(String title) {
         super(title);
         this.setDefaultCloseOperation(this.DISPOSE_ON_CLOSE);

         border = BorderFactory.createCompoundBorder(
                 BorderFactory.createEmptyBorder(4, 4, 4, 4),
                 BorderFactory.createEtchedBorder());

         javax.swing.JPanel dashboard = new javax.swing.JPanel();
         dashboard.setPreferredSize(new java.awt.Dimension(400, 60));
         dashboard.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

         model = new BasicTableModel(1);
         model.setValueAt("foo", 0, 0);
         model.setValueAt(new Double("0.00"), 0, 1);
         model.setValueAt(new Double("0.00"), 0, 2);

         javax.swing.JTable table = new javax.swing.JTable(model);

         TableCellRenderer renderer1 = new DefaultTableCellRenderer();
         TableCellRenderer renderer2 = new NumberCellRenderer();
         table.getColumnModel().getColumn(0).setCellRenderer(renderer1);
         table.getColumnModel().getColumn(1).setCellRenderer(renderer2);
         table.getColumnModel().getColumn(2).setCellRenderer(renderer2);
         table.getColumnModel().getColumn(0).setPreferredWidth(250);

         javax.swing.JScrollPane spane = new javax.swing.JScrollPane(table);
         dashboard.add(spane);
         this.add(dashboard, BorderLayout.NORTH);
         this.pack();
      }

      public void addData(XYDataset dataset) {
         JFreeChart chart = createChart(dataset);
         chartpanel = new mimsPanel(chart);
         //chartpanel = new ChartPanel(chart);
         chartpanel.setPreferredSize(new java.awt.Dimension(600, 400));
         String lastFolder = ui.getLastFolder();
         if (lastFolder != null) {
            if (new File(lastFolder).exists()) {
               chartpanel.setDefaultDirectoryForSaveAs(new File(lastFolder));
            }
         }
         chartpanel.setBorder(border);
         this.add(chartpanel);
      }

      public BasicTableModel getTableModel() {
         return model;
      }
      private Border border;
      private BasicTableModel model;
   }

   private static JFreeChart createChart(XYDataset dataset) {
      JFreeChart chart = ChartFactory.createXYLineChart("", "Plane", "", dataset, PlotOrientation.VERTICAL, true, true, false);
      chart.setBackgroundPaint(Color.white);

      // Create integer x-axis.
      XYPlot plot = (XYPlot) chart.getPlot();
      plot.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

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

      return chart;
   }

   public void setImages(MimsPlus[] images) {
      this.images = images;
   }

   public void setStats(String[] stats) {
      this.stats = stats;
   }

   public void setRois(Roi[] rois) {
      this.rois = rois;
   }

   public void setPlanes(ArrayList planes) {
      this.planes = planes;
   }

    public static double getSingleStat(ImageStatistics stats, String statname) {
        double st;

        if(statname.equals("area"))
            return stats.area;
        if(statname.equals("mean"))
            return stats.mean;
        if(statname.equals("stddev"))
            return stats.stdDev;
        if(statname.equals("mode"))
            return stats.mode;
        if(statname.equals("min"))
            return stats.min;
        if(statname.equals("max"))
            return stats.max;
        if(statname.equals("xcentroid"))
            return stats.xCentroid;
        if(statname.equals("ycentroid"))
            return stats.yCentroid;
        if(statname.equals("xcentermass"))
            return stats.xCenterOfMass;
        if(statname.equals("ycentermass"))
            return stats.yCenterOfMass;
        if(statname.equals("roix"))
            return stats.roiX;
        if(statname.equals("roiy"))
            return stats.roiY;
        if(statname.equals("roiwidth"))
            return stats.roiWidth;
        if(statname.equals("roiheight"))
            return stats.roiHeight;
        if(statname.equals("major"))
            return stats.major;
        if(statname.equals("minor"))
            return stats.minor;
        if(statname.equals("angle"))
            return stats.angle;
        if(statname.equals("feret"))
            return stats.FERET;
        if(statname.equals("sum"))
            return (stats.pixelCount*stats.mean);
        if(statname.equals("median"))
            return stats.median;
        if(statname.equals("kurtosis"))
            return stats.kurtosis;
        if(statname.equals("areafraction"))
            return stats.AREA_FRACTION;
        if(statname.equals("perimeter"))
            return stats.PERIMETER;

        return -999;
    }
}

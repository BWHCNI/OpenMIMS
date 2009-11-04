package com.nrims;

import ij.gui.*;
import ij.process.*;

import java.awt.Color;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.JFrame;

public class MimsJFreeChart extends JFrame {

   private String[] stats;
   private MimsPlus images[];
   private Roi[] rois;
   private ArrayList planes;
   private com.nrims.UI ui;
   private ChartPanel chartpanel;

   public MimsJFreeChart(UI ui) {
      super("Plot");
      this.ui = ui;
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
   }

   public void plotData(boolean appendingData) {

      // Add data to existing plaot if appending.
      if (appendingData && chartpanel != null)
         appendData();      
      else {
         
         // Create an chart empty.
         JFreeChart chart = createChart();
         
         // Get the data.
         XYDataset xydata = getDataset();

         // Apply data to the plot
         XYPlot xyplot = (XYPlot)chart.getPlot();
         xyplot.setDataset(xydata);

         // Generate the layout.
         chartpanel = new ChartPanel(chart);
         chartpanel.setPreferredSize(new java.awt.Dimension(600, 400));
         String lastFolder = ui.getLastFolder();
         if (lastFolder != null) {
            if (new File(lastFolder).exists()) {
               chartpanel.setDefaultDirectoryForSaveAs(new File(lastFolder));
            }
         }
         this.add(chartpanel);
         pack();
         setVisible(true);         

         // Add key listener.
         KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent e) {
               if (e.getID() == KeyEvent.KEY_PRESSED) {
                  chartpanel.keyPressed(e);
               }
               return false;
            }
         });
      }
   }

   // Contruct the Frame.
   private static JFreeChart createChart() {
      JFreeChart chart = ChartFactory.createXYLineChart("", "Plane", "", null, PlotOrientation.VERTICAL, true, true, false);
      chart.setBackgroundPaint(Color.white);

      // Get a reference to the plot.
      XYPlot plot = (XYPlot) chart.getPlot();

      // Create integer x-axis.
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

   // Append data to existing plot.
   private void appendData() {
         XYDataset tempdata = getDataset();

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

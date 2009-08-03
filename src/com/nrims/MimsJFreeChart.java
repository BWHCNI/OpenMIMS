/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims;

import ij.*;
import ij.gui.*;
import ij.process.*;

import java.awt.Color;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;



/**
 *
 * @author cpoczatek
 */
public class MimsJFreeChart {

    public MimsJFreeChart(com.nrims.UI ui, com.nrims.data.Opener im) {
        
        this.ui = ui ;
        this.image = im;
        this.images = ui.getMassImages();
        numberMasses = image.getNMasses();
        imagestacks = new ImageStack[numberMasses];
        rp = ui.getOpenRatioImages();
                
        for (int i=0; i<=(numberMasses-1); i++) {
            if(images[i]!=null)
                imagestacks[i]=this.images[i].getStack();
        }
    }
    
    public class chartFrame extends JFrame {
        public chartFrame(String title) {
            super(title);
            this.setDefaultCloseOperation(this.DISPOSE_ON_CLOSE);
        }
        
        public void addData(XYDataset dataset) {
            JFreeChart chart = createChart(dataset);
            //chart.addProgressListener(arg0);
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
            java.io.File dir = new java.io.File(ui.getLastFolder());
            chartPanel.setDefaultDirectoryForSaveAs(dir);
            setContentPane(chartPanel);
        }
    }

    public void creatNewFrame(Roi[] rois, String title, String[] stats, int[] masses, int min, int max) {
        chartFrame tempframe = new chartFrame("");
        XYDataset tempdata = getDataset(rois, title, stats, masses, min, max);
        tempframe.addData(tempdata);
        tempframe.pack();
        RefineryUtilities.centerFrameOnScreen(tempframe);
        tempframe.setVisible(true);
    }
    
    // This method will generate a set of plots for a given set of: rois, stats, images.
    public XYDataset getDataset(Roi[] rois, String title, String[] stats, int[] masses, int min, int max) {

      // Initialize some variables
      XYSeriesCollection dataset = new XYSeriesCollection();
      rp = ui.getOpenRatioImages();
      XYSeries series[][][] = new XYSeries[rois.length][masses.length][stats.length];
      String seriesname[][][] = new String[rois.length][masses.length][stats.length];
      ImageStatistics tempstats = null;

      // begin looping
      for (int i = 0; i < rois.length; i++) {
         for (int ii = min; ii <= max; ii++) {
            images[0].setSlice(ii);
            for (int j = 0; j < masses.length; j++) {
               for (int k = 0; k < stats.length; k++) {

                  // Generate a name for the dataset.  
                  if (seriesname[i][j][k] == null) {
                     if (masses[j] < numberMasses) {
                        images[masses[j]].setRoi(rois[i]);
                        seriesname[i][j][k] = images[masses[j]].getShortTitle();
                        seriesname[i][j][k] = seriesname[i][j][k] + " " + stats[k] + "\n" + rois[i].getName();
                     } else {
                        images[0].setRoi(rois[i]);
                        seriesname[i][j][k] = rp[masses[j] - numberMasses].getShortTitle();
                        seriesname[i][j][k] = seriesname[i][j][k] + " " + stats[k] + "\n" + rois[i].getName();
                     }
                  }

                  // Get the data.
                  if (masses[j] < numberMasses) {                     
                     tempstats = images[masses[j]].getStatistics();
                  } else {                     
                     tempstats = rp[masses[j] - numberMasses].getStatistics();
                  }

                  // Add data to the series.
                  if (series[i][j][k] == null) {
                     series[i][j][k] = new XYSeries(seriesname[i][j][k]);
                  }
                  series[i][j][k].add(ii, getSingleStat(tempstats, stats[k]));
                  
               } // End of Stats
            } // End of Masses
         } // End of Slice
      } // End of Rois

      // Populate the final data structure.
      for (int i = 0; i < rois.length; i++) {
         for (int j = 0; j < masses.length; j++) {
            for (int k = 0; k < stats.length; k++) {
               dataset.addSeries(series[i][j][k]);
            }
         }
      }

      return dataset;
   }
    
        
    
    private static JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart("", "Plane", "", dataset, PlotOrientation.VERTICAL, true, true, false);
        
        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.setBackgroundPaint(Color.white);
        // get a reference to the plot for further customisation...
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        
        //XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        //depricated...
        //renderer.setShapesVisible(true);
        //renderer.setShapesFilled(true);
        
        // change the auto tick unit selection to integer units only...
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        
        
        
        // OPTIONAL CUSTOMISATION COMPLETED.
        
        return chart;
    }
    
    public double getSingleStat(ImageStatistics stats, String statname) {
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
    
    public void chartProgress(ChartProgressEvent event) {
        
    }
    
    private com.nrims.UI ui;
    private com.nrims.data.Opener image;
    private int numberMasses;
    private MimsPlus[] images;
    private ImageStack[] imagestacks;
    private ij.process.ImageStatistics imagestats;
    private MimsPlus[] rp;
    
}

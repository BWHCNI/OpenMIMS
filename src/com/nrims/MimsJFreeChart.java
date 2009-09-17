/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims;

import ij.gui.*;
import ij.process.*;

import java.awt.Color;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;

import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;

import org.jfree.ui.NumberCellRenderer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import javax.swing.table.DefaultTableCellRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;

/**
 * @author cpoczatek
 */
public class MimsJFreeChart {

   String[] stats;
   MimsPlus images[];
   Roi[] rois;
   ArrayList planes;
   private com.nrims.UI ui;
   private chartFrame chartframe;
   private mimsPanel chartpanel;

    public MimsJFreeChart(UI ui) {
       this.ui = ui;
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
            //chart.addProgressListener(arg0);
            chartpanel = new mimsPanel(chart);
            chartpanel.setPreferredSize(new java.awt.Dimension(600, 400));
            String lastFolder = ui.getLastFolder();
            if (lastFolder != null)
               if (new File(lastFolder).exists())
                  chartpanel.setDefaultDirectoryForSaveAs(new File(lastFolder));
            chartpanel.setBorder(border);
            this.add(chartpanel);
        }

        public BasicTableModel getTableModel() {
            return model;
        }

        private Border border;
        private BasicTableModel model;

    }

    static class BasicTableModel extends AbstractTableModel
                                implements TableModel {

        private Object[][] data;

        /**
         * Creates a new demo table model.
         *
         * @param rows  the row count.
         */
        public BasicTableModel(int rows) {
            this.data = new Object[rows][3];
        }

        /**
         * Returns the number of columns.
         *
         * @return 3.
         */
        public int getColumnCount() {
            return 3;
        }

        /**
         * Returns the row count.
         *
         * @return 1.
         */
        public int getRowCount() {
            return 1;
        }

        /**
         * Returns the value at the specified cell in the table.
         *
         * @param row  the row index.
         * @param column  the column index.
         *
         * @return The value.
         */
        public Object getValueAt(int row, int column) {
            return this.data[row][column];
        }

        /**
         * Sets the value at the specified cell.
         *
         * @param value  the value.
         * @param row  the row index.
         * @param column  the column index.
         */
        public void setValueAt(Object value, int row, int column) {
            this.data[row][column] = value;
            fireTableDataChanged();
        }

        /**
         * Returns the column name.
         *
         * @param column  the column index.
         *
         * @return The column name.
         */
        public String getColumnName(int column) {
            switch(column) {
                case 0 : return "Series:";
                case 1 : return "X:";
                case 2 : return "Y:";
            }
            return null;
        }

    }

    public class mimsPanel extends ChartPanel implements ChartProgressListener {

       JFreeChart chart;
        
        public mimsPanel(JFreeChart chart) {
            super(chart);
            this.chart = chart;
        }

        @Override
        public void chartProgress(ChartProgressEvent event) {
            super.chartProgress(event);
            //add here
            if (chartpanel != null) {
                JFreeChart chart = chartpanel.getChart();
                if (chart != null) {
                    XYPlot plot = (XYPlot) chart.getPlot();
                    double x = plot.getDomainCrosshairValue();
                    double y = plot.getRangeCrosshairValue();

                    if(chartframe!=null) {
                        String name = findMatchingSeriesName(x,y);

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
             int xint = (int)java.lang.Math.round(x);
             xint = xint -1;
             if(xint<0 || xint>=dataset.getItemCount(0)) return "";

             double sx, sy;
             for(int i = 0; i<n; i++) {
                 sx = dataset.getX(i, xint).doubleValue();
                 sy = dataset.getY(i, xint).doubleValue();
                 //System.out.println(y + " - " + sy);
                 if(sy == y) {
                     //System.out.println(sx + " - " + sy);
                     name = plot.getLegendItems().get(i).getLabel();
                 }
             }

            return name;
        }

        public JFreeChart getChart() {
           return this.chart;
        }
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
         chartpanel.getChart().getXYPlot().setDataset(numSeries, tempdata);
         chartpanel.getChart().getXYPlot().setRenderer(numSeries, new StandardXYItemRenderer());
      } else if (rois.length == 0 || images.length == 0 || stats.length == 0) {
         return;
      } else {         
         chartframe = new chartFrame("Plot");
         XYDataset tempdata = getDataset();
         chartframe.addData(tempdata);
         chartframe.pack();
         RefineryUtilities.centerFrameOnScreen(chartframe);
         chartframe.setVisible(true);
      }
   }

    // This method will generate a set of plots for a given set of: rois, stats, images.
    public XYDataset getDataset() {

      // Initialize some variables
      XYSeriesCollection dataset = new XYSeriesCollection();
      XYSeries series[][][] = new XYSeries[rois.length][images.length][stats.length];
      String seriesname[][][] = new String[rois.length][images.length][stats.length];
      ImageStatistics tempstats = null;
      int currentSlice = images[0].getCurrentSlice();

      // begin looping
      for (int i = 0; i < rois.length; i++) {
         for (int ii = 0; ii < planes.size(); ii++) {
            int plane = (Integer)planes.get(ii);
            images[0].setSlice(plane);
            for (int j = 0; j < images.length; j++) {
               for (int k = 0; k < stats.length; k++) {

                  // Generate a name for the dataset.
                  if (seriesname[i][j][k] == null) {                        
                        seriesname[i][j][k] = images[j].getShortTitle();
                        seriesname[i][j][k] = seriesname[i][j][k] + " " + stats[k] + " \n" + "r" + (ui.getRoiManager().getIndex(rois[i].getName())+1);
                  }

                  // Add data to the series.
                  if (series[i][j][k] == null) {
                     series[i][j][k] = new XYSeries(seriesname[i][j][k]);
                  }

                  // Get the stats.
                  Integer[] xy = ui.getRoiManager().getRoiLocation(rois[i].getName(), plane-1);
                  rois[i].setLocation(xy[0], xy[1]);
                  images[j].setRoi(rois[i]);
                  tempstats = images[j].getStatistics();
                  series[i][j][k].add(((Integer)planes.get(ii)).intValue(), getSingleStat(tempstats, stats[k]));

               } // End of Stats
            } // End of Masses
         } // End of Slice
      } // End of Rois

      // Populate the final data structure.
      for (int i = 0; i < rois.length; i++) {
         for (int j = 0; j < images.length; j++) {
            for (int k = 0; k < stats.length; k++) {
               dataset.addSeries(series[i][j][k]);
            }
         }
      }

      images[0].setSlice(currentSlice);

      return dataset;
   }

   public void setImages(MimsPlus[] images){
      this.images = images;
   }

   public void setStats(String[] stats) {
      this.stats = stats;
   }

   public void setRois(Roi[] rois) {
      this.rois = rois;
   }

   void setPlanes(ArrayList planes) {
      this.planes = planes;
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

        return chart;
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

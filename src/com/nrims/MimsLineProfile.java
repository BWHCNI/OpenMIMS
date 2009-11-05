/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims;


import java.awt.Color;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.awt.event.*;
/**
 *
 * @author cpoczatek
 */
public class MimsLineProfile extends JFrame implements ActionListener{

    private JFreeChart chart;
    private ChartPanel chartPanel;
    private int linewidth = 1;
    private UI ui;
    
    public MimsLineProfile(final UI ui) {
        super("Dynamic Line Profile");
        this.setDefaultCloseOperation(this.DISPOSE_ON_CLOSE);
        this.ui = ui;

        XYDataset dataset = createDataset();
        chart = createChart(dataset);

        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        JPopupMenu menu = chartPanel.getPopupMenu();
        JMenuItem menuItem = new javax.swing.JMenuItem("Display text");
        menuItem.addActionListener(this);

        menu.add(menuItem, 2);
        setContentPane(chartPanel);

         // Add menu item for showing/hiding crosshairs.
         JMenuItem xhairs = new JMenuItem("Show/Hide Crosshairs");
         xhairs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               ui.showHideCrossHairs(chartPanel);
            }
         });
         chartPanel.getPopupMenu().addSeparator();
         chartPanel.getPopupMenu().add(xhairs);

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(new KeyEventDispatcher() {
                public boolean dispatchKeyEvent(KeyEvent e) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        chartPanel.keyPressed(e);
                   }
                    return false;
                }
            });

        this.pack();
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand()=="Display text")
            this.displayProfileData();
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
     * @param dataset  the data for the chart.
     * 
     * @return a chart.
     */
    private JFreeChart createChart(final XYDataset dataset) {
        
        // create the chart...
        final JFreeChart chart = ChartFactory.createXYLineChart(
            "",      // chart title
            "L",                      // x axis label
            "P",                      // y axis label
            dataset,                  // data
            PlotOrientation.VERTICAL,
            true,                     // include legend
            true,                     // tooltips
            false                     // urls
        );

      // get a reference to the plot for further customisation...
      XYPlot plot = chart.getXYPlot();

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
    
    public void updateData(double[] newdata, String name, int width) {
        if (newdata == null) {
            return;
        }

        linewidth = width;
        XYSeries series = new XYSeries(name + " w: "+width);
        for(int i = 0; i< newdata.length; i++) {
            series.add(i, newdata[i]);
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        org.jfree.chart.plot.XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDataset(dataset);

        chart.fireChartChanged();
    }

    public void displayProfileData() {
        org.jfree.chart.plot.XYPlot plot = (XYPlot) chart.getPlot();
        XYDataset data = plot.getDataset();

        ij.measure.ResultsTable table = new ij.measure.ResultsTable();
        table.setHeading(1, "Position");
        table.setHeading(2, plot.getLegendItems().get(0).getLabel() + " : width " + linewidth);
        table.incrementCounter();

        for (int i = 0; i < data.getItemCount(0); i++) {
            table.addValue(1, data.getXValue(0, i));
            table.addValue(2, data.getYValue(0, i));
            table.incrementCounter();
        }

        table.show("");
    }

}

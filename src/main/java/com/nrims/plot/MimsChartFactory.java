package com.nrims.plot;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * A Mims extension of the ChartFactory utility methods for creating some standard charts with JFreeChart.
 */
public abstract class MimsChartFactory extends ChartFactory {

    /**
     * Creates a line chart (based on an {@link XYDataset}) with default settings. Uses MimsXYplot instead of XYPlot for
     * customized features.
     *
     * @param title the chart title (<code>null</code> permitted).
     * @param xAxisLabel a label for the X-axis (<code>null</code> permitted).
     * @param yAxisLabel a label for the Y-axis (<code>null</code> permitted).
     * @param dataset the dataset for the chart (<code>null</code> permitted).
     * @param orientation the plot orientation (horizontal or vertical) (<code>null</code> NOT permitted).
     * @param legend a flag specifying whether or not a legend is required.
     * @param tooltips configure chart to generate tool tips?
     * @param urls configure chart to generate URLs?
     *
     * @return The chart.
     */
    public static JFreeChart createMimsXYLineChart(String title,
            String xAxisLabel,
            String yAxisLabel,
            XYDataset dataset,
            PlotOrientation orientation,
            boolean legend,
            boolean tooltips,
            boolean urls) {

        if (orientation == null) {
            throw new IllegalArgumentException("Null 'orientation' argument.");
        }
        NumberAxis xAxis = new NumberAxis(xAxisLabel);
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis(yAxisLabel);
        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        MimsXYPlot plot = new MimsXYPlot(dataset, xAxis, yAxis, renderer);
        plot.setOrientation(orientation);
        if (tooltips) {
            renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        }
        if (urls) {
            renderer.setURLGenerator(new StandardXYURLGenerator());
        }
        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT,
                plot, legend);
        getChartTheme().apply(chart);
        return chart;
    }

    /**
     * Creates a histogram chart. This chart is constructed with an {@link MimsXYPlot} using an {@link XYBarRenderer}.
     * The domain and range axes are {@link NumberAxis} instances.
     *
     * @param title the chart title (<code>null</code> permitted).
     * @param xAxisLabel the x axis label (<code>null</code> permitted).
     * @param yAxisLabel the y axis label (<code>null</code> permitted).
     * @param dataset the dataset (<code>null</code> permitted).
     * @param orientation the orientation (horizontal or vertical) (<code>null</code> NOT permitted).
     * @param legend create a legend?
     * @param tooltips display tooltips?
     * @param urls generate URLs?
     *
     * @return The chart.
     */
    public static JFreeChart createMimsHistogram(String title,
            String xAxisLabel, String yAxisLabel, IntervalXYDataset dataset,
            PlotOrientation orientation, boolean legend, boolean tooltips,
            boolean urls) {

        if (orientation == null) {
            throw new IllegalArgumentException("Null 'orientation' argument.");
        }
        NumberAxis xAxis = new NumberAxis(xAxisLabel);
        xAxis.setAutoRangeIncludesZero(false);
        ValueAxis yAxis = new NumberAxis(yAxisLabel);

        XYItemRenderer renderer = new XYBarRenderer();
        if (tooltips) {
            renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        }
        if (urls) {
            renderer.setURLGenerator(new StandardXYURLGenerator());
        }

        MimsXYPlot plot = new MimsXYPlot(dataset, xAxis, yAxis, renderer);
        plot.setOrientation(orientation);
        plot.setDomainZeroBaselineVisible(true);
        plot.setRangeZeroBaselineVisible(true);
        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT,
                plot, legend);
        getChartTheme().apply(chart);
        return chart;
    }
}

package com.nrims.plot;

/**
 *
 * @author cpoczatek
 */
import ij.IJ;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.AxisEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.Pannable;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.Zoomable;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

public class MimsChartPanel extends ChartPanel {

    /**
     * Added by zkaufman.
     */
    private boolean zoomX;
    private boolean zoomY;
    private int initialZoomX;
    private int initialZoomY;
    private int currentSeriesInt = -1;
    private String currentSeriesName = null;
    private Object e;
    int colorIdx = 0;
    /*
     * old color_list:
    Color[] colorList = {Color.BLACK, Color.BLUE, Color.CYAN, Color.GRAY, Color.GREEN,
                           Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.WHITE,
                           Color.YELLOW};
     */

    //DJ: 09/22/2014
    Color black = new Color(0, 0, 0);        // default values for the black 
    Color blue = new Color(0, 0, 255);      // default values for the blue
    Color gray = new Color(128, 128, 128);  // default values for the gray 
    Color green = new Color(0, 255, 0);      // default values for the green 
    Color red = new Color(255, 0, 0);      // default values for the red
    Color dark_yellow = new Color(220, 220, 0);
    Color purple = new Color(153, 0, 255);
    Color brown = new Color(153, 102, 0);
    Color orange = new Color(255, 200, 0);    // default values for the orange
    Color dark_green = new Color(51, 102, 0);
    Color light_Blue = new Color(102, 153, 255);

    Color[] colorList = {black,
        blue,
        gray,
        green,
        red,
        dark_yellow,
        purple, brown,
        orange,
        dark_green,
        light_Blue};

    private final int BLACK = 0;
    private final int BLUE = 1;
    private final int GRAY = 2;
    private final int GREEN = 3;
    private final int RED = 4;
    private final int DARK_YELLOW = 5;
    private final int PURPLE = 6;
    private final int BROWN = 7;
    private final int ORANGE = 8;
    private final int DARK_GREEN = 9;
    private final int LIGHT_BLUE = 10;

    private float lineThikness = 1.0f;
    private float minLineThikness = 1.0f;
    private float maxLineThikness = 30.0f;
    int numberOfSeries = 0;

    private ArrayList<Integer> hiddenSeries = new ArrayList<Integer>();

    //hiding fields in parent class on purpose
    private double panH;
    private double panW;
    private Point panLast;

    //This is the same as ChartPanel(JFreeChart chart)
    public MimsChartPanel(JFreeChart chart) {
        super(
                chart,
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT,
                DEFAULT_MINIMUM_DRAW_WIDTH,
                DEFAULT_MINIMUM_DRAW_HEIGHT,
                DEFAULT_MAXIMUM_DRAW_WIDTH,
                DEFAULT_MAXIMUM_DRAW_HEIGHT,
                DEFAULT_BUFFER_USED,
                true, // properties
                true, // save
                true, // print
                true, // zoom
                true // tooltips
        );

        //DJ: 09/24/2014
        // Just to extract the DEFAULT rgb values that are used in the default Color class
        /*  
         System.out.println("BLACK = "  + Color.BLACK.getRed()  + "," + Color.BLACK.getGreen()  + ", " +Color.BLACK.getBlue());
         System.out.println("BLUE = "   + Color.BLUE.getRed()   + "," + Color.BLUE.getGreen()   + ", " +Color.BLUE.getBlue());
         System.out.println("GRAY = "   + Color.GRAY.getRed()   + "," + Color.GRAY.getGreen()   + ", " +Color.GRAY.getBlue());
         System.out.println("GREEN = "  + Color.GREEN.getRed()  + "," + Color.GREEN.getGreen()  + ", " +Color.GREEN.getBlue());
         System.out.println("RED = "    + Color.RED.getRed()    + "," + Color.RED.getGreen()    + ", " +Color.RED.getBlue());
         System.out.println("ORANGE = " + Color.ORANGE.getRed() + "," + Color.ORANGE.getGreen() + ", " +Color.ORANGE.getBlue());
         
         Color yellow = Color.YELLOW.darker();
         System.out.println("DARK-YELLOW = " + yellow.getRed() + "," + yellow.getGreen() + ", " +yellow.getBlue());
         */
    }

    /**
     * Returns <code>true</code> if the point (x,y) belongs to an AxisEntity, otherwise returns <code>false</code>.
     *
     * @param viewX the x position
     * @param viewY the y position
     * @return true if point (x,y) belongs to an AxisEntity.
     */
    private boolean isAxisEntity(int viewX, int viewY) {

        ChartEntity chartEntity = null;
        EntityCollection entities = this.getChartRenderingInfo().getEntityCollection();
        for (int i = 0; i < entities.getEntities().size(); i++) {
            chartEntity = entities.getEntity(i);
            if (chartEntity instanceof AxisEntity) {
                if (chartEntity.getArea().contains(viewX, viewY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the AxisEntity belonging to point (x,y). Returns <code>null</code> if point does not belong to an
     * AxisEntity.
     *
     * @param viewX the x position
     * @param viewY the y position
     * @return the AxisEntity, otherwise null.
     */
    private AxisEntity getAxisEntity(int viewX, int viewY) {

        AxisEntity axisEntity = null;
        ChartEntity chartEntity = null;
        EntityCollection entities = this.getChartRenderingInfo().getEntityCollection();
        for (int i = 0; i < entities.getEntities().size(); i++) {
            chartEntity = entities.getEntity(i);
            if (chartEntity instanceof AxisEntity) {
                if (chartEntity.getArea().contains(viewX, viewY)) {
                    axisEntity = (AxisEntity) chartEntity;
                }
            }
        }
        return axisEntity;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Plot plot = this.getChart().getPlot();
        ChartEntity entity = null;
        AxisEntity axisEntity = null;
        boolean isAxisEntity = false;

        // Get the entity the mouse is over (PlotEntity, LegendEntity, AxisEntity...)
        EntityCollection entities = this.getChartRenderingInfo().getEntityCollection();
        if (entities != null) {
            Insets insets = getInsets();
            entity
                    = //entities.getEntity(
                    //(int) ((e.getX() - insets.left) / this.scaleX),
                    //(int) ((e.getY() - insets.top) / this.scaleY));
                    getEntityForPoint(
                            (int) e.getX(),
                            (int) e.getY());

            isAxisEntity = isAxisEntity((int) e.getX(), (int) e.getY());
            if (isAxisEntity) {
                axisEntity = getAxisEntity((int) e.getX(), (int) e.getY());
            }

        }

        if (e.isPopupTrigger()) {
            if (this.getPopupMenu() != null) {
                displayPopupMenu(e.getX(), e.getY());
            }
        } else if (isAxisEntity && plot instanceof XYPlot) {
            XYPlot xyplot = (XYPlot) plot;
            String entityLabel = axisEntity.getAxis().getLabel();
            String domainAxisLabel = xyplot.getDomainAxis().getLabel();
            String rangeAxisLabel = xyplot.getRangeAxis().getLabel();

            // As far as I can tell there is no way to determine if the 'entity'
            // is from an x- or y- axis without comparing labels. However, there
            // exist an ambiguous case when both axes either 1) have the same name, or
            // 2) have a blank string for a name (i.e. ""), or 3) are undefined (i.e. 'null').
            // Therefore these situation should be avoided when creating plots to begin with.
            if (domainAxisLabel == null && rangeAxisLabel == null) {
                return;
            }

            if (domainAxisLabel.matches(rangeAxisLabel)) {
                return;
            }

            if (entityLabel.matches(domainAxisLabel)) {
                this.zoomY = false;
                this.zoomX = true;
                this.initialZoomX = e.getXOnScreen();
                this.initialZoomY = e.getYOnScreen();
            } else if (entityLabel.matches(rangeAxisLabel)) {
                this.zoomY = true;
                this.zoomX = false;
                this.initialZoomX = e.getXOnScreen();
                this.initialZoomY = e.getYOnScreen();
            }

        } else // can we pan this plot?
        {
            if (plot instanceof Pannable) {
                Pannable pannable = (Pannable) plot;
                if (pannable.isDomainPannable() || pannable.isRangePannable()) {
                    Rectangle2D screenDataArea = getScreenDataArea(e.getX(),
                            e.getY());
                    if (screenDataArea != null && screenDataArea.contains(
                            e.getPoint())) {

                        //private 
                        this.panW = screenDataArea.getWidth();
                        this.panH = screenDataArea.getHeight();
                        this.panLast = e.getPoint();

                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                }
                // the actual panning occurs later in the mouseDragged()
                // method
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!IJ.getToolName().equals("Drag To Writer tool")) {
            // if the popup menu has already been triggered, then ignore dragging...
            if (this.getPopupMenu() != null && this.getPopupMenu().isShowing()) {
                return;
            }

            // handle panning if we have a start point
            if (this.panLast != null) {
                double dx = e.getX() - this.panLast.getX();
                double dy = e.getY() - this.panLast.getY();
                if (dx == 0.0 && dy == 0.0) {
                    return;
                }
                double wPercent = -dx / this.panW;
                double hPercent = dy / this.panH;
                boolean old = this.getChart().getPlot().isNotify();
                this.getChart().getPlot().setNotify(false);
                Pannable p = (Pannable) this.getChart().getPlot();
                if (p.getOrientation() == PlotOrientation.VERTICAL) {
                    p.panDomainAxes(wPercent, this.getChartRenderingInfo().getPlotInfo(),
                            this.panLast);
                    p.panRangeAxes(hPercent, this.getChartRenderingInfo().getPlotInfo(),
                            this.panLast);
                } else {
                    p.panDomainAxes(hPercent, this.getChartRenderingInfo().getPlotInfo(),
                            this.panLast);
                    p.panRangeAxes(wPercent, this.getChartRenderingInfo().getPlotInfo(),
                            this.panLast);
                }
                this.panLast = e.getPoint();
                this.getChart().getPlot().setNotify(old);
                return;
            } else if (this.zoomX || zoomY) {
                if (this.getChart().getPlot() instanceof Zoomable) {
                    Zoomable zoom = (Zoomable) this.getChart().getPlot();
                    PlotRenderingInfo plotInfo = this.getChartRenderingInfo().getPlotInfo();
                    double zoomFraction = 1;
                    if (zoomX) {
                        int currentY = e.getYOnScreen();
                        int diff = currentY - initialZoomY;
                        zoomFraction = 1 + (diff * 0.005);
                        zoom.zoomDomainAxes(zoomFraction, plotInfo, new Point(e.getXOnScreen(), e.getYOnScreen()));
                        this.initialZoomY = currentY;
                    } else {
                        int currentX = e.getXOnScreen();
                        int diff = currentX - initialZoomX;
                        zoomFraction = 1 - (diff * 0.005);
                        zoom.zoomRangeAxes(zoomFraction, plotInfo, new Point(e.getXOnScreen(), e.getYOnScreen()));
                        this.initialZoomX = currentX;
                    }
                }
            }
        }
    }

    /**
     * Handles a 'mouse released' event. On Windows, we need to check if this is a popup trigger, but only if we haven't
     * already been tracking a zoom rectangle.
     *
     * @param e information about the event.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (!IJ.getToolName().equals("Drag To Writer tool")) {
            // if we've been panning, we need to reset now that the mouse is
            // released...
            if (this.panLast != null) {
                this.panLast = null;
                setCursor(Cursor.getDefaultCursor());
            }

            if (zoomX || zoomY) {
                zoomX = false;
                zoomY = false;
            } //delete large block of parent method dealing with zoomRectangle
            //which we shouldn't care about since we don't use that for zooming        
            else if (e.isPopupTrigger()) {
                if (this.getPopupMenu() != null) {
                    displayPopupMenu(e.getX(), e.getY());
                }
            }
        } else {
            //mimsUno.dropImage(ui.getScreenCaptureCurrentImage(), libreTitle, title, ui.getDescription());
        }
    }

    /**
     * Receives notification of mouse clicks on the panel. These are translated and passed on to any registered
     * {@link ChartMouseListener}s.
     *
     * @param event Information about the mouse event.
     */
    @Override
    public void mouseClicked(MouseEvent event) {

        Insets insets = getInsets();
        int x = (int) ((event.getX() - insets.left) / this.getScaleX());
        int y = (int) ((event.getY() - insets.top) / this.getScaleY());

        this.setAnchor(new Point2D.Double(x, y));
        if (this.getChart() == null) {
            return;
        }

        this.getChart().setNotify(true);  // force a redraw

        ChartEntity entity = null;
        if (this.getChartRenderingInfo() != null) {
            EntityCollection entities = this.getChartRenderingInfo().getEntityCollection();
            if (entities != null) {
                entity = entities.getEntity(x, y);
            }
        }

        // Spanwn a new thread to handle graphical events (e.g. highlighting).
        if (entity != null) {
            this.e = entity;
            Runnable runnable = new HighLightThread();
            Thread thread = new Thread(runnable);
            thread.start();
        }

        // new entity code...
        Object[] listeners = this.getListeners(ChartMouseListener.class);
        if (listeners.length == 0) {
            return;
        }
        ChartMouseEvent chartEvent = new ChartMouseEvent(getChart(), event, entity);
        for (int i = listeners.length - 1; i >= 0; i -= 1) {
            ((ChartMouseListener) listeners[i]).chartMouseClicked(chartEvent);
        }

    }

    /**
     * Implementation of the MouseMotionListener's method.
     *
     * @param e the event.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        Graphics2D g2 = (Graphics2D) getGraphics();

        /* Unneeded?
         *  Draws a vertical line used to trace the mouse position to the horizontal
         * axis.
         
         if (this.horizontalAxisTrace) {
         drawHorizontalAxisTrace(g2, e.getX());
         }
         if (this.verticalAxisTrace) {
         drawVerticalAxisTrace(g2, e.getY());
         }
         ????????? */
        g2.dispose();

        Insets insets = getInsets();
        int x = (int) ((e.getX() - insets.left) / this.getScaleX());
        int y = (int) ((e.getY() - insets.top) / this.getScaleY());

        ChartEntity entity = null;
        if (this.getChartRenderingInfo() != null) {
            EntityCollection entities = this.getChartRenderingInfo().getEntityCollection();
            if (entities != null) {
                entity = entities.getEntity(x, y);
            }
        }

        String seriesName = null;
        int seriesInt = getSeriesInt(entity);

        if (seriesInt > -1) {
            currentSeriesInt = seriesInt;
            seriesName = getSeriesName(entity);
            if (seriesName != null) {
                currentSeriesName = seriesName;
            }
        } else {
            currentSeriesInt = -1;
            currentSeriesName = null;
        }

        Object[] listeners = this.getListeners(ChartMouseListener.class);
        if (listeners.length == 0) {
            return;
        }
        // we can only generate events if the panel's chart is not null
        // (see bug report 1556951)
        if (this.getChart() != null) {
            ChartMouseEvent event = new ChartMouseEvent(getChart(), e, entity);
            for (int i = listeners.length - 1; i >= 0; i -= 1) {
                ((ChartMouseListener) listeners[i]).chartMouseMoved(event);
            }
        }
    }

    //DJ: 09/22/2014
    public void setNumberOfSeries(int numberOfSeries_arg) {
        numberOfSeries = numberOfSeries_arg;
    }

    //DJ: 09/22/2014
    public float getLineThikness() {
        return lineThikness;
    }

    //DJ: 09/22/2014
    public boolean setLineThikness(float lineThikness_arg) {
        boolean result = false;
        if (lineThikness_arg >= minLineThikness && lineThikness_arg <= maxLineThikness) {
            lineThikness = lineThikness_arg;
            result = true;
        }
        return result;
    }

    public void keyPressed(KeyEvent e) {

        // Get the plot.
        Plot plot = getChart().getPlot();
        XYPlot xyplot;
        if (!(plot instanceof XYPlot)) {
            return;
        }
        xyplot = (XYPlot) plot;

        // Change color.
        if (e.getKeyChar() == 'c') {

            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }

            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[colorIdx]);
            colorIdx++;
            if (colorIdx >= colorList.length) {
                colorIdx = 0;
            }
        } // DJ: 09/22/2014
        else if (e.getKeyChar() == '+') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }

            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();

            //renderer.getSeriesStroke(currentSeriesInt)
            if (setLineThikness(lineThikness + 0.5f)) {
                BasicStroke basicStroke = new BasicStroke(lineThikness);
                for (int i = 0; i < numberOfSeries; i++) {
                    renderer.setSeriesStroke(i, basicStroke);
                }
            }
            /*
         if (lineThikness + 0.5f <=  maxLineThikness){
            lineThikness += 0.5f;
            BasicStroke basicStroke = new BasicStroke(lineThikness);
            renderer.setSeriesStroke(currentSeriesInt, basicStroke);
         }
             */
        } // DJ: 09/22/2014
        else if (e.getKeyChar() == '-') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }

            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();

            //renderer.getSeriesStroke(currentSeriesInt)
            if (setLineThikness(lineThikness - 0.5f)) {
                BasicStroke basicStroke = new BasicStroke(lineThikness);
                for (int i = 0; i < numberOfSeries; i++) {
                    renderer.setSeriesStroke(i, basicStroke);
                }
            }
            /*
         if (lineThikness - 0.5f >=  minLineThikness){
            lineThikness -= 0.5f;
            BasicStroke basicStroke = new BasicStroke(lineThikness);
            renderer.setSeriesStroke(currentSeriesInt, basicStroke);
         }
             */

            // DJ: 09/22/2014   
        } else if (e.getKeyChar() == 'a') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }
            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[BLACK]);

            // DJ: 09/22/2014
        } else if (e.getKeyChar() == 'b') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }
            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[BLUE]);

            // DJ: 09/22/2014
        } else if (e.getKeyChar() == 'e') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }
            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[GRAY]);

            // DJ: 09/22/2014
        } else if (e.getKeyChar() == 'g') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }
            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[GREEN]);

            // DJ: 09/22/2014
        } else if (e.getKeyChar() == 'r') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }
            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[RED]);

            // DJ: 09/22/2014
        } else if (e.getKeyChar() == 'y') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }
            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[DARK_YELLOW]);

            // DJ: 09/22/2014
        } else if (e.getKeyChar() == 'p') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }
            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[PURPLE]);

            // DJ: 09/22/2014
        } else if (e.getKeyChar() == 'w') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }
            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[BROWN]);

            // DJ: 09/22/2014
        } else if (e.getKeyChar() == 'o') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }
            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[ORANGE]);

            // DJ: 09/22/2014
        } else if (e.getKeyChar() == 'd') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }
            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[DARK_GREEN]);

            // DJ: 09/22/2014
        } else if (e.getKeyChar() == 'l') {
            // Get the series.
            if (currentSeriesInt < 0) {
                return;
            }
            // Set the color.
            XYItemRenderer renderer = xyplot.getRenderer();
            renderer.setSeriesPaint(currentSeriesInt, colorList[LIGHT_BLUE]);

            // Hide the plot.
        } else if (e.getKeyChar() == 'h') {

            // Get the series and hide.
            if (currentSeriesInt < 0 && hiddenSeries.isEmpty()) {
                return;
            } else if (currentSeriesInt < 0 && !hiddenSeries.isEmpty()) {
                int seriesInt = (Integer) hiddenSeries.get(0);
                hiddenSeries.remove(0);
                XYItemRenderer renderer = xyplot.getRenderer();
                renderer.setSeriesVisible(seriesInt, true);
            } else {
                XYItemRenderer renderer = xyplot.getRenderer();
                renderer.setSeriesVisible(currentSeriesInt, false);
                hiddenSeries.add(currentSeriesInt);
            }

            // Reset zoom.
        } else if (e.getKeyChar() == 'r') {

            // Auto range.
            xyplot.getDomainAxis().setAutoRange(true);
            xyplot.getRangeAxis().setAutoRange(true);
        }
    }

    // Get the series index associcated with the chartEntity.
    public int getSeriesInt(ChartEntity chartEntity) {
        int seriesKey = -1;
        if (chartEntity instanceof LegendItemEntity) {
            LegendItemEntity entity = (LegendItemEntity) chartEntity;
            Comparable seriesName = entity.getSeriesKey();

            // Get the plot.
            XYPlot plot = (XYPlot) getChart().getPlot();
            XYDataset dataset = plot.getDataset();

            // Loop over all series, find matching one.
            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                if (dataset.getSeriesKey(i) == seriesName) {
                    seriesKey = i;
                    break;
                }
            }
        } else if (chartEntity instanceof XYItemEntity) {
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
            seriesName = (String) entity.getSeriesKey();
        } else if (chartEntity instanceof XYItemEntity) {
            XYItemEntity entity = (XYItemEntity) chartEntity;
            XYPlot plot = (XYPlot) getChart().getPlot();
            XYDataset dataset = plot.getDataset();
            seriesName = (String) dataset.getSeriesKey(entity.getSeriesIndex());
        }
        return seriesName;
    }

    //inner class...
    class HighLightThread implements Runnable {

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
            XYPlot plot = (XYPlot) getChart().getPlot();
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

                        //// DJ: 09/22/2014
                        BasicStroke originalStroke = new BasicStroke(getLineThikness());
                        renderer.setSeriesStroke(i, originalStroke);

                    } catch (InterruptedException ex) {
                        // do nothing
                    }
                    break;
                }
            }
        }
    }

}

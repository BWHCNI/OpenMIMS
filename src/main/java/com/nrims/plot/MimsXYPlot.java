package com.nrims.plot;

import com.nrims.MimsJFreeChart;
import com.nrims.MimsLineProfile;
import ij.gui.Roi;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Map;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

/**
 * A Mims customized extension of the {@link XYPlot} class. Only a small difference in the draw method, relating to
 * drawing and placement of annotations and crosshairs, separates this class from its parent {@link XYPlot}.
 */
public class MimsXYPlot extends XYPlot {

    int pixelX;
    int pixelY;
    boolean pixelsSet = false;
    MimsLineProfile lineProfile = null;
    MimsJFreeChart parent;
    Roi[] rois;

    /**
     * A flag that controls whether or not a label displaying XHair location is displayed.
     */
    private boolean crosshairLabelVisible;
    XYTextAnnotation xyannot = new XYTextAnnotation("", 0, 0);
    XYTextAnnotation pixelCoords = new XYTextAnnotation("", 0, 0);

    public MimsXYPlot(XYDataset dataset, ValueAxis domainAxis,
            ValueAxis rangeAxis, XYItemRenderer renderer) {
        super(dataset, domainAxis,
                rangeAxis, renderer);

        this.crosshairLabelVisible = true;
        this.addAnnotation(pixelCoords);
        this.addAnnotation(xyannot);

    }

    public MimsXYPlot(XYPlot xyplot) {
        super(xyplot.getDataset(), xyplot.getDomainAxis(),
                xyplot.getRangeAxis(), xyplot.getRenderer());

        this.crosshairLabelVisible = true;
        this.addAnnotation(pixelCoords);
        this.addAnnotation(xyannot);

    }

    public void setRois(Roi[] r) {
        rois = r;
    }

    public String identifySeries(double x, double y) {
        XYDataset dataset = getDataset();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            for (int j = 0; j < dataset.getItemCount(i); j++) {
                if (dataset.getXValue(i, j) == x && dataset.getYValue(i, j) == y) {
                    return dataset.getSeriesKey(i).toString();
                }
            }
        }
        return null;
    }

    /**
     * Draws the plot within the specified area on a graphics device.
     *
     * This is a copied and modified version of the parent class method. Could not find another way to change. See end
     * of method for modifications.
     *
     * @param g2 the graphics device.
     * @param area the plot area (in Java2D space).
     * @param anchor an anchor point in Java2D space (<code>null</code> permitted).
     * @param parentState the state from the parent plot, if there is one (<code>null</code> permitted).
     * @param info collects chart drawing information (<code>null</code> permitted).
     */
    @Override
    public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
            PlotState parentState, PlotRenderingInfo info) {

        // if the plot area is too small, just return...
        boolean b1 = (area.getWidth() <= MINIMUM_WIDTH_TO_DRAW);
        boolean b2 = (area.getHeight() <= MINIMUM_HEIGHT_TO_DRAW);
        if (b1 || b2) {
            return;
        }

        // record the plot area...
        if (info != null) {
            info.setPlotArea(area);
        }

        // adjust the drawing area for the plot insets (if any)...
        RectangleInsets insets = getInsets();
        insets.trim(area);

        AxisSpace space = calculateAxisSpace(g2, area);
        Rectangle2D dataArea = space.shrink(area, null);
        getAxisOffset().trim(dataArea);

        //Edit:
        //integerize() is private so we don't have access
        //dataArea = integerise(dataArea);
        //if (dataArea.isEmpty()) {
        //    return;
        //}
        createAndAddEntity((Rectangle2D) dataArea.clone(), info, null, null);
        if (info != null) {
            info.setDataArea(dataArea);
        }

        // draw the plot background and axes...
        drawBackground(g2, dataArea);
        Map axisStateMap = drawAxes(g2, area, dataArea, info);

        PlotOrientation orient = getOrientation();

        // the anchor point is typically the point where the mouse last
        // clicked - the crosshairs will be driven off this point...
        if (anchor != null && !dataArea.contains(anchor)) {
            anchor = null;
        }
        CrosshairState crosshairState = new CrosshairState();
        crosshairState.setCrosshairDistance(Double.POSITIVE_INFINITY);
        crosshairState.setAnchor(anchor);

        crosshairState.setAnchorX(Double.NaN);
        crosshairState.setAnchorY(Double.NaN);
        if (anchor != null) {
            ValueAxis domainAxis = getDomainAxis();
            if (domainAxis != null) {
                double x;
                if (orient == PlotOrientation.VERTICAL) {
                    x = domainAxis.java2DToValue(anchor.getX(), dataArea,
                            getDomainAxisEdge());
                } else {
                    x = domainAxis.java2DToValue(anchor.getY(), dataArea,
                            getDomainAxisEdge());
                }
                crosshairState.setAnchorX(x);
            }
            ValueAxis rangeAxis = getRangeAxis();
            if (rangeAxis != null) {
                double y;
                if (orient == PlotOrientation.VERTICAL) {
                    y = rangeAxis.java2DToValue(anchor.getY(), dataArea,
                            getRangeAxisEdge());
                } else {
                    y = rangeAxis.java2DToValue(anchor.getX(), dataArea,
                            getRangeAxisEdge());
                }
                crosshairState.setAnchorY(y);
            }
        }
        crosshairState.setCrosshairX(getDomainCrosshairValue());
        crosshairState.setCrosshairY(getRangeCrosshairValue());
        Shape originalClip = g2.getClip();
        Composite originalComposite = g2.getComposite();

        g2.clip(dataArea);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                getForegroundAlpha()));

        AxisState domainAxisState = (AxisState) axisStateMap.get(
                getDomainAxis());
        if (domainAxisState == null) {
            if (parentState != null) {
                domainAxisState = (AxisState) parentState.getSharedAxisStates()
                        .get(getDomainAxis());
            }
        }

        AxisState rangeAxisState = (AxisState) axisStateMap.get(getRangeAxis());
        if (rangeAxisState == null) {
            if (parentState != null) {
                rangeAxisState = (AxisState) parentState.getSharedAxisStates()
                        .get(getRangeAxis());
            }
        }
        if (domainAxisState != null) {
            drawDomainTickBands(g2, dataArea, domainAxisState.getTicks());
        }
        if (rangeAxisState != null) {
            drawRangeTickBands(g2, dataArea, rangeAxisState.getTicks());
        }
        if (domainAxisState != null) {
            drawDomainGridlines(g2, dataArea, domainAxisState.getTicks());
            drawZeroDomainBaseline(g2, dataArea);
        }
        if (rangeAxisState != null) {
            drawRangeGridlines(g2, dataArea, rangeAxisState.getTicks());
            drawZeroRangeBaseline(g2, dataArea);
        }

        Graphics2D savedG2 = g2;
        BufferedImage dataImage = null;
        if (getShadowGenerator() != null) {
            dataImage = new BufferedImage((int) dataArea.getWidth(),
                    (int) dataArea.getHeight(), BufferedImage.TYPE_INT_ARGB);
            g2 = dataImage.createGraphics();
            g2.translate(-dataArea.getX(), -dataArea.getY());
            g2.setRenderingHints(savedG2.getRenderingHints());
        }

        // draw the markers that are associated with a specific renderer...
        for (int i = 0; i < getRendererCount(); i++) {
            drawDomainMarkers(g2, dataArea, i, Layer.BACKGROUND);
        }
        for (int i = 0; i < getRendererCount(); i++) {
            drawRangeMarkers(g2, dataArea, i, Layer.BACKGROUND);
        }

        // now draw annotations and render data items...
        boolean foundData = false;
        DatasetRenderingOrder order = getDatasetRenderingOrder();
        if (order == DatasetRenderingOrder.FORWARD) {

            // draw background annotations
            int rendererCount = getRendererCount();
            for (int i = 0; i < rendererCount; i++) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.BACKGROUND, info);
                }
            }

            // render data items...
            for (int i = 0; i < getDatasetCount(); i++) {
                foundData = render(g2, dataArea, i, info, crosshairState)
                        || foundData;
            }

            // draw foreground annotations
            for (int i = 0; i < rendererCount; i++) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.FOREGROUND, info);
                }
            }

        } else if (order == DatasetRenderingOrder.REVERSE) {

            // draw background annotations
            int rendererCount = getRendererCount();
            for (int i = rendererCount - 1; i >= 0; i--) {
                XYItemRenderer r = getRenderer(i);
                if (i >= getDatasetCount()) { // we need the dataset to make
                    continue;                 // a link to the axes
                }      
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                                  
                    // Prevent the lower and upper x axis range from being the same.  This happens 
                    // if the user selects an entire image and does a cut, thus setting all pixels 
                    // to zero.  JFreeChart does not like this, but artifically forcing the upper 
                    // bound to be slightly higher than the lower prevents this error 
                    // ( IllegalArgumentException: Requires xLow < xHigh ).  This is an ugly fix,
                    // but given the low likelihood of this scenario, I will leave it this way
                    // for now.
                    double lower = domainAxis.getRange().getLowerBound();
                    double upper = domainAxis.getRange().getUpperBound();
                    if (upper <= lower) {
                        domainAxis.setUpperBound(lower + 1.0); 
                       // domainAxis.setUpperBound(lower + 0.000001);
                    }
                    lower = rangeAxis.getRange().getLowerBound();
                    upper = rangeAxis.getRange().getUpperBound();
                    if (upper <= lower) {
                        rangeAxis.setUpperBound(lower + 1.0); 
                    }
                    
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.BACKGROUND, info);
                }
            }

            for (int i = getDatasetCount() - 1; i >= 0; i--) {
                foundData = render(g2, dataArea, i, info, crosshairState)
                        || foundData;
            }

            // draw foreground annotations
            for (int i = rendererCount - 1; i >= 0; i--) {
                XYItemRenderer r = getRenderer(i);
                if (i >= getDatasetCount()) { // we need the dataset to make
                    continue;                 // a link to the axes
                }
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.FOREGROUND, info);
                }
            }

        }

        // draw domain crosshair if required...
        int index = crosshairState.getDatasetIndex();

        int xAxisIndex = crosshairState.getDomainAxisIndex();
        // JfreeChairt JavaDocs say "deprecated As of version 1.0.11, the domain axis should be determined
        //   using the dataset index.  ""  Okay, let's try:

        System.out.println("index = " + index + "   xAxisIndex= " + xAxisIndex);
        ValueAxis xAxis = getDomainAxis(xAxisIndex);
        RectangleEdge xAxisEdge = getRangeAxisEdge(xAxisIndex);
        if (!isDomainCrosshairLockedOnData() && anchor != null) {
            double xx;
            if (orient == PlotOrientation.VERTICAL) {
                xx = xAxis.java2DToValue(anchor.getX(), dataArea, xAxisEdge);
            } else {
                xx = xAxis.java2DToValue(anchor.getY(), dataArea, xAxisEdge);
            }
            crosshairState.setCrosshairX(xx);
        }
        setDomainCrosshairValue(crosshairState.getCrosshairX(), false);
        if (isDomainCrosshairVisible()) {
            double x = getDomainCrosshairValue();
            Paint paint = getDomainCrosshairPaint();
            Stroke stroke = getDomainCrosshairStroke();
            drawDomainCrosshair(g2, dataArea, orient, x, xAxis, stroke, paint);
        }

        // draw range crosshair if required...
        int yAxisIndex = crosshairState.getRangeAxisIndex();
        ValueAxis yAxis = getRangeAxis(yAxisIndex);
        RectangleEdge yAxisEdge = getRangeAxisEdge(yAxisIndex);
        if (!isRangeCrosshairLockedOnData() && anchor != null) {
            double yy;
            if (orient == PlotOrientation.VERTICAL) {
                yy = yAxis.java2DToValue(anchor.getY(), dataArea, yAxisEdge);
            } else {
                yy = yAxis.java2DToValue(anchor.getX(), dataArea, yAxisEdge);
            }
            crosshairState.setCrosshairY(yy);
        }
        setRangeCrosshairValue(crosshairState.getCrosshairY(), false);
        if (isRangeCrosshairVisible()) {
            double y = getRangeCrosshairValue();
            Paint paint = getRangeCrosshairPaint();
            Stroke stroke = getRangeCrosshairStroke();
            drawRangeCrosshair(g2, dataArea, orient, y, yAxis, stroke, paint);
        }

        if (!foundData) {
            drawNoDataMessage(g2, dataArea);
        }

        for (int i = 0; i < getRendererCount(); i++) {
            drawDomainMarkers(g2, dataArea, i, Layer.FOREGROUND);
        }
        for (int i = 0; i < getRendererCount(); i++) {
            drawRangeMarkers(g2, dataArea, i, Layer.FOREGROUND);
        }

        if (getShadowGenerator() != null) {
            BufferedImage shadowImage
                    = getShadowGenerator().createDropShadow(dataImage);
            g2 = savedG2;
            g2.drawImage(shadowImage, (int) dataArea.getX()
                    + getShadowGenerator().calculateOffsetX(),
                    (int) dataArea.getY()
                    + getShadowGenerator().calculateOffsetY(), null);
            g2.drawImage(dataImage, (int) dataArea.getX(),
                    (int) dataArea.getY(), null);
        }

        //Added code:
        //get the correct crosshair position, and change label
        //without firing an event that lead to another call to draw()
        //which is bad/recursive
        double aX = getDomainCrosshairValue();
        double aY = getRangeCrosshairValue();
        //       System.out.println("after x: " + aX + "  y: " + aY);

        removeAnnotation(xyannot, false);
        removeAnnotation(pixelCoords, false);
        setCrosshairLabel(aX, aY);
        addAnnotation(xyannot, false);
        addAnnotation(pixelCoords, false);
        drawAnnotations(g2, dataArea, info);
        //end added code

        g2.setClip(originalClip);
        g2.setComposite(originalComposite);

        drawOutline(g2, dataArea);

    }

    /**
     * Set the line profile this XYPlot corresponds to. Needed for drawing the point on a MimsPlus which corresponds to
     * the current crosshair
     *
     * @param profile
     */
    public void setLineProfile(MimsLineProfile profile) {
        lineProfile = profile;
    }

    /**
     * Set the JFreeChart this XYPlot corresponds to. Needed for drawing the point on a MimsPlus which corresponds to
     * the current crosshair
     *
     * @param chart
     */
    public void setParent(MimsJFreeChart chart) {
        parent = chart;
    }

    //Set label
    public void setCrosshairLabel(double x, double y) {
        //      System.out.println("MimsXYPlot.setCrosshairLabel() called");
        if (isDomainCrosshairVisible() && isRangeCrosshairVisible() && this.crosshairLabelVisible) {
            double xmax = getDomainAxis().getUpperBound();
            double ymax = getRangeAxis().getUpperBound();
            DecimalFormat twoDForm = new DecimalFormat("#.##");
            String xhairlabel = "x = " + Double.valueOf(twoDForm.format(x)) + ", y = " + Double.valueOf(twoDForm.format(y));
            xyannot.setText(xhairlabel);
            xyannot.setX(xmax);
            xyannot.setY(ymax);
            xyannot.setTextAnchor(TextAnchor.TOP_RIGHT);
            //draw the point on a MimsPlus which corresponds to the crosshair
            int[] coords;
            if (lineProfile != null) {
                coords = lineProfile.addToOverlay();

            } else {
                coords = parent.addToOverlay(identifySeries(x, y), x, y);
            }
            if (coords != null) {
                pixelX = coords[0];
                pixelY = coords[1];
                pixelsSet = true;
                xhairlabel += "   pX = " + pixelX + ", pY = " + pixelY;
                xyannot.setText(xhairlabel);
            }
        } else {
            xyannot.setText("");
        }

    }
    //Set label

    public void setPixelCoords(int x, int y) {
        double xmax = getDomainAxis().getUpperBound();
        double ymax = getRangeAxis().getUpperBound();

    }

    public void showXHairLabel(boolean visible) {
        this.crosshairLabelVisible = visible;
        //setCrosshairLabel();
    }

    public boolean isCrosshairLabelVisible() {
        return crosshairLabelVisible;
    }

    public XYTextAnnotation getCrossHairAnnotation() {
        return xyannot;
    }

}

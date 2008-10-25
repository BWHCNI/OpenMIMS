package com.nrims;

import java.awt.Graphics;
import java.awt.geom.PathIterator;
import java.awt.Polygon;
import java.util.Hashtable;
import ij.gui.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;

/**
 * Extends ij.gui.ImageCanvas with utility to display all ROIs.
 * @author Douglas Benson
 * @author <a href="mailto:rob.gonzalez@gmail.com">Rob Gonzalez</a>
 */
public class MimsCanvas extends ij.gui.ImageCanvas {

    public static final long serialVersionUID = 1;

    public MimsCanvas(com.nrims.MimsPlus imp, com.nrims.UI ui) {
        super(imp);
        this.mImp = imp;
        this.ui = ui;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        // Check if the MimsRoiManager is open..
        if (MimsRoiManager.getInstance() != null) {
            drawOverlay(g);
        }
    }

    void drawOverlay(Graphics g) {
        MimsRoiManager roiManager = ui.getRoiManager();
        Hashtable rois = roiManager.getROIs();
        if (rois == null) {
            return;
        }
        if (rois.isEmpty()) {
            return;
        }
        if (roiManager.getShowAll() == false) {
            return;
        }

        g.setColor(Color.RED);
        Roi cRoi = mImp.getRoi();
        java.awt.List list = roiManager.getList();

        for (int id = 0; id < list.getItemCount(); id++) {
            Roi roi = (Roi) rois.get(list.getItem(id));
            boolean bDraw = true;
            if (cRoi != null && cRoi.toString().equals(roi.toString())) {
                bDraw = false;
            }
            if (bDraw) {
                switch (roi.getType()) {
                    case Roi.COMPOSITE:
                        roi.setImage(imp);
                        Color tmp = roi.getInstanceColor();
                        roi.setInstanceColor(Color.RED);
                        roi.draw(g);
                        roi.setInstanceColor(tmp);
                        break;
                    case Roi.FREELINE:
                    case Roi.FREEROI:
                    case Roi.OVAL:
                    case Roi.POLYGON:
                    case Roi.POLYLINE:
                    case Roi.RECTANGLE:
                    default:
                         {
                            Polygon p = roi.getPolygon();
                            PathIterator pi = p.getPathIterator(null);
                            int nc = 0;
                            float[] xys = new float[6];
                            int xn = 0, yn = 0, xp = 0, yp = 0, xi = 0, yi = 0;
                            int ct = 0;
                            while (!pi.isDone()) {
                                ct = pi.currentSegment(xys);
                                xn = screenX(Math.round(xys[0]));
                                yn = screenY(Math.round(xys[1]));
                                if (nc == 0) {
                                    xi = xn;
                                    yi = yn;
                                } else {
                                    g.drawLine(xp, yp, xn, yn);
                                }
                                xp = xn;
                                yp = yn;
                                pi.next();
                                nc++;
                            }
                            if (ct == pi.SEG_CLOSE) {
                                g.drawLine(xn, yn, xi, yi);
                            }
                        }
                        break;
                    case Roi.LINE:
                         {
                            Line lroi = (Line) roi;
                            int x1 = screenX(lroi.x1);
                            int x2 = screenX(lroi.x2);
                            int y1 = screenY(lroi.y1);
                            int y2 = screenY(lroi.y2);
                            g.drawLine(x1, y1, x2, y2);
                        }
                        break;
                    case Roi.POINT:
                         {
                            java.awt.Rectangle r = roi.getBounds();
                            int x1 = screenX(r.x);
                            int y1 = screenY(r.y);
                            g.drawLine(x1, y1 - 5, x1, y1 + 5);
                            g.drawLine(x1 - 5, y1, x1 + 5, y1);
                        }
                        break;
                }
            }
            String name = "" + (id + 1);
            java.awt.Rectangle r = roi.getBounds();
            int x = screenX(r.x + r.width / 2 - g.getFontMetrics().stringWidth(name) / 2);
            int y = screenY(r.y + r.height / 2 + g.getFontMetrics().getHeight() / 2);
            g.drawString(name, x, y);

            if (ui.activeRoi == roi) {
                if (roi.getType() == Roi.COMPOSITE) {
                    float[] array = ((ShapeRoi) roi).getShapeAsArray();
                    Shape s = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
                    int index = 0, type, len;
                    float[] seg = new float[7];
                    while (true) {
                        len = getSegment(array, seg, index);
                        if (len < 0) {
                            break;
                        }
                        index += len;
                        type = (int) seg[0];
                        switch (type) {
                            case PathIterator.SEG_MOVETO:
                                ((GeneralPath) s).moveTo(screenX((int) seg[1]), screenY((int) seg[2]));
                                break;
                            case PathIterator.SEG_LINETO:
                                ((GeneralPath) s).lineTo(screenX((int) seg[1]), screenY((int) seg[2]));
                                break;
                            case PathIterator.SEG_QUADTO:
                                ((GeneralPath) s).quadTo(screenX((int) seg[1]), screenY((int) seg[2]), screenX((int) seg[3]), screenY((int) seg[4]));
                                break;
                            case PathIterator.SEG_CUBICTO:
                                ((GeneralPath) s).curveTo(screenX((int) seg[1]), screenY((int) seg[2]), screenX((int) seg[3]), screenY((int) seg[4]), screenX((int) seg[5]), screenY((int) seg[6]));
                                break;
                            case PathIterator.SEG_CLOSE:
                                ((GeneralPath) s).closePath();
                                break;
                            default:
                                break;
                        }
                    }

                    Graphics2D g2 = (Graphics2D) g;
                    g2.fill(s);
                } else {
                    Polygon p = roi.getPolygon();
                    int[] screenXpoints = new int[p.npoints];
                    int[] screenYpoints = new int[p.npoints];
                    for (int i = 0; i < p.npoints; i++) {
                        screenXpoints[i] = screenX(p.xpoints[i]);
                        screenYpoints[i] = screenY(p.ypoints[i]);
                    }
                    Graphics2D g2 = (Graphics2D) g;
                    g2.fill(new Polygon(screenXpoints, screenYpoints, p.npoints));
                }
            }
        }
    }

    private int getSegment(float[] array, float[] seg, int index) {
        int len = array.length;
        if (index >= len) {
            return -1;
        }
        seg[0] = array[index++];
        int type = (int) seg[0];
        if (type == PathIterator.SEG_CLOSE) {
            return 1;
        }
        if (index >= len) {
            return -1;
        }
        seg[1] = array[index++];
        if (index >= len) {
            return -1;
        }
        seg[2] = array[index++];
        if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
            return 3;
        }
        if (index >= len) {
            return -1;
        }
        seg[3] = array[index++];
        if (index >= len) {
            return -1;
        }
        seg[4] = array[index++];
        if (type == PathIterator.SEG_QUADTO) {
            return 5;
        }
        if (index >= len) {
            return -1;
        }
        seg[5] = array[index++];
        if (index >= len) {
            return -1;
        }
        seg[6] = array[index++];
        if (type == PathIterator.SEG_CUBICTO) {
            return 7;
        }
        return -1;
    }
    private com.nrims.UI ui = null;
    private com.nrims.MimsPlus mImp;
}

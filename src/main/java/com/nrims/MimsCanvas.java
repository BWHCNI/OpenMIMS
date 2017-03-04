package com.nrims;

import ij.IJ;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.geom.PathIterator;
import java.util.Hashtable;
import ij.gui.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Extends ij.gui.ImageCanvas with utility to display all ROIs.
 *
 * @author Douglas Benson
 * @author <a href="mailto:rob.gonzalez@gmail.com">Rob Gonzalez</a>
 */
public class MimsCanvas extends ij.gui.ImageCanvas {

    public static final long serialVersionUID = 1;

    public MimsCanvas(MimsPlus imp, UI ui) {
        super(imp);
        this.mImp = imp;
        this.ui = ui;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        // Check if the MimsRoiManager is open..
        if (ui.getRoiManager().getInstance() != null) {
            drawOverlay(g);
        }
    }

    // Handles the drawing of Rois on the Canvas.
    void drawOverlay(Graphics g) {
        MimsRoiManager2 roiManager = ui.getRoiManager();
        Hashtable rois = roiManager.getROIs();
        boolean drawLabel = true;
        if (rois == null || rois.isEmpty() || roiManager.getHideRois()) {
            return;
        }

        // Is the hide labels box checked.
        if (roiManager.getHideLabel()) {
            drawLabel = false;
        }

        Roi cRoi = mImp.getRoi();
        javax.swing.JList list = roiManager.getList();

        int parentplane = 1;
        try {
            if (mImp.getMimsType() == MimsPlus.MASS_IMAGE) {
                parentplane = mImp.getCurrentSlice();
            } else if (mImp.getMimsType() == MimsPlus.RATIO_IMAGE) {
                parentplane = ui.getMassImages()[mImp.getRatioProps().getNumMassIdx()].getCurrentSlice();
            } else if (mImp.getMimsType() == MimsPlus.HSI_IMAGE) {
                parentplane = ui.getMassImages()[mImp.getHSIProps().getNumMassIdx()].getCurrentSlice();
            } else if (mImp.getMimsType() == MimsPlus.SUM_IMAGE) {
                if (mImp.getSumProps().getSumType() == SumProps.MASS_IMAGE) {
                    parentplane = ui.getMassImages()[mImp.getSumProps().getParentMassIdx()].getCurrentSlice();
                } else if (mImp.getSumProps().getSumType() == SumProps.RATIO_IMAGE) {
                    parentplane = ui.getMassImages()[mImp.getSumProps().getNumMassIdx()].getCurrentSlice();
                }

            } // DJ: 08/06/2014 Added to handle composite images
            else if (mImp.getMimsType() == MimsPlus.COMPOSITE_IMAGE) {
                for (int i = 0; i < mImp.getCompositeProps().getImages(ui).length; i++) {

                    MimsPlus channel = (mImp.getCompositeProps().getImages(ui))[i];

                    if (channel != null) {

                        if (channel.getMimsType() == MimsPlus.MASS_IMAGE) {
                            parentplane = channel.getCurrentSlice();
                            break;
                        } else if (channel.getMimsType() == MimsPlus.RATIO_IMAGE) {
                            parentplane = ui.getMassImages()[channel.getRatioProps().getNumMassIdx()].getCurrentSlice();
                            break;
                        } else if (channel.getMimsType() == MimsPlus.SUM_IMAGE) {
                            if (channel.getSumProps().getSumType() == SumProps.MASS_IMAGE) {
                                parentplane = ui.getMassImages()[channel.getSumProps().getParentMassIdx()].getCurrentSlice();
                                break;
                            } else if (channel.getSumProps().getSumType() == SumProps.RATIO_IMAGE) {
                                parentplane = ui.getMassImages()[channel.getSumProps().getNumMassIdx()].getCurrentSlice();
                                break;
                            }
                        }
                    }
                }
            }

        } catch (NullPointerException npe) {
            npe.printStackTrace();
            // Do nothing, assume plane 1.
        }

        // Loop over the Rois and draw each one.
        for (int id = 0; id < list.getModel().getSize(); id++) {
            String label = (list.getModel().getElementAt(id).toString());
            Roi roi = (Roi) rois.get(label);
            roi.setImage(imp);
            Integer[] xy = roiManager.getRoiLocation(label, parentplane);
            if (xy != null && roi.getType() != Roi.LINE) {
                roi.setLocation(xy[0], xy[1]);
            }

            // Set the color.
            Color color;
            if (roiManager.isSelected(label)) {
                color = Color.GREEN;
            } else if (mImp.getMimsType() == MimsPlus.HSI_IMAGE || 
                    mImp.getMimsType() == MimsPlus.COMPOSITE_IMAGE || 
                    mImp.getMimsType() == MimsPlus.SEG_IMAGE) {
                color = Color.WHITE;
            } else {
                color = Color.RED;
            }
            roi.setStrokeColor(color);
            g.setColor(color);

            // Draw the Label.
            if (drawLabel) {
                String name = label;
                Rectangle r = roi.getBounds();
                int x = screenX(r.x + r.width / 2);
                int y = screenY(r.y + r.height / 2);
                g.drawString(name, x, y);
            }

            // We dont want to show the boundry if the mouse is within the roi.
            boolean bDraw = true;
            if (cRoi != null && cRoi.toString().equals(roi.toString())) {
                bDraw = false;
            }

            // Draw the Roi.
            if (bDraw) {
                roi.setStrokeWidth(0.0);
                roi.drawOverlay(g);
            }
        }
    }

    @Override
    protected void handlePopupMenu(MouseEvent e) {
        String[] tileList = ui.getOpener().getTilePositions();
        String[] stackList = ui.getOpener().getStackPositions();
        ReportGenerator rg = ui.getReportGenerator();
        if (rg != null && rg.isVisible()) {
            int x = e.getX();
            int y = e.getY();
            JPopupMenu popup = new JPopupMenu();
            int mX = offScreenX(x);
            int mY = offScreenY(y);
            JMenuItem addImage = new JMenuItem("add to report");
            addImage.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    Image im = ui.getScreenCaptureCurrentImage();
                    if (im != null) {
                        String text = "";
                        if (mImp.getMimsType() == MimsPlus.MASS_IMAGE) {
                            text += "mass ";
                        }
                        text += mImp.getRoundedTitle();
                        text += mImp.getPropsTitle();
                        ui.getReportGenerator().addImage(im, text);
                    }
                }
            });
            popup.add(addImage);
            popup.show(this, x, y);
        } else if ((ui.getOpener().getTilePositions() == null || tileList.length == 0) && (ui.getOpener().getStackPositions() == null || stackList.length == 0)) {
            super.handlePopupMenu(e);
        } else {
            int x = e.getX();
            int y = e.getY();
            JPopupMenu popup = new JPopupMenu();
            int mX = offScreenX(x);
            int mY = offScreenY(y);
            String tileName;
            JMenuItem openTile;
            if (ui.getOpener().getTilePositions() != null && tileList.length != 0) {
                tileName = getClosestTileName(mX, mY);
                openTile = new JMenuItem("open tile: " + tileName);
                openTile.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {

                        AbstractButton aButton = (AbstractButton) event.getSource();
                        String nrrdFileName = aButton.getActionCommand();

                        // Look for nrrd file.
                        final Collection<File> all = new ArrayList<File>();
                        addFilesRecursively(new File(ui.getImageDir()), nrrdFileName, all);

                        // If unable to find nrrd file, look for im file.
                        String imFileName = null;
                        if (all.isEmpty()) {
                            imFileName = nrrdFileName.substring(0, nrrdFileName.lastIndexOf(".")) + UI.MIMS_EXTENSION;
                            addFilesRecursively(new File(ui.getImageDir()), imFileName, all);
                        }

                        // Open a new instance of the plugin.
                        if (all.isEmpty()) {
                            IJ.error("Unable to locate " + nrrdFileName + " or " + imFileName);
                        } else {
                            UI ui_tile = new UI();
                            ui_tile.setLocation(ui.getLocation().x + 35, ui.getLocation().y + 35);
                            ui_tile.setVisible(true);
                            ui_tile.openFile(((File) all.toArray()[0]));
                            if (mImp.getMimsType() == MimsPlus.HSI_IMAGE) {
                                HSIProps tileProps = mImp.getHSIProps().clone();
                                tileProps.setNumMassValue(ui.getMassValue(tileProps.getNumMassIdx()));
                                tileProps.setDenMassValue(ui.getMassValue(tileProps.getDenMassIdx()));
                                tileProps.setXWindowLocation(MouseInfo.getPointerInfo().getLocation().x);
                                tileProps.setYWindowLocation(MouseInfo.getPointerInfo().getLocation().y);
                                HSIProps[] hsiProps = {tileProps};

                                // DJ: 1st "null" added to represent the new mass_props arg that we added to ui.restoreState
                                // DJ: 4th "null" added to represent the new compsite_props arg that we added to ui.restoreState
                                //TODO: explain to DJ
                                ui_tile.restoreState(null, null, hsiProps, null, null, false, false);

                                ui_tile.getHSIView().useSum(ui.getIsSum());
                                ui_tile.getHSIView().medianize(ui.getMedianFilterRatios(), ui.getMedianFilterRadius());
                            }
                        }
                    }
                });
            } else {
                int sliceIndex = mImp.getCurrentSlice();
                tileName = ui.getOpener().getStackPositions()[sliceIndex - 1];
                openTile = new JMenuItem("open slice: " + tileName);
                openTile.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {

                        AbstractButton aButton = (AbstractButton) event.getSource();
                        String nrrdFileName = aButton.getActionCommand();

                        // Look for nrrd file.
                        final Collection<File> all = new ArrayList<File>();
                        addFilesRecursively(new File(ui.getImageDir()), nrrdFileName, all);

                        // If unable to find nrrd file, look for im file.
                        String imFileName = null;
                        if (all.isEmpty()) {
                            imFileName = nrrdFileName.substring(0, nrrdFileName.lastIndexOf(".")) + UI.MIMS_EXTENSION;
                            addFilesRecursively(new File(ui.getImageDir()), imFileName, all);
                        }

                        // Open a new instance of the plugin.
                        if (all.isEmpty()) {
                            IJ.error("Unable to locate " + nrrdFileName + " or " + imFileName);
                        } else {
                            UI ui_tile = new UI();
                            ui_tile.setLocation(ui.getLocation().x + 35, ui.getLocation().y + 35);
                            ui_tile.setVisible(true);
                            ui_tile.openFile(((File) all.toArray()[0]));
                        }
                    }
                });
            }
            openTile.setActionCommand(tileName);
            popup.add(openTile);
            popup.show(this, x, y);
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

    public String getClosestTileName(int x, int y) {

        // Get the mouse x,y position in machine coordinates.
        double x_center_pos, y_center_pos;
        double width = ui.getOpener().getWidth();
        double height = ui.getOpener().getHeight();
        double pixel_width = ui.getOpener().getPixelWidth() / 1000;
        double pixel_height = ui.getOpener().getPixelHeight() / 1000;
        try {
            x_center_pos = new Integer(ui.getOpener().getPosition().split(",")[1]);
            if (ui.getOpener().isPrototype()) {
                x_center_pos = (-1) * x_center_pos;
            }
            y_center_pos = new Integer(ui.getOpener().getPosition().split(",")[0]);

        } catch (NumberFormatException nfe) {
            IJ.error("Unable to convert to X, Y coordinates: " + ui.getOpener().getPosition());
            return null;
        }

        long x_mouse_pos = Math.round(x_center_pos - ((width / 2.0) - (double) x) * pixel_width);
        long y_mouse_pos = -1 * Math.round(y_center_pos - ((height / 2.0) - (double) y) * pixel_height);

        // Find the nearest neighbor.
        String nearestNeighbor = null;
        String tileName = null;
        double nearest_distance = Double.MAX_VALUE;
        double distance = Double.MAX_VALUE;
        int x_tile_pos, y_tile_pos;
        String[] tileList = ui.getOpener().getTilePositions();
        for (String tile : tileList) {
            String[] params = tile.split(",");
            if (params != null && params.length > 0) {
                try {
                    tileName = params[0];
                    x_tile_pos = new Integer(params[2]);
                    if (ui.getOpener().isPrototype()) {
                        x_tile_pos = (-1) * x_tile_pos;
                    }
                    y_tile_pos = (-1) * (new Integer(params[1]));
                    distance = Math.sqrt(Math.pow(x_mouse_pos - x_tile_pos, 2) + Math.pow(y_mouse_pos - y_tile_pos, 2));
                } catch (Exception e) {
                    System.out.println("Unable to convert to X, Y coordinates: " + tile);
                    continue;
                }
            }
            if (distance < nearest_distance) {
                nearest_distance = distance;
                nearestNeighbor = tileName;
            }
        }
        return nearestNeighbor;
    }

    /**
     * A specially designed method designed to search recursively under directory <code>dir</code> for a file named
     * <code>name</code>. Once a file is found that matches, stop the recursive search.
     *
     * @param dir - the root directory.
     * @param name - the name to search for.
     * @param all - the collection of files that match name (not supported, returns first match).
     */
    private static void addFilesRecursively(File dir, String name, Collection<File> all) {
        if (all.size() > 0) {
            return;
        }
        final File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.getName().matches(name)) {
                    all.add(child);
                    return;
                }
                addFilesRecursively(child, name, all);
            }
        }
    }

    private com.nrims.UI ui = null;
    private com.nrims.MimsPlus mImp;
}

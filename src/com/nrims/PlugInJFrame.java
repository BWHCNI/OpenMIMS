/*
 * PlugInJFrame.java
 *
 * Created on May 1, 2006, 12:40 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.nrims;

import javax.swing.JFrame;
import java.awt.*;
import java.awt.event.*;
import ij.*;
import ij.plugin.*;

/**
 *
 * @author Douglas Benson
 */
public class PlugInJFrame extends JFrame implements PlugIn, WindowListener, FocusListener {

    public static final long serialVersionUID = 1;
    /** Creates a new instance of PlugInJFrame */
    String title;

    public PlugInJFrame(String title) {
        super(title);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        this.title = title;
        ImageJ ij = IJ.getInstance();
        if (ij != null) {
            Image img = ij.getIconImage();
            if (img != null) {
                try {
                    setIconImage(img);
                } catch (Exception e) {
                }
            }
        }
        if (IJ.debugMode) {
            IJ.log("opening " + title);
        }
    }

    @Override
    public void run(String arg) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        if (e.getSource() == this) {
            close();
        }
    }

    /** Closes this window. */
    public void close() {
        setVisible(false);
        dispose();
        WindowManager.removeWindow(this);
    }

    @Override
    public void windowActivated(WindowEvent e) {
        if (IJ.isMacintosh() && IJ.getInstance() != null) {
            IJ.wait(10); // needed for 1.4 on OS X
            setMenuBar(Menus.getMenuBar());
        }
        WindowManager.setWindow(this);
    }

    @Override
    public void focusGained(FocusEvent e) {
        //IJ.log("PlugInFrame: focusGained");
        WindowManager.setWindow(this);
    }

    @Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowDeactivated(WindowEvent e) {}

    @Override
    public void focusLost(FocusEvent e) {}
}

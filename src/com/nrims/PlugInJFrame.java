/*
 * PlugInJFrame.java
 *
 * Created on May 1, 2006, 12:40 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.nrims;

import javax.swing.JFrame ;
import java.awt.*;
import java.awt.event.*;
import ij.*;
import ij.plugin.*;

/**
 *
 * @author Douglas Benson
 */
public class PlugInJFrame extends JFrame implements  PlugIn, WindowListener, FocusListener  {
    
    /** Creates a new instance of PlugInJFrame */
    String title;

    public PlugInJFrame(String title) {
        super(title);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        this.title = title;
        ImageJ ij = IJ.getInstance();
        if (ij!=null) {
                Image img = ij.getIconImage();
                if (img!=null)
                        try {setIconImage(img);} catch (Exception e) {}
        }
        if (IJ.debugMode) IJ.log("opening "+title);
    }
	
    public void run(String arg) {
    }
	
    public void windowClosing(WindowEvent e) {
    	if (e.getSource()==this)
    		close();
    }
    
    /** Closes this window. */
    public void close() {
        setVisible(false);
        dispose();
        WindowManager.removeWindow(this);
    }

    public void windowActivated(WindowEvent e) {
        if (IJ.isMacintosh() && IJ.getInstance()!=null) {
                IJ.wait(10); // needed for 1.4 on OS X
                setMenuBar(Menus.getMenuBar());
        }
        WindowManager.setWindow(this);
    }

    public void focusGained(FocusEvent e) {
        //IJ.log("PlugInFrame: focusGained");
        WindowManager.setWindow(this);
    }

    public void windowOpened(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void focusLost(FocusEvent e) {}
}

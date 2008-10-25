package com.nrims;

/*
 * NRIMS.java
 *
 * Created on May 1, 2006, 12:34 PM
 */

import ij.plugin.PlugIn;

import com.nrims.UI;

/**
 * NRIMS Analysis Module
 * 
 * @author Douglas Benson
 * @author <a href="mailto:rob.gonzalez@gmail.com">Rob Gonzalez</a>
 */
public class NRIMS implements PlugIn {

    /** Singleton instance of the NRIMS UI. */
    private static UI ui = new UI(null);

    /**
     * @return the singleton instance of the NRIMS UI.
     */
    public static UI getUI() {
        return ui;
    }

    /**
     * Private to prevent instantiation beyond the singleton class object.
     * @see #getUI() 
     */
    private NRIMS() {
    }

    @Override
    public void run(String arg) {
        String options = ij.Macro.getOptions();
        ui.run(arg + options);
        if (!ui.isVisible()) {
            ui.setVisible(true);
        }
    }
}

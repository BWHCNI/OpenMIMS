/*
 * NRIMS.java
 *
 * Created on May 1, 2006, 12:34 PM
 * 
 * @author Douglas Benson
 * @author <a href="mailto:rob.gonzalez@gmail.com">Rob Gonzalez</a>
 */

import ij.plugin.PlugIn;

import com.nrims.UI;

/**
 * NRIMS Analysis Module
 */
public class NRIMS implements PlugIn {

    /** Singleton instance of the NRIMS UI. */
    private static com.nrims.UI ui = new com.nrims.UI(null);

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

    public void run(String arg) {
        String options = ij.Macro.getOptions();
        ui.run(arg + options);
        if (!ui.isVisible()) {
            ui.setVisible(true);
        }
    }
}

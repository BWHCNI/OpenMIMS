/*
 * NRIMS.java
 *
 * Created on May 1, 2006, 12:34 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author Douglas Benson
 */

import ij.plugin.PlugIn ;

public class NRIMS implements PlugIn {
    
    /** Creates a new instance of NRIMS Analysis Module */
    
    public NRIMS() {
        if(ui == null) ui = new com.nrims.UI(null);
    }

    public static com.nrims.UI getUI(){ return ui ; } 
    
    public void run(String arg) {      
        String options = ij.Macro.getOptions();            
        ui.run(arg + options);
        if(ui.isVisible() == false) ui.setVisible(true);
    }
    
    private static com.nrims.UI ui = null ;
 }

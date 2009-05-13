package com.nrims;

//do not define as part of com.nrims

/*
 * NRIMS_Plugin.java
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

public class NRIMS_Plugin implements PlugIn {
    
    /** Creates a new instance of NRIMS_Plugin Analysis Module */
    
    //must be puclic
    public NRIMS_Plugin() {
        System.out.println("NRIMS constructor");
        if(ui == null) ui = new com.nrims.UI(null);
    }

    public static com.nrims.UI getUI(){ return ui ; } 
    
    @Override
    public void run(String arg) {
        System.out.println("NRIMS.run");
        String options = ij.Macro.getOptions();            
        ui.run(arg + options);
        if(ui.isVisible() == false) ui.setVisible(true);
    }
    
    private static com.nrims.UI ui = null ;
 }

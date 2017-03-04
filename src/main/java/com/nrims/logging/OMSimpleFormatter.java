/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.logging;

import java.util.logging.*;

/**
 *
 * @author cpoczatek
 */
public class OMSimpleFormatter extends SimpleFormatter {

    @Override
    public String format(LogRecord rec) {
        String message = "OpenMIMS: ";
        message += "[" + rec.getLevel().toString() + "] ";
        message += rec.getMessage() + " : ";
        message += rec.getSourceClassName() + " ";
        message += rec.getSourceMethodName();
        message += "\n";
        return message;
    }

}

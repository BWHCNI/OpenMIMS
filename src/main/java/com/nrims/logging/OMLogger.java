/*
 * OMLogger, added real logging to OpenMIMS. Currently at prototyping stage.
 * Log level is fixed at FINE, and all messages go to stderr.
 * In the future level will be tuneable, and possibly loging to a file.
 * 
 */
package com.nrims.logging;

import java.util.logging.*;

/**
 *
 * @author cpoczatek
 */
public class OMLogger {

    public static Logger getOMLogger(String name) {
        Logger logger = Logger.getLogger(name);

        //do not pass log messages up to parent handlers
        logger.setUseParentHandlers(false);

        // LOG this level to the log
        logger.setLevel(Level.FINE);

        //create custom formater, ConsoleHandler, and add to logger
        OMSimpleFormatter formatter = new OMSimpleFormatter();
        ConsoleHandler consoleHandler = new ConsoleHandler();

        // PUBLISH this level
        consoleHandler.setLevel(Level.FINE);

        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);

        return logger;
    }
}

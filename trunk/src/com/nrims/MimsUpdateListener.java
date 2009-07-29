/*
 * mimsUpdateListener.java
 *
 * Created on May 3, 2006, 12:45 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.nrims;

/**
 *
 * @author Douglas Benson
 */
public interface MimsUpdateListener extends java.util.EventListener {    
    void mimsStateChanged( MimsPlusEvent evt );
}

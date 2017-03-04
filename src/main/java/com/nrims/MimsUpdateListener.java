package com.nrims;

/**
 * An extension of the EventListenener class, implemented by the UI class.
 *
 * @author Douglas Benson
 */
public interface MimsUpdateListener extends java.util.EventListener {

    void mimsStateChanged(MimsPlusEvent evt);
}

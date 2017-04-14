/*
 * MIMSFileFilter.java
 *
 * Created on May 1, 2006, 1:17 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.nrims.data;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.filechooser.FileFilter;

/**
 * A convenience implementation of FileFilter that filters out all files except for those type extensions that it knows
 * about.
 *
 * Extensions are of the type ".foo", which is typically found on Windows and Unix boxes, but not on Macinthosh. Case is
 * ignored.
 */
public class MIMSFileFilter extends FileFilter {

    private static String TYPE_UNKNOWN = "Type Unknown";
    private static String HIDDEN_FILE = "Hidden File";

    private Hashtable filters = null;
    private String description = null;
    private String fullDescription = null;
    private boolean useExtensionsInDescription = true;

    /**
     * Creates a file filter. If no filters are added, then all files are accepted.
     *
     * @param extension name of the file filter extension
     * @see #addExtension
     */
    public MIMSFileFilter(String extension) {
        this.filters = new Hashtable();
        addExtension(extension);
        setDescription("MIMS Format images");
    }

    /**
     * Creates a file filter that accepts files with the given extension. Example: new MIMSFileFilter("jpg");
     *
     * @see #addExtension
     */
    /**
     * Return true if this file should be shown in the directory pane, false if it shouldn't.
     *
     * Files that begin with "." are ignored.
     *
     * @see #getExtension
     */
    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            String extension = getExtension(f);
            if (extension != null && filters.get(getExtension(f)) != null) {
                return true;
            } else if ((extension = getDoubleExtension(f)) != null && filters.get(getDoubleExtension(f)) != null) {
                System.out.println(f.getName() + " true");
                return true;
            };
        }
        return false;
    }

    /**
     * Return the extension portion of the file's name .
     *
     * @param f a file reference
     * @see #getExtension
     * @see FileFilter#accept
     * 
     * @return the extension of the supplied file name, or null if file is null
     */
    public String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            };
        }
        return null;
    }

    /**
     * Return the double extension portion of the file's name .
     *
     * @param f a file reference
     * @see #getExtension
     * @see FileFilter#accept
     * 
     * @return the double extension of the supplied file name, or null if file is null
     */
    public String getDoubleExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            i = filename.lastIndexOf('.', i - 1);
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();

            };
        }
        return null;
    }

    public void addExtension(String extension) {
        if (filters == null) {
            filters = new Hashtable();
        }
        filters.put(extension.toLowerCase(), this);
        fullDescription = null;
    }

    /**
     * Returns the human readable description of this filter.
     */
    public String getDescription() {
        if (fullDescription == null) {
            if (description == null || isExtensionListInDescription()) {
                fullDescription = description == null ? "(" : description + " (";
                // build the description from the extension list
                Enumeration extensions = filters.keys();
                if (extensions != null) {
                    fullDescription += "." + (String) extensions.nextElement();
                    while (extensions.hasMoreElements()) {
                        fullDescription += ", ." + (String) extensions.nextElement();
                    }
                }
                fullDescription += ")";
            } else {
                fullDescription = description;
            }
        }
        return fullDescription;
    }

    /**
     * Sets the human readable description of this filter. For example: filter.setDescription("Gif and JPG Images");
     *
     * @param description  descriptionTodo
     * @see setDescription
     * @see setExtensionListInDescription
     * @see isExtensionListInDescription
     */
    public void setDescription(String description) {
        this.description = description;
        fullDescription = null;
    }

    /**
     * Determines whether the extension list (.jpg, .gif, etc) should show up in the human readable description.
     *
     * Only relevant if a description was provided in the constructor or using setDescription();
     *
     * @param b descriptionTodo
     * @see getDescription
     * @see setDescription
     * @see isExtensionListInDescription
     */
    public void setExtensionListInDescription(boolean b) {
        useExtensionsInDescription = b;
        fullDescription = null;
    }

    /**
     * Returns whether the extension list (.jpg, .gif, etc) should show up in the human readable description.
     *
     * Only relevent if a description was provided in the constructor or using setDescription();
     *
     * @return useExtensionsInDescription descriptionTodo
     * @see getDescription
     * @see setDescription
     * @see setExtensionListInDescription
     */
    public boolean isExtensionListInDescription() {
        return useExtensionsInDescription;
    }
}

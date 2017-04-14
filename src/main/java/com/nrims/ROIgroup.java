/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims;

/**
 * ROIgroup class describes ROIs, including the group to which an ROI belongs,
 * its type, and its tag name
 * 
 * @author taylorwrt
 */

public class ROIgroup implements java.io.Serializable {
    private String groupName;
    private String groupType;
    private String tagName;
    //private String[] roiNumbers;

    public ROIgroup(String groupName, String groupType, String tagName) {
        this.groupName = groupName;
        this.groupType = groupType;
        this.tagName = tagName;
        //this.roiNumbers = roiNumbers;
    }
    
     /**
     * Get the name of the group to which this ROI belongs.
     *
     * @return groupName the group name to which this ROI belongs
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Set the name of the group to which this ROI belongs.
     *
     * @param groupName the group name to which this ROI belongs
     */
    public void setGroupName(String groupName) {
        groupName = groupName;
    }

    /**
     * Get the type of the group to which this ROI belongs.
     *
     * @return groupType the type to which this ROI belongs
     */
    public String getGroupType() {
        return groupType;
    }
    
    /**
     * Set the name of the group to which this ROI belongs.
     *
     * @param type the type name to which this ROI belongs
     */
    public void setGroupType(String type) {
        groupType = type;
    }
    
    /**
     * Set the name of the tag to which this ROI belongs.
     *
     * @param tag the tag name to which this ROI belongs
     */
    public void setTagName(String tag) {
        tagName = tag;
    }
    
    /**
     * Get the name of the tag to which this ROI belongs.
     *
     * @return tagName the type to which this ROI belongs
     */
    public String getTagName() {
        return tagName;
    }
    
//    public String[] getROIs() {
//        return roiNumbers;
//    }
}
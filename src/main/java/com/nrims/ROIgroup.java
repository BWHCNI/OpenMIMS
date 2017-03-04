/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims;

/**
 *
 * @author taylorwrt
 */

public class ROIgroup implements java.io.Serializable {
    private String groupName;
    private String groupType;
    //private String[] roiNumbers;

    public ROIgroup(String groupName, String groupType) {
        this.groupName = groupName;
        this.groupType = groupType;
        //this.roiNumbers = roiNumbers;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String name) {
        groupName = name;
    }

    public String getGroupType() {
        return groupType;
    }
    
    public void setGroupType(String type) {
        groupType = type;
    }
    
//    public String[] getROIs() {
//        return roiNumbers;
//    }
}
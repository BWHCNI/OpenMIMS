/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims; 

import java.io.*;
/**
 *
 * @author cpoczatek
 */
public class ThreadSleep extends Thread {
    long sleeptime = 200000;
    
    
    
    public void run() {
        System.out.println("Sleep for 200sec");
        try { this.sleep(sleeptime); } catch(Exception e) { System.out.println("sleep exception"); }
        System.out.println("Wake up!");
        
        
        System.out.print("Enter something: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String userName = null;
        try {
            userName = br.readLine();
        } catch (IOException ioe) {
            System.out.println("IO error trying to read your name!");
        }
        
        //System.out.println("Sleep for a long time");
        //try { this.sleep((long)99999999); } catch(Exception e) { System.out.println("sleep exception"); }
        
    }
}


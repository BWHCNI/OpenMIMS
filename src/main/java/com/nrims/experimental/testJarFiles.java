/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.experimental;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 *
 * @author cpoczatek
 *
 * This is totally unneeded if running in fiji w/ an update site. Should
 * delete...
 *
 */
public class testJarFiles {

    public void testJarsExist() {
        String cp = System.getProperty("java.class.path");
        System.out.println(cp + "\n\n");
        String[] parts = cp.split(":");
        ArrayList<String> jarfiles = new ArrayList<String>();

        for (int i = 0; i < parts.length; i++) {
            jarfiles.add(parts[i].substring(parts[i].lastIndexOf(java.io.File.separator) + 1, parts[i].length()));
        }

        System.out.println("jarfiles:");
        for (int i = 0; i < parts.length; i++) {
            System.out.println(jarfiles.get(i));
        }
        System.out.println("");

        ArrayList<String> libfiles = new ArrayList<String>();
        try {
            InputStream lib = getClass().getResourceAsStream("/liblist.txt");
            InputStreamReader libr = new InputStreamReader(lib);
            BufferedReader br = new BufferedReader(libr);
            String line = "";
            while ((line = br.readLine()) != null) {
                if (line.contains(".jar")) {
                    libfiles.add(line.trim());
                }
            }
            br.close();
            libr.close();
            lib.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("libfiles: ");
        for (int i = 0; i < libfiles.size(); i++) {
            System.out.println(libfiles.get(i));
        }

        boolean error = false;
        for (int i = 0; i < libfiles.size(); i++) {
            if (!jarfiles.contains(libfiles.get(i))) {
                error = true;
            }
        }
        System.out.println("jar error: " + error);
    }
}

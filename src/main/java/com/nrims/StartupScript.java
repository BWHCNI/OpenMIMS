package com.nrims;

import java.io.*;
import java.util.ArrayList;
import javax.swing.*;

public class StartupScript extends Thread {

    UI ui;
    BufferedReader br;
    DataInputStream in;
    File trackingFile;
    JLabel label = new JLabel("");
    File imFile = null;
    RatioProps[] rto_props;
    HSIProps[] hsi_props;
    SumProps[] sum_props;

    public StartupScript(UI ui) {
        this.ui = ui;
    }

    // This method is called when the thread runs
    public void run() {
        MimsJTable table = new MimsJTable(ui);

        // Open image file.
        ui.openFile(new File("/nrims/home3/zkaufman/Images/test_file.nrrd"));

        // Open roi file.
        ui.openFile(new File("/nrims/home3/zkaufman/Images/test_file.rois.zip"));
        table.setRois(ui.getRoiManager().getAllROIs());

        // Set planes.
        ArrayList planes = ui.getMimsTomography().getPlanes();
        table.setPlanes(planes);

        // Set stats.
        String[] statnames = {"area", "group (table only)", "mean", "sum"};
        table.setStats(statnames);

        // Set images.
        SumProps sumProps0 = new SumProps(0);
        SumProps sumProps1 = new SumProps(1);
        MimsPlus sp0 = new MimsPlus(ui, sumProps0, null);
        MimsPlus sp1 = new MimsPlus(ui, sumProps1, null);
        sp0.showWindow();
        sp1.showWindow();
        MimsPlus[] images = new MimsPlus[2];
//       images[0] = ui.getOpenMassImages()[0];
//       images[1] = ui.getOpenMassImages()[1];
        images[0] = sp0;
        images[1] = sp1;
        table.setImages(images);

        // Generate table.
        table.createSumTable(false);
//       table.createTable(false);
        table.showFrame();

    }
}

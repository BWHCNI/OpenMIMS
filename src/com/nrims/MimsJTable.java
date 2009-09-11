package com.nrims;

import ij.IJ;
import ij.gui.Roi;
import ij.process.ImageStatistics;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * @author zkaufman
 */
public class MimsJTable {

   JTable table;
   String[] stats;
   MimsPlus images[];
   Roi[] rois;
   Object[][] data;
   ArrayList planes;
   JFrame frame;

   public MimsJTable() {}

   public void createTable(boolean appendResults) {

      // Cant append if frame doesnt exist
      if (frame == null) {
         appendResults = false;
      } else if (!frame.isVisible()) {
         appendResults = false;
      }

      // If attempting to append, make sure number of column match.
      if (appendResults && data != null) {
         appendResults = tableColumnsMatch();
      }

      // Appending results.
      if (appendResults && data != null) {
         Object[][] newData = getDataSet();
         for (int i = 0; i < newData.length; i++) {
            TableModel tm = table.getModel();
            DefaultTableModel model = (DefaultTableModel) tm;
            model.addRow(newData[i]);
            model.setColumnIdentifiers(getColumnNames());
            int width = 100;
            for (int ii = 0; ii < getColumnNames().length; ii++) {
               TableColumn col = table.getColumnModel().getColumn(ii);
               col.setMinWidth(width);
               col.setPreferredWidth(width);
            }
         }
      } else if (rois.length == 0 || images.length == 0 || stats.length == 0) {
         return;
      } else {

         // Create frame.
         frame = new JFrame("Measure");
         data = getDataSet();
         String[] columnNames = getColumnNames();

         int width = 100;
         DefaultTableModel tm = new DefaultTableModel(data, columnNames);
         table = new JTable(tm);
         for (int i = 0; i < columnNames.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setMinWidth(width);
            col.setPreferredWidth(width);
         }

         //Create the scroll pane and add the table to it.
         JScrollPane scrollPane = new JScrollPane(table);
         table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
         scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

         frame.setContentPane(scrollPane);
         frame.setSize(600, 400);
         //Display the window.
         frame.pack();
      }
   }

   public Object[][] getDataSet() {

      // initialize variables.
      ImageStatistics tempstats = null;
      int currentSlice = images[0].getCurrentSlice();
      Object[][] data = new Object[planes.size()][rois.length * images.length * stats.length + 1];

      // Fill in "slice" field.
      for (int ii = 0; ii < planes.size(); ii++) {
         data[ii][0] = planes.get(ii);
      }   

      // Fill in data.
      for (int ii = 0; ii < planes.size(); ii++) {
         int col = 1;
         images[0].setSlice((Integer)planes.get(ii));
         for (int i = 0; i < rois.length; i++) {
            for (int j = 0; j < images.length; j++) {
               for (int k = 0; k < stats.length; k++) {
                  Roi tempRoi = rois[i];
                  images[j].setRoi(tempRoi);
                  tempstats = images[j].getStatistics();                  
                  data[ii][col] = IJ.d2s(MimsJFreeChart.getSingleStat(tempstats, stats[k]), 2);
                  col++;
               }
            }
         }
      }
      images[0].setSlice(currentSlice);

      return data;
   }

   public String[] getColumnNames(){
      String[] columnNames = new String[rois.length * images.length * stats.length + 1];
      columnNames[0] = "slice";

      int col = 1;
      for (int i = 0; i < rois.length; i++) {
         for (int j = 0; j < images.length; j++) {
            for (int k = 0; k < stats.length; k++) {
               columnNames[col] = stats[k] + "_m" + images[j].getRoundedTitle() + "_" + rois[i].getName();
               col++;
            }
         }
      }

      return columnNames;
   }

   public boolean tableColumnsMatch() {

      String[] columnNames = getColumnNames();

      int numCol1 = columnNames.length;
      int numCol2 = table.getColumnCount();

      if (numCol1 != numCol2) {
         return false;
      }

      return true;
   }

   public void setImages(MimsPlus[] images){
      this.images = images;
   }

   public void setStats(String[] stats) {
      this.stats = stats;
   }

   public void setRois(Roi[] rois) {
      this.rois = rois;
   }

   public void showFrame() {
      if (frame != null) {
         frame.setVisible(true);
         frame.toFront();
      }
   }

   public void close() {
    	data = null;
      if (frame != null)
         frame.setVisible(false);
   }

   void setPlanes(ArrayList planes) {
      this.planes = planes;
   }

}

/*
 * SVMTestGUI.java
 *
 * Created on June 23, 2008, 4:13 PM
 */

package SVM;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.CellEditor;
import javax.swing.JFileChooser;
import javax.swing.ListModel;
import javax.swing.table.TableModel;

/**
 *
 * @author  pgormanns
 */
public class SVMTestGUI extends javax.swing.JFrame {

//   protected String[] suffixList = new String[61];
//   protected Hashtable tokens = new Hashtable();
   protected Hashtable nameSuffix = new Hashtable();
   protected Hashtable globalPositions = new Hashtable();
//   final int mass1 = 3; 
//   final int mass2 = 7; 
//   final int mass3 = 11; 
//   final int mass4 = 15;
//   final int mean1 = 4; 
//   final int mean2 = 8;
//   final int mean3 = 12;
//   final int mean4 = 16;
//   final int vari1 = 5;
//   final int vari2 = 9;
//   final int vari3 = 13;
//   final int vari4 = 7;
//   final int grad1 = 6;
//   final int grad2 = 10;
//   final int grad3 = 14;
//   final int grad4 = 18;
//   final int rat1312 = 19;
//   final int rat2726 = 23;
//   final int rMean1312 = 20;
//   final int rMean2726 = 24;
//   final int rVari1312 = 21;
//   final int rVari2726 = 25;
//   final int rGrad1312 = 22;
//   final int rGrad2726 = 26;
   String massSetup;
   private File myFile;
   
   /** Creates new form SVMTestGUI */
    public SVMTestGUI() {
        initComponents();
        massSetup = "K";
        globalPositions.put("1A", new int[] {3, 7, 11, 15});
        globalPositions.put("1B", new int[] {3, 7, 11});
        globalPositions.put("1C", new int[] {3, 7, 15});
        globalPositions.put("1D", new int[] {3, 11, 15});
        globalPositions.put("1E", new int[] {7, 11, 15});
        globalPositions.put("1F", new int[] {3, 7});
        globalPositions.put("1G", new int[] {3, 11});
        globalPositions.put("1H", new int[] {3, 15});
        globalPositions.put("1I", new int[] {7, 11});
        globalPositions.put("1J", new int[] {7, 15});
        globalPositions.put("1K", new int[] {11, 15});
        globalPositions.put("1L", new int[] {3});
        globalPositions.put("1M", new int[] {5});
        globalPositions.put("1N", new int[] {7});
        globalPositions.put("1O", new int[] {11});
        globalPositions.put("2", new int[]{1, 2});
        globalPositions.put("3", new int[]{4, 5, 8, 9 , 12, 13, 16, 17});
        globalPositions.put("4", new int[]{6, 10,14, 18}); 
        
        //ratio
        globalPositions.put("5", new int[]{19, 20, 21, 22});
        globalPositions.put("6",new int[]{23, 24, 25, 26});
        
        nameSuffix.put("1A", "_1234");
        nameSuffix.put("1B", "_123");
        nameSuffix.put("1C", "_124");
        nameSuffix.put("1D", "_134");
        nameSuffix.put("1E", "_234");
        nameSuffix.put("1F", "_12");
        nameSuffix.put("1G", "_13");
        nameSuffix.put("1H", "_14");
        nameSuffix.put("1I", "_23");
        nameSuffix.put("1J", "_24");
        nameSuffix.put("1K", "_34");
        nameSuffix.put("1L", "_1");
        nameSuffix.put("1M", "_2");
        nameSuffix.put("1N", "_3");
        nameSuffix.put("1O", "_4");
        nameSuffix.put("2", "_coord");
        nameSuffix.put("3", "_massNeighb");
        nameSuffix.put("4", "_massGradient");
        nameSuffix.put("5", "_1312ratio");
        nameSuffix.put("6", "_2726ratio");
//        suffixList[10] = "_N2_Rad_G1";
//        suffixList[19] = "_N3_Rad_G1";
//        suffixList[31] = "_N2_Rad";
//        suffixList[34] = "_N3_Rad";
//        suffixList[37] = "_Rad_G1";
//        suffixList[46] = "_Rad";
//        suffixList[49] = "_N2_Rad_Rat";
//        suffixList[50] = "_N2_Rad_G1_Rat";
//        suffixList[51] = "_Rad_Rat";
//        suffixList[52] = "_N2_Rad_Rat_RN2";
//        suffixList[53] = "_N2_Rad_G1_Rat_RN2";
//        suffixList[54] = "_Rad_Rat_RN2";
//        suffixList[55] = "_N2_Rad_Rat_RG1";
//        suffixList[56] = "_N2_Rad_G1_Rat_RG1";
//        suffixList[57] = "_Rad_Rat_RG1";
//        suffixList[58] = "_N2_Rad_Rat_RN2_RG1";
//        suffixList[59] = "_N2_Rad_G1_Rat_RN2_RG1";
//        suffixList[60] = "_Rad_Rat_RN2_RG1";
        
        
        
//        tokens.put(suffixList[46], new int[] {0, mass1, mass2, mass3, mass4});
//        tokens.put(suffixList[10], new int[] {0,mass1,mean1,vari1,grad1, mass2,mean2,vari2,grad2, mass3,mean3,vari3,grad3, mass4,mean4,vari4,grad4});
//        tokens.put(suffixList[31], new int[] {0,mass1,mean1,vari1, mass2,mean2,vari2, mass3,mean3,vari3, mass4,mean4,vari4});
//        tokens.put(suffixList[37], new int[] {0, mass1,grad1, mass2,grad2, mass3,grad3, mass4,grad4});
//        tokens.put(suffixList[49], new int[] {0,mass1,mean1,vari1, mass2,mean2,vari2, mass3,mean3,vari3, mass4,mean4,vari4, rat1312, rat2726});
//        tokens.put(suffixList[50], new int[] {0,mass1,mean1,vari1,grad1, mass2,mean2,vari2,grad2, mass3,mean3,vari3,grad3, mass4,mean4,vari4,grad4, rat1312, rat2726});
//        tokens.put(suffixList[51], new int[] {0, mass1, mass2, mass3, mass4, rat1312, rat2726});
//        tokens.put(suffixList[52], new int[] {0, 3, 4, 5, 7, 8, 9, 11, 12, 13, 15, 16, 17, 19, 20, 23, 24});
//        tokens.put(suffixList[53], new int[] {0, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 23, 24});
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jCheckBox3 = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jCheckBox7 = new javax.swing.JCheckBox();
        jCheckBox8 = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        jCheckBox4 = new javax.swing.JCheckBox();
        jCheckBox6 = new javax.swing.JCheckBox();
        jCheckBox9 = new javax.swing.JCheckBox();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jCheckBox10 = new javax.swing.JCheckBox();
        jCheckBox11 = new javax.swing.JCheckBox();
        jCheckBox12 = new javax.swing.JCheckBox();
        jCheckBox13 = new javax.swing.JCheckBox();
        jPanel6 = new javax.swing.JPanel();
        jCheckBox14 = new javax.swing.JCheckBox();
        jCheckBox15 = new javax.swing.JCheckBox();
        jPanel7 = new javax.swing.JPanel();
        jCheckBox5 = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "SVM Testing Suite", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.ABOVE_TOP, new java.awt.Font("Bell MT", 1, 24))); // NOI18N

        jButton1.setText("Load");

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Neighbourhood", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jCheckBox1.setText("04pixel");
        jCheckBox1.setEnabled(false);
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jCheckBox2.setSelected(true);
        jCheckBox2.setText("08pixel");

        jCheckBox3.setText("24pixel");
        jCheckBox3.setEnabled(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox3)
                    .addComponent(jCheckBox2)
                    .addComponent(jCheckBox1))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jCheckBox1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckBox2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckBox3)
                .addContainerGap(32, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Ratio setup", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jCheckBox7.setText("Ratio 13/12");

        jCheckBox8.setSelected(true);
        jCheckBox8.setText("Ratio 27/26");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox7)
                    .addComponent(jCheckBox8))
                .addContainerGap(54, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(37, Short.MAX_VALUE)
                .addComponent(jCheckBox7)
                .addGap(18, 18, 18)
                .addComponent(jCheckBox8)
                .addGap(25, 25, 25))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Gradient", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jCheckBox4.setSelected(true);
        jCheckBox4.setText("+1");

        jCheckBox6.setSelected(true);
        jCheckBox6.setText("+2");

        jCheckBox9.setSelected(true);
        jCheckBox9.setText("+3");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox9)
                    .addComponent(jCheckBox6)
                    .addComponent(jCheckBox4))
                .addContainerGap(81, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBox4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckBox6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox9)
                .addContainerGap(45, Short.MAX_VALUE))
        );

        jButton2.setText("Add File");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("Run");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jTextField1.setEditable(false);

        jLabel2.setText("Status");

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Mass Selection"));

        jCheckBox10.setSelected(true);
        jCheckBox10.setText("Mass 1");

        jCheckBox11.setSelected(true);
        jCheckBox11.setText("Mass 2");

        jCheckBox12.setSelected(true);
        jCheckBox12.setText("Mass 3");

        jCheckBox13.setSelected(true);
        jCheckBox13.setText("Mass 4");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBox10)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jCheckBox11)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jCheckBox12)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jCheckBox13)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jCheckBox10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckBox12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox13)
                .addContainerGap(45, Short.MAX_VALUE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Coordinates"));

        jCheckBox14.setText("Use coordinates");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jCheckBox14))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(13, Short.MAX_VALUE)
                .addComponent(jCheckBox14, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jCheckBox14.getAccessibleContext().setAccessibleName("Use\ncoordinates");

        jCheckBox15.setSelected(true);
        jCheckBox15.setText("train");

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Cross - Validation ", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jCheckBox5.setText("5-fold");

        jLabel1.setText("(default: 2-fold)");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(jCheckBox5))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1)))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBox5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addContainerGap(31, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGap(70, 70, 70)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                            .addGap(12, 12, 12)
                                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                    .addGap(12, 12, 12)
                                                    .addComponent(jLabel2))
                                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addGap(248, 248, 248)
                            .addComponent(jCheckBox15)
                            .addGap(59, 59, 59)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButton2)
                        .addGap(43, 43, 43)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(294, 294, 294)
                        .addComponent(jButton1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(64, 64, 64)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(31, 31, 31)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton1)
                            .addComponent(jButton2))
                        .addGap(27, 27, 27))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jCheckBox15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGap(18, 18, 18)
                            .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jLabel2)
                            .addGap(61, 61, 61))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 622, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_jCheckBox1ActionPerformed
private File chooseFile () {
        JFileChooser chooser = new JFileChooser();
        

        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        // cancel was clicked
        return null;
    }
private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
try{
    File currentFile = chooseFile();
    if (currentFile != null){
        myFile = currentFile;
        /*
        this.myFiles.add(currentFile);//GEN-LAST:event_jButton2ActionPerformed
       */
        // int noFiles = new Integer(this.jTextField2.getText());
        //noFiles = noFiles+1;///
        //this.jTextField2.setText(noFiles+"");
    }
}catch (Exception e){e.printStackTrace();}
}


//private String createFiles(boolean[] setup, String runName, File file) throws FileNotFoundException, IOException{
//
//    for (int i=0; i<setup.length; i++){
//        BufferedReader bfr = new BufferedReader(new FileReader(file));
//        BufferedWriter bfw = new BufferedWriter((new FileWriter(file.getAbsoluteFile()+this.suffixList[i], true)));
//        boolean running = true;
//        int[] indices = (int[]) this.tokens.get(suffixList[i]);
//        while(running){
//        
//            String currentLine = bfr.readLine();
//            if (currentLine != null){
//                String[] strTok =  currentLine.split(" ");
//                String result = " ";
//                for (int j = 0; j<indices.length; j++){
//                    //          String res32 =strTok[0]+" "/*+ strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "*/+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
//                        
//                    result +=strTok[indices[j]]+ " ";
//                
//                }
//                bfw.append(result);
//                bfw.append("\n");
//                
//            }else{ running = false;}
//        }
//        bfw.close();
//    }
//return null;}
private void createTestSetup(boolean[] config, File file) throws FileNotFoundException, IOException{
    
    
    //Generic START
    int amount = 0;
    for (int a =0; a< config.length; a++){
        if (config[a]){
            amount++;
        }
        
    }
    
    int[] selectedSetup = new int[amount];
    
    int current=0; // kann man eigentlich durch amount ersetzen
    
    for (int b=0; b<config.length;b++){
        
        if (config[b]){
            selectedSetup[current] = b;
            current++;
        }        
    }
    Hashtable allVari = new Hashtable();
    String g= "";
    int[] indices;
    PermutationGenerator x = new PermutationGenerator (selectedSetup.length);
    StringBuffer permutation;   
    while (x.hasMore ()) { 
        permutation = new StringBuffer ();
        indices = x.getNext ();
        int[] currentPermutation = new int[amount]; 
        for (int i = 0; i < indices.length; i++) {
            currentPermutation[i] = selectedSetup[indices[i]];
           // permutation.append (selectedSetup[indices[i]]);
        }
        for (int t=1; t< currentPermutation.length+1; t++){
        
            int[] permu = new int[t];
            for (int s=0; s< permu.length; s++){
                permu[s] = currentPermutation[s];
            }
            java.util.Arrays.sort(permu);
            StringBuffer id = new StringBuffer();
            
            for (int h =0;h< permu.length; h++){
                id.append(permu[h]);
            }
            
            if (!(allVari.containsKey(id.toString()))){
                allVari.put(id.toString(), permu);
            }else{
            }
            //if (allVari.containsKey(g))
            
        }
        //System.out.println (permutation.toString ());
    }
    BufferedWriter bfw2 = null;

    if (this.jCheckBox15.isSelected()){
        bfw2 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"exec",true));
    }
    
    
//    for (int i=1; i < selectedSetup.length+1; i++){
    
        //ArrayList<Integer> subset = new ArrayList<Integer>();
        //subset.add(0); //just for growing
        System.out.println("stop");
//        for (int j=0; j< selectedSetup.length-i;j++){
//           
//            
//            for (int k=1; k+i+j<selectedSetup.length; k++){
//                 ArrayList<Integer> subset = new ArrayList<Integer>();
//                 subset.add(selectedSetup[j]);
//                for (int p=k+j; p<j+i+k ; p++) {
//            
//                    subset.add(selectedSetup[p]);
//                }
//                try{
//                    subset.set(subset.size(), selectedSetup[k]);
//                }catch (Exception e){
//                    subset.add(selectedSetup[k]);
//                }
                Iterator iti = allVari.keySet().iterator();
                while(iti.hasNext()){
                    
                    int[] subset2 = (int[]) allVari.get(iti.next());
                    
                    ArrayList<Integer> positions = new ArrayList<Integer>();
                    String pathEnding="";
                    int[] tempPositions;
                    if (subset2[0] ==1){
                        pathEnding = (String) this.nameSuffix.get(1+this.massSetup);
                
                        tempPositions = (int[]) this.globalPositions.get(1+this.massSetup);
                
                        for (int z=0; z<tempPositions.length; z++){
                            positions.add(tempPositions[z]);
                        }
                    
                        for (int c=1; c < subset2.length; c++){
                    
                            tempPositions = (int[]) this.globalPositions.get(subset2[c]+"");
                            pathEnding += (String) this.nameSuffix.get(subset2[c]+""); 
                    
                            for (int z=0; z<tempPositions.length; z++){
                                positions.add(tempPositions[z]);
                            }
                        }
                
                    }else{
                        for (int c=0; c < subset2.length; c++){
                    
                            tempPositions = (int[]) this.globalPositions.get(subset2[c]+"");
                            pathEnding += (String) this.nameSuffix.get(subset2[c]+"");
                    
                            for (int z=0; z<tempPositions.length; z++){
                                positions.add(tempPositions[z]);
                            }
                        }
                    }
                    boolean running = true;
                    BufferedReader bfr = new BufferedReader(new FileReader(file));
                    BufferedWriter bfw1 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+pathEnding, true));
                
                    while (running) {
                    
                        String currentLine = bfr.readLine();
                        if (currentLine != null){
                            String[] strTok =  currentLine.split(" ");
                            String resultLine = strTok[0];
                        
                            for (int f = 0; f<positions.size();f++){
                                String[] innerTokens = strTok[positions.get(f)].split(":");
                                resultLine += " "+(f+1)+":"+innerTokens[1];
                            }
                            bfw1.append(resultLine);
                            bfw1.append("\n");
                        }else{
                            running = false;}
                    }
                    bfw1.close();
                    bfr.close();
                    if (this.jCheckBox15.isSelected()){
                        String writer= file.getAbsoluteFile()+pathEnding;
                        if (this.jCheckBox5.isSelected()){
                            bfw2.append("./rad5.py "+writer+" "+writer.replaceFirst("train", "test"));
                            bfw2.append("\n");
                        }else{
                            bfw2.append("./rad2.py "+writer+" "+writer.replaceFirst("train", "test"));
                            bfw2.append("\n");
                        }
                    }
                }
     
                if (this.jCheckBox15.isSelected()){
                    bfw2.close();
                }
//                ArrayList<Integer> positions = new ArrayList<Integer>();
//                String pathEnding="";
//                int[] tempPositions;
//                if (subset.contains(1)){
//                    pathEnding = (String) this.nameSuffix.get(1+this.massSetup);
//                
//                    tempPositions = (int[]) this.globalPositions.get(1+this.massSetup);
//                
//                for (int z=0; z<tempPositions.length; z++){
//                    positions.add(tempPositions[z]);
//                }
//                    
//                    for (int c=1; c < subset.size(); c++){
//                    
//                        tempPositions = (int[]) this.globalPositions.get(subset.get(c)+"");
//                        pathEnding += (String) this.nameSuffix.get(subset.get(c)+""); 
//                    
//                        for (int z=0; z<tempPositions.length; z++){
//                            positions.add(tempPositions[z]);
//                        }
//                    
//                }
//                }else{
//                for (int c=0; c < subset.size(); c++){
//                    
//                    tempPositions = (int[]) this.globalPositions.get(subset.get(c)+"");
//                    pathEnding += (String) this.nameSuffix.get(subset.get(c)+"");
//                    
//                    for (int z=0; z<tempPositions.length; z++){
//                        positions.add(tempPositions[z]);
//                    }
//                    
//                }
//                }
//                boolean running = true;
//                BufferedReader bfr = new BufferedReader(new FileReader(file));
//                BufferedWriter bfw1 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+pathEnding, true));
//                
//                while (running) {
//                    
//                    String currentLine = bfr.readLine();
//                    if (currentLine != null){
//                        String[] strTok =  currentLine.split(" ");
//                        String resultLine = strTok[0];
//                        
//                        for (int f = 0; f<positions.size();f++){
//                            String[] innerTokens = strTok[positions.get(f)].split(":");
//                            resultLine += " "+(f+1)+":"+innerTokens[1];
//                        
//                        }
//                        bfw1.append(resultLine);
//                        bfw1.append("\n");
//                    }else{
//                        running = false;}
//                }
//                bfw1.close();
//                bfr.close();
//                
//                String execution = "./rad2.py "+file.getAbsoluteFile()+pathEnding+" "+pathEnding.replaceFirst("train", "test");
               
//            }
//        }
   // }
   
    //Generic END
    /*
     
     */
    
    
//    boolean[] output = new boolean[48];
//    
//    BufferedReader bfr = new BufferedReader(new FileReader(file));
//    boolean running = true;
//    BufferedWriter bfw0 = null;
//    BufferedWriter bfw1 = null;
//    BufferedWriter bfw2 = null;
//    BufferedWriter bfw3 = null;
//    BufferedWriter bfw4 = null;
//    BufferedWriter bfw5 = null;
//    BufferedWriter bfw6 = null;
//    BufferedWriter bfw7 = null;
//    BufferedWriter bfw8 = null;
//    BufferedWriter bfw9 = null;
//    BufferedWriter bfw10 = null;
//    BufferedWriter bfw11 = null;
//    BufferedWriter bfw12 = null;
//    BufferedWriter bfw13 = null;
//    BufferedWriter bfw14 = null;
//    BufferedWriter bfw15 = null;
//    BufferedWriter bfw16 = null;
//    BufferedWriter bfw17 = null;
//    BufferedWriter bfw18 = null;
//    BufferedWriter bfw19 = null;
//    BufferedWriter bfw20 = null;
//    BufferedWriter bfw21 = null;
//    BufferedWriter bfw22 = null;
//    BufferedWriter bfw23 = null;
//    BufferedWriter bfw24 = null;
//    BufferedWriter bfw25 = null;
//    BufferedWriter bfw26 = null;
//    BufferedWriter bfw27 = null;
//    BufferedWriter bfw28 = null;
//    BufferedWriter bfw29 = null;
//    BufferedWriter bfw30 = null;
//    BufferedWriter bfw31 = null;
//    BufferedWriter bfw32 = null;
//    BufferedWriter bfw33 = null;
//    BufferedWriter bfw34 = null;
//    BufferedWriter bfw35 = null;
//    BufferedWriter bfw36 = null;
//    BufferedWriter bfw37 = null;
//    BufferedWriter bfw38 = null;
//    BufferedWriter bfw39 = null;
//    BufferedWriter bfw40 = null;
//    BufferedWriter bfw41 = null;
//    BufferedWriter bfw42 = null;
//    BufferedWriter bfw43 = null;
//    BufferedWriter bfw44 = null;
//    BufferedWriter bfw45 = null;
//    BufferedWriter bfw46 = null;
//    BufferedWriter bfw47 = null;
//    
// //   String[] suffixList ={"_N1_Lin_G1","_N1_Rad_G1","_N1_Sig_G1","_N1_Lin_G2","_N1_Rad_G2",};// new String[48];
//   String[] suffixList = new String[48];
//   suffixList[10] = "_N2_Rad_G1";
//   suffixList[19] = "_N3_Rad_G1";
//   suffixList[31] = "_N2_Rad";
//   suffixList[34] = "_N3_Rad";
//   suffixList[37] = "_Rad_G1";
//   suffixList[46] = "_Rad";
//   if (config[1] && config[4] && config[6] ){
//        output[10] = true;
//        bfw10 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Rad_G1", true));
//        }else{output[10] = false;}
//    
//    
//    if (config[2] && config[4] && config[6] ){
//        
//        output[19] = true;
//        bfw19 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Rad_G1", true));
//    }else{output[19] = false;}
//    
//    if (config[2] && config[4]){
//        output[34] = true;
//        bfw34 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Rad", true));
//    }else{output[34] =false;}
//    
//    if (config[1] && config[4]){
//        output[31] = true;
//        bfw31 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Rad", true));
//    }else{output[31] =false;}
//    
//    if (config[6] && config[4]){
//        output[37] = true;
//        bfw37 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Rad_G1", true));
//    }else{output[37] =false;}
//    
//    
//    
//    if (config[4]){
//        output[46] = true;
//        bfw46 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Rad", true));
//    }else{output[46] =false;}
//    
//    
//////     if (config[0] && config[3] && config[6] ){
//////        output[0] = true;
//////        bfw0 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Lin_G1", true));
//////    }else{output[0] = false;}
////
////    if (config[0] && config[4] && config[6] ){
////        output[1] = true;
////        bfw1 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Rad_G1", true));
////    }else{output[1] = false;}
////    
//////    if (config[0] && config[5] && config[6] ){
//////        output[2] = true;
//////        bfw2 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Sig_G1", true));
//////        }else{output[2] = false;}
//////    
//////    if (config[0] && config[3] && config[7] ){
//////        output[3] = true;
//////        bfw3 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Lin_G2", true));
//////        }else{output[3] = false;}
//////        
////    if (config[0] && config[4] && config[7] ){
////        output[4] = true;
////        bfw4 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Rad_G2", true));
////    }else{output[4] = false;}
////    
//////    if (config[0] && config[5] && config[7] ){
//////        output[5] = true;
//////        bfw5 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Sig_G2", true));
//////        }else{output[5] = false;}
//////    
//////    if (config[0] && config[3] && config[8] ){
//////        output[6] = true;
//////        bfw6 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Lin_G3", true));
//////        }else{output[6] = false;}
//////    
////    if (config[0] && config[4] && config[8] ){
////        output[7] = true;
////        bfw7 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Rad_G3", true));    
////    }else{output[7] = false;}
////    
////    if (config[0] && config[5] && config[8] ){
////        output[8] = true;
////        bfw8 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Sig_G3", true));
////    }else{output[8] = false;}
////    
////    if (config[1] && config[3] && config[6] ){
////        output[9] = true;
////        bfw9 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Lin_G1", true));
////    }else{output[9] = false;}
////    
//   
////    if (config[1] && config[5] && config[6] ){
////        output[11] = true;
////        bfw11 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Sig_G1", true));
////        }else{output[11] = false;}
////    
////    if (config[1] && config[3] && config[7] ){
////        output[12] = true;
////        bfw12 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Lin_G2", true));
////        }else{output[12] = false;}
//////    
////    if (config[1] && config[4] && config[7] ){
////        output[13] = true;
////        bfw13 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Rad_G2", true));
////        }else{output[13] = false;}
////    
////    if (config[1] && config[5] && config[7] ){
////        output[14] = true;
////        bfw14 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Sig_G2", true));
////        }else{output[14] = false;}
////    
////    if (config[1] && config[3] && config[8] ){
////        output[15] = true;
////        bfw15 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Lin_G3", true));
////        }else{output[15] = false;}
////    
////    if (config[1] && config[4] && config[8] ){
////        output[16] = true;
////        bfw16 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Rad_G3", true));
////        }else{output[16] = false;}
////    
////    if (config[1] && config[5] && config[8] ){
////        output[17] = true;
////        bfw17 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Sig_G3", true));
////        }else{output[17] = false;}
////    
////    if (config[2] && config[3] && config[6] ){
////        output[18] = true;
////        bfw18 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Lin_G1", true));
////        }else{output[18] = false;}
////    
//
////    if (config[2] && config[5] && config[6] ){
////        output[20] = true;
////        bfw20 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Sig_G1", true));
////        }else{output[20] = false;}
////    
////    if (config[2] && config[3] && config[7] ){
////        output[21] = true;
////        bfw21 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Lin_G2", true));
////    }else{output[21] = false;}
////    
////    if (config[2] && config[4] && config[7] ){
////        output[22] = true;
////        bfw22 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Rad_G2", true));
////        }else{output[22] = false;}
//    
////    if (config[2] && config[5] && config[7] ){
////        output[23] = true;
////        bfw23 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Sig_G2", true));
////        }else{output[23] = false;}
////    
////    if (config[2] && config[3] && config[8] ){
////        output[24] = true;
////        bfw24 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Lin_G3", true));
////        }else{output[24] = false;}
//    
////    if (config[2] && config[4] && config[8] ){
////        output[25] = true;
////        bfw25 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Rad_G3", true));
////        }else{output[25] = false;}
//    
////    if (config[2] && config[5] && config[8] ){
////        output[26] = true;
////        bfw26 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Sig_G3", true));
////        }else{output[26] = false;}
////    
////    
////    if (config[0] && config[3]){
////        output[27] = true;
////        bfw27 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Lin", true));
////    }else{output[27] =false;}
//    
////    if (config[0] && config[4]){
////        output[28] = true;
////        bfw28 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Rad", true));
////    }else{output[28] =false;}
//    
////    if (config[0] && config[5]){
////        output[29] = true;
////        bfw29 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N1_Sig", true));
////    }else{output[29] =false;}
////    
////    if (config[1] && config[3]){
////        output[30] = true;
////        bfw30 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Lin", true));
////    }else{output[30] =false;}
//    
//
////    if (config[1] && config[5]){
////        output[32] = true;
////        bfw32 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N2_Sig", true));
////    }else{output[32] =false;}
////    
////    if (config[2] && config[3]){
////        output[33] = true;
////        bfw33 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Lin", true));
////    }else{output[33] =false;}
//
////    if (config[2] && config[5]){
////        output[35] = true;
////        bfw35 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_N3_Sig", true));
////    }else{output[35] =false;}
////    
////    
////    if (config[6] && config[3]){
////        output[36] = true;
////        bfw36 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Lin_G1", true));
////    }else{output[36] =false;}
//    
//   
////    if (config[6] && config[5]){
////        output[38] = true;
////        bfw38 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Sig_G1", true));
////    }else{output[38] =false;}
////    
////    if (config[7] && config[3]){
//////        output[39] = true;
////        bfw39 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Lin_G2", true));
////    }else{output[39] =false;}
////    
////    if (config[7] && config[4]){
////        output[40] = true;
////        bfw40 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Rad_G2", true));
////    }else{output[40] =false;}
//    
////    if (config[7] && config[5]){
////        output[41] = true;
////        bfw41 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Sig_G2", true));
////    }else{output[41] =false;}
////    
////    if (config[8] && config[3]){
////        output[42] = true;
////        bfw42 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Lin_G3", true));
////    }else{output[42] =false;}
//    
////    if (config[8] && config[4]){
////        output[43] = true;
////        bfw43 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Rad_G3", true));
////    }else{output[43] =false;}
//    
////    if (config[8] && config[5]){
////        output[44] = true;
////        bfw44 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Sig_G3", true));
////    }else{output[44] =false;}
////    
////    if (config[3]){
////        output[45] = true;
////        bfw45 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Lin", true));
////    }else{output[45] =false;}
//    
//
//    
////    if (config[5]){
////        output[47] = true;
////        bfw47 = new BufferedWriter(new FileWriter(file.getAbsoluteFile()+"_Sig", true));
////    }else{output[47] =false;}
//// 
////    
////    
//    
//    while(running){
//        
//    String currentLine = bfr.readLine();
//    if (currentLine != null){
//    String[] strTok =  currentLine.split(" ");
//    
//    
//    
//    Hashtable tokens = new Hashtable();
//    tokens.put(suffixList[46], new int[] {0, 3, 7, 11,15});
//    tokens.put(suffixList[10], new int[] {0, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18});
//    tokens.put(suffixList[31], new int[] {0, 3, 4, 5, 7, 8, 9, 11, 12, 13, 15, 16, 17});
//    tokens.put(suffixList[37], new int[] {0, 3, 6, 7, 10, 11, 14, 15, 18});
//    
//    if (output[45] ){
//            String res45 =strTok[0]+" "/*+ strTok[3]+" "+strTok[7]+" "*/+strTok[11]+" "+strTok[15];
//            bfw45.append(res45);
//            bfw45.append("\n");
//     }if (output[46] ){
//            String res46 =strTok[0]+" "/*+ strTok[3]+" "+strTok[7]+" "*/+strTok[11]+" "+strTok[15];
//            bfw46.append(res46);
//            bfw46.append("\n");
//     }if (output[47] ){
//            String res47 =strTok[0]+" "/*+ strTok[3]+" "+strTok[7]+" "*/+strTok[11]+" "+strTok[15];
//            bfw47.append(res47);
//            bfw47.append("\n");
//     }    
//     if (output[10] ){
//            String res10 =strTok[0]+" "/*+ strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "*/+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17]+" "+strTok[18];
//            bfw10.append(res10);
//            bfw10.append("\n");
//     }
//    
//    if (output[9] ){
//            String res9 =strTok[0]+" "/*+ strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "*/+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17]+" "+strTok[18];
//            bfw9.append(res9);
//            bfw9.append("\n");
//     }
//    if (output[11] ){
//            String res11 =strTok[0]+" "/*+ strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "*/+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17]+" "+strTok[18];
//            bfw11.append(res11);
//            bfw11.append("\n");
//     }
//    
//        if (output[30] ){
//            String res30 =strTok[0]+" "/*+ strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "*/+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
//            bfw30.append(res30);
//            bfw30.append("\n");
//     }
//    
//         if (output[31] ){
//            String res31 =strTok[0]+" "+ strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17] +" "+strTok[19]/*+" "+strTok[20]+" "+strTok[21]+" "+strTok[22]+" "+strTok[23]+" "+strTok[24]+" "+strTok[25]/*+" "+strTok[26]*/;
//            bfw31.append(res31);
//            bfw31.append("\n");
//     }
//        if (output[32] ){
//            String res32 =strTok[0]+" "/*+ strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "*/+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
//            bfw32.append(res32);
//            bfw32.append("\n");
//     }
//         if (output[36] ){
//            String res36 =strTok[0]+" "/*+ strTok[3]+" "+strTok[6]+" "+strTok[7]+" "+strTok[10]+" "+strTok[11]+" "*/+strTok[14]+" "+strTok[15]+" "+strTok[18];
//            bfw36.append(res36);
//            bfw36.append("\n");
//     }
//     if (output[37] ){
//            String res37 =strTok[0]+" "/*+ strTok[3]+" "+strTok[6]+" "+strTok[7]+" "+strTok[10]+" "+strTok[11]+" "*/+strTok[14]+" "+strTok[15]+" "+strTok[18];
//            bfw37.append(res37);
//            bfw37.append("\n");
//     }
//    if (output[38] ){
//            String res38 =strTok[0]+" "/*+ strTok[3]+" "+strTok[6]+" "+strTok[7]+" "+strTok[10]+" "+strTok[11]+" "*/+strTok[14]+" "+strTok[15]+" "+strTok[18];
//            bfw38.append(res38);
//            bfw38.append("\n");
//     }
//
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
////    
////    if (output[0] ){
////            String res0 = strTok[2]+" "+strTok[3]+" "+strTok[4]+" "+strTok[5]+" "+strTok[6]+" "+strTok[7]+" "+strTok[8]+" "+strTok[9]+" "+strTok[10]+" "+strTok[11]+" "+strTok[12]+" "+strTok[13]+" "+strTok[14]+" "+strTok[15]+" "+strTok[16]+" "+strTok[17];
////            bfw0.append(res0);
////            bfw0.append("\n");
////     }
//    
////    
////    if (config[0] && config[5] && config[6] ){
////        output[2] = true;
////        }else{output[2] = false;}
////    
////    if (config[0] && config[3] && config[7] ){
////        output[3] = true;
////        }else{output[3] = false;}
////        
////    if (config[0] && config[4] && config[7] ){
////        output[4] = true;
////    }else{output[4] = false;}
////    
////    if (config[0] && config[5] && config[7] ){
////        output[5] = true;
////        }else{output[5] = false;}
////    
////    if (config[0] && config[3] && config[8] ){
////        output[6] = true;
////        }else{output[6] = false;}
////    
////    if (config[0] && config[4] && config[8] ){
////        output[7] = true;
////        }else{output[7] = false;}
////    
////    if (config[0] && config[5] && config[8] ){
////        output[8] = true;
////        }else{output[8] = false;}
////    
////    if (config[1] && config[3] && config[6] ){
////        output[9] = true;
////        }else{output[9] = false;}
////    
////    if (config[1] && config[4] && config[6] ){
////        output[10] = true;
////        }else{output[10] = false;}
////    
////    if (config[1] && config[5] && config[6] ){
////        output[11] = true;
////        }else{output[11] = false;}
////    
////    if (config[1] && config[3] && config[7] ){
////        output[12] = true;
////        }else{output[12] = false;}
////    
////    if (config[1] && config[4] && config[7] ){
////        output[13] = true;
////        }else{output[13] = false;}
////    
////    if (config[1] && config[5] && config[7] ){
////        output[14] = true;
////        }else{output[14] = false;}
////    
////    if (config[1] && config[3] && config[8] ){
////        output[15] = true;
////        }else{output[15] = false;}
////    
////    if (config[1] && config[4] && config[8] ){
////        output[16] = true;
////        }else{output[16] = false;}
////    
////    if (config[1] && config[5] && config[8] ){
////        output[17] = true;
////        }else{output[17] = false;}
////    
////    if (config[2] && config[3] && config[6] ){
////        output[18] = true;
////        }else{output[18] = false;}
////    
////    if (config[2] && config[4] && config[6] ){
////        output[19] = true;
////        }else{output[19] = false;}
////    
////    if (config[2] && config[5] && config[6] ){
////        output[20] = true;
////        }else{output[20] = false;}
////    
////    if (config[2] && config[4] && config[7] ){
////        output[21] = true;
////        }else{output[21] = false;}
////    
////    if (config[2] && config[4] && config[7] ){
////        output[22] = true;
////        }else{output[22] = false;}
////    
////    if (config[2] && config[5] && config[7] ){
////        output[23] = true;
////        }else{output[23] = false;}
////    
////    if (config[2] && config[3] && config[8] ){
////        output[24] = true;
////        }else{output[24] = false;}
////    
////    if (config[2] && config[4] && config[8] ){
////        output[25] = true;
////        }else{output[25] = false;}
////    
////    if (config[2] && config[5] && config[8] ){
////        output[26] = true;
////        }else{output[26] = false;}
//   }else{running = false;} 
//    }
//    bfw10.close();
//    bfw31.close();
//    bfw37.close();
//    
//return output;
}

private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed

    boolean[] config = new boolean[33];
    boolean mass1 = this.jCheckBox10.isSelected();
    boolean mass2 = this.jCheckBox11.isSelected();
    boolean mass3 = this.jCheckBox12.isSelected();
    boolean mass4 = this.jCheckBox13.isSelected();
    
    /*     globalPositions.put("1A", new int[] {3, 7, 11, 15});
        globalPositions.put("1B", new int[] {3, 7, 11});
        globalPositions.put("1C", new int[] {3, 7, 15});
        globalPositions.put("1D", new int[] {3, 11, 15});
        globalPositions.put("1E", new int[] {7, 11, 15});
        globalPositions.put("1F", new int[] {3, 7});
        globalPositions.put("1G", new int[] {3, 11});
        globalPositions.put("1H", new int[] {3, 15});
        globalPositions.put("1I", new int[] {7, 11});
        globalPositions.put("1J", new int[] {7, 15});
        globalPositions.put("1K", new int[] {11, 15});
        globalPositions.put("1L", new int[] {3});
        globalPositions.put("1M", new int[] {5});
        globalPositions.put("1N", new int[] {7});
        globalPositions.put("1O", new int[] {11});
        globalPositions.put("2", new int[]{1, 2});
        globalPositions.put("3", new int[]{4, 5, 8, 9 , 12, 13, 16, 17});
        globalPositions.put("4",new int[]{4, 5, 8, 9});                                                               
        globalPositions.put("5",new int[]{4, 5, 12, 13});
        globalPositions.put("6",new int[]{4, 5, 16, 17});
        globalPositions.put("7",new int[]{ 8, 9, 12,13});
        globalPositions.put("8",new int[]{8, 9, 16,17});
        globalPositions.put("9",new int[]{12,13,16,17});
        globalPositions.put("10",new int[]{4, 5, 8, 9, 12, 13});
        globalPositions.put("11",new int[]{4, 5, 8, 9, 16, 17});
        globalPositions.put("12",new int[]{8, 9, 12, 13, 16, 17});
        globalPositions.put("13",new int[]{4, 5});
        globalPositions.put("14",new int[]{8, 9});
        globalPositions.put("15",new int[]{12,13});
        globalPositions.put("16",new int[]{16,17});
        globalPositions.put("17", new int[]{6, 10,14, 18}); 
        globalPositions.put("18", new int[]{6, 10}); 
        globalPositions.put("19", new int[]{6, 14}); 
        globalPositions.put("20", new int[]{6, 18}); 
        globalPositions.put("21", new int[]{10,14});
        globalPositions.put("22", new int[]{10,18});
        globalPositions.put("23", new int[]{14, 18});
        globalPositions.put("24", new int[]{6, 10, 14}); 
        globalPositions.put("25", new int[]{6, 10, 18}); 
        globalPositions.put("26", new int[]{10, 14, 18});
        globalPositions.put("27", new int[]{6});
        globalPositions.put("28", new int[]{10}); 
        globalPositions.put("29", new int[]{14}); 
        globalPositions.put("30", new int[]{18}); 
        
        //ratio
        globalPositions.put("31", new int[]{19, 20, 21, 22});
        globalPositions.put("32",new int[]{23, 24, 25, 26});
        
        nameSuffix.put("1A", "_1234");
        nameSuffix.put("1B", "_123");
        nameSuffix.put("1C", "_124");
        nameSuffix.put("1D", "_134");
        nameSuffix.put("1E", "_234");
        nameSuffix.put("1F", "_12");
        nameSuffix.put("1G", "_13");
        nameSuffix.put("1H", "_14");
        nameSuffix.put("1I", "_23");
        nameSuffix.put("1J", "_24");
        nameSuffix.put("1K", "_34");
        nameSuffix.put("1L", "_1");
        nameSuffix.put("1M", "_2");
        nameSuffix.put("1N", "_3");
        nameSuffix.put("1O", "_4");
        nameSuffix.put("2", "_coord");
        nameSuffix.put("3", "_massNeighb");
        nameSuffix.put("4", "_massGradient");
        nameSuffix.put("5", "_1312ratio");
        nameSuffix.put("6", "_2726ratio");*/
    if (mass1 && mass2 && mass3 && mass4){
        this.massSetup = "A";
        globalPositions.put("3", new int[]{4, 5, 8, 9 , 12, 13, 16, 17});
        globalPositions.put("4", new int[]{6, 10,14, 18}); 
    } 
    if (mass1 && !mass2 && !mass3 && !mass4){
        this.massSetup = "L";
        globalPositions.put("3", new int[]{4, 5});
        globalPositions.put("4", new int[]{6}); 
    }
    if (!mass1 && mass2 && !mass3 && !mass4){
        this.massSetup = "M";
        globalPositions.put("3", new int[]{8, 9});
        globalPositions.put("4", new int[]{10}); 
    }
    if (!mass1 && !mass2 && mass3 && !mass4){
        this.massSetup = "N";
        globalPositions.put("3", new int[]{12, 13});
        globalPositions.put("4", new int[]{14}); 
    }
    if (!mass1 && !mass2 && !mass3 && mass4){
        this.massSetup = "O";
        globalPositions.put("3", new int[]{16, 17});
        globalPositions.put("4", new int[]{18}); 
    }
    if (mass1 && mass2 && !mass3 && !mass4){
        this.massSetup = "F";
        globalPositions.put("3", new int[]{4, 5, 8, 9});
        globalPositions.put("4", new int[]{6, 10}); 
    }
    if (mass1 && !mass2 && mass3 && !mass4){
        this.massSetup = "G";
        globalPositions.put("3", new int[]{4, 5, 12, 13});
        globalPositions.put("4", new int[]{6, 14}); 
    }
    if (mass1 && !mass2 && !mass3 && mass4){
        this.massSetup = "H";
        globalPositions.put("3", new int[]{4, 5, 16, 17});
        globalPositions.put("4", new int[]{6, 18}); 
    }
    if (!mass1 && mass2 && mass3 && !mass4){
        this.massSetup = "I";
        globalPositions.put("3", new int[]{ 8, 9 , 12, 13});
        globalPositions.put("4", new int[]{10,14}); 
    }
    if (!mass1 && mass2 && !mass3 && mass4){
        this.massSetup = "J";
        globalPositions.put("3", new int[]{ 8, 9, 16, 17});
        globalPositions.put("4", new int[]{10, 18}); 
    }
    if (!mass1 && !mass2 && mass3 && mass4){
        this.massSetup = "K";
        globalPositions.put("3", new int[]{12, 13, 16, 17});
        globalPositions.put("4", new int[]{14, 18}); 
    }
    if (mass1 && mass2 && mass3 && !mass4){
        this.massSetup = "B";
        globalPositions.put("3", new int[]{4, 5, 8, 9 , 12, 13});
        globalPositions.put("4", new int[]{6, 10,14}); 
    }
    if (mass1 && mass2 && !mass3 && mass4){
        this.massSetup = "C";
        globalPositions.put("3", new int[]{4, 5, 8, 9 , 16, 17});
        globalPositions.put("4", new int[]{6, 10, 18}); 
    }
    if (mass1 && !mass2 && mass3 && mass4){
        this.massSetup = "D";
        globalPositions.put("3", new int[]{4, 5, 12, 13, 16, 17});
        globalPositions.put("4", new int[]{6,14, 18});
    }
    if (!mass1 && mass2 && mass3 && mass4){
        this.massSetup = "E";
        globalPositions.put("3", new int[]{8, 9 , 12, 13, 16, 17});
        globalPositions.put("4", new int[]{10,14, 18});
    }
    if (!mass1 && !mass2 && !mass3 && !mass4){
        //in plugin...throw error
    }
    
    config[0] = false;
    config[1] = this.jCheckBox2.isSelected();
    config[2] = this.jCheckBox14.isSelected();
    
    config[3] = this.jCheckBox2.isSelected();
    config[4] = this.jCheckBox4.isSelected();
    config[5] = this.jCheckBox7.isSelected();
    config[6] = this.jCheckBox8.isSelected();
//    config[5] = this.jCheckBox9.isSelected();
//
//    config[6] = this.jCheckBox4.isSelected();
//    config[7] = this.jCheckBox5.isSelected();
//   // config[8] = this.jCheckBox6.isSelected();
//
//    config[10] = this.jCheckBox10.isSelected();
//    config[11] = this.jCheckBox11.isSelected();
//    config[12] = this.jCheckBox12.isSelected();
//    config[13] = this.jCheckBox13.isSelected();
//    config[14] = this.jCheckBox14.isSelected();
    
    
    
    //CellEditor cgValues = this.jTable1.getCellEditor();
//TableModel cgValues = this.jTable1.getModel();

//System.out.println(cgValues.getValueAt(8, 1));


            
    try {

                
            this.createTestSetup(config,myFile);
      
            } catch (IOException ex) {
                Logger.getLogger(SVMTestGUI.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(SVMTestGUI.class.getName()).log(Level.SEVERE, null, ex);
            }

}//GEN-LAST:event_jButton3ActionPerformed

private void jCheckBox11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox11ActionPerformed
}//GEN-LAST:event_jCheckBox11ActionPerformed

private void jCheckBox12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox12ActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_jCheckBox12ActionPerformed

private void runSetup(boolean[] setup, TableModel parameters, String path) throws IOException {

        if (setup[10]){
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/easy.py "+path+"_N2_Rad_G1 test10 ");           
        }
        if (setup[31]){
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/easy.py "+path+"_N2_Rad test31");
        }
        if (setup[37]){
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/easy.py "+path+"_Rad_G1 test37");
        }
//    if (setup[45]){
//            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/easy.py "+path+"_Lin test10 ");           
//        }
        if (setup[46]){
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/easy.py "+path+"_Rad test46");
//        }
//        if (setup[47]){
//            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/easy.py "+path+"_Sig test37");
        }
    
    
    
    int p =0;
    boolean running = true;
    while (running){
    double c = (Double) parameters.getValueAt(p, 0);
    double g = (Double) parameters.getValueAt(p, 1);
        
        
        if (setup[10]){
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_scale >"+path+"_N2_Rad_G1.scale "+path+"_N2_Rad_G1");           
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_train -c"+c+" -g"+g+" "+path+"_N2_Rad_G1.scale "+path+"_N2_Rad_G1_"+c+g+".model");
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_scale > test10.scale test10");
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_predict test10.scale"+path+"_N2_Rad_G1_"+c+g+".model "+path+"_N2_Rad_G1_"+c+g+".predict");
        }
        if (setup[31]){
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_scale >"+path+"_N2_Rad.scale "+path+"_N2_Rad");           
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_train -c"+c+" -g"+g+" "+path+"_N2_Rad.scale "+path+"_N2_Rad_"+c+g+".model");
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_scale > test31.scale test31");
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_predict test31.scale"+path+"_N2_Rad_"+c+g+".model "+path+"N2_Rad_"+c+g+".predict");
        }
        if (setup[37]){
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_scale >"+path+"_Rad_G1.scale "+path+"_Rad_G1");           
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_train -c"+c+" -g"+g+" "+path+"_Rad_G1.scale "+path+"_Rad_G1_"+c+g+".model");
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_scale > test37.scale test37");
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_predict test37.scale"+path+"_Rad_G1_"+c+g+".model "+path+"_Rad_G1_"+c+g+".predict");
        }

    if (setup[46]){
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_scale >"+path+"_Rad.scale "+path+"_Rad");           
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_train -c"+c+" -g"+g+" "+path+"_Rad.scale "+path+"_Rad_"+c+g+".model");
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_scale > test46.scale test46");
            Runtime.getRuntime().exec(".//nrims/home3/pgormanns/libsvm-2.86/svm_predict test46.scale"+path+"_Rad_"+c+g+".model "+path+"_Rad_"+c+g+".predict");
        }
    }

}
    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        
        
        Hashtable tokens = new Hashtable();
        //tokens.put("TEST", new int[] {0, 3, 7, 11,15});
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SVMTestGUI().setVisible(true);
            }
        });
    }
ArrayList<File> myFiles = new ArrayList();

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox10;
    private javax.swing.JCheckBox jCheckBox11;
    private javax.swing.JCheckBox jCheckBox12;
    private javax.swing.JCheckBox jCheckBox13;
    private javax.swing.JCheckBox jCheckBox14;
    private javax.swing.JCheckBox jCheckBox15;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JCheckBox jCheckBox5;
    private javax.swing.JCheckBox jCheckBox6;
    private javax.swing.JCheckBox jCheckBox7;
    private javax.swing.JCheckBox jCheckBox8;
    private javax.swing.JCheckBox jCheckBox9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables

}

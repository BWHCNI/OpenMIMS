/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

/**
 *
 * @author zkaufman
 */
public class MimsRatioManager extends PlugInJFrame implements ActionListener {

   static Frame instance;
   HSIView hsiview;
   UI ui;
   
   ButtonGroup numeratorGroup;
   ButtonGroup denomatorGroup;
   
   
   public MimsRatioManager(HSIView hsiview, UI ui){
      super("Ratio Manager");      
      this.ui = ui;
      this.hsiview = hsiview;
      if (instance != null) {
            instance.toFront();
            return;
      }
      instance = this;      
      
      // Get the mass names.
      String[] massNames = ui.getMimsImage().getMassNames();
      
      // Create the numerator and denominator panel.
      JPanel numeratorPanel = new JPanel(new GridLayout(0,1));      
      JPanel denomatorPanel = new JPanel(new GridLayout(0,1));
      numeratorPanel.add(new JLabel("  numerator: "));
      denomatorPanel.add(new JLabel("denominator: "));
      numeratorPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 25, 5));
      denomatorPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 25, 10));
      
      // Create the button panel
      JPanel buttonPanel = new JPanel();
      JButton add = new JButton("Add");
      add.addActionListener(this);
      JButton cancel = new JButton("Cancel");
      cancel.addActionListener(this);
      buttonPanel.add(add);
      buttonPanel.add(cancel);
      
      // Create the button group.
      numeratorGroup = new ButtonGroup();
      denomatorGroup = new ButtonGroup();                
                 
      // Loop over masses and create buttons
      for (int i = 0; i < massNames.length; i++){  
         
         // Must create two instances of the button.
         JRadioButton jrb_num = new JRadioButton("m"+massNames[i]);
         jrb_num.setName((new Integer(i)).toString());
         JRadioButton jrb_den = new JRadioButton("m"+massNames[i]);
         jrb_den.setName((new Integer(i)).toString());
         
         // Add radiobutton to the group.
         numeratorGroup.add(jrb_num);
         denomatorGroup.add(jrb_den);
         
         // Add radiobutton to the panel.
         numeratorPanel.add(jrb_num);            
         denomatorPanel.add(jrb_den);                    
      }     
      
      // Add panels to the frame    
      setLayout(new FlowLayout());
      add(numeratorPanel);
      add(new JSeparator(JSeparator.VERTICAL));
      add(denomatorPanel);   
      add(new JSeparator(JSeparator.HORIZONTAL));           
      add(buttonPanel);
               
      setSize(new Dimension(250, 275));     
   }      
   
   public void actionPerformed(ActionEvent e) {
      JRadioButton num = null;
      JRadioButton den = null;              
        
      if (e.getActionCommand() == "Add") {
         
        // Determine the selected radio button in the numerator group.
        for (Enumeration enu=numeratorGroup.getElements(); enu.hasMoreElements();) {
            JRadioButton b = (JRadioButton)enu.nextElement();
            if (b.getModel() == numeratorGroup.getSelection()) {
                num = b;
            }
        }
        
        // Determine the selected radio button in the denominator group.
        for (Enumeration enu=denomatorGroup.getElements(); enu.hasMoreElements();) {
            JRadioButton b = (JRadioButton)enu.nextElement();
            if (b.getModel() == denomatorGroup.getSelection()) {
                den = b;
            }
        }
        
        hsiview.addToRatioList(new Integer(num.getName()), new Integer(den.getName()));
      }
      
      super.close();
      instance = null;
      this.setVisible(false);         
   }
   
   
    // Returns a reference to the MimsRatioManager
    // or null if it is not open.
    public static MimsRatioManager getInstance() {
        return (MimsRatioManager) instance;
    }
   
    // Show the frame.
    public void showFrame() {
        setLocation(400, 400);
        setVisible(true);
        toFront();
        setExtendedState(NORMAL);
    }   
}

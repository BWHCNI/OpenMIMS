/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims;
import com.nrims.*;
import java.awt.event.ActionEvent;


/**
 *
 * @author cpoczatek
 */
public class imageNotes extends com.nrims.PlugInJFrame implements java.awt.event.ActionListener {

    java.awt.Frame instance;
    private javax.swing.JTextArea textArea;
    private javax.swing.JScrollPane scrollPane;

    public imageNotes() {
        super("Image Notes:");
        super.setDefaultCloseOperation(PlugInJFrame.HIDE_ON_CLOSE);
        setSize(new java.awt.Dimension(350, 400));

        scrollPane = new javax.swing.JScrollPane();
        this.add(scrollPane);
        textArea = new javax.swing.JTextArea("");
        textArea.setColumns(50);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        scrollPane.setViewportView(textArea);

    }

    public String getNoteText() {
        return textArea.getText();
    }

    public void setNoteText(String note) {
        textArea.setText(note);
    }

    public String getOutputFormatedText() {
        String text = textArea.getText();
        text = text.replaceAll("(\r)|(\f)", "\n");
        return text.replaceAll("\n", "&/&/&");
    }

    public void setOutputFormatedText(String text) {
        textArea.setText(text.replaceAll("&/&/&","\n"));
    }

    public void actionPerformed(ActionEvent e) {

    }
    }
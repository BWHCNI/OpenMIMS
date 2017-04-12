package com.nrims;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The imageNotes class provides the user with an interface for writing and storing notes.
 *
 * @author cpoczatek
 */
public class imageNotes extends com.nrims.PlugInJFrame implements DocumentListener {

    //java.awt.Frame instance;
    private javax.swing.JTextArea textArea;
    private javax.swing.JScrollPane scrollPane;
    private boolean isModified = false;

    /**
     * Constructor. Instantiates class.
     */
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
        textArea.getDocument().addDocumentListener(this);

        scrollPane.setViewportView(textArea);
    }

    public void changedUpdate(DocumentEvent evt) {
        isModified = true;
    }

    public void removeUpdate(DocumentEvent e) {
        isModified = true;
    }

    public void insertUpdate(DocumentEvent evt) {
        // Gets called when imageNotes in instantiated, also when user types into the textArea.
//        DocumentEvent.EventType type = evt.getType();
//        String typeString = null;
//        if (type.equals(DocumentEvent.EventType.CHANGE)) {
//          typeString = "Change";
//        }  else if (type.equals(DocumentEvent.EventType.INSERT)) {
//          typeString = "Insert";
//        }  else if (type.equals(DocumentEvent.EventType.REMOVE)) {
//          typeString = "Remove";
//        }
        isModified = true;
    }

    /**
     * Gets the text entered into the text area. Returns a formatted String.
     *
     * @return replacedText
     */
    public String getOutputFormatedText() {
        String text = textArea.getText();
        text = text.replaceAll("(\r)|(\f)", "\n");
        String replacedText = text.replaceAll("\n", "&/&/&");
        return replacedText;
    }

    /**
     * Sets the test in the text area. Must be formatted by the <code>getOutputFormatedText</code> method.
     *
     * @param text text to be put in text area
     */
    public void setOutputFormatedText(String text) {
        textArea.setText(text.replaceAll("&/&/&", "\n"));
    }

    /**
     * Returns true if the text of the note has been changed.
     *
     * @return isModified
     */
    public boolean isModified() {
        return isModified;
    }

    /**
     * Sets the isModified flag to true or false.
     *
     * @param modified the modified flag.  Duh.
     */
    public void setIsModified(boolean modified) {
        isModified = modified;
    }
}

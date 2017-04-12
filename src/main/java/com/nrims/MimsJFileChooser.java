package com.nrims;

import com.nrims.data.MIMSFileFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class MimsJFileChooser extends JFileChooser implements PropertyChangeListener, ActionListener {

    UI ui;
    FilterAccessory fa;
    private static final Dimension FILE_CHOOSER_DIMENSION = new Dimension(800, 500);  // 650, 500

    /**
     * MimsJFileChooser.java
     *
     * Custom JFileChooser that contains a filter accessory and keeps the 'lastFolder' field up to date.
     *
     * @param ui a reference to the UI object
     * @author zkaufman
     */
    public MimsJFileChooser(UI ui) {
        this.ui = ui;

        // Make sure filechooser points to the last place we opened something.
        String lastFolder = ui.getLastFolder();
        if (lastFolder != null) {
            File lastFolderDir = new File(lastFolder);
            if (lastFolderDir.exists()) {
                setCurrentDirectory(lastFolderDir);
            }
        } else {
            String ijDef = ij.io.OpenDialog.getDefaultDirectory();
            if (ijDef != null) {
                File ijDefDir = new File(ijDef);
                if (ijDefDir.exists()) {
                    setCurrentDirectory(ijDefDir);
                }
            }
        }

        setPreferredSize(FILE_CHOOSER_DIMENSION);

        MIMSFileFilter mff_nrrd = new MIMSFileFilter("nrrd");
        mff_nrrd.setDescription("Mims image");
        addChoosableFileFilter(mff_nrrd);

        MIMSFileFilter mff_im = new MIMSFileFilter("im");
        mff_im.setDescription("Mims image");
        addChoosableFileFilter(mff_im);

        MIMSFileFilter mff_img = new MIMSFileFilter("im");
        mff_img.addExtension("nrrd");
        mff_img.setDescription("Mims image");
        addChoosableFileFilter(mff_img);

        // Add the filter accessory.
        fa = new FilterAccessory();
        setAccessory(fa);

        // Add listeners.
        addPropertyChangeListener(this);
        addActionListener(this);
    }

    // If user changes directories we must filter again.
    public void propertyChange(PropertyChangeEvent evt) {
        String pname = evt.getPropertyName();
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(pname)) {
            fa.filterFiles();
        }
    }

    // If user hits OK button or CANCEL button, update last directory
    public void actionPerformed(ActionEvent e) {
        String aname = e.getActionCommand();
        if (JFileChooser.APPROVE_SELECTION.equals(aname)
                || JFileChooser.CANCEL_SELECTION.equals(aname)) {
            ui.setLastFolder(getCurrentDirectory());
            ui.setIJDefaultDir(getCurrentDirectory().getAbsolutePath());
        }
    }

    // Always ask user if they want to overwrite existing file.
    @Override
    public void approveSelection() {
        if (getDialogType() == SAVE_DIALOG) {
            File file = getSelectedFile();
            if ((file != null) && file.exists()) {
                int answer = JOptionPane.showConfirmDialog(
                        this, file + " exists. Overwrite?", "Overwrite?",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (answer != JOptionPane.OK_OPTION) {
                    return;
                }
            }
        }
        super.approveSelection();
    }

    // Checks to see if the current directory is read-only
    public boolean isCurrentDirReadOnly() {
        if (!getCurrentDirectory().canWrite()) {
            return true;
        } else {
            return false;
        }

    }

    // This accessory has a text field that the user
    // can use for filtering which files will be displayed.
    class FilterAccessory extends JPanel {

        SimpleMIMSFileFilter mff;
        JTextField jtextfield;

        public FilterAccessory() {

            // Create components.
            JPanel cpanel = new JPanel();
            JLabel label = new JLabel("Filter:");
            jtextfield = new JTextField(10);

            // Set layout.
            setLayout(new BorderLayout());
            add(label, BorderLayout.NORTH);
            cpanel.add(jtextfield);
            add(cpanel, BorderLayout.CENTER);

            // Add key listener so that filtering occurs everytime
            // a character is typed.
            KeyListener keyListener = new KeyListener() {
                public void keyPressed(KeyEvent keyEvent) {
                }

                public void keyTyped(KeyEvent keyEvent) {
                }

                public void keyReleased(KeyEvent keyEvent) {
                    filterFiles();
                }
            };
            jtextfield.addKeyListener(keyListener);
        }

        // Grab the text in the textfield and do the filtering.
        public void filterFiles() {

            removeChoosableFileFilter(mff);
            File dir = getCurrentDirectory();

            // Get the text. Empty string to be treated as "*".
            String filterString = jtextfield.getText();
            if (filterString.trim().length() == 0) {
                filterString = "*";
            }
            filterString = "*" + filterString + "*";

            // Get the list of files that satifies search.
            FileFilter fileFilter = new WildcardFileFilter(filterString);
            File[] filelist = dir.listFiles(fileFilter);
            ArrayList<File> files = new ArrayList<File>();
            for (int i = 0; i < filelist.length; i++) {
                files.add(filelist[i]);
            }

            // Create and set filter.
            mff = new SimpleMIMSFileFilter(files);
            setFileFilter(mff);
        }
    }

    class SimpleMIMSFileFilter extends javax.swing.filechooser.FileFilter {

        ArrayList<File> files;

        public SimpleMIMSFileFilter(ArrayList<File> files) {
            this.files = files;
        }

        // Accept all entries that exist in the ArrayList.
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            if (files.contains(f)) {
                return true;
            } else {
                return false;
            }
        }

        // The description of this filter
        public String getDescription() {
            return "Custom Filter";
        }

    }

}

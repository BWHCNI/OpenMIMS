package SVM;

import com.nrims.segmentation.*;

/**
 *
 * @author reckow
 */
public class SVMSetupDialog extends EngineSetupDialog {

    private SVMSetupPanel panel;

    public SVMSetupDialog(javax.swing.JDialog parent) {
        super(parent);
        this.panel = new SVMSetupPanel();
        super.setSetupPanel(panel);
    }

    @Override
    public void getProperties(SegmentationProperties props) {
        props.setValueOf(SvmEngine.KERNELTYPE, panel.getKernelType());
        props.setValueOf(SvmEngine.CVFOLD, panel.getCVFold());
    }

    @Override
    public void setProperties(SegmentationProperties props) {
        int cvFold = 5; // default
        if (props.getValueOf(SvmEngine.CVFOLD) != null) {
            cvFold = (Integer) props.getValueOf(SvmEngine.CVFOLD);
        }
        panel.setCVFold(cvFold);

        int kernelType = 1; // default;
        if (props.getValueOf(SvmEngine.KERNELTYPE) != null) {
            kernelType = (Integer) props.getValueOf(SvmEngine.KERNELTYPE);
        }
        panel.setKernelType(kernelType);
    }
}

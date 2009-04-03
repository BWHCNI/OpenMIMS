package SVM;

import com.nrims.segmentation.SegmentationProperties;
import SVM.libsvm.svm_model;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author pgormanns
 */
public class SvmEngine extends com.nrims.segmentation.SegmentationEngine {

    public static final int RBF_KERNEL = 1,  LINEAR_KERNEL = 2,  SIGMOID_KERNEL = 3;
    public static final String SCALINGPARAMS = "scalingParams",  CVFOLD = "CV_fold",  KERNELTYPE = "kernel_type";

    public SvmEngine(int type, ArrayList<ArrayList<ArrayList<Double>>> data, SegmentationProperties props) {
        super(type, data, props);
    }

    @Override
    public void checkParams(SegmentationProperties props) {
        if (props.getValueOf(CVFOLD) == null) {
            props.setValueOf(CVFOLD, 5);
        }
        if (props.getValueOf(KERNELTYPE) == null) {
            props.setValueOf(KERNELTYPE, RBF_KERNEL);
        }
    }

    @Override
    protected boolean train() throws Exception {
        this.setProgress(10);
        ArrayList<String> convData = convertData();
        svm_scale sc = new svm_scale();
        ArrayList<String> tempTrainData = sc.run(convData, this.getProperties(), true);

        this.setProgress(50);
        svm_train st = new svm_train();
        svm_model model = st.run(this.getProperties(), tempTrainData);
        this.getProperties().setValueOf(MODEL, model);

        this.setProgress(100);
        return true;

    }

    @Override
    protected boolean predict() throws Exception {
        this.setProgress(10);
        ArrayList<String> convData = convertData();
        svm_scale sc = new svm_scale();
        ArrayList<String> tempTestData = sc.run(convData, this.getProperties(), false);

        this.setProgress(50);
        svm_model model = (svm_model) this.getProperties().getValueOf(MODEL);
        ArrayList<String> prediction = svm_predict.predict(tempTestData, model, 0);

        this.setProgress(90);
        byte[] classMap = createOutput(prediction);
        this.setClassification(classMap);

        this.setProgress(100);
        return true;
    }

    public ArrayList<String> convertData() {
        ArrayList<String> convData = new ArrayList<String>();
        for (int u = 0; u < this.getData().size(); u++) {
            Iterator pointIT = this.getData().get(u).iterator();
            while (pointIT.hasNext()) {
                Iterator featureIT = ((ArrayList) pointIT.next()).iterator();
                String res = u + "";
                int i = 0;
                while (featureIT.hasNext()) {// for (int i =0; i<currentFeatures.length; i++){

                    res += " " + (i + 1) + ":" + (Double) featureIT.next();
                    i++;
                }
                convData.add(res);
            }
        }
        return convData;
    }

    private byte[] createOutput(ArrayList<String> data) {
        byte[] pixels = new byte[data.size()];
        Iterator dataIT = data.iterator();
        int i = 0;
        while (dataIT.hasNext()) {
            pixels[i] = new Byte((((String) dataIT.next()).split("\\.")[0]));
            i++;
        }
        return pixels;
    }

   
}

package SVM;


import com.nrims.segmentation.SegmentationProperties;
import java.io.*;
import java.util.*;
/**
 *
 * original class from libSVM
 * modified to fit NRIMS by: pgormanns
 */

class svm_scale {

    private String line = null;
    private double lower = -1.0;
    private double upper = 1.0;
    private double[] feature_max;
    private double[] feature_min;
    private int max_index;
    private boolean trainData = true;
    private Hashtable<String, double[]> scalingParams = new Hashtable();

    public void setDatatype(boolean train) {

        this.trainData = train;
    }

    private double output(int index, double value) {

        if (feature_max[index] == feature_min[index]) {
            return 0.0;
        }

        if (value == feature_min[index]) {
            value = lower;
        } else if (value == feature_max[index]) {
            value = upper;
        } else {
            value = lower + (upper - lower) *
                    (value - feature_min[index]) /
                    (feature_max[index] - feature_min[index]);
        }
        return value;
    }

    public ArrayList<String> run(ArrayList<String> data, SegmentationProperties props, boolean train) throws IOException {
        int i, index;


        Iterator dataIT = data.iterator();

        max_index = 0;
        while (dataIT.hasNext()) {
            //canceled?

            this.line = (String) dataIT.next();
            StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
            st.nextToken();
            while (st.hasMoreTokens()) {
                index = Integer.parseInt(st.nextToken());
                max_index = Math.max(max_index, index);
                st.nextToken();
            }
        }

        try {
            feature_max = new double[(max_index + 1)];
            feature_min = new double[(max_index + 1)];
        } catch (OutOfMemoryError e) {
            System.err.println("can't allocate enough memory");
            System.exit(1);
        }

        for (i = 0; i <= max_index; i++) {
            feature_max[i] = -Double.MAX_VALUE;
            feature_min[i] = Double.MAX_VALUE;
        }

        dataIT = data.iterator();
        /* pass 2: find out min/max value */
        while (dataIT.hasNext()) {

            this.line = (String) dataIT.next();
            int next_index = 1;
            double target;
            double value;

            StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
            target = Double.parseDouble(st.nextToken());

            while (st.hasMoreTokens()) {
                index = Integer.parseInt(st.nextToken());
                value = Double.parseDouble(st.nextToken());

                for (i = next_index; i < index; i++) {
                    feature_max[i] = Math.max(feature_max[i], 0);
                    feature_min[i] = Math.min(feature_min[i], 0);
                }

                feature_max[index] = Math.max(feature_max[index], value);
                feature_min[index] = Math.min(feature_min[index], value);
                next_index = index + 1;
            }

            for (i = next_index; i <= max_index; i++) {
                feature_max[i] = Math.max(feature_max[i], 0);
                feature_min[i] = Math.min(feature_min[i], 0);
            }
        }

        String idx;
        double[] minMax = new double[2];

        if (!train) {

            this.scalingParams = (Hashtable) props.getValueOf(SvmEngine.SCALINGPARAMS);
            Iterator scalingIT = scalingParams.keySet().iterator();
            while (scalingIT.hasNext()) {

                idx = (String) scalingIT.next();
                minMax = scalingParams.get(idx);
                int tempID = Integer.valueOf(idx).intValue();
                feature_min[tempID] = minMax[0];
                feature_max[tempID] = minMax[1];
            }
        } else {

            for (i = 1; i <= max_index; i++) {
                if (feature_min[i] != feature_max[i]) {
                    this.scalingParams.put(i + "", new double[]{feature_min[i], feature_max[i]});
                    System.out.println(i + " " + feature_min[i] + " " + feature_max[i]);
                }
            }
            props.setValueOf(SvmEngine.SCALINGPARAMS, scalingParams);
        }
        /* pass 3: scale */
        ArrayList<String> scaledData = new ArrayList();
        dataIT = data.iterator();
        while (dataIT.hasNext()) {
            line = (String) dataIT.next();
            int next_index = 1;
            double target;
            double value;
            int u = 1;
            String res = line.substring(0, 1);
            StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
            target = Double.parseDouble(st.nextToken());
            while (st.hasMoreElements()) {
                index = Integer.parseInt(st.nextToken());
                value = Double.parseDouble(st.nextToken());
                res += " " + u + ":" + output(index, value);
                u += 1;
                next_index = index + 1;
            }
            scaledData.add(res);
        }
        return scaledData;
    }
}

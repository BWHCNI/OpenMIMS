package SVM;

import SVM.libsvm.*;
import java.io.*;
import java.util.*;

/**
 *
 * original class from libSVM
 * modified to fit NRIMS by pgormanns
 */


class svm_predict {

    private static double atof(String s) {
        return Double.valueOf(s).doubleValue();
    }

    private static int atoi(String s) {
        return Integer.parseInt(s);
    }

    public static ArrayList<String> predict(ArrayList<String> data, svm_model model, int predict_probability) throws IOException {
        int correct = 0;
        int total = 0;
        double error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;

        int svm_type = svm.svm_get_svm_type(model);
        int nr_class = svm.svm_get_nr_class(model);
        double[] prob_estimates = null;
        ArrayList<String> listOutput = new ArrayList<String>();
        String res = "";
        Iterator dataIT = data.iterator();
        if (predict_probability == 1) {
            if (svm_type == svm_parameter.EPSILON_SVR ||
                    svm_type == svm_parameter.NU_SVR) {
                System.out.print("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma=" + svm.svm_get_svr_probability(model) + "\n");
            } else {
                int[] labels = new int[nr_class];
                svm.svm_get_labels(model, labels);
                prob_estimates = new double[nr_class];
      
                res += "labels";
                for (int j = 0; j < nr_class; j++) {
                    res += " " + labels[j];
               
                }
 
                listOutput.add(res);
            }
        }

        while (dataIT.hasNext()) {  
          
            res = "";
            String line = (String) dataIT.next();
            if (line == null) {
                break;
            }

            StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");

            double target = atof(st.nextToken());
            int m = st.countTokens() / 2;
            svm_node[] x = new svm_node[m];
            for (int j = 0; j < m; j++) {
                x[j] = new svm_node();
                x[j].index = atoi(st.nextToken());
                x[j].value = atof(st.nextToken());
            }

            double v;
            if (predict_probability == 1 && (svm_type == svm_parameter.C_SVC || svm_type == svm_parameter.NU_SVC)) {
                v = svm.svm_predict_probability(model, x, prob_estimates);
             
                for (int j = 0; j < nr_class; j++) {
                    res += prob_estimates[j] + " ";

                }

                listOutput.add(res);
                res = "";
            } else {
                v = svm.svm_predict(model, x);
                res += v;

                listOutput.add(res);
                res = "";
            }

            if (v == target) {
                ++correct;
            }
            error += (v - target) * (v - target);
            sumv += v;
            sumy += target;
            sumvv += v * v;
            sumyy += target * target;
            sumvy += v * target;
            ++total;
        }
        if (svm_type == svm_parameter.EPSILON_SVR ||
                svm_type == svm_parameter.NU_SVR) {
            System.out.print("Mean squared error = " + error / total + " (regression)\n");
            System.out.print("Squared correlation coefficient = " +
                    ((total * sumvy - sumv * sumy) * (total * sumvy - sumv * sumy)) /
                    ((total * sumvv - sumv * sumv) * (total * sumyy - sumy * sumy)) +
                    " (regression)\n");
        } else {
            System.out.print("Accuracy = " + (double) correct / total * 100 +
                    "% (" + correct + "/" + total + ") (classification)\n");
        }
        return listOutput;
    }

    public static void run(ArrayList<String> data, svm_model model) throws IOException {
        int  predict_probability = 0;



            if (predict_probability == 1) {
                if (svm.svm_check_probability_model(model) == 0) {
                    System.err.print("Model does not support probabiliy estimates\n");
                    System.exit(1);
                }
            } else {
                if (svm.svm_check_probability_model(model) != 0) {
                    System.out.print("Model supports probability estimates, but disabled in prediction.\n");
                }
            }
         


    }
}

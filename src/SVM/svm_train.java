package SVM;

import NelderMead.Function;
import NelderMead.NelderMead;
import NelderMead.Point;
import SVM.libsvm.*;
import com.nrims.segmentation.SegmentationProperties;
import java.io.*;
import java.util.*;

/**
 *
 * original class from libSVM
 * modified to fit NRIMS by pgormanns
 */

class svm_train {

    private svm_parameter param;		// set by parse_command_line
    private svm_problem prob;		// set by read_problem
    private svm_model model;
    private ArrayList<String> data;

    private double do_cross_validation(int g, int cCost, int cvFold) {

//                this.param.C=gamma[e];
//                this.param.gamma = gamma[r];

        this.param.gamma = g;
        this.param.C = cCost;
        int i;
        int total_correct = 0;
        double total_error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
        double[] target = new double[prob.l];
        double accuracy = 0.0;

        svm.svm_cross_validation(prob, param, cvFold, target);
        if (param.svm_type == svm_parameter.EPSILON_SVR ||
                param.svm_type == svm_parameter.NU_SVR) {
            for (i = 0; i < prob.l; i++) {
                double y = prob.y[i];
                double v = target[i];
                total_error += (v - y) * (v - y);
                sumv += v;
                sumy += y;
                sumvv += v * v;
                sumyy += y * y;
                sumvy += v * y;
            }
        //System.out.print("Cross Validation Mean squared error = "+total_error/prob.l+"\n");
        //System.out.print("Cross Validation Squared correlation coefficient = "+
        //((prob.l*sumvy-sumv*sumy)*(prob.l*sumvy-sumv*sumy))/
        //((prob.l*sumvv-sumv*sumv)*(prob.l*sumyy-sumy*sumy))+"\n"
        //);
        } else {
            for (i = 0; i < prob.l; i++) {
                if (target[i] == prob.y[i]) {
                    ++total_correct;
                }
            }
            accuracy = 100.0 * total_correct / prob.l;
            System.out.print("Cross Validation Accuracy = " + 100.0 * total_correct / prob.l + "%"+ " C = " + this.param.C + " g = " + this.param.gamma + "\n");
        }

        return accuracy;
//                     }
//            }

    }

    public svm_model run(SegmentationProperties props, ArrayList<String> data) throws IOException {
        double bestAcc = 0.0;
        int bestg = 0;
        int bestc = 0;
        this.data = data;
        int cvFold = (Integer) props.getValueOf(SvmEngine.CVFOLD);
        initParameters(props);
        read_problem();


        //testing parameter search
        //case 0 -> grid,  case 1 -> NelderMead

        int gridmethod = 1;

        if (gridmethod == 0) {
            //parameter grid search
            int[] gamma = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
            //Original default grid
            //   int[] gamma = new int[]{0,4,8,12,16,20,24,28,32};
            for (int g = 0; g < gamma.length; g++) {

                for (int c = 0; c < gamma.length; c++) {

                    double acc = do_cross_validation(g, c, 2);
                    if (acc > bestAcc) {
                        bestAcc = acc;
                        bestg = g;
                        bestc = c;
                    }
                }
            }

            System.out.print("times = " + 31 * 31 + ", gamma = " + bestg + ", c = " + bestc + ", acc = " + bestAcc + "%\n");

        } else if (gridmethod == 1) {

            //Nelder-Mead search
        Function cost = new accuracyCostFunction();
        Point[] initialSimplex = {
            new Point(new double[]{(3200 * Math.random()), (3200 * Math.random())}),
            new Point(new double[]{(3200 * Math.random()), (3200 * Math.random())}),
            new Point(new double[]{(3200 * Math.random()), (3200 * Math.random())})};

        NelderMead nm = new NelderMead(cost, initialSimplex);
        Point minP = nm.search();
        bestg = (int) java.lang.Math.round(minP.get(1) / 100);
        bestc = (int) java.lang.Math.round(minP.get(2) / 100);
        bestAcc = 100 - cost.at(minP);

        double min = cost.at(minP);
        System.out.print("times = " + cost.getTimesCalled() + ", gamma = " + param.gamma + ", c = " + param.C + ", acc = " + min + "%\n");
        }

        param.gamma = bestg;
        param.C = bestc;
        System.out.println(bestAcc);

        model = svm.svm_train(prob, param);

        return model;
    }

    private static double atof(String s) {
        double d = Double.valueOf(s).doubleValue();

        return (d);
    }

    private static int atoi(String s) {
        return Integer.parseInt(s);
    }

    private void initParameters(SegmentationProperties props) {
        int i;

        param = new svm_parameter();
        // default values
        int kernelType = (Integer) props.getValueOf(SvmEngine.KERNELTYPE);
        param.svm_type = svm_parameter.C_SVC;
        switch (kernelType) {

            case 1:
                param.kernel_type = svm_parameter.RBF;
                break;

            case 2:
                param.kernel_type = svm_parameter.LINEAR;
                break;

            case 3:
                param.kernel_type = svm_parameter.SIGMOID;
                break;
        }


        param.degree = 3;
        param.gamma = 0;	// 1/k
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = 1;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 0;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];

    // parse options
//		for(i=0;i<argv.length;i++)
//		{
//			if(argv[i].charAt(0) != '-') break;
//
//
//			switch(argv[i-1].charAt(1))
//			{
//				case 's':
//					param.svm_type = atoi(argv[i]);
//					break;
//				case 't':
//					param.kernel_type = atoi(argv[i]);
//					break;
//				case 'd':
//					param.degree = atoi(argv[i]);
//					break;
//				case 'g':
//					param.gamma = atof(argv[i]);
//					break;
//				case 'r':
//					param.coef0 = atof(argv[i]);
//					break;
//				case 'n':
//					param.nu = atof(argv[i]);
//					break;
//				case 'm':
//					param.cache_size = atof(argv[i]);
//					break;
//				case 'c':
//					param.C = atof(argv[i]);
//					break;
//				case 'e':
//					param.eps = atof(argv[i]);
//					break;
//				case 'p':
//					param.p = atof(argv[i]);
//					break;
//				case 'h':
//					param.shrinking = atoi(argv[i]);
//					break;
//			        case 'b':
//					param.probability = atoi(argv[i]);
//					break;
//				case 'v':
//					cross_validation = 1;
//					nr_fold = atoi(argv[i]);
//
//					break;
//				case 'w':
//					++param.nr_weight;
//					{
//						int[] old = param.weight_label;
//						param.weight_label = new int[param.nr_weight];
//						System.arraycopy(old,0,param.weight_label,0,param.nr_weight-1);
//					}
//
//					{
//						double[] old = param.weight;
//						param.weight = new double[param.nr_weight];
//						System.arraycopy(old,0,param.weight,0,param.nr_weight-1);
//					}
//
//					param.weight_label[param.nr_weight-1] = atoi(argv[i-1].substring(2));
//					param.weight[param.nr_weight-1] = atof(argv[i]);
//					break;
//				default:
//					//System.err.print("unknown option\n");
//
//			}
//		}
//
//		// determine filenames
//
//
//
//
//		input_file_name = argv[i];
//
//		if(i<argv.length-1)
//			model_file_name = argv[i+1];
//		else
//		{
//			int p = argv[i].lastIndexOf('/');
//			++p;	// whew...
//			model_file_name = argv[i].substring(p)+".model";
//		}
    }

    // read in a problem (in svmlight format)
    private void read_problem() throws IOException {
        Iterator dataIT = this.data.iterator();
        //BufferedReader fp = new BufferedReader(new FileReader(input_file_name));
        Vector<Double> vy = new Vector<Double>();
        Vector<svm_node[]> vx = new Vector<svm_node[]>();
        int max_index = 0;

        while (dataIT.hasNext()) {
            String line = (String) dataIT.next();
            if (line == null) {
                break;
            }

            StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");

            vy.addElement(atof(st.nextToken()));
            int m = st.countTokens() / 2;
            svm_node[] x = new svm_node[m];
            for (int j = 0; j < m; j++) {
                x[j] = new svm_node();
                x[j].index = atoi(st.nextToken());
                x[j].value = atof(st.nextToken());
            }
            if (m > 0) {
                max_index = Math.max(max_index, x[m - 1].index);
            }
            vx.addElement(x);
        }

        prob = new svm_problem();
        prob.l = vy.size();
        prob.x = new svm_node[prob.l][];
        for (int i = 0; i < prob.l; i++) {
            prob.x[i] = vx.elementAt(i);
        }
        prob.y = new double[prob.l];
        for (int i = 0; i < prob.l; i++) {
            prob.y[i] = vy.elementAt(i);
        }

        if (param.gamma == 0) {
            param.gamma = 1.0 / max_index;
        }

        if (param.kernel_type == svm_parameter.PRECOMPUTED) {
            for (int i = 0; i < prob.l; i++) {
                if (prob.x[i][0].index != 0) {
                    //System.err.print("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
                    System.exit(1);
                }
                if ((int) prob.x[i][0].value <= 0 || (int) prob.x[i][0].value > max_index) {
                    //System.err.print("Wrong input format: sample_serial_number out of range\n");
                    System.exit(1);
                }
            }
        }

    //fp.close();
    }

    private class accuracyCostFunction implements Function {
        int times = 0;

        public double at(Point p) {
            times++;
            final int gamma = (int)java.lang.Math.round(p.get(1)/100);
            final int c = (int)java.lang.Math.round(p.get(2)/100);
            //cross validation level hard coded at 2
            return 100 - do_cross_validation(gamma, c, 2);
        }

        private double sq(double a) { return a * a; };

        public int getTimesCalled() { return times; }
    }


}

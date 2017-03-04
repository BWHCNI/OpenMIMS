/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SVM;

import java.util.ArrayList;

/**
 *
 * @author pgormanns
 */
public class svm_validation {

    private int fold;//
    private int c_begin,  c_end,  c_step;
    private int g_begin,  g_end,  g_step;
    private String dataset_pathname;

    public svm_validation(int fold, int[] c_setup, int[] g_setup, String path) {

        this.fold = fold;
        this.dataset_pathname = path;

        this.c_begin = c_setup[0];
        this.c_end = c_setup[1];
        this.c_step = c_setup[2];

        this.g_begin = g_setup[0];
        this.g_end = g_setup[1];
        this.g_step = g_setup[2];

    }

    public ArrayList<Integer> rangeCalc(int beginx, int stepx, int endx) {

        ArrayList<Integer> sequence = new ArrayList<Integer>();
        int begin = beginx;
        while (true) {
            if ((stepx > 0 && begin > endx) || (stepx < 0 && begin < endx)) {
                break;
            } else {
                sequence.add(begin);
                begin += stepx;
            }
        }
        return sequence;
    }

    public ArrayList<Integer> permuteSequences(ArrayList<Integer> seq) {

        int n = seq.size();
        if (n <= 1) {
            return seq;
        }
        int mid = n / 2;
        ArrayList<Integer> left_temp = new ArrayList<Integer>();
        for (int j = 0; j < mid; j++) {

            left_temp.add(seq.get(j));

        }
        ArrayList<Integer> left = permuteSequences(left_temp);

        ArrayList<Integer> right_temp = new ArrayList<Integer>();
        for (int h = mid + 1; h < n; h++) {

            right_temp.add(seq.get(h));

        }
        ArrayList<Integer> right = permuteSequences(right_temp);
        ArrayList<Integer> ret = new ArrayList<Integer>();
        ret.add(seq.get(mid));

        while (!left.isEmpty() || !right.isEmpty()) {

            if (!left.isEmpty()) {

                ret.add(left.get(0));
                left.remove(0);
            }
            if (!right.isEmpty()) {

                ret.add(right.get(0));
                right.remove(0);

            }
        }
        return ret;
    }

    public ArrayList<ArrayList> calcJobs() {



        ArrayList<Integer> cSeq = permuteSequences(rangeCalc(this.c_begin, this.c_step, this.c_end));
        ArrayList<Integer> gSeq = permuteSequences(rangeCalc(this.g_begin, this.g_step, this.g_end));

        float nr_c = cSeq.size();
        float nr_g = gSeq.size();
        int i = 0;
        int j = 0;
        ArrayList<ArrayList> jobs = new ArrayList<ArrayList>();

        while (i < nr_c || j < nr_g) {

            if (i / nr_c < j / nr_g) {

                ArrayList<int[]> current = new ArrayList<int[]>();
                for (int k = 0; k < j; k++) {

                    current.add(new int[]{cSeq.get(i), gSeq.get(k)});
                    i = i + 1;
                    jobs.add(current);
                }

            } else {

                ArrayList<int[]> current = new ArrayList<int[]>();
                for (int k = 0; k < i; k++) {

                    current.add(new int[]{cSeq.get(k), gSeq.get(j)});
                    j = +1;
                    jobs.add(current);
                }
            }
        }
        return jobs;
    }
}
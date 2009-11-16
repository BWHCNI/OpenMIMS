/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * NelderMead.java
 * Created on Mar 26, 2005
 */
package NelderMead;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A Nelder-Mead simplex search.
 */
public final class NelderMead {

    private final Function f;
    private final int dim;
    private final List/*<Point>*/ simplex;
    private final int minIdx = 0, maxIdx, nextIdx;
    private final Comparator pointComparator = new PointComparator();

    private final Map/*<Point, Double>*/ point2Value = new HashMap();
    private void putValue(Point p, double d) {
        point2Value.put(p, new Double(d));
    }
    private double getValue(Point p) {
        return ((Double)point2Value.get(p)).doubleValue();
    }

    public NelderMead(Function f, Point[] initialSimplex) {
        this.f = f;
        this.dim = initialSimplex.length - 1;
        this.simplex = new ArrayList(Arrays.asList(initialSimplex));
        this.maxIdx = simplex.size() - 1;
        this.nextIdx = maxIdx - 1;
    }

    public Point search() {
        // evaluate the function at each point in simplex,
        for(Iterator/*<Point>*/ i = simplex.iterator(); i.hasNext();) {
            Point p = (Point)i.next();
            putValue(p, f.at(p));
        }
        //printData();

        // sort the points and get the best, worst, and next to worst
        Collections.sort(simplex, pointComparator);
        Point minP  = (Point)simplex.get(minIdx);
        Point maxP  = (Point)simplex.get(maxIdx);
        Point nextP = (Point)simplex.get(nextIdx);
        double min  = getValue(minP);
        double max  = getValue(maxP);
        double next = getValue(nextP);
        printPointValue("min", minP, min);
        printPointValue("max", maxP, max);
        printPointValue("next", nextP, next);

        //hard coded parameter limits
        double xmin = 0;
        double xmax = 3200;
        double ymin = 0;
        double ymax = 3200;

        //if we're above 98% return
        if(min < 2.0) return minP;

        // sum all but the worst point
        Point total = new Point(dim);
        for(Iterator/*<Point>*/ i = simplex.iterator(); i.hasNext();) {
            Point p = (Point)i.next();
            if (!p.equals(maxP)) total = total.add(p);
        }
        Point mean = total.div(dim);
        System.out.println("mean: " + mean);

        // reflect
        Point reflect = mean.mul(2).sub(maxP);
        if ( reflect.coords[0] >= xmin && reflect.coords[0] <= xmax && reflect.coords[1] >= ymin && reflect.coords[1] <= ymax)  {
        double reflectVal = f.at(reflect);
        printPointValue("reflect", reflect, reflectVal);

        // check if reflect or reflectGrow is better
        if (reflectVal < min) {
            Point reflectGrow = mean.mul(3).sub(maxP.mul(2));
             if ( reflectGrow.coords[0] >= xmin && reflectGrow.coords[0] <= xmax && reflectGrow.coords[1] >= ymin && reflectGrow.coords[1] <= ymax) {
            double reflectGrowVal = f.at(reflectGrow);
            printPointValue("reflectGrow", reflectGrow, reflectGrowVal);

            if (reflectGrowVal < reflectVal) {
                System.out.println("reflect and grow!");
                simplex.set(maxIdx, reflectGrow);
                // return checkStop(max, reflectGrowVal, reflectGrow);
                return checkStopArea(max, reflectGrowVal, minP, nextP, reflectGrow);
            } else {
                System.out.println("reflect!");
                simplex.set(maxIdx, reflect);
                // return checkStop(max, reflectVal, reflect);
                return checkStopArea(max, reflectVal, minP, nextP, reflect);
            }
        } else if (reflectVal < next) {
            System.out.println("reflect!");
            simplex.set(maxIdx, reflect);
           //  return checkStop(max, reflectVal, reflect);
            return checkStopArea(max, reflectVal, minP, nextP, reflect);
        }
        }
        }
        // reflect and shrink
        Point reflectShrink = mean.mul(3).sub(maxP).div(2);
        if (reflectShrink.coords[0] >= xmin && reflectShrink.coords[0] <= xmax && reflectShrink.coords[1] >= ymin && reflectShrink.coords[1] <= ymax) {
        double reflectShrinkVal = f.at(reflectShrink);
        printPointValue("reflectShrink", reflectShrink, reflectShrinkVal);

        if (reflectShrinkVal < next) {
            System.out.println("reflect and shrink!");
            simplex.set(maxIdx, reflectShrink);
            // return checkStop(max, reflectShrinkVal, reflectShrink);
            return checkStopArea(max, reflectShrinkVal, minP, nextP, reflectShrink);
        }
        }
        // shrink
        Point shrink = mean.add(maxP).div(2);
        double shrinkVal = f.at(shrink);
        printPointValue("shrink", shrink, shrinkVal);

        if (shrinkVal < next && shrinkVal < min) {
            System.out.println("shrink!");
            simplex.set(maxIdx, shrink);
            // return checkStop(max, shrinkVal, shrink);
            return checkStopArea(max, shrinkVal, minP, nextP, shrink);
        }
        // shrink all
        System.out.println("shrink all!");
        for (ListIterator/*<Point>*/ i = simplex.listIterator(); i.hasNext();) {
            Point p = (Point)i.next();
            if (!p.equals(minP)) i.set(p.add(minP).div(2));
        }

        return search();
    }

   //  private Point checkStop(double oldMin, double newMin, Point p) {
        //printPointValue("new", p, newMin);
   //     return (oldMin - newMin < 0.5) ? p : search();
    // }

    private Point checkStopArea(double oldMin, double newMin, Point minP, Point nextP, Point p) {
        double edgeA = Math.sqrt( Math.pow((minP.coords[0] - nextP.coords[0]), 2) + Math.pow((minP.coords[1] - nextP.coords[1]),2)) ;
        double edgeB = Math.sqrt( Math.pow((nextP.coords[0] - p.coords[0]), 2) + Math.pow((nextP.coords[1] - p.coords[1]),2)) ;
        double edgeC =Math.sqrt( Math.pow((p.coords[0] - minP.coords[0]), 2) + Math.pow((p.coords[1] - minP.coords[1]),2))  ;
        double perimeter = edgeA + edgeB + edgeC;
        double s = perimeter / 2;
        double area = Math.sqrt(s * ( (s - edgeA) * (s - edgeB) * (s - edgeC) ) );
        //System.out.println(area + " | " + newMin + " | " + (NMMain.request() + 1));
        //Original oldMin - newMin < 0.01
        //stopping conditions of simplex area and improvement
        //improvement == 0.1% accuracy improvement WORNG?
        return (area < 1) &&  (oldMin - newMin < 10) ? p : search();

    }

    private void printData() {
        System.out.println();
        System.out.print("[");
        for(Iterator/*<Point>*/ i = simplex.iterator(); i.hasNext();) {
            Point p = (Point)i.next();
            double d = getValue(p);
            System.out.print(p + " = " + d);
            if (i.hasNext()) System.out.print("; ");
        }
        System.out.println("]");
    }

    private static void printPointValue(String s, Point p, double v) {
        System.out.println(s + " : " + p + " = " + v);
    }

    private class PointComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Point p1 = (Point)o1;
            Point p2 = (Point)o2;
            double v1 = getValue(p1);
            double v2 = getValue(p2);
            return Double.compare(v1, v2);
        }
    }

}


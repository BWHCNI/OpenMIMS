/*
 * Point.java
 * Created on Mar 26, 2005
 */
package NelderMead;

/**
 * A point of any dimension.
 */
public final class Point {

    public final double[] coords;

    public Point(double[] coords) {
        this.coords = new double[coords.length];
        System.arraycopy(coords, 0, this.coords, 0, coords.length);
    }

    public Point(int dim) {
        this.coords = new double[dim];
    }

    /** returns the ith coordinate (1-based) */
    public double get(int i) {
        return coords[i - 1];
    }

    public int dim() {
        return coords.length;
    }

    public Point add(Point p) {
        Point sum = new Point(this.coords);
        for(int i = 0; i < coords.length; i++) {
            sum.coords[i] += p.coords[i];
        }
        return sum;
    }

    public Point sub(Point p) {
        Point diff = new Point(this.coords);
        for(int i = 0; i < coords.length; i++) {
            diff.coords[i] -= p.coords[i];
        }
        return diff;
    }

    public Point mul(double a) {
        Point prod = new Point(this.coords);
        for(int i = 0; i < coords.length; i++) {
            prod.coords[i] *= a;
        }
        return prod;
    }

    public Point div(double a) {
        Point quot = new Point(this.coords);
        for(int i = 0; i < coords.length; i++) {
            quot.coords[i] /= a;
        }
        return quot;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("<");
        for(int i = 0; i < coords.length; i++) {
            sb.append(coords[i]);
            if (i != coords.length - 1) sb.append(", ");
        }
        return sb.append(">").toString();
    }

}

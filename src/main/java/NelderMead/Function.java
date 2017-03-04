/*
 * Function.java
 * Created on Mar 27, 2005
 */
package NelderMead;

/**
 * A function from a vector to a scalar.
 */
public interface Function {

    /** evaluate the function at p*/
    public double at(Point p);
    public int getTimesCalled();

}

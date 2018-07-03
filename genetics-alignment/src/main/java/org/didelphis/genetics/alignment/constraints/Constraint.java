package org.didelphis.genetics.alignment.constraints;


import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/3/2015
 */
public interface Constraint<T> {

	double evaluate(AlignmentAlgorithm<T> algorithm);

	String getName();

	double getStrength();
}

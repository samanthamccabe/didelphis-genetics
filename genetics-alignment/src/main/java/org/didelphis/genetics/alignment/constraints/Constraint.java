package org.didelphis.genetics.alignment.constraints;


import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 5/3/2015
 */
public interface Constraint {

	double evaluate(AlignmentAlgorithm algorithm);

	String getName();

	double getStrength();
}

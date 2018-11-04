package org.didelphis.genetics.alignment.constraints;

import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;

/**
 *
 * @param <T>
 */
public class PhoneticConstraint<T> implements Constraint<T> {
	@Override
	public double evaluate(AlignmentAlgorithm<T> algorithm) {
		return 0;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public double getStrength() {
		return 0;
	}
}

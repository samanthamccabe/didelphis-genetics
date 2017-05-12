package org.didelphis.genetics.alignment.operators.comparators;

import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.genetics.alignment.operators.Comparator;

/**
 * Samantha Fiona Morrigan McCabe Created: 6/29/2016
 */
public class MatrixSegmentComparator implements Comparator<Segment<Double>> {
	@Override
	public Double apply(Segment<Double> left, Segment<Double> right) {
		return 0.0;
	}
}

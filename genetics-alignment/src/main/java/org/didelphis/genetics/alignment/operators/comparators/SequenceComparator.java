package org.didelphis.genetics.alignment.operators.comparators;

import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.operators.Comparator;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 5/23/15
 */
public class SequenceComparator implements Comparator<Sequence<Double>> {

	private final Comparator<Segment<Double>> comparator;

	public SequenceComparator(Comparator<Segment<Double>> comparatorParam) {
		comparator = comparatorParam;
	}

	@Override
	public Double apply(Sequence<Double> left, Sequence<Double> right) {
		double score = left.stream()
				.mapToDouble(l -> right.stream()
						.mapToDouble(r -> comparator.apply(l, r)).sum()).sum();
		// This is intended for use on relatively short subsequences, l <= 3
		return score / (left.size() + right.size()) * left.size() *
				right.size();
	}
}

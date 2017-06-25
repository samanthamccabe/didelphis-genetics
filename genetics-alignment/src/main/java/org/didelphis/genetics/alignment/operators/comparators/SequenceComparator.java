package org.didelphis.genetics.alignment.operators.comparators;

import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.operators.Comparator;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/23/15
 */
public class SequenceComparator<T> implements Comparator<T, Double> {

	private final Comparator<T, Double> comparator;

	public SequenceComparator(Comparator<T, Double> comparatorParam) {
		comparator = comparatorParam;
	}

	@Override
	public Double apply(Sequence<T> left, Sequence<T> right, int i, int j) {
/*		double score = left.stream()
				.mapToDouble(l -> right.stream()
						.mapToDouble(r -> comparator.apply(l, r, i, j)).sum()).sum();
		// This is intended for use on relatively short subsequences, l <= 3
		return score / (left.size() + right.size()) * left.size() *
				right.size();*/
return Double.NaN;
	}
}

package org.didelphis.genetics.alignment.operators.comparators;

import org.didelphis.language.phonetic.sequences.Sequence;
import org.jetbrains.annotations.NotNull;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/23/15
 */
public class SequenceComparator<T> implements org.didelphis.genetics.alignment.operators.SequenceComparator<T> {

	private final org.didelphis.genetics.alignment.operators.SequenceComparator<T>
			comparator;

	public SequenceComparator(org.didelphis.genetics.alignment.operators.SequenceComparator<T> comparatorParam) {
		comparator = comparatorParam;
	}

	@Override
	public double apply(@NotNull Sequence<T> left, @NotNull Sequence<T> right, int i, int j) {
/*		double score = left.stream()
				.mapToDouble(l -> right.stream()
						.mapToDouble(r -> comparator.apply(l, r, i, j)).sum()).sum();
		// This is intended for use on relatively short subsequences, l <= 3
		return score / (left.size() + right.size()) * left.size() *
				right.size();*/
return Double.NaN;
	}
}

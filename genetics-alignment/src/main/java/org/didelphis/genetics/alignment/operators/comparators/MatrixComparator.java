package org.didelphis.genetics.alignment.operators.comparators;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.Table;

@ToString
@EqualsAndHashCode
public class MatrixComparator<T> implements SequenceComparator<T> {

	private final FeatureType<T> type;
	private final Table<Double> weights;

	public MatrixComparator(FeatureType<T> type, Table<Double> weights) {
		this.type = type;
		this.weights = weights;
	}

	@Override
	public double apply(
			@NonNull Sequence<T> left,
			@NonNull Sequence<T> right,
			int i,
			int j
	) {
		FeatureArray<T> f1 = left.get(i).getFeatures();
		FeatureArray<T> f2 = right.get(j).getFeatures();
		double sum = 0.0;
		int size = f1.size();
		for (int x = 0; x < size; x++) {
			T t1 = f1.get(x);
			for (int y = 0; y <= x; y++) {
				T t2 = f2.get(y);
				sum += weights.get(x, y) * type.difference(t1, t2);
			}
		}
		return sum;
	}
}

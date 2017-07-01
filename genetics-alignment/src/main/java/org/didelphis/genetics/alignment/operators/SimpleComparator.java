package org.didelphis.genetics.alignment.operators;

import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.sequences.Sequence;

import java.util.stream.IntStream;

/**
 * Class {@code SimpleComparator}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-06-30
 */
public class SimpleComparator<T> implements Comparator<T, Double> {
	private final FeatureType<T> featureType;

	public SimpleComparator(FeatureType<T> featureType) {
		this.featureType = featureType;
	}

	@Override
	public Double apply(Sequence<T> left, Sequence<T> right, int i, int j) {
			FeatureArray<T> z = left.get(i).getFeatures();
			FeatureArray<T> x = right.get(j).getFeatures();
			return IntStream.range(0, z.size())
					.mapToDouble(k -> featureType.difference(z.get(k),x.get(k)))
					.sum();
	}
}

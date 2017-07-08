package org.didelphis.genetics.alignment.operators;

import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Class {@code SimpleComparator}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-06-30
 */
public class SimpleComparator<T> implements Comparator<T> {
	private static final Function<Integer, Double> FUNCTION = i -> 1.0;
	private final FeatureType<T> featureType;
	private final Function<Integer, Double> function;

	public SimpleComparator(FeatureType<T> featureType, Function<Integer, Double> function) {
		this.featureType = featureType;
		this.function = function;
	}

	public SimpleComparator(FeatureType<T> featureType) {
		this(featureType, FUNCTION);
	}

	@Override
	public double apply(@NotNull Sequence<T> left, @NotNull Sequence<T> right, int i, int j) {
			FeatureArray<T> z = left.get(i).getFeatures();
			FeatureArray<T> x = right.get(j).getFeatures();
			return IntStream.range(0, z.size())
					.mapToDouble(k -> featureType.difference(z.get(k),x.get(k))*function.apply(k))
					.sum();
	}
}

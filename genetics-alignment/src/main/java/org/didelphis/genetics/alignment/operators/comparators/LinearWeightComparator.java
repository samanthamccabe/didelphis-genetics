package org.didelphis.genetics.alignment.operators.comparators;

import org.didelphis.common.language.phonetic.features.FeatureArray;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.genetics.alignment.operators.Comparator;

import java.util.List;
import java.util.Objects;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 5/22/15
 */
public class LinearWeightComparator implements Comparator<Segment<Double>> {

	private static final double DEFAULT_NAN_DIFFERENCE = 2.5;

	private final double nanDifference;
	private final List<Double> weights;

	public LinearWeightComparator(List<Double> list) {
		this(list, DEFAULT_NAN_DIFFERENCE);
	}

	public LinearWeightComparator(List<Double> list, double nanParam) {
		weights = list;
		nanDifference = nanParam;
	}

	@Override
	public Double apply(Segment<Double> left, Segment<Double> right) {
		double score = 0.0;
		FeatureArray<Double> lFeatures = left.getFeatures();
		FeatureArray<Double> rFeatures = right.getFeatures();
		for (int i = 0; i < weights.size(); i++) {
			Double l = lFeatures.get(i);
			Double r = rFeatures.get(i);
			Double d = getDifference(l, r);
			Double w = weights.get(i);

			score += w * d;
		}
		return score;
	}

	@Override
	public String toString() {
		return "LinearWeightComparator{" + "nanDifference=" + nanDifference +
				", weights=" + weights + '}';
	}

	private Double getDifference(Double a, Double b) {
		if (Objects.equals(a,b)) {
			return 0.0;
		} else if (a.isNaN()) {
			return Math.abs(b) + nanDifference;
		} else if (b.isNaN()) {
			return Math.abs(a) + nanDifference;
		} else {
			return Math.abs(a - b);
		}
	}
}

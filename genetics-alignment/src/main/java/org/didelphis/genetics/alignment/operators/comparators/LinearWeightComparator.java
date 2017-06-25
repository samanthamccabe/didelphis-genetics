package org.didelphis.genetics.alignment.operators.comparators;

import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.operators.Comparator;

import java.util.List;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/22/15
 */
public class LinearWeightComparator<T> implements Comparator<T, Double> {

	private final FeatureType<T> type;
	private final List<Double> weights;

	public LinearWeightComparator(FeatureType<T> type, List<Double> list) {
		this.type = type;
		weights = list;
	}

	@Override
	public Double apply(Sequence<T> left, Sequence<T> right, int l, int r) {
		double score = 0.0;
		FeatureArray<T> lFeatures = left.get(l).getFeatures();
		FeatureArray<T> rFeatures = right.get(r).getFeatures();
		for (int i = 0; i < weights.size(); i++) {
			T lF = lFeatures.get(i);
			T rF = rFeatures.get(i);
			Double d = type.difference(lF, rF);
			Double w = weights.get(i);
			score += w * d;
		}
		return score;
	}

	@Override
	public String toString() {
		return "LinearWeightComparator{weights=" + weights + '}';
	}
}

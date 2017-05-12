package org.didelphis.genetics.alignment.operators.comparators;

import org.didelphis.common.language.phonetic.features.FeatureArray;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.common.structures.tables.Table;
import org.didelphis.genetics.alignment.operators.Comparator;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 5/23/15
 */
public class MutliplexSequenceComparator
		implements Comparator<Sequence<Double>> {

	private final Table<Double> weights;
	private final Comparator<Segment<Double>> comparator;

	public MutliplexSequenceComparator(Table<Double> weightsParam,
			Comparator<Segment<Double>> comparatorParam) {
		comparator = comparatorParam;
		weights = weightsParam;
	}

	@Override
	public Double apply(Sequence<Double> left, Sequence<Double> right) {
		double score = 0.0;
		int index = 0;
		for (Segment<Double> l : left) {
			for (Segment<Double> r : right) {

				FeatureArray<Double> lFeatures = l.getFeatures();
				FeatureArray<Double> rFeatures = r.getFeatures();

				double modifier = 0.0;
				for (int i = 0; i < lFeatures.size(); i++) {
					Double a = lFeatures.get(i);
					Double b = rFeatures.get(i);
					double difference = getDifference(a, b);
					modifier += difference * weights.get(i, index);
				}
				index++;

				double value = comparator.apply(l, r);

				score += value + modifier;
			}
		}
		return score;
	}

	private static Double getDifference(Double a, Double b) {
		if (a.equals(b)) {
			return 0.0;
		} else if (a.isNaN()) {
			return Math.abs(b);
		} else if (b.isNaN()) {
			return Math.abs(a);
		} else {
			return Math.abs(a - b);
		}
	}
}

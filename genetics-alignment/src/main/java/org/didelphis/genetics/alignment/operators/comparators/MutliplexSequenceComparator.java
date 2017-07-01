package org.didelphis.genetics.alignment.operators.comparators;

import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.Table;
import org.didelphis.genetics.alignment.operators.Comparator;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/23/15
 */
public class MutliplexSequenceComparator<T>
		implements Comparator<T, Double> {

	private final Table<Double> weights;
	private final Comparator<T, Double> comparator;

	public MutliplexSequenceComparator(Table<Double> weightsParam,
			Comparator<T, Double> comparatorParam) {
		comparator = comparatorParam;
		weights = weightsParam;
	}

	@Override
	public Double apply(Sequence<T> left, Sequence<T> right, int i, int j) {
		double score = 0.0;
		int index = 0;
//		for (Segment<Integer> lF : left) {
//			for (Segment<Integer> rF : right) {
//
//				FeatureArray<Integer> lFeatures = lF.getFeatures();
//				FeatureArray<Integer> rFeatures = rF.getFeatures();
//
//				double modifier = 0.0;
//				for (int i = 0; i < lFeatures.size(); i++) {
//					Double a = lFeatures.get(i);
//					Double b = rFeatures.get(i);
//					double difference = getDifference(a, b);
//					modifier += difference * weights.get(i, index);
//				}
//				index++;
//
//				double value = comparator.apply(lF, rF);
//
//				score += value + modifier;
//			}
//		}
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

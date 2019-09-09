/******************************************************************************
 * General components for language modeling and analysis                      *
 *                                                                            *
 * Copyright (C) 2014-2019 Samantha F McCabe                                  *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.     *
 *                                                                            *
 ******************************************************************************/

package org.didelphis.genetics.alignment.operators.comparators;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.interfaces.TwoKeyMap;
import org.didelphis.structures.tuples.Triple;

import java.util.List;
import java.util.Map;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/22/15
 */
@ToString
@EqualsAndHashCode
public class SparseMatrixComparator<T> implements SequenceComparator<T> {

	private final SequenceComparator<T> comparator;
	private final TwoKeyMap<Integer, Integer, Double> sparseWeights;
	private final FeatureType<? super T> type;

	public SparseMatrixComparator(
			FeatureType<? super T> type,
			List<Double> weights,
			TwoKeyMap<Integer, Integer, Double> sparseWeights
	) {
		comparator = new LinearWeightComparator<>(type, weights);
		this.type = type;
		this.sparseWeights = sparseWeights;
	}

	@Override
	public double apply(@NonNull Sequence<T> left, @NonNull Sequence<T> right, int i, int j) {
		double score = comparator.apply(left, right, i, j);

		FeatureArray<T> lFeatures = left.get(i).getFeatures();
		FeatureArray<T> rFeatures = right.get(j).getFeatures();

//		for (Map.Entry<Integer, Map<Integer, Double>> e : sparseWeights.entrySet()) {
//			Integer i1 = e.getKey();
//			Map<Integer, Double> map = e.getValue();
//			for (Map.Entry<Integer, Double> entry : map.entrySet()) {
//				Integer i2 = entry.getKey();
//				T lFeature1 = lFeatures.get(i1);
//				T lFeature2 = lFeatures.get(i2);
//				T rFeature1 = rFeatures.get(i1);
//				T rFeature2 = rFeatures.get(i2);
//
//				double d1 = type.difference(lFeature1, rFeature2);
//				double d2 = type.difference(lFeature2, rFeature1);
//
//				score += (d1 + d2) * entry.getValue();
//			}
//		}

		for (Triple<Integer, Integer, Double> triple : sparseWeights) {
			T lFeature1 = lFeatures.get(triple.getFirstElement());
			T lFeature2 = lFeatures.get(triple.getSecondElement());
			T rFeature1 = rFeatures.get(triple.getFirstElement());
			T rFeature2 = rFeatures.get(triple.getSecondElement());

			double d1 = type.difference(lFeature1, rFeature2);
			double d2 = type.difference(lFeature2, rFeature1);

			score += (d1 + d2) * triple.getThirdElement();
		}
		return score;
	}
}

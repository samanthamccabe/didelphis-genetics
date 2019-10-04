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
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.GeneralTwoKeyMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMap;
import org.didelphis.structures.tuples.Triple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ToString
@EqualsAndHashCode
public final class SparseMatrixComparator<T> implements SequenceComparator<T> {

	private final List<Double> weights;
	private final TwoKeyMap<Integer, Integer, Double> sparseWeights;
	private final FeatureType<? super T> type;

	private final TwoKeyMap<String, String, Double> cache;

	public SparseMatrixComparator(
			FeatureType<? super T> type,
			List<Double> weights,
			TwoKeyMap<Integer, Integer, Double> sparseWeights
	) {
		this.weights = weights;
		this.type = type;
		this.sparseWeights = sparseWeights;

		cache=new GeneralTwoKeyMap<>();
	}

	@Override
	public double apply(@NonNull Sequence<T> left, @NonNull Sequence<T> right, int i, int j) {

		Segment<T> lSegment = left.get(i);
		Segment<T> rSegment = right.get(j);

		String lSymbol = lSegment.getSymbol();
		String rSymbol = rSegment.getSymbol();

		FeatureArray<T> lFeatures = lSegment.getFeatures();
		FeatureArray<T> rFeatures = rSegment.getFeatures();

		if (cache.contains(lSymbol, rSymbol)) {
			return cache.get(lSymbol,rSymbol);
		}

		double score = 0.0;
		for (Map.Entry<Integer, Map<Integer, Double>> e : sparseWeights.entrySet()) {
			Integer i1 = e.getKey();
			Map<Integer, Double> map = e.getValue();
			for (Map.Entry<Integer, Double> entry : map.entrySet()) {
				Integer i2 = entry.getKey();
				T lFeature1 = lFeatures.get(i1);
				T lFeature2 = lFeatures.get(i2);
				T rFeature1 = rFeatures.get(i1);
				T rFeature2 = rFeatures.get(i2);

				double d1 = type.difference(lFeature1, rFeature2);
				double d2 = type.difference(lFeature2, rFeature1);

				score += (d1 + d2) * entry.getValue();
			}
		}

		for (int k = 0; k < weights.size(); k++) {
			T lF = lFeatures.get(k);
			T rF = rFeatures.get(k);
			Double d = type.difference(lF, rF);
			Double w = weights.get(k);
			score += w * d;
		}

		if (score < 0.0) {
			score = 0.0;
		}

		cache.put(lSymbol,rSymbol, score);
		return score;
	}
}

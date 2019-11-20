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

import javax.swing.table.TableRowSorter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ToString
@EqualsAndHashCode
public final class SparseMatrixComparator<T> implements SequenceComparator<T> {

	private final SequenceComparator<T> sequenceComparator;
	private final TwoKeyMap<Integer, Integer, Double> sparseWeights;
	private final FeatureType<? super T> type;

	private final TwoKeyMap<String, String, Double> cache;

	public SparseMatrixComparator(
			FeatureType<? super T> type,
			SequenceComparator<T> sequenceComparator,
			TwoKeyMap<Integer, Integer, Double> sparseWeights
	) {
		this.type = type;
		this.sequenceComparator = sequenceComparator;
		this.sparseWeights = sparseWeights;
		cache=new GeneralTwoKeyMap<>();
	}

	@Override
	public double apply(@NonNull Sequence<T> left, @NonNull Sequence<T> right, int i, int j) {

		Segment<T> lSegment = left.get(i);
		Segment<T> rSegment = right.get(j);

		if (lSegment.equals(rSegment)) return 0.0;

		String lSymbol = lSegment.getSymbol();
		String rSymbol = rSegment.getSymbol();

		if (cache.contains(lSymbol, rSymbol)) {
			return cache.get(lSymbol,rSymbol);
		}

		double score = sequenceComparator.apply(left, right, i, j);

		FeatureArray<T> lFeatures = lSegment.getFeatures();
		FeatureArray<T> rFeatures = rSegment.getFeatures();

		for (Triple<Integer, Integer, Double> triple : sparseWeights) {
			int i1 = triple.first();
			int i2 = triple.second();

			T lFeature1 = lFeatures.get(i1);
			T lFeature2 = lFeatures.get(i2);
			T rFeature1 = rFeatures.get(i1);
			T rFeature2 = rFeatures.get(i2);

			double d1 = type.difference(lFeature1, rFeature2);
			double d2 = type.difference(lFeature2, rFeature1);

			score += (d1 + d2) * triple.third();
		}

		if (score < 0.0) {
			score = 0.0;
		}

		cache.put(lSymbol,rSymbol, score);
		return score;
	}
}

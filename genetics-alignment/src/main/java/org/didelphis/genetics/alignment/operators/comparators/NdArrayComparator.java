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
import org.didelphis.language.phonetic.features.EmptyFeatureArray;
import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.List;
import java.util.stream.IntStream;

@ToString
@EqualsAndHashCode
public final class NdArrayComparator<T> implements SequenceComparator<T> {

	private final INDArray weights;
	private final INDArray emptyArray;
	private final FeatureType<? super T> type;

	public NdArrayComparator(FeatureType<? super T> type, List<Double> list) {
		this.type = type;
		double[] array = list.stream()
				.mapToDouble(Double::doubleValue)
				.toArray();

		double[] baseArray = IntStream.range(0, array.length)
				.mapToDouble(i -> 0)
				.toArray();

		weights    = Nd4j.create(array, array.length);
		emptyArray = Nd4j.create(baseArray, array.length);
	}

	public NdArrayComparator(FeatureType<? super T> type, double[] array) {
		this.type = type;

		double[] baseArray = IntStream.range(0, array.length)
				.mapToDouble(i -> type.doubleValue(null))
				.toArray();

		weights = Nd4j.create(array, array.length);
		emptyArray = Nd4j.create(baseArray, array.length);
	}

	@Override
	public double apply(
			@NonNull Sequence<T> left,
			@NonNull Sequence<T> right,
			int i,
			int j
	) {

		INDArray lF = getNdFeatureArray(left.get(i));
		INDArray rF = getNdFeatureArray(right.get(j));

		// TODO: this doesn't compute absolute value
		INDArray dif = lF.sub(rF);

		// in-place element-wise multiplication
		dif.muli(weights);
		double v = dif.sumNumber().doubleValue();
		return v;
	}

	private INDArray getNdFeatureArray(Segment<T> segment) {
		FeatureArray<T> featureArray = segment.getFeatures();

		if (featureArray instanceof EmptyFeatureArray) {
			return emptyArray;
		}

		double[] array = featureArray.stream()
				.mapToDouble(type::doubleValue)
				.toArray();
		return Nd4j.create(array, featureArray.size());
	}
}

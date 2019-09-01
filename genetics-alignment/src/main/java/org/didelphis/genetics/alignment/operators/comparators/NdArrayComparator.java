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

import lombok.NonNull;
import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by samantha on 8/4/16.
 */
public final class NdArrayComparator<T> implements SequenceComparator<T> {

	private static final transient Logger LOGGER =
			getLogger(NdArrayComparator.class);

	private final INDArray weights;

	public NdArrayComparator(double[] array) {
		weights = Nd4j.create(array);
	}

	@Override
	public double apply(@NonNull Sequence<T> left, @NonNull Sequence<T> right, int i, int j) {

		INDArray lF = getNdFeatureArray(left.get(i));
		INDArray rF = getNdFeatureArray(right.get(j));

		INDArray dif = lF.sub(rF);
		// in-place element-wise multiplication
		dif.muli(weights);
		return dif.sumNumber().doubleValue();
	}

	private INDArray getNdFeatureArray(Segment<T> segment) {
		FeatureArray<T> featureArray = segment.getFeatures();
		INDArray ndArray;
		//		if (featureArray instanceof NdFeatureArray) {
		//			ndArray = ((NdFeatureArray) featureArray).getNdArray();
		//		} else {
		//			ndArray = new NdFeatureArray(featureArray).getNdArray();
		//		}
		//		return ndArray;
		return null; // TODO:
	}
}

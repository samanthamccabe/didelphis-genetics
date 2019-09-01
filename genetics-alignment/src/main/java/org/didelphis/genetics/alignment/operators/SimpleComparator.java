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

package org.didelphis.genetics.alignment.operators;

import lombok.NonNull;
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
public class SimpleComparator<T> implements SequenceComparator<T> {
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
	public double apply(@NonNull Sequence<T> left, @NonNull Sequence<T> right, int i, int j) {
			FeatureArray<T> z = left.get(i).getFeatures();
			FeatureArray<T> x = right.get(j).getFeatures();
			return IntStream.range(0, z.size())
					.mapToDouble(k -> featureType.difference(z.get(k),x.get(k))*function.apply(k))
					.sum();
	}
}

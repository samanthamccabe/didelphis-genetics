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
import org.didelphis.structures.tables.Table;

@ToString
@EqualsAndHashCode
public class MatrixComparator<T> implements SequenceComparator<T> {

	private final FeatureType<? super T> type;
	private final Table<Double> weights;

	public MatrixComparator(FeatureType<? super T> type, Table<Double> weights) {
		this.type = type;
		this.weights = weights;
	}

	@Override
	public double apply(
			@NonNull Sequence<T> left,
			@NonNull Sequence<T> right,
			int i,
			int j
	) {
		FeatureArray<T> f1 = left.get(i).getFeatures();
		FeatureArray<T> f2 = right.get(j).getFeatures();

		double sum = 0.0;
		int size = f1.size();
		for (int row = 0; row < size; row++) {
			T t1 = f1.get(row);
			for (int col = 0; col <= row; col++) {
				T t2 = f2.get(col);
				sum += weights.get(row, col) * type.difference(t1, t2);
			}
		}

		return sum;
	}
}

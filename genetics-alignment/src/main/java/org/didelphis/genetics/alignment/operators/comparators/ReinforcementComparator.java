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

import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.interfaces.TwoKeyMap;

public class ReinforcementComparator<T> implements SequenceComparator<T> {

	private final SequenceComparator<T> comparator;
	private final @NonNull TwoKeyMap<? super Segment<T>, ? super Segment<T>, Double> corrMap;
	private final double weight;

	public ReinforcementComparator(
			@NonNull SequenceComparator<T> comparator,
			@NonNull TwoKeyMap<? super Segment<T>, ? super Segment<T>, Double> corrMap,
			double weight
	) {
		this.comparator = comparator;
		this.corrMap = corrMap;
		this.weight = weight;
	}

	@Override
	public double apply(
			@NonNull Sequence<T> left, @NonNull Sequence<T> right, int i, int j
	) {
		Segment<T> s1 = left.get(i);
		Segment<T> s2 = right.get(j);

		double factor;
		if (corrMap.contains(s1, s2)) {
			Double value = corrMap.get(s1, s2);
			factor = value == null ? 1.0 : -Math.log(value);
		} else {
			factor = 1.0;
		}

		double apply = comparator.apply(left, right, i, j);
		return apply + weight * factor;
	}
}

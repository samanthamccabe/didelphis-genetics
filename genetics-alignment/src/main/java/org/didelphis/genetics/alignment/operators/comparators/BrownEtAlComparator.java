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
import org.didelphis.structures.contracts.Streamable;
import org.didelphis.structures.maps.SymmetricalTwoKeyMap;
import org.didelphis.structures.tuples.Triple;

/**
 * Class {@code MatrixComparator}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-07-04
 */
public class BrownEtAlComparator<T> implements SequenceComparator<T> {

	private final SymmetricalTwoKeyMap<Segment<T>, Double> map;
	private final double max;

	public BrownEtAlComparator(
			Streamable<Triple<Segment<T>, Segment<T>, Double>> streamable
	) {
		map = new SymmetricalTwoKeyMap<>();
		max = streamable.stream()
				.map(Triple::getThirdElement)
				.max(Double::compare)
				.orElse(100.0);
		streamable.stream().forEach(t -> map.put(
				t.getFirstElement(),
				t.getSecondElement(),
				(max - t.getThirdElement()) / 10.0));
	}

	@Override
	public double apply(@NonNull Sequence<T> left, @NonNull Sequence<T> right,
			int i, int j) {
		Segment<T> sL = left.get(i);
		Segment<T> sR = right.get(j);
		Double value = sL.equals(sR)
				? 0.0
				: (map.contains(sL, sR) ? map.get(sL, sR) : max);
		return value;
	}
}

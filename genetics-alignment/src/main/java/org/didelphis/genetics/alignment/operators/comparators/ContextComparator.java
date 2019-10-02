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
import org.didelphis.language.phonetic.sequences.Sequence;

public final class ContextComparator<T> implements SequenceComparator<T> {

	private final SequenceComparator<T> comparator;
	private final double p1;
	private final double p2;
	private final double p3;
	private final double p4;

	public ContextComparator(SequenceComparator<T> comparator, double p1, double p2, double p3, double p4) {

		this.comparator = comparator;
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		this.p4 = p4;
	}

	@Override
	public double apply(
			@NonNull Sequence<T> left, @NonNull Sequence<T> right, int i, int j
	) {
		double score = comparator.apply(left, right, i, j);

		if (i - 1 >= 0) {
			score += comparator.apply(left, right, i - 1, j) * p1;
		}

		if (i + 1 < left.size()) {
			score += comparator.apply(left, right, i + 1, j) * p2;
		}

		if (j - 1 >= 0) {
			score += comparator.apply(left, right, i, j - 1) * p3;
		}

		if (j + 1 < right.size()) {
			score += comparator.apply(left, right, i, j) + 1 * p4;
		}

		return score;
	}
}

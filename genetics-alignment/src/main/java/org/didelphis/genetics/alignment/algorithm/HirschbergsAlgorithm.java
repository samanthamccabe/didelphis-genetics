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

package org.didelphis.genetics.alignment.algorithm;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.optimization.Optimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.sequences.Sequence;

import java.util.List;

@ToString
@EqualsAndHashCode
public class HirschbergsAlgorithm<T> implements AlignmentAlgorithm<T> {

	private final SequenceComparator<T> comparator;
	private final Optimization optimization;
	private final GapPenalty<T> gapPenalty;
	private final SequenceFactory<T> factory;

	public HirschbergsAlgorithm(
			SequenceComparator<T> comparator,
			Optimization optimization,
			GapPenalty<T> gapPenalty,
			SequenceFactory<T> factory
	) {
		this.comparator = comparator;
		this.optimization = optimization;
		this.gapPenalty = gapPenalty;
		this.factory = factory;
	}

	@NonNull
	@Override
	public GapPenalty<T> getGapPenalty() {
		return gapPenalty;
	}

	@NonNull
	@Override
	public SequenceFactory<T> getFactory() {
		return factory;
	}

	@NonNull
	@Override
	public SequenceComparator<T> getComparator() {
		return comparator;
	}

	@NonNull
	@Override
	public Optimization getOptimization() {
		return optimization;
	}

	@NonNull
	@Override
	public AlignmentResult<T> apply(@NonNull Sequence<T>left, @NonNull Sequence<T> right) {
		// TODO:

		return null;
	}
}

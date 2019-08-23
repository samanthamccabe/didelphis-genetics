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
import lombok.ToString;
import org.didelphis.genetics.alignment.algorithm.optimization.Optimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Class AbstractAlignmentAlgorithm
 *
 * @since 06/05/2017
 */
@ToString
@EqualsAndHashCode
public abstract class AbstractAlignmentAlgorithm<N>
		implements AlignmentAlgorithm<N> {

	private final SequenceComparator<N> comparator;
	private final Optimization<Double> optimization;
	private final GapPenalty<N> gapPenalty;
	private final SequenceFactory<N> factory;

	protected AbstractAlignmentAlgorithm(
			Optimization<Double> optimization,
			SequenceComparator<N> comparator,
			GapPenalty<N> gapPenalty,
			SequenceFactory<N> factory
	) {
		this.comparator = comparator;
		this.optimization = optimization;
		this.gapPenalty = gapPenalty;
		this.factory = factory;
	}

	@Override
	public @NotNull GapPenalty<N> getGapPenalty() {
		return gapPenalty;
	}

	@Override
	public @NotNull SequenceFactory<N> getFactory() {
		return factory;
	}

	@Override
	public @NotNull SequenceComparator<N> getComparator() {
		return comparator;
	}

	@Override
	public @NotNull Optimization<Double> getOptimization() {
		return optimization;
	}
}

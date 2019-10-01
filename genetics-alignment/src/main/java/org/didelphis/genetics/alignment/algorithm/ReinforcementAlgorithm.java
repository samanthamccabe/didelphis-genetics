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

import lombok.NonNull;

import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.optimization.Optimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.comparators.ReinforcementComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.sequences.Sequence;

public class ReinforcementAlgorithm<T> implements AlignmentAlgorithm<T>  {

	private final AlignmentAlgorithm<T> algorithm;

	public ReinforcementAlgorithm(AlignmentAlgorithm<T> algorithm) {
		this.algorithm = algorithm;
	}

	@Override
	public @NonNull GapPenalty<T> getGapPenalty() {
		return algorithm.getGapPenalty();
	}

	@Override
	public @NonNull SequenceFactory<T> getFactory() {
		return algorithm.getFactory();
	}

	@Override
	public @NonNull SequenceComparator<T> getComparator() {
		return algorithm.getComparator();
	}

	@Override
	public @NonNull Optimization getOptimization() {
		return algorithm.getOptimization();
	}

	@Override
	public AlignmentResult<T> apply(
			Sequence<T> left, Sequence<T> right
	) {
		return algorithm.apply(left, right);
	}
}

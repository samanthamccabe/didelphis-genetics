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

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import org.didelphis.genetics.alignment.algorithm.optimization.BaseOptimization;
import org.didelphis.genetics.alignment.algorithm.optimization.Optimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.SimpleComparator;
import org.didelphis.genetics.alignment.operators.comparators.SparseMatrixComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.genetics.alignment.operators.gap.NullGapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;

@ToString
@FieldDefaults (level = AccessLevel.PRIVATE)
@Setter (onParam = @__ ({@NonNull}))
public class AlgorithmBuilder<T> {

	final SequenceFactory<T> factory;

	AlignmentMode alignmentMode;
	SequenceComparator<T> comparator;
	Optimization optimization;
	GapPenalty<T> gapPenalty;
	Segment<T> anchor;

	public AlgorithmBuilder(SequenceFactory<T> factory) {
		this.factory = factory;

		Sequence<T> gap = factory.toSequence("_");

		FeatureType<T> featureType = factory.getFeatureMapping()
				.getFeatureModel()
				.getFeatureType();

		alignmentMode = AlignmentMode.GLOBAL;
		comparator = new SimpleComparator<>(featureType);
		optimization = BaseOptimization.MIN;
		gapPenalty = new NullGapPenalty<>(gap);
		anchor = factory.toSegment("#");
	}


	public AlignmentAlgorithm<T> build() {
		return new NeedlemanWunschAlgorithm<>(
				optimization,
				alignmentMode,
				comparator,
				gapPenalty,
				factory
		);
	}


}

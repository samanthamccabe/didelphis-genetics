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

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.optimization.BaseOptimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.genetics.alignment.operators.gap.NullGapPenalty;
import org.didelphis.io.ClassPathFileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.BinaryFeature;
import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntToDoubleFunction;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Class {@code NeedlemanWunschAlgorithmTest}
 *
 * @since 0.1.0
 */
class NeedlemanWunschAlgorithmTest {

	private static final FormatterMode MODE = FormatterMode.INTELLIGENT;

	private static SequenceFactory<Integer> factory;
	private static GapPenalty<Integer> penalty;
	private static AlignmentAlgorithm<Integer> algorithm;

	@BeforeAll
	static void init() {
		String path = "AT_hybrid_reduced.model";
		FeatureType<Integer> type = IntegerFeature.INSTANCE;
		ClassPathFileHandler handler = ClassPathFileHandler.INSTANCE;
		FeatureModelLoader<Integer> loader =
				new FeatureModelLoader<>(type, handler, path);

		factory = new SequenceFactory<>(loader.getFeatureMapping(), MODE);

		Sequence<Integer> gap = factory.toSequence("░");
		penalty = new NullGapPenalty<>(gap);
		SequenceComparator<Integer> comparator = (t, r, i, j) -> {
			FeatureArray<Integer> z = t.get(i).getFeatures();
			FeatureArray<Integer> x = r.get(j).getFeatures();
			IntToDoubleFunction func = k -> type.difference(z.get(k), x.get(k));
			return IntStream.range(0, z.size()).mapToDouble(func).sum();
		};
		algorithm = new NeedlemanWunschAlgorithm<>(
				BaseOptimization.MIN,
				AlignmentMode.GLOBAL,
				comparator,
				penalty,
				factory
		);
	}

	@Test
	void getAlignment_01() {
		AlignmentResult<Integer> result = algorithm.apply(factory.toSequence("#amapar"),
				factory.toSequence("#omber"));
		Alignment<Integer> alignment = result.getAlignments().get(0);
//		String message = '\n' + alignment.getPrettyTable();
//		assertEquals(7, alignment.columns(), message);
	}

	@Test
	void getAlignment_02() {
		AlignmentResult<Integer> alignmentResult = algorithm.apply(factory.toSequence("#amapar"),
				factory.toSequence("#kombera"));
		Alignment<Integer> alignment = alignmentResult.getAlignments().get(0);
//		assertEquals("# ░ a m a p a r ░ \n# k o m ░ b e r a \n",
//				alignmentResult.getAlignments().get(0).getPrettyTable());
	}

	@Test
	void getAlignment_03() {
		AlignmentResult<Integer> alignmentResult = algorithm.apply(factory.toSequence("#ammapar"),
				factory.toSequence("#kamabra"));
		Alignment<Integer> alignment = alignmentResult.getAlignments().get(0);
//		String message = '\n' + alignment.getPrettyTable();
//		assertEquals(10, alignment.columns(), message);
	}

	@Test
	void getAlignment_04() {
		FeatureModelLoader<Boolean> loader = BinaryFeature.INSTANCE.emptyLoader();
		SequenceFactory<Boolean> factory = new SequenceFactory<>(
				loader.getFeatureMapping(), FormatterMode.NONE);
		SequenceComparator<Boolean> comparator = (left, right, i, j) ->
				Objects.equals(left.get(i),right.get(j)) ? 0 : 1;

		AlignmentAlgorithm<Boolean> algorithm = new NeedlemanWunschAlgorithm<>(BaseOptimization.MIN,
				AlignmentMode.GLOBAL,
				comparator,
				new NullGapPenalty<>(factory.toSequence("_")),
				factory
		);

		AlignmentResult<Boolean> result = algorithm.apply(
						factory.toSequence("#baba"),
						factory.toSequence("#ababb"));

		assertFalse(result.getAlignments().isEmpty());
		assertEquals(2.0, result.getScore());
		String expected = "# _ b a b a \t# a b a b b \t";
		assertEquals(expected, result.getAlignments().get(0).toString());
	}
}

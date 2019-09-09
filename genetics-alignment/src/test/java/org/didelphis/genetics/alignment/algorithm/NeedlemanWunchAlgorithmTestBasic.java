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
import org.didelphis.genetics.alignment.operators.gap.ConstantGapPenalty;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.BinaryFeature;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Class {@code NeedlemanWunchAlgorithmTestBasic}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-07-13
 */
public class NeedlemanWunchAlgorithmTestBasic {

	private static SequenceFactory<Boolean> factory;
	private static AlignmentAlgorithm<Boolean> simpleAlgorithm;

	@BeforeAll
	static void init() {
		FeatureModelLoader<Boolean> loader = BinaryFeature.INSTANCE.emptyLoader();
		FeatureMapping<Boolean> mapping = loader.getFeatureMapping();
		factory = new SequenceFactory<>(mapping, FormatterMode.NONE);

		SequenceComparator<Boolean> simpleComparator = (left, right, i, j)
				-> Objects.equals(left.get(i), right.get(j)) ? -1 : 1;

		Sequence<Boolean> gap = factory.toSequence("_");
		GapPenalty<Boolean> penalty = new ConstantGapPenalty<>(gap, 0);
		simpleAlgorithm = new NeedlemanWunschAlgorithm<>(BaseOptimization.MIN,
				simpleComparator,
				penalty,
				factory
		);
	}

	@Test
	void classicNW() {
		SequenceComparator<Boolean> nwComparator = (left, right, i, j) ->
				Objects.equals(left.get(i), right.get(j)) ? 1 : -1;

		Sequence<Boolean> gap = factory.toSequence("-");
		GapPenalty<Boolean> penalty = new ConstantGapPenalty<>(gap, 0);

		AlignmentAlgorithm<Boolean> alg = new NeedlemanWunschAlgorithm<>(
				BaseOptimization.MAX,
				nwComparator,
				penalty,
				factory
		);

		AlignmentResult<Boolean> result = align(alg, "+GATTACA", "+GCATGCU");

		List<Alignment<Boolean>> received = result.getAlignments();
		List<Alignment<Boolean>> expected = result.getAlignments();

		expected.add(toAlignment("GCATG-CU", "G-ATTACA"));
		expected.add(toAlignment("GCA-TGCU", "G-ATTACA"));
		expected.add(toAlignment("GCAT-GCU", "G-ATTACA"));

		assertTrue(received.removeAll(expected));
	}

	@Test
	void classicNW_Big() {
		SequenceComparator<Boolean> nwComparator = (left, right, i, j) ->
				Objects.equals(left.get(i), right.get(j)) ? 1 : -1;

		Sequence<Boolean> gap = factory.toSequence("-");
		GapPenalty<Boolean> penalty = new ConstantGapPenalty<>(gap, 0);

		AlignmentAlgorithm<Boolean> alg = new NeedlemanWunschAlgorithm<>(
				BaseOptimization.MAX,
				nwComparator,
				penalty,
				factory
		);

		String left  = "+GATTACACATTGUUCTAAGATTACAUCCCCATGTTTTTTAC";
		String right = "+GCATGCUGGTUCATCCCATGC";

		AlignmentResult<Boolean> result = align(alg, left, right);

		List<Alignment<Boolean>> received = result.getAlignments();
		List<Alignment<Boolean>> expected = result.getAlignments();

//		expected.add(toAlignment("GCATG-CU", "G-ATTACA"));
//		expected.add(toAlignment("GCA-TGCU", "G-ATTACA"));
//		expected.add(toAlignment("GCAT-GCU", "G-ATTACA"));

//		assertTrue(received.removeAll(expected));
	}

	@Test
	void getAlignment_01() {
		AlignmentResult<Boolean> result = simpleAlgorithm.apply(
				factory.toSequence("#aba"),
				factory.toSequence("#baba")
		);
		Assertions.assertFalse(result.getAlignments().isEmpty());
		//		assertEquals(2.0, result.getScore());
		assertEquals("# _ a b a \n# b a b a \n",
				result.getAlignments().get(0).getPrettyTable()
		);
	}

	@Disabled
	@Test
	void getAlignment_02() {
		AlignmentResult<Boolean> result = simpleAlgorithm.apply(factory.toSequence("#abab"), factory.toSequence("#baba"));
		Assertions.assertFalse(result.getAlignments().isEmpty());
		assertEquals("# _ a b a b \n# b a b a _ \n",
				result.getAlignments().get(0).getPrettyTable()
		);
	}

	@DisplayName ("Very simple alignment: #b ~ #ab")
	@Test
	void getAlignmentSimple() {
		AlignmentResult<Boolean> result = simpleAlgorithm.apply(factory.toSequence(
				"#b"), factory.toSequence("#ab"));
		Assertions.assertFalse(result.getAlignments().isEmpty());
		//		assertEquals(4.0, result.getScore());
		assertEquals("# _ b \n# a b \n",
				result.getAlignments().get(0).getPrettyTable()
		);
	}

	@DisplayName ("Very simple alignment: #ab ~ #b")
	@Test
	void getAlignmentSimpleReerse() {
		AlignmentResult<Boolean> result = simpleAlgorithm.apply(factory.toSequence(
				"#ab"), factory.toSequence("#b"));
		Assertions.assertFalse(result.getAlignments().isEmpty());
		//		assertEquals(4.0, result.getScore());
		assertEquals("# a b \n# _ b \n",
				result.getAlignments().get(0).getPrettyTable()
		);
	}

	@Test
	void getAlignment_04() {
		AlignmentResult<Boolean> result = simpleAlgorithm.apply(factory.toSequence(
				"#baba"), factory.toSequence("#ababb"));

		Assertions.assertFalse(result.getAlignments().isEmpty());
		String expected = "# _ b a b a \t# a b a b b \t";
		assertEquals(expected, result.getAlignments().get(0).toString());
	}

	@Test
	void getAlignment_Disjoint() {
		AlignmentResult<Boolean> alignmentResult = simpleAlgorithm.apply(factory
				.toSequence("#abc---"), factory.toSequence("#---abc"));
		Alignment<Boolean> alignment = alignmentResult.getAlignments().get(0);
		String message = '\n' + alignment.getPrettyTable();
		//		assertEquals(10, alignment.columns(), message);
	}


	private static Alignment<Boolean> toAlignment(String left, String right) {
		return new Alignment<>(
				factory.toSequence(left),
				factory.toSequence(right)
		);
	}

	private static AlignmentResult<Boolean> align(
			AlignmentAlgorithm<Boolean> algorithm,
			String left,
			String right
	) {
		return algorithm.apply(
				factory.toSequence(left),
				factory.toSequence(right)
		);
	}
}

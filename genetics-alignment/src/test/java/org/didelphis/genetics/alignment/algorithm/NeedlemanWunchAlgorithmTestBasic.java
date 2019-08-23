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

import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.optimization.BaseOptimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.NullGapPenalty;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.BinaryFeature;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Class {@code NeedlemanWunchAlgorithmTestBasic}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-07-13
 */
public class NeedlemanWunchAlgorithmTestBasic {

	private static SequenceFactory<Boolean> factory;
	private static AlignmentAlgorithm<Boolean> simpleAlgorithm;

	public NeedlemanWunchAlgorithmTestBasic() {
	}

	@BeforeAll
	static void init() {
		FeatureModelLoader<Boolean> loader = BinaryFeature.INSTANCE.emptyLoader();
		FeatureMapping<Boolean> mapping = loader.getFeatureMapping();
		factory = new SequenceFactory<>(mapping, FormatterMode.NONE);
		
		SequenceComparator<Boolean> simpleComparator = (left, right, i, j) -> 
				Objects.equals(left.get(i), right.get(j)) ? 0 : 1;
		
		simpleAlgorithm = new NeedlemanWunschAlgorithm<>(
				BaseOptimization.MIN,
				simpleComparator,
				new NullGapPenalty<>(factory.toSequence("_")), factory
		);
	}

	@Test
	void getAlignment_01() {
		AlignmentResult<Boolean> result = simpleAlgorithm.apply(
				Arrays.asList(
						factory.toSequence("#aba"),
						factory.toSequence("#baba")));
		Assertions.assertFalse(result.getAlignments().isEmpty());
		assertEquals(1.0, result.getScore());
		assertEquals("# _ a b a \n# b a b a \n",
				result.getAlignments().get(0).getPrettyTable());
	}

	@Test
	void getAlignment_02() {
		AlignmentResult<Boolean> result = simpleAlgorithm.apply(
				Arrays.asList(
						factory.toSequence("#abab"),
						factory.toSequence("#baba")));
		Assertions.assertFalse(result.getAlignments().isEmpty());
		assertEquals(2.0, result.getScore());
		assertEquals("# _ a b a b \n# b a b a _ \n",
				result.getAlignments().get(0).getPrettyTable());
	}

	@Test
	void getAlignment_04() {
		AlignmentResult<Boolean> result = simpleAlgorithm.apply(
				Arrays.asList(
						factory.toSequence("#baba"),
						factory.toSequence("#ababb")));

		Assertions.assertFalse(result.getAlignments().isEmpty());
		assertEquals(2.0, result.getScore());
		//		String expected = "# a b a b a \t" + "# a b a _ a \t";
		//		assertEquals(expected, result.getAlignments().get(0).toString());
	}
}

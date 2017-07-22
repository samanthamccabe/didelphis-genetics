package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.gap.NullGapPenalty;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.BinaryFeature;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
		FeatureModelLoader<Boolean> loader = BinaryFeature.emptyLoader();
		factory = new SequenceFactory<>(
				loader.getFeatureMapping(), FormatterMode.NONE);
		Comparator<Boolean> simpleComparator = (left, right, i, j) ->
				Objects.equals(left.get(i),right.get(j)) ? 0 : 1;
		simpleAlgorithm = new NeedlemanWunschAlgorithm<>(
				simpleComparator,
				Optimization.MIN,
				new NullGapPenalty<>(factory.getSequence("_")), factory
		);
	}

	@Test
	void getAlignment_01() {
		AlignmentResult<Boolean> result = simpleAlgorithm.getAlignment(
				Arrays.asList(
						factory.getSequence("#aba"),
						factory.getSequence("#baba")));
		assertFalse(result.getAlignments().isEmpty());
		assertEquals(1.0, result.getScore());
		assertEquals("# _ a b a \n# b a b a \n",
				result.getAlignments().get(0).getPrettyTable());
	}

	@Test
	void getAlignment_02() {
		AlignmentResult<Boolean> result = simpleAlgorithm.getAlignment(
				Arrays.asList(
						factory.getSequence("#abab"),
						factory.getSequence("#baba")));
		assertFalse(result.getAlignments().isEmpty());
		assertEquals(2.0, result.getScore());
		assertEquals("# a b a b _ \n# _ b a b a \n",
				result.getAlignments().get(0).getPrettyTable());
	}

	@Test
	void getAlignment_04() {
		AlignmentResult<Boolean> result = simpleAlgorithm.getAlignment(
				Arrays.asList(
						factory.getSequence("#baba"),
						factory.getSequence("#ababb")));

		assertFalse(result.getAlignments().isEmpty());
		assertEquals(2.0, result.getScore());
		//		String expected = "# a b a b a \t" + "# a b a _ a \t";
		//		assertEquals(expected, result.getAlignments().get(0).toString());
	}
}

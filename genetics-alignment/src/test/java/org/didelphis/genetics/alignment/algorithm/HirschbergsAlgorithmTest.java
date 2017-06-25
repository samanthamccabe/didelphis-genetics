package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.io.ClassPathFileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.genetics.alignment.operators.gap.NullGapPenalty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by samantha on 5/22/17.
 */
class HirschbergsAlgorithmTest {

	private static final FormatterMode MODE = FormatterMode.INTELLIGENT;

	private static SequenceFactory<Integer> factory;
	private static GapPenalty<Integer> penalty;
	private static AlignmentAlgorithm<Integer> algorithm;

	@BeforeAll
	static void init() {
		String path = "AT_hybrid_reduced.model";

		FeatureType<Integer> featureType = IntegerFeature.INSTANCE;

		FeatureModelLoader<Integer> loader = new FeatureModelLoader<>(
				featureType, ClassPathFileHandler.INSTANCE, path);

		factory = new SequenceFactory<Integer>(loader.getFeatureMapping(), FormatterMode.INTELLIGENT);

		Sequence<Integer> gap = factory.getSequence("░");
		penalty = new NullGapPenalty<>(gap);
		algorithm = new HirschbergsAlgorithm<>((l, r, i, j) -> {
					FeatureArray<Integer> z = l.get(i).getFeatures();
					FeatureArray<Integer> x = r.get(j).getFeatures();
					return IntStream.range(0, z.size())
							.mapToDouble(k -> Math.abs(z.get(k) - x.get(k)))
							.sum();
				}, penalty, factory);
	}

	@Test
	void testAlignDNA() {
		
	}
	
	@Test
	void testGetAlignment() {
		List<Sequence<Integer>> sequences = Arrays.asList(
				factory.getSequence("amapar"),
				factory.getSequence("omber")
		);

		Alignment<Integer> alignment = algorithm.getAlignment(sequences);

//		for (CharSequence sequence : alignment.buildPrettyAlignments()) {
//			System.out.println(sequence);
//		}
		String table = alignment.getPrettyTable();
		System.out.println(table);
	}

}

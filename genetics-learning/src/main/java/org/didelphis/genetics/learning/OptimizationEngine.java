package org.didelphis.genetics.learning;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.didelphis.genetic.data.generation.BrownAlignmentGenerator;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.BaseOptimization;
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.comparators.BrownEtAlComparator;
import org.didelphis.genetics.alignment.operators.gap.ConstantGapPenalty;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.SymmetricalTwoKeyMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class {@code OptimizationEngine}
 *
 * @author Samantha Fiona McCabe
 * Date: 2017-08-01
 * @since 0.1.0 
 */
@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class OptimizationEngine<T> {

	Pattern NONE = Pattern.compile("Ø");
	FileHandler HANDLER = new DiskFileHandler("UTF-8");

	public static void main(String[] args) {
		// Basic case for a static model
		// 1. Load Data
		// 2. Load Model
		// 3. Generate Algorithm
		// 4. Align Data
		// 5. Score Data

		// -----------------------------------------------
		FeatureType<Integer> type = IntegerFeature.INSTANCE;
		String modelPath = "../data/ASJPcode.model";
		FormatterMode mode = FormatterMode.INTELLIGENT;

		SequenceFactory<Integer> factory = loadFactory(modelPath, HANDLER,
				type,
				mode
		);

		String dataPath = "../data/brown_correspondences.txt";
		BrownAlignmentGenerator generator
				= new BrownAlignmentGenerator(dataPath, 0.0, 1.0);
		Supplier<? extends List<String>> supplier = generator.supplier(4, 12);

		SymmetricalTwoKeyMap<Segment<Integer>, Double> scores
				= new SymmetricalTwoKeyMap<>();

		generator.getScores().stream()
				.forEach(t -> scores.put(factory.toSegment(t.getFirstElement()),
						factory.toSegment(t.getSecondElement()),
						t.getThirdElement()
				));

		Comparator<Integer> comparator = new BrownEtAlComparator<>(scores);
		Sequence<Integer> gap = factory.toSequence("Ø");

		int w = 1000;
		double interval = 0.1;
		double d = -5.0;
		while (d <= 10.0) {

			GapPenalty<Integer> penalty = new ConstantGapPenalty<>(gap, d);
			int count = 0;
			for (int i = 0; i < w; i++) {
				List<String> list = supplier.get();
				Alignment<Integer> goodAlignment = toAlignment(list, factory);

				AlignmentAlgorithm<Integer> algorithm
						= new NeedlemanWunschAlgorithm<>(comparator,
						BaseOptimization.MIN,
						penalty,
						factory
				);

				List<String> input = list.stream()
						.map(s -> NONE.matcher(s).replaceAll("").trim())
						.collect(Collectors.toList());
				List<Sequence<Integer>> sequences = toSequences(input, factory);

				AlignmentResult<Integer> result = algorithm.apply(sequences);
				for (Alignment<Integer> alignment : result.getAlignments()) {
					if (alignment.equals(goodAlignment)) {
						count++;
						break;
					}
				}
			}
			double fitness = count / (double) w;
			System.out.println(d + "\t" + fitness);

			d += interval;
		}
	}

	private static @NotNull <T> Alignment<T> toAlignment(
			@NotNull Iterable<String> list, @NotNull SequenceFactory<T> factory
	) {

		FeatureModel<T> model = factory.getFeatureMapping().getFeatureModel();
		List<Sequence<T>> sequences = toSequences(list, factory);
		return new Alignment<>(sequences, model);
	}

	private static @NotNull <T> List<Sequence<T>> toSequences(
			@NotNull Iterable<String> list, @NotNull SequenceFactory<T> factory
	) {
		List<Sequence<T>> sequences = new ArrayList<>();
		FeatureModel<T> model = factory.getFeatureMapping().getFeatureModel();
		for (String string : list) {
			Sequence<T> sequence = new BasicSequence<>(model);
			for (String s : string.split("\\s+")) {
				sequence.add(factory.toSegment(s));
			}
			sequences.add(sequence);
		}
		return sequences;
	}

	private static <T> SequenceFactory<T> loadFactory(
			String path,
			FileHandler handler,
			FeatureType<T> type,
			FormatterMode mode
	) {
		FeatureModelLoader<T> loader = new FeatureModelLoader<>(type,
				handler,
				path
		);
		return new SequenceFactory<>(loader.getFeatureMapping(), mode);
	}
}

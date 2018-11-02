package org.didelphis.genetics.learning;

import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.BaseOptimization;
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.operators.comparators.LinearWeightComparator;
import org.didelphis.genetics.alignment.operators.gap.ConvexGapPenalty;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.utilities.Splitter;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;
import static io.jenetics.engine.Limits.byFixedGeneration;

/**
 * Class {@code OptimizationEngine}
 *
 * @author Samantha Fiona McCabe
 * Date: 2017-08-01
 * @since 0.1.0 
 */
@ToString
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class Calibrator<T> {
	
	static NumberFormat DOUBLE_FORMAT = new DecimalFormat(" 0.00000;-0.00000");
	
	FileHandler handler;
	Sequence<T> gap;
	SequenceFactory<T> factory;
	FeatureModel<T> featureModel;
	Map<String, List<List<Alignment<T>>>> trainingData;

	public Calibrator(
			FileHandler handler, Sequence<T> gap, SequenceFactory<T> factory
	) {
		this.handler = handler;
		this.gap = gap;
		this.factory = factory;
		trainingData = new LinkedHashMap<>();
		featureModel = factory.getFeatureMapping().getFeatureModel();
	}

	public static void main(String[] args) {
		// Basic case for a static model
		// 1. Load Data
		// 2. Load Model
		// 3. Generate Algorithm
		// 4. Align Data
		// 5. Score Data

		// -----------------------------------------------
		FeatureType<Integer> type = IntegerFeature.INSTANCE;
		String modelPath = "../data/AT_extended_x.model";
		FormatterMode mode = FormatterMode.INTELLIGENT;

		FileHandler handler = new DiskFileHandler("UTF-8");

		SequenceFactory<Integer> factory = loadFactory(modelPath, handler,
				type,
				mode
		);
		
		Sequence<Integer> gap = factory.toSequence("░");
		
		Calibrator<Integer> optimizer = new Calibrator<>(handler, gap, factory);

		optimizer.addFile("E:/git/data/training/training_CHM-TND_aligned.csv");
		optimizer.addFile("E:/git/data/training/training_ING-CHE_aligned.csv");
		
		int extraParams = 2;

		FeatureMapping<Integer> featureMapping = factory.getFeatureMapping();
		FeatureSpecification specification = featureMapping.getSpecification();
		
		Engine<DoubleGene, Double> engine = Engine.builder(
				optimizer::fitness,
				DoubleChromosome.of(-10, 10, extraParams),
				DoubleChromosome.of(0, 10, specification.size())
		)
				.maximizing()
				.populationSize(500)
//				.maximalPhenotypeAge(20)
//				.survivorsFraction(0.8)
//				.offspringSize(3)
//				.selector(new MonteCarloSelector<>())
//				.selector(new BoltzmannSelector<>(0.5))
//				.selector(new StochasticUniversalSelector<>())
				.selector(new EliteSelector<>())
				.alterers(new Mutator<>(0.2))
				.build();

		EvolutionStatistics<Double, ?> stats = EvolutionStatistics.ofNumber();
		Phenotype<DoubleGene, Double> phenotype = engine.stream()
				.limit(byFixedGeneration(100)).peek(stats)
				.peek(result -> print(result))
				.collect(toBestPhenotype());

		Genotype<DoubleGene> genotype = phenotype.getGenotype();

		List<Double> chromosomeA = toList(genotype.get(0));
		List<Double> chromosomeB = toList(genotype.get(1));

		normalize(chromosomeA, chromosomeB);

		System.out.println(stats);
		System.out.println("F: "
				+ DOUBLE_FORMAT.format(phenotype.getFitness())
				+ " -> "
				+ formatList(chromosomeA)
				+ " | "
				+ formatList(chromosomeB)
		);
	}

	private void addFile(String filePath) {
		String fileData = handler.read(filePath);
		if (fileData == null) return;
		List<List<Alignment<T>>> list = new ArrayList<>();
		for (String line : Splitter.lines(fileData)) {
			
			List<Alignment<T>> alignmentGroup = new ArrayList<>();
			for (String group : line.split(";")) {

				List<Sequence<T>> sequences = new ArrayList<>();
				for (String element : group.split("[,\t]")) {

					element = element.replaceAll("\"([^\"]+)\"", "$1");

					Sequence<T> sequence = new BasicSequence<>(featureModel);
					for (String segment : Splitter.whitespace(element, Collections.emptyMap())) {
						sequence.add(factory.toSequence(segment));
					}
					sequences.add(sequence);
				}
				alignmentGroup.add(new Alignment<>(sequences, featureModel));
			}
			list.add(alignmentGroup);
			
		}
		
		// pop header
		list.remove(0);
		
		trainingData.put(filePath, list);
	}

	private <G extends Gene<Double, G>> double fitness(Genotype<G> genotype) {

		List<Double> chromosomeA = toList(genotype.get(0));
		List<Double> chromosomeB = toList(genotype.get(1));

		normalize(chromosomeA, chromosomeB);
		
		Set<String> filePaths = trainingData.keySet();

		double correct = 0.0;
		double total = 0.0;
		
		double openPenalty = chromosomeA.get(0);
		double growPenalty = chromosomeA.get(1);

		FeatureType<T> type = featureModel.getFeatureType();
		
		AlignmentAlgorithm<T> algorithm = new NeedlemanWunschAlgorithm<>(
				BaseOptimization.MIN,
				new LinearWeightComparator<>(type, chromosomeB),
				new ConvexGapPenalty<>(gap, openPenalty, growPenalty),
				factory
		);
		
		for (String filePath : filePaths) {
			List<List<Alignment<T>>> alignmentGroup = trainingData.get(filePath);
			
			for (List<Alignment<T>> alignments : alignmentGroup) {

				Alignment<T> baseAlignment = alignments.get(0);
				List<Sequence<T>> sequences = new ArrayList<>();
				for (int i = 0; i < baseAlignment.rows(); i++) {
					List<Segment<T>> list = baseAlignment.getRow(i)
							.stream()
							.filter(segment -> !segment.equals(gap.get(0)))
							.collect(Collectors.toList());
					sequences.add(new BasicSequence<>(list, featureModel));
				}
				
				// Difficulty is assessed on the basis of how many gaps are
				// present in the training data
				long nGaps = 1 + baseAlignment.getDelegate()
						.stream()
						.filter(tSegment -> tSegment.equals(gap.get(0)))
						.count();

				if (sequences.size() < 2) {
					continue;
				}
				AlignmentResult<T> result = algorithm.apply(sequences);
				for (Alignment<T> alignment : alignments) {
					if (result.getAlignments().contains(alignment)) {
						correct += Math.pow(nGaps,2);
						break;
					}
				}
				total += Math.pow(nGaps,2);
			}
		}
		
		return correct / total;
	}

	private static void print(EvolutionResult<DoubleGene, Double> result) {
		Phenotype<DoubleGene, Double> best = result.getBestPhenotype();

		Genotype<DoubleGene> genotype = best.getGenotype();

		List<Double> chromosomeA = toList(genotype.get(0));
		List<Double> chromosomeB = toList(genotype.get(1));

		normalize(chromosomeA, chromosomeB);

		//noinspection UseOfSystemOutOrSystemErr
		System.out.println(
				result.getGeneration() + " ("
				+ result.getPopulation().size()
				+ ") "
				+ DOUBLE_FORMAT.format(result.getWorstFitness())
				+ " : "
				+ DOUBLE_FORMAT.format(result.getBestFitness())
				+ " -> "
				+ formatList(chromosomeA)
				+" | "
				+ formatList(chromosomeB)
		);
	}
	
	private static String formatList(Collection<? extends Number> collection) {
		return collection.stream()
				.map(DOUBLE_FORMAT::format)
				.collect(Collectors.joining(" "));
	}

	@SafeVarargs
	@NonNull
	private static void normalize(List<Double>... lists) {
		double sum = Arrays.stream(lists)
				.flatMap(Collection::stream)
				.mapToDouble(value -> Math.abs(value))
				.sum();
		for (List<Double> list : lists) {
			for (int i = 0; i < list.size(); i++) {
				double d = list.get(i);
				list.set(i, 10 * d / sum);
			}
		}
	}

	@NonNull
	private static <G extends Gene<Double, G>> List<Double> toList(
			@NonNull Chromosome<G> chromosome
	) {
		return chromosome.stream()
				.map(gene -> gene.getAllele())
				.collect(Collectors.toList());
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

package org.didelphis.genetics.learning;

import io.jenetics.*;
import io.jenetics.engine.Engine;
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
	
	static NumberFormat DOUBLE_FORMAT = new DecimalFormat("0.00000");
	
	FileHandler handler;
	Sequence<T> gap;
	SequenceFactory<T> factory;
	FeatureModel<T> featureModel;
	Map<String, List<Alignment<T>>> trainingData;

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
		String modelPath = "../data/AT_extended.model";
		FormatterMode mode = FormatterMode.INTELLIGENT;

		FileHandler handler = new DiskFileHandler("UTF-8");

		SequenceFactory<Integer> factory = loadFactory(modelPath, handler,
				type,
				mode
		);
		
		Sequence<Integer> gap = factory.toSequence("░");
		
		Calibrator<Integer> optimizer = new Calibrator<>(handler, gap, factory);

		optimizer.addFile("E:/git/data/avar-andi/training_CHM-TND_aligned.csv");
		
		int extraParams = 2;

		FeatureMapping<Integer> featureMapping = factory.getFeatureMapping();
		FeatureSpecification specification = featureMapping.getSpecification();
		
		Engine<DoubleGene, Double> engine = Engine.builder(
				optimizer::fitness,
				DoubleChromosome.of(-5, 5, extraParams),
				DoubleChromosome.of(0, 10, specification.size())
		)
				.maximizing()
				.populationSize(1000)
				.maximalPhenotypeAge(20)
				.survivorsFraction(0.8)
				.selector(new TournamentSelector<>())
//				.selector(new StochasticUniversalSelector<>())
//				.selector(new EliteSelector<>())
				.alterers(new Mutator<>(0.1))
				.build();

		EvolutionStatistics<Double, ?> stats = EvolutionStatistics.ofNumber();


		Phenotype<DoubleGene, Double> phenotype = engine.stream()
				.limit(byFixedGeneration(200)).peek(stats).peek(result -> {
					Phenotype<DoubleGene, Double> best
							= result.getBestPhenotype();
					System.out.println(result.getGeneration() + " ("
							+ result.getPopulation().size()
							+ ") "
							+ DOUBLE_FORMAT.format(result.getWorstFitness())
							+ " : "
							+ DOUBLE_FORMAT.format(result.getBestFitness())
							+ " -> "
							+ formatList(toList(best.getGenotype()
							.getChromosome(0)))
							+" | "
							+ formatList(toList(best.getGenotype()
							.getChromosome(1))));
				})
				.collect(toBestPhenotype());
		
		System.out.println(stats);
		System.out.println("F: "
				+ phenotype.getFitness()
				+ ' '
				+ formatList(toList(phenotype.getGenotype().getChromosome(0)))
				+ " | "
				+ formatList(toList(phenotype.getGenotype().getChromosome(1))));
	}
	
	private static String formatList(List<? extends Number> list) {
		return list.stream().map(DOUBLE_FORMAT::format).collect(Collectors.joining(" "));
	}

	private void addFile(String filePath) {
		CharSequence fileData = handler.read(filePath);
		if (fileData == null) return;
		List<Alignment<T>> list = new ArrayList<>();
		for (String line : Splitter.lines(fileData)) {
			List<Sequence<T>> sequences = new ArrayList<>();
			for (String element : line.split("[,\t]")) {
				
				element = element.replaceAll("\"([^\"]+)\"", "$1");
				
				Sequence<T> sequence = new BasicSequence<>(featureModel);
				for (String segment : Splitter.whitespace(element)) {
					sequence.add(factory.toSequence(segment));
				}
				sequences.add(sequence);
			}
			list.add(new Alignment<>(sequences, featureModel));
		}
		
		// pop header
		list.remove(0);
		
		trainingData.put(filePath, list);
	}
	
	private <G extends Gene<Double, G>> double fitness(Genotype<G> genotype) {

		Chromosome<G> chromosomeA = genotype.get(0);
		Chromosome<G> chromosomeB = genotype.get(1);

		Set<String> filePaths = trainingData.keySet();

		int numberCorrect = 0;
		int numberTotal = 0;

		List<Double> values = toList(chromosomeB);

		double openPenalty = chromosomeA.getGene(0).getAllele();
		double growPenalty = chromosomeA.getGene(0).getAllele();

		FeatureType<T> type = featureModel.getFeatureType();
		
		AlignmentAlgorithm<T> algorithm = new NeedlemanWunschAlgorithm<>(
				BaseOptimization.MIN,
				new LinearWeightComparator<>(type, values),
				new ConvexGapPenalty<>(gap, openPenalty, growPenalty),
				factory
		);
		
		for (String filePath : filePaths) {
			List<Alignment<T>> alignments = trainingData.get(filePath);
			
			for (Alignment<T> alignment : alignments) {
				// TODO: convert alignments to unaligned sequences

				List<Sequence<T>> sequences = new ArrayList<>();
				for (int i = 0; i < alignment.rows(); i++) {
					List<Segment<T>> list = alignment.getRow(i)
							.stream()
							.filter(segment -> !segment.equals(gap.get(0)))
							.collect(Collectors.toList());
					sequences.add(new BasicSequence<T>(list, featureModel));
				}

				AlignmentResult<T> result = algorithm.apply(sequences);
				
				if (result.getAlignments().contains(alignment)) {
					numberCorrect++;
				}
				numberTotal++;
			}
		}
		
		return numberCorrect / (double) numberTotal;
	}

	@NonNull
	private static <G extends Gene<Double, G>> List<Double> normalize(
			@NonNull Chromosome<G> chromosome
	) {
		List<Double> doubles = toList(chromosome);
		double sum = doubles.stream().mapToDouble(value -> value).sum();
		return doubles.stream()
				.map(value -> value / sum)
				.collect(Collectors.toList());
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

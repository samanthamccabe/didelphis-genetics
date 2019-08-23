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

package org.didelphis.genetics.learning;

import io.jenetics.Chromosome;
import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.EliteSelector;
import io.jenetics.Gene;
import io.jenetics.Genotype;
import io.jenetics.MonteCarloSelector;
import io.jenetics.Mutator;
import io.jenetics.Phenotype;
import io.jenetics.StochasticUniversalSelector;
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
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.algorithm.optimization.BaseOptimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.comparators.LinearWeightComparator;
import org.didelphis.genetics.alignment.operators.comparators.MatrixComparator;
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
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;
import org.didelphis.utilities.Splitter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	
	static final NumberFormat DOUBLE_FORMAT = new DecimalFormat(" 0.00000;-0.00000");
	
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

	/* TODO: fields needed for an instance
	 *  - Model Path
	 *  -? Formatter Mode
	 *  - gap symbol
	 *  - training file or files
	 *  - Selector mode
	 *  - Population size
	 *  - Number of generations
	 */

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
		
		Calibrator<Integer> calibrator = new Calibrator<>(handler, gap, factory);

		calibrator.addFile("D:/git/data/training/training_synthetic.csv");
//		calibrator.addFile("D:/git/data/training/training_CHM-TND_aligned.csv");
//		calibrator.addFile("D:/git/data/training/training_ING-CHE_aligned.csv");
		
		int extraParams = 2;

		FeatureMapping<Integer> featureMapping = factory.getFeatureMapping();
		FeatureSpecification specification = featureMapping.getSpecification();

		int size = specification.size() - 1;
//		int bigSize = (size * size - size) / 2;
		Engine<DoubleGene, Double> engine = Engine.builder(
				calibrator::fitness,
				DoubleChromosome.of(-10, 10, extraParams),
				DoubleChromosome.of(  0, 15, size)
		)
				.maximizing()
				.populationSize(1000)
				.maximalPhenotypeAge(20)
//				.survivorsFraction(0.8)
//				.offspringSize(3)
//				.selector(new MonteCarloSelector<>())
//				.selector(new BoltzmannSelector<>(0.5))
//				.selector(new StochasticUniversalSelector<>())
				.selector(new EliteSelector<>())
				.alterers(new Mutator<>(0.4))
				.build();

		EvolutionStatistics<Double, ?> stats = EvolutionStatistics.ofNumber();
		Phenotype<DoubleGene, Double> phenotype = engine.stream()
				.limit(byFixedGeneration(100))
				.peek(stats)
				.peek(result -> print(result))
				.collect(toBestPhenotype());

		Genotype<DoubleGene> genotype = phenotype.getGenotype();

		List<Double> chromosomeA = toList(genotype.get(0));
		List<Double> chromosomeB = toList(genotype.get(1));
//		List<Double> chromosomeC = toList(genotype.get(2));

//		normalize(chromosomeA, chromosomeB, chromosomeC);

		System.out.println(stats);
		System.out.println("F: "
				+ DOUBLE_FORMAT.format(phenotype.getFitness())
				+ " -> "
				+ formatList(chromosomeA)
				+ " | "
				+ formatList(chromosomeB)
//				+ " | "
//				+ formatList(chromosomeC)
		);
	}

	private void addFile(String filePath) {
		String fileData = null;
		try {
			fileData = handler.read(filePath);
		} catch (IOException e) {
			return;
		}
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

		Set<String> filePaths = trainingData.keySet();

		double openPenalty = chromosomeA.get(0);
		double growPenalty = chromosomeA.get(1);

		SequenceComparator<T> comparator = getFlatComparator(chromosomeB);

		AlignmentAlgorithm<T> algorithm = new NeedlemanWunschAlgorithm<>(
				BaseOptimization.MIN,
//				new LinearWeightComparator<>(type, chromosomeB),
				comparator,
				new ConvexGapPenalty<>(gap, openPenalty, growPenalty),
				factory
		);

		return evaluate(filePaths, algorithm);
	}

	@SuppressWarnings ("MagicNumber")
	@NonNull
	private SequenceComparator<T> getFlatComparator(List<Double> chromosome) {
		chromosome.add(0, 10.0);
		FeatureType<T> type = featureModel.getFeatureType();
		return new LinearWeightComparator<>(type, chromosome);
	}

	@NonNull
	private SequenceComparator<T> getMatrixComparator(
			List<Double> chromosomeB,
			List<Double> chromosomeC
	) {
		int size = chromosomeB.size();
		Table<Double> table = new RectangularTable<>(0.0, size, size);
		int x = 0;
		for (int row = 1; row < table.rows(); row++) {
			for (int col = 0; col < row; col++) {
				table.set(row, col, chromosomeC.get(x));
				x++;
			}
		}
		for (int i = 0; i < table.rows(); i++) {
			table.set(i, i, chromosomeB.get(i));
		}
		FeatureType<T> type = featureModel.getFeatureType();
		return new MatrixComparator<>(type, table);
	}

	private double evaluate(
			Set<String> filePaths, AlignmentAlgorithm<T> algorithm
	) {
		double correct = 0.0;
		double total = 0.0;
		for (String filePath : filePaths) {
			List<List<Alignment<T>>> alignmentGroup = trainingData.get(filePath);

			for (List<Alignment<T>> alignments : alignmentGroup) {

				Alignment<T> baseAlignment = alignments.get(0);
				List<Sequence<T>> sequences = getSequences(baseAlignment);

				if (sequences.size() < 2) {
					continue;
				}

				// Difficulty is assessed on the basis of how many gaps are
				// present in the training data
//				long nGaps = 1 + baseAlignment.getDelegate()
//						.stream()
//						.filter(tSegment -> tSegment.equals(gap.get(0)))
//						.count();

				AlignmentResult<T> result = algorithm.apply(sequences);
				for (Alignment<T> alignment : alignments) {
					if (result.getAlignments().contains(alignment)) {
//						correct += Math.pow(nGaps,2);
						correct++;
						break;
					}
				}
//				total += Math.pow(nGaps,2);
				total++;
			}
		}

		return correct / total;
	}

	@NonNull
	private List<Sequence<T>> getSequences(Alignment<T> baseAlignment) {
		List<Sequence<T>> sequences = new ArrayList<>();
		for (int i = 0; i < baseAlignment.rows(); i++) {
			List<Segment<T>> list = baseAlignment.getRow(i)
					.stream()
					.filter(segment -> !segment.equals(gap.get(0)))
					.collect(Collectors.toList());
			sequences.add(new BasicSequence<>(list, featureModel));
		}
		return sequences;
	}

	private static void print(EvolutionResult<DoubleGene, Double> result) {
		Phenotype<DoubleGene, Double> best = result.getBestPhenotype();

		Genotype<DoubleGene> genotype = best.getGenotype();

		List<Double> chromosomeA = toList(genotype.get(0));
		List<Double> chromosomeB = toList(genotype.get(1));
//		List<Double> chromosomeC = toList(genotype.get(2));

//		normalize(chromosomeA, chromosomeB, chromosomeC);

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
//				+ " | "
//				+ formatList(chromosomeC)
		);
	}
	
	private static String formatList(Collection<? extends Number> collection) {
		return collection.stream()
				.map(DOUBLE_FORMAT::format)
				.collect(Collectors.joining(" "));
	}

	@SafeVarargs
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

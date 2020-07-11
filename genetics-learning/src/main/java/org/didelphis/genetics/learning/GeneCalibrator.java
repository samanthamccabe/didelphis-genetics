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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.AlignmentMode;
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.algorithm.optimization.BaseOptimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.comparators.ContextComparator;
import org.didelphis.genetics.alignment.operators.comparators.LinearWeightComparator;
import org.didelphis.genetics.alignment.operators.comparators.MatrixComparator;
import org.didelphis.genetics.alignment.operators.comparators.SparseMatrixComparator;
import org.didelphis.genetics.alignment.operators.gap.ConvexGapPenalty;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.parsing.ParseException;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.GeneralTwoKeyMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMap;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;
import org.didelphis.structures.tuples.Twin;

import io.jenetics.Chromosome;
import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.EliteSelector;
import io.jenetics.GaussianMutator;
import io.jenetics.Gene;
import io.jenetics.Genotype;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.jenetics.engine.EvolutionResult.*;
import static io.jenetics.engine.Limits.*;

/**
 * Class {@code OptimizationEngine}
 *
 * @since 0.1.0
 */
@ToString
@EqualsAndHashCode (callSuper = true)
@FieldDefaults (level = AccessLevel.PRIVATE, makeFinal = true)
public final class GeneCalibrator<T>
		extends AbstractCalibrator<T, Genotype<DoubleGene>> {

	private static final NumberFormat DOUBLE_FORMAT = new DecimalFormat("0.000");
	private static final NumberFormat DOUBLE_FORMAT_LONG = new DecimalFormat(" 0.00000;-0.00000");

	private static final double FIXED_WEIGHT = Double.NaN;
	private static final int FIXED_POSITION = 0;
	private static final double Z = 0.000000001;

	long startTime;
	FeatureModel<T> featureModel;
	Map<String, List<List<Alignment<T>>>> trainingData;

	public static void main(String[] args) {
		// Basic case for a static model
		// 1. Load Data
		// 2. Load Model
		// 3. Generate Algorithm
		// 4. Align Data
		// 5. Score Data

		boolean useReinforcement = false;

		// -----------------------------------------------
		String dataPath  = "/git/didelphis-genetics-data/";
		String modelPath = dataPath + "model/AT_extended_x.model";
		String sdmPath   = dataPath + "sdm/";

		FeatureType<Integer> type = IntegerFeature.INSTANCE;
		FormatterMode mode = FormatterMode.INTELLIGENT;

		FileHandler handler = new DiskFileHandler("UTF-8");
		FeatureModelLoader<Integer> loader = new FeatureModelLoader<>(type,
				handler,
				modelPath
		);
		FeatureMapping<Integer> mapping = loader.getFeatureMapping();
		SequenceFactory<Integer> factory = new SequenceFactory<>(mapping, mode);

		Sequence<Integer> gap = factory.toSequence("░");

		GeneCalibrator<Integer> calibrator = new GeneCalibrator<>(
				handler, gap, factory, useReinforcement
		);

		calibrator.addCorrelation("con", "son");
		calibrator.addCorrelation("con", "cnt");
		calibrator.addCorrelation("son", "cnt");
		calibrator.addCorrelation("lat", "nas");
		calibrator.addCorrelation("vce", "son");
		calibrator.addCorrelation("lab", "rnd");
		calibrator.addCorrelation("eje", "rel");
		calibrator.addCorrelation("con", "eje");
		calibrator.addCorrelation("con", "rel");

		calibrator.addCorrelation("frn", "bck");
		calibrator.addCorrelation("frn", "hgt");
		calibrator.addCorrelation("hgt", "bck");
		calibrator.addCorrelation("hgt", "low");
		calibrator.addCorrelation("bck", "low");

		// Load all SDM training data
		File[] files = new File(sdmPath).listFiles();
		assert files != null : "Files under " + sdmPath + " cannot be read.";
		for (File file : files) {
			try {
				calibrator.addSDM(file.getAbsolutePath());
			} catch (RuntimeException e) {
				throw new ParseException("Error reading " + file.getName(), e);
			}
		}

		Genotype<DoubleGene> genotype = calibrator.optimize();
		AlignmentAlgorithm<Integer> algorithm = calibrator.toAlgorithm(genotype);

		long timestamp = System.currentTimeMillis();
		for (String path : calibrator.trainingData.keySet()) {
			String fileName = path.replaceAll("\\.sdm$", "_" + timestamp);
			calibrator.writeBestAlignments(path, fileName + ".csv", algorithm);
		}

		double fitness = calibrator.fitness(genotype, 1.0);

		System.out.printf("│ %s | %s │%n",
				DOUBLE_FORMAT_LONG.format(fitness).trim(),
				toParameterString(genotype, DOUBLE_FORMAT_LONG));
	}

	public GeneCalibrator(
			FileHandler handler,
			Sequence<T> gap,
			SequenceFactory<T> factory,
			boolean useReinforcement
	) {
		super(handler, gap, factory, useReinforcement);
		startTime = System.nanoTime();
		trainingData = new LinkedHashMap<>();
		featureModel = factory.getFeatureMapping().getFeatureModel();
	}

	@NonNull
	@Override
	public Genotype<DoubleGene> optimize() {

		FeatureMapping<T> featureMapping = getFactory().getFeatureMapping();
		FeatureSpecification specification = featureMapping.getSpecification();

		int size = Double.isNaN(FIXED_WEIGHT)
				? specification.size()
				: specification.size() - 1;

//		int fSize = getCorrelatedFeatures().size();
//		double rf = isUseReinforcement() ? 20 : 0.000001;

		Engine<DoubleGene, Double> engine = Engine.builder(
				(Genotype<DoubleGene> genotype) -> fitness(genotype, 0.33),
				DoubleChromosome.of(  -5, 5,        2), // Gaps
				DoubleChromosome.of(  -3, 3, size * 6) // Main Features

//				DoubleChromosome.of( -3,  3, fSize), // Correlated Features
//				DoubleChromosome.of(  0,  Z,     4), // Context Re-weighting
//				DoubleChromosome.of(  0, rf,     1)  // Reinforcement weight
		)
				.maximizing()
				.populationSize(500)
				.selector(new EliteSelector<>())
				.alterers(new GaussianMutator<>(0.3))
				.build();

		return engine.stream()
				.limit(bySteadyFitness(200))
				.peek(this::print)
				.collect(toBestGenotype());
	}

	/* TODO: fields needed for an instance
	 *  - Model Path
	 *  -? Formatter Mode
	 *  - gap symbol
	 *  - training file or files
	 *  - Selector mode
	 *
	 *  - Population size
	 *  - Number of generations
	 */

	@Override
	@NonNull
	public AlignmentAlgorithm<T> toAlgorithm(
			@NonNull Genotype<DoubleGene> parameters
	) {
		List<Double> listA = toList(parameters, 0);
		ConvexGapPenalty<T> gapPenalty = new ConvexGapPenalty<>(
				getGap(),
				listA.get(0),
				listA.get(1)
		);

		List<Double> listB = toList(parameters, 1);
//		List<Double> listC = toList(parameters, 2);
//		TwoKeyMap<Integer, Integer, Double> sparseWeights = toSparseWeights(listC);
//		SequenceComparator<T> comparator = getSparseComparator(listB, sparseWeights);

//		Chromosome<DoubleGene> chromosome = parameters.getChromosome(3);
		ContextComparator<T> contextComparator = new ContextComparator<>(
				featureModel.getFeatureType(), listB
		);

		return new NeedlemanWunschAlgorithm<>(
				BaseOptimization.MIN,
				AlignmentMode.GLOBAL,
				contextComparator,
				gapPenalty,
				getFactory()
		);
	}

	@Override
	public double getReinforcementWeight(@NonNull Genotype<DoubleGene> parameter) {
		return parameter.get(parameter.length() - 1).get(0).doubleValue();
	}

	@NonNull
	private TwoKeyMap<Integer, Integer, Double> toSparseWeights(List<Double> chromosome) {
		List<Twin<Integer>> correlatedFeatures = getCorrelatedFeatures();
		TwoKeyMap<Integer, Integer, Double> correlates = new GeneralTwoKeyMap<>();
		for (int i = 0; i < correlatedFeatures.size(); i++) {
			Twin<Integer> feature = correlatedFeatures.get(i);
			Double aDouble = chromosome.get(i);
			correlates.put(feature.getLeft(), feature.getRight(), aDouble);
		}
		return correlates;
	}

	@NonNull
	private SequenceComparator<T> getFlatComparator(List<Double> weights) {
		if (!Double.isNaN(FIXED_WEIGHT)) {
			weights.add(FIXED_POSITION, FIXED_WEIGHT);
		}
		FeatureType<T> type = featureModel.getFeatureType();
		return new LinearWeightComparator<>(type, weights);
	}

	@NonNull
	private SequenceComparator<T> getSparseComparator(
			List<Double> weights,
			TwoKeyMap<Integer, Integer, Double> sparseWeights
	) {
		if (!Double.isNaN(FIXED_WEIGHT)) {
			weights.add(FIXED_POSITION, FIXED_WEIGHT);
		}
		FeatureType<T> type = featureModel.getFeatureType();
		SequenceComparator<T> comparator = new LinearWeightComparator<>(type, weights);
		return new SparseMatrixComparator<>(type, comparator, sparseWeights);
	}

	@NonNull
	private SequenceComparator<T> getMatrixComparator(
			List<Double> chromosomeB, List<Double> chromosomeC
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

	private void print(EvolutionResult<DoubleGene, Double> result) {
		Genotype<DoubleGene> genotype = result.getBestPhenotype().getGenotype();
		String join = toParameterString(genotype, DOUBLE_FORMAT);
		System.out.printf("[%d] %d (%d) %s : %s -> %s%n",
				result.getGeneration(),
				(System.nanoTime() - startTime) / 1000000,
				result.getPopulation().size(),
				DOUBLE_FORMAT.format(result.getWorstFitness()),
				DOUBLE_FORMAT.format(result.getBestFitness()),
				join
		);
	}

	@NonNull
	private static String toParameterString(
			Genotype<DoubleGene> genotype,
			NumberFormat format
	) {
		Collection<String> parameterGroups = new ArrayList<>();
		for (int i = 0; i < genotype.length(); i++) {
			List<Double> collection = toList(genotype, i);
			if (i == 1 && Double.isFinite(FIXED_WEIGHT)) {
				collection.add(FIXED_POSITION, FIXED_WEIGHT);
			}
			parameterGroups.add(formatList(collection, format));
		}
		return String.join(" │ ", parameterGroups);
	}

	private static String formatList(
			Collection<? extends Number> collection, NumberFormat format
	) {
		return collection.stream()
				.map(format::format)
				.collect(Collectors.joining(" "));
	}

	@NonNull
	private static <G extends Gene<Double, G>> List<Double> toList(
			@NonNull Genotype<G> genotype, int i
	) {
		Chromosome<G> chromosome = genotype.get(i);
		return chromosome.stream()
				.map(Gene::getAllele)
				.collect(Collectors.toList());
	}
}

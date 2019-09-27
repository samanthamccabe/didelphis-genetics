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
import io.jenetics.Gene;
import io.jenetics.Genotype;
import io.jenetics.Mutator;
import io.jenetics.Phenotype;
import io.jenetics.SinglePointCrossover;
import io.jenetics.StochasticUniversalSelector;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import lombok.NonNull;
import lombok.ToString;
import org.didelphis.genetic.data.generation.BrownAlignmentGenerator;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.optimization.BaseOptimization;
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.common.StringTransformer;
import org.didelphis.genetics.alignment.operators.comparators.BrownEtAlComparator;
import org.didelphis.genetics.alignment.operators.gap.ConstantGapPenalty;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.DoubleFeature;
import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.StandardFeatureArray;
import org.didelphis.language.phonetic.model.DefaultFeatureSpecification;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.language.phonetic.model.GeneralFeatureMapping;
import org.didelphis.language.phonetic.model.GeneralFeatureModel;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.SymmetricalTwoKeyMap;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tables.Table;
import org.didelphis.structures.tuples.Triple;
import org.didelphis.utilities.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;
import static io.jenetics.engine.Limits.bySteadyFitness;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.didelphis.genetics.alignment.common.Utilities.loadTable;
import static org.didelphis.genetics.alignment.common.Utilities.toAlignments;
import static org.didelphis.genetics.alignment.common.Utilities.toPhoneticTable;
import static org.didelphis.genetics.alignment.common.Utilities.toTable;

/**
 * Class {@code ModelGenerator}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-06-28
 */
@ToString
public final class ModelGenerator<T> {

	private static final Logger LOG = Logger.create(ModelGenerator.class);

	private static final Pattern SPACE = Pattern.compile("\\s+");
	private static final String DATE_FORMAT = "yyyy-MM-dd/HH-mm-ss";
	private static final Pattern EXTENSION = Pattern.compile("\\.[^.]+$");

	private static final double CUTOFF = 1.0;
	private static final int ITERATIONS = 200;
	private static final String MATRIX_PATH = "brown.utx";

	private static final Function<String, String> TRANSFORMER
			= new StringTransformer("Ø >> ⬚");
	private static final UnaryOperator<String> DELETE_GAP
			= s -> Pattern.compile("⬚").matcher(s).replaceAll("");

	private final FileHandler handler;
	private final int features;
	private final List<String> symbols;
	private final List<String> modifiers;
	private final BrownAlignmentGenerator generator;
	private final FeatureType<T> featureType;
	public static final FileHandler HANDLER = new DiskFileHandler("UTF-8");

	private ModelGenerator(FeatureType<T> featureType, FileHandler handler,
			BrownAlignmentGenerator generator, int features,
			List<String> symbols, List<String> modifiers
	) {
		this.featureType = featureType;
		this.handler = handler;
		this.features = features;
		this.symbols = symbols;
		this.modifiers = modifiers;
		//		this.table = table;

		//		for (int i = 0; i < table.rows(); i++) {
		//			for (int j = 0; j < table.columns(); j++) {
		//				table.set(i,j, '#' +table.get(i,j));
		//			}
		//		}

		this.generator = generator;
	}

	public static void main(String[] args) throws IOException {

		String dataPath = "../data/";
		String symbolsPath = dataPath + "ASJP_Symbols";

		String trainingPath = dataPath + "training/";
		String dataSetName = "out.sample_1k.txt";

//		String timeStamp = new DateTime().toString(DATE_FORMAT);

		String dataSetFolder = EXTENSION.matcher(dataSetName).replaceAll("/");

		Path logPath = new File(trainingPath + dataSetFolder +
				"-fitness.log").toPath();

		Files.createDirectories(logPath.getParent());
		BufferedWriter logWriter = Files.newBufferedWriter(logPath, CREATE);

		DecimalFormat formatter = new DecimalFormat("#.0000");
		Consumer<EvolutionResult<?, Double>> tracker = new StatsTracker(1,
				logWriter, formatter
		);

		CharSequence read = HANDLER.read(symbolsPath);
		String clean = TRANSFORMER.apply(read.toString());
		String[] split = clean.split("\n");
		List<String> symbols = Arrays.asList(SPACE.split(split[0]));
		List<String> modifiers = Arrays.asList(SPACE.split(split[1]));

		int features = 15;

		FeatureType<Double> featureType = DoubleFeature.INSTANCE;

		String correspondenceDataPath = "../data/brown_correspondences.txt";

		BrownAlignmentGenerator brownAlignmentGenerator
				= new BrownAlignmentGenerator(correspondenceDataPath, 0.0, 0.5);
		ModelGenerator<Double> generator = new ModelGenerator<>(featureType,
				HANDLER, brownAlignmentGenerator, features, symbols, modifiers
		);

		int numberOfSymbols = symbols.size() + modifiers.size();

		Engine<DoubleGene, Double> engine = Engine.builder(generator::fitness,
				DoubleChromosome.of(-200, 200)
		)
				.populationSize(100)
				.selector(new StochasticUniversalSelector<>())
				.alterers(
						new Mutator<>(0.10),
						new SinglePointCrossover<>(0.03)
				)
				.build();
		/*
		Engine<IntegerGene, Double> engine = Engine.builder(
				generator::fitness,
				IntegerChromosome.of(-50,100, 1)
//				,
//				IntegerChromosome.of(0, 1, numberOfSymbols * features)
		)
				.populationSize(200)
				.selector(new StochasticUniversalSelector<>())
				.alterers(
						new Mutator<>(0.10),
						new SinglePointCrossover<>(0.03)
				)
				.build();
*/
		EvolutionStatistics<Double, ?> stats = EvolutionStatistics.ofNumber();
		Phenotype<DoubleGene, Double> best = engine.stream()
				.limit(bySteadyFitness(10))
				.limit(100)
				.peek(tracker)
				.peek(stats)
				.collect(toBestPhenotype());

		logWriter.close();

		System.out.println(stats);
		System.out.println(best);

		Genotype<DoubleGene> genotype = best.getGenotype();
		String inputPath = trainingPath + dataSetName;
		String outputPath = trainingPath + dataSetFolder ;
		ColumnTable<String> table = loadTable(inputPath, TRANSFORMER);
		generator.writeBestGenome(outputPath, table, genotype);
	}

	private <G extends Gene<T, G>> void writeBestGenome(String outputPath,
			ColumnTable<String> table,
			Genotype<G> genotype
	) {
		SequenceFactory<T> factory = toFactory(genotype);
		AlignmentAlgorithm<T> algorithm = toAlgorithm(factory, genotype);
		//noinspection DynamicRegexReplaceableByCompiledPattern
		ColumnTable<Sequence<T>> testWords = toPhoneticTable(table, factory,
				DELETE_GAP
		);
		List<Alignment<T>> testData = doAlignment(algorithm, testWords);
		writeAlignments(handler, testData, outputPath);
		writeMapping(handler, factory, outputPath);
	}

	private BrownAlignmentGenerator getGenerator() {
		return generator;
	}

	private <G extends Gene<T, G>> Double fitness(Genotype<G> genotype) {
		SequenceFactory<T> factory = toFactory(genotype);

		AlignmentAlgorithm<T> algorithm = toAlgorithm(factory,
				genotype
		);

		BrownAlignmentGenerator alignmentGenerator = getGenerator();

		String generate = alignmentGenerator.generate(3, 10, ITERATIONS);

		ColumnTable<String> table = toTable(generate, TRANSFORMER);
		ColumnTable<Sequence<T>> testWords = toPhoneticTable(table, factory,
				DELETE_GAP
		);
		List<Alignment<T>> trainData = toTrainingData(factory, table);
		List<Alignment<T>> testsData = doAlignment(algorithm, testWords);
		int correct = 0;
		int tested = 0;
		for (int i = 0; i < testWords.rows(); i++) {
			if (Math.random() < CUTOFF) {
				tested++;
				Alignment<T> aT = trainData.get(i);
				Alignment<T> aO = testsData.get(i);
				if (aO.equals(aT)) {
					correct++;
				}
			}
		}
		return correct / (double) tested;
	}

	@NonNull
	private <G extends Gene<T, G>> SequenceFactory<T> toFactory(
			Genotype<G> genotype
	) {
		GeneralFeatureModel<T> model = new GeneralFeatureModel<>(
				featureType,
				featureType.emptyLoader().getSpecification(),
				Collections.emptyList(),
				Collections.emptyMap()
		);

		GeneralFeatureMapping<T> mapping = new GeneralFeatureMapping<>(model,
				Collections.emptyMap(), Collections.emptyMap()
		);

		return new SequenceFactory<>(mapping, FormatterMode.INTELLIGENT);
	}

	@NonNull
	private FeatureMapping<T> toMapping(Table<T> featureTable,
			FeatureModel<T> model
	) {
		Map<String, FeatureArray<T>> sMap = parseSymbols(featureTable, model);
		Map<String, FeatureArray<T>> mMap = parseModifiers(featureTable, model);
		return new GeneralFeatureMapping<>(model, sMap, mMap);
	}

	@NonNull
	private <G extends Gene<T, G>> Map<String, FeatureArray<T>> parseSymbols(
			Table<T> table, FeatureModel<T> model
	) {
		Map<String, FeatureArray<T>> sMap = new HashMap<>(symbols.size());
		for (int i = 0; i < symbols.size(); i++) {
			List<T> row = table.getRow(i)
					.subList(0, model.getSpecification().size());
			sMap.put(symbols.get(i), new StandardFeatureArray<>(row, model));
		}
		return sMap;
	}

	@NonNull
	private <G extends Gene<T, G>> Map<String, FeatureArray<T>> parseModifiers(
			Table<T> table, FeatureModel<T> model
	) {
		int modelSize = model.getSpecification().size();
		int size = table.rows();
		int symbolsSize = symbols.size() + modifiers.size();

		Map<String, FeatureArray<T>> mMap = new HashMap<>(modifiers.size());
		for (int i = symbols.size(); i < symbolsSize; i++) {
			List<T> row = table.getRow(i).subList(0, modelSize);
			int index = i - symbols.size();
			mMap.put(modifiers.get(index),
					new StandardFeatureArray<>(row, model)
			);
		}
		return mMap;
	}

	private static <T> void writeMapping(FileHandler handler,
			SequenceFactory<T> factory, String pathname
	) {
		FeatureMapping<T> mapping = factory.getFeatureMapping();
		Map<String, FeatureArray<T>> featureMap = mapping.getFeatureMap();
		Map<String, FeatureArray<T>> modifierMap = mapping.getModifiers();
		StringBuilder buffer = new StringBuilder(0x1000);
		buffer.append("\nSYMBOLS\n");
		buffer.append(writeSymbols(featureMap));
		buffer.append("\nMODIFIERS\n");
		buffer.append(writeSymbols(modifierMap));
		try {
			handler.writeString(pathname + ".mapping", buffer.toString());
		} catch (IOException e) {
			LOG.error("Unable to write to file {}", pathname, e);

		}
	}

	private static <T> CharSequence writeSymbols(
			Map<String, FeatureArray<T>> featureMap
	) {
		StringBuilder buffer = new StringBuilder();
		for (Entry<String, FeatureArray<T>> entry : featureMap.entrySet()) {
			String key = entry.getKey();
			buffer.append(key);
			for (T t : entry.getValue()) {
				buffer.append('\t').append(t);
			}
			buffer.append('\n');
		}
		return buffer;
	}

	private static <T> void writeAlignments(FileHandler handler,
			Iterable<Alignment<T>> testData, String pathName
	) {
		Path outputPath = new File(pathName + "-alignment").toPath();
		StringBuilder buffer = new StringBuilder();
		for (Alignment<T> testDatum : testData) {
			String str = testDatum.toString();
			buffer.append(str).append('\n');
		}
		try {
			handler.writeString(outputPath.toString(), buffer.toString());
		} catch (IOException e) {
			LOG.error("Unable to write to file {}", outputPath, e);
		}
	}

	private static @NonNull <T> List<Alignment<T>> doAlignment(
			AlignmentAlgorithm<T> algorithm, Table<Sequence<T>> testWords
	) {
		List<Alignment<T>> testsData = new ArrayList<>(testWords.size());
		for (int i = 0; i < testWords.rows(); i++) {
			List<Sequence<T>> row = testWords.getRow(i);
			AlignmentResult<T> alignmentResult = algorithm.apply(row.get(0), row.get(1));
			List<Alignment<T>> alignment = alignmentResult.getAlignments();
			testsData.add(alignment.get(0));
		}
		return testsData;
	}

	private static <T> List<Alignment<T>> toTrainingData(
			SequenceFactory<T> factory, ColumnTable<String> table
	) {
		ColumnTable<Sequence<T>> raw = toPhoneticTable(table, factory,
				Function.identity()
		);
		return toAlignments(raw, factory);
	}

	@NonNull
	private <G extends Gene<T, G>> AlignmentAlgorithm<T> toAlgorithm(
			SequenceFactory<T> factory,
			Genotype<G> genotype
	) {
//		Comparator<T> comparator = Utilities.loadMatrixComparator(
//				new DiskFileHandler("UTF-8"), factory, Function.identity(),
//				MATRIX_PATH
//		);

		SymmetricalTwoKeyMap<Segment<T>, Double> scores = new SymmetricalTwoKeyMap<>();
		for (Triple<String, String, Double> triple : generator.getScores()) {
			scores.put(
					factory.toSegment(triple.getFirstElement()),
					factory.toSegment(triple.getSecondElement()),
					triple.getThirdElement());
		}

		BrownEtAlComparator<T> comparator = new BrownEtAlComparator<>(scores);

		Chromosome<G> ch = genotype.getChromosome(0);
		double gap1 = featureType.doubleValue(ch.getGene(0).getAllele());
		//		Integer gap2 = chromosome.getGene(1).getAllele();
		GapPenalty<T> penalty = new ConstantGapPenalty<>(
				factory.toSequence("⬚"), gap1);
		//		GapPenalty<T> penalty = new ConvexGapPenalty<>(factory.toSequence("⬚"), gap1, gap2);
		return new NeedlemanWunschAlgorithm<>(BaseOptimization.MIN,
				comparator,
				penalty, factory
		);
	}

	private static <T, G extends Gene<T, G>> List<T> toFeatureBits(
			FeatureType<T> featureType, Genotype<G> genotype
	) {
		Chromosome<G> features = genotype.getChromosome(0);
		return features.stream()
				// TODO: fix this
				.map(integerGene -> featureType.parseValue(
						String.valueOf(integerGene.getAllele())))
				.collect(Collectors.toList());
	}

	private static @NonNull FeatureSpecification getSpecification(int n) {
		List<String> names = new ArrayList<>(n);
		Map<String, Integer> indices = new HashMap<>(n);
		IntStream.range(0, n).forEach(i -> {
			names.add(Integer.toHexString(i));
			indices.put(Integer.toHexString(i), i);
		});
		return new DefaultFeatureSpecification(names, indices);
	}
}

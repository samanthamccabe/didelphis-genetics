package org.didelphis.genetics.learning;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.common.StringTransformer;
import org.didelphis.genetics.alignment.operators.SimpleComparator;
import org.didelphis.genetics.alignment.operators.gap.ConstantGapPenalty;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.features.StandardFeatureArray;
import org.didelphis.language.phonetic.model.DefaultFeatureSpecification;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.language.phonetic.model.GeneralFeatureMapping;
import org.didelphis.language.phonetic.model.GeneralFeatureModel;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;
import org.jenetics.Chromosome;
import org.jenetics.IntegerGene;
import org.jenetics.Genotype;
import org.jenetics.IntegerChromosome;
import org.jenetics.Mutator;
import org.jenetics.Phenotype;
import org.jenetics.RouletteWheelSelector;
import org.jenetics.SinglePointCrossover;
import org.jenetics.StochasticUniversalSelector;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionStatistics;
import org.jetbrains.annotations.NotNull;

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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.file.StandardOpenOption.*;
import static org.didelphis.genetics.alignment.common.Utilities.*;
import static org.jenetics.engine.EvolutionResult.toBestPhenotype;
import static org.jenetics.engine.limit.bySteadyFitness;

/**
 * Class {@code ModelGenerator}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-06-28
 */
public final class ModelGenerator {
	private static final Pattern SPACE = Pattern.compile("\\s+");
	private final List<String> symbols;
	private final List<String> modifiers;
	private final ColumnTable<String> table;

	private ModelGenerator(List<String> symbols, List<String> modifiers,
			ColumnTable<String> table) {
		this.symbols = symbols;
		this.modifiers = modifiers;
		this.table = table;

		for (int i = 0; i < table.rows(); i++) {
			for (int j = 0; j < table.columns(); j++) {
				table.set(i,j, '#' +table.get(i,j));
			}
		}
	}

	public static void main(String[] args) throws IOException {

		String dataPath = "/home/samantha/git/data/";
		String symbolsPath = dataPath+"ASJP_Symbols";

		FileHandler handler = new DiskFileHandler("UTF-8");

		String trainingPath = dataPath + "training/";
		String dataSetName = "out.sample_1k.utx";

		String dataSetFolder = dataSetName.replaceAll("\\.[^.]+$", "/");

		Path logPath = new File(trainingPath + dataSetFolder + "fitness.log").toPath();

		BufferedWriter writer = Files.newBufferedWriter(logPath, CREATE);

		DecimalFormat formatter = new DecimalFormat("#.0000");
		StatsTracker<IntegerGene> tracker = new StatsTracker<>(1, writer, formatter);

		Function<String, String> transformer = new StringTransformer("Ø >> ⬚");

		String clean = transformer.apply(handler.read(symbolsPath).toString());
		String[] split = clean.split("\n");
		List<String> symbols   = Arrays.asList(SPACE.split(split[0]));
		List<String> modifiers = Arrays.asList(SPACE.split(split[1]));
		ColumnTable<String> table = loadTable(trainingPath + dataSetName, transformer);
		ModelGenerator generator = new ModelGenerator(symbols, modifiers, table);

		int numberOfSymbols = symbols.size() + modifiers.size();
		int maximumFeatures = 25;
		int minumumFeatures = 10;

		Engine<IntegerGene, Double> engine = Engine.builder(
				generator::fitness,
				IntegerChromosome.of(minumumFeatures, maximumFeatures),
				IntegerChromosome.of(0, 1, numberOfSymbols*maximumFeatures),
				IntegerChromosome.of(-5,10, 1)
		)
				.populationSize(100)
				.selector(new StochasticUniversalSelector<>())
				.alterers(
						new Mutator<>(0.55),
						new SinglePointCrossover<>(0.06)
				)
				.build();

		EvolutionStatistics<Double, ?> stats = EvolutionStatistics.ofNumber();
		Phenotype<IntegerGene, Double> best = engine.stream()
				.limit(bySteadyFitness(5))
				.limit(100)
				.peek(tracker)
				.peek(stats)
//				.peek(r->System.out.println(r.getGeneration()))
				.collect(toBestPhenotype());

		System.out.println(stats);
		System.out.println(best);

		Genotype<IntegerGene> genotype = best.getGenotype();

		// *********************************************************************
		FeatureType<Integer> featureType = IntegerFeature.INSTANCE;

		SequenceFactory<Integer> factory = generator.toFactory(featureType, genotype);
		AlignmentAlgorithm<Integer> algorithm
				= toAlgorithm(featureType, factory, genotype);

		//noinspection DynamicRegexReplaceableByCompiledPattern
		ColumnTable<Sequence<Integer>> testWords
				= toPhoneticTable(table, factory, s -> s.replaceAll("⬚", ""));

		List<Alignment<Integer>> trainData = toTrainingData(factory, table);
		List<Alignment<Integer>> testData = doAlignment(algorithm, testWords);

		String pathname = trainingPath + dataSetFolder + "alignment";
		Path outputPath = new File(pathname).toPath();

		StringBuilder outputBuffer = new StringBuilder();
		for (int i = 0; i < testData.size(); i++) {
			Alignment<Integer> trainDatum = trainData.get(i);
			Alignment<Integer> testDatum = testData.get(i);
			String str = trainDatum.toString() + '\t' +testDatum.toString();
			outputBuffer.append(str).append(testDatum.getScore()).append('\n');
		}
		handler.writeString(outputPath.toString(), outputBuffer);
		writer.close();
	}

	private Double fitness(Genotype<IntegerGene> genotype) {
		FeatureType<Integer> featureType = IntegerFeature.INSTANCE;

		SequenceFactory<Integer> factory = toFactory(featureType, genotype);
		AlignmentAlgorithm<Integer> algorithm = toAlgorithm(featureType, factory, genotype);

		//noinspection DynamicRegexReplaceableByCompiledPattern
		ColumnTable<Sequence<Integer>> testWords = toPhoneticTable(table, factory,
				s -> s.replaceAll("⬚", ""));
		List<Alignment<Integer>> trainData = toTrainingData(factory, table);
		List<Alignment<Integer>> testsData = doAlignment(algorithm, testWords);
		int correct = 0;
		for (int i = 0; i < testWords.rows(); i++) {
			Alignment<Integer> aT = trainData.get(i);
			Alignment<Integer> aO = testsData.get(i);
			if (aO.equals(aT)) { correct++; }
		}
		return  correct / (double) testWords.rows();
	}

	@NotNull
	private static <T> List<Alignment<T>> doAlignment(
			AlignmentAlgorithm<T> algorithm,
			Table<Sequence<T>> testWords) {
		List<Alignment<T>> testsData = new ArrayList<>(testWords.size());
		for (int i = 0; i < testWords.rows(); i++) {
			List<Sequence<T>> row = testWords.getRow(i);
			Alignment<T> alignment = algorithm.getAlignment(row);
			testsData.add(alignment);
		}
		return testsData;
	}

	private static <T> List<Alignment<T>> toTrainingData(
			SequenceFactory<T> factory, ColumnTable<String> table) {
		ColumnTable<Sequence<T>> raw = toPhoneticTable(table, factory, Function.identity());
		return toAlignments(raw, factory);
	}

	@NotNull
	private <T> SequenceFactory<T> toFactory(FeatureType<T> featureType,
			Genotype<IntegerGene> genotype) {
		int n = genotype.getChromosome(0).getGene(0).getAllele();
		List<T> data = toFeatureBits(featureType, genotype);
		return toFactory(featureType, data, n);
	}

	@NotNull
	private static <T> AlignmentAlgorithm<T> toAlgorithm(
			FeatureType<T> featureType, SequenceFactory<T> factory,
			Genotype<IntegerGene> genotype) {
		SimpleComparator<T> comparator = new SimpleComparator<>(featureType);
		Integer allele = genotype.getChromosome(2).getGene(0).getAllele();
		GapPenalty<T> penalty = new ConstantGapPenalty<>(factory.getSequence("⬚"), allele);
		return new NeedlemanWunschAlgorithm<>(comparator, penalty, factory);
	}

	@NotNull
	private <T> SequenceFactory<T> toFactory(FeatureType<T> featureType,
			List<T> data, int n) {
		int rows = data.size() / n;
		Table<T> featureTable = new RectangularTable<>(rows, n, data);
		FeatureModel<T> model = new GeneralFeatureModel<>(
				featureType,
				getSpecification(n),
				Collections.emptyList(),
				Collections.emptyMap());
		FeatureMapping<T> mapping = toMapping(featureTable, model);
		return new SequenceFactory<>(mapping, FormatterMode.INTELLIGENT);
	}

	@NotNull
	private <T> FeatureMapping<T> toMapping(Table<T> featureTable,
			FeatureModel<T> model) {
		Map<String, FeatureArray<T>> sMap = parseSymbols(featureTable, model);
		Map<String, FeatureArray<T>> mMap = parseModifiers(featureTable, model);
		return new GeneralFeatureMapping<>(model, sMap, mMap);
	}

	private static <T> List<T> toFeatureBits(FeatureType<T> featureType,
			Genotype<IntegerGene> genotype) {
		Chromosome<IntegerGene> features = genotype.getChromosome(1);
		return features.stream()
				// TODO: fix this
				.map(integerGene -> featureType.parseValue(String.valueOf(integerGene.getAllele())) )
				.collect(Collectors.toList());
	}

	@NotNull
	private <T> Map<String, FeatureArray<T>> parseSymbols(
			Table<T> table, FeatureModel<T> model) {
		Map<String, FeatureArray<T>> sMap = new HashMap<>(symbols.size());
		for (int i = 0; i < symbols.size(); i++) {
			List<T> row = table.getRow(i).subList(0, model.getSpecification().size());
			sMap.put(symbols.get(i), new StandardFeatureArray<>(row, model));
		}
		return sMap;
	}

	@NotNull
	private <T> Map<String, FeatureArray<T>> parseModifiers(Table<T> table,
			FeatureModel<T> model) {
		Map<String, FeatureArray<T>> mMap = new HashMap<>(modifiers.size());
		for (int i = symbols.size(); i < modifiers.size(); i++) {
			List<T> row = table.getRow(i).subList(0, model.getSpecification().size());
			mMap.put(modifiers.get(i), new StandardFeatureArray<>(row, model));
		}
		return mMap;
	}

	@NotNull
	private static FeatureSpecification getSpecification(int n) {
		List<String> names = new ArrayList<>(n);
		Map<String, Integer> indices = new HashMap<>(n);
		IntStream.range(0, n).forEach(i -> {
			names.add(Integer.toHexString(i));
			indices.put(Integer.toHexString(i), i);
		});
		return new DefaultFeatureSpecification(names, indices);
	}
}

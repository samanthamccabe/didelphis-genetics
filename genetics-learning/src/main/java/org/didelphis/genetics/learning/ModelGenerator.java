package org.didelphis.genetics.learning;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.common.StringTransformer;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.comparators.LinearWeightComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.genetics.alignment.operators.gap.NullGapPenalty;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureArray;
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
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionStatistics;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
	}

	public static void main(String[] args) throws IOException {

		String dataPath = "/home/samantha/git/data/";
		String symbolsPath = dataPath+"ASJP_Symbols";

		FileHandler handler = new DiskFileHandler("UTF-8");

		String trainingPath = dataPath + "training/";
		String dataSetName = "out.sample_1k.utx";

		String dataSetFolder = dataSetName.replaceAll("\\.[^.]+$", "/");

		Path logPath = new File(trainingPath + dataSetFolder + "fitness.log").toPath();
//		Files.createDirectories(logPath.);

		BufferedWriter writer = Files.newBufferedWriter(logPath, CREATE);

		DecimalFormat formatter = new DecimalFormat("#.00");
		StatsTracker<IntegerGene> tracker = new StatsTracker<>(5, writer, formatter);

		StringTransformer transformer = new StringTransformer("Ø >> ⬚");

		String clean = transformer.apply(handler.read(symbolsPath).toString());
		String[] split = clean.split("\n");
		List<String> symbols   = Arrays.asList(SPACE.split(split[0]));
		List<String> modifiers = Arrays.asList(SPACE.split(split[1]));
		ColumnTable<String> table = loadTable(trainingPath + dataSetName, transformer);
		ModelGenerator generator = new ModelGenerator(symbols, modifiers, table);

		int numberOfSymbols  = symbols.size() + modifiers.size();
		int maximumFeatures  = 20;
		int numberOfFeatures = 5;

		Engine<IntegerGene, Double> engine = Engine.builder(
				generator::fitness,
				IntegerChromosome.of(numberOfFeatures, maximumFeatures),
				IntegerChromosome.of(1, 10, maximumFeatures),
				IntegerChromosome.of(1, 10, maximumFeatures),
				IntegerChromosome.of(0, 1, numberOfSymbols*maximumFeatures)
		)
				.populationSize(200)
				.selector(new RouletteWheelSelector<>())
				.alterers(
						new Mutator<>(0.55),
						new SinglePointCrossover<>(0.06)
				)
				.build();

		EvolutionStatistics<Double, ?> stats = EvolutionStatistics.ofNumber();

		Phenotype<IntegerGene, Double> best = engine.stream()
				.limit(bySteadyFitness(10))
				.limit(100)
				.peek(tracker)
				.peek(stats).collect(toBestPhenotype());

		System.out.println(stats);
		System.out.println(best);

		writer.close();
	}

	private Double fitness(Genotype<IntegerGene> genotype) {

		int n = genotype.getChromosome(0).getGene(0).getAllele();
		Chromosome<IntegerGene> numerator = genotype.getChromosome(1);
		Chromosome<IntegerGene> denominator = genotype.getChromosome(2);
		Chromosome<IntegerGene> features = genotype.getChromosome(3);

		List<Integer> data = features.stream()
				.map(IntegerGene::getAllele)
				.collect(Collectors.toList());

		List<Double> weights = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			double x = numerator.getGene(i).doubleValue();
			double y = denominator.getGene(i).doubleValue();
			weights.add(x / y);
		}

		// *********************************************************************
		int rows = data.size() / n;

		Table<Integer> featureTable = new RectangularTable<>(rows, n, data);

		IntegerFeature featureType = IntegerFeature.INSTANCE;
		FeatureModel<Integer> model = new GeneralFeatureModel<>(
				featureType,
				getSpecification(n),
				Collections.emptyList(),
				Collections.emptyMap());

		Map<String, FeatureArray<Integer>> sMap = parseSymbols(featureTable, model);
		Map<String, FeatureArray<Integer>> mMap = parseModifiers(featureTable, model);

		FeatureMapping<Integer> mapping = new GeneralFeatureMapping<>(
				model, sMap, mMap);

		SequenceFactory<Integer> factory = new SequenceFactory<>(
				mapping, FormatterMode.INTELLIGENT);

		Comparator<Integer, Double> comparator = new LinearWeightComparator<>(featureType, weights);
		GapPenalty<Integer> penalty = new NullGapPenalty<>(factory.getSequence("⬚"));

		// *********************************************************************
		AlignmentAlgorithm<Integer> algorithm = new NeedlemanWunschAlgorithm<>(
				comparator, penalty, factory);

		//noinspection DynamicRegexReplaceableByCompiledPattern
		ColumnTable<Sequence<Integer>> testWords = toPhoneticTable(
				table, factory, s -> s.replaceAll("⬚", ""));

		ColumnTable<Sequence<Integer>> rawAlignments = toPhoneticTable(table,
				factory, Function.identity());

		List<Alignment<Integer>> trainingAlignments = toAlignments(
				rawAlignments, model);

		int correct = 0;
		for (int i = 0; i < testWords.rows(); i++) {
			List<Sequence<Integer>> row = testWords.getRow(i);
			Alignment<Integer> alignment = algorithm.getAlignment(row);
			if (alignment.equals(trainingAlignments.get(i))) {
				correct++;
			}
		}
		return  correct / (double) testWords.rows();
	}

	@NotNull
	private Map<String, FeatureArray<Integer>> parseSymbols(
			Table<Integer> table, FeatureModel<Integer> model) {
		Map<String, FeatureArray<Integer>> sMap = new HashMap<>(symbols.size());
		for (int i = 0; i < symbols.size(); i++) {
			List<Integer> row = table.getRow(i).subList(0, model.getSpecification().size());
			sMap.put(symbols.get(i), new StandardFeatureArray<>(row, model));
		}
		return sMap;
	}

	@NotNull
	private Map<String, FeatureArray<Integer>> parseModifiers(Table<Integer> table,
			FeatureModel<Integer> model) {
		Map<String, FeatureArray<Integer>> mMap = new HashMap<>(modifiers.size());
		for (int i = symbols.size(); i < modifiers.size(); i++) {
			List<Integer> row = table.getRow(i).subList(0, model.getSpecification().size());
			mMap.put(modifiers.get(i), new StandardFeatureArray<>(row, model));
		}
		return mMap;
	}

	@NotNull
	private FeatureSpecification getSpecification(int n) {
		List<String> names = new ArrayList<>(n);
		Map<String, Integer> indices = new HashMap<>(n);
		IntStream.range(0, n).forEach(i -> {
			names.add(Integer.toHexString(i));
			indices.put(Integer.toHexString(i), i);
		});

		return new DefaultFeatureSpecification(
				names, indices);
	}

}

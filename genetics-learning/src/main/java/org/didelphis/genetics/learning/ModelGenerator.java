package org.didelphis.genetics.learning;

import org.didelphis.genetic.data.generation.BrownAlignmentGenerator;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.algorithm.Optimization;
import org.didelphis.genetics.alignment.common.StringTransformer;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.operators.Comparator;
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
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tables.Table;
import org.jenetics.Chromosome;
import org.jenetics.DoubleChromosome;
import org.jenetics.DoubleGene;
import org.jenetics.Gene;
import org.jenetics.Genotype;
import org.jenetics.Phenotype;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import org.jenetics.engine.EvolutionStatistics;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

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

import static java.nio.file.StandardOpenOption.CREATE;
import static org.didelphis.genetics.alignment.common.Utilities.loadTable;
import static org.didelphis.genetics.alignment.common.Utilities.toAlignments;
import static org.didelphis.genetics.alignment.common.Utilities.toPhoneticTable;
import static org.didelphis.genetics.alignment.common.Utilities.toTable;
import static org.jenetics.engine.EvolutionResult.toBestPhenotype;
import static org.jenetics.engine.limit.bySteadyFitness;

/**
 * Class {@code ModelGenerator}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-06-28
 */
@SuppressWarnings({"unused",
		"UseOfSystemOutOrSystemErr",
		"TooBroadScope",
		"FieldCanBeLocal"})
public final class ModelGenerator<T> {
	private static final Pattern SPACE = Pattern.compile("\\s+");
	private static final String DATE_FORMAT = "yyyy-MM-dd/HH-mm-ss";
	private static final Pattern EXTENSION = Pattern.compile("\\.[^.]+$");

	private static final double CUTOFF = 1.0;
	private static final Pattern COMPILE = Pattern.compile("⬚");
	private static final UnaryOperator<String> DELETE_GAP
			= s -> COMPILE.matcher(s).replaceAll("");
	private static final int ITERATIONS = 200;
	private static final String MATRIX_PATH = "brown.utx";
	private static final Function<String, String> TRANSFORMER
			= new StringTransformer("Ø >> ⬚\n^[^#] >> #$0");

	private final FileHandler handler;
	private final int features;
	private final List<String> symbols;
	private final List<String> modifiers;
	private final BrownAlignmentGenerator alignmentGenerator;
	private final FeatureType<T> featureType;

	private ModelGenerator(FeatureType<T> featureType, FileHandler handler,
			BrownAlignmentGenerator alignmentGenerator, int features,
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

		this.alignmentGenerator = alignmentGenerator;
	}

	public static void main(String[] args) throws IOException {

		String dataPath = "/home/samantha/git/data/";
		String symbolsPath = dataPath + "ASJP_Symbols";

		FileHandler handler = new DiskFileHandler("UTF-8");

		String trainingPath = dataPath + "training/";
		String dataSetName = "out.sample_1k.txt";

		String timeStamp = new DateTime().toString(DATE_FORMAT);

		String dataSetFolder = EXTENSION.matcher(dataSetName).replaceAll("/");

		Path logPath = new File(trainingPath + dataSetFolder + timeStamp +
				"-fitness.log").toPath();

		Files.createDirectories(logPath.getParent());
		BufferedWriter logWriter = Files.newBufferedWriter(logPath, CREATE);

		DecimalFormat formatter = new DecimalFormat("#.0000");
		Consumer<EvolutionResult<?, Double>> tracker = new StatsTracker<>(1,
				logWriter, formatter
		);

		String clean = TRANSFORMER.apply(handler.read(symbolsPath).toString());
		String[] split = clean.split("\n");
		List<String> symbols = Arrays.asList(SPACE.split(split[0]));
		List<String> modifiers = Arrays.asList(SPACE.split(split[1]));
		ColumnTable<String> table = loadTable(trainingPath + dataSetName,
				TRANSFORMER
		);
		int features = 15;

		FeatureType<Double> featureType = DoubleFeature.INSTANCE;

		String correspondenceDataPath
				= "/home/samantha/Downloads/data/brown_correspondences.csv";

		BrownAlignmentGenerator brownAlignmentGenerator
				= new BrownAlignmentGenerator(correspondenceDataPath);
		ModelGenerator<Double> generator = new ModelGenerator<>(featureType,
				handler, brownAlignmentGenerator, features, symbols, modifiers
		);

		int numberOfSymbols = symbols.size() + modifiers.size();

		Engine<DoubleGene, Double> engine = Engine.builder(generator::fitness,
				DoubleChromosome.of(-50, 100)
		).build();
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
		String outputPath = trainingPath + dataSetFolder + timeStamp;
		generator.writeBestGenome(outputPath, table, featureType, genotype);
	}

	private <G extends Gene<T, G>> void writeBestGenome(String outputPath,
			ColumnTable<String> table, FeatureType<T> featureType,
			Genotype<G> genotype
	) {

		SequenceFactory<T> factory = toFactory(featureType, genotype);
		AlignmentAlgorithm<T> algorithm = toAlgorithm(featureType, factory,
				genotype
		);

		//noinspection DynamicRegexReplaceableByCompiledPattern
		ColumnTable<Sequence<T>> testWords = toPhoneticTable(table, factory,
				DELETE_GAP
		);

		List<Alignment<T>> testData = doAlignment(algorithm, testWords);

		writeAlignments(handler, testData, outputPath);
		writeMapping(handler, factory, outputPath);
	}

	private BrownAlignmentGenerator getAlignmentGenerator() {
		return alignmentGenerator;
	}

	private <G extends Gene<T, G>> Double fitness(Genotype<G> genotype) {
		SequenceFactory<T> factory = toFactory(featureType, genotype);
		AlignmentAlgorithm<T> algorithm = toAlgorithm(featureType, factory,
				genotype
		);

		BrownAlignmentGenerator alignmentGenerator = getAlignmentGenerator();

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

	@NotNull
	private <G extends Gene<T, G>> SequenceFactory<T> toFactory(
			FeatureType<T> featureType, Genotype<G> genotype
	) {
		/*
		List<T> data = toFeatureBits(featureType, genotype);
		int rows = data.size() / features;
		Table<T> featureTable = new RectangularTable<>(rows, features, data);
		FeatureModel<T> model = new GeneralFeatureModel<>(
				featureType,
				getSpecification(features),
				Collections.emptyList(),
				Collections.emptyMap());
		FeatureMapping<T> mapping = toMapping(featureTable, model);
		return new SequenceFactory<>(mapping, FormatterMode.INTELLIGENT);
		*/

		GeneralFeatureModel<T> model = new GeneralFeatureModel<>(featureType,
				DefaultFeatureSpecification.EMPTY, Collections.emptyList(),
				Collections.emptyMap()
		);

		GeneralFeatureMapping<T> mapping = new GeneralFeatureMapping<>(model,
				Collections.emptyMap(), Collections.emptyMap()
		);

		return new SequenceFactory<>(mapping, FormatterMode.INTELLIGENT);
	}

	@NotNull
	private FeatureMapping<T> toMapping(Table<T> featureTable,
			FeatureModel<T> model
	) {
		Map<String, FeatureArray<T>> sMap = parseSymbols(featureTable, model);
		Map<String, FeatureArray<T>> mMap = parseModifiers(featureTable, model);
		return new GeneralFeatureMapping<>(model, sMap, mMap);
	}

	@NotNull
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

	@NotNull
	private <G extends Gene<T, G>> Map<String, FeatureArray<T>> parseModifiers(
			Table<T> table, FeatureModel<T> model
	) {
		Map<String, FeatureArray<T>> mMap = new HashMap<>(modifiers.size());
		int size = table.rows();
		for (int i = symbols.size();
		     i < symbols.size() + modifiers.size();
		     i++) {
			List<T> row = table.getRow(i)
					.subList(0, model.getSpecification().size());
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
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("\nSYMBOLS\n");
		for (Entry<String, FeatureArray<T>> entry : featureMap.entrySet()) {
			String key = entry.getKey();
			stringBuilder.append(key);
			for (T t : entry.getValue()) {
				stringBuilder.append('\t').append(t);
			}
			stringBuilder.append('\n');
		}
		stringBuilder.append("\nMODIFIERS\n");
		for (Entry<String, FeatureArray<T>> entry : modifierMap.entrySet()) {
			String key = entry.getKey();
			stringBuilder.append(key);
			for (T t : entry.getValue()) {
				stringBuilder.append('\t').append(t);
			}
			stringBuilder.append('\n');
		}
		handler.writeString(pathname + ".mapping", stringBuilder);
	}

	private static <T> void writeAlignments(FileHandler handler,
			Iterable<Alignment<T>> testData, String pathName
	) {
		Path outputPath = new File(pathName + "-alignment").toPath();
		StringBuilder outputBuffer = new StringBuilder();
		for (Alignment<T> testDatum : testData) {
			String str = testDatum.toString();
			outputBuffer.append(str).append('\n');
		}
		handler.writeString(outputPath.toString(), outputBuffer);
	}

	@NotNull
	private static <T> List<Alignment<T>> doAlignment(
			AlignmentAlgorithm<T> algorithm, Table<Sequence<T>> testWords
	) {
		List<Alignment<T>> testsData = new ArrayList<>(testWords.size());
		for (int i = 0; i < testWords.rows(); i++) {
			List<Sequence<T>> row = testWords.getRow(i);
			AlignmentResult<T> alignmentResult = algorithm.getAlignment(row);
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

	@NotNull
	private static <T, G extends Gene<T, G>> AlignmentAlgorithm<T> toAlgorithm(
			FeatureType<T> featureType, SequenceFactory<T> factory,
			Genotype<G> genotype
	) {
		//		SimpleComparator<T> comparator = new SimpleComparator<>(featureType,
		//				i->1.0/Math.sqrt(i+2));
		Comparator<T> comparator = Utilities.getMatrixComparator(
				new DiskFileHandler("UTF-8"), factory, Function.identity(),
				MATRIX_PATH
		);

		Chromosome<G> chromosome = genotype.getChromosome(0);
		double gap1 = featureType.doubleValue(
				chromosome.getGene(0).getAllele());
		//		Integer gap2 = chromosome.getGene(1).getAllele();
		GapPenalty<T> penalty = new ConstantGapPenalty<>(
				factory.getSequence("⬚"), gap1);
		//		GapPenalty<T> penalty = new ConvexGapPenalty<>(factory.getSequence("⬚"), gap1, gap2);
		return new NeedlemanWunschAlgorithm<>(comparator, Optimization.MIN,
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

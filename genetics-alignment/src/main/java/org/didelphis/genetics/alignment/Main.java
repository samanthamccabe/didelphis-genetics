package org.didelphis.genetics.alignment;

import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.algorithm.Optimization;
import org.didelphis.genetics.alignment.common.StringTransformer;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.correspondences.Context;
import org.didelphis.genetics.alignment.correspondences.ContextPair;
import org.didelphis.genetics.alignment.correspondences.PairCorrespondenceSet;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.comparators.LinearWeightComparator;
import org.didelphis.genetics.alignment.operators.comparators.BrownEtAlComparator;
import org.didelphis.genetics.alignment.operators.gap.ConvexGapPenalty;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.GeneralMultiMap;
import org.didelphis.structures.maps.SymmetricalTwoKeyMap;
import org.didelphis.structures.maps.interfaces.MultiMap;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.utilities.Split;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.*;
import static org.slf4j.LoggerFactory.getLogger;

public final class Main {
	private static final transient Logger LOGGER = getLogger(Main.class);
	private static final Pattern EXTENSION_PATTERN = compile("\\.[^.]*?$");
	private static final Pattern HYPHEN = compile("-");
	private static final Pattern WHITESPACE = compile("(\n|\r\n?|\\s)+");
	private static final Pattern HASH = compile("#", LITERAL);

	private Main() {
	}

	/**
	 * TODO: Rehab plan:
	 *    args:
	 *      --model
	 *      --weights
	 *      --transformer
	 *      --input file path
	 *      --fields fields from --input to read
	 *      --operations
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		FileHandler handler = new DiskFileHandler("UTF-8");

//		CharSequence payload = handler.read("transformations");
		CharSequence payload = "Ø >> ";
		StringTransformer transformer = new StringTransformer(payload);

		FeatureType<Integer> type = IntegerFeature.INSTANCE;

		String path = "AT_hybrid_reduced.model";
//		FeatureModelLoader<Integer> loader = new FeatureModelLoader<>(
//				type, handler, path);
		FeatureModelLoader<Integer> loader = IntegerFeature.emptyLoader();
		FeatureMapping<Integer> mapping = loader.getFeatureMapping();

		SequenceFactory<Integer> factory = new SequenceFactory<>(
				mapping, FormatterMode.INTELLIGENT);

		Sequence<Integer> gap = factory.getSequence("░");
		GapPenalty<Integer> gapPenalty = new ConvexGapPenalty<>(gap, 0, 0);

//		String weightsPath = "weights_14";
//		Comparator<Integer> comparator = readWeightsComparator(type, handler, weightsPath);

		String matrixPath = "brown.utx";
		Comparator<Integer> comparator = getMatrixComparator(handler, factory,
				transformer, matrixPath);

		AlignmentAlgorithm<Integer> algorithm = new NeedlemanWunschAlgorithm<>(
				comparator, Optimization.MIN, gapPenalty, factory);

		Map<File, List<String>> files = new HashMap<>();

		files.put(new File("out.sample_1k.txt"), asList("A", "B"));

		Function<String,String> bFunc = new StringTransformer("^[^#] >> #$0");
		for (Entry<File, List<String>> languageEntry : files.entrySet()) {
			File tableFile = languageEntry.getKey();
			List<String> keyList = languageEntry.getValue();
			ColumnTable<String> table = Utilities.loadTable(tableFile.getPath(),
					bFunc);
			ColumnTable<Sequence<Integer>> data = Utilities.toPhoneticTable(
					table, factory, transformer, keyList);
			MultiMap<String, AlignmentResult<Integer>> alignmentMap =
					align(algorithm, keyList, data);

			String rootPath = EXTENSION_PATTERN
					.matcher(tableFile.getCanonicalPath())
					.replaceAll("/");
			writeAlignments(rootPath, alignmentMap);
		}
	}

	@NotNull
	private static Comparator<Integer> getMatrixComparator(FileHandler handler,
			SequenceFactory<Integer> factory,
			StringTransformer transformer,
			String matrixPath) {
		SymmetricalTwoKeyMap<Segment<Integer>, Double> map = new SymmetricalTwoKeyMap<>();
		for (String line : Split.splitLines(handler.read(matrixPath))) {
			String[] matcher = line.split("\t");
			String s1 = matcher[0];
			String s2 = matcher[1];
			map.put(factory.getSegment(s1 == null ? "" : transformer.apply(s1)),
					factory.getSegment(s2 == null ? "" : transformer.apply(s2)),
					Double.parseDouble(matcher[2]));
		}
		return new BrownEtAlComparator<>(map);
	}

	@NotNull
	private static Comparator<Integer> readWeightsComparator(
			FeatureType<Integer> type, FileHandler handler, String path) {
		CharSequence weightsPayload = handler.read(path);
		List<Double> weights = new ArrayList<>();
		for (String string : WHITESPACE.split(weightsPayload)) {
			weights.add(Double.parseDouble(string));
		}
		return new LinearWeightComparator<>(
				type, weights);
	}

	private static void writeContexts(Map<String, PairCorrespondenceSet<Segment<Integer>>> contexts, String rootPath) throws IOException {
		for (Entry<String, PairCorrespondenceSet<Segment<Integer>>> entry : contexts
				.entrySet()) {
			String key = entry.getKey();

			File file = new File(rootPath + "contexts_" + key + ".tab");

			Writer writer = new BufferedWriter(new FileWriter(file));
			writer.write("L_a\tLeft\tL_p\tR_a\tRight\tR_p\n");

			entry.getValue().iterator().forEachRemaining(triple -> {
				Segment<Integer> left = triple.getFirstElement();
				Segment<Integer> right = triple.getSecondElement();
				triple.getThirdElement().forEach(pair -> {
					Context<Segment<Integer>> lContext = pair.getLeft();
					Context<Segment<Integer>> rContext = pair.getRight();

					Segment<Integer> lA = lContext.getAnte();
					Segment<Integer> lP = lContext.getPost();
					Segment<Integer> rA = rContext.getAnte();
					Segment<Integer> rP = rContext.getPost();

					try {
						writer.write(lA.toString());
						writer.write("\t");
						writer.write(left.toString());
						writer.write("\t");
						writer.write(lP.toString());
						writer.write("\t");
						writer.write(rA.toString());
						writer.write("\t");
						writer.write(right.toString());
						writer.write("\t");
						writer.write(rP.toString());
						writer.write("\n");
					} catch (IOException e) {
						LOGGER.error("Failed to write output", e);
					}
				});
			});
			writer.close();
		}
	}

	private static <T> void writeAlignments(String rootPath,
			Iterable<Tuple<String, Collection<AlignmentResult<T>>>> alignments
	) {
		for (Tuple<String, Collection<AlignmentResult<T>>> entry : alignments) {
			String key = entry.getLeft();
			StringBuilder sb = new StringBuilder();
			sb.append(HYPHEN.matcher(key).replaceAll("\t"));
			sb.append('\n');
			for (AlignmentResult<T> result : entry.getRight()) {
				Iterator<Alignment<T>> list = result.getAlignments().iterator();
				Iterable<CharSequence> charSequences = list.hasNext()
						? list.next().buildPrettyAlignments()
						: Collections.emptyList();
				for (CharSequence sequence : charSequences) {
					String normal = Normalizer.normalize(sequence, Form.NFC);
					String str = HASH.matcher(normal)
							.replaceAll(Matcher.quoteReplacement("")).trim();
					sb.append(str);
					sb.append('\t');
				}
				sb.append('\n');
			}

			File file = new File(rootPath + "alignments_" + entry.getLeft() + ".csv");
			Path path = file.toPath();
			try {
				Files.createDirectories(path.getParent());
			} catch (IOException e) {
				e.printStackTrace();
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				writer.write(sb.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static Map<String, PairCorrespondenceSet<Segment<Integer>>> buildContexts(
			SequenceFactory<Integer> factory, Sequence<Integer> gap, MultiMap<String, Alignment<Integer>> alignmentMap) {
		Map<String, PairCorrespondenceSet<Segment<Integer>>> contexts =
				new HashMap<>();
		for (Tuple<String, Collection<Alignment<Integer>>> e : alignmentMap) {

			String key = e.getLeft();

			PairCorrespondenceSet<Segment<Integer>> set =
					new PairCorrespondenceSet<>();
			for (Alignment<Integer> alignment : e.getRight()) {
				if (alignment.columns() > 0) {
					List<Segment<Integer>> left = alignment.getRow(0);
					List<Segment<Integer>> right = alignment.getRow(1);

					left.add(factory.getBorderSegment());
					right.add(factory.getBorderSegment());

					for (int i = 1;
					     i < alignment.columns() - 1; i++) {
						Segment<Integer> l = left.get(i);
						Segment<Integer> r = right.get(i);

						if (!l.equals(r)) {
							Segment<Integer> lA = lookBack(left, i, gap);
							Segment<Integer> rA = lookBack(right, i, gap);

							Segment<Integer> lP =
									lookForward(left, i, gap);
							Segment<Integer> rP =
									lookForward(right, i, gap);

							ContextPair<Segment<Integer>> pair =
									new ContextPair<>(
											new Context<>(lA, lP),
											new Context<>(rA, rP));

							set.add(l, r, pair);
						}
					}
				}
			}
			contexts.put(key, set);
		}
		return contexts;
	}

	private static <T> MultiMap<String, AlignmentResult<T>> align(
			AlignmentAlgorithm<T> algorithm,
			List<String> keyList,
			ColumnTable<Sequence<T>> data
	) {
		MultiMap<String, AlignmentResult<T>> alignmentMap =
				new GeneralMultiMap<>();
		for (int i = 0; i < keyList.size(); i++) {
			String k1 = keyList.get(i);
			List<Sequence<T>> d1 = data.getColumn(k1);
			for (int j = 0; j < i; j++) {
				String k2 = keyList.get(j);
				List<Sequence<T>> d2 = data.getColumn(k2);

				if (d1 == null || d2 == null || d1.size() != d2.size()) {
					return null;
				}

				Collection<AlignmentResult<T>> alignments = new ArrayList<>();
				Iterator<Sequence<T>> it1 = d1.iterator();
				Iterator<Sequence<T>> it2 = d2.iterator();
				while (it1.hasNext() && it2.hasNext()) {
					Sequence<T> e1 = it1.next();
					Sequence<T> e2 = it2.next();
					List<Sequence<T>> list = asList(e1, e2);
					AlignmentResult<T> result = algorithm.getAlignment(list);
					alignments.add(result);
				}
				alignmentMap.addAll(k1 + '-' + k2, alignments);
			}
		}
		return alignmentMap;
	}

	private static Segment<Integer> lookBack(List<Segment<Integer>> left, int i,
	                                        Sequence<Integer> gap) {
		Segment<Integer> a = left.get(i - 1);
		for (int j = 2; a.equals(gap) && (i - j) >= 0; j++) {
			a = left.get(i - j);
		}
		return a;
	}

	private static Segment<Integer> lookForward(List<Segment<Integer>> left,
	                                           int i, Sequence<Integer> gap) {
		Segment<Integer> a = left.get(i + 1);
		for (int j = 2; a.equals(gap) && (i + j) < left.size(); j++) {
			a = left.get(i + j);
		}
		return a;
	}
}

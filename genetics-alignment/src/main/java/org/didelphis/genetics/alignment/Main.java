package org.didelphis.genetics.alignment;

import org.didelphis.io.ClassPathFileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.GeneralMultiMap;
import org.didelphis.structures.maps.interfaces.MultiMap;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.SingleAlignmentAlgorithm;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.correspondences.Context;
import org.didelphis.genetics.alignment.correspondences.ContextPair;
import org.didelphis.genetics.alignment.correspondences.PairCorrespondenceSet;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.comparators.LinearWeightComparator;
import org.didelphis.genetics.alignment.operators.comparators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.ConvexGapPenalty;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public final class Main {
	private static final transient Logger LOGGER = getLogger(Main.class);
	private static final Pattern EXTENSION_PATTERN = Pattern.compile("\\..*?$");
	private static final Pattern HYPHEN = Pattern.compile("-");

	private Main() {
	}

	public static void main(String[] args) throws IOException {
		FormatterMode mode = FormatterMode.INTELLIGENT;

		String path = "AT_hybrid_reduced.model";
		//		String path = "AT_hybrid.model";

		FeatureType<Integer> type = IntegerFeature.INSTANCE;
		FeatureModelLoader<Integer> loader = new FeatureModelLoader<>(
				type, ClassPathFileHandler.INSTANCE, path);

		FeatureMapping<Integer> mapping = loader.getFeatureMapping();

		SequenceFactory<Integer> factory = new SequenceFactory<>(
				mapping, FormatterMode.INTELLIGENT);

		Sequence<Integer> gap = factory.getSequence("░");
		GapPenalty<Integer> gapPenalty = new ConvexGapPenalty<>(gap, 0, 10);

		@SuppressWarnings("MagicNumber")
		double[] array = {
				2.0, // obs
				3.0, // lat
				7.0, // nas
				10.0, // lab
				2.0, // rnd
				5.0, // cor
				8.0, // dor
				10.0, // frn
				3.0, // hgt
				0.5, // atr
				1.0, // glt
				1.0, // vot
				10.0, // dst
				1.0  // lng
		};
		List<Double> weights = getDoublesList(array);
		Comparator<Integer, Double> segComparator =
				new LinearWeightComparator<>(type,weights);

		Comparator<Integer, Double> sequenceComparator =
				new SequenceComparator<>(segComparator);

		AlignmentAlgorithm<Integer> algorithm = new SingleAlignmentAlgorithm<>(
				sequenceComparator, gapPenalty, 1, factory);

		Map<File, List<String>> files = new HashMap<>();

		files.put(new File("../data/nakh.tsv"),
				getList("CHE", "ING", "BCB"));
//			files.put(new File("../data/avar-andi.tsv"),
//					getList("AVA", "AVC", "AND", "AKV", "CHM"));

		for (Map.Entry<File, List<String>> languageEntry : files.entrySet()) {
			File tableFile = languageEntry.getKey();
			List<String> keyList = languageEntry.getValue();

			Collection<Expression> clex = new ArrayList<>();
			clex.add(new Expression("ṭ", "tʼ"));
			clex.add(new Expression("ḳ", "kʼ"));
			clex.add(new Expression("ʠ", "qʼ"));

			clex.add(new Expression("š", "ʃ"));
			clex.add(new Expression("I", "ˤ"));
			clex.add(new Expression("ċ", "tsʼ"));
			clex.add(new Expression("ḉ", "tʃʼ"));
			clex.add(new Expression("č", "tʃ"));
			clex.add(new Expression("ǯ", "dʒ"));
			clex.add(new Expression("c", "ts"));
			clex.add(new Expression("ʒ|ӡ", "dz"));

			clex.add(new Expression("ӓ", "æ"));
			clex.add(new Expression("ü", "y"));
			clex.add(new Expression(":", "ː"));

			clex.add(new Expression(
					"\\([^\\)]*\\)|\\[[^\\]]*\\]|\\{[^\\}]*\\}", ""));
			clex.add(new Expression("\\(|\\)|\\[|\\]|\\{|\\}", ""));
			clex.add(new Expression("\\d*\\s+.*", ""));
			clex.add(new Expression("[,/].*", ""));
			clex.add(new Expression("-|=|\u035C|\u0361|\\*", ""));

			ColumnTable<Sequence<Integer>> data = Utilities.getPhoneticData(
					tableFile, keyList, factory, clex);

			MultiMap<String, Alignment<Integer>> alignmentMap =
					align(algorithm, keyList, data);

			Map<String, PairCorrespondenceSet<Segment<Integer>>> contexts =
					buildContexts(factory, gap, alignmentMap);

			String rootPath = EXTENSION_PATTERN
					.matcher(tableFile.getCanonicalPath())
					.replaceAll("/");
			writeAlignments(alignmentMap, rootPath);
			writeContexts(contexts, rootPath);
		}
	}

	private static void writeContexts(Map<String, PairCorrespondenceSet<Segment<Integer>>> contexts, String rootPath) throws IOException {
		for (Map.Entry<String, PairCorrespondenceSet<Segment<Integer>>> entry : contexts
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

	private static void writeAlignments(
			MultiMap<String, Alignment<Integer>> alignmentMap,
			String rootPath
	) {
		for (Tuple<String, Collection<Alignment<Integer>>> e : alignmentMap) {
			String key = e.getLeft();
			StringBuilder sb = new StringBuilder();
			sb.append(HYPHEN.matcher(key).replaceAll("\t"));
			sb.append('\n');
			for (Alignment<Integer> lists : e.getRight()) {
				Iterable<CharSequence> charSequences =
						lists.buildPrettyAlignments();
				for (CharSequence sequence : charSequences) {
					String normal = Normalizer.normalize(sequence,
							Normalizer.Form.NFC);
					String str = normal.replace("#", "").trim();
					sb.append(str);
					sb.append('\t');
				}
				sb.append('\n');
			}

			// TODO:
//			File file = new File(rootPath + "alignments_" + e.getLeft() + ".csv");
			//					FileUtils.write(file, sb);
			// TODO:
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

	private static MultiMap<String, Alignment<Integer>> align(
			AlignmentAlgorithm<Integer> algorithm,
			List<String> keyList,
			ColumnTable<Sequence<Integer>> data
	) {
		MultiMap<String, Alignment<Integer>> alignmentMap =
				new GeneralMultiMap<>();
		for (int i = 0; i < keyList.size(); i++) {
			String k1 = keyList.get(i);
			List<Sequence<Integer>> d1 = data.getColumn(k1);
			for (int j = 0; j < i; j++) {
				String k2 = keyList.get(j);
				List<Sequence<Integer>> d2 = data.getColumn(k2);

				Map<String, List<Sequence<Integer>>> map =
						new HashMap<>();
				map.put(k1, d1);
				map.put(k2, d2);

//				TODO:
//				ColumnTable<Sequence<Integer>> subTable =
//						new DataTable<>(map);
//
//				List<Alignment<Integer>> alignments =
//						algorithm.align(subTable);
//				alignmentMap.addAll(k1 + '-' + k2, alignments);
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

	private static List<Double> getDoublesList(double... array) {
		return Arrays.stream(array).boxed().collect(Collectors.toList());
	}

	@Deprecated
	private static List<String> getList(String... array) {
		List<String> list = new ArrayList<>();
		Collections.addAll(list, array);
		return list;
	}
}

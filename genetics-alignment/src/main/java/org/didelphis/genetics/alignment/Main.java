package org.didelphis.genetics.alignment;

import org.didelphis.common.language.enums.FormatterMode;
import org.didelphis.common.language.phonetic.SequenceFactory;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.common.structures.tables.ColumnTable;
import org.didelphis.common.structures.tables.DataTable;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.single.SingleAlignmentAlgorithm;
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
		// =====================================================================
		// LOAD ALIGNMENT ENGINE
		// =====================================================================
		FormatterMode mode = FormatterMode.INTELLIGENT;

		String path = "AT_hybrid_reduced.model";
		//		String path = "AT_hybrid.model";

		SequenceFactory<Double> factory =
				Utilities.loadFactoryFromClassPath(path, mode);

		Segment<Double> gap = factory.getSegment("░");
		GapPenalty gapPenalty = new ConvexGapPenalty(gap, 0, 10);

		@SuppressWarnings("MagicNumber") double[] array = {
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
		Comparator<Segment<Double>> segComparator =
				new LinearWeightComparator(weights, 0);

		//		Comparator<Segment> segComparator = new NdArrayComparator
		// (array);

		Comparator<Sequence<Double>> sequenceComparator =
				new SequenceComparator(segComparator);

		//		MultipleAlignmentAlgorithm algorithm =
		//				new GlobalAlignmentAlgorithm(gapPenalty, 1, 
		// sequenceComparator);
		//		AlignmentEngine engine = new AlignmentEngine(factory, 
		// algorithm);

		AlignmentAlgorithm algorithm =
				new SingleAlignmentAlgorithm(sequenceComparator, gapPenalty, 1,
						factory);

		// =====================================================================
		// LOAD LEXICON
		// =====================================================================
		LOGGER.info("trial\ttime[µs]");
		Map<File, List<String>> files = new HashMap<>();
		int tests = 1;
		for (int z = 1; z <= tests; z++) {
			long startTime = System.nanoTime();

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

				ColumnTable<Sequence<Double>> data =
						Utilities.getPhoneticData(tableFile, keyList, factory,
								clex);

				// =====================================================================
				// RUN ALIGNMENT
				// =====================================================================
				Map<String, List<Alignment<Double>>> alignmentMap =
						new HashMap<>();
				for (int i = 0; i < keyList.size(); i++) {
					String k1 = keyList.get(i);
					List<Sequence<Double>> d1 = data.getColumn(k1);
					for (int j = 0; j < i; j++) {
						String k2 = keyList.get(j);
						List<Sequence<Double>> d2 = data.getColumn(k2);

						Map<String, List<Sequence<Double>>> map =
								new HashMap<>();
						map.put(k1, d1);
						map.put(k2, d2);

						ColumnTable<Sequence<Double>> subTable =
								new DataTable<>(map);

						List<Alignment<Double>> alignments =
								algorithm.align(subTable);
						alignmentMap.put(k1 + '-' + k2, alignments);
					}
				}

				// =====================================================================
				// ACCUMULATE CORRESPONDENCES
				// =====================================================================
				Map<String, PairCorrespondenceSet<Segment<Double>>> contexts =
						new HashMap<>();
				for (Map.Entry<String, List<Alignment<Double>>> e : alignmentMap
						.entrySet()) {

					String key = e.getKey();

					PairCorrespondenceSet<Segment<Double>> set =
							new PairCorrespondenceSet<>();
					for (Alignment<Double> alignment : e.getValue()) {
						if (alignment.getNumberColumns() > 0) {
							List<Sequence<Double>> left = alignment.getRow(0);
							List<Sequence<Double>> right = alignment.getRow(1);

							left.add(factory.getBorderSequence());
							right.add(factory.getBorderSequence());

							for (int i = 1;
							     i < alignment.getNumberColumns() - 1; i++) {
								Segment<Double> l = left.get(i).get(0);
								Segment<Double> r = right.get(i).get(0);

								if (!l.equals(r)) {
									Segment<Double> lA = lookBack(left, i, gap);
									Segment<Double> rA =
											lookBack(right, i, gap);

									Segment<Double> lP =
											lookForward(left, i, gap);
									Segment<Double> rP =
											lookForward(right, i, gap);

									ContextPair<Segment<Double>> pair =
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


				// =====================================================================
				// OUTPUT RESULTS
				// =====================================================================
				String rootPath =
						EXTENSION_PATTERN.matcher(tableFile.getCanonicalPath())
								.replaceAll("/");

				for (Map.Entry<String, List<Alignment<Double>>> e : alignmentMap
						.entrySet()) {
					String key = e.getKey();
					StringBuilder sb = new StringBuilder();
					sb.append(HYPHEN.matcher(key).replaceAll("\t"));
					sb.append('\n');
					for (Alignment<Double> lists : e.getValue()) {
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

					File file = new File(
							rootPath + "alignments_" + e.getKey() + ".csv");
					//					FileUtils.write(file, sb);
					// TODO:
				}

				// =====================================================================
				// OUTPUT CONTEXT
				// =====================================================================
				for (Map.Entry<String, PairCorrespondenceSet<Segment<Double>>> entry : contexts
						.entrySet()) {
					String key = entry.getKey();

					File file = new File(rootPath + "contexts_" + key + ".tab");

					Writer writer = new BufferedWriter(new FileWriter(file));
					writer.write("L_a\tLeft\tL_p\tR_a\tRight\tR_p\n");

					entry.getValue().iterator().forEachRemaining(triple -> {
						Segment<Double> left = triple.getFirstElement();
						Segment<Double> right = triple.getSecondElement();
						triple.getThirdElement().forEach(pair -> {
							Context<Segment<Double>> lContext = pair.getLeft();
							Context<Segment<Double>> rContext = pair.getRight();

							Segment<Double> lA = lContext.getAnte();
							Segment<Double> lP = lContext.getPost();
							Segment<Double> rA = rContext.getAnte();
							Segment<Double> rP = rContext.getPost();

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
			long endTime = System.nanoTime();

			LOGGER.info(z + "\t" + ((endTime - startTime) / 1000));
		}
	}

	private static Segment<Double> lookBack(List<Sequence<Double>> left, int i,
			Segment<Double> gap) {
		Segment<Double> a = left.get(i - 1).get(0);
		for (int j = 2; a.equals(gap) && (i - j) >= 0; j++) {
			a = left.get(i - j).get(0);
		}
		return a;
	}

	private static Segment<Double> lookForward(List<Sequence<Double>> left,
			int i, Segment<Double> gap) {
		Segment<Double> a = left.get(i + 1).get(0);
		for (int j = 2; a.equals(gap) && (i + j) < left.size(); j++) {
			a = left.get(i + j).get(0);
		}
		return a;
	}

	private static List<Double> getDoublesList(double... array) {
		return Arrays.stream(array).boxed().collect(Collectors.toList());
	}

	private static List<String> getList(String... array) {
		List<String> list = new ArrayList<>();
		Collections.addAll(list, array);
		return list;
	}
}

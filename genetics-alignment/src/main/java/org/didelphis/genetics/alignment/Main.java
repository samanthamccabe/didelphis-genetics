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

package org.didelphis.genetics.alignment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.optimization.BaseOptimization;
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.common.StringTransformer;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.correspondences.Context;
import org.didelphis.genetics.alignment.correspondences.ContextPair;
import org.didelphis.genetics.alignment.correspondences.PairCorrespondenceSet;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.comparators.LinearWeightComparator;
import org.didelphis.genetics.alignment.operators.comparators.MatrixComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.genetics.alignment.operators.gap.NullGapPenalty;
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
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.Suppliers;
import org.didelphis.structures.maps.GeneralMultiMap;
import org.didelphis.structures.maps.GeneralTwoKeyMap;
import org.didelphis.structures.maps.GeneralTwoKeyMultiMap;
import org.didelphis.structures.maps.interfaces.MultiMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMultiMap;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tables.SymmetricTable;
import org.didelphis.structures.tuples.Couple;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.utilities.Logger;
import org.didelphis.utilities.Splitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.LITERAL;
import static java.util.regex.Pattern.compile;

@UtilityClass
public final class Main {

	private static final Logger LOGGER = Logger.create(Main.class);
	private static final Pattern EXTENSION_PATTERN = compile("\\.[^.]*?$");
	private static final Pattern HYPHEN = compile("-");
	private static final Pattern WHITESPACE = compile("(\n|\r\n?|\\s)+");
	private static final Pattern HASH = compile("#", LITERAL);
	private static final Pattern ZERO = compile("0");

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final FileHandler HANDLER = new DiskFileHandler("UTF-8");

	static {
		Logger.addAppender(System.out);
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

		if (args.length == 0) {
			LOGGER.error("You must provide a JSON configuration");
			System.exit(-1);
		}

		String configPath = args[0];

		String basePath = configPath.contains("/") 
				? configPath.replaceAll("/[^/]+$", "/") 
				: "";

		String configData = String.valueOf(HANDLER.read(configPath));

		// Read Configuration
		JsonNode configNode = OBJECT_MAPPER.readTree(configData);
		String modelPath   = basePath + configNode.get("model_path").asText();
		String weightsPath = basePath + configNode.get("weights_path").asText();

		List<String> transformPayload = readConfigArray("transformations", configNode);
		String gapSymbol = readConfigString("gap_symbol", configNode);

		Map<File, List<List<String>>> files = loadData(basePath, configNode);

		Function<String, String> transformer = new StringTransformer(transformPayload);

		FeatureType<Integer> type = IntegerFeature.INSTANCE;

		FeatureModelLoader<Integer> loader = new FeatureModelLoader<>(
				type,
				HANDLER,
				modelPath
		);

		FeatureMapping<Integer> mapping = loader.getFeatureMapping();

		SequenceFactory<Integer> factory = new SequenceFactory<>(
				mapping, FormatterMode.INTELLIGENT);
		
		Sequence<Integer> gap = factory.toSequence(gapSymbol);
		GapPenalty<Integer> gapPenalty = new NullGapPenalty<>(gap);

		SequenceComparator<Integer> comparator = readWeightsComparator(
				type, weightsPath
		);
		
		AlignmentAlgorithm<Integer> algorithm = new NeedlemanWunschAlgorithm<>(
				BaseOptimization.MIN,
				comparator,
				gapPenalty,
				factory
		);

		Function<String,String> bFunc = new StringTransformer("^[^#] >> #$0");
		for (Entry<File, List<List<String>>> languageEntry : files.entrySet()) {
			File tableFile = languageEntry.getKey();
			List<List<String>> keyList = languageEntry.getValue();
			String path = tableFile.getPath();
			ColumnTable<String> table = Utilities.loadTable(path, bFunc);

			for (List<String> keys : keyList) {
				ColumnTable<Sequence<Integer>> data = Utilities.toPhoneticTable(
						table,
						factory,
						transformer,
						keys
				);

				MultiMap<String, AlignmentResult<Integer>> alignmentMap = align(
						algorithm,
						keys,
						data
				);

				String tablePath = tableFile.getCanonicalPath();
				String rootPath = EXTENSION_PATTERN.matcher(tablePath)
						.replaceAll("/aligned/");
				writeAlignments(rootPath, alignmentMap);

				Map<String, PairCorrespondenceSet<Integer>> contexts
						= buildContexts(factory, gap.get(0), alignmentMap);
				writeContexts(contexts, rootPath);
			}
		}
	}

	@NonNull
	private static Map<File, List<List<String>>> loadData(
			String basePath, JsonNode configNode
	) {
		Map<File, List<List<String>>> files = new LinkedHashMap<>();
		for (JsonNode file : configNode.get("files")) {
			String path = file.get("path").asText();

			List<List<String>> list = new ArrayList<>();
			JsonNode jsonNode = file.get("cols");
			for (JsonNode node : jsonNode) {
				List<String> cols = new ArrayList<>();
				for (JsonNode colNode : node) {
					cols.add(colNode.asText());
				}
				list.add(cols);
			}
			files.put(new File(basePath + path), list);
		}
		return files;
	}

	private static String readConfigString(String key, JsonNode configNode)
			throws IOException {
		String pathFieldName = key + "_path";
		if (configNode.has(pathFieldName)) {
			String path = configNode.get(pathFieldName).asText();
			return String.valueOf(HANDLER.read(path));
		} else if (configNode.has(key)) {
			JsonNode jsonNode = configNode.get(key);
			return jsonNode.asText();
		} else {
			throw new IllegalArgumentException("Configuration item "
					+ key
					+ " and "
					+ pathFieldName
					+ " not found");
		}
	}
	
	private static List<String> readConfigArray(String key, JsonNode configNode)
			throws IOException {
		String pathFieldName = key + "_path";
		if (configNode.has(pathFieldName)) {
			String path = configNode.get(pathFieldName).asText();
			String value = String.valueOf(HANDLER.read(path));
			return Splitter.lines(value);
		} else if (configNode.has(key)) {
			JsonNode jsonNode = configNode.get(key);
			List<String> list = new ArrayList<>();
			for (JsonNode node : jsonNode) {
				list.add(node.asText(""));
			}
			return list;
		} else {
			throw new IllegalArgumentException("Configuration item "
					+ key
					+ " and "
					+ pathFieldName
					+ " not found");
		}
	}

	private static @NonNull SequenceComparator<Integer> readWeightsComparator(
			FeatureType<Integer> type, String path
	) {
		String payload = null;
		try {
			payload = HANDLER.read(path);
		} catch (IOException e) {
			throw new ParseException("Failed to load file from path " + path, e);
		}

		List<String> lines = Splitter.lines(payload.trim());
		if (lines.size() == 1) {
			List<Double> weights = new ArrayList<>();
			for (String string : WHITESPACE.split(payload, -1)) {
				weights.add(Double.parseDouble(string));
			}
			return new LinearWeightComparator<>(type, weights);
		} else {
			int size = lines.size();
			SymmetricTable<Double> table = new SymmetricTable<>(0.0, size);
			int j = 0;
			for (String line : lines) {
				String[] split = WHITESPACE.split(line, -1);
				for (int i = 0; i < split.length; i++) {
					table.set(i, j, Double.parseDouble(split[i]));
				}
				j++;
			}
			return new MatrixComparator<>(type, table);
		}
	}

	private static <T> void writeContexts(
			Map<String, PairCorrespondenceSet<T>> contexts,
			String rootPath
	) throws IOException {
		for (Entry<String, PairCorrespondenceSet<T>> entry : contexts.entrySet()) {
			String key = entry.getKey();

			File file = new File(rootPath + "contexts_" + key + ".tab");

			Writer writer = new BufferedWriter(new FileWriter(file));
			writer.write("L_orig\tR_orig\tL_a\tLeft\tL_p\tR_a\tRight\tR_p\n");

			PairCorrespondenceSet<T> set = entry.getValue();

			set.iterator().forEachRemaining(element -> {

				Sequence<T> leftSource = element.getLeftSource();
				Sequence<T> rightSource = element.getRightSource();

				Segment<T> left = element.getLeft();
				Segment<T> right = element.getRight();

				ContextPair<T> contextPair = element.getContextPair();

				Context<T> lContext = contextPair.getLeft();
				Context<T> rContext = contextPair.getRight();

				Sequence<T> lA = lContext.getLeft();
				Sequence<T> lP = lContext.getRight();
				Sequence<T> rA = rContext.getLeft();
				Sequence<T> rP = rContext.getRight();

				try {
					writer.write(leftSource.toString());
					writer.write("\t");
					writer.write(rightSource.toString());
					writer.write("\t");
					writer.write(collect(lA));
					writer.write("\t");
					writer.write(left.toString());
					writer.write("\t");
					writer.write(collect(lP));
					writer.write("\t");
					writer.write(collect(rA));
					writer.write("\t");
					writer.write(right.toString());
					writer.write("\t");
					writer.write(collect(rP));
					writer.write("\n");
				} catch (IOException e) {
					LOGGER.error("Failed to write output", e);
				}
			});
			writer.close();
		}
	}

	private static <T> String collect(@Nullable Collection<Segment<T>> sequence) {
		if (sequence == null) return "";
		return sequence.stream()
				.map(Objects::toString)
				.collect(Collectors.joining(" "));
	}

	private static <T> void writeAlignments(String rootPath,
			Iterable<Tuple<String, Collection<AlignmentResult<T>>>> alignments) {
		for (Tuple<String, Collection<AlignmentResult<T>>> entry : alignments) {
			String key = entry.getLeft();
			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			sb1.append(HYPHEN.matcher(key).replaceAll("\t"));
			sb1.append('\n');
			for (AlignmentResult<T> result : entry.getRight()) {
				Iterator<Alignment<T>> list = result.getAlignments().iterator();
				List<CharSequence> charSequences = list.hasNext()
						? list.next().buildPrettyAlignments()
						: Collections.emptyList();
				for (CharSequence sequence : charSequences) {
					String normal = Normalizer.normalize(sequence, Form.NFC);
					String str = HASH.matcher(normal)
							.replaceAll("").trim();
					sb1.append(str);
					sb1.append('\t');
				}
				String stackedSequences = charSequences.stream()
						.map(q -> Normalizer.normalize(q, Form.NFC))
						.map(q -> HASH.matcher(q).replaceAll("").trim())
						.collect(Collectors.joining("\r", "\"", "\""));
				sb1.append(stackedSequences);
				sb1.append('\n');

				ObjectNode node = new ObjectNode(OBJECT_MAPPER.getNodeFactory());
				node.put("left",result.getLeft().toString());
				node.put("right",result.getRight().toString());
				List<Object> objects = new ArrayList<>();
				for (Alignment<T> alignment : result.getAlignments()) {
					objects.add(alignment.getPrettyTable().split("\n"));
				}
				List<Object> table = new ArrayList<>();
				Iterator<Collection<Double>> it = result.getTable().rowIterator();
				while (it.hasNext()) {
					table.add(it.next());
				}
				node.putPOJO("alignments",objects);
				node.putPOJO("table", table);
				try {
					String value = OBJECT_MAPPER
							.writerWithDefaultPrettyPrinter()
							.writeValueAsString(node);
					sb2.append(value);
				} catch (JsonProcessingException e) {
					LOGGER.error("{}", e);
				}
			}

			File file1 = new File(rootPath + "alignments_" + key + ".csv");
			File file2 = new File(rootPath + "alignments_" + key + ".json");
			Path path = file1.toPath();
			try {
				Files.createDirectories(path.getParent());
			} catch (IOException e) {
				LOGGER.error("{}", e);
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file1))) {
				writer.write(sb1.toString());
			} catch (IOException e) {
				LOGGER.error("{}", e);
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file2))) {
				writer.write(sb2.toString());
			} catch (IOException e) {
				LOGGER.error("{}", e);
			}
		}
	}

	private static <T> Map<String, PairCorrespondenceSet<T>> buildContexts(
			SequenceFactory<T> factory,
			Segment<T> gap,
			MultiMap<String, AlignmentResult<T>> alignmentMap
	) {
		FeatureModel<T> model = factory.getFeatureMapping().getFeatureModel();
		Map<String, PairCorrespondenceSet<T>> contexts = new LinkedHashMap<>();
		for (Tuple<String, Collection<AlignmentResult<T>>> e : alignmentMap) {

			String key = e.getLeft();

			PairCorrespondenceSet<T> set = new PairCorrespondenceSet<>();
			for (AlignmentResult<T> alignmentResult : e.getRight()) {

				List<Alignment<T>> alignments = alignmentResult.getAlignments();

				for (Alignment<T> alignment : alignments) {
					
					if (alignment.columns() > 0) {
						List<Segment<T>> left = new ArrayList<>(alignment.getRow(0));
						List<Segment<T>> right = new ArrayList<>(alignment.getRow(1));

						left.add(factory.toSegment("#"));
						right.add(factory.toSegment("#"));

						Sequence<T> lSource = new BasicSequence<>(left, model);
						Sequence<T> rSource = new BasicSequence<>(right, model);

						for (int i = 1; i < alignment.columns() - 1; i++) {
							Segment<T> l = left.get(i);
							Segment<T> r = right.get(i);

								Sequence<T> lA = lookBack(left, i, gap);
								Sequence<T> rA = lookBack(right, i, gap);

								Sequence<T> lP = lookForward(left, i, gap);
								Sequence<T> rP = lookForward(right, i, gap);

								ContextPair<T> pair = new ContextPair<>(
										new Context<>(lA, lP),
										new Context<>(rA, rP)
								);

								set.add(lSource, rSource, l, r, pair);
						}
					}
				}
			}
			contexts.put(key, set);
		}
		return contexts;
	}

	@NonNull
	private static <T> MultiMap<String, AlignmentResult<T>> align(
			@NonNull AlignmentAlgorithm<T> algorithm,
			@NonNull List<String> keyList,
			@NonNull ColumnTable<Sequence<T>> data
	) {
		MultiMap<String, AlignmentResult<T>> alignmentMap = new GeneralMultiMap<>(
				new LinkedHashMap<>(),
				Suppliers.ofList()
		);
		for (int i = 0; i < keyList.size(); i++) {
			String k1 = keyList.get(i);
			List<Sequence<T>> d1 = data.getColumn(k1);
			for (int j = 1; j < keyList.size() && j != i; j++) {
				String k2 = keyList.get(j);
				List<Sequence<T>> d2 = data.getColumn(k2);

				if (d1 == null || d2 == null || d1.size() != d2.size()) {
					continue;
				}

				Collection<AlignmentResult<T>> alignments = new ArrayList<>();
				Iterator<Sequence<T>> it1 = d1.iterator();
				Iterator<Sequence<T>> it2 = d2.iterator();
				while (it1.hasNext() && it2.hasNext()) {
					Sequence<T> e1 = it1.next();
					Sequence<T> e2 = it2.next();

					if (e1.isEmpty() || e2.isEmpty()) {
						continue;
					}

					AlignmentResult<T> result = algorithm.apply(e1, e2);
					alignments.add(result);
				}

				// Start assembling alignments
				TwoKeyMap<Segment<T>, Segment<T>, Integer> correspondenceCounts = new GeneralTwoKeyMap<>();
				TwoKeyMultiMap<Segment<T>, Segment<T>, AlignmentSlice<T>> correspondences = new GeneralTwoKeyMultiMap<>();

				alignments.forEach(result -> {
					for (Alignment<T> alignment : result.getAlignments()) {
						for (int k = 0; k < alignment.columns(); k++) {
							List<Segment<T>> column = alignment.getColumn(k);
							Segment<T> s1 = column.get(0);
							Segment<T> s2 = column.get(1);
							add(correspondenceCounts, s1, s2);
						}
					}
				});

				alignments.forEach(result -> {
					for (Alignment<T> alignment : result.getAlignments()) {
						for (int k = 0; k < alignment.columns(); k++) {
							List<Segment<T>> column = alignment.getColumn(k);
							Segment<T> s1 = column.get(0);
							Segment<T> s2 = column.get(1);
							AlignmentSlice<T> slice = new AlignmentSlice<>(alignment, k);
							correspondences.add(s1, s2, slice);
						}
					}
				});

				alignmentMap.addAll(k1 + '-' + k2, alignments);
			}
		}
		return alignmentMap;
	}

	private static <E> void add(TwoKeyMap<E, E, Integer> map, E s1, E s2) {
		if (map.contains(s1, s2)) {
			Integer value = map.get(s1, s2);
			if (value != null) {
				map.put(s1, s2, value + 1);
				return;
			}
		}
		map.put(s1, s2, 1);
	}

	private static <T> Sequence<T> lookBack(List<Segment<T>> segments, int i, Segment<T> gap) {
		List<Segment<T>> collect = segments.subList(0, i)
				.stream()
				.filter(segment -> !segment.equals(gap))
				.collect(Collectors.toList());
		return new BasicSequence<>(collect, gap.getFeatureModel());
	}

	private static <T> Sequence<T> lookForward(List<Segment<T>> segments, int i, Segment<T> gap) {
		List<Segment<T>> collect = segments.subList(i+1, segments.size())
				.stream()
				.filter(segment -> !segment.equals(gap))
				.collect(Collectors.toList());
		return new BasicSequence<>(collect, gap.getFeatureModel());
	}
}

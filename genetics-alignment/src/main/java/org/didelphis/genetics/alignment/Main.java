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

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.analysis.UnmappedSymbolFinder;
import org.didelphis.genetics.alignment.common.StringTransformer;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.configuration.AlgorithmConfig;
import org.didelphis.genetics.alignment.configuration.ConfigObject;
import org.didelphis.genetics.alignment.configuration.DataFile;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.interfaces.TwoKeyMap;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tables.Table;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.utilities.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.*;

@UtilityClass
public final class Main {

	private static final Logger LOG = Logger.create(Main.class);

	private static final Pattern EXTENSION_PATTERN = compile("\\.[^.]*?$");
	private static final Pattern HYPHEN = compile("-");
	private static final Pattern WHITESPACE = compile("(\n|\r\n?|\\s)+");
	private static final Pattern HASH = compile("#", LITERAL);
	private static final Pattern ZERO = compile("0");

	private static final ObjectMapper OM = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
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
			LOG.error("You must provide a JSON configuration");
			System.exit(-1);
		}

		ConfigObject runConfig = OM.readValue(
				HANDLER.read(args[0]),
				ConfigObject.class);

		File outputFolder = new File(runConfig.getDestinationPath());

		AlgorithmConfig algConfig = OM.readValue(
				HANDLER.read(args[1]),
				AlgorithmConfig.class);

		FormatterMode mode = FormatterMode.INTELLIGENT;

		Function<String, String> transformer = new StringTransformer(runConfig.getTransformations(), mode);

		IntegerFeature instance = IntegerFeature.INSTANCE;

		AlignmentAlgorithm<Integer> algorithm = buildAlgorithm(instance, algConfig);

		SequenceFactory<Integer> factory = algorithm.getFactory();

		for (DataFile dataFile : runConfig.getFiles()) {

			Collection<String> allKeys = new HashSet<>();
			dataFile.getKeys().forEach(allKeys::addAll);

			ColumnTable<String> table = parseDataFile(dataFile, mode, allKeys);
			if (table == null) return;

			ColumnTable<Sequence<Integer>> data = Utilities.toPhoneticTable(
					table,
					factory,
					transformer
			);

			for (String headerKey : data.getKeys()) {
				String gapSymbol = algConfig.getGapSymbol();
				UnmappedSymbolFinder<Integer> finder = new UnmappedSymbolFinder<>(gapSymbol, factory, true);
				List<Sequence<Integer>> column = data.getColumn(headerKey);
				finder.countInSequences(column);
				System.out.println(dataFile.getDisplayNames().get(headerKey));
				finder.write(System.out);
			}

			for (List<String> keys : dataFile.getKeys()) {
				Collection<AlignmentResult<Integer>> results = align(algorithm,
						keys,
						data
				);
				writeResults(outputFolder, keys, results);
			}
		}
	}

	private static <T> void writeResults(
			File outputFolder,
			List<String> keys,
			Collection<AlignmentResult<T>> results
	) {

		if (!outputFolder.exists()) {
			boolean failed = !outputFolder.mkdirs();
			if (failed) {
				LOG.error("Failed to create folder {}", outputFolder);
			}
		}

		DecimalFormat decimalFormat = new DecimalFormat("0.00");
		String fileName = String.join("-", keys) + ".sdm";
		try(BufferedWriter writer = openWriter(outputFolder, fileName)) {

			writer.write("%% "+fileName+"\n");
			writer.write(keys.get(0) + "\n");
			writer.write(keys.get(1) + "\n");
			writer.write("\n");

			for (AlignmentResult<T> result : results) {
				Collection<String> leftList = new ArrayList<>();
				Collection<String> rightList = new ArrayList<>();
				for (Alignment<T> alignment : result.getAlignments()) {
					List<String> charSequences = Alignment.buildPrettyAlignments(alignment);
					leftList.add(charSequences.get(0));
					rightList.add(charSequences.get(1));
				}

				String leftGroup = String.join(" | ", leftList);
				String rightGroup = String.join(" | ", rightList);

				Sequence<T> left  = result.getLeft();
				Sequence<T> right = result.getRight();

				writer.write(leftGroup + "\n");
				writer.write(rightGroup + "\n");
				writer.write("\n");
				writer.flush();

				// Write alignment table in SDM comment block
				String collect1 = right.stream()
						.map(Segment::getSymbol)
						.map((String s) -> pad(s, 5))
						.collect(Collectors.joining(" "));
				writer.write("%      " + collect1 + "\n");
				Table<Double> table = result.getTable();
				for (int i = 0; i < table.rows(); i++) {
					writer.write("% ");
					writer.write(pad(left.get(i).getSymbol(), -5));
					List<Double> row = table.getRow(i);
					String collect = row.stream()
							.map(decimalFormat::format)
							.map((String string) -> pad(string, 5))
							.collect(Collectors.joining(" "));
					writer.write(collect);
					writer.write("\n");
				}
				writer.write("\n");
				writer.write("\n");
				writer.flush();
			}
		} catch (IOException e) {
			LOG.error("Unable to write outputs", e);
		}
	}

	@NonNull
	private static BufferedWriter openWriter(File outputFolder, String fileName)
			throws IOException {
		return new BufferedWriter(new FileWriter(new File(outputFolder, fileName)));
	}

	@NonNull
	private static String pad(String string, int width) {
		StringBuilder stringBuilder = new StringBuilder(string);
		if (width < 0) {
			while (stringBuilder.length() < -width) {
				stringBuilder.append(" ");
			}
		} else {
			while (stringBuilder.length() < width) {
				stringBuilder.insert(0, " ");
			}
		}
		return stringBuilder.toString();
	}

	@NonNull
	private static <T> AlignmentAlgorithm<T> buildAlgorithm(FeatureType<T> type, AlgorithmConfig config) {

		FeatureModelLoader<T> loader = new FeatureModelLoader<>(
				type,
				HANDLER,
				config.getModelPath()
		);

		SequenceFactory<T> factory = new SequenceFactory<>(
				loader.getFeatureMapping(),
				FormatterMode.INTELLIGENT
		);

		return config.buildAlgorithm(factory);
	}

	@Nullable
	private static ColumnTable<String> parseDataFile(
			@NonNull DataFile data, FormatterMode form, Collection<String> allKeys
	) {
		String path = data.getPath();
		String format = data.getType();
		ColumnTable<String> table;
		if (format.equals("csv")) {
			table = Utilities.csvToTable(path, form, allKeys);
		} else if (format.equals("tsv")) {
			table = Utilities.tsvToTable(path, form, allKeys);
		} else if (format.equals("dsv")) {
			table = Utilities.dsvToTable(path, form, allKeys);
		} else {
			LOG.error("Unsupported file type {}", format);
			return null;
		}
		return table;
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
				List<String> charSequences = list.hasNext()
						? Alignment.buildPrettyAlignments(list.next())
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

//				ObjectNode node = new ObjectNode(OM.getNodeFactory());
//				node.put("left",result.getLeft().toString());
//				node.put("right",result.getRight().toString());
//				List<Object> objects = new ArrayList<>();
//				for (Alignment<T> alignment : result.getAlignments()) {
//					objects.add(alignment.getPrettyTable().split("\n"));
//				}
//				List<Object> table = new ArrayList<>();
//				Iterator<Collection<Double>> it = result.getTable().rowIterator();
//				while (it.hasNext()) {
//					table.add(it.next());
//				}
//				node.putPOJO("alignments",objects);
//				node.putPOJO("table", table);
//				try {
//					String value = OM
//							.writerWithDefaultPrettyPrinter()
//							.writeValueAsString(node);
//					sb2.append(value);
//				} catch (JsonProcessingException e) {
//					LOG.error("{}", e);
//				}
			}

			File file1 = new File(rootPath + "alignments_" + key + ".csv");
//			File file2 = new File(rootPath + "alignments_" + key + ".json");
			Path path = file1.toPath();
			try {
				Files.createDirectories(path.getParent());
			} catch (IOException e) {
				LOG.error("{}", e);
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file1))) {
				writer.write(sb1.toString());
			} catch (IOException e) {
				LOG.error("{}", e);
			}

//			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file2))) {
//				writer.write(sb2.toString());
//			} catch (IOException e) {
//				LOG.error("{}", e);
//			}
		}
	}

	@NonNull
	private static <T> Collection<AlignmentResult<T>> align(
			@NonNull AlignmentAlgorithm<T> algorithm,
			@NonNull List<String> keyList,
			@NonNull ColumnTable<Sequence<T>> data
	) {
		String k1 = keyList.get(0);
		String k2 = keyList.get(1);

		List<Sequence<T>> d1 = data.getColumn(k1);
		List<Sequence<T>> d2 = data.getColumn(k2);

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
		return alignments;
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

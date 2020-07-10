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

import lombok.experimental.UtilityClass;

import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.configuration.AlgorithmConfig;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.didelphis.genetics.alignment.Processor.*;

@UtilityClass
public class DistanceCalculator {

	private final Logger LOG = LogManager.getLogger(Main.class);

	private final ObjectMapper MAPPER  = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
	private final FileHandler  HANDLER = new DiskFileHandler("UTF-8");
	private final Pattern      COMPILE = Pattern.compile("\r?\n");

	public void main(String[] args) throws IOException {
		if (args.length == 0) {
			LOG.error("You must provide a path to the JSON configurations");
			System.exit(-1);
		}

		AlgorithmConfig algConfig = MAPPER.readValue(new File(args[1]), AlgorithmConfig.class);

		FormatterMode mode = FormatterMode.NONE;
		IntegerFeature type = IntegerFeature.INSTANCE;

		AlignmentAlgorithm<Integer> algorithm = buildAlgorithm(
				type,
				algConfig
		);
		SequenceFactory<Integer> factory = algorithm.getFactory();
		Set<Sequence<Integer>> set;

/*
		// This code originally loaded sequences from every configuration in the
		// folder provided as an input argument
		// ---------------------------------------------------------------------
		UnmappedSymbolFinder<Integer> counter = new UnmappedSymbolFinder<>("░", factory, true);

		File folder = new File(args[0]);
		if (folder.isDirectory()) {
			for (File file : folder.listFiles()) {
				if (file.isDirectory()) continue;

				LOG.info("File: {}", file.getAbsolutePath());

				ConfigObject config = OM.readValue(file, ConfigObject.class);
				List<List<String>> tfList = config.getTransformations();
				StringTransformer transformer = new StringTransformer(tfList, mode);

				for (DataFile dataFile : config.getFiles()) {
					Map<String, String> displayNames = dataFile.getDisplayNames();
					ColumnTable<String> table = parseDataFile(dataFile, mode, displayNames.keySet());
					ColumnTable<Sequence<Integer>> data = Utilities.toPhoneticTable(
							table,
							factory,
							transformer
					);

					counter.countInTable(data);
					counter.write(System.out);
					counter.reset();

					data.rowIterator().forEachRemaining(row -> {
						for (Sequence<Integer> segments : row) {
							for (Segment<Integer> segment : segments) {
								set.add(new BasicSequence<>(segment));
							}
						}
					});
				}
			}
		}
*/

		String read = HANDLER.read(args[0]);
		set = Arrays.stream(COMPILE.split(read))
				.map(factory::toSequence)
				.collect(Collectors.toSet());

		SequenceComparator<Integer> comparator = algorithm.getComparator();

		// This instead
		List<Sequence<Integer>> list = new ArrayList<>(set);

		Table<Double> scores = new RectangularTable<>(
				0.0,
				list.size(),
				list.size()
		);



		for (int i = 0; i < list.size(); i++) {
			for (int j = 0; j <= i; j++) {
				Sequence<Integer> qL = list.get(i);
				Sequence<Integer> qR = list.get(j);
				// It's 0,0 because the comparator looks at sequence and needs
				// to know which segment in the sequence to look at
				double score = comparator.apply(qL, qR, 0, 0);
				scores.set(i, j, score);
				scores.set(j, i, score);
			}
		}

		List<String> collect = new ArrayList<>();
		for (Sequence<Integer> q : list) {
			Segment<Integer> integerSegment = q.get(0);
			String symbol = integerSegment.getSymbol();
			collect.add(symbol);
		}

		String displayTable = Utilities.formatDistanceTable(collect, scores);
		File tableFile = new File("./", "distances.table");
		try {
			String absolutePath = tableFile.getAbsolutePath();
			HANDLER.writeString(absolutePath, displayTable);
		} catch (IOException e) {
			LOG.error("Failed to write distance table", e);
		}

	}
}

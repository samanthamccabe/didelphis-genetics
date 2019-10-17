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

package org.didelphis.genetics.alignment.analysis;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.parsing.ParseException;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.segments.SemidefinedSegment;
import org.didelphis.language.phonetic.segments.UndefinedSegment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tuples.Couple;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@SuppressWarnings ("UseOfSystemOutOrSystemErr")
@Getter
@ToString
@EqualsAndHashCode
public final class UnmappedSymbolFinder<T> {

	private final String gap;
	private final SequenceFactory<T> factory;
	private final boolean countWholeSymbols;
	private final Map<String, Integer> counts;

	public static void main(String[] args) {

		String dataPath = "/projects/project-data/";
		String modelPath = dataPath + "AT_extended_x.model";
		String sdmPath = dataPath + "sdm/";

		String gap = "░";

		FormatterMode mode = FormatterMode.INTELLIGENT;
		FileHandler handler = new DiskFileHandler("UTF-8");
		FeatureModelLoader<?> loader = new FeatureModelLoader<>(
				IntegerFeature.INSTANCE,
				handler,
				modelPath
		);

		FeatureMapping<?> mapping = loader.getFeatureMapping();
		SequenceFactory<?> fac = new SequenceFactory<>(mapping, mode);
		UnmappedSymbolFinder<?> counter = new UnmappedSymbolFinder<>(gap, fac);

		for (File file : new File(sdmPath).listFiles()) {
			try {
				String data = handler.read(file.getCanonicalPath());
				counter.countInSDM(data);
			} catch (RuntimeException | IOException e) {
				throw new ParseException("Error reading " + file.getName(), e);
			}

			Map<String, Integer> counts = counter.counts;

			if (counts.isEmpty()) continue;

			counter.write(System.out);
		}
	}

	public UnmappedSymbolFinder(String gap, SequenceFactory<T> factory) {
		this(gap, factory, false);
	}

	public UnmappedSymbolFinder(String gap, SequenceFactory<T> factory, boolean countWholeSymbols) {
		this.gap = gap;
		this.factory = factory;
		this.countWholeSymbols = countWholeSymbols;
		counts = new HashMap<>();

	}

	public void countStringInTable(ColumnTable<String> table) {
		for (String key : table.getKeys()) {
			for (String segments : table.getColumn(key)) {
				for (int i = 0; i < segments.length(); i++) {
					update(segments.substring(i, i+1));
				}
			}
		}
	}

	public void countInTable(ColumnTable<Sequence<T>> table) {
		for (String key : table.getKeys()) {
			for (Sequence<T> segments : table.getColumn(key)) {
				for (Segment<T> segment : segments) {
					update(segment);
				}
			}
		}
	}

	public void countInSDM(String data) {
		List<List<Alignment<T>>> lists = Utilities.loadSDM(data, factory);
		for (List<Alignment<T>> list : lists) {
			for (Alignment<T> alignment : list) {
				for (int i = 0; i < alignment.columns(); i++) {
					List<Segment<T>> col = alignment.getColumn(i);
					for (Segment<T> segment : col) {
						update(segment);
					}
				}
			}
		}
	}

	public void write(OutputStream out) {
		counts.entrySet()
				.stream()
				.map(e -> new Couple<>(e.getKey(), e.getValue()))
				.filter(couple -> couple.getLeft() != null)
				.filter(couple -> couple.getRight() != null)
				.sorted(Comparator.comparing(Couple::getRight))
				.forEach(couple -> {
					String symbol = couple.getLeft();
					Integer count = couple.getRight();
					String s = " " + symbol + "\t" + count+"\n";
					try {
						out.write(s.getBytes());
					} catch (IOException e) {
//						LOG
					}
				});
	}

	public void reset() {
		counts.clear();
	}

	private void update(String seg) {
		if (seg.equals("#") || seg.equals("░")) return;

		FeatureMapping<T> featureMapping = factory.getFeatureMapping();
		Set<String> keys = featureMapping.getFeatureMap().keySet();
		Set<String> mods = featureMapping.getModifiers().keySet();

		if (!keys.contains(seg) && !mods.contains(seg)) {
			counts.put(seg, counts.getOrDefault(seg, 0) + 1);
		}
	}

	private void update(Segment<T> seg) {
		String symbol = seg.getSymbol();

		if (symbol.equals("#") || symbol.equals("░")) return;

		//noinspection ChainOfInstanceofChecks
		if (seg instanceof UndefinedSegment) {
			counts.put(symbol, counts.getOrDefault(symbol, 0) + 1);
		} else if (seg instanceof SemidefinedSegment) {
			SemidefinedSegment<T> sdg = (SemidefinedSegment<T>) seg;
			if (countWholeSymbols) {
				counts.put(symbol, counts.getOrDefault(symbol, 0) + 1);
			} else {
				String prefix = sdg.getPrefix();
				String suffix = sdg.getSuffix();
				if (!prefix.isEmpty()) {
					counts.put(prefix, counts.getOrDefault(prefix, 0) + 1);
				}
				if (!suffix.isEmpty()) {
					counts.put(suffix, counts.getOrDefault(suffix, 0) + 1);
				}
			}
		}
	}

}

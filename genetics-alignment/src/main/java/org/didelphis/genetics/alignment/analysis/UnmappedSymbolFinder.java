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
import org.didelphis.structures.tuples.Couple;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings ("UseOfSystemOutOrSystemErr")
@Getter
@ToString
@EqualsAndHashCode
public final class UnmappedSymbolFinder<T> {

	private final String gap;
	private final SequenceFactory<T> factory;
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
				counter.count(data);
			} catch (RuntimeException | IOException e) {
				throw new ParseException("Error reading " + file.getName(), e);
			}

			Map<String, Integer> counts = counter.counts;

			if (counts.isEmpty()) continue;

			System.out.println(file.getName());
			counts.entrySet()
					.stream()
					.map(e -> new Couple<>(e.getKey(), e.getValue()))
					.filter(couple -> couple.getLeft() != null)
					.filter(couple -> couple.getRight() != null)
					.sorted(Comparator.comparing(Couple::getRight))
					.forEach(couple -> {
						String symbol = couple.getLeft();
						Integer count = couple.getRight();
						System.out.println(" " + symbol + "\t" + count);
					});
		}
	}

	public UnmappedSymbolFinder(String gap, SequenceFactory<T> factory) {
		this.gap = gap;
		this.factory = factory;
		counts = new HashMap<>();
	}

	private void count(String data) {
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

	private void update(Segment<T> seg) {
		String symbol = seg.getSymbol();

		if (symbol.equals("#") || symbol.equals("░")) return;

		//noinspection ChainOfInstanceofChecks
		if (seg instanceof UndefinedSegment) {
			counts.put(symbol, counts.getOrDefault(symbol, 0) + 1);
		} else if (seg instanceof SemidefinedSegment) {
			SemidefinedSegment<T> sdg = (SemidefinedSegment<T>) seg;
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

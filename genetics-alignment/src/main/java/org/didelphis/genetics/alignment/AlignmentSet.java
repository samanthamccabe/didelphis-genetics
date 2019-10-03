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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.sequences.Sequence;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(of = "keys")
@EqualsAndHashCode(callSuper = true)
public final class AlignmentSet<T> extends ArrayList<Alignment<T>> {

	private static final Pattern PATTERN = Pattern.compile("\n|\r\n?");
	private static final Pattern TAB = Pattern.compile("\\t");
	private static final Pattern SPACE = Pattern.compile("\\s+");

	Iterable<String> keys;

	private AlignmentSet(Collection<String> keys) {
		this.keys = keys;
	}

	public static <T> AlignmentSet<T> loadFromFile(String file,
			SequenceFactory<T> factory) throws IOException {

		CharSequence input = new DiskFileHandler("UTF-8").read(file);
		List<String> list = Arrays.stream(PATTERN.split(input))
				.collect(Collectors.toList());

		List<String> headers = new ArrayList<>();
		Collections.addAll(headers, TAB.split(list.remove(0)));

		AlignmentSet<T> alignments = new AlignmentSet<>(headers);
		for (String line : list) {

			List<List<Sequence<T>>> data = new ArrayList<>();
			for (String cell : TAB.split("\\t", -1)) {
				List<Sequence<T>> sequences;
				if (cell.trim().isEmpty()) {
					sequences = null;
				} else {
					sequences = new ArrayList<>();
					sequences.add(factory.toSequence("#"));
					for (String element : SPACE.split(cell)) {
						sequences.add(factory.toSequence(element));
					}
				}
				data.add(sequences);
			}

			// Fill in the empty cells
			Iterator<List<Sequence<T>>> it = data.iterator();
			int length = -1;
			while (it.hasNext() && length == -1) {
				Collection<Sequence<T>> item = it.next();
				if (item != null) {
					length = item.size();
				}
			}

			for (int i = 0; i < data.size(); i++) {
				List<Sequence<T>> object = data.get(i);
				if (object == null) {
					List<Sequence<T>> sequences = Collections.nCopies(length, factory.toSequence(""));
					data.set(i, sequences);
				}
			}
// TODO:
//			alignments.add(new Alignment<>(data, factory));
		}
		return alignments;
	}

	public Iterable<String> getKeys() {
		return keys;
	}
}

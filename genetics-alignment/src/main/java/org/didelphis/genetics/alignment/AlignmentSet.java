package org.didelphis.genetics.alignment;

import org.didelphis.io.DiskFileHandler;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.sequences.Sequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Samantha Fiona McCabe
 * Created: 11/6/2015
 */
public final class AlignmentSet<T> extends ArrayList<Alignment<T>> {

	private static final Pattern PATTERN = Pattern.compile("\n|\r\n?");
	private static final Pattern TAB = Pattern.compile("\\t");
	private static final Pattern SPACE = Pattern.compile("\\s+");

	private final Iterable<String> keys;

	private AlignmentSet(Collection<String> keys) {
		this.keys = keys;
	}

	public static <T> AlignmentSet<T> loadFromFile(String file,
			SequenceFactory<T> factory) {

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

	@Override
	public String toString() {
		return "AlignmentSet{" + "keys=" + keys + '}';
	}
}

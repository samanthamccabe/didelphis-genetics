package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.common.io.DiskFileHandler;
import org.didelphis.common.language.phonetic.SequenceFactory;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.Alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 11/6/2015
 */
public class AlignmentSet extends ArrayList<Alignment<Double>> {

	private static final Pattern PATTERN = Pattern.compile("\n|\r\n?");
	private static final Pattern TAB = Pattern.compile("\\t");
	private static final Pattern SPACE = Pattern.compile(" +");

	private final Iterable<String> keys;

	private AlignmentSet(Collection<String> keys) {
		this.keys = keys;
	}

	public static AlignmentSet loadFromFile(String file,
			SequenceFactory<Double> factory) {


		CharSequence input = new DiskFileHandler("UTF-8").read(file);
		List<String> list = Arrays.stream(PATTERN.split(input))
				.collect(Collectors.toList());

		List<String> headers = new ArrayList<>();
		Collections.addAll(headers, TAB.split(list.remove(0)));

		AlignmentSet alignments = new AlignmentSet(headers);
		for (String line : list) {

			List<List<Sequence<Double>>> data = new ArrayList<>();
			for (String cell : TAB.split("\\t", -1)) {
				List<Sequence<Double>> sequences;
				if (cell.trim().isEmpty()) {
					sequences = null;
				} else {
					sequences = new ArrayList<>();
					sequences.add(factory.getBorderSequence());
					for (String element : SPACE.split(cell)) {
						sequences.add(factory.getSequence(element));
					}
				}
				data.add(sequences);
			}

			// Fill in the empty cells
			Iterator<List<Sequence<Double>>> it = data.iterator();
			int length = -1;
			while (it.hasNext() && length == -1) {
				Collection<Sequence<Double>> item = it.next();
				if (item != null) {
					length = item.size();
				}
			}

			for (int i = 0; i < data.size(); i++) {
				List<Sequence<Double>> object = data.get(i);
				if (object == null) {
					List<Sequence<Double>> sequences =
							IntStream.range(0, length)
									.mapToObj(j -> factory.getNewSequence())
									.collect(Collectors.toList());
					data.set(i, sequences);
				}
			}

			alignments.add(new Alignment<>(data, factory));
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

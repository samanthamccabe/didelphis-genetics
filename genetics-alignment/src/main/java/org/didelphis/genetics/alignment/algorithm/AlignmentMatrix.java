package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.Alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 6/7/2015
 */
public class AlignmentMatrix {
	
	private final List<Object> matrix;
	private final List<Sequence<Double>> sequences;

	public AlignmentMatrix(List<Sequence<Double>> sequenceParam) {
		sequences = sequenceParam;
		matrix = buildMatrix(-1);
	}

	public List<Sequence<Double>> getSequences() {
		return sequences;
	}

	public Alignment<Double> get(int... indices) {
		List<Object> array = recurse(matrix, indices, 0);
		//noinspection unchecked
		return (Alignment<Double>) array.get(indices[indices.length - 1]);
	}

	public void set(Alignment<Double> alignment, int[] indices) {
		List<Object> array = recurse(matrix, indices, 0);
		array.set(indices[indices.length - 1], alignment);
	}

	private List<Object> buildMatrix(int x) {
		List<Object> list = new ArrayList<>();

		if (x < sequences.size() - 1) {
			// Not the last layer
			x++;
			Sequence<Double> sequence = sequences.get(x);
			if (sequence.isEmpty()) {
				list.add(buildMatrix(x));
			} else {
				for (int i = 0; i < sequence.size(); i++) {
					list.add(buildMatrix(x));
				}
			}
		} else {
			Sequence<Double> sequence = sequences.get(x);
			if (sequence.isEmpty()) {
				list.add(null);
			} else {
				// default value here is null
				list = IntStream.range(0, sequence.size())
						.mapToObj(i -> null)
						.collect(Collectors.toList());
			}
		}
		//todo: is this copying neccesssary?
		return new ArrayList<>(list);
	}

	private List<Object> recurse(List<Object> obj, int[] indices, int depth) {
		int x = indices[depth];
		@SuppressWarnings("unchecked") List<Object> buffer =
				(List<Object>) obj.get(x);

		depth++;
		return depth == sequences.size()
		       ? buffer
		       : recurse(buffer, indices, depth);
	}
}

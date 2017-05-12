package org.didelphis.genetics.alignment;

import org.didelphis.common.language.phonetic.SequenceFactory;
import org.didelphis.common.language.phonetic.model.interfaces.FeatureModel;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.BasicSequence;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.common.structures.tables.ColumnTable;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AlignmentEngine {

	private static final transient Logger LOGGER =
			LoggerFactory.getLogger(AlignmentEngine.class);

	private final Segment<Double> boundary;
	private final SequenceFactory<Double> factory;
	private final FeatureModel<Double> model;

	private final AlignmentAlgorithm algorithm;

	public AlignmentEngine(SequenceFactory<Double> factoryParam,
			AlignmentAlgorithm algorithmParam) {
		factory = factoryParam;
		algorithm = algorithmParam;
		model = factory.getFeatureMapping().getFeatureModel();
		boundary = factory.getBorderSegment();
	}

	public List<Alignment<Double>> alignMultiSequence(
			ColumnTable<Sequence<Double>> data) {
		List<Alignment<Double>> alignments = new ArrayList<>(data.getRows());

		for (Iterable<Sequence<Double>> sequences : data) {
			List<Sequence<Double>> list = new ArrayList<>();
			for (Sequence<Double> sequence : sequences) {

				Sequence<Double> segments = new BasicSequence<>(sequence);
				if (!sequence.isEmpty()) {
					segments.addFirst(boundary);
				}
				list.add(segments);
			}
			alignments.add(algorithm.getAlignment(list));
		}
		return alignments;
	}

	public List<Alignment<Double>> alignSequencePairs(
			List<Sequence<Double>> leftColumn,
			List<Sequence<Double>> rightColumn) {
		if (leftColumn.size() != rightColumn.size()) {
			throw new RuntimeException(
					"Mismatch in right and left column sizes! " +
							leftColumn.size() + " vs " + rightColumn.size());
		} else {
			List<Alignment<Double>> alignments =
					new ArrayList<>(leftColumn.size());

			for (int i = 0; i < leftColumn.size(); i++) {
				Sequence<Double> left = new BasicSequence<>(leftColumn.get(i));
				Sequence<Double> right =
						new BasicSequence<>(rightColumn.get(i));

				Alignment<Double> alignment;
				if (!left.isEmpty() && !right.isEmpty()) {
					alignment = getAlignment(left, right);
				} else {
					alignment = new Alignment<>(model);
				}

				alignments.add(alignment);
			}
			return alignments;
		}
	}

	private Alignment<Double> getAlignment(Sequence<Double> left,
			Sequence<Double> right) {
		left.addFirst(boundary);
		right.addFirst(boundary);

		List<Sequence<Double>> sequences = new ArrayList<>();
		sequences.add(left);
		sequences.add(right);
		return algorithm.getAlignment(sequences);
	}
}

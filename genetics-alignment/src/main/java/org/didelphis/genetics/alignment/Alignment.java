package org.didelphis.genetics.alignment;

import org.didelphis.common.language.phonetic.ModelBearer;
import org.didelphis.common.language.phonetic.SequenceFactory;
import org.didelphis.common.language.phonetic.SpecificationBearer;
import org.didelphis.common.language.phonetic.model.interfaces.FeatureModel;
import org.didelphis.common.language.phonetic.model.interfaces.FeatureSpecification;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.BasicSequence;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.common.structures.Structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by samantha on 1/9/17.
 */
public class Alignment<N extends Number>
		implements Iterable<List<Sequence<N>>>, SpecificationBearer, Structure {

	private final FeatureSpecification specification;
	private final List<List<Sequence<N>>> sequences;

	// start indices of the subsequences within the original sequences
	private final List<List<Integer>> indices;

	// Original sequences the alignment is derived from
	private final List<Sequence<N>> originalSequences;

	private double score;

	public Alignment(FeatureSpecification modelParam) {
		specification = modelParam;
		sequences = new ArrayList<>();
		indices = new ArrayList<>();
		originalSequences = new ArrayList<>();
	}

	public Alignment(int n, FeatureSpecification modelParam) {
		specification = modelParam;
		sequences = new ArrayList<>();
		indices = new ArrayList<>();
		originalSequences = new ArrayList<>();

		for (int i = 0; i < n; i++) {
			sequences.add(new ArrayList<>());
			indices.add(new ArrayList<>());
		}
	}

	public Alignment(Alignment<N> alignment) {
		specification = alignment.specification;
		sequences = new ArrayList<>();
		indices = new ArrayList<>();

		// Defensively copy inner lists
		for (List<Sequence<N>> item : alignment.sequences) {
			sequences.add(new ArrayList<>(item));
		}

		// Defensively copy inner lists
		for (List<Integer> item : alignment.indices) {
			indices.add(new ArrayList<>(item));
		}

		originalSequences = new ArrayList<>(alignment.originalSequences);
	}

	public Alignment(List<List<Sequence<N>>> list, SequenceFactory<N> factory) {
		if (list.isEmpty()) {
			throw new IllegalArgumentException(
					"Cannot provide empty list to initializer!");
		}

		ModelBearer<N> featureMapping = factory.getFeatureMapping();
		FeatureModel<N> featureModel = featureMapping.getFeatureModel();
		specification = featureMapping.getSpecification();

		sequences = list;
		indices = new ArrayList<>();

		originalSequences = new ArrayList<>();
		Sequence<N> gapSequence = factory.getSequence("░");
		for (Iterable<Sequence<N>> sequenceList : list) {
			Sequence<N> original = new BasicSequence<>(featureModel);

			Collection<Integer> indexRow = new ArrayList<>();
			int i = 0;
			for (Sequence<N> sequence : sequenceList) {
				if (sequence.equals(gapSequence)) {
					indexRow.add(-1 * i);
				} else {
					original.add(sequence);
					indexRow.add(i);
				}
				i++;
			}
			originalSequences.add(original);
		}
	}

	public Alignment(ModelBearer<N> left, Segment<N> right) {
		this(2, left.getFeatureModel());
		// TODO:
	}

	public double getScore() {
		return score;
	}

	public void setScore(double scoreParam) {
		score = scoreParam;
	}

	public List<Sequence<N>> getRow(int i) {
		return sequences.get(i);
	}

	public Collection<Integer> getRowIndices(int i) {
		return indices.get(i);
	}

	/**
	 * Retrieves a slice through the Alignment, across languages.
	 *
	 * @param index position from which to get the slice
	 *
	 * @return a list containing the sequences from each language
	 */
	public List<Sequence<N>> slice(int index) {
		List<Sequence<N>> slice = new ArrayList<>(getNumberRows());
		for (List<Sequence<N>> sequence : sequences) {
			slice.add(sequence.get(index));
		}
		return slice;
	}

	/**
	 * Retrieves a slice through the Alignment, across languages.
	 *
	 * @param index position from which to get the slice
	 *
	 * @return a list containing the sequences from each language
	 */
	public List<Integer> sliceIndices(int index) {
		List<Integer> slice = new ArrayList<>(getNumberRows());
		for (List<Integer> indexList : indices) {
			slice.add(indexList.get(index));
		}
		return slice;
	}

	public void add(Alignment<N> alignment) {
		if (getNumberRows() != alignment.getNumberRows()) {
			throw new IllegalArgumentException(
					"Attempting to add Alignments of difference sizes");
		}
		for (int i = 0; i < getNumberRows(); i++) {
			sequences.get(i).addAll(alignment.getRow(i));
			indices.get(i).addAll(alignment.getRowIndices(i));
		}
	}

	/**
	 * @param sequenceList
	 * @param indexList
	 */
	public void add(List<Sequence<N>> sequenceList, List<Integer> indexList) {
		if (sequenceList.size() != getNumberRows() ||
				sequenceList.size() != indexList.size()) {
			throw new IllegalArgumentException(
					"Input list must have the same number of dimensions as " +
							"this Alignment.");
		}
		for (int i = 0; i < getNumberRows(); i++) {
			sequences.get(i).add(sequenceList.get(i));
			indices.get(i).add(indexList.get(i));
		}
	}

	public int getNumberColumns() {
		if (!sequences.isEmpty()) {
			return sequences.get(0).size();
		}
		return 0;
	}

	public int getNumberRows() {
		return sequences.size();
	}

	public String getPrettyTable() {

		StringBuilder stringBuilder = new StringBuilder();
		for (CharSequence charSequence : buildPrettyAlignments()) {
			stringBuilder.append(charSequence);
			stringBuilder.append('\n');
		}
		return stringBuilder.toString();
	}

	// Checks spacing of alignment parts in monospace type so they will 
	// getAlignment nicely
	// Data is placed into the provided buffers
	public Collection<CharSequence> buildPrettyAlignments() {

		Collection<CharSequence> builders = new ArrayList<>(getNumberRows());

		int n = getNumberColumns();

		List<Integer> maxima = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			maxima.add(0);
		}

		for (List<Sequence<N>> sequence : sequences) {
			for (int i = 0; i < n; i++) {
				int v = maxima.get(i);
				String s = sequence.get(i).toString();
				int size = getPrintableLength(s);

				if (v < size) {
					maxima.set(i, size);
				}
			}
		}

		for (List<Sequence<N>> sequence : sequences) {

			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < n; i++) {
				String s = sequence.get(i).toString();
				int maximum = maxima.get(i);
				int visible = getPrintableLength(s);

				builder.append(s).append(' ');

				while (maximum > visible) {
					builder.append(' ');
					visible++;
				}
			}
			builders.add(builder);
		}
		return builders;
	}

	@Override
	public Iterator<List<Sequence<N>>> iterator() {
		return sequences.iterator();
	}

	@Override
	public int size() {
		return 0; // todo
	}

	@Override
	public boolean isEmpty() {
		return false; // todo
	}

	@Override
	public boolean clear() {
		return false; // todo
	}

	@Override
	public FeatureSpecification getSpecification() {
		return specification;
	}

	@Override
	public int hashCode() {
		int result = specification.hashCode();
		result = 31 * result + sequences.hashCode();
		result = 31 * result + indices.hashCode();
		result = 31 * result + originalSequences.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Alignment)) {
			return false;
		}

		Alignment<?> alignment = (Alignment<?>) obj;
		return sequences.equals(alignment.sequences) &&
				indices.equals(alignment.indices) &&
				originalSequences.equals(alignment.originalSequences);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		Iterator<List<Sequence<N>>> it = sequences.iterator();

		while (it.hasNext()) {
			Iterable<Sequence<N>> list = it.next();
			Iterator<Sequence<N>> l = list.iterator();
			while (l.hasNext()) {
				Iterable<Segment<N>> next = l.next();
				for (Segment<N> segment : next) {
					sb.append(segment);
				}

				if (l.hasNext()) {
					sb.append(' ');
				}
			}
			if (it.hasNext()) {
				sb.append('\t');
			}
		}
		return sb.toString();
	}

	private int getPrintableLength(String l) {
		int leftVisible = 0;
		for (char c : l.toCharArray()) {
			if (Character.getType(c) != Character.NON_SPACING_MARK) {
				leftVisible++;
			}
		}
		return leftVisible;
	}

	private void validateModelOrFail(SpecificationBearer that) {
		FeatureSpecification thatFeatureModel = that.getSpecification();
		if (!specification.equals(thatFeatureModel)) {
			throw new RuntimeException("Attempting to add " + that.getClass() +
					" with an incompatible specification!\n" + '\t' + this +
					'\t' + specification.getFeatureNames() + '\n' + '\t' +
					that + '\t' + thatFeatureModel.getFeatureNames());
		}
	}

	private static void modelConsistencyCheck(SpecificationBearer l,
			SpecificationBearer r) {
		FeatureSpecification mL = l.getSpecification();
		FeatureSpecification mR = r.getSpecification();
		if (!mL.equals(mR)) {
			throw new RuntimeException(
					"Attempting to create Alignment using incompatible " +
							"models!\n" + '\t' + l + '\t' + mL + '\n' + '\t' +
							r + '\t' + mR + '\n');
		}
	}
}

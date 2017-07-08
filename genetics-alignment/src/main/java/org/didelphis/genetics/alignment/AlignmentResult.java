package org.didelphis.genetics.alignment;

import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.Table;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Class {@code AlignmentResult}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-07-03
 */
public class AlignmentResult<T> {

	private final Sequence<T> left;
	private final Sequence<T> right;
	private final Table<Double> table;
	private final List<Alignment<T>> alignments;

	public AlignmentResult(Sequence<T> left, Sequence<T> right, Table<Double> table,
			Alignment<T> alignment) {
		this.left = left;
		this.right = right;
		this.table = table;
		this.alignments = Collections.singletonList(alignment);
	}

	public AlignmentResult(Sequence<T> left, Sequence<T> right, Table<Double> table,
			List<Alignment<T>> alignments) {
		this.left = left;
		this.right = right;
		this.table = table;
		this.alignments = alignments;
	}

	@Override
	public int hashCode() {
		return Objects.hash(left, right, table, alignments);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof AlignmentResult)) { return false; }
		final AlignmentResult other = (AlignmentResult) obj;
		return Objects.equals(this.left, other.left) &&
				Objects.equals(this.right, other.right) &&
				Objects.equals(this.table, other.table) &&
				Objects.equals(this.alignments, other.alignments);
	}

	public List<Alignment<T>> getAlignments() {
		return alignments;
	}

	public Table<Double> getTable() {
		return table;
	}

	public Sequence<T> getRight() {
		return right;
	}

	public Sequence<T> getLeft() {
		return left;
	}
}

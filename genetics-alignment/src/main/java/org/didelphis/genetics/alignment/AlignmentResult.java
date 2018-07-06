package org.didelphis.genetics.alignment;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.Table;

import java.util.Collections;
import java.util.List;

/**
 * Class {@code AlignmentResult}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-07-03
 */
@SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
public class AlignmentResult<T> {

	Sequence<T> left;
	Sequence<T> right;
	Table<Double> table;
	List<Alignment<T>> alignments;

	public AlignmentResult(
			Sequence<T> left,
			Sequence<T> right,
			Table<Double> table,
			Alignment<T> alignment
	) {
		this.left = left;
		this.right = right;
		this.table = table;
		alignments = Collections.singletonList(alignment);
	}

	public AlignmentResult(
			Sequence<T> left,
			Sequence<T> right,
			Table<Double> table,
			List<Alignment<T>> alignments
	) {
		this.left = left;
		this.right = right;
		this.table = table;
		this.alignments = alignments;
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

	public double getScore() {
		return table.get(table.rows() - 1, table.columns() - 1);
	}
}

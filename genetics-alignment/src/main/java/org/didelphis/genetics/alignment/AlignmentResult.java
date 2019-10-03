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

import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.Table;

import java.util.Collections;
import java.util.List;

/**
 * Class {@code AlignmentResult}
 *
 * @since 0.1.0
 */
@SuppressWarnings ("AssignmentOrReturnOfFieldWithMutableType")
@FieldDefaults (makeFinal = true, level = AccessLevel.PRIVATE)
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

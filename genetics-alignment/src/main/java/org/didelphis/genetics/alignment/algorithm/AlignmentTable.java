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

package org.didelphis.genetics.alignment.algorithm;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;

import java.util.Collections;
import java.util.Set;

@ToString
@EqualsAndHashCode
@FieldDefaults (level = AccessLevel.PRIVATE, makeFinal = true)
public class AlignmentTable<T> {

	Table<Set<Operation>> operations;
	Table<Double> scores;

	Sequence<T> left;
	Sequence<T> right;

	public AlignmentTable(Sequence<T> left, Sequence<T> right) {

		int rows = left.size();
		int cols = right.size();

		this.left = left;
		this.right = right;

		Set<Operation> set = Collections.emptySet();
		operations = new RectangularTable<>(set, rows, cols);
		scores = new RectangularTable<>(0.0, rows, cols);
	}

	public void setScore(int i, int j, double value) {
		scores.set(i, j, value);
	}

	public int rows() {
		return left.size();
	}

	public int cols() {
		return right.size();
	}

	public double getScore(int i, int j) {
		return scores.get(i,j);
	}

	public Set<Operation> getOperations(int i, int j) {
		return operations.get(i,j);
	}

	public void setOperations(int i, int j, Set<Operation> opSet) {
		operations.set(i,j,opSet);
	}

	public Table<Double> getScores() {
		return scores;
	}

	public @NonNull Sequence<T> getLeft() {
		return left;
	}

	public @NonNull Sequence<T> getRight() {
		return right;
	}
}

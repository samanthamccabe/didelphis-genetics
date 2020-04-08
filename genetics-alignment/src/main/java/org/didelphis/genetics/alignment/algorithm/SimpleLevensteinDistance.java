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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;

import java.util.EnumMap;
import java.util.Map;

import static org.didelphis.genetics.alignment.algorithm.Operation.*;

@ToString
@EqualsAndHashCode
public class SimpleLevensteinDistance<T> {

	public int distance(Alignment<T> thing1, Alignment<T> thing2) {
		int m = thing1.columns();
		int n = thing2.columns();

		Table<Integer> table = new RectangularTable<>(0, m, n);

		for (int j = 1; j < n; j++) {
			table.set(0, j, j);
		}

		for (int i = 1; i < m; i++) {
			table.set(i, 0, i);

			for (int j = 1; j < n; j++) {
				Map<Operation, Integer> ops = new EnumMap<>(Operation.class);

				int v = thing1.getColumn(i).equals(thing2.getColumn(j)) ? 0 : 2;

				ops.put(SUB, table.get(i - 1, j - 1) + v);
				ops.put(DEL, table.get(i - 1, j) + 1);
				ops.put(INS, table.get(i, j - 1) + 1);

				int bestValue = ops.values()
						.stream()
						.min(Integer::compareTo)
						.orElse(0);

				table.set(i, j, bestValue);
			}
		}
		return table.get(m -1, n-1);
	}
}

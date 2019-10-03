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

package org.didelphis.genetics.alignment.operators.gap;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.didelphis.language.phonetic.sequences.Sequence;

@ToString
@EqualsAndHashCode(callSuper = true)
public class ConvexGapPenalty<T> extends AbstractGapPenalty<T> {

	private final double openPenalty;
	private final double extensionPenalty;

	public ConvexGapPenalty(@NonNull Sequence<T> gap, double openPenalty, double extensionPenalty) {
		super(gap);
		this.openPenalty = openPenalty;
		this.extensionPenalty = extensionPenalty;
	}

	@Override
	public double applyAsDouble(int value) {
		return value == 0 ? openPenalty : extensionPenalty;
	}
}

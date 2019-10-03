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

package org.didelphis.genetic.data.generation;

public final class Correspondence {

	private final String leftSymbol;
	private final String rightSymbol;
	private final double score;
	
	public Correspondence(String left, String right, double score) {
		leftSymbol = left;
		rightSymbol = right;
		this.score = score;
	}

	public String getLeftSymbol() {
		return leftSymbol;
	}

	public String getRightSymbol() {
		return rightSymbol;
	}

	public double getScore() {
		return score;
	}
	
	@Override
	public String toString() {
		return leftSymbol + '\t' + rightSymbol + '\t' + score;
	}
}

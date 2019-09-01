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

package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.language.phonetic.segments.Segment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Samantha Fiona McCabe
 * Created: 4/10/2016
 */
public class PairwiseCorrespondence<T> {

	private final Segment<T> leftSegment;
	private final Segment<T> rightSegment;

	private final List<Context> leftContexts;
	private final List<Context> rightContexts;

	public PairwiseCorrespondence(Segment<T> leftSegment,
			Segment<T> rightSegment) {
		this.leftSegment = leftSegment;
		this.rightSegment = rightSegment;

		leftContexts = new ArrayList<>();
		rightContexts = new ArrayList<>();
	}

	public void addConext(List<Context> leftContext,
			List<Context> rightContext) {
	}


}

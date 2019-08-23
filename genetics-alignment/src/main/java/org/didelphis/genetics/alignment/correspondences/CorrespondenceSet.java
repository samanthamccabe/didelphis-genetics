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
import org.didelphis.genetics.alignment.Alignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/8/2015
 */
public class CorrespondenceSet<T> {

	private final Map<List<T>, ContextSet> countMap;

	public CorrespondenceSet() {
		countMap = new HashMap<>();
	}

	public void addContext(List<T> list, Alignment<Integer> preContext,
			Alignment<Integer> postContext) {
		ContextSet contextSet;
		if (countMap.containsKey(list)) {
			contextSet = countMap.get(list);
		} else {
			contextSet = new ContextSet();
			countMap.put(list, contextSet);
		}
		contextSet.increment();
		contextSet.addContext(preContext, postContext);
	}

	public boolean contains(List<T> list) {
		return countMap.containsKey(list);
	}


	public ContextSet get(List<T> list) {
		return countMap.get(list);
	}


	public ContextSet put(List<T> list, ContextSet value) {
		return countMap.put(list, value);
	}


	public ContextSet remove(List<Segment<T>> list) {
		return countMap.remove(list);
	}

	public void addAlignment(Alignment<T> alignment) {

		if (alignment.rows() == 0) {
			return;
		}

		//		Alignment head = new Alignment(alignment.getModel());
		//		Alignment tail = new Alignment(alignment);

		//		tail.removeFirst(); // throw away the #

		// TODO:
		//		while (tail.getNumberColumns() > 0) {
		//			List<Sequence> sequences = tail.removeFirst();
		//			addContext(
		//					alignment.getRow(0).get(0),
		//					alignment.getRow(1).get(0),
		//					new Alignment(head),
		//					new Alignment(tail)
		//			);
		//			head.add(a);
		//		}
	}
}

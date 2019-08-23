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

import org.didelphis.genetics.alignment.Alignment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/9/2015
 */
public class ContextSet<T> {

	private final Collection<AlignmentContext<T>> environments;
	private int count;

	public ContextSet() {
		environments = new HashSet<>();
	}

	public void addContext(Alignment<T> ante, Alignment<T> post) {
		environments.add(new AlignmentContext<>(ante, post));
	}

	@Override
	public int hashCode() {
		return environments.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ContextSet)) {
			return false;
		}

		ContextSet contextSet = (ContextSet) o;
		return environments.equals(contextSet.environments);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		Iterator<AlignmentContext<T>> li = environments.iterator();
		while (li.hasNext()) {
			AlignmentContext context = li.next();
			sb.append(context);
			if (li.hasNext()) {
				sb.append('\t');
			}
		}
		return sb.toString();
	}

	public void increment() {
		count++;
	}

	public void decrement() {
		count--;
	}

	public int getCount() {
		return count;
	}
}

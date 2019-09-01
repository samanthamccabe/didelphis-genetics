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

import lombok.NonNull;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.structures.tuples.Twin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Samantha Fiona McCabe
 * Created: 8/17/2015
 */

public class AlignmentContext<T> extends Twin<Alignment<T>> {
	
	private static final int HASH_ID = 0x732a970b;
	
	public AlignmentContext(Alignment<T> pre, Alignment<T> post) {
		super(pre, post);
	}

	@Override
	public int hashCode() {
		return HASH_ID ^ super.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (!(object instanceof AlignmentContext)) {
			return false;
		}
		return super.equals(object);
	}

	@NonNull
	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		Collection<CharSequence> anteSequence = getLeft().buildPrettyAlignments();
		Collection<CharSequence> postSequence = getRight().buildPrettyAlignments();

		for (int i = 0; i < anteSequence.size(); i++) {
			sb.append(anteSequence);
			sb.append('_');
			sb.append(postSequence);
			sb.append('\n');
		}
		return sb.toString();
	}

	private static List<String> getStringList(
			Collection<Sequence<Integer>> sequences) {
		List<String> strings = new ArrayList<>(sequences.size());
		for (Sequence<Integer> sequence : sequences) {
			strings.add(sequence.toString());
		}
		return strings;
	}
}

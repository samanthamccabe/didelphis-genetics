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

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author Samantha Fiona McCabe
 * Created: 4/10/2016
 */
@ToString
@EqualsAndHashCode
public class PairCorrespondenceSet<T>
		implements Iterable<PairCorrespondenceSet.CorrespondenceEntry<T>> {
	
	private final List<CorrespondenceEntry<T>> list;
	
	public PairCorrespondenceSet() {
		list = new ArrayList<>();
	}
	
	@NonNull
	@Override
	public Iterator<CorrespondenceEntry<T>> iterator() {
		return list.iterator();
	}

	public void add(
			Sequence<T> leftSource,
			Sequence<T> rightSource,
			Segment<T> left,
			Segment<T> right,
			ContextPair<T> pair
	) {
		CorrespondenceEntry<T> entry = new CorrespondenceEntry<>();
		entry.setLeftSource(leftSource);
		entry.setRightSource(rightSource);
		entry.setLeft(left);
		entry.setRight(right);
		entry.setContextPair(pair);
		list.add(entry);
	}
	
	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	public static final class CorrespondenceEntry<T> {
		Sequence<T> leftSource;
		Sequence<T> rightSource;
		Segment<T> left;
		Segment<T> right;
		ContextPair<T> contextPair;
		
	}
}

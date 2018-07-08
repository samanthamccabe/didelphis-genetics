package org.didelphis.genetics.alignment.correspondences;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
	
	private List<CorrespondenceEntry<T>> list;
	
	public PairCorrespondenceSet() {
		list = new ArrayList<>();
	}
	
	@Override
	public @NotNull Iterator<CorrespondenceEntry<T>> iterator() {
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

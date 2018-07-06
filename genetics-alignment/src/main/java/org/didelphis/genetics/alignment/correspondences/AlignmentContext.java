package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.structures.tuples.Twin;
import org.jetbrains.annotations.NotNull;

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

	@Override
	public @NotNull String toString() {

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

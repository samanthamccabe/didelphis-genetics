package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.Alignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 8/17/2015
 */

public class AlignmentContext {

	private final Alignment<Double> ante;
	private final Alignment<Double> post;

	public AlignmentContext(Alignment<Double> a, Alignment<Double> b) {
		ante = a;
		post = b;
	}

	@Override
	public int hashCode() {
		int hash = 11;
		hash *= 31 + ante.hashCode();
		hash *= 31 + post.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (!(object instanceof AlignmentContext)) {
			return false;
		}
		AlignmentContext context = (AlignmentContext) object;
		return ante.equals(context.ante) && post.equals(context.post);
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		Collection<CharSequence> anteSequence = ante.buildPrettyAlignments();
		Collection<CharSequence> postSequence = post.buildPrettyAlignments();

		for (int i = 0; i < anteSequence.size(); i++) {
			sb.append(anteSequence);
			sb.append('_');
			sb.append(postSequence);
			sb.append('\n');
		}
		return sb.toString();
	}

	public Alignment<? extends Number> getAnte() {
		return ante;
	}

	public Alignment<? extends Number> getPost() {
		return post;
	}

	private static List<String> getStringList(
			Collection<Sequence<? extends Number>> sequences) {
		List<String> strings = new ArrayList<>(sequences.size());
		for (Sequence<? extends Number> sequence : sequences) {
			strings.add(sequence.toString());
		}
		return strings;
	}
}

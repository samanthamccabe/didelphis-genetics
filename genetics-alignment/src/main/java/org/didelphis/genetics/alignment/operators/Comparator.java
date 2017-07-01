package org.didelphis.genetics.alignment.operators;

import org.didelphis.language.phonetic.sequences.Sequence;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/22/15
 */
@FunctionalInterface
public interface Comparator<T, V> {

	V apply(Sequence<T> left, Sequence<T> right, int i, int j);
}

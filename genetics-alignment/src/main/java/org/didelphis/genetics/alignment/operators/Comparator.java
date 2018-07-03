package org.didelphis.genetics.alignment.operators;

import org.didelphis.language.phonetic.sequences.Sequence;
import org.jetbrains.annotations.NotNull;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/22/15
 */
@FunctionalInterface
public interface Comparator<T> {

	double apply(@NotNull Sequence<T> left,@NotNull Sequence<T> right, int i, int j);
}

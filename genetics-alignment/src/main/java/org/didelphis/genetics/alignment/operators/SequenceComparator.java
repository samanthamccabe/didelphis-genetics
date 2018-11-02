package org.didelphis.genetics.alignment.operators;

import lombok.NonNull;
import org.didelphis.language.phonetic.sequences.Sequence;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/22/15
 */
@FunctionalInterface
public interface SequenceComparator<T> {

	double apply(@NonNull Sequence<T> left,@NonNull Sequence<T> right, int i, int j);
}

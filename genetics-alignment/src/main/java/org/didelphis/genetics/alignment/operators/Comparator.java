package org.didelphis.genetics.alignment.operators;

import java.util.function.BiFunction;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 5/22/15
 */
@FunctionalInterface
public interface Comparator<T> extends BiFunction<T, T, Double> {
}

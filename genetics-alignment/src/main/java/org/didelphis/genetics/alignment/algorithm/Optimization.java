package org.didelphis.genetics.alignment.algorithm;

import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

/**
 * Class {@code Optimization}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0
 * 	Date: 2017-07-23
 */
public interface Optimization<T> extends BinaryOperator<T>,
		BiPredicate<T, T> {

	T defaultValue();

	@Override
	default T apply(T t1, T t2) {
		return test(t1, t2) ? t1 : t2;
	}
}

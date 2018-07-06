package org.didelphis.genetics.alignment.algorithm;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;
/**
 * Class {@code Optimization}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-07-05
 */
@ToString
@EqualsAndHashCode
public final class BaseOptimization<T> implements Optimization<T> {

	public static final Optimization<Double> MAX = new BaseOptimization<>(
			(x, y) -> x > y, Double.MIN_VALUE
	);

	public static final Optimization<Double> MIN = new BaseOptimization<>(
			(x, y) -> x < y, Double.MAX_VALUE
	);

	private final BiPredicate<T, T> predicate;
	private final T defaultValue;

	/**
	 * Creates a basic {@link Optimization} using the provided values
	 * @param predicate a {@link BiPredicate}; tests if the first parameter is
	 * more optimal than the second, the meaning of "optimal" being dependant on
	 * the optimization's implementation
	 * @param defaultValue a default value to use
	 */
	public BaseOptimization(
			@NotNull BiPredicate<T, T> predicate,
			@NotNull T defaultValue
	) {
		this.predicate = predicate;
		this.defaultValue = defaultValue;
	}

	@Override
	public boolean test(T x, T y) {
		return predicate.test(x, y);
	}

	@Override
	public T defaultValue() {
		return defaultValue;
	}

}

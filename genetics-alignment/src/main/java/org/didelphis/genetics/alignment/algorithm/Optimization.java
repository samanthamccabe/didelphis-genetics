package org.didelphis.genetics.alignment.algorithm;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.MIN_VALUE;

/**
 * Class {@code Optimization}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-07-05
 */
public enum Optimization implements BinaryOperator<Double> {
	MAX((x,y) -> x > y, Constants.MAX_BY, MIN_VALUE),
	MIN((x,y) -> x < y, Constants.MIN_BY, MAX_VALUE),
	GEQ((x,y) -> x >= y, Constants.MAX_BY, MIN_VALUE),
	LEQ((x,y) -> x <= y, Constants.MIN_BY, MAX_VALUE);


	private final BiFunction<Double, Double, Boolean> function;
	private final BinaryOperator<Double> reducer;
	private final Double defaultValue;

	Optimization(BiFunction<Double,Double, Boolean> function,
			BinaryOperator<Double> reducer, Double defaultValue) {

		this.function = function;
		this.reducer = reducer;
		this.defaultValue = defaultValue;
	}

	public boolean test(Double x, Double y) {
		return function.apply(x,y);
	}

	@Override
	public Double apply(Double x, Double y) {
		return reducer.apply(x,y);
	}

	public Double getDefaultValue() {
		return defaultValue;
	}

	private static final class Constants {
		private static final BinaryOperator<Double> MAX_BY
				= BinaryOperator.maxBy(Double::compare);
		private static final BinaryOperator<Double> MIN_BY
				= BinaryOperator.minBy(Double::compare);
	}
}

/******************************************************************************
 * General components for language modeling and analysis                      *
 *                                                                            *
 * Copyright (C) 2014-2019 Samantha F McCabe                                  *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.     *
 *                                                                            *
 ******************************************************************************/

package org.didelphis.genetics.alignment.algorithm.optimization;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.function.BiPredicate;
/**
 * Class {@code Optimization}
 *
 * @since 0.1.0
 */
@ToString
@EqualsAndHashCode
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class BaseOptimization implements Optimization {

	public static final BaseOptimization MAX = new BaseOptimization(
			(x, y) -> x > y, Double.MIN_VALUE
	);

	public static final BaseOptimization MIN = new BaseOptimization(
			(x, y) -> x < y, Double.MAX_VALUE
	);

	BiPredicate<? super Double, ? super Double> predicate;
	double defaultValue;

	public static BaseOptimization byName(String name) {
		if (name.equals("MAX")) return MAX;
		if (name.equals("MIN")) return MIN;
		throw new IllegalArgumentException("Unknown optimization name " + name);
	}

	/**
	 * Creates a basic {@link Optimization} using the provided values
	 * @param predicate a {@link BiPredicate}; tests if the first parameter is
	 * more optimal than the second, the meaning of "optimal" being dependant on
	 * the optimization's implementation
	 * @param defaultValue a default value to use
	 */
	public BaseOptimization(
			@NonNull BiPredicate<? super Double, ? super Double> predicate,
			double defaultValue
	) {
		this.predicate    = predicate;
		this.defaultValue = defaultValue;
	}

	@Override
	public boolean test(Double x, Double y) {
		return predicate.test(x, y);
	}

	@Override
	public double defaultValue() {
		return defaultValue;
	}

}

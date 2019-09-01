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

import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

/**
 * Class {@code Optimization}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0
 * 	Date: 2017-07-23
 */
public interface Optimization extends BinaryOperator<Double>,
		BiPredicate<Double, Double> {

	double defaultValue();

	@Override
	default Double apply(Double t1, Double t2) {
		return test(t1, t2) ? t1 : t2;
	}
}

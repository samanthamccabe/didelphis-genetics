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

package org.didelphis.genetics.alignment.common;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Class {@code StringTransformer}
 *
 * @since 0.1.0
 */
@ToString
@EqualsAndHashCode
public class StringTransformer implements UnaryOperator<String> {

	private static final Pattern OPERATOR = Pattern.compile("\\s>>\\s");
	private static final Pattern NEWLINE = Pattern.compile("\r\n?|\n");
	private final Collection<Expression> expressions;

	public StringTransformer() {
		expressions = Collections.emptyList();
	}

	public StringTransformer(Iterable<String> lines) {
		expressions = new ArrayList<>();
		for (String string : lines) {
			String[] split = OPERATOR.split(string, -1);
			expressions.add(new Expression(split[0], split[1]));
		}
	}
	
	public StringTransformer(CharSequence payload) {
		expressions = new ArrayList<>();
		for (String string : NEWLINE.split(payload)) {
			String[] split = OPERATOR.split(string, -1);
			expressions.add(new Expression(split[0], split[1]));
		}
	}

	@Override
	public String apply(String string) {
		String transformed = string;
		for (Expression expression : expressions) {
			transformed = expression.apply(transformed);
		}
		return transformed;
	}

	private static final class Expression {

		private final Pattern pattern;
		private final String replacement;

		private Expression(String pattern, String replacement) {
			this.pattern = Pattern.compile(pattern.trim());
			this.replacement = replacement.trim();
		}

		private String apply(CharSequence input) {
			return pattern.matcher(input).replaceAll(replacement);
		}
	}
}

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
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-06-27
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

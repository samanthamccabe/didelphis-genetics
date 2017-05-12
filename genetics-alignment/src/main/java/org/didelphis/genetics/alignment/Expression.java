package org.didelphis.genetics.alignment;

import java.util.regex.Pattern;

/**
 * Created by samantha on 9/7/15.
 */
public class Expression {

	private final Pattern pattern;
	private final String replacement;

	public Expression(String pattern, String replacement) {
		this.pattern = Pattern.compile(pattern);
		this.replacement = replacement;
	}

	public String apply(CharSequence input) {
		return pattern.matcher(input).replaceAll(replacement);
	}

	@Override
	public String toString() {
		return "Expression{" + "pattern=" + pattern + ", replacement='" +
				replacement + '\'' + '}';
	}
}

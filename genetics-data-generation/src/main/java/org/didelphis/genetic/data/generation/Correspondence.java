package org.didelphis.genetic.data.generation;

/**
 * Created by samantha on 4/24/17.
 */
public final class Correspondence {

	private final String leftSymbol;
	private final String rightSymbol;
	private final int numberOfGenera;
	private final double percentage;
	
	public Correspondence(String left, String right, int genera, double percentage) {
		leftSymbol = left;
		rightSymbol = right;
		numberOfGenera = genera;
		this.percentage = percentage;
	}

	public String getLeftSymbol() {
		return leftSymbol;
	}

	public String getRightSymbol() {
		return rightSymbol;
	}

	public int getNumberOfGenera() {
		return numberOfGenera;
	}

	public double getPercentage() {
		return percentage;
	}
	
	@Override
	public String toString() {
		return leftSymbol + '\t' + rightSymbol +
		       '\t' + numberOfGenera +
		       '\t' + percentage;
	}
}

package org.didelphis.genetic.data.generation;

/**
 * Created by samantha on 4/24/17.
 */
public final class Correspondence {

	private final String leftSymbol;
	private final String rightSymbol;
	private final double score;
	
	public Correspondence(String left, String right, double score) {
		leftSymbol = left;
		rightSymbol = right;
		this.score = score;
	}

	public String getLeftSymbol() {
		return leftSymbol;
	}

	public String getRightSymbol() {
		return rightSymbol;
	}

	public double getScore() {
		return score;
	}
	
	@Override
	public String toString() {
		return leftSymbol + '\t' + rightSymbol + '\t' + score;
	}
}

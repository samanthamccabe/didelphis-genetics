
package org.didelphis.genetics.learning;

import org.jenetics.BitChromosome;
import org.jenetics.BitGene;
import org.jenetics.Genotype;
import org.jenetics.Mutator;
import org.jenetics.Phenotype;
import org.jenetics.RouletteWheelSelector;
import org.jenetics.SinglePointCrossover;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionStatistics;

import static org.jenetics.engine.EvolutionResult.toBestPhenotype;
import static org.jenetics.engine.limit.bySteadyFitness;

/**
 * Example code from Jenetics manual
 */
public final class OnesCountingExample {

	public static void main(String[] args) {

		Engine<BitGene, Integer> engine = Engine.builder(
				OnesCountingExample::count,
				BitChromosome.of(20, 0.15))
				.populationSize(500)
				.selector(new RouletteWheelSelector<>())
				.alterers(
						new Mutator<>(0.55),
						new SinglePointCrossover<>(0.06)
				)
				.build();

		EvolutionStatistics<Integer, ?> stats = EvolutionStatistics.ofNumber();

		Phenotype<BitGene, Integer> best = engine.stream()
				.limit(bySteadyFitness(7))
				.limit(100)
				.peek(stats).collect(toBestPhenotype());

		System.out.println(stats);
		System.out.println(best);
	}

	private static Integer count(Genotype<BitGene> gt) {
		return gt.getChromosome().as(BitChromosome.class).bitCount();
	}
}

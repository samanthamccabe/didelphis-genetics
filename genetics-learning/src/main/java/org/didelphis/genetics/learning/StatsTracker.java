package org.didelphis.genetics.learning;

import org.jenetics.Gene;
import org.jenetics.Phenotype;
import org.jenetics.engine.EvolutionResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Class {@code StatsTracker}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-06-28
 */
final class StatsTracker<G extends Gene<?, G>>
		implements Consumer<EvolutionResult<G, Double>> {

	private final int interval;
	private final BufferedWriter writer;
	private final DecimalFormat formatter;

	StatsTracker(int interval, BufferedWriter writer, DecimalFormat formatter) {
		this.interval = interval;
		this.writer = writer;
		this.formatter = formatter;
	}

	@Override
	public void accept(EvolutionResult<G, Double> result) {
		if (interval < 1 || result.getGeneration() % interval == 0) {

			Double minimum = result.getWorstFitness();
			Double maximum = result.getBestFitness();
			Double average = result.getPopulation().parallelStream()
					.collect(Collectors.averagingDouble(
							Phenotype::getFitness));

			StringBuilder sb = new StringBuilder();

			sb.append(formatter.format(maximum)).append('\n');
			sb.append(formatter.format(average)).append('\n');
			sb.append(formatter.format(minimum)).append('\n');

			try {
				writer.write(sb.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

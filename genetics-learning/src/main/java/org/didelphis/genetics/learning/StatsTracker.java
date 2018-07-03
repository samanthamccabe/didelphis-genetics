package org.didelphis.genetics.learning;

import io.jenetics.Phenotype;
import io.jenetics.engine.EvolutionResult;
import lombok.ToString;
import org.didelphis.utilities.Logger;

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
@ToString
final class StatsTracker
		implements Consumer<EvolutionResult<?, Double>> {

	private static final Logger LOG = Logger.create(StatsTracker.class);
	
	private final int interval;
	private final BufferedWriter writer;
	private final DecimalFormat formatter;

	StatsTracker(int interval, BufferedWriter writer, DecimalFormat formatter) {
		this.interval = interval;
		this.writer = writer;
		this.formatter = formatter;

		try {
			writer.write("Generation\t'Maximum Fitness'\t'Mean Fitness'\t'Minimum Fitness'\n");
			writer.flush();
		} catch (IOException e) {
			LOG.error("{}",e);
		}
	}

	@Override
	public void accept(EvolutionResult<?, Double> result) {
		try {
		if (interval < 1 || result.getGeneration() % interval == 0) {
			Double minimum = result.getWorstFitness();
			Double maximum = result.getBestFitness();
			Double average = result.getPopulation().parallelStream()
					.collect(Collectors.averagingDouble(
							Phenotype::getFitness));
				writer.write(result.getGeneration()+"\t");
				writer.write(formatter.format(maximum)+ '\t');
				writer.write(formatter.format(average)+ '\t');
				writer.write(formatter.format(minimum)+ '\t');
				writer.write('\n');
				writer.flush();
			}
		} catch (IOException e) {
			LOG.error("{}",e);
		}
	}
}

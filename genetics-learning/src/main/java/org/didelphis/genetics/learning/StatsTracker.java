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

package org.didelphis.genetics.learning;

import io.jenetics.Phenotype;
import io.jenetics.engine.EvolutionResult;
import lombok.ToString;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Class {@code StatsTracker}
 *
 * @since 0.1.0
 */
@ToString
final class StatsTracker implements Consumer<EvolutionResult<?, Double>> {

	private static final Logger LOG = LogManager.getLogger(StatsTracker.class);

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

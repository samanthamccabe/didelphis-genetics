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

package org.didelphis.genetics.alignment;

import lombok.experimental.UtilityClass;

import org.didelphis.genetics.alignment.configuration.AlgorithmConfig;
import org.didelphis.genetics.alignment.configuration.ConfigObject;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.features.IntegerFeature;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@UtilityClass
public class Main {

	private final Logger LOG = LogManager.getLogger(Main.class);

	private final ObjectMapper OM      = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
	private final FileHandler  HANDLER = new DiskFileHandler("UTF-8");

	public void main(String[] args) throws IOException {

		if (args.length == 0) {
			LOG.error("You must provide a JSON configuration");
			System.exit(-1);
		}

		ConfigObject runConfig = OM.readValue(
				HANDLER.read(args[0]),
				ConfigObject.class);

		AlgorithmConfig algConfig = OM.readValue(
				HANDLER.read(args[1]),
				AlgorithmConfig.class);

	int integer = args.length < 3 ? 0 : Integer.parseInt(args[2]);

		FormatterMode mode = FormatterMode.INTELLIGENT;
		IntegerFeature type = IntegerFeature.INSTANCE;

		Processor<Integer> processor = new Processor<>(
				integer,
				type,
				mode,
				runConfig,
				algConfig
		);
		processor.process();
	}

}

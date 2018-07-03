package org.didelphis.genetics.alignment;

import org.didelphis.language.phonetic.ModelBearer;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureSpecification;

/**
 * Class {@code Dataset}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0
 * 		Date: 2017-08-02
 */
public class Dataset<T> implements ModelBearer<T> {

	public Dataset() {
	}

	@Override
	public FeatureModel<T> getFeatureModel() {
		return null;
	}

	@Override
	public FeatureSpecification getSpecification() {
		return null;
	}
}

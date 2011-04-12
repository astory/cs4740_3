package org.maltparser.core.feature;

import java.net.URL;

import org.maltparser.core.config.ConfigurationRegistry;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.spec.SpecificationModels;
import org.maltparser.core.feature.system.FeatureEngine;

/**
*
*
* @author Johan Hall
*/
public class FeatureModelManager {
	protected SpecificationModels specModels;
	protected FeatureEngine featureEngine;
	protected ConfigurationRegistry registry;
	
	public FeatureModelManager(FeatureEngine engine, ConfigurationRegistry registry) throws MaltChainedException {
		specModels = new SpecificationModels(engine.getConfiguration());
		setFeatureEngine(engine);
		setRegistry(registry);
	}
	
	public void loadSpecification(String specModelFileName) throws MaltChainedException {
		specModels.load(specModelFileName);
	}
	
	public void loadSpecification(URL specModelURL) throws MaltChainedException {
		specModels.load(specModelURL);
	}
	
	public FeatureModel getFeatureModel(String specModelURL, int specModelUrlIndex) throws MaltChainedException {
		return new FeatureModel(specModels.getSpecificationModel(specModelURL, specModelUrlIndex), registry, featureEngine);
	}
	
	public FeatureModel getFeatureModel(String specModelURL) throws MaltChainedException {
		return new FeatureModel(specModels.getSpecificationModel(specModelURL, 0), registry, featureEngine);
	}
	
	public SpecificationModels getSpecModels() {
		return specModels;
	}

	protected void setSpecModels(SpecificationModels specModel) {
		this.specModels = specModel;
	}
	
	public FeatureEngine getFeatureEngine() {
		return featureEngine;
	}

	public void setFeatureEngine(FeatureEngine featureEngine) {
		this.featureEngine = featureEngine;
	}

	public ConfigurationRegistry getRegistry() {
		return registry;
	}

	public void setRegistry(ConfigurationRegistry registry) {
		this.registry = registry;
	}

	public String toString() {
		return specModels.toString();
	}
}

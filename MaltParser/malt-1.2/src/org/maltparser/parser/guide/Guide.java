package org.maltparser.parser.guide;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModelManager;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.history.GuideHistory;
import org.maltparser.parser.history.action.GuideDecision;
/**
*
* @author Johan Hall
* @since 1.1
**/
public interface Guide {
	public enum GuideMode { TRAIN, CLASSIFY }
	
	public void addInstance(GuideDecision decision) throws MaltChainedException;
	public void finalizeSentence(DependencyStructure dependencyGraph) throws MaltChainedException;
	public void noMoreInstances() throws MaltChainedException;
	public void terminate() throws MaltChainedException;
	
	public void predict(GuideDecision decision) throws MaltChainedException;
	public boolean predictFromKBestList(GuideDecision decision) throws MaltChainedException;
	
	public DependencyParserConfig getConfiguration();
	public GuideMode getGuideMode();
	public GuideHistory getHistory();
	public FeatureModelManager getFeatureModelManager();
}

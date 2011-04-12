package org.maltparser.parser;

import org.maltparser.core.config.Configuration;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.algorithm.ParsingAlgorithm;
import org.maltparser.parser.guide.Guide;

public interface DependencyParserConfig extends Configuration {
	public void parse(DependencyStructure graph) throws MaltChainedException;
	public void oracleParse(DependencyStructure goldGraph, DependencyStructure oracleGraph) throws MaltChainedException;
	public Guide getGuide();
	public ParsingAlgorithm getParsingAlgorithm();
}

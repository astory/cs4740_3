package org.maltparser.parser.algorithm;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.history.GuideUserHistory;


/**
 * 
 * @author Joakim Nivre
 * @author Johan Hall
 * @since 1.0
*/
public interface ParsingAlgorithm {
	public DependencyStructure parse(DependencyStructure parseDependencyGraph) throws MaltChainedException;
	public DependencyStructure oracleParse(DependencyStructure goldDependencyGraph, DependencyStructure parseDependencyGraph) throws MaltChainedException;
	public GuideUserHistory getHistory();
	public DependencyParserConfig getConfiguration();
	public String getName();
	public DependencyNode getLeftTarget();
	public DependencyNode getRightTarget();
}
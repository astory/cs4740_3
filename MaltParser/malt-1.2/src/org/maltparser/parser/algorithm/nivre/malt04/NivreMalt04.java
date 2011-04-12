package org.maltparser.parser.algorithm.nivre.malt04;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.SingleMalt;
import org.maltparser.parser.algorithm.nivre.Nivre;

/**
 * 
 * @author Joakim Nivre
 * @author Johan Hall
 * @since 1.0
*/
public abstract class NivreMalt04 extends Nivre {
	protected SymbolTable deprel;
	protected boolean inPostProcessingMode = false;
	

	public NivreMalt04(SingleMalt configuration) throws MaltChainedException {
		super(configuration);
	}
	
	public DependencyStructure parse(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		clear(parseDependencyGraph);
		inPostProcessingMode = false;
		while (!input.isEmpty()) {
			currentAction = history.getEmptyGuideUserAction();
			if (rootHandling != NORMAL && stack.peek().isRoot()) {
				updateActionContainers(SHIFT, null);
			} else {
				configuration.predict(currentAction); 
			}
			transition(parseDependencyGraph);
		}

		if (postProcessing == true) {
			input.clear();
			inPostProcessingMode = true;
			
			for (int i = parseDependencyGraph.getHighestTokenIndex(); i > 0; i--) {
				DependencyNode node = parseDependencyGraph.getDependencyNode(i);
				if (node != null) { 
					input.push(node);
				}
			}
			int last = input.size();
			for (int i = 1; i < stack.size(); i++) {
				if (!stack.get(i).hasHead() || stack.get(i).getHead().isRoot()) {
					input.set(--last, stack.get(i));
				} 
			}
			stack.clear();
			stack.push(parseDependencyGraph.getDependencyRoot());
			while (!input.isEmpty() && input.size() > last) {
				currentAction = history.getEmptyGuideUserAction();
				if (rootHandling != NORMAL && stack.peek().isRoot()) {
					updateActionContainers(SHIFT, null);
				} else {
					configuration.predict(currentAction); 
				}
				transition(parseDependencyGraph);
			}
		}
		parseDependencyGraph.linkAllTreesToRoot();
		return parseDependencyGraph;
	}
	
	public DependencyStructure oracleParse(DependencyStructure goldDependencyGraph, DependencyStructure parseDependencyGraph) throws MaltChainedException {
//		if (!(goldDependencyGraph instanceof SingleHeadedDependencyGraph)) {
//			throw new ParsingException("The gold standard graph must be a single headed graph. ");
//		}
		inPostProcessingMode = false;
		clear(parseDependencyGraph);
		while (!input.isEmpty()) {
			currentAction = history.getEmptyGuideUserAction();
			if (rootHandling != NORMAL && stack.peek().isRoot()) {
				updateActionContainers(SHIFT, null);
			} else {
				oraclePredict(goldDependencyGraph, parseDependencyGraph);
				configuration.setInstance(currentAction);
			}
			transition(parseDependencyGraph);
		}
		if (postProcessing == true) {
			input.clear();
			inPostProcessingMode = true;
			for (int i = parseDependencyGraph.getHighestTokenIndex(); i > 0; i--) {
				DependencyNode node = parseDependencyGraph.getDependencyNode(i);
				if (node != null) { 
					input.push(node);
				}
			}

			int last = input.size();
			for (int i = 1; i < stack.size(); i++) {
				if (!stack.get(i).hasHead() || stack.get(i).getHead().isRoot()) {
					input.set(--last, stack.get(i));
				} 
			}
			stack.clear();
			stack.push(parseDependencyGraph.getDependencyRoot());
			while (!input.isEmpty() && input.size() > last) {
				currentAction = history.getEmptyGuideUserAction();
				if (rootHandling != NORMAL && stack.peek().isRoot()) {
					updateActionContainers(SHIFT, null);
				} else {
					oraclePredict(goldDependencyGraph, parseDependencyGraph);
					configuration.setInstance(currentAction);
				}
				transition(parseDependencyGraph);
			}
		}
		parseDependencyGraph.linkAllTreesToRoot();
		return parseDependencyGraph;
	}
	
	protected abstract void transition(DependencyStructure dg) throws MaltChainedException; 
	protected abstract boolean checkParserAction(DependencyStructure dg) throws MaltChainedException;
	protected abstract void oraclePredict(DependencyStructure gold, DependencyStructure parseDependencyGraph) throws MaltChainedException;
	public abstract String getName();

}


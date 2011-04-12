package org.maltparser.parser.algorithm.nivre.malt04;


import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.LabelSet;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.parser.SingleMalt;
import org.maltparser.parser.algorithm.helper.TransitionTable;
import org.maltparser.parser.history.container.ActionContainer;

/**
 * 
 * @author Joakim Nivre
 * @author Johan Hall
 * @since 1.0
*/
public class NivreEagerMalt04 extends NivreMalt04 {
	protected static final int REDUCE = 2;
	protected static final int RIGHTARC = 3;
	protected static final int LEFTARC = 4;
	
	public NivreEagerMalt04(SingleMalt configuration) throws MaltChainedException {
		super(configuration);
	}
	
	protected void transition(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		currentAction.getAction(actionContainers);
		if (checkParserAction(parseDependencyGraph) == false) {
			updateActionContainers(SHIFT, null);//default parser action
		}
		Edge e = null;
		switch (transActionContainer.getActionCode()) {
		case LEFTARC:
			e = parseDependencyGraph.addDependencyEdge(input.peek().getIndex(), stack.peek().getIndex());
			addEdgeLabels(e);
			stack.pop();
			break;
		case RIGHTARC:
			if (!(inPostProcessingMode == true && !stack.peek().isRoot()
					&& parseDependencyGraph.getDefaultRootEdgeLabelCode(deprel) == getArcLabels(parseDependencyGraph).get(deprel).intValue()
					)) {
				e = parseDependencyGraph.addDependencyEdge(stack.peek().getIndex(), input.peek().getIndex());
				addEdgeLabels(e);
			}
			stack.push(input.pop());
			break;
		case REDUCE:
			stack.pop();
			break;
		default:
			stack.push(input.pop()); // SHIFT
			break;
		}
	}
	
	protected boolean checkParserAction(DependencyStructure dg) throws MaltChainedException {
		int trans = transActionContainer.getActionCode();
		if ((trans == LEFTARC || trans == RIGHTARC) && !isActionContainersLabeled()) {
			// LabelCode cannot be found for transition LEFTARC and RIGHTARC
			return false;
		}
		/* if ((trans == SHIFT || trans == REDUCE) && parserAction.getLastLabelCode() != null) {
		    // LabelCode can be found for transition SHIFT and REDUCE 
			return false;
		}*/
		if ((trans == LEFTARC || trans == REDUCE) && stack.peek().isRoot()) { 
			// The token on top of the stack is the root for LEFTARC and REDUCE
			return false;
		}
		if (trans == LEFTARC && stack.peek().hasHead()) { 
			// The token on top of the stack has already a head for transition LEFTARC
			return false;
		}
		if (trans == REDUCE && !stack.peek().hasHead() && rootHandling == STRICT) {
			// The token on top of the stack have not a head for transition REDUCE
			return false;
		}
		return true;
	}

	
	protected void oraclePredict(DependencyStructure gold, DependencyStructure parseDependencyGraph) throws MaltChainedException {
		if (!stack.peek().isRoot() && gold.getTokenNode(stack.peek().getIndex()).getHead().getIndex() == input.peek().getIndex()) {
			updateActionContainers(LEFTARC, gold.getTokenNode(stack.peek().getIndex()).getHeadEdge().getLabelSet());
		} else if (gold.getTokenNode(input.peek().getIndex()).getHead().getIndex() == stack.peek().getIndex()) {
			updateActionContainers(RIGHTARC, gold.getTokenNode(input.peek().getIndex()).getHeadEdge().getLabelSet());
		} else if (gold.getTokenNode(input.peek().getIndex()).hasLeftDependent() &&
				gold.getTokenNode(input.peek().getIndex()).getLeftmostDependent().getIndex() < stack.peek().getIndex()) {
			updateActionContainers(REDUCE, null);	
		} else if (gold.getTokenNode(input.peek().getIndex()).getHead().getIndex() < stack.peek().getIndex() 
				&& (!(gold.getTokenNode(input.peek().getIndex()).getHeadEdgeLabelCode(deprel) == parseDependencyGraph.getDefaultRootEdgeLabelCode(deprel))) 
						) {
			updateActionContainers(REDUCE, null);
		} else {
			updateActionContainers(SHIFT, null);
		}
	}

	public String getName() {
		return "nivreeager";
	}
	
	
	protected int getTransition() {
		return transActionContainer.getActionCode();
	}
	
	protected void updateActionContainers(int transition, LabelSet arcLabels) throws MaltChainedException {
		transActionContainer.setAction(transition);
		if (arcLabels == null) {
			for (ActionContainer container : arcLabelActionContainers) {
				container.setAction(-1);
			}
		} else {
			for (ActionContainer container : arcLabelActionContainers) {
				container.setAction(arcLabels.get(container.getTable()).shortValue());
			}		
		}
		currentAction.addAction(actionContainers);
	}
	
	protected void addAvailableTransitionToTable(TransitionTable ttable) throws MaltChainedException {
		ttable.addTransition(SHIFT, "SH", false, null);
		ttable.addTransition(REDUCE, "RE", false, null);
		ttable.addTransition(RIGHTARC, "RA", true, null);
		ttable.addTransition(LEFTARC, "LA", true, null);
	}
	
	protected void initWithDefaultTransitions() throws MaltChainedException {
		addTransition(transActionContainer, currentAction, SHIFT);
		addTransition(transActionContainer, currentAction, REDUCE);
		deprel = configuration.getSymbolTables().getSymbolTable("DEPREL");
		for (int i = 1; i < deprel.getValueCounter(); i++) {
			if (!getConfiguration().getOptionValue("graph", "root_label").toString().equals(deprel.getSymbolCodeToString(i))) {
				actionContainers.get(0).setAction(RIGHTARC);
				actionContainers.get(1).setAction(i);
				currentAction.addAction(actionContainers);
			}
		}
		for (int i = 1; i < deprel.getValueCounter(); i++) {
			if (!getConfiguration().getOptionValue("graph", "root_label").toString().equals(deprel.getSymbolCodeToString(i))) {
				actionContainers.get(0).setAction(LEFTARC);
				actionContainers.get(1).setAction(i);
				currentAction.addAction(actionContainers);
			}
		}
	}
}


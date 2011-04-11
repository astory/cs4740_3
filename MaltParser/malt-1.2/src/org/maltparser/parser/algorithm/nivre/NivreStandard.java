package org.maltparser.parser.algorithm.nivre;


import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.LabelSet;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.parser.SingleMalt;
import org.maltparser.parser.algorithm.ParsingException;
import org.maltparser.parser.algorithm.helper.TransitionTable;
import org.maltparser.parser.history.container.ActionContainer;

/**
 * 
 * @author Joakim Nivre
 * @author Johan Hall
 * @since 1.0
*/
public class NivreStandard extends Nivre {
	protected static final int RIGHTARC = 2;
	protected static final int LEFTARC = 3;
	
	public NivreStandard(SingleMalt configuration) throws MaltChainedException {
		super(configuration);
	}
	
	protected void transition(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		currentAction.getAction(actionContainers);
		while (checkParserAction(parseDependencyGraph) == false) {
			if (configuration.getMode() == SingleMalt.LEARN || configuration.predictFromKBestList(currentAction) == false) {
				updateActionContainers(SHIFT, null); // default parser action
				break;
			}
			currentAction.getAction(actionContainers);
		}
		Edge e = null;
		switch (getTransition()) {
		case LEFTARC:
			e = parseDependencyGraph.addDependencyEdge(input.peek().getIndex(), stack.peek().getIndex());
			addEdgeLabels(e);
			stack.pop();
			break;
		case RIGHTARC:
			e = parseDependencyGraph.addDependencyEdge(stack.peek().getIndex(), input.peek().getIndex());
			addEdgeLabels(e);
			input.pop();
			if (!stack.peek().isRoot()) {
				input.push(stack.pop());	
			}
			break;
		default:
			stack.push(input.pop()); // SHIFT
			break;
		}
	}
	
	protected boolean checkParserAction(DependencyStructure dg) throws MaltChainedException {
		int trans = getTransition();
		if ((trans == LEFTARC || trans == RIGHTARC) && !isActionContainersLabeled()) {
			// Label code is null for transitions LEFTARC and RIGHTARC
			return false;
		}
		/* if (trans == SHIFT && parserAction.getLastLabelCode() != null) {
			// Label code is not null for transition SHIFT
		}*/
		if (trans == LEFTARC && stack.peek().isRoot()) { 
			// The token on top of the stack is root for transition LEFTARC
			return false;
		}
		return true;
	}

	
	protected void oraclePredict(DependencyStructure gold, DependencyStructure parseDependencyGraph) throws MaltChainedException {
		if (!stack.peek().isRoot() && gold.getTokenNode(stack.peek().getIndex()).getHead().getIndex() == input.peek().getIndex()) {
			updateActionContainers(LEFTARC, gold.getTokenNode(stack.peek().getIndex()).getHeadEdge().getLabelSet());
		} else if (gold.getTokenNode(input.peek().getIndex()).getHead().getIndex() == stack.peek().getIndex() && checkRightDependent(gold, parseDependencyGraph)) {
			updateActionContainers(RIGHTARC, gold.getTokenNode(input.peek().getIndex()).getHeadEdge().getLabelSet());
		} else {
			updateActionContainers(SHIFT, null);
		}
	}

	private boolean checkRightDependent(DependencyStructure gold, DependencyStructure parseDependencyGraph) throws MaltChainedException {
		if (gold.getTokenNode(input.peek().getIndex()).getRightmostDependent() == null) {
			return true;
		} else if (parseDependencyGraph.getTokenNode(input.peek().getIndex()).getRightmostDependent() != null) {
			if (gold.getTokenNode(input.peek().getIndex()).getRightmostDependent().getIndex() == parseDependencyGraph.getTokenNode(input.peek().getIndex()).getRightmostDependent().getIndex()) {
				return true;
			}
		}
		return false;
	}

	
	protected int getTransition() {
		return transActionContainer.getActionCode();
	}
	
	protected void updateActionContainers(int transition, LabelSet arcLabels) throws MaltChainedException {
		if (complexTransition) {
			switch (transition) {
			case SHIFT:
				pushActionContainer.setAction(1);
				transActionContainer.setAction(transition);
				break;
			case RIGHTARC:
				pushActionContainer.setAction(1);
				transActionContainer.setAction(transition);
				break;
			case LEFTARC:
				pushActionContainer.setAction(2);
				transActionContainer.setAction(transition);
				break;
			default:
				throw new ParsingException("Unknown transition "+transition+". ");
			}
		} else { 
			transActionContainer.setAction(transition);
		}
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
		ttable.addTransition(RIGHTARC, "RA", true, null);
		ttable.addTransition(LEFTARC, "LA", true, null);
	}
	
	protected void initWithDefaultTransitions() throws MaltChainedException {
		if (!complexTransition) {
			if (transActionContainer == null) {
				throw new ParsingException("The decision settings does not contain T.TRANS or T.PUSH;T.TRANS");
			}
			addTransition(transActionContainer, currentAction, SHIFT);
		}  else {
			if (pushActionContainer == null || transActionContainer == null) {
				throw new ParsingException("The decision settings does not contain T.TRANS or T.PUSH;T.TRANS");
			}
			addTransition(transActionContainer, currentAction, SHIFT);
		}
	}
	
	public String getName() {
		return "nivrestandard";
	}
}
package org.maltparser.parser.algorithm.nivre;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.TableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.LabelSet;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.SingleMalt;
import org.maltparser.parser.algorithm.ParsingAlgorithm;
import org.maltparser.parser.algorithm.ParsingException;
import org.maltparser.parser.algorithm.helper.TransitionTable;
import org.maltparser.parser.algorithm.helper.TransitionTableHandler;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.History;
import org.maltparser.parser.history.action.GuideUserAction;
import org.maltparser.parser.history.container.ActionContainer;

/**
 * 
 * @author Joakim Nivre
 * @author Johan Hall
 * @since 1.0
*/
public abstract class Nivre implements ParsingAlgorithm {	
	// Transitions
	protected static final int SHIFT = 1;

	// Root Handling
	public static final int STRICT = 1; //root tokens unattached, Reduce not permissible
	public static final int RELAXED = 2; //root tokens unattached, Reduce permissible
	public static final int NORMAL = 3; //root tokens attached to Root with RightArc

	protected GuideUserHistory history;
	protected ArrayList<ActionContainer> actionContainers;
	protected ActionContainer transActionContainer;
	protected ActionContainer pushActionContainer;
	protected TransitionTable pushTable;
	protected HashSet<ActionContainer> arcLabelActionContainers;
	protected final SingleMalt configuration;
	protected GuideUserAction currentAction;
	protected HashMap<String, TableHandler> tableHandlers;
	protected TransitionTableHandler transitionTableHandler;
	protected int rootHandling;
	protected boolean postProcessing;
	protected final Stack<DependencyNode> stack;
	protected final Stack<DependencyNode> input;
	protected boolean complexTransition = false;
	
	
	public Nivre(SingleMalt configuration) throws MaltChainedException {
		this.configuration = configuration;
		initRootHandling();
		initPostProcessing();
		stack = new Stack<DependencyNode>();
		input = new Stack<DependencyNode>();
		initHistory();
	}
	
	
	public DependencyStructure parse(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		clear(parseDependencyGraph);
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
			while (!stack.isEmpty() && !stack.peek().isRoot()) {
				if (!stack.peek().hasHead()) {
					input.push(stack.pop());
				} else {
					stack.pop();
				}
			}
			while (!input.isEmpty()) {
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
		parseDependencyGraph.linkAllTreesToRoot();
		return parseDependencyGraph;
	}
	
	protected boolean isActionContainersLabeled() {
		for (ActionContainer container : arcLabelActionContainers) {
			if (container.getActionCode() < 0) {
				return false;
			}
		}
		return true;
	}
	
	protected void addEdgeLabels(Edge e) throws MaltChainedException {
		if (e != null) { 
			for (ActionContainer container : arcLabelActionContainers) {
				e.addLabel((SymbolTable)container.getTable(), container.getActionCode());
			}
		}
	}
	
	protected LabelSet getArcLabels(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		final LabelSet arcLabels = parseDependencyGraph.checkOutNewLabelSet();
		for (ActionContainer container : arcLabelActionContainers) {
			arcLabels.put((SymbolTable)container.getTable(), container.getActionCode());
		}
		return arcLabels;
	}
	
	public DependencyNode getStackNode(int index) throws MaltChainedException {
		if (index < 0) {
			throw new ParsingException("Stack index must be non-negative in feature specification. ");
		}
		if (stack.size()-index > 0) {
			return stack.get(stack.size()-1-index);
		}
		return null;
	}
	
	public DependencyNode getInputNode(int index) throws MaltChainedException {
		if (index < 0) {
			throw new ParsingException("Input index must be non-negative in feature specification. ");
		}
		if (input.size()-index > 0) {
			return input.get(input.size()-1-index);
		}	
		return null;
	}
	
	public DependencyNode getLeftTarget() {
		return stack.peek();
	}
	
	public DependencyNode getRightTarget() {
		return input.peek();
	}
	
	public DependencyNode getNode(String dataStructure, int index) throws MaltChainedException {
		if (dataStructure.equalsIgnoreCase("Stack")) {
			if (index < 0) {
				throw new ParsingException("Stack index must be non-negative in feature specification. ");
			}
			if (stack.size()-index > 0) {
				return stack.get(stack.size()-1-index);
			}
		} else if (dataStructure.equalsIgnoreCase("Input")) {
			if (index < 0) {
				throw new ParsingException("Input index must be non-negative in feature specification. ");
			}
			if (input.size()-index > 0) {
				return input.get(input.size()-1-index);
			}			
		} else {
			throw new ParsingException("Undefined data structure in feature specification. ");
		}
		return null;
	}

	public int getRootHandling() {
		return rootHandling;
	}

	protected void clear(DependencyStructure dg) throws MaltChainedException {
		stack.clear();
		input.clear();
		history.clear();
		stack.push(dg.getDependencyRoot());
		for (int i = dg.getHighestTokenIndex(); i > 0; i--) {
			final DependencyNode node = dg.getDependencyNode(i);
			if (node != null) { 
				input.push(node);
			}
		}		
	}
	
	protected void initRootHandling() throws MaltChainedException {
		final String rh = getConfiguration().getOptionValue("nivre", "root_handling").toString();
		if (rh.equalsIgnoreCase("strict")) {
			rootHandling = Nivre.STRICT;
		} else if (rh.equalsIgnoreCase("relaxed")) {
			rootHandling = Nivre.RELAXED;
		} else if (rh.equalsIgnoreCase("normal")) {
			rootHandling = Nivre.NORMAL;
		} else {
			throw new ParsingException("The root handling '"+rh+"' is unknown");
		}
	}
	
	protected void addTransition(ActionContainer transitionContainer, GuideUserAction action, int value) throws MaltChainedException {
		transitionContainer.setAction(value);
		for (ActionContainer container : arcLabelActionContainers) {
			container.setAction(-1);
		}
		currentAction.addAction(actionContainers);
	}
	
	protected void initPostProcessing() throws MaltChainedException {
		postProcessing = ((Boolean)getConfiguration().getOptionValue("nivre", "post_processing")).booleanValue();
	}
	
	public SingleMalt getConfiguration() {
		return configuration;
	}
	
	
	protected void initHistory() throws MaltChainedException {
		String decisionSettings = configuration.getOptionValue("guide", "decision_settings").toString().trim();
		transitionTableHandler = new TransitionTableHandler();
		tableHandlers = new HashMap<String, TableHandler>();

		final String[] decisionElements =  decisionSettings.split(",|#|;|\\+");
		
		int nTrans = 0;
		int nArc = 0;
		for (int i = 0; i < decisionElements.length; i++) {
			int index = decisionElements[i].indexOf('.');
			if (index == -1) {
				throw new ParsingException("Decision settings '"+decisionSettings+"' contain an item '"+decisionElements[i]+"' that does not follow the format {TableHandler}.{Table}. ");
			}
			if (decisionElements[i].substring(0,index).equals("T")) {
				if (!tableHandlers.containsKey("T")) {
					tableHandlers.put("T", transitionTableHandler);
				}
				if (decisionElements[i].substring(index+1).equals("TRANS")) {
					if (nTrans == 0) {
						TransitionTable ttable = (TransitionTable)transitionTableHandler.addSymbolTable("TRANS");
						addAvailableTransitionToTable(ttable);
					} else {
						throw new ParsingException("Illegal decision settings '"+decisionSettings+"'");
					}
					nTrans++;
				}  else if (decisionElements[i].substring(index+1).equals("PUSH")) {
					if (nArc == 0) {
						complexTransition = true;
						pushTable = (TransitionTable)transitionTableHandler.addSymbolTable("PUSH");
						pushTable.addTransition(1, "YES", true, null);
						pushTable.addTransition(2, "NO", true, null);
					} else {
						throw new ParsingException("Illegal decision settings '"+decisionSettings+"'");
					}
					nArc++;
				}
			} else if (decisionElements[i].substring(0,index).equals("A")) {
				if (!tableHandlers.containsKey("A")) {
					tableHandlers.put("A", configuration.getSymbolTables());
				}
			} else {
				throw new ParsingException("The decision settings '"+decisionSettings+"' contains an unknown table handler '"+decisionElements[i].substring(0,index)+"'. " +
						"Only T (Transition table handler) and A (ArcLabel table handler) is allowed for '"+getName()+"' parsing algorithm. ");
			}
		}
		
		history = new History(decisionSettings, getConfiguration().getOptionValue("guide", "classitem_separator").toString(), tableHandlers);
		actionContainers = history.getActionContainers();
		if (actionContainers.size() < 1) {
			throw new ParsingException("Problem when initialize the history (sequence of actions). There are no action containers. ");
		}
		
		for (int i = 0; i < actionContainers.size(); i++) {
			if (actionContainers.get(i).getTableContainerName().equals("T.TRANS")) {
				transActionContainer = actionContainers.get(i);
			} else if (actionContainers.get(i).getTableContainerName().equals("T.PUSH")) {
				pushActionContainer = actionContainers.get(i); 
			} else if (actionContainers.get(i).getTableContainerName().startsWith("A.")) {
				if (arcLabelActionContainers == null) {
					arcLabelActionContainers = new HashSet<ActionContainer>();
				}
				arcLabelActionContainers.add(actionContainers.get(i));
			}
		}
		currentAction = history.getEmptyGuideUserAction();
		initWithDefaultTransitions();
	}
	
	public GuideUserHistory getHistory() {
		return history;
	}
	
	protected abstract int getTransition();
	protected abstract void updateActionContainers(int transition, LabelSet arcLabels) throws MaltChainedException;
	protected abstract void transition(DependencyStructure dg) throws MaltChainedException; 
	protected abstract void addAvailableTransitionToTable(TransitionTable ttable) throws MaltChainedException;
	protected abstract void initWithDefaultTransitions() throws MaltChainedException;
	protected abstract boolean checkParserAction(DependencyStructure dg) throws MaltChainedException;
	protected abstract void oraclePredict(DependencyStructure gold, DependencyStructure parseDependencyGraph) throws MaltChainedException;
	public abstract String getName();

}


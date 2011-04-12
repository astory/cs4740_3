package org.maltparser.parser.algorithm.covington;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
import org.maltparser.parser.history.History;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.action.GuideUserAction;
import org.maltparser.parser.history.container.ActionContainer;


/**
 * 
 * @author Joakim Nivre
 * @author Johan Hall
 * @since 1.0
*/
public abstract class Covington implements ParsingAlgorithm {
	// Behavior
	public final static int MALT_0_4  = 1;
	public final static int MALT_1_0  = 2;
	
	// Transitions
	protected static final int SHIFT = 1;
	protected static final int NOARC = 2;
	protected static final int RIGHTARC = 3;
	protected static final int LEFTARC = 4;
	

	protected GuideUserHistory history;
	protected ArrayList<ActionContainer> actionContainers;
	protected ActionContainer transActionContainer;
	protected ActionContainer pushActionContainer;
	protected TransitionTable pushTable;
	protected HashSet<ActionContainer> arcLabelActionContainers;
	protected SingleMalt configuration;
	protected GuideUserAction currentAction;
	protected HashMap<String, TableHandler> tableHandlers;
	protected TransitionTableHandler transitionTableHandler;
	protected boolean allowShift;
	protected boolean complexTransition = false;
	protected int behavior;
	protected ArrayList<DependencyNode> input;
	protected int right;
	protected int left;
	protected int leftstop;
	protected int rightstop;
	
	public Covington(SingleMalt configuration) throws MaltChainedException {
		this.configuration = configuration;
		initAllowRoot();
		initAllowShift();
		initBehavior();
		input = new ArrayList<DependencyNode>();
		initHistory();
	}
	
	
	public DependencyStructure parse(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		clear(parseDependencyGraph);
		right = 1;
		while (right <= rightstop) {
			left = right -1;
			while (left >= leftstop) {
				currentAction = history.getEmptyGuideUserAction();
				configuration.predict(currentAction);
				transition(parseDependencyGraph);
			}
			right++;
		}
		
		parseDependencyGraph.linkAllTreesToRoot();
		return parseDependencyGraph;
	}
	
	public DependencyStructure oracleParse(DependencyStructure goldDependencyGraph, DependencyStructure parseDependencyGraph) throws MaltChainedException {
//		if (!(goldDependencyGraph instanceof SingleHeadedDependencyGraph)) {
//			throw new ParsingException("The gold standard graph must be a single headed graph. ");
//		}
		
		clear(parseDependencyGraph);
		right = 1;
		while (right <= rightstop) {
			left = right - 1;
			while (left >= leftstop) { 
				currentAction = history.getEmptyGuideUserAction();
				oraclePredict(goldDependencyGraph);
				configuration.setInstance(currentAction);
				transition(parseDependencyGraph);
			}
			right++;
		}
		parseDependencyGraph.linkAllTreesToRoot();
		return parseDependencyGraph;
	}
	
	protected void transition(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		currentAction.getAction(actionContainers);
		while (checkParserAction(parseDependencyGraph) == false) {
			if (configuration.getMode() == SingleMalt.LEARN || configuration.predictFromKBestList(currentAction) == false) {
				updateActionContainers(NOARC, null); // default parser action
				break;
			}
			currentAction.getAction(actionContainers);
		}
		Edge e = null;
		switch (transActionContainer.getActionCode()) {
		case LEFTARC:
			e = parseDependencyGraph.addDependencyEdge(input.get(right).getIndex(), input.get(left).getIndex());
			addEdgeLabels(e);
			break;
		case RIGHTARC:
			e = parseDependencyGraph.addDependencyEdge(input.get(left).getIndex(), input.get(right).getIndex());
			addEdgeLabels(e);
			break;
		default:
			break;
		}
		updateLeft(parseDependencyGraph, actionContainers.get(0).getActionCode());	
	}
	
	protected boolean checkParserAction(DependencyStructure dg) throws MaltChainedException {
		int trans = transActionContainer.getActionCode();
		
		if (trans == SHIFT && allowShift == false) {
			return false;
		}
		if ((trans == LEFTARC || trans == RIGHTARC) && !isActionContainersLabeled()) {
			// LabelCode cannot be found for transition LEFTARC and RIGHTARC
			return false;
		}
		/* if ((trans == SHIFT || trans == REDUCE) && parserAction.getLastLabelCode() != null) {
		    // LabelCode can be found for transition SHIFT and REDUCE 
			return false;
		}*/
		if (trans == LEFTARC && input.get(left).isRoot()) { 
			// The token on top of the stack is the root for LEFTARC
			return false;
		}
		if (trans == LEFTARC && dg.hasLabeledDependency(input.get(left).getIndex())) { 
			
			
			// The token on top of the stack has already a head for transition LEFTARC
			return false;
		}
		if (trans == RIGHTARC && dg.hasLabeledDependency(input.get(right).getIndex())) { 
			// The token on top of the stack has already a head for transition LEFTARC
			return false;
		}
		return true;
	}
	
	protected void oraclePredict(DependencyStructure gold) throws MaltChainedException {
		if (!input.get(left).isRoot() && gold.getTokenNode(input.get(left).getIndex()).getHead().getIndex() == input.get(right).getIndex()) {
			updateActionContainers(LEFTARC, gold.getTokenNode(input.get(left).getIndex()).getHeadEdge().getLabelSet());
		} else if (gold.getTokenNode(input.get(right).getIndex()).getHead().getIndex() == input.get(left).getIndex()) {
			updateActionContainers(RIGHTARC, gold.getTokenNode(input.get(right).getIndex()).getHeadEdge().getLabelSet());
		} else if (allowShift == true && (!(gold.getTokenNode(input.get(right).getIndex()).hasLeftDependent() 
				&& gold.getTokenNode(input.get(right).getIndex()).getLeftmostDependent().getIndex() < input.get(left).getIndex())
				&& !(gold.getTokenNode(input.get(right).getIndex()).getHead().getIndex() < input.get(left).getIndex() 
						&& (!gold.getTokenNode(input.get(right).getIndex()).getHead().isRoot() || leftstop == 0)))) {
			updateActionContainers(SHIFT, null);
		} else {
			updateActionContainers(NOARC, null);
		}
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
			case NOARC:
				pushActionContainer.setAction(2);
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
				container.setAction(arcLabels.get(container.getTable()).intValue());
			}		
		}
		currentAction.addAction(actionContainers);
	}
	
	protected boolean isActionContainersLabeled() {
		for (int i = 1; i < actionContainers.size(); i++) {
			if (actionContainers.get(i).getActionCode() < 0) {
				return false;
			}
		}
		return true;
	}
	
//	protected LabelSet getArcLabels(DependencyGraph parseDependencyGraph) throws MaltChainedException {
//		LabelSet arcLabels = parseDependencyGraph.checkOutNewLabelSet();
//		for (ActionContainer container : arcLabelActionContainers) {
//			arcLabels.put((SymbolTable)container.getTable(), container.getActionCode());
//		}
//		return arcLabels;
//	}
	
	protected void addEdgeLabels(Edge e) throws MaltChainedException {
		if (e != null) { 
			for (ActionContainer container : arcLabelActionContainers) {
				e.addLabel((SymbolTable)container.getTable(), container.getActionCode());
			}
		}
	}
	
	public DependencyNode getLeftNode(int index) throws MaltChainedException {
		if (index < 0) {
			throw new ParsingException("Left index must be non-negative in feature specification. ");
		}
		if (behavior == Covington.MALT_0_4) {
			int tmpindex = 0;
			int tmpleft = left;
			while (tmpindex < index && tmpleft >= 0) {
				if (input.get(tmpleft).hasHead() && input.get(tmpleft).getHead().getIndex() < tmpleft) {
					tmpleft = input.get(tmpleft).getHead().getIndex();
					tmpindex++;
				} else if (input.get(tmpleft).hasHead()) {
					tmpleft--;
				} else {
					tmpleft--;
					tmpindex++;
				}
			}
			if (tmpleft >= 0) {
				return input.get(tmpleft);
			}
		} else {
			if (left-index >= 0) {
				return input.get(left-index);
			}
		}
		return null;
	}
	
	public DependencyNode getRightNode(int index) throws MaltChainedException {
		if (index < 0) {
			throw new ParsingException("Right index must be non-negative in feature specification. ");
		}
		if (right+index < input.size()) {
			return input.get(right+index);
		}
		return null;
	}
	
	public DependencyNode getLeftContextNode(int index) throws MaltChainedException {
		if (index < 0) {
			throw new ParsingException("LeftContext index must be non-negative in feature specification. ");
		}
		
		int tmpindex = 0;
		for (int i = left+1; i < right; i++) {
			if (!input.get(i).hasAncestorInside(left, right)) {
				if (tmpindex == index) {
					return input.get(i);
				} else {
					tmpindex++;
				}
			}
		}
		return null;
	}
	
	public DependencyNode getRightContextNode(int index) throws MaltChainedException {
		if (index < 0) {
			throw new ParsingException("RightContext index must be non-negative in feature specification. ");
		}
		int tmpindex = 0;
		for (int i = right-1; i > left; i--) {
			if (!input.get(i).hasAncestorInside(left, right)) {
				if (tmpindex == index) {
					return input.get(i);
				} else {
					tmpindex++;
				}
			}
		}
		return null;
	}
	
	public DependencyNode getLeftTarget() {
		return input.get(left);
	}
	
	public DependencyNode getRightTarget() {
		return input.get(right);
	}
	
	public DependencyNode getNode(String dataStructure, int index) throws MaltChainedException {
		if (dataStructure.equals("Left")) {
			if (index < 0) {
				throw new ParsingException("Left index must be non-negative in feature specification. ");
			}
			if (behavior == Covington.MALT_0_4) {
				int tmpindex = 0;
				int tmpleft = left;
				while (tmpindex < index && tmpleft >= 0) {
					if (input.get(tmpleft).hasHead() && input.get(tmpleft).getHead().getIndex() < tmpleft) {
						tmpleft = input.get(tmpleft).getHead().getIndex();
						tmpindex++;
					} else if (input.get(tmpleft).hasHead()) {
						tmpleft--;
					} else {
						tmpleft--;
						tmpindex++;
					}
				}
				if (tmpleft >= 0) {
					return input.get(tmpleft);
				}
			} else {
				if (left-index >= 0) {
					return input.get(left-index);
				}
			}
		} else if (dataStructure.equals("Right")) {
			if (index < 0) {
				throw new ParsingException("Right index must be non-negative in feature specification. ");
			}
			if (right+index < input.size()) {
				return input.get(right+index);
			}
		} else if (dataStructure.equals("LeftContext")) {
			if (index < 0) {
				throw new ParsingException("LeftContext index must be non-negative in feature specification. ");
			}
			
			int tmpindex = 0;
			for (int i = left+1; i < right; i++) {
				if (!input.get(i).hasAncestorInside(left, right)) {
					if (tmpindex == index) {
						return input.get(i);
					} else {
						tmpindex++;
					}
				}
			}
		} else if (dataStructure.equals("RightContext")) {
			if (index < 0) {
				throw new ParsingException("RightContext index must be non-negative in feature specification. ");
			}
			int tmpindex = 0;
			for (int i = right-1; i > left; i--) {
				if (!input.get(i).hasAncestorInside(left, right)) {
					if (tmpindex == index) {
						return input.get(i);
					} else {
						tmpindex++;
					}
				}
			}
		} else {
			throw new ParsingException("Undefined data structure in feature specification. ");
		}
		return null;
	}

	private void clear(DependencyStructure dg) throws MaltChainedException {
//		dg.clear();

		input.clear();
		history.clear(); //parserAction.clear();
		
		for (int i = 0; i <= dg.getHighestTokenIndex(); i++) {
			DependencyNode node = dg.getDependencyNode(i);
			if (node != null) { 
				input.add(node);
			}
		}	

		rightstop = dg.getHighestTokenIndex();
	}
	
	private void initAllowRoot() throws MaltChainedException {
		if ((Boolean)configuration.getOptionValue("covington", "allow_root") == true) {
			leftstop = 0;
		} else {
			leftstop = 1;
		}
	}
	
	private void initAllowShift() throws MaltChainedException {
		allowShift = (Boolean)configuration.getOptionValue("covington", "allow_shift");
	}
	
	private void initBehavior() throws MaltChainedException {
		if ((Boolean)configuration.getOptionValue("malt0.4", "behavior") == true) {
			behavior = MALT_0_4;
		} else {
			behavior = MALT_1_0;
		}		
	}
	
	protected void addTransition(ActionContainer transitionContainer, GuideUserAction action, int value) throws MaltChainedException {
		transitionContainer.setAction(value);
		for (ActionContainer container : arcLabelActionContainers) {
			container.setAction(-1);
		}
		currentAction.addAction(actionContainers);
	}
	
	protected void initHistory() throws MaltChainedException {
		String decisionSettings = configuration.getOptionValue("guide", "decision_settings").toString().trim();
		transitionTableHandler = new TransitionTableHandler();
		tableHandlers = new HashMap<String, TableHandler>();

		String[] decisionElements =  decisionSettings.split(",|#|;|\\+");
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
						ttable.addTransition(SHIFT, "SH", false, null);
						ttable.addTransition(NOARC, "NA", false, null);
						ttable.addTransition(RIGHTARC, "RA", true, null);
						ttable.addTransition(LEFTARC, "LA", true, null);
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
					//tableHandlers.put("A", sentence.getDataFormatInstance().getSymbolTables());
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
		if (transActionContainer == null) {
			throw new ParsingException("The decision settings does not contain T.TRANS or T.PUSH;T.TRANS");
		} else {
			if (complexTransition && pushActionContainer == null) {
				throw new ParsingException("The decision settings does not contain T.TRANS or T.PUSH;T.TRANS");
			}
		}

		addTransition(transActionContainer, currentAction, SHIFT);
		addTransition(transActionContainer, currentAction, NOARC);

	}
	
	public GuideUserHistory getHistory() {
		return history;
	}
	
	public SingleMalt getConfiguration() {
		return configuration;
	}
	
	public abstract String getName();
	protected abstract void updateLeft(DependencyStructure dg, int trans) throws MaltChainedException;
}



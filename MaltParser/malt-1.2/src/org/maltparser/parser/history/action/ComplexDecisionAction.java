package org.maltparser.parser.history.action;

import java.util.ArrayList;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.parser.history.GuideHistory;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.HistoryException;
import org.maltparser.parser.history.History;
import org.maltparser.parser.history.container.ActionContainer;
import org.maltparser.parser.history.container.CombinedTableContainer;

/**
*
* @author Johan Hall
* @since 1.1
**/
public class ComplexDecisionAction implements GuideUserAction, MultipleDecision {
	protected History history;
	protected ArrayList<SimpleDecisionAction> decisions;
	
	public ComplexDecisionAction(History history) throws MaltChainedException {
		setHistory(history);
		initDecisions();
	}
	
	public ComplexDecisionAction(GuideHistory history) throws MaltChainedException {
		setHistory((History)history);
		initDecisions();
	}
	
	/* GuideUserAction interface */
	public void addAction(ArrayList<ActionContainer> actionContainers) throws MaltChainedException {
		if (actionContainers == null || actionContainers.size() != history.getActionTables().size()) {
			throw new HistoryException("The action containers does not exist or is not of the same size as the action table. ");
		}
		int j = 0;
		for (int i = 0, n = history.getDecisionTables().size(); i < n; i++) {
			if (history.getDecisionTables().get(i) instanceof CombinedTableContainer) {
				int nContainers = ((CombinedTableContainer)history.getDecisionTables().get(i)).getNumberContainers();
				decisions.get(i).addDecision(((CombinedTableContainer)history.getDecisionTables().get(i)).getCombinedCode(actionContainers.subList(j, j + nContainers)));
				j = j + nContainers;
			} else {
				decisions.get(i).addDecision(actionContainers.get(j).getActionCode());
				j++;
			}
		}
	}
	
	public void getAction(ArrayList<ActionContainer> actionContainers) throws MaltChainedException {
		if (actionContainers == null || actionContainers.size() != history.getActionTables().size()) {
			throw new HistoryException("The action containers does not exist or is not of the same size as the action table. ");
		}
		int j = 0;
		for (int i = 0, n=history.getDecisionTables().size(); i < n; i++) {
			if (history.getDecisionTables().get(i) instanceof CombinedTableContainer) {
				int nContainers = ((CombinedTableContainer)history.getDecisionTables().get(i)).getNumberContainers();
				((CombinedTableContainer)history.getDecisionTables().get(i)).setActionContainer(actionContainers.subList(j, j + nContainers), decisions.get(i).getDecisionCode());
				j = j + nContainers;
			} else {
				actionContainers.get(j).setAction(decisions.get(i).getDecisionCode());
				j++;
			}
		}
	}

	public int numberOfActions() {
		return history.getActionTables().size();
	}
	
	public GuideUserHistory getGuideUserHistory() {
		return (GuideUserHistory)history;
	}
	
	public void clear() {
		for (int i=0, n = decisions.size(); i < n;i++) {
			decisions.get(i).clear();
		}
	}
	
	/* MultipleDecision */
	public SingleDecision getSingleDecision(int decisionIndex) throws MaltChainedException {
		return decisions.get(decisionIndex);
	}

	/* GuideDecision */
	public int numberOfDecisions() {
		return history.getDecisionTables().size();
	}

	public GuideHistory getGuideHistory() {
		return (GuideHistory)history;
	}
	
	/* Initializer */
	protected void initDecisions() throws MaltChainedException {
		decisions = new ArrayList<SimpleDecisionAction>(history.getDecisionTables().size());
		for (int i=0, n = history.getDecisionTables().size(); i < n; i++) {
			decisions.add(new SimpleDecisionAction(history, history.getDecisionTables().get(i)));
		}
	}
	
	/* Getters and Setters */
	protected void setHistory(History history) {
		this.history = history;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0, n = decisions.size(); i < n; i++) {
			sb.append(decisions.get(i));
			sb.append(';');
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length()-1);
		}
		return sb.toString();
	}
}

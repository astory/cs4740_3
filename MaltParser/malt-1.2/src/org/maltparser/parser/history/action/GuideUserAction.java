package org.maltparser.parser.history.action;

import java.util.ArrayList;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.container.ActionContainer;
/**
*
* @author Johan Hall
* @since 1.1
**/
public interface GuideUserAction {
	public void addAction(ArrayList<ActionContainer> actionContainers) throws MaltChainedException;
	public void getAction(ArrayList<ActionContainer> actionContainers) throws MaltChainedException;
	public int numberOfActions();
	public GuideUserHistory getGuideUserHistory();
}

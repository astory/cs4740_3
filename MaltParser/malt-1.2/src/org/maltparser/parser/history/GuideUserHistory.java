package org.maltparser.parser.history;

import java.util.ArrayList;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.parser.history.action.GuideUserAction;
import org.maltparser.parser.history.container.ActionContainer;
/**
*
* @author Johan Hall
* @since 1.1
**/
public interface GuideUserHistory {
	public GuideUserAction getEmptyGuideUserAction() throws MaltChainedException; // During learning
	public ArrayList<ActionContainer> getActionContainers();
	public void clear(); 
}

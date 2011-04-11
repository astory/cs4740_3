package org.maltparser.parser.algorithm.covington;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.SingleMalt;

/**
 * 
 * @author Joakim Nivre
 * @author Johan Hall
 * @since 1.0
*/
public class CovingtonProjective extends Covington {

	public CovingtonProjective(SingleMalt configuration) throws MaltChainedException {
		super(configuration);
	}
	
	protected void updateLeft(DependencyStructure dg, int trans) throws MaltChainedException {
		if (trans == SHIFT  || trans == RIGHTARC) {
			left = leftstop - 1;
		} else if (trans == NOARC) {
			if (dg.getTokenNode(input.get(left).getIndex()) != null && dg.getTokenNode(input.get(left).getIndex()).hasHead()) {
				left = dg.getTokenNode(input.get(left).getIndex()).getHead().getIndex();
			} else {
				left = leftstop - 1;
			}
		} else { 
			left--;
			while (left >= leftstop) {
				if (input.get(right).findComponent() != input.get(left).findComponent()) {
					break;
				}
				left--;
			}
		}
	}
	
	public String getName() {
		return "covproj";
	}
}

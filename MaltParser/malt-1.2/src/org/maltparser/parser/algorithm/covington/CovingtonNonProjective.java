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
public class CovingtonNonProjective extends Covington {

	public CovingtonNonProjective(SingleMalt configuration) throws MaltChainedException {
		super(configuration);
	}
	
	protected void updateLeft(DependencyStructure dg, int trans) {
		if (trans == SHIFT) {
			left = leftstop - 1;
		} else { 
			left--;
			while (left >= leftstop) {
				if (input.get(right).findComponent().getIndex() != input.get(left).findComponent().getIndex() &&
						!(input.get(left).hasHead() && input.get(right).hasHead())) {
					break;
				}
				left--;
			}
		}
	}
	
	public String getName() {
		return "covnonproj";
	}
}
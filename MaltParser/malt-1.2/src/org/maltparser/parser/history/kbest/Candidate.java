package org.maltparser.parser.history.kbest;


/**
 * A candidate in the k-best list. 
 * 
 * @author Johan Hall
 * @since 1.1
*/
public class Candidate  {
	/**
	 * The integer representation of the predicted action
	 */
	protected int actionCode;
	
	/**
	 * Constructs a candidate object
	 */
	public Candidate() {
		reset();
	}

	/**
	 * Returns an integer representation of the predicted action
	 * 
	 * @return an integer representation of the predicted action
	 */
	public int getActionCode() {
		return actionCode;
	}

	/**
	 * Sets the integer representation of the predicted action
	 * 
	 * @param actionCode an integer representation of the predicted action
	 */
	public void setActionCode(int actionCode) {
		this.actionCode = actionCode;
	}

	
	/**
	 * Resets the candidate object
	 */
	public void reset() {
		this.actionCode = -1;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Candidate)) {
			return false;
		}
		Candidate item = (Candidate)obj;
		return (actionCode == item.getActionCode());
	}
	

	public int hashCode() {
		return 31 * 7 + actionCode;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return Integer.toString(actionCode);
	}
}


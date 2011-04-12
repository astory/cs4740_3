package org.maltparser.parser.algorithm.covington;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.function.AddressFunction;
import org.maltparser.core.feature.value.AddressValue;
import org.maltparser.parser.algorithm.ParsingAlgorithm;
import org.maltparser.parser.algorithm.ParsingException;

/**
*
* @author Johan Hall
* @since 1.1
**/
public class CovAddressFunction extends AddressFunction {
	public enum CovSubFunction {
		LEFT, RIGHT, LEFTCONTEXT, RIGHTCONTEXT
	};
	protected String subFunctionName;
	protected CovSubFunction subFunction;
	protected Covington parsingAlgorithm;
	protected int index;
	
	public CovAddressFunction(String subFunctionName, ParsingAlgorithm parsingAlgorithm) {
		super();
		setSubFunctionName(subFunctionName);
		setParsingAlgorithm((Covington)parsingAlgorithm);
	}

	/* (non-Javadoc)
	 * @see org.maltparser.core.feature.function.Function#initialize(java.lang.Object[])
	 */
	public void initialize(Object[] arguments) throws MaltChainedException {
		if (arguments.length != 1) {
			throw new ParsingException("Could not initialize "+this.getClass().getName()+": number of arguments are not correct. ");
		}
		if (!(arguments[0] instanceof Integer)) {
			throw new ParsingException("Could not initialize "+this.getClass().getName()+": the first argument is not an integer. ");
		}
		
		setIndex(((Integer)arguments[0]).intValue());
	}
	
	/* (non-Javadoc)
	 * @see org.maltparser.core.feature.function.Function#getParameterTypes()
	 */
	public Class<?>[] getParameterTypes() {
		Class<?>[] paramTypes = { java.lang.Integer.class };
		return paramTypes; 
	}
	
	/* (non-Javadoc)
	 * @see org.maltparser.core.feature.function.Function#update()
	 */
	public void update() throws MaltChainedException {
		if (subFunction == CovSubFunction.LEFT) {
			address.setAddress(parsingAlgorithm.getLeftNode(index));
		} else if (subFunction == CovSubFunction.RIGHT) {
			address.setAddress(parsingAlgorithm.getRightNode(index));
		} else if (subFunction == CovSubFunction.LEFTCONTEXT) {
			address.setAddress(parsingAlgorithm.getLeftContextNode(index));
		} else if (subFunction == CovSubFunction.RIGHTCONTEXT) {
			address.setAddress(parsingAlgorithm.getRightContextNode(index));
		} else {
			address.setAddress(null);
		}
	}
	
	/**
	 * Returns the string representation of subfunction name
	 * 
	 * @return the string representation of subfunction name
	 */
	public String getSubFunctionName() {
		return subFunctionName;
	}

	/**
	 * Sets the string representation of subFunction name
	 * 
	 * @param subFunctionName the subfunction name
	 */
	public void setSubFunctionName(String subFunctionName) {
		this.subFunctionName = subFunctionName;
		subFunction = CovSubFunction.valueOf(subFunctionName.toUpperCase());
	}
	
	/**
	 * Returns the subfunction (LEFT, RIGHT, LEFTCONTEXT, RIGHTCONTEXT)
	 * 
	 * @return the subfunction (LEFT, RIGHT, LEFTCONTEXT, RIGHTCONTEXT)
	 */
	public CovSubFunction getSubFunction() {
		return subFunction;
	}
	
	/* (non-Javadoc)
	 * @see org.maltparser.core.feature.function.AddressFunction#getAddressValue()
	 */
	public AddressValue getAddressValue() {
		return address;
	}
	
	/**
	 * Returns one of the two version of the covington parsing algorithm
	 * 
	 * @return one of the two version of the covington parsing algorithm
	 */
	public Covington getParsingAlgorithm() {
		return parsingAlgorithm;
	}

	/**
	 * Sets the parsing algorthm
	 * 
	 * @param parsingAlgorithm	a covington parsing algorithm
	 */
	public void setParsingAlgorithm(Covington parsingAlgorithm) {
		this.parsingAlgorithm = parsingAlgorithm;
	}

	/**
	 * Returns the index that is used for indexing the data structures: Left, Right, LeftContext and RightContext
	 * 
	 * @return the index that is used for indexing the data structures: Left, Right, LeftContext and RightContext
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Sets the index that is used for indexing the data structures: Left, Right, LeftContext and RightContext
	 * 
	 * @param index the index
	 */
	public void setIndex(int index) {
		this.index = index;
	}
	
	/* (non-Javadoc)
	 * @see org.maltparser.core.feature.function.AddressFunction#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof CovAddressFunction)) {
			return false;
		} else if (!parsingAlgorithm.equals(((CovAddressFunction)obj).getParsingAlgorithm())) {
			return false;
		} else if (index != ((CovAddressFunction)obj).getIndex()) {
			return false;
		} else if (!subFunction.equals(((CovAddressFunction)obj).getSubFunction())) {
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.maltparser.core.feature.function.AddressFunction#toString()
	 */
	public String toString() {
		return subFunctionName + "[" + index + "]";
	}
}

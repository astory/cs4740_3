package org.maltparser.parser.algorithm.nivre;

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
public class NivreAddressFunction extends AddressFunction {
	public enum NivreSubFunction {
		STACK, INPUT
	};
	protected String subFunctionName;
	protected NivreSubFunction subFunction;
	protected Nivre parsingAlgorithm;
	protected int index;
	
	public NivreAddressFunction(String subFunctionName, ParsingAlgorithm parsingAlgorithm) {
		super();
		setSubFunctionName(subFunctionName);
		setParsingAlgorithm((Nivre)parsingAlgorithm);
	}
	
	public void initialize(Object[] arguments) throws MaltChainedException {
		if (arguments.length != 1) {
			throw new ParsingException("Could not initialize "+this.getClass().getName()+": number of arguments are not correct. ");
		}
		if (!(arguments[0] instanceof Integer)) {
			throw new ParsingException("Could not initialize "+this.getClass().getName()+": the first argument is not an integer. ");
		}
		
		setIndex(((Integer)arguments[0]).intValue());
	}
	
	public Class<?>[] getParameterTypes() {
		Class<?>[] paramTypes = { java.lang.Integer.class };
		return paramTypes; 
	}
	
	public void update() throws MaltChainedException {
		if (subFunction == NivreSubFunction.STACK) {
			address.setAddress(parsingAlgorithm.getStackNode(index));
		} else if (subFunction == NivreSubFunction.INPUT) {
			address.setAddress(parsingAlgorithm.getInputNode(index));
		} else {
			address.setAddress(null);
		}
	}
	
	public String getSubFunctionName() {
		return subFunctionName;
	}

	public void setSubFunctionName(String subFunctionName) {
		this.subFunctionName = subFunctionName;
		subFunction = NivreSubFunction.valueOf(subFunctionName.toUpperCase());
	}
	
	public NivreSubFunction getSubFunction() {
		return subFunction;
	}
	
	public AddressValue getAddressValue() {
		return address;
	}
	
	public Nivre getParsingAlgorithm() {
		return parsingAlgorithm;
	}

	public void setParsingAlgorithm(Nivre parsingAlgorithm) {
		this.parsingAlgorithm = parsingAlgorithm;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		NivreAddressFunction other = (NivreAddressFunction) obj;
		if (index != other.index)
			return false;
		if (parsingAlgorithm == null) {
			if (other.parsingAlgorithm != null)
				return false;
		} else if (!parsingAlgorithm.equals(other.parsingAlgorithm))
			return false;
		if (subFunction == null) {
			if (other.subFunction != null)
				return false;
		} else if (!subFunction.equals(other.subFunction))
			return false;
		return true;
	}
	
	public String toString() {
		return subFunctionName + "[" + index + "]";
	}
}

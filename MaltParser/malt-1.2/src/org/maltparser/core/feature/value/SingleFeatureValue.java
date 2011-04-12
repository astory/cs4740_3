package org.maltparser.core.feature.value;

import org.maltparser.core.feature.function.Function;
/**
 *  
 *
 * @author Johan Hall
 * @since 1.0
**/
public class SingleFeatureValue extends FeatureValue {
	protected int code;
	protected String symbol;
	protected boolean known;
	
	public SingleFeatureValue(Function function) {
		super(function);
		setCode(0);
		setSymbol(null);
		setKnown(true);
	}
	
	public void reset() {
		super.reset();
		setCode(0);
		setSymbol(null);
		setKnown(true);
	}
	
	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public boolean isKnown() {
		return known;
	}

	public void setKnown(boolean known) {
		this.known = known;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof SingleFeatureValue)) {
			return false;
		} else if (!symbol.equals(((SingleFeatureValue)obj).symbol)) {
			return false;
		} else if (code != ((SingleFeatureValue)obj).code) {
			return false;
		}
		return super.equals(obj);
	}
	
	public String toString() {
		return super.toString()+ "{" + symbol + " -> " + code + ", known=" + known +"} ";
	}
}

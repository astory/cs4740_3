package org.maltparser.parser.algorithm;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureException;
import org.maltparser.core.feature.function.FeatureFunction;
import org.maltparser.core.feature.value.FeatureValue;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.io.dataformat.ColumnDescription;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.nullvalue.NullValues.NullValueId;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
/**
*
* @author Johan Hall
* @since 1.1
**/
public class InputArcFeature implements FeatureFunction {
	protected ColumnDescription column;
	protected DataFormatInstance dataFormatInstance;
	protected SymbolTableHandler tableHandler;
	protected SymbolTable table;
	protected SingleFeatureValue featureValue;
	protected ParsingAlgorithm parsingAlgorithm;
	
	public InputArcFeature(DataFormatInstance dataFormatInstance, SymbolTableHandler tableHandler, ParsingAlgorithm parsingAlgorithm) throws MaltChainedException {
		super();
		setDataFormatInstance(dataFormatInstance);
		setTableHandler(tableHandler);
		setFeatureValue(new SingleFeatureValue(this));
		setParsingAlgorithm(parsingAlgorithm);
	}
	
	public void initialize(Object[] arguments) throws MaltChainedException {
		if (arguments.length != 1) {
			throw new FeatureException("Could not initialize InputArcFeature: number of arguments are not correct. ");
		}
		if (!(arguments[0] instanceof String)) {
			throw new FeatureException("Could not initialize InputArcFeature: the first argument is not a string. ");
		}
		setColumn(dataFormatInstance.getColumnDescriptionByName((String)arguments[0]));
		setSymbolTable(tableHandler.addSymbolTable("ARC_"+column.getName(),ColumnDescription.INPUT, "one"));
		table.addSymbol("LEFT");
		table.addSymbol("RIGHT");
	}
	
	public Class<?>[] getParameterTypes() {
		Class<?>[] paramTypes = { java.lang.String.class };
		return paramTypes;
	}
	
	public int getCode(String symbol) throws MaltChainedException {
		return table.getSymbolStringToCode(symbol);
	}


	public FeatureValue getFeatureValue() {
		return featureValue;
	}


	public String getSymbol(int code) throws MaltChainedException {
		return table.getSymbolCodeToString(code);
	}


	public void updateCardinality() throws MaltChainedException {
		featureValue.setCardinality(table.getValueCounter());
	}

	public void update() throws MaltChainedException {
		DependencyNode left = parsingAlgorithm.getLeftTarget();
		DependencyNode right = parsingAlgorithm.getRightTarget();
		try {

			if (left == null || right == null) {
				featureValue.setCode(table.getNullValueCode(NullValueId.NO_NODE));
				featureValue.setSymbol(table.getNullValueSymbol(NullValueId.NO_NODE));
				featureValue.setKnown(true);
				featureValue.setNullValue(true); 
			} else {
				int lindex = Integer.parseInt(left.getLabelSymbol(column.getSymbolTable()));
				int rindex = Integer.parseInt(left.getLabelSymbol(column.getSymbolTable()));
				if (!left.isRoot() && lindex == right.getIndex()) {
					featureValue.setCode(table.getSymbolStringToCode("LEFT"));
					featureValue.setSymbol("LEFT");
					featureValue.setKnown(true);
					featureValue.setNullValue(false);
				} else if (rindex == left.getIndex()) {
					featureValue.setCode(table.getSymbolStringToCode("RIGHT"));
					featureValue.setSymbol("RIGHT");
					featureValue.setKnown(true);
					featureValue.setNullValue(false);			
				} else {
					featureValue.setCode(table.getNullValueCode(NullValueId.NO_NODE));
					featureValue.setSymbol(table.getNullValueSymbol(NullValueId.NO_NODE));
					featureValue.setKnown(true);
					featureValue.setNullValue(true);
				}
			}
		} catch (NumberFormatException e) {
			throw new FeatureException("The index of the feature must be an integer value. ", e);
		}
	}

	
	public ParsingAlgorithm getParsingAlgorithm() {
		return parsingAlgorithm;
	}

	public void setParsingAlgorithm(ParsingAlgorithm parsingAlgorithm) {
		this.parsingAlgorithm = parsingAlgorithm;
	}

	public ColumnDescription getColumn() {
		return column;
	}

	public void setColumn(ColumnDescription column) throws MaltChainedException {
		if (column.getType() != ColumnDescription.INTEGER) {
			throw new FeatureException("InputArc feature column must be of type integer. ");
		}
		this.column = column;
	}

	public DataFormatInstance getDataFormatInstance() {
		return dataFormatInstance;
	}

	public void setDataFormatInstance(DataFormatInstance dataFormatInstance) {
		this.dataFormatInstance = dataFormatInstance;
	}

	public void setFeatureValue(SingleFeatureValue featureValue) {
		this.featureValue = featureValue;
	}
	
	public SymbolTable getSymbolTable() {
		return table;
	}

	public void setSymbolTable(SymbolTable table) {
		this.table = table;
	}
	
	public SymbolTableHandler getTableHandler() {
		return tableHandler;
	}

	public void setTableHandler(SymbolTableHandler tableHandler) {
		this.tableHandler = tableHandler;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof InputArcFeature)) {
			return false;
		}
		if (!obj.toString().equals(this.toString())) {
			return false;
		}
		return true;
	}
	
	public String toString() {
		return "InputArc(" + column.getName() + ")";
	}
}

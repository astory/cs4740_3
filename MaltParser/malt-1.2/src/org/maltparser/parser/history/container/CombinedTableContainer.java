package org.maltparser.parser.history.container;

import java.util.List;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.Table;
import org.maltparser.core.symbol.TableHandler;
/**
*
* @author Johan Hall
* @since 1.1
**/
public class CombinedTableContainer extends TableContainer implements Table {
	protected TableHandler tableHandler;
	protected final char separator;
	protected List<TableContainer> containers;
	protected final StringBuilder[] cachedSymbols;
	protected final int[] cachedCodes;
	
	public CombinedTableContainer(TableHandler tableHandler, String separator, List<TableContainer> containers, char decisionSeparator) throws MaltChainedException {
		super(null, null, decisionSeparator);
		this.tableHandler = tableHandler;
		if (separator.length() > 0) {
			this.separator = separator.charAt(0);
		} else {
			this.separator = '~';
		};
		setContainers(containers);
		initSymbolTable();
		cachedSymbols = new StringBuilder[containers.size()];
		cachedCodes = new int[containers.size()];
		for (int i = 0; i < containers.size(); i++) {
			cachedCodes[i] = -1;
			cachedSymbols[i] = new StringBuilder();
		};
	}
	
	public void clearCache() {
		super.clearCache();
		for (int i = 0, n = cachedCodes.length; i < n; i++) {
			cachedCodes[i] = -1;
		}
		for (int i = 0, n = cachedSymbols.length; i < n; i++) {
			cachedSymbols[i].setLength(0);
		}
	}
	
	public int addSymbol(String value) throws MaltChainedException {
		return table.addSymbol(value);
	}

	public String getName() {
		return table.getName();
	}

	public String getSymbolCodeToString(int code)
			throws MaltChainedException {
		return table.getSymbolCodeToString(code);
	}

	public int getSymbolStringToCode(String symbol) throws MaltChainedException {
		return table.getSymbolStringToCode(symbol);
	}
	
	public int getNumberContainers() {
		return containers.size();
	}
	
	
	/* override TableContainer */
	public String getSymbol(int code) throws MaltChainedException {
		if (code < 0 && !containCode(code)) {
			clearCache();
			return null;
		}
		if (cachedCode != code) {
			clearCache();
			cachedCode = code;
			cachedSymbol.append(table.getSymbolCodeToString(cachedCode));
			split();
		}
		return cachedSymbol.toString();
	}
	
	public int getCode(String symbol) throws MaltChainedException {
		if (cachedSymbol == null || !cachedSymbol.equals(symbol)) {
			clearCache();
			cachedSymbol.append(symbol);
			cachedCode = table.getSymbolStringToCode(symbol);
			split();
		}
		return cachedCode;
	}
	
	public boolean containCode(int code) throws MaltChainedException {
		if (cachedCode != code) {
			clearCache();
			cachedSymbol.append(table.getSymbolCodeToString(code));
			if (cachedSymbol == null && cachedSymbol.length() == 0) {
				return false;
			}
			cachedCode = code;
			split();
		}
		return true;
	}
	
	public boolean containSymbol(String symbol) throws MaltChainedException {
		if (cachedSymbol == null || !cachedSymbol.equals(symbol)) {
			clearCache();
			cachedCode = table.getSymbolStringToCode(symbol);
			if (cachedCode < 0) {
				return false;
			}
			cachedSymbol.append(symbol);
			split();
		}
		return true;
	}
	
	
	
	public int getCombinedCode(List<ActionContainer> codesToCombine) throws MaltChainedException {
		boolean cachedUsed = true;
		if (containers.size() != codesToCombine.size()) {
			clearCache();
			return -1;
		}
		
		for (int i = 0, n = containers.size(); i < n; i++) {
			if (codesToCombine.get(i).getActionCode() != cachedCodes[i]) {
				cachedUsed = false;
				if (codesToCombine.get(i).getActionCode() >= 0 && containers.get(i).containCode(codesToCombine.get(i).getActionCode())) {
					cachedSymbols[i].setLength(0);
					cachedSymbols[i].append(containers.get(i).getSymbol(codesToCombine.get(i).getActionCode()));
					cachedCodes[i] = codesToCombine.get(i).getActionCode(); 
				} else {
					cachedSymbols[i].setLength(0);
					cachedCodes[i] = -1;
				}
			}
		}
	
		if (!cachedUsed) {
			cachedSymbol.setLength(0);
			for (int i = 0, n = containers.size(); i < n; i++) {
				if (cachedSymbols[i].length() != 0) {
					cachedSymbol.append(cachedSymbols[i]);
					cachedSymbol.append(separator);
				}
			}
			if (cachedSymbol.length() > 0) {
				cachedSymbol.setLength(cachedSymbol.length()-1);
			}
			if (cachedSymbol.length() > 0) {
				cachedCode = table.addSymbol(cachedSymbol.toString());
			} else {
				cachedCode = -1;
			}
		}
		return cachedCode; 
	}
	
	public void setActionContainer(List<ActionContainer> actionContainers, int decision) throws MaltChainedException {
		if (decision != cachedCode) {
			clearCache();
			if (decision != -1) {
				cachedSymbol.append(table.getSymbolCodeToString(decision));
				cachedCode = decision;
			}
			split();
		}

		for (int i = 0, n = containers.size(); i < n; i++) {
			if (cachedSymbols[i].length() != 0) {
				cachedCodes[i] = actionContainers.get(i).setAction(cachedSymbols[i].toString());
			} else {
				cachedCodes[i] = actionContainers.get(i).setAction(null);
			}
		}
	}
	
	protected void split() throws MaltChainedException {
		int j = 0;
		for (int i = 0, n = containers.size(); i < n; i++) {
			cachedSymbols[i].setLength(0);
		}
		for (int i = 0, n = cachedSymbol.length(); i < n; i++) {
			if (cachedSymbol.charAt(i) == separator) {
				j++;
			} else {
				cachedSymbols[j].append(cachedSymbol.charAt(i));
			}
		}
		for (int i = j+1, n = containers.size(); i < n; i++) {
			cachedSymbols[i].setLength(0);
		}
		for (int i = 0, n = containers.size(); i < n; i++) {
			if (cachedSymbols[i].length() != 0) {
				cachedCodes[i] = containers.get(i).getCode(cachedSymbols[i].toString());
			} else {
				cachedCodes[i] = -1;
			}
		}
	}

	public char getSeparator() {
		return separator;
	}

	public List<TableContainer> getContainers() {
		return containers;
	}
	
	
	protected void setContainers(List<TableContainer> containers) {
		this.containers = containers;
	}
	
	protected void initSymbolTable() throws MaltChainedException {
		final StringBuilder sb = new StringBuilder();

		for (TableContainer container : containers) {
			sb.append(container.getTableContainerName()+"+");
		}
		sb.setLength(sb.length()-1);
		setTable((Table)tableHandler.addSymbolTable(sb.toString())); 
		setName(sb.toString());
	}
}

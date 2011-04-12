package org.maltparser.core.syntaxgraph.reader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Iterator;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.io.dataformat.ColumnDescription;
import org.maltparser.core.io.dataformat.DataFormatException;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.Element;
import org.maltparser.core.syntaxgraph.TokenStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
/**
*
*
* @author Johan Hall
*/
public class TabReader implements SyntaxGraphReader {
	private BufferedReader reader;
	private int sentenceCount;
	private final StringBuilder input;
	private DataFormatInstance dataFormatInstance;
	private static final String IGNORE_COLUMN_SIGN = "_";
	private static final char TAB = '\t';
	private static final char NEWLINE = '\n';
	private static final char CARRIAGE_RETURN = '\r';
	
	
	public TabReader() { 
		input = new StringBuilder();
	}
	
	public void open(String fileName, String charsetName) throws MaltChainedException {
		try {
			open(new FileInputStream(fileName), charsetName);
		}catch (FileNotFoundException e) {
			throw new DataFormatException("The input file '"+fileName+"' cannot be found. ", e);
		}
	}
	
	public void open(URL url, String charsetName) throws MaltChainedException {
		try {
			open(url.openStream(), charsetName);
		} catch (IOException e) {
			throw new DataFormatException("The URL '"+url.toString()+"' cannot be opened. ", e);
		}
	}
	
	public void open(InputStream is, String charsetName) throws MaltChainedException {
		try {
			open(new InputStreamReader(is, charsetName));
		} catch (UnsupportedEncodingException e) {
			throw new DataFormatException("The character encoding set '"+charsetName+"' isn't supported. ", e);
		}
	}
	
	public void open(InputStreamReader isr) throws MaltChainedException {
		setReader(new BufferedReader(isr));
		setSentenceCount(0);
	}
	
	public void readProlog() throws MaltChainedException {
		
	}
	
	public boolean readSentence(TokenStructure syntaxGraph) throws MaltChainedException  {
		if (syntaxGraph == null || dataFormatInstance == null) {
			return false;
		}
		
		Element node = null;
		Edge edge = null;
		input.setLength(0);
		int i = 0;
		int terminalCounter = 0;
		int nNewLines = 0;

		syntaxGraph.clear();
		Iterator<ColumnDescription> columns = dataFormatInstance.iterator();
		while (true) {
			int c;

			try {
				c = reader.read();
			} catch (IOException e) {
				close();
				throw new DataFormatException("Error when reading from the input file. ", e);
			}
			if (c == TAB || c == NEWLINE || c == CARRIAGE_RETURN || c == -1) {
				if (input.length() != 0) {					
					if (i == 0) {
						terminalCounter++;
						node = syntaxGraph.addTokenNode(terminalCounter);
					}
					ColumnDescription column = null;
					if (columns.hasNext()) {
						column = columns.next();
						if (column.getCategory() == ColumnDescription.INPUT && node != null) {
							syntaxGraph.addLabel(node, column.getName(), input.toString());
						} else if (column.getCategory() == ColumnDescription.HEAD) {
							if (syntaxGraph instanceof DependencyStructure) {
								if (!input.toString().equals(IGNORE_COLUMN_SIGN)) {
									edge = ((DependencyStructure)syntaxGraph).addDependencyEdge(Integer.parseInt(input.toString()), terminalCounter);
								}
							} 
							else {
								close();
								throw new DataFormatException("The input graph is not a dependency graph and therefore it is not possible to add dependncy edges. ");
							}
						} else if (column.getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL && edge != null) {
							syntaxGraph.addLabel(edge, column.getName(), input.toString());
						}
					}
					input.setLength(0);
					nNewLines = 0;
					i++;
				}
				if (c == NEWLINE) {
					nNewLines++;
					i = 0;
					columns = dataFormatInstance.iterator();
				}
			} else {
				input.append((char)c);
			}
			
			if (nNewLines == 2 && c == NEWLINE) {
				if (syntaxGraph.hasTokens()) {
					sentenceCount++;
				}
				return true;
			} else if (c == -1) {
				if (syntaxGraph.hasTokens()) {
					sentenceCount++;
				}
				return false;					
			}
		}
	}
	
	public void readEpilog() throws MaltChainedException {
		
	}
	
	public BufferedReader getReader() {
		return reader;
	}

	public void setReader(BufferedReader reader) throws MaltChainedException {
		close();
		this.reader = reader;
	}
	
	public DataFormatInstance getDataFormatInstance() {
		return dataFormatInstance;
	}

	public void setDataFormatInstance(DataFormatInstance dataFormatInstance) {
		this.dataFormatInstance = dataFormatInstance;
	}

	public int getSentenceCount() throws MaltChainedException {
		return sentenceCount;
	}
	
	public void setSentenceCount(int sentenceCount) {
		this.sentenceCount = sentenceCount;
	}
	
	public String getOptions() {
		return null;
	}
	
	public void setOptions(String optionString) throws MaltChainedException {
		
	}
	
	public void close() throws MaltChainedException {
		try {
			if (reader != null) {
				reader.close();
				reader = null;
			}
		} catch (IOException e) {
			throw new DataFormatException("Error when closing the input file. ", e);
		} 
	}
	
	public void clear() throws MaltChainedException {
		close();
		input.setLength(0);
		dataFormatInstance = null;
		sentenceCount = 0;
	}
}

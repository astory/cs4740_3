package org.maltparser.ml.libsvm.malt04;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureException;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.feature.function.FeatureFunction;
import org.maltparser.core.feature.map.SplitFeature;
import org.maltparser.core.feature.value.FeatureValue;
import org.maltparser.core.feature.value.MultipleFeatureValue;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.helper.NoPrintStream;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.Table;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.feature.InputColumnFeature;
import org.maltparser.core.syntaxgraph.feature.OutputColumnFeature;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.ml.LearningMethod;
import org.maltparser.ml.libsvm.LibsvmException;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.algorithm.nivre.malt04.NivreEagerMalt04;
import org.maltparser.parser.algorithm.nivre.malt04.NivreStandardMalt04;
import org.maltparser.parser.guide.instance.InstanceModel;
import org.maltparser.parser.history.action.SingleDecision;

import libsvm28.svm;
import libsvm28.svm_model;
import libsvm28.svm_node;
import libsvm28.svm_parameter;
import libsvm28.svm_problem;
/**
 * Implements an interface to the LIBSVM learner (LIBSVM 2.80 is used). More information about 
 * LIBSVM can be found at <a href="http://www.csie.ntu.edu.tw/~cjlin/libsvm/" target="_blank">LIBSVM -- A Library for Support Vector Machines</a>.
 * 
 * This class tries to reproduce the same behavior as MaltParser 0.4. Unfortunately we have to introduce some strange behaviors and bugs to 
 * able to reproduce the results:
 * 
 * <ol>
 * <li>RightArc{CLASSITEM_SEPARATOR}{ROOT_LABEL} is mapped to the Reduce transition for the Nivre Arc-eager and Nivre Arc-standard algorthm, where {ROOT_LABEL} is specified
 * by the <code>--graph-root_label</code> option and the <code>--guide-classitem_separator</code> option (bug in MaltParser 0.4).
 * <li>LeftArc{CLASSITEM_SEPARATOR}{ROOT_LABEL} is mapped to the Right Arc transition with last dependency type in the DEPREL tagset, here {ROOT_LABEL} is specified
 * by the <code>--graph-root_label</code> option and the <code>--guide-classitem_separator</code> option (bug in MaltParser 0.4).
 * <li>The mapping of RightArc{CLASSITEM_SEPARATOR}{ROOT_LABEL} into Reduce results in an illegal transition and therefore the default transition (Shift) is used during parsing (indirect bug in MaltParser 0.4).
 * <li>Null-value of the LEMMA, FORM, FEATS columns in the CoNLL shared task format is not written into the instance file (this can be controlled 
 * by the <code>--libsvm-libsvm_exclude_null</code> and  <code>--libsvm-libsvm_exclude_columns</code> options in the new MaltParser)
 * <li>If feature is an output feature and <code>feature != "OutputColumn(DEPREL, Stack[0])"</code> and it points at a node which has the root as head it will not extract the dependency type of informative root label,
 * instead it will extract the root label specified by the <code>--graph-root_label</code> option (bug in MaltParser 0.4).
 * <li>If <code>feature = "Split(InputColumn(FEATS, X), \|")</code>, where <code>X</code> is arbitrary node. The set of syntactic and/or morphological features will not be ordered correctly
 * according to the LIBSVM format (bug in MaltParser 0.4).
 * <li>If <code>feature = "Split(InputColumn(FEATS, X), \|")</code>, where <code>X</code> is arbitrary node. It will not regard the set of syntactic and/or morphological features as set. In some cases, there are treebanks that does not follow the
 * CoNLL data format and have individual syntactic and/or morphological features twice in the FEATS column (bug in MaltParser 0.4). 
 * <li>Unfortunately there is minor difference between LIBSVM 2.80 (used by MaltParser 0.4) and the latest version of LIBSVM. Therefore we have to use
 * the LIBSVM 2.80 to able to reproduce the results.
 * </ol>
 * 
 * @author Johan Hall
 * @since 1.0
*/
public class LibsvmMalt04 implements LearningMethod {
	public final static String LIBSVM_VERSION = "2.80";
	private StringBuilder sb;
	/**
	 * The parent instance model
	 */
	protected InstanceModel owner;
	/**
	 * The learner/classifier mode
	 */
	protected int learnerMode;
	/**
	 * The name of the learner
	 */
	protected String name;
	/**
	 * Number of processed instances
	 */
	protected int numberOfInstances;
	/**
	 * Instance output stream writer 
	 */
	private BufferedWriter instanceOutput = null; 
	//private BufferedWriter debugTransOut = null; 
	//private int sentenceCount = 1;
	
	protected String pathExternalSVMTrain = null;
	/**
	 * LIBSVM svm_model object, only used during classification.
	 */
	private svm_model model = null;
	/**
	 * LIBSVM svm_parameter object
	 */
	private svm_parameter svmParam;
	/**
	 * Parameter string
	 */
	private String paramString;
	/**
	 * An array of LIBSVM svm_node objects, only used during classification.
	 */
	private ArrayList<svm_node> xlist = null;
	/**
	 * RA_ROOT is used for mapping RightArc_ROOT - REDUCE (bug in MaltParser 0.4)
	 */
	private String RA_ROOT = "";
	/**
	 * LA_ROOT is used for mapping RightArc_ROOT - RightArc_{Last dependency type in the DEPREL tagset} (bug in MaltParser 0.4)
	 */
	private String LA_ROOT = "";
	/**
	 * Root handling of the Nivre arc-eager and Nivre arc-standard algorithm. Used for introducing a bug in MaltParser 0.
	 */
	private int rootHandling = -1;
	/**
	 * true if Nivre arc-standard is the current parsing algorthm, otherwise false 
	 */
	private boolean nivrestandard = false;
	/**
	 * true if Nivre arc-eager/arc-standard is the current parsing algorthm, otherwise false
	 */
	private boolean nivre = false;
	
	private boolean saveInstanceFiles;
	/**
	 * Constructs a LIBSVM learner.
	 * 
	 * @param owner the guide model owner
	 * @param learnerMode the mode of the learner TRAIN or CLASSIFY
	 */
	public LibsvmMalt04(InstanceModel owner, Integer learnerMode) throws MaltChainedException {
		setOwner(owner);
		setLearningMethodName("libsvmmalt04");
		setLearnerMode(learnerMode.intValue());
		setNumberOfInstances(0);
		initSpecialParameters();
		initSvmParam(getConfiguration().getOptionValue("libsvm", "libsvm_options").toString());
		if (learnerMode == TRAIN) {
			instanceOutput = new BufferedWriter(getInstanceOutputStreamWriter(".ins"));
			//debugTransOut = new BufferedWriter(getInstanceOutputStreamWriter(".trans"));
		}
		sb = new StringBuilder(6);
	}
	
	/* (non-Javadoc)
	 * @see org.maltparser.ml.LearningMethod#addInstance(org.maltparser.parser.guide.classtable.ClassTable, org.maltparser.parser.guide.feature.FeatureVector)
	 */
	public void addInstance(SingleDecision decision, FeatureVector featureVector) throws MaltChainedException {
		if (featureVector == null) {
			throw new LibsvmException("The feature vector cannot be found");
		} else if (decision == null) {
			throw new LibsvmException("The decision cannot be found");
		}
		try {
			if (nivre == true && RA_ROOT.equals(decision.getDecisionSymbol()) == true) {
				instanceOutput.write("2\t");
				//debugTransOut.write(2+" "+classCodeTable.getCurrentClassString()+" "+sentenceCount+"\n");
			} else if (nivre == true && LA_ROOT.equals(decision.getDecisionSymbol()) == true) {
				Table table = decision.getGuideHistory().getTableHandler("A").getSymbolTable("DEPREL");
				int code = 2 + ((SymbolTable)table).getValueCounter() - 1;
				//int code = 2 + classCodeTable.getParserAction().getOutputSymbolTables().get("DEPREL").getValueCounter() - 1;
				instanceOutput.write(code+"\t");
				//debugTransOut.write(code+" "+classCodeTable.getCurrentClassString()+" "+sentenceCount+"\n");
			} else {
				instanceOutput.write(decision.getDecisionCode()+"\t");
				//debugTransOut.write(classCodeTable.getCurrentClassCode()+" "+classCodeTable.getCurrentClassString()+" "+sentenceCount+"\n");
			}
			
			for (int i = 0; i < featureVector.size(); i++) {
				FeatureValue featureValue = featureVector.get(i).getFeatureValue();
				if (featureValue.isNullValue()) {
					if (featureVector.get(i) instanceof InputColumnFeature) {
						if (((InputColumnFeature)featureVector.get(i)).getColumnName().equals("FORM") || 
							((InputColumnFeature)featureVector.get(i)).getColumnName().equals("LEMMA") ||
							((InputColumnFeature)featureVector.get(i)).getColumnName().equals("FEATS")) {
							instanceOutput.write("-1");
							if (i != featureVector.size()) {
								instanceOutput.write('\t');
							}
							continue;
						}
					} else if (featureVector.get(i) instanceof SplitFeature && ((SplitFeature)featureVector.get(i)).getParentFeature() instanceof InputColumnFeature) {
						if (((InputColumnFeature)((SplitFeature)featureVector.get(i)).getParentFeature()).getColumnName().equals("FEATS")) {
							instanceOutput.write("-1");
							if (i != featureVector.size()) {
								instanceOutput.write('\t');
							}
							continue;						
						}
					}
				}
				if (featureVector.get(i) instanceof OutputColumnFeature && !featureVector.get(i).toString().endsWith("DEPREL, Stack[0])")) {
					OutputColumnFeature ocf = (OutputColumnFeature)featureVector.get(i);
					DependencyNode node = null;
					if (ocf.getAddressFunction().getAddressValue().getAddress() instanceof DependencyNode) {
						node = (DependencyNode)ocf.getAddressFunction().getAddressValue().getAddress();
					}
					if (node != null && node.getHead() != null && node.getHead().isRoot()) {
						instanceOutput.write("0");
					} else {
						if (featureValue instanceof SingleFeatureValue) {
							instanceOutput.write(((SingleFeatureValue)featureValue).getCode()+"");
						} else if (featureValue instanceof MultipleFeatureValue) {
							Set<Integer> values = ((MultipleFeatureValue)featureValue).getCodes();
							int j=0;
							for (Integer value : values) {
								instanceOutput.write(value.toString());
								if (j != values.size()-1) {
									instanceOutput.write("|");
								}
								j++;
							}
						}
					}
				} else if (featureVector.get(i) instanceof SplitFeature && ((SplitFeature)featureVector.get(i)).getParentFeature() instanceof InputColumnFeature) {
					if (((InputColumnFeature)((SplitFeature)featureVector.get(i)).getParentFeature()).getColumnName().equals("FEATS")) {
						SplitFeature sf = (SplitFeature)featureVector.get(i);
						String value = ((SingleFeatureValue)sf.getParentFeature().getFeatureValue()).getSymbol();
						if (sf.getFeatureValue().isNullValue()) {
							instanceOutput.write("-1");
						} else {
							int code;
							String items[];
							try {
								items = value.split(sf.getSeparators());
							} catch (PatternSyntaxException e) {
								throw new FeatureException("The split feature '"+featureVector.get(i).toString()+"' could not split the value using the following separators '"+sf.getSeparators()+"'",e);
							}
							for (int j = 0; j < items.length; j++) {
								code = sf.getSymbolTable().addSymbol(items[j]);
								instanceOutput.write(code+"");
								if (j != items.length-1) {
									instanceOutput.write("|");
								}
							}
						}
					}
				} else {
					if (featureValue instanceof SingleFeatureValue) {
						instanceOutput.write(((SingleFeatureValue)featureValue).getCode()+"");
					} else if (featureValue instanceof MultipleFeatureValue) {
						Set<Integer> values = ((MultipleFeatureValue)featureValue).getCodes();
						int j=0;
						for (Integer value : values) {
							instanceOutput.write(value.toString());
							if (j != values.size()-1) {
								instanceOutput.write("|");
							}
							j++;
						}
					}
				}
				
				if (i != featureVector.size()) {
					instanceOutput.write('\t');
				}
			}

			instanceOutput.write('\n');
			increaseNumberOfInstances();
		} catch (IOException e) {
			throw new LibsvmException("The LIBSVM learner cannot write to the instance file. ", e);
		}

	}

	/* (non-Javadoc)
	 * @see org.maltparser.ml.LearningMethod#finalizeSentence(org.maltparser.core.sentence.Sentence, org.maltparser.core.graph.DependencyGraph)
	 */
	public void finalizeSentence(DependencyStructure dependencyGraph) throws MaltChainedException {
//		sentenceCount++;
	}
	
	/* (non-Javadoc)
	 * @see org.maltparser.ml.LearningMethod#noMoreInstances()
	 */
	public void noMoreInstances() throws MaltChainedException {
		closeInstanceWriter();
	}


	/* (non-Javadoc)
	 * @see org.maltparser.ml.LearningMethod#train(org.maltparser.parser.guide.feature.FeatureVector)
	 */
	public void train(FeatureVector featureVector) throws MaltChainedException {
		if (featureVector == null) {
			throw new LibsvmException("The feature vector cannot be found. ");
		} else if (owner == null) {
			throw new LibsvmException("The parent guide model cannot be found. ");
		}
		
		if (pathExternalSVMTrain != null) {
			trainExternal(featureVector);
			return;
		}
		svm_problem prob = new svm_problem();
		File modelFile = getFile(".mod");
		try {		
			
			ArrayList<Integer> cardinalities = new ArrayList<Integer>();
			for (FeatureFunction feature : featureVector) {
				cardinalities.add(feature.getFeatureValue().getCardinality());
			}

			readProblemMaltSVMFormat(getInstanceInputStreamReader(".ins"), prob, cardinalities, svmParam);
			
			String errorMessage = svm.svm_check_parameter(prob, svmParam);
			if(errorMessage != null) {
				throw new LibsvmException(errorMessage);
			}
			getConfiguration().getConfigLogger().info("Creating LIBSVM model "+modelFile.getName()+"\n");
			PrintStream out = System.out;
			PrintStream err = System.err;
			System.setOut(NoPrintStream.NO_PRINTSTREAM);
			//System.setErr(new PrintStream(new LoggingOutputStream(owner.getConfiguration().getConfigLogger(), owner.getConfiguration().getConfigLogger().getLevel()), true));
			System.setErr(NoPrintStream.NO_PRINTSTREAM);
			
			svm.svm_save_model(modelFile.getAbsolutePath(), svm.svm_train(prob, svmParam));
			
			System.setOut(err);
			System.setOut(out); 
			if (!saveInstanceFiles) {
				getFile(".ins").delete();
			}
		} catch (OutOfMemoryError e) {
			throw new LibsvmException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		} catch (IllegalArgumentException e) {
			throw new LibsvmException("The LIBSVM learner was not able to redirect Standard Error stream. ", e);
		} catch (SecurityException e) {
			throw new LibsvmException("The LIBSVM learner cannot remove the instance file. ", e);
		} catch (IOException e) {
			throw new LibsvmException("The LIBSVM learner cannot save the model file '"+modelFile.getAbsolutePath()+"'. ", e);
		}
	}

	private void trainExternal(FeatureVector featureVector) throws MaltChainedException {

		try {		
			ArrayList<Integer> cardinalities = new ArrayList<Integer>();
			for (FeatureFunction feature : featureVector) {
				cardinalities.add(feature.getFeatureValue().getCardinality());
			}

			maltSVMFormat2OriginalSVMFormat(getInstanceInputStreamReader(".ins"), getInstanceOutputStreamWriter(".ins.tmp"), cardinalities);
			getConfiguration().getConfigLogger().info("Creating LIBSVM model (svm-train) "+getFile(".mod").getName());

			ArrayList<String> commands = new ArrayList<String>();
			commands.add(pathExternalSVMTrain);
			String[] params = getSVMParamStringArray(svmParam);
			for (int i=0; i < params.length; i++) {
				commands.add(params[i]);
			}
			commands.add(getFile(".ins.tmp").getAbsolutePath());
			commands.add(getFile(".mod").getAbsolutePath());
			String[] arrayCommands =  commands.toArray(new String[commands.size()]);
			Process child = Runtime.getRuntime().exec(arrayCommands);
	        InputStream in = child.getInputStream();
	        while (in.read() != -1){}
            if (child.waitFor() != 0) {
            	owner.getGuide().getConfiguration().getConfigLogger().info(" FAILED ("+child.exitValue()+")");
            }
	        in.close();
	        if (!saveInstanceFiles) {
	        	getFile(".ins").delete();
	        	getFile(".ins.tmp").delete();
	        }
	        owner.getGuide().getConfiguration().getConfigLogger().info("\n");
		} catch (InterruptedException e) {
			 throw new LibsvmException("SVM-trainer is interrupted. ", e);
		} catch (IllegalArgumentException e) {
			throw new LibsvmException("The LIBSVM learner was not able to redirect Standard Error stream. ", e);
		} catch (SecurityException e) {
			throw new LibsvmException("The LIBSVM learner cannot remove the instance file. ", e);
		} catch (IOException e) {
			throw new LibsvmException("The LIBSVM learner cannot save the model file '"+getFile(".mod").getAbsolutePath()+"'. ", e);
		} catch (OutOfMemoryError e) {
			throw new LibsvmException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.maltparser.ml.LearningMethod#moveAllInstances(java.io.BufferedWriter, org.maltparser.parser.guide.feature.Feature, java.util.ArrayList)
	 */
	public void moveAllInstances(LearningMethod method, FeatureFunction divideFeature, ArrayList<Integer> divideFeatureIndexVector) throws MaltChainedException {
		if (method == null) {
			throw new LibsvmException("The learning method cannot be found. ");
		} else if (divideFeature == null) {
			throw new LibsvmException("The divide feature cannot be found. ");
		} 
		try {
			BufferedReader in = new BufferedReader(getInstanceInputStreamReader(".ins"));
			BufferedWriter out = method.getInstanceWriter();
			int l = in.read();
			char c;
			int j = 0;
			while(true) {
				if (l == -1) {
					sb.setLength(0);
					break;
				}
				c = (char)l; 
				l = in.read();
				if (c == '\t') {
					out.write(sb.toString());
					out.write('\t');
					j++;
					sb.setLength(0);
				} else if (c == '\n') {
					out.write(Integer.toString(((SingleFeatureValue)divideFeature.getFeatureValue()).getCode()));
					out.write('\n');
					sb.setLength(0);
					method.increaseNumberOfInstances();
					this.decreaseNumberOfInstances();
					j = 0;
				} else {
					sb.append(c);
				}
			}
			in.close();
			getFile(".ins").delete();
		} catch (SecurityException e) {
			throw new LibsvmException("The LIBSVM learner cannot remove the instance file. ", e);
		} catch (NullPointerException  e) {
			throw new LibsvmException("The instance file cannot be found. ", e);
		} catch (FileNotFoundException e) {
			throw new LibsvmException("The instance file cannot be found. ", e);
		} catch (IOException e) {
			throw new LibsvmException("The LIBSVM learner read from the instance file. ", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.maltparser.ml.LearningMethod#predict(org.maltparser.parser.guide.feature.FeatureVector, org.maltparser.ml.KBestList)
	 */
	public boolean predict(FeatureVector featureVector, SingleDecision decision) throws MaltChainedException {
		if (model == null) {
			File modelFile = getFile(".mod");
			try {
				model = svm.svm_load_model(modelFile.getAbsolutePath());	
			} catch (IOException e) {
				throw new LibsvmException("The file '"+modelFile.getAbsolutePath()+"' cannot be loaded. ", e);
			}
		}
		if (xlist == null) {
			xlist = new ArrayList<svm_node>(featureVector.size()); 
		}
		if (model == null) {
			throw new LibsvmException("The LIBSVM learner cannot predict the next class, because the learning model cannot be found. ");
		} else if (featureVector == null) {
			throw new LibsvmException("The LIBSVM learner cannot predict the next class, because the feature vector cannot be found. ");
		}
		int j = 0;
		int offset = 0;

		for (FeatureFunction feature : featureVector) {
			if (feature instanceof SplitFeature && feature.toString().startsWith("Split(InputColumn(FEATS")) {
				SplitFeature sf = (SplitFeature)feature;
				String value = ((SingleFeatureValue)sf.getParentFeature().getFeatureValue()).getSymbol();

				SymbolTable table = sf.getSymbolTable();
				String items[];
				try {
					items = value.split(sf.getSeparators());
				} catch (PatternSyntaxException e) {
					throw new FeatureException("The split feature '"+feature.toString()+"' could not split the value using the following separators '"+sf.getSeparators()+"'",e);
				}
				for (int k=0; k < items.length; k++) {
					if (!(table.isNullValue(items[k]) && table.getSymbolStringToCode(items[k]) == 0)) {
						if (j >= xlist.size()) {
							svm_node x =  new svm_node();
							x.value = 1.0;
							xlist.add(j,x);
						}
						xlist.get(j++).index = table.addSymbol(items[k]) + offset;
					}
				}
			} else {
				FeatureValue featureValue = feature.getFeatureValue();
				if (featureValue instanceof SingleFeatureValue) {
					if (((SingleFeatureValue)featureValue).isKnown()) {
						if (j >= xlist.size()) {
							svm_node x =  new svm_node();
							x.value = 1.0;
							xlist.add(j,x);
						}
						if (feature instanceof OutputColumnFeature && !feature.toString().endsWith("DEPREL, Stack[0])")) {
							OutputColumnFeature ocf = (OutputColumnFeature)feature;
							DependencyNode node = null;
							if (ocf.getAddressFunction().getAddressValue().getAddress() instanceof DependencyNode) {
								node = (DependencyNode)ocf.getAddressFunction().getAddressValue().getAddress();
							}
							if (node != null && node.getHead() != null && node.getHead().isRoot()) {
								xlist.get(j++).index = 0 + offset;
							} else {
								xlist.get(j++).index = ((SingleFeatureValue)featureValue).getCode() + offset;
							}
						} else {
							xlist.get(j++).index = ((SingleFeatureValue)featureValue).getCode() + offset;
						}
					}
				} else if (featureValue instanceof MultipleFeatureValue) {
					Set<Integer> values = ((MultipleFeatureValue)featureValue).getCodes();
					for (Integer value : values) {
						if (((MultipleFeatureValue)featureValue).isKnown(value)) {
							if (j >= xlist.size()) {
								svm_node x =  new svm_node();
								x.value = 1.0;
								xlist.add(j,x);
							}
							if (feature instanceof OutputColumnFeature && !feature.toString().endsWith("DEPREL, Stack[0])")) {
								OutputColumnFeature ocf = (OutputColumnFeature)feature;
								DependencyNode node = null;
								if (ocf.getAddressFunction().getAddressValue().getAddress() instanceof DependencyNode) {
									node = (DependencyNode)ocf.getAddressFunction().getAddressValue().getAddress();
								}
								if (node != null && node.getHead() != null && node.getHead().isRoot()) {
									xlist.get(j++).index = 0 + offset;
								} else {
									xlist.get(j++).index = value + offset;
								}
							} else {
								xlist.get(j++).index = value + offset;
							}
						}
					}
				}
			}
			offset += feature.getFeatureValue().getCardinality();
		}
		int transition = (int)svm.svm_predict(model, xlist.subList(0, j).toArray(new svm_node[0]));
		if (nivrestandard == true && rootHandling == NivreStandardMalt04.NORMAL && transition == 2) {
			transition = 1;
		}
			
		decision.getKBestList().add(transition);

		return true;
	} 

	/* (non-Javadoc)
	 * @see org.maltparser.ml.LearningMethod#terminate()
	 */
	public void terminate() throws MaltChainedException { 
		closeInstanceWriter();
		model = null;
		svmParam = null;
		xlist = null;
		owner = null;
	}

	/* (non-Javadoc)
	 * @see org.maltparser.ml.LearningMethod#getInstanceWriter()
	 */
	public BufferedWriter getInstanceWriter() {
		return instanceOutput;
	}
	
	/**
	 * Close the instance writer
	 * 
	 * @throws MaltChainedException
	 */
	protected void closeInstanceWriter() throws MaltChainedException {
		try {
			if (instanceOutput != null) {
				instanceOutput.flush();
				instanceOutput.close();
				instanceOutput = null;
			}
			
			/*if (debugTransOut != null) {
				debugTransOut.flush();
				debugTransOut.close();
				debugTransOut = null;
			}*/
		} catch (IOException e) {
			throw new LibsvmException("The LIBSVM learner cannot close the instance file. ", e);
		}
	}
	
	/**
	 * Initialize the LIBSVM according to the parameter string
	 * 
	 * @param paramString the parameter string to configure the LIBSVM learner.
	 * @throws MaltChainedException
	 */
	protected void initSvmParam(String paramString) throws MaltChainedException {
		this.paramString = paramString;
		svmParam = new svm_parameter();
		initParameters(svmParam);
		parseParameters(paramString, svmParam);
	}
	
	/**
	 * Initialize the LIBSVM with a coding and a behavior strategy. This strategy parameter is
	 * used for reproduce the behavior of MaltParser 0.4 (C-impl). 
	 * 
	 * @throws MaltChainedException
	 */
	protected void initSpecialParameters() throws MaltChainedException {
		if (getConfiguration().getParsingAlgorithm() instanceof NivreEagerMalt04 || getConfiguration().getParsingAlgorithm() instanceof NivreStandardMalt04) {
			nivre = true;
			RA_ROOT = "RA"+getConfiguration().getOptionValue("guide", "classitem_separator").toString()+getConfiguration().getOptionValue("graph", "root_label").toString();	
			LA_ROOT = "LA"+getConfiguration().getOptionValue("guide", "classitem_separator").toString()+getConfiguration().getOptionValue("graph", "root_label").toString();	
			if (getConfiguration().getParsingAlgorithm() instanceof NivreEagerMalt04) {
				rootHandling = ((NivreEagerMalt04)getConfiguration().getParsingAlgorithm()).getRootHandling();
			} else if (getConfiguration().getParsingAlgorithm() instanceof NivreStandardMalt04) {
				rootHandling = ((NivreStandardMalt04)getConfiguration().getParsingAlgorithm()).getRootHandling();
				nivrestandard = true;
			}
		}

		saveInstanceFiles = ((Boolean)getConfiguration().getOptionValue("libsvm", "save_instance_files")).booleanValue();
		if (!getConfiguration().getOptionValue("libsvm", "libsvm_external").toString().equals("")) {
			try {
				if (!new File(getConfiguration().getOptionValue("libsvm", "libsvm_external").toString()).exists()) {
					throw new LibsvmException("The path to the external LIBSVM trainer 'svm-train' is wrong.");
				}
				if (new File(getConfiguration().getOptionValue("libsvm", "libsvm_external").toString()).isDirectory()) {
					throw new LibsvmException("The option --libsvm-libsvm_external points to a directory, the path should point at the 'svm-train' file or the 'svm-train.exe' file");
				}
				if (!(getConfiguration().getOptionValue("libsvm", "libsvm_external").toString().endsWith("svm-train") || getConfiguration().getOptionValue("libsvm", "libsvm_external").toString().endsWith("svm-train.exe"))) {
					throw new LibsvmException("The option --libsvm-libsvm_external does not specify the path to 'svm-train' file or the 'svm-train.exe' file. ");
				}
				pathExternalSVMTrain = getConfiguration().getOptionValue("libsvm", "libsvm_external").toString();
			} catch (SecurityException e) {
				throw new LibsvmException("Access denied to the file specified by the option --libsvm-libsvm_external. ", e);
			}
		}
	}
	
	/**
	 * Returns the parameter string for used for configure LIBSVM
	 * 
	 * @return the parameter string for used for configure LIBSVM
	 */
	public String getParamString() {
		return paramString;
	}
	
	/**
	 * Returns the parent instance model
	 * 
	 * @return the parent instance model
	 */
	public InstanceModel getOwner() {
		return owner;
	}

	/**
	 * Sets the parent instance model
	 * 
	 * @param owner a instance model
	 */
	protected void setOwner(InstanceModel owner) {
		this.owner = owner;
	}
	
	/**
	 * Returns the learner mode
	 * 
	 * @return the learner mode
	 */
	public int getLearnerMode() {
		return learnerMode;
	}

	/**
	 * Sets the learner mode
	 * 
	 * @param learnerMode the learner mode
	 */
	public void setLearnerMode(int learnerMode) {
		this.learnerMode = learnerMode;
	}
	
	/**
	 * Returns the name of the learning method
	 * 
	 * @return the name of the learning method
	 */
	public String getLearningMethodName() {
		return name;
	}
	
	/**
	 * Returns the current configuration
	 * 
	 * @return the current configuration
	 * @throws MaltChainedException
	 */
	public DependencyParserConfig getConfiguration() throws MaltChainedException {
		return owner.getGuide().getConfiguration();
	}
	
	/**
	 * Returns the number of processed instances
	 * 
	 * @return the number of processed instances
	 */
	public int getNumberOfInstances() {
		return numberOfInstances;
	}

	/* (non-Javadoc)
	 * @see org.maltparser.ml.LearningMethod#increaseNumberOfInstances()
	 */
	public void increaseNumberOfInstances() {
		numberOfInstances++;
		owner.increaseFrequency();
	}
	
	/* (non-Javadoc)
	 * @see org.maltparser.ml.LearningMethod#decreaseNumberOfInstances()
	 */
	public void decreaseNumberOfInstances() {
		numberOfInstances--;
		owner.decreaseFrequency();
	}
	
	/**
	 * Sets the number of instance
	 * 
	 * @param numberOfInstances the number of instance
	 */
	protected void setNumberOfInstances(int numberOfInstances) {
		this.numberOfInstances = 0;
	}

	/**
	 * Sets the learning method name
	 * 
	 * @param name the learning method name
	 */
	protected void setLearningMethodName(String name) {
		this.name = name;
	}
	
	/**
	 * Returns the instance output writer. The naming of the file is standardized according to the learning method name, but file suffix can vary. 
	 * 
	 * @param suffix the file suffix of the file name
	 * @return the instance output writer
	 * @throws MaltChainedException
	 */
	protected OutputStreamWriter getInstanceOutputStreamWriter(String suffix) throws MaltChainedException {
		return getConfiguration().getConfigurationDir().getOutputStreamWriter(owner.getModelName()+getLearningMethodName()+suffix);
	}
	
	/**
	 * Returns the instance input reader. The naming of the file is standardized according to the learning method name, but file suffix can vary.
	 * 
	 * @param suffix the file suffix of the file name
	 * @return the instance input reader
	 * @throws MaltChainedException
	 */
	protected InputStreamReader getInstanceInputStreamReader(String suffix) throws MaltChainedException {
		return getConfiguration().getConfigurationDir().getInputStreamReader(owner.getModelName()+getLearningMethodName()+suffix);
	}
	
	/**
	 * Returns a file object. The naming of the file is standardized according to the learning method name, but file suffix can vary.
	 * 
	 * @param suffix the file suffix of the file name
	 * @return Returns a file object
	 * @throws MaltChainedException
	 */
	protected File getFile(String suffix) throws MaltChainedException {
		return getConfiguration().getConfigurationDir().getFile(owner.getModelName()+getLearningMethodName()+suffix);
	}
	
	
	/**
	 * Reads an instance file into a svm_problem object according to the Malt-SVM format, which is column fixed format (tab-separated).
	 * 
	 * @param isr	the instance stream reader for the instance file
	 * @param prob	a svm_problem object
	 * @param cardinality	a vector containing the number of distinct values for a particular column.
	 * @param param	a svm_parameter object
	 * @throws LibsvmException
	 */
	public void readProblemMaltSVMFormat(InputStreamReader isr, svm_problem prob, ArrayList<Integer> cardinality, svm_parameter param) throws LibsvmException {
		try {
			BufferedReader fp = new BufferedReader(isr);
			int max_index = 0;
			if (xlist == null) {
				xlist = new ArrayList<svm_node>(); 
			}
			prob.l = getNumberOfInstances();
			prob.x = new svm_node[prob.l][];
			prob.y = new double[prob.l];
			int i = 0;
			Pattern tabPattern = Pattern.compile("\t");
			Pattern pipePattern = Pattern.compile("\\|");
			while(true) {
				String line = fp.readLine();
				if(line == null) break;
				String[] columns = tabPattern.split(line);
				if (columns.length == 0) {
					continue;
				}
				
				int offset = 0; 
				int j = 0;
				try {
					prob.y[i] = (double)Integer.parseInt(columns[j]);
					int p = 0;
					for(j = 1; j < columns.length; j++) {
						String[] items = pipePattern.split(columns[j]);	
						for (int k = 0; k < items.length; k++) {
							try {
								if (Integer.parseInt(items[k]) != -1) {
									xlist.add(p, new svm_node());
									xlist.get(p).value = 1.0;
									xlist.get(p).index = Integer.parseInt(items[k])+offset;
									p++;
								}
							} catch (NumberFormatException e) {
								throw new LibsvmException("The instance file contain a non-integer value '"+items[k]+"'", e);
							}
						}
						offset += cardinality.get(j-1);
					}
					prob.x[i] = xlist.subList(0, p).toArray(new svm_node[0]);
					if(columns.length>0) {
						max_index = Math.max(max_index, xlist.get(p-1).index);
					}
					i++;
					xlist.clear();
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new LibsvmException("Cannot read from the instance file. ", e);
				}
			}
			fp.close();	
			if (param.gamma == 0) {
				param.gamma = 1.0/max_index;
			}
			xlist = null;
		} catch (IOException e) {
			throw new LibsvmException("Cannot read from the instance file. ", e);
		}
	}
	
	
	/**
	 * Assign a default value to all svm parameters
	 * 
	 * @param param	a svm_parameter object
	 */
	public void initParameters(svm_parameter param) throws LibsvmException {
		if (param == null) {
			throw new LibsvmException("Svm-parameters cannot be found. ");
		}
		param.svm_type = svm_parameter.C_SVC;
		param.kernel_type = svm_parameter.POLY;
		param.degree = 2.0; // libsvm 2.8
		param.gamma = 0.2;	// 1/k
		param.coef0 = 0;
		param.nu = 0.5;
		param.cache_size = 40; 
		param.C = 0.5; 
		param.eps = 1.0; 
		param.p = 0.1;
		param.shrinking = 1;
		param.probability = 0;
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];
	}
	
	/**
	 * Returns a string containing all svm-parameters of interest
	 * 
	 * @param param a svm_parameter object
	 * @return a string containing all svm-parameters of interest
	 */
	public String toStringParameters(svm_parameter param)  {
		if (param == null) {
			throw new IllegalArgumentException("Svm-parameters cannot be found. ");
		}
		StringBuffer sb = new StringBuffer();
		
		String[] svmtypes = {"C_SVC", "NU_SVC","ONE_CLASS","EPSILON_SVR","NU_SVR"};
		String[] kerneltypes = {"LINEAR", "POLY","RBF","SIGMOID","PRECOMPUTED"};
		DecimalFormat dform = new DecimalFormat("#0.0#"); 
		DecimalFormatSymbols sym = new DecimalFormatSymbols();
		sym.setDecimalSeparator('.');
		dform.setDecimalFormatSymbols(sym);
		sb.append("LIBSVM SETTINGS\n");
		sb.append("  SVM type      : " + svmtypes[param.svm_type] + " (" + param.svm_type + ")\n");
		sb.append("  Kernel        : " + kerneltypes[param.kernel_type] + " (" + param.kernel_type + ")\n");
		if (param.kernel_type == svm_parameter.POLY) {
			sb.append("  Degree        : " + param.degree + "\n");
		}
		if (param.kernel_type == svm_parameter.POLY || param.kernel_type == svm_parameter.RBF || param.kernel_type == svm_parameter.SIGMOID) {
			sb.append("  Gamma         : " + dform.format(param.gamma) + "\n");
			if (param.kernel_type == svm_parameter.POLY || param.kernel_type == svm_parameter.SIGMOID) {
				sb.append("  Coef0         : " + dform.format(param.coef0) + "\n");
			}
		}
		if (param.svm_type == svm_parameter.NU_SVC || param.svm_type == svm_parameter.NU_SVR || param.svm_type == svm_parameter.ONE_CLASS) {
			sb.append("  Nu            : " + dform.format(param.nu) + "\n");
		}
		sb.append("  Cache Size    : " + dform.format(param.cache_size) + " MB\n");
		if (param.svm_type == svm_parameter.C_SVC || param.svm_type == svm_parameter.NU_SVR || param.svm_type == svm_parameter.EPSILON_SVR) {
			sb.append("  C             : " + dform.format(param.C) + "\n");
		}
		sb.append("  Eps           : " + dform.format(param.eps) + "\n");
		if (param.svm_type == svm_parameter.EPSILON_SVR) {
			sb.append("  P             : " + dform.format(param.p) + "\n");
		}
		sb.append("  Shrinking     : " + param.shrinking + "\n");
		sb.append("  Probability   : " + param.probability + "\n");
		if (param.svm_type == svm_parameter.C_SVC) {
			sb.append("  #Weight       : " + param.nr_weight + "\n");
			if (param.nr_weight > 0) {
				sb.append("  Weight labels : ");
				for (int i = 0; i < param.nr_weight; i++) {
					sb.append(param.weight_label[i]);
					if (i != param.nr_weight-1) {
						sb.append(", ");
					}
				}
				sb.append("\n");
				for (int i = 0; i < param.nr_weight; i++) {
					sb.append(dform.format(param.weight));
					if (i != param.nr_weight-1) {
						sb.append(", ");
					}
				}
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	
	public String[] getSVMParamStringArray(svm_parameter param) {
		ArrayList<String> params = new ArrayList<String>();

		if (param.svm_type != 0) {
			params.add("-s"); params.add(new Integer(param.svm_type).toString());
		}
		if (param.kernel_type != 2) {
			params.add("-t"); params.add(new Integer(param.kernel_type).toString());
		}
		if (param.degree != 3) {
			params.add("-d"); params.add(new Double(param.degree).toString());
		}
		params.add("-g"); params.add(new Double(param.gamma).toString());
		if (param.coef0 != 0) {
			params.add("-r"); params.add(new Double(param.coef0).toString());
		}
		if (param.nu != 0.5) {
			params.add("-n"); params.add(new Double(param.nu).toString());
		}
		if (param.cache_size != 100) {
			params.add("-m"); params.add(new Double(param.cache_size).toString());
		}
		if (param.C != 1) {
			params.add("-c"); params.add(new Double(param.C).toString());
		}
		if (param.eps != 0.001) {
			params.add("-e"); params.add(new Double(param.eps).toString());
		}
		if (param.p != 0.1) {
			params.add("-p"); params.add(new Double(param.p).toString());
		}
		if (param.shrinking != 1) {
			params.add("-h"); params.add(new Integer(param.shrinking).toString());
		}
		if (param.probability != 0) {
			params.add("-b"); params.add(new Integer(param.probability).toString());
		}

		return params.toArray(new String[params.size()]);
	}
	
	/**
	 * Parses the parameter string. The parameter string must contain parameter and value pairs, which are seperated by a blank 
	 * or a underscore. The parameter begins with a character '-' followed by a one-character flag and the value must comply with
	 * the parameters data type. Some examples:
	 * 
	 * -s 0 -t 1 -d 2 -g 0.4 -e 0.1
	 * -s_0_-t_1_-d_2_-g_0.4_-e_0.1
	 * 
	 * @param paramstring	the parameter string 
	 * @param param	a svm_parameter object
	 * @throws LibsvmException
	 */
	public void parseParameters(String paramstring, svm_parameter param) throws LibsvmException {
		if (param == null) {
			throw new LibsvmException("Svm-parameters cannot be found. ");
		}
		if (paramstring == null) {
			return;
		}
		String[] argv;
		try {
			argv = paramstring.split("[_\\p{Blank}]");
		} catch (PatternSyntaxException e) {
			throw new LibsvmException("Could not split the svm-parameter string '"+paramstring+"'. ", e);
		}
		for (int i=0; i < argv.length-1; i++) {
			if(argv[i].charAt(0) != '-') {
				throw new LibsvmException("The argument flag should start with the following character '-', not with "+argv[i].charAt(0));
			}
			if(++i>=argv.length) {
				throw new LibsvmException("The last argument does not have any value. ");
			}
			try {
				switch(argv[i-1].charAt(1)) {
				case 's':
					param.svm_type = Integer.parseInt(argv[i]);
					break;
				case 't':
					param.kernel_type = Integer.parseInt(argv[i]);
					break;
				case 'd':
					param.degree = Double.valueOf(argv[i]).doubleValue(); //libsvm2.8
					break;
				case 'g':
					param.gamma = Double.valueOf(argv[i]).doubleValue();
					break;
				case 'r':
					param.coef0 = Double.valueOf(argv[i]).doubleValue();
					break;
				case 'n':
					param.nu = Double.valueOf(argv[i]).doubleValue();
					break;
				case 'm':
					param.cache_size = Double.valueOf(argv[i]).doubleValue();
					break;
				case 'c':
					param.C = Double.valueOf(argv[i]).doubleValue();
					break;
				case 'e':
					param.eps = Double.valueOf(argv[i]).doubleValue();
					break;
				case 'p':
					param.p = Double.valueOf(argv[i]).doubleValue();
					break;
				case 'h':
					param.shrinking = Integer.parseInt(argv[i]);
					break;
			    case 'b':
					param.probability = Integer.parseInt(argv[i]);
					break;
				case 'w':
					++param.nr_weight;
					{
						int[] old = param.weight_label;
						param.weight_label = new int[param.nr_weight];
						System.arraycopy(old,0,param.weight_label,0,param.nr_weight-1);
					}
	
					{
						double[] old = param.weight;
						param.weight = new double[param.nr_weight];
						System.arraycopy(old,0,param.weight,0,param.nr_weight-1);
					}
	
					param.weight_label[param.nr_weight-1] = Integer.parseInt(argv[i].substring(2));
					param.weight[param.nr_weight-1] = Double.valueOf(argv[i]).doubleValue();
					break;
				case 'Y':
				case 'V':
				case 'S':
				case 'F':
				case 'T':
				case 'M':
				case 'N':
					break;
				default:
					throw new LibsvmException("Unknown svm parameter: '"+argv[i-1]+"' with value '"+argv[i]+"'. ");		
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new LibsvmException("The svm-parameter '"+argv[i-1]+"' could not convert the string value '"+argv[i]+"' into a correct numeric value. ", e);
			} catch (NumberFormatException e) {
				throw new LibsvmException("The svm-parameter '"+argv[i-1]+"' could not convert the string value '"+argv[i]+"' into a correct numeric value. ", e);	
			} catch (NullPointerException e) {
				throw new LibsvmException("The svm-parameter '"+argv[i-1]+"' could not convert the string value '"+argv[i]+"' into a correct numeric value. ", e);	
			}
		}
	}
	
	/**
	 * Converts the instance file (Malt's own SVM format) into the LIBSVM (SVMLight) format. The input instance file is removed (replaced)
	 * by the instance file in the LIBSVM (SVMLight) format. If a column contains -1, the value will be removed in destination file. 
	 * 
	 * @param isr the input stream reader for the source instance file
	 * @param osw	the output stream writer for the destination instance file
	 * @param cardinality a vector containing the number of distinct values for a particular column
	 * @throws LibsvmException
	 */
	public static void maltSVMFormat2OriginalSVMFormat(InputStreamReader isr, OutputStreamWriter osw, ArrayList<Integer> cardinality) throws LibsvmException {
		try {
			final BufferedReader in = new BufferedReader(isr);
			final BufferedWriter out = new BufferedWriter(osw);
			int c;
			int j = 0;
			int offset = 0; 
			int code = 0;
			while(true) {
				c = in.read();
				if (c == -1) {
					break;
				}
				
				if (c == '\t' || c == '|') {
					if (j == 0) {
						out.write(Integer.toString(code));
						j++;
					} else {
						if (code != -1) {
							out.write(' ');
							out.write(Integer.toString(code+offset));
							out.write(":1");
						}
						if (c == '\t') {
							offset += cardinality.get(j-1);
							j++;
						}
					}
					code = 0;
				} else if (c == '\n') {
					j = 0;
					offset = 0;
					out.write('\n');
					code = 0;
				} else if (c == '-') {
					code = -1;
				} else if (code != -1) {
					if (c > 47 && c < 58) {
						code = code * 10 + (c-48);
					} else {
						throw new LibsvmException("The instance file contain a non-integer value, when converting the Malt SVM format into LIBSVM format.");
					}
				}	
			}	
			in.close();	
			out.close();
		} catch (IOException e) {
			throw new LibsvmException("Cannot read from the instance file, when converting the Malt SVM format into LIBSVM format. ", e);
		}
	}
	
	/**
	 * Returns the double (floating-point) value of the string s
	 * 
	 * @param s string value that should be converted into a double.
	 * @return the double (floating-point) value of the string s
	 * @throws LibsvmException
	 */
	public static double atof(String s) throws LibsvmException {
		try {
			return Double.valueOf(s).doubleValue();
		} catch (NumberFormatException e) {
			throw new LibsvmException("Could not convert the string value '"+s+"' into a correct numeric value. ", e);	
		} catch (NullPointerException e) {
			throw new LibsvmException("Could not convert the string value '"+s+"' into a correct numeric value. ", e);	
		}
	}

	/**
	 * Returns the integer value of the string s
	 * 
	 * @param s string value that should be converted into an integer
	 * @return the integer value of the string s
	 * @throws LibsvmException
	 */
	public static int atoi(String s) throws LibsvmException {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new LibsvmException("Could not convert the string value '"+s+"' into a correct integer value. ", e);	
		} catch (NullPointerException e) {
			throw new LibsvmException("Could not convert the string value '"+s+"' into a correct integer value. ", e);	
		}
	}
	
	/**
	 * Reads an instance file into a svm_problem object according to the LIBSVM (SVMLight) format.
	 * 
	 * @param isr the input stream reader for the source instance file
	 * @param prob	a svm_problem object
	 * @param param	a svm_parameter object
	 * @throws LibsvmException
	 */
	public static void readProblemOriginalSVMFormat(InputStreamReader isr, svm_problem prob, svm_parameter param) throws LibsvmException {
		BufferedReader fp = new BufferedReader(isr);

		Vector<String> vy = new Vector<String>();
		Vector<svm_node[]> vx = new Vector<svm_node[]>();
		int max_index = 0;

		while(true) {
			String line;
			try {
				line = fp.readLine();
			} catch (IOException e) {
				throw new LibsvmException("", e);
			}
			if(line == null) break;

			StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

			vy.addElement(st.nextToken());
			int m = st.countTokens()/2;
			svm_node[] x = new svm_node[m];
			for(int j=0;j<m;j++) {
				x[j] = new svm_node();
				x[j].index = atoi(st.nextToken());
				x[j].value = atof(st.nextToken());
			}
			if(m>0) max_index = Math.max(max_index, x[m-1].index);
			vx.addElement(x);
		}

		prob.l = vy.size();
		prob.x = new svm_node[prob.l][];
		for(int i=0;i<prob.l;i++) {
			prob.x[i] = (svm_node[])vx.elementAt(i);
		}
		prob.y = new double[prob.l];
		for(int i=0;i<prob.l;i++) {
			prob.y[i] = atof((String)vy.elementAt(i));
		}
		if(param.gamma == 0.0) {
			param.gamma = 1.0/max_index;
		}
		
		try {
			fp.close();
		} catch (IOException e) {
			throw new LibsvmException("The instance file cannot be closed. ", e);
		}
	}
	
	protected void finalize() throws Throwable {
		try {
			closeInstanceWriter();
		} finally {
			super.finalize();
		}
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("\nLIBSVM INTERFACE\n");
		sb.append("  LIBSVM version: "+LIBSVM_VERSION+"\n");
		sb.append("  SVM-param string: "+paramString+"\n");
		sb.append("  Coding and behavior strategy: MaltParser 0.4\n");
		sb.append(toStringParameters(svmParam));
		return sb.toString();
	}
}
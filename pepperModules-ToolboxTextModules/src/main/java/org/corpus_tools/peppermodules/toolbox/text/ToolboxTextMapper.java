/*******************************************************************************
 * Copyright 2016 Humboldt-Universität zu Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Stephan Druskat - initial API and implementation
 *******************************************************************************/
package org.corpus_tools.peppermodules.toolbox.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mapper for the Toolbox text format.
 * <p>
 * The mapping works as follows. The Toolbox file is read line by line, cleaned up and compiled into a map mapping Toolbox markers to lists of "values", i.e., the remainder of the marker line (and subsequent unmarked non-empty lines).
 * <p>
 * This list is iterated and all lines belonging to a "block" - i.e., all lines following a reference marker, including the line with the reference marker itself - are subsequently mapped onto the Salt document graph.
 * <p>
 * One special case is the lines before the first reference marker, which are assumed to belong to the "header". These lines are mapped onto meta annotations on the document itself.
 * <p>
 * The marked lines following a reference marker are mapped onto the Salt document graph as follows. The values from the lexical and morphology markers (as defined by {@link ToolboxTextImporterProperties#PROP_LEX_MARKER} and
 * {@link ToolboxTextImporterProperties#PROP_MORPH_MARKER}) are used as two separate tokenizations on two separate {@link STextualDS}s, which are synchronized via a {@link STimeline}. The tokens are subsequently annotated with the values
 * from the text annotations markers (as defined by {@link ToolboxTextImporterProperties#PROP_LEX_ANNOTATION_MARKERS}) and the morphology annotation markers (as defined by {@link ToolboxTextImporterProperties#PROP_MORPH_ANNOTATION_MARKERS})
 * respectively. (<strong>NOTE:</strong>It is assumed that the number of annotations always matches the number of tokens per reference!).
 * <p>
 * The reference marker itself is modeled as a span over the <em>text</em> marker tokens of the reference. All markers <em>not</em> belonging to either lexical or morphology annotation layer are assumed to be reference level annotations and
 * are as such modeled as annotations on the reference span.
 *
 * @author Stephan Druskat <mail@sdruskat.net>
 */
public class ToolboxTextMapper extends PepperMapperImpl {

	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextMapper.class);

	private static final String DATE_LAST_MAPPING = "date-last-mapping";

	private static final String SALT_NAMESPACE_TOOLBOX = "toolbox";

	/**
	 * Whether the header block of the file has already been mapped.
	 */
	private boolean isHeaderBlockMapped = false;

	/**
	 * The textual data source containing the lexical source text of the whole document.
	 */
	private final STextualDS lexicalTextualDS = SaltFactory.createSTextualDS();

	/**
	 * The textual data source containing the "morphological source text", i.e., the concatenated morpheme-based morphological units.
	 */
	private final STextualDS morphologicalTextualDS = SaltFactory.createSTextualDS();

	/**
	 * The affix delimiter as defined in the property {@link ToolboxTextImporterProperties#PROP_MORPHEME_DELIMITERS}.
	 */
	private String affixDelimiter;

	/**
	 * The clitics delimiter as defined in the property {@link ToolboxTextImporterProperties#PROP_MORPHEME_DELIMITERS}.
	 */
	private String cliticsDelimiter;

	/**
	 * Maps marker {@link String}s to {@link SLayer}s belonging to that marker.
	 */
	private Map<String, SLayer> layers = new HashMap<>();

	/**
	 * Adds a single metadate to the corpus, namely the current date (no time).
	 */
	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		DateFormat dateFormat = new SimpleDateFormat("dd MM yyyy");
		Date date = new Date();
		SMetaAnnotation dateLastMappingAnno = getCorpus().getMetaAnnotation(DATE_LAST_MAPPING);
		if (dateLastMappingAnno != null) {
			dateLastMappingAnno.setValue(dateFormat.format(date));
		}
		else {
			getCorpus().createMetaAnnotation(null, DATE_LAST_MAPPING, dateFormat.format(date));
		}

		return (DOCUMENT_STATUS.COMPLETED);
	}

	/*
	 * @copydoc @see org.corpus_tools.pepper.impl.PepperMapperImpl#mapSDocument()
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		// Set up fields
		getMorphologicalTextualDS().setText("");
		getMorphologicalTextualDS().setName("morph"); // FIXME: Check whether ID or NAme, check useful name
		getLexicalTextualDS().setText("");
		String[] delimiters = getProperties().getMorphemeDelimiters().split("\\s*,\\s*");
		affixDelimiter = delimiters[0].trim();
		cliticsDelimiter = delimiters[1].trim();

		getDocument().setDocumentGraph(SaltFactory.createSDocumentGraph());
		getDocument().getDocumentGraph().createTimeline();

		getDocument().getDocumentGraph().addNode(getMorphologicalTextualDS());
		getDocument().getDocumentGraph().addNode(getLexicalTextualDS());

		URI resource = getResourceURI();
		File file = null;

		// Set up layers
		createLayer(morphMarker);
		createLayer(lexMarker);

		logger.debug("Importing the file {}.", resource);

		// Create a File object from the resource
		if (getResourceURI() == null) {
			throw new PepperModuleException(this, "Cannot map Toolbox file, because the given resource URI is empty.");
		}
		else {
			if (resource.isFile()) {
				String path = resource.toFileString();
				file = new File(path);
			}
			else {
				throw new PepperModuleException(this, "Resource is not a file!");
			}
		}

		// Create the SDocumentGraph
		if (getDocument().getDocumentGraph() == null) {
			getDocument().setDocumentGraph(SaltFactory.createSDocumentGraph());
		}

		// Read and clean up file
		LinkedList<String> lineList = readFileToList(file);

		// Go through the list and compile a list of tuples of marker and values
		// Map the tuple list whenever you hit a ref marker
		// List<MarkerValuesTuple> block = new ArrayList<>();
		Map<String, List<String>> block = new HashMap<>();
		int lineListSize = lineList.size();
		for (String line : lineList) {
			if (!line.startsWith("\\" + getProperties().getRefMarker())) {
				String[] markerAndValues = line.split("\\s+");
				if (markerAndValues[0] == null) {
					LinkedList<String> markerandValuesList = new LinkedList<String>(Arrays.asList(markerAndValues));
					markerandValuesList.removeFirst();
					markerAndValues = (String[]) markerandValuesList.toArray();
				}
				String marker = markerAndValues[0].substring(1, markerAndValues[0].length());
				ArrayList<String> valueList = new ArrayList<>();
				for (int i = 1; i < markerAndValues.length; i++) {
					valueList.add(markerAndValues[i]);
				}
				if (!block.containsKey(marker)) {
					block.put(marker, valueList);
				}
				else {
					block.get(marker).addAll(valueList);
				}
			}
			else { // Hit a ref marker
				if (!isHeaderBlockMapped) { // First hit of ref marker, i.e., block must be header block
					mapHeaderToModel(block);
					isHeaderBlockMapped = true;
				}
				else {
					mapRefToModel(block);
				}
				block.clear();
			}
			addProgress(((double) 1) / lineListSize);
		}
		/*
		 * Map ref block to model once, because we cannot hit a ref marker at the end of the list anymore to trigger a block mapping process.
		 */
		mapRefToModel(block);

		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * Reads a {@link File} and writes it to a {@link LinkedList}, line by line, removing empty lines and trimming non-empty lines in the process
	 *
	 * @param file The file to write to the {@link LinkedList}
	 * @return the list of non-empty, trimmed lines included in the file
	 */
	private LinkedList<String> readFileToList(File file) {
		BufferedReader br = null;
		LinkedList<String> lineList = new LinkedList<>();
		// Compile a list of trimmed, non-empty lines
		try {
			br = new BufferedReader(new FileReader(file));
			String line = null;
			line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (!line.isEmpty()) {
					lineList.add(line);
				}
				line = br.readLine();
			}
		}
		catch (FileNotFoundException e) {
			throw new PepperModuleException(this, "Cannot read file '" + getResourceURI() + "', because of nested exception: ", e);
		}
		catch (IOException e) {
			throw new PepperModuleException(this, "Cannot read file '" + getResourceURI() + "', because of nested exception: ", e);
		}
		finally {
			if (br != null) {
				try {
					br.close();
				}
				catch (IOException e) {
					throw new PepperModuleException(this, "Cannot close file '" + getResourceURI() + "', because of nested exception: ", e);
				}
			}
		}

		// Iterate through the list and pull together non-marked lines under the respective ref marker
		String attachmentLine = null;
		ListIterator<String> iterator = lineList.listIterator(lineList.size());
		while (iterator.hasPrevious()) {
			String thisLine = iterator.previous();
			if (thisLine.startsWith("\\")) {
				if (attachmentLine != null) {
					iterator.set(thisLine + System.getProperty("line.separator") + attachmentLine);
				}
				attachmentLine = null;
			}
			else if (attachmentLine != null) {
				attachmentLine = new String(thisLine) + System.getProperty("line.separator") + attachmentLine;
				iterator.remove();
			}
			else {
				attachmentLine = new String(thisLine);
				iterator.remove();
			}
		}
		return lineList;
	}

	/**
	 * Maps the header block onto the Salt model, i.e., adding its marker lines as meta annotations on the document.
	 *
	 * @param block The header block to process
	 */
	private void mapHeaderToModel(Map<String, List<String>> block) {
		for (Entry<String, List<String>> line : block.entrySet()) {
			String marker = line.getKey();
			String qualifiedId = SALT_NAMESPACE_TOOLBOX + "::" + marker;
			SMetaAnnotation annotation;
			// One meta annotation per marker, hence concatenate the list of values.
			StringBuilder builder = new StringBuilder();
			for (String val : line.getValue()) {
				builder.append(val).append(" ");
			}
			String value = builder.toString().trim();
			if ((annotation = getDocument().getMetaAnnotation(qualifiedId)) != null) {
				annotation.setValue(value);
			}
			else {
				getDocument().createMetaAnnotation(SALT_NAMESPACE_TOOLBOX, marker, value);
			}
		}
	}

	public Map<String, Integer> processRefs(Map<String, List<String>> block) {
		String[] delimiters = getProperties().getMorphemeDelimiters().split("\\s*,\\s*");
		affixDelimiter = delimiters[0].trim();
		cliticsDelimiter = delimiters[1].trim();

		Map<String, Integer> fakeMap = new HashMap<>();
		
		Map<String, String> wordToMorph = new LinkedHashMap<>();
			
		for (Entry<String, List<String>> line : block.entrySet()) {
			List<String> lexicalTokens = block.get(lexMarker);
			if (lexicalTokens != null) {
				if (line.getKey().equals(morphMarker)) {
					List<String> vals = line.getValue();
					int morphCounter = 0;
					int wordCounter = -1;
					String lastVal = null;
					StringBuilder morphUnitBuilder = new StringBuilder();
					for (int i = 0; i < vals.size(); i++) {
						if (i > 0) {
							morphCounter++;
						}
						String singleVal = vals.get(i);
						if (isHead(singleVal, lastVal) && lastVal != null) {
							wordCounter++;
							String lexicalToken = lexicalTokens.get(wordCounter);
							
							fakeMap.put(lexicalToken, morphCounter);
							
							// Prepare next iteration
							morphCounter = 0;
							morphUnitBuilder.setLength(0);
						}
						morphUnitBuilder.append(singleVal);
						lastVal = singleVal;
					}
					// Write one last time because no further head will be hit
					wordCounter++;
					morphCounter++;
					String lexicalToken = lexicalTokens.get(wordCounter);
					
					fakeMap.put(lexicalToken, morphCounter);
				}
			}
			else {
				throw new PepperModuleException("Reference block does not contain any lexical tokens!");
			}
		}
		for (Entry<String, String> line : wordToMorph.entrySet()) {
			System.err.println(line.getKey() + " > " + line.getValue());
		}
		return fakeMap;
	}

	/**
	 * TODO: Description
	 *
	 * @param singleVal
	 * @param lastVal
	 * @return
	 */
	private boolean isHead(String singleVal, String lastVal) {
		if (lastVal == null) {
			return true;
		}
		else {
			return !singleVal.startsWith(getAffixDelimiter()) && !singleVal.startsWith(getCliticsDelimiter()) && !lastVal.endsWith(getAffixDelimiter()) && !lastVal.endsWith(getCliticsDelimiter());
		}
	}

	/**
	 * Maps a reference block onto the Salt model, i.e., adding its marker lines as tokens, spans, and annotations to the document graph.
	 *
	 * @param block The reference block to process
	 */
	private void mapRefToModel(Map<String, List<String>> block) {
		// TODO Catch free morpheme delimiter error
		// Resolve properties
		String commaDelimRegex = "\\s*,\\s*";
		String[] delimiters = getProperties().getMorphemeDelimiters().split(commaDelimRegex);
		affixDelimiter = delimiters[0].trim();
		cliticsDelimiter = delimiters[1].trim();
		String lexMarker = getProperties().getLexMarker();
		String morphMarker = getProperties().getMorphMarker();
		String refMarker = getProperties().getRefMarker();
		
		Set<String> lexAnnotationMarkers = new HashSet<>(Arrays.asList(getProperties().getLexAnnotationMarkers().split(commaDelimRegex)));
		Set<String> morphAnnotationMarkers = new HashSet<>(Arrays.asList(getProperties().getMorphAnnotationMarkers().split(commaDelimRegex)));
		
		StringBuilder morphDSBuilder = new StringBuilder();
		StringBuilder lexDSBuilder = new StringBuilder();
		
		List<String> lexicalTokens = block.get(lexMarker);
		Set<SToken> refTokens = new HashSet<>();
		
		HashMap<String, List<String>> lexAnnotationLines = new HashMap<>();
		HashMap<String, List<String>> morphAnnotationLines = new HashMap<>();
		HashMap<String, List<String>> refAnnotationLines = new HashMap<>();
		
		// Build maps of annotation levels
		Iterator<Entry<String, List<String>>> mapIterator = block.entrySet().iterator();
		while (mapIterator.hasNext()) {
			Entry<String, List<String>> line = mapIterator.next();
			String key = line.getKey();
			if (key.equals(lexMarker) || key.equals(morphMarker) || key.equals(refMarker)) {
				continue;
			}
			else if (lexAnnotationMarkers.contains(key)) {
				lexAnnotationLines.put(key, line.getValue());
			}
			else if (morphAnnotationMarkers.contains(key)) {
				morphAnnotationLines.put(key, line.getValue());
			}
			else {
				refAnnotationLines.put(key, line.getValue());
			}
		}

		List<String> morphologyValues = block.get(morphMarker);
		// Build morphological and lexical tokens with annotations, and build data sources
		// for (Entry<String, List<String>> line : block.entrySet()) {
		// if (lexicalTokens != null) {
		// if (line.getKey().equals(morphMarker)) {
//		List<String> vals = line.getValue();
		int morphCounter = 0;
		int lexIndex = -1;
		String lastMorpheme = null;
		// StringBuilder morphUnitBuilder = new StringBuilder();
		for (int morphIndex = 0; morphIndex < morphologyValues.size(); morphIndex++) {
			if (morphIndex > 0) {
				morphCounter++;
			}
			String morphemeTextToken = morphologyValues.get(morphIndex);
			if (isHead(morphemeTextToken, lastMorpheme) && lastMorpheme != null) {
				lexIndex++;
				String lexicalTextToken = lexicalTokens.get(lexIndex);
				// Map
				lexDSBuilder.append(lexDSBuilder.length() > 0 ? " " : "").append(lexicalTextToken);
				refTokens.add(createLexicalToken(lexicalTextToken, morphCounter, lexIndex, lexAnnotationLines));

				// Prepare next iteration
				morphCounter = 0;
				// morphUnitBuilder.setLength(0);
			}
			// morphUnitBuilder.append(singleVal);
			morphDSBuilder.append(morphDSBuilder.length() > 0 ? " " : "").append(morphemeTextToken);
			createMorphToken(morphemeTextToken, morphIndex, morphAnnotationLines);
			lastMorpheme = morphemeTextToken;
		}
		// Map the loop's final iteration results as it has ended.
		lexIndex++;
		morphCounter++;
		String lexicalTextToken = lexicalTokens.get(lexIndex);
		lexDSBuilder.append(lexDSBuilder.length() > 0 ? " " : "").append(lexicalTextToken);
		refTokens.add(createLexicalToken(lexicalTextToken, morphCounter, lexIndex, lexAnnotationLines));
		
		int lastMorphIndex = morphologyValues.size() - 1;
		String morphemeTextToken = morphologyValues.get(lastMorphIndex);
		morphDSBuilder.append(morphDSBuilder.length() > 0 ? " " : "").append(morphemeTextToken);
		createMorphToken(morphemeTextToken, lastMorphIndex, morphAnnotationLines);

		// Add to data sources
		String oldLexDSText = getLexicalTextualDS().getText();
		getLexicalTextualDS().setText(oldLexDSText.concat(oldLexDSText.isEmpty() ? "" : " ").concat(lexDSBuilder.toString()));
		String oldMorphDSText = getMorphologicalTextualDS().getText();
		getMorphologicalTextualDS().setText(oldMorphDSText.concat(oldMorphDSText.isEmpty() ? "" : morphDSBuilder.toString()));
		
		// Now build the ref span and add ref-level annotations
	}

	// }
	// else {
	// throw new PepperModuleException("Reference block does not contain any lexical tokens!");
	// }
	// // Do work on ref-related lines
	// }
	// // Do block-wide work
//	}

	/**
	 * TODO: Description
	 *
	 * @param singleVal
	 * @param i 
	 */
	private SToken createMorphToken(String singleVal, int i) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * TODO: Description
	 *
	 * @param lexicalTextToken
	 * @param morphCounter
	 * @return 
	 */
	private SToken createLexicalToken(String lexicalTextToken, int morphCounter) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Creates and names an {@link SLayer} and puts it to the {@link #layers} {@link Map} under its name.
	 * 
	 * @param name
	 */
	private void createLayer(String name) {
		SLayer layer = SaltFactory.createSLayer();
		layer.setName(name);
		getDocument().getDocumentGraph().addLayer(layer);
		layers.put(name, layer);
	}

	/**
	 * TODO: Description
	 *
	 * @param dSStartIndex
	 * @param tokenLength
	 */
	private void createToken(int dSStartIndex, int tokenLength, int tlStartIndex, int timelineUnits, SLayer layer, STextualDS ds) {
		SDocumentGraph graph = getDocument().getDocumentGraph();
		SToken token = SaltFactory.createSToken();
		graph.addNode(token);
		if (layer != null) {
			layer.addNode(token);
		}

		// Create STextualRelation
		STextualRelation textRel = SaltFactory.createSTextualRelation();
		textRel.setSource(token);
		textRel.setTarget(ds);
		textRel.setStart(dSStartIndex);
		textRel.setEnd(dSStartIndex + tokenLength);
		graph.addRelation(textRel);

		// Create STimelineRelation
		STimelineRelation timeRel = SaltFactory.createSTimelineRelation();
		timeRel.setSource(token);
		timeRel.setTarget(graph.getTimeline());
		timeRel.setStart(tlStartIndex);
		timeRel.setEnd(tlStartIndex + timelineUnits);
		graph.addRelation(timeRel);
	}

	/**
	 * @return the lexicalTextualDS
	 */
	public STextualDS getLexicalTextualDS() {
		return lexicalTextualDS;
	}

	/**
	 * @return the morphologicalTextualDS
	 */
	public STextualDS getMorphologicalTextualDS() {
		return morphologicalTextualDS;
	}

	/**
	 * @return the affixDelimiter
	 */
	private String getAffixDelimiter() {
		return affixDelimiter;
	}

	/**
	 * @return the cliticsDelimiter
	 */
	private String getCliticsDelimiter() {
		return cliticsDelimiter;
	}

	/*
	 * @copydoc @see org.corpus_tools.pepper.impl.PepperMapperImpl#getProperties()
	 */
	@Override
	public ToolboxTextImporterProperties getProperties() {
		return (ToolboxTextImporterProperties) super.getProperties();
	}

}

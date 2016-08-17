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
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

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
 * The reference marker itself is modeled as a span over those marker tokens of the reference which are defined by {@link ToolboxTextImporterProperties#PROP_MAP_REF_ANNOTATIONS_TO_LEXICAL_LAYER}. All markers <em>not</em> belonging to either
 * a lexical or a morphological annotation layer are assumed to be reference level annotations and are as such modeled as annotations on the reference span.
 *
 * @author Stephan Druskat <mail@sdruskat.net>
 */
public class ToolboxTextMapper extends PepperMapperImpl {

	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextMapper.class);

	private static final String DATE_LAST_MAPPING = "date-last-mapping";

	private static final String SALT_NAMESPACE_TOOLBOX = "toolbox";

	private static final String IMPORTER_NAME = "toolbox-text-importer";

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

	private boolean containsMorphology;

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

	private SDocumentGraph graph = null;

	SToken lastMorphToken = null;
	SToken lastLexToken = null;

	private int lexTimelineIndex = 0;
	private int morphTimelineIndex = 0;
	private int lexDSIndex = 0;
	private int morphDSIndex = 0;

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
		containsMorphology = getProperties().containsMorphology();
		getMorphologicalTextualDS().setText("");
		getMorphologicalTextualDS().setName(getProperties().getMorphMarker());
		getLexicalTextualDS().setText("");
		getLexicalTextualDS().setName(getProperties().getLexMarker());
		String[] delimiters = getProperties().getMorphemeDelimiters().split("\\s*,\\s*");
		affixDelimiter = delimiters[0].trim();
		cliticsDelimiter = delimiters[1].trim();
		String morphMarker = null;
		String lexMarker = getProperties().getLexMarker();

		getDocument().setDocumentGraph(SaltFactory.createSDocumentGraph());
		setGraph(getDocument().getDocumentGraph());
		getGraph().createTimeline();

		getGraph().addNode(getLexicalTextualDS());

		URI resource = getResourceURI();
		File file = null;

		// Set up morphology-related stuff as necessary
		if (containsMorphology) {
			morphMarker = getProperties().getMorphMarker();
			getGraph().addNode(getMorphologicalTextualDS());
			createLayer(morphMarker);
		}

		createLayer(lexMarker);
		createLayer(IMPORTER_NAME);

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
		if (getGraph() == null) {
			getDocument().setDocumentGraph(SaltFactory.createSDocumentGraph());
		}

		// Read and clean up file
		LinkedList<String> lineList = readFileToList(file);

		// Go through the list and compile a list of tuples of marker and values
		// Map the tuple list whenever you hit a ref marker
		ListMultimap<String, List<String>> block = ArrayListMultimap.create();
		int lineListSize = lineList.size();
		ToolboxIdBlock idBlock = new ToolboxIdBlock();
		if (getProperties().hasIds()) {
			String idMarker = getProperties().getIdMarker();
			createLayer(idMarker);
			for (String line : lineList) {
				if (!line.startsWith("\\" + idMarker + " ")) {
					if (!line.startsWith("\\" + getProperties().getRefMarker() + " ")) {
						addLineToBlock(block, line);
					}
					else { // HIT a ref
						if (!idBlock.isHeaderMapped() && !block.isEmpty()) { // I.e., first ref in ID
							idBlock.setId(idMarker, "\\" + idMarker + " " + resolveListToString(block.get(idMarker).get(0)));
							idBlock.setIdAnnotations(convertIdHeader(block));
							idBlock.setHeaderMapped(true);
							block.clear();
							addLineToBlock(block, line);
						}
						else { // I.e., not first REF in ID
							SSpan span = mapRefToModel(block);
							if (span != null) {
								idBlock.getRefs().add(span);
							}
							block.clear();
							addLineToBlock(block, line);
						}
					}
				}
				else { // Hit an ID
					if (!isHeaderBlockMapped) {
						mapHeaderToModel(block);
						isHeaderBlockMapped = true;
						block.clear();
						addLineToBlock(block, line); // ID Line from first ID
					}
					else {
						SSpan span = mapRefToModel(block);
						if (span != null) {
							idBlock.getRefs().add(span);
						}
						mapIdToModel(idBlock);
						block.clear();
						idBlock.reset();
						addLineToBlock(block, line);
					}
				}
			}
			SSpan span = mapRefToModel(block);
			if (span != null) {
				idBlock.getRefs().add(span);
			}
			mapIdToModel(idBlock);
			block.clear();
			idBlock.reset();
		}
		else {
			for (String line : lineList) {
				if (!line.startsWith("\\" + getProperties().getRefMarker() + " ")) {
					addLineToBlock(block, line);
				}
				else { // Hit a ref marker
					if (!isHeaderBlockMapped) { // First hit of ref marker, i.e., block must be header block
						mapHeaderToModel(block);
						isHeaderBlockMapped = true;
						addProgress((double) (block.size() / lineListSize) * 100);
					}
					else {
						mapRefToModel(block);
						addProgress((double) (block.size() / lineListSize) * 100);
					}
					block.clear();
					// Add the ref marker to the new block!
					addLineToBlock(block, line);
				}
			}
			mapRefToModel(block);
			addProgress((double) (block.size() / lineListSize) * 100);
		}

		/*
		 * Map ref block to model once, because we cannot hit a ref marker at the end of the list anymore to trigger a block mapping process.
		 */

		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * TODO: Description
	 *
	 * @param list
	 * @return
	 */
	private String resolveListToString(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (String string : list) {
			sb.append(string);
			sb.append(" ");
		}
		return sb.toString().trim();
	}

	/**
	 * Creates a span for an IdBlock, spanning those tokens which are spanned by the IdBlock's refs.
	 *
	 * @param idBlock
	 */
	private void mapIdToModel(ToolboxIdBlock idBlock) {
		SSpan span = null;
		List<SToken> tokens = new ArrayList<>();
		for (SSpan refSpan : idBlock.getRefs()) {
			tokens.addAll(getGraph().getOverlappedTokens(refSpan));
		}
		span = getGraph().createSpan(tokens);
		span.setName(idBlock.getId());
		for (Entry<String, String> line : idBlock.getIdAnnotations().entrySet()) {
			span.createAnnotation(SALT_NAMESPACE_TOOLBOX, line.getKey(), line.getValue());
		}
		span.addLayer(layers.get(IMPORTER_NAME));
	}

	/**
	 * TODO: Description
	 *
	 * @param block
	 * @return
	 */
	private Map<String, String> convertIdHeader(ListMultimap<String, List<String>> block) {
		Map<String, String> IdAnnotations = new HashMap<>();
		for (Entry<String, List<String>> line : block.entries()) {
			StringBuilder sb = new StringBuilder();
			for (String string : line.getValue()) {
				sb.append(string);
				sb.append(" ");
			}
			IdAnnotations.put(line.getKey(), sb.toString().trim());
		}
		return IdAnnotations;
	}

	/**
	 * Creates and names an {@link SLayer} and puts it to the {@link #layers} {@link Map} under its name.
	 * 
	 * @param name
	 */
	private void createLayer(String name) {
		SLayer layer = SaltFactory.createSLayer();
		layer.setName(name);
		getGraph().addLayer(layer);
		layers.put(name, layer);
	}

	/**
	 * Adds a line from a Toolbox file to a block, i.e., a data structure that maps the line's marker to its content (a {@link List} of trimmed {@link String}s). If the marker exists, i.e., Toolbox has split up the content belonging to that
	 * marker over more than one line in the files, the marker String itself is ignored and the contents are added to the already existing list of Strings. If there is a marker without content, the line is dropped completely.
	 *
	 * @param block
	 * @param line
	 */
	private void addLineToBlock(ListMultimap<String, List<String>> block, String line) {
		String[] markerAndValues = line.split("\\s+");
		if (markerAndValues[0] == null) {
			LinkedList<String> markerandValuesList = new LinkedList<String>(Arrays.asList(markerAndValues));
			markerandValuesList.removeFirst();
			markerAndValues = (String[]) markerandValuesList.toArray();
		}
		String marker = markerAndValues[0].substring(1, markerAndValues[0].length());
		ArrayList<String> valueList = new ArrayList<>();
		if (marker.equals(getProperties().getMorphMarker()) && markerAndValues.length == 1) {
			if (!getProperties().getSubstituteMissingMorpologicalItems()) {
				throw new PepperModuleException("There are no morphological items for reference \"" + block.get(getProperties().getRefMarker()) + "\", and the subsitution of missing morphological items by a placeholder is switched off.");
			}
		}
		for (int i = 1; i < markerAndValues.length; i++) {
			valueList.add(markerAndValues[i]);
		}
		if (!block.containsKey(marker)) {
			block.put(marker, valueList);
		}
		else {
			if (getProperties().getUnitRefAnnotationMarkers() != null && Arrays.asList(getProperties().getUnitRefAnnotationMarkers().split("\\s*,\\s*")).contains(marker)) {
				block.put(marker, valueList);
			}
			else if (getProperties().getUnitRefDefinitionMarker().equals(marker)) {
				block.put(marker, valueList);
			}
			else {
				block.get(marker).get(0).addAll(valueList);
			}
		}
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
	private void mapHeaderToModel(ListMultimap<String, List<String>> block) {
		for (Entry<String, List<String>> line : block.entries()) {
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

	/**
	 * Returns whether a morpheme (the current morpheme) is the head morpheme. This is detected solely on the basis of notation, i.e. a "head morpheme" in the meaning used here is a morpheme which is neither an affix or a clitic.
	 *
	 * @param currentMorpheme
	 * @param lastMorpheme
	 * @return whether the first argument is a "head morpheme" as defined in the description.
	 */
	private boolean isHead(String currentMorpheme, String lastMorpheme) {
		if (lastMorpheme == null) {
			return true;
		}
		else {
			return !currentMorpheme.startsWith(getAffixDelimiter()) && !currentMorpheme.startsWith(getCliticsDelimiter()) && !lastMorpheme.endsWith(getAffixDelimiter()) && !lastMorpheme.endsWith(getCliticsDelimiter());
		}
	}

	/**
	 * Maps a reference block onto the Salt model, i.e., adding its marker lines as tokens, spans, and annotations to the document graph.
	 *
	 * @param block The reference block to proces s
	 * @return
	 */
	private SSpan mapRefToModel(ListMultimap<String, List<String>> block) {
		// TODO Re-implement catching empty lex lines before block is mapped for better performance
		List<List<String>> lexLine = block.get(getProperties().getLexMarker());
		if (lexLine.size() == 1 && lexLine.get(0).isEmpty()) {
			logger.info("Dropping reference " + block.get(getProperties().getRefMarker()) + " because it does not contain any lexical information with marker " + getProperties().getLexMarker() + "!");
			return null;
		}
		// Fix missing morphological items where needed, if there is morphology
		if (containsMorphology) {
			try {
			if (block.get(getProperties().getMorphMarker()).get(0).size() == 0) {
				if (getProperties().getSubstituteMissingMorpologicalItems()) {
					for (int i = 0; i < block.get(getProperties().getLexMarker()).get(0).size() - 1; i++) {
						block.get(getProperties().getMorphMarker()).get(0).add(getProperties().getMissingMorphologicalItemsPlaceholder() + " ");
					}
					block.get(getProperties().getMorphMarker()).get(0).add(getProperties().getMissingMorphologicalItemsPlaceholder());
				}
			}
			}
			catch (IndexOutOfBoundsException e) {
				throw new PepperModuleException("The reference \"" + block.get(getProperties().getRefMarker()) + "\" does not seem to contain any morphological items, although the parameter \"" + getProperties().getProperty(ToolboxTextImporterProperties.PROP_CONTAINS_MORPHOLOGY).getName() + "\" is set to true! Either add morphological annotation for this reference, with the marker \"" + getProperties().getMorphMarker() + "\", or set the property to false.", e);
			}
		}
		// Resolve properties
		String refMarker = getProperties().getRefMarker();
		logger.debug("Mapping ref block \"" + block.get(refMarker).toString() + "\".");
		String commaDelimRegex = "\\s*,\\s*";
		String[] delimiters = getProperties().getMorphemeDelimiters().split(commaDelimRegex);
		String lexMarker = getProperties().getLexMarker();
		String morphMarker = getProperties().getMorphMarker();
		String unitRefDefMarker = getProperties().getUnitRefDefinitionMarker();
		boolean hasDefinedUnitRefs = block.get(unitRefDefMarker) != null;
		affixDelimiter = delimiters[0].trim();
		cliticsDelimiter = delimiters[1].trim();
		Set<String> unitRefAnnotationsMarkers = null;
		if (hasUnitRefAnnotationProperty()) {
			unitRefAnnotationsMarkers = new HashSet<>(Arrays.asList(getProperties().getUnitRefAnnotationMarkers().split(commaDelimRegex)));
		}
		Set<String> lexAnnotationMarkers = null;
		if (hasLexAnnotationProperty()) { // FIXME: Implement so that all levels (but \tx???) can be empty
			lexAnnotationMarkers = new HashSet<>(Arrays.asList(getProperties().getLexAnnotationMarkers().split(commaDelimRegex)));
		}
		Set<String> morphAnnotationMarkers = null;
		if (containsMorphology) {
			 morphAnnotationMarkers = new HashSet<>(Arrays.asList(getProperties().getMorphAnnotationMarkers().split(commaDelimRegex)));
		}
		Set<String> documentMetaAnnotationMarkers = null;
		if (hasDocMetadataProperty()) {
			documentMetaAnnotationMarkers = new HashSet<>(Arrays.asList(getProperties().getDocMetadataMarkers().split(commaDelimRegex)));
		}
		Set<String> refMetaAnnotationMarkers = null;
		if (hasRefMetadataProperty()) {
			refMetaAnnotationMarkers = new HashSet<>(Arrays.asList(getProperties().getRefMetadataMarkers().split(commaDelimRegex)));
		}

		// Init String builders for STextualDSs
		StringBuilder morphDSBuilder = new StringBuilder();
		StringBuilder lexDSBuilder = new StringBuilder();

		// Init unit refs
		Map<String, int[]> definedUnitRefs = new HashMap<>();

		// Get text tokens for lex and morph
		List<String> lexicalTextTokens = null;
		try {
			lexicalTextTokens = block.get(lexMarker).get(0);
		}
		catch (IndexOutOfBoundsException e) {
			throw new PepperModuleException("This block (\"" + block.get(refMarker) + "\") does not contain a line with lexical items! Perhaps you have not defined the lexical marker (default: \\tx)?");
		}
		if (lexicalTextTokens == null) {
			throw new PepperModuleException("There is no lexical layer for this reference block. Aborting conversion. EVERY reference block must have a lexical layer.");
		}
		
		List<String> morphologicalTextTokens = null;
		if (containsMorphology) {
			try {
				morphologicalTextTokens = block.get(morphMarker).get(0);
			}
			catch (IndexOutOfBoundsException e) {
				throw new PepperModuleException("This block (\"" + block.get(refMarker) + "\") does not contain a line with morphological items! Perhaps you have not defined the morphological marker (default: \\mb)?");
			}
			if (morphologicalTextTokens == null) {
				throw new PepperModuleException("There is no morphological layer for this reference block. Aborting conversion. EVERY reference block must have a morphological layer.");
			}
	
			// Increment timeline
			getGraph().getTimeline().increasePointOfTime(morphologicalTextTokens.size());
		}

		// Init maps for annotation lines sorted by layer
		HashMap<String, List<String>> lexAnnotationLines = new HashMap<>();
		HashMap<String, List<String>> morphAnnotationLines = new HashMap<>();
		ListMultimap<String, List<String>> unitRefAnnotationLines = ArrayListMultimap.create();
		HashMap<String, List<String>> refAnnotationLines = new HashMap<>();
		HashMap<String, List<String>> docMetaAnnotationLines = new HashMap<>();
		HashMap<String, List<String>> refMetaAnnotationLines = new HashMap<>();

		HashMap<String, List<String>> refLine = new HashMap<>(1);

		// Build maps of annotation levels
		Iterator<Entry<String, List<String>>> mapIterator = block.entries().iterator();
		while (mapIterator.hasNext()) {
			Entry<String, List<String>> line = mapIterator.next();
			String key = line.getKey();
			if (key.equals(refMarker) || key.equals(morphMarker) || key.equals(lexMarker)) {
				if (key.equals(refMarker)) {
					refLine.put(key, line.getValue());
				}
				continue;
			}
			else if (key.equals(unitRefDefMarker)) {
				try {
					if (line.getValue().size() == 2) {// undefined definitor
						definedUnitRefs.put("", new int[] { Integer.valueOf(line.getValue().get(0)), Integer.valueOf(line.getValue().get(1)) });
					}
					else {
						definedUnitRefs.put(line.getValue().get(0), new int[] { Integer.valueOf(line.getValue().get(1)), Integer.valueOf(line.getValue().get(2)) });
					}
					continue;
				}
				catch (NumberFormatException e) {
					logger.error("The range of the unit ref definition {} is in the wrong format! Please format unit ref definitions as follows: \"\\{}\" <Definition name (String)> <Range from (Integer)> <Range to (Integer)>.", key, e);
				}
			}
			else if (lexAnnotationMarkers != null && lexAnnotationMarkers.contains(key)) {
				lexAnnotationLines.put(key, line.getValue());
			}
			else if (containsMorphology && morphAnnotationMarkers.contains(key)) {
				morphAnnotationLines.put(key, line.getValue());
			}
			else if (documentMetaAnnotationMarkers != null && documentMetaAnnotationMarkers.contains(key)) {
				docMetaAnnotationLines.put(key, line.getValue());
			}
			else if (refMetaAnnotationMarkers != null && refMetaAnnotationMarkers.contains(key)) {
				refMetaAnnotationLines.put(key, line.getValue());
			}
			else if (unitRefAnnotationsMarkers != null && unitRefAnnotationsMarkers.contains(key)) {
				/*
				 * It could be, however, that there are unitref-able lines which are annotations on the whole ref! I.e., when there is no unitref marker in the block, and the unitref-able line does not have unit ref markup. In this case,
				 * skip.
				 */
				if (!hasDefinedUnitRefs && !hasUnitRefMarkup(line.getValue(), morphologicalTextTokens.size(), lexicalTextTokens.size())) {
					break;
				}
				else {
					unitRefAnnotationLines.put(key, line.getValue());
				}
			}
			else { // "True" reference annotations, always on the whole reference
				refAnnotationLines.put(key, line.getValue());
			}
		}
		// Sanitize detached morpheme delimiters if configured & if morphology exists
		if (containsMorphology && getProperties().attachDetachedMorphemeDelimiter()) {
			// Check if it is necessary to sanitize morphology tokens
			if (morphologicalTextTokens.contains(cliticsDelimiter) || morphologicalTextTokens.contains(affixDelimiter)) {
				ListIterator<String> iterator = morphologicalTextTokens.listIterator();
				while (iterator.hasNext()) {
					String token = (String) iterator.next();
					if (token.equals(cliticsDelimiter) || token.equals(affixDelimiter)) {
						if (getProperties().attachDetachedMorphemeDelimiterToSubsequentElement()) {
							String nextToken = morphologicalTextTokens.get(iterator.nextIndex());
							morphologicalTextTokens.set(iterator.nextIndex(), token + nextToken);
							iterator.remove();
						}
						else {
							String previousToken = morphologicalTextTokens.get(iterator.previousIndex() - 1);
							morphologicalTextTokens.set(iterator.previousIndex() - 1, previousToken + token);
							iterator.remove();
						}
					}
				}
				/*
				 * Now sanitize all annotation lines as they should have the same detached delimiter if the file is aligned correctly.
				 */
				for (Entry<String, List<String>> annotationLine : morphAnnotationLines.entrySet()) {
					List<String> list = annotationLine.getValue();
					iterator = list.listIterator();
					while (iterator.hasNext()) {
						String token = (String) iterator.next();
						if (token.equals(cliticsDelimiter) || token.equals(affixDelimiter)) {
							if (getProperties().attachDetachedMorphemeDelimiterToSubsequentElement()) {
								String nextToken = list.get(iterator.nextIndex());
								list.set(iterator.nextIndex(), token + nextToken);
								iterator.remove();
							}
							else {
								String previousToken = list.get(iterator.previousIndex() - 1);
								list.set(iterator.previousIndex() - 1, previousToken + token);
								iterator.remove();
							}
						}
					}
				}

			}
		}

		// Build morphological and lexical tokens with annotations, and build data sources
		int morphCounter = 0;
		int lexIndex = -1;
		List<SToken> spanMorphTokens = new ArrayList<>();
		List<SToken> spanLexTokens = new ArrayList<>();
		String lastMorpheme = null;
		if (containsMorphology) {
			for (int morphIndex = 0; morphIndex < morphologicalTextTokens.size(); morphIndex++) {
				String morphemeTextToken = morphologicalTextTokens.get(morphIndex);
				if (lastMorpheme != null && isHead(morphemeTextToken, lastMorpheme)) {
					lexIndex++;
					String lexicalTextToken = null;
					if (getProperties().fixAlignment()) {
						if (lexIndex >= lexicalTextTokens.size()) {
							lexicalTextTokens.add(lexIndex, getProperties().getFixAlignmentString());
							logger.info("Fixed broken alignment for missing lexical token by adding the alignment warning string \"" + getProperties().getFixAlignmentString() + "\" to the line \"" + lexicalTextTokens + "\".");
							for (Entry<String, List<String>> line : lexAnnotationLines.entrySet()) {
								line.getValue().add(getProperties().getFixAlignmentString());
								logger.info("Fixed broken alignment for missing lexical token by adding the alignment warning string \"" + getProperties().getFixAlignmentString() + "\" to the annotation line \"" + line.getKey() + "\".");
							}
						}
					}
					try {
						lexicalTextToken = lexicalTextTokens.get(lexIndex);
					}
					catch (Exception e) {
						throw new PepperModuleException("\n\n#####\nAlignment problem in block \"" + block.get(refMarker).toString() + "\"! There are only " + lexicalTextTokens.size() + " lexical items in the reference block, but the importer is trying to access item number " + (lexIndex + 1) + ".\nThis indicates an issue with the alignment, i.e., for n lexical units there are at least n+1 morphological units, of which each should represent exactly one lexical unit.\nPlease fix the alignment between lexical and morphological lines in this block!\n#####\n\nStack trace:\n", e);
					}

					// Map
					lexDSBuilder.append(lexDSBuilder.length() > 0 ? " " : "").append(lexicalTextToken);
					lastLexToken = createLexicalToken(lexicalTextToken, lexIndex, morphCounter, lexAnnotationLines, refLine.values().toString());
					spanLexTokens.add(lastLexToken);

					// Prepare next iteration
					morphCounter = 0;

				}
				morphDSBuilder.append(morphDSBuilder.length() > 0 ? " " : "").append(morphemeTextToken);
				morphCounter++;
				lastMorphToken = createMorphToken(morphemeTextToken, morphIndex, morphAnnotationLines, refLine.values().toString());
				spanMorphTokens.add(lastMorphToken);
				lastMorpheme = morphemeTextToken;
				// Check if there are lexical tokens without morphological counterparts
				if (morphIndex == morphologicalTextTokens.size() - 1 && lexIndex + 1 < lexicalTextTokens.size() - 1) {
					// Last morpheme reached, but the next lexIndex is still smaller than the number of lexical tokens
					if (getProperties().fixAlignment()) {
						morphologicalTextTokens.add(getProperties().getFixAlignmentString());
						logger.info("Fixed broken alignment between lexical items and morphological items. Added to the line of morphological items to match the number of lexical items by appending the alignment warning string \"" + getProperties().getFixAlignmentString() + "\".");
					}
					else {
						if (!getProperties().ignoreMissingMorphemes()) {
							throw new PepperModuleException("There are lexical items without morphological counterparts, i.e., not all lexical items have been annotated for morphology. The importer is not set to ignore this. Please annotate the morphology for block \"" + block.get(refMarker) + "\".");
						}
					}
				}

			}
		}
		else {
			for (int i = 0; i < lexicalTextTokens.size(); i++) {
				lexIndex++;
				String lexicalTextToken = null;
				lexicalTextToken = lexicalTextTokens.get(lexIndex);
				lexDSBuilder.append(lexDSBuilder.length() > 0 ? " " : "").append(lexicalTextToken);
				lastLexToken = createLexicalToken(lexicalTextToken, lexIndex, morphCounter, lexAnnotationLines, refLine.values().toString());
				spanLexTokens.add(lastLexToken);
			}
		}
		// Finalize the timeline
		// Map the loop's final iteration results as it has ended.
		if (containsMorphology) {
			int iterationsForMissingLexItems = 1;
			if (getProperties().ignoreMissingMorphemes()) {
				iterationsForMissingLexItems = lexicalTextTokens.size() - (lexIndex + 1);
			}
			for (int i = 0; i < iterationsForMissingLexItems; i++) {
				lexIndex++;
				String lexicalTextToken = null;
				if (getProperties().fixAlignment()) {
					if (lexIndex >= lexicalTextTokens.size()) {
						lexicalTextTokens.add(lexIndex, getProperties().getFixAlignmentString());
						logger.info("Fixed broken alignment for missing lexical token by adding the alignment warning string \"" + getProperties().getFixAlignmentString() + "\" to the line + \"" + lexicalTextTokens + "\".");
						for (Entry<String, List<String>> line : lexAnnotationLines.entrySet()) {
							line.getValue().add(getProperties().getFixAlignmentString());
							logger.info("Fixed broken alignment for missing lexical token by adding the alignment warning string \"" + getProperties().getFixAlignmentString() + "\" to the annotation line + \"" + line.getKey() + "\".");
						}
					}
				}
				try {
					lexicalTextToken = lexicalTextTokens.get(lexIndex);
				}
				catch (Exception e) {
					throw new PepperModuleException("\n\n#####\nAlignment problem in block \"" + block.get(refMarker).toString() + "\"! There are only " + lexicalTextTokens.size() + " lexical items in the reference block, but the importer is trying to access item number " + (lexIndex + 1) + ".\nThis indicates an issue with the alignment, i.e., for n lexical units there are at least n+1 morphological units, of which each should represent exactly one lexical unit.\nPlease fix the alignment between lexical and morphological lines in this block!\n#####\n\nStack trace:\n", e);
				}

				lexDSBuilder.append(lexDSBuilder.length() > 0 ? " " : "").append(lexicalTextToken);
				lastLexToken = createLexicalToken(lexicalTextToken, lexIndex, morphCounter, lexAnnotationLines, refLine.values().toString());
				spanLexTokens.add(lastLexToken);
			}
		}
		else {
			
		}

		// Add to data sources
		String oldLexDSText = getLexicalTextualDS().getText();
		getLexicalTextualDS().setText(oldLexDSText.concat(oldLexDSText.isEmpty() ? "" : " ").concat(lexDSBuilder.toString()));
		String oldMorphDSText = getMorphologicalTextualDS().getText();
		getMorphologicalTextualDS().setText(oldMorphDSText.concat(oldMorphDSText.isEmpty() ? "" : " ").concat(morphDSBuilder.toString()));

		// Build the general ref span and add ref-level annotations
		SSpan ref = createRefSpan(refLine, spanMorphTokens, spanLexTokens, refAnnotationLines, refMetaAnnotationLines);
		for (Entry<String, List<String>> unitRefLine : unitRefAnnotationLines.entries()) {
			createUnitRefSpan(getGraph().getSortedTokenByText(spanMorphTokens), getGraph().getSortedTokenByText(spanLexTokens), unitRefLine, definedUnitRefs);
		}

		createDocumentMetaAnnotations(docMetaAnnotationLines);
		return ref;
	}

	/**
	 * TODO: Description
	 *
	 * @param sortedMorphTokens
	 * @param unitRefLine
	 * @return
	 */
	private SSpan createUnitRefSpan(List<SToken> sortedMorphTokens, List<SToken> sortedLexTokens, Entry<String, List<String>> unitRefLine, Map<String, int[]> definedUnitRefs) {
		List<SToken> targetedTokens = new ArrayList<>();
		List<String> lineContents = unitRefLine.getValue();
		if (hasUnitRefMarkup(lineContents, sortedMorphTokens.size(), sortedLexTokens.size())) { // If line has direct unitref markup
			if (lineContents.get(0).equals(getProperties().getMorphMarker())) {
				for (int i = Integer.parseInt(lineContents.get(1)); i <= (Integer.parseInt(lineContents.get(2))); i++) {
					targetedTokens.add(sortedMorphTokens.get(i));
				}
			}
			else if (lineContents.get(0).equals(getProperties().getLexMarker())) { // Line has defined unit ref
				for (int i = Integer.parseInt(lineContents.get(1)); i < Integer.parseInt(lineContents.get(2)) + 1; i++) {
					targetedTokens.add(sortedLexTokens.get(i));
				}
			}
			lineContents = lineContents.subList(3, lineContents.size());
		}
		else if (definedUnitRefs.size() == 1) { // There is exactly one unit ref definition without a definitor
			for (int[] value : definedUnitRefs.values()) {
				for (int i = value[0]; i < value[1] + 1; i++) {
					targetedTokens.add(sortedMorphTokens.get(i));
				}
			}
		}
		else {// if (definedUnitRefs.get(lineContents.get(0)) != null && (getMorphologicalTextualDS().getText().startsWith(lineContents.get(0)) || getLexicalTextualDS().getText().startsWith(lineContents.get(0)))) { // Line has defined unit
				// ref
			int[] value = definedUnitRefs.get(lineContents.get(0));
			for (int i = value[0]; i < value[1] + 1; i++) {
				targetedTokens.add(sortedMorphTokens.get(i));
			}
			lineContents = lineContents.subList(1, lineContents.size());
		}
		SSpan span = getGraph().createSpan(targetedTokens);
		StringBuilder sb = new StringBuilder();
		for (String s : lineContents) {
			sb.append(s);
			sb.append(" ");
		}
		String key = unitRefLine.getKey();
		span.createAnnotation(SALT_NAMESPACE_TOOLBOX, key, sb.toString().trim());
		span.setName(sb.toString().trim());

		// TODO Check whether we really want all the spans in the same layer!
		// if (layers.get(key) == null) {
		// createLayer(key);
		// }
		// span.addLayer(layers.get(key));
		span.addLayer(layers.get(IMPORTER_NAME));

		return span;
	}

	/**
	 * Expects a list of lines, i.e., markers and contents belonging to that marker, and from them creates meta annotations on the document itself.
	 *
	 * @param docMetaAnnotationLines
	 */
	private void createDocumentMetaAnnotations(HashMap<String, List<String>> docMetaAnnotationLines) {
		SMetaAnnotation annotation;
		for (Entry<String, List<String>> line : docMetaAnnotationLines.entrySet()) {
			String marker = line.getKey();
			String qualifiedId = SALT_NAMESPACE_TOOLBOX + "::" + marker;
			StringBuilder builder = new StringBuilder();
			for (String s : line.getValue()) {
				builder.append(s);
				builder.append(" ");
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

	/**
	 * Creates a span over tokens. This span is annotated with all annotations from the {@code refAnnotationLines} parameter and added to the reference layer.
	 * <p>
	 * Note that spans are <strong>always</strong> built over morphological tokens!
	 *
	 * @param refLine
	 * @param spanMorphTokens
	 * @param refAnnotationLines
	 * @param refMetaAnnotationLines
	 */
	private SSpan createRefSpan(Map<String, List<String>> refLine, List<SToken> spanMorphTokens, List<SToken> spanLexTokens, Map<String, List<String>> refAnnotationLines, HashMap<String, List<String>> refMetaAnnotationLines) {
		SSpan span = null;
		if (spanMorphTokens.size() > 0) {
			span = getGraph().createSpan(spanMorphTokens);
		}
		else if (spanLexTokens.size() > 0) {
			span = getGraph().createSpan(spanLexTokens);
		}

		for (Entry<String, List<String>> line : refAnnotationLines.entrySet()) {
			List<String> lineContents = line.getValue();
			StringBuilder sb = new StringBuilder();
			for (String s : lineContents) {
				sb.append(s);
				sb.append(" ");
			}
			span.createAnnotation(SALT_NAMESPACE_TOOLBOX, line.getKey(), sb.toString().trim());
		}
		for (Entry<String, List<String>> line : refMetaAnnotationLines.entrySet()) {
			StringBuilder sb = new StringBuilder();
			for (String s : line.getValue()) {
				sb.append(s);
				sb.append(" ");
			}
			span.createMetaAnnotation(SALT_NAMESPACE_TOOLBOX, line.getKey(), sb.toString().trim());
		}
		StringBuilder sb = new StringBuilder();
		for (String s : refLine.get(getProperties().getRefMarker())) {
			sb.append(s);
			sb.append(" ");
		}
		span.createAnnotation(SALT_NAMESPACE_TOOLBOX, getProperties().getRefMarker(), sb.toString().trim());
		span.setName(sb.toString().trim());

		span.addLayer(layers.get(IMPORTER_NAME));
		return span;
	}

	/**
	 * Tests whether the first 3 objects in the list are compatible to <strong>free</strong> unit ref markup, i.e., if the first object is a String equalling either the morph or the lex marker, and if the second and third object are
	 * parseable to {@link Integer}s, and if the second item is not smaller than 0, and if the third item is not larger than the last index in the list of tokens for the marker.
	 *
	 * @param lineContents
	 * @param numberOfLexemes
	 * @param numberOfMorphemes
	 * @return whether the first three objects in the first argument adhere to unit ref markup format
	 */
	private boolean hasUnitRefMarkup(List<String> lineContents, int numberOfMorphemes, int numberOfLexemes) {
		String marker = lineContents.get(0);
		if (!(marker.equals(getProperties().getLexMarker()) || marker.equals(getProperties().getMorphMarker()))) {
			return false;
		}
		Integer from = null;
		Integer to = null;
		try {
			from = Integer.valueOf(lineContents.get(1));
			to = Integer.valueOf(lineContents.get(2));
		}
		catch (NumberFormatException e) {
			return false;
		}
		if (marker.equals(getProperties().getLexMarker())) {
			if (from < 0 || to > (numberOfLexemes - 1)) {
				return false;
			}
		}
		else {
			if (from < 0 || to > (numberOfMorphemes - 1)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Creates a morphological token in the Salt graph.
	 * <p>
	 * More precisely, creates a token in this document's {@link SDocumentGraph} on the data source object defined by {@link #morphologicalTextualDS},<br>
	 * then creates an order relation from the last token (if it exists) to this token,<br>
	 * then links the token to exactly one step on the timeline (i.e., e.g., [7,8]),<br>
	 * then adds the token to the morphological layer.
	 *
	 * @param morpheme
	 * @param start
	 * @param morphIndex
	 * @param morphAnnotationLines
	 * @param lastMorphToken
	 * @param timelineCounter
	 * @param refId Can be used for error reporting
	 * @return
	 */
	private SToken createMorphToken(String morpheme, int morphIndex, HashMap<String, List<String>> morphAnnotationLines, String refId) {
		SToken token = getGraph().createToken(getMorphologicalTextualDS(), morphDSIndex, morphDSIndex += morpheme.length());
		morphDSIndex++; // Accounting for whitespace
		for (Entry<String, List<String>> annotationLine : morphAnnotationLines.entrySet()) {
			if (getProperties().fixAlignment()) {
				if (morphIndex >= annotationLine.getValue().size()) {
					annotationLine.getValue().add(getProperties().getFixAlignmentString());
					logger.info("Fixed broken alignment between morphological tokens and their annotations by adding the alignment warning string \"" + getProperties().getFixAlignmentString() + "\" to the annotation line + \"" + annotationLine.getKey() + "\".");
				}
			}
			try {
				token.createAnnotation(SALT_NAMESPACE_TOOLBOX, annotationLine.getKey(), annotationLine.getValue().get(morphIndex));
			}
			catch (IndexOutOfBoundsException e) {
				throw new PepperModuleException("\n\n#####\nAlignment problem in block \"" + refId + "\" with morpheme \'" + morpheme + "\' and its annotation on level \'" + annotationLine.getKey() + "\'!\nThere are only " + annotationLine.getValue().size() + " values on this line, whereas the importer is trying to access value number " + (morphIndex + 1) + "...\nAs the importer does not allow null elements for annotations, please fix the annotations and/or their alignment in this block!\n#####\n\nStack trace:\n", e);
			}
		}

		if (lastMorphToken != null) {
			SOrderRelation rel = SaltFactory.createSOrderRelation();
			rel.setSource(lastMorphToken);
			rel.setTarget(token);
			rel.setName("morph");
			rel.setType(getMorphologicalTextualDS().getName());
			getGraph().addRelation(rel);
		}

		STimelineRelation timeRel = SaltFactory.createSTimelineRelation();
		timeRel.setSource(token);
		timeRel.setTarget(getGraph().getTimeline());

		timeRel.setStart(morphTimelineIndex);
		morphTimelineIndex += 1;
		timeRel.setEnd(morphTimelineIndex);
		getGraph().addRelation(timeRel);

		token.addLayer(layers.get(getProperties().getMorphMarker()));

		/*
		 * ############# FIXME ############### Du to a bug in the ANNISExporter, with multiple segmentations, token annotations are only displayed, when they are "in" a span, therefore, create a dummy span for each token.
		 */
		getGraph().createSpan(token).addLayer(layers.get(IMPORTER_NAME));
		// ############ FIXME ###############

		return token;
	}

	/**
	 * Creates a lexical token in the Salt graph.
	 * <p>
	 * More precisely, creates a token in this document's {@link SDocumentGraph} on the data source object defined by {@link #lexicalTextualDS},<br>
	 * then creates an order relation from the last token (if it exists) to this token,<br>
	 * then links the token to the number of index spans on the timeline as defined by the {@code timelineUnits} parameter,<br>
	 * then adds the token to the lexical layer.
	 *
	 * @param lexicalTextToken
	 * @param start
	 * @param timelineUnits
	 * @param lexIndex
	 * @param lexAnnotationLines
	 * @param lastLexToken
	 * @param timelineCounter
	 * @return
	 */
	private SToken createLexicalToken(String lexicalTextToken, int lexIndex, int timelineUnits, HashMap<String, List<String>> lexAnnotationLines, String refId) {

		SToken token = getGraph().createToken(getLexicalTextualDS(), lexDSIndex, lexDSIndex += lexicalTextToken.length());
		lexDSIndex++; // Accounting for whitespace
		for (Entry<String, List<String>> annotationLine : lexAnnotationLines.entrySet()) {
			if (getProperties().fixAlignment()) {
				if (lexIndex >= annotationLine.getValue().size()) {
					annotationLine.getValue().add(getProperties().getFixAlignmentString());
					logger.info("Fixed broken alignment between morphological tokens and their annotations by adding the alignment warning string \"" + getProperties().getFixAlignmentString() + "\" to the annotation line + \"" + annotationLine.getKey() + "\".");
				}
			}
			try {
				token.createAnnotation(SALT_NAMESPACE_TOOLBOX, annotationLine.getKey(), annotationLine.getValue().get(lexIndex));
			}
			catch (IndexOutOfBoundsException e) {
				throw new PepperModuleException("\n\n#####\nAlignment problem in block \"" + refId + "\" with lexical unit \'" + lexicalTextToken + "\' and its annotation on level \'" + annotationLine.getKey() + "\'!\nThere are only " + annotationLine.getValue().size() + " values on this line, whereas the importer is trying to access value number " + (lexIndex + 1) + "...\nAs the importer does not allow null elements for annotations, please fix the annotations and/or their alignment in this block!\n#####\n\nStack trace:\n", e);
			}
		}
		if (lastLexToken != null) {
			SOrderRelation rel = SaltFactory.createSOrderRelation();
			rel.setSource(lastLexToken);
			rel.setTarget(token);
			rel.setName("lex");
			rel.setType(getLexicalTextualDS().getName());
			getGraph().addRelation(rel);
		}

		STimelineRelation timeRel = SaltFactory.createSTimelineRelation();
		timeRel.setSource(token);
		timeRel.setTarget(getGraph().getTimeline());

		timeRel.setStart(lexTimelineIndex);
		lexTimelineIndex += timelineUnits;
		timeRel.setEnd(lexTimelineIndex);
		getGraph().addRelation(timeRel);

		token.addLayer(layers.get(getProperties().getLexMarker()));

		/*
		 * ############# FIXME ############### Du to a bug in the ANNISExporter, with multiple segmentations, token annotations are only displayed, when they are "in" a span, therefore, create a dummy span for each token.
		 */
		getGraph().createSpan(token).addLayer(layers.get(IMPORTER_NAME));
		// ############ FIXME ###############

		return token;
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

	/**
	 * @return the graph
	 */
	public SDocumentGraph getGraph() {
		return graph;
	}

	/**
	 * @param graph the graph to set
	 */
	public void setGraph(SDocumentGraph graph) {
		this.graph = graph;
	}

	private boolean hasDocMetadataProperty() {
		return (getProperties().getDocMetadataMarkers() != null);
	}

	private boolean hasRefMetadataProperty() {
		return (getProperties().getRefMetadataMarkers() != null);
	}

	private boolean hasLexAnnotationProperty() {
		return (getProperties().getLexAnnotationMarkers() != null);
	}

	private boolean hasUnitRefAnnotationProperty() {
		return (getProperties().getUnitRefAnnotationMarkers() != null);
	}

}

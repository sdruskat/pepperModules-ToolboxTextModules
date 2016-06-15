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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mapper for the Toolbox text format.
 * <p>
 * The mapping works as follows.
 * The Toolbox file is read line by line, cleaned up and compiled
 * into a list of {@link MarkerValuesTuple}s, which represent a
 * Toolbox marker and its "values", i.e., the remainder of the marker
 * line (and subsequent unmarked non-empty lines). 
 * <p>
 * This list is iterated and all lines belonging to a "block" -
 * i.e., all lines following a reference marker, including the line with
 * the reference marker itself - are subsequently mapped onto the Salt document graph.
 * <p>
 * One special case is the lines before the first reference marker, 
 * which are assumed to belong to the "header". These lines are
 * mapped onto meta annotations on the document itself.
 * <p>
 * The marked lines following a reference marker are mapped onto
 * the Salt document graph as follows.
 * The values from the lexical and morphology markers (as defined
 * by {@link ToolboxTextImporterProperties#PROP_LEX_MARKER} and
 * {@link ToolboxTextImporterProperties#PROP_MORPH_MARKER}) are
 * used as two separate tokenizations on two separate {@link STextualDS}s,
 * which are synchronized via a {@link STimeline}. 
 * The tokens are subsequently annotated
 * with the values from the text annotations markers (as defined by
 * {@link ToolboxTextImporterProperties#PROP_LEX_ANNOTATION_MARKERS})
 * and the morphology annotation markers (as defined by
 * {@link ToolboxTextImporterProperties#PROP_MORPH_ANNOTATION_MARKERS})
 * respectively. (<strong>NOTE:</strong>It is assumed that the 
 * number of annotations always matches the number of tokens 
 * per reference!).
 * <p>
 * The reference marker itself is modeled as a span over the <em>text</em>
 * marker tokens of the reference. All markers <em>not</em>
 * belonging to either lexical or morphology annotation layer are assumed
 * to be reference level annotations and are as such modeled as annotations
 * on the reference span.
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
	 * The timeline used to anchor both lexical and morphological tokens.
	 */
	private final STimeline timeline = SaltFactory.createSTimeline();

	/**
	 * The textual data source containing the lexical source text of the whole document.
	 */
	private final STextualDS lexicalTextualDS = SaltFactory.createSTextualDS();

	/**
	 * The textual data source containing the "morphological source text", i.e.,
	 * the concatenated morpheme-based morphological units.
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
		URI resource = getResourceURI();
		File file = null;
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
		List<MarkerValuesTuple> block = new ArrayList<>();
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
				block.add(new MarkerValuesTuple(marker, valueList));
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
		/* Map ref block to model once, because we cannot hit a 
		 * ref marker at the end of the list anymore to trigger
		 * a block mapping process.
		 */
		mapRefToModel(block);
		
		getDocument().getDocumentGraph().addNode(getMorphologicalTextualDS());

		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * Reads a {@link File} and writes it to a {@link LinkedList},
	 * line by line, removing empty lines and trimming non-empty lines
	 * in the process
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
			} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
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
			else if (attachmentLine != null){
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
	 * Maps the header block onto the Salt model, i.e.,
	 * adding its marker lines as meta annotations on
	 * the document.
	 *
	 * @param block The header block to process
	 */
	private void mapHeaderToModel(List<MarkerValuesTuple> block) {
		for (MarkerValuesTuple line : block) {
			String marker = line.getMarker();
			String qualifiedId = SALT_NAMESPACE_TOOLBOX + "::" + marker;
			SMetaAnnotation annotation;
			// One meta annotation per marker, hence concatenate the list of values.
			StringBuilder builder = new StringBuilder();
			for (String val : line.getValues()) {
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
	 * Maps a reference block onto the Salt model,
	 * i.e., adding its marker lines as tokens,
	 * spans, and annotations to the document graph.
	 *
	 * @param block The reference block to process
	 */
	private void mapRefToModel(List<MarkerValuesTuple> block) {
		for (MarkerValuesTuple line : block) {
			// Morphology marker: Add to STextualDS, create Tokens, connect to timeline
			if (line.getMarker().equals(getProperties().getMorphMarker())) {
				StringBuilder morphSourceTextBuilder = new StringBuilder();
				StringBuilder morphologicalUnitBuilder = new StringBuilder();
				for (String value : line.getValues()) {
					if (!value.startsWith(getAffixDelimiter()) && !value.startsWith(getCliticsDelimiter())) {
						if (morphologicalUnitBuilder.length() > 0) { 
							/* 
							 * There is at least one morpheme, and possibly affixes and/or clitics 
							 * represented in the builder, but now we've hit a new morpheme, so
							 * first of all append what's in the unit builder to the source text builder.
							 */
							morphSourceTextBuilder.append(" ").append(morphologicalUnitBuilder.toString());
							morphologicalUnitBuilder.setLength(0);
						}
					}
					morphologicalUnitBuilder.append(value);
				}
				/* 
				 * Append what's in the unit builder one last time, as once we
				 * hit the end of the list, the appending won't be triggered
				 * as no new morpheme is hit. 
				 */
				morphSourceTextBuilder.append(" ").append(morphologicalUnitBuilder.toString());
				// Now append the source text to the morphological data source
				String currentDataSource = getMorphologicalTextualDS().getText();
				int currentDataSourceLength = currentDataSource.length(); // Needed for correct tokenization
				// Replace all double whitespaces with 1 whitespace. Two whitespaces occur when marker runs over more than one line...  
				String updatedDataSource = currentDataSource.concat(" ").concat(morphSourceTextBuilder.toString());
				// Replace all double whitespaces with 1 whitespace. Two whitespaces occur when marker runs over more than one line...  
				getMorphologicalTextualDS().setText(updatedDataSource.replaceAll("\\s{2}", " "));
				
				// FIXME: Check how duplicate whitespaces can creep in!
				// FIXME: Check for affixes "asd-" !
				// FIXME: Check how to treat non lexical information = ELAN indices, etc.
				
			}
		}
	}

	/**
	 * @return the timeline
	 */
	private STimeline getTimeline() {
		return timeline;
	}

	/**
	 * @return the lexicalTextualDS
	 */
	private STextualDS getLexicalTextualDS() {
		return lexicalTextualDS;
	}

	/**
	 * @return the morphologicalTextualDS
	 */
	private STextualDS getMorphologicalTextualDS() {
		return morphologicalTextualDS;
	}
	
	/**
	 * @return the affixDelimiter
	 */
	private String getAffixDelimiter() {
		return affixDelimiter;
	}

	/**
	 * @param affixDelimiter the affixDelimiter to set
	 */
	private void setAffixDelimiter(String affixDelimiter) {
		this.affixDelimiter = affixDelimiter;
	}

	/**
	 * @return the cliticsDelimiter
	 */
	private String getCliticsDelimiter() {
		return cliticsDelimiter;
	}

	/**
	 * @param cliticsDelimiter the cliticsDelimiter to set
	 */
	private void setCliticsDelimiter(String cliticsDelimiter) {
		this.cliticsDelimiter = cliticsDelimiter;
	}

	/* 
	 * @copydoc @see org.corpus_tools.pepper.impl.PepperMapperImpl#getProperties()
	 */
	@Override
	public ToolboxTextImporterProperties getProperties() {
		return (ToolboxTextImporterProperties) super.getProperties();
	}

	/**
	 * A tuple of a Toolbox marker and its values, i.e., the content of the marker line excluding the marker itself.
	 * <p>
	 * The marker is a {@link String}, the values are represented as a {@link List<String>}.
	 *
	 * @author Stephan Druskat <mail@sdruskat.net>
	 */
	protected class MarkerValuesTuple {
	
		private String marker;
		private List<String> values;
	
		/**
		 * Constructor taking the marker and values list as arguments.
		 */
		public MarkerValuesTuple(String marker, List<String> values) {
			this.marker = marker;
			this.values = values;
		}
	
		/**
		 * @return the marker
		 */
		public String getMarker() {
			return marker;
		}
	
		/**
		 * @return the values
		 */
		public List<String> getValues() {
			return values;
		}
	
	}

}

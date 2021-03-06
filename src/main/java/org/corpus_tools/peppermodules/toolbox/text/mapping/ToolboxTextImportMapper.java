/*******************************************************************************
 * Copyright (c) 2016, 2018ff. Stephan Druskat
 * Exploitation rights for this version belong exclusively to Humboldt-Universität zu Berlin
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
 *     Stephan Druskat (mail@sdruskat.net) - initial API and implementation
 *******************************************************************************/
package org.corpus_tools.peppermodules.toolbox.text.mapping;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.toolbox.text.AbstractToolboxTextMapper;
import org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextImporterProperties;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;
import com.google.common.io.CountingInputStream;

/**
 * A mapper for Toolbox Text to Salt mapping.
 * 
 * This class is responsible for the actual mapping process.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class ToolboxTextImportMapper extends AbstractToolboxTextMapper {
	
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextImportMapper.class);
	
	private final Long headerEndOffset;
	private final Map<Long, List<Long>> refMap;
	private final Range<Long> idRange;
	
	private final boolean hasMorphology;
	/**
	 * A map mapping the defined marker to its target marker,
	 * for normalization purposes.
	 */
	private final Map<String, String> markerMap = new HashMap<>(4);
	private ToolboxTextImporterProperties properties = null;
	
	/**
	 * Maps marker {@link String}s to {@link SLayer}s belonging to that marker.
	 */
	private Map<String, SLayer> layers = new HashMap<>();

	/**
	 * @param headerEndOffset
	 * @param refMap
	 * @param idRange
	 * @param hasMorphology 
	 */
	public ToolboxTextImportMapper(Long headerEndOffset, Map<Long, List<Long>> refMap, Range<Long> idRange, boolean hasMorphology) {
		this.idRange = idRange;
		this.refMap = refMap;
		this.headerEndOffset = headerEndOffset;
		this.hasMorphology = hasMorphology;
	}

	/**
	 * {@inheritDoc PepperMapper#setDocument(SDocument)}
	 * 
	 * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		// Set up marker map
		if (getProperties() instanceof ToolboxTextImporterProperties) {
			this.properties = (ToolboxTextImporterProperties) getProperties();
		}
		if (properties.normalizeMarkers()) {
			markerMap.put(properties.getRefMarker(), properties.getProperty(ToolboxTextImporterProperties.PROP_REF_MARKER).getDefaultValue().toString());
			markerMap.put(properties.getSubrefDefinitionMarker(), properties.getProperty(ToolboxTextImporterProperties.PROP_SUB_REF_DEFINITION_MARKER).getDefaultValue().toString());
			markerMap.put(properties.getLexMarker(), properties.getProperty(ToolboxTextImporterProperties.PROP_LEX_MARKER).getDefaultValue().toString());
			markerMap.put(properties.getMorphMarker(), properties.getProperty(ToolboxTextImporterProperties.PROP_MORPH_MARKER).getDefaultValue().toString());
		}
		else {
			markerMap.put(properties.getRefMarker(), properties.getRefMarker());
			markerMap.put(properties.getSubrefDefinitionMarker(), properties.getSubrefDefinitionMarker());
			markerMap.put(properties.getLexMarker(), properties.getLexMarker());
			markerMap.put(properties.getMorphMarker(), properties.getMorphMarker());
		}

		SDocumentGraph graph = getDocument().getDocumentGraph();
		if (graph == null) {
			graph = SaltFactory.createSDocumentGraph();
			getDocument().setDocumentGraph(graph);
		}
		File file = new File(getResourceURI().toFileString());
		
		// Create layers
		getLayer(getMarker(markerMap.get(getProperties().getLexMarker())));
		getLayer(getMarker(markerMap.get(getProperties().getMorphMarker())));
		getLayer(getMarker(markerMap.get(getProperties().getRefMarker())));
		
		// Create a timeline to linearize lexical and morphological tokens
		graph.createTimeline();
		
		// Create primary data sources
		final STextualDS lexDS = graph.createTextualDS("");
		lexDS.setName(getMarker(markerMap.get(getProperties().getLexMarker())));
		STextualDS morphDS = null;
		if (hasMorphology) {
			morphDS = graph.createTextualDS("");
			morphDS.setName(getMarker(markerMap.get(getProperties().getMorphMarker())));
		}
 
		try (RandomAccessFile raf = new RandomAccessFile(file, "r"); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			int currentByte;
			// Whether this document is an orphan, i.e., contains no \refs
			boolean isOrphan = false;
			List<Long> refOffsets = refMap.get(idRange.lowerEndpoint());
			if (isMonolithic()) {
				/*
				 * If a document is monolithic, i.e., contains no \id markers,
				 * it cannot have a header. Hence, no header will be mapped, and
				 * instead of calling an instance of DocumentHeaderMapper, the
				 * name of the SDocument will be set to the file name sans extension.
				 */
				String fileName = file.getName();
				graph.getDocument().setName(fileName.substring(0, fileName.lastIndexOf('.')));
				refOffsets = refMap.get(-1L);
			}
			else {
				// The offset at which the header of this document ends
				long docHeaderEndOffset;
				/*
				 * If the list of \ref offsets for this document is not empty, i.e.,
				 * the document is not an orphan, the headerEndOffset is the offset 
				 * of the start of the first ref, i.e., the first offset in the list.
				 * Otherwise, it's the end of the idRange, i.e., its upper endpoint.
				 */
				if (refOffsets.size() == 0) {
					docHeaderEndOffset = idRange.upperEndpoint();
					isOrphan = true;
				}
				else {
					docHeaderEndOffset = refOffsets.get(0);
				}
				// Parse document header
				raf.seek(idRange.lowerEndpoint());
				while ((currentByte = raf.read()) > 0 && raf.getFilePointer() <= docHeaderEndOffset) {
					bos.write(currentByte);
				}
				// Create and call a mapper for the document header
				DocumentHeaderMapper documentHeaderMapper = new DocumentHeaderMapper(getProperties(), graph, bos.toString().trim());
				documentHeaderMapper.map();
				bos.reset();
			}

			// Parse refs if the document is not an orphan
			if (!isOrphan) {
				for (Long refOffset : refOffsets) {
					Long nextOffset;
					if (refOffsets.indexOf(refOffset) == refOffsets.size() - 1) {
						nextOffset = idRange.upperEndpoint();
					}
					else {
						nextOffset = refOffsets.get(refOffsets.indexOf(refOffset) + 1);
					}
					raf.seek(refOffset);
					while ((currentByte = raf.read()) > 0 && raf.getFilePointer() <= nextOffset) {
						bos.write(currentByte);
					}
					// Create and call a mapper for the \ref section
					RefMapper refMapper = new RefMapper(markerMap, getProperties(), graph, bos.toString().trim(), hasMorphology, lexDS, morphDS, layers);
					boolean refHasLexicalTokens = refMapper.map();
					if (refHasLexicalTokens) {
						SubrefMapper subrefMapper = new SubrefMapper(markerMap, getProperties(), graph, refMapper.getRefData(), refMapper.getLexTokens(), refMapper.getMorphTokens(), refMapper.getMarkerContentMap(), refMapper.refHasMorphology());
						subrefMapper.map();
					}
					bos.reset();
				}
				getLayer(getMarker(markerMap.get(getProperties().getLexMarker()))).addNode(lexDS);
				if (morphDS != null) {
					getLayer(getMarker(markerMap.get(getProperties().getMorphMarker()))).addNode(morphDS);
				}
			}
		}
		catch (FileNotFoundException e) {
			throw new PepperModuleException("The corpus file " + getResourceURI().toFileString() + " has not been found.", e);
		}
		catch (IOException e) {
			throw new PepperModuleException("Error while parsing the corpus file " + getResourceURI().toFileString() + "!", e);
		}
		return DOCUMENT_STATUS.COMPLETED;
	}

	/**
	 * {@inheritDoc PepperMapper#setCorpus(SCorpus)}
	 * 
	 * Streams the corpus file from 0 to the byte offset of the end
	 * of the header (i.e., the offset of the first \id or \ref) and
	 * writes all read bytes into a {@link ByteArrayOutputStream}.
	 * Every time the stream encounters a marker (starting with \),
	 * it calls the private getMarkerAndValueFromString(String) passing
	 * the (non-empty) contents of the {@link ByteArrayOutputStream} 
	 * as parameter. The resulting two elements of the returned
	 * {@link String} array are then written to a new {@link SMetaAnnotation}
	 * on the {@link SDocument}, using the first element as annotation name,
	 * and the second element as annotation value, or the empty {@link String}
	 * if no second element exists. Consecutively, the 
	 * {@link ByteArrayOutputStream} is reset to take up the next line.
	 * 
	 */
	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		File file = new File(getResourceURI().toFileString());
		/*
		 * headerEndOffset should only be null if the corpus represents
		 * a directory. In this case, don't parse obviously.
		 */
		if (!file.isDirectory() && headerEndOffset != null) {
			headerParsing: try (CountingInputStream stream = new CountingInputStream(new BufferedInputStream(new FileInputStream(file))); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				int currentByte;
				String[] markerAndValue = null;
				while ((currentByte = stream.read()) > 0 && stream.getCount() < headerEndOffset) {
					/*
					 * If we hit a new marker, split the trimmed contents of bos
					 * into marker and value and write them to a meta 
					 * annotation, unless the marker is the \ref marker, 
					 * which means that we have hit an orphan \ref before the 
					 * first \id marker, in which case write and abort!
					 */
					if (currentByte == '\\' && bos.size() > 0) {
						markerAndValue = getMarkerAndValueFromString(bos.toString().trim());
						if (!markerAndValue[0].equals(getProperties().getRefMarker())) {
							// If the meta annotation already exists, overwrite
							// its value
							if (getCorpus()
									.getMetaAnnotation(SALT_NAMESPACE_TOOLBOX + "::" + markerAndValue[0]) != null) {
								getCorpus().getMetaAnnotation(SALT_NAMESPACE_TOOLBOX + "::" + markerAndValue[0])
										.setValue(markerAndValue.length > 1 ? markerAndValue[1] : "");
							}
							else {
								getCorpus().createMetaAnnotation(SALT_NAMESPACE_TOOLBOX, markerAndValue[0],
										markerAndValue.length > 1 ? markerAndValue[1] : "");
							}
							bos.reset();
						}
						else {
							logger.warn("Found an orphan \\ref in the corpus header of \"" + file.getName() + "\" at byte " + stream.getCount() + ".\nWill neglect it and stop parsing the corpus header, and write the content that has already been parsed to the model.");
							// Break the whole try block
							break headerParsing;
						}
					}
					bos.write(currentByte);
				}
				// bos still contains the last marker line, so write that to the
				// list of marker lines.
				markerAndValue = getMarkerAndValueFromString(bos.toString().trim());
				getCorpus().createMetaAnnotation(SALT_NAMESPACE_TOOLBOX, markerAndValue[0], markerAndValue.length > 1 ? markerAndValue[1] : "");
			}
			catch (FileNotFoundException e) {
				throw new PepperModuleException("The corpus file " + getResourceURI().toFileString() + " has not been found.", e);
			}
			catch (IOException e) {
				throw new PepperModuleException("Error while parsing the corpus file " + getResourceURI().toFileString() + "!", e);
			}
		}
		return DOCUMENT_STATUS.COMPLETED;
	}

	/**
	 * Takes a {@link String} parameter, and splits its substring from
	 * index 1 (dropping the \ of the marker) once at the first whitespace,
	 * the resulting {@link String} of which it returns.
	 * 
	 * Example: the {@link String} "<code>\id Some id or other</code>"
	 * returns <code>[id, Some id or other]</code>.
	 *
	 * @param line
	 * @return A {@link String} array with two elements, of which the first 
	 * is the marker {@link String} sans backslash, and of which the second 
	 * is the contents of the marked line
	 */
	private String[] getMarkerAndValueFromString(String line) {
		return line.substring(1).split(" ", 2);
	}

	/**
	 * Verifies whether the document to be mapped is the
	 * single document in a monolithic corpus, i.e., a
	 * corpus which contains only one document, which in
	 * turn contains all the \refs.
	 * 
	 * In this case, the Toolbox file will not contain any
	 * `\id`s but only `\ref`s.
	 *
	 * @return
	 */
	private boolean isMonolithic() {
		return refMap.size() == 1 && refMap.containsKey(-1L);
	}
	
	/**
	 * Creates and names an {@link SLayer} and puts it to the {@link #layers}
	 * {@link Map} under its name.
	 * 
	 * @param name
	 * @return 
	 */
	private SLayer getLayer(String name) {
		SLayer layer = null;
		if ((layer = layers.get(name)) == null) {
			layer = SaltFactory.createSLayer();
			layer.setName(name);
			getDocument().getDocumentGraph().addLayer(layer);
			layers.put(name, layer);
		}
		return layer;
	}

	protected String getMarker(String string) {
		Map<String, String> markerMap = null;
		if (!(markerMap = ((ToolboxTextImporterProperties) getProperties()).getMarkerMap()).isEmpty()) {
			String idName = markerMap.get(string);
			if (idName != null) {
				return idName;
			}
		}
		return string;
	}


	/**
	 * @return the properties
	 */
	@Override
	public ToolboxTextImporterProperties getProperties() {
		return (ToolboxTextImporterProperties) super.getProperties();
	}

}

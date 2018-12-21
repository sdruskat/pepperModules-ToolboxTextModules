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
package org.corpus_tools.peppermodules.toolbox.text;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.corpus_tools.pepper.impl.PepperImporterImpl;
import org.corpus_tools.pepper.modules.PepperImporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleNotReadyException;
import org.corpus_tools.peppermodules.toolbox.text.mapping.ToolboxTextImportMapper;
import org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextImporterProperties;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.graph.IdentifiableElement;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;

/**
 * The point-of-entry class for import of Toolbox text to a Salt model.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 * 
 */
@Component(name = "ToolboxTextImporterComponent", factory = "PepperImporterComponentFactory")
public class ToolboxTextImporter extends PepperImporterImpl implements PepperImporter {

	/**
	 * A static logger.
	 **/
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextImporter.class);
	
	private Map<Identifier, ToolboxParseBean> parseMap = new HashMap<>();

	/**
	 * Constructor setting metadata mostly.
	 */
	public ToolboxTextImporter() {
		super();
		setName("ToolboxTextImporter");
		setVersion("1.0.0");
		setSupplierContact(URI.createURI("stephan.druskat@hu-berlin.de"));
		setSupplierHomepage(URI.createURI("https://github.com/sdruskat/pepperModules-ToolboxTextModules"));
		setDesc("An importer for the text-based format written by SIL Toolbox (as opposed to the respective XML format).");
		addSupportedFormat("toolbox-text", "3.0", null);
		this.setProperties(new ToolboxTextImporterProperties());
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.corpus_tools.pepper.impl.PepperImporterImpl#importCorpusStructure(org
	 * .corpus_tools.salt.common.SCorpusGraph)
	 */
	@Override
	public void importCorpusStructure(SCorpusGraph corpusGraph) throws PepperModuleException {
		this.setCorpusGraph(corpusGraph);
		URI fileURI = getCorpusDesc().getCorpusPath();
		File corpusFile = new File(fileURI.toFileString());
		importCorpusStructure(corpusGraph, null, corpusFile);
	}

	/**
	 * Imports the corpus structure from the Toolbox text files.
	 *
	 * @param corpusGraph The {@link SCorpusGraph} to contain the data
	 * @param parent The parent {@link SCorpus} for the data
	 * @param corpusFile The {@link File} (can be a directory) that contains the Toolbox data
	 */
	private void importCorpusStructure(SCorpusGraph corpusGraph, SCorpus parent, File corpusFile) {
		if (corpusFile == null) {
			throw new PepperModuleException("Corpus file is null. Exiting!");
		}
		List<Long> idOffsets;
		Map<Long, List<Long>> refMap;
		Long headerEndOffset = null;
		boolean monolithic = false;
		final Map<Identifier, Long> offsetMap = new HashMap<>();
		Map<Long, Boolean> idStructureMap;
		URI corpusFileURI = URI.createFileURI(corpusFile.getAbsolutePath());
		String corpusFileName = corpusFile.getName();
		if (corpusFile.isDirectory()) {
			SCorpus subCorpus = corpusGraph.createCorpus(parent, corpusFileName);
			getIdentifier2ResourceTable().put(subCorpus.getIdentifier(), corpusFileURI);
			File[] files = corpusFile.listFiles();
			if (files == null) {
				throw new PepperModuleException("Corpus file " + corpusFile.getName() + " has no children!");
			}
			for (File child : files) {
				importCorpusStructure(corpusGraph, subCorpus, child);
			}
		}
		else if (corpusFile.isFile()) {
			// Create a corpus for the file
			SCorpus subCorpus = corpusGraph.createCorpus(parent, corpusFileName.substring(0, corpusFileName.lastIndexOf('.')));
			getIdentifier2ResourceTable().put(subCorpus.getIdentifier(), corpusFileURI);

			ToolboxTextImporterProperties p;
			// Parse file
			ToolboxTextSegmentationParser parser = new ToolboxTextSegmentationParser(corpusFile, (p = getProperties()).getIdMarker(), p.getRefMarker(), p.getMorphMarker());
			parser.parse();
			idOffsets = parser.getIdOffsets();
			refMap = parser.getRefMap();
			idStructureMap = parser.getIdStructureMap();
			// Do some sanity checks on the documents, and write irregularities
			// to log
			if (idOffsets.isEmpty()) {
				// Corpus has no \ids
				if (refMap.isEmpty()) {
					// Corpus also has no \refs
					throw new PepperModuleException("The corpus file " + corpusFile.getAbsolutePath() + " contains neither \\ids nor \\refs. Aborting import!");
				}
				else {
					if (refMap.size() == 1 && refMap.containsKey(-1L)) {
						// Corpus has no \ids but \refs, so create it with a
						// single document containing all refs
						headerEndOffset = refMap.get(-1L).get(0);
						monolithic = true;
					}
				}
			}
			else {
				// Corpus has \ids
				if (refMap.isEmpty()) {
					// Corpus has only empty \ids, so log a warning but create
					// the empty documents
					logger.info("The corpus file " + corpusFile.getAbsolutePath() + " contains \\ids, but none of them contain \\refs. Will create empty documents with only metadata.");
				}
				else if (refMap.containsKey(-1L)) {
					// There are \refs that are not attached to an \id, so log a
					// warning and drop them
					List<Long> orphanRefOffsets = refMap.get(-1L);
					warnAboutOrphanRefs(orphanRefOffsets, corpusFile);
					refMap.remove(-1L);
					if (refMap.isEmpty()) {
						// Corpus now only has empty \ids, so log a warning but
						// create the empty documents
						logger.info("The corpus file " + corpusFile.getAbsolutePath() + " contains \\ids, but none of them contain \\refs. Will create empty documents with only metadata.");
					}
					else {
						// Corpus has ids, of which some might be orphans!
						// Orphans must be caught in ToolboxTextImportMapper.
					}

				}
				headerEndOffset = idOffsets.get(0);
			}
			// Create documents for \ids in file
			if (!monolithic) {
				for (Long idOffset : idOffsets) {
					String name = ToolboxTextDocumentNameParser.parseId(idOffset, getProperties().getIdMarker(), corpusFile, getProperties().normalizeDocNames());
					SDocument doc = corpusGraph.createDocument(subCorpus, name);
					getIdentifier2ResourceTable().put(doc.getIdentifier(), corpusFileURI);
					offsetMap.put(doc.getIdentifier(), idOffset);
					parseMap.put(doc.getIdentifier(), new ToolboxParseBean(idOffsets, refMap, headerEndOffset, monolithic, offsetMap, idStructureMap));
				}
			}
			else {
				SDocument doc = corpusGraph.createDocument(subCorpus, corpusFileName.substring(0, corpusFileName.lastIndexOf('.')));
				getIdentifier2ResourceTable().put(doc.getIdentifier(), corpusFileURI);
				parseMap.put(doc.getIdentifier(), new ToolboxParseBean(idOffsets, refMap, headerEndOffset, monolithic, offsetMap, idStructureMap));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.corpus_tools.pepper.impl.PepperModuleImpl#createPepperMapper(org.corpus_tools.salt.graph.Identifier)
	 */
	@Override
	public PepperMapper createPepperMapper(Identifier identifier) {
		ToolboxParseBean parse = parseMap.get(identifier);
		PepperMapper mapper = null;
		URI resource = null;
		try {
			resource = getIdentifier2ResourceTable().get(identifier);
		}
		catch (NullPointerException e) {
			throw new PepperModuleException("Cannot create a Pepper mapper! The identifier is null!");
		}
		if (identifier.getIdentifiableElement() == null) {
			throw new PepperModuleException(
					"Cannot create a Pepper mapper! The identifier " + identifier + "'s identifiable element is null!");
		}
		IdentifiableElement element = identifier.getIdentifiableElement();
		if (element instanceof SDocument) {
			Range<Long> idRange = null;
			if (parse.monolithic) {
				idRange = Range.closed(parse.headerEndOffset, new File(resource.toFileString()).length());
			}
			else {
				// Get range for ID, pass to constructor, pass refmap
				Long idOffset = parse.offsetMap.get(identifier);
				int indexOfNextIdOffset = parse.idOffsets.indexOf(idOffset) + 1;
				Long nextIdOffset = new File(resource.toFileString()).length();
				// Check if this offset is the last one in the list
				if (!(indexOfNextIdOffset == parse.idOffsets.size())) {
					nextIdOffset = parse.idOffsets.get(indexOfNextIdOffset);
				}
				idRange = Range.closed(parse.offsetMap.get(identifier), nextIdOffset);
			}
			mapper = new ToolboxTextImportMapper(null, parse.refMap, idRange, parse.idStructureMap.get(idRange.lowerEndpoint()));
		}
		else if (element instanceof SCorpus) {
			if (parse != null) {
				mapper = new ToolboxTextImportMapper(parse.headerEndOffset, null, null, false);
			}
			else { // If there is no parse, we are dealing with a directory!
				mapper = new ToolboxTextImportMapper(null, null, null, false);
			}
		}
		else {
			throw new PepperModuleException("Cannot create a mapper for elements that are neither of type SCorpus or SDocument.");
		}
		mapper.setResourceURI(resource);
		return (mapper);
	}

	/* (non-Javadoc)
	 * @see org.corpus_tools.pepper.impl.PepperModuleImpl#isReadyToStart()
	 */
	@Override
	public boolean isReadyToStart() throws PepperModuleNotReadyException {
		for (String fileExtension : getProperties().getFileExtensions().split("\\s*,\\s*")) {
			getDocumentEndings().add(fileExtension);
		}
		return (super.isReadyToStart());
	}

	/* (non-Javadoc)
	 * @see org.corpus_tools.pepper.impl.PepperModuleImpl#getProperties()
	 */
	@Override
	public ToolboxTextImporterProperties getProperties() {
		return (ToolboxTextImporterProperties) super.getProperties();
	}

	/**
	 * @param orphanRefOffsets
	 * @param file
	 */
	private void warnAboutOrphanRefs(List<Long> orphanRefOffsets, File file) {
		logger.warn(file.getName() + ": Found \\refs that do not belong to any \\ids! Those will not be processed.");
	}

	/**
	 * A bean-like container for parsed data.
	 *
	 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
	 * 
	 */
	private class ToolboxParseBean {
	
		private final List<Long> idOffsets;
		private final Map<Long, List<Long>> refMap;
		private final Long headerEndOffset;
		private final boolean monolithic;
		private final Map<Identifier, Long> offsetMap;
		private final Map<Long, Boolean> idStructureMap;

		/**
		 * @param idOffsets
		 * @param refMap
		 * @param headerEndOffset
		 * @param monolithic
		 * @param offsetMap
		 * @param idStructureMap
		 */
		private ToolboxParseBean(List<Long> idOffsets, Map<Long, List<Long>> refMap, Long headerEndOffset, boolean monolithic, Map<Identifier, Long> offsetMap, Map<Long, Boolean> idStructureMap) {
			this.idOffsets = idOffsets;
			this.refMap = refMap;
			this.headerEndOffset = headerEndOffset;
			this.monolithic = monolithic;
			this.offsetMap = offsetMap;
			this.idStructureMap = idStructureMap;
			
		}
	
	}

}

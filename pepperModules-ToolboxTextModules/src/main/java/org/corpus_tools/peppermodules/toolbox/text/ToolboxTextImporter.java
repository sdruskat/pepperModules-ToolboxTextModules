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

import java.io.File; 
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.corpus_tools.pepper.impl.PepperImporterImpl;
import org.corpus_tools.pepper.modules.PepperImporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.PepperModule;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleNotReadyException;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(name = "ToolboxTextImporterComponent", factory = "PepperImporterComponentFactory")
public class ToolboxTextImporter extends PepperImporterImpl implements PepperImporter {
	
	/** this is a logger, for recording messages during program process, like debug messages**/
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextImporter.class);
	
	private final Map<Identifier, Long> offsetMap = new HashMap<>();

	private final Map<URI, Long> headerMap = new HashMap<>();

	private List<Long> sortedOffsets = new ArrayList<>();
	
	private StringBuilder warningBuilder = new StringBuilder();

	public ToolboxTextImporter() {
		super();
		logger.warn("Starting toolbox text module");
		setName("ToolboxTextImporter");
		setVersion("1.0.0-SNAPSHOT");
		setSupplierContact(URI.createURI("stephan.druskat@hu-berlin.de"));
		setSupplierHomepage(URI.createURI("http://corpus-tools.org"));
		setDesc("An importer for the text-based format written by SIL Toolbox (as opposed to the respective XML format).");
		addSupportedFormat("toolbox-text", "3.0", null);
		this.setProperties(new ToolboxTextImporterProperties());
	}
	

	/* (non-Javadoc)
	 * @see org.corpus_tools.pepper.impl.PepperImporterImpl#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)
	 */
	@Override
	public void importCorpusStructure(SCorpusGraph corpusGraph) throws PepperModuleException {
		this.setCorpusGraph(corpusGraph);
		URI fileURI = getCorpusDesc().getCorpusPath();
		File corpusFile = new File(fileURI.toFileString());
		importCorpusStructure(corpusGraph, null, corpusFile);
	}

	/**
	 * @param corpusGraph
	 * @param parent
	 * @param corpusFile
	 */
	private void importCorpusStructure(SCorpusGraph corpusGraph, SCorpus parent, File corpusFile) {
		
		if (corpusFile.isDirectory()) {
			SCorpus subCorpus = corpusGraph.createCorpus(parent, corpusFile.getName());
			getIdentifier2ResourceTable().put(subCorpus.getIdentifier(), URI.createFileURI(corpusFile.getAbsolutePath()));
			for (File child : corpusFile.listFiles()) {
				importCorpusStructure(corpusGraph, subCorpus, child);
			}
		}
		else if (corpusFile.isFile()) {
			URI corpusFileURI = URI.createFileURI(corpusFile.getAbsolutePath());
			Long fileLength = corpusFile.length();
			
			// Create a corpus for the file
	        SCorpus subCorpus = corpusGraph.createCorpus(parent, corpusFile.getName());
	        getIdentifier2ResourceTable().put(subCorpus.getIdentifier(), URI.createFileURI(corpusFile.getAbsolutePath()));
	        
	        // Parse file
	        ToolboxTextSegmentationParser parser = new ToolboxTextSegmentationParser(corpusFile, getProperties().getIdMarker(), getProperties().getRefMarker());
	        parser.parse();
	        
	        // Do some sanity checks on the documents, and write irregularities to log
	        if (parser.getIdOffsets().isEmpty() && parser.getRefMap().size() == 1 && parser.getRefMap().containsKey(-1L)) {
	        	// Corpus has no documents, i.e., build a corpus with exactly one document
	        }
	        else {
	        	// Corpus has documents
	        	if (!parser.getIdOffsets().isEmpty() && parser.getRefMap().isEmpty()) {
	        		// Corpus has only empty \ids, so log a warning
	        	}
	        	if (parser.getRefMap().containsKey(-1L)) {
	        		// There are \refs that are not attached to an \id, so log a warning and drop them
	        		List<Long> orphanRefOffsets = parser.getRefMap().get(-1L);
//	        		warnAboutOrphanRefs(orphanRefOffsets);
	        		parser.getRefMap().remove(-1L);
	        		if (parser.getRefMap().isEmpty()) {
	        			throw new PepperModuleException("There are neither \\id nor \\ref marked sections in the file " + corpusFile.getAbsolutePath() + "! Aborting import.");
	        		}
	        	}
	        	
	        }
	        
	        /* 
	         * TODO
	         * - Parse header
	         * - Parse ids
	         * - Parse refs
	         */
		}
//	        // Create documents for \ids in file
//	        ToolboxTextIdFinder finder = new ToolboxTextIdFinder(corpusFile, ((ToolboxTextImporterProperties) getProperties()).getIdMarker());
//	        Map<String, Long> idNameOffsetMap = finder.parse();
//	        for (Entry<String, Long> entry : idNameOffsetMap.entrySet()) {
//	        	SDocument doc = corpusGraph.createDocument(subCorpus, entry.getKey());
//	        	getIdentifier2ResourceTable().put(doc.getIdentifier(), corpusFileURI);
//	        	Long offset;
//	        	System.err.println("          ????????   " + entry.getKey() + "::" + entry.getValue());
//				offsetMap.put(doc.getIdentifier(), (offset = entry.getValue()));
//	        	sortedOffsets.add(offset);
//	        }
//	        headerMap.put(finder.getResourceHeader().getResource(), finder.getResourceHeader().getHeaderEndOffset());
//		}
		else {
			throw new PepperModuleException("Input " + corpusFile + " is neither directory nor file!");
		}
	}


	
	@Override
	public PepperMapper createPepperMapper(Identifier identifier) {
		Collections.sort(sortedOffsets);
		if (identifier == null) {
			throw new PepperModuleException("Cannot create a Pepper mapper! The identifier is null!");
		}
		else if (identifier.getIdentifiableElement() == null) {
			throw new PepperModuleException("Cannot create a Pepper mapper! The identifier " + identifier + "'s identifiable element is null!");
		}
		PepperMapper mapper = null;
		URI resource = getIdentifier2ResourceTable().get(identifier);
		if (((ToolboxTextImporterProperties) getProperties()).splitIdsToDocuments()) {
			Long offset = offsetMap.get(identifier);
			int offsetIndex = sortedOffsets.indexOf(offset);
			Long nextOffset;
			try {
				nextOffset = sortedOffsets.get(offsetIndex + 1);
			}
			catch (IndexOutOfBoundsException e) {
				URI fileURI = getCorpusDesc().getCorpusPath();
				File corpusFile = new File(fileURI.toFileString());
				nextOffset = corpusFile.length();
			}
			if (identifier.getIdentifiableElement() instanceof SCorpus) {
				mapper = new IdBasedToolboxTextMapper(headerMap.get(getIdentifier2ResourceTable().get(identifier)), resource);
			}
			else {
				mapper = new IdBasedToolboxTextMapper(offset, resource, nextOffset);
			}
		}
		else {
			mapper = new MonolithicToolboxTextMapper();
			mapper.setResourceURI(resource);
		}
		return (mapper);
	}

	/**
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong> <br/>
	 * This method is called by the pepper framework and returns if a corpus
	 * located at the given {@link URI} is importable by this importer. If yes,
	 * 1 must be returned, if no 0 must be returned. If it is not quite sure, if
	 * the given corpus is importable by this importer any value between 0 and 1
	 * can be returned. If this method is not overridden, null is returned.
	 * 
	 * @return 1 if corpus is importable, 0 if corpus is not importable, 0 < X <
	 *         1, if no definitive answer is possible, null if method is not
	 *         overridden
	 */
//	public Double isImportable(URI corpusPath) {
//		return (null);
//	}

	// =================================================== optional
	// ===================================================
	/**
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong> <br/>
	 * This method is called by the pepper framework after initializing this
	 * object and directly before start processing. Initializing means setting
	 * properties {@link PepperModuleProperties}, setting temporary files,
	 * resources etc. returns false or throws an exception in case of
	 * {@link PepperModule} instance is not ready for any reason. <br/>
	 * So if there is anything to do, before your importer can start working, do
	 * it here.
	 * 
	 * @return false, {@link PepperModule} instance is not ready for any reason,
	 *         true, else.
	 */
	@Override
	public boolean isReadyToStart() throws PepperModuleNotReadyException {
		for (String fileExtension : ((String) ((ToolboxTextImporterProperties) getProperties()).getFileExtensions()).split("\\s*,\\s*")) {
			getDocumentEndings().add(fileExtension);
		}
		return (super.isReadyToStart());
	}
	
	@Override
	public ToolboxTextImporterProperties getProperties() {
		return (ToolboxTextImporterProperties) super.getProperties();
	}
	
}

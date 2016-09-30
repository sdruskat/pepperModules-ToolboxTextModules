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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
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
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "ToolboxTextImporterComponent", factory = "PepperImporterComponentFactory")
public class ToolboxTextImporter extends PepperImporterImpl implements PepperImporter {

	/**
	 * this is a logger, for recording messages during program process, like
	 * debug messages
	 **/
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextImporter.class);

	private List<Long> idOffsets;

	private Map<Long, List<Long>> refMap;

	private Long headerEndOffset;

	private boolean monolithic = false;
	
	private final Map<Identifier, Long> offsetMap = new HashMap<>();

	public ToolboxTextImporter() {
		super();
		setName("ToolboxTextImporter");
		setVersion("1.0.0-SNAPSHOT");
		setSupplierContact(URI.createURI("stephan.druskat@hu-berlin.de"));
		setSupplierHomepage(URI.createURI("http://corpus-tools.org"));
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
	 * TODO: Description
	 *
	 * @param corpusGraph
	 * @param parent
	 * @param corpusFile
	 */
	private void importCorpusStructure(SCorpusGraph corpusGraph, SCorpus parent, File corpusFile) {
		URI corpusFileURI = URI.createFileURI(corpusFile.getAbsolutePath());
		String corpusFileName = corpusFile.getName();
		if (corpusFile.isDirectory()) {
			SCorpus subCorpus = corpusGraph.createCorpus(parent, corpusFileName);
			getIdentifier2ResourceTable().put(subCorpus.getIdentifier(), corpusFileURI);
			for (File child : corpusFile.listFiles()) {
				importCorpusStructure(corpusGraph, subCorpus, child);
			}
		} else if (corpusFile.isFile()) {
			// Create a corpus for the file
			SCorpus subCorpus = corpusGraph.createCorpus(parent, corpusFileName);
			getIdentifier2ResourceTable().put(subCorpus.getIdentifier(), corpusFileURI);

			// Parse file
			ToolboxTextSegmentationParser parser = new ToolboxTextSegmentationParser(corpusFile, getProperties().getIdMarker(), getProperties().getRefMarker());
			parser.parse();
			idOffsets = parser.getIdOffsets();
			refMap = parser.getRefMap();
			// Do some sanity checks on the documents, and write irregularities
			// to log
			if (idOffsets.isEmpty()) {
				// Corpus has no \ids
				if (refMap.isEmpty()) {
					// Corpus also has no \refs
					throw new PepperModuleException("The corpus file " + corpusFile.getAbsolutePath() + " contains neither \\ids nor \\refs. Aborting import!");
				} else {
					if (refMap.size() == 1 && refMap.containsKey(-1L)) {
						// Corpus has no \ids but \refs, so create it with a single document containing all refs
						headerEndOffset = refMap.get(-1L).get(0);
						setMonolithic(true);
					}
				}
			} else {
				// Corpus has \ids
				if (refMap.isEmpty()) {
					// Corpus has only empty \ids, so log a warning but create
					// the empty documents
					logger.warn("The corpus file " + corpusFile.getAbsolutePath() + " contains \\ids, but none of them contain \\refs. Will create empty documents with only metadata.");
				} else if (refMap.containsKey(-1L)) {
					// There are \refs that are not attached to an \id, so log a
					// warning and drop them
					List<Long> orphanRefOffsets = refMap.get(-1L);
					warnAboutOrphanRefs(orphanRefOffsets, corpusFile);
					refMap.remove(-1L);
					if (refMap.isEmpty()) {
						// Corpus now only has empty \ids, so log a warning but
						// create the empty documents
						logger.warn("The corpus file " + corpusFile.getAbsolutePath() + " contains \\ids, but none of them contain \\refs. Will create empty documents with only metadata.");
					}
				}
				headerEndOffset = idOffsets.get(0);
			}
			// Create documents for \ids in file
			if (!isMonolithic()) {
				for (Long idOffset : idOffsets) {
					SDocument doc = corpusGraph.createDocument(subCorpus, ToolboxTextDocumentNameParser.parseId(idOffset, getProperties().getIdMarker(), corpusFile));
					getIdentifier2ResourceTable().put(doc.getIdentifier(), corpusFileURI);
					offsetMap.put(doc.getIdentifier(), idOffset);
				}
			}
			else {
				SDocument doc = corpusGraph.createDocument(subCorpus, corpusFileName.substring(0, corpusFileName.lastIndexOf('.')));
				getIdentifier2ResourceTable().put(doc.getIdentifier(), corpusFileURI);
			}
		} 
		
		// ToolboxTextIdFinder finder = new ToolboxTextIdFinder(corpusFile,
		// ((ToolboxTextImporterProperties) getProperties()).getIdMarker());
		// Map<String, Long> idNameOffsetMap = finder.parse();
		// for (Entry<String, Long> entry : idNameOffsetMap.entrySet()) {
		// SDocument doc = corpusGraph.createDocument(subCorpus,
		// entry.getKey());
		// getIdentifier2ResourceTable().put(doc.getIdentifier(),
		// corpusFileURI);
		// Long offset;
		// System.err.println(" ???????? " + entry.getKey() + "::" +
		// entry.getValue());
		// offsetMap.put(doc.getIdentifier(), (offset = entry.getValue()));
		// sortedOffsets.add(offset);
		// }
		// headerMap.put(finder.getResourceHeader().getResource(),
		// finder.getResourceHeader().getHeaderEndOffset());
		// }
		// else {
		// throw new PepperModuleException("Input " + corpusFile + " is neither
		// directory nor file!");
		// }
	}

	@Override
	public PepperMapper createPepperMapper(Identifier identifier) {
		// TODO Check for isMonolitihic() and create mapper objects accordingly via different constructors
//		Collections.sort(sortedOffsets);
		if (identifier == null) {
			throw new PepperModuleException("Cannot create a Pepper mapper! The identifier is null!");
		} else if (identifier.getIdentifiableElement() == null) {
			throw new PepperModuleException("Cannot create a Pepper mapper! The identifier " + identifier + "'s identifiable element is null!");
		}
		PepperMapper mapper = null;
		URI resource = getIdentifier2ResourceTable().get(identifier);
//		if (getProperties().splitIdsToDocuments()) {
//			Long offset = offsetMap.get(identifier);
//			int offsetIndex = sortedOffsets.indexOf(offset);
//			Long nextOffset;
//			try {
//				nextOffset = sortedOffsets.get(offsetIndex + 1);
//			} catch (IndexOutOfBoundsException e) {
//				URI fileURI = getCorpusDesc().getCorpusPath();
//				File corpusFile = new File(fileURI.toFileString());
//				nextOffset = corpusFile.length();
//			}
//			if (identifier.getIdentifiableElement() instanceof SCorpus) {
//				mapper = new IdBasedToolboxTextMapper(headerMap.get(getIdentifier2ResourceTable().get(identifier)), resource);
//			} else {
//				mapper = new IdBasedToolboxTextMapper(offset, resource, nextOffset);
//			}
//		} else {
//			mapper = new MonolithicToolboxTextMapper();
//			mapper.setResourceURI(resource);
//		}
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
	// public Double isImportable(URI corpusPath) {
	// return (null);
	// }

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
		for (String fileExtension : getProperties().getFileExtensions().split("\\s*,\\s*")) {
			getDocumentEndings().add(fileExtension);
		}
		return (super.isReadyToStart());
	}

	@Override
	public ToolboxTextImporterProperties getProperties() {
		return (ToolboxTextImporterProperties) super.getProperties();
	}

	/**
	 * Extracts orphaned \refs by streaming the input file from offset to offset
	 * for recorded \ref orphans taken from the parameter list (or the next \id
	 * or EOF in case of the last orphaned \ref offset in the list), and
	 * building them into a string that will be logged at level warn (which in
	 * Pepper versions >= 3.1 will append a separate file containing war-level
	 * logs only).
	 *
	 * @param orphanRefOffsets
	 */
	private void warnAboutOrphanRefs(List<Long> orphanRefOffsets, File file) {
		StringBuilder warningBuilder = new StringBuilder("====================================================\n" + "================= W A R N I N G ! ==================\n" + "====================================================\n" + "\nFound \\refs that do not belong to any \\ids!\n" + "The following orphaned \\refs will not be processed:\n\n" + "====================================================\n");
		for (Long orphanRefOffset : orphanRefOffsets) {
			int offsetIndex = orphanRefOffsets.indexOf(orphanRefOffset);
			Long nextOffset = null;
			if (orphanRefOffsets.size() == offsetIndex + 1) {
				// offsetIndex is the last index in the list, so leave
				// nextOffset == null
			} else {
				nextOffset = orphanRefOffsets.get(offsetIndex + 1);
			}
			try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
				if (nextOffset != null) {
					// Read until the next offset and append text to warning
					// message
					byte[] buf = new byte[nextOffset.intValue() - orphanRefOffset.intValue()];
					raf.seek(orphanRefOffset);
					raf.readFully(buf);
					String readRef = new String(buf, StandardCharsets.UTF_8);
					warningBuilder.append(readRef + "====================================================\n");
				} else {
					// Read until the next instance of \id (or EOF) and append
					// text to warning message
					raf.seek(orphanRefOffset);
					String line, marker;
					while ((line = raf.readLine()) != null) {
						if (!line.trim().isEmpty()) {
							// Extract the marker from the line and check
							// whether it is an \id marker
							marker = line.split("\\s+")[0].trim().substring(1);
							if (!(marker.equals(getProperties().getIdMarker()))) {
								// Append line to warning message
								warningBuilder.append(line + "\n");
							} else {
								break;
							}
						}
					}
				}
			} catch (IOException e) {
				throw new PepperModuleException("Could not read file " + file.getAbsolutePath() + "!", e);
			}
		}
		// Log warning
		logger.warn(warningBuilder.toString() + "====================================================\n====================================================\n====================================================\n");
	}

	/**
	 * @return the headerEndOffset
	 */
	public Long getHeaderEndOffset() {
		return headerEndOffset;
	}

	/**
	 * @return the monolithic
	 */
	boolean isMonolithic() {
		return monolithic;
	}

	/**
	 * @param monolithic
	 *            the monolithic to set
	 */
	private void setMonolithic(boolean monolithic) {
		this.monolithic = monolithic;
	}

}

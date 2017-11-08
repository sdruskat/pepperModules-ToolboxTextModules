package org.corpus_tools.peppermodules.toolbox.text.mapping;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.peppermodules.toolbox.text.AbstractToolboxTextMapper;
import org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties;
import org.corpus_tools.peppermodules.toolbox.text.utils.ToolboxTextModulesUtils;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.util.DataSourceSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import java.io.IOException;

/**
 * // TODO Add description
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 * 
 */
public class ToolboxTextExportMapper extends AbstractToolboxTextMapper {
	
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextImportMapper.class);
	
	private ToolboxTextExporterProperties properties = null;
	
	/**
	 * Stores each document-structure to location given by
	 * {@link #getResourceURI()}.
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		// Set properties
		if (getProperties() instanceof ToolboxTextExporterProperties) {
			this.properties = (ToolboxTextExporterProperties) getProperties();
		}
		else {
			logger.error("Properties are not of the right type, expected {}, found {}.", ToolboxTextExporterProperties.class.getName(), getProperties().getClass());
			return DOCUMENT_STATUS.FAILED;
		}
		// workaround to deal with a bug in Salt
		SCorpusGraph sCorpusGraph = getDocument().getGraph();

		DOCUMENT_STATUS status = map();

		// workaround to deal with a bug in Salt
		if (getDocument().getGraph() == null) {
			getDocument().setGraph(sCorpusGraph);
		}
		addProgress(1.0);
		return (status);
	}
	
	private DOCUMENT_STATUS map() {
		SDocumentGraph graph = getDocument().getDocumentGraph();
		URI outputURI = URI.create(getResourceURI().toString());
		final List<String> lines = new ArrayList<>();

		// Write Toolbox header line
		// Until I have found out what the integer bit is for: 🍀Eh 'mon the Hoops!🍀
		lines.add("\\_sh v3.0 1888 Pepper ToolboxTextExporter");
		lines.add("");
		
		// Add document annotations and meta annotations
		for (SAnnotation a : getDocument().getAnnotations()) {
			lines.add("\\" + a.getName() + " " + a.getValue_STEXT());
		}
		for (SMetaAnnotation ma : getDocument().getMetaAnnotations()) {
			lines.add("\\" + ma.getName() + " " + ma.getValue_STEXT());
		}
		lines.add("");
		
		/*
		 * Map all \ids
		 */
		// Get \id spans
		String idSpanName = properties.getIdSpanLayer();
		SLayer layer = graph.getLayerByName(idSpanName).get(0);
		Set<SNode> potentialIdNodes = layer.getNodes();
		Set<SSpan> idSpans = new HashSet<>();
		for (SNode node : potentialIdNodes) {
			if (node instanceof SSpan) {
				idSpans.add((SSpan) node);
			}
		}
		// Order \id spans by indices of tokens they're covering
		List<SSpan> orderedIdSpans = ToolboxTextModulesUtils.sortSpansByTextCoverageOfIncludedToken(idSpans);
		for (SSpan idSpan : orderedIdSpans) {
			lines.add("\\id " + idSpan.getAnnotation(properties.getIdIdentifierAnnotation()).getValue_STEXT());
			System.err.println(idSpan.getAnnotations());
			for (SAnnotation a : idSpan.getAnnotations()) {
				if (!a.getQName().equals(properties.getIdIdentifierAnnotation())) {
					lines.add("\\" + a.getName() + " " + a.getValue_STEXT());
				}
			}
			lines.add(" ");
			// Get refs per id
			List<DataSourceSequence> dsSequences = graph.getOverlappedDataSourceSequence(idSpan, SALT_TYPE.STEXT_OVERLAPPING_RELATION);
			// We are currently working with only 1 data source! TODO Add this info to docs
			assert dsSequences.size() == 1;
			DataSourceSequence dsSequence = dsSequences.get(0);
			List<SNode> allNodes = graph.getNodesBySequence(dsSequence);
			Set<SSpan> refSpans = new HashSet<>();
			for (SNode node : allNodes) {
				if (node instanceof SSpan && node.getLayers().contains(graph.getLayerByName(properties.getRefSpanLayer()).get(0))) {
					refSpans.add((SSpan) node);
				}
			}
			List<SSpan> orderedRefs = ToolboxTextModulesUtils.sortSpansByTextCoverageOfIncludedToken(refSpans);
			for (SSpan refSpan : orderedRefs) {
				lines.add("\\ref " + refSpan.getAnnotation(properties.getRefIdentifierAnnotation()).getValue_STEXT());
				for (SAnnotation a : refSpan.getAnnotations()) {
					if (!a.getQName().equals(properties.getRefIdentifierAnnotation())) {
						lines.add("\\" + a.getName() + " " + a.getValue_STEXT());
					}
				}
				lines.add("");
			}
		}
		
		
		// Write the Toolbox text files
		// Remove the document file if it exists
		try {
			Files.delete(Paths.get(outputURI));
		}
		catch (NoSuchFileException e) {
			// That's fine, just keep going.
			try {
				Files.createFile(Paths.get(outputURI));
			}
			catch (IOException e1) {
				logger.error("Cannot create file {}!", outputURI.toString(), e);
				return DOCUMENT_STATUS.FAILED;
			}
		}
		catch (IOException e) {
			logger.error("Error deleting existing document {}!", outputURI.toString(), e);
			return DOCUMENT_STATUS.FAILED;
		}
		try {
			Files.write(Paths.get(outputURI), lines, UTF_8, APPEND, CREATE);
			return DOCUMENT_STATUS.COMPLETED;
		}
		catch (IOException e) {
			logger.error("Error writing to document file {}!", outputURI.toString(), e);
			return DOCUMENT_STATUS.FAILED;
		}
	}

	/**
	 * Storing the corpus-structure once
	 */
	@Override
	public DOCUMENT_STATUS mapSCorpus() {
//		List<SNode> roots = getCorpus().getGraph().getRoots();
//		if ((roots != null) && (!roots.isEmpty())) {
//			if (getCorpus().equals(roots.get(0))) {
////				SaltUtil.save_DOT(getCorpus().getGraph(), getResourceURI());
//			}
//		}
		return (DOCUMENT_STATUS.COMPLETED);
	}
}
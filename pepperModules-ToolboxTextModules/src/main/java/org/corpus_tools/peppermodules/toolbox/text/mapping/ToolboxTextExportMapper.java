package org.corpus_tools.peppermodules.toolbox.text.mapping;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.peppermodules.toolbox.text.AbstractToolboxTextMapper;
import org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties;
import org.corpus_tools.peppermodules.toolbox.text.utils.ToolboxTextModulesUtils;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAbstractAnnotation;
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
 * A mapper for Salt to Toolbox Text mapping.
 * 
 * This class is responsible for the actual mapping process.
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
		/*
		 * Explanation of Toolbox header line from https://groups.google.com/forum/#!topic/shoeboxtoolbox-field-linguists-toolbox/JunMJ4COtJA
		 * 
		 * "The \_sh and the v3.0 are not related to export; in fact, Toolbox export removes this whole line. Your export, of course, must add it.
		 *
		 * The items in this first line are in fixed positions.
		 * The \_sh is a flag that the data on this line is Toolbox's information, not user data. This line must be the very first line.
		 * The \_sh is followed by a single space.
		 * The v3.0 is the version the settings files (I think). Don't change that number.
		 * The separation here is two spaces.
		 * The 400 or whatever is the "wrap" length for lines in that database. 400 is a fine number if you are making a new file from something else -- it is the number Toolbox defaults to when creating a new file or bringing in some non-Toolbox data.
		 * If the wrap is less than 1000, there are two space; if larger, there is only one.
		 * The last item is indeed the internal name of the database type. This name may include spaces.
		 * 
		 * Toolbox database types (and other settings files) have an internal name distinct from the file name of the settings file. Toolbox was written when file names were restricted to an 8.3 format. We felt this was too limiting and created the concept of an internal name."
		 */
		lines.add("\\_sh v3.0 400 Text");
		lines.add("");
		
		// Add document annotations and meta annotations
		for (SAnnotation a : getDocument().getAnnotations()) {
			lines.add(createMarkerAnnoString(a));
		}
		for (SMetaAnnotation ma : getDocument().getMetaAnnotations()) {
			lines.add(createMarkerAnnoString(ma));
		}
		lines.add("");
		
		/*
		 * Map all \ids
		 */
		// Get \id spans
		String idSpanName = properties.getIdSpanLayer();
		// TODO Document that we're using the first layer, issues with > 1 layers of the same name
		// TODO Document that SLayers NEED TO BE PRESENT for the conversion to work at all
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
			// FIXME Externalize \\id, etc. to properties for use here
			lines.add("\\id " + idSpan.getAnnotation(properties.getIdIdentifierAnnotation()).getValue_STEXT());
			for (SAnnotation a : idSpan.getAnnotations()) {
				if (!a.getQName().equals(properties.getIdIdentifierAnnotation())) {
					lines.add(createMarkerAnnoString(a));
				}
			}
			for (SMetaAnnotation ma : idSpan.getMetaAnnotations()) {
				if (!ma.getQName().equals(properties.getIdIdentifierAnnotation())) {
					lines.add(createMarkerAnnoString(ma));
				}
			}
			lines.add(" ");
			
			// Map \refs
			// Get refs per id
			@SuppressWarnings("rawtypes")
			List<DataSourceSequence> dsSequences = graph.getOverlappedDataSourceSequence(idSpan, SALT_TYPE.STEXT_OVERLAPPING_RELATION);
			/*
			 *  We are currently working with only 1 data source! TODO Add this info to docs.
			 *  Spans should arguably only overlap one data source anyway, but some
			 *  importers (notably the FLExImporter) will tie "phrase" spans (i.e., Toolbox refs)
			 *  to both morphological and lexical tokens. Here it is assumed, that the spanned
			 *  lexical tokens exactly overlap the spanned morphological tokens, and hence any
			 *  DataSourceSequence of the two (or possibly more) will have the same effect
			 *  on the Toolbox ref.
			 */
			@SuppressWarnings("rawtypes")
			DataSourceSequence dsSequence = dsSequences.get(0);
			List<SNode> allNodes = graph.getNodesBySequence(dsSequence);
			Set<SSpan> refSpans = new HashSet<>();
			for (SNode node : allNodes) {
				// TODO Document that we're using the first layer, issues with > 1 layers of the same name
				if (node instanceof SSpan && node.getLayers().contains(graph.getLayerByName(properties.getRefSpanLayer()).get(0))) {
					refSpans.add((SSpan) node);
				}
			}
			List<SSpan> orderedRefs = ToolboxTextModulesUtils.sortSpansByTextCoverageOfIncludedToken(refSpans);
			for (SSpan refSpan : orderedRefs) {
				lines.add("\\ref " + refSpan.getAnnotation(properties.getRefIdentifierAnnotation()).getValue_STEXT());
				for (SAnnotation a : refSpan.getAnnotations()) {
					if (!a.getQName().equals(properties.getRefIdentifierAnnotation())) {
						lines.add(createMarkerAnnoString(a));
					}
				}
				for (SMetaAnnotation ma : refSpan.getMetaAnnotations()) {
					if (!ma.getQName().equals(properties.getRefIdentifierAnnotation())) {
						lines.add(createMarkerAnnoString(ma));
					}
				}
				
				// Map \tx
				String txLine = "\\tx";
				List<SToken> txTokens = new ArrayList<>();
				List<SToken> txCandidateTokens = graph.getOverlappedTokens(refSpan);
				Set<SNode> txLayerNodes = graph.getLayerByName(properties.getTxTokenLayer()).get(0).getNodes();
				for (SToken token : txCandidateTokens) {
					if (txLayerNodes.contains(token)) {
						txTokens.add(token);
					}
				}
				List<SToken> orderedTxTokens = graph.getSortedTokenByText(txTokens);
				// Build list of tx token annotations
				Set<String> txTokenAnnotations = new HashSet<>();
				for (SToken txToken : orderedTxTokens) {
					for (SAnnotation a : txToken.getAnnotations()) {
						txTokenAnnotations.add(a.getQName());
					}
					for (SMetaAnnotation ma : txToken.getMetaAnnotations()) {
						txTokenAnnotations.add(ma.getQName());
					}
				}
				// Create annotation line for each annotation in the set
				Map<String, String> txAnnotationLines = new HashMap<>();
				for (String annoQName : txTokenAnnotations) {
					String[] split = annoQName.split("::");
					String namespace = null;
					String name = null;
					if (split.length > 1) {
						namespace = split[0];
						name = split[1];
					}
					else if (split.length == 1) {
						name = split[0];
					}
					String marker = createMarkerString(namespace, name);
					txAnnotationLines.put(annoQName, marker);
				}
				// Clean list from annotations that contain primary lexical or morphological material
				for (String txa : properties.getTxMaterialAnnotations()) {
					txAnnotationLines.remove(txa);
				}
				for (String mba : properties.getMbMaterialAnnotations()) {
					txAnnotationLines.remove(mba);
				}
				// Build tx text
				for (SToken txToken : orderedTxTokens) {
					txLine += " " + graph.getText(txToken);
					for (String aKey : txAnnotationLines.keySet()) {
						if (txToken.getAnnotation(aKey) != null) {
							String oldValue = txAnnotationLines.get(aKey); 
							// Sanitize lexical annotations, as these may include spaces that break the item count
							SAnnotation annotation = txToken.getAnnotation(aKey);
							if (annotation.getValue_STEXT() == null) {
								annotation.setValue("[missing-annotation]");
							}
							String sanitizedValue = annotation.getValue_STEXT().replaceAll("\\s", properties.getSpaceReplacement());
							txAnnotationLines.put(aKey, oldValue += " " + sanitizedValue);
						}
						else if (txToken.getMetaAnnotation(aKey) != null) {
							String oldValue = txAnnotationLines.get(aKey); 
							// Sanitize lexical annotations, as these may include spaces that break the item count
							SMetaAnnotation annotation = txToken.getMetaAnnotation(aKey);
							if (annotation.getValue_STEXT() == null) {
								annotation.setValue("[missing-annotation]");
							}
							String sanitizedValue = annotation.getValue_STEXT().replaceAll("\\s", properties.getSpaceReplacement());
							txAnnotationLines.put(aKey, oldValue += " " + sanitizedValue);
						}

					}
				}
				lines.add(txLine);
				
				for (String al : txAnnotationLines.values()) {
					lines.add(al);
				}
				
				
				// Map \mb
				String mbLine = "\\mb";
				List<SToken> mbTokens = new ArrayList<>();
				List<SToken> mbCandidateTokens = graph.getOverlappedTokens(refSpan);
				Set<SNode> mbLayerNodes = graph.getLayerByName(properties.getMbTokenLayer()).get(0).getNodes();
				for (SToken token : mbCandidateTokens) {
					if (mbLayerNodes.contains(token)) {
						mbTokens.add(token);
					}
				}
				List<SToken> orderedMbTokens = graph.getSortedTokenByText(mbTokens);
				
				// Build list of mb token annotations
				Set<String> mbTokenAnnotations = new HashSet<>();
				for (SToken mbToken : orderedMbTokens) {
					for (SAnnotation a : mbToken.getAnnotations()) {
						mbTokenAnnotations.add(a.getQName());
					}
					for (SMetaAnnotation ma : mbToken.getMetaAnnotations()) {
						mbTokenAnnotations.add(ma.getQName());
					}
				}
				// Create annotation line for each annotation in the set
				Map<String, String> mbAnnotationLines = new HashMap<>();
				for (String annoQName : mbTokenAnnotations) {
					String[] split = annoQName.split("::");
					String namespace = null;
					String name = null;
					if (split.length > 1) {
						namespace = split[0];
						name = split[1];
					}
					else if (split.length == 1) {
						name = split[0];
					}
					String marker = createMarkerString(namespace, name);
					mbAnnotationLines.put(annoQName, marker);
				}
				// Clean list from annotations that contain primary lexical or morphological material
				for (String mba : properties.getMbMaterialAnnotations()) {
					mbAnnotationLines.remove(mba);
				}
				for (String mbmba : properties.getMbMaterialAnnotations()) {
					mbAnnotationLines.remove(mbmba);
				}
				// Build mb text
				for (SToken mbToken : orderedMbTokens) {
					mbLine += " " + graph.getText(mbToken);
					for (String aKey : mbAnnotationLines.keySet()) {
						if (mbToken.getAnnotation(aKey) != null) {
							String oldValue = mbAnnotationLines.get(aKey);
							// Sanitize morphological annotations, as these may include spaces that break the item count
							SAnnotation annotation = mbToken.getAnnotation(aKey);
							if (annotation.getValue_STEXT() == null) {
								annotation.setValue("[missing-annotation]");
							}
							String sanitizedValue = annotation.getValue_STEXT().replaceAll("\\s", properties.getSpaceReplacement());
							mbAnnotationLines.put(aKey, oldValue += " " + sanitizedValue);
						}
						else if (mbToken.getMetaAnnotation(aKey) != null) {
							String oldValue = mbAnnotationLines.get(aKey);
							// Sanitize morphological annotations, as these may include spaces that break the item count
							SMetaAnnotation annotation = mbToken.getMetaAnnotation(aKey);
							if (annotation.getValue_STEXT() == null) {
								annotation.setValue("[missing-annotation]");
							}
							String sanitizedValue = annotation.getValue_STEXT().replaceAll("\\s", properties.getSpaceReplacement());
							mbAnnotationLines.put(aKey, oldValue += " " + sanitizedValue);
						}

					}
				}
				lines.add(mbLine);
				
				for (String al : mbAnnotationLines.values()) {
					lines.add(al);
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
	 * Takes an annotation and compiles a string representing
	 * a valid line in a Toolbox text file with the following pattern.
	 * 
	 * ```java
	 * \{marker} {annotation value(s)}
	 * ```
	 * 
	 * @param annotation The annotation for which the Toolbox text line is compiled.
	 * @return A compiled Toolbox text line
	 */
	private String createMarkerAnnoString(SAbstractAnnotation annotation) {
		String marker = createMarkerString(annotation.getNamespace(), annotation.getName());
		return marker + " " + annotation.getValue_STEXT();
	}

	/**
	 * Creates the marker string for the string representation of a 
	 * valid Toolbox text line.
	 * 
	 * The passed namespace and name are combines to a valid Salt
	 * qualified name string. This combination is either compiled
	 * into a Toolbox marker with the pattern `name_[namespace]`,
	 * or a marker that should be used for this combination and
	 * has been supplied by the user via the properties.
	 * 
	 * In the process, all whitespaces in namespace and name
	 * are being replaced with a dash (`-`).
	 * 
	 * @param namespace
	 * @param name
	 * @return
	 */
	private String createMarkerString(String namespace, String name) {
		String nsn = namespace != null ? namespace + "::" + name : name;
		String marker = properties.getAnnotationMarkerMap().get(nsn);
		String line = "MISSING " + nsn;
		if (marker != null) {
			line = "\\" + marker;
		}
		else {
			line = "\\" + name.replaceAll("\\s", "-")
					+ (namespace != null ? "_[" + namespace.replaceAll("\\s", "-") : "") + "]";
		}
		return line;
	}
}
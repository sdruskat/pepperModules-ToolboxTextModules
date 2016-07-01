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

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;

/**
 * Properties for the ToolboxTextImporter. They are
 * 
 * <table summary="ToolboxTextImporter properties at a glance.">
 * <tr>
 * 	<td><strong>Name</strong></td>
 * 	<td><strong>Type</strong></td>
 * 	<td><strong>Mandatory</strong></td>
 * 	<td><strong>Default</strong></td>
 * </tr>
 * <tr>
 * 	<td>textMarker</td>
 * 	<td>String</td>
 * 	<td><center>X</center></td>
 * 	<td>tx</td>
 * </tr>
 * <tr>
 * 	<td>morphologyMarker</td>
 * 	<td>String</td>
 * 	<td><center>X</center></td>
 * 	<td>mb</td>
 * </tr>
 * <tr>
 * 	<td>textAnnotationMarkers</td>
 * 	<td>String</td>
 * 	<td></td>
 * 	<td></td>
 * </tr>
 * <tr>
 * 	<td>morphologyAnnotationMarkers</td>
 * 	<td>String</td>
 * 	<td></td>
 * 	<td>ge,ps</td>
 * </tr>
 * <tr>
 * 	<td>morphemeDelimiters</td>
 * 	<td>String</td>
 * 	<td></td>
 * 	<td>-,=</td>
 * </tr>
 * </table>
 * TODO FIXME Finalize table
 * @author Stephan Druskat <mail@sdruskat.net>
 *
 */
public class ToolboxTextImporterProperties extends PepperModuleProperties {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -1648280949759895879L;
	
	/**
	 * The Toolbox marker that precedes lines with source text (usually "words"),
	 * without the preceding backslash.
	 */
	public static final String PROP_LEX_MARKER = "lexicalMarker";
	
	/**
	 * The Toolbox marker that precedes lines with morphological information,
	 * without the preceding backslash.
	 */
	public static final String PROP_MORPH_MARKER = "morphologyMarker";
	
	/**
	 * All Toolbox markers which precede lines with annotations of source text segments (usually "words"),
	 * without the preceding backslashes, and as a comma-separated list.
	 */
	public static final String PROP_LEX_ANNOTATION_MARKERS = "lexAnnotationMarkers";
	
	/**
	 * All Toolbox markers which precede lines with annotations of morphemes,
	 * without the preceding backslashes, and as a comma-separated list.
	 */
	public static final String PROP_MORPH_ANNOTATION_MARKERS = "morphologyAnnotationMarkers";
	
	/**
	 * All Toolbox markers which precede lines with document-specific metadata, 
	 * without the preceding backslashes, and as a comma-separated list. 
	 */
	public static final String PROP_DOCUMENT_METADATA_MARKERS = "documentMetaDataMarkers";
	
	/**
	 * All Toolbox markers which precede lines with reference-specific metadata, 
	 * without the preceding backslashes, and as a comma-separated list. 
	 */
	public static final String PROP_REF_METADATA_MARKERS = "refMetaDataMarkers";
	
	/**
	 * The morpheme delimiters used in the Toolbox files as a comma-separated two-point list where
	 * the first element is the <strong>affix</strong> delimiter, and the second element is the
	 * <strong>clitics</strong> delimiter.
	 */
	public static final String PROP_MORPHEME_DELIMITERS = "morphemeDelimiters";
	
	/**
	 * The marker used for references, i.e., usually "ref" or "id".
	 */
	public static final String PROP_REF_MARKER = "refMarker";

	/**
	 * The file extensions that corpus files can have as a comma-separated list.
	 */
	public static final String PROP_FILE_EXTENSIONS = "fileExtensions";
	
	/**
	 * Wether reference-level annotations should be mapped as a span onto tokens on the lexical layer.
	 * {@code true} if so, and {@code false} if they should be mapped onto the morphological layer tokens.
	 */
	public static final String PROP_MAP_REF_ANNOTATIONS_TO_LEXICAL_LAYER = "mapRefAnnotationsToLexicalLayer";
	
	/**
	 * Wether detached delimiters (as in "item - item" or similar) should be attached to the previous or
	 * subsequent item, as a two-item comma-separated , where the first item signifies whether the delimiter should
	 * be attached (if <strong>true</strong> it will be attached), and the second item signifies 
	 * whether the delimiter should be attached to the <strong>subsequent</strong> item (if <strong>true</strong>
	 * it will be attached to the subsequent item, making the latter a suffix).
	 */
	public static final String PROP_ATTACH_DETACHED_MORPHEME_DELIMITER = "attachDetachedDelimiter";
	
	
	public ToolboxTextImporterProperties() {
		addProperty(new PepperModuleProperty<>(PROP_LEX_MARKER, 
				String.class, 
				"The Toolbox marker that precedes lines with source text (usually lexical items), without the preceding backslash.", 
				"tx",
				true));
		addProperty(new PepperModuleProperty<>(PROP_MORPH_MARKER, 
				String.class, 
				"The Toolbox marker that precedes lines with morphological information, without the preceding backslash.",
				"mb",
				true));
		addProperty(new PepperModuleProperty<>(PROP_LEX_ANNOTATION_MARKERS, 
				String.class, 
				"All Toolbox markers which precede lines with annotations of source text segments (usually lexical items), without the preceding backslashes, and as a comma-separated list.",
				false));
		addProperty(new PepperModuleProperty<>(PROP_MORPH_ANNOTATION_MARKERS, 
				String.class,
				"All Toolbox markers which precede lines with annotations of morphemes, without the preceding backslashes, and as a comma-separated list.",
				"ge,ps",
				false));
		addProperty(new PepperModuleProperty<>(PROP_MORPHEME_DELIMITERS, 
				String.class,
				"The morpheme delimiters used in the Toolbox files as a comma-separated two-point list where the first element is the affix delimiter, and the second element is the clitics delimiter.",
				"-,=",
				false));
		addProperty(new PepperModuleProperty<>(PROP_REF_MARKER, 
				String.class,
				"The marker used for references, i.e., usually \"ref\" or \"id\".",
				"ref",
				true));
		addProperty(new PepperModuleProperty<>(PROP_FILE_EXTENSIONS, 
				String.class,
				"The file extensions that corpus files can have as a comma-separated list.",
				"txt,lbl",
				true));
		addProperty(new PepperModuleProperty<>(PROP_MAP_REF_ANNOTATIONS_TO_LEXICAL_LAYER, 
				Boolean.class,
				"Wether reference-level annotations should be mapped as a span onto tokens on the lexical layer. \"true\" if so, and \"false\" if they should be mapped onto the morphological layer tokens.",
				true,
				true));
		addProperty(new PepperModuleProperty<>(PROP_DOCUMENT_METADATA_MARKERS, 
				String.class,
				"All Toolbox markers which precede lines with document-specific metadata, without the preceding backslashes, and as a comma-separated list.",
				false));
		addProperty(new PepperModuleProperty<>(PROP_REF_METADATA_MARKERS, 
				String.class,
				"All Toolbox markers which precede lines with reference-specific metadata, without the preceding backslashes, and as a comma-separated list.",
				false));
		addProperty(new PepperModuleProperty<>(PROP_ATTACH_DETACHED_MORPHEME_DELIMITER, 
				String.class,
				"Wether detached delimiters (as in \"item - item\" or similar) should be attached to the previous or subsequent item, as a two-item array, where the first item signifies whether the delimiter should be attached (if true it will be attached), and the second item signifies whether the delimiter should be attached to the subsequent item (if true it will be attached to the subsequent item, making the latter a suffix).",
				"true,true",
				false));
	}
	
	// Getter methods for the different property values.
	
	public String getLexMarker() {
		return (String) getProperty(PROP_LEX_MARKER).getValue();
	}
	
	public String getMorphMarker() {
		return (String) getProperty(PROP_MORPH_MARKER).getValue();
	}
	
	public String getLexAnnotationMarkers() {
		return (String) getProperty(PROP_LEX_ANNOTATION_MARKERS).getValue();
	}
	
	public String getMorphAnnotationMarkers() {
		return (String) getProperty(PROP_MORPH_ANNOTATION_MARKERS).getValue();
	}
	
	public String getMorphemeDelimiters() {
		return (String) getProperty(PROP_MORPHEME_DELIMITERS).getValue();
	}
	
	public String getRefMarker() {
		return (String) getProperty(PROP_REF_MARKER).getValue();
	}

	public String getFileExtensions() {
		return (String) getProperty(PROP_FILE_EXTENSIONS).getValue();
	}
	
	public Boolean mapRefAnnotationsToLexicalLayer() {
		return (Boolean) getProperty(PROP_MAP_REF_ANNOTATIONS_TO_LEXICAL_LAYER).getValue();
	}
	
	public String getDocMetadataMarkers() {
		return (String) getProperty(PROP_DOCUMENT_METADATA_MARKERS).getValue();
	}
	
	public String getRefMetadataMarkers() {
		return (String) getProperty(PROP_REF_METADATA_MARKERS).getValue();
	}
	
	public Boolean attachDetachedMorphemeDelimiter() {
		String value = getProperty(PROP_ATTACH_DETACHED_MORPHEME_DELIMITER).getValue().toString();
		String[] split = value.trim().split("\\s*,\\s*");
		return Boolean.valueOf(split[0]);
	}
	
	public Boolean attachDetachedMorphemeDelimiterToSubsequentElement() {
		String value = getProperty(PROP_ATTACH_DETACHED_MORPHEME_DELIMITER).getValue().toString();
		String[] split = value.trim().split("\\s*,\\s*");
		return Boolean.valueOf(split[1]);
	}

}

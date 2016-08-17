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
	 * The Toolbox marker that precedes lines with IDs,
	 * without the preceding backslash.
	 */
	public static final String PROP_ID_MARKER = "idMarker";

	
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
	 * All Toolbox markers which precede lines with annotations that can potentially span
	 * subranges of the complete morphological data source.
	 */
	public static final String PROP_UNIT_REF_ANNOTATION_MARKERS = "unitRefAnnotationMarkers";
	
	/**
	 * The marker used to define unit refs.
	 */
	public static final String PROP_UNIT_REF_DEFINITION_MARKER = "unitRefDefinitionMarker";
	
	/**
	 * Wether detached delimiters (as in "item - item" or similar) should be attached to the previous or
	 * subsequent item, as a two-item comma-separated , where the first item signifies whether the delimiter should
	 * be attached (if <strong>true</strong> it will be attached), and the second item signifies 
	 * whether the delimiter should be attached to the <strong>subsequent</strong> item (if <strong>true</strong>
	 * it will be attached to the subsequent item, making the latter a suffix).
	 */
	public static final String PROP_ATTACH_DETACHED_MORPHEME_DELIMITER = "attachDetachedDelimiter";
	
	/**
	 * Whether the importer should fix broken alignment in cases where the number of annotations
	 * is smaller than the number of tokens. If this is not set to true, the importer will throw
	 * errors whenever it encounters broken alignments.
	 */
	public static final String PROP_FIX_ALIGNMENT = "fixAlignment";
	
	/**
	 * The {@link String} that will be used to flag misalignment. This will be attached <em>n</em>
	 * times to the end of the line which includes the misalignment, where <em>n</em> is
	 * the number of missing items. 
	 */
	public static final String PROP_FIX_ALIGNMENT_STRING = "fixAlignmentString";
	
	/**
	 * Whether to ignore lexical items which miss a morphological representation.
	 */
	public static final String PROP_IGNORE_MISSING_MORPHEMES = "ignoreMissingMorphemes";

	/**
	 * Whether a Toolbox file contains IDs, usually marked with \id, for structuring files.
	 */
	public static final String PROP_HAS_IDS = "hasIds";
	
	/**
	 * Whether to replace missing morphological items with a placeholder string.
	 */
	public static final String PROP_SUBSTITUTE_MISSING_MORPHOLOGICAL_ITEMS = "substituteMissingMorphologicalItems";
	
	/**
	 * A custom placeholder string substituting missing morphological items.
	 */
	public static final String PROP_MISSING_MORPHOLOGICAL_ITEMS_PLACEHOLDER = "missingMorphologicalItemsPlaceholder";
	
	/**
	 * Whether the data contains morphological annotations.
	 */
	public static final String PROP_CONTAINS_MORPHOLOGY = "containsMorphology";
	
	
	public ToolboxTextImporterProperties() {
		addProperty(new PepperModuleProperty<String>(PROP_LEX_MARKER, 
				String.class, 
				"The Toolbox marker that precedes lines with source text (usually lexical items), without the preceding backslash.", 
				"tx",
				true));
		addProperty(new PepperModuleProperty<String>(PROP_MORPH_MARKER, 
				String.class, 
				"The Toolbox marker that precedes lines with morphological information, without the preceding backslash.",
				"mb",
				false));
		addProperty(new PepperModuleProperty<String>(PROP_ID_MARKER, 
				String.class, 
				"The Toolbox marker that precedes lines with IDs, without the preceding backslash.",
				"id",
				false));
		addProperty(new PepperModuleProperty<String>(PROP_LEX_ANNOTATION_MARKERS, 
				String.class, 
				"All Toolbox markers which precede lines with annotations of source text segments (usually lexical items), without the preceding backslashes, and as a comma-separated list.",
				false));
		addProperty(new PepperModuleProperty<String>(PROP_MORPH_ANNOTATION_MARKERS, 
				String.class,
				"All Toolbox markers which precede lines with annotations of morphemes, without the preceding backslashes, and as a comma-separated list.",
				"ge,ps",
				false));
		addProperty(new PepperModuleProperty<String>(PROP_MORPHEME_DELIMITERS, 
				String.class,
				"The morpheme delimiters used in the Toolbox files as a comma-separated two-point list where the first element is the affix delimiter, and the second element is the clitics delimiter.",
				"-,=",
				false));
		addProperty(new PepperModuleProperty<String>(PROP_REF_MARKER, 
				String.class,
				"The marker used for references, i.e., usually \"ref\" or \"id\".",
				"ref",
				true));
		addProperty(new PepperModuleProperty<String>(PROP_FILE_EXTENSIONS, 
				String.class,
				"The file extensions that corpus files can have as a comma-separated list.",
				"txt,lbl",
				true));
		addProperty(new PepperModuleProperty<String>(PROP_DOCUMENT_METADATA_MARKERS, 
				String.class,
				"All Toolbox markers which precede lines with document-specific metadata, without the preceding backslashes, and as a comma-separated list.",
				false));
		addProperty(new PepperModuleProperty<String>(PROP_REF_METADATA_MARKERS, 
				String.class,
				"All Toolbox markers which precede lines with reference-specific metadata, without the preceding backslashes, and as a comma-separated list.",
				false));
		addProperty(new PepperModuleProperty<String>(PROP_ATTACH_DETACHED_MORPHEME_DELIMITER, 
				String.class,
				"Wether detached delimiters (as in \"item - item\" or similar) should be attached to the previous or subsequent item, as a two-item array, where the first item signifies whether the delimiter should be attached (if true it will be attached), and the second item signifies whether the delimiter should be attached to the subsequent item (if true it will be attached to the subsequent item, making the latter a suffix).",
				"true,true",
				false));
		addProperty(new PepperModuleProperty<String>(PROP_UNIT_REF_ANNOTATION_MARKERS, 
				String.class,
				"All Toolbox markers which precede lines with annotations that can potentially span subranges of the complete morphological data source.",
				false));
		addProperty(new PepperModuleProperty<String>(PROP_UNIT_REF_DEFINITION_MARKER, 
				String.class,
				"The marker used to define unit refs.",
				"unitref",
				false));
		addProperty(new PepperModuleProperty<Boolean>(PROP_FIX_ALIGNMENT, 
				Boolean.class,
				"Whether the importer should fix broken alignment in cases where the number of annotations is smaller than the number of tokens. If this is not set to true, the importer will throw errors whenever it encounters broken alignments.",
				false,
				false));
		addProperty(new PepperModuleProperty<String>(PROP_FIX_ALIGNMENT_STRING, 
				String.class,
				"The {@link String} that will be used to flag misalignment. This will be attached n times to the end of the line which includes the misalignment, where n is the number of missing items.",
				"BROKEN_ALIGNMENT",
				false));
		addProperty(new PepperModuleProperty<Boolean>(PROP_IGNORE_MISSING_MORPHEMES, 
				Boolean.class,
				"Whether to ignore lexical items which miss a morphological representation.",
				false,
				false));
		addProperty(new PepperModuleProperty<Boolean>(PROP_HAS_IDS,
				Boolean.class,
				"Whether a Toolbox file contains IDs, usually marked with \\id, for structuring files.",
				true,
				true));
		addProperty(new PepperModuleProperty<Boolean>(PROP_SUBSTITUTE_MISSING_MORPHOLOGICAL_ITEMS,
				Boolean.class,
				"Whether to replace missing morphological items with a placeholder string.",
				true,
				true));
		addProperty(new PepperModuleProperty<String>(PROP_MISSING_MORPHOLOGICAL_ITEMS_PLACEHOLDER,
				String.class,
				"A custom placeholder string substituting missing morphological items.",
				"***",
				false));
		addProperty(new PepperModuleProperty<Boolean>(PROP_CONTAINS_MORPHOLOGY,
				Boolean.class,
				"Whether the data contains morphological annotations.",
				true,
				true));
	}
	
	// Getter methods for the different property values.
	
	public String getLexMarker() {
		return (String) getProperty(PROP_LEX_MARKER).getValue();
	}
	
	public String getIdMarker() {
		return (String) getProperty(PROP_ID_MARKER).getValue();
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

	public String getUnitRefAnnotationMarkers() {
		return (String) getProperty(PROP_UNIT_REF_ANNOTATION_MARKERS).getValue();
	}
	
	public String getUnitRefDefinitionMarker() {
		return (String) getProperty(PROP_UNIT_REF_DEFINITION_MARKER).getValue();
	}
	
	public Boolean fixAlignment() {
		return (Boolean) getProperty(PROP_FIX_ALIGNMENT).getValue();
	}
	
	public String getFixAlignmentString() {
		return (String) getProperty(PROP_FIX_ALIGNMENT_STRING).getValue();
	}
	
	public Boolean ignoreMissingMorphemes() {
		return (Boolean) getProperty(PROP_IGNORE_MISSING_MORPHEMES).getValue();
	}
	
	public Boolean hasIds() {
		return (Boolean) getProperty(PROP_HAS_IDS).getValue();
	}

	public boolean getSubstituteMissingMorpologicalItems() {
		return (Boolean) getProperty(PROP_SUBSTITUTE_MISSING_MORPHOLOGICAL_ITEMS).getValue();
	}
	
	public boolean containsMorphology() {
		return (Boolean) getProperty(PROP_CONTAINS_MORPHOLOGY).getValue();
	}
	
	public String getMissingMorphologicalItemsPlaceholder() {
		return (String) getProperty(PROP_MISSING_MORPHOLOGICAL_ITEMS_PLACEHOLDER).getValue();
	}
	
}

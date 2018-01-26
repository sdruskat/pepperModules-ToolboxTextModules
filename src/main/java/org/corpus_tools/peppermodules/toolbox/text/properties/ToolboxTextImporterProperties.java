/**
 * Copyright 2016ff. Humboldt-Universität zu Berlin.
 * Exploitation rights belong exclusively to Humboldt-Universität zu Berlin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Stephan Druskat (toolboxtextmodules@sdruskat.net) - initial API and implementation
 */
package org.corpus_tools.peppermodules.toolbox.text.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextDocumentNameParser;
import org.corpus_tools.peppermodules.toolbox.text.utils.ToolboxTextModulesUtils;

/**
 * Properties for the ToolboxTextImporter.
 * 
 * The single properties are explained in the respective field Javadoc.
 * 
 * **Note:** The properties should be considered the central API for this
 * and the other Pepper modules. 
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 */
public class ToolboxTextImporterProperties extends PepperModuleProperties {

	/**
	 * Serial version UID
	 */
	public static final long serialVersionUID = -1648280949759895879L;
	
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
	 * The morpheme delimiters used in the Toolbox files as a comma-separated two-point list where
	 * the first element is the <strong>affix</strong> delimiter, and the second element is the
	 * <strong>clitics</strong> delimiter.
	 */
	public static final String PROP_MORPHEME_DELIMITERS = "morphemeDelimiters";
	
	/**
	 * The morpheme delimiter used in the Toolbox files to mark words (represented on the
	 * morphological layer) that are contracted into words on the lexical layer, e.g.,
	 * Saliba `tane = ta wane`. This delimiter can be used for cases where the importer
	 * may otherwise not have enough information to figure out that the lexical word should
	 * contain the "morphological word". 
	 * 
	 * It will be **dropped after parsing** and will not show up in either the Salt model
	 * or any further model transformations.
	 * 
	 * The marker is only picked up when used to **suffix the second to nth word**, i.e. for the
	 * Saliba example above, `ta _wane` (property default is the underscore `_`) will be 
	 * mapped as two items on the morphological layer which are ruled by one item on 
	 * the lexical layer:
	 * 
	 * ```
	 *   lex: | tane      |
	 *        |-----------|
	 * morph: | ta | wane |
	 * ```
	 */
	public static final String PROP_LIAISON_DELIMITER = "liaisonDelimiter";
	
	/**
	 * The marker used for references, i.e., usually "ref" or "id".
	 */
	public final static String PROP_REF_MARKER = "refMarker";

	/**
	 * The file extensions that corpus files can have as a comma-separated list.
	 */
	public static final String PROP_FILE_EXTENSIONS = "fileExtensions";
	
	/**
	 * The marker which precedes lines with annotations that can potentially span
	 * subranges of the complete morphological data source.
	 */
	public static final String PROP_SUB_REF_ANNOTATION_MARKERS = "subrefAnnotationMarkers";
	
	/**
	 * The marker used to define unit refs.
	 */
	public static final String PROP_SUB_REF_DEFINITION_MARKER = "subrefDefinitionMarker";
	
	/**
	 * Whether detached morphology delimiters (as in "item - item" or similar) should be attached to the previous or
	 * subsequent item, as a two-item comma-separated list, where the first item signifies whether the delimiter should
	 * be attached at all (if `true` it will be attached), and the second item signifies 
	 * whether the delimiter should be attached to the **subsequent** item (if `true`
	 * it will be attached to the subsequent item, making the latter a suffix).
	 */
	public static final String PROP_ATTACH_DETACHED_MORPHEME_DELIMITER = "attachDelimiter";
	
	/**
	 * Whether lines with the same marker in the same block should be merged into one line.
	 * 
	 * `true`: subsequent lines marked with {marker} are concatenated to the first
	 * line marked with {marker}.
	 * 
	 * `false`: all lines but the first line marked with {marker} are dropped.
	 */
	public static final String PROP_MERGE_DUPL_MARKERS = "mergeDuplMarkers";

	/**
	 * Whether the importer should record errors.
	 * 
	 * `true` (default): Errors in the data model will be recorded, i.e., annotations
	 * on an error layer (called `err`) will be added for each line which
	 * seems to contain an error. Additionally, another annotation will be added
	 * to discrete layers, recording the original faulty line.
	 * 
	 * `false`: Errors will not be recorded. 
	 */
	public static final String PROP_RECORD_ERRORS = "recordErrors";

	/**
	 * Whether annotation namespace-name combinations for the default
	 * layers should be normalized to Toolbox standards (after MDF). 
	 */
	public static final String PROP_NORMALIZE_MARKERS = "normalizeMarkers";
	
	/**
	 * Whether special characters and whitespaces in document names should
	 * be replaced with default characters.
	 * 
	 * @see ToolboxTextDocumentNameParser
	 */
	public static final String PROP_NORMALIZE_DOC_NAMES = "normalizeDocNames";

	/**
	 * Whether the importer should fix interlinearization.
	 * 
	 * `true` (default): Interlinearization error in the data model will be fixed as follows.
	 * 
	 * - For **discrepancies between the number of lexical and morphological
	 * tokens**, morphological tokens will either be added to until their 
	 * number is equal to that of lexical tokens (using the property 
	 * {@link #PROP_MISSING_ANNO_STRING}), or all tokens at indices >
	 * index of the last lexical token will be dropped.
	 * 
	 * - For **discrepancies between the number of tokens and their annotations**
	 * as defined by {@link #PROP_LEX_ANNOTATION_MARKERS} and 
	 * {@link #PROP_MORPH_ANNOTATION_MARKERS}, annotations will either be
	 * added to until their number is equal to that of the token layer they
	 * refer to, or all tokens at indices > index of last token they refer
	 * to will be dropped.
	 * 
	 * `false`: Interlinearization errors will not be fixed. For missing morphological tokens
	 * or annotations, nothing will be inserted. Morphological tokens and
	 * annotations at indices > last index of lexical token, or last index
	 * of token layer they refer to will, respectively, be concatenated to the last element
	 * on their line, and separated by whitespaces.
	 * 
	 * **NOTE:** If the property is set to `false`, unfixed interl11n errors may
	 * cause an exception to be thrown during runtime!
	 */
	public static final String PROP_FIX_INTERL11N = "fixInterl11n";

	/**
	 * A {@link String} used to fill interlinearization gaps.
	 * 
	 * Default: *\*\*\**
	 */
	public static final String PROP_MISSING_ANNO_STRING = "missingAnnoString";
	
	/**
	 * Constructor adding all properties to the instance.	 
	 */
	public ToolboxTextImporterProperties() {
		addProperty(PepperModuleProperty.create().withName(PROP_LEX_MARKER).withType(String.class).withDescription(
				"The Toolbox marker that precedes lines with source text (usually lexical items), without the preceding backslash.")
				.withDefaultValue("tx").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(PROP_MORPH_MARKER).withType(String.class).withDescription(
				"The Toolbox marker that precedes lines with morphological information, without the preceding backslash.")
				.withDefaultValue("mb").isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(PROP_ID_MARKER).withType(String.class)
				.withDescription("The Toolbox marker that precedes lines with IDs, without the preceding backslash.")
				.withDefaultValue("id").isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(PROP_LEX_ANNOTATION_MARKERS).withType(String.class)
				.withDescription(
						"All Toolbox markers which precede lines with annotations of source text segments (usually lexical items), without the preceding backslashes, and as a comma-separated list.")
				.isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(PROP_MORPH_ANNOTATION_MARKERS).withType(String.class)
				.withDescription(
						"All Toolbox markers which precede lines with annotations of morphemes, without the preceding backslashes, and as a comma-separated list.")
				.withDefaultValue("ge,ps").isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(PROP_MORPHEME_DELIMITERS).withType(String.class)
				.withDescription(
						"The morpheme delimiters used in the Toolbox files as a comma-separated two-point list where the first element is the affix delimiter, and the second element is the clitics delimiter.")
				.withDefaultValue("-,=").isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(PROP_LIAISON_DELIMITER).withType(String.class)
				.withDescription(
						"The morpheme delimiter used in the Toolbox files to mark words (represented on the morphological layer) that are contracted into words on the lexical layer.")
				.withDefaultValue("_").isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(PROP_REF_MARKER).withType(String.class)
				.withDescription("The marker used for references, i.e., usually \"ref\" or \"id\".")
				.withDefaultValue("ref").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(PROP_FILE_EXTENSIONS).withType(String.class)
				.withDescription("The file extensions that corpus files can have as a comma-separated list.")
				.withDefaultValue("txt").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(PROP_ATTACH_DETACHED_MORPHEME_DELIMITER)
				.withType(String.class)
				.withDescription(
						"Wether detached delimiters (as in \"item - item\" or similar) should be attached to the previous or subsequent item, as a two-item array, where the first item signifies whether the delimiter should be attached (if true it will be attached), and the second item signifies whether the delimiter should be attached to the subsequent item (if true it will be attached to the subsequent item, making the latter a suffix).")
				.withDefaultValue("true,true").isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(PROP_SUB_REF_ANNOTATION_MARKERS).withType(String.class)
				.withDescription(
						"All Toolbox markers which precede lines with annotations that can potentially span subranges of the complete morphological data source, without the preceding backslashes, and as a comma-separated list.")
				.withDefaultValue("").isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(PROP_SUB_REF_DEFINITION_MARKER).withType(String.class)
				.withDescription("The marker used to define unit refs.").withDefaultValue("subref").isRequired(false)
				.build());
		addProperty(PepperModuleProperty.create().withName(PROP_MERGE_DUPL_MARKERS).withType(Boolean.class)
				.withDescription(
						"Whether lines with the same marker in the same block should be merged into one line, or just the first line kept.")
				.withDefaultValue(true).isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(PROP_MISSING_ANNO_STRING).withType(String.class)
				.withDescription("A string used to fill gaps in annotations. Default: ***.").withDefaultValue("***")
				.isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(PROP_RECORD_ERRORS).withType(Boolean.class)
				.withDescription("Whether the importer should record errors.").withDefaultValue(true).isRequired(true)
				.build());
		addProperty(PepperModuleProperty.create().withName(PROP_FIX_INTERL11N).withType(Boolean.class)
				.withDescription("Whether the importer should fix interlinearization errors.").withDefaultValue(true)
				.isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(PROP_NORMALIZE_MARKERS).withType(Boolean.class)
				.withDescription("Whether the importer should rename layers and annotations to defaults.")
				.withDefaultValue(false).isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(PROP_NORMALIZE_DOC_NAMES).withType(Boolean.class)
				.withDescription("Whether the importer should rename documents to defaults (no special characters).")
				.withDefaultValue(true).isRequired(true).build());
	}

	// Getter methods for the different property values.
	
	@SuppressWarnings("javadoc")
	public String getLexMarker() {
		return (String) getProperty(PROP_LEX_MARKER).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public String getIdMarker() {
		return (String) getProperty(PROP_ID_MARKER).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public String getMorphMarker() {
		return (String) getProperty(PROP_MORPH_MARKER).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public String getLexAnnotationMarkers() {
		return (String) getProperty(PROP_LEX_ANNOTATION_MARKERS).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public String getMorphAnnotationMarkers() {
		return (String) getProperty(PROP_MORPH_ANNOTATION_MARKERS).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public String getMorphemeDelimiters() {
		return (String) getProperty(PROP_MORPHEME_DELIMITERS).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public String getRefMarker() {
		return (String) getProperty(PROP_REF_MARKER).getValue();
	}

	@SuppressWarnings("javadoc")
	public String getFileExtensions() {
		return (String) getProperty(PROP_FILE_EXTENSIONS).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public List<String> getSubRefAnnotationMarkers() {
		return new ArrayList<>(Arrays.asList(((String) getProperty(PROP_SUB_REF_ANNOTATION_MARKERS).getValue()).split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)));
	}
	
	@SuppressWarnings("javadoc")
	public String getSubrefDefinitionMarker() {
		return (String) getProperty(PROP_SUB_REF_DEFINITION_MARKER).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public boolean mergeDuplicateMarkers() {
		return (Boolean) getProperty(PROP_MERGE_DUPL_MARKERS).getValue();
	}

	@SuppressWarnings("javadoc")
	public String getMissingAnnoString() {
		return (String) getProperty(PROP_MISSING_ANNO_STRING).getValue();
	}

	@SuppressWarnings("javadoc")
	public boolean recordErrors() {
		return (Boolean) getProperty(PROP_RECORD_ERRORS).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public boolean fixInterl11n() {
		return (Boolean) getProperty(PROP_FIX_INTERL11N).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public Boolean attachDelimiter() {
		return Boolean.valueOf(getProperty(PROP_ATTACH_DETACHED_MORPHEME_DELIMITER).getValue().toString().trim().split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)[0]);
	}
	
	@SuppressWarnings("javadoc")
	public Boolean attachDelimiterToNext() {
		return Boolean.valueOf(getProperty(PROP_ATTACH_DETACHED_MORPHEME_DELIMITER).getValue().toString().trim().split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)[1]);
	}
	
	@SuppressWarnings("javadoc")
	public String getAffixDelim() {
		return getProperty(PROP_MORPHEME_DELIMITERS).getValue().toString().trim().split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)[0];
	}
	
	@SuppressWarnings("javadoc")
	public String getCliticDelim() {
		return getProperty(PROP_MORPHEME_DELIMITERS).getValue().toString().trim().split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)[1];
	}

	@SuppressWarnings("javadoc")
	public String getLiaisonDelim() {
		return (String) getProperty(PROP_LIAISON_DELIMITER).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public boolean normalizeMarkers() {
		return (Boolean) getProperty(PROP_NORMALIZE_MARKERS).getValue();
	}

	@SuppressWarnings("javadoc")
	public boolean normalizeDocNames() {
		return (Boolean) getProperty(PROP_NORMALIZE_DOC_NAMES).getValue();
	}
	
}


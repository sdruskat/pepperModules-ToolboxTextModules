/*******************************************************************************
 * Copyright (c) 2017 Stephan Druskat
 * Exploitation rights belong exclusively to Humboldt-Universität zu Berlin
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
package org.corpus_tools.peppermodules.toolbox.text.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter;
import org.corpus_tools.peppermodules.toolbox.text.utils.ToolboxTextModulesUtils;

/**
 * Properties for the ToolboxTextImporter.
 * 
 * The single properties are explained in the respective field Javadoc.
 * 
 * **Note:** The properties should be considered the central API for this
 * and other Pepper modules. 
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
	public final String PROP_LIAISON_DELIMITER = "liaisonDelimiter";
	
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
	 * Whether the importer should be run in **error detection mode**.
	 * 
	 * `true`: No mapping will take place, but warnings will be logged. 
	 * This mode can be used to detect faulty data before attempting
	 * a conversion.
	 * 
	 * `false` (default): Corpora and documents will be mapped in full. 
	 */
	public static final String PROP_DETECTION_MODE = "errorDetectionMode";

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
	 * TODO Documentation 
	 */
	public static final String PROP_NORMALIZE_MARKERS = "normalizeMarkers";
	
	/**
	 * TODO Documentation 
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
				false));
		addProperty(new PepperModuleProperty<>(PROP_ID_MARKER, 
				String.class, 
				"The Toolbox marker that precedes lines with IDs, without the preceding backslash.",
				"id",
				false));
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
		addProperty(new PepperModuleProperty<>(PROP_LIAISON_DELIMITER, 
				String.class,
				"The morpheme delimiter used in the Toolbox files to mark words (represented on the morphological layer) that are contracted into words on the lexical layer.",
				"_",
				false));
		addProperty(new PepperModuleProperty<>(PROP_REF_MARKER, 
				String.class,
				"The marker used for references, i.e., usually \"ref\" or \"id\".",
				"ref",
				true));
		addProperty(new PepperModuleProperty<>(PROP_FILE_EXTENSIONS, 
				String.class,
				"The file extensions that corpus files can have as a comma-separated list.",
				"txt",
				true));
		addProperty(new PepperModuleProperty<>(PROP_ATTACH_DETACHED_MORPHEME_DELIMITER, 
				String.class,
				"Wether detached delimiters (as in \"item - item\" or similar) should be attached to the previous or subsequent item, as a two-item array, where the first item signifies whether the delimiter should be attached (if true it will be attached), and the second item signifies whether the delimiter should be attached to the subsequent item (if true it will be attached to the subsequent item, making the latter a suffix).",
				"true,true",
				false));
		addProperty(new PepperModuleProperty<>(PROP_SUB_REF_ANNOTATION_MARKERS, 
				String.class,
				"All Toolbox markers which precede lines with annotations that can potentially span subranges of the complete morphological data source, without the preceding backslashes, and as a comma-separated list.",
				"",
				false));
		addProperty(new PepperModuleProperty<>(PROP_SUB_REF_DEFINITION_MARKER, 
				String.class,
				"The marker used to define unit refs.",
				"subref",
				false));
		addProperty(new PepperModuleProperty<>(PROP_MERGE_DUPL_MARKERS,
				Boolean.class,
				"Whether lines with the same marker in the same block should be merged into one line, or just the first line kept.",
				true,
				true));
		addProperty(new PepperModuleProperty<>(PROP_DETECTION_MODE,
				Boolean.class,
				"Whether the importer should be run in error detection mode. true: Corpora and documents will be mapped, but documents will remain empty. This mode can be used to detect faulty data before attempting a conversion. false (default): Corpora and documents will be mapped in full. ",
				false,
				true));
		addProperty(new PepperModuleProperty<>(PROP_MISSING_ANNO_STRING,
				String.class,
				"A string used to fill gaps in annotations. Default: ***.",
				"***",
				true));
		addProperty(new PepperModuleProperty<>(PROP_RECORD_ERRORS,
				Boolean.class,
				"Whether the importer should record errors.",
				true,
				true));
		addProperty(new PepperModuleProperty<>(PROP_FIX_INTERL11N,
				Boolean.class,
				"Whether the importer should fix interlinearization errors.",
				true,
				true));
		addProperty(new PepperModuleProperty<>(PROP_NORMALIZE_MARKERS,
				Boolean.class,
				"Whether the importer should rename layers and annotations to defaults.",
				false,
				true));
		addProperty(new PepperModuleProperty<>(PROP_NORMALIZE_DOC_NAMES,
				Boolean.class,
				"Whether the importer should rename documents to defaults (no special characters).",
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
	
	public List<String> getSubRefAnnotationMarkers() {
		return new ArrayList<>(Arrays.asList(((String) getProperty(PROP_SUB_REF_ANNOTATION_MARKERS).getValue()).split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)));
	}
	
	public String getSubrefDefinitionMarker() {
		return (String) getProperty(PROP_SUB_REF_DEFINITION_MARKER).getValue();
	}
	
	public boolean mergeDuplicateMarkers() {
		return (Boolean) getProperty(PROP_MERGE_DUPL_MARKERS).getValue();
	}

	public boolean errorDetectionMode() {
		return (Boolean) getProperty(PROP_DETECTION_MODE).getValue();
	}
	
	public String getMissingAnnoString() {
		return (String) getProperty(PROP_MISSING_ANNO_STRING).getValue();
	}

	public boolean recordErrors() {
		return (Boolean) getProperty(PROP_RECORD_ERRORS).getValue();
	}
	
	public boolean fixInterl11n() {
		return (Boolean) getProperty(PROP_FIX_INTERL11N).getValue();
	}
	
	public Boolean attachDelimiter() {
		return Boolean.valueOf(getProperty(PROP_ATTACH_DETACHED_MORPHEME_DELIMITER).getValue().toString().trim().split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)[0]);
	}
	
	public Boolean attachDelimiterToNext() {
		return Boolean.valueOf(getProperty(PROP_ATTACH_DETACHED_MORPHEME_DELIMITER).getValue().toString().trim().split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)[1]);
	}
	
	public String getAffixDelim() {
		return getProperty(PROP_MORPHEME_DELIMITERS).getValue().toString().trim().split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)[0];
	}
	
	public String getCliticDelim() {
		return getProperty(PROP_MORPHEME_DELIMITERS).getValue().toString().trim().split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)[1];
	}

	public String getLiaisonDelim() {
		return (String) getProperty(PROP_LIAISON_DELIMITER).getValue();
	}
	
	public boolean normalizeMarkers() {
		return (Boolean) getProperty(PROP_NORMALIZE_MARKERS).getValue();
	}

	public boolean normalizeDocNames() {
		return (Boolean) getProperty(PROP_NORMALIZE_DOC_NAMES).getValue();
	}
	

}


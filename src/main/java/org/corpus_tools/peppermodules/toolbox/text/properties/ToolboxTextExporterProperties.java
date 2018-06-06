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
/**
 * 
 */
package org.corpus_tools.peppermodules.toolbox.text.properties;

import java.util.ArrayList; 
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.peppermodules.toolbox.text.utils.ToolboxTextModulesUtils;
import org.corpus_tools.salt.core.SAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Properties for the ToolboxTextExporter.
 * 
 * The single properties are explained in the respective field Javadoc.
 * 
 * **Note:** The properties should be considered the central API for this
 * and the other Pepper modules.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 * 
 */
public class ToolboxTextExporterProperties extends PepperModuleProperties {
	
	private static final Set<String> mdfKeys = new HashSet<>(Arrays.asList("an", "bb", "bw", "ce", "cf", "cn", "cr", "de", "dn", "dr", "dt", "dv", "ec", "ee", "eg", "en", "er", "es", "et", "ev", "ge", "gn", "gr", "gv", "hm", "is", "lc", "le", "lf", "ln", "lr", "lt", "lx", "mn", "mr", "na", "nd", "ng", "np", "nq", "ns", "nt", "oe", "on", "or", "ov", "pc", "pd", "ph", "pl", "pn", "ps", "rd", "re", "rf", "rn", "rr", "sc", "sd", "se", "sg", "sn", "so", "st", "sy", "tb", "th", "ue", "un", "ur", "uv", "va", "ve", "vn", "vr", "we", "wn", "wr", "xe", "xg", "xn", "xr", "xv", "1d", "1e", "1i", "1p", "1s", "2d", "2p", "2s", "3d", "3p", "3s", "4d", "4p", "4s"));
	
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextExporterProperties.class);
	
	private static final long serialVersionUID = -778762953092094905L;
	
	/*
	 * Layer names
	 */
	/**
	 * Name of layer with ref spans
	 */
	public static final String REF_SPAN_LAYER = "refSpanLayer";
	
	/**
	 * Name of layer with id spans
	 */
	public static final String ID_SPAN_LAYER = "idSpanLayer";
	
	/**
	 * Name of layer with lexical tokens
	 */
	public static final String TX_TOKEN_LAYER = "txTokenLayer";
	
	/**
	 * Name of layer with morphological tokens
	 */
	public static final String MB_TOKEN_LAYER = "mbTokenLayer";
	
	/**
	 * Identifier annotations
	 * 
	 * These would be annotations which identify Toolbox'
	 * segmentations, i.e., \id and \ref, i.e., 
	 * annotations that would end up as the content
	 * of \id and \ref lines, e.g. 
	 * \id Document Thisandthat
	 * \ref 123.5746
	 */
	public static final String ID_IDENT_ANNOTATION = "idIdentifierAnnotation";
	/**
	 * 
	 */
	public static final String REF_IDENT_ANNOTATION = "refIdentifierAnnotation";
	
	/**
	 * Annotations which contain primary data, i.e., lexical material
	 * which will already be mapped to tokens but still exists as annotation
	 * and should thus be left out during export to annotations (as they will
	 * already be mapped to \tx).
	 */
	public static final String TX_ANNOTATIONS_TO_IGNORE = "txMaterialAnnotations";
	
	/**
	 * Annotations which contain primary data, i.e., morphological material
	 * which will already be mapped to tokens but still exists as annotation
	 * and should thus be left out during export to annotations (as they will
	 * already be mapped to \mb).
	 */
	public static final String MB_ANNOTATIONS_TO_IGNORE = "mbMaterialAnnotations";
	
	// Other properties
	/**
	 * The replacement String to be used for replacing whitespaces in
	 * annotation values which may break the item count if not replaced.
	 */
	public static final String SPACE_REPLACEMENT = "spaceReplacement";
	
	/**
	 * A map whose keys are {@link SAnnotation}s
	 * with the pattern `namespace::name`, and whose values
	 * are MDF markers.
	 * 
	 * In the export process, the value for the defined {@link SAnnotation} will
	 * be mapped to the line marked with the respective MDF pattern.
	 * 
	 * - Example: `morph::gloss:ge` will map an {@link SAnnotation}
	 * `morph::gloss:myglossvalue` to `\ge myglossvalue`.
	 * 
	 * @see Coward, David F.; Grimes, Charles E. (2000): "Making Dictionaries. A guide to lexicography and the Multi-Dictionary Formatter". SIL International: Waxhaw, North Carolina. 183-185. URL http://downloads.sil.org/legacy/shoebox/MDF_2000.pdf.
	 */
	public static final String MDF_MAP = "mdfMap";
	
	/**
	 * A map whose keys are {@link SAnnotation}s
	 * with the pattern `namespace::name`, and whose
	 * values are custom markers.
	 * 
	 * In the export process, the value for the defined {@link SAnnotation} will
	 * be mapped to the line marked with the respective custom pattern.
	 * 
	 * @see #MDF_MAP
	 */
	public static final String CUSTOM_MARKERS = "customMarkers";

	/**
	 * 
	 */
	public static final String MARKER_SPLIT_REGEX = "(?<!:):(?!:)";
	
	/**
	 * A placeholder inserted for null values to keep linearization intact.
	 */
	public static final String NULL_PLACEHOLDER = "nullPlaceholder";
	
	/**
	 * Constructor adding all properties to the instance.
	 */
	public ToolboxTextExporterProperties() {
		addProperty(PepperModuleProperty.create().withName(REF_SPAN_LAYER).withType(String.class)
				.withDescription("The Salt layer that contains the spans to be mapped to Toolbox \\refs.")
				.withDefaultValue("ref").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(ID_SPAN_LAYER).withType(String.class)
				.withDescription("The Salt layer that contains the spans to be mapped to Toolbox \\ids.")
				.withDefaultValue("id").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(TX_TOKEN_LAYER).withType(String.class)
				.withDescription("The Salt layer that contains the tokens to be mapped to Toolbox' \\tx lines.")
				.withDefaultValue("tx").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(MB_TOKEN_LAYER).withType(String.class)
				.withDescription("The Salt layer that contains the tokens to be mapped to Toolbox' \\mb lines.")
				.withDefaultValue("mb").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(ID_IDENT_ANNOTATION).withType(String.class)
				.withDescription("The annotation (namespace::name) that contains the identifier of \\ids.")
				.isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(REF_IDENT_ANNOTATION).withType(String.class)
				.withDescription("The annotation (namespace::name) that contains the identifier of \\refs.")
				.isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(TX_ANNOTATIONS_TO_IGNORE).withType(String.class)
				.withDescription("Annotations (namespace::name) on lexical tokens which should be ignored, as a comma-separated list.")
				.isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(MB_ANNOTATIONS_TO_IGNORE).withType(String.class)
				.withDescription("Annotations (namespace::name) on lexical tokens which should be ignored, as a comma-separated list.")
				.isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(SPACE_REPLACEMENT).withType(String.class)
				.withDescription("String to replace whitespaces in annotation values with, as these whitespaces may break the item count in Toolbox interlinearization.")
				.withDefaultValue("-").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(MDF_MAP).withType(String.class)
				.withDescription("Map mapping existing annotation keys to MDF markers.")
				.isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(CUSTOM_MARKERS).withType(String.class)
				.withDescription("Map mapping existing annotation keys to custom Toolbox markers supplementing the official MDF markers.")
				.isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(NULL_PLACEHOLDER).withType(String.class)
				.withDescription("Placeholder for null values to be inserted into Toolbox file to keep linearization intact.")
				.isRequired(false).build());
	}
	
	@SuppressWarnings("javadoc")
	public String getRefSpanLayer() {
		return (String) getProperty(REF_SPAN_LAYER).getValue();
	}

	@SuppressWarnings("javadoc")
	public String getIdSpanLayer() {
		return (String) getProperty(ID_SPAN_LAYER).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public String getTxTokenLayer() {
		return (String) getProperty(TX_TOKEN_LAYER).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public String getMbTokenLayer() {
		return (String) getProperty(MB_TOKEN_LAYER).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public String getIdIdentifierAnnotation() {
		return (String) getProperty(ID_IDENT_ANNOTATION).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public String getRefIdentifierAnnotation() {
		return (String) getProperty(REF_IDENT_ANNOTATION).getValue();
	}
	
	/**
	 * Returns a list of annotations that re-duplicate primary
	 * lexical material and should be ignored during conversion.
	 * 
	 * @return The list of annotations to ignore, or an empty {@link List}
	 */
	public List<String> getTxMaterialAnnotations() {
		if (getProperty(TX_ANNOTATIONS_TO_IGNORE).getValue() != null) {
			return new ArrayList<>(Arrays.asList(((String) getProperty(TX_ANNOTATIONS_TO_IGNORE).getValue()).split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)));
		}
		else return new ArrayList<String>();
	}
	
	/**
	 * Returns a list of annotations that re-duplicate primary
	 * morphological material and should be ignored during conversion.
	 * 
	 * @return The list of annotations to ignore, or an empty {@link List}
	 */
	public List<String> getMbMaterialAnnotations() {
		if (getProperty(MB_ANNOTATIONS_TO_IGNORE).getValue() != null) {
			return new ArrayList<>(Arrays.asList(((String) getProperty(MB_ANNOTATIONS_TO_IGNORE).getValue()).split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)));
		}
		else {
			return new ArrayList<String>();
		}
	}

	@SuppressWarnings("javadoc")
	public String getSpaceReplacement() {
		return (String) getProperty(SPACE_REPLACEMENT).getValue();
	}
	
	@SuppressWarnings("javadoc")
	public String getNullPlaceholder() {
		return (String) getProperty(NULL_PLACEHOLDER).getValue();
	}
	
	/**
	 * Compiles and returns a map of annotations to MDF
	 * markers.
	 * 
	 * **Note:** Direct access to this map is discouraged. Instead, use
	 * {@link #getAnnotationMarkerMap()}!
	 * 
	 * During compilation, both keys and values are checked
	 * for uniqueness. If a duplicate key or value is found,
	 * an {@link IllegalArgumentException} is thrown, as
	 * processing duplicate keys or values will break the
	 * data model.
	 * 
	 * @return The deduplicated map of annotations to MDF markers
	 */
	public Map<String, String> getMDFMap() {
		Map<String, String> mdfMap = HashBiMap.create();
		if (getProperty(MDF_MAP).getValue() != null) {
			List<String> entries = new ArrayList<>(Arrays.asList(
					((String) getProperty(MDF_MAP).getValue()).split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)));
			for (String entry : entries) {
				try {
					String[] split = entry.split(MARKER_SPLIT_REGEX, 2);
					String key = split[0].trim(); // Salt annotation
					String mdf = split[1].trim(); // MDF marker
					// Check if key exists
					if (mdfMap.containsKey(key)) {
						IllegalArgumentException iae = new IllegalArgumentException("Key already present: " + key);
						logger.error("MDF Map: {}!", iae.getMessage(), iae);
						throw iae;
					}
					if (!mdfKeys.contains(mdf)) {
						logger.error(
								"MDF Map: The value '{}' (key '{}') is not a valid MDF marker! Entry will be ignored! Please refer to the following reference for a list of valid MDF markers: "
										+ "Coward, David F.; Grimes, Charles E. (2000): \"Making Dictionaries. A guide to lexicography and the Multi-Dictionary Formatter\"."
										+ "SIL International: Waxhaw, North Carolina. 183-185. URL http://downloads.sil.org/legacy/shoebox/MDF_2000.pdf.",
								mdf, key);
						continue;
					}
					try {
						mdfMap.put(key, mdf);
					}
					catch (IllegalArgumentException e) {
						logger.error("MDF Map: {}!", e.getMessage(), e);
						throw e;
					}
				}
				catch (ArrayIndexOutOfBoundsException e) {
					logger.error(
							"Map of annotation keys to MDF markers produced an error. Please check the syntactical correctness and re-try conversion.");
					throw e;
				}
			}
		}
		return mdfMap;
	}
	
	/**
	 * Compiles and returns a map of annotations to user-defined
	 * custom markers.
	 * 
	 * **Note:** Direct access to this map is discouraged. Instead, use
	 * {@link #getAnnotationMarkerMap()}!
	 * 
	 * During compilation, both keys and values are checked
	 * for uniqueness. If a duplicate key or value is found,
	 * an {@link IllegalArgumentException} is thrown, as
	 * processing duplicate keys or values will break the
	 * data model.
	 * 
	 * @return The deduplicated map of annotations to custom markers
	 */
	public Map<String, String> getCustomMarkerMap() {
		Map<String, String> customMarkerMap = HashBiMap.create();
		if (getProperty(CUSTOM_MARKERS).getValue() != null) {
			List<String> entries = new ArrayList<>(Arrays.asList(((String) getProperty(CUSTOM_MARKERS).getValue())
					.split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)));
			for (String entry : entries) {
				try {
					String[] split = entry.split(MARKER_SPLIT_REGEX, 2);
					String key = split[0].trim();
					String marker = split[1].trim();
					// Check if key exists
					if (customMarkerMap.containsKey(key)) {
						IllegalArgumentException iae = new IllegalArgumentException("Key already present: " + key);
						logger.error("Custom Marker Map: {}!", iae.getMessage(), iae);
						throw iae;
					}
					if (mdfKeys.contains(marker)) {
						logger.error("Custom Marker Map: The value '{}' (key '{}') is a reserved MDF marker! Entry will be ignored! Please refer to the following reference for a list of MDF markers and change the custom marker to something not contained in the list: \n" 
																		+ "Coward, David F.; Grimes, Charles E. (2000): \"Making Dictionaries. A guide to lexicography and the Multi-Dictionary Formatter\".", 
								marker, key);
						continue;
					}
					try {
						customMarkerMap.put(key, marker);
					}
					catch (IllegalArgumentException e) {
						logger.error("Custom Marker Map: {}!", e.getMessage(), e);
						throw e;
					}
				}
				catch (ArrayIndexOutOfBoundsException e) {
					logger.error(
							"Map of annotation keys to custom markers produced an error. Please check the syntactical correctness and re-try conversion.",
							e);
					throw e;
				}
			}
		}
		return customMarkerMap;
	}
	
	/**
	 * Compiles and returns a map of annotations to Toolbox markers.
	 * 
	 * The returned map comprises both the MDF map and the custom
	 * marker map.
	 * 
	 * During compilation, both keys and values are checked
	 * for uniqueness. If a duplicate key or value is found,
	 * an {@link IllegalArgumentException} is thrown, as
	 * processing duplicate keys or values will break the
	 * data model.
	 * 
	 * @return The deduplicated map of annotations to Toolbox markers
	 * 
	 * @see #getMDFMap()
	 * @see #getCustomMarkerMap()
	 */
	public Map<String, String> getAnnotationMarkerMap() {
		BiMap<String, String> annotationMarkerBiMap = HashBiMap.create();
		for (Entry<String, String> mdf : getMDFMap().entrySet()) {
			String key = mdf.getKey();
			String marker = mdf.getValue();
			if (annotationMarkerBiMap.containsKey(key)) {
				IllegalArgumentException iae = new IllegalArgumentException("Key already present: " + key);
				logger.error("Annotation Marker Map: {}!", iae.getMessage(), iae);
				throw iae;
			}
			try {
				annotationMarkerBiMap.put(key, marker);
			}
			catch (IllegalArgumentException e) {
				logger.error("Annotation Marker Map: {}!", e.getMessage(), e);
				throw e;
			}
		}
		for (Entry<String, String> customMarker : getCustomMarkerMap().entrySet()) {
			String key = customMarker.getKey();
			String marker = customMarker.getValue();
			if (annotationMarkerBiMap.containsKey(key)) {
				IllegalArgumentException iae = new IllegalArgumentException("Key already present: " + key);
				logger.error("Annotation Marker Map: {}!", iae.getMessage(), iae);
				throw iae;
			}
			try {
				annotationMarkerBiMap.put(key, marker);
			}
			catch (IllegalArgumentException e) {
				logger.error("Annotation Marker Map: {}!", e.getMessage(), e);
				throw e;
			}
		}
		return annotationMarkerBiMap;
	}

	
}

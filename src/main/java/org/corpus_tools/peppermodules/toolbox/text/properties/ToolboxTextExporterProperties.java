/**
 * Copyright (c) 2016ff. Stephan Druskat.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.toolbox.text.utils.ToolboxTextModulesUtils;

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
	 * TODO CHeck if this is API
	 */
	public static final String MARKER_SPLIT_REGEX = "(?<!:):(?!:)";
	
	/**
	 * A placeholder inserted for null values to keep linearization intact.
	 */
	public static final String NULL_PLACEHOLDER = "nullPlaceholder";
	
	/**
	 * A map mapping annotation names to other annotation names.
	 * 
	 * Format: `markerMap=aa=bb,cc=dd`.
	 * 
	 * This will result in annotations with name "aa" mapped
	 * to a line "\bb ..." in the Toolbox file output.
	 */
	public static final String MARKER_MAP = "markerMap";

	/**
	 * TODO
	 */
	public static final String MAP_LAYER = "mapLayer";

	/**
	 * TODO
	 */
	public static final String MAP_NAMESPACE = "mapNamespace";
	
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
		addProperty(PepperModuleProperty.create().withName(NULL_PLACEHOLDER).withType(String.class)
				.withDescription("Placeholder for null values to be inserted into Toolbox file to keep linearization intact.")
				.isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(MARKER_MAP).withType(String.class)
				.withDescription("map mapping annotation names to other annotation names.")
				.isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(MAP_LAYER).withType(Boolean.class)
				.withDescription("Whether to map the layer of an annotation's container to the Toolbox marker.")
				.isRequired(false).withDefaultValue(false).build());
		addProperty(PepperModuleProperty.create().withName(MAP_NAMESPACE).withType(Boolean.class)
				.withDescription("Whether to map the namespace of an annotation to the Toolbox marker.")
				.isRequired(false).withDefaultValue(false).build());
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
	
	
	@SuppressWarnings("javadoc")
	public Map<Triple<String,String,String>,String> getMarkerMap() {
		Map<Triple<String,String,String>,String> map = new HashMap<>();
		String prop = (String) getProperty(MARKER_MAP).getValue();
		if (prop != null) {
			String[] split = prop.split(",");
			for (String mapping : split) {
				String[] newNameSplit = mapping.split("=");
				if (newNameSplit.length == 2) {
					String tripleString = newNameSplit[0];
					String newName = newNameSplit[1];
					Triple<String, String, String> triple = createTripleFromString(tripleString);
					map.put(triple, newName);
				}
				else {
					throw new PepperModuleException(
							"Property 'markerMap' is formatted incorrectly (no '=' found for value).");
				}
			}
		}
		return map;
	}

	private Triple<String, String, String> createTripleFromString(String string) {
		String layer = null;
		String namespace = null;
		String name = null;
		String[] layerSplit = string.split("::");
		if (layerSplit.length == 2) {
			// layer::name | layer::namespace:name
			layer = layerSplit[0].trim();
			String[] languageNameSplit = null;
			if ((languageNameSplit = layerSplit[1].split(":")).length == 2) {
				// layer::namespace:name
				namespace = languageNameSplit[0].trim();
				name = languageNameSplit[1].trim();
			}
			else {
				// layer::name
				name = layerSplit[1];
			}
		}
		else {
			// layer:name | name
			String[] languageNameSplit = string.split(":");
			if (languageNameSplit.length == 2) {
				// layer:name
				namespace = languageNameSplit[0].trim();
				name = languageNameSplit[1].trim();
			}
			else {
				// name
				name = string.trim();
			}
		}
		return Triple.of(layer, namespace, name);
	}

	@SuppressWarnings("javadoc")
	public Boolean mapLayer() {
		return (Boolean) getProperty(MAP_LAYER).getValue();
	}

	@SuppressWarnings("javadoc")
	public Boolean mapNamespace() {
		return (Boolean) getProperty(MAP_NAMESPACE).getValue();
	}

	
}

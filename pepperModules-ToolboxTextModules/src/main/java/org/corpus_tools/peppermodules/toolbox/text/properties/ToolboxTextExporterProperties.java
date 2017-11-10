/**
 * 
 */
package org.corpus_tools.peppermodules.toolbox.text.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter;
import org.corpus_tools.peppermodules.toolbox.text.utils.ToolboxTextModulesUtils;

/**
 * // TODO Add description
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 * 
 */
public class ToolboxTextExporterProperties extends PepperModuleProperties {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -778762953092094905L;
	
	/*
	 * Layer names
	 */
	/**
	 * TODO
	 */
	public static final String REF_SPAN_LAYER = "refSpanLayer";
	
	public static final String ID_SPAN_LAYER = "idSpanLayer";
	
	public static final String TX_TOKEN_LAYER = "txTokenLayer";
	
	public static final String MB_TOKEN_LAYER = "mbTokenLayer";
	
	/*
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
	public static final String REF_IDENT_ANNOTATION = "refIdentifierAnnotation";
	public static final String TX_IDENT_ANNOTATION = "txIdentifierAnnotation";
	public static final String MB_IDENT_ANNOTATION = "mbIdentifierAnnotation";
	
	/*
	 * Annotations which contain primary data, i.e., lexical or morphological material
	 * which will already be mapped to tokens but still exists as annotation
	 * and should thus be left out during export to annotations (as they will
	 * already be mapped to \tx or \mb).
	 */
	public static final String TX_ANNOTATIONS_TO_IGNORE = "ignoreTxAnnotations";
	public static final String MB_ANNOTATIONS_TO_IGNORE = "ignoreMbAnnotations";
	
	// Other properties
	/*
	 * The replacement String to be used for replacing whitespaces in
	 * annotation values which may break the item count if not replaced.
	 */
	public static final String SPACE_REPLACEMENT = "spaceReplacement";
	
	/**
	 * // TODO Add description
	 * 
	 */
	public ToolboxTextExporterProperties() {
		addProperty(PepperModuleProperty.create().withName(REF_SPAN_LAYER).withType(String.class)
				.withDescription("The Salt layer that contains the spans to be mapped to Toolbox \\refs.")
				.withDefaultValue("ref").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(ID_SPAN_LAYER).withType(String.class)
				.withDescription("The Salt layer that contains the spans to be mapped to Toolbox \\ids.")
				.withDefaultValue("id").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(ID_IDENT_ANNOTATION).withType(String.class)
				.withDescription("The annotation (namespace::name) that contains the identifier of \\ids.")
				.withDefaultValue("").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(REF_IDENT_ANNOTATION).withType(String.class)
				.withDescription("The annotation (namespace::name) that contains the identifier of \\refs.")
				.withDefaultValue("").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(TX_IDENT_ANNOTATION).withType(String.class)
				.withDescription("The annotation (namespace::name) that contains the (partial) content of \\tx lines.")
				.withDefaultValue("").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(MB_IDENT_ANNOTATION).withType(String.class)
				.withDescription("The annotation (namespace::name) that contains the (partial) content of \\mb lines.")
				.withDefaultValue("").isRequired(true).build());
		addProperty(PepperModuleProperty.create().withName(TX_ANNOTATIONS_TO_IGNORE).withType(String.class)
				.withDescription("Annotations (namespace::name) on lexical tokens which should be ignored, as a comma-separated list.")
				.withDefaultValue("").isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(MB_ANNOTATIONS_TO_IGNORE).withType(String.class)
				.withDescription("Annotations (namespace::name) on lexical tokens which should be ignored, as a comma-separated list.")
				.withDefaultValue("").isRequired(false).build());
		addProperty(PepperModuleProperty.create().withName(SPACE_REPLACEMENT).withType(String.class)
				.withDescription("String to replace whitespaces in annotation values with, as these whitespaces may break the item count in Toolbox interlinearization.")
				.withDefaultValue("-").isRequired(true).build());
	}
	
	/**
	 * // TODO Add description
	 * 
	 * @return
	 */
	public String getRefSpanLayer() {
		return (String) getProperty(REF_SPAN_LAYER).getValue();
	}

	public String getIdSpanLayer() {
		return (String) getProperty(ID_SPAN_LAYER).getValue();
	}
	
	public String getTxTokenLayer() {
		return (String) getProperty(TX_TOKEN_LAYER).getValue();
	}
	
	public String getMbTokenLayer() {
		return (String) getProperty(MB_TOKEN_LAYER).getValue();
	}
	
	public String getIdIdentifierAnnotation() {
		return (String) getProperty(ID_IDENT_ANNOTATION).getValue();
	}
	
	public String getRefIdentifierAnnotation() {
		return (String) getProperty(REF_IDENT_ANNOTATION).getValue();
	}
	
	public String getTxIdentifierAnnotation() {
		return (String) getProperty(TX_IDENT_ANNOTATION).getValue();
	}
	
	public List<String> getTxMaterialAnnotations() {
		return new ArrayList<>(Arrays.asList(((String) getProperty(TX_ANNOTATIONS_TO_IGNORE).getValue()).split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)));
	}
	
	public List<String> getMbMaterialAnnotations() {
		return new ArrayList<>(Arrays.asList(((String) getProperty(MB_ANNOTATIONS_TO_IGNORE).getValue()).split(ToolboxTextModulesUtils.COMMA_DELIM_SPLIT_REGEX)));
	}

	public String getSpaceReplacement() {
		return (String) getProperty(SPACE_REPLACEMENT).getValue();
	}
	
}

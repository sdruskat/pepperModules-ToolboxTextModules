/**
 * 
 */
package org.corpus_tools.peppermodules.toolbox.text.properties;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;

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
	
	public String getIdIdentifierAnnotation() {
		return (String) getProperty(ID_IDENT_ANNOTATION).getValue();
	}
	
	public String getRefIdentifierAnnotation() {
		return (String) getProperty(REF_IDENT_ANNOTATION).getValue();
	}

}

package org.corpus_tools.peppermodules.toolbox.text;

import org.corpus_tools.pepper.impl.PepperExporterImpl;
import org.corpus_tools.pepper.modules.PepperExporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleNotReadyException;
import org.corpus_tools.peppermodules.toolbox.text.mapping.ToolboxTextExportMapper;
import org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;

/**
 * // TODO Add description
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 * 
 */
@Component(name = "ToolboxTextExporterComponent", factory = "PepperExporterComponentFactory")
public class ToolboxTextExporter extends PepperExporterImpl implements PepperExporter {

	/**
	 * TODO
	 */
	public static final String FORMAT_NAME = "toolbox-text";
	/**
	 * TODO
	 */
	public static final String FORMAT_VERSION = "3.0";

	/**
	 * // TODO Add description
	 * 
	 */
	public ToolboxTextExporter() {
		super();
		setName("ToolboxTextExporter");
		setVersion("1.0.0-SNAPSHOT");
		setSupplierContact(URI.createURI("stephan.druskat@hu-berlin.de"));
		setSupplierHomepage(URI.createURI("http://corpus-tools.org"));
		setDesc("An exporter for the text-based format written by SIL Toolbox (as opposed to the respective XML format).");
		addSupportedFormat("toolbox-text", "3.0", null);
		this.setProperties(new ToolboxTextExporterProperties());
		setDocumentEnding("txt");
		setExportMode(EXPORT_MODE.DOCUMENTS_IN_FILES);
	}

	/* (non-Javadoc)
	 * @see org.corpus_tools.pepper.impl.PepperModuleImpl#createPepperMapper(org.corpus_tools.salt.graph.Identifier)
	 */
	@Override
	public PepperMapper createPepperMapper(Identifier identifier) {
		PepperMapper mapper = new ToolboxTextExportMapper();
		URI resourceURI = getIdentifier2ResourceTable().get(identifier);
		// TODO: Customize document file name by changing the resourceURI!
		mapper.setResourceURI(resourceURI);
		return (mapper);
	}

	/* (non-Javadoc)
	 * @see org.corpus_tools.pepper.impl.PepperModuleImpl#isReadyToStart()
	 */
	@Override
	public boolean isReadyToStart() throws PepperModuleNotReadyException {
		PepperModuleProperties properties = getProperties();
		properties.checkProperties();
		return (super.isReadyToStart());
	}
}

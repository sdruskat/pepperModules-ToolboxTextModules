/*******************************************************************************
 * Copyright (c) 2016, 2018ff. Stephan Druskat
 * Exploitation rights for this version belong exclusively to Humboldt-Universität zu Berlin
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
 *     Stephan Druskat (mail@sdruskat.net) - initial API and implementation
 *******************************************************************************/
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
 * The point-of-entry class for export of Salt models to Toolbox text.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 * 
 */
@Component(name = "ToolboxTextExporterComponent", factory = "PepperExporterComponentFactory")
public class ToolboxTextExporter extends PepperExporterImpl implements PepperExporter {

	/**
	 * Format name constant.
	 * 
	 * This is an arbitrary value, which called just as well
	 * be `mdf` after the "Multi-Dictionary Formatter" format.
	 */
	public static final String FORMAT_NAME = "toolbox-text";
	/**
	 * Format version constant.
	 */
	public static final String FORMAT_VERSION = "3.0";

	/**
	 * Constructor setting metadata mostly. 
	 */
	public ToolboxTextExporter() {
		super();
		setName("ToolboxTextExporter");
		setVersion("1.0.0");
		setSupplierContact(URI.createURI("stephan.druskat@hu-berlin.de"));
		setSupplierHomepage(URI.createURI("https://github.com/sdruskat/pepperModules-ToolboxTextModules"));
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

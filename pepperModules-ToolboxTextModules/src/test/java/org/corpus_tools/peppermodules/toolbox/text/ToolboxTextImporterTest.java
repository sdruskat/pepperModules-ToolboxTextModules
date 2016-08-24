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

import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.graph.Identifier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ToolboxTextImporter}.
 *
 * @author Stephan Druskat
 *
 */
public class ToolboxTextImporterTest extends PepperImporterTest {
	@Before
	public void setUp() {
		ToolboxTextImporter importer = new ToolboxTextImporter();
		importer.setProperties(new ToolboxTextImporterProperties());
		setFixture(importer);

		FormatDesc formatDef = new FormatDesc();
		formatDef.setFormatName("toolbox-text");
		formatDef.setFormatVersion("3.0");
		this.supportedFormatsCheck.add(formatDef);
	}

	@Test
	public void testFileEndings() {
		assertTrue(getFixture().isReadyToStart());
		assertTrue(getFixture().getDocumentEndings().contains("txt"));
		assertTrue(getFixture().getDocumentEndings().contains("lbl"));
	}
	
	@Test(expected=PepperModuleException.class)
	public void testCreatePepperMapper() {
		getFixture().createPepperMapper(null);
	}
	
	@Test(expected=PepperModuleException.class)
	public void testCreatePepperMapperNullIdentifier() {
		Identifier nullIdentifier = SaltFactory.createIdentifier(null, "Null identifier");
		getFixture().createPepperMapper(nullIdentifier);
	}
	
	@Test(expected=PepperModuleException.class)
	public void testCreatePepperMapperWrongIdentifier() {
		Identifier wrongIdentifier = SaltFactory.createIdentifier(SaltFactory.createSCorpus(), "Wrong identifier (Corpus)");
		getFixture().createPepperMapper(wrongIdentifier);
	}
	
	@Test
	public void testCreatePepperMapperInstanceOf() {
		PepperMapper mapper = getFixture().createPepperMapper(SaltFactory.createIdentifier(SaltFactory.createSDocument(), "Document"));
		assertNotNull(mapper);
		assertTrue(mapper instanceof PepperMapper);
	}

}

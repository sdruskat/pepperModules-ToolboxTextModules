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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ToolboxTextImporter}.
 *
 * @author Stephan Druskat
 *
 */
public class ToolboxTextImporterTest_NEW extends PepperImporterTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.setFixture(new ToolboxTextImporter());
		this.supportedFormatsCheck.add(new FormatDesc().setFormatName("toolbox-text").setFormatVersion("3.0"));
		this.getFixture().getCorpusDesc().getFormatDesc().setFormatName("toolbox-text").setFormatVersion("3.0");
		getFixture().getSaltProject().createCorpusGraph();
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#createPepperMapper(org.corpus_tools.salt.graph.Identifier)}.
	 */
	@Test
	public void testCreatePepperMapper() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 0 \ids and 0 \refs,
	 * i.e., an empty corpus.
	 */
	@Test(expected=PepperModuleException.class)
	public void testParseQuasiEmptyCorpus() {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getFile("empty-corpus.txt"))));
		start();
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 0 \ids and n \refs,
	 * i.e., what will become a single corpus with a single document.
	 */
	@Test
	public void testParseMonolithicDocument() {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getFile("no-ids.txt"))));
		start();
		assertEquals((Long) 32L, getFixture().getHeaderEndOffset());
		assertTrue(getFixture().isMonolithic());
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 0 \ids and n \refs,
	 * i.e., what will become a single corpus with a single document.
	 */
	@Test
	public void testParseStandardDocument() {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getFile("ids.txt"))));
		start();
		assertEquals((Long) 32L, getFixture().getHeaderEndOffset());
		assertFalse(getFixture().isMonolithic());
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 0 \ids and 0 \refs,
	 * i.e., an empty corpus.
	 */
	@Test
	public void testParseOrphanRefs() {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getFile("orphan-ids-and-refs.txt"))));
		start();
	}

	private String getFile(String fileName) {
		return this.getClass().getClassLoader().getResource(fileName).getFile();
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#getProperties()}.
	 */
	@Test
	public void testGetProperties() {
		assertTrue(getFixture().getProperties() instanceof ToolboxTextImporterProperties);
	}
	
	@Override
	public ToolboxTextImporter getFixture() {
		return (ToolboxTextImporter) super.getFixture();
	}
	

}

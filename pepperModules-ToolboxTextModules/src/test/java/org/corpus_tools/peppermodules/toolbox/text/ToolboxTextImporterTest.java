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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;

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

		FormatDesc formatDef = new FormatDesc().setFormatName("toolbox-text").setFormatVersion("3.0");
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
		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_SPLIT_IDS_TO_DISCRETE_DOCUMENTS, false);
		getFixture().createPepperMapper(null);
	}
	
	@Test(expected=PepperModuleException.class)
	public void testCreatePepperMapperNullIdentifier() {
		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_SPLIT_IDS_TO_DISCRETE_DOCUMENTS, false);
		Identifier nullIdentifier = SaltFactory.createIdentifier(null, "Null identifier");
		getFixture().createPepperMapper(nullIdentifier);
	}
	
	@Test
	public void testCreatePepperMapperInstanceOf() {
		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_SPLIT_IDS_TO_DISCRETE_DOCUMENTS, false);
		PepperMapper mapper = getFixture().createPepperMapper(SaltFactory.createIdentifier(SaltFactory.createSDocument(), "Document"));
		assertNotNull(mapper);
		assertTrue(mapper instanceof PepperMapper);
	}
	
	@Test
	public void testIdBasedMapping() {
		run("ids.txt");
		assertEquals(1, getFixture().getSaltProject().getCorpusGraphs().size());
		assertEquals(3, getFixture().getCorpusGraph().getDocuments().size());
		for (SDocument doc : getFixture().getCorpusGraph().getDocuments()) {
			assertThat(doc.getName(), anyOf(is("ID1"), is("ID2"), is("ID3")));
		}
		SDocument doc1 = null;
		SDocument doc2 = null;
		SDocument doc3 = null;
		for (SDocument doc : getFixture().getCorpusGraph().getDocuments()) {
			if (doc.getName().equals("ID1")) {
				doc1 = doc;
			}
			else if(doc.getName().equals("ID2")) {
				doc2 = doc;
			}
			else {
				doc3 = doc;
			}
		}
		assertEquals(4, doc1.getDocumentGraph().getTokens().size());
		assertEquals(4, doc2.getDocumentGraph().getTokens().size());
		assertEquals(4, doc3.getDocumentGraph().getTokens().size());
		assertEquals(2, doc1.getDocumentGraph().getSpans().size());
		assertEquals(2, doc2.getDocumentGraph().getSpans().size());
		assertEquals(2, doc3.getDocumentGraph().getSpans().size());
	}
	
	private void run(String pathToTestFile) {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(new File(this.getClass().getClassLoader().getResource(pathToTestFile).getFile()).getAbsolutePath())));
		getFixture().getCorpusDesc().getFormatDesc().setFormatName("toolbox-text").setFormatVersion("3.0");
		start();
	}

}

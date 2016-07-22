/*******************************************************************************
 * Copyright 2016 Stephan Druskat
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

import static org.junit.Assert.*; 

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SSpanningRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNamedElement;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests testing the ID handling of {@link ToolboxTextMapper}.
 *
 * @author Stephan Druskat <mail@sdruskat.net>
 *
 */
public class ToolboxTextMapperIdTest {
	
	private ToolboxTextMapper fixture = null;

	/**
	 * Set up the fixture.
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		ToolboxTextMapper mapper = new ToolboxTextMapper();
		File file = new File(this.getClass().getClassLoader().getResource("ids.txt").getFile());
		String path = file.getAbsolutePath();
		mapper.setResourceURI(URI.createFileURI(path));
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		mapper.setProperties(properties);
		SDocument doc = SaltFactory.createSDocument();
		mapper.setDocument(doc);
		setFixture(mapper);
	}

	@Test
	public void testIDs() {
		getFixture().mapSDocument();
		SDocumentGraph graph = getFixture().getGraph();
		SLayer lexLayer = graph.getLayerByName(getFixture().getProperties().getLexMarker()).get(0);
		List<SToken> tokenList = new ArrayList<>();
		for (SNode node : lexLayer.getNodes()) {
			if (node instanceof SToken) {
				tokenList.add((SToken) node);
			}
		}
		List<SToken> sortedTokens = graph.getSortedTokenByText(tokenList);
		SLayer idLayer = graph.getLayerByName(getFixture().getProperties().getIdMarker()).get(0);
		List<SSpan> idSpans = new ArrayList<>();
		for (SNode node : idLayer.getNodes()) {
			if (node instanceof SSpan) {
				idSpans.add((SSpan) node);
			}
		}
		assertEquals(3, idSpans.size());
		for (SSpan span : idSpans) {
			if (span.getName().equals("ID1")) {
				for (SRelation rel : graph.getOutRelations(span.getId())) {
					if (rel instanceof SSpanningRelation && rel.getTarget() instanceof SSpan) {
						SSpan target = (SSpan) rel.getTarget();
						assertTrue(target.getName().equals("REF1") || target.getName().equals("REF2"));
					}
				}
				assertEquals("Info on ID1", span.getAnnotation("toolbox::idinfo").getValue());
			}
			else if (span.getName().equals("ID2")) {
				for (SRelation rel : graph.getOutRelations(span.getId())) {
					if (rel instanceof SSpanningRelation && rel.getTarget() instanceof SSpan) {
						SSpan target = (SSpan) rel.getTarget();
						assertTrue(target.getName().equals("REF3") || target.getName().equals("REF4"));
					}
				}
				assertEquals("Info on ID2", span.getAnnotation("toolbox::idinfo").getValue());
			}
			else if (span.getName().equals("ID3")) {
				for (SRelation rel : graph.getOutRelations(span.getId())) {
					if (rel instanceof SSpanningRelation && rel.getTarget() instanceof SSpan) {
						SSpan target = (SSpan) rel.getTarget();
						assertTrue(target.getName().equals("REF5") || target.getName().equals("REF6"));
					}
				}
				assertEquals("Info on ID3", span.getAnnotation("toolbox::idinfo").getValue());
			}
		}
	}
	
	@Test
	public void testMeta() {
		getFixture().mapSDocument();
		assertEquals("Some info", getFixture().getDocument().getMetaAnnotation("toolbox::info"));
	}

	/**
	 * @return the fixture
	 */
	private ToolboxTextMapper getFixture() {
		return fixture;
	}

	/**
	 * @param fixture the fixture to set
	 */
	private void setFixture(ToolboxTextMapper fixture) {
		this.fixture = fixture;
	}

}

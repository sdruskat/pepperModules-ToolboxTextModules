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
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests testing the alignment fixing functionality of {@link ToolboxTextMapper}.
 *
 * @author Stephan Druskat <mail@sdruskat.net>
 *
 */
public class ToolboxTextMapperAlignmentTest4 {
	
	private MonolithicToolboxTextMapper fixture = null;

	/**
	 * Set up the fixture.
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		MonolithicToolboxTextMapper mapper = new MonolithicToolboxTextMapper();
		File file = new File(this.getClass().getClassLoader().getResource("alignment-not-enough-morph-annos.txt").getFile());
		String path = file.getAbsolutePath();
		mapper.setResourceURI(URI.createFileURI(path));
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_FIX_ALIGNMENT, true);
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_HAS_IDS, false);
		mapper.setProperties(properties);
		SDocument doc = SaltFactory.createSDocument();
		mapper.setDocument(doc);
		setFixture(mapper);
	}
	
	@Test(expected = PepperModuleException.class)
	public void testException() {
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_FIX_ALIGNMENT, false);
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_HAS_IDS, false);
		getFixture().setProperties(properties);
		getFixture().mapSDocument();
	}

	@Test
	public void testFixMissingMorphAnnotations() {
		getFixture().mapSDocument();
		SDocumentGraph graph = getFixture().getGraph();
		SLayer morphLayer = graph.getLayerByName(getFixture().getProperties().getMorphMarker()).get(0);
		List<SToken> tokenList = new ArrayList<>();
		for (SNode node : morphLayer.getNodes()) {
			if (node instanceof SToken) {
				tokenList.add((SToken) node);
			}
		}
		List<SToken> sortedMorphTokens = graph.getSortedTokenByText(tokenList);
		assertEquals("BROKEN_ALIGNMENT", sortedMorphTokens.get(sortedMorphTokens.size() - 1).getAnnotation("toolbox::ge").getValue());
	}
	
	/**
	 * @return the fixture
	 */
	private MonolithicToolboxTextMapper getFixture() {
		return fixture;
	}

	/**
	 * @param fixture the fixture to set
	 */
	private void setFixture(MonolithicToolboxTextMapper fixture) {
		this.fixture = fixture;
	}

}

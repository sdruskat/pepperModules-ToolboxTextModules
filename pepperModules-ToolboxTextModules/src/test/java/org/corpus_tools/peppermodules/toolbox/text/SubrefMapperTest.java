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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;

import org.apache.commons.lang3.Range;
import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporterProperties;
import org.corpus_tools.peppermodules.toolbox.text.data.SubrefDefinition;
import org.corpus_tools.peppermodules.toolbox.text.data.SubrefDefinition.SUBREF_TYPE;
import org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO Description
 *
 * @author Stephan Druskat
 *
 */
public class SubrefMapperTest {
	
	private SubrefMapper fixture;
	
	/**
	 * TODO: Description
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		ToolboxTextImporterProperties props = setProperties("subref.properties");
		SubrefMapper subrefMapper = new SubrefMapper(new HashMap<String, String>(), props, null, null, null, null, null, true);
		setFixture(subrefMapper);
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper#createSubrefFromSubrefLine(java.lang.String)}.
	 */
	@Test
	public void testCreateSubrefFromSubrefLine() {
		String unid = "1123 7653", unidT = "mb 123 456", id = "def 321 654", idT = "def tx 21 43", dis = "def mb 12 34 56 78 90 100 123 456";
		String unidFail = "1123 76S3", unidTFail = "mb 1Z3 456", idFail = "def 321 6S4", idTFail = "def tx 2I 43", disFail = "def mb 12 34 56 7B 90 100 123 456";
		SubrefDefinition sd = getFixture().createSubrefDefinitionFromSubrefLine(unid);
		assertNull(sd.getIdentifier());
		assertNull(sd.getTargetLayer());
		assertThat(sd.getType(), is(SUBREF_TYPE.UNIDENTIFIED_GLOBAL));
		assertThat(sd.getRanges().size(), is(1));
		assertThat(sd.getRanges().get(0), is(Range.between(1123, 7653)));
		
		sd = getFixture().createSubrefDefinitionFromSubrefLine(unidT);
		assertNull(sd.getIdentifier());
		assertThat(sd.getTargetLayer(), is("mb"));
		assertThat(sd.getType(), is(SUBREF_TYPE.UNIDENTIFIED_GLOBAL_TARGETED));
		assertThat(sd.getRanges().size(), is(1));
		assertThat(sd.getRanges().get(0), is(Range.between(123, 456)));

		sd = getFixture().createSubrefDefinitionFromSubrefLine(id);
		assertThat(sd.getIdentifier(), is("def"));
		assertNull(sd.getTargetLayer());
		assertThat(sd.getType(), is(SUBREF_TYPE.IDENTIFIED_GLOBAL));
		assertThat(sd.getRanges().size(), is(1));
		assertThat(sd.getRanges().get(0), is(Range.between(321, 654)));

		sd = getFixture().createSubrefDefinitionFromSubrefLine(idT);
		assertThat(sd.getIdentifier(), is("def"));
		assertThat(sd.getTargetLayer(), is("tx"));
		assertThat(sd.getType(), is(SUBREF_TYPE.IDENTIFIED_GLOBAL_TARGETED));
		assertThat(sd.getRanges().size(), is(1));
		assertThat(sd.getRanges().get(0), is(Range.between(21, 43)));

		sd = getFixture().createSubrefDefinitionFromSubrefLine(dis);
		assertThat(sd.getIdentifier(), is("def"));
		assertThat(sd.getTargetLayer(), is("mb"));
		assertThat(sd.getType(), is(SUBREF_TYPE.DISCONTINUOUS_TARGETED));
		assertThat(sd.getRanges().size(), is(4));
		assertThat(sd.getRanges().get(0), is(Range.between(12, 34)));
		assertThat(sd.getRanges().get(1), is(Range.between(56, 78)));
		assertThat(sd.getRanges().get(2), is(Range.between(90, 100)));
		assertThat(sd.getRanges().get(3), is(Range.between(123, 456)));
		
		assertNull(getFixture().createSubrefDefinitionFromSubrefLine(unidFail));
		assertNull(getFixture().createSubrefDefinitionFromSubrefLine(unidTFail));
		assertNull(getFixture().createSubrefDefinitionFromSubrefLine(idFail));
		assertNull(getFixture().createSubrefDefinitionFromSubrefLine(idTFail));
		assertNull(getFixture().createSubrefDefinitionFromSubrefLine(disFail));
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper#determineSubrefType(java.lang.String[])}.
	 */
	@Test
	public void testDetermineSubrefType() {
		assertThat(getFixture().determineSubrefType(new String[]{"1", "4"}), is(SUBREF_TYPE.UNIDENTIFIED_GLOBAL));
		assertNull(getFixture().determineSubrefType(new String[]{"1", "four"}));
		assertThat(getFixture().determineSubrefType(new String[]{"tx", "1", "4"}), is(SUBREF_TYPE.UNIDENTIFIED_GLOBAL_TARGETED));
		assertNull(getFixture().determineSubrefType(new String[]{"tx", "one", "4"}));
		assertThat(getFixture().determineSubrefType(new String[]{"def", "1", "4"}), is(SUBREF_TYPE.IDENTIFIED_GLOBAL));
		assertNull(getFixture().determineSubrefType(new String[]{"def", "1", "four"}));
		assertThat(getFixture().determineSubrefType(new String[]{"def", "tx", "1", "4"}), is(SUBREF_TYPE.IDENTIFIED_GLOBAL_TARGETED));
		assertNull(getFixture().determineSubrefType(new String[]{"def", "tx", "one", "4"}));
		assertThat(getFixture().determineSubrefType(new String[]{"def", "tx", "1", "4", "7", "9"}), is(SUBREF_TYPE.DISCONTINUOUS_TARGETED));
		assertNull(getFixture().determineSubrefType(new String[]{"def", "tx", "1", "4", "7", "9", "11"}));
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper#determineSubrefType(java.lang.String[])}.
	 */
	@Test
	public void testDetermineSubrefTypeFail() {
		getFixture().determineSubrefType(new String[]{"def", "tx", "1", "4", "7", "9", "11"});
	}

	/**
	 * @return the fixture
	 */
	private final SubrefMapper getFixture() {
		return fixture;
	}

	/**
	 * @param fixture the fixture to set
	 */
	private final void setFixture(SubrefMapper fixture) {
		this.fixture = fixture;
	}

	private ToolboxTextImporterProperties setProperties(String fileName) {
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		properties.setPropertyValues(new File(getFile(fileName)));
		return properties;
	}
	
	private String getFile(String fileName) {
		return this.getClass().getClassLoader().getResource(fileName).getFile();
	}
}

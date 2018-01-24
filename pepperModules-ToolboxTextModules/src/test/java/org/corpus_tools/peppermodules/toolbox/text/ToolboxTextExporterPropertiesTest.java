/**
 * Copyright 2016ff. Humboldt-Universität zu Berlin.
 * Exploitation rights belong exclusively to Humboldt-Universität zu Berlin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Stephan Druskat (toolboxtextmodules@sdruskat.net) - initial API and implementation
 */
/**
 * 
 */
package org.corpus_tools.peppermodules.toolbox.text;

import static org.junit.Assert.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

import java.util.HashMap;
import java.util.Map;

/**
 * // TODO Add description
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 * 
 */
public class ToolboxTextExporterPropertiesTest {
	
	private ToolboxTextExporterProperties fixture = null;
	
	@SuppressWarnings("rawtypes")
	private Appender mockAppender;
	
	@Captor
	private ArgumentCaptor<LoggingEvent> captorLoggingEvent;
	private Logger rootLogger;

	/**
	 * // TODO Add description
	 * 
	 * @throws java.lang.Exception
	 */
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		setFixture(new ToolboxTextExporterProperties());
		
		// Logging
		rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
	    mockAppender = mock(Appender.class);
	    when(mockAppender.getName()).thenReturn("MOCK");
	    rootLogger.addAppender(mockAppender);
	    rootLogger.setLevel(Level.WARN);
	}
	
    @SuppressWarnings("unchecked")
	@After
    public void clearLoggers() {
    	rootLogger.detachAppender(mockAppender);
    }

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getMDFMap()}.
	 */
	@Test
	public final void testGetValidMDFMap() {
		Map<String, String> testMDFMap = new HashMap<>();
		testMDFMap.put("ntTest", "nt");
		testMDFMap.put("anTest", "an");
		testMDFMap.put("bbTest", "bb");
		getFixture().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "ntTest:nt, anTest : an , bbTest    :       bb   ");
		assertEquals(testMDFMap, getFixture().getMDFMap());
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getMDFMap()}.
	 */
	@Test
	public final void testGetInvalidMDFMap() {
		Map<String, String> invalidMDFMap = new HashMap<>();
		invalidMDFMap.put("ntTest", "nt");
		invalidMDFMap.put("anTest", "an");
		getFixture().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "ntTest:nt, anTest : an  , bbTest    :       invalid   ");
		assertEquals(invalidMDFMap, getFixture().getMDFMap());
		checkLog("MDF Map: The value \'invalid\' (key 'bbTest') is not a valid MDF marker! Entry will be ignored! Please refer to the following reference for a list of valid MDF markers: " + 
				"Coward, David F.; Grimes, Charles E. (2000): \"Making Dictionaries. A guide to lexicography and the Multi-Dictionary Formatter\"." + 
				"SIL International: Waxhaw, North Carolina. 183-185. URL http://downloads.sil.org/legacy/shoebox/MDF_2000.pdf.", Level.ERROR);
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getMDFMap()}.
	 */
	@Test(expected=IllegalArgumentException.class)
	public final void testGetInvalidMDFMapKeyExists() {
		getFixture().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "ntTest:nt, anTest : an  , ntTest    :       invalid   ");
		getFixture().getMDFMap(); // Should trigger exception
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getMDFMap()}.
	 */
	@Test(expected=IllegalArgumentException.class)
	public final void testGetInvalidMDFMapValueExists() {
		getFixture().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "ntTest:nt, anTest : an  , bbTest    :       nt   ");
		getFixture().getMDFMap(); // Should trigger exception
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getMDFMap()}.
	 */
	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public final void testGetInvalidMDFMapAIOOB() {
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "ntTest:nt, anTest : an , invalid    ");
		getFixture().getCustomMarkerMap();
	}
	


	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getCustomMarkerMap()}.
	 */
	@Test
	public final void testGetValidCustomMarkerMap() {
		Map<String, String> testCustomMarkerMap = new HashMap<>();
		testCustomMarkerMap.put("valTest", "val");
		testCustomMarkerMap.put("iTest", "i");
		testCustomMarkerMap.put("dTest", "d");
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "valTest:val, iTest : i  , dTest    :       d   ");
		assertEquals(testCustomMarkerMap, getFixture().getCustomMarkerMap());
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getCustomMarkerMap()}.
	 */
	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public final void testGetInvalidCustomMarkerMap() {
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "ntTest:in, anTest : va , invalid    ");
		getFixture().getCustomMarkerMap();
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getMDFMap()}.
	 */
	@Test
	public final void testGetInvalidCustomMarkerMapMDFValue() {
		Map<String, String> invalidCustomMarkerMap = new HashMap<>();
		invalidCustomMarkerMap.put("ntTest", "cus1");
		invalidCustomMarkerMap.put("anTest", "cus2");
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "ntTest:cus1, anTest : cus2  , bbTest    :       va   ");
		assertEquals(invalidCustomMarkerMap, getFixture().getCustomMarkerMap());
		checkLog("Custom Marker Map: The value 'va' (key 'bbTest') is a reserved MDF marker! Entry will be ignored! Please refer to the following reference for a list of MDF markers and change the custom marker to something not contained in the list: \n" 
				+ "Coward, David F.; Grimes, Charles E. (2000): \"Making Dictionaries. A guide to lexicography and the Multi-Dictionary Formatter\".", Level.ERROR);
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getCustomMarkerMap()}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void testGetInvalidCustomMarkerMapKeyExists() {
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "ntTest:in, anTest : va ,    ntTest   :      invalid    ");
		getFixture().getCustomMarkerMap();
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getCustomMarkerMap()}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void testGetInvalidCustomMarkerMapValueExists() {
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "ntTest:in, anTest : va ,    bbTest   :      in    ");
		getFixture().getCustomMarkerMap();
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getAnnotationMarkerMap()}.
	 */
	@Test
	public final void testGetValidAnnotationMarkerMap() {
		BiMap<String, String> testMap = HashBiMap.create();
		testMap.put("anTest", "an"); // MDF
		testMap.put("bbTest", "bb"); // MDF
		testMap.put("ntTest", "nt"); // MDF
		testMap.put("iTest", "i"); // custom
		testMap.put("dTest", "d"); // custom
		testMap.put("valTest", "val"); // custom
		getFixture().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "ntTest:nt, anTest : an , bbTest    :       bb   ");
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "valTest:val, iTest : i  , dTest    :       d   ");
		assertEquals(testMap, getFixture().getAnnotationMarkerMap());
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getAnnotationMarkerMap()}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void testGetInvalidAnnotationMarkerMapKeyExists() {
		getFixture().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "ntTest:nt, anTest : an , bbTest    :       bb   ");
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "valTest:val, iTest : i  , bbTest    :       d   ");
		getFixture().getAnnotationMarkerMap();
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getAnnotationMarkerMap()}.
	 */
	@Test
	public final void testGetInvalidAnnotationMarkerMapReservedMarker() {
		BiMap<String, String> testMap = HashBiMap.create();
		testMap.put("anTest", "an"); // MDF
		testMap.put("bbTest", "bb"); // MDF
		testMap.put("ntTest", "nt"); // MDF
		testMap.put("iTest", "i"); // custom
		testMap.put("dTest", "d"); // custom
		getFixture().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "ntTest:nt, anTest : an , bbTest    :       bb   ");
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "vaTest:va, iTest : i  , dTest    :       d   ");
		Map<String, String> resultMap = getFixture().getAnnotationMarkerMap();
		assertEquals(testMap, resultMap);
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getAnnotationMarkerMap()}.
	 */
	@Test
	public final void testGetInvalidAnnotationMarkerMapNoMDFMarker() {
		BiMap<String, String> testMap = HashBiMap.create();
		testMap.put("bbTest", "bb"); // MDF
		testMap.put("ntTest", "nt"); // MDF
		testMap.put("valTest", "val"); // custom
		testMap.put("iTest", "i"); // custom
		testMap.put("dTest", "d"); // custom
		getFixture().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "ntTest:nt, anTest : nomdf , bbTest    :       bb   ");
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "valTest:val, iTest : i  , dTest    :       d   ");
		Map<String, String> resultMap = getFixture().getAnnotationMarkerMap();
		assertEquals(testMap, resultMap);
	}
	
	/**
	 * Test method for the marker split regex
	 * {@link ToolboxTextExporterProperties#MARKER_SPLIT_REGEX}
	 */
	@Test
	public final void testMarkerSplitRegex() {
		String regex = ToolboxTextExporterProperties.MARKER_SPLIT_REGEX;
		String s1 = "ns::n:val"; // [ns::n, val]
		String s2 = "ns::n1::n2:val"; // [ns::n1::n2, val]
		String s3 = "ns::n:val1:val2"; // [ns::n, val1:val2]
		String s4 = "ns::n:val1:val2:val3"; // [ns::n, val1:val2:val3]
		String s5 = "ns::n:val1:val2::val3"; // [ns::n, val1:val2::val3]
		String[] a1 = new String[] {"ns::n", "val"};
		String[] a2 = new String[] {"ns::n1::n2", "val"};
		String[] a3 = new String[] {"ns::n", "val1:val2"};
		String[] a4 = new String[] {"ns::n", "val1:val2:val3"};
		String[] a5 = new String[] {"ns::n", "val1:val2::val3"};
		assertThat(s1.split(regex, 2), is(a1));
		assertThat(s2.split(regex, 2), is(a2));
		assertThat(s3.split(regex, 2), is(a3));
		assertThat(s4.split(regex, 2), is(a4));
		assertThat(s5.split(regex, 2), is(a5));
	}



	/**
	 * @return the fixture
	 */
	private final ToolboxTextExporterProperties getFixture() {
		return fixture;
	}

	/**
	 * @param fixture the fixture to set
	 */
	private final void setFixture(ToolboxTextExporterProperties fixture) {
		this.fixture = fixture;
	}
	
	/**
	 * Verifies whether the log contains a specific message.
	 *
	 * @param string
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void checkLog(final String string, final Level level) {
		verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
		      @Override
		      public boolean matches(final Object argument) {
		    	  if (level == null) {
		    		  return ((LoggingEvent) argument).getFormattedMessage().contains(string);
		    	  }
		    	  else {
		    		  return ((LoggingEvent) argument).getFormattedMessage().contains(string) && ((LoggingEvent) argument).getLevel().equals(level);
		    	  }
		      }
		    }));
	}

}

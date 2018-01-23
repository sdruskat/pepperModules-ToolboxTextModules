/**
 * 
 */
package org.corpus_tools.peppermodules.toolbox.text;

import static org.junit.Assert.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.slf4j.LoggerFactory;

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
		testMDFMap.put("nt", "ntTest");
		testMDFMap.put("an", "anTest");
		testMDFMap.put("bb", "bbTest");
		getFixture().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "nt:ntTest, an : anTest , bb    :       bbTest   ");
		assertEquals(testMDFMap, getFixture().getMDFMap());
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getMDFMap()}.
	 */
	@Test
	public final void testGetInvalidMDFMap() {
		Map<String, String> invalidMDFMap = new HashMap<>();
		invalidMDFMap.put("nt", "ntTest");
		invalidMDFMap.put("an", "anTest");
		getFixture().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "nt:ntTest, an : anTest , invalid    :       bbTest   ");
		assertEquals(invalidMDFMap, getFixture().getMDFMap());
		checkLog("MDF Map: The key \'invalid\' is not a valid MDF marker! Please refer to the following reference for a list of valid MDF markers: " + 
				"Coward, David F.; Grimes, Charles E. (2000): \"Making Dictionaries. A guide to lexicography and the Multi-Dictionary Formatter\"." + 
				"SIL International: Waxhaw, North Carolina. 183-185. URL http://downloads.sil.org/legacy/shoebox/MDF_2000.pdf.", Level.ERROR);
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getMDFMap()}.
	 */
	@Test
	public final void testGetValidCustomMarkerMap() {
		Map<String, String> testCustomMarkerMap = new HashMap<>();
		testCustomMarkerMap.put("va", "vaTest");
		testCustomMarkerMap.put("li", "liTest");
		testCustomMarkerMap.put("d", "dTest");
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "va:vaTest, li : liTest , d    :       dTest   ");
		assertEquals(testCustomMarkerMap, getFixture().getCustomMarkerMap());
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties#getMDFMap()}.
	 */
	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public final void testGetInvalidCustomMarkerMap() {
		getFixture().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "nt:ntTest, an : anTest , invalid    ");
		getFixture().getCustomMarkerMap();
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

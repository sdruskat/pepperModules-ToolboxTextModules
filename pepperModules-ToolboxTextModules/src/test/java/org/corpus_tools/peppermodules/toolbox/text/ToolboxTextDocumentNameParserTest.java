package org.corpus_tools.peppermodules.toolbox.text;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.junit.Test;

public class ToolboxTextDocumentNameParserTest {
	
	/**
	 * // TODO Add description
	 * 
	 */
	@Test(expected = PepperModuleException.class)
	public final void testParseIdFailNoFile() {
		ToolboxTextDocumentNameParser.parseId(1L, "", new File(""), false);
	}

	/**
	 * // TODO Add description
	 * 
	 */
	@Test(expected = PepperModuleException.class)
	public final void testParseIdFailIO() {
		File file = new File(this.getClass().getClassLoader().getResource("exporter/saltProject.salt").getFile());
		ToolboxTextDocumentNameParser.parseId(-10L, "", file, false);
	}
	
	/**
	 * // TODO Add description
	 * 
	 */
	@Test
	public final void testParseIdDefaultName() {
		File file = new File(this.getClass().getClassLoader().getResource("importer/no-id-name.txt").getFile());
		String name = ToolboxTextDocumentNameParser.parseId(15L, "", file, false);
		assertThat(name, is("Document at offset 15"));
	}

}

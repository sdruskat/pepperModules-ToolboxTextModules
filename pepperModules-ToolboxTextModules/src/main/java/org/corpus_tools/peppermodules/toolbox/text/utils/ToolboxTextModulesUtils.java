/*******************************************************************************
 * Copyright (c) 2017 Stephan Druskat
 * Exploitation rights belong exclusively to Humboldt-Universität zu Berlin
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
package org.corpus_tools.peppermodules.toolbox.text.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SRelation;

/**
 * // TODO Add description
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 * 
 */
public class ToolboxTextModulesUtils {
	
	/**
	 * A utility method to determine whether a
	 * {@link String} represents a valid `int`.
	 * 
	 * Originally published as [a Stack Overflow answer](http://stackoverflow.com/a/237204/731040)
	 * by user [Jonas Klemming](http://stackoverflow.com/users/26609/jonas-klemming) under
	 * CC-BY-SA-3.0.
	 *
	 * @param str
	 * @return
	 */
	public static boolean isInteger(String str) {
	    if (str == null) {
	        return false;
	    }
	    int length = str.length();
	    if (length == 0) {
	        return false;
	    }
	    int i = 0;
	    if (str.charAt(0) == '-') {
	        if (length == 1) {
	            return false;
	        }
	        i = 1;
	    }
	    for (; i < length; i++) {
	        char c = str.charAt(i);
	        if (c < '0' || c > '9') {
	            return false;
	        }
	    }
	    return true;
	}
	
	/**
	 * Trims any whitespaces pre- and suffixing a
	 * {@link String} and replaces all string-internal
	 * spaces (including linebreaks) with a single 
	 * whitespace.
	 * 
	 * @param string The {@link String} to trim and condense spaces on 
	 * @return A {@link String} not affixed with spaces, and containing only single whitespaces and no line breaks.
	 */
	public static String trimAndCondense(String string) {
		final String[] stringArr = string.trim().split("\\s+");
		return String.join(" ", stringArr);
	}
	
	/**
	 * // TODO Add description
	 * 
	 * @param unorderedSpans
	 * @return
	 */
	public static List<SSpan> sortSpansByTextCoverageOfIncludedToken(Set<SSpan> unorderedSpans) {
		final List<SSpan> sortedSpans = new ArrayList<>(); 
		TreeMap<Integer, SSpan> indexMap = new TreeMap<>();
		for (SSpan span : unorderedSpans) {
			SDocumentGraph graph = span.getGraph();
			List<SToken> sortedTokens = graph.getSortedTokenByText(graph.getOverlappedTokens(span));
			SToken firstToken = sortedTokens.get(0);
			List<STextualRelation> textualRels = new ArrayList<>(); 
			for (SRelation<?, ?> rel : firstToken.getOutRelations()) {
				if (rel instanceof STextualRelation) {
					textualRels.add((STextualRelation) rel);
				}
			}
			assert textualRels.size() == 1;
			indexMap.put(textualRels.get(0).getStart(), span);
		}
		sortedSpans.addAll(indexMap.values());
		return sortedSpans;
		
	}

}

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

import java.util.List;
import java.util.Map;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;

import com.google.common.collect.Range;

/**
 * TODO Description
 *
 * @author Stephan Druskat
 *
 */
public class ToolboxTextMapper extends AbstractToolboxTextMapper {
	
	private final Long headerEndOffset;
	private final Map<Long, List<Long>> refMap;
	private final Range<Long> idRange;

	/**
	 * @param headerEndOffset2
	 * @param refMap
	 * @param idRange
	 */
	public ToolboxTextMapper(Long headerEndOffset2, Map<Long, List<Long>> refMap, Range<Long> idRange) {
		System.err.println(idRange);
		this.idRange = idRange;
		this.refMap = refMap;
		this.headerEndOffset = headerEndOffset2;
	}

	/**
	 * {@inheritDoc PepperMapper#setDocument(SDocument)}
	 * 
	 * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		System.err.println("DOC: " + getDocument().getIdentifier());
		return DOCUMENT_STATUS.COMPLETED;
	}

	/**
	 * {@inheritDoc PepperMapper#setCorpus(SCorpus)}
	 * 
	 * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
	 */
	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		System.err.println("CORPUS: " + getCorpus().getIdentifier());
		return DOCUMENT_STATUS.COMPLETED;
	}

}

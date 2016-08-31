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

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Description
 *
 * @author Stephan Druskat <mail@sdruskat.net>
 *
 */
public class IdBasedToolboxTextMapper extends PepperMapperImpl {
	
	private static final Logger logger = LoggerFactory.getLogger(IdBasedToolboxTextMapper.class);

	private final Long headerEnd;
	private final Long offset;

	/**
	 * @param long1
	 * @param string
	 */
	public IdBasedToolboxTextMapper(Long offset, Long headerEnd) {
		this.offset = offset;
		this.headerEnd = headerEnd;
	}
	
	/**
	 * @param offset
	 */
	public IdBasedToolboxTextMapper(Long offset) {
		this.offset = offset;
		this.headerEnd = null;
	}

	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		// TODO
		return (DOCUMENT_STATUS.COMPLETED);
	}
	
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		// TODO
		return (DOCUMENT_STATUS.COMPLETED);
	}


}

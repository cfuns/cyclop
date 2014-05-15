/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cyclop.service.completion.impl.parser;

import org.cyclop.model.CqlCompletion;
import org.cyclop.model.CqlNotSupported;
import org.cyclop.model.CqlPart;
import org.cyclop.model.CqlQuery;

import javax.annotation.PostConstruct;
import javax.inject.Named;

/** @author Maciej Miklas */
@Named
public abstract class NotSupportedCompletion extends MarkerBasedCompletion {

	private CqlCompletion completion;

	protected NotSupportedCompletion(CqlPart startMarker) {
		super(startMarker);
	}

	@PostConstruct
	public void init() {
		completion = CqlCompletion.Builder.naturalOrder().all(new CqlNotSupported("'" + getNotSupportedText() + "' is" +
				" not supported yet.....")).build();
	}

	@Override
	public final CqlCompletion getCompletion(CqlQuery query) {
		return completion;
	}

	protected abstract String getNotSupportedText();

}

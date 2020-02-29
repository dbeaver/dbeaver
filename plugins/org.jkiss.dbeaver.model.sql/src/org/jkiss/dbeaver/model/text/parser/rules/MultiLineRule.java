/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
 */
package org.jkiss.dbeaver.model.text.parser.rules;


import org.jkiss.dbeaver.model.text.parser.TPToken;

/**
 * A rule for detecting patterns which begin with a given
 * sequence and may end with a given sequence thereby spanning
 * multiple lines.
 */
public class MultiLineRule extends PatternRule {

	public MultiLineRule(String startSequence, String endSequence, TPToken token) {
		this(startSequence, endSequence, token, (char) 0);
	}

	public MultiLineRule(String startSequence, String endSequence, TPToken token, char escapeCharacter) {
		this(startSequence, endSequence, token, escapeCharacter, false);
	}

	public MultiLineRule(String startSequence, String endSequence, TPToken token, char escapeCharacter, boolean breaksOnEOF) {
		super(startSequence, endSequence, token, escapeCharacter, false, breaksOnEOF);
	}
}

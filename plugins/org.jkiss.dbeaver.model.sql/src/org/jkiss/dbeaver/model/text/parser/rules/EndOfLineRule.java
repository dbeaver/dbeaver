/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
 * A specific configuration of a single line rule
 * whereby the pattern begins with a specific sequence but
 * is only ended by a line delimiter.
 */
public class EndOfLineRule extends SingleLineRule {

	/**
	 * Creates a rule for the given starting sequence
	 * which, if detected, will return the specified token.
	 *
	 * @param startSequence the pattern's start sequence
	 * @param token the token to be returned on success
	 */
	public EndOfLineRule(String startSequence, TPToken token) {
		this(startSequence, token, (char) 0);
	}

	/**
	 * Creates a rule for the given starting sequence
	 * which, if detected, will return the specified token.
	 * Any character which follows the given escape character
	 * will be ignored.
	 *
	 * @param startSequence the pattern's start sequence
	 * @param token the token to be returned on success
	 * @param escapeCharacter the escape character
	 */
	public EndOfLineRule(String startSequence, TPToken token, char escapeCharacter) {
		super(startSequence, null, token, escapeCharacter, true);
	}

	/**
	 * Creates a rule for the given starting sequence
	 * which, if detected, will return the specified token.
	 * Any character which follows the given escape character
	 * will be ignored. In addition, an escape character
	 * immediately before an end of line can be set to continue
	 * the line.
	 *
	 * @param startSequence the pattern's start sequence
	 * @param token the token to be returned on success
	 * @param escapeCharacter the escape character
	 * @param escapeContinuesLine indicates whether the specified escape
	 *        character is used for line continuation, so that an end of
	 *        line immediately after the escape character does not
	 *        terminate the line, even if <code>breakOnEOL</code> is true
	 * @since 3.0
	 */
	public EndOfLineRule(String startSequence, TPToken token, char escapeCharacter, boolean escapeContinuesLine) {
		super(startSequence, null, token, escapeCharacter, true, escapeContinuesLine);
	}

	/**
	 * Creates a rule for the given starting sequence
	 * which, if detected, will return the specified token.
	 * Any character which follows the given escape character
	 * will be ignored. In addition, an escape character
	 * immediately before an end of line can be set to continue
	 * the line.
	 * If <code>excludeLineDelimiter</code> flag is set to <code>true</code> and
	 * this rule was terminated by reaching line delimiter, then that line
	 * delimiter will be included into produced token
	 *
	 * @param startSequence the pattern's start sequence
	 * @param token the token to be returned on success
	 * @param escapeCharacter the escape character
	 * @param escapeContinuesLine indicates whether the specified escape
	 *        character is used for line continuation, so that an end of
	 *        line immediately after the escape character does not
	 *        terminate the line, even if <code>breakOnEOL</code> is true
	 * @param excludeLineDelimiter  indicates whether the line delimiter should be included into produced token or not
	 * @since 7.2.5
	 */
	public EndOfLineRule(String startSequence, TPToken token, char escapeCharacter, boolean escapeContinuesLine, boolean excludeLineDelimiter) {
		super(startSequence, null, token, escapeCharacter, true, escapeContinuesLine, excludeLineDelimiter);
	}
}

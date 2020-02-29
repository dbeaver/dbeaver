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

import org.jkiss.dbeaver.model.text.parser.*;

import java.util.HashMap;
import java.util.Map;


/**
 * Word rule
 */
public class WordRule implements TPRule {

	protected static final int UNDEFINED	= -1;

	private TPWordDetector fDetector;
	private TPToken fDefaultToken;
	private int fColumn= UNDEFINED;
	private Map<String, TPToken> fWords= new HashMap<>();
	private StringBuilder fBuffer= new StringBuilder();
	private boolean fIgnoreCase;

	public WordRule(TPWordDetector detector) {
		this(detector, TPTokenAbstract.UNDEFINED, false);
	}

	public WordRule(TPWordDetector detector, TPToken defaultToken) {
		this(detector, defaultToken, false);
	}

	public WordRule(TPWordDetector detector, TPToken defaultToken, boolean ignoreCase) {
		fDetector= detector;
		fDefaultToken= defaultToken;
		fIgnoreCase= ignoreCase;
	}

	public void addWord(String word, TPToken token) {
		// If case-insensitive, convert to lower case before adding to the map
		if (fIgnoreCase)
			word= word.toLowerCase();
		fWords.put(word, token);
	}

	public void setColumnConstraint(int column) {
		if (column < 0)
			column= UNDEFINED;
		fColumn= column;
	}

	@Override
	public TPToken evaluate(TPCharacterScanner scanner) {
		int c= scanner.read();
		if (c != TPCharacterScanner.EOF && fDetector.isWordStart((char) c)) {
			if (fColumn == UNDEFINED || (fColumn == scanner.getColumn() - 1)) {

				fBuffer.setLength(0);
				do {
					fBuffer.append((char) c);
					c= scanner.read();
				} while (c != TPCharacterScanner.EOF && fDetector.isWordPart((char) c));
				scanner.unread();

				String buffer= fBuffer.toString();
				// If case-insensitive, convert to lower case before accessing the map
				if (fIgnoreCase)
					buffer= buffer.toLowerCase();

				TPToken token= fWords.get(buffer);

				if (token != null)
					return token;

				if (fDefaultToken.isUndefined())
					unreadBuffer(scanner);

				return fDefaultToken;
			}
		}

		scanner.unread();
		return TPTokenAbstract.UNDEFINED;
	}

	/**
	 * Returns the characters in the buffer to the scanner.
	 *
	 * @param scanner the scanner to be used
	 */
	private void unreadBuffer(TPCharacterScanner scanner) {
		for (int i= fBuffer.length() - 1; i >= 0; i--)
			scanner.unread();
	}

}

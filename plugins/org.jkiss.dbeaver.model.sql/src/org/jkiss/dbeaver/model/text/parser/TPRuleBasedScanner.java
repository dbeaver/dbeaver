/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.text.parser;


import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLDelimiterRule;


/**
 * Rule based text scanner
 */
public class TPRuleBasedScanner implements TPCharacterScanner, TPTokenScanner, TPEvalScanner {

	/** The list of rules of this scanner */
	private TPRule[] fRules;
	/** The token to be returned by default if no rule fires */
	private TPToken fDefaultReturnToken;
	/** The document to be scanned */
	private IDocument fDocument;
	/** The cached legal line delimiters of the document */
	private char[][] fDelimiters;
	/** The offset of the next character to be read */
	private int fOffset;
	/** The end offset of the range to be scanned */
	private int fRangeEnd;
	/** The offset of the last read token */
	private int fTokenOffset;
	/** The cached column of the current scanner position */
	private int fColumn;
	/** Internal setting for the un-initialized column cache. */
	private static final int UNDEFINED= -1;

	private boolean evalMode;

	/**
	 * Creates a new rule based scanner which does not have any rule.
	 */
	public TPRuleBasedScanner() {
	}

	/**
	 * Configures the scanner with the given sequence of rules.
	 *
	 * @param rules the sequence of rules controlling this scanner
	 */
	public void setRules(TPRule[] rules) {
		if (rules != null) {
			fRules= new TPRule[rules.length];
			System.arraycopy(rules, 0, fRules, 0, rules.length);
		} else
			fRules= null;
	}

	/**
	 * Configures the scanner's default return token. This is the token
	 * which is returned when none of the rules fired and EOF has not been
	 * reached.
	 *
	 * @param defaultReturnToken the default return token
	 * @since 2.0
	 */
	public void setDefaultReturnToken(TPToken defaultReturnToken) {
		Assert.isNotNull(defaultReturnToken.getData());
		fDefaultReturnToken= defaultReturnToken;
	}

	@Override
	public void setRange(final IDocument document, int offset, int length) {
		Assert.isLegal(document != null);
		final int documentLength = document.getLength();

		// Sometimes we have length longer than document length
		while (offset + length >= documentLength) {
			length--;
		}
		checkRange(offset, length, documentLength);

		fDocument= document;
		fOffset= offset;
		fColumn= UNDEFINED;
		fRangeEnd= offset + length;

		String[] delimiters= fDocument.getLegalLineDelimiters();
		fDelimiters= new char[delimiters.length][];
		for (int i= 0; i < delimiters.length; i++)
			fDelimiters[i]= delimiters[i].toCharArray();

		if (fDefaultReturnToken == null)
			fDefaultReturnToken= TPTokenAbstract.UNDEFINED;
	}

	/**
	 * Checks that the given range is valid.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=69292
	 *
	 * @param offset the offset of the document range to scan
	 * @param length the length of the document range to scan
	 * @param documentLength the document's length
	 * @since 3.3
	 */
	private void checkRange(int offset, int length, int documentLength) {
		Assert.isLegal(offset > -1);
		Assert.isLegal(length > -1);
		Assert.isLegal(offset + length <= documentLength);
	}

	@Override
	public int getTokenOffset() {
		return fTokenOffset;
	}

	@Override
	public int getTokenLength() {
		if (fOffset < fRangeEnd)
			return fOffset - getTokenOffset();
		return fRangeEnd - getTokenOffset();
	}


	@Override
	public int getColumn() {
		if (fColumn == UNDEFINED) {
			try {
				int line= fDocument.getLineOfOffset(fOffset);
				int start= fDocument.getLineOffset(line);

				fColumn= fOffset - start;

			} catch (BadLocationException ex) {
			}
		}
		return fColumn;
	}

	@Override
	public char[][] getLegalLineDelimiters() {
		return fDelimiters;
	}

	@Override
	public TPToken nextToken() {

		fTokenOffset= fOffset;
		fColumn= UNDEFINED;

		if (fRules != null) {
			for (TPRule fRule : fRules) {
				TPToken token= (fRule.evaluate(this));
				if (!token.isUndefined())
					return token;
			}
		}

		if (read() == EOF)
			return TPTokenAbstract.EOF;
		return fDefaultReturnToken;
	}

	@Override
	public int read() {

		try {

			if (fOffset < fRangeEnd) {
				try {
					return fDocument.getChar(fOffset);
				} catch (BadLocationException e) {
				}
			}

			return EOF;

		} finally {
			++ fOffset;
			fColumn= UNDEFINED;
		}
	}

	@Override
	public void unread() {
		--fOffset;
		fColumn= UNDEFINED;
	}

	//////////////////////////////////////////////////////
	// Eval

	@Override
	public boolean isEvalMode() {
		return evalMode;
	}

	public void startEval() {
		this.evalMode = true;
	}

	public void endEval() {
		this.evalMode = false;
		if (fRules != null) {
			for (TPRule rule : fRules) {
				if (rule instanceof SQLDelimiterRule) {
					((SQLDelimiterRule)rule).changeDelimiter(null);
				}
			}
		}
	}

}



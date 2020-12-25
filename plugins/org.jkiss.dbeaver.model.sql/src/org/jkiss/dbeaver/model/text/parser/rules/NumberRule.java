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

import org.eclipse.core.runtime.Assert;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;


/**
 * An implementation of <code>IRule</code> detecting a numerical value.
 */
public class NumberRule implements TPRule {

	/** Internal setting for the un-initialized column constraint */
	protected static final int UNDEFINED= -1;
	/** The token to be returned when this rule is successful */
	protected TPToken fToken;
	/** The column constraint */
	protected int fColumn= UNDEFINED;

	/**
	 * Creates a rule which will return the specified
	 * token when a numerical sequence is detected.
	 *
	 * @param token the token to be returned
	 */
	public NumberRule(TPToken token) {
		Assert.isNotNull(token);
		fToken= token;
	}

	/**
	 * Sets a column constraint for this rule. If set, the rule's token
	 * will only be returned if the pattern is detected starting at the
	 * specified column. If the column is smaller then 0, the column
	 * constraint is considered removed.
	 *
	 * @param column the column in which the pattern starts
	 */
	public void setColumnConstraint(int column) {
		if (column < 0)
			column= UNDEFINED;
		fColumn= column;
	}

	@Override
	public TPToken evaluate(TPCharacterScanner scanner) {
		int c= scanner.read();
		if (Character.isDigit((char)c)) {
			if (fColumn == UNDEFINED || (fColumn == scanner.getColumn() - 1)) {
				int charNum = 0;
				do {
					c = scanner.read();
					charNum++;
				} while (Character.isDigit((char) c) || (charNum > 0 && Character.toLowerCase(c) == 'e'));
				scanner.unread();
				return fToken;
			}
		}

		scanner.unread();
		return TPTokenAbstract.UNDEFINED;
	}
}

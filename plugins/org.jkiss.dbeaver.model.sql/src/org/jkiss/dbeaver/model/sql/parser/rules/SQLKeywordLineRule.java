/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.parser.rules;

import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;
import org.jkiss.dbeaver.model.text.parser.rules.EndOfLineRule;

public class SQLKeywordLineRule extends EndOfLineRule {

    public SQLKeywordLineRule(String keywordString, TPToken token)  {
        super(keywordString,  token);
    }

    /**
     * Same as underlying, but case-insensitive for start sequence
     */
    @Override
    protected TPToken doEvaluate(TPCharacterScanner scanner, boolean resume) {
        if (resume) {
            if (endSequenceDetected(scanner))
                return fToken;
        } else {
            int c = scanner.read();
            if (Character.toLowerCase(c) == Character.toLowerCase(fStartSequence[0])) {
                if (caselessSequenceDetected(scanner, fStartSequence, false)) {
                    if (endSequenceDetected(scanner))
                        return fToken;
                }
            }
        }

        scanner.unread();
        return TPTokenAbstract.UNDEFINED;
    }

    /**
     * Same as underlying, but case-insensitive for start sequence
     */
    @Override
    public TPToken evaluate(TPCharacterScanner scanner, boolean resume) {
        if (fColumn == UNDEFINED)
            return doEvaluate(scanner, resume);

        int c = scanner.read();
        scanner.unread();
        if (Character.toLowerCase(c) == Character.toLowerCase(fStartSequence[0]))
            return (fColumn == scanner.getColumn() ? doEvaluate(scanner, resume) : TPTokenAbstract.UNDEFINED);
        return TPTokenAbstract.UNDEFINED;
    }

    /**
     * Same as underlying {@link #sequenceDetected(TPCharacterScanner, char[], boolean)}, but case-insensitive
     */
    protected boolean caselessSequenceDetected(TPCharacterScanner scanner, char[] sequence, boolean eofAllowed) {
        for (int i = 1; i < sequence.length; i++) {
            int c = scanner.read();
            if (Character.toLowerCase(c) != Character.toLowerCase(sequence[i])) {
                // Non-matching character detected, rewind the scanner back to the start.
                // Do not unread the first character.
                scanner.unread();
                for (int j = i - 1; j > 0; j--)
                    scanner.unread();
                return false;
            }
        }

        int nc = scanner.read();
        if (Character.isLetterOrDigit(nc)) {
            for (int j = sequence.length; j > 0; j--)
                scanner.unread();
            return false;
        } else {
            scanner.unread();
            return true;
        }
    }
}

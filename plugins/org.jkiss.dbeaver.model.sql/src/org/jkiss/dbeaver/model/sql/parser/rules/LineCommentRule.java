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
package org.jkiss.dbeaver.model.sql.parser.rules;

import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;
import org.jkiss.dbeaver.model.text.parser.rules.EndOfLineRule;

/**
 * The same as end-of-line rule but matches word in case-insensitive fashion +
 * needs whitespace after last letter or digit
 */
public class LineCommentRule extends EndOfLineRule
{
    public LineCommentRule(String startSequence, TPToken token) {
        super(startSequence, token, (char) 0);
    }

    public LineCommentRule(String startSequence, TPToken token, char escapeCharacter) {
        super(startSequence, token, escapeCharacter);
    }

    public LineCommentRule(String startSequence, TPToken token, char escapeCharacter, boolean escapeContinuesLine) {
        super(startSequence, token, escapeCharacter, escapeContinuesLine);
    }

    protected TPToken doEvaluate(TPCharacterScanner scanner, boolean resume) {

        if (resume) {

            if (endSequenceDetected(scanner))
                return fToken;

        } else {
/*
            // Check we are at the line beginning
            for (;;) {
                scanner.unread();
                int c = scanner.read();
            }

*/
            int c= scanner.read();
            if (Character.toUpperCase(c) == Character.toUpperCase(fStartSequence[0])) {
                if (sequenceDetected(scanner, fStartSequence, false)) {
                    if (endSequenceDetected(scanner))
                        return fToken;
                }
            }
        }

        scanner.unread();
        return TPTokenAbstract.UNDEFINED;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner, boolean resume) {
        if (fColumn == UNDEFINED)
            return doEvaluate(scanner, resume);

        int c= scanner.read();
        scanner.unread();
        if (Character.toUpperCase(c) == Character.toUpperCase(fStartSequence[0]))
            return (fColumn == scanner.getColumn() ? doEvaluate(scanner, resume) : TPTokenAbstract.UNDEFINED);
        return TPTokenAbstract.UNDEFINED;
    }

    @Override
    protected boolean sequenceDetected(TPCharacterScanner scanner, char[] sequence, boolean eofAllowed) {
        for (int i= 1; i < sequence.length; i++) {
            int c= scanner.read();
            if (c == TPCharacterScanner.EOF && eofAllowed) {
                return true;
            } else if (Character.toUpperCase(c) != Character.toUpperCase(sequence[i])) {
                // Non-matching character detected, rewind the scanner back to the start.
                // Do not unread the first character.
                scanner.unread();
                for (int j= i-1; j > 0; j--)
                    scanner.unread();
                return false;
            }
        }

        if (Character.isLetterOrDigit(sequence[sequence.length - 1])) {
            // Check for trailing whitespace
            int lastChar = scanner.read();
            scanner.unread();
            if (lastChar != TPCharacterScanner.EOF) {
                if (!Character.isWhitespace((char) lastChar)) {
                    return false;
                }
            }
        }

        return true;
    }

}
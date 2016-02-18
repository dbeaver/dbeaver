/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.rules.*;

/**
 * The same as end-of-line rule but matches word in case-insensitive fashion +
 * needs whitespace after last letter or digit
 */
public class LineCommentRule extends EndOfLineRule
{
    public LineCommentRule(String startSequence, IToken token) {
        super(startSequence, token, (char) 0);
    }

    public LineCommentRule(String startSequence, IToken token, char escapeCharacter) {
        super(startSequence, token, escapeCharacter);
    }

    public LineCommentRule(String startSequence, IToken token, char escapeCharacter, boolean escapeContinuesLine) {
        super(startSequence, token, escapeCharacter, escapeContinuesLine);
    }

    protected IToken doEvaluate(ICharacterScanner scanner, boolean resume) {

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
        return Token.UNDEFINED;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner, boolean resume) {
        if (fColumn == UNDEFINED)
            return doEvaluate(scanner, resume);

        int c= scanner.read();
        scanner.unread();
        if (Character.toUpperCase(c) == Character.toUpperCase(fStartSequence[0]))
            return (fColumn == scanner.getColumn() ? doEvaluate(scanner, resume) : Token.UNDEFINED);
        return Token.UNDEFINED;
    }

    @Override
    protected boolean sequenceDetected(ICharacterScanner scanner, char[] sequence, boolean eofAllowed) {
        for (int i= 1; i < sequence.length; i++) {
            int c= scanner.read();
            if (c == ICharacterScanner.EOF && eofAllowed) {
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
            if (lastChar != ICharacterScanner.EOF) {
                if (!Character.isWhitespace((char) lastChar)) {
                    return false;
                }
            }
        }

        return true;
    }

}
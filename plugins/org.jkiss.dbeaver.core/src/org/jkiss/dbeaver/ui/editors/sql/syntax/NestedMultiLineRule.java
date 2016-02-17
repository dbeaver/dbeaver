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

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.Token;

public class NestedMultiLineRule extends MultiLineRule
{
    protected int _commentNestingDepth = 0;

    public NestedMultiLineRule(String startSequence, String endSequence, IToken token)
    {
        super(startSequence, endSequence, token);
    }

    public NestedMultiLineRule(String startSequence, String endSequence, IToken token, char escapeCharacter)
    {
        super(startSequence, endSequence, token, escapeCharacter);
    }

    public NestedMultiLineRule(String startSequence, String endSequence, IToken token, char escapeCharacter,
        boolean breaksOnEOF)
    {
        super(startSequence, endSequence, token, escapeCharacter, breaksOnEOF);
    }

    @Override
    protected boolean endSequenceDetected(ICharacterScanner scanner)
    {
        int c;
        //char[][] delimiters = scanner.getLegalLineDelimiters();
        //boolean previousWasEscapeCharacter = false;
        while ((c = scanner.read()) != ICharacterScanner.EOF)
        {
            if (c == fEscapeCharacter)
            {
                // Skip the escaped character.
                scanner.read();
            }
            else if (fEndSequence.length > 0 && c == fEndSequence[0])
            {
                // Check if the specified end sequence has been found.
                if (sequenceDetected(scanner, fEndSequence, true))
                {
                    _commentNestingDepth--;
                }
                if (_commentNestingDepth <= 0)
                {
                    return true;
                }
            }
            else if (fStartSequence.length > 0 && c == fStartSequence[0])
            {
                // Check if the nested start sequence has been found.
                if (sequenceDetected(scanner, fStartSequence, false))
                {
                    _commentNestingDepth++;
                }
            }
            //previousWasEscapeCharacter = (c == fEscapeCharacter);
        }
        if (fBreaksOnEOF)
        {
            return true;
        }
        scanner.unread();
        return false;
    }

    @Override
    protected IToken doEvaluate(ICharacterScanner scanner, boolean resume)
    {
        if (resume)
        {
            _commentNestingDepth = 0;
            if (scanner instanceof SQLPartitionScanner)
            {
                String scanned = ((SQLPartitionScanner) scanner).getScannedPartitionString();
                if (scanned != null && scanned.length() > 0)
                {
                    String startSequence = new String(fStartSequence);
                    int index = 0;
                    while ((index = scanned.indexOf(startSequence, index)) >= 0)
                    {
                        index++;
                        _commentNestingDepth++;
                    }
                    //must be aware of the closing sequences
                    String endSequence = new String(fEndSequence);
                    index = 0;
                    while ((index = scanned.indexOf(endSequence, index)) >= 0)
                    {
                        index++;
                        _commentNestingDepth--;
                    }
                }
            }
            if (endSequenceDetected(scanner))
            {
                return fToken;
            }

        }
        else
        {

            int c = scanner.read();
            if (c == fStartSequence[0])
            {
                if (sequenceDetected(scanner, fStartSequence, false))
                {
                    _commentNestingDepth = 1;
                    if (endSequenceDetected(scanner))
                    {
                        return fToken;
                    }
                }
            }
        }

        scanner.unread();
        return Token.UNDEFINED;

    }
}
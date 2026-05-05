/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;
import org.jkiss.dbeaver.Log;

/**
 * Process double clicks in the SQL content.
 */
public class SQLDoubleClickStrategy implements ITextDoubleClickStrategy {
    private static final Log log = Log.getLog(SQLDoubleClickStrategy.class);

    protected ITextViewer fText;
    protected int curPos;
    protected int startPos;
    protected int endPos;

    protected static char[] fgBrackets = { '(', ')', '[', ']', '\'', '\'', '"', '"' };

    /**
     * Constructs an instance of this class.  This is the default constructor.
     */
    public SQLDoubleClickStrategy() {
        super();
    }

    /**
     * Handles a double click action by selecting the current word.
     *
     * @see org.eclipse.jface.text.ITextDoubleClickStrategy#doubleClicked(ITextViewer)
     */
    @Override
    public void doubleClicked(ITextViewer viewer) {
        // Get the viewer we are dealing with.
        fText = viewer;

        // Get the double click location in the document.
        curPos = viewer.getSelectedRange().x;

        if (curPos < 0 || curPos >= fText.getDocument().getLength()) {
            return;
        }

        if (!selectBracketBlock()) {
            selectWord();
        }
    }

    /**
     * Attempts to find and match opening or closing brackets just ahead of the
     * double click location.  Sets fStartPos and fEndPos to the bracket locations
     * if found.
     *
     * @return true if brackets found and matched, otherwise false
     */
     protected boolean matchBracketsAt() {
        if (curPos == 0) {
            return false;
        }
        char prevChar, nextChar;
        int i;
        int bracketIndex1 = fgBrackets.length;
        int bracketIndex2 = fgBrackets.length;

        startPos = -1;
        endPos = -1;

        // Get the chars preceding and following the start position.
        try {
            IDocument doc = fText.getDocument();
            prevChar = doc.getChar(curPos - 1);
            nextChar = doc.getChar(curPos);

            // Is the char either an open or close bracket?
            for (i= 0; i < fgBrackets.length; i = i + 2) {
                if (prevChar == fgBrackets[i]) {
                    startPos = curPos - 1;
                    bracketIndex1 = i;
                }
            }
            for (i= 1; i < fgBrackets.length; i = i + 2) {
                if (nextChar == fgBrackets[i]) {
                    endPos = curPos;
                    bracketIndex2 = i;
                }
            }

            // If we found an open bracket, find the matching closing bracket.
            if (startPos > -1 && bracketIndex1 < bracketIndex2) {
                endPos = searchForClosingBracket(startPos, prevChar, fgBrackets[bracketIndex1 + 1], doc );
                if (endPos > -1)
                    return true;

                startPos = -1;
            }
            // Otherwise if we found a closing bracket, find the matching open bracket.
            else if (endPos > -1) {
                startPos = searchForOpenBracket(endPos, fgBrackets[bracketIndex2 - 1], nextChar, doc );
                if (startPos > -1)
                    return true;

                endPos = -1;
            }

        } catch (BadLocationException x) {
            log.debug(x);
        }

        return false;
    }

    /**
     * Attempts to determine and set the start (fStartPos) and end (fEndPos) of the word
     * that was double-clicked.
     *
     * @return true if the bounds of the word were successfully determined, otherwise false.
     */
    protected boolean matchWord() {
        IDocument doc = fText.getDocument();

        try {
            int pos = curPos;
            char c;

            // Scan backwards for the start of the word.
            while (pos >= 0) {
                c = doc.getChar(pos);
                // Yes we know this isn't Java code we are parsing but the
                // Java identifier rule is close enough for now.
                if (!Character.isJavaIdentifierPart(c))
                    break;
                --pos;
            }
            startPos = pos;

            // Scan forward for the end of the word.
            pos = curPos;
            int length = doc.getLength();
            while (pos < length) {
                c = doc.getChar(pos);
                if (!Character.isJavaIdentifierPart(c))
                    break;
                ++pos;
            }
            endPos = pos;

            return true;
        } catch (BadLocationException x) {
            log.debug(x);
        }

        return false;
    }

    /**
     * Returns the position of the closing bracket after startPosition.
     *
     * @param startPosition the starting position for the search
     * @param openBracket the open bracket character
     * @param closeBracket the close bracket character
     * @param document the document being searched
     * @return the location of the closing bracket
     */
     protected int searchForClosingBracket(int startPosition, char openBracket, char closeBracket, IDocument document ) throws BadLocationException {
        int stack = 1;
        int closePosition = startPosition + 1;
        int length = document.getLength();
        char nextChar;

        // Scan forward for the closing bracket.  Ignore "nested" bracket pairs.
        while (closePosition < length && stack > 0) {
            nextChar = document.getChar( closePosition );
            if (nextChar == openBracket && nextChar != closeBracket)
                stack++;
            else if (nextChar == closeBracket)
                stack--;
            closePosition++;
        }

        if (stack == 0)
            return closePosition - 1;

        return -1;
    }

    /**
     * Returns the position of the open bracket before startPosition.
     *
     * @param startPosition the starting position for the search
     * @param openBracket the open bracket character
     * @param closeBracket the close bracket character
     * @param document the document being searched
     * @return the location of the open bracket
     */
     protected int searchForOpenBracket( int startPosition, char openBracket, char closeBracket, IDocument document ) throws BadLocationException {
        int stack = 1;
        int openPos = startPosition - 1;
        char nextChar;

        // Scan backward for the opening bracket.  Ignore "nested" bracket pairs.
        while (openPos >= 0 && stack > 0) {
            nextChar= document.getChar(openPos);
            if (nextChar == closeBracket && nextChar != openBracket)
                stack++;
            else if (nextChar == openBracket)
                stack--;
            openPos--;
        }

        if (stack == 0)
            return openPos + 1;

        return -1;
    }

    /**
     * Select the area between the selected bracket and the closing bracket. Return
     * true if successful.
     *
     * @return <code>true</code> when selection is OK, <code>false</code> when not
     */
     protected boolean selectBracketBlock() {
        if (matchBracketsAt()) {
            if (startPos == endPos)
                fText.setSelectedRange(startPos, 0);
            else
                fText.setSelectedRange(startPos + 1, endPos - startPos - 1);

            return true;
        }
        return false;
    }

    /**
     * Selects the word at the current selection location.
     */
    protected void selectWord() {
        if (matchWord()) {
            if (startPos == endPos)
                fText.setSelectedRange(startPos, 0);
            else
                fText.setSelectedRange(startPos + 1, endPos - startPos - 1);
        }
    }

}

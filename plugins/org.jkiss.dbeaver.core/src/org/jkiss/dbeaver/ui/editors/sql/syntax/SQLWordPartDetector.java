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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Used to scan and detect for SQL keywords.
 */
public class SQLWordPartDetector extends SQLIdentifierDetector
{
    private String prevKeyWord = "";
    private String prevDelimiter = null;
    private List<String> prevWords = null;
    private String wordPart;
    private String fullWord;
    private int cursorOffset;
    private int startOffset;
    private int endOffset;
    private int delimiterOffset;

    /**
     * Method SQLWordPartDetector.
     *
     * @param document text document
     * @param syntaxManager syntax manager
     * @param documentOffset into the SQL document
     */
    public SQLWordPartDetector(IDocument document, SQLSyntaxManager syntaxManager, int documentOffset)
    {
        super(syntaxManager.getStructSeparator(), syntaxManager.getQuoteSymbol());
        cursorOffset = documentOffset;
        startOffset = documentOffset - 1;
        endOffset = documentOffset;
        int topIndex = 0, documentLength = document.getLength();
        try {
            while (startOffset >= topIndex && isWordPart(document.getChar(startOffset))) {
                startOffset--;
            }
            while (endOffset < documentLength && isWordPart(document.getChar(endOffset))) {
                endOffset++;
            }

            int prevOffset = startOffset;
            //we've been one step too far : increase the offset
            startOffset++;
            wordPart = document.get(startOffset, documentOffset - startOffset);
            fullWord = document.get(startOffset, endOffset - startOffset);

            // Get previous keyword
            while (prevOffset >= topIndex) {
                StringBuilder prevPiece = new StringBuilder();
                while (prevOffset >= topIndex) {
                    char ch = document.getChar(prevOffset);
                    if (isWordPart(ch)) {
                        break;
                    } else if (!Character.isWhitespace(ch)) {
                        delimiterOffset = prevOffset;
                    }
                    prevPiece.append(ch);
                    prevOffset--;
                }
                if (prevDelimiter == null) {
                    //startOffset - prevPiece.length();
                    prevDelimiter = prevPiece.toString().trim();
                }
                for (String delim : syntaxManager.getStatementDelimiters()) {
                    if (prevPiece.indexOf(delim) != -1) {
                        // Statement delimiter found - do not process to previous keyword
                        return;
                    }
                }
                int prevStartOffset = prevOffset + 1;
                while (prevOffset >= topIndex) {
                    char ch = document.getChar(prevOffset);
                    if (isWordPart(ch)) {
                        prevOffset--;
                    } else {
                        prevOffset++;
                        break;
                    }
                }
                if (prevOffset < topIndex) {
                    prevOffset = topIndex;
                }

                String prevWord = document.get(prevOffset, prevStartOffset - prevOffset);
                SQLDialect dialect = syntaxManager.getDialect();
                if (dialect.isEntityQueryWord(prevWord) || dialect.isAttributeQueryWord(prevWord)) {
                    this.prevKeyWord = prevWord.toUpperCase(Locale.ENGLISH);
                    break;
                }
                if (prevWords == null) {
                    prevWords = new ArrayList<>();
                }
                prevWords.add(prevWord);
                prevOffset--;
            }
        } catch (BadLocationException e) {
            // do nothing
        }
    }

    /**
     * Method getWordPart.
     *
     * @return String
     */
    public String getWordPart()
    {
        return wordPart;
    }

    public String getFullWord()
    {
        return fullWord;
    }

    public String getPrevDelimiter()
    {
        return prevDelimiter;
    }

    public List<String> getPrevWords()
    {
        return prevWords;
    }

    public int getCursorOffset()
    {
        return cursorOffset;
    }

    public int getStartOffset()
    {
        return startOffset;
    }

    public int getEndOffset()
    {
        return endOffset;
    }

    public int getLength()
    {
        return endOffset - startOffset;
    }

    /**
     * Previous valuable entity or attribute manipulation keyword.
     * All functions, aggregate operators and other keywords are ignored.
     */
    public String getPrevKeyWord()
    {
        return prevKeyWord;
    }

    public List<String> splitWordPart()
    {
        return super.splitIdentifier(wordPart);
    }

    public void moveToDelimiter() {
        int shift = startOffset - delimiterOffset;
        startOffset -= shift;
    }
}
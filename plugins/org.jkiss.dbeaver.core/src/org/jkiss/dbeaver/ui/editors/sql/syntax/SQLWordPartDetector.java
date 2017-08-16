/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
        super(syntaxManager.getStructSeparator(), syntaxManager.getQuoteStrings());
        cursorOffset = documentOffset;
        startOffset = documentOffset - 1;
        endOffset = documentOffset;
        int topIndex = 0, documentLength = document.getLength();
        try {
            boolean inQuote = false;
            while (startOffset >= topIndex) {
                char c = document.getChar(startOffset);
                if (inQuote) {
                    startOffset--;
                    // Opening quote
                    if (isQuote(c)) {
                        break;
                    }
                } else if (isQuote(c)) {
                    startOffset--;
                    inQuote = true;
                } else if (isWordPart(c)) {
                    startOffset--;
                } else {
                    break;
                }
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

    public String[] splitWordPart()
    {
        return super.splitIdentifier(wordPart);
    }

    public void moveToDelimiter() {
        int shift = startOffset - delimiterOffset;
        startOffset -= shift;
    }
}
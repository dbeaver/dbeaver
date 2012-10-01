/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.jkiss.dbeaver.model.DBPKeywordType;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to scan and detect for SQL keywords
 */
public class SQLWordPartDetector extends SQLIdentifierDetector
{
    String prevKeyWord = "";
    String prevDelimiter = null;
    List<String> prevWords = null;
    String wordPart = "";
    int docOffset;
    int endOffset;

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
        docOffset = documentOffset - 1;
        endOffset = documentOffset;
        int topIndex = 0, documentLength = document.getLength();
        try {
            while (docOffset >= topIndex && isWordPart(document.getChar(docOffset))) {
                docOffset--;
            }
            while (endOffset < documentLength && isWordPart(document.getChar(endOffset))) {
                endOffset++;
            }

            int prevOffset = docOffset;
            //we've been one step too far : increase the offset
            docOffset++;
            wordPart = document.get(docOffset, documentOffset - docOffset);

            // Get previous keyword
            while (prevOffset >= topIndex) {
                StringBuilder prevPiece = new StringBuilder();
                while (prevOffset >= topIndex) {
                    char ch = document.getChar(prevOffset);
                    if (isWordPart(ch)) {
                        break;
                    }
                    prevPiece.append(ch);
                    prevOffset--;
                }
                if (prevDelimiter == null) {
                    prevDelimiter = prevPiece.toString();
                }
                if (prevPiece.indexOf(syntaxManager.getStatementDelimiter()) != -1) {
                    // Statement delimiter found - do not process to previous keyword
                    return;
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
                if (syntaxManager.getKeywordManager().getKeywordType(prevWord) == DBPKeywordType.KEYWORD) {
                    this.prevKeyWord = prevWord.toUpperCase();
                    break;
                }
                if (prevWords == null) {
                    prevWords = new ArrayList<String>();
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

    public String getPrevDelimiter()
    {
        return prevDelimiter;
    }

    public List<String> getPrevWords()
    {
        return prevWords;
    }

    public int getOffset()
    {
        return docOffset;
    }

    public int getLength()
    {
        return endOffset - docOffset;
    }

    public String getPrevKeyWord()
    {
        return prevKeyWord;
    }

    public List<String> splitWordPart()
    {
        return super.splitIdentifier(wordPart);
    }

}
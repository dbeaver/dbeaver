/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

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

    /**
     * Method SQLWordPartDetector.
     *
     * @param document text document
     * @param syntaxManager syntax manager
     * @param documentOffset into the SQL document
     */
    public SQLWordPartDetector(IDocument document, SQLSyntaxManager syntaxManager, int documentOffset)
    {
        super(syntaxManager.getCatalogSeparator());
        docOffset = documentOffset - 1;
        int topIndex = 0;//viewer.getTopIndexStartOffset();
        try {
            while (docOffset >= topIndex && isWordPart(document.getChar(docOffset))) {
                docOffset--;
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
                if (syntaxManager.getKeywordType(prevWord.toUpperCase()) == SQLSyntaxManager.KeywordType.KEYWORD) {
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

    public String getPrevKeyWord()
    {
        return prevKeyWord;
    }

    public List<String> splitWordPart()
    {
        return super.splitIdentifier(wordPart);
    }

}
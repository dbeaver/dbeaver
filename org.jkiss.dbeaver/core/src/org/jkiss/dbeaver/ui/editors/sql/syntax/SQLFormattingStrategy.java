/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;

import java.util.StringTokenizer;

/**
 * The formatting strategy that transforms SQL keywords to upper case
 */
public class SQLFormattingStrategy extends ContextBasedFormattingStrategy
{

    private SQLSyntaxManager sqlSyntax;

    /**
   * According to profileName to determine which the database syntax keywords highlighted.
   *
   * @param syntax syntax manager
   */
    public SQLFormattingStrategy(SQLSyntaxManager syntax)
    {
        sqlSyntax = syntax;
    }

    /**
   * @see org.eclipse.jface.text.formatter.IFormattingStrategy#formatterStarts(String)
   */
    public void formatterStarts(String initialIndentation)
    {
    }

    /**
   * @see org.eclipse.jface.text.formatter.IFormattingStrategy#format(String, boolean, String, int[])
   */
    public String format(String content, boolean isLineStart, String indentation, int[] positions)
    {
    	if (sqlSyntax == null)
    	{
    		return allToUpper(content);
    	}
        return keyWordsToUpper(content);
    }

    private String allToUpper( String content ) {
        return content.toUpperCase();
    }

    /**
   * Method keyWordsToUpper.
   *
   * @param content
   * @return String
   */
    private String keyWordsToUpper(String content)
    {
        StringTokenizer st = new StringTokenizer(content, " \n", true);
        StringBuilder newContent = new StringBuilder();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            SQLSyntaxManager.KeywordType type = sqlSyntax.getKeywordType(token.toUpperCase());
            if (type == SQLSyntaxManager.KeywordType.KEYWORD) {
                token = token.toUpperCase();
            }
            newContent.append(token);
        }
        return newContent.toString();
    }

    public void formatterStops()
    {
    }

}
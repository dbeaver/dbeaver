/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.ui.editors.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.ui.editors.sql.format.tokenized.SQLTokenizedFormatter;

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
    @Override
    public void formatterStarts(String initialIndentation)
    {
    }

    /**
   * @see org.eclipse.jface.text.formatter.IFormattingStrategy#format(String, boolean, String, int[])
   */
    @Override
    public String format(String content, boolean isLineStart, String indentation, int[] positions)
    {
        SQLFormatterConfiguration configuration = new SQLFormatterConfiguration(sqlSyntax);
        configuration.setKeywordCase(SQLFormatterConfiguration.KEYWORD_UPPER_CASE);
        //configuration.setIndentString(indentation);
        return new SQLTokenizedFormatter().format(content, configuration);
        //return new SQLSimpleFormatter().format(content);
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
            DBPKeywordType type = sqlSyntax.getDialect().getKeywordType(token);
            if (type == DBPKeywordType.KEYWORD) {
                token = token.toUpperCase();
            }
            newContent.append(token);
        }
        return newContent.toString();
    }

    @Override
    public void formatterStops()
    {
    }

}
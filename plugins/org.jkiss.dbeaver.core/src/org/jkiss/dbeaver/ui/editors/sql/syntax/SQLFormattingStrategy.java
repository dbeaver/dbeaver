/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.ui.editors.sql.format.SQLFormatter;
import org.jkiss.dbeaver.ui.editors.sql.format.SQLFormatterConfiguration;

import java.util.StringTokenizer;

/**
 * The formatting strategy that transforms SQL keywords to upper case
 */
public class SQLFormattingStrategy extends ContextBasedFormattingStrategy
{
    static final Log log = LogFactory.getLog(SQLFormattingStrategy.class);

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
        SQLFormatterConfiguration configuration = new SQLFormatterConfiguration(sqlSyntax);
        configuration.setKeywordCase(SQLFormatterConfiguration.KEYWORD_UPPER_CASE);
        //configuration.setIndentString(indentation);
        return new SQLFormatter(configuration).format(content);
/*
    	if (sqlSyntax == null)
    	{
    		return allToUpper(content);
    	}
        return keyWordsToUpper(content);
*/
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
            DBPKeywordType type = sqlSyntax.getKeywordManager().getKeywordType(token.toUpperCase());
            if (type == DBPKeywordType.KEYWORD) {
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
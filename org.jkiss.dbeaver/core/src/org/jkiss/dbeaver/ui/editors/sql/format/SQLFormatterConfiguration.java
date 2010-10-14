/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.format;

import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLSyntaxManager;

/**
 * SQLFormatterConfiguration
 */
public class SQLFormatterConfiguration {

    public static final int KEYWORD_NONE = 0;
    public static final int KEYWORD_UPPER_CASE = 1;
    public static final int KEYWORD_LOWER_CASE = 2;

    private int keywordCase = KEYWORD_UPPER_CASE;
    private String indentString = "    ";
    private SQLSyntaxManager syntaxManager;

    public SQLFormatterConfiguration(SQLSyntaxManager syntaxManager)
    {
        this.syntaxManager = syntaxManager;
    }

    public SQLSyntaxManager getSyntaxManager()
    {
        return syntaxManager;
    }

    public String getIndentString()
    {
        return indentString;
    }

    public void setIndentString(String indentString)
    {
        this.indentString = indentString;
    }

    public int getKeywordCase()
    {
        return keywordCase;
    }

    public void setKeywordCase(int keyword) {
        this.keywordCase = keyword;
    }

    boolean isFunction(String name) {
        return syntaxManager.getKeywordType(name) == SQLSyntaxManager.KeywordType.FUNCTION;
    }

}

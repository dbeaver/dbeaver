/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.sql.format;

import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;

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

    public boolean isFunction(String name) {
        return syntaxManager.getDialect().getKeywordType(name) == DBPKeywordType.FUNCTION;
    }

}

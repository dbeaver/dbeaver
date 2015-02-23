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

package org.jkiss.dbeaver.ui.editors.sql.format;

import org.jkiss.dbeaver.model.DBPKeywordType;
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

    public boolean isFunction(String name) {
        return syntaxManager.getDialect().getKeywordType(name) == DBPKeywordType.FUNCTION;
    }

}

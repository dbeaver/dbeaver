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

package org.jkiss.dbeaver.model.sql.format;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.format.external.SQLExternalFormatter;
import org.jkiss.dbeaver.model.sql.format.tokenized.SQLTokenizedFormatter;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * SQLFormatterConfiguration
 */
public class SQLFormatterConfiguration {

    @NotNull
    private DBPIdentifierCase keywordCase;
    private String indentString = "    ";
    private SQLSyntaxManager syntaxManager;
    @NotNull
    private String sourceEncoding = GeneralUtils.DEFAULT_ENCODING;

    public SQLFormatterConfiguration(SQLSyntaxManager syntaxManager)
    {
        this.syntaxManager = syntaxManager;
        this.keywordCase = syntaxManager.getKeywordCase();
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

    @NotNull
    public DBPIdentifierCase getKeywordCase()
    {
        return keywordCase;
    }

    public void setKeywordCase(@NotNull DBPIdentifierCase keyword) {
        this.keywordCase = keyword;
    }

    @NotNull
    public String getSourceEncoding() {
        return sourceEncoding;
    }

    public void setSourceEncoding(@NotNull String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    public boolean isFunction(String name) {
        return syntaxManager.getDialect().getKeywordType(name) == DBPKeywordType.FUNCTION;
    }

    public SQLFormatter createFormatter() {
        final String formatterId = CommonUtils.notEmpty(syntaxManager.getPreferenceStore().getString(ModelPreferences.SQL_FORMAT_FORMATTER)).toUpperCase(Locale.ENGLISH);
        if (SQLExternalFormatter.FORMATTER_ID.equals(formatterId)) {
            return new SQLExternalFormatter();
        } else {
            return new SQLTokenizedFormatter();
        }
    }
}

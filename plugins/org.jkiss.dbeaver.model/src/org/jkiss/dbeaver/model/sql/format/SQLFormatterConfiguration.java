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

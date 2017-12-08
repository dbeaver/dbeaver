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
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SQLFormatterConfiguration
 */
public class SQLFormatterConfiguration {

    private String formatterId;

    @NotNull
    private DBPIdentifierCase keywordCase;
    private String indentString = "    ";
    private SQLSyntaxManager syntaxManager;
    @NotNull
    private String sourceEncoding = GeneralUtils.DEFAULT_ENCODING;

    private Map<String, Object> properties = new HashMap<>();

    /**
     * Create formatter config with default (set in properties) formatter
     */
    public SQLFormatterConfiguration(SQLSyntaxManager syntaxManager)
    {
        this(syntaxManager, CommonUtils.notEmpty(syntaxManager.getPreferenceStore().getString(ModelPreferences.SQL_FORMAT_FORMATTER)).toUpperCase(Locale.ENGLISH));
    }

    public SQLFormatterConfiguration(SQLSyntaxManager syntaxManager, String formatterId)
    {
        this.syntaxManager = syntaxManager;
        this.keywordCase = syntaxManager.getKeywordCase();

        this.formatterId = formatterId;
    }

    public SQLSyntaxManager getSyntaxManager()
    {
        return syntaxManager;
    }

    public String getFormatterId() {
        return formatterId;
    }

    public void setFormatterId(String formatterId) {
        this.formatterId = formatterId;
        syntaxManager.getPreferenceStore().setValue(
            ModelPreferences.SQL_FORMAT_FORMATTER, formatterId.toUpperCase(Locale.ENGLISH));
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

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public DBPPreferenceStore getPreferenceStore() {
        return syntaxManager.getPreferenceStore();
    }

    public void loadSettings() {

    }

    public void saveSettings() {

    }

}

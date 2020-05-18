/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * SQLSyntaxManager.
 * <p/>
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public class SQLSyntaxManager {

    @NotNull
    private SQLDialect sqlDialect = BasicSQLDialect.INSTANCE;
    @NotNull
    private DBPPreferenceStore preferenceStore = ModelPreferences.getPreferences();
    @Nullable
    private String[][] identifierQuoteStrings;
    private String[][] stringQuoteStrings;
    private char structSeparator;
    private boolean parametersEnabled;
    private boolean anonymousParametersEnabled;
    private char anonymousParameterMark;
    private String[] namedParameterPrefixes;
    private String controlCommandPrefix;
    private boolean variablesEnabled;
    @NotNull
    private String catalogSeparator = String.valueOf(SQLConstants.STRUCT_SEPARATOR);
    @NotNull
    private String[] statementDelimiters = new String[0];

    private char escapeChar;
    private boolean blankLineDelimiter;

    public SQLSyntaxManager()
    {
    }

    @NotNull
    public SQLDialect getDialect() {
        return sqlDialect;
    }

    @NotNull
    public DBPPreferenceStore getPreferenceStore() {
        return preferenceStore;
    }

    public char getStructSeparator()
    {
        return structSeparator;
    }

    @NotNull
    public String getCatalogSeparator()
    {
        return catalogSeparator;
    }

    @NotNull
    public String[] getStatementDelimiters()
    {
        return statementDelimiters;
    }

    public boolean isBlankLineDelimiter() {
        return blankLineDelimiter;
    }

    @Nullable
    public String[][] getIdentifierQuoteStrings() {
        return identifierQuoteStrings;
    }

    public String[][] getStringQuoteStrings() {
        return stringQuoteStrings;
    }

    public char getEscapeChar() {
        return escapeChar;
    }

    public boolean isParametersEnabled() {
        return parametersEnabled;
    }

    public boolean isAnonymousParametersEnabled() {
        return anonymousParametersEnabled;
    }

    public char getAnonymousParameterMark() {
        return anonymousParameterMark;
    }

    public String[] getNamedParameterPrefixes() {
        return namedParameterPrefixes;
    }

    public String getControlCommandPrefix() {
        return controlCommandPrefix;
    }

    public boolean isVariablesEnabled() {
        return variablesEnabled;
    }

    public void init(@NotNull DBPDataSource dataSource) {
        init(SQLUtils.getDialectFromObject(dataSource), dataSource.getContainer().getPreferenceStore());
    }

    public void init(@NotNull SQLDialect dialect, @NotNull DBPPreferenceStore preferenceStore)
    {
        this.statementDelimiters = new String[0];
        this.sqlDialect = dialect;
        this.preferenceStore = preferenceStore;
        this.identifierQuoteStrings = sqlDialect.getIdentifierQuoteStrings();
        this.stringQuoteStrings = sqlDialect.getStringQuoteStrings();
        this.structSeparator = sqlDialect.getStructSeparator();
        this.catalogSeparator = sqlDialect.getCatalogSeparator();
        this.escapeChar = dialect.getStringEscapeCharacter();
        if (!preferenceStore.getBoolean(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER)) {
            this.statementDelimiters = new String[] { sqlDialect.getScriptDelimiter().toLowerCase() };
        }

        String extraDelimiters = preferenceStore.getString(ModelPreferences.SCRIPT_STATEMENT_DELIMITER);
        StringTokenizer st = new StringTokenizer(extraDelimiters, " \t,");
        while (st.hasMoreTokens()) {
            String delim = st.nextToken();
            if (!ArrayUtils.contains(this.statementDelimiters, delim)) {
                this.statementDelimiters = ArrayUtils.add(String.class, this.statementDelimiters, delim);
            }
        }
        blankLineDelimiter = preferenceStore.getBoolean(ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK);

        this.parametersEnabled = preferenceStore.getBoolean(ModelPreferences.SQL_PARAMETERS_ENABLED);
        this.anonymousParametersEnabled = preferenceStore.getBoolean(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED);
        this.variablesEnabled = preferenceStore.getBoolean(ModelPreferences.SQL_VARIABLES_ENABLED);
        String markString = preferenceStore.getString(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK);
        if (CommonUtils.isEmpty(markString)) {
            this.anonymousParameterMark = SQLConstants.DEFAULT_PARAMETER_MARK;
        } else {
            this.anonymousParameterMark = markString.charAt(0);
        }
        Set<String> paramsPrefixes = new LinkedHashSet<>();
        String paramPrefixString = preferenceStore.getString(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX);
        if (!CommonUtils.isEmpty(paramPrefixString)) {
            paramsPrefixes.add(paramPrefixString);
        }
        Collections.addAll(paramsPrefixes, dialect.getParametersPrefixes());
        namedParameterPrefixes = paramsPrefixes.toArray(new String[0]);

        this.controlCommandPrefix = preferenceStore.getString(ModelPreferences.SQL_CONTROL_COMMAND_PREFIX);
        if (CommonUtils.isEmpty(this.controlCommandPrefix)) {
            this.controlCommandPrefix = SQLConstants.DEFAULT_CONTROL_COMMAND_PREFIX;
        }
    }

    @NotNull
    public DBPIdentifierCase getKeywordCase() {
        final String caseName = preferenceStore.getString(ModelPreferences.SQL_FORMAT_KEYWORD_CASE);
        if (CommonUtils.isEmpty(caseName)) {
            // Database specific
            return sqlDialect.storesUnquotedCase();
        } else {
            try {
                return DBPIdentifierCase.valueOf(caseName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return DBPIdentifierCase.MIXED;
            }
        }
    }
}

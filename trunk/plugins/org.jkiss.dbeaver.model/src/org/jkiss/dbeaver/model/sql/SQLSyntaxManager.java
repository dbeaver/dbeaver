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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.utils.CommonUtils;

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
    private SQLDialect sqlDialect;
    @Nullable
    private String quoteSymbol;
    private char structSeparator;
    private boolean parametersEnabled;
    private boolean anonymousParametersEnabled;
    private char anonymousParameterMark;
    @NotNull
    private String catalogSeparator;
    @NotNull
    private Set<String> statementDelimiters = new LinkedHashSet<String>();//SQLConstants.DEFAULT_STATEMENT_DELIMITER;

    private char escapeChar;
    private boolean unassigned;

    public SQLSyntaxManager()
    {
    }

    /**
     * Returns true if this syntax manager wasn't assigned to a some particular data source container/ SQL dialect
     */
    public boolean isUnassigned() {
        return unassigned;
    }

    @NotNull
    public SQLDialect getDialect() {
        return sqlDialect;
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
    public Set<String> getStatementDelimiters()
    {
        return statementDelimiters;
    }

    @Nullable
    public String getQuoteSymbol()
    {
        return quoteSymbol;
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

    public void setDataSource(@Nullable SQLDataSource dataSource)
    {
        this.unassigned = dataSource == null;
        this.statementDelimiters.clear();
        DBPPreferenceStore preferenceStore;
        if (dataSource == null) {
            preferenceStore = ModelPreferences.getPreferences();
            sqlDialect = new BasicSQLDialect();
            quoteSymbol = null;
            structSeparator = SQLConstants.STRUCT_SEPARATOR;
            catalogSeparator = String.valueOf(SQLConstants.STRUCT_SEPARATOR);
            escapeChar = '\\';
            statementDelimiters.add(SQLConstants.DEFAULT_STATEMENT_DELIMITER);
        } else {
            preferenceStore = dataSource.getContainer().getPreferenceStore();

            sqlDialect = dataSource.getSQLDialect();
            quoteSymbol = sqlDialect.getIdentifierQuoteString();
            structSeparator = sqlDialect.getStructSeparator();
            catalogSeparator = sqlDialect.getCatalogSeparator();
            sqlDialect.getSearchStringEscape();
            escapeChar = '\\';
            if (!preferenceStore.getBoolean(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER)) {
                statementDelimiters.add(sqlDialect.getScriptDelimiter().toLowerCase());
            }

            String extraDelimiters = preferenceStore.getString(ModelPreferences.SCRIPT_STATEMENT_DELIMITER);
            StringTokenizer st = new StringTokenizer(extraDelimiters, " \t,");
            while (st.hasMoreTokens()) {
                statementDelimiters.add(st.nextToken());
            }
        }

        parametersEnabled = preferenceStore.getBoolean(ModelPreferences.SQL_PARAMETERS_ENABLED);
        anonymousParametersEnabled = preferenceStore.getBoolean(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED);
        String markString = preferenceStore.getString(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK);
        if (CommonUtils.isEmpty(markString)) {
            anonymousParameterMark = SQLConstants.DEFAULT_PARAMETER_MARK;
        } else {
            anonymousParameterMark = markString.charAt(0);
        }
    }

}

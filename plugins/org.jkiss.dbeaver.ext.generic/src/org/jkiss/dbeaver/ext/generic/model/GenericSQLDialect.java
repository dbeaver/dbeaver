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

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.utils.CommonUtils;

/**
 * Generic data source info
 */
public class GenericSQLDialect extends JDBCSQLDialect {

    public static final String GENERIC_DIALECT_ID = "generic";

    private static String[] EXEC_KEYWORDS =  { "EXEC", "CALL" };

    private String scriptDelimiter;
    private char stringEscapeCharacter = '\0';
    private String scriptDelimiterRedefiner;
    private boolean legacySQLDialect;
    private boolean supportsUpsert;
    private boolean quoteReservedWords;
    private boolean useSearchStringEscape;
    private String dualTable;
    private String testSQL;
    private boolean hasDelimiterAfterQuery;
    private boolean hasDelimiterAfterBlock;
    private boolean callableQueryInBrackets;

    public GenericSQLDialect() {
        super("Generic");
    }

    protected GenericSQLDialect(String name) {
        super(name);
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        DBPDriver driver = dataSource.getContainer().getDriver();
        this.scriptDelimiter = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_SCRIPT_DELIMITER));
        String escapeStr = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_STRING_ESCAPE_CHAR));
        if (!CommonUtils.isEmpty(escapeStr)) {
            this.stringEscapeCharacter = escapeStr.charAt(0);
        }
        this.scriptDelimiterRedefiner = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_SCRIPT_DELIMITER_REDEFINER));
        this.hasDelimiterAfterQuery = CommonUtils.toBoolean(driver.getDriverParameter(GenericConstants.PARAM_SQL_DELIMITER_AFTER_QUERY));
        this.hasDelimiterAfterBlock = CommonUtils.toBoolean(driver.getDriverParameter(GenericConstants.PARAM_SQL_DELIMITER_AFTER_BLOCK));
        this.legacySQLDialect = CommonUtils.toBoolean(driver.getDriverParameter(GenericConstants.PARAM_LEGACY_DIALECT));
        this.supportsUpsert = ((GenericDataSource)dataSource).getMetaModel().supportsUpsertStatement();
        if (this.supportsUpsert) {
            addSQLKeyword("UPSERT");
        }
        this.useSearchStringEscape = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_USE_SEARCH_STRING_ESCAPE), false);
        this.quoteReservedWords = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_QUOTE_RESERVED_WORDS), true);
        this.testSQL = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_QUERY_PING));
        if (CommonUtils.isEmpty(this.testSQL)) {
            this.testSQL = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_QUERY_GET_ACTIVE_DB));
        }
        this.dualTable = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_DUAL_TABLE));
        if (this.dualTable.isEmpty()) {
            this.dualTable = null;
        }
    }

    @NotNull
    @Override
    public String getScriptDelimiter() {
        return CommonUtils.isEmpty(scriptDelimiter) ? super.getScriptDelimiter() : scriptDelimiter;
    }

    @Override
    public char getStringEscapeCharacter() {
        return stringEscapeCharacter;
    }

    @Override
    public String getScriptDelimiterRedefiner() {
        return scriptDelimiterRedefiner;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return super.supportsAliasInSelect();
    }

    @Override
    public boolean isDelimiterAfterQuery() {
        return hasDelimiterAfterQuery;
    }

    @Override
    public boolean isDelimiterAfterBlock() {
        return hasDelimiterAfterBlock;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords()
    {
        return EXEC_KEYWORDS;
    }

    public boolean isLegacySQLDialect() {
        return legacySQLDialect;
    }

    @Override
    public boolean supportsUpsertStatement() {
        return supportsUpsert;
    }

    @Override
    public boolean isQuoteReservedWords() {
        return quoteReservedWords;
    }

    @Override
    public String formatStoredProcedureCall(DBPDataSource dataSource, String sqlText) {
        if (callableQueryInBrackets) {
            return "{" + sqlText + "}";
        }
        return super.formatStoredProcedureCall(dataSource, sqlText);
    }

    @Override
    public String getTestSQL() {
        return testSQL;
    }

    @Nullable
    @Override
    public String getDualTableName() {
        return dualTable;
    }

    @NotNull
    @Override
    public String getSearchStringEscape() {
        if (useSearchStringEscape) {
            return super.getSearchStringEscape();
        } else {
            return null;
        }
    }
}

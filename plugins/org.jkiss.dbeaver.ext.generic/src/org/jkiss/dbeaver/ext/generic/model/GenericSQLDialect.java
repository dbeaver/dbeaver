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

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.utils.CommonUtils;

/**
 * Generic data source info
 */
public class GenericSQLDialect extends JDBCSQLDialect {

    private static String[] EXEC_KEYWORDS =  { "EXEC", "CALL" };

    private String scriptDelimiter;
    private String scriptDelimiterRedefiner;
    private boolean legacySQLDialect;
    private boolean suportsUpsert;
    private boolean quoteReservedWords;
    private String dualTable;
    private String testSQL;
    private boolean hasDelimiterAfterQuery;
    private boolean hasDelimiterAfterBlock;

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
        this.scriptDelimiterRedefiner = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_SCRIPT_DELIMITER_REDEFINER));
        this.hasDelimiterAfterQuery = CommonUtils.toBoolean(driver.getDriverParameter(GenericConstants.PARAM_SQL_DELIMITER_AFTER_QUERY));
        this.hasDelimiterAfterBlock = CommonUtils.toBoolean(driver.getDriverParameter(GenericConstants.PARAM_SQL_DELIMITER_AFTER_BLOCK));
        this.legacySQLDialect = CommonUtils.toBoolean(driver.getDriverParameter(GenericConstants.PARAM_LEGACY_DIALECT));
        this.suportsUpsert = ((GenericDataSource)dataSource).getMetaModel().supportsUpsertStatement();
        if (this.suportsUpsert) {
            addSQLKeyword("UPSERT");
        }
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
    public String getScriptDelimiter()
    {
        return CommonUtils.isEmpty(scriptDelimiter) ? super.getScriptDelimiter() : scriptDelimiter;
    }

    @Override
    public String getScriptDelimiterRedefiner() {
        return scriptDelimiterRedefiner;
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
        return suportsUpsert;
    }

    @Override
    public boolean isQuoteReservedWords() {
        return quoteReservedWords;
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
}

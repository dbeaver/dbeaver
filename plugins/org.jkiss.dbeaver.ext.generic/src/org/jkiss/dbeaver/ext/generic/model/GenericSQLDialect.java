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

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.utils.CommonUtils;

/**
 * Generic data source info
 */
public class GenericSQLDialect extends JDBCSQLDialect {

    private static String[] EXEC_KEYWORDS =  { "EXEC", "CALL" };

    private final String scriptDelimiter;
    private final String scriptDelimiterRedefiner;
    private final boolean legacySQLDialect;
    private final boolean suportsUpsert;
    private final boolean quoteReservedWords;
    private String dualTable;
    private String testSQL;

    public GenericSQLDialect(GenericDataSource dataSource, JDBCDatabaseMetaData metaData)
    {
        super("Generic", metaData);
        DBPDriver driver = dataSource.getContainer().getDriver();
        this.scriptDelimiter = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_SCRIPT_DELIMITER));
        this.scriptDelimiterRedefiner = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_SCRIPT_DELIMITER_REDEFINER));
        this.legacySQLDialect = CommonUtils.toBoolean(driver.getDriverParameter(GenericConstants.PARAM_LEGACY_DIALECT));
        this.suportsUpsert = dataSource.getMetaModel().supportsUpsertStatement();
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

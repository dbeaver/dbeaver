/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.greptime.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.greptime.GreptimeUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.Map;

public class GreptimeMetaModel extends GenericMetaModel {

    private static final String ORDINAL_POSITION = "ORDINAL_POSITION"; //$NON-NLS-1$

    @NotNull
    @Override
    public GenericDataSource createDataSourceImpl(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container
    ) throws DBException {
        return new GreptimeDataSource(monitor, container, this);
    }

    @NotNull
    @Override
    public JDBCStatement prepareTableLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase object,
        @Nullable String objectName
    ) throws SQLException {
        String targetName = object != null ? object.getName() : objectName;
        String sql = """
            SELECT table_schema AS TABLE_CAT,
                   CAST(NULL AS STRING) AS TABLE_SCHEM,
                   table_name AS TABLE_NAME,
                   table_type AS TABLE_TYPE,
                   table_comment AS REMARKS
              FROM information_schema.tables
             WHERE table_schema = ?""" + (targetName == null ? "" : " AND table_name = ?"); //$NON-NLS-1$

        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, getDatabaseName(owner));
        if (targetName != null) {
            dbStat.setString(2, targetName);
        }
        return dbStat;
    }

    @NotNull
    @Override
    public JDBCStatement prepareTableColumnLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase forTable
    ) throws SQLException {
        if (forTable == null) {
            throw new SQLException("Cannot load columns without specifying a table"); //$NON-NLS-1$
        }

        String sql = """
            SELECT column_name,
                   column_type,
                   is_nullable,
                   column_default,
                   column_comment,
                   ordinal_position,
                   character_maximum_length,
                   character_octet_length,
                   numeric_precision,
                   numeric_scale,
                   datetime_precision,
                   semantic_type
              FROM information_schema.columns
             WHERE table_schema = ?
               AND table_name = ?
             ORDER BY ordinal_position"""; //$NON-NLS-1$
        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, getDatabaseName(owner));
        dbStat.setString(2, forTable.getName());
        return dbStat;
    }

    @Override
    public GenericTableColumn fetchTableColumn(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @NotNull GenericTableBase table,
        @NotNull JDBCResultSet dbResult
    ) throws DBException {
        return new GreptimeTableColumn(table, dbResult, JDBCUtils.safeGetInt(dbResult, ORDINAL_POSITION));
    }

    @Nullable
    @Override
    public String getTableDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase sourceObject,
        @NotNull Map<String, Object> options
    ) throws DBException {
        return GreptimeUtils.loadShowCreateDDL(monitor, sourceObject, "SHOW CREATE TABLE"); //$NON-NLS-1$
    }

    @Nullable
    @Override
    public String getViewDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericView sourceObject,
        @NotNull Map<String, Object> options
    ) throws DBException {
        return GreptimeUtils.loadShowCreateDDL(monitor, sourceObject, "SHOW CREATE VIEW"); //$NON-NLS-1$
    }

    @Override
    public boolean hasProcedureSupport() {
        return false;
    }

    @Override
    public boolean hasFunctionSupport() {
        return false;
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return false;
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return false;
    }

    @Override
    public boolean supportsSynonyms(@NotNull GenericDataSource dataSource) {
        return false;
    }

    private static String getDatabaseName(@NotNull GenericStructContainer owner) {
        GenericCatalog catalog = owner.getCatalog();
        return catalog == null ? owner.getName() : catalog.getName();
    }
}

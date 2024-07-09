/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.exec.jdbc;

import org.jkiss.code.Nullable;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Manageable connection metadata
 */
public interface JDBCDatabaseMetaData extends DatabaseMetaData {

    @Override
    JDBCResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
        throws SQLException;

    @Override
    JDBCResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern)
        throws SQLException;

    @Override
    JDBCResultSet getTables(@Nullable String catalog, @Nullable String schemaPattern, String tableNamePattern, @Nullable String[] types)
        throws SQLException;

    @Override
    JDBCResultSet getSchemas()
        throws SQLException;

    @Override
    JDBCResultSet getCatalogs()
        throws SQLException;

    @Override
    JDBCResultSet getTableTypes()
        throws SQLException;

    @Override
    JDBCResultSet getColumns(@Nullable String catalog, @Nullable String schemaPattern, String tableNamePattern,
                         String columnNamePattern)
        throws SQLException;

    @Override
    JDBCResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern)
        throws SQLException;

    @Override
    JDBCResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern)
        throws SQLException;

    @Override
    JDBCResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable)
        throws SQLException;

    @Override
    JDBCResultSet getVersionColumns(String catalog, String schema, String table)
        throws SQLException;

    @Override
    JDBCResultSet getPrimaryKeys(@Nullable String catalog, @Nullable String schema, String table)
        throws SQLException;

    @Override
    JDBCResultSet getImportedKeys(String catalog, String schema, String table)
        throws SQLException;

    @Override
    JDBCResultSet getExportedKeys(String catalog, String schema, String table)
        throws SQLException;

    @Override
    JDBCResultSet getCrossReference(
        String parentCatalog, String parentSchema, String parentTable,
        String foreignCatalog, String foreignSchema, String foreignTable)
        throws SQLException;

    @Override
    JDBCResultSet getTypeInfo()
        throws SQLException;

    @Override
    JDBCResultSet getIndexInfo(@Nullable String catalog, @Nullable String schema, String table, boolean unique, boolean approximate)
        throws SQLException;

    @Override
    JDBCResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types)
        throws SQLException;

    @Override
    JDBCResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern)
        throws SQLException;

    @Override
    JDBCResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern)
        throws SQLException;

    @Override
    JDBCResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern)
        throws SQLException;

    @Override
    JDBCResultSet getSchemas(String catalog, String schemaPattern)
        throws SQLException;

    @Override
    JDBCResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern)
        throws SQLException;

    @Override
    JDBCResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern)
        throws SQLException;
}

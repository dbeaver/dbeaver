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
package org.jkiss.dbeaver.model.exec.jdbc;

import org.jkiss.code.Nullable;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Managable connection metadata
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

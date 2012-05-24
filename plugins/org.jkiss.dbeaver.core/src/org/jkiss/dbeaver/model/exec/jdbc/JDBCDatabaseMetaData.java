/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.jdbc;

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
    JDBCResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
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
    JDBCResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern,
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
    JDBCResultSet getPrimaryKeys(String catalog, String schema, String table)
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
    JDBCResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
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

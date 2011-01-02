/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Managable connection metadata
 */
public interface JDBCDatabaseMetaData extends DatabaseMetaData {

    JDBCResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
        throws SQLException;

    JDBCResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern)
        throws SQLException;

    JDBCResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
        throws SQLException;

    JDBCResultSet getSchemas()
        throws SQLException;

    JDBCResultSet getCatalogs()
        throws SQLException;

    JDBCResultSet getTableTypes()
        throws SQLException;

    JDBCResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern,
                         String columnNamePattern)
        throws SQLException;

    JDBCResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern)
        throws SQLException;

    JDBCResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern)
        throws SQLException;

    JDBCResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable)
        throws SQLException;

    JDBCResultSet getVersionColumns(String catalog, String schema, String table)
        throws SQLException;

    JDBCResultSet getPrimaryKeys(String catalog, String schema, String table)
        throws SQLException;

    JDBCResultSet getImportedKeys(String catalog, String schema, String table)
        throws SQLException;

    JDBCResultSet getExportedKeys(String catalog, String schema, String table)
        throws SQLException;

    JDBCResultSet getCrossReference(
        String parentCatalog, String parentSchema, String parentTable,
        String foreignCatalog, String foreignSchema, String foreignTable)
        throws SQLException;

    JDBCResultSet getTypeInfo()
        throws SQLException;

    JDBCResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
        throws SQLException;

    JDBCResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types)
        throws SQLException;

    JDBCResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern)
        throws SQLException;

    JDBCResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern)
        throws SQLException;

    JDBCResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern)
        throws SQLException;

    JDBCResultSet getSchemas(String catalog, String schemaPattern)
        throws SQLException;

    JDBCResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern)
        throws SQLException;

    JDBCResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern)
        throws SQLException;
}

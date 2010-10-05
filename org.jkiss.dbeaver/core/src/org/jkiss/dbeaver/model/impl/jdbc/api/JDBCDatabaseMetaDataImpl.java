/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

import java.sql.DatabaseMetaData;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

/**
 * JDBC database metadata managable
 */
public class JDBCDatabaseMetaDataImpl implements JDBCDatabaseMetaData  {

    private JDBCConnectionImpl connection;
    private DatabaseMetaData original;

    public JDBCDatabaseMetaDataImpl(JDBCConnectionImpl connection, DatabaseMetaData original)
    {
        this.connection = connection;
        this.original = original;
    }

    private JDBCResultSet makeResultSet(java.sql.ResultSet resultSet, String functionName, Object ... args)
    {
        String description = functionName;
        if (args.length > 0) {
            description += "[]";
        }
        return JDBCResultSetImpl.makeResultSet(connection, resultSet, description);
    }

    public JDBCConnectionImpl getConnection()
    {
        return connection;
    }

    public boolean supportsSavepoints()
        throws SQLException
    {
        return getOriginal().supportsSavepoints();
    }

    public boolean supportsNamedParameters()
        throws SQLException
    {
        return getOriginal().supportsNamedParameters();
    }

    public boolean supportsMultipleOpenResults()
        throws SQLException
    {
        return getOriginal().supportsMultipleOpenResults();
    }

    public boolean supportsGetGeneratedKeys()
        throws SQLException
    {
        return getOriginal().supportsGetGeneratedKeys();
    }

    public JDBCResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getSuperTypes(catalog, schemaPattern, typeNamePattern),
            "Load super types", catalog, schemaPattern, typeNamePattern);
    }

    public JDBCResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getSuperTables(catalog, schemaPattern, tableNamePattern),
            "Load super tables", catalog, schemaPattern, tableNamePattern);
    }

    public JDBCResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern),
            "Load UDT attributes", catalog, schemaPattern, typeNamePattern, attributeNamePattern);
    }

    public boolean supportsResultSetHoldability(int holdability)
        throws SQLException
    {
        return getOriginal().supportsResultSetHoldability(holdability);
    }

    public int getResultSetHoldability()
        throws SQLException
    {
        return getOriginal().getResultSetHoldability();
    }

    public int getDatabaseMajorVersion()
        throws SQLException
    {
        return getOriginal().getDatabaseMajorVersion();
    }

    public int getDatabaseMinorVersion()
        throws SQLException
    {
        return getOriginal().getDatabaseMinorVersion();
    }

    public int getJDBCMajorVersion()
        throws SQLException
    {
        return getOriginal().getJDBCMajorVersion();
    }

    public int getJDBCMinorVersion()
        throws SQLException
    {
        return getOriginal().getJDBCMinorVersion();
    }

    public int getSQLStateType()
        throws SQLException
    {
        return getOriginal().getSQLStateType();
    }

    public boolean locatorsUpdateCopy()
        throws SQLException
    {
        return getOriginal().locatorsUpdateCopy();
    }

    public boolean supportsStatementPooling()
        throws SQLException
    {
        return getOriginal().supportsStatementPooling();
    }

    public RowIdLifetime getRowIdLifetime()
        throws SQLException
    {
        return getOriginal().getRowIdLifetime();
    }

    public JDBCResultSet getSchemas(String catalog, String schemaPattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getSchemas(catalog, schemaPattern),
            "Load schemas", catalog, schemaPattern);
    }

    public boolean supportsStoredFunctionsUsingCallSyntax()
        throws SQLException
    {
        return getOriginal().supportsStoredFunctionsUsingCallSyntax();
    }

    public boolean autoCommitFailureClosesAllResultSets()
        throws SQLException
    {
        return getOriginal().autoCommitFailureClosesAllResultSets();
    }

    public JDBCResultSet getClientInfoProperties()
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getClientInfoProperties(),
            "Load client info");
    }

    public JDBCResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getFunctions(catalog, schemaPattern, functionNamePattern),
            "Load functions", catalog, schemaPattern, functionNamePattern);
    }

    public JDBCResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern),
            "Load function columns", catalog, schemaPattern, functionNamePattern, columnNamePattern);
    }

    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        return getOriginal().unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        return getOriginal().isWrapperFor(iface);
    }

    public DatabaseMetaData getOriginal()
    {
        return original;
    }

    public boolean allProceduresAreCallable()
        throws SQLException
    {
        return getOriginal().allProceduresAreCallable();
    }

    public boolean allTablesAreSelectable()
        throws SQLException
    {
        return getOriginal().allTablesAreSelectable();
    }

    public String getURL()
        throws SQLException
    {
        return getOriginal().getURL();
    }

    public String getUserName()
        throws SQLException
    {
        return getOriginal().getUserName();
    }

    public boolean isReadOnly()
        throws SQLException
    {
        return getOriginal().isReadOnly();
    }

    public boolean nullsAreSortedHigh()
        throws SQLException
    {
        return getOriginal().nullsAreSortedHigh();
    }

    public boolean nullsAreSortedLow()
        throws SQLException
    {
        return getOriginal().nullsAreSortedLow();
    }

    public boolean nullsAreSortedAtStart()
        throws SQLException
    {
        return getOriginal().nullsAreSortedAtStart();
    }

    public boolean nullsAreSortedAtEnd()
        throws SQLException
    {
        return getOriginal().nullsAreSortedAtEnd();
    }

    public String getDatabaseProductName()
        throws SQLException
    {
        return getOriginal().getDatabaseProductName();
    }

    public String getDatabaseProductVersion()
        throws SQLException
    {
        return getOriginal().getDatabaseProductVersion();
    }

    public String getDriverName()
        throws SQLException
    {
        return getOriginal().getDriverName();
    }

    public String getDriverVersion()
        throws SQLException
    {
        return getOriginal().getDriverVersion();
    }

    public int getDriverMajorVersion()
    {
        return getOriginal().getDriverMajorVersion();
    }

    public int getDriverMinorVersion()
    {
        return getOriginal().getDriverMinorVersion();
    }

    public boolean usesLocalFiles()
        throws SQLException
    {
        return getOriginal().usesLocalFiles();
    }

    public boolean usesLocalFilePerTable()
        throws SQLException
    {
        return getOriginal().usesLocalFilePerTable();
    }

    public boolean supportsMixedCaseIdentifiers()
        throws SQLException
    {
        return getOriginal().supportsMixedCaseIdentifiers();
    }

    public boolean storesUpperCaseIdentifiers()
        throws SQLException
    {
        return getOriginal().storesUpperCaseIdentifiers();
    }

    public boolean storesLowerCaseIdentifiers()
        throws SQLException
    {
        return getOriginal().storesLowerCaseIdentifiers();
    }

    public boolean storesMixedCaseIdentifiers()
        throws SQLException
    {
        return getOriginal().storesMixedCaseIdentifiers();
    }

    public boolean supportsMixedCaseQuotedIdentifiers()
        throws SQLException
    {
        return getOriginal().supportsMixedCaseQuotedIdentifiers();
    }

    public boolean storesUpperCaseQuotedIdentifiers()
        throws SQLException
    {
        return getOriginal().storesUpperCaseQuotedIdentifiers();
    }

    public boolean storesLowerCaseQuotedIdentifiers()
        throws SQLException
    {
        return getOriginal().storesLowerCaseQuotedIdentifiers();
    }

    public boolean storesMixedCaseQuotedIdentifiers()
        throws SQLException
    {
        return getOriginal().storesMixedCaseQuotedIdentifiers();
    }

    public String getIdentifierQuoteString()
        throws SQLException
    {
        return getOriginal().getIdentifierQuoteString();
    }

    public String getSQLKeywords()
        throws SQLException
    {
        return getOriginal().getSQLKeywords();
    }

    public String getNumericFunctions()
        throws SQLException
    {
        return getOriginal().getNumericFunctions();
    }

    public String getStringFunctions()
        throws SQLException
    {
        return getOriginal().getStringFunctions();
    }

    public String getSystemFunctions()
        throws SQLException
    {
        return getOriginal().getSystemFunctions();
    }

    public String getTimeDateFunctions()
        throws SQLException
    {
        return getOriginal().getTimeDateFunctions();
    }

    public String getSearchStringEscape()
        throws SQLException
    {
        return getOriginal().getSearchStringEscape();
    }

    public String getExtraNameCharacters()
        throws SQLException
    {
        return getOriginal().getExtraNameCharacters();
    }

    public boolean supportsAlterTableWithAddColumn()
        throws SQLException
    {
        return getOriginal().supportsAlterTableWithAddColumn();
    }

    public boolean supportsAlterTableWithDropColumn()
        throws SQLException
    {
        return getOriginal().supportsAlterTableWithDropColumn();
    }

    public boolean supportsColumnAliasing()
        throws SQLException
    {
        return getOriginal().supportsColumnAliasing();
    }

    public boolean nullPlusNonNullIsNull()
        throws SQLException
    {
        return getOriginal().nullPlusNonNullIsNull();
    }

    public boolean supportsConvert()
        throws SQLException
    {
        return getOriginal().supportsConvert();
    }

    public boolean supportsConvert(int fromType, int toType)
        throws SQLException
    {
        return getOriginal().supportsConvert(fromType, toType);
    }

    public boolean supportsTableCorrelationNames()
        throws SQLException
    {
        return getOriginal().supportsTableCorrelationNames();
    }

    public boolean supportsDifferentTableCorrelationNames()
        throws SQLException
    {
        return getOriginal().supportsDifferentTableCorrelationNames();
    }

    public boolean supportsExpressionsInOrderBy()
        throws SQLException
    {
        return getOriginal().supportsExpressionsInOrderBy();
    }

    public boolean supportsOrderByUnrelated()
        throws SQLException
    {
        return getOriginal().supportsOrderByUnrelated();
    }

    public boolean supportsGroupBy()
        throws SQLException
    {
        return getOriginal().supportsGroupBy();
    }

    public boolean supportsGroupByUnrelated()
        throws SQLException
    {
        return getOriginal().supportsGroupByUnrelated();
    }

    public boolean supportsGroupByBeyondSelect()
        throws SQLException
    {
        return getOriginal().supportsGroupByBeyondSelect();
    }

    public boolean supportsLikeEscapeClause()
        throws SQLException
    {
        return getOriginal().supportsLikeEscapeClause();
    }

    public boolean supportsMultipleResultSets()
        throws SQLException
    {
        return getOriginal().supportsMultipleResultSets();
    }

    public boolean supportsMultipleTransactions()
        throws SQLException
    {
        return getOriginal().supportsMultipleTransactions();
    }

    public boolean supportsNonNullableColumns()
        throws SQLException
    {
        return getOriginal().supportsNonNullableColumns();
    }

    public boolean supportsMinimumSQLGrammar()
        throws SQLException
    {
        return getOriginal().supportsMinimumSQLGrammar();
    }

    public boolean supportsCoreSQLGrammar()
        throws SQLException
    {
        return getOriginal().supportsCoreSQLGrammar();
    }

    public boolean supportsExtendedSQLGrammar()
        throws SQLException
    {
        return getOriginal().supportsExtendedSQLGrammar();
    }

    public boolean supportsANSI92EntryLevelSQL()
        throws SQLException
    {
        return getOriginal().supportsANSI92EntryLevelSQL();
    }

    public boolean supportsANSI92IntermediateSQL()
        throws SQLException
    {
        return getOriginal().supportsANSI92IntermediateSQL();
    }

    public boolean supportsANSI92FullSQL()
        throws SQLException
    {
        return getOriginal().supportsANSI92FullSQL();
    }

    public boolean supportsIntegrityEnhancementFacility()
        throws SQLException
    {
        return getOriginal().supportsIntegrityEnhancementFacility();
    }

    public boolean supportsOuterJoins()
        throws SQLException
    {
        return getOriginal().supportsOuterJoins();
    }

    public boolean supportsFullOuterJoins()
        throws SQLException
    {
        return getOriginal().supportsFullOuterJoins();
    }

    public boolean supportsLimitedOuterJoins()
        throws SQLException
    {
        return getOriginal().supportsLimitedOuterJoins();
    }

    public String getSchemaTerm()
        throws SQLException
    {
        return getOriginal().getSchemaTerm();
    }

    public String getProcedureTerm()
        throws SQLException
    {
        return getOriginal().getProcedureTerm();
    }

    public String getCatalogTerm()
        throws SQLException
    {
        return getOriginal().getCatalogTerm();
    }

    public boolean isCatalogAtStart()
        throws SQLException
    {
        return getOriginal().isCatalogAtStart();
    }

    public String getCatalogSeparator()
        throws SQLException
    {
        return getOriginal().getCatalogSeparator();
    }

    public boolean supportsSchemasInDataManipulation()
        throws SQLException
    {
        return getOriginal().supportsSchemasInDataManipulation();
    }

    public boolean supportsSchemasInProcedureCalls()
        throws SQLException
    {
        return getOriginal().supportsSchemasInProcedureCalls();
    }

    public boolean supportsSchemasInTableDefinitions()
        throws SQLException
    {
        return getOriginal().supportsSchemasInTableDefinitions();
    }

    public boolean supportsSchemasInIndexDefinitions()
        throws SQLException
    {
        return getOriginal().supportsSchemasInIndexDefinitions();
    }

    public boolean supportsSchemasInPrivilegeDefinitions()
        throws SQLException
    {
        return getOriginal().supportsSchemasInPrivilegeDefinitions();
    }

    public boolean supportsCatalogsInDataManipulation()
        throws SQLException
    {
        return getOriginal().supportsCatalogsInDataManipulation();
    }

    public boolean supportsCatalogsInProcedureCalls()
        throws SQLException
    {
        return getOriginal().supportsCatalogsInProcedureCalls();
    }

    public boolean supportsCatalogsInTableDefinitions()
        throws SQLException
    {
        return getOriginal().supportsCatalogsInTableDefinitions();
    }

    public boolean supportsCatalogsInIndexDefinitions()
        throws SQLException
    {
        return getOriginal().supportsCatalogsInIndexDefinitions();
    }

    public boolean supportsCatalogsInPrivilegeDefinitions()
        throws SQLException
    {
        return getOriginal().supportsCatalogsInPrivilegeDefinitions();
    }

    public boolean supportsPositionedDelete()
        throws SQLException
    {
        return getOriginal().supportsPositionedDelete();
    }

    public boolean supportsPositionedUpdate()
        throws SQLException
    {
        return getOriginal().supportsPositionedUpdate();
    }

    public boolean supportsSelectForUpdate()
        throws SQLException
    {
        return getOriginal().supportsSelectForUpdate();
    }

    public boolean supportsStoredProcedures()
        throws SQLException
    {
        return getOriginal().supportsStoredProcedures();
    }

    public boolean supportsSubqueriesInComparisons()
        throws SQLException
    {
        return getOriginal().supportsSubqueriesInComparisons();
    }

    public boolean supportsSubqueriesInExists()
        throws SQLException
    {
        return getOriginal().supportsSubqueriesInExists();
    }

    public boolean supportsSubqueriesInIns()
        throws SQLException
    {
        return getOriginal().supportsSubqueriesInIns();
    }

    public boolean supportsSubqueriesInQuantifieds()
        throws SQLException
    {
        return getOriginal().supportsSubqueriesInQuantifieds();
    }

    public boolean supportsCorrelatedSubqueries()
        throws SQLException
    {
        return getOriginal().supportsCorrelatedSubqueries();
    }

    public boolean supportsUnion()
        throws SQLException
    {
        return getOriginal().supportsUnion();
    }

    public boolean supportsUnionAll()
        throws SQLException
    {
        return getOriginal().supportsUnionAll();
    }

    public boolean supportsOpenCursorsAcrossCommit()
        throws SQLException
    {
        return getOriginal().supportsOpenCursorsAcrossCommit();
    }

    public boolean supportsOpenCursorsAcrossRollback()
        throws SQLException
    {
        return getOriginal().supportsOpenCursorsAcrossRollback();
    }

    public boolean supportsOpenStatementsAcrossCommit()
        throws SQLException
    {
        return getOriginal().supportsOpenStatementsAcrossCommit();
    }

    public boolean supportsOpenStatementsAcrossRollback()
        throws SQLException
    {
        return getOriginal().supportsOpenStatementsAcrossRollback();
    }

    public int getMaxBinaryLiteralLength()
        throws SQLException
    {
        return getOriginal().getMaxBinaryLiteralLength();
    }

    public int getMaxCharLiteralLength()
        throws SQLException
    {
        return getOriginal().getMaxCharLiteralLength();
    }

    public int getMaxColumnNameLength()
        throws SQLException
    {
        return getOriginal().getMaxColumnNameLength();
    }

    public int getMaxColumnsInGroupBy()
        throws SQLException
    {
        return getOriginal().getMaxColumnsInGroupBy();
    }

    public int getMaxColumnsInIndex()
        throws SQLException
    {
        return getOriginal().getMaxColumnsInIndex();
    }

    public int getMaxColumnsInOrderBy()
        throws SQLException
    {
        return getOriginal().getMaxColumnsInOrderBy();
    }

    public int getMaxColumnsInSelect()
        throws SQLException
    {
        return getOriginal().getMaxColumnsInSelect();
    }

    public int getMaxColumnsInTable()
        throws SQLException
    {
        return getOriginal().getMaxColumnsInTable();
    }

    public int getMaxConnections()
        throws SQLException
    {
        return getOriginal().getMaxConnections();
    }

    public int getMaxCursorNameLength()
        throws SQLException
    {
        return getOriginal().getMaxCursorNameLength();
    }

    public int getMaxIndexLength()
        throws SQLException
    {
        return getOriginal().getMaxIndexLength();
    }

    public int getMaxSchemaNameLength()
        throws SQLException
    {
        return getOriginal().getMaxSchemaNameLength();
    }

    public int getMaxProcedureNameLength()
        throws SQLException
    {
        return getOriginal().getMaxProcedureNameLength();
    }

    public int getMaxCatalogNameLength()
        throws SQLException
    {
        return getOriginal().getMaxCatalogNameLength();
    }

    public int getMaxRowSize()
        throws SQLException
    {
        return getOriginal().getMaxRowSize();
    }

    public boolean doesMaxRowSizeIncludeBlobs()
        throws SQLException
    {
        return getOriginal().doesMaxRowSizeIncludeBlobs();
    }

    public int getMaxStatementLength()
        throws SQLException
    {
        return getOriginal().getMaxStatementLength();
    }

    public int getMaxStatements()
        throws SQLException
    {
        return getOriginal().getMaxStatements();
    }

    public int getMaxTableNameLength()
        throws SQLException
    {
        return getOriginal().getMaxTableNameLength();
    }

    public int getMaxTablesInSelect()
        throws SQLException
    {
        return getOriginal().getMaxTablesInSelect();
    }

    public int getMaxUserNameLength()
        throws SQLException
    {
        return getOriginal().getMaxUserNameLength();
    }

    public int getDefaultTransactionIsolation()
        throws SQLException
    {
        return getOriginal().getDefaultTransactionIsolation();
    }

    public boolean supportsTransactions()
        throws SQLException
    {
        return getOriginal().supportsTransactions();
    }

    public boolean supportsTransactionIsolationLevel(int level)
        throws SQLException
    {
        return getOriginal().supportsTransactionIsolationLevel(level);
    }

    public boolean supportsDataDefinitionAndDataManipulationTransactions()
        throws SQLException
    {
        return getOriginal().supportsDataDefinitionAndDataManipulationTransactions();
    }

    public boolean supportsDataManipulationTransactionsOnly()
        throws SQLException
    {
        return getOriginal().supportsDataManipulationTransactionsOnly();
    }

    public boolean dataDefinitionCausesTransactionCommit()
        throws SQLException
    {
        return getOriginal().dataDefinitionCausesTransactionCommit();
    }

    public boolean dataDefinitionIgnoredInTransactions()
        throws SQLException
    {
        return getOriginal().dataDefinitionIgnoredInTransactions();
    }

    public JDBCResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getProcedures(catalog, schemaPattern, procedureNamePattern),
            "Load procedures", catalog, schemaPattern, procedureNamePattern);
    }

    public JDBCResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern,
                                         String columnNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern),
            "Load procedure columns", catalog, schemaPattern, procedureNamePattern, columnNamePattern);
    }

    public JDBCResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getTables(catalog, schemaPattern, tableNamePattern, types),
            "Load tables", catalog, schemaPattern, tableNamePattern, types);
    }

    public JDBCResultSet getSchemas()
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getSchemas(),
            "Loa schemas");
    }

    public JDBCResultSet getCatalogs()
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getCatalogs(),
            "Load catalogs");
    }

    public JDBCResultSet getTableTypes()
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getTableTypes(),
            "Load table types");
    }

    public JDBCResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern),
            "Load columns", catalog, schemaPattern, tableNamePattern, columnNamePattern);
    }

    public JDBCResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getColumnPrivileges(catalog, schema, table, columnNamePattern),
            "Load column privileges", catalog, schema, table, columnNamePattern);
    }

    public JDBCResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getTablePrivileges(catalog, schemaPattern, tableNamePattern),
            "Load table privileges", catalog, schemaPattern, tableNamePattern);
    }

    public JDBCResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getBestRowIdentifier(catalog, schema, table, scope, nullable),
            "Find best row identifier", catalog, schema, table, scope, nullable);
    }

    public JDBCResultSet getVersionColumns(String catalog, String schema, String table)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getVersionColumns(catalog, schema, table),
            "Find version columns", catalog, schema, table);
    }

    public JDBCResultSet getPrimaryKeys(String catalog, String schema, String table)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getPrimaryKeys(catalog, schema, table),
            "Load primary keys", catalog, schema, table);
    }

    public JDBCResultSet getImportedKeys(String catalog, String schema, String table)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getImportedKeys(catalog, schema, table),
            "Load imported keys", catalog, schema, table);
    }

    public JDBCResultSet getExportedKeys(String catalog, String schema, String table)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getExportedKeys(catalog, schema, table),
            "Load exported keys", catalog, schema, table);
    }

    public JDBCResultSet getCrossReference(
        String parentCatalog, String parentSchema, String parentTable,
        String foreignCatalog, String foreignSchema, String foreignTable)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable),
            "Load cross reference", parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable);
    }

    public JDBCResultSet getTypeInfo()
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getTypeInfo(),
            "Load type info");
    }

    public JDBCResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getIndexInfo(catalog, schema, table, unique, approximate),
            "Load indexes", catalog, schema, table, unique, approximate);
    }

    public boolean supportsResultSetType(int type)
        throws SQLException
    {
        return getOriginal().supportsResultSetType(type);
    }

    public boolean supportsResultSetConcurrency(int type, int concurrency)
        throws SQLException
    {
        return getOriginal().supportsResultSetConcurrency(type, concurrency);
    }

    public boolean ownUpdatesAreVisible(int type)
        throws SQLException
    {
        return getOriginal().ownUpdatesAreVisible(type);
    }

    public boolean ownDeletesAreVisible(int type)
        throws SQLException
    {
        return getOriginal().ownDeletesAreVisible(type);
    }

    public boolean ownInsertsAreVisible(int type)
        throws SQLException
    {
        return getOriginal().ownInsertsAreVisible(type);
    }

    public boolean othersUpdatesAreVisible(int type)
        throws SQLException
    {
        return getOriginal().othersUpdatesAreVisible(type);
    }

    public boolean othersDeletesAreVisible(int type)
        throws SQLException
    {
        return getOriginal().othersDeletesAreVisible(type);
    }

    public boolean othersInsertsAreVisible(int type)
        throws SQLException
    {
        return getOriginal().othersInsertsAreVisible(type);
    }

    public boolean updatesAreDetected(int type)
        throws SQLException
    {
        return getOriginal().updatesAreDetected(type);
    }

    public boolean deletesAreDetected(int type)
        throws SQLException
    {
        return getOriginal().deletesAreDetected(type);
    }

    public boolean insertsAreDetected(int type)
        throws SQLException
    {
        return getOriginal().insertsAreDetected(type);
    }

    public boolean supportsBatchUpdates()
        throws SQLException
    {
        return getOriginal().supportsBatchUpdates();
    }

    public JDBCResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getUDTs(catalog, schemaPattern, typeNamePattern, types),
            "Load UDTs", catalog, schemaPattern, typeNamePattern, types);
    }

}

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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * JDBC database metadata managable
 */
public class JDBCDatabaseMetaDataImpl implements JDBCDatabaseMetaData  {

    private JDBCSession connection;
    private DatabaseMetaData original;

    public JDBCDatabaseMetaDataImpl(JDBCSession connection, DatabaseMetaData original)
    {
        this.connection = connection;
        this.original = original;
    }

    public DatabaseMetaData getOriginal() throws SQLException
    {
        if (original == null) {
            throw new SQLException("Database metadata not supported by driver");
        }
        return original;
    }

    private JDBCResultSet makeResultSet(java.sql.ResultSet resultSet, String functionName, Object ... args)
        throws SQLException
    {
        String description = functionName;
        if (args.length > 0) {
            description += " " + Arrays.toString(args);
        }
        return JDBCResultSetImpl.makeResultSet(connection, null, resultSet, description, false);
    }

    @Override
    public JDBCSession getConnection()
    {
        return connection;
    }

    @Override
    public boolean supportsSavepoints()
        throws SQLException
    {
        return getOriginal().supportsSavepoints();
    }

    @Override
    public boolean supportsNamedParameters()
        throws SQLException
    {
        return getOriginal().supportsNamedParameters();
    }

    @Override
    public boolean supportsMultipleOpenResults()
        throws SQLException
    {
        return getOriginal().supportsMultipleOpenResults();
    }

    @Override
    public boolean supportsGetGeneratedKeys()
        throws SQLException
    {
        return getOriginal().supportsGetGeneratedKeys();
    }

    @Override
    public JDBCResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getSuperTypes(catalog, schemaPattern, typeNamePattern),
            "Load super types", catalog, schemaPattern, typeNamePattern);
    }

    @Override
    public JDBCResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getSuperTables(catalog, schemaPattern, tableNamePattern),
            "Load super tables", catalog, schemaPattern, tableNamePattern);
    }

    @Override
    public JDBCResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern),
            "Load UDT attributes", catalog, schemaPattern, typeNamePattern, attributeNamePattern);
    }

    @Override
    public boolean supportsResultSetHoldability(int holdability)
        throws SQLException
    {
        return getOriginal().supportsResultSetHoldability(holdability);
    }

    @Override
    public int getResultSetHoldability()
        throws SQLException
    {
        return getOriginal().getResultSetHoldability();
    }

    @Override
    public int getDatabaseMajorVersion()
        throws SQLException
    {
        return getOriginal().getDatabaseMajorVersion();
    }

    @Override
    public int getDatabaseMinorVersion()
        throws SQLException
    {
        return getOriginal().getDatabaseMinorVersion();
    }

    @Override
    public int getJDBCMajorVersion()
        throws SQLException
    {
        return getOriginal().getJDBCMajorVersion();
    }

    @Override
    public int getJDBCMinorVersion()
        throws SQLException
    {
        return getOriginal().getJDBCMinorVersion();
    }

    @Override
    public int getSQLStateType()
        throws SQLException
    {
        return getOriginal().getSQLStateType();
    }

    @Override
    public boolean locatorsUpdateCopy()
        throws SQLException
    {
        return getOriginal().locatorsUpdateCopy();
    }

    @Override
    public boolean supportsStatementPooling()
        throws SQLException
    {
        return getOriginal().supportsStatementPooling();
    }

    @Override
    public RowIdLifetime getRowIdLifetime()
        throws SQLException
    {
        return getOriginal().getRowIdLifetime();
    }

    @Override
    public JDBCResultSet getSchemas(String catalog, String schemaPattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getSchemas(catalog, schemaPattern),
            "Load schemas", catalog, schemaPattern);
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax()
        throws SQLException
    {
        return getOriginal().supportsStoredFunctionsUsingCallSyntax();
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets()
        throws SQLException
    {
        return getOriginal().autoCommitFailureClosesAllResultSets();
    }

    @Override
    public JDBCResultSet getClientInfoProperties()
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getClientInfoProperties(),
            "Load client info");
    }

    @Override
    public JDBCResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getFunctions(catalog, schemaPattern, functionNamePattern),
            "Load functions", catalog, schemaPattern, functionNamePattern);
    }

    @Override
    public JDBCResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern),
            "Load function columns", catalog, schemaPattern, functionNamePattern, columnNamePattern);
    }

    @Override
    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        return makeResultSet(
            getOriginal().getPseudoColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern),
            "Load pseudo columns", catalog, schemaPattern, tableNamePattern, columnNamePattern);
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        return getOriginal().generatedKeyAlwaysReturned();
    }

    @Override
    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        return getOriginal().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        return getOriginal().isWrapperFor(iface);
    }

    @Override
    public boolean allProceduresAreCallable()
        throws SQLException
    {
        return getOriginal().allProceduresAreCallable();
    }

    @Override
    public boolean allTablesAreSelectable()
        throws SQLException
    {
        return getOriginal().allTablesAreSelectable();
    }

    @Override
    public String getURL()
        throws SQLException
    {
        return getOriginal().getURL();
    }

    @Override
    public String getUserName()
        throws SQLException
    {
        return getOriginal().getUserName();
    }

    @Override
    public boolean isReadOnly()
        throws SQLException
    {
        return getOriginal().isReadOnly();
    }

    @Override
    public boolean nullsAreSortedHigh()
        throws SQLException
    {
        return getOriginal().nullsAreSortedHigh();
    }

    @Override
    public boolean nullsAreSortedLow()
        throws SQLException
    {
        return getOriginal().nullsAreSortedLow();
    }

    @Override
    public boolean nullsAreSortedAtStart()
        throws SQLException
    {
        return getOriginal().nullsAreSortedAtStart();
    }

    @Override
    public boolean nullsAreSortedAtEnd()
        throws SQLException
    {
        return getOriginal().nullsAreSortedAtEnd();
    }

    @Override
    public String getDatabaseProductName()
        throws SQLException
    {
        return getOriginal().getDatabaseProductName();
    }

    @Override
    public String getDatabaseProductVersion()
        throws SQLException
    {
        return getOriginal().getDatabaseProductVersion();
    }

    @Override
    public String getDriverName()
        throws SQLException
    {
        return getOriginal().getDriverName();
    }

    @Override
    public String getDriverVersion()
        throws SQLException
    {
        return getOriginal().getDriverVersion();
    }

    @Override
    public int getDriverMajorVersion()
    {
        return original == null ? 0 : original.getDriverMajorVersion();
    }

    @Override
    public int getDriverMinorVersion()
    {
        return original == null ? 0 : original.getDriverMinorVersion();
    }

    @Override
    public boolean usesLocalFiles()
        throws SQLException
    {
        return getOriginal().usesLocalFiles();
    }

    @Override
    public boolean usesLocalFilePerTable()
        throws SQLException
    {
        return getOriginal().usesLocalFilePerTable();
    }

    @Override
    public boolean supportsMixedCaseIdentifiers()
        throws SQLException
    {
        return getOriginal().supportsMixedCaseIdentifiers();
    }

    @Override
    public boolean storesUpperCaseIdentifiers()
        throws SQLException
    {
        return getOriginal().storesUpperCaseIdentifiers();
    }

    @Override
    public boolean storesLowerCaseIdentifiers()
        throws SQLException
    {
        return getOriginal().storesLowerCaseIdentifiers();
    }

    @Override
    public boolean storesMixedCaseIdentifiers()
        throws SQLException
    {
        return getOriginal().storesMixedCaseIdentifiers();
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers()
        throws SQLException
    {
        return getOriginal().supportsMixedCaseQuotedIdentifiers();
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers()
        throws SQLException
    {
        return getOriginal().storesUpperCaseQuotedIdentifiers();
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers()
        throws SQLException
    {
        return getOriginal().storesLowerCaseQuotedIdentifiers();
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers()
        throws SQLException
    {
        return getOriginal().storesMixedCaseQuotedIdentifiers();
    }

    @Override
    public String getIdentifierQuoteString()
        throws SQLException
    {
        return getOriginal().getIdentifierQuoteString();
    }

    @Override
    public String getSQLKeywords()
        throws SQLException
    {
        return getOriginal().getSQLKeywords();
    }

    @Override
    public String getNumericFunctions()
        throws SQLException
    {
        return getOriginal().getNumericFunctions();
    }

    @Override
    public String getStringFunctions()
        throws SQLException
    {
        return getOriginal().getStringFunctions();
    }

    @Override
    public String getSystemFunctions()
        throws SQLException
    {
        return getOriginal().getSystemFunctions();
    }

    @Override
    public String getTimeDateFunctions()
        throws SQLException
    {
        return getOriginal().getTimeDateFunctions();
    }

    @Override
    public String getSearchStringEscape()
        throws SQLException
    {
        return getOriginal().getSearchStringEscape();
    }

    @Override
    public String getExtraNameCharacters()
        throws SQLException
    {
        return getOriginal().getExtraNameCharacters();
    }

    @Override
    public boolean supportsAlterTableWithAddColumn()
        throws SQLException
    {
        return getOriginal().supportsAlterTableWithAddColumn();
    }

    @Override
    public boolean supportsAlterTableWithDropColumn()
        throws SQLException
    {
        return getOriginal().supportsAlterTableWithDropColumn();
    }

    @Override
    public boolean supportsColumnAliasing()
        throws SQLException
    {
        return getOriginal().supportsColumnAliasing();
    }

    @Override
    public boolean nullPlusNonNullIsNull()
        throws SQLException
    {
        return getOriginal().nullPlusNonNullIsNull();
    }

    @Override
    public boolean supportsConvert()
        throws SQLException
    {
        return getOriginal().supportsConvert();
    }

    @Override
    public boolean supportsConvert(int fromType, int toType)
        throws SQLException
    {
        return getOriginal().supportsConvert(fromType, toType);
    }

    @Override
    public boolean supportsTableCorrelationNames()
        throws SQLException
    {
        return getOriginal().supportsTableCorrelationNames();
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames()
        throws SQLException
    {
        return getOriginal().supportsDifferentTableCorrelationNames();
    }

    @Override
    public boolean supportsExpressionsInOrderBy()
        throws SQLException
    {
        return getOriginal().supportsExpressionsInOrderBy();
    }

    @Override
    public boolean supportsOrderByUnrelated()
        throws SQLException
    {
        return getOriginal().supportsOrderByUnrelated();
    }

    @Override
    public boolean supportsGroupBy()
        throws SQLException
    {
        return getOriginal().supportsGroupBy();
    }

    @Override
    public boolean supportsGroupByUnrelated()
        throws SQLException
    {
        return getOriginal().supportsGroupByUnrelated();
    }

    @Override
    public boolean supportsGroupByBeyondSelect()
        throws SQLException
    {
        return getOriginal().supportsGroupByBeyondSelect();
    }

    @Override
    public boolean supportsLikeEscapeClause()
        throws SQLException
    {
        return getOriginal().supportsLikeEscapeClause();
    }

    @Override
    public boolean supportsMultipleResultSets()
        throws SQLException
    {
        return getOriginal().supportsMultipleResultSets();
    }

    @Override
    public boolean supportsMultipleTransactions()
        throws SQLException
    {
        return getOriginal().supportsMultipleTransactions();
    }

    @Override
    public boolean supportsNonNullableColumns()
        throws SQLException
    {
        return getOriginal().supportsNonNullableColumns();
    }

    @Override
    public boolean supportsMinimumSQLGrammar()
        throws SQLException
    {
        return getOriginal().supportsMinimumSQLGrammar();
    }

    @Override
    public boolean supportsCoreSQLGrammar()
        throws SQLException
    {
        return getOriginal().supportsCoreSQLGrammar();
    }

    @Override
    public boolean supportsExtendedSQLGrammar()
        throws SQLException
    {
        return getOriginal().supportsExtendedSQLGrammar();
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL()
        throws SQLException
    {
        return getOriginal().supportsANSI92EntryLevelSQL();
    }

    @Override
    public boolean supportsANSI92IntermediateSQL()
        throws SQLException
    {
        return getOriginal().supportsANSI92IntermediateSQL();
    }

    @Override
    public boolean supportsANSI92FullSQL()
        throws SQLException
    {
        return getOriginal().supportsANSI92FullSQL();
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility()
        throws SQLException
    {
        return getOriginal().supportsIntegrityEnhancementFacility();
    }

    @Override
    public boolean supportsOuterJoins()
        throws SQLException
    {
        return getOriginal().supportsOuterJoins();
    }

    @Override
    public boolean supportsFullOuterJoins()
        throws SQLException
    {
        return getOriginal().supportsFullOuterJoins();
    }

    @Override
    public boolean supportsLimitedOuterJoins()
        throws SQLException
    {
        return getOriginal().supportsLimitedOuterJoins();
    }

    @Override
    public String getSchemaTerm()
        throws SQLException
    {
        return getOriginal().getSchemaTerm();
    }

    @Override
    public String getProcedureTerm()
        throws SQLException
    {
        return getOriginal().getProcedureTerm();
    }

    @Override
    public String getCatalogTerm()
        throws SQLException
    {
        return getOriginal().getCatalogTerm();
    }

    @Override
    public boolean isCatalogAtStart()
        throws SQLException
    {
        return getOriginal().isCatalogAtStart();
    }

    @Override
    public String getCatalogSeparator()
        throws SQLException
    {
        return getOriginal().getCatalogSeparator();
    }

    @Override
    public boolean supportsSchemasInDataManipulation()
        throws SQLException
    {
        return getOriginal().supportsSchemasInDataManipulation();
    }

    @Override
    public boolean supportsSchemasInProcedureCalls()
        throws SQLException
    {
        return getOriginal().supportsSchemasInProcedureCalls();
    }

    @Override
    public boolean supportsSchemasInTableDefinitions()
        throws SQLException
    {
        return getOriginal().supportsSchemasInTableDefinitions();
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions()
        throws SQLException
    {
        return getOriginal().supportsSchemasInIndexDefinitions();
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions()
        throws SQLException
    {
        return getOriginal().supportsSchemasInPrivilegeDefinitions();
    }

    @Override
    public boolean supportsCatalogsInDataManipulation()
        throws SQLException
    {
        return getOriginal().supportsCatalogsInDataManipulation();
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls()
        throws SQLException
    {
        return getOriginal().supportsCatalogsInProcedureCalls();
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions()
        throws SQLException
    {
        return getOriginal().supportsCatalogsInTableDefinitions();
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions()
        throws SQLException
    {
        return getOriginal().supportsCatalogsInIndexDefinitions();
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions()
        throws SQLException
    {
        return getOriginal().supportsCatalogsInPrivilegeDefinitions();
    }

    @Override
    public boolean supportsPositionedDelete()
        throws SQLException
    {
        return getOriginal().supportsPositionedDelete();
    }

    @Override
    public boolean supportsPositionedUpdate()
        throws SQLException
    {
        return getOriginal().supportsPositionedUpdate();
    }

    @Override
    public boolean supportsSelectForUpdate()
        throws SQLException
    {
        return getOriginal().supportsSelectForUpdate();
    }

    @Override
    public boolean supportsStoredProcedures()
        throws SQLException
    {
        return getOriginal().supportsStoredProcedures();
    }

    @Override
    public boolean supportsSubqueriesInComparisons()
        throws SQLException
    {
        return getOriginal().supportsSubqueriesInComparisons();
    }

    @Override
    public boolean supportsSubqueriesInExists()
        throws SQLException
    {
        return getOriginal().supportsSubqueriesInExists();
    }

    @Override
    public boolean supportsSubqueriesInIns()
        throws SQLException
    {
        return getOriginal().supportsSubqueriesInIns();
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds()
        throws SQLException
    {
        return getOriginal().supportsSubqueriesInQuantifieds();
    }

    @Override
    public boolean supportsCorrelatedSubqueries()
        throws SQLException
    {
        return getOriginal().supportsCorrelatedSubqueries();
    }

    @Override
    public boolean supportsUnion()
        throws SQLException
    {
        return getOriginal().supportsUnion();
    }

    @Override
    public boolean supportsUnionAll()
        throws SQLException
    {
        return getOriginal().supportsUnionAll();
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit()
        throws SQLException
    {
        return getOriginal().supportsOpenCursorsAcrossCommit();
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback()
        throws SQLException
    {
        return getOriginal().supportsOpenCursorsAcrossRollback();
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit()
        throws SQLException
    {
        return getOriginal().supportsOpenStatementsAcrossCommit();
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback()
        throws SQLException
    {
        return getOriginal().supportsOpenStatementsAcrossRollback();
    }

    @Override
    public int getMaxBinaryLiteralLength()
        throws SQLException
    {
        return getOriginal().getMaxBinaryLiteralLength();
    }

    @Override
    public int getMaxCharLiteralLength()
        throws SQLException
    {
        return getOriginal().getMaxCharLiteralLength();
    }

    @Override
    public int getMaxColumnNameLength()
        throws SQLException
    {
        return getOriginal().getMaxColumnNameLength();
    }

    @Override
    public int getMaxColumnsInGroupBy()
        throws SQLException
    {
        return getOriginal().getMaxColumnsInGroupBy();
    }

    @Override
    public int getMaxColumnsInIndex()
        throws SQLException
    {
        return getOriginal().getMaxColumnsInIndex();
    }

    @Override
    public int getMaxColumnsInOrderBy()
        throws SQLException
    {
        return getOriginal().getMaxColumnsInOrderBy();
    }

    @Override
    public int getMaxColumnsInSelect()
        throws SQLException
    {
        return getOriginal().getMaxColumnsInSelect();
    }

    @Override
    public int getMaxColumnsInTable()
        throws SQLException
    {
        return getOriginal().getMaxColumnsInTable();
    }

    @Override
    public int getMaxConnections()
        throws SQLException
    {
        return getOriginal().getMaxConnections();
    }

    @Override
    public int getMaxCursorNameLength()
        throws SQLException
    {
        return getOriginal().getMaxCursorNameLength();
    }

    @Override
    public int getMaxIndexLength()
        throws SQLException
    {
        return getOriginal().getMaxIndexLength();
    }

    @Override
    public int getMaxSchemaNameLength()
        throws SQLException
    {
        return getOriginal().getMaxSchemaNameLength();
    }

    @Override
    public int getMaxProcedureNameLength()
        throws SQLException
    {
        return getOriginal().getMaxProcedureNameLength();
    }

    @Override
    public int getMaxCatalogNameLength()
        throws SQLException
    {
        return getOriginal().getMaxCatalogNameLength();
    }

    @Override
    public int getMaxRowSize()
        throws SQLException
    {
        return getOriginal().getMaxRowSize();
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs()
        throws SQLException
    {
        return getOriginal().doesMaxRowSizeIncludeBlobs();
    }

    @Override
    public int getMaxStatementLength()
        throws SQLException
    {
        return getOriginal().getMaxStatementLength();
    }

    @Override
    public int getMaxStatements()
        throws SQLException
    {
        return getOriginal().getMaxStatements();
    }

    @Override
    public int getMaxTableNameLength()
        throws SQLException
    {
        return getOriginal().getMaxTableNameLength();
    }

    @Override
    public int getMaxTablesInSelect()
        throws SQLException
    {
        return getOriginal().getMaxTablesInSelect();
    }

    @Override
    public int getMaxUserNameLength()
        throws SQLException
    {
        return getOriginal().getMaxUserNameLength();
    }

    @Override
    public int getDefaultTransactionIsolation()
        throws SQLException
    {
        return getOriginal().getDefaultTransactionIsolation();
    }

    @Override
    public boolean supportsTransactions()
        throws SQLException
    {
        return getOriginal().supportsTransactions();
    }

    @Override
    public boolean supportsTransactionIsolationLevel(int level)
        throws SQLException
    {
        return getOriginal().supportsTransactionIsolationLevel(level);
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions()
        throws SQLException
    {
        return getOriginal().supportsDataDefinitionAndDataManipulationTransactions();
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly()
        throws SQLException
    {
        return getOriginal().supportsDataManipulationTransactionsOnly();
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit()
        throws SQLException
    {
        return getOriginal().dataDefinitionCausesTransactionCommit();
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions()
        throws SQLException
    {
        return getOriginal().dataDefinitionIgnoredInTransactions();
    }

    @Override
    public JDBCResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getProcedures(catalog, schemaPattern, procedureNamePattern),
            "Load procedures", catalog, schemaPattern, procedureNamePattern);
    }

    @Override
    public JDBCResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern,
                                         String columnNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern),
            "Load procedure columns", catalog, schemaPattern, procedureNamePattern, columnNamePattern);
    }

    @Override
    public JDBCResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getTables(catalog, schemaPattern, tableNamePattern, types),
            "Load tables", catalog, schemaPattern, tableNamePattern, types);
    }

    @Override
    public JDBCResultSet getSchemas()
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getSchemas(),
            "Load schemas");
    }

    @Override
    public JDBCResultSet getCatalogs()
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getCatalogs(),
            "Load catalogs");
    }

    @Override
    public JDBCResultSet getTableTypes()
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getTableTypes(),
            "Load table types");
    }

    @Override
    public JDBCResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern),
            "Load columns", catalog, schemaPattern, tableNamePattern, columnNamePattern);
    }

    @Override
    public JDBCResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getColumnPrivileges(catalog, schema, table, columnNamePattern),
            "Load column privileges", catalog, schema, table, columnNamePattern);
    }

    @Override
    public JDBCResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getTablePrivileges(catalog, schemaPattern, tableNamePattern),
            "Load table privileges", catalog, schemaPattern, tableNamePattern);
    }

    @Override
    public JDBCResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getBestRowIdentifier(catalog, schema, table, scope, nullable),
            "Find best row identifier", catalog, schema, table);
    }

    @Override
    public JDBCResultSet getVersionColumns(String catalog, String schema, String table)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getVersionColumns(catalog, schema, table),
            "Find version columns", catalog, schema, table);
    }

    @Override
    public JDBCResultSet getPrimaryKeys(String catalog, String schema, String table)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getPrimaryKeys(catalog, schema, table),
            "Load primary keys", catalog, schema, table);
    }

    @Override
    public JDBCResultSet getImportedKeys(String catalog, String schema, String table)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getImportedKeys(catalog, schema, table),
            "Load imported keys", catalog, schema, table);
    }

    @Override
    public JDBCResultSet getExportedKeys(String catalog, String schema, String table)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getExportedKeys(catalog, schema, table),
            "Load exported keys", catalog, schema, table);
    }

    @Override
    public JDBCResultSet getCrossReference(
        String parentCatalog, String parentSchema, String parentTable,
        String foreignCatalog, String foreignSchema, String foreignTable)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable),
            "Load cross reference", parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable);
    }

    @Override
    public JDBCResultSet getTypeInfo()
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getTypeInfo(),
            "Load type info");
    }

    @Override
    public JDBCResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getIndexInfo(catalog, schema, table, unique, approximate),
            "Load indexes", catalog, schema, table);
    }

    @Override
    public boolean supportsResultSetType(int type)
        throws SQLException
    {
        return getOriginal().supportsResultSetType(type);
    }

    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency)
        throws SQLException
    {
        return getOriginal().supportsResultSetConcurrency(type, concurrency);
    }

    @Override
    public boolean ownUpdatesAreVisible(int type)
        throws SQLException
    {
        return getOriginal().ownUpdatesAreVisible(type);
    }

    @Override
    public boolean ownDeletesAreVisible(int type)
        throws SQLException
    {
        return getOriginal().ownDeletesAreVisible(type);
    }

    @Override
    public boolean ownInsertsAreVisible(int type)
        throws SQLException
    {
        return getOriginal().ownInsertsAreVisible(type);
    }

    @Override
    public boolean othersUpdatesAreVisible(int type)
        throws SQLException
    {
        return getOriginal().othersUpdatesAreVisible(type);
    }

    @Override
    public boolean othersDeletesAreVisible(int type)
        throws SQLException
    {
        return getOriginal().othersDeletesAreVisible(type);
    }

    @Override
    public boolean othersInsertsAreVisible(int type)
        throws SQLException
    {
        return getOriginal().othersInsertsAreVisible(type);
    }

    @Override
    public boolean updatesAreDetected(int type)
        throws SQLException
    {
        return getOriginal().updatesAreDetected(type);
    }

    @Override
    public boolean deletesAreDetected(int type)
        throws SQLException
    {
        return getOriginal().deletesAreDetected(type);
    }

    @Override
    public boolean insertsAreDetected(int type)
        throws SQLException
    {
        return getOriginal().insertsAreDetected(type);
    }

    @Override
    public boolean supportsBatchUpdates()
        throws SQLException
    {
        return getOriginal().supportsBatchUpdates();
    }

    @Override
    public JDBCResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types)
        throws SQLException
    {
        return makeResultSet(
            getOriginal().getUDTs(catalog, schemaPattern, typeNamePattern, types),
            "Load UDTs", catalog, schemaPattern, typeNamePattern);
    }

}

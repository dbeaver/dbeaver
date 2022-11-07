/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.List;
import java.util.Map;

/**
 * PostgreServerExtension
 */
public interface PostgreServerExtension {
    String getServerTypeName();

    boolean supportsTransactions();

    boolean supportsOids();

    boolean supportsIndexes();

    boolean supportsMaterializedViews();

    boolean supportsPartitions();

    boolean supportsInheritance();

    boolean supportsTriggers();

    boolean supportsEventTriggers();

    boolean supportsFunctionDefRead();

    boolean supportsFunctionCreate();

    boolean supportsRules();

    boolean supportsRowLevelSecurity();

    boolean supportsExtensions();

    boolean supportsEncodings();

    boolean supportsCollations();

    boolean supportsTablespaces();

    boolean supportsSequences();

    PostgreSequence createSequence(@NotNull PostgreSchema schema);

    boolean supportsRoles();

    boolean supportsSessionActivity();

    boolean supportsLocks();

    boolean supportsForeignServers();

    boolean supportsAggregates();

    boolean supportsResultSetLimits();

    boolean supportsClientInfo();

    boolean supportsRelationSizeCalc();

    boolean supportsExplainPlan();

    boolean supportsExplainPlanXML();

    boolean supportsExplainPlanVerbose();

    boolean supportsDatabaseDescription();

    boolean supportsTemporalAccessor();

    boolean supportsTeblespaceLocation();

    boolean supportsTemplates();

    // Stored procedures support (workarounds for Redshift mostly)
    boolean supportsStoredProcedures();
    String getProceduresSystemTable();
    String getProceduresOidColumn();

    // Table DDL extraction
    String readTableDDL(DBRProgressMonitor monitor, PostgreTableBase table) throws DBException;

    // Custom schema cache.
    JDBCObjectLookupCache<PostgreDatabase, PostgreSchema> createSchemaCache(PostgreDatabase database);

    PostgreTableBase createRelationOfClass(PostgreSchema schema, PostgreClass.RelKind kind, JDBCResultSet dbResult);

    PostgreTableBase createNewRelation(DBRProgressMonitor monitor, PostgreSchema schema, PostgreClass.RelKind kind, Object copyFrom) throws DBException;

    void configureDialect(PostgreDialect dialect);

    String getTableModifiers(DBRProgressMonitor monitor, PostgreTableBase tableBase, boolean alter);

    PostgreTableColumn createTableColumn(DBRProgressMonitor monitor, PostgreSchema schema, PostgreTableBase table, JDBCResultSet dbResult) throws DBException;

    // Initializes SSL config if SSL wasn't enabled explicitly. By default disables SSL explicitly.
    void initDefaultSSLConfig(DBPConnectionConfiguration connectionInfo, Map<String, String> props);

    List<PostgrePrivilege> readObjectPermissions(DBRProgressMonitor monitor, PostgreTableBase object, boolean includeNestedObjects) throws DBException;

    Map<String, String> getDataTypeAliases();

    boolean supportsTableStatistics();

    // True if driver returns source table name in ResultSetMetaData.
    // It works for original PG driver but doesn't work for many forks (e.g. Redshift).
    boolean supportsEntityMetadataInResults();

    /** True if supports special column with check constraint expression */
    boolean supportsPGConstraintExpressionColumn();

    /** True if supports special "Has OIDs" metadata column*/
    boolean supportsHasOidsColumn();

    boolean supportsDatabaseSize();

    boolean isAlterTableAtomic();

    // Roles

    boolean supportsSuperusers();

    boolean supportsRolesWithCreateDBAbility();

    /** True if supports role replication parameter.
     * "A role must have this attribute (or be a superuser) in order to be able to connect to the server in replication mode (physical or logical replication)
     * and in order to be able to create or drop replication slots."
     * */
    boolean supportsRoleReplication();

    /** True if supports role BYPASSRLS parameter.
     * "These clauses determine whether a role bypasses every row-level security (RLS) policy."
     * */
    boolean supportsRoleBypassRLS();

    /**
     * Determines whether the database supports syntax like {@code COMMENT ON ROLE roleName IS 'comment'} or not
     */
    boolean supportsCommentsOnRole();

    // Data types

    /** True if supports serials - serial types are auto-incrementing integer data types */
    boolean supportSerialTypes();

    /** True if supports external types - types from another databases (like Athena). These types in this case will be turned into fake types */
    boolean supportsExternalTypes();

    boolean supportsBackslashStringEscape();

    /** The ability to disable triggers need for the data transfer */
    boolean supportsDisablingAllTriggers();

    /** True if supports table generated columns */
    boolean supportsGeneratedColumns();

    /** True if supports table rowid columns. Rowid columns usually replace primary key in the table */
    boolean isHiddenRowidColumn(@NotNull PostgreAttribute attribute);

    /** Nor all databases support all types of columns. Also, some databases return comments with table DDL from the server-side */
    boolean supportsShowingOfExtraComments();

    boolean supportsKeyAndIndexRename();

    /** Makes it possible to change the name of the user of the current user via UI */
    boolean supportsAlterUserChangePassword();

    /** COPY FROM STDIN is special command for the better table insert performance */
    boolean supportsCopyFromStdIn();

    int getParameterBindType(DBSTypedObject type, Object value);

    /** Necessary for the "Truncate table" tool */
    int getTruncateToolModes();

    boolean supportsDistinctForStatementsWithAcl();

    /** True if supports operator families as access methods (System Info) */
    boolean supportsOpFamily();

    /**
     * Determines whether the database supports syntax
     * like {@code ALTER TABLE tableName ALTER COLUMN columnName USING columnName::dataTypeName} or not
     */
    boolean supportsAlterTableColumnWithUSING();
}

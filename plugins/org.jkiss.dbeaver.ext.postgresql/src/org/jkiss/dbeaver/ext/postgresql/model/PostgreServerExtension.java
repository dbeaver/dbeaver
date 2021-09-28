/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

    boolean supportsFunctionDefRead();

    boolean supportsFunctionCreate();

    boolean supportsRules();

    boolean supportsExtensions();

    boolean supportsEncodings();

    boolean supportsCollations();

    boolean supportsTablespaces();

    boolean supportsSequences();

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

    boolean supportsPGConstraintExpressionColumn();

    boolean supportsHasOidsColumn();

    boolean supportsDatabaseSize();

    boolean isAlterTableAtomic();

    boolean supportsSuperusers();

    boolean supportsRolesWithCreateDBAbility();

    boolean supportSerialTypes();

    boolean supportsExternalTypes();

    boolean supportsBackslashStringEscape();

    boolean supportsDisablingAllTriggers();

    boolean supportsGeneratedColumns();

    boolean supportsKeyAndIndexRename();

    boolean supportsAlterUserChangePassword();

    int getParameterBindType(DBSTypedObject type, Object value);

    int getTruncateToolModes();
}

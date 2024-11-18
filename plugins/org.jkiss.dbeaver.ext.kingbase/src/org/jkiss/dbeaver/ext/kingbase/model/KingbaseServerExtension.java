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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.dpi.DPIElement;
import org.jkiss.dbeaver.model.dpi.DPIObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * KingbaseServerExtension
 */
@DPIObject
@DPIElement
public interface KingbaseServerExtension {
    String getServerTypeName();

    boolean supportsTransactions();

    boolean supportsOids();

    boolean supportsIndexes();

    boolean supportsForeignKeys();

    boolean supportsMaterializedViews();

    boolean supportsPartitions();

    boolean supportsInheritance();

    boolean supportsTriggers();

    boolean supportsEventTriggers();

    boolean supportsDependencies();

    boolean supportsFunctionDefRead();

    boolean supportsFunctionCreate();

    boolean supportsRules();

    boolean supportsRowLevelSecurity();

    boolean supportsExtensions();

    boolean supportsEncodings();

    boolean supportsCollations();

    boolean supportsLanguages();

    boolean supportsTablespaces();

    boolean supportsSequences();

    KingbaseSequence createSequence(@NotNull KingbaseSchema schema);

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

    boolean supportsTablespaceLocation();

    boolean supportsTemplates();

    boolean supportsStoredProcedures();
    String getProceduresSystemTable();
    String getProceduresOidColumn();

    String readTableDDL(DBRProgressMonitor monitor, KingbaseTableBase table) throws DBException;

    String readViewDDL(DBRProgressMonitor monitor, KingbaseViewBase view) throws DBException;

    JDBCObjectLookupCache<KingbaseDatabase, KingbaseSchema> createSchemaCache(KingbaseDatabase database);

    KingbaseTableBase createRelationOfClass(KingbaseSchema schema, KingbaseClass.RelKind kind, JDBCResultSet dbResult);

    KingbaseTableBase createNewRelation(DBRProgressMonitor monitor, KingbaseSchema schema, KingbaseClass.RelKind kind, Object copyFrom) throws DBException;

    void configureDialect(KingbaseDialect dialect);

    String getTableModifiers(DBRProgressMonitor monitor, KingbaseTableBase tableBase, boolean alter);

    void initDefaultSSLConfig(DBPConnectionConfiguration connectionInfo, Map<String, String> props);

    List<KingbasePrivilege> readObjectPermissions(DBRProgressMonitor monitor, KingbaseTableBase object, boolean includeNestedObjects) throws DBException;

    Map<String, String> getDataTypeAliases();

    boolean supportsTableStatistics();

    boolean supportsEntityMetadataInResults();

    boolean supportsKBConstraintExpressionColumn();

    boolean supportsHasOidsColumn();

    boolean supportsColumnsRequiring();

    boolean supportsDatabaseSize();

    boolean isAlterTableAtomic();

    boolean supportsSuperusers();

    boolean supportsRolesWithCreateDBAbility();

    boolean supportsRoleReplication();

    boolean supportsRoleBypassRLS();

    boolean supportsCommentsOnRole();

    boolean supportsDefaultPrivileges();

    boolean supportSerialTypes();

    boolean supportsExternalTypes();

    boolean supportsBackslashStringEscape();

    boolean supportsDisablingAllTriggers();

    boolean supportsGeneratedColumns();

    boolean isHiddenRowidColumn(@NotNull KingbaseAttribute attribute);

    boolean supportsShowingOfExtraComments();

    boolean supportsKeyAndIndexRename();

    boolean supportsAlterUserChangePassword();

    boolean supportsCopyFromStdIn();

    int getParameterBindType(DBSTypedObject type, Object value);

    int getTruncateToolModes();

    boolean supportsAcl();

    boolean supportsCustomDataTypes();

    boolean supportsDistinctForStatementsWithAcl();

    boolean supportsOpFamily();

    boolean supportsAlterTableColumnWithUSING();

    boolean supportsAlterTableForViewRename();

    boolean supportsNativeClient();

}

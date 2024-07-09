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
package org.jkiss.dbeaver.ext.postgresql.model.impls;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * PostgreServerExtensionBase
 */
public abstract class PostgreServerExtensionBase implements PostgreServerExtension {

    public static final int TRUNCATE_TOOL_MODE_SUPPORT_ONLY_ONE_TABLE = 1;
    public static final int TRUNCATE_TOOL_MODE_SUPPORT_IDENTITIES = 1 << 1;
    public static final int TRUNCATE_TOOL_MODE_SUPPORT_CASCADE = 1 << 2;

    private static final Log log = Log.getLog(PostgreServerExtensionBase.class);

    protected final PostgreDataSource dataSource;

    protected PostgreServerExtensionBase(PostgreDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean supportsTransactions() {
        return true;
    }

    @Override
    public boolean supportsOids() {
        return true;
    }

    @Override
    public boolean supportsIndexes() {
        return true;
    }

    @Override
    public boolean supportsForeignKeys() {
        return true;
    }

    @Override
    public boolean supportsMaterializedViews() {
        return dataSource.isServerVersionAtLeast(9, 3);
    }

    @Override
    public boolean supportsPartitions() {
        return dataSource.isServerVersionAtLeast(10, 0);
    }

    @Override
    public boolean supportsInheritance() {
        return true;
    }

    @Override
    public boolean supportsTriggers() {
        return true;
    }

    @Override
    public boolean supportsEventTriggers() {
        return false;
    }

    @Override
    public boolean supportsDependencies() {
        return true;
    }

    @Override
    public boolean supportsFunctionCreate() {
        return true;
    }

    @Override
    public boolean supportsRules() {
        return true;
    }

    @Override
    public boolean supportsRowLevelSecurity() {
        return false;
    }

    @Override
    public boolean supportsExtensions() {
        return dataSource.isServerVersionAtLeast(9, 1);
    }

    @Override
    public boolean supportsEncodings() {
        return true;
    }

    @Override
    public boolean supportsCollations() {
        return dataSource.isServerVersionAtLeast(9, 1);
    }

    @Override
    public boolean supportsLanguages() {
        return true;
    }

    @Override
    public boolean supportsTablespaces() {
        return true;
    }

    @Override
    public boolean supportsSequences() {
        return true;//dataSource.isServerVersionAtLeast(10, 0);
    }

    @Override
    public PostgreSequence createSequence(@NotNull PostgreSchema schema) {
        return new PostgreSequence(schema);
    }

    @Override
    public boolean supportsRoles() {
        return dataSource.isServerVersionAtLeast(8, 1);
    }

    @Override
    public boolean supportsSessionActivity() {
        return dataSource.isServerVersionAtLeast(9, 3);
    }

    @Override
    public boolean supportsLocks() {
        return dataSource.isServerVersionAtLeast(9, 3);
    }

    @Override
    public boolean supportsForeignServers() {
        return dataSource.isServerVersionAtLeast(8, 4);
    }

    @Override
    public boolean supportsAggregates() {
        return true;
    }

    @Override
    public boolean supportsResultSetLimits() {
        return true;
    }

    @Override
    public boolean supportsClientInfo() {
        return true;
    }

    @Override
    public String readTableDDL(DBRProgressMonitor monitor, PostgreTableBase table) throws DBException {
        return null;
    }

    @Override
    public String readViewDDL(DBRProgressMonitor monitor, PostgreViewBase view) throws DBException {
        return null;
    }

    @Override
    public boolean supportsTemplates() {
        return true;
    }

    @Override
    public PostgreDatabase.SchemaCache createSchemaCache(PostgreDatabase database) {
        return new PostgreDatabase.SchemaCache();
    }

    @Override
    public PostgreTableBase createRelationOfClass(PostgreSchema schema, PostgreClass.RelKind kind, JDBCResultSet dbResult) {
        if (kind == PostgreClass.RelKind.r) {
            return new PostgreTableRegular(schema, dbResult);
        } else if (kind == PostgreClass.RelKind.R) {
                return new PostgreTablePartition(schema, dbResult);    
        } else if (kind == PostgreClass.RelKind.v) {
            return new PostgreView(schema, dbResult);
        } else if (kind == PostgreClass.RelKind.m) {
            return new PostgreMaterializedView(schema, dbResult);
        } else if (kind == PostgreClass.RelKind.f) {
            return new PostgreTableForeign(schema, dbResult);
        } else if (kind == PostgreClass.RelKind.S) {
            return new PostgreSequence(schema, dbResult);
        } else if (kind == PostgreClass.RelKind.t) {
            return new PostgreTableRegular(schema, dbResult);
        } else if (kind == PostgreClass.RelKind.p) {
            return new PostgreTableRegular(schema, dbResult);
        } else {
            log.debug("Unsupported PG class: '" + kind + "'");
            return null;
        }
    }

    @Override
    public PostgreTableBase createNewRelation(DBRProgressMonitor monitor, PostgreSchema schema, PostgreClass.RelKind kind, Object copyFrom) throws DBException {
        if (kind == PostgreClass.RelKind.v) {
            return new PostgreView(schema);
        } else if (kind == PostgreClass.RelKind.m) {
            return new PostgreMaterializedView(schema);
        } else if (kind == PostgreClass.RelKind.f) {
            return new PostgreTableForeign(schema);
        } else if (kind == PostgreClass.RelKind.S) {
            return new PostgreSequence(schema);
        } else {
            if (copyFrom instanceof PostgreTableRegular) {
                return new PostgreTableRegular(monitor, schema, (PostgreTableRegular) copyFrom);
            }
            return new PostgreTableRegular(schema);
        }
    }

    @Override
    public boolean supportsRelationSizeCalc() {
        return true;
    }

    @Override
    public boolean supportsFunctionDefRead() {
        return dataSource.isServerVersionAtLeast(8, 4);
    }


    @Override
    public void configureDialect(PostgreDialect dialect) {

    }

    @Override
    public String getTableModifiers(DBRProgressMonitor monitor, PostgreTableBase tableBase, boolean alter) {
        StringBuilder ddl = new StringBuilder();
        if (tableBase instanceof PostgreTable) {
            PostgreTable table = (PostgreTable) tableBase;
            if (!alter) {
                try {
                    final List<PostgreTableInheritance> superTables = table.getSuperInheritance(monitor);
                    if (!CommonUtils.isEmpty(superTables) && ! tableBase.isPartition()) {
                        ddl.append("\nINHERITS (");
                        for (int i = 0; i < superTables.size(); i++) {
                            if (i > 0) ddl.append(",");
                            ddl.append(superTables.get(i).getAssociatedEntity().getFullyQualifiedName(DBPEvaluationContext.DDL));
                        }
                        ddl.append(")");
                    }
                } catch (DBException e) {
                    log.error(e);
                }
                if (!CommonUtils.isEmpty(table.getPartitionKey())) {
                    ddl.append("\nPARTITION BY ").append(table.getPartitionKey());
                }
            }
            if (tableBase instanceof PostgreTablePartition && !alter) {
                String expression = ((PostgreTablePartition) tableBase).getPartitionExpression();
                if (CommonUtils.isNotEmpty(expression)) {
                    ddl.append(" ").append(expression);
                }
            }
        }

        if (tableBase instanceof PostgreTableRegular) {
            if (!alter) {
                createUsingClause((PostgreTableRegular) tableBase, ddl);
            }
        }

        if (tableBase instanceof PostgreTableRegular) {
            PostgreTableRegular table = (PostgreTableRegular) tableBase;
            try {
                if (!alter) {
                    ddl.append(createWithClause(table, tableBase));
                }
                boolean hasOtherSpecs = false;
                if (table.isTablespaceSpecified()) {
                    PostgreTablespace tablespace = table.getTablespace(monitor);
                    if (tablespace != null) {
                        if (!alter) {
                            ddl.append("\nTABLESPACE ").append(tablespace.getName());
                        }
                        hasOtherSpecs = true;
                    }
                }
                if (!alter && hasOtherSpecs) {
                    ddl.append("\n");
                }
            } catch (DBException e) {
                log.error(e);
            }
        } else if (tableBase instanceof PostgreTableForeign) {
            PostgreTableForeign table = (PostgreTableForeign)tableBase;
            try {
                String foreignServerName = table.getForeignServerName();
                if (CommonUtils.isEmpty(foreignServerName)) {
                    PostgreForeignServer foreignServer = table.getForeignServer(monitor);
                    if (foreignServer != null) {
                        foreignServerName = DBUtils.getQuotedIdentifier(foreignServer);
                    }
                }
                if (foreignServerName != null ) {
                    ddl.append("\nSERVER ").append(foreignServerName);
                }
                String[] foreignOptions = table.getForeignOptions(monitor);
                if (!ArrayUtils.isEmpty(foreignOptions)) {
                    ddl.append("\nOPTIONS ").append(PostgreUtils.getOptionsString(foreignOptions));
                }
            } catch (DBException e) {
                log.error(e);
            }
        }
        tableBase.appendTableModifiers(monitor, ddl);

        return ddl.toString();
    }

    @Override
    public void initDefaultSSLConfig(DBPConnectionConfiguration connectionInfo, Map<String, String> props) {
        if (connectionInfo.getProperty(PostgreConstants.PROP_SSL) == null) {
            // We need to disable SSL explicitly (see #4928)
            props.put(PostgreConstants.PROP_SSL, "false");
        }
    }

    @Override
    public List<PostgrePrivilege> readObjectPermissions(DBRProgressMonitor monitor, PostgreTableBase object, boolean includeNestedObjects) throws DBException {
        List<PostgrePrivilege> tablePermissions = PostgreUtils.extractPermissionsFromACL(monitor, object, object.getAcl(), false);
        if (!includeNestedObjects) {
            return tablePermissions;
        }
        tablePermissions = new ArrayList<>(tablePermissions);
        for (PostgreTableColumn column : CommonUtils.safeCollection(object.getAttributes(monitor))) {
            if (column.getAcl() == null || column.isHidden()) {
                continue;
            }
            tablePermissions.addAll(column.getPrivileges(monitor, true));
        }

        return tablePermissions;
    }

    @Override
    public Map<String, String> getDataTypeAliases() {
        return PostgreConstants.DATA_TYPE_ALIASES;
    }

    @Override
    public boolean supportsTableStatistics() {
        return true;
    }

    @Override
    public boolean supportsEntityMetadataInResults() {
        return false;
    }

    @Override
    public boolean supportsExplainPlan() {
        return true;
    }

    @Override
    public boolean supportsExplainPlanXML() {
        return dataSource.isServerVersionAtLeast(9, 0);
    }

    @Override
    public boolean supportsExplainPlanVerbose() {
        return true;
    }

    @Override
    public boolean supportsDatabaseDescription() {
        return dataSource.isServerVersionAtLeast(9, 4);
    }

    @Override
    public boolean supportsTemporalAccessor() {
        // Disable temporal accessor (which stands for java.util.Date).
        // It doesn't make sense as PG server doesn't support timezones.
        // Everything is in UTC.
        return false;
    }

    @Override
    public boolean supportsTablespaceLocation() {
        return dataSource.isServerVersionAtLeast(9, 2);
    }

    @Override
    public boolean supportsStoredProcedures() {
        return dataSource.isServerVersionAtLeast(11, 0);
    }

    @Override
    public String getProceduresSystemTable() {
        return "pg_proc";
    }

    @Override
    public String getProceduresOidColumn() {
        return "oid";
    }

    public String createWithClause(PostgreTableRegular table, PostgreTableBase tableBase) {
        StringBuilder withClauseBuilder = new StringBuilder();

        boolean hasExtraOptions = dataSource.isServerVersionAtLeast(8, 2) && table.getRelOptions() != null;
        boolean tableSupportOids = table.getDataSource().getServerType().supportsOids() && table.isHasOids() && table.getDataSource().getServerType().supportsHasOidsColumn();

        List<String> extraOptions = new ArrayList<>();

        if (tableSupportOids) {
            extraOptions.add("OIDS=TRUE");
        }
        if (hasExtraOptions) {
            extraOptions.addAll(Arrays.asList(table.getRelOptions()));
        }

        if (!CommonUtils.isEmpty(extraOptions)) {
            withClauseBuilder.append("\nWITH (");
            for (int i = 0; i < extraOptions.size(); i++) {
                if (i > 0) {
                    withClauseBuilder.append(",");
                }
                withClauseBuilder.append("\n\t");
                withClauseBuilder.append(extraOptions.get(i));
            }
            withClauseBuilder.append("\n)");
        }

        return withClauseBuilder.toString();
    }

    public void createUsingClause(@NotNull PostgreTableRegular table, @NotNull StringBuilder ddl) {
        // Do nothing
    }

    @Override
    public boolean supportsPGConstraintExpressionColumn() {
        return true;
    }

    @Override
    public boolean supportsHasOidsColumn() {
        return true;
    }

    @Override
    public boolean supportsColumnsRequiring() {
        return true;
    }

    @Override
    public boolean supportsDatabaseSize() {
        return false;
    }

    @Override
    public boolean isAlterTableAtomic() {
        return false;
    }

    @Override
    public boolean supportsSuperusers() {
        return true;
    }

    @Override
    public boolean supportsRolesWithCreateDBAbility() {
        return supportsRoles();
    }

    @Override
    public boolean supportsRoleReplication() {
        return dataSource.isServerVersionAtLeast(9, 1);
    }

    @Override
    public boolean supportsRoleBypassRLS() {
        return dataSource.isServerVersionAtLeast(9, 5);
    }

    @Override
    public boolean supportsCommentsOnRole() {
        return supportsRoles();
    }

    @Override
    public boolean supportsDefaultPrivileges() {
        return dataSource.isServerVersionAtLeast(9, 0);
    }

    @Override
    public boolean supportSerialTypes() {
        return true;
    }

    @Override
    public boolean supportsExternalTypes() {
        return false;
    }

    @Override
    public boolean supportsBackslashStringEscape() {
        return false;
    }

    @Override
    public boolean supportsDisablingAllTriggers() {
        return false;
    }

    @Override
    public boolean supportsGeneratedColumns() {
        return false;
    }

    @Override
    public boolean isHiddenRowidColumn(@NotNull PostgreAttribute attribute) {
        return false;
    }

    @Override
    public boolean supportsShowingOfExtraComments() {
        return true;
    }

    @Override
    public boolean supportsKeyAndIndexRename() {
        return false;
    }

    @Override
    public boolean supportsAlterUserChangePassword() {
        return false;
    }

    @Override
    public boolean supportsCopyFromStdIn() {
        return false;
    }

    @Override
    public int getParameterBindType(DBSTypedObject type, Object value) {
        return Types.OTHER;
    }

    @Override
    public int getTruncateToolModes() {
        return TRUNCATE_TOOL_MODE_SUPPORT_ONLY_ONE_TABLE | TRUNCATE_TOOL_MODE_SUPPORT_IDENTITIES | TRUNCATE_TOOL_MODE_SUPPORT_CASCADE;
    }

    @Override
    public boolean supportsAcl() {
        return true;
    }

    @Override
    public boolean supportsCustomDataTypes() {
        return true;
    }

    @Override
    public boolean supportsDistinctForStatementsWithAcl() {
        return true;
    }

    @Override
    public boolean supportsOpFamily() {
        return  dataSource.isServerVersionAtLeast(8, 3);
    }

    @Override
    public boolean supportsAlterTableColumnWithUSING() {
        return dataSource.isServerVersionAtLeast(8, 0);
    }

    @Override
    public boolean supportsAlterTableForViewRename() {
        return false;
    }

    @Override
    public boolean supportsNativeClient() {
        return true;
    }
}

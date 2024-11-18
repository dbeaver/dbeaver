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
package org.jkiss.dbeaver.ext.kingbase.model.impls;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseAttribute;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseClass;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataSource;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDatabase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDialect;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseMaterializedView;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseSchema;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseSequence;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseServerExtension;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTable;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableBase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableColumn;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableInheritance;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTablePartition;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableRegular;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTablespace;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseView;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseViewBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseServerExtensionBase
 */
public abstract class KingbaseServerExtensionBase implements KingbaseServerExtension {

    public static final int TRUNCATE_TOOL_MODE_SUPPORT_ONLY_ONE_TABLE = 1;
    public static final int TRUNCATE_TOOL_MODE_SUPPORT_IDENTITIES = 1 << 1;
    public static final int TRUNCATE_TOOL_MODE_SUPPORT_CASCADE = 1 << 2;

    private static final Log log = Log.getLog(KingbaseServerExtensionBase.class);

    protected final KingbaseDataSource dataSource;

    protected KingbaseServerExtensionBase(KingbaseDataSource dataSource) {
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
        return true;
    }

    @Override
    public boolean supportsPartitions() {
        return true;
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
        return true;
    }

    @Override
    public boolean supportsEncodings() {
        return true;
    }

    @Override
    public boolean supportsCollations() {
        return true;
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
        return true;
    }

    @Override
    public KingbaseSequence createSequence(@NotNull KingbaseSchema schema) {
        return new KingbaseSequence(schema);
    }

    @Override
    public boolean supportsRoles() {
        return true;
    }

    @Override
    public boolean supportsSessionActivity() {
        return true;
    }

    @Override
    public boolean supportsLocks() {
        return true;
    }

    @Override
    public boolean supportsForeignServers() {
        return true;
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
    public String readTableDDL(DBRProgressMonitor monitor, KingbaseTableBase table) throws DBException {
        return null;
    }

    @Override
    public String readViewDDL(DBRProgressMonitor monitor, KingbaseViewBase view) throws DBException {
        return null;
    }

    @Override
    public boolean supportsTemplates() {
        return true;
    }

    @Override
    public KingbaseDatabase.SchemaCache createSchemaCache(KingbaseDatabase database) {
        return new KingbaseDatabase.SchemaCache();
    }

    @Override
    public KingbaseTableBase createRelationOfClass(KingbaseSchema schema, KingbaseClass.RelKind kind, JDBCResultSet dbResult) {
        if (kind == KingbaseClass.RelKind.r) {
            return new KingbaseTableRegular(schema, dbResult);
        } else if (kind == KingbaseClass.RelKind.R) {
                return new KingbaseTablePartition(schema, dbResult);    
        } else if (kind == KingbaseClass.RelKind.v) {
            return new KingbaseView(schema, dbResult);
        } else if (kind == KingbaseClass.RelKind.m) {
            return new KingbaseMaterializedView(schema, dbResult);
        } else if (kind == KingbaseClass.RelKind.S) {
            return new KingbaseSequence(schema, dbResult);
        } else if (kind == KingbaseClass.RelKind.t) {
            return new KingbaseTableRegular(schema, dbResult);
        } else if (kind == KingbaseClass.RelKind.p) {
            return new KingbaseTableRegular(schema, dbResult);
        } else {
            log.debug("Unsupported PG class: '" + kind + "'");
            return null;
        }
    }

    @Override
    public KingbaseTableBase createNewRelation(DBRProgressMonitor monitor, KingbaseSchema schema, KingbaseClass.RelKind kind, Object copyFrom) throws DBException {
        if (kind == KingbaseClass.RelKind.v) {
            return new KingbaseView(schema);
        } else if (kind == KingbaseClass.RelKind.m) {
            return new KingbaseMaterializedView(schema);
        } else if (kind == KingbaseClass.RelKind.S) {
            return new KingbaseSequence(schema);
        } else {
            if (copyFrom instanceof KingbaseTableRegular) {
                return new KingbaseTableRegular(monitor, schema, (KingbaseTableRegular) copyFrom);
            }
            return new KingbaseTableRegular(schema);
        }
    }

    @Override
    public boolean supportsRelationSizeCalc() {
        return true;
    }

    @Override
    public boolean supportsFunctionDefRead() {
        return true;
    }


    @Override
    public void configureDialect(KingbaseDialect dialect) {

    }

    @Override
    public String getTableModifiers(DBRProgressMonitor monitor, KingbaseTableBase tableBase, boolean alter) {
        StringBuilder ddl = new StringBuilder();
        if (tableBase instanceof KingbaseTable) {
            KingbaseTable table = (KingbaseTable) tableBase;
            if (!alter) {
                try {
                    final List<KingbaseTableInheritance> superTables = table.getSuperInheritance(monitor);
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
            if (tableBase instanceof KingbaseTablePartition && !alter) {
                String expression = ((KingbaseTablePartition) tableBase).getPartitionExpression();
                if (CommonUtils.isNotEmpty(expression)) {
                    ddl.append(" ").append(expression);
                }
            }
        }

        if (tableBase instanceof KingbaseTableRegular) {
            if (!alter) {
                createUsingClause((KingbaseTableRegular) tableBase, ddl);
            }
        }

        if (tableBase instanceof KingbaseTableRegular) {
            KingbaseTableRegular table = (KingbaseTableRegular) tableBase;
            try {
                if (!alter) {
                    ddl.append(createWithClause(table, tableBase));
                }
                boolean hasOtherSpecs = false;
                if (table.isTablespaceSpecified()) {
                    KingbaseTablespace tablespace = table.getTablespace(monitor);
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
        }
        tableBase.appendTableModifiers(monitor, ddl);

        return ddl.toString();
    }

    @Override
    public void initDefaultSSLConfig(DBPConnectionConfiguration connectionInfo, Map<String, String> props) {
        if (connectionInfo.getProperty(KingbaseConstants.PROP_SSL) == null) {
            props.put(KingbaseConstants.PROP_SSL, "false");
        }
    }

    @Override
    public List<KingbasePrivilege> readObjectPermissions(DBRProgressMonitor monitor, KingbaseTableBase object, boolean includeNestedObjects) throws DBException {
        List<KingbasePrivilege> tablePermissions = KingbaseUtils.extractPermissionsFromACL(monitor, object, object.getAcl(), false);
        if (!includeNestedObjects) {
            return tablePermissions;
        }
        tablePermissions = new ArrayList<>(tablePermissions);
        for (KingbaseTableColumn column : CommonUtils.safeCollection(object.getAttributes(monitor))) {
            if (column.getAcl() == null || column.isHidden()) {
                continue;
            }
            tablePermissions.addAll(column.getPrivileges(monitor, true));
        }

        return tablePermissions;
    }

    @Override
    public Map<String, String> getDataTypeAliases() {
        return KingbaseConstants.DATA_TYPE_ALIASES;
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
        return true;
    }

    @Override
    public boolean supportsExplainPlanVerbose() {
        return true;
    }

    @Override
    public boolean supportsDatabaseDescription() {
        return true;
    }

    @Override
    public boolean supportsTemporalAccessor() {
        return false;
    }

    @Override
    public boolean supportsTablespaceLocation() {
        return true;
    }

    @Override
    public boolean supportsStoredProcedures() {
        return true;
    }

    @Override
    public String getProceduresSystemTable() {
        return "sys_proc";
    }

    @Override
    public String getProceduresOidColumn() {
        return "oid";
    }

    public String createWithClause(KingbaseTableRegular table, KingbaseTableBase tableBase) {
        StringBuilder withClauseBuilder = new StringBuilder();

        boolean hasExtraOptions = table.getRelOptions() != null;
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

    public void createUsingClause(@NotNull KingbaseTableRegular table, @NotNull StringBuilder ddl) {
    }

    @Override
    public boolean supportsKBConstraintExpressionColumn() {
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
        return true;
    }

    @Override
    public boolean supportsRoleBypassRLS() {
        return true;
    }

    @Override
    public boolean supportsCommentsOnRole() {
        return supportsRoles();
    }

    @Override
    public boolean supportsDefaultPrivileges() {
        return true;
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
    public boolean isHiddenRowidColumn(@NotNull KingbaseAttribute attribute) {
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
        return  true;
    }

    @Override
    public boolean supportsAlterTableColumnWithUSING() {
        return true;
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

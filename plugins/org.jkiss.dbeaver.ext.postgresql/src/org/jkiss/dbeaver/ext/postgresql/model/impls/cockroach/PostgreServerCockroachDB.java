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
package org.jkiss.dbeaver.ext.postgresql.model.impls.cockroach;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerExtensionBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * PostgreServerCockroachDB
 */
public class PostgreServerCockroachDB extends PostgreServerExtensionBase {

    public PostgreServerCockroachDB(PostgreDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String getServerTypeName() {
        return "CockroachDB";
    }

    @Override
    public PostgreTableBase createRelationOfClass(PostgreSchema schema, PostgreClass.RelKind kind, JDBCResultSet dbResult) {
        if (kind == PostgreClass.RelKind.S) {
            return new CockroachSequence(schema, dbResult);
        }
        return super.createRelationOfClass(schema, kind, dbResult);
    }

    @Override
    public PostgreTableBase createNewRelation(DBRProgressMonitor monitor, PostgreSchema schema, PostgreClass.RelKind kind, Object copyFrom)
        throws DBException {
        if (kind == PostgreClass.RelKind.S) {
            return new CockroachSequence(schema);
        }
        return super.createNewRelation(monitor, schema, kind, copyFrom);
    }

    @Override
    public PostgreSequence createSequence(@NotNull PostgreSchema schema) {
        return new CockroachSequence(schema);
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
    public boolean supportsMaterializedViews() {
        return false;
    }

    @Override
    public boolean supportsPartitions() {
        return false;
    }

    @Override
    public boolean supportsInheritance() {
        return false;
    }

    @Override
    public boolean supportsTriggers() {
        return true;
    }

    @Override
    public boolean supportsFunctionCreate() {
        return false;
    }

    @Override
    public boolean supportsRules() {
        return false;
    }

    @Override
    public boolean supportsExtensions() {
        return false;
    }

    @Override
    public boolean supportsEncodings() {
        return false;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    @Override
    public boolean supportsSequences() {
        return true;
    }

    @Override
    public boolean supportsRoles() {
        return true;
    }

    @Override
    public boolean supportsSessionActivity() {
        return false;
    }

    @Override
    public boolean supportsLocks() {
        return false;
    }

    @Override
    public boolean supportsForeignServers() {
        return false;
    }

    @Override
    public boolean supportsAggregates() {
        return false;
    }

    @Override
    public boolean supportsRelationSizeCalc() {
        return false;
    }

    @Override
    public boolean supportsFunctionDefRead() {
        return false;
    }

    @Override
    public boolean supportsExplainPlan() {
        return false;
    }

    @Override
    public boolean supportsExplainPlanVerbose() {
        return false;
    }

    @Override
    public boolean supportsTablespaceLocation() {
        return false;
    }

    @Override
    public boolean supportsExplainPlanXML() {
        return false;
    }

    @Override
    public List<PostgrePrivilege> readObjectPermissions(DBRProgressMonitor monitor, PostgreTableBase table, boolean includeNestedObjects) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Load CockroachDB table grants")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW GRANTS ON " + table.getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    List<PostgrePrivilege> permissions = new ArrayList<>();
                    Map<String, List<PostgrePrivilegeGrant>> privilegeMap = new HashMap<>();
                    while (resultSet.next()) {
                        String databaseName = JDBCUtils.safeGetString(resultSet, "database_name");
                        String schemaName = JDBCUtils.safeGetString(resultSet, "schema_name");
                        String tableName = JDBCUtils.safeGetString(resultSet, "table_name");
                        String grantee = JDBCUtils.safeGetString(resultSet, "grantee");
                        String privilege = JDBCUtils.safeGetString(resultSet, "privilege_type");
                        List<PostgrePrivilegeGrant> privList = privilegeMap.computeIfAbsent(grantee, k -> new ArrayList<>());
                        PostgrePrivilegeType privType = CommonUtils.valueOf(PostgrePrivilegeType.class, privilege, PostgrePrivilegeType.UNKNOWN);
                        assert grantee != null;
                        privList.add(new PostgrePrivilegeGrant(null, new PostgreRoleReference(table.getDatabase(), grantee, null),
                            databaseName, schemaName, tableName, privType, true, true
                        ));
                    }
                    for (Map.Entry<String, List<PostgrePrivilegeGrant>> entry : privilegeMap.entrySet()) {
                        PostgrePrivilege permission = new PostgreObjectPrivilege(table, new PostgreRoleReference(table.getDatabase(), entry.getKey(), null), entry.getValue());
                        permissions.add(permission);
                    }
                    return permissions;
                }
            }
        } catch (Exception e) {
            throw new DBDatabaseException(e, table.getDataSource());
        }
    }

    @Override
    public Map<String, String> getDataTypeAliases() {
        Map<String, String> aliasMap = new LinkedHashMap<>(super.getDataTypeAliases());
        aliasMap.put("string", "text");
        aliasMap.put("bytes", "bytea");
        aliasMap.put("decimal", "numeric");
        aliasMap.put("dec", "numeric");
        aliasMap.put("float", "float8");
        return aliasMap;
    }

    @Override
    public boolean supportsTableStatistics() {
        return false;
    }

    @Override
    public String readTableDDL(DBRProgressMonitor monitor, PostgreTableBase table) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Load CockroachDB table DDL")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW CREATE TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (resultSet.next()) {
                        String line = JDBCUtils.safeGetString(resultSet, "CreateTable");
                        if (line == null) {
                            line = JDBCUtils.safeGetString(resultSet, 2);
                        }
                        if (line == null) {
                            continue;
                        }
                        sql.append(line);
                    }
                    return sql.toString();
                }
            }
        } catch (Exception e) {
            throw new DBDatabaseException(e, table.getDataSource());
        }
    }

    @Override
    public boolean supportsSuperusers() {
        return false;
    }

    @Override
    public boolean supportsRolesWithCreateDBAbility() {
        return false;
    }

    @Override
    public boolean supportsRoleBypassRLS() {
        return false;
    }

    @Override
    public boolean supportsRoleReplication() {
        return false;
    }

    @Override
    public boolean supportsCommentsOnRole() {
        return false;
    }

    @Override
    public boolean supportsKeyAndIndexRename() {
        return true;
    }

    @Override
    public boolean supportsAlterUserChangePassword() {
        return true;
    }

    @Override
    public boolean supportsCopyFromStdIn() {
        return true;
    }

    @Override
    public int getTruncateToolModes() {
        return TRUNCATE_TOOL_MODE_SUPPORT_ONLY_ONE_TABLE | TRUNCATE_TOOL_MODE_SUPPORT_CASCADE;
    }

    @Override
    public boolean isHiddenRowidColumn(@NotNull PostgreAttribute attribute) {
        String defaultValue = attribute.getDefaultValue();
        if (CommonUtils.isNotEmpty(defaultValue)) {
            // Rowid column is a special case in Cockroach #14557
            // Rowid column is hidden from DDL (we read it from Cocroach) and from the data viewer.
            // It will added automatically after table creation without keys.
            // But you can create rowid column by yourself with no restrictions, therefore, conditions below may accidentally hide the user column.
            // Let's hope that users do not create such columns independently
            return attribute.isRequired() && "unique_rowid()".equals(defaultValue) && "rowid".equals(attribute.getName());
        }
        return false;
    }

    @Override
    public boolean supportsShowingOfExtraComments() {
        return false;
    }
}

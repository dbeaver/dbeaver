/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerExtensionBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

/**
 * PostgreServerGreenplum
 */
public class PostgreServerGreenplum extends PostgreServerExtensionBase {

    public PostgreServerGreenplum(PostgreDataSource dataSource) {
        super(dataSource);
    }

    @NotNull
    @Override
    public String getServerTypeName() {
        return "Greenplum";
    }

    @Override
    public boolean supportsFunctionDefRead() {
        return dataSource.isServerVersionAtLeast(12, 0);
    }

    @Override
    public boolean supportsSessionActivity() {
        return true;
    }

    @Nullable
    @Override
    public PostgreTableBase createRelationOfClass(@NotNull PostgreSchema schema, @NotNull PostgreClass.RelKind kind, @NotNull JDBCResultSet dbResult) {
        if (kind == PostgreClass.RelKind.r || kind == PostgreClass.RelKind.p) {
            if (isRelationExternal(dbResult)) {
                return new GreenplumExternalTable(schema, dbResult);
            }
            return new GreenplumTable(schema, dbResult);
        } else if (kind == PostgreClass.RelKind.m) {
            return new GreenplumMaterializedView(schema, dbResult);
        } else if (kind == PostgreClass.RelKind.f && isRelationExternal(dbResult)) {
            // Greenplum 7 / Cloudberry: external tables are stored as foreign tables
            // backed by the gp_exttable_fdw FDW. Surface them under the External Tables folder.
            return new GreenplumExternalTable(schema, dbResult);
        }
        return super.createRelationOfClass(schema, kind, dbResult);
    }

    @NotNull
    @Override
    public PostgreTableBase createNewRelation(@NotNull DBRProgressMonitor monitor, @NotNull PostgreSchema schema, @NotNull PostgreClass.RelKind kind, @Nullable
    Object copyFrom) throws DBException {
        if (kind == PostgreClass.RelKind.r || kind == PostgreClass.RelKind.p) {
            return new GreenplumTable(schema);
        } else if (kind == PostgreClass.RelKind.m) {
            return new GreenplumMaterializedView(schema);
        }
        return super.createNewRelation(monitor, schema, kind, copyFrom);
    }

    private boolean isRelationExternal(JDBCResultSet dbResult) {
        return JDBCUtils.safeGetBoolean(dbResult, "is_ext_table");
    }

    @NotNull
    @Override
    public PostgreDatabase.SchemaCache createSchemaCache(@NotNull PostgreDatabase database) {
        return new GreenplumSchemaCache();
    }

    @Override
    public void configureDialect(@NotNull PostgreDialect dialect) {
        dialect.addExtraKeywords("DISTRIBUTED", "SEGMENT", "REJECT", "FORMAT", "MASTER", "WEB", "WRITABLE", "READABLE",
                "LOG", "ERRORS");
    }

    @Override
    public boolean supportsEntityMetadataInResults() {
        return true;
    }

    @Override
    public boolean supportsExplainPlanXML() {
        return dataSource.isServerVersionAtLeast(12, 0);
    }

    @Override
    public boolean supportsExplainPlanVerbose() {
        return dataSource.isServerVersionAtLeast(12, 0);
    }

    @Override
    public String createWithClause(PostgreTableRegular table, PostgreTableBase tableBase) {
        return GreenplumWithClauseBuilder.generateWithClause(table, tableBase);
    }

    @Override
    public void createUsingClause(@NotNull PostgreTableRegular table, @NotNull StringBuilder ddl) {
        if (table instanceof GreenplumTable) {
            String accessMethod = ((GreenplumTable) table).getAccessMethod();
            if (CommonUtils.isNotEmpty(accessMethod)) {
                ddl.append("\nUSING ").append(accessMethod);
            }
        }
    }

    @Nullable
    @Override
    public String readTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull PostgreTableBase table) throws DBException {
        if (table instanceof GreenplumExternalTable) {
            return ((GreenplumExternalTable) table).generateDDL(monitor);
        } else {
            return super.readTableDDL(monitor, table);
        }
    }

    @Override
    public boolean supportsHasOidsColumn() {
        return true;
    }

    @Override
    public boolean supportsDatabaseSize() {
        return true;
    }

    @Override
    public boolean supportsPartitions() {
        return true;
    }

    @Override
    public boolean supportsAlterUserChangePassword() {
        return true;
    }

    @Override
    public boolean supportsRoleReplication() {
        return dataSource.isServerVersionAtLeast(9, 1);
    }

    @Override
    public boolean supportsRoleBypassRLS() {
        return dataSource.isServerVersionAtLeast(12, 0);
    }

    @Override
    public boolean supportsDefaultPrivileges() {
        return dataSource.isServerVersionAtLeast(12, 0);
    }

    @Override
    public boolean supportsCopyFromStdIn() {
        return true;
    }

    @Override
    public boolean supportsExternalTypes() {
        return true;
    }

    @Override
    public boolean supportsDistinctForStatementsWithAcl() {
        return dataSource.isServerVersionAtLeast(12, 0);
    }

    @Override
    public boolean supportsEventTriggers() {
        return dataSource.isServerVersionAtLeast(9, 3);
    }

    @Override
    public boolean supportsRowLevelSecurity() {
        return dataSource.isServerVersionAtLeast(12, 0);
    }
}

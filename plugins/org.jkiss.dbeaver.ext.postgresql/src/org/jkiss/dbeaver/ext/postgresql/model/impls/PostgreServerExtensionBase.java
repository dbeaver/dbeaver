/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PostgreServerExtensionBase
 */
public abstract class PostgreServerExtensionBase implements PostgreServerExtension {

    private static final Log log = Log.getLog(PostgreServerExtensionBase.class);

    protected final PostgreDataSource dataSource;

    protected PostgreServerExtensionBase(PostgreDataSource dataSource) {
        this.dataSource = dataSource;
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
    public boolean supportsRules() {
        return true;
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
    public boolean supportsTablespaces() {
        return true;
    }

    @Override
    public boolean supportsSequences() {
        return true;//dataSource.isServerVersionAtLeast(10, 0);
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
    public boolean isSupportsLimits() {
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
            log.warn("Unsupported PostgreClass '" + kind + "'");
            return null;
        }
    }

    @Override
    public PostgreTableBase createNewRelation(PostgreSchema schema, PostgreClass.RelKind kind) {
        if (kind == PostgreClass.RelKind.r) {
            return new PostgreTableRegular(schema);
        } else if (kind == PostgreClass.RelKind.v) {
            return new PostgreView(schema);
        } else if (kind == PostgreClass.RelKind.m) {
            return new PostgreMaterializedView(schema);
        } else if (kind == PostgreClass.RelKind.f) {
            return new PostgreTableForeign(schema);
        } else if (kind == PostgreClass.RelKind.S) {
            return new PostgreSequence(schema);
        } else if (kind == PostgreClass.RelKind.t) {
            return new PostgreTableRegular(schema);
        } else if (kind == PostgreClass.RelKind.p) {
            return new PostgreTableRegular(schema);
        } else {
            return new PostgreTableRegular(schema);
        }
    }

    @Override
    public boolean supportsRelationSizeCalc() {
        return true;
    }

    @Override
    public boolean supportFunctionDefRead() {
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
                    if (!CommonUtils.isEmpty(superTables)) {
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
            }
        }

        if (tableBase instanceof PostgreTableRegular) {
            PostgreTableRegular table = (PostgreTableRegular) tableBase;
            try {
                if (!alter) {
                    ddl.append(createWithClause(table, tableBase));
                }
                boolean hasOtherSpecs = false;
                PostgreTablespace tablespace = table.getTablespace(monitor);
                if (tablespace != null && table.isTablespaceSpecified()) {
                    if (!alter) {
                        ddl.append("\nTABLESPACE ").append(tablespace.getName());
                    }
                    hasOtherSpecs = true;
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
                PostgreForeignServer foreignServer = table.getForeignServer(monitor);
                if (foreignServer != null ) {
                    ddl.append("\nSERVER ").append(DBUtils.getQuotedIdentifier(foreignServer));
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
    public PostgreTableColumn createTableColumn(DBRProgressMonitor monitor, PostgreSchema schema, PostgreTableBase table, JDBCResultSet dbResult) throws DBException {
        return new PostgreTableColumn(monitor, table, dbResult);
    }

    @Override
    public void initDefaultSSLConfig(DBPConnectionConfiguration connectionInfo, Map<String, String> props) {
        if (connectionInfo.getProperty(PostgreConstants.PROP_SSL) == null) {
            // We need to disable SSL explicitly (see #4928)
            props.put(PostgreConstants.PROP_SSL, "false");
        }
    }

    @Override
    public List<PostgrePermission> readObjectPermissions(DBRProgressMonitor monitor, PostgreTableBase object, boolean includeNestedObjects) throws DBException {
        List<PostgrePermission> tablePermissions = PostgreUtils.extractPermissionsFromACL(monitor, object, object.getAcl());
        if (!includeNestedObjects) {
            return tablePermissions;
        }
        tablePermissions = new ArrayList<>(tablePermissions);
        for (PostgreTableColumn column : CommonUtils.safeCollection(object.getAttributes(monitor))) {
            if (column.getAcl() == null || column.isHidden()) {
                continue;
            }
            tablePermissions.addAll(column.getPermissions(monitor, true));
        }

        return tablePermissions;
    }

    public String createWithClause(PostgreTableRegular table, PostgreTableBase tableBase) {
        StringBuilder withClauseBuilder = new StringBuilder();

        if (table.getDataSource().getServerType().supportsOids() && table.isHasOids()) {
            withClauseBuilder.append("\nWITH (\n\tOIDS=").append(table.isHasOids() ? "TRUE" : "FALSE");
            withClauseBuilder.append("\n)");
        }

        return withClauseBuilder.toString();
    }

}


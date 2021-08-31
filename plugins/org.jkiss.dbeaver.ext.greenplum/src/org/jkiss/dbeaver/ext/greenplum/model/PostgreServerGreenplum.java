/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2019 Dmitriy Dubson (ddubson@pivotal.io)
 * Copyright (C) 2019 Gavin Shaw (gshaw@pivotal.io)
 * Copyright (C) 2019 Zach Marcin (zmarcin@pivotal.io)
 * Copyright (C) 2019 Nikhil Pawar (npawar@pivotal.io)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerExtensionBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * PostgreServerGreenplum
 */
public class PostgreServerGreenplum extends PostgreServerExtensionBase {

    public PostgreServerGreenplum(PostgreDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String getServerTypeName() {
        return "Greenplum";
    }

    @Override
    public boolean supportsFunctionDefRead() {
        return false;
    }

    @Override
    public boolean supportsSessionActivity() {
        return true;
    }

    @Override
    public boolean supportsClientInfo() {
        return false;
    }

    @Override
    public PostgreTableBase createRelationOfClass(PostgreSchema schema, PostgreClass.RelKind kind, JDBCResultSet dbResult) {
        if (kind == PostgreClass.RelKind.r) {
            if (isRelationExternal(dbResult)) {
                return new GreenplumExternalTable(schema, dbResult);
            }
            return new GreenplumTable(schema, dbResult);
        } else if (kind == PostgreClass.RelKind.m) {
            return new GreenplumMaterializedView(schema, dbResult);
        }
        return super.createRelationOfClass(schema, kind, dbResult);
    }

    @Override
    public PostgreTableBase createNewRelation(DBRProgressMonitor monitor, PostgreSchema schema, PostgreClass.RelKind kind, Object copyFrom) throws DBException {
        if (kind == PostgreClass.RelKind.r) {
            return new GreenplumTable(schema);
        } else if (kind == PostgreClass.RelKind.m) {
            return new GreenplumMaterializedView(schema);
        }
        return super.createNewRelation(monitor, schema, kind, copyFrom);
    }

    private boolean isRelationExternal(JDBCResultSet dbResult) {
        return JDBCUtils.safeGetBoolean(dbResult, "is_ext_table");
    }

    @Override
    public PostgreDatabase.SchemaCache createSchemaCache(PostgreDatabase database) {
        return new GreenplumSchemaCache();
    }

    @Override
    public void configureDialect(PostgreDialect dialect) {
        dialect.addExtraKeywords("DISTRIBUTED", "SEGMENT", "REJECT", "FORMAT", "MASTER", "WEB", "WRITABLE", "READABLE",
                "LOG", "ERRORS");
    }

    @Override
    public boolean supportsEntityMetadataInResults() {
        return true;
    }

    @Override
    public boolean supportsExplainPlanXML() {
        return false;
    }

    @Override
    public boolean supportsExplainPlanVerbose() {
        return false;
    }

    @Override
    public String createWithClause(PostgreTableRegular table, PostgreTableBase tableBase) {
        return GreenplumWithClauseBuilder.generateWithClause(table, tableBase);
    }

    @Override
    public String readTableDDL(DBRProgressMonitor monitor, PostgreTableBase table) throws DBException {
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
    public boolean supportsExternalTypes() {
        return true;
    }
}

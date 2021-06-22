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
package org.jkiss.dbeaver.ext.hive.model.edit;

import org.jkiss.dbeaver.ext.generic.edit.GenericIndexManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndexColumn;
import org.jkiss.dbeaver.ext.hive.model.HiveIndex;
import org.jkiss.dbeaver.ext.hive.model.HiveTable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.List;
import java.util.Map;

public class HiveIndexManager extends GenericIndexManager {

    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }

    @Override
    public boolean canDeleteObject(GenericTableIndex object) {
        return true;
    }

    @Override
    protected GenericTableIndex createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object from, Map<String, Object> options) {
        return new HiveIndex((HiveTable) container, "NewIndex",false, "", "Compact", null);
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        HiveIndex tableIndex = (HiveIndex) command.getObject();
        DBSIndexType indexType = tableIndex.getIndexType();
        String indexName = tableIndex.getName();
        String tableName = tableIndex.getTable().getName();
        String hiveIndexType;
        List<GenericTableIndexColumn> indexColumns = tableIndex.getAttributeReferences(monitor);
        if (indexType.getId().equals("COMPACT")) {
            hiveIndexType = "\'COMPACT\'";
        } else {
            hiveIndexType = "\'BITMAP\'";
        }
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE INDEX ").append(indexName).append(" ON TABLE ").append(tableName).append(" (");
        if (indexColumns != null) {
            for (int i = 0; i < indexColumns.size(); i++) {
                if (i == 0) {
                    ddl.append(DBUtils.getQuotedIdentifier(indexColumns.get(i)));
                } else {
                    ddl.append(", ").append(DBUtils.getQuotedIdentifier(indexColumns.get(i)));
                }
            }
        }
        ddl.append(") AS ").append(hiveIndexType).append(" WITH DEFERRED REBUILD");
        actions.add(new SQLDatabasePersistAction("Create table index", ddl.toString()));
        actions.add(new SQLDatabasePersistAction("ALTER INDEX " + indexName + " ON " + tableName + " REBUILD"));
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        GenericTableIndex tableIndex = command.getObject();
        actions.add(new SQLDatabasePersistAction("Drop index table", "DROP INDEX " + tableIndex.getName() +
                            " ON " + tableIndex.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))); //$NON-NLS-2$
    }
}

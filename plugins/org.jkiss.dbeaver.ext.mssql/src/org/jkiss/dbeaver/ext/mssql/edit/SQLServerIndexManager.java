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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQL Server index manager
 */
public class SQLServerIndexManager extends SQLIndexManager<SQLServerTableIndex, SQLServerTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, SQLServerTableIndex> getObjectsCache(SQLServerTableIndex object)
    {
        return object.getTable().getContainer().getIndexCache();
    }

    @Override
    protected SQLServerTableIndex createDatabaseObject(
        @NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container,
        Object from, @NotNull Map<String, Object> options)
    {
        SQLServerTable table = (SQLServerTable) container;

        return new SQLServerTableIndex(
            table,
            true,
            false,
            null,
            DBSIndexType.UNKNOWN,
            null,
            false);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        SQLServerTableIndex index = command.getObject();
        SQLServerTableBase indexTable = index.getTable();
        if (indexTable instanceof SQLServerTableType) {
            return;
        }
        if (index.isPersisted()) {
            try {

                String indexDDL = index.getObjectDefinitionText(monitor, DBPScriptObject.EMPTY_OPTIONS);
                if (!CommonUtils.isEmpty(indexDDL)) {
                    actions.add(
                        new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, indexDDL)
                    );
                    return;
                }
            } catch (DBException e) {
                log.warn("Can't extract index DDL", e);
            }
        }
        DBSIndexType indexType = index.getIndexType();
        String sqlServerIndexType = null;
        if (indexType == DBSIndexType.CLUSTERED) {
            sqlServerIndexType = "CLUSTERED";
        } else if (indexType == SQLServerConstants.INDEX_TYPE_NON_CLUSTERED) {
            sqlServerIndexType = "NONCLUSTERED";
        }
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE ");
        if (index.isUnique()) {
            ddl.append("UNIQUE ");
        }
        boolean columnStore = index.isColumnStore();
        if (sqlServerIndexType != null) {
            ddl.append(sqlServerIndexType).append(" ");
            if (columnStore) {
                ddl.append("COLUMNSTORE ");
            }
        }
        ddl.append("INDEX ").append(DBUtils.getQuotedIdentifier(index)).append(" ON ").append(indexTable.getFullyQualifiedName(DBPEvaluationContext.DDL));
        List<SQLServerTableIndexColumn> indexColumns = index.getAttributeReferences(monitor);
        if (columnStore && index.getIndexType() == DBSIndexType.CLUSTERED) {
            // Do not add columns list in this case, it will not work (SQL Error [35335] [S0001])
        } else if (indexColumns != null) {
            ddl.append(indexColumns.stream()
                .filter(x -> !x.isIncluded() || columnStore)
                .map(DBUtils::getQuotedIdentifier)
                .collect(Collectors.joining(", ", " (", ")"))
            );

            if (!columnStore) {
                final String includedColumns = indexColumns.stream()
                    .filter(SQLServerTableIndexColumn::isIncluded)
                    .map(DBUtils::getQuotedIdentifier)
                    .collect(Collectors.joining(", "));

                if (!includedColumns.isEmpty()) {
                    ddl.append(" INCLUDE (").append(includedColumns).append(")");
                }
            }
        } else {
            super.addObjectCreateActions(monitor, executionContext, actions, command, options);
            return;
        }
        actions.add(
                new SQLDatabasePersistAction("Create new SQL Server index", ddl.toString())
        );
    }

    protected String getDropIndexPattern(SQLServerTableIndex index)
    {
        return "DROP INDEX " + DBUtils.getQuotedIdentifier(index) + " ON " + index.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL);
    }


    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        addObjectDeleteActions(monitor, executionContext, actionList, new ObjectDeleteCommand(command.getObject(), command.getTitle()), options);
        addObjectCreateActions(monitor, executionContext, actionList, makeCreateCommand(command.getObject(), options), options);
    }

}

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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options)
    {
        SQLServerTable table = (SQLServerTable) container;

        final SQLServerTableIndex index = new SQLServerTableIndex(
            table,
            true,
            false,
            null,
            DBSIndexType.UNKNOWN,
            null,
            false);

        return new UITask<SQLServerTableIndex>() {
            @Override
            protected SQLServerTableIndex runTask() {
                EditIndexPage editPage = new EditIndexPage(
                    "Create index",
                    index,
                    Collections.singletonList(DBSIndexType.OTHER));
                if (!editPage.edit()) {
                    return null;
                }
                index.setUnique(editPage.isUnique());
                index.setIndexType(editPage.getIndexType());
                index.setDescription(editPage.getDescription());
                StringBuilder idxName = new StringBuilder(64);
                idxName.append(CommonUtils.escapeIdentifier(table.getName()));
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    if (colIndex == 1) {
                        idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName()));
                    }
                    index.addColumn(
                        new SQLServerTableIndexColumn(
                            index,
                            0,
                            (SQLServerTableColumn) tableColumn,
                            colIndex++,
                            !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC))));
                }
                idxName.append("_IDX");
                index.setName(DBObjectNameCaseTransformer.transformObjectName(index, idxName.toString()));
                return index;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        SQLServerTableIndex index = command.getObject();
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
        super.addObjectCreateActions(monitor, actions, command, options);
    }

    protected String getDropIndexPattern(SQLServerTableIndex index)
    {
        return "DROP INDEX " + index.getName() + " ON " + index.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL);
    }

}

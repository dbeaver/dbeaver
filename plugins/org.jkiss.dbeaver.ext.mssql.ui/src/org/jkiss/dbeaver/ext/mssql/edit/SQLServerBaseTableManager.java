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

import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerObjectClass;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerSchema;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableBase;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerView;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;

import java.util.List;
import java.util.Map;

/**
 * SQLServer table manager
 */
public abstract class SQLServerBaseTableManager<OBJECT extends SQLServerTableBase> extends SQLTableManager<OBJECT, SQLServerSchema> implements DBEObjectRenamer<OBJECT> {

    @Override
    public DBSObjectCache<SQLServerSchema, OBJECT> getObjectsCache(OBJECT object) {
        return (DBSObjectCache) object.getSchema().getTableCache();
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, NestedObjectCommand<OBJECT, PropertyHandler> command, Map<String, Object> options) {
        final OBJECT table = command.getObject();
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            boolean isUpdate = SQLServerUtils.isCommentSet(
                monitor,
                table.getDatabase(),
                SQLServerObjectClass.OBJECT_OR_COLUMN,
                table.getObjectId(),
                0);
            actionList.add(
                new SQLDatabasePersistAction(
                    "Add table comment",
                    "EXEC " + SQLServerUtils.getSystemTableName(table.getDatabase(), isUpdate ? "sp_updateextendedproperty" : "sp_addextendedproperty") +
                        " 'MS_Description', " + SQLUtils.quoteString(command.getObject(), command.getObject().getDescription()) + "," +
                        " 'schema', '" + table.getSchema().getName() + "'," +
                        " '" + (table.isView() ? "view" : "table") + "', '" + table.getName() + "'"));
        }
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        OBJECT object = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "EXEC " + SQLServerUtils.getSystemTableName(object.getDatabase(), "sp_rename") +
                    " '" + object.getSchema().getFullyQualifiedName(DBPEvaluationContext.DML) + "." + DBUtils.getQuotedIdentifier(object.getDataSource(), command.getOldName()) +
                    "' , '" + DBUtils.getQuotedIdentifier(object.getDataSource(), command.getNewName()) + "', 'OBJECT'")
        );
    }


}

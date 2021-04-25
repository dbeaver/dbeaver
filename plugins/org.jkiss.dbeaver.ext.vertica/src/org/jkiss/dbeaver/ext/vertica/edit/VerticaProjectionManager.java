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
package org.jkiss.dbeaver.ext.vertica.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.vertica.model.VerticaProjection;
import org.jkiss.dbeaver.ext.vertica.model.VerticaProjectionColumn;
import org.jkiss.dbeaver.ext.vertica.model.VerticaSchema;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

public class VerticaProjectionManager extends SQLTableManager<VerticaProjection, VerticaSchema> implements DBEObjectRenamer<VerticaProjection> {

    @NotNull
    @Override
    public Class<?>[] getChildTypes() {
        return new Class[]{VerticaProjectionColumn.class};
    }

    @Override
    public boolean canCreateObject(Object container) {
        return false;
    }

    @Override
    protected VerticaProjection createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        return null;
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        VerticaProjection projection = command.getObject();
        actions.add(
                new SQLDatabasePersistAction("Drop projection", "DROP PROJECTION " + DBUtils.getQuotedIdentifier(projection.getContainer()) + "." + DBUtils.getQuotedIdentifier(projection.getDataSource(), projection.getBaseName())) //$NON-NLS-2$
        );
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, VerticaProjection> getObjectsCache(VerticaProjection object) {
        return null;
    }

    @Override
    public void renameObject(DBECommandContext commandContext, VerticaProjection object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        VerticaProjection projection = command.getObject();
        actions.add(
                new SQLDatabasePersistAction(
                        "Rename projection",
                        "ALTER PROJECTION " + DBUtils.getQuotedIdentifier(projection.getContainer()) + "." + DBUtils.getQuotedIdentifier(projection.getDataSource(), projection.getBaseName()) + //$NON-NLS-1$
                                " RENAME TO " + DBUtils.getQuotedIdentifier(projection.getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

}

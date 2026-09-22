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
package org.jkiss.dbeaver.ext.mimer.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabank;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabankShadow;
import org.jkiss.dbeaver.ext.mimer.model.MimerUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;
import java.util.Map;

/**
 * Adds CREATE / DROP support for Mimer SQL databank shadows ({@code CREATE SHADOW ... FOR ...
 * IN '...'} / {@code DROP SHADOW ...}), plus modify support for the two grid-editable properties:
 * the online/offline transition ({@code SET SHADOW ...}, see
 * {@link MimerDatabankShadow#buildSetOnlineDDL}) via the "Change state" dropdown, and the file
 * name ({@code ALTER SHADOW ... INTO '...'}, see {@link MimerDatabankShadow#buildAlterFileNameDDL}).
 * The third {@code ALTER SHADOW} form, {@code ADD ... PAGES}, is a navigator action instead
 * ({@code MimerShadowAddPagesHandler}), and {@code TO MASTER} another.
 * <p>
 * {@code SQLDB} cannot be shadowed (it is a temporary databank) - refused up front rather than
 * left to a server-side error.
 * <p>
 * See "CREATE SHADOW", "DROP" (Shadow), "SET SHADOW", and "ALTER SHADOW" in the
 * <a href="https://docs.mimer.com/MimerSqlManual/latest">Mimer SQL Manual</a>.
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankShadowManager extends SQLObjectEditor<MimerDatabankShadow, MimerDatabank> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        if (container instanceof MimerDatabank databank && "SQLDB".equalsIgnoreCase(databank.getName())) {
            return false;
        }
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerDatabankShadow object) {
        // Only the "Change state" dropdown - nothing else on a persisted shadow is editable.
        return true;
    }

    @Nullable
    @Override
    public DBSObjectCache<MimerDatabank, MimerDatabankShadow> getObjectsCache(MimerDatabankShadow object) {
        return object.getDatabank().getShadowCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_SHADOW";
    }

    @Override
    protected MimerDatabankShadow createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerDatabankShadow((MimerDatabank) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Create shadow", command.getObject().buildCreateDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Drop shadow", command.getObject().buildDropDDL()));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) {
        if (command.hasProperty("fileName")) {
            MimerDatabankShadow shadow = command.getObject();
            String ddl = shadow.buildAlterFileNameDDL();
            if (ddl != null) {
                actionList.add(MimerFileRenameUtil.confirmedRenameAction(
                    "Set shadow file", ddl, null, shadow.getFileName(), null));
            }
        }
        if (command.hasProperty("onlineTransition")) {
            MimerDatabankShadow shadow = command.getObject();
            String onlineDDL = shadow.buildSetOnlineDDL();
            if (onlineDDL != null) {
                boolean goesOnline = MimerUtils.stateGoesOnline(shadow.getOnlineTransition());
                // See MimerDatabankManager - flip the checkbox, reset the dropdown, reload the editor.
                actionList.add(new SQLDatabasePersistAction("Set shadow online state", onlineDDL) {
                    @Override
                    public void afterExecute(@NotNull DBCSession session, @Nullable Throwable error) {
                        if (error == null) {
                            shadow.setOnline(goesOnline);
                            shadow.setOnlineTransition(MimerUtils.ONLINE_NOCHANGE);
                            MimerUtils.refreshObjectEditor(shadow);
                        }
                    }
                });
            }
        }
    }
}

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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabank;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds CREATE / ALTER / DROP support for Mimer SQL databanks, so "Create New Databank"
 * is available on the DBA Views &gt; Databanks tree node and its properties view is
 * editable. ALTER DATABANK covers File/File&nbsp;Size/Option/Min/Goal/Max&nbsp;size (the sizes
 * single-file only - see {@link MimerDatabank#isSingleFile})/Removable via
 * {@link MimerDatabank#buildAlterDDL}, plus COMMENT ON DATABANK. Online/offline is a separate
 * statement family ({@code SET DATABANK}, see {@link MimerDatabank#buildSetOnlineDDL}) driven by
 * the "Change state" dropdown. File Size is never re-hydrated on load (no catalog column for it),
 * so it shows blank for an existing databank but can still be set.
 * <p>
 * {@code DROP DATABANK "n" [CASCADE]} - {@code CASCADE} (which also drops the databank's tables,
 * sequences and shadows) is offered via the delete-confirmation "Cascade" checkbox with an extra
 * "are you sure" prompt (see {@link MimerCascadeDropUtil}); omitted, {@code RESTRICT} is implicit
 * and the drop fails loud if the databank isn't empty.
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankManager extends SQLObjectEditor<MimerDatabank, MimerDataSource> {

    // "file" is handled separately (its own confirmed statement) - see addObjectModifyActions.
    private static final String[] ALTERABLE_PROPERTIES = {"fileSize", "type", "minSize", "goalSize", "maxSize", "removable"};

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY | FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canDeleteObject(@NotNull MimerDatabank object) {
        // Mimer SQL's built-in system databanks (SQLDB/TRANSDB/LOGDB/SYSDB) can't be dropped - disable
        // it up front rather than leave it to a server-side error.
        return !object.isSystemDatabank() && super.canDeleteObject(object);
    }

    @Nullable
    @Override
    public DBSObjectCache<MimerDataSource, MimerDatabank> getObjectsCache(MimerDatabank object) {
        return object.getDataSource().getDatabankCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_DATABANK";
    }

    @Override
    protected MimerDatabank createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerDatabank((MimerDataSource) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerDatabank databank = command.getObject();
        actions.add(new SQLDatabasePersistAction("Create databank", databank.buildCreateDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(MimerCascadeDropUtil.dropAction("Drop databank",
            command.getObject().buildDropDDL(), options, "databank", command.getObject().getName(), executionContext));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        Set<String> changed = new HashSet<>();
        for (String propId : ALTERABLE_PROPERTIES) {
            if (command.hasProperty(propId)) {
                changed.add(propId);
            }
        }
        for (String ddl : command.getObject().buildAlterDDL(changed)) {
            actionList.add(new SQLDatabasePersistAction("Alter databank", ddl));
        }
        if (command.hasProperty("file")) {
            MimerDatabank databank = command.getObject();
            String ddl = databank.isSystemDatabank() ? null : databank.buildSetFileDDL();
            if (ddl != null) {
                actionList.add(MimerFileRenameUtil.confirmedRenameAction(
                    "Set databank file", ddl, null, databank.getFile(), null));
            }
        }
        if (command.hasProperty("onlineTransition")) {
            MimerDatabank databank = command.getObject();
            String onlineDDL = databank.buildSetOnlineDDL();
            if (onlineDDL != null) {
                boolean goesOnline = MimerUtils.stateGoesOnline(databank.getOnlineTransition());
                // Once the SET succeeds: flip the checkbox, reset the dropdown to its sentinel,
                // and force any open properties editor to reload so it shows the new state.
                actionList.add(new SQLDatabasePersistAction("Set databank online state", onlineDDL) {
                    @Override
                    public void afterExecute(@NotNull DBCSession session, @Nullable Throwable error) {
                        if (error == null) {
                            databank.setOnline(goesOnline);
                            databank.setOnlineTransition(MimerUtils.ONLINE_NOCHANGE);
                            MimerUtils.refreshObjectEditor(databank);
                        }
                    }
                });
            }
        }
        if (command.hasProperty("comment")) {
            MimerDatabank databank = command.getObject();
            actionList.add(new SQLDatabasePersistAction("Comment databank",
                MimerUtils.buildCommentDDL(databank, "DATABANK", "\"" + databank.getName() + "\"", databank.getComment(monitor))));
        }
    }
}

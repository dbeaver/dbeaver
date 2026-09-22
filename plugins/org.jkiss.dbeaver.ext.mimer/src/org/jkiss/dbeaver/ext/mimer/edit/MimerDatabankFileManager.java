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
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabank;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabankFile;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
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
 * Adds CREATE / DROP support for Mimer SQL databank files ({@code ALTER DATABANK ...
 * ADD FILE} / {@code ... DROP FILE}), plus per-file size edits and rename
 * ({@code ALTER DATABANK "db" ALTER FILE '<file>' SET/DROP FILESIZE|MINSIZE|GOALSIZE|MAXSIZE} and
 * {@code ... ALTER FILE '<old>' SET FILE '<new>'}, see {@link MimerDatabankFile#buildAlterFileDDL}
 * / {@link MimerDatabankFile#buildRenameFileDDL}) - the form Mimer SQL requires once a databank has
 * more than one file. A single-file databank's sole file is edited on the databank itself
 * ({@link MimerDatabankManager}). The file-name property change (id {@code "fileName"}, so it's
 * plain-property-editable in both the grid and the file's own editor, unlike the special
 * {@code "name"} id) is handled in {@link #addObjectModifyActions}; {@link DBEObjectRenamer} is
 * also implemented so the navigator's inline Rename gesture works. All Mimer SQL 11.0+ (10.1 has no
 * multi-file databanks) - see {@link MimerDataSource#supportsMultiFileDatabanks}.
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankFileManager extends SQLObjectEditor<MimerDatabankFile, MimerDatabank>
    implements DBEObjectRenamer<MimerDatabankFile> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        // Multi-file databanks (this whole feature) are Mimer SQL 11.0+ - a 10.1 server
        // rejects ADD FILE outright, so don't offer it.
        if (container instanceof MimerDatabank databank && !databank.getDataSource().supportsMultiFileDatabanks()) {
            return false;
        }
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerDatabankFile object) {
        // 11.0+ only - on 10.1 the databank's own file/size properties are used instead. The
        // per-file DDL (buildRenameFileDDL / buildAlterFileDDL) picks ALTER FILE vs the file-less
        // form by file count, so a single-file 11.0 databank is edited here too.
        return object.getDatabank().getDataSource().supportsMultiFileDatabanks();
    }

    @Override
    public boolean canDeleteObject(@NotNull MimerDatabankFile object) {
        MimerDataSource dataSource = object.getDatabank().getDataSource();
        if (!dataSource.supportsMultiFileDatabanks()) {
            return false;
        }
        // Mimer SQL requires at least one file per databank - DROP FILE has nothing to
        // transfer the last file's data to and the server would reject it anyway, but
        // disabling Delete up front is clearer than a round-trip error.
        if (object.getDatabank().getFileCache().getCachedObjects().size() <= 1) {
            return false;
        }
        return super.canDeleteObject(object);
    }

    @Nullable
    @Override
    public DBSObjectCache<MimerDatabank, MimerDatabankFile> getObjectsCache(MimerDatabankFile object) {
        return object.getDatabank().getFileCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_FILE";
    }

    @Override
    protected MimerDatabankFile createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerDatabankFile((MimerDatabank) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Add databank file", command.getObject().buildAddFileDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Drop databank file", command.getObject().buildDropFileDDL()));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerDatabankFile file = command.getObject();
        if (command.hasProperty("fileName") && !file.getDatabank().isSystemDatabank()) {
            String ddl = file.buildRenameFileDDL();
            if (ddl != null) {
                actionList.add(MimerFileRenameUtil.confirmedRenameAction(
                    "Rename databank file", ddl, file.getLoadedFileName(), file.getName(),
                    file::resyncLoadedFileName));
            }
        }
        Set<String> changed = new HashSet<>();
        for (String propId : new String[]{"fileSize", "minSize", "goalSize", "maxSize"}) {
            if (command.hasProperty(propId)) {
                changed.add(propId);
            }
        }
        for (String ddl : file.buildAlterFileDDL(changed)) {
            actionList.add(new SQLDatabasePersistAction("Alter databank file", ddl));
        }
    }

    @Override
    public boolean canRenameObject(@NotNull MimerDatabankFile object) {
        // A system databank's file can't be renamed from here (see MimerDatabank#isSystemDatabank).
        return canEditObject(object) && !object.getDatabank().isSystemDatabank();
    }

    @Override
    public void renameObject(
        @NotNull DBECommandContext commandContext,
        @NotNull MimerDatabankFile object,
        @NotNull Map<String, Object> options,
        @NotNull String newName
    ) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectRenameCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerDatabankFile file = command.getObject();
        if (file.getDatabank().isSystemDatabank()) {
            // canRenameObject() already hides the inline Rename gesture for these; this is a backstop.
            throw new IllegalStateException("The file of the built-in system databank \""
                + file.getDatabank().getName() + "\" cannot be renamed from DBeaver - it requires "
                + "stopping the server and re-defining the file in bsql.");
        }
        actions.add(MimerFileRenameUtil.confirmedRenameAction(
            "Rename databank file",
            file.buildRenameFileDDL(command.getOldName(), command.getNewName()),
            command.getOldName(), command.getNewName(),
            file::resyncLoadedFileName));
    }
}

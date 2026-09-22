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
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerLibrary;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;
import java.util.Map;

/**
 * Adds CREATE / DROP support for Mimer SQL external libraries, so "Create New Library" is
 * available on the DBA Views &gt; Libraries node - see {@link
 * MimerLibrary#buildCreateDDL()} for the DDL itself. No ALTER support - Mimer SQL has no {@code
 * ALTER LIBRARY}. {@code DROP LIBRARY} supports an optional {@code CASCADE} (default
 * {@code RESTRICT}, dropping a library with {@code CASCADE} in effect also
 * drops every routine implemented by it) via the same delete-confirmation "Cascade"
 * checkbox {@link MimerSchemaManager}/{@link MimerDomainManager} use ({@code
 * FEATURE_DELETE_CASCADE}).
 *
 * @author Mimer Information Technology
 */
public class MimerLibraryManager extends SQLObjectEditor<MimerLibrary, MimerDataSource> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY | FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerLibrary object) {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MimerLibrary> getObjectsCache(MimerLibrary object) {
        return object.getDataSource().getLibraryCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_LIBRARY";
    }

    @Override
    protected MimerLibrary createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerLibrary((MimerDataSource) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Create library", command.getObject().buildCreateDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerLibrary library = command.getObject();
        actions.add(MimerCascadeDropUtil.dropAction("Drop library",
            "DROP LIBRARY \"" + library.getName() + "\"",
            options, "library", library.getName(), executionContext));
    }
}

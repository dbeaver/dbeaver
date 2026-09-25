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
import org.jkiss.dbeaver.ext.mimer.model.MimerSchema;
import org.jkiss.dbeaver.ext.mimer.model.MimerUtils;
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
 * CREATE/DROP support for Mimer SQL schemas: {@code CREATE SCHEMA "n"} / {@code DROP SCHEMA "n"
 * [CASCADE]} (RESTRICT is implicit when omitted). {@code FEATURE_DELETE_CASCADE} adds DBeaver's
 * stock "Cascade" checkbox to the delete confirmation. The create dialog is name-only - Mimer SQL
 * allows bundling schema elements into {@code CREATE SCHEMA}, but that belongs in a script, not a
 * wizard. No rename (Mimer SQL has no {@code ALTER SCHEMA ... RENAME}); Comment is the only editable
 * property. Mimer SQL's own built-in schemas ({@link MimerSchema#isSystemSchema}) can't be dropped -
 * {@link #canDeleteObject} disables it up front rather than leave it to a server-side error.
 *
 * @author Mimer Information Technology
 */
public class MimerSchemaManager extends SQLObjectEditor<MimerSchema, MimerDataSource> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY | FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerSchema object) {
        return true;
    }

    @Override
    public boolean canDeleteObject(@NotNull MimerSchema object) {
        // INFORMATION_SCHEMA / SYSTEM / MIMER / ODBC / BUILTIN can't be dropped.
        return !object.isSystemSchema() && super.canDeleteObject(object);
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MimerSchema> getObjectsCache(MimerSchema object) {
        return object.getDataSource().getSchemaCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_SCHEMA";
    }

    @Override
    protected MimerSchema createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        MimerSchema schema = new MimerSchema((MimerDataSource) container, null, getBaseObjectName());
        schema.setPersisted(false);
        return schema;
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Create schema", "CREATE SCHEMA \"" + command.getObject().getName() + "\""));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(MimerCascadeDropUtil.dropAction("Drop schema",
            "DROP SCHEMA \"" + command.getObject().getName() + "\"",
            options, "schema", command.getObject().getName(), executionContext));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (!command.hasProperty("comment")) {
            return;
        }
        MimerSchema schema = command.getObject();
        actionList.add(new SQLDatabasePersistAction("Comment schema",
            MimerUtils.buildCommentDDL(schema, "SCHEMA", "\"" + schema.getName() + "\"", schema.getComment(monitor))));
    }
}

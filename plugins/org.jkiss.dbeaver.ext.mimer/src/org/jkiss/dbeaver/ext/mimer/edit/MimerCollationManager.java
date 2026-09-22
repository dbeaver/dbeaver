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
import org.jkiss.dbeaver.ext.mimer.model.MimerCollation;
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
 * Adds CREATE / DROP support for Mimer SQL collations, so "Create New Collation" is available on
 * each schema's Collations tree node - see {@link MimerCollation#buildCreateDDL()} for the DDL
 * itself. No ALTER support beyond Comment: a collation is always based on an existing one and has
 * no further alterable state (a new definition means dropping and recreating it) - same reasoning
 * as {@link MimerDomainManager}, whose Comment-only / cascade-drop shape this mirrors exactly.
 *
 * @author Mimer Information Technology
 */
public class MimerCollationManager extends SQLObjectEditor<MimerCollation, MimerSchema> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY | FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerCollation object) {
        return true;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MimerCollation> getObjectsCache(MimerCollation object) {
        return object.getSchema().getCollationCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_COLLATION";
    }

    @Override
    protected MimerCollation createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerCollation((MimerSchema) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Create collation", command.getObject().buildCreateDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerCollation collation = command.getObject();
        actions.add(MimerCascadeDropUtil.dropAction("Drop collation",
            "DROP COLLATION \"" + collation.getSchema().getName() + "\".\"" + collation.getName() + "\"",
            options, "collation", collation.getName(), executionContext));
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
        MimerCollation collation = command.getObject();
        String name = "\"" + collation.getSchema().getName() + "\".\"" + collation.getName() + "\"";
        actionList.add(new SQLDatabasePersistAction("Comment collation",
            MimerUtils.buildCommentDDL(collation, "COLLATION", name, collation.getComment(monitor))));
    }
}

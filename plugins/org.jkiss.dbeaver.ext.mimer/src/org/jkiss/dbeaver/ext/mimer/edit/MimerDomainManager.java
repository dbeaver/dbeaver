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
import org.jkiss.dbeaver.ext.mimer.model.MimerDomain;
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
 * Adds CREATE / DROP support for Mimer SQL domains, so "Create New Domain" is available on
 * the Domains tree node - see {@link MimerDomain#buildCreateDDL()} for the DDL itself. No
 * ALTER support beyond Comment: Mimer SQL has no {@code ALTER DOMAIN} statement at all (changing a
 * domain's type/default/check means dropping and recreating it, with the cascading side effects
 * that implies for every column that uses it - no safe place to hang an edit action for those).
 * {@code COMMENT ON DOMAIN} is independent of that and safe to support ({@link
 * #addObjectModifyActions}), so it's the domain's only actually-editable property. {@code DROP
 * DOMAIN} supports an optional {@code CASCADE} (default {@code RESTRICT}, fails loud on a domain
 * still in use) via the same stock delete-confirmation "Cascade" checkbox {@link
 * MimerSchemaManager} uses ({@code FEATURE_DELETE_CASCADE}), not a custom dialog.
 *
 * @author Mimer Information Technology
 */
public class MimerDomainManager extends SQLObjectEditor<MimerDomain, MimerSchema> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY | FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerDomain object) {
        return true;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MimerDomain> getObjectsCache(MimerDomain object) {
        return object.getSchema().getDomainCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_DOMAIN";
    }

    @Override
    protected MimerDomain createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerDomain((MimerSchema) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Create domain", command.getObject().buildCreateDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerDomain domain = command.getObject();
        actions.add(MimerCascadeDropUtil.dropAction("Drop domain",
            "DROP DOMAIN \"" + domain.getSchema().getName() + "\".\"" + domain.getName() + "\"",
            options, "domain", domain.getName(), executionContext));
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
        MimerDomain domain = command.getObject();
        String name = "\"" + domain.getSchema().getName() + "\".\"" + domain.getName() + "\"";
        actionList.add(new SQLDatabasePersistAction("Comment domain",
            MimerUtils.buildCommentDDL(domain, "DOMAIN", name, domain.getComment(monitor))));
    }
}

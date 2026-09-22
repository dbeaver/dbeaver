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
import org.jkiss.dbeaver.ext.mimer.model.MimerGroup;
import org.jkiss.dbeaver.ext.mimer.model.MimerIdentGroupMembership;
import org.jkiss.dbeaver.ext.mimer.model.MimerProgram;
import org.jkiss.dbeaver.ext.mimer.model.MimerUser;
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
 * CREATE / DROP support for the reverse-direction "Group Memberships" folder under {@link
 * MimerUser}/{@link MimerGroup}/{@link MimerProgram} - the same {@code GRANT MEMBER}/{@code
 * REVOKE MEMBER} DDL as {@link MimerGroupMemberManager}, just issued with the ident implicit
 * (whichever folder this is) and the group picked in the create dialog instead - a convenience
 * alongside the group's own "Members" folder; both ultimately manage the identical grant.
 *
 * @author Mimer Information Technology
 */
public class MimerIdentGroupMembershipManager extends SQLObjectEditor<MimerIdentGroupMembership, DBSObject> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerIdentGroupMembership object) {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MimerIdentGroupMembership> getObjectsCache(MimerIdentGroupMembership object) {
        DBSObject ident = object.getIdent();
        if (ident instanceof MimerUser user) {
            return user.getGroupMembershipCache();
        } else if (ident instanceof MimerGroup group) {
            return group.getGroupMembershipCache();
        } else if (ident instanceof MimerProgram program) {
            return program.getGroupMembershipCache();
        }
        return null;
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_GROUP";
    }

    @Override
    protected MimerIdentGroupMembership createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerIdentGroupMembership((DBSObject) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Grant membership", command.getObject().buildGrantDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Revoke membership", command.getObject().buildRevokeDDL()));
    }
}

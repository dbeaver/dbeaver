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
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.model.MimerGroup;
import org.jkiss.dbeaver.ext.mimer.model.MimerProgram;
import org.jkiss.dbeaver.ext.mimer.model.MimerSystemPrivilege;
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
 * CREATE / DROP support for Mimer SQL system privileges, shared unchanged across the "System
 * Privileges" folder under {@link MimerUser}/{@link MimerGroup}/{@link MimerProgram} (three
 * unrelated owner types, hence the {@link DBSObject}-typed container - see {@link
 * MimerSystemPrivilege} for why one class covers all three, same reason every other per-ident
 * privilege class in this plugin does). {@code GRANT <privilege> TO "ident" [WITH GRANT OPTION]}
 * / {@code REVOKE <privilege> FROM "ident"} - the same DDL shape for User, Group, and Program.
 * No modify support - toggling WITH GRANT OPTION after the fact needs a revoke and re-grant, same
 * as every other grant-shaped manager in this plugin.
 *
 * @author Mimer Information Technology
 */
public class MimerSystemPrivilegeManager extends SQLObjectEditor<MimerSystemPrivilege, DBSObject> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerSystemPrivilege object) {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MimerSystemPrivilege> getObjectsCache(MimerSystemPrivilege object) {
        DBSObject ident = object.getParentObject();
        if (ident instanceof MimerUser user) {
            return user.getSystemPrivilegeCache();
        } else if (ident instanceof MimerGroup group) {
            return group.getSystemPrivilegeCache();
        } else if (ident instanceof MimerProgram program) {
            return program.getSystemPrivilegeCache();
        }
        return null;
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return MimerConstants.SYSTEM_PRIVILEGE_TYPES[1];
    }

    @Override
    protected MimerSystemPrivilege createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerSystemPrivilege((DBSObject) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Grant system privilege", command.getObject().buildGrantDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Revoke system privilege", command.getObject().buildRevokeDDL()));
    }
}

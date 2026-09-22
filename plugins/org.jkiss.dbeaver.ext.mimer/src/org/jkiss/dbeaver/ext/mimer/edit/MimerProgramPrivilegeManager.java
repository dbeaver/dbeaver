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
import org.jkiss.dbeaver.ext.mimer.model.MimerProgram;
import org.jkiss.dbeaver.ext.mimer.model.MimerProgramPrivilege;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;
import java.util.Map;

/**
 * Adds CREATE / DROP support for a Mimer SQL program's EXECUTE privilege, so its "Privileges"
 * node can grant/revoke: {@code GRANT EXECUTE ON PROGRAM "p" TO "ident" [WITH GRANT OPTION]} /
 * {@code REVOKE EXECUTE ON PROGRAM "p" FROM "ident"} (no {@code CASCADE} - fails loud rather
 * than silently revoking privileges the grantee held only through this grant). No modify
 * support - toggling WITH GRANT OPTION needs a revoke and re-grant, not an ALTER-style
 * statement, same as {@link MimerGroupMemberManager}.
 *
 * @author Mimer Information Technology
 */
public class MimerProgramPrivilegeManager extends SQLObjectEditor<MimerProgramPrivilege, MimerProgram> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerProgramPrivilege object) {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectCache<MimerProgram, MimerProgramPrivilege> getObjectsCache(MimerProgramPrivilege object) {
        return object.getProgram().getPrivilegeCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_PRIVILEGE";
    }

    @Override
    protected MimerProgramPrivilege createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerProgramPrivilege((MimerProgram) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Grant execute", command.getObject().buildGrantDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Revoke execute", command.getObject().buildRevokeDDL()));
    }
}

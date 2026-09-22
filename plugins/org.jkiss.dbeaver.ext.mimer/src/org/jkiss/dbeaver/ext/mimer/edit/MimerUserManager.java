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
import org.jkiss.dbeaver.ext.mimer.model.MimerUser;
import org.jkiss.dbeaver.ext.mimer.model.MimerUtils;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds CREATE / DROP / ALTER support for Mimer SQL users, so "Create New User" is
 * available on the DBA Views &gt; Users tree node and its properties view is editable:
 * {@code CREATE IDENT "n" AS USER [USING 'password'] [WITHOUT SCHEMA]} /
 * {@code DROP IDENT "n" [CASCADE]} / {@code ALTER IDENT "n" SET PASSWORD '...'} for the Password
 * property (see {@link MimerUser#buildAlterDDL}). {@code CASCADE} is offered via the
 * delete-confirmation "Cascade" checkbox with an extra "are you sure" prompt (see {@link
 * MimerCascadeDropUtil}); omitted, {@code RESTRICT} is implicit and the drop fails loud rather
 * than silently dropping objects the ident owns. OS login authorizations are a
 * separate create/drop-only child object, see {@link
 * MimerUserAuthorizationManager}. Create also emits one {@code GRANT MEMBER} per group picked
 * in the wizard (see {@link MimerUser#buildGroupGrantDDL}), so a new user can be created and
 * joined to its groups in a single "Create New User" instead of a separate grant per group
 * afterward.
 *
 * @author Mimer Information Technology
 */
public class MimerUserManager extends SQLObjectEditor<MimerUser, MimerDataSource> {

    private static final String[] ALTERABLE_PROPERTIES = {"password"};

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY | FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Nullable
    @Override
    public DBSObjectCache<MimerDataSource, MimerUser> getObjectsCache(MimerUser object) {
        return object.getDataSource().getUserCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_USER";
    }

    @Override
    protected MimerUser createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerUser((MimerDataSource) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerUser user = command.getObject();
        actions.add(new SQLDatabasePersistAction("Create user", user.buildCreateDDL()));
        for (String grantDDL : user.buildGroupGrantDDL()) {
            actions.add(new SQLDatabasePersistAction("Grant group membership", grantDDL));
        }
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(MimerCascadeDropUtil.dropAction("Drop user",
            command.getObject().buildDropDDL(), options, "user", command.getObject().getName(), executionContext));
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
            actionList.add(new SQLDatabasePersistAction("Alter user", ddl));
        }
        if (command.hasProperty("comment")) {
            MimerUser user = command.getObject();
            actionList.add(new SQLDatabasePersistAction("Comment user",
                MimerUtils.buildCommentDDL(user, "IDENT", "\"" + user.getName() + "\"", user.getComment(monitor))));
        }
    }
}

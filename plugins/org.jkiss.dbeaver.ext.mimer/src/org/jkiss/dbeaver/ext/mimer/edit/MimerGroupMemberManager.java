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
import org.jkiss.dbeaver.ext.mimer.model.MimerGroupMember;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Map;

/**
 * Adds CREATE / DROP support for Mimer SQL group membership, so a group's "Members" node
 * can grant/revoke {@code MEMBER}: {@code GRANT MEMBER ON GROUP "g" TO "ident" [WITH GRANT
 * OPTION]} / {@code REVOKE MEMBER ON GROUP "g" FROM "ident"} (no {@code CASCADE} - fails
 * loud rather than silently revoking privileges the member held only through this group).
 * No modify support - toggling WITH GRANT OPTION after the fact needs a revoke and
 * re-grant, not an ALTER-style statement. {@code canCreateObject} refuses the built-in
 * PUBLIC group - every ident is already implicitly a member of it and Mimer SQL rejects an
 * explicit grant on it.
 *
 * @author Mimer Information Technology
 */
public class MimerGroupMemberManager extends AbstractMimerPrivilegeManager<MimerGroupMember, MimerGroup> {

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        // Every ident is already implicitly a member of PUBLIC - Mimer SQL rejects an explicit
        // GRANT MEMBER ON GROUP PUBLIC, so don't offer "Create New Member" under it at all.
        if (container instanceof MimerGroup group && MimerConstants.GROUP_PUBLIC.equalsIgnoreCase(group.getName())) {
            return false;
        }
        return super.canCreateObject(container);
    }

    @Nullable
    @Override
    public DBSObjectCache<MimerGroup, MimerGroupMember> getObjectsCache(@NotNull MimerGroupMember object) {
        return object.getGroup().getMemberCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_MEMBER";
    }

    @NotNull
    @Override
    protected MimerGroupMember createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerGroupMember((MimerGroup) container, getBaseObjectName());
    }

    @NotNull
    @Override
    protected String getGrantActionLabel() {
        return "Grant membership";
    }

    @NotNull
    @Override
    protected String getRevokeActionLabel() {
        return "Revoke membership";
    }
}

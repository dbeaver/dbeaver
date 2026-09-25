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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mimer.model.MimerIdentGroupMembership;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;

import java.util.Map;

/**
 * Collects the group to join before a new Mimer SQL group membership grant is created (from the
 * ident's own "Group Memberships" folder).
 *
 * @author Mimer Information Technology
 */
public class MimerGroupMembershipConfigurator implements DBEObjectConfigurator<MimerIdentGroupMembership> {

    @Nullable
    @Override
    public MimerIdentGroupMembership configureObject(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBECommandContext commandContext,
        @Nullable Object container,
        @NotNull MimerIdentGroupMembership membership,
        @NotNull Map<String, Object> options
    ) {
        return UITask.run(() -> {
            MimerCreateGroupMembershipPage page = new MimerCreateGroupMembershipPage(membership);
            if (!page.edit()) {
                return null;
            }
            page.applyChanges();
            return membership;
        });
    }
}

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
import org.jkiss.dbeaver.ext.mimer.model.MimerDomain;
import org.jkiss.dbeaver.ext.mimer.model.MimerDomainPrivilege;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Map;

/**
 * Adds CREATE / DROP support for Mimer SQL domain privileges, so a domain's "Privileges" folder
 * can grant/revoke {@code USAGE}: {@code GRANT USAGE ON DOMAIN "s"."n" TO "ident" [WITH GRANT
 * OPTION]} / {@code REVOKE USAGE ON DOMAIN "s"."n" FROM "ident"}. No {@code [RESTRICT|CASCADE]}
 * clause, same convention as every other privilege manager in this plugin.
 *
 * @author Mimer Information Technology
 */
public class MimerDomainPrivilegeManager extends AbstractMimerPrivilegeManager<MimerDomainPrivilege, MimerDomain> {

    @Nullable
    @Override
    public DBSObjectCache<MimerDomain, MimerDomainPrivilege> getObjectsCache(@NotNull MimerDomainPrivilege object) {
        return object.getDomain().getPrivilegeCache();
    }

    @NotNull
    @Override
    protected MimerDomainPrivilege createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerDomainPrivilege((MimerDomain) container, getBaseObjectName());
    }
}

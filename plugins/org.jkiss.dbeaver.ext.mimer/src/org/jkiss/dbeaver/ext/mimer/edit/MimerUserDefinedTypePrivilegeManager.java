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
import org.jkiss.dbeaver.ext.mimer.model.MimerUserDefinedType;
import org.jkiss.dbeaver.ext.mimer.model.MimerUserDefinedTypePrivilege;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Map;

/**
 * Adds CREATE / DROP support for a Mimer SQL user-defined type's USAGE privilege, so its
 * "Privileges" folder can grant/revoke: {@code GRANT USAGE ON TYPE "s"."n" TO "ident" [WITH
 * GRANT OPTION]} / {@code REVOKE USAGE ON TYPE "s"."n" FROM "ident"}. No {@code
 * [RESTRICT|CASCADE]} clause, same convention as every other privilege manager in this plugin.
 *
 * @author Mimer Information Technology
 */
public class MimerUserDefinedTypePrivilegeManager extends AbstractMimerPrivilegeManager<MimerUserDefinedTypePrivilege, MimerUserDefinedType> {

    @Nullable
    @Override
    public DBSObjectCache<MimerUserDefinedType, MimerUserDefinedTypePrivilege> getObjectsCache(MimerUserDefinedTypePrivilege object) {
        return object.getType().getPrivilegeCache();
    }

    @Override
    protected MimerUserDefinedTypePrivilege createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerUserDefinedTypePrivilege((MimerUserDefinedType) container, getBaseObjectName());
    }

    @NotNull
    @Override
    protected String getGrantActionLabel() {
        return "Grant usage";
    }

    @NotNull
    @Override
    protected String getRevokeActionLabel() {
        return "Revoke usage";
    }
}

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
import org.jkiss.dbeaver.ext.mimer.model.MimerProcedure;
import org.jkiss.dbeaver.ext.mimer.model.MimerRoutinePrivilege;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Map;

/**
 * Adds CREATE / DROP support for Mimer SQL procedure/function privileges, so a routine's
 * "Privileges" folder can grant/revoke EXECUTE: {@code GRANT EXECUTE ON PROCEDURE|FUNCTION
 * "s"."r" TO "ident" [WITH GRANT OPTION]} / {@code REVOKE EXECUTE ON PROCEDURE|FUNCTION "s"."r"
 * FROM "ident"} - see {@link MimerRoutinePrivilege} for the keyword choice. No {@code
 * [RESTRICT|CASCADE]} clause, same convention as {@link MimerObjectPrivilegeManager}.
 *
 * @author Mimer Information Technology
 */
public class MimerRoutinePrivilegeManager extends AbstractMimerPrivilegeManager<MimerRoutinePrivilege, MimerProcedure> {

    @Nullable
    @Override
    public DBSObjectCache<MimerProcedure, MimerRoutinePrivilege> getObjectsCache(@NotNull MimerRoutinePrivilege object) {
        return object.getProcedure().getPrivilegeCache();
    }

    @NotNull
    @Override
    protected MimerRoutinePrivilege createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerRoutinePrivilege((MimerProcedure) container, getBaseObjectName());
    }
}

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
import org.jkiss.dbeaver.ext.mimer.model.MimerStatement;
import org.jkiss.dbeaver.ext.mimer.model.MimerStatementPrivilege;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Map;

/**
 * Adds CREATE / DROP support for a Mimer SQL statement's EXECUTE privilege, so its "Privileges"
 * folder can grant/revoke: {@code GRANT EXECUTE ON STATEMENT "s"."n" TO "ident" [WITH GRANT
 * OPTION]} / {@code REVOKE EXECUTE ON STATEMENT "s"."n" FROM "ident"}. No {@code
 * [RESTRICT|CASCADE]} clause, same convention as every other privilege manager in this plugin.
 *
 * @author Mimer Information Technology
 */
public class MimerStatementPrivilegeManager extends AbstractMimerPrivilegeManager<MimerStatementPrivilege, MimerStatement> {

    @Nullable
    @Override
    public DBSObjectCache<MimerStatement, MimerStatementPrivilege> getObjectsCache(@NotNull MimerStatementPrivilege object) {
        return object.getStatement().getPrivilegeCache();
    }

    @NotNull
    @Override
    protected MimerStatementPrivilege createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerStatementPrivilege((MimerStatement) container, getBaseObjectName());
    }

    @NotNull
    @Override
    protected String getGrantActionLabel() {
        return "Grant execute";
    }

    @NotNull
    @Override
    protected String getRevokeActionLabel() {
        return "Revoke execute";
    }
}

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
import org.jkiss.dbeaver.ext.mimer.model.MimerUdtMethod;
import org.jkiss.dbeaver.ext.mimer.model.MimerUserDefinedType;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

/**
 * DROP-only support for an already-defined Mimer SQL user-defined type method - {@code DROP
 * SPECIFIC METHOD "specificName" [RESTRICT|CASCADE]} (no {@code ALTER TYPE} prefix, unlike {@link
 * MimerUdtMethodSpec}'s drop - this plain form applies once a method has a body). No create or edit support here - a
 * new method always starts as a {@link org.jkiss.dbeaver.ext.mimer.model.MimerUdtMethodSpec} via
 * {@link MimerUdtMethodSpecManager}, and editing an existing method's body is deliberately out of
 * scope for now (see that class's Javadoc for why).
 *
 * @author Mimer Information Technology
 */
public class MimerUdtMethodManager extends SQLObjectEditor<MimerUdtMethod, MimerUserDefinedType> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return false;
    }

    @Override
    public boolean canEditObject(@NotNull MimerUdtMethod object) {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectCache<MimerUserDefinedType, MimerUdtMethod> getObjectsCache(MimerUdtMethod object) {
        return object.getType().getMethodCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_METHOD";
    }

    @Override
    protected MimerUdtMethod createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        throw new IllegalStateException("Create is not supported for an already-defined method - see MimerUdtMethodSpecManager");
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        // Never invoked - canCreateObject() is always false.
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerUdtMethod object = command.getObject();
        boolean cascade = MimerCascadeDropUtil.isCascade(options);
        actions.add(MimerCascadeDropUtil.confirmedDrop("Drop method",
            "DROP SPECIFIC METHOD \"" + object.getUniqueName() + "\" " + (cascade ? "CASCADE" : "RESTRICT"),
            cascade, "method", object.getName(), executionContext));
    }
}

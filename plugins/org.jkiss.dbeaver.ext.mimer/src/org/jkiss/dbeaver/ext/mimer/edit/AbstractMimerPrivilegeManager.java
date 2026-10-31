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
import org.jkiss.dbeaver.ext.mimer.model.MimerGrantable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;
import java.util.Map;

/**
 * Create/drop wiring shared by every manager backing a {@link MimerGrantable} model class -
 * either family, one fixed privilege type per class ({@code AbstractMimerObjectPrivilege}) or a
 * selectable type per instance ({@code AbstractMimerMultiTypePrivilege}) - since both only ever
 * need this same CREATE-emits-{@code GRANT}/DELETE-emits-{@code REVOKE} shape: no in-place edit,
 * no {@code CASCADE}, same convention as every privilege manager in this plugin. A concrete
 * subclass only needs {@link #getObjectsCache} (which owner-side cache to read/write) and {@link
 * #createDatabaseObject} (which constructor to call) - everything else here was previously
 * duplicated verbatim across every privilege manager in this plugin.
 *
 * @author Mimer Information Technology
 */
public abstract class AbstractMimerPrivilegeManager<P extends DBSObject & MimerGrantable, O extends DBSObject>
    extends SQLObjectEditor<P, O> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull P object) {
        return false;
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_PRIVILEGE";
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction(getGrantActionLabel(), command.getObject().buildGrantDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction(getRevokeActionLabel(), command.getObject().buildRevokeDDL()));
    }

    /**
     * The "Persist Changes" preview label for the GRANT action - purely cosmetic (never affects
     * the DDL itself), overridden by subclasses that used a more specific wording than the
     * generic default before this class existed (e.g. {@code "Grant execute"}/{@code "Grant
     * usage"} instead of {@code "Grant privilege"}).
     */
    @NotNull
    protected String getGrantActionLabel() {
        return "Grant privilege";
    }

    /**
     * The "Persist Changes" preview label for the REVOKE action, the counterpart of {@link #getGrantActionLabel()}.
     *
     * @see #getGrantActionLabel()
     */
    @NotNull
    protected String getRevokeActionLabel() {
        return "Revoke privilege";
    }
}

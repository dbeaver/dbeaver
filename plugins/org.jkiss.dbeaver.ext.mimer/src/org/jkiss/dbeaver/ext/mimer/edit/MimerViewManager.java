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
import org.jkiss.dbeaver.ext.generic.edit.GenericViewManager;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;
import java.util.Map;

/**
 * Replaces {@link GenericViewManager}'s edit-in-place behavior for Mimer SQL views. Mimer SQL has
 * neither {@code ALTER VIEW} nor {@code CREATE OR REPLACE VIEW}, so the base class's own {@link
 * GenericViewManager#addObjectModifyActions} - which just re-executes the edited definition
 * text as a single {@code "Create view"} action - doesn't work here: over an already-existing
 * view, Mimer SQL rejects the duplicate {@code CREATE VIEW} outright. Editing an existing view means
 * dropping and recreating it, same "no ALTER, only drop-recreate" pattern already used for
 * {@code MimerModule}/{@code MimerProcedure}/{@code MimerTableTrigger}/{@code MimerStatement}.
 * Create is unchanged from the base class. Delete is overridden only to append {@code CASCADE}
 * when the delete-confirmation dialog's box is ticked - core's {@code SQLTableManager} deliberately
 * never cascades a {@code DROP VIEW}, but Mimer SQL's does support it.
 *
 * @author Mimer Information Technology
 */
public class MimerViewManager extends GenericViewManager {

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(MimerCascadeDropUtil.dropAction("Drop view",
            "DROP VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL),
            options, "view", command.getObject().getName(), executionContext));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) {
        if (command.hasProperty("objectDefinitionText")) {
            GenericView view = (GenericView) command.getObject();
            actionList.add(new SQLDatabasePersistAction("Drop view",
                "DROP VIEW " + view.getFullyQualifiedName(DBPEvaluationContext.DDL)));
            actionList.add(new SQLDatabasePersistAction("Create view", view.getDDL()));
        }
    }
}

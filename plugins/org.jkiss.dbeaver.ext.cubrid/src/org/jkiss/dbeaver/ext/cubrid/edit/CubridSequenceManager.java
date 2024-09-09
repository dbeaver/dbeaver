/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridSequence;
import org.jkiss.dbeaver.ext.generic.edit.GenericSequenceManager;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class CubridSequenceManager extends GenericSequenceManager {

    public static final String BASE_SERIAL_NAME = "new_serial";

    @NotNull
    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return true;
    }

    @NotNull
    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @NotNull
    @Override
    protected GenericSequence createDatabaseObject(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBECommandContext context,
            @Nullable Object container,
            @Nullable Object copyFrom,
            @NotNull Map<String, Object> options) {
        return new CubridSequence((GenericStructContainer) container, BASE_SERIAL_NAME);
    }

    @NotNull
    public String buildStatement(@NotNull CubridSequence sequence, @NotNull boolean forUpdate) {
        StringBuilder sb = new StringBuilder();
        if (forUpdate) {
            sb.append("ALTER SERIAL ");
        } else {
            sb.append("CREATE SERIAL ");
        }
        sb.append(sequence.getFullyQualifiedName(DBPEvaluationContext.DDL));
        buildBody(sequence, sb);
        buildOtherValue(sequence, sb);
        return sb.toString();
    }

    public void buildBody(@NotNull CubridSequence sequence, @NotNull StringBuilder sb) {
        if (sequence.getIncrementBy() != null) {
            sb.append(" INCREMENT BY ").append(sequence.getIncrementBy());
        }
        if (sequence.getStartValue() != null) {
            sb.append(" START WITH ").append(sequence.getStartValue());
        }
        if (sequence.getMaxValue() != null) {
            sb.append(" MAXVALUE ").append(sequence.getMaxValue());
        }
        if (sequence.getMinValue() != null) {
            sb.append(" MINVALUE ").append(sequence.getMinValue());
        }
    }

    public void buildOtherValue(@NotNull CubridSequence sequence, @NotNull StringBuilder sb) {
        if (sequence.getCycle()) {
            sb.append(" CYCLE");
        }
        else {
            sb.append(" NOCYCLE");
        }
        if (sequence.getCachedNum() != 0) {
            sb.append(" CACHE ").append(sequence.getCachedNum());
        }
        if (sequence.getDescription() != null) {
            sb.append(" COMMENT ").append(SQLUtils.quoteString(sequence, CommonUtils.notEmpty(sequence.getDescription())));
        }
    }

    @Override
    protected void addObjectCreateActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectCreateCommand command,
            @NotNull Map<String, Object> options) {
        CubridSequence sequence = (CubridSequence) command.getObject();
        actions.add(new SQLDatabasePersistAction("Create Serial", buildStatement(sequence, false)));
    }

    @Override
    protected void addObjectModifyActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actionList,
            @NotNull ObjectChangeCommand command,
            @NotNull Map<String, Object> options) {
        CubridSequence sequence = (CubridSequence) command.getObject();
        actionList.add(new SQLDatabasePersistAction("Alter Serial", buildStatement(sequence, true)));
    }

    @Override
    protected void addObjectDeleteActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull SQLObjectEditor<GenericSequence, GenericStructContainer>.ObjectDeleteCommand command,
            @NotNull Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction("Drop Serial",
        "DROP SERIAL " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)));
    }

    @Override
    protected void addObjectExtraActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull NestedObjectCommand<GenericSequence, SQLObjectEditor<GenericSequence, GenericStructContainer>.PropertyHandler> command,
            @NotNull Map<String, Object> options) {
        /* This body intentionally empty. */
    }
}

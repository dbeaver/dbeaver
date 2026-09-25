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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericSequenceManager;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.mimer.model.MimerSequence;
import org.jkiss.dbeaver.ext.mimer.model.MimerUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;
import java.util.Map;

/**
 * Adds CREATE / ALTER support (on top of generic's DROP) for Mimer SQL sequences,
 * so "Create New Sequence" is available on the Sequences tree node.
 *
 * @author Mimer Information Technology
 */
public class MimerSequenceManager extends GenericSequenceManager {

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        // FEATURE_SAVE_IMMEDIATELY: MimerCreateSequencePage collects everything up front, no
        // post-create editor. FEATURE_DELETE_CASCADE: Mimer SQL's DROP SEQUENCE accepts CASCADE.
        return FEATURE_SAVE_IMMEDIATELY | FEATURE_DELETE_CASCADE;
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull SQLObjectEditor<GenericSequence, GenericStructContainer>.ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(MimerCascadeDropUtil.dropAction("Drop sequence",
            "DROP SEQUENCE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL),
            options, "sequence", command.getObject().getName(), executionContext));
    }

    @Override
    protected MimerSequence createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        GenericStructContainer structContainer = (GenericStructContainer) container;
        MimerSequence sequence = new MimerSequence(structContainer, getBaseObjectName());
        setNewObjectName(monitor, structContainer.getSchema(), sequence);
        return sequence;
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull SQLObjectEditor<GenericSequence, GenericStructContainer>.ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerSequence sequence = (MimerSequence) command.getObject();
        actions.add(new SQLDatabasePersistAction("Create sequence", sequence.buildCreateDDL()));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull SQLObjectEditor<GenericSequence, GenericStructContainer>.ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        // Mimer SQL's ALTER SEQUENCE only supports RESTART WITH; everything else
        // (type, bounds, increment, cycle) requires drop &amp; recreate.
        if (command.hasProperty("restartWith")) {
            MimerSequence sequence = (MimerSequence) command.getObject();
            actionList.add(new SQLDatabasePersistAction("Alter sequence", sequence.buildRestartDDL(null)));
        }
        if (command.hasProperty("comment")) {
            MimerSequence sequence = (MimerSequence) command.getObject();
            actionList.add(new SQLDatabasePersistAction("Comment sequence",
                MimerUtils.buildCommentDDL(sequence, "SEQUENCE", sequence.getFullyQualifiedName(DBPEvaluationContext.DDL), sequence.getComment(monitor))));
        }
    }
}

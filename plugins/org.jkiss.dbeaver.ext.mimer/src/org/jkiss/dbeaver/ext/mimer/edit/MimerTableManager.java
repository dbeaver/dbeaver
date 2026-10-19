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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.ext.mimer.model.MimerTable;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableColumn;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableIndex;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Companion to {@link MimerTableColumnManager} for the {@code CREATE TABLE} path - a new table's
 * auto-generated columns are declared inline in one composite statement, so the {@code CREATE
 * SEQUENCE} prepend happens here before delegating to the base class. The sequence actions are
 * spliced into {@code actions} at index 0 <i>after</i> {@code super} runs, not added before it,
 * to guarantee {@code CREATE SEQUENCE} lands ahead of {@code CREATE TABLE} in the generated
 * script regardless of what else touches the list in between.
 * <p>
 * {@link #addObjectDeleteActions} is overridden so a ticked "Cascade" box routes through {@link
 * MimerCascadeDropUtil} (extra confirmation, {@code CASCADE} appended) - core's {@code
 * SQLTableManager} appends {@code CASCADE} with no second prompt. The box itself is enabled by
 * {@code MimerSQLDialect#supportsTableDropCascade()}.
 *
 * @author Mimer Information Technology
 */
public class MimerTableManager extends GenericTableManager {

    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
        MimerTableColumn.class,
        GenericUniqueKey.class,
        GenericTableForeignKey.class,
        MimerTableIndex.class
    );

    /**
     * Declares the real Mimer SQL subclasses as child types, not the inherited generic ones.
     * <p>
     * {@code SQLTableManager#getTableDDL} (core) resolves each child object's {@code
     * SQLObjectEditor} via this method - a fixed, statically declared type per child kind, not
     * the actual pending object's runtime class - and {@code ObjectManagerRegistry} does an
     * exact-class-name match. Left at {@code GenericTableIndex.class}, that would resolve to
     * the plain {@code GenericIndexManager} instead of {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerIndexManager} (registered only for the exact {@link
     * MimerTableIndex} class), producing a DDL preview that doesn't reflect Mimer SQL-specific
     * index modifiers (Clustered/Include/Ignore Nulls).
     * <p>
     * The actual Save path is unaffected either way - {@code
     * AbstractCommandContext#getPersistActions} resolves by the pending object's own {@code
     * getClass()}, which is already {@link MimerTableIndex}, so it always executed the correct
     * DDL even before this fix. This method just makes the DDL *preview* match. {@link
     * MimerTableColumn} is declared here for the same reason.
     */
    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Override
    protected void addStructObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull StructCreateCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        List<DBEPersistAction> sequenceActions = new ArrayList<>();
        for (NestedObjectCommand nestedCommand : getNestedOrderedCommands(command)) {
            if (nestedCommand.getObject() instanceof MimerTableColumn column) {
                // Also sets the column's default value as a side effect - must run before
                // super below builds the column's nested declaration, which reads that default.
                sequenceActions.addAll(MimerTableColumnManager.prepareAutoIncrementColumn(monitor, column));
            }
        }
        super.addStructObjectCreateActions(monitor, executionContext, actions, command, options);
        actions.addAll(0, sequenceActions);
    }

    /**
     * Appends {@code IN "<databank>"} to {@code CREATE TABLE} when the new table's "Databank"
     * property is set (see {@link MimerTable#getDatabank}). Only on create - Mimer SQL has no
     * {@code ALTER TABLE ... SET DATABANK}, and {@code appendTableModifiers} is never called with
     * {@code alter == true} from the base anyway; the guard is belt-and-braces.
     */
    @Override
    protected void appendTableModifiers(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase table,
        @NotNull NestedObjectCommand tableProps,
        @NotNull StringBuilder ddl,
        boolean alter,
        @NotNull Map<String, Object> options
    ) throws DBException {
        super.appendTableModifiers(monitor, table, tableProps, ddl, alter, options);
        if (alter || !(table instanceof MimerTable mimerTable)) {
            return;
        }
        String databank = mimerTable.getDatabank(monitor);
        if (!CommonUtils.isEmptyTrimmed(databank)) {
            // Quote the databank name only if the dialect actually needs to (reserved word,
            // special characters, ...) - same as table/column names elsewhere in the DDL.
            ddl.append(" IN ").append(DBUtils.getQuotedIdentifier(table.getDataSource(), databank.trim()));
        }
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(MimerCascadeDropUtil.dropAction("Drop table",
            "DROP TABLE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL),
            options, "table", command.getObject().getName(), executionContext));
    }
}

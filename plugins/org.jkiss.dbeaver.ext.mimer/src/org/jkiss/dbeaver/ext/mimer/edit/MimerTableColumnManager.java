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
import org.jkiss.dbeaver.ext.generic.edit.GenericTableColumnManager;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerSequence;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableColumn;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Gives Mimer SQL's "Auto Generated" checkbox its real meaning for {@code ALTER TABLE ... ADD
 * COLUMN} ({@link MimerTableManager} handles the {@code CREATE TABLE} case). Mimer SQL has no
 * identity syntax, so checking the box prepends a {@code CREATE SEQUENCE} (skipped if the named
 * sequence already exists) and defaults the column to {@code NEXT VALUE FOR}/{@code NEXT_VALUE
 * OF}, whichever the server supports - see {@link MimerDataSource#supportsNextValueForSyntax}.
 * <p>
 * Also the write side of {@link MimerTableColumn#getCollation}: unlike every other property on
 * this class, an existing column's collation can actually be changed, via {@code ALTER TABLE
 * ... ALTER COLUMN ... SET DATA TYPE <type> COLLATE <collation>} - the data type has to be
 * repeated even when only the collation is changing, per {@code ALTER_TABLE.htm}'s own example.
 * {@link #addObjectModifyActions} reuses the inherited {@link #DataTypeModifier} to build that
 * type text, the same one that already builds a new column's declaration.
 *
 * @author Mimer Information Technology
 */
public class MimerTableColumnManager extends GenericTableColumnManager {

    /**
     * A new column's default data type - {@link GenericTableColumnManager#createDatabaseObject}
     * tries {@link org.jkiss.dbeaver.model.DBConstants#DEFAULT_DATATYPE_NAMES} ({@code varchar},
     * {@code varchar2}, {@code string}, {@code char}, {@code integer}), an exact-name match
     * against whatever the datasource's own type list reports - none of the first four match any
     * of Mimer SQL's own type names, so every new column silently defaulted to plain {@code
     * INTEGER}, the last resort. Overridden here to try Mimer SQL's real name for the equivalent
     * type first ({@code CHARACTER VARYING}), falling back to {@code INTEGER} the same way if
     * that's somehow unavailable too - otherwise identical to the inherited method. Also fixes a
     * real, if secondary, effect of the same gap: {@link MimerTableColumn#getCollation}'s
     * character-type-only editability could never apply to a brand-new column, since the generic
     * "Create New Column" dialog evaluates editability once, before the user has changed anything,
     * against whatever type the column started with.
     * <p>
     * Length always starts blank, unlike the inherited method's own {@code 100} default for a
     * {@code STRING}-kind type - {@code CHARACTER VARYING} needs a length, but that default is
     * now supplied by {@link MimerSQLDialect#getColumnTypeModifiers} itself at DDL-generation
     * time instead, precisely so a blank field here never carries a stale value over if the type
     * is changed to something else (e.g. {@code INTEGER}) before saving.
     */
    @Override
    protected GenericTableColumn createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) throws DBException {
        GenericTableBase tableBase = (GenericTableBase) container;
        DBSDataType columnType = findBestDataType(tableBase, "CHARACTER VARYING", "INTEGER");

        GenericTableColumn column = tableBase.getDataSource().getMetaModel().createTableColumnImpl(
            monitor,
            null,
            tableBase,
            getNewColumnName(monitor, context, tableBase),
            columnType == null ? "INTEGER" : columnType.getName(),
            columnType == null ? Types.INTEGER : columnType.getTypeID(),
            columnType == null ? Types.INTEGER : columnType.getTypeID(),
            -1,
            0,
            0,
            null,
            null,
            10,
            false,
            null,
            null,
            false,
            false
        );
        column.setPersisted(false);
        return column;
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        super.addObjectModifyActions(monitor, executionContext, actionList, command, options);
        if (!command.hasProperty("collation") || !(command.getObject() instanceof MimerTableColumn column)) {
            return;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL));
        sql.append(" ALTER COLUMN ").append(DBUtils.getQuotedIdentifier(column)).append(" SET DATA TYPE");
        DataTypeModifier.appendModifier(monitor, column, sql, command);
        actionList.add(new SQLDatabasePersistAction("Set column collation", sql.toString()));
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        List<DBEPersistAction> sequenceActions = command.getObject() instanceof MimerTableColumn column
            ? prepareAutoIncrementColumn(monitor, column)
            : Collections.emptyList();
        super.addObjectCreateActions(monitor, executionContext, actions, command, options);
        // Spliced in after super, not before - see MimerTableManager for why.
        actions.addAll(0, sequenceActions);
    }

    /**
     * If {@code column} is "Auto Generated", resolves its backing sequence (named by the
     * "Sequence" property, else {@code <table>_<column>_seq}), returns a {@code CREATE SEQUENCE}
     * action unless one by that name already exists, and points the column's default at it as a
     * side effect (must happen before the caller builds the column declaration). Shared with
     * {@link MimerTableManager} for the {@code CREATE TABLE} path.
     */
    @NotNull
    static List<DBEPersistAction> prepareAutoIncrementColumn(
        @NotNull DBRProgressMonitor monitor,
        @NotNull MimerTableColumn column
    ) throws DBException {
        if (!column.isAutoIncrement()) {
            return Collections.emptyList();
        }
        GenericTableBase table = column.getParentObject();
        GenericSchema schema = table.getSchema();
        String schemaName = schema == null ? null : schema.getName();
        String seqName = CommonUtils.isEmptyTrimmed(column.getSequenceName())
            ? table.getName() + "_" + column.getName() + "_seq"
            : column.getSequenceName().trim();

        boolean exists = false;
        if (schema != null) {
            Collection<? extends GenericSequence> sequences = schema.getSequences(monitor);
            if (sequences != null) {
                for (GenericSequence seq : sequences) {
                    if (seq.getName().equalsIgnoreCase(seqName)) {
                        exists = true;
                        break;
                    }
                }
            }
        }
        boolean nextValueFor = !(column.getDataSource() instanceof MimerDataSource ds) || ds.supportsNextValueForSyntax();
        column.setDefaultValue((nextValueFor ? "NEXT VALUE FOR \"" : "NEXT_VALUE OF \"") + schemaName + "\".\"" + seqName + "\"");
        if (exists) {
            return Collections.emptyList();
        }
        List<DBEPersistAction> actions = new ArrayList<>(1);
        if (schema != null) {
            actions.add(new SQLDatabasePersistAction("Create sequence", new MimerSequence(schema, seqName).buildCreateDDL()));
        }
        return actions;
    }
}

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
package org.jkiss.dbeaver.ext.sqlite.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableConstraintColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteTable;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteTableColumn;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteTableForeignKey;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * SQLite table manager
 */
public class SQLiteTableManager extends GenericTableManager implements DBEObjectRenamer<GenericTableBase> {

    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
        SQLiteTableColumn.class,
        GenericUniqueKey.class,
        SQLiteTableForeignKey.class,
        GenericTableIndex.class
    );

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options)
    {
        final GenericDataSource dataSource = command.getObject().getDataSource();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "ALTER TABLE " + (command.getObject().getSchema() != null ?
                                  DBUtils.getQuotedIdentifier(dataSource, command.getObject().getSchema().getName())
                                      + "." : "") + DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) +//$NON-NLS-1$
                    " RENAME TO " + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())) //$NON-NLS-1$
        );
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull GenericTableBase object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        if (object.isView()) {
            throw new DBException("View rename is not supported");
        }
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected boolean isIncludeConstraintInDDL(DBRProgressMonitor monitor, DBSEntityConstraint constraint) {
        if (constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY && constraint instanceof GenericUniqueKey key) {
			List<GenericTableConstraintColumn> columns = key.getAttributeReferences(monitor);
            if (columns.size() == 1 && columns.get(0).getAttribute().isAutoIncrement()) {
                return false;
            }
        }
        return super.isIncludeConstraintInDDL(monitor, constraint);
    }

    @Override
    protected boolean isIncludeDropInDDL(@NotNull GenericTableBase tableBase) {
        return false;
    }

    @Override
    protected void appendTableModifiers(
        DBRProgressMonitor monitor,
        GenericTableBase table,
        NestedObjectCommand tableProps,
        StringBuilder ddl,
        boolean alter
    ) {
        if (table instanceof SQLiteTable sqliteTable && sqliteTable.isHasStrictTyping()) {
            ddl.append(" STRICT"); //$NON-NLS-1$
        }
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.firebird.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableColumnManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEObjectReorderer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * FireBirdTableColumnManager
 */
public class FireBirdTableColumnManager extends GenericTableColumnManager
    implements DBEObjectRenamer<GenericTableColumn>, DBEObjectReorderer<GenericTableColumn>
{

    protected final ColumnModifier<GenericTableColumn> FBDefaultModifier = (monitor, column, sql, command) -> {
        if (CommonUtils.isEmpty(command.getObject().getDefaultValue())) {
            sql.append(" DROP DEFAULT");
        } else {
            sql.append(" SET DEFAULT ");
            DefaultModifier.appendModifier(monitor, column, sql, command);
        }
    };

    @Override
    public StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, GenericTable owner, DBECommandAbstract<GenericTableColumn> command, Map<String, Object> options)
    {
        StringBuilder decl = super.getNestedDeclaration(monitor, owner, command, options);
        final GenericTableColumn column = command.getObject();
        if (column.isAutoIncrement()) {
            final String autoIncrementClause = column.getDataSource().getMetaModel().getAutoIncrementClause(column);
            if (autoIncrementClause != null && !autoIncrementClause.isEmpty()) {
                decl.append(" ").append(autoIncrementClause); //$NON-NLS-1$
            }
        }
        return decl;
    }

    @Override
    protected ColumnModifier[] getSupportedModifiers(GenericTableColumn column, Map<String, Object> options) {
        // According to SQL92 DEFAULT comes before constraints
        return new ColumnModifier[] {DataTypeModifier, FBDefaultModifier, NotNullModifier};
    }

    /**
     * Is is pretty standard
     */
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        final GenericTableColumn column = command.getObject();

        String prefix = "ALTER TABLE " + DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL) + " ALTER COLUMN " + DBUtils.getQuotedIdentifier(column) + " ";
        String typeClause = column.getFullTypeName();
        if (command.getProperty(DBConstants.PROP_ID_TYPE_NAME) != null || command.getProperty("maxLength") != null || command.getProperty("precision") != null || command.getProperty("scale") != null) {
            actionList.add(new SQLDatabasePersistAction("Set column type", prefix + "TYPE " + typeClause));
        }
        if (command.getProperty(DBConstants.PROP_ID_DEFAULT_VALUE) != null) {
            if (CommonUtils.isEmpty(column.getDefaultValue())) {
                actionList.add(new SQLDatabasePersistAction("Drop column default", prefix + "DROP DEFAULT"));
            } else {
                actionList.add(new SQLDatabasePersistAction("Set column default", prefix + "SET DEFAULT " + column.getDefaultValue()));
            }
        }
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            actionList.add(new SQLDatabasePersistAction("Set column comment", "COMMENT ON COLUMN " +
                DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL) + "." + DBUtils.getQuotedIdentifier(column) +
                " IS " + SQLUtils.quoteString(column, CommonUtils.notEmpty(column.getDescription()))));
        }
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        final GenericTableColumn column = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename column",
                "ALTER TABLE " + DBUtils.getQuotedIdentifier(column.getTable()) + " ALTER COLUMN " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) +
                    " TO " + DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())));
    }

    @Override
    public void renameObject(DBECommandContext commandContext, GenericTableColumn object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    ///////////////////////////////////////////////
    // Reorder

    @Override
    protected void addObjectReorderActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectReorderCommand command, Map<String, Object> options) {
        final GenericTableColumn column = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Reorder column",
                "ALTER TABLE " + DBUtils.getQuotedIdentifier(command.getObject().getTable()) + " ALTER COLUMN " +
                    DBUtils.getQuotedIdentifier(command.getObject()) + " POSITION " + column.getOrdinalPosition()));
    }

    @Override
    public int getMinimumOrdinalPosition(GenericTableColumn object) {
        return 1;
    }

    @Override
    public int getMaximumOrdinalPosition(GenericTableColumn object) {
        try {
            return object.getTable().getAttributes(new VoidProgressMonitor()).size();
        } catch (DBException e) {
            log.error("Error reading columns for maximum order position", e);
            return 0;
        }
    }

    @Override
    public void setObjectOrdinalPosition(DBECommandContext commandContext, GenericTableColumn object, List<GenericTableColumn> siblingObjects, int newPosition) throws DBException {
        processObjectReorder(commandContext, object, siblingObjects, newPosition);
    }

}

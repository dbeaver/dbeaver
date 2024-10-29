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

package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Generic table column manager
 */
public class GenericTableColumnManager extends SQLTableColumnManager<GenericTableColumn, GenericTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericTableColumn> getObjectsCache(GenericTableColumn object) {
        return object.getParentObject().getContainer().getTableCache().getChildrenCache(object.getParentObject());
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return container instanceof GenericTable && GenericUtils.canAlterTable((GenericTable) container);
    }

    @Override
    public boolean canEditObject(GenericTableColumn object) {
        return GenericUtils.canAlterTable(object);
    }

    @Override
    public boolean canDeleteObject(@NotNull GenericTableColumn object) {
        return GenericUtils.canAlterTable(object);
    }

    @Override
    protected GenericTableColumn createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        Object container,
        Object copyFrom,
        @NotNull Map<String, Object> options
    ) throws DBException {
        GenericTableBase tableBase = (GenericTableBase) container;
        DBSDataType columnType = findBestDataType(tableBase, DBConstants.DEFAULT_DATATYPE_NAMES);

        int columnSize = columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0;
        GenericTableColumn column = tableBase.getDataSource().getMetaModel().createTableColumnImpl(
            monitor,
            null,
            tableBase,
            getNewColumnName(monitor, context, tableBase),
            columnType == null ? "INTEGER" : columnType.getName(),
            columnType == null ? Types.INTEGER : columnType.getTypeID(),
            columnType == null ? Types.INTEGER : columnType.getTypeID(),
            -1,
            columnSize,
            columnSize,
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
    public StringBuilder getNestedDeclaration(@NotNull DBRProgressMonitor monitor, @NotNull GenericTableBase owner, @NotNull DBECommandAbstract<GenericTableColumn> command, @NotNull Map<String, Object> options) {
        StringBuilder decl = super.getNestedDeclaration(monitor, owner, command, options);
        addIncrementClauseToNestedDeclaration(command, decl);
        return decl;
    }

    public void addIncrementClauseToNestedDeclaration(DBECommandAbstract<GenericTableColumn> command, StringBuilder decl) {
        final GenericTableColumn column = command.getObject();
        if (column.isAutoIncrement()) {
            final String autoIncrementClause = column.getDataSource().getMetaModel().getAutoIncrementClause(column);
            if (autoIncrementClause != null && !autoIncrementClause.isEmpty()) {
                decl.append(" ").append(autoIncrementClause); //$NON-NLS-1$
            }
        }
    }

    @Override
    protected ColumnModifier[] getSupportedModifiers(GenericTableColumn column, Map<String, Object> options) {
        // According to SQL92 DEFAULT comes before constraints
        GenericMetaModel metaModel = column.getDataSource().getMetaModel();
        if (!metaModel.supportsNotNullColumnModifiers(column)) {
            return new ColumnModifier[]{
                DataTypeModifier, DefaultModifier
            };
        } else {
            return new ColumnModifier[]{
                DataTypeModifier, DefaultModifier,
                metaModel.isColumnNotNullByDefault() ? NullNotNullModifier : NotNullModifier
            };
        }
    }

    @Override
    protected long getDDLFeatures(GenericTableColumn object) {
        long features = 0;
        if (CommonUtils.toBoolean(object.getDataSource().getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_DDL_DROP_COLUMN_SHORT))) {
            features |= DDL_FEATURE_OMIT_COLUMN_CLAUSE_IN_DROP;
        }
        if (CommonUtils.toBoolean(object.getDataSource().getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_DDL_DROP_COLUMN_BRACKETS))) {
            features |= DDL_FEATURE_USER_BRACKETS_IN_DROP;
        }
        if (CommonUtils.toBoolean(object.getDataSource().getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_ALTER_TABLE_ADD_COLUMN))) {
            features |= FEATURE_ALTER_TABLE_ADD_COLUMN;
        }
        return features;
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        GenericTableColumn column = command.getObject();
        // Add more or less standard COMMENT ON if comment was actually edited (i.e. it is editable at least).
        if (command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            addColumnCommentAction(actionList, column, column.getTable());
        }
    }

}

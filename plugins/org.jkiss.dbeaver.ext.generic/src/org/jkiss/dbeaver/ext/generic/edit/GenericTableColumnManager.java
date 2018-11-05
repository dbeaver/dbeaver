/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.AttributeEditPage;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.Map;

/**
 * Generic table column manager
 */
public class GenericTableColumnManager extends SQLTableColumnManager<GenericTableColumn, GenericTable> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericTableColumn> getObjectsCache(GenericTableColumn object)
    {
        return object.getParentObject().getContainer().getTableCache().getChildrenCache(object.getParentObject());
    }

    @Override
    protected GenericTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, GenericTable parent, Object copyFrom) throws DBException {
        DBSDataType columnType = findBestDataType(parent.getDataSource(), DBConstants.DEFAULT_DATATYPE_NAMES);

        int columnSize = columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0;
        GenericTableColumn column = parent.getDataSource().getMetaModel().createTableColumnImpl(
            monitor,
            parent,
            getNewColumnName(monitor, context, parent),
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
        return new UITask<GenericTableColumn>() {
            @Override
            protected GenericTableColumn runTask() {
                AttributeEditPage page = new AttributeEditPage(null, column);
                if (!page.edit()) {
                    return null;
                }
                return column;
            }
        }.execute();
    }

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
        return new ColumnModifier[] {DataTypeModifier, DefaultModifier, NotNullModifier};
    }

    @Override
    protected long getDDLFeatures(GenericTableColumn object) {
        long features = 0;
        Object shortDrop = object.getDataSource().getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_DDL_DROP_COLUMN_SHORT);
        if (shortDrop != null && CommonUtils.toBoolean(shortDrop)) {
            features |= DDL_FEATURE_OMIT_COLUMN_CLAUSE_IN_DROP;
        }
        return features;
    }

}

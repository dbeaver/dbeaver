/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.Nullable;
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
import org.jkiss.utils.CommonUtils;

import java.sql.Types;

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
    protected GenericTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, GenericTable parent, Object copyFrom)
    {
        DBSDataType columnType = findBestDataType(parent.getDataSource(), DBConstants.DEFAULT_DATATYPE_NAMES);

        final GenericTableColumn column = new GenericTableColumn(parent);
        column.setName(getNewColumnName(monitor, context, parent));
        column.setTypeName(columnType == null ? "INTEGER" : columnType.getName());
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0);
        column.setValueType(columnType == null ? Types.INTEGER : columnType.getTypeID());
        column.setOrdinalPosition(-1);
        return column;
    }

    @Override
    public StringBuilder getNestedDeclaration(GenericTable owner, DBECommandAbstract<GenericTableColumn> command)
    {
        StringBuilder decl = super.getNestedDeclaration(owner, command);
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
    protected long getDDLFeatures(GenericTableColumn object) {
        long features = 0;
        Object shortDrop = object.getDataSource().getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_DDL_DROP_COLUMN_SHORT);
        if (shortDrop != null && CommonUtils.toBoolean(shortDrop)) {
            features |= DDL_FEATURE_OMIT_COLUMN_CLAUSE_IN_DROP;
        }
        return features;
    }
}

/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataType;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Oracle table column manager
 */
public class OracleTableColumnManager extends SQLTableColumnManager<OracleTableColumn, OracleTableBase> {

    protected final ColumnModifier<OracleTableColumn> OracleNotNullModifier = new ColumnModifier<OracleTableColumn>() {
        @Override
        public void appendModifier(OracleTableColumn column, StringBuilder sql) {
            sql.append(column.isRequired() ? " NOT NULL" : " NULL");
        }
    };

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleTableColumn> getObjectsCache(OracleTableColumn object)
    {
        return object.getParentObject().getContainer().tableCache.getChildrenCache(object.getParentObject());
    }

    protected ColumnModifier<OracleTableColumn>[] getSupportedModifiers()
    {
        return new ColumnModifier[] {DataTypeModifier, DefaultModifier, OracleNotNullModifier};
    }

    @Override
    protected OracleTableColumn createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, OracleTableBase parent, Object copyFrom)
    {
        DBSDataType columnType = findBestDataType(parent.getDataSource(), "varchar2"); //$NON-NLS-1$

        final OracleTableColumn column = new OracleTableColumn(parent);
        column.setName(DBObjectNameCaseTransformer.transformName(column, getNewColumnName(context, parent)));
        column.setType((OracleDataType) columnType);
        column.setTypeName(columnType == null ? "INTEGER" : columnType.getName()); //$NON-NLS-1$
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0);
        column.setValueType(columnType == null ? Types.INTEGER : columnType.getTypeID());
        column.setOrdinalPosition(-1);
        return column;
    }

    @Override
    protected DBEPersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        final OracleTableColumn column = command.getObject();
        List<DBEPersistAction> actions = new ArrayList<DBEPersistAction>(2);
        boolean hasComment = command.getProperty("comment") != null;
        if (!hasComment || command.getProperties().size() > 1) {
            actions.add(new SQLDatabasePersistAction(
                "Modify column",
                "ALTER TABLE " + column.getTable().getFullQualifiedName() + //$NON-NLS-1$
                " MODIFY " + getNestedDeclaration(column.getTable(), command))); //$NON-NLS-1$
        }
        if (hasComment) {
            actions.add(new SQLDatabasePersistAction(
                "Comment column",
                "COMMENT ON COLUMN " + column.getTable().getFullQualifiedName() + "." + DBUtils.getQuotedIdentifier(column) +
                    " IS '" + column.getComment() + "'"));
        }
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }
}

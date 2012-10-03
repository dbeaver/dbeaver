/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

/**
 * JDBC table column manager
 */
public abstract class JDBCTableColumnManager<OBJECT_TYPE extends JDBCTableColumn<TABLE_TYPE>, TABLE_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE, TABLE_TYPE>
{
    public static final long DDL_FEATURE_OMIT_COLUMN_CLAUSE_IN_DROP = 1;

/*
    @Override
    protected DBSObjectCache<TABLE_TYPE, OBJECT_TYPE> getObjectsCache(OBJECT_TYPE object)
    {
        return object.getParentObject().getCache();
    }
*/


    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    protected long getDDLFeatures()
    {
        return 0;
    }

    private boolean hasDDLFeature(long feature)
    {
        return (getDDLFeatures() & feature) != 0;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        final TABLE_TYPE table = command.getObject().getTable();
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                CoreMessages.model_jdbc_create_new_table_column,
                "ALTER TABLE " + table.getFullQualifiedName() + " ADD "  + getNestedDeclaration(table, command)) }; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                CoreMessages.model_jdbc_drop_table_column, "ALTER TABLE " + command.getObject().getTable().getFullQualifiedName() + //$NON-NLS-2$
                    " DROP " + (hasDDLFeature(DDL_FEATURE_OMIT_COLUMN_CLAUSE_IN_DROP) ? "" : "COLUMN ") + DBUtils.getQuotedIdentifier(command.getObject())) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        };
    }

    protected String getNewColumnName(DBECommandContext context, TABLE_TYPE table)
    {
        for (int i = 1; ; i++)  {
            final String name = "Column" + i;
            try {
                // check for existing columns
                boolean exists = table.getAttribute(VoidProgressMonitor.INSTANCE, name) != null;
                if (!exists) {
                    // Check for new columns (they are present only within command context)
                    for (DBPObject contextObject : context.getEditedObjects()) {
                        if (contextObject instanceof JDBCTableColumn && ((JDBCTableColumn) contextObject).getTable() == table && name.equalsIgnoreCase(((JDBCTableColumn) contextObject).getName())) {
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) {
                    return name;
                }
            } catch (DBException e) {
                log.warn(e);
                return name;
            }
        }

    }

    @Override
    protected StringBuilder getNestedDeclaration(TABLE_TYPE owner, DBECommandComposite<OBJECT_TYPE, PropertyHandler> command)
    {
        OBJECT_TYPE column = command.getObject();

        // Create column
        String columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), column.getName());

        final String typeName = column.getTypeName();
        boolean useMaxLength = false;
        final DBSDataType dataType = findDataType(column.getDataSource(), typeName);
        if (dataType == null) {
            log.debug("Type name '" + typeName + "' is not supported by driver"); //$NON-NLS-1$ //$NON-NLS-2$
        } else if (dataType.getDataKind() == DBSDataKind.STRING) {
            if (typeName.indexOf('(') == -1) {
                useMaxLength = true;
            }
        }

        final boolean notNull = column.isRequired();
        final long maxLength = column.getMaxLength();
        StringBuilder decl = new StringBuilder(40);
        decl.append(columnName).append(' ').append(typeName);
        if (useMaxLength && maxLength > 0) {
            decl.append('(').append(maxLength).append(')');
        }
        if (notNull) {
            decl.append(" NOT NULL"); //$NON-NLS-1$
        }

        return decl;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Column name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getTypeName())) {
            throw new DBException("Column type name cannot be empty");
        }
    }

    private static DBSDataType findDataType(DBPDataSource dataSource, String typeName)
    {
        if (dataSource instanceof DBPDataTypeProvider) {
            return ((DBPDataTypeProvider) dataSource).getDataType(typeName);
        }
        return null;
    }

    protected static DBSDataType findBestDataType(DBPDataSource dataSource, String ... typeNames)
    {
        if (dataSource instanceof DBPDataTypeProvider) {
            return DBUtils.findBestDataType(((DBPDataTypeProvider) dataSource).getDataTypes(), typeNames);
        }
        return null;
    }

}


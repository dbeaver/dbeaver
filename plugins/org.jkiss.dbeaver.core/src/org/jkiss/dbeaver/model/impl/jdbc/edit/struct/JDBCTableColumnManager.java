/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;

/**
 * JDBC table column manager
 */
public abstract class JDBCTableColumnManager<OBJECT_TYPE extends JDBCTableColumn<TABLE_TYPE>, TABLE_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE, TABLE_TYPE>
{

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectChangeCommand command)
    {
        final TABLE_TYPE table = command.getObject().getTable();
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Create new table column",
                "ALTER TABLE " + table.getFullQualifiedName() + " ADD "  + getNestedDeclaration(table, command)) };
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Drop table column", "ALTER TABLE " + command.getObject().getTable().getFullQualifiedName() +
                    " DROP COLUMN " + command.getObject().getName())
        };
    }

    protected StringBuilder getNestedDeclaration(TABLE_TYPE owner, ObjectChangeCommand command)
    {
        OBJECT_TYPE column = command.getObject();

        // Create column
        String columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), column.getName());

        final String typeName = column.getTypeName();
        boolean useMaxLength = false;
        final DBSDataType dataType = findDataType(column.getDataSource(), typeName);
        if (dataType == null) {
            log.debug("Type name '" + typeName + "' is not supported by driver");
        } else if (dataType.getDataKind() == DBSDataKind.STRING) {
            if (typeName.indexOf('(') == -1) {
                useMaxLength = true;
            }
        }

        final boolean notNull = column.isNotNull();
        final long maxLength = column.getMaxLength();
        StringBuilder decl = new StringBuilder(40);
        decl.append(columnName).append(' ').append(typeName);
        if (useMaxLength && maxLength > 0) {
            decl.append('(').append(maxLength).append(')');
        }
        if (notNull) {
            decl.append(" NOT NULL");
        }

        return decl;
    }

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
        for (DBSDataType type : dataSource.getInfo().getSupportedDataTypes()) {
            if (type.getName().equalsIgnoreCase(typeName)) {
                return type;
            }
        }
        return null;
    }

    protected static DBSDataType findBestDataType(DBPDataSource dataSource, String ... typeNames)
    {
        for (String testType : typeNames) {
            for (DBSDataType type : dataSource.getInfo().getSupportedDataTypes()) {
                if (type.getName().equalsIgnoreCase(testType)) {
                    return type;
                }
            }
        }
        return null;
    }

}


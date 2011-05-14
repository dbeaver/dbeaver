/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.prop.DBECommandDeleteObject;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JDBC table column manager
 */
public abstract class JDBCTableColumnManager<OBJECT_TYPE extends JDBCTableColumn, CONTAINER_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE>
    implements DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE>, JDBCNestedEditor<OBJECT_TYPE, JDBCTable>
{

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext commandContext, CONTAINER_TYPE parent, Object copyFrom)
    {
        OBJECT_TYPE newColumn = createNewTableColumn(parent, copyFrom);

        makeInitialCommands(newColumn, commandContext, new CommandCreateTableColumn(newColumn));

        return newColumn;
    }

    public void deleteObject(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options)
    {
        commandContext.addCommand(new CommandDropTableColumn(object), new DeleteObjectReflector<OBJECT_TYPE>(), true);
    }


    @Override
    protected IDatabasePersistAction[] makeObjectChangeActions(ObjectChangeCommand<OBJECT_TYPE> command)
    {
        final JDBCTable table = command.getObject().getTable();
        final OBJECT_TYPE column = command.getObject();
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        boolean newObject = !column.isPersisted();
        if (newObject) {
            actions.add( new AbstractDatabasePersistAction(
                "Create new table column",
                "ALTER TABLE " + table.getFullQualifiedName() + " ADD "  + getNestedDeclaration(table, command)) );
        }
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

    public StringBuilder getNestedDeclaration(JDBCTable owner, ObjectChangeCommand<OBJECT_TYPE> command)
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

    protected void validateObjectProperties(ObjectChangeCommand<OBJECT_TYPE> command)
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

    protected abstract OBJECT_TYPE createNewTableColumn(CONTAINER_TYPE parent, Object copyFrom);

    private class CommandCreateTableColumn extends ObjectSaveCommand<OBJECT_TYPE> {
        protected CommandCreateTableColumn(OBJECT_TYPE table)
        {
            super(table, "Create table column");
        }
    }

    private class CommandDropTableColumn extends DBECommandDeleteObject<OBJECT_TYPE> {
        protected CommandDropTableColumn(OBJECT_TYPE table)
        {
            super(table, "Drop table column");
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction(
                    "Drop table column", "ALTER TABLE " + getObject().getTable().getFullQualifiedName() + " DROP COLUMN " + getObject().getName())
            };
        }
    }


}


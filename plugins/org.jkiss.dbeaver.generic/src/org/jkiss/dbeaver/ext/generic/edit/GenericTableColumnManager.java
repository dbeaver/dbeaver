/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableColumnManager;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic table column manager
 */
public class GenericTableColumnManager extends JDBCTableColumnManager<GenericTableColumn, GenericTable> {


    @Override
    protected IDatabasePersistAction[] makePersistActions(ObjectChangeCommand<GenericTableColumn> command)
    {
        final GenericTable table = command.getObject().getTable();
        final GenericTableColumn column = command.getObject();
        final Object columnName = CommonUtils.toString(command.getProperty(DBConstants.PROP_ID_NAME));
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        boolean newObject = !column.isPersisted();
        if (newObject) {
            actions.add( new AbstractDatabasePersistAction(
                "Create new table column",
                "ALTER TABLE " + table.getFullQualifiedName() + " ADD "  + getInlineColumnDeclaration(command)) );
        }
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

    @Override
    protected GenericTableColumn createNewTableColumn(GenericTable parent, Object copyFrom)
    {
        DBSDataType columnType = findBestDataType(parent.getDataSource(), "varchar", "char", "integer", "number");

        final GenericTableColumn column = new GenericTableColumn(parent);
        column.setName("NewColumn");
        column.setTypeName(columnType == null ? "INTEGER" : columnType.getName());
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBSDataKind.STRING ? 100 : 0);
        column.setValueType(columnType == null ? Types.INTEGER : columnType.getTypeNumber());
        column.setOrdinalPosition(-1);
        return column;
    }

    private static DBSDataType findBestDataType(GenericDataSource dataSource, String ... typeNames)
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

    public static String getInlineColumnDeclaration(ObjectChangeCommand command)
    {
        GenericTableColumn column = (GenericTableColumn) command.getObject();

        // Create column
        String columnName = DBUtils.getQuotedIdentifier(
            column.getDataSource(),
            CommonUtils.toString(command.getProperty(DBConstants.PROP_ID_NAME)),
            column.getDataSource().getContainer().getPreferenceStore().getBoolean(PrefConstants.META_CASE_SENSITIVE));

        final Object typeName = CommonUtils.toString(command.getProperty(DBConstants.PROP_ID_TYPE_NAME));
        return columnName + " " + typeName;
    }
}

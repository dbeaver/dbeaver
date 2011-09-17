/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.util.List;

/**
 * MySQL ENUM value handler
 */
public class MySQLEnumValueHandler extends JDBCAbstractValueHandler {

    public static final MySQLEnumValueHandler INSTANCE = new MySQLEnumValueHandler();

    public String getValueDisplayString(DBSTypedObject column, Object value) {
        if (!(value instanceof MySQLTypeEnum)) {
            return super.getValueDisplayString(column, value);
        }
        String strValue = ((MySQLTypeEnum) value).getValue();
        return strValue == null ? DBConstants.NULL_VALUE_LABEL : strValue;
    }

    protected Object getColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject column,
        int columnIndex)
        throws SQLException
    {
        DBSTableColumn tableColumn = null;
        if (column instanceof DBSTableColumn) {
            tableColumn = (DBSTableColumn)column;
        } else if (column instanceof DBCColumnMetaData) {
            try {
                tableColumn = ((DBCColumnMetaData)column).getTableColumn(context.getProgressMonitor());
            }
            catch (DBException e) {
                throw new SQLException(e);
            }
        }
        if (tableColumn == null) {
            throw new SQLException("Could not find table column for column '" + columnIndex + "'");
        }
        MySQLTableColumn enumColumn;
        if (tableColumn instanceof MySQLTableColumn) {
            enumColumn = (MySQLTableColumn)tableColumn;
        } else {
            throw new SQLException("Bad column type: " + tableColumn.getClass().getName());
        }
        return new MySQLTypeEnum(enumColumn, resultSet.getString(columnIndex));
    }

    @Override
    public void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value)
        throws SQLException
    {
        MySQLTypeEnum e = (MySQLTypeEnum)value;
        if (e == null || e.isNull()) {
            statement.setNull(paramIndex, paramType.getValueType());
        } else {
            statement.setString(paramIndex, e.getValue());
        }
    }

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            final MySQLTypeEnum value = (MySQLTypeEnum)controller.getValue();

            Combo editor = new Combo(controller.getInlinePlaceholder(), SWT.READ_ONLY);
            List<String> enumValues = value.getColumn().getEnumValues();
            //editor.add("");
            if (enumValues != null) {
                for (String enumValue : enumValues) {
                    editor.add(enumValue);
                }
            }
            editor.setText(value.isNull() ? "" : value.getValue());
            if (editor.getSelectionIndex() < 0) {
                editor.select(0);
            }
            editor.setFocus();
            initInlineControl(controller, editor, new ValueExtractor<Combo>() {
                public Object getValueFromControl(Combo control)
                {
                    int selIndex = control.getSelectionIndex();
                    if (selIndex < 0) {
                        return new MySQLTypeEnum(value.getColumn(), null);
                    } else if (selIndex == 0) {
                        return new MySQLTypeEnum(value.getColumn(), null);
                    } else {
                        return new MySQLTypeEnum(value.getColumn(), control.getItem(selIndex));
                    }
                }
            });
            return true;
        } else {
            EnumViewDialog dialog = new EnumViewDialog(controller);
            dialog.open();
            return true;
        }
    }

    public int getFeatures()
    {
        return FEATURE_VIEWER | FEATURE_EDITOR | FEATURE_INLINE_EDITOR;
    }

    public Class getValueObjectType()
    {
        return MySQLTypeEnum.class;
    }

    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
        throws DBCException
    {
        // String are immutable
        MySQLTypeEnum e = (MySQLTypeEnum)value;
        return new MySQLTypeEnum(e.getColumn(), e.getValue());
    }

/*
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "max_length",
            "Max Length",
            controller.getColumnMetaData().getMaxLength());
    }
*/

}
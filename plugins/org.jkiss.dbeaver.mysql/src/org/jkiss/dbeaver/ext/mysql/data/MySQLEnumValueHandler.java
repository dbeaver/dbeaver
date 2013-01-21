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
package org.jkiss.dbeaver.ext.mysql.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.List;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;

/**
 * MySQL ENUM value handler
 */
public class MySQLEnumValueHandler extends JDBCAbstractValueHandler {

    public static final MySQLEnumValueHandler INSTANCE = new MySQLEnumValueHandler();

    @Override
    public Object getValueFromObject(DBCExecutionContext context, DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return null;
        } else if (object instanceof MySQLTypeEnum) {
            return copy ? new MySQLTypeEnum((MySQLTypeEnum) object) : object;
        } else if (object instanceof String && type instanceof MySQLTableColumn) {
            return new MySQLTypeEnum((MySQLTableColumn) type, (String) object);
        } else {
            throw new DBCException("Unsupported ");
        }
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value)
    {
        if (!(value instanceof MySQLTypeEnum)) {
            return super.getValueDisplayString(column, value);
        }
        String strValue = ((MySQLTypeEnum) value).getValue();
        return strValue == null ? DBConstants.NULL_VALUE_LABEL : strValue;
    }

    @Override
    protected Object fetchColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws SQLException
    {
        DBSEntityAttribute attribute = null;
        if (type instanceof DBSTableColumn) {
            attribute = (DBSTableColumn) type;
        } else if (type instanceof DBCAttributeMetaData) {
            try {
                attribute = ((DBCAttributeMetaData) type).getAttribute(context.getProgressMonitor());
            } catch (DBException e) {
                throw new SQLException(e);
            }
        }
        if (attribute == null) {
            throw new SQLException("Could not find table column for column '" + index + "'");
        }
        MySQLTableColumn enumColumn;
        if (attribute instanceof MySQLTableColumn) {
            enumColumn = (MySQLTableColumn) attribute;
        } else {
            throw new SQLException("Bad column type: " + attribute.getClass().getName());
        }
        return new MySQLTypeEnum(enumColumn, resultSet.getString(index));
    }

    @Override
    public void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value)
        throws SQLException
    {
        // Sometimes we have String in value instead of MySQLTypeEnum
        // It happens when we edit result sets as MySQL reports RS column type as CHAR for enum/set types
        String strValue;
        if (value instanceof MySQLTypeEnum) {
            strValue = ((MySQLTypeEnum) value).getValue();
        } else {
            strValue = CommonUtils.toString(value);
        }
        if (strValue == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            statement.setString(paramIndex, strValue);
        }
    }

    @Override
    public DBDValueEditor createEditor(final DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            {
                final MySQLTableColumn column = ((MySQLTypeEnum) controller.getValue()).getColumn();
                final Combo editor = new Combo(controller.getEditPlaceholder(), SWT.READ_ONLY);
                initInlineControl(controller, editor, new ValueExtractor<Combo>() {
                    @Override
                    public Object getValueFromControl(Combo control)
                    {
                        int selIndex = control.getSelectionIndex();
                        if (selIndex < 0) {
                            return new MySQLTypeEnum(column, null);
                        } else {
                            return new MySQLTypeEnum(column, control.getItem(selIndex));
                        }
                    }
                });
                Collection<String> enumValues = column.getEnumValues();
                if (enumValues != null) {
                    for (String enumValue : enumValues) {
                        editor.add(enumValue);
                    }
                }
                if (editor.getSelectionIndex() < 0) {
                    editor.select(0);
                }
                return new DBDValueEditor() {
                    @Override
                    public void refreshValue()
                    {
                        MySQLTypeEnum value = (MySQLTypeEnum) controller.getValue();
                        editor.setText(value.isNull() ? "" : value.getValue());
                    }
                };
            }
            case PANEL:
            {
                final MySQLTableColumn column = ((MySQLTypeEnum) controller.getValue()).getColumn();
                final List editor = new List(controller.getEditPlaceholder(), SWT.READ_ONLY);
                initInlineControl(controller, editor, new ValueExtractor<List>() {
                    @Override
                    public Object getValueFromControl(List control)
                    {
                        int selIndex = control.getSelectionIndex();
                        if (selIndex < 0) {
                            return new MySQLTypeEnum(column, null);
                        } else {
                            return new MySQLTypeEnum(column, control.getItem(selIndex));
                        }
                    }
                });
                Collection<String> enumValues = column.getEnumValues();
                if (enumValues != null) {
                    for (String enumValue : enumValues) {
                        editor.add(enumValue);
                    }
                }
                if (editor.getSelectionIndex() < 0) {
                    editor.select(0);
                }
                if (controller.getEditType() == DBDValueController.EditType.INLINE) {
                    editor.setFocus();
                }
                return new DBDValueEditor() {
                    @Override
                    public void refreshValue()
                    {
                        MySQLTypeEnum value = (MySQLTypeEnum) controller.getValue();
                        if (value.isNull()) {
                            editor.setSelection(-1);
                        }
                        int itemCount = editor.getItemCount();
                        for (int i = 0 ; i < itemCount; i++) {
                            if (editor.getItem(i).equals(value.getValue())) {
                                editor.setSelection(i);
                                break;
                            }
                        }
                    }
                };
            }
            case EDITOR:
                return new EnumViewDialog(controller);
            default:
                return null;
        }
    }

    @Override
    public int getFeatures()
    {
        return FEATURE_VIEWER | FEATURE_EDITOR | FEATURE_INLINE_EDITOR;
    }

    @Override
    public Class getValueObjectType()
    {
        return MySQLTypeEnum.class;
    }

/*
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "max_length",
            "Max Length",
            controller.getAttributeMetaData().getMaxLength());
    }
*/

}
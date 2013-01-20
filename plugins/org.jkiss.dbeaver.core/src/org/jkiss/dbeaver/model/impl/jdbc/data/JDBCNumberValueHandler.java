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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.data.DefaultDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.data.NumberViewDialog;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Types;

/**
 * JDBC number value handler
 */
public class JDBCNumberValueHandler extends JDBCAbstractValueHandler {

    private static final String TYPE_NAME_NUMBER = "number"; //$NON-NLS-1$
    private static final int MAX_NUMBER_LENGTH = 100;

    private static final String BAD_DOUBLE_VALUE = "2.2250738585072012e-308"; //$NON-NLS-1$

    private DBDDataFormatter formatter;

    public JDBCNumberValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        try {
            formatter = formatterProfile.createFormatter(TYPE_NAME_NUMBER);
        } catch (Exception e) {
            log.error("Could not create formatter for number value handler", e); //$NON-NLS-1$
            formatter = DefaultDataFormatter.INSTANCE;
        }
    }

    /**
     * NumberFormat is not thread safe thus this method is synchronized.
     */
    @Override
    public synchronized String getValueDisplayString(DBSTypedObject column, Object value)
    {
        return value == null ? DBConstants.NULL_VALUE_LABEL : formatter.formatValue(value);
    }

    @Override
    protected Object getColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject column,
        int columnIndex)
        throws DBCException, SQLException
    {
        Number value;
        switch (column.getTypeID()) {
            case java.sql.Types.BIGINT:
                value = resultSet.getLong(columnIndex);
                break;
            case java.sql.Types.DOUBLE:
            case java.sql.Types.REAL:
                value = resultSet.getDouble(columnIndex);
                break;
            case java.sql.Types.FLOAT:
                value = resultSet.getFloat(columnIndex);
                break;
            case java.sql.Types.INTEGER:
                value = resultSet.getInt(columnIndex);
                break;
            case java.sql.Types.SMALLINT:
                value = resultSet.getShort(columnIndex);
                break;
            case java.sql.Types.TINYINT:
            case java.sql.Types.BIT:
                value = resultSet.getByte(columnIndex);
                break;
            default:
                // Here may be any numeric value. BigDecimal or BigInteger for example
                boolean gotValue = false;
                value = null;
                try {
                    Object objectValue = resultSet.getObject(columnIndex);
                    if (objectValue == null || objectValue instanceof Number) {
                        value = (Number) objectValue;
                        gotValue = true;
                    }
                } catch (SQLException e) {
                    log.debug(e);
                }
                if (value == null && !gotValue) {
                    if (column.getScale() > 0) {
                        value = resultSet.getDouble(columnIndex);
                    } else {
                        value = resultSet.getLong(columnIndex);
                    }
                }

                break;
        }
        if (resultSet.wasNull()) {
            return null;
        } else {
            return value;
        }
    }

    @Override
    protected void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement statement, DBSTypedObject paramType,
                                 int paramIndex, Object value) throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            Number number = (Number) value;
            switch (paramType.getTypeID()) {
                case java.sql.Types.BIGINT:
                    statement.setLong(paramIndex, number.longValue());
                    break;
                case java.sql.Types.FLOAT:
                    statement.setFloat(paramIndex, number.floatValue());
                    break;
                case java.sql.Types.DOUBLE:
                case java.sql.Types.REAL:
                    statement.setDouble(paramIndex, number.doubleValue());
                    break;
                case java.sql.Types.INTEGER:
                    statement.setInt(paramIndex, number.intValue());
                    break;
                case java.sql.Types.SMALLINT:
                    statement.setShort(paramIndex, number.shortValue());
                    break;
                case java.sql.Types.TINYINT:
                case java.sql.Types.BIT:
                    statement.setByte(paramIndex, number.byteValue());
                    break;
                case java.sql.Types.NUMERIC:
                    if (number instanceof BigDecimal) {
                        statement.setBigDecimal(paramIndex, (BigDecimal) number);
                    } else {
                        statement.setDouble(paramIndex, number.doubleValue());
                    }
                    break;
                default:
                    if (paramType.getScale() > 0) {
                        statement.setDouble(paramIndex, number.doubleValue());
                    } else {
                        statement.setLong(paramIndex, number.longValue());
                    }
                    break;
            }
        }
    }

    @Override
    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
                final Object value = controller.getValue();

                if (controller.getAttributeMetaData().getTypeID() == java.sql.Types.BIT) {
                    CCombo editor = new CCombo(controller.getEditPlaceholder(), SWT.READ_ONLY);
                    initInlineControl(controller, editor, new ValueExtractor<CCombo>() {
                        @Override
                        public Object getValueFromControl(CCombo control)
                        {
                            switch (control.getSelectionIndex()) {
                                case 0:
                                    return (byte) 0;
                                case 1:
                                    return (byte) 1;
                                default:
                                    return null;
                            }
                        }
                    });
                    editor.add("0"); //$NON-NLS-1$
                    editor.add("1"); //$NON-NLS-1$
                    editor.setText(value == null ? "0" : value.toString()); //$NON-NLS-1$
                    editor.setFocus();
                } else {
                    Text editor = new Text(controller.getEditPlaceholder(), SWT.BORDER);
                    initInlineControl(controller, editor, new ValueExtractor<Text>() {
                        @Override
                        public Object getValueFromControl(Text control)
                        {
                            String text = control.getText();
                            if (CommonUtils.isEmpty(text)) {
                                return null;
                            }
                            return convertStringToNumber(text, value, controller.getAttributeMetaData());
                        }
                    });
                    editor.setText(value == null ? "" : value.toString()); //$NON-NLS-1$
                    editor.setEditable(!controller.isReadOnly());
                    editor.setTextLimit(MAX_NUMBER_LENGTH);
                    editor.selectAll();
                    editor.setFocus();
                    switch (controller.getAttributeMetaData().getTypeID()) {
                        case java.sql.Types.BIGINT:
                        case java.sql.Types.INTEGER:
                        case java.sql.Types.SMALLINT:
                        case java.sql.Types.TINYINT:
                        case java.sql.Types.BIT:
                            editor.addVerifyListener(UIUtils.INTEGER_VERIFY_LISTENER);
                            break;
                        default:
                            editor.addVerifyListener(UIUtils.NUMBER_VERIFY_LISTENER);
                            break;
                    }
                }
                return true;
            case EDITOR:
                NumberViewDialog dialog = new NumberViewDialog(controller);
                dialog.open();
                return true;
            default:
                return false;
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
        return Number.class;
    }

    @Override
    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
        throws DBCException
    {
        // Number are immutable
        return value;
    }

    @Override
    public Object getValueFromClipboard(DBSTypedObject column, Clipboard clipboard)
    {
        String strValue = (String) clipboard.getContents(TextTransfer.getInstance());
        if (CommonUtils.isEmpty(strValue)) {
            return null;
        }
        Number value;
        try {
            switch (column.getTypeID()) {
                case java.sql.Types.BIGINT:
                    value = Long.valueOf(strValue);
                    break;
                case java.sql.Types.DOUBLE:
                case java.sql.Types.REAL:
                    value = Double.valueOf(strValue);
                    break;
                case java.sql.Types.FLOAT:
                    value = Float.valueOf(strValue);
                    break;
                case java.sql.Types.INTEGER:
                    value = Integer.valueOf(strValue);
                    break;
                case java.sql.Types.SMALLINT:
                    value = Short.valueOf(strValue);
                    break;
                case java.sql.Types.TINYINT:
                case java.sql.Types.BIT:
                    value = Byte.valueOf(strValue);
                    break;
                case Types.NUMERIC:
                    value = new BigDecimal(strValue);
                    break;
                default:
                    // Here may be any numeric value. BigDecimal or BigInteger for example
                    value = new BigDecimal(strValue);
                    break;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return value;
    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "precision", //$NON-NLS-1$
            CoreMessages.model_jdbc_precision,
            controller.getAttributeMetaData().getPrecision());
        propertySource.addProperty(
            "scale", //$NON-NLS-1$
            CoreMessages.model_jdbc_scale,
            controller.getAttributeMetaData().getScale());
    }

    public static Number convertStringToNumber(String text, Object originalValue, DBSTypedObject type)
    {
        if (text == null || text.length() == 0) {
            return null;
        }
        try {
            if (originalValue instanceof Long) {
                return Long.valueOf(text);
            } else if (originalValue instanceof Integer) {
                return Integer.valueOf(text);
            } else if (originalValue instanceof Short) {
                return Short.valueOf(text);
            } else if (originalValue instanceof Byte) {
                return Byte.valueOf(text);
            } else if (originalValue instanceof Float) {
                return Float.valueOf(text);
            } else if (originalValue instanceof Double) {
                return Double.valueOf(text);
            } else if (originalValue instanceof BigInteger) {
                return new BigInteger(text);
            } else if (originalValue instanceof BigDecimal) {
                return new BigDecimal(text);
            } else {
                switch (type.getTypeID()) {
                    case java.sql.Types.BIGINT:
                        return Long.valueOf(text);
                    case java.sql.Types.DECIMAL:
                    case java.sql.Types.DOUBLE:
                    case java.sql.Types.REAL:
                        return toDouble(text);
                    case java.sql.Types.FLOAT:
                        return Float.valueOf(text);
                    case java.sql.Types.INTEGER:
                        return Integer.valueOf(text);
                    case java.sql.Types.SMALLINT:
                        return Short.valueOf(text);
                    case java.sql.Types.TINYINT:
                        return Byte.valueOf(text);
                    case java.sql.Types.NUMERIC:
                        return new BigDecimal(text);
                    default:
                        if (type.getScale() > 0) {
                            return toDouble(text);
                        } else {
                            return Long.valueOf(text);
                        }
                }
            }
        } catch (NumberFormatException e) {
            log.error("Bad numeric value '" + text + "' - " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
    }

    private static Number toDouble(String text)
    {
        if (text.equals(BAD_DOUBLE_VALUE)) {
            return Double.MIN_VALUE;
        }
        return Double.valueOf(text);
    }
}
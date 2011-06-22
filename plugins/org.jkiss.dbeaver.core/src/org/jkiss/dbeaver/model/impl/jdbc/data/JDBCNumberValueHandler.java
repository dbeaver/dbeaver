/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.utils.CommonUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.data.DefaultDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.data.NumberViewDialog;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.SQLException;

/**
 * JDBC number value handler
 */
public class JDBCNumberValueHandler extends JDBCAbstractValueHandler {

    private static final String TYPE_NAME_NUMBER = "number";
    private static final int MAX_NUMBER_LENGTH = 100;

    private static final String BAD_DOUBLE_VALUE = "2.2250738585072012e-308";

    private DBDDataFormatter formatter;

    public JDBCNumberValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        try {
            formatter = formatterProfile.createFormatter(TYPE_NAME_NUMBER);
        } catch (Exception e) {
            log.error("Could not create formatter for number value handler", e);
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

    protected Object getColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject column,
                                    int columnIndex)
        throws DBCException, SQLException
    {
        Number value;
        switch (column.getValueType()) {
        case java.sql.Types.BIGINT:
            value = resultSet.getLong(columnIndex);
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

    protected void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement statement, DBSTypedObject paramType,
                                 int paramIndex, Object value) throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getValueType());
        } else {
            Number number = (Number)value;
            switch (paramType.getValueType()) {
            case java.sql.Types.BIGINT:
                statement.setLong(paramIndex, number.longValue());
                break;
            case java.sql.Types.FLOAT:
                statement.setFloat(paramIndex, number.floatValue());
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

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            Object value = controller.getValue();

            if (controller.getColumnMetaData().getValueType() == java.sql.Types.BIT) {
                CCombo editor = new CCombo(controller.getInlinePlaceholder(), SWT.READ_ONLY);
                editor.add("0");
                editor.add("1");
                editor.setText(value == null ? "0" : value.toString());
                editor.setFocus();
                initInlineControl(controller, editor, new ValueExtractor<CCombo>() {
                    public Object getValueFromControl(CCombo control)
                    {
                        switch (control.getSelectionIndex()) {
                            case 0: return (byte)0;
                            case 1: return (byte)1;
                            default: return null;
                        }
                    }
                });
            } else {
                Text editor = new Text(controller.getInlinePlaceholder(), SWT.BORDER);
                editor.setText(value == null ? "" : value.toString());
                editor.setEditable(!controller.isReadOnly());
                editor.setTextLimit(MAX_NUMBER_LENGTH);
                editor.selectAll();
                editor.setFocus();
                switch (controller.getColumnMetaData().getValueType()) {
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

                initInlineControl(controller, editor, new ValueExtractor<Text>() {
                    public Object getValueFromControl(Text control)
                    {
                        String text = control.getText();
                        if (CommonUtils.isEmpty(text)) {
                            return null;
                        }
                        return convertStringToNumber(text, controller.getColumnMetaData());
                    }
                });
            }
            return true;
        } else {
            NumberViewDialog dialog = new NumberViewDialog(controller);
            dialog.open();
            return true;
        }
    }

    public Class getValueObjectType()
    {
        return Number.class;
    }

    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
        throws DBCException
    {
        // Number are immutable
        return value;
    }

    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "precision",
            "Precision",
            controller.getColumnMetaData().getPrecision());
        propertySource.addProperty(
            "scale",
            "Scale",
            controller.getColumnMetaData().getScale());
    }


    public static Number convertStringToNumber(String text, DBSTypedObject type)
    {
        if (text == null || text.length() == 0) {
            return null;
        }
        try {
            switch (type.getValueType()) {
            case java.sql.Types.BIGINT:
                return Long.valueOf(text);
            case java.sql.Types.DECIMAL:
            case java.sql.Types.DOUBLE:
            case java.sql.Types.REAL:
                // Handle Java error
                return toDouble(text);
            case java.sql.Types.FLOAT:
                return Float.valueOf(text);
            case java.sql.Types.INTEGER:
                return Integer.valueOf(text);
            case java.sql.Types.SMALLINT:
                return Short.valueOf(text);
            case java.sql.Types.TINYINT:
                return Byte.valueOf(text);
            default:
                if (type.getScale() > 0) {
                    return toDouble(text);
                } else {
                    return Long.valueOf(text);
                }
            }
        }
        catch (NumberFormatException e) {
            log.error("Bad numeric value '" + text + "' - " + e.getMessage());
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
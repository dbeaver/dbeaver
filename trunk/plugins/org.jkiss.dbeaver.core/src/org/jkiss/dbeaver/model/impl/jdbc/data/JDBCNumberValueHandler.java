/*
 * Copyright (C) 2010-2014 Serge Rieder
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.BaseValueEditor;
import org.jkiss.dbeaver.model.impl.data.DefaultDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Locale;

/**
 * JDBC number value handler
 */
public class JDBCNumberValueHandler extends JDBCAbstractValueHandler {

    private static final String TYPE_NAME_NUMBER = "number"; //$NON-NLS-1$
    private static final int MAX_NUMBER_LENGTH = 100;

    private static final String BAD_DOUBLE_VALUE = "2.2250738585072012e-308"; //$NON-NLS-1$

    private Locale locale;
    private DBDDataFormatter formatter;

    public JDBCNumberValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        try {
            locale = formatterProfile.getLocale();
            formatter = formatterProfile.createFormatter(TYPE_NAME_NUMBER);
        } catch (Exception e) {
            log.error("Could not create formatter for number value handler", e); //$NON-NLS-1$
            formatter = DefaultDataFormatter.INSTANCE;
        }
    }

    /**
     * NumberFormat is not thread safe thus this method is synchronized.
     */
    @NotNull
    @Override
    public synchronized String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format)
    {
        if (value == null) {
            return DBUtils.getDefaultValueDisplayString(null, format);
        }
        if (format == DBDDisplayFormat.NATIVE || format == DBDDisplayFormat.EDIT) {
            return value.toString();
        }
        return formatter.formatValue(value);
    }

    @Nullable
    @Override
    protected Object fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws DBCException, SQLException
    {
        Number value;
        switch (type.getTypeID()) {
            case java.sql.Types.DOUBLE:
            case java.sql.Types.REAL:
                value = resultSet.getDouble(index);
                break;
            case java.sql.Types.FLOAT:
                value = resultSet.getFloat(index);
                break;
            case java.sql.Types.INTEGER:
                value = resultSet.getInt(index);
                break;
            case java.sql.Types.SMALLINT:
                value = resultSet.getShort(index);
                break;
            case java.sql.Types.TINYINT:
                value = resultSet.getShort(index);
                break;
            case java.sql.Types.BIT:
                value = resultSet.getByte(index);
                break;
            default:
                // Here may be any numeric value. BigDecimal or BigInteger for example
                boolean gotValue = false;
                value = null;
                try {
                    Object objectValue = resultSet.getObject(index);
                    if (objectValue == null || objectValue instanceof Number) {
                        value = (Number) objectValue;
                        gotValue = true;
                    }
                } catch (SQLException e) {
                    log.debug(e);
                }
                if (value == null && !gotValue) {
                    if (type.getScale() > 0) {
                        value = resultSet.getDouble(index);
                    } else {
                        value = resultSet.getLong(index);
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
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType,
                                 int paramIndex, Object value) throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            Number number = (Number) value;
            switch (paramType.getTypeID()) {
                case java.sql.Types.BIGINT:
                    if (number instanceof BigInteger) {
                        statement.setBigDecimal(paramIndex, new BigDecimal((BigInteger) number));
                    } else {
                        statement.setLong(paramIndex, number.longValue());
                    }
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
                    statement.setShort(paramIndex, number.shortValue());
                    break;
                case java.sql.Types.BIT:
                    statement.setByte(paramIndex, number.byteValue());
                    break;
                case java.sql.Types.NUMERIC:
                    if (number instanceof Long) {
                        statement.setLong(paramIndex, number.intValue());
                    } else if (number instanceof Integer) {
                        statement.setInt(paramIndex, number.intValue());
                    } else if (number instanceof Short) {
                        statement.setShort(paramIndex, number.shortValue());
                    } else if (number instanceof Byte) {
                        statement.setByte(paramIndex, number.byteValue());
                    } else if (number instanceof Float) {
                        statement.setFloat(paramIndex, number.floatValue());
                    } else if (number instanceof BigDecimal) {
                        statement.setBigDecimal(paramIndex, (BigDecimal) number);
                    } else if (number instanceof BigInteger) {
                        statement.setBigDecimal(paramIndex, new BigDecimal((BigInteger) number));
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

    @Nullable
    @Override
    public DBDValueEditor createEditor(@NotNull DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            case PANEL:
                if (controller.getValueType().getDataKind() == DBPDataKind.BOOLEAN) {
                    return new BaseValueEditor<Combo>(controller) {
                        @Override
                        protected Combo createControl(Composite editPlaceholder)
                        {
                            final Combo editor = new Combo(valueController.getEditPlaceholder(), SWT.READ_ONLY);
                            editor.add("0"); //$NON-NLS-1$
                            editor.add("1"); //$NON-NLS-1$
                            editor.setEnabled(!valueController.isReadOnly());
                            return editor;
                        }
                        @Override
                        public void primeEditorValue(@Nullable Object value) throws DBException
                        {
                            control.setText(value == null ? "0" : value.toString()); //$NON-NLS-1$
                        }
                        @Override
                        public Object extractEditorValue()
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
                    };
                } else {
                    return new BaseValueEditor<Text>(controller) {
                        @Override
                        protected Text createControl(Composite editPlaceholder)
                        {
                            final Text editor = new Text(valueController.getEditPlaceholder(), SWT.BORDER);
                            editor.setEditable(!valueController.isReadOnly());
                            editor.setTextLimit(MAX_NUMBER_LENGTH);
                            switch (valueController.getValueType().getTypeID()) {
                                case java.sql.Types.BIGINT:
                                case java.sql.Types.INTEGER:
                                case java.sql.Types.SMALLINT:
                                case java.sql.Types.TINYINT:
                                case java.sql.Types.BIT:
                                    editor.addVerifyListener(UIUtils.getIntegerVerifyListener(locale));
                                    break;
                                default:
                                    editor.addVerifyListener(UIUtils.getNumberVerifyListener(locale));
                                    break;
                            }
                            return editor;
                        }
                        @Override
                        public void primeEditorValue(@Nullable Object value) throws DBException
                        {
                            if (value != null) {
                                control.setText(getValueDisplayString(valueController.getValueType(), value, DBDDisplayFormat.UI));
                            } else {
                                control.setText("");
                            }
                            if (valueController.getEditType() == DBDValueController.EditType.INLINE) {
                                control.selectAll();
                            }
                        }
                        @Nullable
                        @Override
                        public Object extractEditorValue()
                        {
                            String text = control.getText();
                            if (CommonUtils.isEmpty(text)) {
                                return null;
                            }
                            return convertStringToNumber(formatter, text, valueController.getValue(), valueController.getValueType());
                        }
                    };
                }
            case EDITOR:
                return new DefaultValueViewDialog(controller);
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
        return Number.class;
    }

    @Nullable
    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return null;
        } else if (object instanceof Number) {
            return object;
        } else if (object instanceof String) {
            return convertStringToNumber(formatter, (String)object, null, type);
        } else {
            log.warn("Unrecognized type '" + object.getClass().getName() + "' - can't convert to numeric");
            return null;
        }
    }

    @Nullable
    public static Number convertStringToNumber(DBDDataFormatter formatter, String text, @Nullable Object originalValue, DBSTypedObject type)
    {
        if (text == null || text.length() == 0) {
            return null;
        }
        Class<?> hintType = null;
        try {
            if (originalValue instanceof Number) {
                if (originalValue instanceof Long) {
                    hintType = Long.class;
                    return Long.valueOf(text);
                } else if (originalValue instanceof Integer) {
                    hintType = Integer.class;
                    return Integer.valueOf(text);
                } else if (originalValue instanceof Short) {
                    hintType = Short.class;
                    return Short.valueOf(text);
                } else if (originalValue instanceof Byte) {
                    hintType = Byte.class;
                    return Byte.valueOf(text);
                } else if (originalValue instanceof Float) {
                    hintType = Float.class;
                    return Float.valueOf(text);
                } else if (originalValue instanceof Double) {
                    hintType = Double.class;
                    return Double.valueOf(text);
                } else if (originalValue instanceof BigInteger) {
                    hintType = BigInteger.class;
                    return new BigInteger(text);
                } else {
                    hintType = BigDecimal.class;
                    return new BigDecimal(text);
                }
            } else {
                switch (type.getTypeID()) {
                    case java.sql.Types.BIGINT:
                        hintType = Long.class;
                        try {
                            return Long.parseLong(text);
                        } catch (NumberFormatException e) {
                            return new BigInteger(text);
                        }
                    case java.sql.Types.DECIMAL:
                    case java.sql.Types.DOUBLE:
                    case java.sql.Types.REAL:
                        hintType = Double.class;
                        return toDouble(text);
                    case java.sql.Types.FLOAT:
                        hintType = Float.class;
                        return Float.valueOf(text);
                    case java.sql.Types.INTEGER:
                        hintType = Integer.class;
                        return Integer.valueOf(text);
                    case java.sql.Types.SMALLINT:
                    case java.sql.Types.TINYINT:
                        hintType = Short.class;
                        return Short.valueOf(text);
                    case java.sql.Types.NUMERIC:
                        hintType = BigDecimal.class;
                        return new BigDecimal(text);
                    default:
                        if (type.getScale() > 0) {
                            hintType = Double.class;
                            return toDouble(text);
                        } else {
                            hintType = Long.class;
                            return Long.valueOf(text);
                        }
                }
            }
        } catch (NumberFormatException e) {
            log.debug("Bad numeric value '" + text + "' - " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            try {
                return (Number)formatter.parseValue(text, hintType);
            } catch (ParseException e1) {
                log.debug("Can't parse numeric value [" + text + "] using formatter", e);
                return null;
            }
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
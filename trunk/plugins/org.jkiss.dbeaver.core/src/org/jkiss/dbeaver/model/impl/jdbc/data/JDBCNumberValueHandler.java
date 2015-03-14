/*
 * Copyright (C) 2010-2015 Serge Rieder
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
import org.jkiss.dbeaver.model.impl.data.editors.BitInlineEditor;
import org.jkiss.dbeaver.model.impl.data.formatters.DefaultDataFormatter;
import org.jkiss.dbeaver.model.impl.data.editors.NumberEditorHelper;
import org.jkiss.dbeaver.model.impl.data.editors.NumberInlineEditor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Locale;

/**
 * JDBC number value handler
 */
public class JDBCNumberValueHandler extends JDBCAbstractValueHandler implements NumberEditorHelper {

    private Locale locale;
    private DBDDataFormatter formatter;

    public JDBCNumberValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        try {
            locale = formatterProfile.getLocale();
            formatter = formatterProfile.createFormatter(DBDDataFormatter.TYPE_NAME_NUMBER);
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
                try {
                    // Read value with maximum precision. Some drivers reports FLOAT but means double [JDBC:SQLite]
                    value = resultSet.getDouble(index);
                } catch (SQLException e) {
                    value = resultSet.getFloat(index);
                }
                break;
            case java.sql.Types.INTEGER:
                try {
                    // Read value with maximum precision. Some drivers reports INTEGER but means long [JDBC:SQLite]
                    value = resultSet.getLong(index);
                } catch (SQLException e) {
                    value = resultSet.getInt(index);
                }
                break;
            case java.sql.Types.SMALLINT:
                // Read int in case of unsigned shorts
                value = resultSet.getInt(index);
                break;
            case java.sql.Types.TINYINT:
                // Read short in case of unsigned byte
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
                    return new BitInlineEditor(controller);
                } else {
                    return new NumberInlineEditor(controller, this);
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
            return NumberInlineEditor.convertStringToNumber((String)object, getNumberType(type, null), formatter);
        } else {
            log.warn("Unrecognized type '" + object.getClass().getName() + "' - can't convert to numeric");
            return null;
        }
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public DBDDataFormatter getFormatter() {
        return formatter;
    }

    @Override
    public Class<? extends Number> getNumberType(DBSTypedObject type, Object originalValue) {
        if (originalValue instanceof Number) {
            return ((Number)originalValue).getClass();
        } else {
            switch (type.getTypeID()) {
                case java.sql.Types.BIGINT:
                    return Long.class;
                case java.sql.Types.DECIMAL:
                case java.sql.Types.DOUBLE:
                case java.sql.Types.REAL:
                    return Double.class;
                case java.sql.Types.FLOAT:
                    return Float.class;
                case java.sql.Types.INTEGER:
                    return Integer.class;
                case java.sql.Types.SMALLINT:
                case java.sql.Types.TINYINT:
                    return Short.class;
                case java.sql.Types.BIT:
                    return Byte.class;
                case java.sql.Types.NUMERIC:
                    return BigDecimal.class;
                default:
                    if (type.getScale() > 0) {
                        return Double.class;
                    } else {
                        return Long.class;
                    }
            }
        }
    }
}
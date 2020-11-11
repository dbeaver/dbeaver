/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.formatters.DefaultDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

/**
 * JDBC number value handler
 */
public class JDBCNumberValueHandler extends JDBCAbstractValueHandler implements DBDValueHandlerConfigurable, DBDValueDefaultGenerator {

    private static final Log log = Log.getLog(JDBCNumberValueHandler.class);

    private final DBDFormatSettings formatSettings;
    private int useScientificNotation = -1;
    private DBDDataFormatter formatter;

    public JDBCNumberValueHandler(DBSTypedObject type, DBDFormatSettings formatSettings) {
        this.formatSettings = formatSettings;
    }

    @Override
    public void refreshValueHandlerConfiguration(DBSTypedObject type) {
        this.formatter = null;
        this.useScientificNotation = -1;
    }

    /**
     * NumberFormat is not thread safe thus this method is synchronized.
     */
    @NotNull
    @Override
    public synchronized String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format)
    {
        if (value == null) {
            return DBValueFormatting.getDefaultValueDisplayString(null, format);
        } else if (value instanceof String) {
            // Binary string
            return (String)value;
        } else if (value instanceof Double) {
            double dbl = ((Double) value).doubleValue();
            if (dbl != dbl) {
                return "NaN";
            } else if (dbl == Double.POSITIVE_INFINITY) {
                return "+Infinity";
            } else if (dbl == Double.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
        }
        if (value instanceof Number && (format == DBDDisplayFormat.NATIVE || format == DBDDisplayFormat.EDIT)) {
            if (useScientificNotation < 0) {
                this.useScientificNotation =
                    formatSettings.isUseScientificNumericFormat() ? 1 : 0;
            }

            return DBValueFormatting.convertNumberToNativeString((Number) value, useScientificNotation > 0);
        }
        return getFormatter(column).formatValue(value);
    }

    private DBDDataFormatter getFormatter(@NotNull DBSTypedObject column) {
        if (formatter == null) {
            try {
                formatter = formatSettings.getDataFormatterProfile().createFormatter(DBDDataFormatter.TYPE_NAME_NUMBER, column);
            } catch (Exception e) {
                log.error("Can't create formatter for number value handler", e); //$NON-NLS-1$
                formatter = DefaultDataFormatter.INSTANCE;
            }
        }
        return formatter;
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
        Object value;
        switch (type.getTypeID()) {
            case Types.INTEGER:
                try {
                    // Read value with maximum precision. Some drivers reports INTEGER but means long [JDBC:SQLite]
                    value = resultSet.getLong(index);
                } catch (SQLException | ClassCastException | NumberFormatException e) {
                    value = resultSet.getObject(index);
                }
                break;
            case Types.SMALLINT:
                try {
                    // Read int in case of unsigned shorts
                    value = resultSet.getInt(index);
                } catch (SQLException | ClassCastException | NumberFormatException e) {
                    value = resultSet.getObject(index);
                }
                break;
            case Types.TINYINT:
                try {
                    // Read short in case of unsigned byte
                    value = resultSet.getShort(index);
                } catch (SQLException | ClassCastException | NumberFormatException e) {
                    value = resultSet.getObject(index);
                }
                break;
            case Types.BIT:
                if (CommonUtils.toInt(type.getPrecision()) <= 1) {
                    try {
                        // single bit
                        value = resultSet.getByte(index);
                    } catch (NumberFormatException e) {
                        // Maybe it is boolean? (#1604)
                        try {
                            boolean bValue = resultSet.getBoolean(index);
                            value = bValue ? (byte)1 : (byte)0;
                        } catch (Throwable e1) {
                            // No, it is not - rethrow original error
                            throw e;
                        }
                    }
                } else {
                    // bit string
                    String bitString = CommonUtils.toBinaryString(resultSet.getLong(index), CommonUtils.toInt(type.getPrecision()));
                    if (resultSet.wasNull()) {
                        return null;
                    } else {
                        return bitString;
                    }
                }
                break;
            case Types.DOUBLE:
            case Types.REAL:
            case Types.FLOAT:
                if (isReadDecimalsAsDouble()) {
                    try {
                        // Always read as double to avoid precision loose (#7214)
                        value = resultSet.getDouble(index);
                    } catch (SQLException | ClassCastException | NumberFormatException e) {
                        value = resultSet.getObject(index);
                    }
                    break;
                }
            default:
                // Here may be any numeric value. float, double, BigDecimal or BigInteger for example
                boolean gotValue = false;
                value = null;
                try {
                    Object objectValue = resultSet.getObject(index);
                    if (objectValue == null || objectValue instanceof Number) {
                        value = objectValue;
                        gotValue = true;
                    }
                } catch (SQLException e) {
                    log.debug(e);
                }
                if (value == null && !gotValue) {
                    try {
                        if (CommonUtils.toInt(type.getScale()) > 0) {
                            value = resultSet.getDouble(index);
                        } else {
                            try {
                                value = resultSet.getLong(index);
                            } catch (NumberFormatException e) {
                                // Something went wrong. E.g. #5147
                                value = resultSet.getDouble(index);
                            }
                        }
                    } catch (SQLException e) {
                        // Last chance - get it as string. Sometimes columns marked as numbers are actually not numbers
                        return resultSet.getString(index);
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

    protected boolean isReadDecimalsAsDouble() {
        return false;
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType,
                                 int paramIndex, Object value) throws SQLException, DBCException {
        if (value instanceof String) {
            String strValue = (String) value;
            // Some number. Actually we shouldn't be here
            Object number = DBValueFormatting.convertStringToNumber(strValue, getNumberType(paramType), getFormatter(paramType), true);
            if (number != null) {
                value = number;
            } else if (!strValue.isEmpty()) {
                // String was't empty but it can't be p[arsed as number
                // Let's save as string then
                statement.setString(paramIndex, strValue);
                return;
            } else {
                statement.setNull(paramIndex, paramType.getTypeID());
                return;
            }
        }
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else if (value instanceof Number) {
            Number number = (Number) value;
            switch (paramType.getTypeID()) {
                case Types.BIGINT:
                    if (number instanceof BigInteger) {
                        statement.setBigDecimal(paramIndex, new BigDecimal((BigInteger) number));
                    } else {
                        statement.setLong(paramIndex, number.longValue());
                    }
                    break;
                case Types.FLOAT:
                    if (number instanceof BigDecimal) {
                        statement.setBigDecimal(paramIndex, (BigDecimal) number);
                    } else if (number instanceof Double) {
                        statement.setDouble(paramIndex, number.doubleValue());
                    } else {
                        statement.setFloat(paramIndex, number.floatValue());
                    }
                    break;
                case Types.DOUBLE:
                case Types.REAL:
                    if (number instanceof BigDecimal) {
                        statement.setBigDecimal(paramIndex, (BigDecimal) number);
                    } else if (number instanceof Float) {
                        statement.setFloat(paramIndex, number.floatValue());
                    } else {
                        statement.setDouble(paramIndex, number.doubleValue());
                    }
                    break;
                case Types.INTEGER:
                    if (number instanceof Long) {
                        statement.setLong(paramIndex, number.longValue());
                    } else {
                        statement.setInt(paramIndex, number.intValue());
                    }
                    break;
                case Types.SMALLINT:
                case Types.TINYINT:
                    if (number instanceof Integer) {
                        statement.setInt(paramIndex, number.intValue());
                    } else if (number instanceof Long) {
                        statement.setLong(paramIndex, number.longValue());
                    } else {
                        statement.setShort(paramIndex, number.shortValue());
                    }
                    break;
                case Types.BIT:
                    if (CommonUtils.toInt(paramType.getPrecision()) <= 1) {
                        statement.setByte(paramIndex, number.byteValue());
                    } else {
                        statement.setLong(paramIndex, number.longValue());
                    }
                    break;
                case Types.NUMERIC:
                case Types.DECIMAL:
                    if (number instanceof Long) {
                        statement.setLong(paramIndex, number.longValue());
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
                    if (CommonUtils.toInt(paramType.getScale()) > 0) {
                        statement.setDouble(paramIndex, number.doubleValue());
                    } else {
                        statement.setLong(paramIndex, number.longValue());
                    }
                    break;
            }
        } else {
            throw new SQLException("Numeric value type '" + value.getClass().getName() + "' is not supported");
        }
    }

    @NotNull
    @Override
    public Class<? extends Number> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return getNumberType(attribute);
    }

    @Nullable
    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object == null) {
            return null;
        } else if (object instanceof Number) {
            return object;
        } else if (object instanceof String) {
            String strValue = (String) object;
            if (strValue.isEmpty()) {
                // Empty string means NULL value
                return null;
            }
            return DBValueFormatting.convertStringToNumber(strValue, getNumberType(type), getFormatter(type), validateValue);
        } else if (object instanceof Boolean) {
            return (Boolean) object ? 1 : 0;
        } else if (object instanceof Date) {
            return DBValueFormatting.convertDateToNumber(((Date) object), getNumberType(type), getFormatter(type), validateValue);
        } else {
            log.warn("Unrecognized type '" + object.getClass().getName() + "' - can't convert to numeric");
            return null;
        }
    }

    public Class<? extends Number> getNumberType(DBSTypedObject type) {
        switch (type.getTypeID()) {
            case Types.BIGINT:
                return Long.class;
            case Types.DECIMAL:
            case Types.DOUBLE:
            case Types.REAL:
                if (CommonUtils.toInt(type.getScale()) > 0) {
                    // Workaround for Oracle #3062
                    return Double.class;
                }
                return BigDecimal.class;
            case Types.FLOAT:
                if (CommonUtils.toInt(type.getScale()) > 0) {
                    // Workaround for Oracle #3062
                    return Float.class;
                }
                return BigDecimal.class;
            //Workaround for MySQL Unsigned INTEGER #8786
            case Types.INTEGER:
                return Long.class;
            case Types.SMALLINT:
            case Types.TINYINT:
                return Short.class;
            case Types.BIT:
                if (CommonUtils.toInt(type.getPrecision()) <= 1) {
                    return Byte.class;
                } else {
                    // bit string (hopefully long is enough)
                    return Long.class;
                }
            case Types.NUMERIC:
                return BigDecimal.class;
            default:
                if (CommonUtils.toInt(type.getScale()) > 0) {
                    return Double.class;
                } else {
                    return Long.class;
                }
        }
    }

    @Override
    public String getDefaultValueLabel() {
        return "Zero";
    }

    @Override
    public Object generateDefaultValue(DBCSession session, DBSTypedObject type) {
        switch (type.getTypeID()) {
            case Types.BIGINT:
                return 0L;
            case Types.DECIMAL:
            case Types.DOUBLE:
            case Types.REAL:
                if (CommonUtils.toInt(type.getScale()) > 0) {
                    // Workaround for Oracle #3062
                    return 0D;
                }
                return new BigDecimal(0);
            case Types.FLOAT:
                if (CommonUtils.toInt(type.getScale()) > 0) {
                    // Workaround for Oracle #3062
                    return 0F;
                }
                return new BigDecimal(0);
            case Types.INTEGER:
                return 0;
            case Types.SMALLINT:
            case Types.TINYINT:
                return (short)0;
            case Types.BIT:
                if (CommonUtils.toInt(type.getPrecision()) <= 1) {
                    return (byte)0;
                } else {
                    // bit string (hopefully long is enough)
                    return (long)0;
                }
            case Types.NUMERIC:
                return new BigDecimal(0);
            default:
                if (CommonUtils.toInt(type.getScale()) > 0) {
                    return 0D;
                } else {
                    return 0L;
                }
        }
    }
}

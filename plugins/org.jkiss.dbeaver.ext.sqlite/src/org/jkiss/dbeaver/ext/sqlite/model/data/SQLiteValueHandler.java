/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.sqlite.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandlerConfigurable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.formatters.DefaultDataFormatter;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.Locale;


/**
 * SQLiteValueHandler
 */
public class SQLiteValueHandler extends JDBCAbstractValueHandler implements DBDValueHandlerConfigurable {

    private static final Log log = Log.getLog(SQLiteValueHandler.class);

    private final DBDFormatSettings formatSettings;
    private final DBSTypedObject type;
    private DBDDataFormatter numberFormatter;
    private DBDDataFormatter timestampFormatter;

    public SQLiteValueHandler(DBSTypedObject type, DBDFormatSettings formatSettings)
    {
        this.formatSettings = formatSettings;
        this.type = type;
    }

    @Nullable
    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException {
        Object object = resultSet.getObject(index);
        return getValueFromObject(session, type, object, false, false);
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws DBCException, SQLException {
        statement.setObject(paramIndex, value);
    }

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return Object.class;
    }

    @Nullable
    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue) throws DBCException {
        if (object instanceof String && (type.getTypeID() == Types.REAL || type.getTypeID() == Types.DOUBLE)) {
            switch (((String) object).toLowerCase(Locale.ROOT)) {
                case "inf":
                case "infinity":
                case "+inf":
                case "+infinity":
                    return Double.POSITIVE_INFINITY;
                case "-inf":
                case "-infinity":
                    return Double.NEGATIVE_INFINITY;
                case "nan":
                    return Double.NaN;
                default:
                    break;
            }
        }

        return object;
    }

    @NotNull
    public synchronized String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format)
    {
        if (value instanceof Number) {
            if (format == DBDDisplayFormat.NATIVE || format == DBDDisplayFormat.EDIT) {
                return DBValueFormatting.convertNumberToNativeString(
                    (Number) value,
                    formatSettings.isUseScientificNumericFormat());
            } else {
                if (numberFormatter == null) {
                    try {
                        numberFormatter = formatSettings.getDataFormatterProfile().createFormatter(DBDDataFormatter.TYPE_NAME_NUMBER, type);
                    } catch (Exception e) {
                        log.error("Can't create numberFormatter for number value handler", e); //$NON-NLS-1$
                        numberFormatter = DefaultDataFormatter.INSTANCE;
                    }
                }
                return numberFormatter.formatValue(value);
            }
        } else if (value instanceof Date) {

            if (timestampFormatter == null) {
                try {
                    timestampFormatter = formatSettings.getDataFormatterProfile().createFormatter(DBDDataFormatter.TYPE_NAME_TIMESTAMP, type);
                } catch (Exception e) {
                    log.error("Can't create timestampFormatter for timestamp value handler", e); //$NON-NLS-1$
                    timestampFormatter = DefaultDataFormatter.INSTANCE;
                }
            }

            return timestampFormatter.formatValue(value);
        } else if (value instanceof String string && column.getDataKind() == DBPDataKind.DATETIME) {
            if (format == DBDDisplayFormat.NATIVE && !string.startsWith("'") && !string.endsWith("'")) {
                return "'" + value + "'";
            }
        } else if (value instanceof byte[] && column.getDataKind() == DBPDataKind.STRING) {
            return new String((byte[]) value);
        }
        return super.getValueDisplayString(column, value, format);
    }

    @Override
    public void refreshValueHandlerConfiguration(DBSTypedObject type) {
        this.numberFormatter = null;
        this.timestampFormatter = null;
    }
}

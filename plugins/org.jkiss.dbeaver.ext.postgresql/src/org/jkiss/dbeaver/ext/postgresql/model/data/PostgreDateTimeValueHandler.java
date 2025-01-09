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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.util.Date;

/**
 * PostgreDateTimeValueHandler.
 */
public class PostgreDateTimeValueHandler extends JDBCDateTimeValueHandler {
    private static final String POSITIVE_INFINITY_STRING_REPRESENTATION = "infinity";
    private static final String NEGATIVE_INFINITY_STRING_REPRESENTATION = "-infinity";

    // https://jdbc.postgresql.org/documentation/publicapi/constant-values.html
    private static final long NEGATIVE_INFINITY = -9223372036832400000L;
    private static final long NEGATIVE_SMALLER_INFINITY = -185543533774800000L;
    private static final long POSITIVE_INFINITY = 9223372036825200000L;
    private static final long POSITIVE_SMALLER_INFINITY = 185543533774800000L;

    public PostgreDateTimeValueHandler(DBDFormatSettings formatSettings) {
        super(formatSettings);
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException {
        if (!(object instanceof Date date)) {
            return super.getValueFromObject(session, type, object, copy, validateValue);
        }
        final long time = date.getTime();
        if (time == NEGATIVE_INFINITY || time == NEGATIVE_SMALLER_INFINITY) {
            return NEGATIVE_INFINITY_STRING_REPRESENTATION;
        }
        if (time == POSITIVE_INFINITY || time == POSITIVE_SMALLER_INFINITY) {
            return POSITIVE_INFINITY_STRING_REPRESENTATION;
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @Override
    protected boolean isReadDateAsObject() {
        return true;
    }

    @Override
    public void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject type, int index, Object value) throws DBCException {
        if (value instanceof String) {
            try {
                ((JDBCPreparedStatement)statement).setObject(index + 1, value.toString(),
                    ((PostgreDataSource)session.getDataSource()).getServerType().getParameterBindType(type, value));
            }
            catch (SQLException e) {
                throw new DBCException(ModelMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
            }
            return;
        }
        super.bindValueObject(session, statement, type, index, value);
    }

    @NotNull
    @Override
    protected String getFormatterId(DBSTypedObject column) {
        switch (column.getTypeName()) {
            case PostgreConstants.TYPE_TIMETZ:
                return DBDDataFormatter.TYPE_NAME_TIME_TZ;
            case PostgreConstants.TYPE_TIMESTAMPTZ:
                return DBDDataFormatter.TYPE_NAME_TIMESTAMP_TZ;
        }
        return super.getFormatterId(column);
    }
}
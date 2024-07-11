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
package org.jkiss.dbeaver.ext.clickhouse.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Types;

public class ClickHouseDateTimeValueHandler extends JDBCDateTimeValueHandler {

    public ClickHouseDateTimeValueHandler(DBDFormatSettings formatSettings) {
        super(formatSettings);
    }

    @Override
    public Object fetchValueObject(@NotNull DBCSession session,
        @NotNull DBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index
    ) throws DBCException {
        try {
            if (resultSet instanceof JDBCResultSet) {
                JDBCResultSet dbResults = (JDBCResultSet) resultSet;

                // check for native format
                if (formatSettings.isUseNativeDateTimeFormat()) {
                    try {
                        return dbResults.getString(index + 1);
                    } catch (SQLException e) {
                        log.debug("Can't read date/time value as string: " + e.getMessage());
                    }
                }

                // It seems that some drivers doesn't support reading date/time values with
                // explicit calendar
                // So let's use simple version
                switch (type.getTypeID()) {
                    case Types.TIME:
                    case Types.TIME_WITH_TIMEZONE:
                        return dbResults.getTime(index + 1);
                    // The datetime is type is incorrect fetch from driver skip formatting and
                    // return as string from LocalTime object
                    case Types.DATE:
                    default:
                        Object value = dbResults.getObject(index + 1);
                        return getValueFromObject(session, type, value, false, false);
                }
            }
        } catch (Exception e) {
            throw new DBCException(e.getMessage(), e);
        }
        return super.fetchValueObject(session, resultSet, type, index);
    }

}

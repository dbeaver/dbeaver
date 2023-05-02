/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.BeanUtils;


public class SQLServerDateTimeOffsetHandler extends JDBCDateTimeValueHandler {
    private static final Log log = Log.getLog(JDBCStringValueHandler.class);

    public SQLServerDateTimeOffsetHandler(DBDFormatSettings formatSettings) {
        super(formatSettings);
    }

    /**
     * {@link https://learn.microsoft.com/en-us/sql/connect/jdbc/reference/datetimeoffset-members?view=sql-server
     * -ver16}
     * custom SQL Server type
     */
    @Override
    public Object getValueFromObject(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        Object object,
        boolean copy,
        boolean validateValue
    ) throws DBCException {
        if (
            object != null
            && object.getClass().getName().equals("microsoft.sql.DateTimeOffset")
            && !formatSettings.isUseNativeDateTimeFormat()
        ) {
            try {
                return BeanUtils.invokeObjectMethod(object, "getTimestamp");
            } catch (Throwable e) {
                log.warn("error extracting datetimeoffset timestamp", e);
            }
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @Override
    public void bindValueObject(
        @NotNull DBCSession session,
        @NotNull DBCStatement statement,
        @NotNull DBSTypedObject type,
        int index,
        @Nullable Object value
    ) throws DBCException {
        super.bindValueObject(session, statement, type, index, value);
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCNumberValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;

/**
 * YashanDB number value handler
 */
public class YashanDBNumberValueHandler extends JDBCNumberValueHandler {


    public YashanDBNumberValueHandler(DBSTypedObject type, DBDFormatSettings formatSettings) {
        super(type, formatSettings);
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType,
                                 int paramIndex, Object value) throws SQLException, DBCException {
        if ("Data load".equals(session.getTaskTitle())) {
            String strValue = Long.toString((long)value);
            if (paramType.getTypeID() == Types.BIT) {
                // Bit string
                long longValue;
                try {
                    longValue = Long.parseLong(strValue, 2);
                } catch (NumberFormatException e) {
                    throw new SQLException("Can't convert value '" + value + "' into bit string", e);
                }
                if (CommonUtils.toInt(paramType.getPrecision()) <= 1) {
                    statement.setByte(paramIndex, (byte)longValue);
                } else {
                    statement.setLong(paramIndex, longValue);
                }
            }
        }else {
            super.bindParameter(session, statement, paramType, paramIndex, value);
        }
    }

}

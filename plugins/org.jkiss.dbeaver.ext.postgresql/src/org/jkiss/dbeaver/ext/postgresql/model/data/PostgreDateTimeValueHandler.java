/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Types;

/**
 * JDBC strict string value handler.
 */
public class PostgreDateTimeValueHandler extends JDBCDateTimeValueHandler {


    public PostgreDateTimeValueHandler(DBDDataFormatterProfile formatterProfile) {
        super(formatterProfile);
    }

    @Override
    public void bindValueObject(DBCSession session, DBCStatement statement, DBSTypedObject type, int index, Object value) throws DBCException {
        if (value instanceof String) {
            try {
                ((JDBCPreparedStatement)statement).setObject(index + 1, value.toString(), Types.OTHER);
            }
            catch (SQLException e) {
                throw new DBCException(ModelMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
            }
            return;
        }
        super.bindValueObject(session, statement, type, index, value);
    }
}
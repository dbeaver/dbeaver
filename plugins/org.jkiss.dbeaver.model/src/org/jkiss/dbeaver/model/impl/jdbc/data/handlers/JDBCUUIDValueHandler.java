/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.sql.SQLException;
import java.util.UUID;

/**
 * UUID type support
 */
public class JDBCUUIDValueHandler extends JDBCObjectValueHandler {

    public static final JDBCUUIDValueHandler INSTANCE = new JDBCUUIDValueHandler();

    @NotNull
    @Override
    public Class<UUID> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return UUID.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object == null) {
            return null;
        } else if (object instanceof UUID) {
            return object;
        } else if (object instanceof byte[]) {
            return GeneralUtils.getUUIDFromBytes((byte[]) object);
        } else {
            final String str = object.toString();
            if (str.isEmpty()) {
                return null;
            } else {
                return UUID.fromString(str);
            }
        }
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws DBCException, SQLException {
        if (value instanceof String && paramType.getDataKind() != DBPDataKind.STRING) {
            value = UUID.fromString((String) value);
        }
        if (value instanceof UUID) {
            switch (paramType.getDataKind()) {
                case BINARY:
                    value = GeneralUtils.getBytesFromUUID((UUID) value);
                    break;
                case STRING:
                    value = value.toString();
                    break;
            }
        }

        super.bindParameter(session, statement, paramType, paramIndex, value);
    }
}

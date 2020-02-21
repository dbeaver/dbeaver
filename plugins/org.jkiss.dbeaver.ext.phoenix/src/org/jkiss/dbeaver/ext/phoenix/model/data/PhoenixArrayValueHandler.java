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
package org.jkiss.dbeaver.ext.phoenix.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCArrayValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;


/**
 * PhoenixArrayValueHandler
 */
public class PhoenixArrayValueHandler extends JDBCArrayValueHandler {
    public static final PhoenixArrayValueHandler INSTANCE = new PhoenixArrayValueHandler();
    
    public static final String PHOENIX_ARRAY_TYPE = "PhoenixArray";

    @Override
    public DBDCollection getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object != null && object.getClass().getSimpleName().contains(PHOENIX_ARRAY_TYPE)) {
            return JDBCCollection.makeCollectionFromArray((JDBCSession) session, type, (Array) object);
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }
    
    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, Types.ARRAY);
        } else if (value instanceof DBDCollection) {
            DBDCollection collection = (DBDCollection) value;
            if (collection.isNull()) {
                statement.setNull(paramIndex, Types.ARRAY);
            } else if (collection instanceof JDBCCollection) {
                statement.setArray(paramIndex, ((JDBCCollection) collection).getArrayValue());
            } else {
                statement.setArray(paramIndex, (Array)collection.getRawValue());
            }
        } else {
            throw new DBCException("Array parameter type '" + value.getClass().getName() + "' not supported");
        }
    }
    
}

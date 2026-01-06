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
package org.jkiss.dbeaver.ext.altibase.data;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCObjectValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Types;
import java.util.BitSet;

public class AltibaseBitSetValueHandler extends JDBCObjectValueHandler {

    public static final AltibaseBitSetValueHandler INSTANCE = new AltibaseBitSetValueHandler();

    @Override
    protected Object fetchColumnValue(
            DBCSession session,
            JDBCResultSet resultSet,
            DBSTypedObject type,
            int index) throws DBCException, SQLException {
        String value = null;
        Object obj = resultSet.getObject(index);

        // binary representation of BitSet
        if (obj != null && !resultSet.wasNull()) {
            BitSet bitSet = (BitSet) obj;
            int size = bitSet.size();

            StringBuilder sb = new StringBuilder(size);
            for (int i = 0; i < size; i++) {
                sb.append(bitSet.get(i) ? '1' : '0');
            }

            value = sb.toString();
        }

        return getValueFromObject(session, type, value, false, false);
    }
    
    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value) throws DBCException, SQLException {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            try {
                statement.setObject(paramIndex, value.toString(), Types.BIT);
            } catch (SQLException e) {
                statement.setObject(paramIndex, value);
            }
        }
    }
}

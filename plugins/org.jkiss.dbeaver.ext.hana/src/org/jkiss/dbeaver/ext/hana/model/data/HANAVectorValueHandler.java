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
package org.jkiss.dbeaver.ext.hana.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCArrayValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Types;

public abstract class HANAVectorValueHandler extends JDBCArrayValueHandler {

    private static DBCLogicalOperator[] SUPPORTED_OPERATORS = {
        DBCLogicalOperator.IS_NOT_NULL,
        DBCLogicalOperator.IS_NULL
    };

    @Override
    protected boolean useGetArray(DBCSession session, DBSTypedObject type) {
        return true;
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType,
            int paramIndex, Object value) throws DBCException, SQLException {
        if (value == null) {
            statement.setNull(paramIndex, Types.ARRAY);
        } else if (value instanceof DBDCollection) {
            DBDCollection collection = (DBDCollection) value;
            if (collection.isNull()) {
                statement.setNull(paramIndex, Types.ARRAY);
            } else if (collection instanceof JDBCCollection) {
                bindVectorParameter(statement, paramIndex, (JDBCCollection) collection);
            } else {
                throw new DBCException("Array parameter type '" + value.getClass().getName() + "' not supported");
            }
        } else {
            throw new DBCException("Array parameter type '" + value.getClass().getName() + "' not supported");
        }
    }

    protected abstract void bindVectorParameter(@NotNull JDBCPreparedStatement statement, int paramIndex,
            @NotNull JDBCCollection collection)
            throws DBCException, SQLException;

    @Override
    public DBCLogicalOperator[] getSupportedOperators(DBSTypedObject attribute) {
        return SUPPORTED_OPERATORS;
    }
}

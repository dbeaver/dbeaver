/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
 *
 * Contributors:
 *    Frederick Arand - initial implementation
 */
package org.jkiss.dbeaver.ext.hana.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.hana.model.HANAConstants;
import org.jkiss.dbeaver.ext.hana.model.HANADataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;

public class HANAHalfVectorValueHandler extends HANAVectorValueHandler {

    private static final Log log = Log.getLog(HANAVectorValueHandler.class);

    public static final HANAHalfVectorValueHandler INSTANCE = new HANAHalfVectorValueHandler();

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object,
            boolean copy, boolean validateValue) throws DBCException {
        if (object != null && object instanceof Array array
                && type.getTypeName().equals(HANAConstants.DATA_TYPE_NAME_HALF_VECTOR)) {
            try {
                JDBCSession jdbcSession = (JDBCSession) session;
                DBRProgressMonitor monitor = jdbcSession.getProgressMonitor();
                JDBCDataSource dataSource = jdbcSession.getDataSource();
                String baseTypeName = "REAL";
                DBSDataType elementType = null;
                try {
                    elementType = dataSource.resolveDataType(monitor, baseTypeName);
                } catch (DBException e) {
                    throw new DBCException("Error resolving data type", e);
                }
                final DBDValueHandler elementValueHandler = DBUtils.findValueHandler(session, elementType);
                Object arrObject = array.getArray();
                int arrLength = java.lang.reflect.Array.getLength(arrObject);
                if (arrLength == 0) {
                    throw new DBCException("Non-NULL HALF_VECTOR cannot have 0 dimension");
                }
                Object[] contents = new Object[arrLength];
                for (int i = 0; i < arrLength; i++) {
                    Object item = java.lang.reflect.Array.get(arrObject, i);
                    if (item == null) {
                        throw new DBCException("HALF_VECTOR cannot have NULL element");
                    }
                    contents[i] = elementValueHandler.getValueFromObject(jdbcSession, elementType, item, false, true);
                }
                return new JDBCCollection(monitor, elementType, elementValueHandler, contents);
            } catch (SQLException e) {
                log.warn("Cannot display HALF_VECTOR, using default handling", e);
            }
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @Override
    protected void bindVectorParameter(@NotNull JDBCPreparedStatement statement, int paramIndex,
            @NotNull JDBCCollection collection)
            throws DBCException, SQLException {
        if (collection.getComponentType().getTypeID() != Types.REAL) {
            throw new DBCException("Only REAL numbers are allowed in HALF_VECTOR as ARRAY");
        }
        float[] nvals = new float[collection.size()];
        for (int i = 0; i < nvals.length; ++i) {
            Float val = (Float) collection.get(i);
            if (val == null) {
                throw new DBCException("NULL elements are not allowed in HALF_VECTOR");
            }
            nvals[i] = val;
        }
        statement.setObject(paramIndex, nvals);
    }
}

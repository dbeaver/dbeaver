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
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCReference;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Ref;
import java.sql.SQLException;
import java.sql.Types;

/**
 * JDBC reference value handler.
 * Handle STRUCT types.
 *
 * @author Serge Rider
 */
public class JDBCReferenceValueHandler extends JDBCComplexValueHandler {

    private static final Log log = Log.getLog(JDBCReferenceValueHandler.class);

    public static final JDBCReferenceValueHandler INSTANCE = new JDBCReferenceValueHandler();

    /**
     * NumberFormat is not thread safe thus this method is synchronized.
     */
    @NotNull
    @Override
    public synchronized String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        return DBValueFormatting.getDefaultValueDisplayString(value, format);
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
        JDBCReference reference = (JDBCReference) value;
        statement.setRef(paramIndex, reference.getValue());
    }

    @NotNull
    @Override
    public Class<Ref> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return Ref.class;
    }

    @Override
    public JDBCReference getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        String typeName;
        try {
            if (object instanceof Ref) {
                typeName = ((Ref) object).getBaseTypeName();
            } else {
                typeName = type.getTypeName();
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
        DBSDataType dataType = null;
        try {
            dataType = DBUtils.resolveDataType(session.getProgressMonitor(), session.getDataSource(), typeName);
        } catch (DBException e) {
            log.error("Error resolving data type '" + typeName + "'", e);
        }
        if (dataType == null) {
            dataType = new JDBCDataType(
                session.getDataSource().getContainer(),
                Types.REF,
                typeName,
                "Synthetic struct type for reference '" + typeName + "'",
                false, false, 0, 0, 0);
        }
        if (object == null) {
            return new JDBCReference(dataType, null);
        } else if (object instanceof JDBCReference) {
            return (JDBCReference)object;
        } else if (object instanceof Ref) {
            return new JDBCReference(dataType, (Ref) object);
        } else {
            throw new DBCException("Unsupported struct type: " + object.getClass().getName());
        }
    }

}
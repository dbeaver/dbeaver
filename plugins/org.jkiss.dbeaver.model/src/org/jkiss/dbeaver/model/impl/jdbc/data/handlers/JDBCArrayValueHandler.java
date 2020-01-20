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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.utils.CommonUtils;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

/**
 * JDBC Array value handler.
 * Handle ARRAY types.
 *
 * @author Serge Rider
 */
public class JDBCArrayValueHandler extends JDBCComplexValueHandler {

    public static final JDBCArrayValueHandler INSTANCE = new JDBCArrayValueHandler();

    @NotNull
    @Override
    public Class<DBDCollection> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return DBDCollection.class;
    }

    @Override
    public DBDCollection getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return JDBCCollection.makeCollectionFromArray((JDBCSession) session, type, null);
        } else if (object instanceof JDBCCollection) {
            return (JDBCCollection)(copy ? ((JDBCCollection) object).cloneValue(session.getProgressMonitor()) : object);
        } else if (object instanceof Array) {
            return JDBCCollection.makeCollectionFromArray((JDBCSession) session, type, (Array) object);
        } else if (object instanceof String) {
            return JDBCCollection.makeCollectionFromString((JDBCSession) session, (String)object);
        } else if (object.getClass().isArray()) {
            return JDBCCollection.makeCollectionFromJavaArray((JDBCSession) session, type, object);
        } else if (object instanceof Collection) {
            return JDBCCollection.makeCollectionFromJavaCollection((JDBCSession) session, type, (Collection) object);
        } else {
            return JDBCCollection.makeCollectionFromString((JDBCSession) session, CommonUtils.toString(object));
        }
    }

    @Override
    public Object createNewValueObject(@NotNull DBCSession session, @NotNull DBSTypedObject type) throws DBCException {
        DBSDataType dataType;
        if (type instanceof DBSDataType) {
            dataType = (DBSDataType) type;
        } else if (type instanceof DBSTypedObjectEx) {
            dataType = ((DBSTypedObjectEx) type).getDataType();
        } else {
            throw new DBCException("Can't determine array element data type: " + type.getFullTypeName());
        }
        try {
            DBSDataType componentType = dataType.getComponentType(session.getProgressMonitor());
            if (componentType == null) {
                throw new DBCException("Can't determine component data type from " + dataType.getFullTypeName());
            }
            Array array = ((JDBCSession) session).createArrayOf(componentType.getFullTypeName(), new Object[0]);
            return getValueFromObject(session, type, array, false);
        } catch (Exception e) {
            throw new DBCException("Error creating JDBC array " + type.getFullTypeName());
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value instanceof JDBCCollection) {
            return ((JDBCCollection) value).makeArrayString(format);
        }
        return super.getValueDisplayString(column, value, format);
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
                statement.setObject(paramIndex, ((JDBCCollection) collection).getArrayValue(), Types.ARRAY);
            } else {
                statement.setObject(paramIndex, collection.getRawValue());
            }
        } else {
            throw new DBCException("Array parameter type '" + value.getClass().getName() + "' not supported");
        }
    }

}
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

import com.vividsolutions.jts.geom.Geometry;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Postgre geometry handler
 */
public class PostgreGeometryValueHandler extends JDBCAbstractValueHandler {

    public static final PostgreGeometryValueHandler INSTANCE = new PostgreGeometryValueHandler();

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException {
        return getValueFromObject(session, type,
            resultSet.getObject(index),
            false);
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws DBCException, SQLException {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else if (value instanceof Geometry) {
            statement.setObject(paramIndex, getStringFromGeometry(session, (Geometry)value), Types.OTHER);
        } else if (value.getClass().getName().equals(PostgreConstants.PG_GEOMETRY_CLASS)) {
            statement.setObject(paramIndex, value, Types.OTHER);
        } else {
            statement.setObject(paramIndex, value.toString(), Types.OTHER);
        }
    }

    @Override
    public Class<?> getValueObjectType(DBSTypedObject attribute) {
        return Geometry.class;
    }

    @Override
    public Object getValueFromObject(DBCSession session, DBSTypedObject type, Object object, boolean copy) throws DBCException {
        if (object == null) {
            return null;
        } else if (object instanceof Geometry) {
            return object;
        } else if (object instanceof String) {
            return makeGeometryFromString(session, (String) object);
        } else if (object.getClass().getName().equals(PostgreConstants.PG_GEOMETRY_CLASS)) {
            return makeGeometryFromPGGeometry(session, object);
        } else {
            return makeGeometryFromString(session, object.toString());
        }
    }

    private Object makeGeometryFromPGGeometry(DBCSession session, Object pgGeometry) throws DBCException {
        try {
            return BeanUtils.invokeObjectMethod(pgGeometry, "getGeometry", null, null);
        } catch (Throwable e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    private Object makeGeometryFromString(DBCSession session, String object) throws DBCException {
        if (CommonUtils.isEmpty(object)) {
            return null;
        }
        try {
            Class<?> jtsGeometry = DBUtils.getDriverClass(session.getDataSource(), PostgreConstants.PG_GEOMETRY_CLASS);
            return BeanUtils.invokeStaticMethod(
                jtsGeometry, "geomFromString", new Class[] { String.class }, new Object[] { object }
            );
        } catch (Throwable e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    private String getStringFromGeometry(JDBCSession session, Geometry geometry) throws DBCException {
        try {
            Class<?> jtsGeometry = DBUtils.getDriverClass(session.getDataSource(), PostgreConstants.PG_GEOMETRY_CLASS);
            Object jtsg = jtsGeometry.getConstructor(Geometry.class).newInstance(geometry);
            return (String)BeanUtils.invokeObjectMethod(
                jtsg, "getValue", null, null);
        } catch (Throwable e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

}

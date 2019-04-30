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

import org.jkiss.dbeaver.data.gis.handlers.WKGUtils;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Postgre geometry handler
 */
public class PostgreGeometryValueHandler extends JDBCAbstractValueHandler {

    public static final PostgreGeometryValueHandler INSTANCE = new PostgreGeometryValueHandler();

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException {
        try {
            Object object = resultSet.getObject(index);
            return getValueFromObject(session, type, object,false);
        } catch (SQLException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                // Try to parse as WKG
                String wkbValue = resultSet.getString(index);
                return WKGUtils.parseWKB(wkbValue);
            } else {
                throw e;
            }
        }
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws DBCException, SQLException {
        if (value instanceof DBGeometry) {
            value = ((DBGeometry) value).getRawValue();
        }
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
        return DBGeometry.class;
    }

    @Override
    public Object getValueFromObject(DBCSession session, DBSTypedObject type, Object object, boolean copy) throws DBCException {
        if (object == null) {
            return new DBGeometry();
        } else if (object instanceof Geometry) {
            return new DBGeometry((Geometry) object);
        } else if (object instanceof String) {
            return makeGeometryFromWKT(session, (String) object);
        } else if (object.getClass().getName().equals(PostgreConstants.PG_GEOMETRY_CLASS)) {
            return makeGeometryFromPGGeometry(session, object);
        } else if (object.getClass().getName().equals(PostgreConstants.PG_OBJECT_CLASS)) {
            return makeGeometryFromWKB(session, CommonUtils.toString(PostgreUtils.extractPGObjectValue(object)));
        } else {
            return makeGeometryFromWKT(session, object.toString());
        }
    }

    private DBGeometry makeGeometryFromWKB(DBCSession session, String hexString) throws DBCException {
        byte[] binaryData = WKBReader.hexToBytes(hexString);
        try {
            Geometry geometry = new WKBReader().read(binaryData);
            return new DBGeometry(geometry);
        } catch (Exception e) {
            throw new DBCException("Error parsing WKB value", e);
        }
    }

    private DBGeometry makeGeometryFromPGGeometry(DBCSession session, Object value) throws DBCException {
        try {
            String pgString = value.toString();
            return makeGeometryFromWKT(session, pgString);
        } catch (Throwable e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    private DBGeometry makeGeometryFromWKT(DBCSession session, String pgString) throws DBCException {
        if (CommonUtils.isEmpty(pgString)) {
            return new DBGeometry();
        }
        // Convert from PostGIS EWKT to Geometry type
        try {
            int divPos = pgString.indexOf(';');
            if (divPos == -1) {
                return new DBGeometry(pgString);
            }
            String sridString = pgString.substring(0, divPos);
            String wktString = pgString.substring(divPos + 1);
            Geometry geometry = new WKTReader().read(wktString);
            if (sridString.startsWith("SRID=")) {
                geometry.setSRID(CommonUtils.toInt(sridString.substring(5)));
            }
            return new DBGeometry(geometry);
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

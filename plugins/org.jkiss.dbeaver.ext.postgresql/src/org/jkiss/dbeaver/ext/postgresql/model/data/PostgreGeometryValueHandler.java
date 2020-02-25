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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.data.gis.handlers.WKGUtils;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.gis.GisAttribute;
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
            return getValueFromObject(session, type, object,false, false);
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
        int valueSRID = 0;
        if (value instanceof DBGeometry) {
            valueSRID = ((DBGeometry) value).getSRID();
            value = ((DBGeometry) value).getRawValue();
        }
        if (valueSRID == 0 && paramType instanceof GisAttribute) {
            valueSRID = ((GisAttribute) paramType).getAttributeGeometrySRID(session.getProgressMonitor());
        }
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else if (value instanceof Geometry) {
            if (((Geometry) value).getSRID() == 0) {
                ((Geometry) value).setSRID(valueSRID);
            }
            statement.setObject(paramIndex, getStringFromGeometry(session, (Geometry)value), Types.OTHER);
        } else if (value.getClass().getName().equals(PostgreConstants.PG_GEOMETRY_CLASS)) {
            statement.setObject(paramIndex, value, Types.OTHER);
        } else {
            String strValue = value.toString();
            if (valueSRID != 0 && !strValue.startsWith("SRID=")) {
                strValue = "SRID=" + valueSRID + ";" + strValue;
            }
            statement.setObject(paramIndex, strValue, Types.OTHER);
        }
    }

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return DBGeometry.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException {
        if (object == null) {
            return new DBGeometry();
        } else if (object instanceof DBGeometry) {
            if (copy) {
                return ((DBGeometry) object).copy();
            } else {
                return object;
            }
        } else if (object instanceof Geometry) {
            return new DBGeometry((Geometry) object);
        } else if (object instanceof String) {
            return makeGeometryFromWKT(session, (String) object, 2);
        } else if (object.getClass().getName().equals(PostgreConstants.PG_GEOMETRY_CLASS)) {
            return makeGeometryFromPGGeometry(session, object);
        } else if (object.getClass().getName().equals(PostgreConstants.PG_OBJECT_CLASS)) {
            return makeGeometryFromWKB(session, CommonUtils.toString(PostgreUtils.extractPGObjectValue(object)));
        } else {
            return makeGeometryFromWKT(session, object.toString(), 2);
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (value instanceof DBGeometry && format == DBDDisplayFormat.NATIVE) {
            return "'" + value.toString() + "'";
        }
        return super.getValueDisplayString(column, value, format);
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
            Object geometry = BeanUtils.invokeObjectMethod(value, "getGeometry");
            if (geometry != null) {
                // Handle 3D geometries (#3629)
                Object dimension = BeanUtils.invokeObjectMethod(geometry, "getDimension");
                if (dimension instanceof Number) {
                    return makeGeometryFromWKT(session, geometry.toString(), ((Number) dimension).intValue());
                }
            }
            String pgString = value.toString();
            return makeGeometryFromWKT(session, pgString, 2);
        } catch (Throwable e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    private DBGeometry makeGeometryFromWKT(DBCSession session, String pgString, int dimensions) throws DBCException {
        if (CommonUtils.isEmpty(pgString)) {
            return new DBGeometry();
        }
        // Convert from PostGIS EWKT to Geometry type
        try {
            int divPos = pgString.indexOf(';');
            if (divPos == -1) {
                // No SRID
                if (dimensions == 2) {
                    try {
                        Geometry geometry = new WKTReader().read(pgString);
                        return new DBGeometry(geometry);
                    } catch (ParseException e) {
                        // Can't parse
                        return new DBGeometry(pgString);
                    }
                } else {
                    return new DBGeometry(pgString);
                }
            }
            String sridString = pgString.substring(0, divPos);
            String wktString = pgString.substring(divPos + 1);
            int srid = 0;
            if (sridString.startsWith("SRID=")) {
                srid = CommonUtils.toInt(sridString.substring(5));
            }
            if (dimensions == 2) {
                Geometry geometry = new WKTReader().read(wktString);
                if (srid > 0) {
                    geometry.setSRID(srid);
                }
                return new DBGeometry(geometry);
            } else {
                return new DBGeometry(wktString, srid);
            }
        } catch (Throwable e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    private String getStringFromGeometry(JDBCSession session, Geometry geometry) throws DBCException {
        String strGeom = geometry.toString();
        if (geometry.getSRID() > 0) {
            return "SRID=" + geometry.getSRID() + ";" + strGeom;
        } else {
            return strGeom;
        }
    }

}

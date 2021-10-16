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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
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
import org.locationtech.jts.io.WKTWriter;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Postgre geometry handler
 */
public class PostgreGeometryValueHandler extends JDBCAbstractValueHandler {
    public static final PostgreGeometryValueHandler INSTANCE = new PostgreGeometryValueHandler();

    private static final Log log = Log.getLog(PostgreGeometryValueHandler.class);

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
            return makeGeometryFromWKT(session, (String) object);
        } else if (object.getClass().getName().equals(PostgreConstants.PG_GEOMETRY_CLASS)) {
            return makeGeometryFromPGGeometry(session, object);
        } else if (PostgreUtils.isPGObject(object)) {
            return makeGeometryFromWKB(CommonUtils.toString(PostgreUtils.extractPGObjectValue(object)));
        } else {
            return makeGeometryFromWKT(session, object.toString());
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (value instanceof DBGeometry && format == DBDDisplayFormat.NATIVE) {
            int valueSRID = ((DBGeometry) value).getSRID();
            String strValue = value.toString();
            if (valueSRID != 0 && !strValue.startsWith("SRID=")) {
                strValue = "SRID=" + valueSRID + ";" + strValue;
            }
            return strValue;
        }
        return super.getValueDisplayString(column, value, format);
    }

    protected DBGeometry makeGeometryFromWKB(String hexString) throws DBCException {
        return makeGeometryFromWKB(WKBReader.hexToBytes(hexString));
    }

    protected DBGeometry makeGeometryFromWKB(byte[] binary) throws DBCException {
        try {
            return new DBGeometry(new WKBReader().read(binary));
        } catch (ParseException e) {
            throw new DBCException("Error parsing WKB value", e);
        }
    }

    private DBGeometry makeGeometryFromPGGeometry(DBCSession session, Object value) throws DBCException {
        try {
            final Object geometry = BeanUtils.invokeObjectMethod(value, "getGeometry");

            try {
                // The string representation of geometry values returned from PostGIS
                // lacks 'Z' and 'M' modifiers for 3D and 4D geometries (which is not
                // specification-friendly), thus making it impossible to parse later.
                //
                // Code below is trying to build a valid WKT from available data

                // Use explicit cast because we want to fail if something went wrong
                final String type = (String) BeanUtils.invokeObjectMethod(geometry, "getTypeString");
                final boolean is3D = (Integer) BeanUtils.invokeObjectMethod(geometry, "getDimension") > 2;
                final boolean isMeasured = (Boolean) BeanUtils.invokeObjectMethod(geometry, "isMeasured");
                final int srid = (Integer) BeanUtils.invokeObjectMethod(geometry, "getSrid");

                // PostGIS JDBC uses StringBuffer instead of StringBuilder, yup
                final StringBuffer sb = new StringBuffer(type);

                if (is3D) {
                    sb.append('Z');
                }

                if (isMeasured) {
                    sb.append('M');
                }

                BeanUtils.invokeObjectDeclaredMethod(
                    geometry,
                    "mediumWKT",
                    new Class[]{StringBuffer.class},
                    new Object[]{sb}
                );

                final Geometry result = new WKTReader().read(sb.toString());
                result.setSRID(srid);

                return new DBGeometry(result);
            } catch (Throwable e) {
                log.error("Error reading geometry from PGGeometry", e);
                return makeGeometryFromWKT(session, geometry.toString());
            }
        } catch (Throwable e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    protected DBGeometry makeGeometryFromWKT(DBCSession session, String pgString) throws DBCException {
        if (CommonUtils.isEmpty(pgString)) {
            return new DBGeometry();
        }
        try {
            final String geometry;
            final int srid;

            if (pgString.startsWith("SRID=") && pgString.indexOf(';') > 5) {
                final int index = pgString.indexOf(';');
                geometry = pgString.substring(index + 1);
                srid = CommonUtils.toInt(pgString.substring(5, index));
            } else {
                geometry = pgString;
                srid = 0;
            }

            final Geometry result = new WKTReader().read(geometry);
            result.setSRID(srid);

            return new DBGeometry(result);
        } catch (Throwable e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    private String getStringFromGeometry(JDBCSession session, Geometry geometry) throws DBCException {
        // Use all possible dimensions (4 stands for XYZM) for the most verbose output (see DBGeometry#getString)
        final String strGeom = new WKTWriter(4).write(geometry);
        if (geometry.getSRID() > 0) {
            return "SRID=" + geometry.getSRID() + ";" + strGeom;
        } else {
            return strGeom;
        }
    }
}

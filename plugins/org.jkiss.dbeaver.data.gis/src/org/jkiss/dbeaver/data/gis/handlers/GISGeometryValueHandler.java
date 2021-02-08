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
package org.jkiss.dbeaver.data.gis.handlers;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentBytes;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;

import java.sql.SQLException;

/**
 * GIS geometry handler
 */
public class GISGeometryValueHandler extends JDBCAbstractValueHandler {

    private static final Log log = Log.getLog(GISGeometryValueHandler.class);

    private int defaultSRID;
    private boolean invertCoordinates;

    public GISGeometryValueHandler() {
        this(false);
    }

    public GISGeometryValueHandler(boolean invertCoordinates) {
        this.invertCoordinates = invertCoordinates;
    }

    public boolean isFlipCoordinates() {
        return invertCoordinates;
    }

    public int getDefaultSRID() {
        return defaultSRID;
    }

    public void setDefaultSRID(int defaultSRID) {
        this.defaultSRID = defaultSRID;
    }

    public void setInvertCoordinates(boolean invertCoordinates) {
        this.invertCoordinates = invertCoordinates;
    }

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException {
        return getValueFromObject(session, type,
            fetchBytes(resultSet, index),
            false, invertCoordinates);
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws DBCException, SQLException {
        if (value instanceof DBGeometry) {
            value = ((DBGeometry) value).getRawValue();
        }
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else if (value instanceof byte[]) {
            bindBytes(statement, paramIndex, (byte[]) value);
        } else if (value instanceof Geometry) {
            bindGeometryParameter(session, statement, paramIndex, (Geometry) value);
        }
    }

    protected void bindGeometryParameter(@NotNull JDBCSession session, @NotNull JDBCPreparedStatement statement, int paramIndex, @NotNull Geometry value) throws SQLException, DBCException {
        bindBytes(statement, paramIndex, convertGeometryToBinaryFormat(session, value));
    }

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return DBGeometry.class;
    }

    @NotNull
    @Override
    public DBGeometry getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException {
        DBGeometry geometry;
        if (object == null) {
            geometry = new DBGeometry();
        } else if (object instanceof DBGeometry) {
            if (copy) {
                geometry = ((DBGeometry) object).copy();
            } else {
                geometry = (DBGeometry) object;
            }
        } else if (object instanceof Geometry) {
            geometry = new DBGeometry((Geometry)object);
        } else if (object instanceof byte[] || object instanceof JDBCContentBytes) {
            byte[] bytes;
            if (object instanceof JDBCContentBytes) {
                bytes = ((JDBCContentBytes) object).getRawValue();
            } else {
                bytes = (byte[]) object;
            }
            try {
                Geometry jtsGeometry = convertGeometryFromBinaryFormat(session, bytes);
//            if (invertCoordinates) {
//                jtsGeometry.apply(GeometryConverter.INVERT_COORDINATE_FILTER);
//            }
                geometry = new DBGeometry(jtsGeometry);
            } catch (DBCException e) {
                throw new DBCException("Error parsing geometry value from binary", e);
            }
        } else if (object instanceof String) {
            try {
                Geometry jtsGeometry = new WKTReader().read((String) object);
                geometry = new DBGeometry(jtsGeometry);
            } catch (Exception e) {
                throw new DBCException("Error parsing geometry value from string", e);
            }
        } else {
            throw new DBCException("Unsupported geometry value: " + object);
        }
        if (geometry.getSRID() == 0) {
            geometry.setSRID(defaultSRID);
        }
        return geometry;
    }

    protected Geometry convertGeometryFromBinaryFormat(DBCSession session, byte[] object) throws DBCException {
        try {
            return new WKBReader().read(object);
        } catch (ParseException e) {
            throw new DBCException("Error reading geometry from binary data", e);
        }
    }

    protected byte[] convertGeometryToBinaryFormat(DBCSession session, Geometry geometry) throws DBCException {
        return new WKBWriter(2 /* default */, geometry.getSRID() > 0).write(geometry);
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (value instanceof DBGeometry && format == DBDDisplayFormat.NATIVE) {
            return "'" + value.toString() + "'";
        } else if (value instanceof JDBCContentBytes) {
            byte[] bytes = ((JDBCContentBytes) value).getRawValue();
            if (bytes.length != 0) {
                try {
                    Geometry geometry = convertGeometryFromBinaryFormat(null, bytes);
                    return geometry.toString();
                } catch (DBCException e) {
                    log.debug("Error parsing string geometry value from binary");
                }
            }
        }
        return super.getValueDisplayString(column, value, format);
    }

    protected byte[] fetchBytes(@NotNull JDBCResultSet resultSet, int index) throws SQLException {
        return resultSet.getBytes(index);
    }

    protected void bindBytes(@NotNull JDBCPreparedStatement dbStat, int index, @NotNull byte[] bytes) throws SQLException {
        dbStat.setBytes(index, bytes);
    }

}

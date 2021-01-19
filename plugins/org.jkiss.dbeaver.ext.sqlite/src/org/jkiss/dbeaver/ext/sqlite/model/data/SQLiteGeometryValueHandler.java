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
package org.jkiss.dbeaver.ext.sqlite.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ByteArrayInStream;
import org.locationtech.jts.io.WKBReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;

public class SQLiteGeometryValueHandler extends JDBCAbstractValueHandler {
    public static final SQLiteGeometryValueHandler INSTANCE = new SQLiteGeometryValueHandler();

    public static final String[] GEOMETRY_TYPES = new String[]{
        "GEOMETRY",
        "POINT",
        "LINESTRING",
        "POLYGON",
        "MULTIPOINT",
        "MULTILINESTRING",
        "MULTIPOLYGON",
        "GEOMETRYCOLLECTION"
    };

    private static final Log log = Log.getLog(SQLiteGeometryValueHandler.class);

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return DBGeometry.class;
    }

    @Nullable
    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue) {
        if (object == null) {
            return new DBGeometry();
        }

        if (object instanceof byte[]) {
            final byte[] bytes = (byte[]) object;
            final ByteBuffer buffer = ByteBuffer.wrap(bytes);

            // http://www.geopackage.org/spec121/index.html#gpb_format

            if (buffer.get() != 'G' || buffer.get() != 'P') {
                log.debug("Invalid GeoPackage data");
                return object;
            }

            final byte version = buffer.get();

            if (version != 0) {
                log.debug("Invalid GeoPackage version: " + version);
                return object;
            }

            final byte flags = buffer.get();

            if (CommonUtils.isBitSet(flags, 1)) {
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            }

            final int srsId = buffer.getInt();

            switch ((byte) ((flags >> 1) & 0b111)) {
                case 1:
                    buffer.position(buffer.position() + 32);
                    break;
                case 2:
                case 3:
                    buffer.position(buffer.position() + 48);
                    break;
                case 4:
                    buffer.position(buffer.position() + 64);
                    break;
                default:
                    break;
            }

            byte[] wkb = new byte[bytes.length - buffer.position()];
            System.arraycopy(bytes, buffer.position(), wkb, 0, wkb.length);

            try {
                GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), srsId);
                WKBReader wkbReader = new WKBReader(geometryFactory);
                Geometry geometry = wkbReader.read(new ByteArrayInStream(wkb));
                return new DBGeometry(geometry, srsId);
            } catch (Exception e) {
                log.debug("Error reading GeoPackage WKB", e);
                return object;
            }
        }

        return object;
    }

    @Nullable
    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws SQLException {
        Object object = resultSet.getObject(index);
        return getValueFromObject(session, type, object, false, false);
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws SQLException {
        statement.setObject(paramIndex, value);
    }
}

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
 */
package org.jkiss.dbeaver.ext.duckdb.model.data;

import org.jkiss.dbeaver.data.gis.handlers.GISGeometryValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;

public class DuckDBGeometryValueHandler extends GISGeometryValueHandler {
    public static final DuckDBGeometryValueHandler INSTANCE = new DuckDBGeometryValueHandler();

    @Override
    protected Object fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index
    ) throws DBCException, SQLException {
        try {
            return getValueFromObject(session, type, resultSet.getBytes(index), false, false);
        } catch (Exception e) {
            return getValueFromObject(session, type, resultSet.getString(index), false, false);
        }
    }

    @Override
    protected Geometry convertGeometryFromBinaryFormat(DBCSession session, byte[] object) throws DBCException {
        try {
            var buffer = ByteBuffer.wrap(object).order(ByteOrder.LITTLE_ENDIAN);
            var factory = new GeometryFactory(new PrecisionModel());
            return DuckDBGeometryConverter.deserialize(buffer, factory);
        } catch (Exception e) {
            return super.convertGeometryFromBinaryFormat(session, object);
        }
    }
}

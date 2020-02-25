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
package org.jkiss.dbeaver.ext.h2gis.data;

import org.jkiss.dbeaver.data.gis.handlers.GISGeometryValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;

/**
 * H2GIS Server geometry handler
 */
public class H2GISGeometryValueHandler extends GISGeometryValueHandler {

    public static final H2GISGeometryValueHandler INSTANCE = new H2GISGeometryValueHandler();

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException {
        return getValueFromObject(session, type,
            resultSet.getObject(index),
            false, false);
    }

    /*
    protected void bindGeometryParameter(@NotNull JDBCSession session, @NotNull JDBCPreparedStatement statement, int paramIndex, @NotNull Geometry value) throws SQLException, DBCException {
        statement.setString(paramIndex, value.toString());
    }

    @Override
    protected byte[] convertGeometryToBinaryFormat(DBCSession session, Geometry geometry) throws DBCException {
        throw new DBCException("Saving in SQL Server binary format not supported yet");
    }

    @Override
    public String makeQueryBind(DBSAttributeBase attribute, Object value) throws DBCException {
        int srid = 0;
        if (value instanceof DBGeometry) {
            srid = ((DBGeometry) value).getSRID();
        } else if (value instanceof Geometry) {
            srid = ((Geometry) value).getSRID();
        }
        return "geometry::STGeomFromText(?," + srid + ")";
    }
*/
}

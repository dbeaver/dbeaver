/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.model.data.PostgreGeometryValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.gis.GisAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;

import java.sql.SQLException;
import java.sql.Types;

public class RedshiftGeometryValueHandler extends PostgreGeometryValueHandler {
    public static final RedshiftGeometryValueHandler INSTANCE = new RedshiftGeometryValueHandler();

    private RedshiftGeometryValueHandler() {}

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException {
        return getValueFromObject(session, type, resultSet.getString(index), false, false);
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
            statement.setString(paramIndex, WKBWriter.toHex(new WKBWriter(3).write((Geometry) value)));
        } else {
            throw new DBCException("Can't bind geometry value " + value);
        }
    }

    @Nullable
    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException {
        if (object == null) {
            return null;
        }
        if (object instanceof DBGeometry) {
            return copy ? ((DBGeometry) object).copy() : object;
        }
        if (object instanceof Geometry) {
            return new DBGeometry((Geometry) object);
        }
        if (object instanceof byte[]) {
             return makeGeometryFromWKB((byte[]) object);
        }
        if (object instanceof String) {
            try {
                // It is WKB when read from server
                return makeGeometryFromWKB((String) object);
            } catch (Exception e) {
                // It may be WKT when edited by user
                return makeGeometryFromWKT(session, (String) object);
            }
        }
        return null;
    }
}

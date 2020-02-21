/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 DBeaver Corp and others
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
 *
 * Contributors:
 *    Stefan Uhrig - initial implementation
 */
package org.jkiss.dbeaver.ext.hana.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.hana.model.data.wkb.HANAWKBParser;
import org.jkiss.dbeaver.ext.hana.model.data.wkb.HANAWKBParserException;
import org.jkiss.dbeaver.ext.hana.model.data.wkb.HANAWKBWriter;
import org.jkiss.dbeaver.ext.hana.model.data.wkb.HANAWKBWriterException;
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
import org.locationtech.jts.geom.Geometry;

import java.sql.SQLException;

public class HANAGeometryValueHandler extends JDBCAbstractValueHandler {

    public static final HANAGeometryValueHandler INSTANCE = new HANAGeometryValueHandler();

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index)
            throws DBCException, SQLException {
        byte[] wkb = resultSet.getBytes(index);
        if (wkb == null) {
            return null;
        }
        HANAWKBParser parser = new HANAWKBParser();
        try {
            Geometry g = parser.parse(wkb);
            return new DBGeometry(g);
        } catch (HANAWKBParserException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType,
            int paramIndex, Object value) throws DBCException, SQLException {
        Object geometry = value;
        int srid = 0;
        if (geometry instanceof DBGeometry) {
            srid = ((DBGeometry) geometry).getSRID();
            geometry = ((DBGeometry) geometry).getRawValue();
        }
        if (srid == 0 && paramType instanceof GisAttribute) {
            srid = ((GisAttribute) paramType).getAttributeGeometrySRID(session.getProgressMonitor());
        }
        if (geometry == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else if (geometry instanceof Geometry) {
            Geometry g = (Geometry) geometry;
            if (g.getSRID() == 0) {
                g.setSRID(srid);
            }
            try {
                statement.setBytes(paramIndex, HANAWKBWriter.write(g, HANAXyzmModeFinder.findXyzmMode(g)));
            } catch (HANAWKBWriterException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        } else {
            throw new DBCException("Could not bind the value because the value type is not a known geometry type");
        }
    }

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return DBGeometry.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object,
                                     boolean copy, boolean validateValue) throws DBCException {
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
        } else if (object instanceof byte[]) {
            byte[] wkb = (byte[]) object;
            HANAWKBParser parser = new HANAWKBParser();
            try {
                Geometry g = parser.parse(wkb);
                return new DBGeometry(g);
            } catch (HANAWKBParserException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        } else {
            throw new DBCException(
                    "Could not get geometry value from object because the object type is not a known geometry type");
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value,
            @NotNull DBDDisplayFormat format) {
        if (value instanceof DBGeometry && format == DBDDisplayFormat.NATIVE) {
            return "'" + value.toString() + "'";
        }
        return super.getValueDisplayString(column, value, format);
    }

}

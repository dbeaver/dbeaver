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

package org.jkiss.dbeaver.ext.altibase.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.data.gis.handlers.GISGeometryValueHandler;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueBinder;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.gis.GisAttribute;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.sql.SQLException;

/*
 * AltibaseGeometryValueHandler
 */
public class AltibaseGeometryValueHandler extends GISGeometryValueHandler implements DBDValueBinder {

    private static final Log log = Log.getLog(AltibaseGeometryValueHandler.class);

    public static final AltibaseGeometryValueHandler INSTANCE = new AltibaseGeometryValueHandler();

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, 
            DBSTypedObject type, int index) throws DBCException, SQLException {
        Object object = resultSet.getObject(index);
        return getValueFromObject(session, type, object, false, false);
    }

    @Override
    public DBGeometry getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, 
            Object object, boolean copy, boolean validateValue) throws DBCException {

        DBGeometry dbGeometry = null;
        int srid = 0;

        try {
            if (object == null) {
                return new DBGeometry();
            } else if (object instanceof DBGeometry) {
                if (copy) {
                    return ((DBGeometry) object).copy();
                } else {
                    return ((DBGeometry) object);
                }
            }  else if (object instanceof Geometry) {
                return new DBGeometry((Geometry) object);
            } else {
                /*
                 * EWKT: 32,000 is maximum length of return value.
                 * SRID=xxxx;MULTIPOLYGON (((199....
                 */

                Geometry geom = null;

                String[] content = ((String) object).split(";", 2);

                // start with SRID=xxxx;
                if (content.length == 2) {

                    // get srid
                    content[0] = content[0].replaceAll("SRID=", "");
                    try {
                        srid = Integer.parseInt(content[0]);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse SRID: " + e);
                    }

                    // geom from WKT
                    geom = new WKTReader().read(content[1]);

                } else if (content.length == 1) {
                    // No SRID, just in case.
                    geom = new WKTReader().read(content[0]);
                }

                dbGeometry = new DBGeometry(geom);
                dbGeometry.setSRID(srid);
            }
        } catch (ParseException e) {
            log.warn("Failed to parse objedt: " 
                    + ((object != null) ? object.toString() : "NULL") 
                    + AltibaseConstants.NEW_LINE 
                    + e.getLocalizedMessage());
        }

        return dbGeometry;
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, 
            DBSTypedObject paramType, int paramIndex, Object value) throws DBCException, SQLException {

        int srid = 0;
        if (paramType instanceof DBDAttributeBinding) {
            paramType = ((DBDAttributeBinding) paramType).getAttribute();
        }
        if (value instanceof DBGeometry) {
            srid = ((DBGeometry) value).getSRID();
            value = ((DBGeometry) value).getRawValue();
        }
        if (srid == 0 && paramType instanceof GisAttribute) {
            srid = ((GisAttribute) paramType).getAttributeGeometrySRID(session.getProgressMonitor());
        }
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else if (value instanceof Geometry) {
            if (((Geometry) value).getSRID() == 0) {
                ((Geometry) value).setSRID(srid);
            }
            // format: GEOMFROMTEXT(?, SRID)
            statement.setString(paramIndex, new WKTWriter(4).write((Geometry) value));

        } else {
            String strValue = value.toString();
            if (srid != 0 && !strValue.startsWith("SRID=")) {
                strValue = "SRID=" + srid + ";" + strValue;
            }
            statement.setObject(paramIndex, strValue, AltibaseConstants.TYPE_GEOMETRY);
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (value instanceof DBGeometry) {             
            int valueSRID = ((DBGeometry) value).getSRID();
            String strValue = value.toString();
            if (valueSRID != 0 && !strValue.startsWith("SRID=")) {
                strValue = "SRID=" + valueSRID + ";" + strValue;
            }
            return strValue;
        }
        return super.getValueDisplayString(column, value, format);
    }

    @Override
    public String makeQueryBind(DBSAttributeBase attribute, Object value) throws DBCException {
        return "GEOMFROMTEXT(?, " + ((DBGeometry) value).getSRID() + ")";
    }
}
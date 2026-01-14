/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.data.gis.handlers.GISGeometryValueHandler;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * MySQLGeometryValueHandler
 */
public class MySQLGeometryValueHandler extends GISGeometryValueHandler {

    static final MySQLGeometryValueHandler INSTANCE = new MySQLGeometryValueHandler();

    public MySQLGeometryValueHandler() {
        setInvertCoordinates(true);
        setLeadingSRID(true);
    }

    @NotNull
    @Override
    public DBGeometry getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException {
        final DBGeometry geometry = super.getValueFromObject(session, type, object, copy, validateValue);
        if (geometry.getSRID() == 0) {
            if (type instanceof DBDAttributeBinding binding) {
                type = binding.getEntityAttribute();
            }
            if (type instanceof MySQLTableColumn c && c.getSrid() != null) {
                geometry.setSRID(c.getSrid());
            }
        }
        return geometry;
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (format == DBDDisplayFormat.NATIVE) {
            int srid = 0;
            if (value instanceof DBGeometry geometry) {
                srid = geometry.getSRID();
            }
            if (srid == 0) {
                if (column instanceof DBDAttributeBinding binding) {
                    column = binding.getEntityAttribute();
                }
                if (column instanceof MySQLTableColumn c && c.getSrid() != null) {
                    srid = c.getSrid();
                }
            }
            if (srid != 0) {
                return "ST_GeomFromText('" + value.toString() + "', " + srid + ")";
            } else {
                return "ST_GeomFromText('" + value.toString() + "')";
            }
        }
        return super.getValueDisplayString(column, value, format);
    }
}

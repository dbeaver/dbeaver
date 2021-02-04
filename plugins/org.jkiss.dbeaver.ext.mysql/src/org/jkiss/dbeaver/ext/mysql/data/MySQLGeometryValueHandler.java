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
package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.dbeaver.data.gis.handlers.GISGeometryValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * MySQLGeometryValueHandler
 *
 * MySQL has special data format [SRID] [WKB]
 * http://www.dev-garden.org/2011/11/27/loading-mysql-spatial-data-with-jdbc-and-jts-wkbreader/
 */
public class MySQLGeometryValueHandler extends GISGeometryValueHandler {
    public static final MySQLGeometryValueHandler INSTANCE = new MySQLGeometryValueHandler();

    public MySQLGeometryValueHandler() {
        super(true);
    }

    @Override
    protected Geometry convertGeometryFromBinaryFormat(DBCSession session, byte[] object) throws DBCException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(object)) {
            int srid = 0;
            srid |= is.read();
            srid |= is.read() << 8;
            srid |= is.read() << 16;
            srid |= is.read() << 24;

            Geometry geometry = new WKBReader().read(new InputStreamInStream(is));
            geometry.setSRID(srid);

            return geometry;
        } catch (Exception e) {
            throw new DBCException("Error reading geometry from binary data", e);
        }
    }

    @Override
    protected byte[] convertGeometryToBinaryFormat(DBCSession session, Geometry geometry) throws DBCException {
        final int srid = geometry.getSRID();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            os.write((byte) (srid));
            os.write((byte) (srid >> 8));
            os.write((byte) (srid >> 16));
            os.write((byte) (srid >> 24));

            WKBWriter writer = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN, false);
            writer.write(geometry, new OutputStreamOutStream(os));

            return os.toByteArray();
        } catch (IOException e) {
            throw new DBCException("Error writing geometry to binary data", e);
        }
    }
}

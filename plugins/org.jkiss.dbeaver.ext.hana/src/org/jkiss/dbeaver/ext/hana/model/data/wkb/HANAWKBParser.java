/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.model.data.wkb;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.MessageFormat;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * A parser for the well-known-binary created by HANA.
 *
 * The JTS parser cannot be used to parse 3- or 4-dimensional geometries because
 * of different type code conventions. HANA offsets type codes by multiples of
 * 1000 (as described in the relevant OGC and SQL/MM standard) while JTS expects
 * specific bits to be set.
 */
public class HANAWKBParser {

    private static final byte XDR = 0;

    private static final byte NDR = 1;

    private static final int TYPE_MASK = 0xFFFFF;

    private static final int XYZM_MODE_DIV = 1000;

    private static final int XYZM_MODE_XY = 0;

    private static final int XYZM_MODE_XYZ = 1;

    private static final int XYZM_MODE_XYM = 2;

    private static final int XYZM_MODE_XYZM = 3;

    private static final int EWKB_FLAG = 0x20000000;

    private GeometryFactory factory;

    private ByteBuffer data;

    private XyzmMode xyzmMode;

    private int dimension;

    public Geometry parse(byte[] wkb) throws HANAWKBParserException {
        data = ByteBuffer.wrap(wkb);
        try {
            readAndSetByteOrder();
            int typeCode = data.getInt();
            boolean isEwkb = (typeCode & EWKB_FLAG) != 0;
            if (isEwkb) {
                typeCode -= EWKB_FLAG;
            }
            GeometryType type = getGeometryType(typeCode);
            xyzmMode = getXyzmMode(typeCode);
            dimension = xyzmMode.getCoordinatesPerPoint();

            int srid = isEwkb ? data.getInt() : 0;
            factory = new GeometryFactory(new PrecisionModel(), srid);

            Geometry geometry = parseGeometryOfType(type);
            if (data.hasRemaining()) {
                throw new HANAWKBParserException("There is unparsed WKB data left");
            }
            return geometry;
        } catch (BufferUnderflowException e) {
            throw new HANAWKBParserException("WKB is too short", e);
        }
    }

    private Geometry parseGeometryOfType(GeometryType type) throws HANAWKBParserException {
        switch (type) {
        case POINT:
            return parsePoint();
        case LINESTRING:
            return parseLineString();
        case POLYGON:
            return parsePolygon();
        case MULTIPOINT:
            return parseMultiPoint();
        case MULTILINESTRING:
            return parseMultiLineString();
        case MULTIPOLYGON:
            return parseMultiPolygon();
        case GEOMETRYCOLLECTION:
            return parseGeometryCollection();
        case CIRCULARSTRING:
            throw new HANAWKBParserException("Circular strings are not supported by JTS");
        default:
            throw new AssertionError();
        }
    }

    private Point parsePoint() {
        double x = data.getDouble();
        double y = data.getDouble();
        double z = Double.NaN;
        double m = Double.NaN;
        if (xyzmMode.hasZ()) {
            z = data.getDouble();
        }
        if (xyzmMode.hasM()) {
            m = data.getDouble();
        }
        CoordinateSequenceFactory csf = factory.getCoordinateSequenceFactory();
        if (Double.isNaN(x)) {
            CoordinateSequence cs = csf.create(0, dimension, xyzmMode.hasM() ? 1 : 0);
            return factory.createPoint(cs);
        }
        CoordinateSequence cs = csf.create(1, dimension, xyzmMode.hasM() ? 1 : 0);
        cs.getCoordinate(0).setX(x);
        cs.getCoordinate(0).setY(y);
        if (xyzmMode.hasZ()) {
            cs.getCoordinate(0).setZ(z);
        }
        if (xyzmMode.hasM()) {
            cs.getCoordinate(0).setM(m);
        }
        return factory.createPoint(cs);
    }

    private LineString parseLineString() {
        CoordinateSequence cs = readCoordinateSequence();
        return factory.createLineString(cs);
    }

    private Polygon parsePolygon() {
        int numRings = data.getInt();
        if (numRings == 0) {
            return factory.createPolygon((LinearRing) null);
        }
        LinearRing shell = parseLinearRing();
        if (numRings == 1) {
            return factory.createPolygon(shell);
        }
        LinearRing[] holes = new LinearRing[numRings - 1];
        for (int i = 1; i < numRings; ++i) {
            holes[i - 1] = parseLinearRing();
        }
        return factory.createPolygon(shell, holes);
    }

    private MultiPoint parseMultiPoint() throws HANAWKBParserException {
        int numPoints = data.getInt();
        Point[] points = new Point[numPoints];
        for (int i = 0; i < numPoints; ++i) {
            points[i] = (Point) parseSubGeometry();
        }
        return factory.createMultiPoint(points);
    }

    private MultiLineString parseMultiLineString() throws HANAWKBParserException {
        int numLineStrings = data.getInt();
        LineString[] lineStrings = new LineString[numLineStrings];
        for (int i = 0; i < numLineStrings; ++i) {
            lineStrings[i] = (LineString) parseSubGeometry();
        }
        return factory.createMultiLineString(lineStrings);
    }

    private MultiPolygon parseMultiPolygon() throws HANAWKBParserException {
        int numPolygons = data.getInt();
        Polygon[] polygons = new Polygon[numPolygons];
        for (int i = 0; i < numPolygons; ++i) {
            polygons[i] = (Polygon) parseSubGeometry();
        }
        return factory.createMultiPolygon(polygons);
    }

    private GeometryCollection parseGeometryCollection() throws HANAWKBParserException {
        int numGeometries = data.getInt();
        Geometry[] geometries = new Geometry[numGeometries];
        for (int i = 0; i < numGeometries; ++i) {
            geometries[i] = parseSubGeometry();
        }
        return factory.createGeometryCollection(geometries);
    }

    private Geometry parseSubGeometry() throws HANAWKBParserException {
        readAndSetByteOrder();
        int typeCode = data.getInt();
        GeometryType type = getGeometryType(typeCode);
        return parseGeometryOfType(type);
    }

    private LinearRing parseLinearRing() {
        CoordinateSequence cs = patchRing(readCoordinateSequence());
        return factory.createLinearRing(cs);
    }

    private CoordinateSequence patchRing(CoordinateSequence cs) {
        if ((cs.size() >= 4) || (cs.size() == 0)) {
            return cs;
        }
        Coordinate[] coords = new Coordinate[4];
        for (int i = 0; i < cs.size(); ++i) {
            coords[i] = cs.getCoordinate(i);
        }
        for (int i = cs.size(); i < 4; ++i) {
            coords[i] = cs.getCoordinate(0);
        }
        CoordinateSequenceFactory csf = factory.getCoordinateSequenceFactory();
        return csf.create(coords);
    }

    private CoordinateSequence readCoordinateSequence() {
        CoordinateSequenceFactory csf = factory.getCoordinateSequenceFactory();
        int numPoints = data.getInt();
        CoordinateSequence cs = csf.create(numPoints, dimension, xyzmMode.hasM() ? 1 : 0);
        switch (xyzmMode) {
        case XY:
            for (int i = 0; i < numPoints; ++i) {
                cs.getCoordinate(i).setX(data.getDouble());
                cs.getCoordinate(i).setY(data.getDouble());
            }
            break;
        case XYZ:
            for (int i = 0; i < numPoints; ++i) {
                cs.getCoordinate(i).setX(data.getDouble());
                cs.getCoordinate(i).setY(data.getDouble());
                cs.getCoordinate(i).setZ(data.getDouble());
            }
            break;
        case XYM:
            for (int i = 0; i < numPoints; ++i) {
                cs.getCoordinate(i).setX(data.getDouble());
                cs.getCoordinate(i).setY(data.getDouble());
                cs.getCoordinate(i).setM(data.getDouble());
            }
            break;
        case XYZM:
            for (int i = 0; i < numPoints; ++i) {
                cs.getCoordinate(i).setX(data.getDouble());
                cs.getCoordinate(i).setY(data.getDouble());
                cs.getCoordinate(i).setZ(data.getDouble());
                cs.getCoordinate(i).setM(data.getDouble());
            }
            break;
        default:
            throw new AssertionError();
        }
        return cs;
    }

    private GeometryType getGeometryType(int typeCode) throws HANAWKBParserException {
        int wkbType = typeCode & TYPE_MASK;
        wkbType = wkbType % XYZM_MODE_DIV;
        GeometryType type = GeometryType.getFromCode(wkbType);
        if (type == null) {
            throw new HANAWKBParserException(MessageFormat.format("Unknown WKB type {0}", wkbType));
        }
        return type;
    }

    private XyzmMode getXyzmMode(int typeCode) throws HANAWKBParserException {
        int wkbType = typeCode & TYPE_MASK;
        int xyzmFlag = wkbType / XYZM_MODE_DIV;
        switch (xyzmFlag) {
        case XYZM_MODE_XY:
            return XyzmMode.XY;
        case XYZM_MODE_XYZ:
            return XyzmMode.XYZ;
        case XYZM_MODE_XYM:
            return XyzmMode.XYM;
        case XYZM_MODE_XYZM:
            return XyzmMode.XYZM;
        default:
            throw new HANAWKBParserException(MessageFormat.format("Invalid XYZM-mode {0}", xyzmFlag));
        }
    }

    private void readAndSetByteOrder() throws HANAWKBParserException {
        byte order = data.get();
        switch (order) {
        case XDR:
            data.order(ByteOrder.BIG_ENDIAN);
            break;
        case NDR:
            data.order(ByteOrder.LITTLE_ENDIAN);
            break;
        default:
            throw new HANAWKBParserException(MessageFormat.format("Invalid BOM value {0}", order));
        }
    }
}

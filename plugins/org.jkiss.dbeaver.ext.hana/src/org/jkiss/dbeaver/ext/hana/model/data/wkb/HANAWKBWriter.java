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

import org.locationtech.jts.geom.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.MessageFormat;

/**
 * A well-known binary writer.
 *
 * The JTS WKB writer cannot be used as it rejects empty points.
 */
public class HANAWKBWriter {

    private static final int HEADER_SIZE = 5;

    private static final int COUNT_SIZE = 4;

    private static final int COORD_SIZE = 8;

    private static final byte NDR = 1;

    private static final int Z_OFFSET = 1000;

    private static final int M_OFFSET = 2000;

    public static byte[] write(Geometry geometry, XyzmMode xyzmMode) throws HANAWKBWriterException {
        if (geometry == null) {
            return null;
        }
        int size = computeGeometrySize(geometry, xyzmMode);
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        writeGeometry(geometry, xyzmMode, buffer);
        return buffer.array();
    }

    private static int computeGeometrySize(Geometry geometry, XyzmMode xyzmMode) throws HANAWKBWriterException {
        if (geometry instanceof Point) {
            return computePointSize(xyzmMode);
        } else if (geometry instanceof LineString) {
            return computeLineStringSize((LineString) geometry, xyzmMode);
        } else if (geometry instanceof Polygon) {
            return computePolygonSize((Polygon) geometry, xyzmMode);
        } else if (geometry instanceof MultiPoint) {
            return computeMultiPointSize((MultiPoint) geometry, xyzmMode);
        } else if (geometry instanceof MultiLineString) {
            return computeMultiLineStringSize((MultiLineString) geometry, xyzmMode);
        } else if (geometry instanceof MultiPolygon) {
            return computeMultiPolygonSize((MultiPolygon) geometry, xyzmMode);
        } else if (geometry instanceof GeometryCollection) {
            return computeGeometryCollectionSize((GeometryCollection) geometry, xyzmMode);
        } else {
            throw new HANAWKBWriterException(
                    MessageFormat.format("Unsupported geometry type {0}", geometry.getGeometryType()));
        }
    }

    private static int computePointSize(XyzmMode xyzmMode) {
        return HEADER_SIZE + xyzmMode.getCoordinatesPerPoint() * COORD_SIZE;
    }

    private static int computeLineStringSize(LineString lineString, XyzmMode xyzmMode) {
        return HEADER_SIZE + COUNT_SIZE + lineString.getNumPoints() * xyzmMode.getCoordinatesPerPoint() * COORD_SIZE;
    }

    private static int computePolygonSize(Polygon polygon, XyzmMode xyzmMode) {
        int size = HEADER_SIZE + COUNT_SIZE;
        LineString shell = polygon.getExteriorRing();
        if ((shell == null) || (shell.getNumPoints() == 0)) {
            return size;
        }
        int pointSize = xyzmMode.getCoordinatesPerPoint() * COORD_SIZE;
        size += COUNT_SIZE + shell.getNumPoints() * pointSize;
        int numHoles = polygon.getNumInteriorRing();
        for (int i = 0; i < numHoles; ++i) {
            size += COUNT_SIZE + polygon.getInteriorRingN(i).getNumPoints() * pointSize;
        }
        return size;
    }

    private static int computeMultiPointSize(MultiPoint multiPoint, XyzmMode xyzmMode) {
        int pointSize = xyzmMode.getCoordinatesPerPoint() * COORD_SIZE;
        return HEADER_SIZE + COUNT_SIZE + multiPoint.getNumPoints() * (HEADER_SIZE + pointSize);
    }

    private static int computeMultiLineStringSize(MultiLineString multiLineString, XyzmMode xyzmMode) {
        int size = HEADER_SIZE + COUNT_SIZE;
        for (int i = 0; i < multiLineString.getNumGeometries(); ++i) {
            size += computeLineStringSize((LineString) multiLineString.getGeometryN(i), xyzmMode);
        }
        return size;
    }

    private static int computeMultiPolygonSize(MultiPolygon multiPolygon, XyzmMode xyzmMode) {
        int size = HEADER_SIZE + COUNT_SIZE;
        for (int i = 0; i < multiPolygon.getNumGeometries(); ++i) {
            size += computePolygonSize((Polygon) multiPolygon.getGeometryN(i), xyzmMode);
        }
        return size;
    }

    private static int computeGeometryCollectionSize(GeometryCollection geometryCollection, XyzmMode xyzmMode)
            throws HANAWKBWriterException {
        int size = HEADER_SIZE + COUNT_SIZE;
        for (int i = 0; i < geometryCollection.getNumGeometries(); ++i) {
            size += computeGeometrySize(geometryCollection.getGeometryN(i), xyzmMode);
        }
        return size;
    }

    private static void writeGeometry(Geometry geometry, XyzmMode xyzmMode, ByteBuffer buffer)
            throws HANAWKBWriterException {
        if (geometry instanceof Point) {
            writePoint((Point) geometry, xyzmMode, buffer);
        } else if (geometry instanceof LineString) {
            writeLineString((LineString) geometry, xyzmMode, buffer);
        } else if (geometry instanceof Polygon) {
            writePolygon((Polygon) geometry, xyzmMode, buffer);
        } else if (geometry instanceof MultiPoint) {
            writeMultiPoint((MultiPoint) geometry, xyzmMode, buffer);
        } else if (geometry instanceof MultiLineString) {
            writeMultiLineString((MultiLineString) geometry, xyzmMode, buffer);
        } else if (geometry instanceof MultiPolygon) {
            writeMultiPolygon((MultiPolygon) geometry, xyzmMode, buffer);
        } else if (geometry instanceof GeometryCollection) {
            writeGeometryCollection((GeometryCollection) geometry, xyzmMode, buffer);
        } else {
            throw new HANAWKBWriterException(
                    MessageFormat.format("Unsupported geometry type {0}", geometry.getGeometryType()));
        }
    }

    private static void writePoint(Point point, XyzmMode xyzmMode, ByteBuffer buffer) {
        writeHeader(GeometryType.POINT, xyzmMode, buffer);
        CoordinateSequence cs = point.getCoordinateSequence();
        if (cs.size() == 0) {
            for (int i = 0; i < xyzmMode.getCoordinatesPerPoint(); ++i) {
                buffer.putDouble(Double.NaN);
            }
        } else {
            buffer.putDouble(cs.getX(0));
            buffer.putDouble(cs.getY(0));
            if (xyzmMode.hasZ()) {
                buffer.putDouble(cs.getZ(0));
            }
            if (xyzmMode.hasM()) {
                buffer.putDouble(cs.getM(0));
            }
        }
    }

    private static void writeLineString(LineString lineString, XyzmMode xyzmMode, ByteBuffer buffer) {
        writeHeader(GeometryType.LINESTRING, xyzmMode, buffer);
        writeCoordinateSequence(lineString.getCoordinateSequence(), xyzmMode, buffer);
    }

    private static void writePolygon(Polygon polygon, XyzmMode xyzmMode, ByteBuffer buffer) {
        writeHeader(GeometryType.POLYGON, xyzmMode, buffer);
        LineString shell = polygon.getExteriorRing();
        if ((shell == null) || (shell.getNumPoints() == 0)) {
            buffer.putInt(0);
            return;
        }
        int numHoles = polygon.getNumInteriorRing();
        buffer.putInt(1 + numHoles);

        writeCoordinateSequence(shell.getCoordinateSequence(), xyzmMode, buffer);
        for (int i = 0; i < numHoles; ++i) {
            LineString hole = polygon.getInteriorRingN(i);
            writeCoordinateSequence(hole.getCoordinateSequence(), xyzmMode, buffer);
        }
    }

    private static void writeMultiPoint(MultiPoint multiPoint, XyzmMode xyzmMode, ByteBuffer buffer) {
        writeHeader(GeometryType.MULTIPOINT, xyzmMode, buffer);
        int numPoints = multiPoint.getNumPoints();
        buffer.putInt(numPoints);
        for (int i = 0; i < numPoints; ++i) {
            writePoint((Point) multiPoint.getGeometryN(i), xyzmMode, buffer);
        }
    }

    private static void writeMultiLineString(MultiLineString multiLineString, XyzmMode xyzmMode, ByteBuffer buffer) {
        writeHeader(GeometryType.MULTILINESTRING, xyzmMode, buffer);
        int numLineStrings = multiLineString.getNumGeometries();
        buffer.putInt(numLineStrings);
        for (int i = 0; i < numLineStrings; ++i) {
            writeLineString((LineString) multiLineString.getGeometryN(i), xyzmMode, buffer);
        }
    }

    private static void writeMultiPolygon(MultiPolygon multiPolygon, XyzmMode xyzmMode, ByteBuffer buffer) {
        writeHeader(GeometryType.MULTIPOLYGON, xyzmMode, buffer);
        int numPolygons = multiPolygon.getNumGeometries();
        buffer.putInt(numPolygons);
        for (int i = 0; i < numPolygons; ++i) {
            writePolygon((Polygon) multiPolygon.getGeometryN(i), xyzmMode, buffer);
        }
    }

    private static void writeGeometryCollection(GeometryCollection geometryCollection, XyzmMode xyzmMode,
            ByteBuffer buffer) throws HANAWKBWriterException {
        writeHeader(GeometryType.GEOMETRYCOLLECTION, xyzmMode, buffer);
        int numGeometries = geometryCollection.getNumGeometries();
        buffer.putInt(numGeometries);
        for (int i = 0; i < numGeometries; ++i) {
            writeGeometry(geometryCollection.getGeometryN(i), xyzmMode, buffer);
        }
    }

    private static void writeCoordinateSequence(CoordinateSequence cs, XyzmMode xyzmMode, ByteBuffer buffer) {
        int numPoints = cs.size();
        buffer.putInt(numPoints);
        for (int i = 0; i < numPoints; ++i) {
            buffer.putDouble(cs.getX(i));
            buffer.putDouble(cs.getY(i));
            if (xyzmMode.hasZ()) {
                buffer.putDouble(cs.getZ(i));
            }
            if (xyzmMode.hasM()) {
                buffer.putDouble(cs.getM(i));
            }
        }
    }

    private static void writeHeader(GeometryType geometryType, XyzmMode xyzmMode, ByteBuffer buffer) {
        buffer.put(NDR);
        int typeCode = geometryType.getTypeCode();
        if (xyzmMode.hasZ()) {
            typeCode += Z_OFFSET;
        }
        if (xyzmMode.hasM()) {
            typeCode += M_OFFSET;
        }
        buffer.putInt(typeCode);
    }
}

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

import org.jkiss.code.NotNull;
import org.locationtech.jts.geom.*;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

// https://github.com/duckdb/duckdb-spatial/blob/450094cfa48b0485b55c096d280dbb0fe9185e82/src/spatial/geometry/geometry_serialization.cpp
public final class DuckDBGeometryConverter {
    private DuckDBGeometryConverter() {
    }

    @NotNull
    public static Geometry deserialize(@NotNull ByteBuffer buffer, @NotNull GeometryFactory factory) {
        GeometryType.valueOf(buffer.get()); // type
        var flags = buffer.get();
        buffer.getShort(); // unused
        buffer.getInt();   // padding

        var hasZ = (flags & 0x01) != 0;
        var hasM = (flags & 0x02) != 0;
        var hasBBox = (flags & 0x04) != 0;
        var dimensions = 2 + (hasZ ? 1 : 0) + (hasM ? 1 : 0);

        if (hasBBox) {
            buffer.position(buffer.position() + dimensions * Float.BYTES * 2);
        }

        return deserializeRecursive(buffer, hasZ, hasM, factory);
    }

    @NotNull
    private static Geometry deserializeRecursive(
        @NotNull ByteBuffer buffer,
        boolean hasZ,
        boolean hasM,
        @NotNull GeometryFactory factory
    ) {
        var type = GeometryType.valueOf(buffer.getInt());
        int count = buffer.getInt();

        return switch (type) {
            case POINT -> {
                var coordinates = readCoordinates(buffer, count, hasZ, hasM, factory);
                yield factory.createPoint(coordinates);
            }
            case LINESTRING -> {
                var coordinates = readCoordinates(buffer, count, hasZ, hasM, factory);
                yield factory.createLineString(coordinates);
            }
            case POLYGON -> {
                var ringCount = count + (count % 2 == 1 ? 1 : 0);
                var ringSizes = IntStream.range(0, ringCount)
                    .map(i -> buffer.getInt())
                    .toArray();
                var rings = IntStream.range(0, count)
                    .mapToObj(i -> factory.createLinearRing(readCoordinates(buffer, ringSizes[i], hasZ, hasM, factory)))
                    .toList();
                var shell = rings.get(0);
                var holes = rings.subList(1, rings.size()).toArray(LinearRing[]::new);
                yield factory.createPolygon(shell, holes);
            }
            case MULTI_POINT -> {
                var points = IntStream.range(0, count)
                    .mapToObj(i -> (Point) deserializeRecursive(buffer, hasZ, hasM, factory))
                    .toArray(Point[]::new);
                yield factory.createMultiPoint(points);
            }
            case MULTI_LINESTRING -> {
                var lineStrings = IntStream.range(0, count)
                    .mapToObj(i -> (LineString) deserializeRecursive(buffer, hasZ, hasM, factory))
                    .toArray(LineString[]::new);
                yield factory.createMultiLineString(lineStrings);
            }
            case MULTI_POLYGON -> {
                var polygons = IntStream.range(0, count)
                    .mapToObj(i -> (Polygon) deserializeRecursive(buffer, hasZ, hasM, factory))
                    .toArray(Polygon[]::new);
                yield factory.createMultiPolygon(polygons);
            }
            case MULTI_GEOMETRY -> {
                var geometries = IntStream.range(0, count)
                    .mapToObj(i -> deserializeRecursive(buffer, hasZ, hasM, factory))
                    .toArray(Geometry[]::new);
                yield factory.createGeometryCollection(geometries);
            }
        };
    }

    @NotNull
    private static CoordinateSequence readCoordinates(
        @NotNull ByteBuffer buffer,
        int count,
        boolean hasZ,
        boolean hasM,
        @NotNull GeometryFactory factory
    ) {
        var coordinates = IntStream.range(0, count)
            .mapToObj(i -> readCoordinate(buffer, hasZ, hasM))
            .toArray(Coordinate[]::new);
        return factory.getCoordinateSequenceFactory().create(coordinates);
    }

    @NotNull
    private static Coordinate readCoordinate(@NotNull ByteBuffer buffer, boolean hasZ, boolean hasM) {
        double x = buffer.getDouble();
        double y = buffer.getDouble();
        double z = hasZ ? buffer.getDouble() : 0;
        double m = hasM ? buffer.getDouble() : 0;

        if (hasZ && hasM) {
            return new CoordinateXYZM(x, y, z, m);
        } else if (hasM) {
            return new CoordinateXYM(x, y, m);
        } else if (hasZ) {
            return new Coordinate(x, y, z);
        } else {
            return new Coordinate(x, y);
        }
    }

    private enum GeometryType {
        POINT,
        LINESTRING,
        POLYGON,
        MULTI_POINT,
        MULTI_LINESTRING,
        MULTI_POLYGON,
        MULTI_GEOMETRY;

        static GeometryType valueOf(int value) {
            if (value < 0 || value > values().length) {
                throw new IllegalArgumentException("Invalid geometry type: " + value);
            }
            return values()[value];
        }
    }
}

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
package org.jkiss.dbeaver.ext.clickhouse.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.locationtech.jts.geom.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public class ClickhouseGeometryValueHandler extends JDBCAbstractValueHandler {
    public static final ClickhouseGeometryValueHandler INSTANCE = new ClickhouseGeometryValueHandler();

    // https://clickhouse.com/docs/sql-reference/data-types/geo
    private static final String TYPE_POINT = "point";
    private static final String TYPE_RING = "ring";
    private static final String TYPE_LINESTRING = "linestring";
    private static final String TYPE_MULTILINESTRING = "multilinestring";
    private static final String TYPE_POLYGON = "polygon";
    private static final String TYPE_MULTIPOLYGON = "multipolygon";

    private ClickhouseGeometryValueHandler() {
    }

    public static boolean isGeometryType(@NotNull String typeName) {
        return switch (typeName.toLowerCase(Locale.ROOT)) {
            case TYPE_POINT, TYPE_RING, TYPE_LINESTRING, TYPE_MULTILINESTRING, TYPE_POLYGON, TYPE_MULTIPOLYGON -> true;
            default -> false;
        };
    }

    @Nullable
    @Override
    public Object getValueFromObject(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        @Nullable Object object,
        boolean copy,
        boolean validateValue
    ) throws DBCException {
        if (object == null) {
            return null;
        }
        var factory = new GeometryFactory(new PrecisionModel());
        var geometry = switch (type.getTypeName().toLowerCase(Locale.ROOT)) {
            case TYPE_POINT -> createPoint(factory, (double[]) object);
            case TYPE_RING -> createRing(factory, (double[][]) object);
            case TYPE_LINESTRING -> createLineString(factory, (double[][]) object);
            case TYPE_MULTILINESTRING -> createMultiLineString(factory, (double[][][]) object);
            case TYPE_POLYGON -> createPolygon(factory, (double[][][]) object);
            case TYPE_MULTIPOLYGON -> createMultiPolygon(factory, (double[][][][]) object);
            default -> throw new DBCException("Unexpected geo type: " + type.getTypeName(), null, session.getExecutionContext());
        };
        return new DBGeometry(geometry);
    }

    @Nullable
    @Override
    protected Object fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index
    ) throws DBCException, SQLException {
        return getValueFromObject(session, type, resultSet.getObject(index), false, false);
    }

    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value
    ) throws DBCException {
        throw new DBCException("Editing of geo data is not yet supported", null, session.getExecutionContext());
    }

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return DBGeometry.class;
    }

    @NotNull
    private static Point createPoint(@NotNull GeometryFactory factory, @NotNull double[] object) {
        return factory.createPoint(new Coordinate(object[0], object[1]));
    }

    @NotNull
    private static LinearRing createRing(@NotNull GeometryFactory factory, @NotNull double[][] object) {
        var coordinates = Stream.concat(Arrays.stream(object), Arrays.stream(object).limit(1))
            .map(point -> new Coordinate(point[0], point[1]))
            .toArray(Coordinate[]::new);
        return factory.createLinearRing(coordinates);
    }

    @NotNull
    private static LineString createLineString(@NotNull GeometryFactory factory, @NotNull double[][] object) {
        var coordinates = Arrays.stream(object)
            .map(point -> new Coordinate(point[0], point[1]))
            .toArray(Coordinate[]::new);
        return factory.createLineString(coordinates);
    }

    @NotNull
    private static MultiLineString createMultiLineString(@NotNull GeometryFactory factory, @NotNull double[][][] object) {
        var lineStrings = Arrays.stream(object)
            .map(lineString -> createLineString(factory, lineString))
            .toArray(LineString[]::new);
        return factory.createMultiLineString(lineStrings);
    }

    @NotNull
    private static Polygon createPolygon(@NotNull GeometryFactory factory, @NotNull double[][][] object) {
        var rings = Arrays.stream(object)
            .map(ring -> createRing(factory, ring))
            .toList();
        var shell = rings.get(0);
        var holes = rings.subList(1, rings.size()).toArray(LinearRing[]::new);
        return factory.createPolygon(shell, holes);
    }

    @NotNull
    private static MultiPolygon createMultiPolygon(@NotNull GeometryFactory factory, @NotNull double[][][][] object) {
        var polygons = Arrays.stream(object)
            .map(polygon -> createPolygon(factory, polygon))
            .toArray(Polygon[]::new);
        return factory.createMultiPolygon(polygons);
    }
}

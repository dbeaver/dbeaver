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
package org.jkiss.dbeaver.data.gis.handlers;

import org.cugos.wkg.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.gis.CircularArc;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.utils.CommonUtils;
import org.locationtech.jts.io.WKTReader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * WKG geometry utils
 */
public class WKGUtils {

    /**
     * Parses WKT (Well-known text) or its extension EWKT (Extended well-known text)
     *
     * @return parsed geometry
     * @throws DBCException on parse error
     */
    @NotNull
    public static DBGeometry parseWKT(@NotNull String wkt) throws DBCException {
        int srid = 0;

        if (wkt.startsWith("SRID=") && wkt.indexOf(';') > 5) {
            final int index = wkt.indexOf(';');
            srid = CommonUtils.toInt(wkt.substring(5, index));
            wkt = wkt.substring(index + 1);
        }

        final DBGeometry geometry;

        try {
            geometry = new DBGeometry(new WKTReader().read(wkt));
        } catch (Exception e) {
            throw new DBCException("Error parsing geometry value from string", e);
        }

        if (srid != 0) {
            geometry.setSRID(srid);
        }

        return geometry;
    }

    public static DBGeometry parseWKB(String hexString) throws DBCException {
        org.cugos.wkg.Geometry wkgGeometry = new WKBReader().read(hexString);
        if (wkgGeometry != null) {
            final int srid = CommonUtils.toInt(wkgGeometry.getSrid());
            // Nullify geometry's SRID so it's not included in its toString representation
            wkgGeometry.setSrid(null);
            return new DBGeometry(wkgGeometry, srid);
        }
        throw new DBCException("Invalid geometry object");
    }

    public static boolean isCurve(@Nullable Object value) {
        return value instanceof CircularString
            || value instanceof CompoundCurve
            || value instanceof CurvePolygon
            || value instanceof MultiCurve
            || value instanceof MultiSurface;
    }

    @NotNull
    public static Object linearize(@NotNull Geometry value) {
        // This value results in 32 segments per quadrant, the default tolerance for ST_CurveToLine
        return linearize(value, 0.001);
    }

    @NotNull
    public static Geometry linearize(@NotNull Geometry value, double tolerance) {
        if (value instanceof CircularString) {
            return convertCircularString((CircularString) value, tolerance);
        } else if (value instanceof CompoundCurve) {
            return convertCompoundCurve((CompoundCurve) value, tolerance);
        } else if (value instanceof CurvePolygon) {
            return convertCurvePolygon((CurvePolygon) value, tolerance);
        } else if (value instanceof MultiCurve) {
            return convertMultiCurve((MultiCurve) value, tolerance);
        } else if (value instanceof MultiSurface) {
            return convertMultiSurface((MultiSurface) value, tolerance);
        } else {
            return value;
        }
    }

    @NotNull
    private static LineString convertCircularString(@NotNull CircularString value, double tolerance) {
        final List<Coordinate> input = value.getCoordinates();
        final List<Coordinate> output = new ArrayList<>();

        for (int i = 2; i < input.size(); i += 2) {
            final CircularArc arc = new CircularArc(
                input.get(i - 2),
                input.get(i - 1),
                input.get(i)
            );

            output.addAll(arc.linearize(tolerance));
        }

        return new LineString(output, Dimension.Two, value.getSrid());
    }

    @NotNull
    private static LineString convertCompoundCurve(@NotNull CompoundCurve value, double tolerance) {
        final List<Coordinate> coordinates = value.getCurves().stream()
            .map(x -> linearize(x, tolerance))
            .flatMap(x -> x.getCoordinates().stream())
            .collect(Collectors.toList());

        return new LineString(coordinates, Dimension.Two, value.getSrid());
    }

    @NotNull
    private static Polygon convertCurvePolygon(@NotNull CurvePolygon value, double tolerance) {
        final LinearRing outerLinearRing = Stream.of(value.getOuterCurve())
            .map(x -> linearize(x, tolerance))
            .map(x -> new LinearRing(x.getCoordinates(), x.getDimension(), x.getSrid()))
            .findAny().get();

        final List<LinearRing> innerLinearRings = value.getInnerCurves().stream()
            .map(x -> linearize(x, tolerance))
            .map(x -> new LinearRing(x.getCoordinates(), x.getDimension(), x.getSrid()))
            .collect(Collectors.toList());

        return new Polygon(outerLinearRing, innerLinearRings, Dimension.Two, value.getSrid());
    }

    @NotNull
    private static MultiLineString convertMultiCurve(@NotNull MultiCurve value, double tolerance) {
        final List<LineString> strings = value.getCurves().stream()
            .map(x -> (LineString) linearize(x, tolerance))
            .collect(Collectors.toList());

        return new MultiLineString(strings, Dimension.Two, value.getSrid());
    }

    @NotNull
    private static MultiPolygon convertMultiSurface(@NotNull MultiSurface value, double tolerance) {
        final List<Polygon> polygons = value.getSurfaces().stream()
            .map(x -> (Polygon) linearize(x, tolerance))
            .collect(Collectors.toList());

        return new MultiPolygon(polygons, Dimension.Two, value.getSrid());
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.model.gis;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.util.Map;

/**
 * Wraps a Mimer SQL {@code BUILTIN.GIS_LOCATION}/{@code BUILTIN.GIS_COORDINATE} column's normal
 * value handler so it reports a {@link DBGeometry} point instead of a raw binary chunk - the
 * value-side half of {@link MimerGisLocationTransformer}/{@link MimerGisCoordinateTransformer}.
 * <p>
 * Read-only for now: only fetch-side conversion is implemented. Writing an edited point back
 * would need Mimer's numeric-only constructor ({@code BUILTIN.GIS_LOCATION(lat, long)}/{@code
 * BUILTIN.GIS_COORDINATE(x, y)}) embedded as a literal expression in the generated DML instead
 * of a bound parameter, which isn't wired up here.
 * <p>
 * Same "wrap the real handler with a {@link ProxyValueHandler}" shape as core's own {@code
 * GeometryAttributeTransformer}. The only difference is what {@link #getValueFromObject}/{@link
 * #fetchValueObject} convert the raw fetched value from: first a lookup in {@code precomputed}
 * (built once per fetched page by the owning transformer's own {@code transformAttribute}, see
 * {@link MimerGisUtils#precomputeBatch}), falling back to {@link MimerGisUtils#fetchComponents}'s
 * one-value round trip for anything the batch didn't cover - not a {@code WKBReader}/{@code
 * WKTReader} parse of the bytes themselves (see that class' Javadoc for why raw-byte decoding is
 * avoided entirely here).
 * <p>
 * <b>{@code asGeometry}.</b> The "auto map" and "grid text" preferences are independent -
 * turning the Spatial map off shouldn't also turn grid text back into raw binary - but the value
 * itself needs the exact same conversion either way, so both modes go through this one class
 * with a flag rather than two separate classes.
 * <p>
 * When {@code false} (grid-text-only, map preference off), {@link #getValueObjectType}/{@link
 * #fetchValueObject}/{@link #getValueFromObject} all delegate straight to the wrapped handler
 * unchanged. The underlying value stays exactly what it always was (raw bytes), so nothing ever
 * reports this column as a {@link DBGeometry} - that's what keeps the Spatial page from
 * appearing on its own. Only {@link #getValueDisplayString}, independently gated by its own
 * preference, still shows {@code POINT (...)} text, converting from the raw value on demand for
 * display purposes only and never touching the actual bound value.
 *
 * @author Mimer Information Technology
 */
class MimerGisPointValueHandler extends ProxyValueHandler {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private final Map<String, double[]> precomputed;
    private final String seedConstructor;
    private final String xAccessor;
    private final String yAccessor;
    private final int srid;
    private final boolean asGeometry;

    /**
     * @param precomputed     the current page's already-converted values, keyed by {@link
     *                        MimerGisUtils#toKey(byte[])} - may be empty (e.g. the batch itself
     *                        failed), in which case every value falls back to its own query
     * @param seedConstructor passed straight through to {@link MimerGisUtils#fetchComponents} -
     *                        a call to the type's own two-argument numeric constructor with
     *                        throwaway values, e.g. {@code "BUILTIN.GIS_LOCATION(0,0)"}
     * @param asGeometry      {@code true} for the full "auto map" mode (real {@link DBGeometry}
     *                        value, enables the Spatial page); {@code false} for "grid text
     *                        only" (value stays raw bytes, only display text is converted) - see
     *                        this class' own Javadoc
     */
    MimerGisPointValueHandler(
        @NotNull DBDValueHandler target,
        @NotNull Map<String, double[]> precomputed,
        @NotNull String seedConstructor,
        @NotNull String xAccessor,
        @NotNull String yAccessor,
        int srid,
        boolean asGeometry
    ) {
        super(target);
        this.precomputed = precomputed;
        this.seedConstructor = seedConstructor;
        this.xAccessor = xAccessor;
        this.yAccessor = yAccessor;
        this.srid = srid;
        this.asGeometry = asGeometry;
    }

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return asGeometry ? DBGeometry.class : super.getValueObjectType(attribute);
    }

    @Nullable
    @Override
    public Object fetchValueObject(
        @NotNull DBCSession session,
        @NotNull DBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index
    ) throws DBCException {
        if (!asGeometry) {
            return super.fetchValueObject(session, resultSet, type, index);
        }
        Object raw = super.fetchValueObject(session, resultSet, type, index);
        return getValueFromObject(session, type, raw, false, false);
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
        if (!asGeometry) {
            return super.getValueFromObject(session, type, object, copy, validateValue);
        }
        if (object instanceof DBGeometry g) {
            return copy ? g.copy() : g;
        }
        byte[] bytes = MimerGisUtils.extractBytes(object);
        if (bytes == null || bytes.length == 0) {
            return new DBGeometry();
        }
        double[] xy = precomputed.get(MimerGisUtils.toKey(bytes));
        if (xy == null) {
            // Not covered by the page this handler was built for (e.g. scrolled in afterward) -
            // same one-value round trip as before batching existed.
            xy = MimerGisUtils.fetchComponents(session, bytes, seedConstructor, xAccessor, yAccessor);
        }
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(xy[0], xy[1]));
        DBGeometry geometry = new DBGeometry(point);
        geometry.setSRID(srid);
        return geometry;
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        // Global preference (Preferences -> Mimer SQL, independent of the "auto map" one above)
        // - if off, always fall through to the plain binary display below.
        if (!MimerGisUtils.isGridPointTextEnabled()) {
            return super.getValueDisplayString(column, value, format);
        }
        if (value instanceof DBGeometry) {
            return String.valueOf(value);
        }
        // The plain Grid/Text pages render straight off whatever is already sitting in the
        // fetched row - still the raw bytes, since transformAttribute only ever builds a lookup
        // cache, it doesn't rewrite the already-fetched rows themselves (the Spatial page, by
        // contrast, explicitly re-runs getValueFromObject on demand - see GisTransformUtils - so
        // it never hits this gap). Without this, a value the batch already has the answer for
        // would still show as raw binary here - same fix core's own GISGeometryValueHandler
        // applies for the identical reason (its own getValueDisplayString also special-cases a
        // raw JDBCContentBytes value, not just an already-converted DBGeometry).
        byte[] bytes = MimerGisUtils.extractBytes(value);
        if (bytes != null && bytes.length > 0) {
            double[] xy = precomputed.get(MimerGisUtils.toKey(bytes));
            if (xy != null) {
                return GEOMETRY_FACTORY.createPoint(new Coordinate(xy[0], xy[1])).toString();
            }
        }
        return super.getValueDisplayString(column, value, format);
    }
}

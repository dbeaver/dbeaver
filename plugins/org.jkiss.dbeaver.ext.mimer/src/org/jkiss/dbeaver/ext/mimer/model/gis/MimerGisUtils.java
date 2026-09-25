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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentBytes;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared plumbing for reading one of Mimer SQL's four {@code BUILTIN.GIS_*} types' numeric
 * component(s): a pair for {@code GIS_LOCATION}/{@code GIS_COORDINATE} (via {@link
 * MimerGisPointValueHandler}), a single value for the scalar {@code GIS_LATITUDE}/{@code
 * GIS_LONGITUDE} (via {@link MimerGisScalarValueHandler}).
 * <p>
 * None of the four types have a documented text/WKT/WKB constructor, only numeric ones ({@code
 * BUILTIN.GIS_LOCATION(lat, long)}, {@code BUILTIN.GIS_COORDINATE(x, y)}, {@code
 * BUILTIN.GIS_LATITUDE(decimal)}, {@code BUILTIN.GIS_LONGITUDE(decimal)}), and their on-disk
 * {@code BINARY(4)}/{@code BINARY(8)} layout is undocumented. So instead of decoding the raw
 * bytes ourselves, this reconstructs each value through Mimer SQL's own type system: a small
 * side query hands the already-fetched raw bytes back to the server and calls the real accessor
 * methods - {@code LATITUDE()}/{@code LONGITUDE()}, {@code X()}/{@code Y()}, or plain {@code
 * AS_DECIMAL()} - to read the component(s) back out as plain numbers.
 * <p>
 * <b>How the bound parameter gets its type.</b> A plain {@code CAST(? AS BUILTIN.GIS_LOCATION)}
 * fails at {@code PREPARE} time with {@code com.mimer.jdbc.SQLException: Invalid MAE program} -
 * the compiler can't resolve a distinct-type {@code CAST} against a parameter with no
 * independently inferrable type. Comparing the bound parameter against a real, already-typed
 * value works instead, giving the compiler enough context to infer its type with no {@code
 * CAST} at all. So each query below unions the bound parameter(s) against a throwaway value
 * built with the type's own numeric constructor, purely to give the parameter something typed
 * to infer against.
 * <p>
 * <b>Batched, not one round trip per cell.</b> {@link #precomputeBatch} runs once per fetched
 * page - each of the four transformer classes hands it the whole page's rows via their own
 * {@code transformAttribute} - rather than once per cell. It collects every distinct raw value
 * in the column across the page and converts all of them in one query: a {@code UNION ALL} of
 * every distinct value against the same throwaway-seed trick, with an explicit {@code seq}
 * ordinal column to correlate each result row back to its parameter (plain {@code UNION ALL}
 * row order isn't otherwise guaranteed by the SQL standard). The value handlers look values up
 * in the resulting map by a hex-string key of their raw bytes. A value that isn't in the map -
 * say, a row fetched later by scrolling - falls back to {@link #fetchComponents}'s one-value-
 * at-a-time query, so correctness never depends on every value having been part of the initial
 * batch.
 *
 * @author Mimer Information Technology
 */
final class MimerGisUtils {

    private static final Log log = Log.getLog(MimerGisUtils.class);

    private MimerGisUtils() {
    }

    /**
     * Global switch for {@link MimerGisLocationTransformer}'s auto-apply behavior. Defaults to
     * {@code true} (today's behavior) via {@link #registerGisDefaults}.
     */
    static boolean isLocationAutoMapEnabled() {
        return readDefaultTrue(MimerConstants.PREF_GIS_LOCATION_AUTO_MAP);
    }

    /** Global switch for {@link MimerGisCoordinateTransformer}'s auto-apply behavior. */
    static boolean isCoordinateAutoMapEnabled() {
        return readDefaultTrue(MimerConstants.PREF_GIS_COORDINATE_AUTO_MAP);
    }

    /**
     * Global switch for {@link MimerGisLatitudeTransformer}/{@link MimerGisLongitudeTransformer}'s
     * auto-apply behavior - unlike the two point types, there's no Spatial-page angle here at
     * all (a lone latitude or longitude isn't a point to plot), just plain decimal-number display.
     */
    static boolean isLatLongAutoDecimalEnabled() {
        return readDefaultTrue(MimerConstants.PREF_GIS_LATLONG_AUTO_DECIMAL);
    }

    /**
     * Global switch for whether {@link MimerGisPointValueHandler#getValueDisplayString} shows
     * {@code POINT (...)} text in the data grid for {@code GIS_LOCATION}/{@code GIS_COORDINATE} -
     * independent of whether either type's own auto-apply switch above is on, since someone
     * might want the Spatial page without changing how the plain grid looks, or vice versa.
     */
    static boolean isGridPointTextEnabled() {
        return readDefaultTrue(MimerConstants.PREF_GIS_GRID_POINT_TEXT);
    }

    private static boolean readDefaultTrue(@NotNull String key) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        registerGisDefaults(store);
        return store.getBoolean(key);
    }

    /**
     * Registers the real, Eclipse-level default for every GIS preference. Idempotent, so it's
     * cheap enough to call on every read/write rather than once at some uncertain "startup"
     * point - a user may never open the Mimer SQL preference page before this runs.
     * <p>
     * This is <b>not</b> optional. Without a real registered default, {@code
     * DBPPreferenceStore#setValue(key, false)} compares the new value against Eclipse's own
     * unregistered-key fallback (always {@code false}), so unticking one of these checkboxes
     * would silently never persist. {@code PrefPageMimer} (`.ui`) makes the equivalent {@code
     * setDefault} calls itself on the write side - this method isn't exposed outside this
     * package, since `.ui` already has these constants via {@code MimerConstants}.
     * <p>
     * {@code GIS_COORDINATE}'s auto-map default is {@code false}, unlike the other three. The
     * transformed <em>value</em> defaults to on everywhere - a decoded number/point is strictly
     * more useful than raw binary - but the Spatial <em>map</em> only defaults to on for {@code
     * GIS_LOCATION}, a genuine geographic position, unlike {@code GIS_COORDINATE}'s bare local
     * x/y. Its grid-text preference still defaults to {@code true} independently, so an
     * out-of-the-box {@code GIS_COORDINATE} column reads as {@code POINT (...)} in the grid
     * without an unwanted Spatial tab - exactly the middle ground {@link
     * MimerGisPointValueHandler}'s {@code asGeometry} split exists for.
     */
    static void registerGisDefaults(@NotNull DBPPreferenceStore store) {
        store.setDefault(MimerConstants.PREF_GIS_LOCATION_AUTO_MAP, true);
        store.setDefault(MimerConstants.PREF_GIS_COORDINATE_AUTO_MAP, false);
        store.setDefault(MimerConstants.PREF_GIS_LATLONG_AUTO_DECIMAL, true);
        store.setDefault(MimerConstants.PREF_GIS_GRID_POINT_TEXT, true);
    }

    /**
     * Batch-converts every distinct raw value of the given column found in {@code rows} in one
     * round trip, keyed by {@link #toKey(byte[])}. Never throws - a failed batch (any reason,
     * including simply too many distinct values for one statement, an untested limit) just logs
     * and returns an empty map, so callers always have a safe (if unoptimized) fallback via
     * {@link #fetchComponents}.
     *
     * @param accessors one accessor expression per component wanted, e.g. {@code "X()", "Y()"}
     *                  for a point, or just {@code "AS_DECIMAL()"} for a scalar - each returned
     *                  {@code double[]} has exactly this many entries, in this order
     */
    @NotNull
    static Map<String, double[]> precomputeBatch(
        @NotNull DBCSession session,
        int ordinalPosition,
        @NotNull List<Object[]> rows,
        @NotNull String seedConstructor,
        @NotNull String... accessors
    ) {
        List<byte[]> distinctValues = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        for (Object[] row : rows) {
            if (ordinalPosition < 0 || ordinalPosition >= row.length) {
                continue;
            }
            byte[] bytes = extractBytes(row[ordinalPosition]);
            if (bytes != null && bytes.length > 0 && seenKeys.add(toKey(bytes))) {
                distinctValues.add(bytes);
            }
        }
        if (distinctValues.isEmpty()) {
            return Map.of();
        }
        try {
            return fetchComponentsBatch(session, distinctValues, seedConstructor, accessors);
        } catch (DBCException e) {
            log.debug("Batch " + seedConstructor + " conversion failed for " + distinctValues.size()
                + " value(s), falling back to one-at-a-time queries per cell", e);
            return Map.of();
        }
    }

    /**
     * Converts every value in {@code distinctValues} in a single round trip - see the class
     * Javadoc for the {@code UNION ALL ... seq} shape and why it's needed.
     */
    @NotNull
    private static Map<String, double[]> fetchComponentsBatch(
        @NotNull DBCSession session,
        @NotNull List<byte[]> distinctValues,
        @NotNull String seedConstructor,
        @NotNull String... accessors
    ) throws DBCException {
        if (!(session instanceof JDBCSession jdbcSession)) {
            throw new DBCException("Reading " + seedConstructor + " values needs a JDBC session");
        }
        StringBuilder sql = new StringBuilder("SELECT seq");
        for (String accessor : accessors) {
            sql.append(", p.").append(accessor);
        }
        sql.append(" FROM (SELECT 0 AS seq, ").append(seedConstructor).append(" AS p FROM SYSTEM.ONEROW");
        for (int i = 0; i < distinctValues.size(); i++) {
            sql.append(" UNION ALL SELECT ").append(i + 1).append(", ? AS p FROM SYSTEM.ONEROW");
        }
        sql.append(") t WHERE seq > 0 ORDER BY seq");
        Map<String, double[]> result = new HashMap<>();
        try (JDBCPreparedStatement stmt = jdbcSession.prepareStatement(sql.toString())) {
            for (int i = 0; i < distinctValues.size(); i++) {
                stmt.setBytes(i + 1, distinctValues.get(i));
            }
            try (JDBCResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    int seq = resultSet.getInt(1);
                    double[] values = new double[accessors.length];
                    for (int i = 0; i < accessors.length; i++) {
                        values[i] = resultSet.getDouble(i + 2);
                    }
                    result.put(toKey(distinctValues.get(seq - 1)), values);
                }
            }
        } catch (SQLException e) {
            throw new DBCException("Error reading " + distinctValues.size() + " " + seedConstructor + " value(s)", e);
        }
        return result;
    }

    /**
     * Reads a single value's component(s) from its raw bytes - the fallback path for a value
     * {@link #precomputeBatch} didn't cover.
     *
     * @param seedConstructor a call to the type's own numeric constructor with throwaway
     *                        values (e.g. {@code "BUILTIN.GIS_LOCATION(0,0)"}) - only used to
     *                        give the bound parameter below a type to be inferred against, never
     *                        returned
     */
    @NotNull
    static double[] fetchComponents(
        @NotNull DBCSession session,
        @NotNull byte[] rawValue,
        @NotNull String seedConstructor,
        @NotNull String... accessors
    ) throws DBCException {
        Map<String, double[]> result = fetchComponentsBatch(session, List.of(rawValue), seedConstructor, accessors);
        double[] values = result.get(toKey(rawValue));
        if (values == null) {
            throw new DBCException("No result reading " + seedConstructor + " value");
        }
        return values;
    }

    /** A content-based lookup key for a raw value - plain {@code byte[]} has no such equals/hashCode. */
    @NotNull
    static String toKey(@NotNull byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    @Nullable
    static byte[] extractBytes(@Nullable Object object) {
        if (object instanceof byte[] bytes) {
            return bytes;
        }
        if (object instanceof JDBCContentBytes contentBytes && !DBUtils.isNullValue(object)) {
            return contentBytes.getRawValue();
        }
        return null;
    }
}

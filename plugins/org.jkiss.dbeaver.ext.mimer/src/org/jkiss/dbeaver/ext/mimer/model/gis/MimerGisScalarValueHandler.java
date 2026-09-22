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
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Map;

/**
 * Wraps a Mimer SQL {@code BUILTIN.GIS_LATITUDE}/{@code BUILTIN.GIS_LONGITUDE} column's normal
 * value handler so it reports a plain {@link Double} instead of a raw binary chunk - the
 * scalar-typed sibling of {@link MimerGisPointValueHandler} (used for the two *point* types).
 * Neither scalar type is a geometry - there's no Spatial-page angle here at all, just a decoded
 * number where raw binary used to be, via the same {@link MimerGisUtils#precomputeBatch}/{@link
 * MimerGisUtils#fetchComponents} machinery (with a single accessor, {@code "AS_DECIMAL()"},
 * instead of the two a point needs).
 * <p>
 * Read-only, same reasoning as {@link MimerGisPointValueHandler}: writing an edited value back
 * would need {@code BUILTIN.GIS_LATITUDE(decimal)}/{@code BUILTIN.GIS_LONGITUDE(decimal)}
 * embedded as a literal expression in the generated DML, not wired up here.
 * <p>
 * <b>{@code asDecimal}</b> - same "auto-apply" vs "grid-text-only" split as {@link
 * MimerGisPointValueHandler}'s own {@code asGeometry} flag - see its Javadoc for the full
 * reasoning, which applies here identically (just {@code Double} in place of {@code DBGeometry}).
 *
 * @author Mimer Information Technology
 */
class MimerGisScalarValueHandler extends ProxyValueHandler {

    private final Map<String, double[]> precomputed;
    private final String seedConstructor;
    private final String accessor;
    private final boolean asDecimal;

    /**
     * @param precomputed     the current page's already-converted values, keyed by {@link
     *                        MimerGisUtils#toKey(byte[])} - may be empty (e.g. the batch itself
     *                        failed), in which case every value falls back to its own query
     * @param seedConstructor passed straight through to {@link MimerGisUtils#fetchComponents} -
     *                        a call to the type's own one-argument numeric constructor with a
     *                        throwaway value, e.g. {@code "BUILTIN.GIS_LATITUDE(0)"}
     * @param asDecimal       {@code true} for the full "auto-apply" mode (real {@code Double}
     *                        value); {@code false} for "grid text only" (value stays raw bytes,
     *                        only display text is converted) - see {@link
     *                        MimerGisPointValueHandler}'s own Javadoc for the full reasoning
     */
    MimerGisScalarValueHandler(
        @NotNull DBDValueHandler target,
        @NotNull Map<String, double[]> precomputed,
        @NotNull String seedConstructor,
        @NotNull String accessor,
        boolean asDecimal
    ) {
        super(target);
        this.precomputed = precomputed;
        this.seedConstructor = seedConstructor;
        this.accessor = accessor;
        this.asDecimal = asDecimal;
    }

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return asDecimal ? Double.class : super.getValueObjectType(attribute);
    }

    @Nullable
    @Override
    public Object fetchValueObject(
        @NotNull DBCSession session,
        @NotNull DBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index
    ) throws DBCException {
        if (!asDecimal) {
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
        if (!asDecimal) {
            return super.getValueFromObject(session, type, object, copy, validateValue);
        }
        if (object instanceof Double) {
            return object;
        }
        byte[] bytes = MimerGisUtils.extractBytes(object);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        double[] value = resolve(session, bytes);
        return value[0];
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        // No preference check needed here, unlike the point handler's own getValueDisplayString:
        // this class is only ever installed (see MimerGisLatitudeTransformer/
        // MimerGisLongitudeTransformer) when MimerGisUtils#isLatLongAutoDecimalEnabled() is
        // already true, and asDecimal is always true for a scalar (see this class' own Javadoc -
        // there's no "grid-text-only" half-mode the way there is for a point), so the value is
        // always already a real Double by the time display is needed.
        if (value instanceof Double d) {
            return String.valueOf(d);
        }
        byte[] bytes = MimerGisUtils.extractBytes(value);
        if (bytes != null && bytes.length > 0) {
            double[] cached = precomputed.get(MimerGisUtils.toKey(bytes));
            if (cached != null) {
                return String.valueOf(cached[0]);
            }
        }
        return super.getValueDisplayString(column, value, format);
    }

    @NotNull
    private double[] resolve(@NotNull DBCSession session, @NotNull byte[] bytes) throws DBCException {
        double[] cached = precomputed.get(MimerGisUtils.toKey(bytes));
        if (cached != null) {
            return cached;
        }
        // Not covered by the page this handler was built for (e.g. scrolled in afterward) - same
        // one-value round trip as before batching existed.
        return MimerGisUtils.fetchComponents(session, bytes, seedConstructor, accessor);
    }
}

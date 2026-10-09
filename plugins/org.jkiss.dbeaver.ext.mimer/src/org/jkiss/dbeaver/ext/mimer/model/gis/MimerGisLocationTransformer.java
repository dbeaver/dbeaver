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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.gis.GisConstants;
import org.jkiss.dbeaver.model.impl.data.transformers.TransformerPresentationAttribute;

import java.util.List;
import java.util.Map;

/**
 * Makes a {@code BUILTIN.GIS_LOCATION} column show up as a real point on the map - a geographic
 * lat/long pair, rendered the way any other {@code DBGeometry} column would be. Registered in
 * this plugin's {@code plugin.xml} with {@code custom="false" applyByDefault="true"} - unlike a
 * plain opt-in "View as..." transformer, that combination makes core apply it automatically to
 * every {@code BUILTIN.GIS_LOCATION} column with no manual step ({@code
 * DBVUtils#findBindingTransformers} only auto-applies a transformer when both flags say so).
 * <p>
 * Also where the batching happens - see {@link MimerGisUtils}' own Javadoc for the full story:
 * {@link #transformAttribute} is handed the whole currently-fetched page of rows at once (unlike
 * a plain per-cell value handler), so every distinct raw value in the column gets converted
 * together in one round trip via {@link MimerGisUtils#precomputeBatch}, instead of one round trip
 * per cell.
 * <p>
 * <b>Two independent preferences, not one</b> - "auto map" and "grid text" can each be toggled
 * independently, so the value handler is installed whenever *either* is on, passing {@link
 * MimerGisUtils#isLocationAutoMapEnabled()}'s value through as {@code MimerGisPointValueHandler}'s
 * {@code asGeometry} flag - see that class' Javadoc for what the flag actually changes (whether
 * the bound value itself becomes a real {@code DBGeometry}, or stays raw bytes with only the
 * display text converted).
 *
 * @author Mimer Information Technology
 */
public class MimerGisLocationTransformer implements DBDAttributeTransformer {

    public static final String GIS_TYPE_NAME = "Mimer.GIS_LOCATION";

    private static final String SEED_CONSTRUCTOR = "BUILTIN.GIS_LOCATION(0,0)";
    private static final String X_ACCESSOR = "LONGITUDE().AS_DECIMAL()";
    private static final String Y_ACCESSOR = "LATITUDE().AS_DECIMAL()";

    @Override
    public void transformAttribute(
        @NotNull DBCSession session,
        @NotNull DBDAttributeBinding attribute,
        @NotNull List<Object[]> rows,
        @NotNull Map<String, Object> options
    ) throws DBException {
        boolean autoMap = MimerGisUtils.isLocationAutoMapEnabled();
        boolean gridText = MimerGisUtils.isGridPointTextEnabled();
        if (!autoMap && !gridText) {
            // Both global preferences off (Preferences -> Mimer SQL) - leave the column
            // untouched, same as if this transformer didn't exist. No per-column override; see
            // MimerConstants#PREF_GIS_LOCATION_AUTO_MAP for why.
            return;
        }
        if (autoMap) {
            attribute.setPresentationAttribute(
                new TransformerPresentationAttribute(attribute, GIS_TYPE_NAME, -1, attribute.getDataKind()));
        }
        Map<String, double[]> precomputed = MimerGisUtils.precomputeBatch(
            session, attribute.getOrdinalPosition(), rows, SEED_CONSTRUCTOR, X_ACCESSOR, Y_ACCESSOR);
        attribute.setTransformHandler(new MimerGisPointValueHandler(
            attribute.getValueHandler(), precomputed, SEED_CONSTRUCTOR, X_ACCESSOR, Y_ACCESSOR, GisConstants.SRID_4326, autoMap));
    }
}

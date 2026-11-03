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
 * Makes a {@code BUILTIN.GIS_COORDINATE} column show up as a point on the geometry viewer - "a
 * distinct user-defined type ... used to store points in a two dimensional coordinate system"
 * (see "GIS_COORDINATE" in the <a href="https://docs.mimer.com/MimerSqlManual/latest">Mimer SQL
 * Manual</a>), with integer {@code X()}/{@code Y()} components. Unlike {@link
 * MimerGisLocationTransformer}'s {@code GIS_LOCATION}, this is a
 * bare local 2D point, not a geographic lat/long one, so it's registered with {@link
 * org.jkiss.dbeaver.model.gis.GisConstants#SRID_SIMPLE} (no real-world coordinate system) rather
 * than {@code SRID_4326} - the geometry viewer still plots it fine, just without a real map
 * behind it.
 * <p>
 * Auto-applies ({@code custom="false" applyByDefault="true"}), same as {@code GIS_LOCATION}, but
 * gated by its own separate preference ({@link MimerGisUtils#isCoordinateAutoMapEnabled},
 * defaulting to {@code false} - a bare local 2D point has no real-world backdrop to show, unlike
 * {@code GIS_LOCATION}'s genuine map, so it's opt-in). Controllable independently on the Mimer
 * SQL preferences page, since the two types are different enough - one geographic, one not -
 * that turning one on/off shouldn't affect the other. Same batching, and the same independent
 * auto-map-vs-grid-text handling, as {@link MimerGisLocationTransformer} - see its own Javadoc
 * for the full story on both.
 *
 * @author Mimer Information Technology
 */
public class MimerGisCoordinateTransformer implements DBDAttributeTransformer {

    public static final String GIS_TYPE_NAME = "Mimer.GIS_COORDINATE";

    private static final String SEED_CONSTRUCTOR = "BUILTIN.GIS_COORDINATE(0,0)";
    private static final String X_ACCESSOR = "X()";
    private static final String Y_ACCESSOR = "Y()";

    @Override
    public void transformAttribute(
        @NotNull DBCSession session,
        @NotNull DBDAttributeBinding attribute,
        @NotNull List<Object[]> rows,
        @NotNull Map<String, Object> options
    ) throws DBException {
        boolean autoMap = MimerGisUtils.isCoordinateAutoMapEnabled();
        boolean gridText = MimerGisUtils.isGridPointTextEnabled();
        if (!autoMap && !gridText) {
            return;
        }
        if (autoMap) {
            attribute.setPresentationAttribute(
                new TransformerPresentationAttribute(attribute, GIS_TYPE_NAME, -1, attribute.getDataKind()));
        }
        Map<String, double[]> precomputed = MimerGisUtils.precomputeBatch(
            session, attribute.getOrdinalPosition(), rows, SEED_CONSTRUCTOR, X_ACCESSOR, Y_ACCESSOR);
        attribute.setTransformHandler(new MimerGisPointValueHandler(
            attribute.getValueHandler(), precomputed, SEED_CONSTRUCTOR, X_ACCESSOR, Y_ACCESSOR, GisConstants.SRID_SIMPLE, autoMap));
    }
}

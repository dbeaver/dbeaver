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
import org.jkiss.dbeaver.model.impl.data.transformers.TransformerPresentationAttribute;

import java.util.List;
import java.util.Map;

/**
 * Makes a {@code BUILTIN.GIS_LATITUDE} column show up as a plain decimal number instead of a raw
 * binary chunk - not a point/geometry (that's {@link MimerGisLocationTransformer}/{@link
 * MimerGisCoordinateTransformer}), just a decoded number where garbled binary used to be. Per
 * the user, given the same treatment as `GIS_LOCATION`: {@code custom="false"
 * applyByDefault="true"} in `plugin.xml`, so it auto-applies with no "View as..." step, gated by
 * its own global preference ({@link MimerGisUtils#isLatLongAutoDecimalEnabled}) - see that
 * class' own Javadoc for why the flag combination alone is what makes a transformer auto-apply.
 * Same batching as the two point transformers - see {@link MimerGisUtils}' own Javadoc.
 * <p>
 * Always passes {@code true} for {@link MimerGisScalarValueHandler}'s {@code asDecimal} flag,
 * unlike the point transformers' own conditional {@code asGeometry} - a plain {@code Double}
 * can't trigger the Spatial page the way a {@code DBGeometry} value would, so there's no
 * equivalent "auto-apply vs. grid-text-only" distinction needed here: one preference, one mode.
 *
 * @author Mimer Information Technology
 */
public class MimerGisLatitudeTransformer implements DBDAttributeTransformer {

    public static final String GIS_TYPE_NAME = "Mimer.GIS_LATITUDE";

    private static final String SEED_CONSTRUCTOR = "BUILTIN.GIS_LATITUDE(0)";
    private static final String ACCESSOR = "AS_DECIMAL()";

    @Override
    public void transformAttribute(
        @NotNull DBCSession session,
        @NotNull DBDAttributeBinding attribute,
        @NotNull List<Object[]> rows,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (!MimerGisUtils.isLatLongAutoDecimalEnabled()) {
            return;
        }
        attribute.setPresentationAttribute(
            new TransformerPresentationAttribute(attribute, GIS_TYPE_NAME, -1, attribute.getDataKind()));
        Map<String, double[]> precomputed = MimerGisUtils.precomputeBatch(
            session, attribute.getOrdinalPosition(), rows, SEED_CONSTRUCTOR, ACCESSOR);
        attribute.setTransformHandler(new MimerGisScalarValueHandler(
            attribute.getValueHandler(), precomputed, SEED_CONSTRUCTOR, ACCESSOR, true));
    }
}

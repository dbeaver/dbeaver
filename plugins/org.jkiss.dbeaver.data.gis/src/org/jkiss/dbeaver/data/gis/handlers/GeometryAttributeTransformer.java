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
 */
package org.jkiss.dbeaver.data.gis.handlers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.gis.GisConstants;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.impl.data.transformers.TransformerPresentationAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Transforms string value into geometry
 */
public class GeometryAttributeTransformer implements DBDAttributeTransformer {

    private static final Log log = Log.getLog(GeometryAttributeTransformer.class);

    private static final String PROP_SRID = "srid";
    private static final String PROP_INVERT_COORDINATES = "invertCoordinates";

    public static final String GIS_TYPE_NAME = "GIS.Transformed";

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, Object> options) throws DBException {
        attribute.setPresentationAttribute(
            new TransformerPresentationAttribute(attribute, GIS_TYPE_NAME, -1, attribute.getDataKind()));

        int srid = CommonUtils.toInt(options.get(PROP_SRID));
        if (srid == 0) {
            srid = GisConstants.SRID_4326;
        }
        boolean invertCoordinates = CommonUtils.toBoolean(options.get(PROP_INVERT_COORDINATES ));
        attribute.setTransformHandler(new GISValueHandler(attribute.getValueHandler(), srid, invertCoordinates));
    }

    private class GISValueHandler extends ProxyValueHandler {
        private final GISGeometryValueHandler realHandler;

        private final int srid;

        public GISValueHandler(DBDValueHandler target, int srid, boolean invertCoordinates) {
            super(target);
            this.realHandler = new GISGeometryValueHandler();
            this.realHandler.setDefaultSRID(srid);
            this.realHandler.setInvertCoordinates(invertCoordinates);
            this.srid = srid;
        }

        @NotNull
        @Override
        public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
            return realHandler.getValueObjectType(attribute);
        }

        @Override
        public Object fetchValueObject(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull DBSTypedObject type, int index) throws DBCException {
            Object object = super.fetchValueObject(session, resultSet, type, index);
            return getValueFromObject(session, type, object, false, false);
        }

        @Nullable
        @Override
        public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue) throws DBCException {
            return realHandler.getValueFromObject(session, type, object, copy, false);
        }

    }

}

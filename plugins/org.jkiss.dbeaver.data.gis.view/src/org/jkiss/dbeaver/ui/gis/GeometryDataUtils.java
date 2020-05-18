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

package org.jkiss.dbeaver.ui.gis;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.gis.GisConstants;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.gis.internal.GISViewerActivator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GeometryDataUtils.
 */
public class GeometryDataUtils {

    private static final Log log = Log.getLog(GeometryDataUtils.class);


    public static class GeomAttrs {
        public final DBDAttributeBinding geomAttr;
        public final List<DBDAttributeBinding> descAttrs;

        public GeomAttrs(DBDAttributeBinding geomAttr, List<DBDAttributeBinding> descAttrs) {
            this.geomAttr = geomAttr;
            this.descAttrs = descAttrs;
        }

        public DBDAttributeBinding getGeomAttr() {
            return geomAttr;
        }

        public List<DBDAttributeBinding> getDescAttrs() {
            return descAttrs;
        }
    }

    public static List<GeomAttrs> extractGeometryAttributes(IResultSetController controller) {
        List<GeomAttrs> result = new ArrayList<>();
        ResultSetModel model = controller.getModel();
        List<DBDAttributeBinding> attributes = model.getVisibleAttributes();
        List<DBDAttributeBinding> descAttrs = new ArrayList<>();
        for (DBDAttributeBinding attr : attributes) {
            if (attr.getValueHandler().getValueObjectType(attr.getAttribute()) == DBGeometry.class) {
                GeomAttrs geomAttrs = new GeomAttrs(attr, descAttrs);
                result.add(geomAttrs);
            } else {
                descAttrs.add(attr);
            }
        }
//        if (result.size() == 1) {
//            result.get(0).descAttrs.addAll(descAttrs);
//        }
        return result;
    }

    public static void setGeometryProperties(IResultSetController controller, GeomAttrs geomAttrs, DBGeometry geometry, ResultSetRow row) {
        // Now extract all geom values from data
        ResultSetModel model = controller.getModel();
        if (row != null) {
            // Now get description
            if (!geomAttrs.descAttrs.isEmpty()) {
                Map<String, Object> properties = new LinkedHashMap<>();
                for (DBDAttributeBinding da : geomAttrs.descAttrs) {
                    Object descValue = model.getCellValue(da, row);
                    if (!DBUtils.isNullValue(descValue)) {
                        properties.put(da.getName(), descValue);
                    }
                }
                geometry.setProperties(properties);
            }
        }
    }

    public static int getDefaultSRID() {
        int srid = GISViewerActivator.getDefault().getPreferences().getInt(GeometryViewerConstants.PREF_DEFAULT_SRID);
        if (srid == 0) {
            return GisConstants.SRID_4326;
        }
        return srid;
    }

}

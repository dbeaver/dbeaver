/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.gis.panel.GISLeafletViewer;

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
                descAttrs = new ArrayList<>();
            } else {
                descAttrs.add(attr);
            }
        }
        if (result.size() == 1) {
            result.get(0).descAttrs.addAll(descAttrs);
        }
        return result;
    }

    public static void setGeometryProperties(IResultSetController controller, GeomAttrs geomAttrs, DBGeometry geometry) {
        // Now extract all geom values from data
        ResultSetModel model = controller.getModel();
        ResultSetRow currentRow = controller.getCurrentRow();
        if (currentRow != null) {
            Object value = model.getCellValue(geomAttrs.geomAttr, currentRow);
            // Now get description
            if (!geomAttrs.descAttrs.isEmpty()) {
                Map<String, Object> properties = new LinkedHashMap<>();
                for (DBDAttributeBinding da : geomAttrs.descAttrs) {
                    Object descValue = model.getCellValue(da, currentRow);
                    properties.put(da.getName(), descValue);
                }
                geometry.setProperties(properties);
            }
        }
    }


}

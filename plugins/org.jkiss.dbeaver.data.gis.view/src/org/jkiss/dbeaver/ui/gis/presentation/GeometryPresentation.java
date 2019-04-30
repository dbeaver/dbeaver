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

package org.jkiss.dbeaver.ui.gis.presentation;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.gis.GisTransformUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.gis.GeometryDataUtils;
import org.jkiss.dbeaver.ui.gis.panel.GISLeafletViewer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Geometry presentation.
 */
public class GeometryPresentation extends AbstractPresentation {

    private static final Log log = Log.getLog(GeometryPresentation.class);

    private GISLeafletViewer leafletViewer;

    @Override
    public void createPresentation(@NotNull final IResultSetController controller, @NotNull Composite parent) {
        super.createPresentation(controller, parent);

        leafletViewer = new GISLeafletViewer(parent, null);
        leafletViewer.getBrowserComposite().setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    @Override
    protected void applyThemeSettings() {
    }

    @Override
    public Composite getControl() {
        return leafletViewer.getBrowser();
    }

    @Override
    public void formatData(boolean refreshData) {

    }

    @Override
    public void clearMetaData() {
    }

    @Override
    public void updateValueView() {

    }

    @Override
    public void changeMode(boolean recordMode) {

    }

    @Override
    public void scrollToRow(@NotNull RowPosition position) {
        super.scrollToRow(position);
    }

    @Nullable
    @Override
    public DBDAttributeBinding getCurrentAttribute() {
        return controller.getModel().getDocumentAttribute();
    }

    @Nullable
    @Override
    public String copySelectionToString(ResultSetCopySettings settings) {
        return null;
    }

    ///////////////////////////////////////////////////////////////////////
    // ISelectionProvider

    @Override
    public ISelection getSelection() {
        return new StructuredSelection();
    }

    @Override
    public void setSelection(ISelection selection) {
    }

    @Override
    public void refreshData(boolean refreshMetadata, boolean append, boolean keepState) {
        controller.updateEditControls();

        List<GeometryDataUtils.GeomAttrs> result = GeometryDataUtils.extractGeometryAttributes(getController());
        ResultSetModel model = getController().getModel();

        // Now extract all geom values from data
        List<DBGeometry> geometries = new ArrayList<>();
        for (GeometryDataUtils.GeomAttrs geomAttrs : result) {
            for (ResultSetRow row : model.getAllRows()) {
                Object value = model.getCellValue(geomAttrs.geomAttr, row);

                DBGeometry geometry = GisTransformUtils.getGeometryValueFromObject(
                    controller.getDataContainer(),
                    geomAttrs.geomAttr.getValueHandler(),
                    geomAttrs.geomAttr,
                    value);

                if (geometry != null) {
                    geometries.add(geometry);
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
        }
        try {
            leafletViewer.setGeometryData(geometries.toArray(new DBGeometry[0]));
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Error rendering GIS data", "Error while rendering geometry data", e);
        }
    }


}

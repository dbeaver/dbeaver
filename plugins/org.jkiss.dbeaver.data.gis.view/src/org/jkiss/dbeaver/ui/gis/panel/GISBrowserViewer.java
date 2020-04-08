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
package org.jkiss.dbeaver.ui.gis.panel;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.gis.GisTransformUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetSelection;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.data.IDataController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.gis.GeometryDataUtils;
import org.jkiss.dbeaver.ui.gis.IGeometryViewer;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GISBrowserViewer extends BaseValueEditor<Browser> implements IGeometryViewer {

    private static final Log log = Log.getLog(GISBrowserViewer.class);

    private GISLeafletViewer leafletViewer;

    public GISBrowserViewer(IValueController controller) {
        super(controller);
    }
    
    @Override
    protected Browser createControl(Composite editPlaceholder)
    {
        leafletViewer = new GISLeafletViewer(
            editPlaceholder,
            valueController,
            GisTransformUtils.getSpatialDataProvider(valueController.getExecutionContext().getDataSource()));
        return leafletViewer.getBrowser();
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        List<DBGeometry> geometries = new ArrayList<>();
        IDataController dataController = valueController.getDataController();
        if (dataController instanceof IResultSetController) {
            IResultSetController resultSetController = (IResultSetController) dataController;

            DBSTypedObject valueType = valueController.getValueType();
            List<GeometryDataUtils.GeomAttrs> geomAttrs = null;
            if (valueType instanceof DBSAttributeBase) {
                geomAttrs = GeometryDataUtils.extractGeometryAttributes(resultSetController);
            }

            IResultSetSelection selection = resultSetController.getSelection();
            Object[] selectedValues = selection.toArray();
            if (ArrayUtils.isEmpty(selectedValues)) {
                selectedValues = new Object[] { value };
            }
            for (Object cell : selectedValues) {
                DBGeometry geometry;
                DBDAttributeBinding attr;
                ResultSetRow row;
                if (cell instanceof DBGeometry) {
                    attr = resultSetController.getActivePresentation().getCurrentAttribute();
                    row = resultSetController.getCurrentRow();
                    geometry = (DBGeometry) cell;
                } else {
                    attr = selection.getElementAttribute(cell);
                    row = selection.getElementRow(cell);
                    Object cellValue = resultSetController.getModel().getCellValue(attr, row);
                    geometry = GisTransformUtils.getGeometryValueFromObject(
                        valueController.getDataController().getDataContainer(),
                        valueController.getValueHandler(),
                        valueController.getValueType(),
                        cellValue);
                }

                if (geometry != null) {
                    geometries.add(geometry);

                    // Set properties
                    if (valueType instanceof DBSAttributeBase) {
                        for (GeometryDataUtils.GeomAttrs ga : geomAttrs) {
                            if (ga.geomAttr.matches(attr, false)) {
                                GeometryDataUtils.setGeometryProperties(resultSetController, ga, geometry, row);
                                break;
                            }
                        }
                    }
                    if (geometry.getProperties() == null) {
                        geometry.setProperties(Collections.singletonMap("Object", geometry.getSRID()));
                    }
                }
            }
        }
        leafletViewer.setGeometryData(geometries.toArray(new DBGeometry[0]));
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public Object extractEditorValue() throws DBCException {
        return leafletViewer.getCurrentValue();
    }

}
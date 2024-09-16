/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.data.IAttributeController;
import org.jkiss.dbeaver.ui.data.IDataController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.gis.GeometryDataUtils;
import org.jkiss.dbeaver.ui.gis.IGeometryViewer;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GISBrowserViewer extends BaseValueEditor<Browser> implements IGeometryViewer {

    private static final Log log = Log.getLog(GISBrowserViewer.class);

    private GISLeafletViewer leafletViewer;

    public GISBrowserViewer(IValueController controller) {
        super(controller);
    }
    
    @Nullable
    @Override
    protected Browser createControl(Composite editPlaceholder)
    {
        final IResultSetPresentation presentation;

        if (valueController.getDataController() instanceof IResultSetController) {
            presentation = ((IResultSetController) valueController.getDataController()).getActivePresentation();
        } else {
            presentation = null;
        }

        leafletViewer = new GISLeafletViewer(
            editPlaceholder,
            new DBDAttributeBinding[]{((IAttributeController) valueController).getBinding()},
            GisTransformUtils.getSpatialDataProvider(valueController.getExecutionContext().getDataSource()),
            presentation
        );

        return leafletViewer.getBrowser();
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        if (leafletViewer == null) {
            return;
        }
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

            final ResultSetModel model = resultSetController.getModel();
            final List<DBDAttributeBinding> leaves = model.getVisibleLeafAttributes();

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
                    Object cellValue = model.getCellValue(attr, row);
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
                        for (int i = 0; i < geomAttrs.size(); i++) {
                            final GeometryDataUtils.GeomAttrs ga = geomAttrs.get(i);
                            if (ga.geomAttr.matches(attr, false)) {
                                GeometryDataUtils.setGeometryProperties(resultSetController, ga, geometry, i, row);
                                break;
                            }
                        }
                    }

                    if (row != null && attr != null) {
                        final int leaf = leaves.indexOf(attr);
                        if (leaf >= 0) {
                            geometry.putProperties(Map.of("location", leaf + ":" + row.getRowNumber()));
                        }
                    }
                }
            }
        }
        leafletViewer.setGeometryData(geometries.toArray(new DBGeometry[0]));
    }

    @Override
    public void dispose() {
        // Edge uses callbacks which have a lowest priority in UI
        // So if dispose is sent during that operation this will lead to initialization
        // on already disposed composite, we don't want this at all
        // We can prevent this error by delaying dispose in independent thread operation to allow Edge to finish its
        // initialization to be disposed properly
        //FIXME That should be removed as soon as Edge will be fixed, this is an awfull hack
        if (leafletViewer.getBrowser() != null) {
            super.dispose();
        } else {
            new Job("Disposing browser") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    while (leafletViewer.isBrowserCreating()) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                    UIUtils.syncExec(GISBrowserViewer.super::dispose);
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }
    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Nullable
    @Override
    public Object extractEditorValue() throws DBCException {
        if (leafletViewer == null) {
            return null;
        }
        return leafletViewer.getCurrentValue();
    }

}

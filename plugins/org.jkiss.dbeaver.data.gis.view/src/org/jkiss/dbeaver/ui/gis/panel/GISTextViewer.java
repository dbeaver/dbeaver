/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.gis.GisAttribute;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.css.DBStyles;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.editors.StringInlineEditor;
import org.jkiss.dbeaver.ui.gis.IGeometryValueEditor;
import org.jkiss.dbeaver.ui.gis.IGeometryViewer;
import org.locationtech.jts.geom.Geometry;

/**
 * GISTextViewer.
 * Edits value as string. Also manages SRID.
*/
public class GISTextViewer extends StringInlineEditor implements IGeometryViewer, IGeometryValueEditor {

    private static final Log log = Log.getLog(GISTextViewer.class);

    private int valueSRID;
    private ToolBarManager toolBarManager;

    public GISTextViewer(IValueController controller) {
        super(controller);
    }

    @Override
    protected Control createControl(Composite editPlaceholder) {
        Composite composite = UIUtils.createPlaceholder(editPlaceholder, 1);
        CSSUtils.setCSSClass(composite, DBStyles.COLORED_BY_CONNECTION_TYPE);

        Composite controlPanel = UIUtils.createPlaceholder(composite, 1);
        controlPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        controlPanel.setLayout(new FillLayout());

        Control textControl = super.createControl(controlPanel);

        Composite bottomPanel = UIUtils.createPlaceholder(composite, 1);//new Composite(composite, SWT.NONE);
        bottomPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        CSSUtils.setCSSClass(bottomPanel, DBStyles.COLORED_BY_CONNECTION_TYPE);

        ToolBar bottomToolbar = new ToolBar(bottomPanel, SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);

        toolBarManager = new ToolBarManager(bottomToolbar);

        return textControl;
    }

    @Override
    public void primeEditorValue(Object value) throws DBException {
        super.primeEditorValue(value);
        valueSRID = 0;
        if (value instanceof Geometry) {
            this.valueSRID = ((Geometry) value).getSRID();
        } else if (value instanceof DBGeometry) {
            this.valueSRID = ((DBGeometry) value).getSRID();
        }
        if (valueSRID == 0) {
            DBSTypedObject column = valueController.getValueType();
            if (column instanceof GisAttribute) {
                valueSRID = ((GisAttribute) column).getAttributeGeometrySRID(new VoidProgressMonitor());
            }
        }

        updateToolBar();
    }

    private void updateToolBar() {
        toolBarManager.removeAll();
        toolBarManager.add(ActionUtils.makeActionContribution(new SelectCRSAction(this), true));

        toolBarManager.update(true);
    }

    @Override
    public Object extractEditorValue() throws DBCException {
        Object geometry = super.extractEditorValue();
        return geometry;
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {
        super.contributeActions(manager, controller);
    }

    @Override
    public Control getEditorControl() {
        return getControl();
    }

    @Override
    public int getValueSRID() {
        return valueSRID;
    }

    @Override
    public void setValueSRID(int srid) {
        this.valueSRID = srid;

        try {
            Object geometry = extractEditorValue();
            if (geometry instanceof DBGeometry) {
                ((DBGeometry) geometry).setSRID(valueSRID);
            } else if (geometry instanceof Geometry) {
                ((Geometry) geometry).setSRID(valueSRID);
            }
            valueController.updateValue(geometry, false);
        } catch (DBCException e) {
            log.error(e);
        }

        updateToolBar();
    }

    @Override
    public void refresh() {

    }

}

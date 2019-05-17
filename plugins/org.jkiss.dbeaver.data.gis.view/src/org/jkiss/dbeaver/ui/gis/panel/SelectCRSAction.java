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
package org.jkiss.dbeaver.ui.gis.panel;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.jkiss.dbeaver.model.gis.GisConstants;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.gis.GeometryDataUtils;
import org.jkiss.dbeaver.ui.gis.IGeometryValueEditor;

import java.util.List;

class SelectCRSAction extends Action implements IMenuCreator {

    private MenuManager menuManager;
    private IGeometryValueEditor valueEditor;

    public SelectCRSAction(IGeometryValueEditor valueEditor) {
        super("EPSG:" + valueEditor.getValueSRID(), Action.AS_DROP_DOWN_MENU);
        setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CHART_LINE));
        this.valueEditor = valueEditor;
    }

    @Override
    public void run() {
        SelectSRIDDialog manageCRSDialog = new SelectSRIDDialog(
            UIUtils.getActiveWorkbenchShell(),
            valueEditor.getValueSRID());
        if (manageCRSDialog.open() == IDialogConstants.OK_ID) {
            valueEditor.setValueSRID(manageCRSDialog.getSelectedSRID());
        }
    }

    @Override
    public IMenuCreator getMenuCreator() {
        return this;
    }

    @Override
    public void dispose() {
        if (menuManager != null) {
            menuManager.dispose();
            menuManager = null;
        }
    }

    @Override
    public Menu getMenu(Control parent) {
        if (menuManager == null) {
            menuManager = new MenuManager();
            menuManager.setRemoveAllWhenShown(true);
            menuManager.addMenuListener(manager -> {
                menuManager.add(new SetCRSAction(valueEditor, GeometryDataUtils.getDefaultSRID()));
                menuManager.add(new SetCRSAction(valueEditor, GisConstants.DEFAULT_OSM_SRID));
                menuManager.add(new Separator());
                List<Integer> recentSRIDs = GISEditorUtils.getRecentSRIDs();
                if (!recentSRIDs.isEmpty()) {
                    for (Integer recentSRID : recentSRIDs) {
                        menuManager.add(new SetCRSAction(valueEditor, recentSRID));
                    }
                    menuManager.add(new Separator());
                }
                menuManager.add(new Action("Other ...") {
                    @Override
                    public void run() {
                        SelectCRSAction.this.run();
                    }
                });
                menuManager.add(new Action("Configuration ...") {
                    @Override
                    public void run() {
                        new GISViewerConfigurationDialog(valueEditor.getEditorControl().getShell()).open();
                    }
                });
            });
        }
        return menuManager.createContextMenu(parent);
    }

    @Override
    public Menu getMenu(Menu parent) {
        return null;
    }
}

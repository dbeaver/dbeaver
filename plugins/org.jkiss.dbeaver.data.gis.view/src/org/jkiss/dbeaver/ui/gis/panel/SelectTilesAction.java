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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.MenuCreator;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.gis.IGeometryValueEditor;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;
import org.jkiss.dbeaver.ui.gis.registry.GeometryViewerRegistry;
import org.jkiss.dbeaver.ui.gis.registry.LeafletTilesDescriptor;

import java.util.stream.Stream;

class SelectTilesAction extends Action {
    private final IGeometryValueEditor valueEditor;

    SelectTilesAction(IGeometryValueEditor valueEditor) {
        super(valueEditor.getValueSRID() == 0 ? GISMessages.panel_select_tiles_action_text_plain : getActionText(), Action.AS_DROP_DOWN_MENU);
        setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.PICTURE));
        this.valueEditor = valueEditor;
    }

    private static String getActionText() {
        LeafletTilesDescriptor descriptor = GeometryViewerRegistry.getInstance().getDefaultLeafletTiles();
        if (descriptor == null) {
            return GISMessages.panel_select_tiles_action_no_tiles_selected;
        }
        return descriptor.getLabel();
    }

    @Override
    public boolean isEnabled() {
        return valueEditor.getValueSRID() != 0;
    }

    @Override
    public IMenuCreator getMenuCreator() {
        return new MenuCreator(control -> {
            MenuManager menuManager = new MenuManager();
            menuManager.setRemoveAllWhenShown(true);
            menuManager.addMenuListener(manager -> {
                if (!isEnabled()) {
                    return;
                }
                GeometryViewerRegistry registry = GeometryViewerRegistry.getInstance();
                Stream.concat(registry.getPredefinedLeafletTiles().stream(), registry.getUserDefinedLeafletTiles().stream())
                        .filter(LeafletTilesDescriptor::isVisible)
                        .forEach(tile -> menuManager.add(new SetTilesAction(valueEditor, tile)));
                if (!menuManager.isEmpty()) {
                    menuManager.add(new Separator());
                }
                menuManager.add(new Action(GISMessages.panel_select_tiles_action_manage_tiles_action) {
                    @Override
                    public void run() {
                        int result = new TilesManagementDialog(valueEditor.getEditorControl().getShell()).open();
                        if (result == IDialogConstants.OK_ID) {
                            valueEditor.refresh();
                        }
                    }
                });
            });
            return menuManager;
        });
    }
}

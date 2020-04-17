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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.MenuCreator;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.gis.IGeometryValueEditor;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;
import org.jkiss.dbeaver.ui.gis.registry.GeometryViewerRegistry;
import org.jkiss.dbeaver.ui.gis.registry.LeafletTilesDescriptor;

class SelectTilesAction extends Action {

    private IGeometryValueEditor valueEditor;

    SelectTilesAction(IGeometryValueEditor valueEditor) {
        super(valueEditor.getValueSRID() == 0 ? GISMessages.panel_select_tiles_action_text_plain : GeometryViewerRegistry.getInstance().getDefaultLeafletTiles().getLabel(), Action.AS_DROP_DOWN_MENU);
        setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.PICTURE));
        this.valueEditor = valueEditor;
    }

    @Override
    public boolean isEnabled() {
        return valueEditor.getValueSRID() != 0;
    }

    @Override
    public void run() {
        //valueEditor.refresh();
    }

    @Override
    public IMenuCreator getMenuCreator() {
        return new MenuCreator(control -> {
            MenuManager menuManager = new MenuManager();
            menuManager.setRemoveAllWhenShown(true);
            menuManager.addMenuListener(manager -> {
                if (valueEditor.getValueSRID() != 0) {
                    for (LeafletTilesDescriptor ld : GeometryViewerRegistry.getInstance().getLeafletTiles()) {
                        menuManager.add(new SetTilesAction(valueEditor, ld));
                    }
                }
            });
            return menuManager;
        });
    }

}

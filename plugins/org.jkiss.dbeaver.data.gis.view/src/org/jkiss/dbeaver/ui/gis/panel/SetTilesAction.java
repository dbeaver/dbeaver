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
import org.jkiss.dbeaver.ui.gis.IGeometryValueEditor;
import org.jkiss.dbeaver.ui.gis.registry.GeometryViewerRegistry;
import org.jkiss.dbeaver.ui.gis.registry.LeafletTilesDescriptor;

class SetTilesAction extends Action {
    private final IGeometryValueEditor valueEditor;
    private final LeafletTilesDescriptor tiles;

    public SetTilesAction(IGeometryValueEditor valueEditor, LeafletTilesDescriptor tiles) {
        super(tiles.getLabel(), AS_CHECK_BOX);
        this.valueEditor = valueEditor;
        this.tiles = tiles;
    }

    @Override
    public boolean isChecked() {
        return tiles == GeometryViewerRegistry.getInstance().getDefaultLeafletTiles();
    }

    @Override
    public void run() {
        GeometryViewerRegistry.getInstance().setDefaultLeafletTiles(tiles);
        valueEditor.refresh();
    }
}

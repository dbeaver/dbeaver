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
package org.jkiss.dbeaver.ui.gis.panel.actions;

import org.eclipse.jface.action.Action;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;
import org.jkiss.dbeaver.ui.gis.panel.GISLeafletViewer;
import org.jkiss.dbeaver.ui.gis.presentation.GeometryPresentation;

public class ConfigureLabelsAction extends Action {
    private final GISLeafletViewer viewer;

    public ConfigureLabelsAction(@NotNull GISLeafletViewer viewer) {
        super(GISMessages.panel_configure_labels_action_label, AS_PUSH_BUTTON);
        this.viewer = viewer;
    }

    @Override
    public void run() {
        final IResultSetPresentation presentation = viewer.getPresentation();
        final DBSEntity source = presentation != null ? presentation.getController().getModel().getSingleSource() : null;

        // We're using the "edit dictionary" dialog here, hence
        // potentially changing previously selected dictionary columns.
        // Seems suitable for now.

        if (source != null && new EditDictionaryPage(source).edit()) {
            // HACK: In order to construct new labels for geometries, we need
            // access to the result set. Viewers themselves don't have access,
            // so we instead cause a complete refresh of the presentation

            if (presentation instanceof GeometryPresentation) {
                presentation.refreshData(false, false, true);
            } else {
                presentation.getController().updatePanelsContent(false);
            }
        }
    }
}

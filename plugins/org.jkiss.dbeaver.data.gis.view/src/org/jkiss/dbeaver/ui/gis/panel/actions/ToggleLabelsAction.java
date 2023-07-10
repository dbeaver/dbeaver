/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;
import org.jkiss.dbeaver.ui.gis.panel.GISLeafletViewer;

public class ToggleLabelsAction extends Action {
    private final GISLeafletViewer viewer;

    public ToggleLabelsAction(@NotNull GISLeafletViewer viewer) {
        super(null, AS_CHECK_BOX);
        this.viewer = viewer;

        setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.SMALL_INFO));
        setToolTipText(GISMessages.panel_toggle_labels_action_label);
        setChecked(viewer.isShowLabels());
    }

    @Override
    public void run() {
        viewer.setShowLabels(isChecked());
    }
}

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
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.MenuCreator;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;
import org.jkiss.dbeaver.ui.gis.panel.GISLeafletViewer;

public class ToggleLabelsAction extends Action {
    private final GISLeafletViewer viewer;

    public ToggleLabelsAction(@NotNull GISLeafletViewer viewer) {
        super(null, AS_DROP_DOWN_MENU);
        this.viewer = viewer;

        setText(getActionText());
        setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.SMALL_INFO));
    }

    @Override
    public void run() {
        viewer.setShowLabels(!viewer.isShowLabels());
        setText(getActionText());
        viewer.updateToolbar();
    }

    @Override
    public IMenuCreator getMenuCreator() {
        return new MenuCreator(control -> {
            final MenuManager manager = new MenuManager();
            manager.setRemoveAllWhenShown(true);
            manager.addMenuListener(m -> manager.add(new ConfigureLabelsAction(viewer)));
            return manager;
        });
    }

    @NotNull
    private String getActionText() {
        return viewer.isShowLabels() ? GISMessages.panel_hide_labels_action_label : GISMessages.panel_show_labels_action_label;
    }
}

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
package org.jkiss.dbeaver.erd.ui.action;

import org.eclipse.gef3.Disposable;
import org.eclipse.gef3.palette.PaletteListener;
import org.eclipse.gef3.palette.ToolEntry;
import org.eclipse.gef3.ui.palette.PaletteViewer;
import org.eclipse.jface.action.Action;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.erd.ui.ERDIcon;
import org.jkiss.dbeaver.erd.ui.ERDUIUtils;
import org.jkiss.dbeaver.erd.ui.editor.tools.HandToolEntry;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * Action to cycle between hand tool and previously selected tool
 *
 * @see org.jkiss.dbeaver.erd.ui.editor.tools.HandToolEntry
 */
public class DiagramToggleHandAction extends Action implements PaletteListener, Disposable {
    public static final String CMD_TOGGLE_HAND = "org.jkiss.dbeaver.erd.toggleHand";

    private final PaletteViewer viewer;
    private final ToolEntry handTool;
    private ToolEntry previousTool;

    public DiagramToggleHandAction(@NotNull PaletteViewer viewer) {
        super("Toggle Hand Tool", AS_CHECK_BOX);
        this.viewer = viewer;
        this.viewer.addPaletteListener(this);
        this.handTool = (ToolEntry) ERDUIUtils.findPaletteEntry(viewer.getPaletteRoot(), HandToolEntry.ID);

        setId(CMD_TOGGLE_HAND);
        setActionDefinitionId(CMD_TOGGLE_HAND);
        setImageDescriptor(DBeaverIcons.getImageDescriptor(ERDIcon.MOVE));
    }

    @Override
    public void run() {
        if (viewer.getActiveTool() == handTool) {
            viewer.setActiveTool(previousTool);
        } else {
            viewer.setActiveTool(handTool);
        }
    }

    @Override
    public void activeToolChanged(PaletteViewer viewer, ToolEntry toolEntry) {
        if (toolEntry != handTool) {
            previousTool = toolEntry;
        }
        setChecked(toolEntry == handTool);
    }

    @Override
    public void dispose() {
        viewer.removePaletteListener(this);
    }
}

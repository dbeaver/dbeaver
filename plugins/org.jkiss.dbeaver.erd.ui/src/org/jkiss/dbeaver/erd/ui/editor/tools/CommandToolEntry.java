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
package org.jkiss.dbeaver.erd.ui.editor.tools;

import org.eclipse.gef.Tool;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.tools.SelectionTool;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

public class CommandToolEntry extends ToolEntry {

    private final String commandId;

    public CommandToolEntry(String commandId, String label, DBPImage image) {
        super(
            label,
            ActionUtils.findCommandDescription(commandId, UIUtils.getActiveWorkbenchWindow(), false),
            DBeaverIcons.getImageDescriptor(image),
            null);
        this.commandId = commandId;

        this.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
    }

    public Tool createTool() {
        return new ActionTool();
    }

    public class ActionTool extends SelectionTool {

        @Override
        protected String getCommandName() {
            return commandId;
        }

        @Override
        public void activate() {
            ActionUtils.runCommand(commandId, UIUtils.getActiveWorkbenchWindow());
            super.activate();
        }
    }

}

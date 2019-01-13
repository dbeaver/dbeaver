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
package org.jkiss.dbeaver.ext.erd.editor.tools;

import org.eclipse.gef.Tool;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.tools.AbstractTool;
import org.eclipse.gef.tools.SelectionTool;
import org.eclipse.jface.action.IAction;

public class ActionToolEntry extends ToolEntry {

    private final IAction action;

    public ActionToolEntry(IAction action) {
        super(action.getText().replace("&", ""), action.getDescription(), action.getImageDescriptor(), action.getImageDescriptor());
        this.action = action;

        this.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
    }

    public Tool createTool() {
        return new ActionTool();
    }

    public class ActionTool extends SelectionTool {

        @Override
        protected String getCommandName() {
            return action.getId();
        }

        @Override
        public void activate() {
            action.run();
            super.activate();
        }
    }

}

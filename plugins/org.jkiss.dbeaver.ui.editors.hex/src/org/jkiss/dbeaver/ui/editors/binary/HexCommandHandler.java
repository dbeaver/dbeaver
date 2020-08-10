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
package org.jkiss.dbeaver.ui.editors.binary;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.IActionConstants;

/**
 * HexCommandHandler
 */
public class HexCommandHandler extends AbstractHandler {

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Control control = (Control)HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
        HexEditControl hexControl = null;
        while (control != null) {
            if (control instanceof HexEditControl) {
                hexControl = (HexEditControl) control;
                break;
            }
            control = control.getParent();
        }
        if (hexControl == null) {
            return null;
        }

        Shell activeShell = HandlerUtil.getActiveShell(event);
        String actionId = event.getCommand().getId();
        switch (actionId) {
            case IWorkbenchCommandConstants.EDIT_COPY:
                hexControl.copy();
                break;
            case IWorkbenchCommandConstants.EDIT_PASTE:
            case IActionConstants.CMD_PASTE_SPECIAL:
                hexControl.paste();
                break;
        }


        return null;
    }

}
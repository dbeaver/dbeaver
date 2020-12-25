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
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages;

public class EmergentExitAction extends Action {

    private IWorkbenchWindow window;

    public EmergentExitAction(IWorkbenchWindow window) {
        super(CoreApplicationMessages.actions_menu_exit_emergency);
        this.window = window;
    }

    @Override
    public void run() {
        if (UIUtils.confirmAction(
            window == null ? null : window.getShell(),
            CoreApplicationMessages.actions_menu_exit_emergency,
            CoreApplicationMessages.actions_menu_exit_emergency_message,
            SWT.ICON_WARNING)) {
            System.exit(1);
        }
    }

}
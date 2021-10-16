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
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jface.action.Action;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.DBeaverApplication;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages;
import org.jkiss.utils.CommonUtils;

public class ResetWorkspaceStateAction extends Action
{
    private IWorkbenchWindow window;

    public ResetWorkspaceStateAction(IWorkbenchWindow window) {
        super(CoreApplicationMessages.actions_menu_reset_workspace_state_title);
        this.window = window;
    }

    @Override
    public void run()
    {
        if (UIUtils.confirmAction(
            window == null ? null : window.getShell(),
            CoreApplicationMessages.actions_menu_reset_workspace_state_title,
            CoreApplicationMessages.actions_menu_reset_workspace_state_message,
            DBIcon.STATUS_WARNING))
        {
            DBeaverApplication.getInstance().setResetWorkspaceOnRestart(true);
            IWorkbench workbench = PlatformUI.getWorkbench();
            Location instanceLoc = Platform.getInstanceLocation();

            String resetStateParameter = "-" + org.eclipse.e4.ui.workbench.IWorkbench.CLEAR_PERSISTED_STATE;
            String eclicpseCommands = System.getProperty("eclipse.commands");
            if (CommonUtils.isEmpty(eclicpseCommands)) {
                eclicpseCommands = resetStateParameter;
            } else {
                eclicpseCommands = eclicpseCommands.trim() + "\n" + resetStateParameter;
            }
            System.setProperty("eclipse.commands", eclicpseCommands);

            if (!IApplication.EXIT_RELAUNCH.equals(Workbench.setRestartArguments(instanceLoc.getURL().toString()))) {
                return;
            }
            workbench.restart(true);
        }
    }

}
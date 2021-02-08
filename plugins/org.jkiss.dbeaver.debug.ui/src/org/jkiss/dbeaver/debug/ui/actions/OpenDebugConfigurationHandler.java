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

package org.jkiss.dbeaver.debug.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.contextlaunching.ContextRunner;
import org.eclipse.debug.internal.ui.contextlaunching.LaunchingResourceManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.StructuredSelection;
import org.jkiss.dbeaver.debug.ui.DebugUI;

public class OpenDebugConfigurationHandler extends AbstractHandler implements IHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (LaunchingResourceManager.isContextLaunchEnabled(DebugUI.DEBUG_LAUNCH_GROUP_ID)) {
            ContextRunner.getDefault().launch(
                DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(
                    DebugUI.DEBUG_LAUNCH_GROUP_ID),
                false);
        }
        else {
            ILaunchConfiguration configuration = getLastLaunch();
            if (configuration == null) {
                DebugUITools.openLaunchConfigurationDialogOnGroup(DebugUIPlugin.getShell(), new StructuredSelection(), DebugUI.DEBUG_LAUNCH_GROUP_ID);
            } else {
                DebugUITools.launch(configuration, "debug", false);
            }
        }

        return null;
    }

    protected ILaunchConfiguration getLastLaunch() {
        return DebugUIPlugin.getDefault().getLaunchConfigurationManager().getFilteredLastLaunch(DebugUI.DEBUG_LAUNCH_GROUP_ID);
    }

}

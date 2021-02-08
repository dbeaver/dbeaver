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
package org.jkiss.dbeaver.debug.ui.internal;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.debug.ui.actions.LaunchAction;
import org.eclipse.debug.ui.actions.LaunchShortcutsAction;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.ui.DebugUI;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceMenuContributor;

import java.util.List;

public class DebugConfigurationMenuContributor extends DataSourceMenuContributor
{
    private static final Log log = Log.getLog(DebugConfigurationMenuContributor.class);

    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems)
    {
        // Fill recent debugs
        LaunchConfigurationManager launchConfigurationManager = DebugUIPlugin.getDefault().getLaunchConfigurationManager();
        LaunchHistory launchHistory = launchConfigurationManager.getLaunchHistory(DebugUI.DEBUG_LAUNCH_GROUP_ID);
        ILaunchConfiguration[] filteredConfigs = LaunchConfigurationManager.filterConfigs(launchHistory.getHistory());

        for (ILaunchConfiguration launch : filteredConfigs) {
            LaunchAction action = new LaunchAction(launch, "debug");
            menuItems.add(new ActionContributionItem(action));
        }

        // Fill configuration actions
        if (filteredConfigs.length > 0) {
            menuItems.add(new Separator());
        }
        menuItems.add(new ActionContributionItem(new LaunchShortcutsAction(DebugUI.DEBUG_LAUNCH_GROUP_ID)));
        menuItems.add(new ActionContributionItem(new DebugLaunchDialogAction()));
    }

}
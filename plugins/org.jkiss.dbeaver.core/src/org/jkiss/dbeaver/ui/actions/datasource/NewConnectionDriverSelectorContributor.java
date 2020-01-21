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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionDialog;

import java.util.List;

public class NewConnectionDriverSelectorContributor extends DataSourceMenuContributor
{
    private static final Log log = Log.getLog(NewConnectionDriverSelectorContributor.class);

    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems)
    {
        IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
        if (window == null) {
            return;
        }

        List<DBPDriver> allDrivers = DriverUtils.getAllDrivers();
        List<DBPDriver> recentDrivers = DriverUtils.getRecentDrivers(allDrivers, 10);
        for (DBPDriver driver : recentDrivers) {
            menuItems.add(new ActionContributionItem(new NewConnectionAction(window, driver)));
        }
        MenuManager allDriversMenu = new MenuManager("Other");
        for (DBPDriver driver : allDrivers) {
            if (recentDrivers.contains(driver)) {
                continue;
            }
            allDriversMenu.add(new NewConnectionAction(window, driver));
        }
        menuItems.add(allDriversMenu);
    }

    private static class NewConnectionAction extends Action
    {
        private IWorkbenchWindow window;
        private DBPDriver driver;

        public NewConnectionAction(IWorkbenchWindow window, DBPDriver driver) {
            super(driver.getName(), DBeaverIcons.getImageDescriptor(driver.getIcon()));
            this.window = window;
            this.driver = driver;
        }

        @Override
        public void run() {
            NewConnectionDialog.openNewConnectionDialog(window, driver);
        }
    }

}
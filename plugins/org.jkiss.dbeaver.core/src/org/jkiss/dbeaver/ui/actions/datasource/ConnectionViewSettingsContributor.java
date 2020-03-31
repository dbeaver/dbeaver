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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;

public class ConnectionViewSettingsContributor extends DataSourceMenuContributor
{
    private static final Log log = Log.getLog(ConnectionViewSettingsContributor.class);

    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems)
    {
        IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
        if (window == null) {
            return;
        }

        //menuItems.add(new ActionContributionItem(new NewConnectionAction(window, driver)));
    }

/*
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
*/

}
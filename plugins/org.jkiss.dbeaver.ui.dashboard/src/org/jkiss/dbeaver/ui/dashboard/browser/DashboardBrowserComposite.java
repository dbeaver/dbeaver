/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.dashboard.browser;

import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.ui.ActionBars;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardViewCompositeControl;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemContainer;
import org.jkiss.utils.CommonUtils;

/**
 * Dashboard browser composite
 */
public class DashboardBrowserComposite extends Composite implements DashboardViewCompositeControl {

    private final DashboardContainer viewContainer;
    private final DashboardItemContainer dashboardContainer;
    private Browser browser;

    public DashboardBrowserComposite(DashboardItemContainer dashboardContainer, DashboardContainer viewContainer, Composite parent, int style, Point preferredSize) {
        super(parent, style);

        initializeGlobalBrowser(viewContainer);
        this.dashboardContainer = dashboardContainer;
        this.viewContainer = viewContainer;

        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        this.browser = new Browser(this, SWT.NONE);
        this.browser.setLayoutData(new GridData(GridData.FILL_BOTH));

        DashboardItemConfiguration itemConfig = dashboardContainer.getItemDescriptor();
        String dashboardURL = itemConfig.getDashboardURL();
        if (!CommonUtils.isEmpty(dashboardURL)) {
            this.browser.setUrl(itemConfig.evaluateURL(dashboardURL, dashboardContainer.getProject(), dashboardContainer.getDataSourceContainer()));
        }
    }

    private void closeBrowser() {

    }

    public Browser getBrowser() {
        return browser;
    }

    @Override
    public Control getDashboardControl() {
        return browser;
    }

    private static void initializeGlobalBrowser(DashboardContainer viewContainer) {
        IActionBars actionBars = ActionBars.extractActionBars(viewContainer.getWorkbenchSite());
        if (actionBars != null) {
            IStatusLineManager statusLineManager = actionBars.getStatusLineManager();
            if (statusLineManager != null) {
                if (statusLineManager instanceof SubStatusLineManager sslm) {
                    statusLineManager = (IStatusLineManager) sslm.getParent();
                }
                for (IContributionItem item : statusLineManager.getItems()) {
                    if (item instanceof BrowserContributionItem bci) {
                        return;
                    }
                }
                // Create global browser control
                // It will be disposed when entire application is disposed
                BrowserContributionItem item = new BrowserContributionItem();
                statusLineManager.add(item);
            }
        }
    }


    private static class BrowserContributionItem extends ContributionItem {
        private Composite globalComposite;

        @Override
        public void fill(Composite parent) {
            globalComposite = new Browser(parent, SWT.NONE);
            globalComposite.setLayout(new RowLayout());
            StatusLineLayoutData ld = new StatusLineLayoutData();
            ld.widthHint = 0;
            ld.heightHint = 0;
            globalComposite.setLayoutData(ld);
        }
    }
}

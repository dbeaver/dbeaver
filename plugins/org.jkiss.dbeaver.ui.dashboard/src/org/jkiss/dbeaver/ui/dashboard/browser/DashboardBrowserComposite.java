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

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
        this.dashboardContainer = dashboardContainer;
        this.viewContainer = viewContainer;

        setLayout(new GridLayout(1, false));

        this.browser = new Browser(this, SWT.NONE);
        this.browser.setLayoutData(new GridData(GridData.FILL_BOTH));

        String dashboardURL = dashboardContainer.getItemDescriptor().getDashboardURL();
        if (!CommonUtils.isEmpty(dashboardURL)) {
            this.browser.setUrl(dashboardURL);
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
}

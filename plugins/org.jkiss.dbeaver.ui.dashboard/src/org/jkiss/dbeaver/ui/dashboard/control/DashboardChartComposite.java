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

package org.jkiss.dbeaver.ui.dashboard.control;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.charts.BaseChartComposite;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardChartConfigDialog;

/**
 * Dashboard chart composite
 */
public class DashboardChartComposite extends BaseChartComposite {

    private final DashboardContainer dashboardContainer;

    public DashboardChartComposite(DashboardContainer dashboardContainer, Composite parent, int style, Point preferredSize) {
        super(parent, style, preferredSize);
        this.dashboardContainer = dashboardContainer;
    }

    protected boolean showChartConfigDialog() {
        DashboardChartConfigDialog dialog = new DashboardChartConfigDialog(this, dashboardContainer);
        return dialog.open() == IDialogConstants.OK_ID;
    }

}

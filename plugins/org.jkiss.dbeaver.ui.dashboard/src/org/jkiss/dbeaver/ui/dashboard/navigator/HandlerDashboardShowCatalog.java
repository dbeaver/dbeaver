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
package org.jkiss.dbeaver.ui.dashboard.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardListViewer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewer;

import static org.jkiss.dbeaver.ui.dashboard.DashboardUIConstants.PARAM_CATALOG_PANEL_TOGGLE;

public class HandlerDashboardShowCatalog extends HandlerDashboardAbstract {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DashboardViewer view = getActiveDashboardView(event);
        if (view != null) {
            DashboardListViewer listViewer = view.getDashboardListViewer();
            if (event.getParameter(PARAM_CATALOG_PANEL_TOGGLE) != null) {
                if (listViewer.isVisible()) {
                    listViewer.hideChartCatalog();
                } else {
                    listViewer.showChartCatalog();
                }
            }
        }
        return null;
    }
}

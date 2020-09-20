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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;

public class HandlerDashboardOpen extends AbstractDataSourceHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        DBPDataSourceContainer dataSourceContainer = getActiveDataSourceContainer(event, false);
        if (dataSourceContainer == null) {
            dataSourceContainer = getActiveDataSourceContainer(event, true);
        }
        if (dataSourceContainer == null) {
            DBWorkbench.getPlatformUI().showError(UIDashboardMessages.error_dashboard_view_no_connection_title, UIDashboardMessages.error_dashboard_view_no_connection_msg);
            return null;
        }
        DashboardView.openView(workbenchWindow, dataSourceContainer);
        return null;
    }

}
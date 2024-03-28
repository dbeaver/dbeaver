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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.List;

public class DashboardOpenToolCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        List<DBSObject> selectedObjects = NavigatorUtils.getSelectedObjects(HandlerUtil.getCurrentSelection(event));
        // Just open dashboard view
        if (selectedObjects.isEmpty()) {
            return null;
        }
        DBSObject object = selectedObjects.iterator().next();
        DBPDataSource dataSource = object.getDataSource();
        if (dataSource == null) {
            return null;
        }
        DBPDataSourceContainer dataSourceContainer = dataSource.getContainer();
        return DataSourceDashboardView.openView(HandlerUtil.getActiveWorkbenchWindow(event), dataSourceContainer.getProject(), dataSourceContainer, null);
    }

}
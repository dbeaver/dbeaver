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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.utils.CommonUtils;

public class HandlerDashboardCreate extends AbstractDataSourceHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        DBPDataSourceContainer dataSourceContainer = null;
        if (CommonUtils.toBoolean(event.getParameter("datasource"))) {
            dataSourceContainer = getDataSourceContainerFromPart(HandlerUtil.getActivePart(event));
        }

        ActiveWizardDialog dialog = new ActiveWizardDialog(
            HandlerUtil.getActiveWorkbenchWindow(event),
            new DashboardCreateWizard(dataSourceContainer));
        dialog.open();

        return null;
    }

}
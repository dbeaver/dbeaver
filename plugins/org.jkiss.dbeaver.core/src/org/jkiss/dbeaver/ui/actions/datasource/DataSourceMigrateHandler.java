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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.MigrateConnectionWizard;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;

public class DataSourceMigrateHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        NavigatorViewBase navigatorView = NavigatorUtils.getActiveNavigatorView(event);
        if (navigatorView != null && navigatorView.getNavigatorViewer() != null) {
            final ISelection currentSelection = navigatorView.getNavigatorViewer().getSelection();
            ActiveWizardDialog dialog = new ActiveWizardDialog(
                HandlerUtil.getActiveWorkbenchWindow(event),
                new MigrateConnectionWizard(
                    DBWorkbench.getPlatform().getWorkspace().getActiveProject().getDataSourceRegistry(),
                    currentSelection instanceof IStructuredSelection ? (IStructuredSelection) currentSelection : null));
            dialog.open();
        }

        return null;
    }
}
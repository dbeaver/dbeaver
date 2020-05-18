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
package org.jkiss.dbeaver.ui.app.standalone;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionDialog;

public class WorkbenchInitializerNewConnection implements IWorkbenchWindowInitializer {
    @Override
    public void initializeWorkbenchWindow(IWorkbenchWindow window) {
        // Check for initialized projects
        // In case of advanced security (and master password) workbench initializer can be called earlier
        // than project registries load.
        if (!DataSourceRegistry.isProjectsInitialized() || !DataSourceRegistry.getAllDataSources().isEmpty()) {
            return;
        }
        NewConnectionDialog.openNewConnectionDialog(window);
    }
}


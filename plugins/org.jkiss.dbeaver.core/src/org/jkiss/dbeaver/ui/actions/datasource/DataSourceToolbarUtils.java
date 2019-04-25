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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimElement;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;

public class DataSourceToolbarUtils
{

    public static DBPDataSourceContainer getCurrentDataSource(IWorkbenchWindow workbenchWindow) {
        if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
            return null;
        }
        IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor == null) {
            return null;
        }

        if (activeEditor instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider) activeEditor).getDataSourceContainer();
        }
        return null;
    }

    public static void refreshSelectorToolbar(IWorkbenchWindow window) {
        if (window instanceof WorkbenchWindow) {
            MTrimBar topTrim = ((WorkbenchWindow) window).getTopTrim();
            for (MTrimElement element : topTrim.getChildren()) {
                if ("dbeaver-connection-selector".equals(element.getElementId())) {
                    if (element instanceof MElementContainer) {
                        MElementContainer<? extends MUIElement> container = (MElementContainer<? extends MUIElement>)element;
                        for (MUIElement tbItem : container.getChildren()) {
                            tbItem.setVisible(false);
                        }
                        for (MUIElement tbItem : container.getChildren()) {
                            tbItem.setVisible(true);
                        }
                    }
                    return;
                }
            }
        }
        // By some reason we can't locate the toolbar (#5712?). Let's just refresh elements then - its better than nothing
        updateCommandsUI();
    }

    public static void updateCommandsUI() {
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        commandService.refreshElements("org.jkiss.dbeaver.ui.tools.select.connection", null);
        commandService.refreshElements("org.jkiss.dbeaver.ui.tools.select.schema", null);
        commandService.refreshElements("org.jkiss.dbeaver.ui.editors.sql.sync.connection", null);
    }
}
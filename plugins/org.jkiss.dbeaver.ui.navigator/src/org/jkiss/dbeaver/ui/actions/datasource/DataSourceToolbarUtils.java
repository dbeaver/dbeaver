/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.eclipse.e4.ui.model.application.ui.menu.MHandledItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceContainerProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.ConnectionCommands;
import org.jkiss.dbeaver.ui.editors.EditorUtils;

public class DataSourceToolbarUtils {

    public static final String CONNECTION_SELECTOR_TOOLBAR_ID = "dbeaver-connection-selector";

    public static DBPDataSourceContainer getCurrentDataSource(IWorkbenchWindow workbenchWindow) {
        if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
            return null;
        }
        IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor == null) {
            return null;
        }

        if (activeEditor instanceof DBPDataSourceContainerProvider dscp) {
            return dscp.getDataSourceContainer();
        }
        return null;
    }

    public static void refreshSelectorToolbar(IWorkbenchWindow window) {
        if (window instanceof WorkbenchWindow && window.getActivePage() != null) {
            MTrimBar topTrim = ((WorkbenchWindow) window).getTopTrim();
            boolean showConnectionSelector = false;
            boolean showSchemaSelector = false;
            IEditorPart activeEditor = window.getActivePage().getActiveEditor();
            DBPDataSourceContainer dataSourceContainer = null;
            if (activeEditor instanceof DBPDataSourceContainerProvider dscp) {
                showConnectionSelector = true;
                dataSourceContainer = dscp.getDataSourceContainer();
            }
            if (dataSourceContainer != null && dataSourceContainer.isConnected()) {
                // Show schema selector only for active connections which
                // support schema read or write
                showSchemaSelector = isSchemasSupported(dataSourceContainer);
            }
            DBPProject resourceProj = activeEditor == null ? null : EditorUtils.getFileProject(activeEditor.getEditorInput());
            boolean canChangeConn = resourceProj == null || resourceProj.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT);

            Color bgColor = dataSourceContainer == null ?
                null :
                UIUtils.getConnectionTypeColor(dataSourceContainer.getConnectionConfiguration().getConnectionType());

            for (MTrimElement element : topTrim.getChildren()) {
                if (CONNECTION_SELECTOR_TOOLBAR_ID.equals(element.getElementId())) {
                    if (element instanceof MElementContainer) {
                        MElementContainer<? extends MUIElement> container = (MElementContainer<? extends MUIElement>) element;
                        Object widget = element.getWidget();
                        if (widget instanceof Composite controlsPanel) {
                            Control[] childControl = controlsPanel.getChildren();
                            for (Control cc : childControl) {
                                cc.setBackground(bgColor);
                                cc.setEnabled(showConnectionSelector && canChangeConn);
                            }
                        }

                        for (MUIElement tbItem : container.getChildren()) {
                            // Handle Eclipse bug. By default, it doesn't update contents of main toolbar elements
                            // So we need to hide/show it to force text update
                            if (showConnectionSelector) {
                                tbItem.setVisible(false);
                            }
                            if (tbItem instanceof MHandledItem hi) {
                                String commandId = hi.getCommand().getElementId();
                                if (ConnectionCommands.CMD_SELECT_SCHEMA.equals(commandId)) {
                                    tbItem.setVisible(showSchemaSelector);
                                } else {
                                    tbItem.setVisible(showConnectionSelector);
                                }
                            }
                        }
                    }
                    return;
                }
            }
        }
        // By some reason we can't locate the toolbar (#5712?). Let's just refresh elements then - its better than nothing
        updateCommandsUI();
    }

    public static boolean isSchemasSupported(DBPDataSourceContainer dataSourceContainer) {
        DBCExecutionContext defaultContext = DBUtils.getDefaultContext(dataSourceContainer, false);
        if (defaultContext != null) {
            DBCExecutionContextDefaults<?,?> contextDefaults = defaultContext.getContextDefaults();
            if (contextDefaults != null) {
                if (contextDefaults.getDefaultSchema() != null || contextDefaults.getDefaultCatalog() != null ||
                    contextDefaults.supportsSchemaChange() || contextDefaults.supportsCatalogChange()
                ) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void updateCommandsUI() {
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements(ConnectionCommands.CMD_SELECT_CONNECTION, null);
            commandService.refreshElements(ConnectionCommands.CMD_SELECT_SCHEMA, null);
            //commandService.refreshElements("org.jkiss.dbeaver.ui.editors.sql.sync.connection", null);
        }
    }

    public static void triggerRefreshReadonlyElement() {
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements(ConnectionCommands.CMD_READONLY, null);
        }
    }

}